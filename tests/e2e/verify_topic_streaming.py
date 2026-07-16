#!/usr/bin/env python3
"""引导模式话题步骤：折叠按钮 → 点击 → 600ms 打字 → 6 标题渐显 → 换一批 → 选标题。
首屏是 Kimi 风格 hero，灵感气泡在「改主题」按钮触发后出现。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

MOCK = [
    {"id": 1, "title": "测试主题 A", "summary": "概要 A"},
    {"id": 2, "title": "测试主题 B", "summary": "概要 B"},
    {"id": 3, "title": "测试主题 C", "summary": "概要 C"},
    {"id": 4, "title": "测试主题 D", "summary": "概要 D"},
    {"id": 5, "title": "测试主题 E", "summary": "概要 E"},
    {"id": 6, "title": "测试主题 F", "summary": "概要 F"},
]

batch_state = {"n": 0}


def on_topics(route):
    batch_state["n"] += 1
    payload = [{"id": i + batch_state["n"] * 100, "title": t["title"] + (" (新)" if batch_state["n"] > 1 else ""), "summary": t["summary"]} for i, t in enumerate(MOCK)]
    route.fulfill(json={"code": 0, "data": payload})


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                                "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"bizNo": "S1", "name": "轻松口语", "description": "x", "promptSummary": "x", "prompt": "x", "scope": "通用"}]}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
        page.route("**/api/v1/user/topics/random*", on_topics)

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        # 先走完 4 步到确认卡
        page.fill(".hero-input", "测试主题")
        page.keyboard.press("Enter")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('公众号')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('轻松口语')")
        page.wait_for_timeout(300)
        page.click(".quick-confirm")
        page.wait_for_timeout(500)
        page.click(".quick-option:has-text('公众号深度文')")
        page.wait_for_timeout(300)
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # 点「改主题」→ 触发 TopicSuggestionBubble
        page.click(".confirm-edit:has-text('改主题')")
        page.wait_for_timeout(500)

        ok_collapsed = page.query_selector(".inspire-btn") is not None
        page.click(".inspire-btn")
        page.wait_for_timeout(100)
        ok_typing = page.query_selector(".typing-cursor") is not None
        page.wait_for_selector(".refresh-suggestion", timeout=8000, state="visible")
        page.wait_for_timeout(200)

        cards = page.query_selector_all(".suggestion-title-card")
        visible = [c for c in cards if c.is_visible()]
        ok_six = len(visible) == 6

        visible[0].query_selector(".suggestion-title").click()
        page.wait_for_timeout(400)
        ok_user = page.query_selector(".chat-msg.user") is not None

        page.screenshot(path=f"{SHOTS}/topic_streaming.png")

        before_refresh_n = batch_state["n"]
        page.click(".refresh-suggestion")
        page.wait_for_selector(".refresh-suggestion", timeout=5000)
        ok_refresh = batch_state["n"] == before_refresh_n + 1

        for n, ok in [("collapsed", ok_collapsed), ("typing", ok_typing), ("six-titles", ok_six),
                       ("user-bubble", ok_user), ("refresh-fired", ok_refresh)]:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all([ok_collapsed, ok_typing, ok_six, ok_user, ok_refresh]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
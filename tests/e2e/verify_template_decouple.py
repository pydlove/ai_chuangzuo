#!/usr/bin/env python3
"""平台-模板解耦：选完平台不应自动设 selectedTemplateKey。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                                "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/topics/random*", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"bizNo": "S1", "name": "轻松口语", "description": "x", "promptSummary": "x", "prompt": "x", "scope": "通用"}]}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat",
                 "bgColor": "#fff", "textColor": "#262626", "visualStyle": {}, "description": "微信长文"},
                {"templateKey": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu",
                 "bgColor": "#fff0f2", "textColor": "#1a1a1a", "visualStyle": {}, "description": "小红书种草"},
                {"templateKey": "general-story", "name": "故事卡片", "platform": "general",
                 "bgColor": "#fafafa", "textColor": "#1a1a1a", "visualStyle": {}, "description": "通用"}
            ]}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        page.fill(".topic-input", "测试")
        page.keyboard.press("Enter")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('小红书')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        # 模板步骤 — 故意选一个非平台前缀
        page.click(".quick-option:has-text('故事卡片')")
        page.wait_for_timeout(200)
        page.click(".quick-confirm")
        page.wait_for_timeout(400)

        confirm_text = page.query_selector(".confirm-card").inner_text()
        ok_decouple = "故事卡片" in confirm_text
        ok_no_autopick = "公众号深度文" not in confirm_text

        page.screenshot(path=f"{SHOTS}/template_decouple.png")
        for n, ok in [("decouple", ok_decouple), ("no-autopick", ok_no_autopick)]:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not (ok_decouple and ok_no_autopick):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

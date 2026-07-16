#!/usr/bin/env python3
"""验证引导模式首屏是 Kimi 风格 hero：居中大字 + 宽输入框 + 圆形发送。"""
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
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
        page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化", "prompt": "...", "scope": "通用"}]}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat",
             "bgColor": "#fff", "textColor": "#262626"},
            {"templateKey": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu",
             "bgColor": "#fff0f2", "textColor": "#1a1a1a"}]}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        # 1. 首屏应该有 hero（没有 chat-list）
        ok_hero_visible = page.query_selector(".guided-hero") is not None
        ok_no_chat = page.query_selector(".chat-list") is None
        ok_no_topbar = page.query_selector(".guided-topbar") is None

        # 2. 品牌大字
        brand_text = page.inner_text(".hero-brand") if page.query_selector(".hero-brand") else ""
        ok_brand = "爱" in brand_text and "创" in brand_text and "作" in brand_text

        # 3. 输入框
        ok_input = page.query_selector(".hero-input") is not None
        ok_placeholder = page.get_attribute(".hero-input", "placeholder") == "输入主题开始创作…"

        # 4. 发送按钮（应该 disabled）
        ok_send = page.query_selector(".hero-send") is not None
        send_disabled = page.get_attribute(".hero-send", "disabled") is not None

        page.screenshot(path=f"{SHOTS}/guided_hero.png")

        # 5. 输入内容后 hero 切到对话态
        page.fill(".hero-input", "测试主题")
        page.wait_for_timeout(200)
        ok_enabled = page.get_attribute(".hero-send", "disabled") is None

        page.keyboard.press("Enter")
        page.wait_for_timeout(500)

        ok_hero_gone = page.query_selector(".guided-hero") is None
        ok_chat_shown = page.query_selector(".chat-list") is not None
        ok_topbar_shown = page.query_selector(".guided-topbar") is not None
        # 4 步对话：平台问题应出现
        ok_platform_q = page.query_selector("text=准备发哪个平台？") is not None

        page.screenshot(path=f"{SHOTS}/guided_hero_after_submit.png")

        results = [("hero-visible", ok_hero_visible),
                   ("no-chat-list", ok_no_chat),
                   ("no-topbar", ok_no_topbar),
                   ("brand-text", ok_brand),
                   ("hero-input", ok_input),
                   ("placeholder", ok_placeholder),
                   ("send-btn", ok_send),
                   ("send-disabled-empty", send_disabled),
                   ("send-enabled-typed", ok_enabled),
                   ("hero-gone", ok_hero_gone),
                   ("chat-shown", ok_chat_shown),
                   ("topbar-shown", ok_topbar_shown),
                   ("platform-question", ok_platform_q)]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
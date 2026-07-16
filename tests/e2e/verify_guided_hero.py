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
        page.route("**/api/v1/user/topics/random*", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"id": 1, "title": "AI 写作工具横评"},
                {"id": 2, "title": "职场新人生存指南"},
                {"id": 3, "title": "副业赚钱的 5 个思路"},
                {"id": 4, "title": "小红书爆款拆解"},
                {"id": 5, "title": "如何高效学习一门技能"},
                {"id": 6, "title": "独立开发者的一天"}]}))
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

        # 5. 灵感按钮（折叠态）
        ok_inspire_btn = page.query_selector(".hero-inspire-btn") is not None
        inspire_text = page.inner_text(".hero-inspire-btn") if page.query_selector(".hero-inspire-btn") else ""
        ok_inspire_text = "没灵感" in inspire_text

        page.screenshot(path=f"{SHOTS}/guided_hero.png")

        # 6. 点击灵感按钮 → 思考中
        page.click(".hero-inspire-btn")
        page.wait_for_timeout(150)
        ok_thinking = page.query_selector(".hero-inspire-loading") is not None
        ok_thinking_text = "小爱正在帮您思考选题灵感" in (page.inner_text(".hero-inspire-loading") if page.query_selector(".hero-inspire-loading") else "")
        ok_dots = page.query_selector(".hero-dots") is not None

        # 7. 等思考结束（600ms 延迟 + 流式展开）
        page.wait_for_selector(".hero-inspire-status", timeout=5000)
        ok_status = "小爱帮你推荐了" in page.inner_text(".hero-inspire-status") and "请您参考" in page.inner_text(".hero-inspire-status")

        # 8. 6 个标题渐显（等所有 reveal 完成：600ms 思考 + 6*150ms 流式 ≈ 1.5s）
        page.wait_for_selector(".hero-refresh", timeout=5000)
        page.wait_for_timeout(1200)
        topics = page.query_selector_all(".hero-topic")
        visible_topics = [t for t in topics if t.is_visible()]
        ok_six = len(visible_topics) == 6
        ok_refresh = page.query_selector(".hero-refresh") is not None

        page.screenshot(path=f"{SHOTS}/guided_hero_inspired.png")

        # 9. 点一个标题 → 写入输入框
        visible_topics[0].click()
        page.wait_for_timeout(200)
        ok_filled = page.input_value(".hero-input") == "AI 写作工具横评"

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
                   ("inspire-btn", ok_inspire_btn),
                   ("inspire-text", ok_inspire_text),
                   ("thinking", ok_thinking),
                   ("thinking-text", ok_thinking_text),
                   ("thinking-dots", ok_dots),
                   ("status-text", ok_status),
                   ("six-topics", ok_six),
                   ("refresh-btn", ok_refresh),
                   ("filled-on-click", ok_filled),
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
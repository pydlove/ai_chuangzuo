#!/usr/bin/env python3
"""引导模式 4 步流程 + 模板 effect-card + 完整预览入口 + 确认卡 4 行。"""
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
                {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化", "prompt": "x", "scope": "通用"}]}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"templateKey": "xiaohongshu-note", "name": "种草清单", "description": "小红书种草结构",
                 "platform": "xiaohongshu", "bgColor": "#fff0f2", "textColor": "#1a1a1a", "visualStyle": {"bg": "#fff0f2"}},
                {"templateKey": "wechat-article", "name": "公众号深度文", "description": "公众号长文",
                 "platform": "wechat", "bgColor": "#fff", "textColor": "#262626", "visualStyle": {"bg": "#fff"}},
                {"templateKey": "xiaohongshu-food", "name": "深夜食堂", "description": "美食探店",
                 "platform": "xiaohongshu", "bgColor": "#fff8e1", "textColor": "#5d4037", "visualStyle": {"bg": "#fff8e1"}}
            ]}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        # topic
        page.fill(".topic-input", "测试话题")
        page.keyboard.press("Enter")
        page.wait_for_timeout(400)
        # platform
        page.click(".quick-option:has-text('小红书')")
        page.wait_for_timeout(300)
        ok_template_preview_btn_on_platform = page.query_selector(".template-preview-btn") is None
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        # style
        page.click(".quick-option:has-text('轻松口语')")
        page.wait_for_timeout(300)
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # --- 模板步骤 ---
        ok_template_question = page.query_selector("text=想用哪种模板渲染？") is not None
        page.click(".quick-option:has-text('种草清单')")
        page.wait_for_timeout(400)
        ok_effect_card = page.query_selector(".effect-card") is not None
        ok_color_swatch = page.query_selector(".color-swatch") is not None
        preview_btn = page.query_selector(".template-preview-btn")
        ok_preview_btn = preview_btn is not None
        page.screenshot(path=f"{SHOTS}/template_step_preview.png")

        ok_modal = False
        if preview_btn:
            preview_btn.click()
            page.wait_for_timeout(800)
            ok_modal = page.query_selector(".template-modal .ant-modal-content") is not None
            page.keyboard.press("Escape")
            page.wait_for_timeout(400)

        # 确认
        page.click(".quick-confirm")
        page.wait_for_timeout(500)
        confirm = page.query_selector(".confirm-card")
        ok_confirm = confirm is not None
        meta_text = confirm.inner_text() if confirm else ""
        ok_platform_in = "小红书" in meta_text
        ok_style_in = "轻松口语" in meta_text
        ok_template_in = "种草清单" in meta_text
        ok_edit_btns = all(confirm.query_selector(f"button:has-text('{t}')") for t in ["改主题", "改平台", "改风格", "改模板"])
        page.screenshot(path=f"{SHOTS}/template_step_confirm.png")

        results = [("template-question", ok_template_question),
                   ("template-effect-card", ok_effect_card),
                   ("color-swatch", ok_color_swatch),
                   ("preview-btn", ok_preview_btn),
                   ("no-preview-btn-on-platform", ok_template_preview_btn_on_platform),
                   ("full-preview-opens-modal", ok_modal),
                   ("confirm-card", ok_confirm),
                   ("platform-in-meta", ok_platform_in),
                   ("style-in-meta", ok_style_in),
                   ("template-in-meta", ok_template_in),
                   ("edit-buttons", ok_edit_btns)]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

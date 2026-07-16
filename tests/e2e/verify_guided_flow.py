#!/usr/bin/env python3
"""引导模式全流程：四步走完出确认卡片（4 行 + 4 编辑按钮）。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def mock_all(page):
    page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
        "code": 0, "data": {"planKey": "pro", "planName": "专业版", "expiredAt": "",
                            "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
    page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
    page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
    page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化、短句", "prompt": "...", "scope": "通用"},
        {"bizNo": "S2", "name": "专业严谨", "description": "正式专业", "promptSummary": "术语准确", "prompt": "...", "scope": "通用"}]}))
    page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"templateKey": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu",
         "bgColor": "#fff0f2", "textColor": "#1a1a1a"},
        {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat",
         "bgColor": "#fff", "textColor": "#262626"}]}))


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_all(page)
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        page.fill(".hero-input", "35岁被裁后，我靠副业翻身")
        page.keyboard.press("Enter")
        page.wait_for_timeout(500)

        page.click(".quick-option:has-text('小红书')")
        page.wait_for_timeout(400)
        ok_effect = page.query_selector(".quick-preview .effect-card") is not None
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        page.click(".quick-option:has-text('轻松口语')")
        page.wait_for_timeout(400)
        page.click(".quick-confirm")
        page.wait_for_timeout(600)

        # 第 4 步：模板
        ok_template_q = page.query_selector("text=想用哪种模板渲染？") is not None
        page.click(".quick-option:has-text('种草清单')")
        page.wait_for_timeout(400)
        page.click(".quick-confirm")
        page.wait_for_timeout(600)

        ok_confirm = page.query_selector(".confirm-card") is not None
        meta = page.inner_text(".confirm-card") if ok_confirm else ""
        ok_meta = ("小红书" in meta) and ("800" in meta) and ("轻松口语" in meta) and ("种草清单" in meta)
        page.screenshot(path=f"{SHOTS}/guided_confirm.png")

        # 改平台 inline 入口应弹出平台 quick replies
        page.click(".confirm-edit:has-text('改平台')")
        page.wait_for_timeout(600)
        ok_edit_platform_open = page.query_selector("text=准备发哪个平台？") is not None
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)

        for name, ok in [("effect-card", ok_effect), ("template-question", ok_template_q),
                         ("confirm-card", ok_confirm), ("confirm-meta", ok_meta),
                         ("edit-platform-open", ok_edit_platform_open)]:
            print(("PASS " if ok else "FAIL ") + name)
        browser.close()
        if not all([ok_effect, ok_template_q, ok_confirm, ok_meta, ok_edit_platform_open]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

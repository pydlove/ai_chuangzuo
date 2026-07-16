#!/usr/bin/env python3
"""引导模式全流程：四步走完出确认卡片，平台默认配置正确带出。"""
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
        {"templateKey": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu"},
        {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]}))


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_all(page)
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        page.fill(".topic-input", "35岁被裁后，我靠副业翻身")
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

        ok_confirm = page.query_selector(".confirm-card") is not None
        meta = page.inner_text(".confirm-card") if ok_confirm else ""
        ok_meta = ("小红书" in meta) and ("800" in meta) and ("轻松口语" in meta)
        page.screenshot(path=f"{SHOTS}/guided_confirm.png")

        page.click("text=改配置")
        page.wait_for_timeout(600)
        ok_modal = page.query_selector(".platform-modal .ant-modal-content") is not None
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)
        ok_back = page.query_selector(".confirm-card") is not None

        for name, ok in [("effect-card", ok_effect), ("confirm-card", ok_confirm),
                         ("confirm-meta", ok_meta), ("edit-config-modal", ok_modal), ("back-to-confirm", ok_back)]:
            print(("PASS " if ok else "FAIL ") + name)
        browser.close()
        if not all([ok_effect, ok_confirm, ok_meta, ok_modal, ok_back]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

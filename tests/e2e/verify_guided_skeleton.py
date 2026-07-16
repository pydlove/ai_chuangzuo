#!/usr/bin/env python3
"""引导模式骨架：默认 guided、主题步骤、平台胶囊两段式、模式切换记忆。"""
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
        page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        ok_default = page.query_selector(".guided-hero") is not None
        page.fill(".hero-input", "测试主题：AI 写作工具横评")
        page.keyboard.press("Enter")
        page.wait_for_timeout(600)
        ok_user_bubble = page.query_selector(".chat-msg.user") is not None
        ok_options = len(page.query_selector_all(".quick-option")) >= 5

        page.click("text=小红书", timeout=3000)
        page.wait_for_timeout(400)
        ok_preview = page.query_selector(".quick-preview") is not None
        page.screenshot(path=f"{SHOTS}/guided_skeleton.png")

        page.click("text=熟手模式", timeout=3000)
        page.wait_for_timeout(600)
        ok_minimal = page.query_selector(".minimal-panel") is not None
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(1000)
        ok_remember = page.query_selector(".minimal-panel") is not None

        for name, ok in [("default-guided", ok_default), ("user-bubble", ok_user_bubble),
                         ("platform-options", ok_options), ("preview-card", ok_preview),
                         ("switch-minimal", ok_minimal), ("remember", ok_remember)]:
            print(("PASS " if ok else "FAIL ") + name)
        browser.close()
        if not all([ok_default, ok_user_bubble, ok_options, ok_preview, ok_minimal, ok_remember]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

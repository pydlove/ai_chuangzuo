#!/usr/bin/env python3
"""Verify Create style editor header text is visible in dark theme on mobile."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(600)

        # Open style library modal
        page.locator('.settings-chip').nth(2).click()
        page.wait_for_timeout(600)

        # Ensure on 我的风格 tab
        page.locator(".style-modal .style-tab").nth(0).click()
        page.wait_for_timeout(300)

        # Click 新建我的风格 card
        page.click("text=新建我的风格", timeout=2000)
        page.wait_for_timeout(600)

        page.screenshot(path=f"{SCREENSHOT_DIR}/create_style_editor_dark.png")

        back = page.query_selector(".style-editor-back")
        title = page.query_selector(".style-editor-title")
        if back:
            print(f"back color={back.evaluate('e => getComputedStyle(e).color')}")
        if title:
            print(f"title color={title.evaluate('e => getComputedStyle(e).color')}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

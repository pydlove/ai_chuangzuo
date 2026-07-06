#!/usr/bin/env python3
"""Verify Create style modal learned tab empty state in dark theme on mobile."""
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
        page.evaluate("localStorage.removeItem('aichuangzuo_learned_styles')")
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(600)

        # Open style library modal (third settings chip)
        page.locator('.settings-chip').nth(2).click()
        page.wait_for_timeout(600)

        # Switch to learned tab
        page.click("text=学习的风格", timeout=2000)
        page.wait_for_timeout(600)

        page.screenshot(path=f"{SCREENSHOT_DIR}/create_learned_empty_dark.png")

        empty_el = page.query_selector(".style-empty")
        if empty_el:
            print(f"empty text={empty_el.inner_text().strip()}")
            print(f"color={empty_el.evaluate('e => getComputedStyle(e).color')}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

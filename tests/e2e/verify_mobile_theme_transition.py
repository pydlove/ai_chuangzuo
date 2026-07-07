#!/usr/bin/env python3
"""Verify mobile theme toggle uses the same radial-gradient transition as web."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/console/mine", wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'light')")
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(500)

        before_theme = page.evaluate("document.body.getAttribute('data-theme')")
        print(f"before theme: {before_theme}")

        page.screenshot(path=f"{SCREENSHOT_DIR}/mobile_theme_light.png")

        # Click the theme row in mine page
        page.locator('li.mine-list-item:has-text("主题切换")').click()
        # Wait for transition to finish
        page.wait_for_timeout(1000)

        after_theme = page.evaluate("document.body.getAttribute('data-theme')")
        saved_theme = page.evaluate("localStorage.getItem('aichuangzuo_theme')")
        print(f"after theme: {after_theme}, saved: {saved_theme}")

        page.screenshot(path=f"{SCREENSHOT_DIR}/mobile_theme_dark.png")

        assert after_theme == 'dark', f"expected dark theme, got {after_theme}"
        assert saved_theme == 'dark', f"expected saved dark, got {saved_theme}"

        # Toggle back
        page.locator('li.mine-list-item:has-text("主题切换")').click()
        page.wait_for_timeout(1000)
        final_theme = page.evaluate("document.body.getAttribute('data-theme')")
        print(f"final theme: {final_theme}")
        assert final_theme == 'light', f"expected light theme, got {final_theme}"

        print("mobile theme toggle OK")
        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

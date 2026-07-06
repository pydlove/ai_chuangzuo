#!/usr/bin/env python3
"""Inspect learned styles empty state in dark theme on mobile."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/console/styles", wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.evaluate("localStorage.removeItem('aichuangzuo_learned_styles')")
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(500)

        # Click learned tab
        page.click("text=学习的风格", timeout=2000)
        page.wait_for_timeout(600)

        page.screenshot(path=f"{SCREENSHOT_DIR}/learned_empty_dark.png")

        text_el = page.query_selector(".styles-empty .style-add-text")
        card_el = page.query_selector(".styles-empty .style-add-card")
        banner_el = page.query_selector(".learned-banner")
        if text_el:
            color = text_el.evaluate("e => getComputedStyle(e).color")
            bg = card_el.evaluate("e => getComputedStyle(e).backgroundColor") if card_el else None
            print(f"text color={color}, card bg={bg}")
        if banner_el:
            print(f"banner color={banner_el.evaluate('e => getComputedStyle(e).color')}, bg={banner_el.evaluate('e => getComputedStyle(e).backgroundColor')}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

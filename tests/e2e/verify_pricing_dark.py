#!/usr/bin/env python3
"""Capture Pricing page in dark theme at port 22345."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/login")
        page.evaluate("""() => {
          localStorage.setItem('aichuangzuo_theme', 'dark')
        }""")
        page.goto(f"{BASE}/pricing", wait_until="networkidle")
        page.wait_for_timeout(1500)

        page.screenshot(path=f"{SCREENSHOT_DIR}/pricing_dark_mobile.png", full_page=True)

        primary_btn = page.query_selector(".plan-btn.primary")
        if primary_btn:
            bg = primary_btn.evaluate("e => getComputedStyle(e).backgroundImage")
            color = primary_btn.evaluate("e => getComputedStyle(e).color")
            print(f"primary btn bg={bg} color={color}")

        body_theme = page.evaluate("() => document.body.getAttribute('data-theme')")
        print(f"body data-theme: {body_theme}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

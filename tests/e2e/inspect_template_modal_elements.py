#!/usr/bin/env python3
"""Inspect all template-modal elements."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.locator('.settings-chip').nth(3).click()
        page.wait_for_timeout(800)

        els = page.query_selector_all('.template-modal')
        print(f"count: {len(els)}")
        for i, el in enumerate(els):
            print(i, el.bounding_box(), el.evaluate("e => getComputedStyle(e).position"))

        # screenshot full page
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/template_modal_full.png", full_page=True)

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

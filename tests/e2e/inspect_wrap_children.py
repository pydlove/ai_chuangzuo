#!/usr/bin/env python3
"""List children of modal wrap."""
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

        wrap = page.query_selector(".template-modal-wrap")
        children = wrap.query_selector_all(":scope > *")
        for i, c in enumerate(children):
            print(i, c.evaluate("e => e.className"), c.bounding_box())

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

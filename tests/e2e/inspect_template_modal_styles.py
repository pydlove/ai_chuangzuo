#!/usr/bin/env python3
"""Inspect template modal computed styles."""
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

        sel = '.template-modal'
        el = page.query_selector(sel)
        styles = el.evaluate("""e => ({
            position: getComputedStyle(e).position,
            top: getComputedStyle(e).top,
            marginTop: getComputedStyle(e).marginTop,
            transform: getComputedStyle(e).transform,
            height: getComputedStyle(e).height,
            maxHeight: getComputedStyle(e).maxHeight,
            boxSizing: getComputedStyle(e).boxSizing,
            paddingTop: getComputedStyle(e).paddingTop,
            paddingBottom: getComputedStyle(e).paddingBottom
        })""")
        print(styles)

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

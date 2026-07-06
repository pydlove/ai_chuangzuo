#!/usr/bin/env python3
"""Inspect wrap computed styles."""
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
        modal = page.query_selector(".template-modal")
        print("wrap styles:", wrap.evaluate("""e => ({
            position: getComputedStyle(e).position,
            display: getComputedStyle(e).display,
            alignItems: getComputedStyle(e).alignItems,
            justifyContent: getComputedStyle(e).justifyContent,
            flexDirection: getComputedStyle(e).flexDirection,
            height: getComputedStyle(e).height,
            overflow: getComputedStyle(e).overflow,
            top: getComputedStyle(e).top,
            left: getComputedStyle(e).left
        })"""))
        print("modal styles:", modal.evaluate("""e => ({
            alignSelf: getComputedStyle(e).alignSelf,
            marginTop: getComputedStyle(e).marginTop,
            top: getComputedStyle(e).top,
            transform: getComputedStyle(e).transform,
            height: getComputedStyle(e).height,
            maxHeight: getComputedStyle(e).maxHeight
        })"""))

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

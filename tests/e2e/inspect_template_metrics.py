#!/usr/bin/env python3
"""Inspect template modal layout metrics on mobile."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(500)

        page.locator('.settings-chip').nth(3).click()
        page.wait_for_timeout(800)

        for sel in ['.template-modal', '.template-modal .ant-modal-content', '.template-modal .ant-modal-body', '.template-modal .template-footer']:
            el = page.query_selector(sel)
            if el:
                box = el.bounding_box()
                styles = el.evaluate("""e => ({
                    height: getComputedStyle(e).height,
                    maxHeight: getComputedStyle(e).maxHeight,
                    top: getComputedStyle(e).top,
                    marginTop: getComputedStyle(e).marginTop,
                    paddingTop: getComputedStyle(e).paddingTop
                })""")
                print(f"{sel}: box={box}, styles={styles}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

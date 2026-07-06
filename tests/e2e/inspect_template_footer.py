#!/usr/bin/env python3
"""Inspect template modal footer visibility."""
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

        for sel in ['.template-modal-wrap', '.template-modal', '.template-modal .ant-modal-body', '.template-modal .template-footer']:
            el = page.query_selector(sel)
            if el:
                box = el.bounding_box()
                print(f"{sel}: box={box}")

        # Scroll footer into view and screenshot
        footer = page.query_selector(".template-modal .template-footer")
        if footer:
            footer.scroll_into_view_if_needed()
            page.wait_for_timeout(300)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/template_modal_footer.png")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

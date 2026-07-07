#!/usr/bin/env python3
"""Verify improved template modal mobile layout."""
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
        page.reload(wait_until="networkidle")
        page.wait_for_timeout(500)

        # Open template modal
        page.locator('.settings-chip').nth(3).click()
        page.wait_for_timeout(1200)

        page.screenshot(path=f"{SCREENSHOT_DIR}/template_modal_mobile_improved.png")

        modal = page.query_selector(".template-modal")
        body = page.query_selector(".template-body")
        preview = page.query_selector(".template-preview-pane")
        list_pane = page.query_selector(".template-list-pane")
        if modal:
            box = modal.bounding_box()
            print(f"modal size: {box['width']}x{box['height']}")
        if preview:
            box = preview.bounding_box()
            print(f"preview size: {box['width']}x{box['height']}")
        if list_pane:
            box = list_pane.bounding_box()
            print(f"list size: {box['width']}x{box['height']}")

        # Click second template card
        rows = page.query_selector_all(".template-row")
        if len(rows) >= 2:
            rows[1].click()
            page.wait_for_timeout(400)
            page.screenshot(path=f"{SCREENSHOT_DIR}/template_modal_mobile_selected.png")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

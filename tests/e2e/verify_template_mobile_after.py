#!/usr/bin/env python3
"""Verify template modal is adapted to mobile."""
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

        # Open template modal (fourth settings chip)
        page.locator('.settings-chip').nth(3).click()
        page.wait_for_timeout(800)

        page.screenshot(path=f"{SCREENSHOT_DIR}/template_modal_mobile_after.png")

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

        # Check preview is not overflowing viewport horizontally
        assert preview is not None
        pbox = preview.bounding_box()
        assert pbox['width'] <= 375, f"preview too wide: {pbox['width']}"

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

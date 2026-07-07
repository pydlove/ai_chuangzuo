#!/usr/bin/env python3
"""Inspect current template modal on mobile."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(500)

        # Open template modal (fourth settings chip)
        page.locator('.settings-chip').nth(3).click()
        page.wait_for_timeout(800)

        page.screenshot(path=f"{SCREENSHOT_DIR}/template_modal_mobile_before.png")

        # Print sizes
        modal = page.query_selector(".template-modal")
        body = page.query_selector(".template-body")
        preview = page.query_selector(".template-preview-pane")
        if modal:
            box = modal.bounding_box()
            print(f"modal size: {box['width']}x{box['height']}")
        if preview:
            box = preview.bounding_box()
            print(f"preview size: {box['width']}x{box['height']}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

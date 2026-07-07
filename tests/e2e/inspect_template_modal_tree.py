#!/usr/bin/env python3
"""Find actual template modal root."""
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

        title = page.query_selector("text=导出模板库")
        # Walk up parents
        el = title
        for i in range(8):
            if not el:
                break
            box = el.bounding_box()
            tag = el.evaluate("e => e.tagName")
            cls = el.evaluate("e => e.className")
            print(f"{i}: {tag} classes={cls} box={box}")
            el = el.evaluate_handle("e => e.parentElement")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

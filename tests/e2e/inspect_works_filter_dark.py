#!/usr/bin/env python3
"""Inspect works filter time range tab separator colors in dark theme."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/login")
        page.evaluate("""() => {
          localStorage.setItem('aichuangzuo_access_token', 'dummy')
          localStorage.setItem('aichuangzuo_theme', 'dark')
        }""")
        page.goto(f"{BASE}/console/works")
        page.wait_for_selector(".works-filter-time", timeout=10000)
        page.wait_for_timeout(1500)

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/works_filter_dark.png", full_page=True)

        wrappers = page.query_selector_all(".works-filter-time .ant-radio-button-wrapper")
        for i, w in enumerate(wrappers):
            cls = w.evaluate("e => e.className")
            bg = w.evaluate("e => getComputedStyle(e, '::before').backgroundColor")
            border = w.evaluate("e => getComputedStyle(e).borderLeftColor")
            print(f"{i}: {cls} ::before bg={bg} border-left={border}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

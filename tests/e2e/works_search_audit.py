#!/usr/bin/env python3
"""Audit 我的作品 search box in dark mode."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/works", wait_until="networkidle")
        page.wait_for_timeout(800)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_search.png", full_page=False)

        sels = [
            ".works-filter-bar",
            ".works-search",
            ".works-search input",
            ".works-search .ant-input-affix-wrapper",
            ".works-search .ant-input-prefix",
            ".works-search .ant-input-suffix",
            ".works-search .ant-input-suffix .ant-input-search-icon",
            ".works-search .ant-input-suffix .ant-input-search-button",
            ".works-filter-select",
            ".works-filter-select .ant-select-selector",
            ".works-filter-time",
            ".works-filter-time .ant-radio-button-wrapper",
        ]
        bad_colors = {"rgb(255, 255, 255)", "rgb(245, 245, 245)", "rgb(250, 250, 250)", "rgb(248, 248, 248)", "rgb(252, 252, 252)"}
        found = 0
        bad = 0
        for sel in sels:
            for i, el in enumerate(page.query_selector_all(sel)):
                found += 1
                bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
                color = el.evaluate("e => getComputedStyle(e).color")
                is_bad = bg in bad_colors
                marker = "⚠" if is_bad else "✓"
                if is_bad:
                    bad += 1
                print(f"  {marker} {sel}[{i}] bg={bg} color={color}")
        print(f">>> found={found}, bad={bad}")

        # Switch to drafts tab
        try:
            page.click("text=草稿箱", timeout=2000)
            page.wait_for_timeout(500)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_search_drafts.png", full_page=False)
        except Exception as e:
            print(f"drafts click ERR {e}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
#!/usr/bin/env python3
"""Check the search button's actual computed background."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/works?bust=99", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Inspect the search button area
        info = page.evaluate("""() => {
          const addon = document.querySelector('.works-search .ant-input-group-addon');
          const btn = document.querySelector('.works-search .ant-input-search-button');
          const icon = document.querySelector('.works-search .ant-input-search-icon');
          return {
            addon: addon ? {
              cls: addon.className,
              bg: getComputedStyle(addon).backgroundColor,
              color: getComputedStyle(addon).color,
              border: getComputedStyle(addon).borderColor,
            } : null,
            btn: btn ? {
              cls: btn.className,
              bg: getComputedStyle(btn).backgroundColor,
              color: getComputedStyle(btn).color,
              border: getComputedStyle(btn).borderColor,
            } : null,
            icon: icon ? {
              cls: icon.className,
              bg: getComputedStyle(icon).backgroundColor,
              color: getComputedStyle(icon).color,
            } : null,
          };
        }""")
        print("Search button state:")
        for k, v in info.items():
            print(f"  {k}: {v}")

        # Focus and screenshot
        page.click(".works-search input")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_search_button_v3.png", clip={"x": 200, "y": 80, "width": 800, "height": 80})
        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
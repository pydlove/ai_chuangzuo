#!/usr/bin/env python3
"""Inspect DOM structure of search box."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/works?cache=bust1", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        html = page.evaluate("""() => {
          const s = document.querySelector('.works-search');
          return s ? s.outerHTML : 'none';
        }""")
        print(html[:3000])

        # List all selectors that match within .works-search
        sel_list = page.evaluate("""() => {
          const s = document.querySelector('.works-search');
          if (!s) return [];
          const seen = new Set();
          s.querySelectorAll('*').forEach(el => {
            if (el.className && typeof el.className === 'string') {
              el.className.split(' ').forEach(c => {
                if (c.startsWith('ant-')) seen.add(c);
              });
            }
          });
          return [...seen];
        }""")
        print("\nAnt classes in .works-search:")
        for c in sel_list:
            print(f"  {c}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
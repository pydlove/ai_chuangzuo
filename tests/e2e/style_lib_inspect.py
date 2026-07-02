#!/usr/bin/env python3
"""Inspect style library modal DOM."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/create?bust=62", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        page.click(".smart-defaults .settings-chip:nth-child(3)", timeout=2000)
        page.wait_for_timeout(800)

        # List all ant classes and custom classes inside style-modal
        info = page.evaluate("""() => {
          const modal = document.querySelector('.style-modal');
          if (!modal) return { error: 'modal not found' };
          const seen = new Set();
          modal.querySelectorAll('*').forEach(el => {
            if (el.className && typeof el.className === 'string') {
              el.className.split(' ').forEach(c => {
                if (c && !c.startsWith('css-dev-only')) seen.add(c);
              });
            }
          });
          return [...seen];
        }""")
        print("Classes in style-modal:")
        for c in sorted(info):
            print(f"  {c}")

        # Get outerHTML of first card
        html = page.evaluate("""() => {
          const modal = document.querySelector('.style-modal');
          const card = modal.querySelector('[class*="item"]') || modal.querySelector('[class*="card"]') || modal.querySelector('.style-list > *');
          return card ? card.outerHTML : 'none';
        }""")
        print("\nFirst card HTML:")
        print(html[:1500])

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
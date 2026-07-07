#!/usr/bin/env python3
"""Debug focus state — verify what background the search has when focused."""
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

        # Click and focus
        page.click(".works-search input")
        page.wait_for_timeout(500)

        # Get all classes on the input wrapper
        info = page.evaluate("""() => {
          const w = document.querySelector('.works-search .ant-input-affix-wrapper');
          const wFocused = document.querySelector('.works-search .ant-input-affix-wrapper-focused');
          const i = document.querySelector('.works-search input');
          const ret = {
            wrapperClass: w ? w.className : 'none',
            wrapperBg: w ? getComputedStyle(w).backgroundColor : 'none',
            wrapperColor: w ? getComputedStyle(w).color : 'none',
            focusedExists: !!wFocused,
            inputClass: i ? i.className : 'none',
            inputBg: i ? getComputedStyle(i).backgroundColor : 'none',
            bodyTheme: document.body.getAttribute('data-theme'),
          };
          // walk up parents and dump bg
          let parent = w;
          const chain = [];
          while (parent && chain.length < 6) {
            chain.push({
              tag: parent.tagName,
              cls: parent.className,
              bg: getComputedStyle(parent).backgroundColor,
              color: getComputedStyle(parent).color,
            });
            parent = parent.parentElement;
          }
          ret.chain = chain;
          return ret;
        }""")
        print("FOCUS DEBUG:")
        for k, v in info.items():
            if k == "chain":
                print("  chain:")
                for c in v:
                    print(f"    {c}")
            else:
                print(f"  {k}: {v}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_search_focused_v2.png", full_page=False, clip={"x": 200, "y": 80, "width": 600, "height": 80})

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
#!/usr/bin/env python3
"""Inject a style-card-remove button to verify dark mode styling."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/styles?bust=41", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Inject a card with 删除 button — append to body so it stays visible
        page.evaluate("""() => {
          const wrapper = document.createElement('div');
          wrapper.style.cssText = 'position:fixed;top:160px;left:300px;z-index:9999;display:flex;gap:24px;background:#1f1f1f;padding:24px;border-radius:12px;border:1px solid #303030;';
          wrapper.innerHTML = `
            <div class="style-card" data-v-139a0d15 style="background:#1f1f1f;border:1px solid #303030;border-radius:10px;padding:16px;min-width:280px;">
              <div class="style-card-head" style="display:flex;align-items:center;gap:12px;">
                <div class="style-card-avatar" style="width:36px;height:36px;border-radius:50%;background:#2a2a2a;display:flex;align-items:center;justify-content:center;color:#f0f0f0;">A</div>
                <div class="style-card-title-wrap" style="flex:1;"><div class="style-card-title" style="color:#f0f0f0;">测试风格</div></div>
                <button class="style-card-remove" data-v-139a0d15>删除</button>
              </div>
            </div>
          `;
          document.body.appendChild(wrapper);
        }""")
        page.wait_for_timeout(300)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_remove_inject.png", full_page=False)

        sels = [".style-card-remove"]
        for sel in sels:
            for i, el in enumerate(page.query_selector_all(sel)):
                bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
                color = el.evaluate("e => getComputedStyle(e).color")
                print(f"  {sel}[{i}] bg={bg} color={color}")

        # Hover state
        first = page.query_selector(".style-card-remove")
        if first:
            first.hover()
            page.wait_for_timeout(400)
            bg = first.evaluate("e => getComputedStyle(e).backgroundColor")
            color = first.evaluate("e => getComputedStyle(e).color")
            print(f"  HOVER bg={bg} color={color}")
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_remove_hover.png", full_page=False)

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
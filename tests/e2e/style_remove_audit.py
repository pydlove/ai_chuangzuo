#!/usr/bin/env python3
"""Audit style card 删除 button in dark mode."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"

BAD = {"rgb(255, 255, 255)", "rgb(245, 245, 245)", "rgb(250, 250, 250)", "rgb(248, 248, 248)", "rgb(252, 252, 252)", "rgb(240, 240, 240)"}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        # Seed favorites so 收藏 tab has data with 删除 button
        page.evaluate("""
          localStorage.setItem('aichuangzuo_theme', 'dark');
          const favs = [
            { id: 'f1', name: '种草达人', prompt: '你是一位小红书种草达人...', creatorName: 'AI创作官方', scope: '美妆,穿搭', desc: '小红书爆款公式' },
            { id: 'f2', name: '职场干货', prompt: '你是一位职场作者...', creatorName: '老司机', scope: '职场,成长', desc: '职场高效指南' }
          ];
          localStorage.setItem('aichuangzuo_favorite_styles', JSON.stringify(favs));
        """)
        page.goto(f"{BASE}/console/styles?bust=31", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Audit each tab — collect 删除 buttons + their parents
        for tab in ["收藏", "学习"]:
            for t in page.query_selector_all(".styles-tab"):
                if tab in t.inner_text():
                    t.click()
                    page.wait_for_timeout(500)
                    break

            sels = [".style-card-remove"]
            print(f"\n--- {tab} tab 删除 ---")
            for sel in sels:
                for i, el in enumerate(page.query_selector_all(sel)):
                    bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
                    color = el.evaluate("e => getComputedStyle(e).color")
                    txt = el.inner_text()
                    is_bad = bg in BAD
                    marker = "BAD" if is_bad else "ok"
                    print(f"  {marker} {sel}[{i}] '{txt}' bg={bg} color={color}")
            page.screenshot(path=f"/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_remove_{tab}.png", full_page=False)

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
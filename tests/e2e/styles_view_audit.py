#!/usr/bin/env python3
"""Audit style-card 查看 expanded state in dark mode."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"

BAD = {"rgb(255, 255, 255)", "rgb(245, 245, 245)", "rgb(250, 250, 250)", "rgb(248, 248, 248)", "rgb(252, 252, 252)", "rgb(240, 240, 240)"}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/styles?bust=11", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_styles_my_v2.png", full_page=True)

        # Click first 查看 button (try system tab which always has data)
        for tab_text in ["系统", "我的"]:
            for t in page.query_selector_all(".styles-tab"):
                if tab_text in t.inner_text():
                    t.click()
                    page.wait_for_timeout(500)
                    break

            view_btn = None
            for btn in page.query_selector_all(".style-action-btn"):
                txt = btn.inner_text()
                if "查看" in txt:
                    view_btn = btn
                    break
            if view_btn:
                print(f"clicking 查看 on tab: {tab_text}")
                view_btn.click()
                break
        if not view_btn:
            print("no 查看 button found")
            return
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_styles_view_expanded.png", full_page=True)

        sels = [
            ".style-prompt-full",
            ".style-card-prompt",
            ".style-card-scope",
            ".style-card-scope-list",
            ".style-card",
            ".style-card-desc",
            ".style-card-meta",
            ".style-card-avatar",
            ".style-action-btn",
            ".style-action-btn.primary",
        ]
        print("\n--- 查看 expanded state ---")
        found = 0
        bad = 0
        for sel in sels:
            for i, el in enumerate(page.query_selector_all(sel)):
                found += 1
                bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
                color = el.evaluate("e => getComputedStyle(e).color")
                is_bad = bg in BAD
                marker = "BAD" if is_bad else "ok"
                if is_bad:
                    bad += 1
                print(f"  {marker} {sel}[{i}] bg={bg} color={color}")
        print(f">>> found={found}, bad={bad}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
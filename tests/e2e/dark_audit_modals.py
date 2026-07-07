#!/usr/bin/env python3
"""Verify platform / template / draft-box modals are dark."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Platform modal
        try:
            page.click("text=公众号", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_platform.png")
            print("--- platform modal ---")
            for sel in [".platform-modal .ant-modal-content", ".platform-item", ".platform-name", ".platform-desc"]:
                els = page.query_selector_all(sel)
                for i, el in enumerate(els):
                    if not el.is_visible(): continue
                    print(f"  {sel}[{i}] bg={el.evaluate('e => getComputedStyle(e).backgroundColor')} color={el.evaluate('e => getComputedStyle(e).color')}")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"platform ERR {e}")

        # Template modal
        try:
            page.click("text=公众标题模板", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_template.png")
            print("--- template modal ---")
            for sel in [".template-modal .ant-modal-content", ".template-row", ".template-row-name", ".template-group-title"]:
                els = page.query_selector_all(sel)
                for i, el in enumerate(els):
                    if not el.is_visible(): continue
                    print(f"  {sel}[{i}] bg={el.evaluate('e => getComputedStyle(e).backgroundColor')} color={el.evaluate('e => getComputedStyle(e).color')}")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"template ERR {e}")

        # Draft box
        try:
            page.click("text=草稿箱", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_draftbox.png")
            print("--- draft box modal ---")
            for sel in [".draft-box-modal .ant-modal-content", ".draft-empty-text", ".draft-item"]:
                els = page.query_selector_all(sel)
                for i, el in enumerate(els):
                    if not el.is_visible(): continue
                    print(f"  {sel}[{i}] bg={el.evaluate('e => getComputedStyle(e).backgroundColor')} color={el.evaluate('e => getComputedStyle(e).color')}")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"draft ERR {e}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

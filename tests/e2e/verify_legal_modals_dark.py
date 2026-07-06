#!/usr/bin/env python3
"""Verify terms / privacy modals render correctly in dark theme."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def inspect_modal(page, name):
    print(f"--- {name} modal ---")
    content = page.query_selector(".legal-modal .ant-modal-content")
    header = page.query_selector(".legal-modal .ant-modal-header")
    title = page.query_selector(".legal-modal .ant-modal-title")
    body = page.query_selector(".terms-content")
    if content:
        print(f"  content bg={content.evaluate('e => getComputedStyle(e).backgroundColor')}")
    if header:
        print(f"  header bg={header.evaluate('e => getComputedStyle(e).backgroundColor')} border={header.evaluate('e => getComputedStyle(e).borderBottomColor')}")
    if title:
        print(f"  title color={title.evaluate('e => getComputedStyle(e).color')}")
    if body:
        print(f"  body color={body.evaluate('e => getComputedStyle(e).color')}")
        h4 = body.query_selector("h4")
        if h4:
            print(f"  h4 color={h4.evaluate('e => getComputedStyle(e).color')}")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        page.goto(f"{BASE}/console/mine", wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Open about modal via the "about us" icon (circle with info path)
        page.evaluate("""() => {
            const btn = document.querySelector('button:has(svg path[d="M12 16v-4"])');
            if (btn) btn.click();
        }""")
        page.wait_for_timeout(600)

        # User agreement
        page.click("button.about-link-btn:has-text('用户协议')", timeout=2000)
        page.wait_for_timeout(800)
        inspect_modal(page, "terms")
        page.screenshot(path=f"{SCREENSHOT_DIR}/legal_terms_dark.png")
        page.keyboard.press("Escape")
        page.wait_for_timeout(300)

        # Re-open about modal
        page.evaluate("""() => {
            const btn = document.querySelector('button:has(svg path[d="M12 16v-4"])');
            if (btn) btn.click();
        }""")
        page.wait_for_timeout(600)

        # Privacy policy
        page.click("button.about-link-btn:has-text('隐私政策')", timeout=2000)
        page.wait_for_timeout(800)
        inspect_modal(page, "privacy")
        page.screenshot(path=f"{SCREENSHOT_DIR}/legal_privacy_dark.png")
        page.keyboard.press("Escape")
        page.wait_for_timeout(300)

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

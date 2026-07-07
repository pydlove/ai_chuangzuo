#!/usr/bin/env python3
"""Verify dark theme for invite binding modal."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/login")
        page.evaluate("""() => {
          localStorage.setItem('aichuangzuo_access_token', 'dummy')
          localStorage.setItem('aichuangzuo_theme', 'dark')
        }""")
        page.goto(f"{BASE}/console/mine", wait_until="networkidle")
        page.wait_for_timeout(1200)

        # Click 绑定邀请人 list item
        item = page.locator('.mine-list-item', has_text='绑定邀请人')
        if item.count():
            item.click()
            page.wait_for_timeout(800)

        page.screenshot(path=f"{SCREENSHOT_DIR}/invite_binding_dark.png")

        modal = page.query_selector(".invite-binding-modal")
        print(f"modal visible: {modal.is_visible() if modal else False}")

        input_el = page.query_selector(".invite-binding-input")
        if input_el:
            bg = input_el.evaluate("e => getComputedStyle(e).backgroundColor")
            color = input_el.evaluate("e => getComputedStyle(e).color")
            print(f"input bg={bg} color={color}")

        hint = page.query_selector(".invite-binding-hint")
        if hint:
            color = hint.evaluate("e => getComputedStyle(e).color")
            print(f"hint color={color}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

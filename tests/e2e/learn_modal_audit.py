#!/usr/bin/env python3
"""Audit 学习写作风格 modal in dark mode — all 3 states."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"

BAD = {"rgb(255, 255, 255)", "rgb(245, 245, 245)", "rgb(250, 250, 250)", "rgb(248, 248, 248)", "rgb(252, 252, 252)", "rgb(240, 240, 240)"}


def check(page, sels, label):
    print(f"\n--- {label} ---")
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
    print(f">>> {label}: found={found}, bad={bad}")
    return bad


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/styles?bust=21", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Switch to 学习的风格 tab and click 学习新风格 add card
        for t in page.query_selector_all(".styles-tab"):
            if "学习" in t.inner_text():
                t.click()
                page.wait_for_timeout(500)
                break

        # Click the add card "学习新风格"
        for c in page.query_selector_all(".style-add-card"):
            txt = c.inner_text()
            if "学习" in txt:
                c.click()
                page.wait_for_timeout(800)
                break

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_learn_modal_paste.png", full_page=False)
        sels_paste = [
            ".learned-import-modal .ant-modal-content",
            ".learned-import-modal .ant-modal-header",
            ".learned-import-modal .ant-modal-close-x",
            ".learned-subtabs",
            ".learned-subtab",
            ".learned-subtab.active",
            ".learned-pane",
            ".learned-textarea",
            ".learned-counter",
            ".learned-submit-btn",
        ]
        check(page, sels_paste, "paste tab")

        # Switch to upload tab
        for t in page.query_selector_all(".learned-subtab"):
            if "上传" in t.inner_text():
                t.click()
                page.wait_for_timeout(400)
                break
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_learn_modal_upload.png", full_page=False)
        sels_upload = [
            ".learned-upload-zone",
            ".learned-upload-hint",
            ".learned-upload-types",
        ]
        check(page, sels_upload, "upload tab")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
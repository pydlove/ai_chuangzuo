#!/usr/bin/env python3
"""Audit 风格库弹框 list view in dark mode."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"

BAD = {"rgb(255, 255, 255)", "rgb(245, 245, 245)", "rgb(250, 250, 250)", "rgb(248, 248, 248)", "rgb(252, 252, 252)", "rgb(240, 240, 240)"}


def check(page, sels, label):
    print(f"\n--- {label} ---")
    found = 0
    bad = 0
    for sel in sels:
        for i, el in enumerate(page.query_selector_all(sel)):
            if not el.is_visible():
                continue
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
        page.goto(f"{BASE}/console/create?bust=61", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Open style modal
        page.click(".smart-defaults .settings-chip:nth-child(3)", timeout=2000)
        page.wait_for_timeout(800)

        # Click 系统预设风格 tab to ensure cards
        for t in page.query_selector_all(".style-tab"):
            if "系统" in t.inner_text():
                t.click()
                page.wait_for_timeout(500)
                break

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_lib_system.png", full_page=False)

        sels = [
            ".style-modal .ant-modal-content",
            ".style-modal .ant-modal-header",
            ".style-tab",
            ".style-tab.active",
            ".style-grid",
            ".style-card",
            ".style-card-title",
            ".style-card-desc",
            ".style-card-prompt",
            ".style-card-scope",
            ".style-add-card",
            ".style-add-icon",
            ".style-add-text",
            ".style-footer",
            ".style-apply-btn",
        ]
        check(page, sels, "风格库弹框 - system tab")

        # Click 我的风格 tab
        for t in page.query_selector_all(".style-tab"):
            if "我的" in t.inner_text():
                t.click()
                page.wait_for_timeout(500)
                break
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_lib_my.png", full_page=False)
        check(page, sels, "风格库弹框 - my tab")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
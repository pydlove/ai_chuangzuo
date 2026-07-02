#!/usr/bin/env python3
"""Audit styles page - all 4 tabs, no reload."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def audit(page, label):
    print(f"\n--- {label} ---")
    sels = [
        ".styles-tabs", ".styles-tab", ".styles-tab.active",
        ".styles-search-input",
        ".style-add-card", ".style-card", ".style-card-title",
        ".style-card-scope", ".style-card-prompt",
        ".style-action-btn", ".style-action-btn.primary",
        ".learned-banner", ".learned-subtab",
        ".learned-textarea", ".learned-input",
        ".learned-upload-zone", ".learned-upload-hint",
        ".learned-progress", ".learned-error",
        ".learned-submit-btn", ".learned-result",
        ".style-editor-input", ".style-editor-textarea",
        ".style-editor-label", ".style-editor-hint",
        ".style-editor-preset", ".save-style-btn",
        ".style-scope-tags", ".style-scope-tag",
        ".style-scope-tag-remove", ".style-scope-tag-input",
        ".style-scope-hint",
        ".style-preset-card", ".style-preset-title",
        ".style-preset-desc", ".style-editor-error",
        ".learned-excerpt", ".learned-result-field",
        ".learned-result-label", ".learned-result-name",
        ".modal-title",
    ]
    bad = 0
    found = 0
    for sel in sels:
        for i, el in enumerate(page.query_selector_all(sel)):
            found += 1
            bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
            color = el.evaluate("e => getComputedStyle(e).color")
            is_bad = bg in ("rgb(255, 255, 255)", "rgb(245, 245, 245)", "rgb(250, 250, 250)", "rgb(248, 248, 248)", "rgb(252, 252, 252)")
            marker = "⚠" if is_bad else "✓"
            if is_bad: bad += 1
            print(f"  {marker} {sel}[{i}] bg={bg} color={color}")
    print(f"  >>> found={found}, bad={bad}")
    return bad


def click_tab(page, text):
    for t in page.query_selector_all(".styles-tab"):
        if text in t.inner_text():
            t.click()
            return True
    return False


with sync_playwright() as p:
    browser = p.chromium.launch()
    ctx = browser.new_context(viewport={"width": 1440, "height": 900})
    page = ctx.new_page()
    page.goto(f"{BASE}/console/styles", wait_until="load")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2500)
    page.evaluate("document.body.setAttribute('data-theme', 'dark')")
    page.wait_for_timeout(500)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_styles_my.png", full_page=True)
    audit(page, "我的风格 tab")

    cards = page.query_selector_all(".style-add-card")
    if cards:
        cards[0].click()
        page.wait_for_timeout(800)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_styles_editor.png")
        audit(page, "style editor (open)")
        page.keyboard.press("Escape")
        page.wait_for_timeout(500)

    if click_tab(page, "学习"):
        page.wait_for_timeout(800)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_styles_learned.png", full_page=True)
        audit(page, "学习的风格 tab")
        cards2 = page.query_selector_all(".style-add-card")
        if cards2:
            cards2[-1].click()
            page.wait_for_timeout(500)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_styles_learned_form.png")
            audit(page, "learned form (open)")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)

    if click_tab(page, "系统"):
        page.wait_for_timeout(800)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_styles_system.png", full_page=True)
        audit(page, "系统预设风格 tab")

    ctx.close()
    browser.close()
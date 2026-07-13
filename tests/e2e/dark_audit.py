#!/usr/bin/env python3
"""Dark mode audit: open modals, simulate queue data, inspect computed backgrounds."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"

# Mini-queue panel and works list now load from backend; only set theme here.
QUEUE_JS = """
localStorage.setItem('aichuangzuo_theme', 'dark');
"""

WORKS_JS = """
localStorage.setItem('aichuangzuo_theme', 'dark');
"""


def check(page, selectors, label):
    print(f"\n--- {label} ---")
    for sel in selectors:
        try:
            els = page.query_selector_all(sel)
            for i, el in enumerate(els):
                if not el.is_visible():
                    continue
                bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
                color = el.evaluate("e => getComputedStyle(e).color")
                print(f"  {sel}[{i}] bg={bg} color={color}")
        except Exception as e:
            print(f"  {sel} ERR {e}")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # === Create page with queue data ===
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate(QUEUE_JS)
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(800)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_create_with_data.png", full_page=True)
        check(page, [".hero-title-input", ".hero-textarea", ".queue-panel-item", ".queue-panel-item.completed", ".queue-panel-item.generating", ".queue-panel-item.queued"], "create / queue panel")

        # Open word count modal — clicks "1500字标准" chip
        try:
            page.click("text=1500", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_wc_modal.png", full_page=False)
            check(page, [".wc-custom-input", ".ant-modal-content", ".ant-modal-header"], "word count modal")
            # Switch to "自定义" tab
            page.click("text=自定义", timeout=2000)
            page.wait_for_timeout(500)
            check(page, [".wc-custom-input"], "word count modal custom")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"wc modal ERR {e}")

        # Open style modal then go to editor
        try:
            page.click("text=年度总结", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_modal.png", full_page=False)
            check(page, [".ant-modal-content", ".ant-modal-header"], "style modal")
            page.click("text=新建我的风格", timeout=2000)
            page.wait_for_timeout(500)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_editor.png", full_page=False)
            check(page, [".style-editor-input", ".style-editor-textarea", ".ant-modal-content"], "style editor")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"style modal ERR {e}")
        ctx.close()

        # === Works page ===
        ctx2 = browser.new_context(viewport={"width": 1440, "height": 900})
        page2 = ctx2.new_page()
        page2.goto(BASE, wait_until="networkidle")
        page2.evaluate(WORKS_JS)
        page2.goto(f"{BASE}/console/works", wait_until="networkidle")
        page2.wait_for_timeout(800)
        page2.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page2.wait_for_timeout(500)
        page2.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works.png", full_page=True)
        check(page2, [".work-card", ".draft-card", ".work-title", ".work-action-btn", ".work-action-btn.primary", ".work-action-btn.danger"], "works page / works tab")
        # Switch to drafts
        try:
            page2.click("text=草稿箱", timeout=2000)
            page2.wait_for_timeout(500)
            check(page2, [".draft-card", ".draft-card .work-title"], "works / drafts tab")
        except Exception as e:
            print(f"drafts click ERR {e}")
        ctx2.close()
        browser.close()


if __name__ == "__main__":
    main()

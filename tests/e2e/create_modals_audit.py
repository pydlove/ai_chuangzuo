#!/usr/bin/env python3
"""Audit three create-page modals in dark mode: 字数, 风格/年度总结, 导出模板."""
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
        page.goto(f"{BASE}/console/create?bust=51", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # === 字数 modal ===
        try:
            page.click("text=1500", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_wc_modal.png", full_page=False)
            sels_wc = [
                ".word-count-modal .ant-modal-content",
                ".word-count-modal .ant-modal-header",
                ".wc-tabs",
                ".wc-tab",
                ".wc-tab.active",
                ".wc-item",
                ".wc-item.selected",
                ".wc-item-wide",
                ".wc-item-wide.selected",
                ".wc-count",
                ".wc-label",
                ".wc-desc",
                ".wc-custom-input",
                ".wc-custom-row",
            ]
            check(page, sels_wc, "字数 modal")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"wc ERR {e}")

        # === 风格 modal — opens to list, then click 新建我的风格 to enter editor ===
        try:
            page.click(".smart-defaults .settings-chip:nth-child(3)", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_modal.png", full_page=False)
            sels_style = [
                ".style-modal .ant-modal-content",
                ".style-modal .ant-modal-header",
                ".modal-title",
                ".modal-subtitle",
                ".style-tabs",
                ".style-tab",
                ".style-tab.active",
            ]
            check(page, sels_style, "风格 modal (list)")
            # Click "新建我的风格" button to enter editor
            try:
                page.click("text=新建我的风格", timeout=2000)
                page.wait_for_timeout(500)
                page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_editor.png", full_page=False)
                sels_editor = [
                    ".style-editor-input",
                    ".style-editor-textarea",
                    ".style-editor-label",
                    ".style-editor-hint",
                    ".style-preset-card",
                    ".style-preset-title",
                    ".style-preset-desc",
                    ".save-style-btn",
                ]
                check(page, sels_editor, "风格 modal (editor)")
                # Click a preset
                try:
                    page.click("text=年度总结", timeout=2000)
                    page.wait_for_timeout(400)
                except Exception:
                    pass
            except Exception as e:
                print(f"editor open ERR {e}")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"style ERR {e}")

        # === 导出模板 modal — click the template settings-chip (4th chip) ===
        try:
            page.click(".smart-defaults .settings-chip:nth-child(4)", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_template_modal.png", full_page=False)
            sels_tpl = [
                ".template-modal .ant-modal-content",
                ".template-modal .ant-modal-header",
                ".modal-title",
                ".modal-subtitle",
                ".template-tabs",
                ".template-tab",
                ".template-tab.active",
                ".template-body",
                ".template-preview-pane",
                ".template-list-pane",
                ".template-row",
                ".template-row.selected",
                ".template-row-name",
                ".template-row-desc",
                ".template-group-title",
                ".template-footer",
                ".template-apply-btn",
            ]
            check(page, sels_tpl, "导出模板 modal")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"template ERR {e}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
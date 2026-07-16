#!/usr/bin/env python3
"""4 个配置弹框回归：极简模式下逐一点开，断言弹框可见并截图（浅色/深色主页）。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.goto(f"{BASE}/console/create", wait_until="domcontentloaded")
        page.evaluate("localStorage.setItem('aichuangzuo_create_mode', 'minimal')")
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)
        page.screenshot(path=f"{SHOTS}/cmp_after_main.png")

        chips = page.query_selector_all("button.settings-chip")
        names = ["platform", "wc", "style", "template"]
        results = []
        for i, name in enumerate(names):
            ok = False
            if i < len(chips):
                chips[i].click()
                page.wait_for_timeout(700)
                ok = page.query_selector(".ant-modal-content") is not None
                page.screenshot(path=f"{SHOTS}/cmp_after_{name}.png")
                page.keyboard.press("Escape")
                page.wait_for_timeout(400)
            results.append((name, ok))
            print(("PASS modal-" + name) if ok else ("FAIL modal-" + name))

        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark'); document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(600)
        page.screenshot(path=f"{SHOTS}/cmp_after_main_dark.png")
        browser.close()
        if not all(ok for _, ok in results) or len(results) != 4:
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

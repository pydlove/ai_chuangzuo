#!/usr/bin/env python3
"""弹框提取前后对比：截取 4 个弹框 + 主页面（浅色/深色）。"""
import os, sys
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22346")
TAG = sys.argv[1] if len(sys.argv) > 1 else "before"
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.screenshot(path=f"{SHOTS}/cmp_{TAG}_main.png")

        chips = page.query_selector_all("button.settings-chip")
        names = ["platform", "wc", "style", "template"]
        for i, name in enumerate(names):
            if i < len(chips):
                chips[i].click()
                page.wait_for_timeout(700)
                page.screenshot(path=f"{SHOTS}/cmp_{TAG}_{name}.png")
                page.keyboard.press("Escape")
                page.wait_for_timeout(400)

        # 深色主页
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark'); document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(600)
        page.screenshot(path=f"{SHOTS}/cmp_{TAG}_main_dark.png")
        browser.close()
        print("done", TAG)

main()

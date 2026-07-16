#!/usr/bin/env python3
"""极简模式：一体化卡片、配置胶囊、textarea 自动撑高、深色兼容。"""
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

        ok_card = page.query_selector(".hero-card") is not None
        chips = page.query_selector_all(".hero-chips .settings-chip")
        ok_chips = len(chips) == 4 and all("chip-icon" in (c.inner_html() or "") for c in chips)

        h0 = page.eval_on_selector(".hero-textarea", "e => e.offsetHeight")
        page.fill(".hero-textarea", "测试内容\n" * 30)
        page.wait_for_timeout(300)
        h1 = page.eval_on_selector(".hero-textarea", "e => e.offsetHeight")
        ok_grow = h0 <= 130 and h1 > h0

        page.screenshot(path=f"{SHOTS}/minimal_light.png")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark'); document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)
        page.screenshot(path=f"{SHOTS}/minimal_dark.png")

        print("PASS card" if ok_card else "FAIL card")
        print(f"PASS chips({len(chips)})" if ok_chips else f"FAIL chips({len(chips)})")
        print(f"PASS autogrow({h0}->{h1})" if ok_grow else f"FAIL autogrow({h0}->{h1})")
        browser.close()
        if not (ok_card and ok_chips and ok_grow):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

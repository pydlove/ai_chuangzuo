#!/usr/bin/env python3
"""移动端：引导模式聊天流、极简模式卡片、队列抽屉全屏。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"
VIEW = {"width": 390, "height": 844}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport=VIEW, is_mobile=True, has_touch=True)
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                                "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/**", lambda r: r.fulfill(json={"code": 0, "data": {}}))
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        ok_chat = page.query_selector(".guided-chat") is not None
        page.screenshot(path=f"{SHOTS}/mobile_guided.png")

        page.click("text=熟手模式")
        page.wait_for_timeout(600)
        ok_minimal = page.query_selector(".minimal-panel") is not None
        page.screenshot(path=f"{SHOTS}/mobile_minimal.png")

        page.click("text=队列")
        page.wait_for_timeout(800)
        drawer = page.query_selector(".ant-drawer-content-wrapper")
        w = page.evaluate("e => e.offsetWidth", drawer) if drawer else 0
        ok_full = w >= 390
        page.screenshot(path=f"{SHOTS}/mobile_drawer.png")

        for name, ok in [("guided", ok_chat), ("minimal", ok_minimal), ("drawer-fullscreen", ok_full)]:
            print(("PASS " if ok else "FAIL ") + name)
        browser.close()
        if not all([ok_chat, ok_minimal, ok_full]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

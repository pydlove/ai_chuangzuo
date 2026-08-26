#!/usr/bin/env python3
"""Verify mobile weekly data page hero header."""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:28586")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"
VIEW = {"width": 390, "height": 844}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport=VIEW, is_mobile=True, has_touch=True)

        page.route("**/api/v1/user/**", lambda route: route.fulfill(json={"code": 0, "data": {}}))
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.evaluate('localStorage.setItem("aichuangzuo_access_token", "mock-token")')
        page.goto(f"{BASE}/console/weekly-data", wait_until="networkidle")
        try:
            page.locator("#app-loader").wait_for(state="detached", timeout=8000)
        except Exception:
            pass
        page.wait_for_timeout(300)

        hero = page.query_selector(".weekly-data-hero")
        logo = page.query_selector(".weekly-data-hero-logo")
        icon = page.query_selector(".weekly-data-hero-icon")
        ok = hero is not None and logo is not None and icon is not None
        page.screenshot(path=f"{SHOTS}/weekly_data_page_mobile.png")
        print(("PASS " if ok else "FAIL ") + "hero-header")
        browser.close()
        if not ok:
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

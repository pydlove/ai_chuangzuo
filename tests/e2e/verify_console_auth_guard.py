#!/usr/bin/env python3
"""验证：未登录直接访问 /console/create 应被重定向到 /login，而不是弹出 403。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        # 清除登录态，模拟未登录用户直接打开创作页
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.removeItem('aichuangzuo_access_token')")
        page.evaluate("localStorage.removeItem('aichuangzuo_refresh_token')")

        # 直接访问创作页
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(500)

        current_path = page.evaluate("window.location.pathname")
        assert current_path == "/login", f"未登录访问 /console/create 应重定向到 /login，实际为 {current_path}"

        print("PASS - 未登录访问 /console/create 已正确重定向到 /login")
        browser.close()


main()

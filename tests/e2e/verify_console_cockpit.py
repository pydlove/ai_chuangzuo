#!/usr/bin/env python3
"""管理端 - 运营驾驶舱页面验证。

前置：
- admin-api (26060) 与 admin-web dev (22347) 已启动
- 管理员账号：环境变量 ADMIN_USER / ADMIN_PASS（默认 admin / Root1qaz!QAZ）

输出截图到 tests/e2e/screenshots/console_cockpit/
"""

import os
import time
from pathlib import Path

from playwright.sync_api import sync_playwright

ADMIN_URL = os.environ.get("ADMIN_URL", "http://localhost:22347")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "Root1qaz!QAZ")

SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "console_cockpit"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def login(page):
    page.goto(f"{ADMIN_URL}/login")
    page.fill('input[placeholder="请输入管理员账号"]', ADMIN_USER)
    page.fill('input[placeholder="请输入密码"]', ADMIN_PASS)
    page.click('.ant-btn-primary:has-text("登")')
    # 滑块人机验证：等弹框动画稳定后拖到底
    handle = page.locator(".slider-handle")
    handle.wait_for(state="visible", timeout=8_000)
    page.wait_for_timeout(600)
    track = page.locator(".slider-track").first
    t = track.bounding_box()
    h = handle.bounding_box()
    hy = h["y"] + h["height"] / 2
    start_x = h["x"] + h["width"] / 2
    end_x = t["x"] + t["width"] - h["width"] / 2 - 1
    handle.hover()
    page.mouse.down()
    steps = 30
    for i in range(1, steps + 1):
        page.mouse.move(start_x + (end_x - start_x) * i / steps, hy)
        time.sleep(0.02)
    page.mouse.up()
    page.wait_for_url(lambda u: "/login" not in u, timeout=10_000)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1600, "height": 1000}, device_scale_factor=2)
        page = ctx.new_page()

        login(page)
        page.goto(f"{ADMIN_URL}/console/dashboard")
        page.wait_for_selector("text=运营驾驶舱", timeout=10_000)
        # 等待图表渲染
        page.wait_for_timeout(2500)
        page.screenshot(path=SCREENSHOTS_DIR / "01-cockpit-full.png", full_page=True)

        # 切换趋势 tab：用户 / 创作
        page.click('label:has-text("用户")')
        page.wait_for_timeout(800)
        page.screenshot(path=SCREENSHOTS_DIR / "02-trend-user.png")
        page.click('label:has-text("创作")')
        page.wait_for_timeout(800)
        page.screenshot(path=SCREENSHOTS_DIR / "03-trend-article.png")

        # 接口冒烟：用页面上下文 fetch（带 Authorization header）
        smoke = page.evaluate("""async () => {
          const token = JSON.parse(localStorage.getItem('admin_access_token'))
          const headers = { 'Authorization': 'Bearer ' + token }
          const trend = await fetch('/api/v1/admin/stats/dashboard/trend?days=30', { headers }).then(r => r.json())
          const dist = await fetch('/api/v1/admin/stats/dashboard/distribution', { headers }).then(r => r.json())
          return {
            trendPoints: trend.data.points.length,
            lastPoint: trend.data.points[trend.data.points.length - 1],
            distribution: dist.data
          }
        }""")
        print("smoke:", smoke)

        browser.close()
        print("screenshots ->", SCREENSHOTS_DIR)


if __name__ == "__main__":
    main()

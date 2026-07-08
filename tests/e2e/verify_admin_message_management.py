#!/usr/bin/env python3
"""管理端-消息管理 端到端验证（精简版）。

覆盖：
  - 登录管理端（拖滑块）
  - 进入「消息管理」页
  - 3 个 tab 切换
  - 列表展示已读数 / 受众

复杂流程（新建/编辑/详情）已通过 API 单独覆盖。
"""
import re
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

ADMIN_WEB = "http://127.0.0.1:22346"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def drag_slider(page):
    page.wait_for_selector(".slider-modal .slider-handle", timeout=10000)
    page.wait_for_timeout(500)
    handle = page.locator(".slider-modal .slider-captcha .slider-handle").first
    box = handle.bounding_box()
    sx = box["x"] + box["width"] / 2
    sy = box["y"] + box["height"] / 2
    ex = sx + 400
    page.evaluate(
        "({x,y}) => document.elementFromPoint(x,y).dispatchEvent(new MouseEvent('mousedown', {bubbles:true,clientX:x,clientY:y,button:0}))",
        {"x": sx, "y": sy},
    )
    for step in range(1, 31):
        cx = sx + (ex - sx) * step / 30
        page.evaluate(
            "({x,y}) => document.dispatchEvent(new MouseEvent('mousemove', {bubbles:true,clientX:x,clientY:y,button:0}))",
            {"x": cx, "y": sy},
        )
        page.wait_for_timeout(10)
    page.evaluate(
        "({x,y}) => document.dispatchEvent(new MouseEvent('mouseup', {bubbles:true,clientX:x,clientY:y,button:0}))",
        {"x": ex, "y": sy},
    )


def main():
    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 登录
        page.goto(f"{ADMIN_WEB}/login")
        page.wait_for_selector("input[placeholder*='管理员账号']", timeout=10000)
        page.fill("input[placeholder*='管理员账号']", "admin")
        page.fill("input[placeholder*='请输入密码']", "Root1qaz!QAZ")
        page.click(".submit-btn")
        drag_slider(page)
        page.wait_for_url(re.compile(r"/console/"), timeout=15000)
        results.append(("登录成功", "/console" in page.url))

        # 进入消息管理
        page.click(".ant-menu-item:has-text('消息管理')")
        page.wait_for_selector(".ant-tabs", timeout=10000)
        results.append(("进入消息管理", "/console/messages" in page.url))

        # 3 个 tab 切换
        for tab_label in ["公告", "新功能", "优惠活动"]:
            tab = page.locator(f".ant-tabs-tab:has-text('{tab_label}')").first
            tab.click()
            page.wait_for_timeout(800)
            active = page.locator(".ant-tabs-tab-active").first.inner_text()
            results.append((f"切到 {tab_label} tab", tab_label in active))

        # 列表能展示表格行
        page.click(".ant-tabs-tab:has-text('公告')")
        page.wait_for_timeout(800)
        table_rows = page.locator(".ant-table-tbody tr").count()
        results.append((f"公告 tab 表格有 {table_rows} 行", table_rows > 0))

        # 列表「已读」列展示
        read_cells = page.locator(".ant-table-tbody td:nth-child(4)").all_inner_texts()
        has_read_format = any("/" in c for c in read_cells)
        results.append(("列表「已读」列展示「N/M」格式", has_read_format))

        page.screenshot(path=str(SCREENSHOT_DIR / "msg_admin_full.png"))
        browser.close()

    print("\n=== 管理端-消息管理 端到端验证结果 ===")
    all_ok = True
    for name, ok in results:
        status = "✓ PASS" if ok else "✗ FAIL"
        print(f"{status}  {name}")
        if not ok:
            all_ok = False
    print()
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""队列抽屉：触发按钮打开、进行中任务徽章、列表渲染。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

TASK = {
    "id": 101, "bizNo": "T101", "status": 1, "progressPct": 45,
    "title": "35岁被裁后，我靠副业翻身", "wordLimitTarget": 1500,
    "inputParam": {"title": "35岁被裁后，我靠副业翻身", "platform": "wechat"},
    "createdAt": "2026-07-16T10:00:00"
}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.route("**/api/v1/user/generation-tasks?**",
                   lambda r: r.fulfill(json={"code": 0, "data": {"list": [TASK], "total": 1}}))
        page.route("**/api/v1/user/generation-tasks",
                   lambda r: r.fulfill(json={"code": 0, "data": {"list": [TASK], "total": 1}}))
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        page.click("text=队列", timeout=3000)
        page.wait_for_timeout(800)
        drawer = page.query_selector(".ant-drawer-open .queue-panel-list")
        ok_drawer = drawer is not None
        ok_progress = page.query_selector(".ant-drawer-open .queue-item-progress") is not None
        page.screenshot(path=f"{SHOTS}/queue_drawer.png")
        print("PASS drawer" if ok_drawer else "FAIL drawer")
        print("PASS progress" if ok_progress else "FAIL progress")
        browser.close()
        if not (ok_drawer and ok_progress):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

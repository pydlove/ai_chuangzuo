#!/usr/bin/env python3
"""AI 创作队列端到端验证。

覆盖场景：
  - 登录：调用 /api/v1/user/auth/login 获取 token
  - 打开 /console/ai-generate?title=... 验证从 CreateIndex 跳转过来的预填参数
  - 提交生成任务
  - 轮询等待任务进入终端状态（因使用 fake AI endpoint，预期最终失败）
  - 验证历史列表出现该任务
  - 验证额度被预扣并在失败后退回

前置条件：
  - user-api 运行在 25050，admin-api 运行在 26060
  - user-web dev server 运行在 22345
  - DB 中已存在 test@local / Test@123 用户，且余额充足
  - DB 中已存在 active 模型配置和 enabled 提示词模板（指向不存在的服务端点）

用法：
  python3 tests/e2e/verify_generation_queue.py
"""
import sys
import time
from pathlib import Path

import requests
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:22345"
API_URL = "http://localhost:25050/api/v1/user"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)

EMAIL = "test@local"
PASSWORD = "Test@123"


def login():
    resp = requests.post(
        f"{API_URL}/auth/login",
        json={"email": EMAIL, "password": PASSWORD},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"登录失败: {body}"
    return body["data"]["accessToken"]


def get_balance():
    resp = requests.get(
        f"{API_URL}/account/summary",
        headers={"Authorization": f"Bearer {TOKEN}"},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"查余额失败: {body}"
    return float(body["data"]["coinBalance"])


def main():
    global TOKEN
    TOKEN = login()
    balance_before = get_balance()
    print(f"[info] 登录成功，用户余额: {balance_before}")

    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 预设 token 后打开 AI 创作页（模拟从 CreateIndex 跳转）
        page.goto(BASE_URL)
        page.evaluate(
            f"() => {{ localStorage.setItem('aichuangzuo_access_token', '{TOKEN}'); }}"
        )
        page.goto(
            f"{BASE_URL}/console/ai-generate?title=SmokeTest+%E6%A0%87%E9%A2%98&description=SmokeTest+%E6%8F%8F%E8%BF%B0&platform=xiaohongshu&wordCount=1200&styleRef=%E5%8F%A3%E8%AF%AD%E5%8C%96"
        )
        page.wait_for_selector(".page-title:has-text('AI 创作')", timeout=10000)
        results.append(("打开 AI 创作页", "/console/ai-generate" in page.url))

        # 验证预填参数
        title_value = page.locator(".generation-queue input.ant-input").first.input_value()
        desc_value = page.locator(".generation-queue textarea.ant-input").first.input_value()
        platform_value = page.locator(".generation-queue .ant-select-selection-item").first.inner_text()
        results.append(("标题已预填", "SmokeTest 标题" in title_value))
        results.append(("描述已预填", "SmokeTest 描述" in desc_value))
        results.append(("平台已预填", platform_value == "小红书"))
        page.screenshot(path=str(SCREENSHOT_DIR / "generation_queue_01_prefilled.png"))

        # 提交任务
        page.click("button:has-text('开始生成')")
        page.wait_for_timeout(500)
        page.screenshot(path=str(SCREENSHOT_DIR / "generation_queue_02_submitting.png"))

        # 等待当前任务卡片出现
        page.wait_for_selector(".current-card", timeout=10000)
        results.append(("提交后显示当前任务卡片", True))

        # 轮询等待终端状态（最多 90 秒）
        terminal = False
        for _ in range(90):
            status_text = page.locator(".status-tag").first.inner_text()
            if status_text in ("已完成", "失败"):
                terminal = True
                break
            time.sleep(1)
        results.append(("任务到达终端状态", terminal))
        page.screenshot(path=str(SCREENSHOT_DIR / "generation_queue_03_terminal.png"))

        # 刷新历史列表
        page.click("button:has-text('刷新')")
        page.wait_for_timeout(1000)
        page.screenshot(path=str(SCREENSHOT_DIR / "generation_queue_04_history.png"))

        # 历史列表至少有一条
        rows = page.locator(".ant-table-tbody tr").count()
        results.append(("历史列表有记录", rows >= 1))

        browser.close()

    # 验证余额最终一致（预扣 1，失败后退回 1）
    time.sleep(2)
    balance_after = get_balance()
    results.append(("失败后余额退回", abs(balance_after - balance_before) < 0.01))
    print(f"[info] 测试后余额: {balance_after}")

    print("\n=== AI 创作队列端到端验证结果 ===")
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

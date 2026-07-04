#!/usr/bin/env python3
"""重置密码端到端验证。

场景 A — 完整重置流程：
  1. 通过后端 API 注册测试账号（绕过 UI）
  2. /forgot 页面：填邮箱 → 点获取验证码 → 弹框拖滑块 → 通过自动发码
  3. 填 mock 邮箱码 000000 + 新密码
  4. 点重置密码 → 弹框拖滑块 → 通过自动调重置接口 → 跳转 /login
  5. /login 用新密码登录成功
  6. 旧密码登录应被拒

场景 B — refresh token 失效：
  1. 用场景 A 拿到的旧 refresh_token 直接调 /refresh-token
  2. 期望返回 REFRESH_TOKEN_INVALID（密码重置前签发，已失效）

前置：
  - 后端 SPRING_PROFILES_ACTIVE=test 启动（captcha=TEST12, email=000000）
  - 前端 22345 运行

用法：
  python3 tests/e2e/verify_reset_password.py
"""
import re
import sys
import time
from pathlib import Path

import requests
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:22345"
API_URL = "http://localhost:25050/api/v1/user"
EMAIL_CODE_MOCK = "000000"
OLD_PASSWORD = "OldPass123"
NEW_PASSWORD = "BrandNew789"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def drag_slider_to_end(page, modal_selector):
    """与 verify_user_auth.py 相同的滑块模拟拖动实现。

    modal_selector 传入当前要操作的弹框类，避免和另一个弹框冲突。
    """
    handle = page.locator(f"{modal_selector} .slider-captcha .slider-handle").first
    handle.wait_for(state="visible", timeout=5000)
    page.wait_for_timeout(500)
    handle_box = handle.bounding_box()
    assert handle_box, "未找到滑块 handle 位置"
    start_x = handle_box["x"] + handle_box["width"] / 2
    start_y = handle_box["y"] + handle_box["height"] / 2
    end_x = start_x + 400

    page.evaluate(
        """({sel, x, y}) => {
            const el = document.querySelector(sel + ' .slider-captcha .slider-handle');
            el.dispatchEvent(new MouseEvent('mousedown', {
                bubbles: true, cancelable: true,
                clientX: x, clientY: y, button: 0
            }));
        }""",
        {"sel": modal_selector, "x": start_x, "y": start_y},
    )
    for step in range(1, 31):
        cur_x = start_x + (end_x - start_x) * step / 30
        page.evaluate(
            """({x, y}) => {
                document.dispatchEvent(new MouseEvent('mousemove', {
                    bubbles: true, cancelable: true,
                    clientX: x, clientY: y, button: 0
                }));
            }""",
            {"x": cur_x, "y": start_y},
        )
        page.wait_for_timeout(10)
    page.evaluate(
        """({x, y}) => {
            document.dispatchEvent(new MouseEvent('mouseup', {
                bubbles: true, cancelable: true,
                clientX: x, clientY: y, button: 0
            }));
        }""",
        {"x": end_x, "y": start_y},
    )
    page.wait_for_timeout(200)
    slider_class = page.locator(f"{modal_selector} .slider-captcha").first.get_attribute("class")
    assert "is-passed" in (slider_class or ""), f"滑块未通过，class={slider_class}"


def register_via_api(email, password):
    """通过后端 API（不经过 UI）注册一个账号，拿到 refresh token 用于场景 B。"""
    cap = requests.get(f"{API_URL}/auth/captcha").json()
    requests.post(f"{API_URL}/auth/email-codes", json={
        "email": email,
        "captchaKey": cap["data"]["captchaKey"],
        "captchaCode": "TEST12",
    })
    cap2 = requests.get(f"{API_URL}/auth/captcha").json()
    resp = requests.post(f"{API_URL}/auth/register", json={
        "email": email,
        "emailCode": EMAIL_CODE_MOCK,
        "password": password,
        "confirmPassword": password,
        "captchaKey": cap2["data"]["captchaKey"],
        "captchaCode": "TEST12",
    })
    assert resp.status_code == 200, f"register failed: {resp.text}"
    body = resp.json()
    assert body.get("code") == 0, f"register business error: {body}"
    return body["data"]


def main():
    email = f"e2e_reset_{int(time.time())}@example.com"
    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # ============== 0. 通过 API 预备测试账号 ==============
        api_token = register_via_api(email, OLD_PASSWORD)
        assert api_token and api_token.get("accessToken"), "API 注册失败"
        results.append(("API 注册测试账号成功", True))

        # ============== 场景 A — 完整重置流程 ==============
        page.goto(f"{BASE_URL}/forgot")
        page.wait_for_selector(".forgot-card", timeout=10000)
        results.append(("打开 /forgot 页面", "/forgot" in page.url))

        # 1. 填邮箱
        page.fill("input[placeholder='请输入注册邮箱']:visible", email)
        results.append(("填写邮箱", True))

        # 2. 点获取验证码 → 弹框 → 拖滑块 → 通过后自动发码
        page.click("button.code-btn:has-text('获取验证码')")
        page.wait_for_selector(".slider-modal-send-code", state="visible", timeout=5000)
        results.append(("点获取验证码弹出滑块弹框", True))
        drag_slider_to_end(page, ".slider-modal-send-code")
        page.wait_for_selector(".slider-modal-send-code", state="hidden", timeout=10000)
        page.wait_for_selector("button.code-btn:has-text('s')", timeout=5000)
        results.append(("滑块通过自动发送邮箱验证码", True))

        # 3. 填邮箱码 + 新密码
        page.fill("input[placeholder='输入 6 位验证码']:visible", EMAIL_CODE_MOCK)
        page.fill("input[placeholder='6-20 位新密码']:visible", NEW_PASSWORD)
        page.fill("input[placeholder='再次输入新密码']:visible", NEW_PASSWORD)

        # 4. 点重置 → 弹框 → 拖滑块 → 通过 → 跳 /login
        page.click("button.submit-btn:has-text('重置密码')")
        page.wait_for_selector(".slider-modal-reset", state="visible", timeout=5000)
        results.append(("点重置密码弹出滑块弹框", True))
        drag_slider_to_end(page, ".slider-modal-reset")
        page.wait_for_url(re.compile(r"/login"), timeout=15000)
        results.append(("滑块通过自动重置并跳转 /login", "/login" in page.url))

        page.screenshot(path=str(SCREENSHOT_DIR / "reset_password_01_after_reset.png"))

        # 5. /login 用新密码登录成功
        page.fill("input[placeholder='请输入邮箱']:visible", email)
        page.fill("input[placeholder='请输入密码']:visible", NEW_PASSWORD)
        page.click("button.submit-btn:has-text('登录')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        drag_slider_to_end(page, ".slider-modal")
        page.wait_for_url(re.compile(r"/console"), timeout=15000)
        results.append(("用新密码登录成功跳转 /console", "/console" in page.url))

        new_access = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        results.append(("新密码登录后 token 已写入", bool(new_access)))

        # 清理：退出登录
        page.click(".console-avatar")
        page.wait_for_selector(".user-action-logout", timeout=5000)
        page.click(".user-action-logout")
        page.wait_for_url(re.compile(r"/login"), timeout=10000)

        # 6. 旧密码登录应失败（在 /login 页面再走一遍）
        page.fill("input[placeholder='请输入邮箱']:visible", email)
        page.fill("input[placeholder='请输入密码']:visible", OLD_PASSWORD)
        page.click("button.submit-btn:has-text('登录')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        drag_slider_to_end(page, ".slider-modal")
        # 期望 error 提示或不跳转（在 /login）
        page.wait_for_timeout(2000)
        results.append(("旧密码登录被拒绝", "/login" in page.url))
        page.screenshot(path=str(SCREENSHOT_DIR / "reset_password_02_old_pwd_rejected.png"))

        # ============== 场景 B — refresh token 失效 ==============
        old_refresh = api_token["refreshToken"]
        resp = requests.post(f"{API_URL}/auth/refresh-token",
                             json={"refreshToken": old_refresh})
        # 期望返回非 0 的业务码（refreshToken 无效）
        try:
            body = resp.json()
            rejected = (resp.status_code != 200) or (body.get("code") != 0)
        except Exception:
            rejected = resp.status_code != 200
        results.append(("旧 refresh token 在密码重置后被拒绝", rejected))

        browser.close()

    print("\n=== 重置密码端到端验证结果 ===")
    print(f"测试邮箱：{email}")
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

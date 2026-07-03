#!/usr/bin/env python3
"""用户认证流程端到端验证。

覆盖场景：
  1) 注册：打开 /login → 切到注册 tab → 后端 API 获取 captcha + email code →
            表单填写 → 提交 → 跳转 /console
  2) 验证：localStorage 中 access_token / refresh_token 已存
  3) 退出：点击头像 → 退出登录 → 跳转 /login
  4) 验证：localStorage 中 access_token 已被清空

前置条件：
  - 后端运行在 25050，前端运行在 22345
  - 后端以 SPRING_PROFILES_ACTIVE=test 启动（启用 mock 验证码）

用法：
  python3 tests/e2e/verify_user_auth.py
"""
import re
import sys
import time
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:22345"
API_URL = "http://localhost:25050/api/v1/user"
CAPTCHA_MOCK_CODE = "TEST12"
EMAIL_CODE_MOCK = "000000"
PASSWORD = "Test123456"

SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def main():
    # 用时间戳保证邮箱唯一，避免重复注册命中唯一约束
    email = f"e2e_user_{int(time.time())}@example.com"
    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # ============== 1. 打开登录页 ==============
        page.goto(f"{BASE_URL}/login")
        page.wait_for_selector(".login-card", timeout=10000)
        results.append(("打开登录页", "/login" in page.url))

        # ============== 2. 切到注册 tab ==============
        page.click("button.auth-tab:has-text('注册')")
        page.wait_for_selector("text=创建账号", timeout=5000)
        # 用 :visible 过滤掉被 v-show 隐藏的登录表单标题
        register_title = page.locator(".form-title:visible").inner_text()
        results.append(("切换到注册 tab", "创建账号" in register_title))

        # 等注册表单中的 captcha 图片就绪（DOM 中共有 2 个 .captcha-img，
        # 通过 :visible 过滤掉登录表单那个被 v-show 隐藏的）
        page.wait_for_selector(".captcha-img:visible", timeout=10000)

        # ============== 3. 通过 API 获取 captcha key ==============
        captcha_resp = ctx.request.get(f"{API_URL}/auth/captcha").json()
        captcha_key = captcha_resp.get("data", {}).get("captchaKey")
        results.append(("captcha API 返回 captchaKey", bool(captcha_key)))

        # ============== 4. 发送邮箱验证码 ==============
        email_resp = ctx.request.post(
            f"{API_URL}/auth/email-codes",
            data={
                "email": email,
                "captchaKey": captcha_key,
                "captchaCode": CAPTCHA_MOCK_CODE,
            },
        ).json()
        results.append(("邮箱验证码发送成功", email_resp.get("code") == 0))

        # ============== 5. 填写注册表单 ==============
        # 登录/注册两个表单都有相同 placeholder，:visible 过滤掉被 v-show 隐藏的
        page.fill("input[placeholder='请输入邮箱']:visible", email)
        page.fill("input[placeholder='输入验证码']:visible", CAPTCHA_MOCK_CODE)
        page.fill("input[placeholder='输入 6 位验证码']:visible", EMAIL_CODE_MOCK)
        page.fill("input[placeholder='6-20 位密码']:visible", PASSWORD)
        page.fill("input[placeholder='再次输入密码']:visible", PASSWORD)
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_01_register_filled.png"))

        # ============== 6. 提交注册 ==============
        page.click("button.submit-btn:has-text('注册')")
        page.wait_for_url(re.compile(r"/console"), timeout=15000)
        results.append(("注册成功跳转 /console", "/console" in page.url))

        # ============== 7. 验证 token 写入 ==============
        access_token = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        refresh_token = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_refresh_token')"
        )
        results.append(("access_token 已写入 localStorage", bool(access_token)))
        results.append(("refresh_token 已写入 localStorage", bool(refresh_token)))

        # 等待 console 页面渲染完成
        page.wait_for_timeout(1500)
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_02_console.png"))

        # ============== 8. 退出登录 ==============
        page.click(".console-avatar")
        page.wait_for_selector(".user-action-logout", timeout=5000)
        page.click(".user-action-logout")
        page.wait_for_url(re.compile(r"/login"), timeout=10000)
        results.append(("退出登录跳转 /login", "/login" in page.url))
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_03_logout.png"))

        # ============== 9. 验证 token 已清空 ==============
        access_after = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        results.append(("退出后 access_token 已清空", access_after is None))

        browser.close()

    # ============== 输出结果 ==============
    print("\n=== 用户认证端到端验证结果 ===")
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

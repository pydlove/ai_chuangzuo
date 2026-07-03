#!/usr/bin/env python3
"""用户认证流程端到端验证。

覆盖场景：
  - 注册：打开 /login → 切到注册 tab → 填邮箱 → 点「获取验证码」→
          弹框内拖动滑块 → 通过后自动发邮件码 → 填写邮箱码+密码 →
          提交 → 跳转 /console
  - 验证：localStorage 中 access_token / refresh_token 已存
  - 退出：点击头像 → 退出登录 → 跳转 /login
  - 验证：退出后 access_token 已被清空
  - 登录：填邮箱+密码 → 点「登录」→ 弹框内拖动滑块 → 通过后调后端 →
          跳转 /console（再次写入 token）
  - 再退出：清空 token

前置条件：
  - 后端运行在 25050，前端运行在 22345
  - 后端以 SPRING_PROFILES_ACTIVE=test 启动（启用 mock 验证码：captcha=TEST12, email=000000）

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
EMAIL_CODE_MOCK = "000000"
PASSWORD = "Test123456"

SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def drag_slider_to_end(page):
    """通过 dispatchEvent 模拟滑块拖动，触发 passed 状态。

    Ant Design 的 a-modal 会把内容 Teleport 到 body，
    Playwright 的真实鼠标事件在弹框场景下未能稳定触发
    SliderCaptcha 的 @mousedown 监听，因此直接派发鼠标事件。
    """
    handle = page.locator(".slider-modal .slider-captcha .slider-handle").first
    handle.wait_for(state="visible", timeout=5000)
    page.wait_for_timeout(500)  # 等待弹框入场动画
    handle_box = handle.bounding_box()
    assert handle_box, "未找到滑块 handle 位置"
    page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_debug_before_drag.png"))
    print(f"[debug] handle_box: {handle_box}")
    start_x = handle_box["x"] + handle_box["width"] / 2
    start_y = handle_box["y"] + handle_box["height"] / 2
    # 弹框内 slider 轨道约 372px、handle 38px，maxDelta≈334px；
    # 拖 400px 确保 progress 达到 100%（≥95% 阈值）
    end_x = start_x + 400

    # 1) 在 handle 上派发 mousedown（@mousedown.onDragStart 监听在 handle 上）
    page.evaluate(
        """({x, y}) => {
            const el = document.querySelector('.slider-modal .slider-captcha .slider-handle');
            el.dispatchEvent(new MouseEvent('mousedown', {
                bubbles: true, cancelable: true,
                clientX: x, clientY: y, button: 0
            }));
        }""",
        {"x": start_x, "y": start_y},
    )

    # 2) 在 document 上分步派发 mousemove（onDragMove 监听在 document 上）
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

    # 3) 在 document 上派发 mouseup（onDragEnd 监听在 document 上）
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
    page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_debug_after_drag.png"))
    slider_class = page.locator(".slider-modal .slider-captcha").first.get_attribute("class")
    print(f"[debug] slider class: {slider_class}")
    assert "is-passed" in (slider_class or ""), f"滑块未通过，class={slider_class}"


def main():
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
        register_title = page.locator(".form-title:visible").inner_text()
        results.append(("切换到注册 tab", "创建账号" in register_title))

        # ============== 3. 填写邮箱（点获取验证码前必填） ==============
        page.fill("input[placeholder='请输入邮箱']:visible", email)
        results.append(("填写邮箱", True))

        # ============== 4. 点击「获取验证码」弹出滑块弹框 ==============
        page.click("button.code-btn:has-text('获取验证码')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        results.append(("点击获取验证码弹出滑块弹框", True))

        # ============== 5. 弹框内拖动滑块 → 通过后自动发码 ==============
        drag_slider_to_end(page)
        # 弹框关闭 + 倒计时按钮出现 = 验证码已发送
        page.wait_for_selector(".slider-modal", state="hidden", timeout=10000)
        page.wait_for_selector(
            "button.code-btn:has-text('s')", timeout=5000
        )
        results.append(("滑块通过自动发送邮箱验证码", True))

        # 截一张弹框 + 表单状态（弹框已关，按钮在倒计时）
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_01_after_slider.png"))

        # ============== 6. 填写剩余注册表单 ==============
        page.fill("input[placeholder='输入 6 位验证码']:visible", EMAIL_CODE_MOCK)
        page.fill("input[placeholder='6-20 位密码']:visible", PASSWORD)
        page.fill("input[placeholder='再次输入密码']:visible", PASSWORD)
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_02_register_filled.png"))

        # ============== 7. 提交注册 ==============
        page.click("button.submit-btn:has-text('注册')")
        page.wait_for_url(re.compile(r"/console"), timeout=15000)
        results.append(("注册成功跳转 /console", "/console" in page.url))

        # ============== 8. 验证 token 写入 ==============
        access_token = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        refresh_token = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_refresh_token')"
        )
        results.append(("access_token 已写入 localStorage", bool(access_token)))
        results.append(("refresh_token 已写入 localStorage", bool(refresh_token)))

        page.wait_for_timeout(1500)
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_03_console.png"))

        # ============== 9. 退出登录 ==============
        page.click(".console-avatar")
        page.wait_for_selector(".user-action-logout", timeout=5000)
        page.click(".user-action-logout")
        page.wait_for_url(re.compile(r"/login"), timeout=10000)
        results.append(("退出登录跳转 /login", "/login" in page.url))
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_04_logout.png"))

        # ============== 10. 验证 token 已清空 ==============
        access_after = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        results.append(("退出后 access_token 已清空", access_after is None))

        # ============== 11. 登录：填邮箱+密码 → 弹框 → 拖滑块 → 跳转 ==============
        # 当前已经在 /login 页面，登录 tab 默认激活
        page.fill("input[placeholder='请输入邮箱']:visible", email)
        page.fill("input[placeholder='请输入密码']:visible", PASSWORD)
        results.append(("登录页填写邮箱+密码", True))

        # 点击「登录」按钮 → 应弹出滑块弹框
        page.click("button.submit-btn:has-text('登录')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        results.append(("点击登录弹出滑块弹框", True))

        # 弹框内拖动滑块 → 通过后自动调后端登录接口 → 跳转 /console
        drag_slider_to_end(page)
        page.wait_for_url(re.compile(r"/console"), timeout=15000)
        results.append(("滑块通过自动登录跳转 /console", "/console" in page.url))

        # 验证 token 再次写入
        access_after_login = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        results.append(("登录后 access_token 已写入", bool(access_after_login)))

        page.wait_for_timeout(1000)
        page.screenshot(path=str(SCREENSHOT_DIR / "user_auth_05_relogin.png"))

        # ============== 12. 再退出（清理） ==============
        page.click(".console-avatar")
        page.wait_for_selector(".user-action-logout", timeout=5000)
        page.click(".user-action-logout")
        page.wait_for_url(re.compile(r"/login"), timeout=10000)
        access_final = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        results.append(("再退出后 access_token 已清空", access_final is None))

        browser.close()

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

#!/usr/bin/env python3
"""Verify the account detection modal shows a clear warning when daily limit is reached."""
import re
import time
import uuid
import jwt
from playwright.sync_api import sync_playwright

USER_SECRET = "please-change-this-access-secret-at-least-256-bits-long"
USER_WEB = "http://localhost:22345"


def make_token(user_id: int) -> str:
    now = int(time.time())
    payload = {
        "sub": str(user_id),
        "jti": str(uuid.uuid4()),
        "iat": now,
        "exp": now + 7200,
    }
    return jwt.encode(payload, USER_SECRET, algorithm="HS256")


def main():
    token = make_token(999998)
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        page.goto(USER_WEB)
        page.wait_for_load_state("networkidle")
        page.evaluate(f"localStorage.setItem('aichuangzuo_access_token', '{token}')")
        page.reload()
        page.wait_for_load_state("networkidle")

        # Mock a valid current plan so the check button is usable
        context.route(re.compile(r".*self-media-plans/current"), lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"code":0,"message":"success","data":{"platformKey":"xiaohongshu","platformName":"小红书","nicheKey":"zhichang","nicheName":"职场转型","personaKey":"shizhan","personaName":"实战派博主","goalKey":"zhangfen","goalName":"涨粉","pillars":[]}}'
        ))

        # First check returns daily limit exceeded
        checked = {"count": 0}

        def handle_check(route):
            checked["count"] += 1
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":113008,"message":"今日账号检测次数已达上限，请明天再试","data":null}'
            )

        context.route(re.compile(r".*self-media/nickname/check"), handle_check)

        page.goto(f"{USER_WEB}/console/workbench")
        page.wait_for_selector(".shortcut-card", timeout=15000)

        # Dismiss any plan onboarding modal that may appear
        page.wait_for_timeout(500)
        page.evaluate("""() => {
          document.querySelectorAll('.ant-modal-wrap').forEach(w => {
            if (getComputedStyle(w).display !== 'none') w.remove();
          });
          document.querySelectorAll('.ant-modal-mask').forEach(m => m.remove());
        }""")

        # Open account detection modal
        page.locator(".shortcut-item:has-text('账号名检测')").click()
        page.wait_for_selector(".account-modal", state="visible", timeout=5000)

        # Choose "已有账号" and trigger check
        page.click("text=已有账号")
        page.fill(".account-modal input[placeholder='输入你的账号昵称']", "测试昵称")
        page.click("text=检测名称")

        # A warning alert should appear and the check button should be disabled
        page.wait_for_selector(".ant-alert-warning:has-text('今日账号检测次数已达上限')", timeout=10000)
        button = page.locator(".account-modal button:has-text('检测名称')").first
        assert button.is_disabled(), "检测名称按钮应在达到上限后被禁用"

        print(f"Test passed: limit warning shown, check button disabled (intercepted {checked['count']} check call(s)).")

        browser.close()


if __name__ == "__main__":
    main()

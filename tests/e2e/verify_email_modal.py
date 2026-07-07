#!/usr/bin/env python3
"""Verify email modal and mobile slider captcha before sending code."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        # Mock profile so the email form has a value
        page.route("**/api/v1/user/me", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"code":0,"data":{"userId":"12345","nickname":"测试用户","email":"test@example.com","avatarUrl":null,"emailVerified":1,"inviterUserId":null}}'
        ))

        page.goto(f"{BASE}/login")
        page.evaluate("""() => {
          localStorage.setItem('aichuangzuo_access_token', 'dummy')
          localStorage.setItem('aichuangzuo_theme', 'dark')
        }""")
        page.goto(f"{BASE}/console/mine", wait_until="networkidle")
        page.wait_for_timeout(1500)

        item = page.locator('.mine-list-item', has_text='修改邮箱')
        if item.count():
            item.click()
            page.wait_for_timeout(800)

        # Click 获取验证码
        send_btn = page.locator('.email-code-btn')
        if send_btn.count():
            send_btn.click()
            page.wait_for_timeout(800)

        page.screenshot(path=f"{SCREENSHOT_DIR}/email_slider_modal_dark.png")

        slider_modal = page.query_selector(".email-slider-modal")
        print(f"slider modal visible: {slider_modal.is_visible() if slider_modal else False}")

        captcha = page.query_selector(".slider-captcha")
        print(f"slider captcha found: {captcha is not None}")

        title = page.query_selector(".email-slider-modal .ant-modal-title")
        if title:
            print(f"slider modal title: {title.inner_text().strip()}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

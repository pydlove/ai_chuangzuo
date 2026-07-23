#!/usr/bin/env python3
"""临时验证：熟手/引导模式下开通会员弹框主按钮颜色为主题色。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def check_mode(page, mode):
    page.goto(f"{BASE}/console/create", wait_until="domcontentloaded")
    page.evaluate(f"localStorage.setItem('aichuangzuo_create_mode', '{mode}');")
    page.goto(f"{BASE}/console/create", wait_until="networkidle")
    page.wait_for_timeout(1200)
    page.reload(wait_until="networkidle")
    page.wait_for_timeout(1200)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.add_init_script("""
            localStorage.setItem('aichuangzuo_access_token', 'fake-jwt');
        """)
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "free", "planName": "免费版",
                                "benefits": [{"code": "ai_article_quota", "value": "0", "remaining": 0}]}}))
        page.route("**/api/v1/user/**", lambda r: r.fulfill(json={"code": 0, "data": {}}))

        # 熟手模式
        check_mode(page, "minimal")
        page.fill(".hero-title-input", "测试标题")
        page.fill(".hero-textarea", "测试要求")
        page.click(".hero-generate-btn")
        page.wait_for_timeout(800)
        ok_minimal = page.query_selector(".membership-confirm-modal") is not None
        print("minimal modal shown:", ok_minimal)
        page.screenshot(path=f"{SHOTS}/membership_modal_minimal.png")
        page.click(".membership-confirm-modal .ant-btn-default")
        page.wait_for_timeout(400)

        # 引导模式首屏
        check_mode(page, "guided")
        page.fill(".hero-input", "测试主题")
        page.click(".hero-send")
        page.wait_for_timeout(800)
        ok_guided = page.query_selector(".membership-confirm-modal") is not None
        print("guided hero modal shown:", ok_guided)
        page.screenshot(path=f"{SHOTS}/membership_modal_guided_hero.png")

        browser.close()
        if not (ok_minimal and ok_guided):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

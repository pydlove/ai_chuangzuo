#!/usr/bin/env python3
"""验证注册页（PC + 手机端）展示客服福利卡片和二维码。"""
import sys
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
OUT = "tests/e2e/screenshots"

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # PC 端
        pc = browser.new_context(viewport={"width": 1440, "height": 900})
        page = pc.new_page()
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.click(".auth-tab:nth-child(2)")  # 注册 tab
        page.wait_for_timeout(600)
        card = page.locator(".cs-gift")
        assert card.count() == 1, "PC 注册页未找到客服福利卡片"
        assert page.locator(".cs-gift__qr").get_attribute("src"), "PC 二维码缺失"
        page.screenshot(path=f"{OUT}/register-gift-pc.png", full_page=True)
        pc.close()

        # 手机端
        mb = browser.new_context(
            viewport={"width": 390, "height": 844},
            user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
            is_mobile=True,
            has_touch=True,
        )
        page = mb.new_page()
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.click(".ml-form >> nth=1 >> .. >> text=注册", timeout=3000) if False else None
        # MobileLogin 默认登录 tab，切到注册
        page.locator(".mobile-login .tabs >> text=注册").first.click()
        page.wait_for_timeout(600)
        card = page.locator(".cs-gift")
        assert card.count() == 1, "手机端注册页未找到客服福利卡片"
        assert page.locator(".cs-gift__qr").get_attribute("src"), "手机端二维码缺失"
        page.screenshot(path=f"{OUT}/register-gift-mobile.png", full_page=True)
        mb.close()

        browser.close()
    print("OK: PC + 移动端注册页均展示客服福利卡片与二维码")

if __name__ == "__main__":
    sys.exit(run())

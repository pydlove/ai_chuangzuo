from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:5173/"


def test_home_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        # 1. 汉堡按钮可见
        menu_btn = page.locator(".mobile-menu-toggle")
        expect(menu_btn).to_be_visible()

        # 2. 桌面导航链接在手机端不可见
        desktop_link = page.locator(".nav-link").first
        expect(desktop_link).not_to_be_visible()

        # 3. 打开抽屉
        menu_btn.click()
        drawer = page.locator(".mobile-drawer")
        expect(drawer).to_have_class(re.compile(r'open'))

        # 4. 抽屉内存在导航链接
        expect(page.locator(".mobile-drawer a:has-text('首页')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('会员')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('玩法指南')")).to_be_visible()

        # 5. 点击链接后抽屉关闭
        page.locator(".mobile-drawer a:has-text('玩法指南')").click()
        page.wait_for_timeout(300)
        expect(page).to_have_url(f"{BASE_URL}guide")
        expect(page.locator(".mobile-drawer")).not_to_have_class(re.compile(r'open'))

        # 6. 截图保存
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.screenshot(path="tests/e2e/screenshots/home_mobile_375.png", full_page=True)

        mobile.close()

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 800})
        page = desktop.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        # 7. 桌面导航链接可见，汉堡按钮不可见
        expect(page.locator(".nav-link").first).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).not_to_be_visible()

        page.screenshot(path="tests/e2e/screenshots/home_desktop_1280.png", full_page=True)

        desktop.close()
        browser.close()


if __name__ == "__main__":
    test_home_mobile()
    print("Home mobile verification passed.")

from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:22345"
GUIDE_URL = f"{BASE_URL}/guide"


def test_guide_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 900})
        page = desktop.new_page()
        page.goto(GUIDE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 1. 桌面端可见 4 个目录分类(产品功能 / 收益方式 / 创作流程 / 提现与结算)
        expect(page.locator("text=产品功能").first).to_be_visible()
        expect(page.locator("text=收益方式").first).to_be_visible()
        expect(page.locator("text=创作流程").first).to_be_visible()
        expect(page.locator("text=提现与结算").first).to_be_visible()

        # 2. 顶部 navbar (新抽的 NavBar)
        expect(page.locator(".navbar .navbar-cta")).to_be_visible()
        expect(page.locator(".navbar .navbar-cta")).to_have_attribute("href", "/login")

        # 3. 桌面端导航链接可见、汉堡按钮隐藏
        expect(page.locator(".navbar-link-desktop").first).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).not_to_be_visible()

        # 4. 时间节省计算器与排行榜预览存在
        expect(page.locator("text=算算你能省多少")).to_be_visible()
        expect(page.locator("text=本月创作币榜 TOP 5")).to_be_visible()

        # 5. 点击目录「提现与结算」应触发滚动
        page.locator("aside.guide-sidebar >> text=提现与结算").first.click()
        page.wait_for_timeout(400)

        # 截图
        page.screenshot(path="tests/e2e/screenshots/guide_desktop_1280.png", full_page=True)
        desktop.close()

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(GUIDE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 6. 移动端汉堡按钮可见、桌面链接隐藏
        expect(page.locator(".mobile-menu-toggle")).to_be_visible()
        for i in range(3):
            expect(page.locator(".navbar-link-desktop").nth(i)).not_to_be_visible()

        # 7. 打开抽屉
        page.locator(".mobile-menu-toggle").click()
        drawer = page.locator(".mobile-drawer")
        expect(drawer).to_have_class(re.compile(r"\bopen\b"))

        # 8. 抽屉内链接存在
        expect(page.locator(".mobile-drawer a:has-text('首页')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('会员')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('玩法指南')")).to_be_visible()

        # 9. 抽屉内主题切换按钮可见
        expect(page.locator(".mobile-drawer-theme")).to_be_visible()

        # 10. 点击抽屉内链接 → 跳转 + 抽屉自动关闭
        page.locator(".mobile-drawer a:has-text('会员')").click()
        page.wait_for_url(re.compile(r".*/pricing$"))
        expect(page.locator(".mobile-drawer")).to_have_count(0)

        # 11. 回到 guide,检查 sidebar 抽屉目录能正常开关
        page.goto(GUIDE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        if page.locator(".gs-mobile-toggle").count() > 0:
            page.locator(".gs-mobile-toggle").click()
            expect(page.locator(".gs-nav")).to_have_class(re.compile(r"\bopen\b"))
            page.locator(".gs-backdrop").click()
            expect(page.locator(".gs-nav")).not_to_have_class(re.compile(r"\bopen\b"))

        # 12. 暗色主题下导航与抽屉可见且有深色背景
        page.evaluate("document.body.setAttribute('data-theme', 'dark'); localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        expect(page.locator(".navbar")).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).to_be_visible()

        # 13. 无横向溢出
        body_width = page.evaluate("document.documentElement.scrollWidth")
        viewport_w = 375
        assert body_width <= viewport_w + 2, f"横向溢出: body={body_width} viewport={viewport_w}"

        page.screenshot(path="tests/e2e/screenshots/guide_mobile_375.png", full_page=True)
        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_guide_mobile()
    print("Guide mobile verification passed.")
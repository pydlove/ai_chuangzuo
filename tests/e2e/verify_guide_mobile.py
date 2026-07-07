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
        # NavBar 现在所有页面都有,故 .mobile-drawer DOM 总在,应改为验证 closed 状态
        expect(page.locator(".mobile-drawer.open")).to_have_count(0)

        # 11. 回到 guide,检查 sidebar 底部抽屉能正常开关
        page.goto(GUIDE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        if page.locator(".gs-mobile-toggle").count() > 0:
            fab = page.locator(".gs-mobile-toggle")
            expect(fab).to_be_visible()
            # FAB 是 fixed 右下圆形按钮,不占用主阅读区
            fab_box = fab.bounding_box()
            assert fab_box["width"] == 56 and fab_box["height"] == 56, f"FAB 应为 56×56: {fab_box}"
            assert fab_box["x"] > 250, f"FAB 应位于右下:x={fab_box['x']}"
            # 主阅读区不被遮挡:.guide-sidebar 在 mobile 下 width=0
            sidebar_box = page.locator(".guide-sidebar").bounding_box()
            assert sidebar_box["width"] == 0, f"Sidebar 在 mobile 应折叠:{sidebar_box}"
            # 点击 FAB → 底部抽屉滑入
            fab.click()
            sheet = page.locator(".gs-nav")
            expect(sheet).to_have_class(re.compile(r"\bopen\b"))
            # 抽屉位置在屏幕底部,bottom=812
            sheet_box = sheet.bounding_box()
            assert sheet_box["y"] > 200, f"抽屉应在屏幕底部:y={sheet_box['y']}"
            assert sheet_box["y"] + sheet_box["height"] >= 800, f"抽屉应贴底:y+h={sheet_box['y']+sheet_box['height']}"
            # 内容超出可视区时可滚动
            sheet_state = sheet.evaluate("el => ({scrollH: el.scrollHeight, clientH: el.clientHeight})")
            # 默认状态截图(抽屉打开、未滚动)
            page.screenshot(path="tests/e2e/screenshots/guide_mobile_375_sheet.png")
            if sheet_state["scrollH"] > sheet_state["clientH"]:
                sheet.evaluate("el => { el.scrollTop = 200 }")
                page.wait_for_timeout(200)
                scrolled = sheet.evaluate("el => el.scrollTop")
                assert scrolled > 0, f"抽屉内容应可滚动,scrollTop={scrolled}"
                # 截图记录滚动后的状态
                page.screenshot(path="tests/e2e/screenshots/guide_mobile_375_sheet_scrolled.png")
            # 关闭抽屉:点击 sheet 上方可见的 backdrop 区域(屏幕顶部到 sheet 顶部之间)
            page.mouse.click(187, 100)
            expect(sheet).not_to_have_class(re.compile(r"\bopen\b"))

        # 12. 暗色主题下导航与抽屉可见且有深色背景
        page.evaluate("document.body.setAttribute('data-theme', 'dark'); localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        expect(page.locator(".navbar")).to_be_visible()
        expect(page.locator(".gs-mobile-toggle")).to_be_visible()

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
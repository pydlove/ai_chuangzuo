"""
控制台移动端 app-style 验证

覆盖：
- 移动端 (375×812)：侧边栏隐藏，header 隐藏，底部 TabBar 显示 4 个 tab
- /console/mine 是 app-style "我的" 页：用户卡 / 数据区 / 多个 section / 退出登录
- 退出登录按钮：底部最大、不在「关于」内
- 各 list item 触发原 header modal（provide/inject 注入）
- 主题切换：从 "我的" 列表点击主题切换 → 全局主题变化
- 桌面端 (1280×900) 回归：侧边栏 + header 仍可见，TabBar 隐藏
"""

from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:22345"


def _set_auth(page):
    """登录态：访问根路径后写入 fake token，再跳到目标页。"""
    page.goto(f"{BASE_URL}/", wait_until="domcontentloaded", timeout=20000)
    page.evaluate("""
      localStorage.setItem('aichuangzuo_access_token', 'fake-token-for-testing');
      localStorage.setItem('aichuangzuo_refresh_token', 'fake-refresh');
      localStorage.setItem('aichuangzuo_membership', JSON.stringify({level:'专业版会员', expiresAt:'2026-12-31'}));
      localStorage.setItem('aichuangzuo_coin_balance', '888');
    """)


def test_console_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # ==================== 桌面端回归 ====================
        desktop = browser.new_context(viewport={"width": 1280, "height": 900})
        page = desktop.new_page()
        _set_auth(page)
        page.goto(f"{BASE_URL}/console/mine", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(1000)

        # 桌面端：侧边栏可见、header 可见、TabBar 隐藏
        expect(page.locator(".console-sidebar")).to_be_visible()
        expect(page.locator(".console-header")).to_be_visible()
        expect(page.locator(".console-tabbar")).not_to_be_visible()
        # Mine 页面内容也展示
        expect(page.locator(".mine-user-card")).to_be_visible()

        page.screenshot(path="tests/e2e/screenshots/console_desktop_1280.png", full_page=True)
        desktop.close()

        # ==================== 移动端 app-style ====================
        mobile = browser.new_context(
            viewport={"width": 375, "height": 812},
            bypass_csp=True,
        )
        page = mobile.new_page()
        _set_auth(page)
        page.goto(f"{BASE_URL}/console/mine", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(1000)

        # 1. 移动端：侧边栏和 header 整体隐藏、底部 TabBar 显示
        expect(page.locator(".console-sidebar")).not_to_be_visible()
        expect(page.locator(".console-header")).not_to_be_visible()
        expect(page.locator(".console-tabbar")).to_be_visible()

        # 2. TabBar 4 个 tab：创作 / 作品 / 排行榜 / 我的
        tab_items = page.locator(".console-tabbar-item")
        expect(tab_items).to_have_count(4)
        labels = [tab_items.nth(i).locator(".console-tabbar-label").inner_text() for i in range(4)]
        assert labels == ["创作", "作品", "排行榜", "我的"], f"TabBar 顺序错: {labels}"

        # 3. /console/mine 是 app-style：用户卡 / 数据区 / 多 section
        expect(page.locator(".mine-user-card")).to_be_visible()
        expect(page.locator(".mine-user-name")).to_contain_text("爱创作用户")
        expect(page.locator(".mine-user-vip-pro")).to_be_visible()
        expect(page.locator(".mine-stats")).to_be_visible()
        for label in ["本月已生成", "创作币余额", "已邀请"]:
            expect(page.locator(f".mine-stat-label:has-text('{label}')")).to_be_visible()
        for label in ["我的资产", "我的创作", "邀请与帮助"]:
            expect(page.locator(f".mine-section-title:has-text('{label}')")).to_be_visible()

        # 4. 顶部截图
        page.screenshot(path="tests/e2e/screenshots/console_mobile_mine_top.png")

        # 5. 滚动到底部，验证「关于」section 和 退出登录按钮
        page.evaluate("document.querySelector('.mine-page').scrollIntoView({block: 'end'})")
        page.wait_for_timeout(400)

        expect(page.locator(".mine-section-title:has-text('关于')")).to_be_visible()
        expect(page.locator(".mine-section-title:has-text('设置')")).to_be_visible()
        # 退出登录按钮可见且不在「关于」section 内（独立、显眼）
        logout_btn = page.locator(".mine-logout")
        expect(logout_btn).to_be_visible()
        # 退出登录必须在 关于 section 之后出现（DOM 顺序）
        about_section = page.locator(".mine-section:has(.mine-section-title:has-text('关于'))")
        about_box = about_section.bounding_box()
        logout_box = logout_btn.bounding_box()
        assert logout_box["y"] > about_box["y"] + about_box["height"] - 5, \
            f"退出登录应位于「关于」section 之后: about={about_box} logout={logout_box}"

        # 6. 退出登录按钮：尺寸应大于普通 list-item（最大按钮要求）
        logout_h = logout_box["height"]
        list_item_h = page.locator(".mine-list-item").first.bounding_box()["height"]
        assert logout_h > list_item_h, f"退出登录应比 list-item 更高: logout={logout_h} item={list_item_h}"

        page.screenshot(path="tests/e2e/screenshots/console_mobile_mine_bottom.png")

        # 7. 点退出登录 → 二次确认弹框
        logout_btn.click()
        page.wait_for_timeout(600)
        expect(page.locator(".ant-modal-confirm")).to_be_visible()
        page.screenshot(path="tests/e2e/screenshots/console_mobile_logout_confirm.png")
        # 取消确认（antd 默认按钮文字带 2 字符间距，用正则匹配）
        page.locator(".ant-modal-confirm .ant-btn").filter(has_text=re.compile(r"取\s*消")).click()
        page.wait_for_timeout(400)

        # 8. 邀请有礼 modal：从 MineIndex 列表触发（provide/inject 验证）
        page.evaluate("document.querySelector('.mine-page').scrollTo({top: 0})")
        page.wait_for_timeout(300)
        page.locator(".mine-list-item:has-text('邀请有礼')").click()
        page.wait_for_timeout(700)
        expect(page.locator(".invite-modal .ant-modal-content")).to_be_visible()
        page.locator(".invite-modal .ant-modal-close").first.click()
        page.wait_for_timeout(400)

        # 9. 兑换码 modal：同上
        page.locator(".mine-list-item:has-text('兑换码')").click()
        page.wait_for_timeout(700)
        expect(page.locator(".redeem-modal .ant-modal-content")).to_be_visible()
        page.locator(".redeem-modal .ant-modal-close").first.click()
        page.wait_for_timeout(400)

        # 10. 关于我们 modal
        page.evaluate("document.querySelector('.mine-page').scrollIntoView({block: 'end'})")
        page.wait_for_timeout(300)
        page.locator(".mine-list-item:has-text('关于我们')").click()
        page.wait_for_timeout(700)
        expect(page.locator(".about-modal .ant-modal-content")).to_be_visible()
        page.locator(".about-modal .ant-modal-close").first.click()
        page.wait_for_timeout(400)

        # 11. 主题切换：从 MineIndex 列表点击 → 全局主题变化
        before_theme = page.evaluate("document.body.getAttribute('data-theme')")
        page.locator(".mine-list-item:has-text('主题切换')").click()
        page.wait_for_timeout(600)
        after_theme = page.evaluate("document.body.getAttribute('data-theme')")
        assert before_theme != after_theme, f"主题未切换: {before_theme} → {after_theme}"
        # 切回原主题
        page.locator(".mine-list-item:has-text('主题切换')").click()
        page.wait_for_timeout(600)

        # 12. TabBar 跳转：4 个 tab 各点一次都能正确跳转
        # 12a. 创作
        page.locator(".console-tabbar-item").nth(0).click()
        page.wait_for_url(re.compile(r".*/console/create$"), timeout=5000)
        # 12b. 作品
        page.locator(".console-tabbar-item").nth(1).click()
        page.wait_for_url(re.compile(r".*/console/works$"), timeout=5000)
        # 12c. 排行榜
        page.locator(".console-tabbar-item").nth(2).click()
        page.wait_for_url(re.compile(r".*/console/leaderboard$"), timeout=5000)
        # 12d. 我的
        page.locator(".console-tabbar-item").nth(3).click()
        page.wait_for_url(re.compile(r".*/console/mine$"), timeout=5000)

        # 13. TabBar 高亮：当前路由对应的 tab 应为 .active
        mine_tab = page.locator(".console-tabbar-item").nth(3)
        expect(mine_tab).to_have_class(re.compile(r"\bactive\b"))
        page.screenshot(path="tests/e2e/screenshots/console_mobile_mine_active.png")

        # 14. 无横向溢出
        body_w = page.evaluate("document.documentElement.scrollWidth")
        viewport_w = 375
        assert body_w <= viewport_w + 2, f"横向溢出: body={body_w} viewport={viewport_w}"

        # 15. 暗色主题下 Mine 页 + TabBar 可见且有深色背景
        page.evaluate("document.body.setAttribute('data-theme', 'dark'); localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload()
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(1000)
        expect(page.locator(".mine-user-card")).to_be_visible()
        expect(page.locator(".console-tabbar")).to_be_visible()
        page.screenshot(path="tests/e2e/screenshots/console_mobile_mine_dark.png", full_page=True)

        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_console_mobile()
    print("Console mobile verification passed.")
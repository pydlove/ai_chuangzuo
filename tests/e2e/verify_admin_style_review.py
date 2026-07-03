from playwright.sync_api import sync_playwright
import time

BASE_URL = 'http://localhost:22346'


def login_as_admin(page):
    """在登录页注入 token 后跳转控制台"""
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate("""
      window.localStorage.setItem('admin_access_token', JSON.stringify('mock-token'))
    """)


def test_admin_style_review():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        login_as_admin(page)
        page.goto(f'{BASE_URL}/console/styles')
        page.wait_for_selector('.ant-table-row', timeout=10000)

        # 验证侧边栏和菜单
        assert '风格审核' in page.inner_text('.admin-menu')

        # 验证表格加载
        rows = page.query_selector_all('.ant-table-row')
        assert len(rows) >= 5, f'表格至少应有 5 行，实际 {len(rows)}'

        # 验证搜索过滤
        page.fill('input[placeholder="风格名称或创作者"]', '用户001')
        page.click('button:has-text("查 询")')
        page.wait_for_load_state('networkidle')
        rows_after_search = page.query_selector_all('.ant-table-row')
        assert len(rows_after_search) >= 1, '搜索后应至少有 1 条结果'

        # 重置搜索
        page.click('button:has-text("重 置")')
        page.wait_for_load_state('networkidle')

        # 验证打回弹框
        first_pending = page.query_selector('.ant-table-row:first-child button:has-text("打回")')
        assert first_pending, '列表中应至少有一条待审核数据'
        first_pending.click()
        page.wait_for_selector('.ant-modal-content', timeout=5000)
        assert '打回风格' in page.inner_text('.ant-modal-title')

        # 未填原因时确认打回
        page.click('.ant-modal-footer button:last-child')
        page.wait_for_timeout(500)
        assert page.is_visible('.ant-modal-content'), '未填原因时应保持弹框'

        # 填写原因并确认
        page.fill('.ant-modal-content textarea', '风格描述过于宽泛，请补充具体写作要求')
        page.click('.ant-modal-footer button:last-child')
        page.wait_for_selector('.ant-modal-title:has-text("打回风格")', state='hidden', timeout=5000)

        # 验证状态变为已打回
        page.wait_for_timeout(500)
        assert '已打回' in page.inner_text('.ant-table-row:first-child')

        # 验证查看原因
        page.click('.ant-table-row:first-child button:has-text("查看原因")')
        page.wait_for_timeout(500)
        reason_modal = page.locator('.ant-modal-content').last
        assert '打回原因' in reason_modal.inner_text()
        assert '风格描述过于宽泛' in reason_modal.inner_text()
        reason_modal.locator('.ant-modal-close-x').click()
        page.wait_for_timeout(500)
        assert not reason_modal.is_visible()

        page.screenshot(path='tests/e2e/screenshots/admin_style_review.png')
        browser.close()


if __name__ == '__main__':
    test_admin_style_review()

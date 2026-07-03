from playwright.sync_api import sync_playwright

BASE_URL = 'http://localhost:22346'


def login_as_admin(page):
    """在登录页注入 token 后跳转控制台"""
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate("""
      window.localStorage.setItem('admin_access_token', JSON.stringify('mock-token'))
    """)


def test_admin_user_management():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        # 通过登录页写入 token
        login_as_admin(page)
        page.goto(f'{BASE_URL}/console/users')
        page.wait_for_selector('.ant-table-row', timeout=10000)

        # 验证侧边栏和菜单
        brand_text = page.inner_text('.sider-brand-text')
        assert '爱创作' in brand_text
        assert '管理控制台' in brand_text
        assert '用户管理' in page.inner_text('.admin-menu')

        # 验证表格加载
        rows = page.query_selector_all('.ant-table-row')
        assert len(rows) >= 5, f'表格至少应有 5 行，实际 {len(rows)}'

        # 验证搜索过滤
        page.fill('input[placeholder="账号或邮箱"]', 'aichuang_001')
        page.click('button:has-text("查 询")')
        page.wait_for_selector('.ant-table-row', timeout=5000)
        rows_after_search = page.query_selector_all('.ant-table-row')
        assert len(rows_after_search) >= 1, '搜索后应至少有 1 条结果'
        first_account = page.inner_text('.ant-table-row:first-child td:nth-child(2)')
        assert 'aichuang_001' in first_account.lower() or 'user1' in first_account.lower()

        # 重置搜索
        page.click('button:has-text("重 置")')
        page.wait_for_selector('.ant-table-row', timeout=5000)

        # 验证查看详情抽屉
        page.click('.ant-table-row:first-child button:has-text("查看详情")')
        page.wait_for_selector('.ant-drawer-content', timeout=5000)
        assert '用户详情' in page.inner_text('.ant-drawer-title')
        assert '账号' in page.inner_text('.ant-drawer-content')
        page.click('.ant-drawer-footer button:has-text("关 闭")')
        page.wait_for_selector('.ant-drawer-content', state='hidden', timeout=5000)

        # 验证重置密码弹框
        page.click('.ant-table-row:first-child button:has-text("重置密码")')
        page.wait_for_selector('.ant-modal-content', timeout=5000)
        assert '重置用户密码' in page.inner_text('.ant-modal-title')
        page.click('.ant-modal-content button:has-text("取 消")')
        page.wait_for_selector('.ant-modal-content', state='hidden', timeout=5000)

        # 验证启用/禁用 popconfirm
        page.click('.ant-table-row:first-child button:has-text("禁用")')
        page.wait_for_selector('.ant-popover-content', timeout=5000)
        page.click('.ant-popover-content button:has-text("取 消")')
        page.wait_for_selector('.ant-popover-content', state='hidden', timeout=5000)

        page.screenshot(path='tests/e2e/screenshots/admin_user_list.png')
        browser.close()


if __name__ == '__main__':
    test_admin_user_management()

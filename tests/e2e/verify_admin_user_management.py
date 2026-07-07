from playwright.sync_api import sync_playwright
import requests

BASE_URL = 'http://localhost:22346'
API_URL = 'http://localhost:26060'


def get_admin_token():
    resp = requests.post(
        f'{API_URL}/api/v1/admin/auth/login',
        json={'username': 'admin', 'password': 'Root1qaz!QAZ'}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def login_as_admin(page):
    token = get_admin_token()
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate(f"""
      window.localStorage.setItem('admin_access_token', JSON.stringify('{token}'))
      window.localStorage.setItem('admin_refresh_token', JSON.stringify('{token}'))
    """)


def test_admin_user_management():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        login_as_admin(page)
        page.goto(f'{BASE_URL}/console/users')
        page.wait_for_selector('.ant-table-row', timeout=10000)

        # 验证侧边栏和菜单
        brand_text = page.inner_text('.sider-brand-text')
        assert '爱创作' in brand_text
        assert '管理控制台' in brand_text
        assert '用户管理' in page.inner_text('.admin-menu')

        # 验证表格加载出真实用户数据
        rows = page.query_selector_all('.ant-table-row')
        assert len(rows) >= 1, f'表格至少应有 1 行，实际 {len(rows)}'

        # 验证搜索过滤（用真实邮箱前缀）
        page.fill('input[placeholder="账号或邮箱"]', 'pzy')
        page.click('button:has-text("查 询")')
        page.wait_for_selector('.ant-table-row', timeout=5000)
        rows_after_search = page.query_selector_all('.ant-table-row')
        assert len(rows_after_search) >= 1, '搜索后应至少有 1 条结果'

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
        modal_text = page.inner_text('.ant-modal-content')
        assert 'adc123456' in modal_text, f'弹框应显示重置密码 adc123456，实际：{modal_text}'
        page.click('.ant-modal-content button:has-text("取 消")')
        page.wait_for_selector('.ant-modal-content', state='hidden', timeout=5000)

        # 验证启用/禁用 popconfirm
        page.click('.ant-table-row:first-child button:has-text("禁用"), .ant-table-row:first-child button:has-text("启用")')
        page.wait_for_selector('.ant-popover-content', timeout=5000)
        page.click('.ant-popover-content button:has-text("取 消")')
        page.wait_for_selector('.ant-popover-content', state='hidden', timeout=5000)

        page.screenshot(path='tests/e2e/screenshots/admin_user_list.png')
        browser.close()


if __name__ == '__main__':
    test_admin_user_management()

from playwright.sync_api import sync_playwright

errors = []


def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})

        page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)

        # 1. 管理端登录
        page.goto('http://localhost:5174/login')
        page.wait_for_load_state('networkidle')
        page.fill('input[type="text"]', 'admin')
        page.fill('input[type="password"]', 'admin123')
        page.click('button:has-text("登录")')
        page.wait_for_timeout(1500)

        # 2. 进入抽奖活动页
        page.goto('http://localhost:5174/console/lottery')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)
        page.screenshot(path='tests/e2e/screenshots/lottery_admin_page.png', full_page=True)

        # 3. 新建活动弹窗
        page.click('button:has-text("新建活动")')
        page.wait_for_timeout(500)
        page.fill('.ant-modal input:visible', '自动化测试活动')
        page.click('.ant-modal button:has-text("确 定")')
        page.wait_for_timeout(1000)
        page.screenshot(path='tests/e2e/screenshots/lottery_admin_campaign.png', full_page=True)

        print('管理端抽奖页验证完成')
        print(f'Console errors: {len(errors)}')
        for e in errors:
            print(f'  ERROR: {e}')

        browser.close()


if __name__ == '__main__':
    run()

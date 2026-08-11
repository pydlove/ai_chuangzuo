from playwright.sync_api import sync_playwright
import time

errors = []


def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})

        page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)

        # 1. 访问用户端登录页并登录（测试账号按实际项目调整）
        page.goto('http://localhost:5173/login')
        page.wait_for_load_state('networkidle')
        page.fill('input[type="text"]', 'test@example.com')
        page.fill('input[type="password"]', '123456')
        page.click('button:has-text("登录")')
        page.wait_for_timeout(1500)

        # 2. 进入活动中心抽奖页
        page.goto('http://localhost:5173/console/lottery')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)
        page.screenshot(path='tests/e2e/screenshots/lottery_user_page.png', full_page=True)

        # 3. 尝试抽奖按钮（无论是否有次数都截图）
        draw_btn = page.locator('.wheel-center')
        if draw_btn.count() > 0:
            draw_btn.click()
            page.wait_for_timeout(1500)
            page.screenshot(path='tests/e2e/screenshots/lottery_user_draw.png', full_page=True)

        print('用户端抽奖页验证完成')
        print(f'Console errors: {len(errors)}')
        for e in errors:
            print(f'  ERROR: {e}')

        browser.close()


if __name__ == '__main__':
    run()

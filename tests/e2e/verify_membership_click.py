from playwright.sync_api import sync_playwright
import sys

URL = 'http://localhost:28586'

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        # 访问控制台页面
        page.goto(f'{URL}/console/create')
        page.wait_for_selector('.console-membership-badge', timeout=10000)

        # 确保显示年会员
        badge = page.locator('.console-membership-badge')
        text = badge.inner_text()
        print(f'Badge text before click: {text}')
        assert '年会员' in text, f'Expected 年会员 badge, got: {text}'

        # 点击年会员 badge
        badge.click()

        # 验证跳转到 pricing
        page.wait_for_url(f'{URL}/pricing', timeout=10000)
        print(f'URL after click: {page.url}')
        assert page.url == f'{URL}/pricing'

        # 截图
        page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/membership_click_pricing.png')
        print('验证通过：header 年会员点击后跳转到 pricing')

        browser.close()

if __name__ == '__main__':
    main()

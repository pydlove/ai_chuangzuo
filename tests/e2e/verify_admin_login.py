from playwright.sync_api import sync_playwright
import time

BASE_URL = 'http://localhost:22346'


def test_admin_login_page():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=100)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        page.goto(f'{BASE_URL}/login')
        page.wait_for_selector('.login-card', timeout=10000)

        # 验证页面标题与文案
        assert '管理控制台登录' in page.inner_text('.form-title')
        assert '仅限授权管理员访问' in page.inner_text('.form-subtitle')

        # 验证必填校验
        page.click('.submit-btn')
        time.sleep(0.5)
        assert '请输入管理员账号' in page.inner_text('.login-card')
        assert '请输入密码' in page.inner_text('.login-card')
        assert '请输入验证码' in page.inner_text('.login-card')

        # 读取验证码
        captcha = page.inner_text('.captcha-box').strip()

        # 输入错误密码触发失败提示
        page.fill('input[placeholder="请输入管理员账号"]', 'admin')
        page.fill('input[placeholder="请输入密码"]', 'wrong')
        page.fill('input[placeholder="输入验证码"]', captcha)
        page.click('.submit-btn')
        page.wait_for_selector('.ant-alert-error', timeout=10000)
        assert '账号、密码或验证码错误' in page.inner_text('.ant-alert-error')

        # 切换主题
        page.click('.theme-toggle')
        time.sleep(0.3)
        body_theme = page.get_attribute('body', 'data-theme')
        assert body_theme == 'dark'

        page.screenshot(path='tests/e2e/screenshots/admin_login.png')
        browser.close()


if __name__ == '__main__':
    test_admin_login_page()

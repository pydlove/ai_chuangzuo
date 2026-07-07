from playwright.sync_api import sync_playwright
import requests
from pathlib import Path

BASE_URL = 'http://localhost:22346'
API_URL = 'http://localhost:26060'
SCREENSHOT_DIR = Path(__file__).resolve().parent / 'screenshots'


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


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=150)
        page = browser.new_page(viewport={'width': 1440, 'height': 1100})
        login_as_admin(page)

        page.goto(f'{BASE_URL}/console/earnings/accounts')
        page.wait_for_load_state('networkidle')
        page.screenshot(path=str(SCREENSHOT_DIR / 'earnings_accounts.png'), full_page=True)
        print('[OK] earnings accounts page loaded')

        page.goto(f'{BASE_URL}/console/earnings/settlements')
        page.wait_for_load_state('networkidle')
        page.screenshot(path=str(SCREENSHOT_DIR / 'earnings_settlements.png'), full_page=True)
        print('[OK] earnings settlements page loaded')

        browser.close()


if __name__ == '__main__':
    main()

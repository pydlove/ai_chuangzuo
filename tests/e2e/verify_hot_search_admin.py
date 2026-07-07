from playwright.sync_api import sync_playwright
import requests
from pathlib import Path

BASE_URL = 'http://localhost:22346'
API_URL = 'http://localhost:26060'
SCREENSHOT_DIR = Path(__file__).resolve().parent / 'screenshots'


def get_admin_token():
    r = requests.post(f'{API_URL}/api/v1/admin/auth/login',
                      json={'username': 'admin', 'password': 'Root1qaz!QAZ'})
    r.raise_for_status()
    return r.json()['data']['accessToken']


def login_as_admin(page):
    token = get_admin_token()
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate(f"""
      window.localStorage.setItem('admin_access_token', JSON.stringify('{token}'))
      window.localStorage.setItem('admin_refresh_token', JSON.stringify('{token}'))
    """)


def test_hot_search_admin():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})
        login_as_admin(page)

        for path, key in [
            ('/console/hot-search/platforms', 'platforms'),
            ('/console/hot-search/daily', 'daily'),
            ('/console/hot-search/config', 'config'),
        ]:
            page.goto(f'{BASE_URL}{path}')
            page.wait_for_load_state('networkidle')
            page.wait_for_timeout(800)
            page.screenshot(path=str(SCREENSHOT_DIR / f'hot_search_{key}.png'))
        browser.close()


if __name__ == '__main__':
    test_hot_search_admin()

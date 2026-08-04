from playwright.sync_api import sync_playwright, TimeoutError as PWTimeoutError
import requests

BASE_URL = 'http://localhost:22347'
API_URL = 'http://localhost:26060'


def get_token():
    r = requests.post(f'{API_URL}/api/v1/admin/auth/login', json={
        'username': 'admin',
        'password': 'Root1qaz!QAZ'
    })
    r.raise_for_status()
    return r.json()['data']['accessToken']


def test_manual_submission_ui():
    token = get_token()
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1400, 'height': 900})
        page.goto(BASE_URL + '/login')
        page.evaluate("""(token) => {
            localStorage.setItem('admin_access_token', JSON.stringify(token));
        }""", token)
        page.goto(BASE_URL + '/console/commission-tasks')
        page.wait_for_selector('.commission-admin', timeout=15000)
        print('commission page loaded')
        page.get_by_text('详情/采纳').first.click()
        page.wait_for_selector('text=任务详情与稿件采纳', timeout=15000)
        print('drawer opened')
        page.get_by_role('button', name='添加投稿人').click()
        page.locator('.ant-modal-title:has-text("添加投稿人")').wait_for(timeout=15000)
        print('modal opened')
        page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/commission_manual_submission_modal.png')
        print('screenshot saved')
        browser.close()


if __name__ == '__main__':
    try:
        test_manual_submission_ui()
    except PWTimeoutError as e:
        print('Timeout:', e)
        raise

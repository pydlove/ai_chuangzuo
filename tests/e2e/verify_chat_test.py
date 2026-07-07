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
        page.goto(f'{BASE_URL}/console/model-configs')
        page.wait_for_load_state('networkidle')
        page.wait_for_function("document.querySelectorAll('.config-card').length >= 2", timeout=10000)

        # scroll to the chat test section to make sure it's visible
        page.screenshot(path=str(SCREENSHOT_DIR / 'model_config_with_chat_test.png'), full_page=True)

        # check the chat test UI is present
        sections = page.query_selector_all('.ant-divider')
        chat_divider = [s for s in sections if '问答测试' in (s.inner_text() or '')]
        print(f'[DIVIDER] found chat-test dividers: {len(chat_divider)}')

        # find a "发送测试" button and click it on the second card (MiniMax)
        buttons = page.query_selector_all('button:has-text("发送测试")')
        print(f'[BUTTONS] found 发送测试 buttons: {len(buttons)}')

        if buttons:
            # pre-fill baseUrl/apiKey/modelCode on second card so we don't get warning
            cards = page.query_selector_all('.config-card')
            minimax_card = cards[1]
            inputs = minimax_card.query_selector_all('input')
            inputs[0].fill('https://api.moonshot.cn')   # baseUrl (use moonshot, MiniMax DNS may be slow)
            inputs[1].fill('sk-test-bad')              # apiKey
            # modelCode select: skip; handleChatTest should warn
            buttons[1].click()  # second card's 发送测试

            page.wait_for_timeout(2000)
            page.screenshot(path=str(SCREENSHOT_DIR / 'chat_test_warning.png'), full_page=True)

            # now click with valid modelCode (set via JS)
            page.evaluate("""
                (() => {
                    const cards = document.querySelectorAll('.config-card');
                    const card = cards[1];
                    const sel = card.querySelector('.ant-select');
                    // open select
                    sel.click();
                })()
            """)
            page.wait_for_timeout(500)
            # type into select search and press enter
            page.keyboard.type('abab6.5s-chat')
            page.wait_for_timeout(500)
            page.keyboard.press('Enter')
            page.wait_for_timeout(500)

            # click send
            buttons[1].click()
            page.wait_for_timeout(3000)
            page.screenshot(path=str(SCREENSHOT_DIR / 'chat_test_result.png'), full_page=True)

            # dump the result text
            result = page.evaluate("""
                (() => {
                    const cards = document.querySelectorAll('.config-card');
                    const card = cards[1];
                    const pre = card.querySelector('pre.chat-result');
                    return pre ? pre.innerText : '(no result)';
                })()
            """)
            print(f'\n[RESULT]\n{result[:2000]}\n')

        browser.close()


if __name__ == '__main__':
    main()
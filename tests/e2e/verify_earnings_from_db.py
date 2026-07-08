from playwright.sync_api import sync_playwright
import requests
import pymysql
from pathlib import Path
from datetime import datetime, timedelta

BASE_URL = 'http://localhost:22345'
API_URL = 'http://localhost:25050'
SCREENSHOT_DIR = Path(__file__).resolve().parent / 'screenshots'

DB_HOST = 'localhost'
DB_PORT = 3306
DB_USER = 'root'
DB_PASSWORD = '123456'
DB_NAME = 'aichuangzuo'

TEST_EMAIL = 'e2e_earnings@example.com'
TEST_PASSWORD = 'Test1234!'


def db_conn():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )


def setup_user():
    password_hash = '$2b$12$S731baUHuDmz98BnvnBvu.kXKIqfYMtNoxPRg2zJSPKYQ7AONt9TO'
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """
                INSERT INTO u_user (biz_no, email, password_hash, invite_code, user_status, email_verified, coin_balance)
                VALUES (%s, %s, %s, %s, 1, 1, 0)
                ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), user_status = 1, email_verified = 1
                """,
                ('B' + str(hash(TEST_EMAIL) % 1000000), TEST_EMAIL, password_hash, 'I' + str(hash(TEST_EMAIL) % 1000000))
            )
        conn.commit()
        with conn.cursor() as c:
            c.execute('SELECT id FROM u_user WHERE email = %s', (TEST_EMAIL,))
            return c.fetchone()['id']


def clear_earnings(user_id):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute('DELETE FROM u_earnings_record WHERE user_id = %s', (user_id,))
        conn.commit()


def insert_earnings(user_id, type_code, source_type, title, description, amount, status, settlement_month):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """
                INSERT INTO u_earnings_record
                (user_id, type, source_type, source_id, title, description, amount, status, settlement_month, is_deleted)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 0)
                """,
                (user_id, type_code, source_type, None, title, description, amount, status, settlement_month)
            )
        conn.commit()


def get_user_token():
    resp = requests.post(
        f'{API_URL}/api/v1/user/auth/login',
        json={'email': TEST_EMAIL, 'password': TEST_PASSWORD}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def main():
    user_id = setup_user()
    clear_earnings(user_id)

    today = datetime.now()
    current_month = today.strftime('%Y-%m')
    last_month = (today.replace(day=1) - timedelta(days=1)).strftime('%Y-%m')

    # last month unsettled: should be settleable
    insert_earnings(user_id, 'usage', 'test', '使用收益', '其他用户使用「测试风格」生成文章', 12.50, 0, last_month)
    insert_earnings(user_id, 'milestone', 'test', '里程碑奖励', '风格使用达 50 次奖励', 5.00, 0, last_month)
    # current month unsettled: should remain unsettled
    insert_earnings(user_id, 'usage', 'test', '使用收益', '本月其他用户使用「测试风格」生成文章', 8.00, 0, current_month)

    token = get_user_token()

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 1100})

        page.goto(f'{BASE_URL}/login')
        page.wait_for_selector('.login-card', timeout=10000)
        page.evaluate(f"""
            window.localStorage.setItem('aichuangzuo_access_token', '{token}')
            window.localStorage.setItem('aichuangzuo_refresh_token', '{token}')
        """)

        page.goto(f'{BASE_URL}/console/earnings')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1500)
        page.screenshot(path=str(SCREENSHOT_DIR / 'earnings_from_db.png'), full_page=True)
        print('[SCREENSHOT] earnings_from_db.png saved')

        # click settle-last-month button if enabled
        settle_btn = page.query_selector('button:has-text("结算上月")')
        if settle_btn:
            settle_btn.click()
            page.wait_for_timeout(2000)
            page.screenshot(path=str(SCREENSHOT_DIR / 'earnings_after_settle.png'), full_page=True)
            print('[SCREENSHOT] earnings_after_settle.png saved')
        else:
            print('[WARN] settle button not found')

        browser.close()


if __name__ == '__main__':
    main()

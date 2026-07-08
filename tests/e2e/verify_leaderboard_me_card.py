#!/usr/bin/env python3
"""验证排行榜修复:用户未上榜时不应显示「我的奖励状态」卡。

用法:
  # 1. 启动 user-api (localhost:25050) 与 user-web dev server (localhost:22345)
  # 2. python3 tests/e2e/verify_leaderboard_me_card.py
"""
import sys
from playwright.sync_api import sync_playwright
import requests
import pymysql
from pathlib import Path

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

# 用一个全新无收益的账号来验证未上榜场景,避开榜一缓存
UNRANKED_EMAIL = 'e2e_unranked_leaderboard@example.com'
UNRANKED_PASSWORD = 'Test1234!'


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


def setup_unranked_user():
    """创建或重置一个无任何收益记录的新账号。"""
    password_hash = '$2b$12$S731baUHuDmz98BnvnBvu.kXKIqfYMtNoxPRg2zJSPKYQ7AONt9TO'
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """
                INSERT INTO u_user (biz_no, email, password_hash, invite_code, user_status, email_verified, coin_balance)
                VALUES (%s, %s, %s, %s, 1, 1, 0)
                ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), user_status = 1, email_verified = 1
                """,
                ('B' + str(hash(UNRANKED_EMAIL) % 1000000), UNRANKED_EMAIL, password_hash, 'I' + str(hash(UNRANKED_EMAIL) % 1000000))
            )
            c.execute('SELECT id FROM u_user WHERE email = %s', (UNRANKED_EMAIL,))
            uid = c.fetchone()['id']
            c.execute('DELETE FROM u_earnings_record WHERE user_id = %s', (uid,))
        conn.commit()
    return uid


def get_user_token(email=TEST_EMAIL, password=TEST_PASSWORD):
    resp = requests.post(
        f'{API_URL}/api/v1/user/auth/login',
        json={'email': email, 'password': password}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def main():
    setup_unranked_user()
    token = get_user_token(UNRANKED_EMAIL, UNRANKED_PASSWORD)
    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=80)
        page = browser.new_page(viewport={'width': 1440, 'height': 1100})
        page.goto(f'{BASE_URL}/login')
        page.wait_for_selector('.login-card', timeout=10000)
        page.evaluate(f"""
            window.localStorage.setItem('aichuangzuo_access_token', '{token}')
            window.localStorage.setItem('aichuangzuo_refresh_token', '{token}')
        """)

        page.goto(f'{BASE_URL}/console/leaderboard')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1500)

        page.screenshot(path=str(SCREENSHOT_DIR / 'leaderboard_coin_after_fix.png'), full_page=True)
        print('[SCREENSHOT] leaderboard_coin_after_fix.png saved')

        # 1. 我的奖励状态卡不应显示（rank=null 时）
        my_card_visible = page.locator('.my-reward-card').count() > 0
        results.append(('未上榜用户不应显示「我的奖励状态」卡', not my_card_visible))

        # 2. 列表中不应出现 isMe 项（带 ghost 0 元数据）
        me_rows = page.locator('.leaderboard-item.is-me, .leaderboard-top-card.is-me').count()
        results.append(('TOP20 列表中不包含当前用户（无 ghost）', me_rows == 0))

        # 3. 整页不应包含 "TOP null" 字样
        body_text = page.locator('body').inner_text()
        results.append(('页面无 "TOP null" 字样', 'TOP null' not in body_text and '已锁定 TOP null' not in body_text))

        # 4. 整页不应包含「第  名」（双空格）
        results.append(('页面无 "第  名" 字样', '第  名' not in body_text))

        # 5. 切换到收入榜,确认同样不显示
        page.locator('.leaderboard-tab', has_text='自媒体收入榜').click()
        page.wait_for_timeout(800)
        body_text_income = page.locator('body').inner_text()
        results.append(('收入榜无 "TOP null"', 'TOP null' not in body_text_income))
        page.screenshot(path=str(SCREENSHOT_DIR / 'leaderboard_income_after_fix.png'), full_page=True)
        print('[SCREENSHOT] leaderboard_income_after_fix.png saved')

        browser.close()

    print('\n=== 排行榜修复验证 ===')
    all_ok = True
    for name, ok in results:
        status = '✓ PASS' if ok else '✗ FAIL'
        print(f'{status}  {name}')
        if not ok:
            all_ok = False
    print()
    return 0 if all_ok else 1


if __name__ == '__main__':
    sys.exit(main())
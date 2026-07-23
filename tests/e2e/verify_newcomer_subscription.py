#!/usr/bin/env python3
"""新人首冲优惠真实后端验证：无邀请用户订阅旗舰版年包，金额按 DB 定价再打 8 折。"""
import sys
from datetime import datetime
from pathlib import Path

import bcrypt
import pymysql
import requests
from playwright.sync_api import sync_playwright

BASE_URL = 'http://localhost:22345'
API_URL = 'http://localhost:25050'
SCREENSHOT_DIR = Path(__file__).resolve().parent / 'screenshots'

DB_HOST = 'localhost'
DB_PORT = 3306
DB_USER = 'root'
DB_PASSWORD = '123456'
DB_NAME = 'aichuangzuo'


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


def _hash_password(password):
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(rounds=12)).decode('utf-8')


def insert_standalone_user(email, password):
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S%f')
    biz_no = f'NEW{timestamp}'
    invite_code = f'N{timestamp[-8:]}'
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """INSERT INTO u_user
                   (biz_no, email, password_hash, invite_code, user_status, user_type,
                    email_verified, tenant_id, is_deleted, created_by, updated_by)
                   VALUES (%s, %s, %s, %s, 1, 1, 1, 0, 0, 0, 0)""",
                (biz_no, email, _hash_password(password), invite_code)
            )
            user_id = c.lastrowid
        conn.commit()
        return user_id


def get_token(email, password):
    resp = requests.post(
        f'{API_URL}/api/v1/user/auth/login',
        json={'email': email, 'password': password}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def main():
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
    email = f'newcomer_{timestamp}@example.com'
    password = 'Test1234!'
    user_id = insert_standalone_user(email, password)
    token = get_token(email, password)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=80)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})
        page.goto(f'{BASE_URL}/login')
        page.wait_for_selector('.login-card', timeout=10000)
        page.evaluate(
            f"""
            window.localStorage.setItem('aichuangzuo_access_token', '{token}');
            window.localStorage.setItem('aichuangzuo_refresh_token', '{token}');
            """
        )

        page.goto(f'{BASE_URL}/pricing?newcomer=1')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)

        card = page.locator('.newcomer-offer-card')
        assert card.is_visible(), '新人优惠卡片未显示'
        card_text = card.inner_text()
        assert '671.36' in card_text, '卡片未显示 8 折后价格'
        assert '1198.8' in card_text, '卡片未显示划线原价'
        assert '839.2' in card_text, '卡片未显示年付折扣价'

        page.screenshot(path=str(SCREENSHOT_DIR / 'newcomer_real_card.png'), full_page=True)

        card.locator('.newcomer-offer-btn').click()
        page.wait_for_selector('.subscribe-modal', timeout=5000)
        page.locator('.subscribe-modal input').fill('123456')
        page.locator('.subscribe-modal .ant-btn-primary').click()
        page.wait_for_url('**/console/create', timeout=10000)
        page.wait_for_timeout(1000)
        page.screenshot(path=str(SCREENSHOT_DIR / 'newcomer_real_subscribed.png'), full_page=True)
        browser.close()

    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                'SELECT level, expires_at FROM u_user_membership WHERE user_id = %s',
                (user_id,)
            )
            membership = c.fetchone()
            assert membership is not None, '未写入会员记录'
            assert membership['level'] == 'flagship', f'会员等级期望 flagship，实际 {membership["level"]}'

            c.execute(
                'SELECT amount FROM u_order WHERE user_id = %s ORDER BY id DESC LIMIT 1',
                (user_id,)
            )
            order = c.fetchone()
            assert order is not None, '未写入订单记录'
            assert float(order['amount']) == 671.36, f'订单金额期望 671.36，实际 {order["amount"]}'

    print('PASS 新人优惠卡片展示')
    print('PASS 旗舰版年包会员写入')
    print('PASS 订单金额为 671.36')
    print('ALL PASS')
    return 0


if __name__ == '__main__':
    sys.exit(main())

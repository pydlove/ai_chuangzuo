#!/usr/bin/env python3
"""验证用户端立即订阅（测试支付）流程。

准备数据（DB 直连）:
  - 注册测试用户 A（有邀请人 B）
  - 注册测试用户 C（无邀请人）

验证流程:
  - 用户 C: Pricing 页选择专业版/年度 → 弹框输入 123456 → 订阅成功 → 跳转 /console/create
  - 用户 A: 同上，订阅成功后邀请人 B 获得创作币奖励并收到消息通知

用法:
  python3 tests/e2e/verify_subscription.py
"""
import sys
from datetime import datetime, timedelta
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


def _insert_user(conn, email, password):
    """直接写入 u_user，绕过邮箱验证码注册流程。"""
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S%f')
    biz_no = f'SUB{timestamp}'
    invite_code = f'S{timestamp[-8:]}'
    with conn.cursor() as c:
        c.execute(
            """INSERT INTO u_user
               (biz_no, email, password_hash, invite_code, user_status, user_type,
                email_verified, tenant_id, is_deleted, created_by, updated_by)
               VALUES (%s, %s, %s, %s, 1, 1, 1, 0, 0, 0, 0)""",
            (biz_no, email, _hash_password(password), invite_code)
        )
    return conn.insert_id()


def setup_test_users():
    """准备测试用户：inviter、invitee、standalone。"""
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
    inviter_email = f'sub_inviter_{timestamp}@example.com'
    invitee_email = f'sub_invitee_{timestamp}@example.com'
    standalone_email = f'sub_standalone_{timestamp}@example.com'
    password = 'Test1234!'

    with db_conn() as conn:
        inviter_id = _insert_user(conn, inviter_email, password)
        invitee_id = _insert_user(conn, invitee_email, password)
        standalone_id = _insert_user(conn, standalone_email, password)

        with conn.cursor() as c:
            c.execute('SELECT invite_code FROM u_user WHERE id = %s', (inviter_id,))
            inviter_code = c.fetchone()['invite_code']
            c.execute(
                """INSERT INTO u_user_invite_relation
                   (inviter_id, invitee_id, invite_code, source_type, effective_status)
                   VALUES (%s, %s, %s, 2, 1)""",
                (inviter_id, invitee_id, inviter_code)
            )
        conn.commit()

    return {
        'inviter': {'email': inviter_email, 'password': password, 'id': inviter_id},
        'invitee': {'email': invitee_email, 'password': password, 'id': invitee_id},
        'standalone': {'email': standalone_email, 'password': password, 'id': standalone_id}
    }


def get_token(email, password):
    resp = requests.post(
        f'{API_URL}/api/v1/user/auth/login',
        json={'email': email, 'password': password}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def run_browser_subscription(email, password, label, screenshot_name):
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

        page.goto(f'{BASE_URL}/pricing')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)

        # 切换到年度
        page.locator('.toggle-btn', has_text='年度').click()
        page.wait_for_timeout(500)

        # 点击专业版「立即订阅」
        pro_card = page.locator('.pricing-card', has_text='专业版').first
        pro_card.locator('button:has-text("立即订阅")').click()
        page.wait_for_selector('.subscribe-modal', timeout=5000)
        page.wait_for_timeout(500)

        page.locator('.subscribe-modal input').fill('123456')
        page.locator('.subscribe-modal .ant-btn-primary').click()

        page.wait_for_url('**/console/create', timeout=10000)
        page.wait_for_timeout(1000)

        page.screenshot(path=str(SCREENSHOT_DIR / screenshot_name), full_page=True)
        print(f'[SCREENSHOT] {screenshot_name} saved')

        result = page.url.endswith('/console/create')
        browser.close()
        return result


def assert_membership(user_id, expected_level, min_days):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                'SELECT level, expires_at FROM u_user_membership WHERE user_id = %s',
                (user_id,)
            )
            row = c.fetchone()
            assert row is not None, f'user_id={user_id} 无会员记录'
            assert row['level'] == expected_level, f'期望等级 {expected_level}，实际 {row["level"]}'
            expiry = row['expires_at']
            assert expiry >= datetime.now().date() + timedelta(days=min_days - 1), f'到期日 {expiry} 不足 {min_days} 天'


def assert_inviter_reward(inviter_id, invitee_id):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """SELECT COUNT(*) AS cnt FROM u_user_coin_record
                   WHERE user_id = %s AND biz_type = 'invite_reward' AND ref_id IN
                   (SELECT id FROM u_order WHERE user_id = %s)""",
                (inviter_id, invitee_id)
            )
            row = c.fetchone()
            assert row['cnt'] == 1, f'邀请人奖励记录应为 1，实际 {row["cnt"]}'

            c.execute(
                """SELECT COUNT(*) AS cnt FROM u_message
                   WHERE msg_type = 'reward' AND target_user_id = %s""",
                (inviter_id,)
            )
            row = c.fetchone()
            assert row['cnt'] >= 1, '邀请人未收到奖励到账通知'


def main():
    users = setup_test_users()
    results = []

    results.append((
        '无邀请人用户订阅成功并跳转创作页',
        run_browser_subscription(
            users['standalone']['email'],
            users['standalone']['password'],
            'standalone',
            'subscription_success.png'
        )
    ))
    assert_membership(users['standalone']['id'], 'pro', 365)

    results.append((
        '有邀请人用户订阅成功并给邀请人发放奖励',
        run_browser_subscription(
            users['invitee']['email'],
            users['invitee']['password'],
            'invitee',
            'subscription_inviter_reward.png'
        )
    ))
    assert_membership(users['invitee']['id'], 'pro', 365)
    assert_inviter_reward(users['inviter']['id'], users['invitee']['id'])

    print('\n=== 立即订阅 E2E 验证 ===')
    all_ok = True
    for name, ok in results:
        status = 'PASS' if ok else 'FAIL'
        print(f'{status}  {name}')
        if not ok:
            all_ok = False
    print()
    return 0 if all_ok else 1


if __name__ == '__main__':
    sys.exit(main())

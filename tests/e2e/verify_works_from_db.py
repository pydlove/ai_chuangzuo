#!/usr/bin/env python3
"""用户端「我的作品」前后端数据库化 E2E 验证。

用法：
  # 1. 启动 user-api (localhost:25050) 与 user-web dev server (localhost:22345)
  # 2. python3 tests/e2e/verify_works_from_db.py
"""
import sys
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


def clear_works(user_id):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute('DELETE FROM u_article WHERE user_id = %s', (user_id,))
            c.execute('DELETE FROM u_draft WHERE user_id = %s', (user_id,))
        conn.commit()


def insert_article(user_id, biz_no, title, body, platform, style, template, word_count, completed_at):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """
                INSERT INTO u_article
                (biz_no, user_id, title, body, style_overrides, platform, style, template, word_count, completed_at, is_deleted)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 0)
                """,
                (biz_no, user_id, title, body, '{"blocks": {}, "inlines": []}', platform, style, template, word_count, completed_at)
            )
        conn.commit()


def insert_draft(user_id, biz_no, custom_title, custom_requirement, platform, style, template, word_count, saved_at):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """
                INSERT INTO u_draft
                (biz_no, user_id, custom_title, custom_requirement, platform, style, template, word_count, saved_at, is_deleted)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 0)
                """,
                (biz_no, user_id, custom_title, custom_requirement, platform, style, template, word_count, saved_at)
            )
        conn.commit()


def get_user_token():
    resp = requests.post(
        f'{API_URL}/api/v1/user/auth/login',
        json={'email': TEST_EMAIL, 'password': TEST_PASSWORD}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def login(page, token):
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate(f"""
        window.localStorage.setItem('aichuangzuo_access_token', '{token}')
        window.localStorage.setItem('aichuangzuo_refresh_token', '{token}')
    """)


def count_cards(page):
    return page.locator('.work-card').count()


def switch_tab(page, name):
    # 用 index 避免 sidebar 等同名文字干扰
    tabs = page.locator('.works-tab').all()
    target = None
    for tab in tabs:
        if tab.inner_text().strip() == name:
            target = tab
            break
    if not target:
        raise RuntimeError(f'未找到 tab: {name}')
    target.click(force=True)
    page.wait_for_timeout(1000)
    active_text = page.locator('.works-tab.active').inner_text().strip()
    if active_text != name:
        raise RuntimeError(f'切换 tab 失败，当前 active: {active_text}')


def main():
    user_id = setup_user()
    clear_works(user_id)

    now = datetime.now()
    week_ago = now - timedelta(days=5)
    month_ago = now - timedelta(days=30)

    insert_article(user_id, 'A000000000000001', '小红书夏日穿搭指南', 'body1', '小红书', '产品评测', 'card-01', 800, week_ago.strftime('%Y-%m-%d %H:%M:%S'))
    insert_article(user_id, 'A000000000000002', '职场效率提升方法论', 'body2', '微信公众号', '职场干货', 'wechat', 1500, month_ago.strftime('%Y-%m-%d %H:%M:%S'))

    insert_draft(user_id, 'D000000000000001', '未完成的种草文案', ' requirement1', '小红书', '营销文案', 'xiaohongshu-list', 500, week_ago.strftime('%Y-%m-%d %H:%M:%S'))
    insert_draft(user_id, 'D000000000000002', '旧选题备忘', 'requirement2', '知乎', '知识科普', 'zhihu-answer', 1200, month_ago.strftime('%Y-%m-%d %H:%M:%S'))

    token = get_user_token()

    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=80)
        page = browser.new_page(viewport={'width': 1440, 'height': 1100})
        login(page, token)

        page.goto(f'{BASE_URL}/console/works')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1200)

        # 1. 已生成 tab 应有 2 条
        results.append(('已生成 tab 显示 2 条作品', count_cards(page) == 2))
        page.screenshot(path=str(SCREENSHOT_DIR / 'works_from_db.png'), full_page=True)
        print('[SCREENSHOT] works_from_db.png saved')

        # 2. 筛选「小红书」后应只剩 1 条
        page.locator('.works-filter-select').first.click()
        page.locator('.ant-select-item', has_text='小红书').click()
        page.wait_for_timeout(600)
        results.append(('平台筛选「小红书」生效', count_cards(page) == 1))
        page.locator('.ant-select-clear').first.click()
        page.wait_for_timeout(400)

        # 3. 筛选「近 7 天」后应只剩 1 条（因为 month_ago 的作品被过滤）
        page.locator('.works-filter-time .ant-radio-button-wrapper', has_text='近7天').click()
        page.wait_for_timeout(600)
        results.append(('时间筛选「近 7 天」生效', count_cards(page) == 1))

        # 清空筛选
        page.locator('.works-filter-time .ant-radio-button-wrapper', has_text='全部').click()
        page.wait_for_timeout(400)

        # 4. 删除第一条作品
        initial_count = count_cards(page)
        page.locator('button.work-action-btn.danger').first.click()
        page.wait_for_timeout(1000)
        results.append(('删除作品后列表减少 1 条', count_cards(page) == initial_count - 1))

        # 刷新页面后应仍只有 1 条
        page.reload()
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1200)
        results.append(('刷新后已生成仍剩 1 条', count_cards(page) == 1))

        # 5. 切换到草稿箱
        switch_tab(page, '草稿箱')
        results.append(('草稿箱 tab 显示 2 条草稿', count_cards(page) == 2))
        page.screenshot(path=str(SCREENSHOT_DIR / 'works_drafts_from_db.png'), full_page=True)
        print('[SCREENSHOT] works_drafts_from_db.png saved')

        # 6. 直接调 API 验证 openArticle 能加载内容
        article_resp = requests.get(
            f'{API_URL}/api/v1/user/articles/A000000000000002',
            headers={'Authorization': f'Bearer {token}'}
        )
        results.append(('GET /articles/{bizNo} 返回 200', article_resp.status_code == 200))
        data = article_resp.json().get('data', {})
        results.append(('作品详情包含标题与正文', data.get('title') == '职场效率提升方法论' and 'body2' in data.get('body', '')))

        browser.close()

    print('\n=== 我的作品数据库化验证结果 ===')
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
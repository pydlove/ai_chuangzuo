#!/usr/bin/env python3
"""验证消息中心:详情弹框 + 会员到期续订弹框。

准备数据(在 DB 直接操作):
  - u_message id=1 (announcement broadcast):content 加长到 > 1500 字符(验证滚动)
  - u_message id=10 (membership.expiring broadcast):已有 763 字符,用于续订弹框
  - 新增 1 条 membership.subscribed personal(scope=2, target_user_id=1299)

验证流程:
  - 浏览器打开 /console/create,点铃铛 → 公告 tab → 点长公告 → 详情弹框 + 可滚动 + 关
  - 会员提醒 tab → 点 expiring → 续订弹框 + 「取消」URL 不变 + 重开 + 「去续订」→ /pricing
  - 会员提醒 tab → 点 subscribed → 详情弹框(不是续订弹框)

用法:
  python3 tests/e2e/verify_message_modal.py
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

TEST_EMAIL = 'e2e_unranked_leaderboard@example.com'
TEST_PASSWORD = 'Test1234!'
TEST_USER_ID = 1299

# 验证用:把 id=1 announcement 的 content 加长到 > 1500 字符,验证滚动
LONG_ANNOUNCEMENT_CONTENT = (
    '尊敬的用户:\n\n'
    '欢迎使用爱创作,我们将于本周发布全新功能。\n\n'
) + ('爱创作致力于帮助每一位创作者把脑海里的灵感变成优质文章。\n' * 60)


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


def setup_messages():
    """准备测试数据:id=1 加长 content;新增 membership.subscribed personal。"""
    with db_conn() as conn:
        with conn.cursor() as c:
            # 1. id=1 内容加长,验证详情弹框滚动
            c.execute(
                'UPDATE u_message SET content = %s WHERE id = 1',
                (LONG_ANNOUNCEMENT_CONTENT,)
            )
            # 2. 新增 membership.subscribed personal 消息
            # 先清掉旧的(避免重复测试数据累积)
            c.execute(
                "DELETE FROM u_message WHERE msg_type = 'membership' AND sub_type = 'subscribed' AND target_user_id = %s",
                (TEST_USER_ID,)
            )
            c.execute(
                """
                INSERT INTO u_message
                    (msg_type, scope, sub_type, target_user_id, title, summary, content, tenant_id)
                VALUES (%s, 2, 'subscribed', %s, %s, %s, %s, 0)
                """,
                (
                    'membership',
                    TEST_USER_ID,
                    '订阅成功通知',
                    '您已成功开通专业版会员',
                    '亲爱的用户:\n\n您的专业版会员已成功开通,有效期 365 天。\n\n'
                    '【会员权益】\n1. 无限次 AI 内容生成\n2. 多平台爆款标题优化\n3. 高级模板库全部解锁\n4. 创作币每月自动到账 100 枚\n\n'
                    '感谢您对爱创作的支持!'
                )
            )
        conn.commit()


def get_user_token(email=TEST_EMAIL, password=TEST_PASSWORD):
    resp = requests.post(
        f'{API_URL}/api/v1/user/auth/login',
        json={'email': email, 'password': password}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def main():
    setup_messages()
    token = get_user_token()
    results = []

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

        page.goto(f'{BASE_URL}/console/create')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1500)

        # 点铃铛
        page.locator('.bell-btn').click()
        page.wait_for_selector('.notif-panel', timeout=5000)
        page.wait_for_timeout(500)

        # ========== Test 1: announcement 详情弹框 ==========
        page.locator('.notif-tab', has_text='公告').click()
        page.wait_for_timeout(500)

        # 找到 id=1 那条(id=1 title=系统维护通知),点击
        ann_item = page.locator('.notif-item', has_text='系统维护通知').first
        ann_item.click()
        page.wait_for_selector('.notif-detail-modal', timeout=5000)
        page.wait_for_timeout(800)

        # 1a. 详情弹框可见
        detail_visible = page.locator('.notif-detail-panel').is_visible()
        results.append(('点公告 → 详情弹框可见', detail_visible))

        # 1b. content 文案完整(> 1500 字符)
        content_text = page.locator('.notif-detail-content').inner_text()
        results.append(('详情 content 包含加长文案', len(content_text) > 1500))

        # 1c. content 可滚动(.ant-modal-body scrollHeight > clientHeight)
        scroll_info = page.evaluate(
            """() => {
                const modal = document.querySelector('.notif-detail-modal .ant-modal-body');
                if (!modal) return null;
                return { scrollHeight: modal.scrollHeight, clientHeight: modal.clientHeight };
            }"""
        )
        is_scrollable = scroll_info and scroll_info['scrollHeight'] > scroll_info['clientHeight']
        results.append(
            (f'详情弹框内容可滚动 ({scroll_info})', bool(is_scrollable))
        )

        page.screenshot(
            path=str(SCREENSHOT_DIR / 'message_detail_modal.png'),
            full_page=True
        )
        print('[SCREENSHOT] message_detail_modal.png saved')

        # 1d. 「我知道了」关弹框
        page.locator('.notif-detail-modal button:has-text("我知道了")').click()
        page.wait_for_timeout(500)
        detail_closed = page.locator('.notif-detail-modal:visible').count() == 0
        results.append(('「我知道了」可关闭详情弹框', detail_closed))

        # ========== Test 2: membership.expiring 续订弹框 ==========
        page.locator('.bell-btn').click()
        page.wait_for_selector('.notif-panel', timeout=5000)
        page.locator('.notif-tab', has_text='会员提醒').click()
        page.wait_for_timeout(500)

        # 点 expiring 那条
        exp_item = page.locator('.notif-item', has_text='您的会员即将到期').first
        exp_item.click()
        page.wait_for_selector('.renewal-modal', timeout=5000)
        page.wait_for_timeout(800)

        renewal_visible = page.locator('.renewal-panel').is_visible()
        results.append(('点 expiring → 续订弹框可见', renewal_visible))

        # 文案包含 content(>700 字符)
        renewal_body = page.locator('.renewal-body').inner_text()
        results.append(('续订弹框 content 文案非空', len(renewal_body) > 100))

        page.screenshot(
            path=str(SCREENSHOT_DIR / 'message_renewal_modal.png'),
            full_page=True
        )
        print('[SCREENSHOT] message_renewal_modal.png saved')

        # 2a. 「取消」不跳转
        page.locator('.renewal-modal button:has-text("取 消")').click()
        page.wait_for_timeout(500)
        renewal_closed = page.locator('.renewal-modal:visible').count() == 0
        still_at_console = '/pricing' not in page.url
        results.append(
            (f'「取消」关闭弹框且不跳 /pricing (URL={page.url})', renewal_closed and still_at_console)
        )

        # 2b. 重开 → 「去续订」跳 /pricing
        page.locator('.bell-btn').click()
        page.wait_for_selector('.notif-panel', timeout=5000)
        page.locator('.notif-tab', has_text='会员提醒').click()
        page.wait_for_timeout(500)
        page.locator('.notif-item', has_text='您的会员即将到期').first.click()
        page.wait_for_selector('.renewal-modal', timeout=5000)
        page.wait_for_timeout(500)

        page.locator('.renewal-modal button:has-text("去续订")').click()
        page.wait_for_url('**/pricing', timeout=5000)
        results.append(('「去续订」跳 /pricing', '/pricing' in page.url))

        # ========== Test 3: membership.subscribed → 走详情弹框,不是续订弹框 ==========
        page.goto(f'{BASE_URL}/console/create')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)
        page.locator('.bell-btn').click()
        page.wait_for_selector('.notif-panel', timeout=5000)
        page.locator('.notif-tab', has_text='会员提醒').click()
        page.wait_for_timeout(500)

        sub_item = page.locator('.notif-item', has_text='订阅成功通知').first
        sub_item.click()
        page.wait_for_timeout(800)

        detail_open = page.locator('.notif-detail-modal:visible').count() > 0
        renewal_not_open = page.locator('.renewal-modal:visible').count() == 0
        results.append(
            ('点 subscribed → 走详情弹框(非续订弹框)',
             detail_open and renewal_not_open)
        )

        # ========== Test 4: 暗色主题下弹框与页面背景区分清晰 ==========
        page.goto(f'{BASE_URL}/console/create')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)
        page.evaluate("""
            document.body.setAttribute('data-theme', 'dark');
            window.localStorage.setItem('aichuangzuo_theme', 'dark');
        """)
        page.wait_for_timeout(800)
        page.locator('.bell-btn').click()
        page.wait_for_selector('.notif-panel', timeout=5000)
        page.locator('.notif-tab', has_text='公告').click()
        page.wait_for_timeout(500)
        page.locator('.notif-item', has_text='系统维护通知').first.click()
        page.wait_for_selector('.notif-detail-modal', timeout=5000)
        page.wait_for_timeout(800)
        page.screenshot(
            path=str(SCREENSHOT_DIR / 'message_detail_modal_dark.png'),
            full_page=True
        )
        print('[SCREENSHOT] message_detail_modal_dark.png saved')
        # 暗色下内容区仍可见(取 computed color,不是 white-on-white)
        detail_content_color = page.evaluate(
            """() => {
                const el = document.querySelector('.notif-detail-content');
                if (!el) return null;
                return getComputedStyle(el).color;
            }"""
        )
        # rgb(217, 217, 217) = #d9d9d9
        results.append(
            ('暗色主题:详情 content 颜色可读', detail_content_color == 'rgb(217, 217, 217)')
        )
        page.locator('.notif-detail-modal button:has-text("我")').click()
        page.wait_for_timeout(500)

        page.locator('.bell-btn').click()
        page.wait_for_selector('.notif-panel', timeout=5000)
        page.locator('.notif-tab', has_text='会员提醒').click()
        page.wait_for_timeout(500)
        page.locator('.notif-item', has_text='您的会员即将到期').first.click()
        page.wait_for_selector('.renewal-modal', timeout=5000)
        page.wait_for_timeout(800)
        page.screenshot(
            path=str(SCREENSHOT_DIR / 'message_renewal_modal_dark.png'),
            full_page=True
        )
        print('[SCREENSHOT] message_renewal_modal_dark.png saved')
        renewal_body_color = page.evaluate(
            """() => {
                const el = document.querySelector('.renewal-body');
                if (!el) return null;
                return {
                    color: getComputedStyle(el).color,
                    background: getComputedStyle(el).backgroundColor
                };
            }"""
        )
        # #262626 = rgb(38, 38, 38), text #d9d9d9
        results.append(
            ('暗色主题:renewal-body 背景和内容颜色可读',
             renewal_body_color == {'color': 'rgb(217, 217, 217)', 'background': 'rgb(38, 38, 38)'})
        )

        # 恢复浅色主题,避免影响其他测试
        page.evaluate("""
            document.body.setAttribute('data-theme', 'light');
            window.localStorage.setItem('aichuangzuo_theme', 'light');
        """)

        browser.close()

    print('\n=== 消息弹框验证 ===')
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
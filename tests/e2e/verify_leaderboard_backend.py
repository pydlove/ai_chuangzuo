#!/usr/bin/env python3
"""
收益排行榜前后端跨端联调 E2E 脚本。

流程：
1. 在 MySQL 创建测试用户（BCrypt 密码）。
2. 用户登录，提交自媒体收入申报（上传截图）。
3. 管理端登录，审核通过申报。
4. 用户端查看 income 榜，确认金额生效。
5. 管理端对 income 榜发放奖励。
6. 用户端查看 coin 榜，确认 +100 创作币。
7. Playwright 打开用户端榜单页，截图并校验关键文案。

运行前请确保：
- MySQL 127.0.0.1:3307 / aichuangzuo / root / 空密码
- user-api  http://localhost:25050
- admin-api http://localhost:26060
- user-web  http://localhost:22345
"""

import json
import os
import sys
import uuid
import tempfile
from datetime import datetime

import bcrypt
import pymysql
import requests
from PIL import Image
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeout

MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = 3307
MYSQL_USER = "root"
MYSQL_PASSWORD = ""
MYSQL_DB = "aichuangzuo"

USER_API = "http://localhost:25050"
ADMIN_API = "http://localhost:26060"
USER_WEB = "http://localhost:22345"

TEST_EMAIL = "leaderboard_e2e@example.com"
TEST_PASSWORD = "Test1234!"
TEST_NICKNAME = "E2E Tester"
TEST_AMOUNT = "1234.56"
TEST_PLATFORM = "douyin"
TEST_PERIOD = datetime.now().strftime("%Y-%m")

SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


def db_conn():
    return pymysql.connect(
        host=MYSQL_HOST,
        port=MYSQL_PORT,
        user=MYSQL_USER,
        password=MYSQL_PASSWORD,
        database=MYSQL_DB,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def cleanup_test_data(conn):
    """删除本脚本创建的测试用户及相关业务数据，保证可重复运行。"""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM u_user WHERE email = %s", (TEST_EMAIL,))
        row = cur.fetchone()
        if row:
            user_id = row["id"]
            cur.execute("DELETE FROM u_leaderboard_income_submission WHERE user_id = %s", (user_id,))
            cur.execute("DELETE FROM u_user_coin_record WHERE user_id = %s", (user_id,))
            cur.execute("DELETE FROM u_leaderboard_reward_record WHERE user_id = %s", (user_id,))
            cur.execute("DELETE FROM u_user WHERE id = %s", (user_id,))
        conn.commit()


def create_test_user(conn):
    """插入一个可直接登录的测试用户。"""
    password_hash = bcrypt.hashpw(TEST_PASSWORD.encode("utf-8"), bcrypt.gensalt(rounds=12)).decode("utf-8")
    biz_no = "U" + uuid.uuid4().hex[:16].upper()
    invite_code = uuid.uuid4().hex[:6].upper()
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO u_user (
                biz_no, nickname, email, password_hash, invite_code,
                user_status, email_verified, tenant_id, is_deleted, created_by, updated_by
            ) VALUES (%s, %s, %s, %s, %s, 1, 1, 0, 0, 0, 0)
            """,
            (biz_no, TEST_NICKNAME, TEST_EMAIL, password_hash, invite_code),
        )
        user_id = cur.lastrowid
    conn.commit()
    return user_id


def login_user():
    r = requests.post(
        f"{USER_API}/api/v1/user/auth/login",
        json={"email": TEST_EMAIL, "password": TEST_PASSWORD},
        timeout=10,
    )
    assert r.status_code == 200, f"user login http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"user login business error: {payload}"
    return payload["data"]["accessToken"]


def login_admin():
    r = requests.post(
        f"{ADMIN_API}/api/v1/admin/auth/login",
        json={"username": "admin", "password": "Root1qaz!QAZ"},
        timeout=10,
    )
    assert r.status_code == 200, f"admin login http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"admin login business error: {payload}"
    return payload["data"]["accessToken"]


def make_test_image():
    """生成一张临时 PNG 截图用于上传。"""
    f = tempfile.NamedTemporaryFile(suffix=".png", delete=False)
    img = Image.new("RGB", (200, 200), color=(255, 100, 100))
    img.save(f.name, format="PNG")
    return f.name


def submit_income(token, image_path):
    with open(image_path, "rb") as f:
        files = {"screenshots": ("income.png", f, "image/png")}
        data = {"periodMonth": TEST_PERIOD, "amount": TEST_AMOUNT, "platform": TEST_PLATFORM}
        r = requests.post(
            f"{USER_API}/api/v1/user/leaderboards/income-submissions",
            data=data,
            files=files,
            headers={"Authorization": f"Bearer {token}"},
            timeout=20,
        )
    assert r.status_code == 200, f"submit income http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"submit income business error: {payload}"
    return payload["data"]["bizNo"]


def find_pending_submission(admin_token):
    r = requests.get(
        f"{ADMIN_API}/api/v1/admin/leaderboards/income-submissions",
        params={"status": 0, "page": 1, "size": 20},
        headers={"Authorization": f"Bearer {admin_token}"},
        timeout=10,
    )
    assert r.status_code == 200, f"list submissions http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"list submissions business error: {payload}"
    for rec in payload["data"]["records"]:
        if rec.get("platform") == TEST_PLATFORM and rec.get("periodMonth") == TEST_PERIOD:
            return rec["id"]
    raise AssertionError("未找到刚提交的待审核收入申报")


def approve_submission(admin_token, submission_id):
    r = requests.post(
        f"{ADMIN_API}/api/v1/admin/leaderboards/income-submissions/{submission_id}/approve",
        headers={"Authorization": f"Bearer {admin_token}"},
        timeout=10,
    )
    assert r.status_code == 200, f"approve http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"approve business error: {payload}"


def get_income_leaderboard(token):
    r = requests.get(
        f"{USER_API}/api/v1/user/leaderboards/income",
        params={"periodType": "month", "periodValue": TEST_PERIOD},
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    assert r.status_code == 200, f"income leaderboard http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"income leaderboard business error: {payload}"
    return payload["data"]


def grant_rewards(admin_token):
    r = requests.post(
        f"{ADMIN_API}/api/v1/admin/leaderboards/rewards/actions/grant",
        json={"leaderboardType": 2, "periodMonth": TEST_PERIOD},
        headers={"Authorization": f"Bearer {admin_token}"},
        timeout=10,
    )
    assert r.status_code == 200, f"grant http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"grant business error: {payload}"
    return payload["data"]


def get_coin_leaderboard(token):
    r = requests.get(
        f"{USER_API}/api/v1/user/leaderboards/coin",
        params={"month": TEST_PERIOD},
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    assert r.status_code == 200, f"coin leaderboard http error: {r.status_code} {r.text}"
    payload = r.json()
    assert payload.get("code") == 0, f"coin leaderboard business error: {payload}"
    return payload["data"]


def amount_str(amount):
    """把 API 返回的 Decimal（可能是 str/float/int）统一格式化为两位小数。"""
    return f"{float(amount):.2f}"


def verify_ui(user_token):
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        # 先进入同域名页面注入 token，再跳转榜单页，避免被拦截到登录页
        page.goto(f"{USER_WEB}/login")
        page.evaluate(f"localStorage.setItem('aichuangzuo_access_token', {json.dumps(user_token)})")
        page.goto(f"{USER_WEB}/console/leaderboard")

        page.wait_for_selector(".leaderboard-page", timeout=10000)
        page.wait_for_timeout(800)

        # 创作币榜截图与校验
        page.click(".leaderboard-tabs button:has-text('创作币榜')")
        page.wait_for_timeout(500)
        coin_path = os.path.join(SCREENSHOT_DIR, "leaderboard_coin.png")
        page.screenshot(path=coin_path)
        coin_content = page.inner_text(".leaderboard-page")
        assert TEST_NICKNAME in coin_content, f"创作币榜未显示测试用户昵称: {coin_content[:500]}"
        assert "100.00" in coin_content, f"创作币榜未显示奖励 100.00: {coin_content[:500]}"

        # 自媒体收入榜截图与校验
        page.click(".leaderboard-tabs button:has-text('自媒体收入榜')")
        page.wait_for_timeout(500)
        income_path = os.path.join(SCREENSHOT_DIR, "leaderboard_income.png")
        page.screenshot(path=income_path)
        income_content = page.inner_text(".leaderboard-page")
        assert TEST_NICKNAME in income_content, f"收入榜未显示测试用户昵称: {income_content[:500]}"
        assert TEST_AMOUNT in income_content, f"收入榜未显示申报金额 {TEST_AMOUNT}: {income_content[:500]}"

        browser.close()
        return coin_path, income_path


def main():
    conn = db_conn()
    image_path = None
    try:
        cleanup_test_data(conn)
        user_id = create_test_user(conn)
        print(f"[1/8] 创建测试用户 id={user_id}")

        user_token = login_user()
        print("[2/8] 用户登录成功")

        image_path = make_test_image()
        submit_income(user_token, image_path)
        print("[3/8] 收入申报提交成功")

        admin_token = login_admin()
        print("[4/8] 管理员登录成功")

        submission_id = find_pending_submission(admin_token)
        print(f"[5/8] 找到待审核申报 id={submission_id}")

        approve_submission(admin_token, submission_id)
        print("[6/8] 申报审核通过")

        income_lb = get_income_leaderboard(user_token)
        income_me = next((e for e in income_lb.get("topList", []) if e.get("isMe")), None)
        assert income_me is not None, "income 榜未找到当前用户"
        assert amount_str(income_me["amount"]) == TEST_AMOUNT, f"income 金额不匹配: {income_me}"
        print(f"[7/8] income 榜校验通过: {income_me}")

        grant_result = grant_rewards(admin_token)
        print(f"[8/8] 奖励发放结果: {grant_result}")

        coin_lb = get_coin_leaderboard(user_token)
        coin_me = next((e for e in coin_lb.get("topList", []) if e.get("isMe")), None)
        assert coin_me is not None, "coin 榜未找到当前用户"
        assert float(coin_me["amount"]) == 100.0, f"coin 奖励金额不匹配: {coin_me}"
        print(f"[9/8] coin 榜校验通过: {coin_me}")

        coin_path, income_path = verify_ui(user_token)
        print(f"[10/8] UI 截图已保存: {coin_path}, {income_path}")

        print("\n全部 E2E 校验通过")
        return 0
    except Exception as e:
        print(f"\nE2E 校验失败: {e}", file=sys.stderr)
        return 1
    finally:
        if image_path and os.path.exists(image_path):
            os.unlink(image_path)
        cleanup_test_data(conn)
        conn.close()


if __name__ == "__main__":
    sys.exit(main())

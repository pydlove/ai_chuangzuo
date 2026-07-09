#!/usr/bin/env python3
"""验证用户端意见反馈 → 管理端回复 → 用户收到消息。

运行前请确保：
- MySQL 127.0.0.1:3306 / aichuangzuo / root / 123456
- user-api  http://localhost:25050
- admin-api http://localhost:26060
- user-web  http://localhost:22345
"""
import sys
import uuid
from datetime import datetime

import bcrypt
import pymysql
import requests

USER_API = "http://localhost:25050"
ADMIN_API = "http://localhost:26060"
USER_WEB = "http://localhost:22345"

DB = dict(
    host="127.0.0.1",
    port=3306,
    user="root",
    password="123456",
    database="aichuangzuo",
    charset="utf8mb4",
    cursorclass=pymysql.cursors.DictCursor,
)


def db():
    return pymysql.connect(**DB)


def make_user():
    pw = "Test1234!"
    email = f"fb_e2e_{datetime.now().strftime('%Y%m%d%H%M%S%f')}@example.com"
    pwh = bcrypt.hashpw(pw.encode(), bcrypt.gensalt(12)).decode()
    biz = "F" + uuid.uuid4().hex[:8].upper()
    invite = uuid.uuid4().hex[:6].upper()
    conn = db()
    try:
        with conn.cursor() as c:
            c.execute(
                """INSERT INTO u_user
                   (biz_no,email,password_hash,invite_code,user_status,user_type,email_verified,tenant_id,is_deleted,created_by,updated_by)
                   VALUES (%s,%s,%s,%s,1,1,1,0,0,0,0)""",
                (biz, email, pwh, invite),
            )
            uid = c.lastrowid
        conn.commit()
    finally:
        conn.close()
    token = requests.post(
        f"{USER_API}/api/v1/user/auth/login",
        json={"email": email, "password": pw},
        timeout=10,
    ).json()["data"]["accessToken"]
    return uid, token, email


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


def test_submit_and_reply_and_notify():
    uid, token, email = make_user()
    print(f"  + created user id={uid} email={email}")

    # 提交 5 条都应成功
    for i in range(5):
        r = requests.post(
            f"{USER_API}/api/v1/user/feedback/submit",
            headers={"Authorization": f"Bearer {token}"},
            json={"type": "功能建议", "content": f"测试 {i}"},
            timeout=10,
        )
        assert r.status_code == 200, r.text
        assert r.json()["code"] == 0, r.text
    print("  + submitted 5 feedbacks OK")

    # 第 6 条触发限频
    over = requests.post(
        f"{USER_API}/api/v1/user/feedback/submit",
        headers={"Authorization": f"Bearer {token}"},
        json={"type": "其他", "content": "第六条"},
        timeout=10,
    )
    body = over.json()
    assert over.status_code == 200 and body["code"] == 117001, f"expected 117001 daily limit, got {body}"
    print(f"  + 6th submission blocked by daily limit (code={body['code']})")

    # 取出用户最新一条
    with db() as conn, conn.cursor() as c:
        c.execute("SELECT id FROM u_feedback WHERE user_id=%s ORDER BY id DESC LIMIT 1", (uid,))
        fb_id = c.fetchone()["id"]
    print(f"  + fetched latest feedback id={fb_id}")

    # 管理端登录
    admin_token = login_admin()
    print("  + admin logged in")

    # 回复
    rr = requests.post(
        f"{ADMIN_API}/api/v1/admin/feedbacks/{fb_id}/reply",
        headers={"Authorization": f"Bearer {admin_token}"},
        json={"content": "已收到，感谢反馈"},
        timeout=10,
    )
    assert rr.status_code == 200 and rr.json().get("code") == 0, rr.text
    print("  + admin replied OK")

    # 校验状态 & 消息通知
    with db() as conn, conn.cursor() as c:
        c.execute("SELECT status, reply_content FROM u_feedback WHERE id=%s", (fb_id,))
        row = c.fetchone()
        assert row["status"] == 1, f"expected status=1, got {row['status']}"
        assert row["reply_content"].startswith("已收到"), f"reply content unexpected: {row['reply_content']}"

        c.execute(
            "SELECT id FROM u_message WHERE target_user_id=%s AND msg_type='feedback' AND sub_type='reply' ORDER BY id DESC LIMIT 1",
            (uid,),
        )
        msg = c.fetchone()
        assert msg is not None, "no feedback message was inserted for user"
    print("  + DB verified: feedback status=1, reply persisted, u_message notification inserted")

    # 用户消息列表能拉到这条
    msg_list = requests.get(
        f"{USER_API}/api/v1/user/messages",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    assert msg_list.status_code == 200, msg_list.text
    items = msg_list.json().get("data") or []
    found = any(
        it.get("type") == "feedback" and it.get("subType") == "reply"
        for it in items
    )
    assert found, f"feedback reply message not visible to user: {items}"
    print("  + user message list includes feedback.reply")

    print("PASS  反馈提交、限频、回复、消息通知")


def test_user_feedback_history_api():
    """用户端 GET /feedback/mine:分页 + status 过滤 + 跨用户隔离。"""
    uid_a, token_a, email_a = make_user()
    uid_b, token_b, email_b = make_user()
    print(f"  + created user A id={uid_a} email={email_a}")
    print(f"  + created user B id={uid_b} email={email_b}")

    # 用户 A 提交 2 条待回复
    for i in range(2):
        r = requests.post(
            f"{USER_API}/api/v1/user/feedback/submit",
            headers={"Authorization": f"Bearer {token_a}"},
            json={"type": "功能建议", "content": f"A 的反馈 {i}"},
            timeout=10,
        )
        assert r.status_code == 200 and r.json()["code"] == 0, r.text
    # 用户 B 提交 1 条
    r = requests.post(
        f"{USER_API}/api/v1/user/feedback/submit",
        headers={"Authorization": f"Bearer {token_b}"},
        json={"type": "问题反馈", "content": "B 的反馈"},
        timeout=10,
    )
    assert r.status_code == 200, r.text
    print("  + A 提交 2 条, B 提交 1 条")

    # A 查自己的历史 → 应只看到 2 条
    list_a = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine",
        headers={"Authorization": f"Bearer {token_a}"},
        timeout=10,
    )
    assert list_a.status_code == 200, list_a.text
    body_a = list_a.json()
    assert body_a["code"] == 0, body_a
    assert body_a["data"]["total"] == 2, body_a
    assert len(body_a["data"]["list"]) == 2
    print("  + A 调 /mine 只看到自己 2 条,跨用户隔离 OK")

    # A 加 status=0 过滤 → 应还是 2 条(都待回复)
    list_pending = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine?status=0",
        headers={"Authorization": f"Bearer {token_a}"},
        timeout=10,
    )
    assert list_pending.json()["data"]["total"] == 2, list_pending.text
    # A 加 status=1 过滤 → 应是 0 条
    list_replied = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine?status=1",
        headers={"Authorization": f"Bearer {token_a}"},
        timeout=10,
    )
    assert list_replied.json()["data"]["total"] == 0, list_replied.text
    print("  + status 过滤 OK(0→2 条,1→0 条)")

    # 字段齐全
    item = body_a["data"]["list"][0]
    for key in ("id", "type", "content", "status", "createdAt"):
        assert key in item, f"missing key: {key}"
    print("  + VO 字段齐全")

    # 未登录访问应 401
    noauth = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine", timeout=10
    )
    assert noauth.status_code in (401, 403), f"expected 401/403, got {noauth.status_code}"
    print(f"  + 未登录返回 {noauth.status_code}")

    print("PASS  反馈历史 API:分页 / status 过滤 / 跨用户隔离")


if __name__ == "__main__":
    test_submit_and_reply_and_notify()
    test_user_feedback_history_api()
    sys.exit(0)

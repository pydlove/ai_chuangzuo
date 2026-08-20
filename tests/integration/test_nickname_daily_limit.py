#!/usr/bin/env python3
"""Test nickname check daily limit via admin config and user API."""
import json
import time
import uuid
import requests
import jwt

USER_SECRET = "please-change-this-access-secret-at-least-256-bits-long"
ADMIN_SECRET = "please-change-this-admin-access-secret-at-least-256-bits-long"
USER_API = "http://localhost:25050"
ADMIN_API = "http://localhost:26060"


def make_user_token(user_id: int) -> str:
    now = int(time.time())
    payload = {
        "sub": str(user_id),
        "jti": str(uuid.uuid4()),
        "iat": now,
        "exp": now + 7200,
    }
    return jwt.encode(payload, USER_SECRET, algorithm="HS256")


def make_admin_token(admin_user_id: int) -> str:
    now = int(time.time())
    payload = {
        "sub": str(admin_user_id),
        "jti": str(uuid.uuid4()),
        "iat": now,
        "exp": now + 7200,
    }
    return jwt.encode(payload, ADMIN_SECRET, algorithm="HS256")


def admin_get_config(token: str):
    r = requests.get(
        f"{ADMIN_API}/api/v1/admin/security/rate-limit-config",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    r.raise_for_status()
    return r.json()


def admin_update_config(token: str, enabled: int, daily_limit: int):
    r = requests.put(
        f"{ADMIN_API}/api/v1/admin/security/rate-limit-config",
        headers={"Authorization": f"Bearer {token}"},
        json={"isLoginRateLimitEnabled": enabled, "nicknameCheckDailyLimit": daily_limit},
        timeout=10,
    )
    r.raise_for_status()
    return r.json()


def user_check(token: str, nickname: str):
    r = requests.post(
        f"{USER_API}/api/v1/user/self-media/nickname/check",
        headers={"Authorization": f"Bearer {token}"},
        json={"nickname": nickname},
        timeout=30,
    )
    return r.status_code, r.json()


def main():
    admin_token = make_admin_token(1)
    user_token = make_user_token(999999)  # non-existent user, no plan

    print("=== 1. Get current admin config ===")
    cfg = admin_get_config(admin_token)
    print(json.dumps(cfg, ensure_ascii=False, indent=2))

    print("\n=== 2. Set daily limit to 1 ===")
    upd = admin_update_config(admin_token, 1, 1)
    print(json.dumps(upd, ensure_ascii=False, indent=2))

    print("\n=== 3. First user /check (should pass limiter, fail with no plan) ===")
    status1, body1 = user_check(user_token, "测试昵称")
    print(f"status={status1}, body={json.dumps(body1, ensure_ascii=False)}")

    print("\n=== 4. Second user /check (should hit daily limit 113008) ===")
    status2, body2 = user_check(user_token, "测试昵称2")
    print(f"status={status2}, body={json.dumps(body2, ensure_ascii=False)}")

    assert body2.get("code") == 113008, f"Expected code 113008, got {body2.get('code')}"

    print("\n=== 5. Reset daily limit to 10 ===")
    reset = admin_update_config(admin_token, 1, 10)
    print(json.dumps(reset, ensure_ascii=False, indent=2))

    print("\nTest passed: daily limit enforced.")


if __name__ == "__main__":
    main()

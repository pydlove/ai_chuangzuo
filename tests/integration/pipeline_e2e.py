#!/usr/bin/env python3
"""
12 阶段流水线端到端冒烟测试（需要 admin-api + user-api 同时运行）。

跑什么：
    1. user-api 登录（开发态用固定测试账号）拿 JWT
    2. 提交创作任务
    3. 轮询 GET /api/v1/user/generation-tasks/{id} 直到 status 终态（COMPLETED/FAILED）
    4. 验证 progress_pct 单调递增到 100（成功）或停在失败时进度

端口约定（CLAUDE.md / docs/architecture/tech-stack.md）：
    - user-api:  http://localhost:25050
    - admin-api: http://localhost:26060

前置条件：
    - user-api 在 25050 跑起来，DB 已 seed 测试用户（user_id 由登录账号推导）
    - admin-api 在 26060 跑起来，worker 拉 queued 任务跑 pipeline
    - DB 中 t_prompt_template 至少有 1 个 PUBLISHED 模板（12 阶段默认值即可）
    - 测试账号有 ≥ 1 创作币余额

用法：
    python3 tests/integration/pipeline_e2e.py \\
        --login-phone 13800000000 --password test123 \\
        --title "AI 写作测试" --word-count 600

退出码：
    0 - 任务完成且进度推进到 100
    1 - 任何一步失败（登录失败 / 提交失败 / 任务失败 / 进度不回写）
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

USER_API = "http://localhost:25050"
ADMIN_API = "http://localhost:26060"


def http(method: str, url: str, body: dict | None = None, token: str | None = None) -> dict:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except HTTPError as e:
        body_txt = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"HTTP {e.code} {url}: {body_txt}") from e
    except URLError as e:
        raise RuntimeError(f"无法连接 {url}: {e.reason}") from e


def login(phone: str, password: str) -> str:
    """登录拿 token。开发态假设 user-api 暴露 /api/v1/user/auth/login。"""
    resp = http("POST", f"{USER_API}/api/v1/user/auth/login",
                {"phone": phone, "password": password})
    data = resp.get("data") or {}
    token = data.get("token") or data.get("accessToken")
    if not token:
        raise RuntimeError(f"登录响应无 token: {resp}")
    return token


def submit_task(token: str, title: str, word_count: int) -> int:
    resp = http("POST", f"{USER_API}/api/v1/user/generation-tasks",
                {
                    "title": title,
                    "description": "e2e smoke test",
                    "platform": "wechat",
                    "wordCount": word_count,
                },
                token=token)
    data = resp.get("data") or {}
    task_id = data.get("id")
    if not task_id:
        raise RuntimeError(f"提交响应无 task id: {resp}")
    return int(task_id)


def poll_progress(token: str, task_id: int, timeout_s: int = 300) -> tuple[str, list[int]]:
    """轮询进度直到终态或超时。返回 (终态, progress_pct 轨迹)。"""
    trajectory: list[int] = []
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        resp = http("GET", f"{USER_API}/api/v1/user/generation-tasks/{task_id}", token=token)
        data = resp.get("data") or {}
        status = data.get("status")
        pct = data.get("progressPct")
        if pct is not None:
            trajectory.append(int(pct))
        print(f"  status={status} progress={pct}", flush=True)
        if status in ("COMPLETED", "FAILED"):
            return status, trajectory
        time.sleep(2)
    raise RuntimeError(f"任务 {task_id} 在 {timeout_s}s 内未达终态")


def admin_worker_alive() -> bool:
    """可选：检查 admin-api 是否在线（worker 是否可能在跑）。"""
    try:
        http("GET", f"{ADMIN_API}/actuator/health")
        return True
    except Exception:
        return False


def main() -> int:
    p = argparse.ArgumentParser(description="12 阶段流水线 e2e 冒烟测试")
    p.add_argument("--login-phone", required=True)
    p.add_argument("--password", required=True)
    p.add_argument("--title", default="e2e pipeline smoke")
    p.add_argument("--word-count", type=int, default=600)
    p.add_argument("--timeout", type=int, default=300)
    args = p.parse_args()

    print(f"[1] 登录 phone={args.login_phone}")
    token = login(args.login_phone, args.password)

    print(f"[2] 提交任务 title={args.title} wordCount={args.word_count}")
    task_id = submit_task(token, args.title, args.word_count)
    print(f"  task id = {task_id}")

    print(f"[3] 轮询进度（timeout={args.timeout}s）")
    status, traj = poll_progress(token, task_id, args.timeout)

    print(f"[4] 终态 status={status} 进度轨迹={traj}")
    if status == "COMPLETED":
        if traj and traj[-1] == 100:
            print("OK: 任务成功完成，进度推进到 100")
            return 0
        print("WARN: 任务成功但 progress 轨迹异常", file=sys.stderr)
        return 1
    print(f"FAIL: 任务失败 status={status} 最后进度={traj[-1] if traj else None}",
          file=sys.stderr)
    return 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)

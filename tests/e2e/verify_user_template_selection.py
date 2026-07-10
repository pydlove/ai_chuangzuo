#!/usr/bin/env python3
"""创作模板阶段 3 端到端验证：用户端模板选择 + 任务版本锁定。

覆盖场景：
  - user 端 API 能列出已发布模板（含默认模板，不含草稿/下线模板）
  - user 端 UI 创作页显示「创作模板」下拉框
  - admin 克隆并发布一个自定义模板
  - user 选择自定义模板提交任务，任务记录锁定该模板 id + version
  - admin 将自定义模板下线
  - user 列表中不再出现已下线模板
  - user 用已下线模板 id 提交任务应失败（212009）
  - user 已提交任务仍保留锁定版本信息（不受模板下线影响）

前置条件：
  - admin-api 运行在 26060，user-api 运行在 25050
  - user-web 运行在 22345，admin-web 运行在 22346
  - DB 已执行 V2.0.0_001 ~ V2.0.0_019
  - 默认模板 id=1 存在且为已发布状态
  - 已 seed admin / user 测试账号且用户有足够的创作币余额

用法：
  python3 tests/e2e/verify_user_template_selection.py
"""
import sys
import time
from pathlib import Path

import requests
from playwright.sync_api import sync_playwright

ADMIN_API = "http://localhost:26060/api/v1/admin"
USER_API = "http://localhost:25050/api/v1/user"
USER_WEB = "http://localhost:22345"

ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "Root1qaz!QAZ"
USER_EMAIL = "test@local"
USER_PASSWORD = "Test@123"

SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def admin_login():
    resp = requests.post(
        f"{ADMIN_API}/auth/login",
        json={"username": ADMIN_USERNAME, "password": ADMIN_PASSWORD},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"admin 登录失败: {body}"
    return body["data"]["accessToken"]


def user_login():
    resp = requests.post(
        f"{USER_API}/auth/login",
        json={"email": USER_EMAIL, "password": USER_PASSWORD},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"user 登录失败: {body}"
    return body["data"]["accessToken"]


def list_user_templates(token):
    resp = requests.get(
        f"{USER_API}/prompt-templates",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"user 列模板失败: {body}"
    return body["data"]


def get_user_template(token, tid):
    resp = requests.get(
        f"{USER_API}/prompt-templates/{tid}",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    return resp.json()


def clone_template(token, source_id, name):
    resp = requests.post(
        f"{ADMIN_API}/prompt-templates/{source_id}/actions/clone",
        headers={"Authorization": f"Bearer {token}"},
        json={"name": name},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"克隆失败: {body}"
    return body["data"]


def publish_template(token, tid, change_note=""):
    resp = requests.post(
        f"{ADMIN_API}/prompt-templates/{tid}/actions/publish",
        headers={"Authorization": f"Bearer {token}"},
        json={"changeNote": change_note},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"发布失败: {body}"
    return body["data"]


def offline_template(token, tid):
    resp = requests.post(
        f"{ADMIN_API}/prompt-templates/{tid}/actions/offline",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    body = resp.json()
    assert body.get("code") == 0, f"下线失败: {body}"


def delete_template(token, tid):
    resp = requests.delete(
        f"{ADMIN_API}/prompt-templates/{tid}",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    return resp.json().get("code")


def submit_generation(token, template_id=None, **kwargs):
    payload = {
        "title": kwargs.get("title", "阶段 3 验证"),
        "description": kwargs.get("description", "验证模板选择"),
        "platform": kwargs.get("platform", "wechat"),
        "wordCount": kwargs.get("wordCount", 600),
    }
    if template_id is not None:
        payload["templateId"] = template_id
    resp = requests.post(
        f"{USER_API}/generation-tasks",
        headers={
            "Authorization": f"Bearer {token}",
            "Idempotency-Key": kwargs.get("idempotency_key", f"e2e-stage3-{int(time.time())}"),
        },
        json=payload,
        timeout=15,
    )
    return resp.json()


def get_task(token, task_id):
    resp = requests.get(
        f"{USER_API}/generation-tasks/{task_id}",
        headers={"Authorization": f"Bearer {token}"},
        timeout=10,
    )
    return resp.json()


def main():
    results = []

    # ===== Part 1：admin 准备自定义模板 =====
    admin_token = admin_login()
    print("[info] admin 登录成功")

    clone_name = f"阶段3-E2E-{int(time.time())}"
    custom_id = clone_template(admin_token, 1, clone_name)
    custom_v1 = publish_template(admin_token, custom_id, "阶段 3 首次发布")
    assert custom_v1 == 1, f"自定义模板首次发布应返回 v1，实际 {custom_v1}"
    print(f"[info] 自定义模板创建并发布 id={custom_id} v={custom_v1}")

    # ===== Part 2：user API 验证模板列表与选择 =====
    user_token = user_login()
    print("[info] user 登录成功")

    templates = list_user_templates(user_token)
    assert isinstance(templates, list), "user 模板列表应为数组"
    default_in_list = next((t for t in templates if t["id"] == 1), None)
    assert default_in_list is not None, "默认模板应在 user 列表中"
    assert default_in_list.get("isBuiltin") is True, "默认模板应标记 isBuiltin"
    assert default_in_list.get("latestPublishedVersion") is not None, "默认模板应有版本号"

    custom_in_list = next((t for t in templates if t["id"] == custom_id), None)
    assert custom_in_list is not None, f"自定义模板 {custom_id} 应在 user 列表中"
    assert custom_in_list.get("latestPublishedVersion") == 1, "自定义模板应为 v1"
    assert custom_in_list.get("isBuiltin") is False, "自定义模板不应是内置"
    print("[info] user 模板列表包含默认模板和自定义模板")
    results.append(("user API 列出已发布模板（含默认+自定义）", True))

    # 详情接口
    custom_detail = get_user_template(user_token, custom_id)
    assert custom_detail.get("code") == 0, f"user 查自定义模板详情失败: {custom_detail}"
    assert custom_detail["data"]["id"] == custom_id
    results.append(("user API 查模板详情", True))

    # 选择自定义模板提交任务
    submit_body = submit_generation(
        user_token,
        template_id=custom_id,
        title="阶段 3 验证：选择自定义模板",
        description="验证 prompt_template_version 锁定",
        platform="wechat",
        wordCount=600,
    )
    assert submit_body.get("code") == 0, f"选择自定义模板提交失败: {submit_body}"
    task_data = submit_body["data"]
    task_id = task_data["taskId"]
    assert task_data.get("promptTemplateId") == custom_id, \
        f"任务应锁定自定义模板 id，实际 {task_data.get('promptTemplateId')}"
    assert task_data.get("promptTemplateVersion") == 1, \
        f"任务应锁定版本 1，实际 {task_data.get('promptTemplateVersion')}"
    print(f"[info] 任务提交成功 taskId={task_id}，锁定 templateId={custom_id} version=1")
    results.append(("选择自定义模板提交任务并锁定 version=1", True))

    # ===== Part 3：admin 下线自定义模板 =====
    offline_template(admin_token, custom_id)
    print(f"[info] 自定义模板 {custom_id} 已下线")

    # user 列表中不应再包含自定义模板
    templates_after = list_user_templates(user_token)
    custom_after = next((t for t in templates_after if t["id"] == custom_id), None)
    assert custom_after is None, "下线后 user 列表不应再包含自定义模板"
    results.append(("模板下线后从 user 列表消失", True))

    # user 用已下线模板提交应失败
    offline_submit = submit_generation(
        user_token,
        template_id=custom_id,
        title="阶段 3 验证：使用已下线模板",
        idempotency_key=f"e2e-stage3-offline-{int(time.time())}",
    )
    assert offline_submit.get("code") == 212009, \
        f"已下线模板提交应返回 212009，实际 {offline_submit.get('code')}"
    results.append(("已下线模板提交返回 212009", True))

    # 已提交任务仍保留锁定信息
    task_body = get_task(user_token, task_id)
    assert task_body.get("code") == 0, f"查任务失败: {task_body}"
    assert task_body["data"].get("promptTemplateId") == custom_id
    assert task_body["data"].get("promptTemplateVersion") == 1
    results.append(("已提交任务保留锁定模板版本（不受下线影响）", True))

    # ===== Part 4：user UI 验证模板下拉框 =====
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        page.goto(f"{USER_WEB}/login", wait_until="networkidle")
        page.fill('input[name="email"]', USER_EMAIL)
        page.fill('input[name="password"]', USER_PASSWORD)
        page.click('button[type="submit"]')
        page.wait_for_url("**/console/**", timeout=10000)

        page.goto(f"{USER_WEB}/console/generation-queue", wait_until="networkidle")
        page.wait_for_selector('.generation-queue', timeout=10000)

        # 检查「创作模板」标签和下拉框存在
        template_label = page.locator('.ant-form-item-label:has-text("创作模板")')
        assert template_label.count() >= 1, "未找到「创作模板」标签"

        template_select = page.locator('.ant-form-item:has(.ant-form-item-label:has-text("创作模板")) .ant-select')
        assert template_select.count() >= 1, "未找到创作模板下拉框"

        # 点击下拉应能看到默认模板
        template_select.click()
        page.wait_for_selector('.ant-select-dropdown', timeout=5000)
        assert page.locator('.ant-select-dropdown:has-text("默认去 AI 味模板")').count() >= 1, \
            "下拉中未找到默认模板"

        page.screenshot(
            path=str(SCREENSHOT_DIR / "user_template_selector.png"),
            full_page=True,
        )
        browser.close()
        results.append(("UI：创作页显示模板选择下拉框并包含默认模板", True))

    # ===== Part 5：清理 =====
    # 重新发布再删除，或如果删除接口允许删除下线模板则直接删除
    delete_code = delete_template(admin_token, custom_id)
    # 删除接口对任何非内置模板都应成功
    assert delete_code == 0, f"清理自定义模板失败 code={delete_code}"
    results.append(("清理自定义模板", True))

    # ===== Summary =====
    print()
    print("=" * 60)
    passed = sum(1 for _, ok in results if ok)
    print(f"阶段 3 用户模板选择验证：{passed}/{len(results)} 通过")
    for name, ok in results:
        print(f"  {'PASS' if ok else 'FAIL'}  {name}")
    print("=" * 60)

    failed = [name for name, ok in results if not ok]
    if failed:
        print(f"\n失败项：{failed}")
        sys.exit(1)


if __name__ == "__main__":
    main()

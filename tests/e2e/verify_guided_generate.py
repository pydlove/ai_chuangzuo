#!/usr/bin/env python3
"""引导模式生成链路：确认→进度卡→结果卡；失败态→重试按钮。额度 0 拦截。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def mock_common(page, quota_remaining=12, quota_value="50"):
    page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
        "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                            "benefits": [{"code": "ai_article_quota", "value": quota_value, "remaining": quota_remaining}]}}))
    page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
    page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化", "prompt": "x", "scope": "通用"}]}))
    page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]}))


def run_flow_to_confirm(page):
    page.goto(f"{BASE}/console/create", wait_until="networkidle")
    page.wait_for_timeout(1200)
    page.fill(".hero-input", "测试主题")
    page.keyboard.press("Enter")
    page.wait_for_timeout(400)
    page.click(".quick-option:has-text('公众号')")
    page.click(".quick-confirm")
    page.wait_for_timeout(400)
    page.click(".quick-option:has-text('轻松口语')")
    page.click(".quick-confirm")
    page.wait_for_timeout(500)
    # 第 4 步：模板
    page.click(".quick-option:has-text('公众号深度文')")
    page.wait_for_timeout(300)
    page.click(".quick-confirm")
    page.wait_for_timeout(600)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # --- 场景 1：成功链路 ---
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_common(page)
        page.route("**/api/v1/user/generation-tasks", lambda r: (
            r.fulfill(json={"code": 0, "data": {"id": 900, "status": 1, "progressPct": 0}})
            if r.request.method == "POST" else
            r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}})))
        poll_state = {"n": 0}

        def on_poll(route):
            poll_state["n"] += 1
            done = poll_state["n"] >= 2
            route.fulfill(json={"code": 0, "data": {
                "id": 900, "status": 2 if done else 1, "progressPct": 100 if done else 45,
                "title": "测试主题", "inputParam": {}}})
        page.route("**/api/v1/user/generation-tasks/900", on_poll)
        run_flow_to_confirm(page)
        page.click(".confirm-generate")
        page.wait_for_timeout(1000)
        ok_progress = page.query_selector(".chat-progress") is not None
        page.screenshot(path=f"{SHOTS}/guided_progress.png")
        page.wait_for_timeout(6500)  # 等第二轮轮询（间隔 3s）
        ok_result = page.query_selector("text=已生成完成") is not None
        page.screenshot(path=f"{SHOTS}/guided_result.png")
        print("PASS progress" if ok_progress else "FAIL progress")
        print("PASS result" if ok_result else "FAIL result")
        page.close()

        # --- 场景 2：失败 → 重试按钮 ---
        page3 = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_common(page3)
        page3.route("**/api/v1/user/generation-tasks", lambda r: (
            r.fulfill(json={"code": 0, "data": {"id": 901, "status": 1, "progressPct": 0}})
            if r.request.method == "POST" else
            r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}})))
        page3.route("**/api/v1/user/generation-tasks/901", lambda r: r.fulfill(json={
            "code": 0, "data": {"id": 901, "status": 3, "progressPct": 60, "failedReason": "内容审核未通过", "inputParam": {}}}))
        run_flow_to_confirm(page3)
        page3.click(".confirm-generate")
        page3.wait_for_timeout(4500)
        ok_failed = page3.query_selector(".failed-text") is not None
        ok_retry = page3.query_selector("button:has-text('重试')") is not None
        page3.screenshot(path=f"{SHOTS}/guided_failed.png")
        print("PASS failed" if ok_failed else "FAIL failed")
        print("PASS retry" if ok_retry else "FAIL retry")
        page3.close()

        # --- 场景 3：额度 0 拦截 ---
        page2 = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_common(page2, quota_remaining=0)
        page2.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
        page2.goto(f"{BASE}/console/create", wait_until="networkidle")
        page2.wait_for_timeout(1200)
        ok_quota = page2.query_selector("text=本月额度已用完") is not None
        ok_no_input = page2.query_selector(".hero-input") is None
        print("PASS quota-block" if ok_quota else "FAIL quota-block")
        print("PASS no-input" if ok_no_input else "FAIL no-input")
        page2.screenshot(path=f"{SHOTS}/guided_quota_block.png")
        page2.close()

        browser.close()
        if not all([ok_progress, ok_result, ok_failed, ok_retry, ok_quota, ok_no_input]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

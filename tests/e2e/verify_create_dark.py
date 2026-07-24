#!/usr/bin/env python3
"""深色走查：引导模式、极简模式、队列抽屉、确认卡片 浅色/深色各一组截图。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def mock_all(page):
    def handle(route):
        url = route.request.url
        if "/api/v1/user/me" in url:
            return route.fulfill(json={"code": 0, "data": {"userId": "U1", "nickname": "Tester", "email": "t@example.com", "avatarUrl": None, "emailVerified": 1, "inviterUserId": None, "inviterNickname": None}})
        if "/api/v1/user/messages" in url:
            return route.fulfill(json={"code": 0, "data": []})
        if "/api/v1/user/membership/me" in url:
            return route.fulfill(json={"code": 0, "data": {"hasMembership": False}})
        if "/api/v1/user/account/invite-stats" in url:
            return route.fulfill(json={"code": 0, "data": {"inviteCode": "", "invitedCount": 0, "membershipDaysEarned": 0, "coinEarned": 0, "coinBalance": 0, "friends": []}})
        if "/api/v1/user/plans/newcomer-offer" in url:
            return route.fulfill(json={"code": 0, "data": {"eligible": False}})
        if "/api/v1/user/articles/monthly-count" in url:
            return route.fulfill(json={"code": 0, "data": 0})
        if "/api/v1/user/articles" in url:
            return route.fulfill(json={"code": 0, "data": {"list": [], "total": 0, "page": 1, "pageSize": 10}})
        if "/api/v1/user/benefits/me" in url:
            return route.fulfill(json={"code": 0, "data": {"planKey": "pro", "planName": "专业版", "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}})
        if "/api/v1/user/generation-tasks" in url:
            return route.fulfill(json={"code": 0, "data": {"list": [], "total": 0}})
        if "/api/v1/user/topics/" in url:
            return route.fulfill(json={"code": 0, "data": []})
        if "/api/v1/user/styles/system-styles" in url:
            return route.fulfill(json={"code": 0, "data": [{"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化", "prompt": "x", "scope": "通用"}]})
        if "/api/v1/user/styles" in url:
            return route.fulfill(json={"code": 0, "data": []})
        if "/api/v1/user/market-styles" in url:
            return route.fulfill(json={"code": 0, "data": []})
        if "/api/v1/user/export-templates" in url:
            return route.fulfill(json={"code": 0, "data": [
                {"templateKey": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu"},
                {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]})
        if "/api/v1/user/home/banners" in url:
            return route.fulfill(json={"code": 0, "data": []})
        route.fallback()

    page.route("**/api/v1/user/**", handle)


def set_theme(page, theme):
    page.evaluate(f"localStorage.setItem('aichuangzuo_theme', '{theme}'); document.body.setAttribute('data-theme', '{theme}')")
    page.wait_for_timeout(400)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_all(page)
        # 模拟已登录，避免被新的路由守卫重定向到 /login
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_access_token', 'mock-token')")
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        # 引导模式 → 走完到确认卡
        page.fill(".hero-input", "深夜食堂的经济学")
        page.keyboard.press("Enter")
        page.wait_for_timeout(600)
        # hero 提交后进入 topic 步骤，点击发送确认主题
        page.click(".topic-send")
        page.wait_for_timeout(600)
        page.click(".quick-option:has-text('小红书')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('公众号深度文')")
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        page.screenshot(path=f"{SHOTS}/dark_guided_confirm_light.png")
        set_theme(page, "dark")
        page.screenshot(path=f"{SHOTS}/dark_guided_confirm.png")

        # 切极简模式
        set_theme(page, "light")
        page.click("text=熟手模式")
        page.wait_for_timeout(600)
        page.screenshot(path=f"{SHOTS}/dark_minimal_light.png")
        set_theme(page, "dark")
        page.screenshot(path=f"{SHOTS}/dark_minimal.png")

        # 队列抽屉
        page.click("text=队列")
        page.wait_for_timeout(800)
        page.screenshot(path=f"{SHOTS}/dark_drawer.png")

        print("DONE - 走查截图已生成")
        browser.close()


main()

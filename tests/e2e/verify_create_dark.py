#!/usr/bin/env python3
"""深色走查：引导模式、极简模式、队列抽屉、确认卡片 浅色/深色各一组截图。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def mock_all(page):
    page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
        "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                            "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
    page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))
    page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
    page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化", "prompt": "x", "scope": "通用"}]}))
    page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
        {"templateKey": "xiaohongshu-note", "name": "种草清单", "platform": "xiaohongshu"},
        {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]}))


def set_theme(page, theme):
    page.evaluate(f"localStorage.setItem('aichuangzuo_theme', '{theme}'); document.body.setAttribute('data-theme', '{theme}')")
    page.wait_for_timeout(400)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        mock_all(page)
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        # 引导模式 → 走完到确认卡
        page.fill(".topic-input", "深夜食堂的经济学")
        page.keyboard.press("Enter")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('小红书')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('轻松口语')")
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

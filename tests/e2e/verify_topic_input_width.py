"""回归：点改主题 → 输入框宽度应 > 400px（之前只有 188px 被 85% 气泡压缩）。"""
import os
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/styles*", lambda r: r.fulfill(json={"code": 0, "data": [
            {"bizNo": "S1", "styleName": "轻松口语", "prompt": "p", "scope": "通用", "useCount": 1, "sourceType": 1}
        ]}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"bizNo": "S1", "name": "轻松口语", "prompt": "p", "scope": "通用"}
        ]}))
        page.route("**/api/v1/user/market-styles**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"templateKey": "wechat", "name": "公众号深度文", "description": "d", "platform": "wechat",
             "bgColor": "#fff", "textColor": "#1a1a1a", "visualStyle": {"bg":"#fff"}}
        ]}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        # 走到确认卡
        page.fill(".hero-input", "测试")
        page.keyboard.press("Enter")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('公众号')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('公众号深度文')")
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # 点改主题
        page.click(".confirm-edit:has-text('改主题')")
        page.wait_for_timeout(400)

        # 测量
        width = page.evaluate("() => document.querySelector('.topic-input')?.getBoundingClientRect().width")
        bubble_w = page.evaluate("() => document.querySelector('.chat-bubble:has(.topic-input-row)')?.getBoundingClientRect().width")
        list_w = page.evaluate("() => document.querySelector('.chat-list')?.getBoundingClientRect().width")
        print(f"Input: {width:.0f}px, Bubble: {bubble_w:.0f}px, List: {list_w:.0f}px")
        print(f"Input / List: {width / list_w * 100:.1f}%")

        # 输入框至少 400px 宽（之前只有 188px）
        ok_wide = width >= 400
        # 输入框应至少是 chat-list 的 60%
        ok_ratio = (width / list_w) >= 0.6
        # 仍然能输入
        page.fill(".topic-input", "新主题")
        ok_input = page.input_value(".topic-input") == "新主题"

        page.screenshot(path=f"{SHOTS}/topic_input_wide.png")

        results = [("input-wide", ok_wide), ("ratio-ok", ok_ratio), ("input-works", ok_input)]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

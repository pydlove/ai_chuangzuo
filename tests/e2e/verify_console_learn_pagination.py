#!/usr/bin/env python3
"""控制台 - 创作学院「全部」tab 分页验证。

前置条件：
- user-web dev 启动（默认 http://localhost:22345）

本脚本通过 Playwright 拦截相关 API，避免依赖真实登录态与后端数据。
"""

import os
import sys
import time
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "console_learn"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def _ok(data):
    return json.dumps({"code": 0, "data": data, "message": "ok"})


def _make_all_articles_response(page=1, size=20, total=45):
    start = (page - 1) * size + 1
    count = min(size, total - start + 1)
    records = [
        {
            "id": start + i,
            "title": f"全部文章 {start + i}",
            "summary": f"摘要 {start + i}",
            "content": "x" * 600,
            "coverImageUrl": f"https://picsum.photos/400/200?random={start + i}",
            "categoryName": "创作技巧" if page == 1 else "平台运营",
            "isFree": 1 if page == 1 else 0,
            "canRead": True if page == 1 else False,
            "requiredPlanName": None if page == 1 else "Pro 会员",
            "publishedAt": "2026-08-01T00:00:00",
            "updatedAt": "2026-08-01T00:00:00"
        }
        for i in range(count)
    ]
    return _ok({"current": page, "size": size, "total": total, "records": records})


def _mock_routes(page):
    # 兜底：其它用户端接口返回空成功，避免 401 跳转；先注册，优先级最低
    page.route("**/api/v1/user/**", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body=_ok({})
    ))
    # 特定接口后注册，优先匹配
    page.route("**/api/v1/user/me", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body=_ok({"userId": "88886666", "nickname": "测试用户", "email": "test@example.com", "avatarUrl": None, "emailVerified": 1, "inviterUserId": None, "inviterNickname": None})
    ))
    page.route("**/api/v1/user/learn/category/tree", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body=_ok([
            {"id": 1, "parentId": None, "name": "创作技巧", "sort": 1, "children": []},
            {"id": 2, "parentId": None, "name": "平台运营", "sort": 2, "children": []},
            {"id": 3, "parentId": None, "name": "爆款方法", "sort": 3, "children": []},
            {"id": 4, "parentId": None, "name": "变现指南", "sort": 4, "children": []},
            {"id": 5, "parentId": None, "name": "AI 工具", "sort": 5, "children": []},
        ])
    ))
    page.route("**/api/v1/user/learn/article/recommended", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body=_ok([
            {"id": 101, "title": "推荐文章一", "coverImageUrl": "https://picsum.photos/400/200?random=1", "summary": "摘要一", "isFree": 1, "canRead": True},
            {"id": 102, "title": "推荐文章二", "coverImageUrl": "https://picsum.photos/400/200?random=2", "summary": "摘要二", "isFree": 1, "canRead": True},
        ])
    ))
    page.route("**/api/v1/user/learn/article/all**", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body=_make_all_articles_response(page=1)
    ))
    page.route("**/api/v1/user/learn/banner", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body=_ok([])
    ))


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # Desktop
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        _mock_routes(page)
        page.goto(USER_URL)
        page.evaluate("localStorage.setItem('aichuangzuo_access_token', 'fake-token-for-e2e')")
        page.goto(f"{USER_URL}/console/learn")
        page.wait_for_selector(".article-list .article-card", timeout=10000)

        cards = page.locator(".article-list .article-card").count()
        pagination_visible = page.locator(".all-articles-pagination").is_visible()
        page_items = page.locator(".all-articles-pagination .ant-pagination-item").count() if pagination_visible else 0

        print(f"desktop cards: {cards}")
        print(f"desktop pagination visible: {pagination_visible}")
        print(f"desktop pagination page items: {page_items}")

        page.screenshot(path=SCREENSHOTS_DIR / "01-desktop-all-tab.png", full_page=True)

        if pagination_visible:
            # 覆盖 all 接口返回第二页数据；后注册优先匹配
            page.route("**/api/v1/user/learn/article/all**", lambda route: route.fulfill(
                status=200,
                content_type="application/json",
                body=_make_all_articles_response(page=2)
            ))
            page.locator(".all-articles-pagination .ant-pagination-item-2").click()
            page.wait_for_timeout(1000)
            page.wait_for_selector(".article-list .article-card", timeout=10000)
            cards_p2 = page.locator(".article-list .article-card").count()
            print(f"desktop page 2 cards: {cards_p2}")
            page.screenshot(path=SCREENSHOTS_DIR / "02-desktop-page-2.png", full_page=True)

        ctx.close()

        # Mobile
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        _mock_routes(page2)
        page2.goto(USER_URL)
        page2.evaluate("localStorage.setItem('aichuangzuo_access_token', 'fake-token-for-e2e')")
        page2.goto(f"{USER_URL}/console/learn")
        page2.wait_for_selector(".article-list .article-card", timeout=10000)

        cards_m = page2.locator(".article-list .article-card").count()
        pagination_visible_m = page2.locator(".all-articles-pagination").is_visible()
        print(f"mobile cards: {cards_m}")
        print(f"mobile pagination visible: {pagination_visible_m}")

        page2.screenshot(path=SCREENSHOTS_DIR / "03-mobile-all-tab.png", full_page=True)
        ctx2.close()
        browser.close()

        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)

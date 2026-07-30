#!/usr/bin/env python3
"""Verify preview/export meta bar shows real word count and skill name."""
from playwright.sync_api import sync_playwright

BASE = "http://127.0.0.1:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

ARTICLE = {
    "bizNo": "A123",
    "title": "示例标题",
    "body": "这是第一段正文内容。\n\n这是第二段正文内容，用于测试字数统计。",
    "wordCount": 0,
    "completedAt": "2026-07-30T10:29:00",
    "skill": "去除AI味的提示词",
    "skillName": "去除AI味的提示词",
    "platform": "xiaohongshu",
    "template": "xiaohongshu_default",
    "description": "",
    "tags": []
}


def handle_route(route):
    url = route.request.url
    if url.endswith("/api/v1/user/articles/A123"):
        return route.fulfill(status=200, content_type="application/json",
                             body='{"code":0,"data":' + __import__('json').dumps(ARTICLE, ensure_ascii=False) + '}')
    if url.endswith("/api/v1/user/export-templates"):
        return route.fulfill(status=200, content_type="application/json", body='{"code":0,"data":[]}')
    if "/api/v1/user/" in url:
        return route.fulfill(status=200, content_type="application/json", body='{"code":0,"data":null}')
    route.continue_()


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1280, "height": 800})
        page = ctx.new_page()
        page.route("**/api/v1/user/**", handle_route)

        page.goto(f"{BASE}/login")
        page.evaluate('() => { localStorage.setItem("aichuangzuo_access_token", "dummy"); }')
        page.goto(f"{BASE}/console/preview/A123", wait_until="networkidle")
        page.wait_for_timeout(1200)

        print("page url:", page.url)
        meta = page.inner_text(".article-meta")
        print("meta text:", meta)

        page.screenshot(path=f"{SCREENSHOT_DIR}/preview_meta_fix.png", full_page=True)

        assert "约 29 字" in meta, f"expected computed word count fallback, got: {meta}"
        assert "提示词：去除AI味的提示词" in meta, f"expected skill label/name, got: {meta}"

        ctx.close()
        browser.close()
        print("preview meta OK")


if __name__ == "__main__":
    main()

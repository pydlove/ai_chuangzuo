#!/usr/bin/env python3
"""Verify mobile free create skill prompt modal cards are compact."""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:28586")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"
VIEW = {"width": 390, "height": 844}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport=VIEW, is_mobile=True, has_touch=True)

        def handle(route):
            url = route.request.url
            if "/skills/system-skills" in url:
                return route.fulfill(json={
                    "code": 0,
                    "data": {
                        "records": [
                            {"bizNo": "s1", "name": "系统默认", "description": "系统预设", "promptSummary": "系统默认提示词摘要", "prompt": "系统默认提示词", "scope": "通用"},
                            {"bizNo": "s2", "name": "小红书爆款", "description": "小红书风格", "promptSummary": "写出小红书爆款笔记", "prompt": "写出小红书爆款笔记", "scope": "小红书"},
                        ],
                        "total": 2,
                    },
                })
            if "/skills" in url and "sourceType=1" in url:
                return route.fulfill(json={
                    "code": 0,
                    "data": {
                        "records": [
                            {"bizNo": "m1", "name": "我的职场", "description": "我的职场提示词", "promptSummary": "职场成长类文章", "prompt": "职场成长类文章", "scope": "职场", "count": 5},
                            {"bizNo": "m2", "name": "我的副业", "description": "副业探索", "promptSummary": "副业探索类文章", "prompt": "副业探索类文章", "scope": "副业", "count": 3},
                        ],
                        "total": 2,
                    },
                })
            if "/skills" in url and "sourceType=2" in url:
                return route.fulfill(json={
                    "code": 0,
                    "data": {
                        "records": [
                            {"bizNo": "l1", "name": "学习的文案", "description": "学习来的文案风格", "promptSummary": "学习文案风格", "prompt": "学习文案风格", "scope": "文案", "createdAt": "2026-08-20T10:00:00"},
                        ],
                        "total": 1,
                    },
                })
            if "/market-skills/favorites" in url:
                return route.fulfill(json={"code": 0, "data": {"records": [], "total": 0}})
            route.fulfill(json={"code": 0, "data": {}})

        page.route("**/api/v1/user/**", handle)
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.evaluate('localStorage.setItem("aichuangzuo_access_token", "mock-token")')
        page.goto(f"{BASE}/console/create/free", wait_until="networkidle")
        try:
            page.locator("#app-loader").wait_for(state="detached", timeout=8000)
        except Exception:
            pass
        page.wait_for_timeout(300)

        # Screenshot free create page hero/header
        page.screenshot(path=f"{SHOTS}/free_create_page_mobile.png")

        # Open skill prompt modal
        page.click('text=选择提示词')
        page.wait_for_timeout(600)

        # Verify skill cards render
        cards = page.query_selector_all('.style-grid .skill-card--compact')
        ok_cards = len(cards) >= 2

        # Verify cards are compact (height should not exceed 150px due to removed min-height)
        heights = [page.evaluate('(el) => el.getBoundingClientRect().height', c) for c in cards]
        ok_compact = all(h < 150 for h in heights) if heights else False

        page.screenshot(path=f"{SHOTS}/free_create_skill_modal_mobile.png")

        for name, ok in [
            ("cards", ok_cards),
            ("compact", ok_compact),
        ]:
            print(("PASS " if ok else "FAIL ") + name)

        browser.close()
        if not all([ok_cards, ok_compact]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

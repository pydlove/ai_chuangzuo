#!/usr/bin/env python3
"""Verify mobile create recommended step 2 viewpoint cards and edit modal."""
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
            if "recommended-creation/session" in url and route.request.method == "GET":
                return route.fulfill(json={
                    "code": 0,
                    "data": {
                        "currentStep": 2,
                        "selectedTopic": {"id": "t1", "title": "35岁转型做自由职业", "risk": "low", "riskLabel": "低风险", "caseCount": 12, "recommendedAngle": "真实转型记录"},
                        "topics": [{"id": "t1", "title": "35岁转型做自由职业", "risk": "low", "riskLabel": "低风险", "caseCount": 12, "recommendedAngle": "真实转型记录"}],
                        "angles": [
                            {"id": "a1", "text": "不要等准备好了再转型，边做边调整才是普通人的路径"},
                            {"id": "a2", "text": "35岁不是职场终点，而是第二曲线的起点"},
                            {"id": "a3", "text": "自由职业第一年，学会比会做什么更重要"},
                        ],
                        "selectedAngles": [
                            {"id": "a1", "text": "不要等准备好了再转型，边做边调整才是普通人的路径"},
                        ],
                        "wordCount": 1500,
                        "prompt": "系统默认",
                        "template": "default"
                    }
                })
            if "platforms" in url:
                return route.fulfill(json={
                    "code": 0,
                    "data": [{"name": "小红书", "isDefault": True, "recommendWords": 1500, "wordCountPresets": [{"count": 800, "label": "短笔记"}, {"count": 1500, "label": "标准"}]}]
                })
            if "export-templates" in url:
                return route.fulfill(json={"code": 0, "data": [{"key": "default", "name": "默认", "desc": "通用", "platform": "general", "accessible": True}]})
            if "/skills/" in url or "skills" in url:
                return route.fulfill(json={"code": 0, "data": [{"name": "系统默认", "prompt": "系统默认", "desc": "系统预设"}]})
            route.fulfill(json={"code": 0, "data": {}})

        page.route("**/api/v1/user/**", handle)
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.evaluate("""() => {
            localStorage.setItem('aichuangzuo_access_token', 'mock-token')
        }""")
        page.goto(f"{BASE}/console/create/recommended", wait_until="networkidle")
        try:
            page.locator("#app-loader").wait_for(state="detached", timeout=8000)
        except Exception:
            pass
        page.wait_for_timeout(300)

        # Verify viewport cards render
        cards = page.query_selector_all(".angle-card")
        ok_cards = len(cards) == 3

        # Verify selected chips render
        chips = page.query_selector_all(".mobile-angle-chip")
        ok_chips = len(chips) == 1

        # Screenshot for visual check
        page.screenshot(path=f"{SHOTS}/create_recommended_step2_mobile.png")

        # Open edit modal on first selected card
        if cards:
            page.click(".angle-card.selected .angle-card-edit-btn")
            page.wait_for_timeout(400)
            modal = page.query_selector(".angle-edit-modal")
            ok_modal = modal is not None
            page.screenshot(path=f"{SHOTS}/create_recommended_step2_edit_modal.png")

            # Edit text and save
            page.fill(".angle-edit-modal textarea", "")
            page.fill(".angle-edit-modal textarea", "这是编辑后的新观点文案")
            page.click(".angle-edit-modal .ant-btn-primary")
            page.wait_for_timeout(400)
            card_text = page.inner_text(".angle-card.selected .angle-card-text").strip()
            chip_text = page.inner_text(".mobile-angle-chip-text").strip()
            ok_edit_save = "这是编辑后的新观点文案" in card_text and "这是编辑后的新观点文案" in chip_text

            # Cancel should restore original text
            page.click(".angle-card.selected .angle-card-edit-btn")
            page.wait_for_timeout(300)
            page.fill(".angle-edit-modal textarea", "")
            page.fill(".angle-edit-modal textarea", "临时文案")
            page.click(".angle-edit-modal .ant-btn:not(.ant-btn-primary)")
            page.wait_for_timeout(400)
            card_text_after_cancel = page.inner_text(".angle-card.selected .angle-card-text").strip()
            ok_edit_cancel = "这是编辑后的新观点文案" in card_text_after_cancel
        else:
            ok_modal = False
            ok_edit_save = False
            ok_edit_cancel = False

        for name, ok in [
            ("cards", ok_cards),
            ("chips", ok_chips),
            ("edit-modal", ok_modal),
            ("edit-save", ok_edit_save),
            ("edit-cancel", ok_edit_cancel),
        ]:
            print(("PASS " if ok else "FAIL ") + name)

        browser.close()
        if not all([ok_cards, ok_chips, ok_modal, ok_edit_save, ok_edit_cancel]):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

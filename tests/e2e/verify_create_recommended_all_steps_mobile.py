#!/usr/bin/env python3
"""Verify mobile create recommended flow across all 5 steps."""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:28586")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"
VIEW = {"width": 390, "height": 844}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport=VIEW, is_mobile=True, has_touch=True)

        session_state = {"step": 1}

        def handle(route):
            url = route.request.url
            method = route.request.method
            if "recommended-creation/session" in url and method == "GET":
                step = session_state["step"]
                data = {
                    "currentStep": step,
                    "selectedTopic": {"id": "t1", "title": "35岁转型做自由职业", "risk": "low", "riskLabel": "低风险", "caseCount": 12, "recommendedAngle": "真实转型记录"},
                    "topics": [{"id": "t1", "title": "35岁转型做自由职业", "risk": "low", "riskLabel": "低风险", "caseCount": 12, "recommendedAngle": "真实转型记录"}],
                    "angles": [
                        {"id": "a1", "text": "不要等准备好了再转型，边做边调整才是普通人的路径"},
                        {"id": "a2", "text": "35岁不是职场终点，而是第二曲线的起点"},
                        {"id": "a3", "text": "自由职业第一年，学会比会做什么更重要"},
                    ],
                    "selectedAngles": [{"id": "a1", "text": "不要等准备好了再转型，边做边调整才是普通人的路径"}] if step >= 2 else [],
                    "wordCount": 1500,
                    "prompt": "系统默认",
                    "template": "default" if step >= 5 else ""
                }
                return route.fulfill(json={"code": 0, "data": data})
            if "recommended-creation/angles" in url and method == "POST":
                return route.fulfill(json={"code": 0, "data": [
                    {"id": "a1", "text": "不要等准备好了再转型，边做边调整才是普通人的路径"},
                    {"id": "a2", "text": "35岁不是职场终点，而是第二曲线的起点"},
                    {"id": "a3", "text": "自由职业第一年，学会比会做什么更重要"},
                ]})
            if "platforms" in url:
                return route.fulfill(json={
                    "code": 0,
                    "data": [{"name": "小红书", "isDefault": True, "recommendWords": 1500, "wordCountPresets": [
                        {"count": 800, "label": "短笔记"}, {"count": 1500, "label": "标准"}, {"count": 2000, "label": "长文"}
                    ]}]
                })
            if "export-templates" in url:
                return route.fulfill(json={"code": 0, "data": [
                    {"key": "default", "name": "默认模板", "desc": "通用", "platform": "general", "accessible": True},
                    {"key": "xiaohongshu", "name": "小红书", "desc": "图文", "platform": "xiaohongshu", "accessible": True},
                ]})
            if "/skills/" in url or "skills" in url:
                return route.fulfill(json={"code": 0, "data": [
                    {"name": "系统默认", "prompt": "系统默认", "desc": "系统预设"}
                ]})
            route.fulfill(json={"code": 0, "data": {}})

        page.route("**/api/v1/user/**", handle)
        page.goto(f"{BASE}/login", wait_until="networkidle")
        page.evaluate("""() => {
            localStorage.setItem('aichuangzuo_access_token', 'mock-token')
        }""")

        results = []

        def wait_loaded():
            try:
                page.locator("#app-loader").wait_for(state="detached", timeout=8000)
            except Exception:
                pass
            page.wait_for_timeout(300)

        # Step 1
        session_state["step"] = 1
        page.goto(f"{BASE}/console/create/recommended", wait_until="networkidle")
        wait_loaded()
        topics = page.query_selector_all(".flow-panel--topics .topic-option")
        ok_step1 = len(topics) > 0
        page.screenshot(path=f"{SHOTS}/create_recommended_step1_mobile.png")
        results.append(("step1-topics", ok_step1))

        # Step 2
        session_state["step"] = 2
        page.goto(f"{BASE}/console/create/recommended", wait_until="networkidle")
        wait_loaded()
        cards = page.query_selector_all(".angle-card")
        chips = page.query_selector_all(".mobile-angle-chip")
        ok_step2 = len(cards) == 3 and len(chips) == 1
        page.screenshot(path=f"{SHOTS}/create_recommended_step2_mobile.png")
        results.append(("step2-angles", ok_step2))

        # Step 3
        session_state["step"] = 3
        page.goto(f"{BASE}/console/create/recommended", wait_until="networkidle")
        wait_loaded()
        presets = page.query_selector_all(".flow-panel--words .word-presets .ant-btn")
        slider = page.query_selector(".flow-panel--words .ant-slider")
        ok_step3 = len(presets) > 0 and slider is not None
        page.screenshot(path=f"{SHOTS}/create_recommended_step3_mobile.png")
        results.append(("step3-words", ok_step3))

        # Step 4
        session_state["step"] = 4
        page.goto(f"{BASE}/console/create/recommended", wait_until="networkidle")
        wait_loaded()
        prompt_cards = page.query_selector_all(".flow-panel--prompts .prompt-grid .skill-card")
        ok_step4 = len(prompt_cards) > 0
        page.screenshot(path=f"{SHOTS}/create_recommended_step4_mobile.png")
        results.append(("step4-prompts", ok_step4))

        # Step 5
        session_state["step"] = 5
        page.goto(f"{BASE}/console/create/recommended", wait_until="networkidle")
        wait_loaded()
        preview = page.query_selector(".flow-panel--templates .template-preview-pane")
        template_rows = page.query_selector_all(".flow-panel--templates .template-row")
        ok_step5 = preview is not None and len(template_rows) > 0
        page.screenshot(path=f"{SHOTS}/create_recommended_step5_mobile.png")
        results.append(("step5-templates", ok_step5))

        for name, ok in results:
            print(("PASS " if ok else "FAIL ") + name)

        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

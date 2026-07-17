"""验证：主题步骤加描述输入框 + 提交时带上 description。"""
import os
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        submit_payloads = []

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

        def route_submit(r):
            if r.request.method == "POST":
                try: submit_payloads.append(r.request.post_data_json)
                except: pass
            r.fulfill(json={"code": 0, "data": {"id": 100, "status": 1, "progressPct": 0}})
        page.route("**/api/v1/user/generation-tasks", route_submit)
        # 单任务查询：第一次返回 progressPct:50，第二次返回 status:2 完成
        # 注意：先注册更宽泛的，避免后面更具体的被拦截不到
        page.route("**/api/v1/user/generation-tasks/**",
                   lambda r: r.fulfill(json={"code": 0, "data": {"id": 100, "status": 2, "progressPct": 100}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        # ===== 第一遍：完整走一遍 hero → topic → platform → style → template → 生成 =====
        page.fill(".hero-input", "AI 工具评测")
        page.click(".hero-send")
        page.wait_for_timeout(500)

        # 主题步骤应出现
        topic_input_visible = page.is_visible(".topic-input")
        req_input = page.query_selector(".topic-requirement")
        ok_textarea_exists = req_input is not None
        ok_placeholder = ok_textarea_exists and "观点" in (req_input.get_attribute("placeholder") or "")
        title_in_topic = page.input_value(".topic-input")

        # 填描述
        page.fill(".topic-requirement", "从用户实际使用场景出发，对比 3 款主流 AI 写作工具")
        page.click(".topic-send")
        page.wait_for_timeout(400)

        # 走到确认卡
        page.click(".quick-option:has-text('公众号')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('公众号深度文')")
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # 确认卡应显示描述
        confirm_text = page.inner_text(".confirm-card")
        ok_requirement_in_confirm = "从用户实际使用场景出发" in confirm_text

        page.screenshot(path=f"{SHOTS}/diag_requirement.png")

        # 点开始生成（在 改主题 测试前）
        page.click(".confirm-generate")
        # 等待 poll 完成：poll 间隔 3s 一次，两次后 status 变 2（result 卡）
        page.wait_for_timeout(7500)

        # 提交 payload 应包含 description
        ok_payload = bool(submit_payloads) and any(
            p.get("title") == "AI 工具评测" and "用户实际使用场景" in (p.get("description") or "")
            for p in submit_payloads
        )

        # ===== 第二遍：从结果页点"再写一篇" → 验证 textarea 仍存在并可填 =====
        page.click(".confirm-edit:has-text('再写一篇')")
        page.wait_for_timeout(500)

        ok_restart_shows_topic = page.is_visible(".topic-input")
        page.fill(".topic-input", "第二个主题")
        page.fill(".topic-requirement", "这是第二段描述")
        ok_second_input_works = page.input_value(".topic-requirement") == "这是第二段描述"

        results = [
            ("topic-step-after-hero", topic_input_visible),
            ("title-prefilled", title_in_topic == "AI 工具评测"),
            ("textarea-exists", ok_textarea_exists),
            ("placeholder-hint", ok_placeholder),
            ("requirement-in-confirm", ok_requirement_in_confirm),
            ("payload-has-description", ok_payload),
            ("restart-keeps-textarea", ok_restart_shows_topic),
            ("second-input-works", ok_second_input_works),
        ]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
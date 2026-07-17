"""验证：点灵感话题后进入主题步骤（标题/描述都回填），用户点发送才走。"""
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
        # 灵感接口：返回带 summary 的标题
        page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"id": 1, "title": "AI 写作工具横评", "summary": "对比 5 款主流 AI 写作工具的优缺点和适用场景"},
            {"id": 2, "title": "深度学习入门指南", "summary": "用通俗语言讲清楚深度学习的核心概念"},
        ]}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"templateKey": "wechat", "name": "公众号深度文", "description": "d", "platform": "wechat",
             "bgColor": "#fff", "textColor": "#1a1a1a", "visualStyle": {"bg":"#fff"}}
        ]}))
        page.route("**/api/v1/user/generation-tasks", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        # ===== 路径 1：手动敲字 → 主题步骤 → 发送 =====
        page.fill(".hero-input", "手动敲的主题")
        page.click(".hero-send")
        page.wait_for_timeout(400)
        ok_manual_topic = page.is_visible(".topic-input")
        ok_manual_title = page.input_value(".topic-input") == "手动敲的主题"

        page.click(".topic-send")
        page.wait_for_timeout(400)
        ok_manual_to_platform = page.is_visible(".quick-option")

        # 走完确认 + 生成（验证手动路径完整）
        page.click(".quick-option:has-text('公众号')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('公众号深度文')")
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # ===== 路径 2：从结果点"再写一篇" → 这次走灵感 =====
        # 先把当前生成完成 → 点再写一篇
        page.route("**/api/v1/user/generation-tasks/**",
                   lambda r: r.fulfill(json={"code": 0, "data": {"id": 100, "status": 2, "progressPct": 100}}))
        # 监听 POST 提交
        def route_post(r):
            if r.request.method == "POST":
                try: submit_payloads.append(r.request.post_data_json)
                except: pass
            r.fulfill(json={"code": 0, "data": {"id": 100, "status": 1, "progressPct": 0}})
        page.route("**/api/v1/user/generation-tasks", route_post)

        page.click(".confirm-generate")
        page.wait_for_timeout(7500)
        # 现在应该看到"再写一篇"
        page.click(".confirm-edit:has-text('再写一篇')")
        page.wait_for_timeout(500)

        # 展开灵感
        page.click(".inspire-btn")
        page.wait_for_timeout(2500)  # 等流式展开 + loading + 标题逐个出现

        # 第一个灵感话题
        page.click(".suggestion-title:has-text('AI 写作工具横评')")
        page.wait_for_timeout(500)

        # 点灵感后应进主题步骤（不再直接跳平台）
        ok_inspire_topic_visible = page.is_visible(".topic-input")
        ok_inspire_title_prefilled = page.input_value(".topic-input") == "AI 写作工具横评"
        ok_inspire_summary_prefilled = page.input_value(".topic-requirement") == "对比 5 款主流 AI 写作工具的优缺点和适用场景"

        # 此时不应有平台选项出现（用户在主题步骤）
        ok_no_platform_yet = not page.is_visible(".quick-option:has-text('公众号')")

        # 用户点发送 → 进平台
        page.click(".topic-send")
        page.wait_for_timeout(400)
        ok_inspire_to_platform = page.is_visible(".quick-option:has-text('公众号')")

        page.screenshot(path=f"{SHOTS}/diag_inspire_path.png", full_page=True)

        # ===== 路径 3：点灵感后，用户改描述再发送 =====
        page.click(".confirm-edit:has-text('再写一篇')") if page.is_visible(".confirm-edit:has-text('再写一篇')") else None
        page.wait_for_timeout(300)
        # 没在确认卡就手工回到主题步骤（hero 已经空，直接从主题步骤开始）
        if not page.is_visible(".inspire-btn"):
            # 已是主题步骤状态
            pass

        # 也可测试：点灵感 → 修改 description → 发送 → payload 应该是改后的
        # 走完一遍
        page.click(".inspire-btn") if page.is_visible(".inspire-btn") else None
        page.wait_for_timeout(2500)
        page.click(".suggestion-title:has-text('深度学习入门指南')")
        page.wait_for_timeout(400)
        # 改一下描述
        page.fill(".topic-requirement", "改写后的描述：加上图示")
        page.click(".topic-send")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('公众号')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('公众号深度文')")
        page.click(".quick-confirm")
        page.wait_for_timeout(500)
        page.click(".confirm-generate")
        page.wait_for_timeout(800)

        ok_payload_modified = bool(submit_payloads) and any(
            "改写后的描述" in (p.get("description") or "")
            for p in submit_payloads
        )

        results = [
            ("manual-shows-topic", ok_manual_topic),
            ("manual-title-prefilled", ok_manual_title),
            ("manual-to-platform", ok_manual_to_platform),
            ("inspire-shows-topic", ok_inspire_topic_visible),
            ("inspire-title-prefilled", ok_inspire_title_prefilled),
            ("inspire-summary-prefilled", ok_inspire_summary_prefilled),
            ("inspire-no-platform-yet", ok_no_platform_yet),
            ("inspire-to-platform", ok_inspire_to_platform),
            ("payload-has-modified-desc", ok_payload_modified),
        ]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
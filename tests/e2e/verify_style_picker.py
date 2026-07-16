#!/usr/bin/env python3
"""引导模式风格选择：合并 4 来源（系统/我的/学习/收藏）+ 完整 prompt 预览。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "planName": "专业版",
                                "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        # 4 类风格 mock
        page.route("**/api/v1/user/styles**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"bizNo": "M1", "styleName": "我的小红书风", "prompt": "你是小红书博主：\n- 短段落多 emoji\n- 强种草调性\n- 必带 5 个 hashtag", "scope": "小红书种草", "useCount": 12, "sourceType": 1}]}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"bizNo": "S1", "name": "轻松口语", "description": "像朋友聊天", "promptSummary": "口语化短句", "prompt": "你是朋友聊天风格：\n- 口语化、避免书面语\n- 短句优先\n- 加 emoji 让文字活泼", "scope": "通用"},
                {"bizNo": "S2", "name": "专业严谨", "description": "正式专业", "promptSummary": "术语准确", "prompt": "你是严谨专家：\n- 用专业术语\n- 数据说话\n- 结论先行", "scope": "通用"}]}))
        page.route("**/api/v1/user/market-styles**", lambda r: r.fulfill(json={
            "code": 0, "data": [
                {"id": 1001, "name": "干货导师", "prompt": "你是干货博主：\n- 结构清晰\n- 步骤详细\n- 必有 checklist", "scope": "教程类", "status": "approved"}]}))
        # favorites 通过 localStorage 注入
        page.add_init_script("""
            localStorage.setItem('aichuangzuo_favorite_styles', JSON.stringify([1001]))
        """)
        page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"templateKey": "wechat-article", "name": "公众号深度文", "platform": "wechat"}]}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1500)

        # 走到风格步骤
        page.fill(".hero-input", "测试主题")
        page.keyboard.press("Enter")
        page.wait_for_timeout(400)
        page.click(".quick-option:has-text('公众号')")
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # 风格问题应出现
        ok_style_q = page.query_selector("text=想要什么风格？") is not None

        # 选项数 = 4 个（1 我的 + 2 系统 + 1 收藏；学习为 0）
        # 我自己的会排在最前面
        options = page.query_selector_all(".quick-option")
        labels = [o.inner_text() for o in options]
        ok_my_first = len(labels) > 0 and "我的小红书风" in labels[0]
        ok_my_in = any("我的小红书风" in l for l in labels)
        ok_system_in = any("轻松口语" in l for l in labels) and any("专业严谨" in l for l in labels)
        ok_favorite_in = any("干货导师" in l for l in labels)

        # 点"我的小红书风" → effect-card 应显示完整 prompt
        page.click(".quick-option:has-text('我的小红书风')")
        page.wait_for_timeout(300)
        tag_text = page.inner_text(".style-tag") if page.query_selector(".style-tag") else ""
        ok_my_tag = "我的" in tag_text
        # prompt 内容应包含多行（不止 summary）
        prompt_text = page.inner_text(".effect-prompt-full") if page.query_selector(".effect-prompt-full") else ""
        ok_prompt_full = "小红书博主" in prompt_text and "hashtag" in prompt_text

        page.screenshot(path=f"{SHOTS}/style_picker_my.png")

        # 确认 → 模板步骤
        page.click(".quick-confirm")
        page.wait_for_timeout(400)

        # 点收藏的风格应该也能选
        # 再次回到风格：用"改风格"
        # 先走完模板
        page.click(".quick-option:has-text('公众号深度文')")
        page.wait_for_timeout(200)
        page.click(".quick-confirm")
        page.wait_for_timeout(500)

        # 在确认卡里点"改风格"→ 收藏的风格应仍可见
        page.click(".confirm-edit:has-text('改风格')")
        page.wait_for_timeout(400)
        options2 = page.query_selector_all(".quick-option")
        labels2 = [o.inner_text() for o in options2]
        ok_favorite_still = any("干货导师" in l for l in labels2)

        page.click(".quick-option:has-text('干货导师')")
        page.wait_for_timeout(300)
        fav_tag_text = page.inner_text(".style-tag") if page.query_selector(".style-tag") else ""
        ok_favorite_tag = "收藏" in fav_tag_text

        page.screenshot(path=f"{SHOTS}/style_picker_favorite.png")

        results = [("style-question", ok_style_q),
                   ("my-style-first", ok_my_first),
                   ("my-in-list", ok_my_in),
                   ("system-in-list", ok_system_in),
                   ("favorite-in-list", ok_favorite_in),
                   ("my-tag", ok_my_tag),
                   ("prompt-full", ok_prompt_full),
                   ("favorite-still", ok_favorite_still),
                   ("favorite-tag", ok_favorite_tag)]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()
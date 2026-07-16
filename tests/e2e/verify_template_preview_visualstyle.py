"""验证模板预览：选择小红书爆款 → 预览应使用红色（#ff2442），不能是公众号黑色。"""
import os, json
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

# 两种格式的同一份视觉样式
VISUAL_OBJ = {
    "bg": "#fff", "font": "-apple-system", "titleColor": "#ff2442", "titleSize": "22px",
    "metaColor": "#8c8c8c", "metaBorder": "#ffd1d9", "bodyColor": "#262626",
    "bodySize": "14px", "bodyLine": "1.8", "headingColor": "#ff2442",
    "headingSize": "15px", "headingBorder": "none", "headingPl": "0",
    "calloutVariant": "pill"
}
VISUAL_STR = json.dumps(VISUAL_OBJ, ensure_ascii=False)


def run(scenario, tpl):
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        page.route("**/api/v1/user/benefits/me", lambda r: r.fulfill(json={
            "code": 0, "data": {"planKey": "pro", "benefits": [{"code": "ai_article_quota", "value": "50", "remaining": 12}]}}))
        page.route("**/api/v1/user/styles*", lambda r: r.fulfill(json={"code": 0, "data": [
            {"bizNo": "S1", "styleName": "轻松口语", "prompt": "口语", "scope": "通用", "useCount": 1, "sourceType": 1}
        ]}))
        page.route("**/api/v1/user/styles/system-styles**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"bizNo": "S1", "name": "轻松口语", "prompt": "你是朋友", "scope": "通用"}
        ]}))
        page.route("**/api/v1/user/market-styles**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/topics/**", lambda r: r.fulfill(json={"code": 0, "data": []}))
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": tpl}))
        page.route("**/api/v1/user/generation-tasks**", lambda r: r.fulfill(json={"code": 0, "data": {"list": [], "total": 0}}))

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        # 走到模板步骤
        page.fill(".hero-input", "测试")
        page.keyboard.press("Enter")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('公众号')")
        page.click(".quick-confirm")
        page.wait_for_timeout(300)
        page.click(".quick-option:has-text('轻松口语')")
        page.click(".quick-confirm")
        page.wait_for_timeout(400)

        # 选小红书爆款 → 查看完整预览
        page.click(".quick-option:has-text('小红书爆款')")
        page.wait_for_timeout(200)
        page.click(".template-preview-btn")
        page.wait_for_timeout(500)

        # 抓预览 HTML
        html = page.inner_html(".template-preview-pane")
        ok_red_title = "color: #ff2442" in html
        ok_pink_meta = "#ffd1d9" in html
        ok_no_green_callout = "#07c160" not in html  # 公众号绿色 callout 不应出现
        ok_pill_present = "core要点" in html or "核心要点" in html

        page.screenshot(path=f"{SHOTS}/diag_fix_{scenario}.png")

        results = [("red-title", ok_red_title), ("pink-meta-border", ok_pink_meta),
                   ("no-green-callout", ok_no_green_callout), ("pill-callout", ok_pill_present)]
        for n, ok in results:
            print(f"  [{scenario}] {('PASS' if ok else 'FAIL')} {n}")
        browser.close()
        return all(ok for _, ok in results)


# 场景 1：visualStyle 是对象（后端正确返回时）
tpl_obj = [
    {"templateKey": "wechat", "name": "公众号标准模板", "description": "深度长文", "platform": "wechat",
     "bgColor": "#fff", "textColor": "#1a1a1a", "visualStyle": VISUAL_OBJ, "signatureText": "— 完 —",
     "signaturePosition": "end", "sortOrder": 1},
    {"templateKey": "xiaohongshu", "name": "小红书爆款模板", "description": "种草安利", "platform": "xiaohongshu",
     "bgColor": "#fff", "textColor": "#ff2442", "visualStyle": VISUAL_OBJ, "signatureText": "#小红书",
     "signaturePosition": "end", "sortOrder": 6}
]
# 场景 2：visualStyle 是字符串（防御性测试 — 后端偶发返回字符串时也应正确）
tpl_str = [
    {"templateKey": "wechat", "name": "公众号标准模板", "description": "深度长文", "platform": "wechat",
     "bgColor": "#fff", "textColor": "#1a1a1a", "visualStyle": VISUAL_STR, "signatureText": "— 完 —",
     "signaturePosition": "end", "sortOrder": 1},
    {"templateKey": "xiaohongshu", "name": "小红书爆款模板", "description": "种草安利", "platform": "xiaohongshu",
     "bgColor": "#fff", "textColor": "#ff2442", "visualStyle": VISUAL_STR, "signatureText": "#小红书",
     "signaturePosition": "end", "sortOrder": 6}
]

print("=== Scenario 1: visualStyle as Object ===")
ok1 = run("object", tpl_obj)
print("=== Scenario 2: visualStyle as String ===")
ok2 = run("string", tpl_str)
if not (ok1 and ok2):
    raise SystemExit("FAILED")
print("ALL PASS")

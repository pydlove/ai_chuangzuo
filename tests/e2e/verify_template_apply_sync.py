"""验证：模板步骤点查看完整预览 → 弹框里改选 → 应用 → 外面 chip 和 effect-card 同步更新。"""
import os, json
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        VISUAL_WECHAT = {"bg":"#fff","font":"-apple-system","titleColor":"#1a1a1a","titleSize":"22px","metaColor":"#8c8c8c","metaBorder":"#eee","bodyColor":"#262626","bodySize":"14px","bodyLine":"1.85","headingColor":"#1a1a1a","headingSize":"16px","headingBorder":"none","headingPl":"0","calloutBg":"#f6ffed","calloutBorder":"4px solid #07c160","calloutColor":"#262626"}
        VISUAL_XHS = {"bg":"#fff","font":"-apple-system","titleColor":"#ff2442","titleSize":"22px","metaColor":"#8c8c8c","metaBorder":"#ffd1d9","bodyColor":"#262626","bodySize":"14px","bodyLine":"1.8","headingColor":"#ff2442","headingSize":"15px","headingBorder":"none","headingPl":"0","calloutVariant":"pill"}

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
        page.route("**/api/v1/user/export-templates**", lambda r: r.fulfill(json={"code": 0, "data": [
            {"templateKey": "wechat", "name": "公众号标准模板", "description": "深度长文", "platform": "wechat",
             "bgColor": "#fff", "textColor": "#1a1a1a", "visualStyle": VISUAL_WECHAT,
             "signatureText": "— 完 —", "signaturePosition": "end", "sortOrder": 1},
            {"templateKey": "xiaohongshu", "name": "小红书爆款模板", "description": "种草安利", "platform": "xiaohongshu",
             "bgColor": "#fff", "textColor": "#ff2442", "visualStyle": VISUAL_XHS,
             "signatureText": "#小红书", "signaturePosition": "end", "sortOrder": 6}
        ]}))
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

        # 选公众号模板
        page.click(".quick-option:has-text('公众号标准模板')")
        page.wait_for_timeout(200)
        # effect-card 应显示「公众号标准模板」
        effect1 = page.inner_text(".effect-card")
        ok_eff_wechat = "公众号标准模板" in effect1
        # chip 上 selected 应是公众号
        sel1 = page.query_selector(".quick-option.selected")
        sel1_text = sel1.inner_text() if sel1 else ""
        ok_chip_wechat = "公众号" in sel1_text

        # 点击「查看完整预览」
        page.click(".template-preview-btn")
        page.wait_for_timeout(500)

        # 弹框打开，右侧选中公众号
        modal_sel1 = page.query_selector(".template-row.selected .template-row-name")
        modal_sel1_text = modal_sel1.inner_text() if modal_sel1 else ""
        ok_modal_wechat = "公众号" in modal_sel1_text

        # 在弹框里点「小红书爆款模板」
        page.click(".template-row:has-text('小红书爆款模板')")
        page.wait_for_timeout(300)

        # 应用
        page.click(".template-apply-btn")
        page.wait_for_timeout(1500)  # 等弹框动画结束

        # 弹框应已关闭（ant-modal 元素保留在 DOM，但 display:none，用 is_visible 判断）
        modal_still_open = page.is_visible(".template-modal")

        # 外面：effect-card 应变成「小红书爆款模板」
        effect2 = page.inner_text(".effect-card")
        ok_eff_xhs = "小红书爆款模板" in effect2

        # 外面：chip 选中态应变成小红书爆款
        sel2 = page.query_selector(".quick-option.selected")
        sel2_text = sel2.inner_text() if sel2 else ""
        ok_chip_xhs = "小红书" in sel2_text

        page.screenshot(path=f"{SHOTS}/diag_apply_sync.png")

        results = [
            ("eff-wechat-before", ok_eff_wechat),
            ("chip-wechat-before", ok_chip_wechat),
            ("modal-wechat-on-open", ok_modal_wechat),
            ("modal-closed-after-apply", not modal_still_open),
            ("eff-xhs-after-apply", ok_eff_xhs),
            ("chip-xhs-after-apply", ok_chip_xhs),
        ]
        for n, ok in results:
            print(("PASS " if ok else "FAIL ") + n)
        browser.close()
        if not all(ok for _, ok in results):
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

#!/usr/bin/env python3
"""新人首冲优惠前端展示验证：年包弹框 -> 邀请有礼弹框 -> 横幅/卡片 -> 订阅金额。"""
import os
from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE", "http://localhost:22345")
SHOTS = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

OFFER = {
    "eligible": True,
    "planKey": "flagship",
    "planName": "旗舰版",
    "cycle": "year",
    "originalPrice": 1198.8,
    "regularPrice": 839.2,
    "finalPrice": 671.36,
    "savings": 527.44,
    "benefits": [
        "300 篇/月",
        "导出 Word",
        "复制正文",
        "AI 选题灵感",
        "AI 标题优化",
        "在线编辑",
        "自定义 + 记忆",
        "SEO 关键词建议",
        "全部 + 自定义",
        "100 张/月",
        "批量生成/改写",
        "批量导出",
        "永久",
        "极速",
        "队列最多 10 个任务",
        "每月可发布 2 个风格",
        "每月可学习 2 次 AI 风格分析",
    ],
}

CATALOG = {
    "code": 0,
    "data": {
        "plans": [
            {
                "key": "basic",
                "name": "基础版",
                "monthly": {"current": 29.9, "articles": "30 篇 AI 文章/月"},
                "quarter": {"current": 80.7, "original": 89.7, "articles": "90 篇 AI 文章/季"},
                "year": {"current": 251.2, "original": 358.8, "articles": "360 篇 AI 文章/年", "savings": 107.6},
                "features": [],
            },
            {
                "key": "pro",
                "name": "专业版",
                "recommended": True,
                "monthly": {"current": 59.9, "articles": "100 篇 AI 文章/月"},
                "quarter": {"current": 161.7, "original": 179.7, "articles": "300 篇 AI 文章/季"},
                "year": {"current": 503.2, "original": 718.8, "articles": "1200 篇 AI 文章/年", "savings": 215.6},
                "features": [],
            },
            {
                "key": "flagship",
                "name": "旗舰版",
                "monthly": {"current": 99.9, "articles": "300 篇 AI 文章/月"},
                "quarter": {"current": 269.7, "original": 299.7, "articles": "900 篇 AI 文章/季"},
                "year": {"current": 839.2, "original": 1198.8, "articles": "3600 篇 AI 文章/年", "savings": 359.6},
                "features": [],
            },
        ],
        "compareRows": [],
    },
}


def setup_auth(page):
    page.add_init_script("""
        localStorage.setItem('aichuangzuo_access_token', 'fake-jwt-for-newcomer-test');
        localStorage.removeItem('aichuangzuo_newcomer_banner_dismissed');
        localStorage.removeItem('aichuangzuo_invite_modal_dismissed');
        localStorage.removeItem('aichuangzuo_membership');
    """)


def mock_api(page, pattern, json_body):
    page.route(pattern, lambda r: r.fulfill(json=json_body))


def mock_console_apis(page):
    def fallback(route):
        url = route.request.url
        if url.startswith(f"{BASE}/api/v1/user/"):
            route.fulfill(json={"code": 0, "data": {}})
        else:
            route.fallback()
    page.route("**/api/v1/user/**", fallback)

    mock_api(page, "**/api/v1/user/messages", {"code": 0, "data": []})
    mock_api(page, "**/api/v1/user/membership/me", {"code": 0, "data": {"hasMembership": False}})
    mock_api(page, "**/api/v1/user/me", {"code": 0, "data": {"userId": 1, "nickname": "新用户", "email": "test@example.com"}})
    mock_api(page, "**/api/v1/user/benefits/me", {"code": 0, "data": {"planKey": "free", "planName": "免费版", "benefits": []}})
    mock_api(page, "**/api/v1/user/articles/monthly-count", {"code": 0, "data": 0})
    mock_api(page, "**/api/v1/user/articles**", {"code": 0, "data": {"list": [], "total": 0}})
    mock_api(page, "**/api/v1/user/plans/newcomer-offer", {"code": 0, "data": OFFER})
    mock_api(page, "**/api/v1/user/plans", CATALOG)
    mock_api(page, "**/api/v1/user/generation-tasks**", {"code": 0, "data": {"list": [], "total": 0}})
    mock_api(page, "**/api/v1/user/topics/**", {"code": 0, "data": []})
    mock_api(page, "**/api/v1/user/styles/system-styles**", {"code": 0, "data": []})
    mock_api(page, "**/api/v1/user/styles**", {"code": 0, "data": []})
    mock_api(page, "**/api/v1/user/market-styles**", {"code": 0, "data": []})
    mock_api(page, "**/api/v1/user/export-templates**", {"code": 0, "data": []})


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        results = []

        # ---------- 场景 1：弹框顺序 + 横幅/卡片 + 订阅 ----------
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        setup_auth(page)
        mock_console_apis(page)

        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(600)

        newcomer_modal = page.locator(".newcomer-modal")
        ok_newcomer_auto = newcomer_modal.is_visible()
        results.append(("newcomer-modal-auto", ok_newcomer_auto))

        modal_text = newcomer_modal.inner_text() if ok_newcomer_auto else ""
        ok_newcomer_benefits = all(b in modal_text for b in OFFER["benefits"])
        results.append(("newcomer-modal-benefits", ok_newcomer_benefits))

        # 关闭年包弹框 -> 邀请有礼弹框
        newcomer_modal.locator(".newcomer-modal-later").click()
        page.wait_for_timeout(400)
        invite_modal = page.locator(".invite-modal")
        ok_invite_auto = invite_modal.is_visible()
        results.append(("invite-modal-auto", ok_invite_auto))

        invite_modal.locator(".ant-modal-close").click()
        page.wait_for_timeout(400)

        banner = page.locator(".newcomer-banner")
        page.wait_for_selector(".newcomer-banner", timeout=5000)
        ok_banner = banner.is_visible()
        banner_text = banner.inner_text() if ok_banner else ""
        ok_banner_price = "671.36" in banner_text and "1198.8" in banner_text
        results.append(("newcomer-banner-visible", ok_banner))
        results.append(("newcomer-banner-prices", ok_banner_price))
        page.screenshot(path=f"{SHOTS}/newcomer_banner.png")

        banner.locator(".newcomer-banner-text").click()
        page.wait_for_url(lambda url: "/pricing" in url and "newcomer=1" in url)
        page.wait_for_timeout(600)
        card = page.locator(".newcomer-offer-card")
        ok_card = card.is_visible()
        card_text = card.inner_text() if ok_card else ""
        ok_card_prices = (
            "671.36" in card_text and
            "839.2" in card_text and
            "1198.8" in card_text and
            "527.44" in card_text
        )
        results.append(("newcomer-card-visible", ok_card))
        results.append(("newcomer-card-prices", ok_card_prices))
        page.screenshot(path=f"{SHOTS}/newcomer_card.png")

        subscribe_called = {"payload": None}

        def handle_subscribe(route):
            subscribe_called["payload"] = route.request.post_data_json
            route.fulfill(json={"code": 0, "data": {
                "orderNo": "SUB000000000001",
                "level": "flagship",
                "days": 365,
                "expiresAt": "2027-07-20",
                "inviterRewarded": False,
                "rewardAmount": 0,
            }})

        page.route("**/api/v1/user/membership/subscribe", handle_subscribe)
        card.locator(".newcomer-offer-btn").click()
        page.wait_for_timeout(400)
        subscribe_modal = page.locator(".subscribe-modal")
        ok_subscribe_modal = subscribe_modal.is_visible()
        results.append(("subscribe-modal-open", ok_subscribe_modal))
        page.screenshot(path=f"{SHOTS}/newcomer_subscribe_modal.png")

        page.locator(".subscribe-modal input").fill("123456")
        page.locator(".subscribe-modal .ant-btn-primary").click()
        page.wait_for_timeout(800)
        ok_navigated = "/console/create" in page.url
        ok_amount = subscribe_called["payload"] and subscribe_called["payload"].get("amount") == 671.36
        results.append(("subscribe-amount", ok_amount))
        results.append(("subscribe-navigated", ok_navigated))
        page.screenshot(path=f"{SHOTS}/newcomer_subscribed.png")
        page.close()

        # ---------- 场景 2：两个弹框的“不再弹出”记忆 ----------
        page2 = browser.new_page(viewport={"width": 1440, "height": 900})
        setup_auth(page2)
        mock_console_apis(page2)

        page2.goto(f"{BASE}/console/create", wait_until="networkidle")
        page2.wait_for_timeout(600)

        newcomer_modal2 = page2.locator(".newcomer-modal")
        ok_newcomer_auto2 = newcomer_modal2.is_visible()
        results.append(("newcomer-modal-auto-2", ok_newcomer_auto2))
        if ok_newcomer_auto2:
            page2.locator(".newcomer-modal .ant-checkbox-input").check()
            newcomer_modal2.locator(".newcomer-modal-later").click()
        page2.wait_for_timeout(400)

        invite_modal2 = page2.locator(".invite-modal")
        ok_invite_auto2 = invite_modal2.is_visible()
        results.append(("invite-modal-auto-2", ok_invite_auto2))
        if ok_invite_auto2:
            page2.locator(".invite-modal-auto-footer .ant-checkbox-input").check()
            invite_modal2.locator(".ant-modal-close").click()
        page2.wait_for_timeout(400)

        page2.reload(wait_until="networkidle")
        page2.wait_for_timeout(600)
        ok_newcomer_not_again = not page2.locator(".newcomer-modal").is_visible()
        ok_invite_not_again = not page2.locator(".invite-modal").is_visible()
        results.append(("newcomer-modal-not-again", ok_newcomer_not_again))
        results.append(("invite-modal-not-again", ok_invite_not_again))
        page2.screenshot(path=f"{SHOTS}/newcomer_modals_dismissed.png")
        page2.close()

        browser.close()

        all_pass = True
        for name, ok in results:
            status = "PASS" if ok else "FAIL"
            print(f"{status} {name}")
            if not ok:
                all_pass = False
        if not all_pass:
            raise SystemExit("FAILED")
        print("ALL PASS")


main()

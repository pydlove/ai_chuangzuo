from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:22345/"


def test_home_marketing():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 900})
        page = desktop.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 1. Hero 重写文案
        expect(page.locator(".hero-title")).to_have_text("会增值的自媒体账号,从第一篇文章开始")
        expect(page.locator(".hero-btn")).to_be_visible()
        expect(page.locator(".hero-btn-secondary")).to_have_attribute("href", "/guide")

        # 2. 数据区 4 个数字与标签
        nums = page.locator(".stat-num").all_text_inner_texts()
        assert any("360 万" in n for n in nums), f"未找到 360 万 数字: {nums}"
        assert any("12 万" in n for n in nums), f"未找到 12 万 数字: {nums}"
        assert any("6" in n for n in nums), f"未找到 6 数字: {nums}"
        assert any("3" in n for n in nums), f"未找到 3 数字: {nums}"

        # 3. 特色功能 6 张卡
        feature_cards = page.locator(".features .feature-card")
        expect(feature_cards).to_have_count(6)

        # 4. 收益玩法矩阵 (NEW 板块)
        expect(page.locator(".earnings-section")).to_be_visible()
        earnings_cards = page.locator(".earnings-section .feature-card")
        expect(earnings_cards).to_have_count(4)
        expect(page.locator(".earnings-section .section-cta")).to_have_attribute("href", "/guide")

        # 5. 三步化简
        expect(page.locator(".steps-title")).to_have_text("3 步起一个会增值的账号")
        step_items = page.locator(".steps-list .step-item")
        expect(step_items).to_have_count(3)

        # 6. 终 CTA 主+次双按钮
        expect(page.locator(".cta-section .cta-title")).to_have_text("现在起号,3 个月后看复利")
        expect(page.locator(".cta-section .hero-btn")).to_be_visible()
        expect(page.locator(".cta-section .hero-btn-secondary")).to_have_attribute("href", "/guide")

        # 截图
        page.screenshot(path="tests/e2e/screenshots/home_marketing_1280.png", full_page=True)

        desktop.close()

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 7. 手机端:6 张特色卡单列、收益玩法 4 张单列
        expect(feature_cards.first).to_be_visible()
        expect(earnings_cards.first).to_be_visible()

        # 8. 手机端无内容溢出(横向滚动)
        body_width = page.evaluate("document.documentElement.scrollWidth")
        viewport_w = 375
        assert body_width <= viewport_w + 2, f"横向溢出: body={body_width} viewport={viewport_w}"

        # 9. 终 CTA 在手机端主按钮可见
        expect(page.locator(".cta-section .hero-btn")).to_be_visible()

        page.screenshot(path="tests/e2e/screenshots/home_marketing_375.png", full_page=True)
        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_home_marketing()
    print("Home marketing verification passed.")

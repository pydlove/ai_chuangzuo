from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:22345"
PRICING_URL = f"{BASE_URL}/pricing"


def test_pricing_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 900})
        page = desktop.new_page()
        page.goto(PRICING_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 1. 标题文案
        expect(page.locator(".pricing-title")).to_have_text("每天 3 分钟，AI 帮你写完一篇文章")

        # 2. NavBar 顶部导航
        expect(page.locator(".navbar .navbar-cta")).to_be_visible()
        expect(page.locator(".navbar .navbar-cta")).to_have_attribute("href", "/login")
        expect(page.locator(".navbar-link-desktop").first).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).not_to_be_visible()

        # 3. 3 张定价卡
        expect(page.locator(".pricing-card")).to_have_count(3)
        cards = page.locator(".pricing-card").all()
        for c in cards:
            expect(c).to_be_visible()

        # 4. 对比表行数 ≥ 10
        rows = page.locator(".compare-table tbody tr").count()
        assert rows >= 10, f"对比表行数应 ≥ 10: 实际 {rows}"

        # 5. 周期切换按钮 3 个
        expect(page.locator(".toggle-btn")).to_have_count(3)
        expect(page.locator(".toggle-btn.active")).to_have_text("月度")

        # 6. 切换到「年度」周期,价格更新(专业版应显示 ¥503.2)
        page.locator(".toggle-btn:has-text('年度')").click()
        page.wait_for_timeout(300)
        pro_card = page.locator(".pricing-card.recommended")
        expect(pro_card.locator(".plan-price")).to_contain_text("503.2")

        page.screenshot(path="tests/e2e/screenshots/pricing_desktop_1280.png", full_page=True)
        desktop.close()

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(PRICING_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 7. 汉堡按钮可见、桌面链接隐藏
        expect(page.locator(".mobile-menu-toggle")).to_be_visible()
        for i in range(3):
            expect(page.locator(".navbar-link-desktop").nth(i)).not_to_be_visible()

        # 8. 打开抽屉
        page.locator(".mobile-menu-toggle").click()
        sheet = page.locator(".mobile-drawer")
        expect(sheet).to_have_class(re.compile(r"\bopen\b"))
        expect(page.locator(".mobile-drawer a:has-text('玩法指南')")).to_be_visible()
        page.screenshot(path="tests/e2e/screenshots/pricing_mobile_375_sheet.png")

        # 9. 点击抽屉内「玩法指南」跳转 + 抽屉关闭
        page.mouse.click(187, 100)  # 点击 sheet 顶部之上,触发 backdrop 关闭
        expect(sheet).not_to_have_class(re.compile(r"\bopen\b"))
        page.locator(".mobile-menu-toggle").click()
        page.locator(".mobile-drawer a:has-text('玩法指南')").click()
        page.wait_for_url(re.compile(r".*/guide$"))

        # 10. 回到 pricing,检查 3 张定价卡为单列(每张宽度 ≈ viewport)
        page.goto(PRICING_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        card_widths = page.evaluate("""
() => {
  const cards = Array.from(document.querySelectorAll('.pricing-card'));
  return cards.map(c => c.getBoundingClientRect().width);
}
""")
        viewport_w = 375
        for w in card_widths:
            assert w > 300, f"移动端单列卡宽应接近 viewport: 实际 {w}"

        # 11. 对比表横向可滚动
        wrap = page.locator(".compare-table-wrap")
        if wrap.count() > 0:
            scroll_state = wrap.evaluate("el => ({scrollW: el.scrollWidth, clientW: el.clientWidth})")
            if scroll_state["scrollW"] > scroll_state["clientW"]:
                wrap.evaluate("el => { el.scrollLeft = 200 }")
                page.wait_for_timeout(200)
                assert wrap.evaluate("el => el.scrollLeft") > 0, "对比表应可横向滚动"
                wrap.evaluate("el => { el.scrollLeft = 0 }")

        # 12. 暗色主题
        page.evaluate("document.body.setAttribute('data-theme', 'dark'); localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        expect(page.locator(".navbar")).to_be_visible()
        expect(page.locator(".pricing-card")).to_have_count(3)

        # 13. 无横向溢出
        body_width = page.evaluate("document.documentElement.scrollWidth")
        assert body_width <= viewport_w + 2, f"横向溢出: body={body_width} viewport={viewport_w}"

        page.screenshot(path="tests/e2e/screenshots/pricing_mobile_375.png", full_page=True)
        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_pricing_mobile()
    print("Pricing mobile verification passed.")
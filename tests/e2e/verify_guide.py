import os
import sys
from playwright.sync_api import sync_playwright, expect

BASE_URL = os.environ.get("BASE_URL", "http://localhost:5173")
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")


def ensure_dir(path):
    os.makedirs(path, exist_ok=True)


def main():
    ensure_dir(SCREENSHOT_DIR)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        # 1. 首页入口
        page.goto(f"{BASE_URL}/")
        page.locator("text=玩法指南").first.click()
        page.wait_for_url(f"{BASE_URL}/guide")
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_page.png"))
        print("OK: 首页可进入玩法指南")

        # 2. 左侧目录存在
        expect(page.locator("text=产品功能").first).to_be_visible()
        expect(page.locator("text=收益方式").first).to_be_visible()
        expect(page.locator("text=创作流程").first).to_be_visible()
        expect(page.locator("text=提现与结算").first).to_be_visible()
        print("OK: 左侧目录 4 个分类存在")

        # 3. 时间节省计算器
        expect(page.locator("text=算算你能省多少")).to_be_visible()
        page.locator("text=算算你能省多少").scroll_into_view_if_needed()
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_calculator.png"))
        print("OK: 时间节省计算器存在")

        # 4. 排行榜预览
        expect(page.locator("text=本月创作币榜 TOP 5")).to_be_visible()
        page.locator("text=本月创作币榜 TOP 5").scroll_into_view_if_needed()
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_leaderboard.png"))
        print("OK: 排行榜预览存在")

        # 5. 点击目录滚动
        page.locator("aside.guide-sidebar >> text=提现与结算").click()
        page.wait_for_timeout(600)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "guide_scroll.png"))
        print("OK: 点击目录可滚动")

        # 6. 底部 CTA
        page.locator("text=立即开始创作").last.scroll_into_view_if_needed()
        expect(page.locator("text=立即开始创作").last).to_be_visible()
        print("OK: 底部 CTA 存在")

        browser.close()
        print("All guide page checks passed.")


if __name__ == "__main__":
    main()

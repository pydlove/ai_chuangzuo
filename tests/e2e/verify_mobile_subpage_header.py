"""Verify mobile subpage header and tabbar behavior."""
import os

from playwright.sync_api import sync_playwright, expect

BASE_URL = os.environ.get("BASE_URL", "http://127.0.0.1:28586")
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


def check_page(page, path, name, expect_tabbar, expect_header):
    page.goto(f"{BASE_URL}{path}")
    page.wait_for_load_state("networkidle")

    tabbar = page.locator(".console-tabbar").first
    header = page.locator(".mobile-subpage-header").first

    page.screenshot(path=os.path.join(SCREENSHOT_DIR, f"mobile-{name}.png"))

    if expect_tabbar:
        expect(tabbar).to_be_visible()
    else:
        expect(tabbar).to_be_hidden()

    if expect_header:
        expect(header).to_be_visible()
        header.locator(".mobile-subpage-back").first.click()
        page.wait_for_timeout(300)
    else:
        expect(header).to_be_hidden()


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(
            viewport={"width": 375, "height": 667},
            has_touch=True,
            is_mobile=True,
            user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1",
        )
        page = context.new_page()
        page.goto(f"{BASE_URL}/login")
        page.evaluate("""
            localStorage.setItem('aichuangzuo_access_token', 'test-token');
            localStorage.setItem('aichuangzuo_user_id', 'test-user-id');
        """)

        # TabBar pages: show tabbar, no subpage header
        check_page(page, "/console/create", "create-tab", True, False)
        check_page(page, "/console/works", "works-tab", True, False)
        check_page(page, "/console/leaderboard", "leaderboard-tab", True, False)
        check_page(page, "/console/mine", "mine-tab", True, False)

        # Subpages: hide tabbar, show back header
        check_page(page, "/console/styles", "styles-sub", False, True)
        check_page(page, "/console/earnings", "earnings-sub", False, True)
        check_page(page, "/console/hot-search", "hotsearch-sub", False, True)

        browser.close()
        print("Mobile header/tabbar verification passed.")


if __name__ == "__main__":
    main()

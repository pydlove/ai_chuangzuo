from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        # Step 1: start at preview, open invite modal
        page.goto(f"{BASE}/console/preview")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        page.click(".console-invite-btn")
        page.wait_for_selector(".invite-panel", timeout=5000)
        page.wait_for_timeout(300)

        # Step 2: click "去提现" button — modal closes, navigate to /console/coin
        page.click(".invite-stat-go-withdraw")
        page.wait_for_url(f"{BASE}/console/coin", timeout=5000)
        page.wait_for_timeout(500)
        print(f"after go to withdraw, URL = {page.url}")

        # Step 3: click "返回邀请有礼"
        back_btn = page.locator(".coin-page-header .invite-btn-secondary")
        print(f"back button text: {back_btn.text_content()}")
        print(f"back button visible: {back_btn.is_visible()}")
        back_btn.click()
        page.wait_for_timeout(800)
        print(f"after go back, URL = {page.url}")

        # Step 4: invite modal should be open again
        modal_open = page.locator(".invite-panel").is_visible()
        print(f"invite modal re-opened: {modal_open}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_reopened_after_back.png", full_page=False)
        print("Saved: invite_reopened_after_back.png")

        browser.close()
        print("Done.")


if __name__ == "__main__":
    main()
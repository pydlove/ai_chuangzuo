from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def capture(page, hover=False):
    page.goto(f"{BASE}/console/preview")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(500)

    page.click(".console-invite-btn")
    page.wait_for_selector(".invite-stat-item:nth-child(3)", timeout=5000)
    page.wait_for_timeout(500)

    label = page.locator(".invite-stat-label-tooltip")
    print(f"tooltip trigger visible: {label.is_visible()}")
    print(f"info icon visible: {page.locator('.invite-info-icon').is_visible()}")

    if hover:
        label.hover()
        page.wait_for_timeout(600)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_coin_tooltip_hover.png", full_page=False)
        print("Saved: invite_coin_tooltip_hover.png")
    else:
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_coin_tooltip_default.png", full_page=False)
        print("Saved: invite_coin_tooltip_default.png")


if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()
        capture(page, hover=False)
        capture(page, hover=True)
        browser.close()
        print("Done.")
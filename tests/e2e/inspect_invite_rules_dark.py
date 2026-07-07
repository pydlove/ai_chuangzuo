from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def capture_invite_rules_dark(page):
    page.goto(f"{BASE}/console/preview")
    page.wait_for_load_state("networkidle")
    page.evaluate("document.body.setAttribute('data-theme', 'dark')")
    page.wait_for_timeout(500)

    page.click(".console-invite-btn")
    page.wait_for_selector(".invite-panel", timeout=5000)
    page.wait_for_timeout(500)

    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_rules_dark.png", full_page=False)
    print("Saved: invite_rules_dark.png")


if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()
        capture_invite_rules_dark(page)
        browser.close()
        print("Done.")
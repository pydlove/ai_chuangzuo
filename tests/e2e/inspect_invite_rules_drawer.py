from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def capture_light(page):
    page.goto(f"{BASE}/console/preview")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(500)
    page.click(".console-invite-btn")
    page.wait_for_selector(".invite-rules-detail-btn", timeout=5000)
    page.click(".invite-rules-detail-btn")
    page.wait_for_selector(".invite-rules-detail", timeout=5000)
    page.wait_for_timeout(500)

    sections = page.locator(".invite-rules-detail-section").count()
    print(f"detail sections count: {sections}")

    headings = [el.strip() for el in page.locator(".invite-rules-detail-heading").all_text_contents()]
    print(f"detail headings: {headings}")

    list_count = page.locator(".invite-rules-detail-list li").count()
    print(f"detail list items: {list_count}")

    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_rules_drawer.png", full_page=False)
    print("Saved: invite_rules_drawer.png")


def capture_dark(page):
    page.goto(f"{BASE}/console/preview")
    page.wait_for_load_state("networkidle")
    page.evaluate("document.body.setAttribute('data-theme', 'dark')")
    page.wait_for_timeout(500)
    page.click(".console-invite-btn")
    page.wait_for_selector(".invite-rules-detail-btn", timeout=5000)
    page.click(".invite-rules-detail-btn")
    page.wait_for_selector(".invite-rules-detail", timeout=5000)
    page.wait_for_timeout(500)

    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_rules_drawer_dark.png", full_page=False)
    print("Saved: invite_rules_drawer_dark.png")


def capture_button_visible(page):
    page.goto(f"{BASE}/console/preview")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(500)
    page.click(".console-invite-btn")
    page.wait_for_selector(".invite-rules-detail-btn", timeout=5000)
    page.wait_for_timeout(500)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_rules_button.png", full_page=False)
    print("Saved: invite_rules_button.png")


if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()
        capture_button_visible(page)
        capture_light(page)
        browser.close()

        browser2 = p.chromium.launch()
        context2 = browser2.new_context(viewport={"width": 1440, "height": 900})
        page2 = context2.new_page()
        capture_dark(page2)
        browser2.close()
        print("Done.")
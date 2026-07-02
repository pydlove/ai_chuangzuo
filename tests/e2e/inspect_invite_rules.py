from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def capture_invite_rules(page):
    page.goto(f"{BASE}/console/preview")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(800)

    page.click(".console-invite-btn")
    page.wait_for_selector(".invite-panel", timeout=5000)
    page.wait_for_timeout(500)

    rules_visible = page.locator(".invite-rules").is_visible()
    print(f"invite-rules visible: {rules_visible}")

    title = page.locator(".invite-rules-title").text_content()
    print(f"rules title: {title}")

    rule_items = page.locator(".invite-rule-item").count()
    print(f"rule items count: {rule_items}")

    labels = [el.strip() for el in page.locator(".invite-rule-label").all_text_contents()]
    print(f"rule labels: {labels}")

    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_rules_modal.png", full_page=False)
    print("Saved: invite_rules_modal.png")


if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()
        capture_invite_rules(page)
        browser.close()
        print("Done.")
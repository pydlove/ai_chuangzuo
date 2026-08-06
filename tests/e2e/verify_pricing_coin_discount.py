from playwright.sync_api import sync_playwright

errors = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1280, 'height': 900})

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    page.goto('http://localhost:22345/pricing')
    page.evaluate("() => { localStorage.setItem('aichuangzuo_access_token', 'mock-token') }")
    page.reload()
    page.wait_for_timeout(2000)

    # Click subscribe button on pro plan
    try:
        btn = page.locator('.pricing-card:has(.plan-name:has-text("专业版")) .plan-btn').first
        if btn.is_visible():
            btn.click()
            page.wait_for_timeout(1000)
    except Exception as e:
        print(f"Could not click subscribe button: {e}")

    # Screenshot to see the modal
    page.screenshot(path='tests/e2e/screenshots/pricing_coin_discount_modal.png', full_page=True)
    print("Screenshot saved to tests/e2e/screenshots/pricing_coin_discount_modal.png")

    print(f"\nConsole errors: {len(errors)}")
    for e in errors:
        print(f"  ERROR: {e}")

    browser.close()

print("\nVerification complete")

from playwright.sync_api import sync_playwright
import time

url = "https://www.xingyuexiezuo.com/#/welcome"
output_dir = "/Users/panyong/aio_project/ai_chuangzuo"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})

    print(f"Navigating to {url}")
    page.goto(url, wait_until="networkidle", timeout=30000)
    time.sleep(3)

    welcome_path = f"{output_dir}/ref_welcome.png"
    page.screenshot(path=welcome_path, full_page=True)
    print(f"Saved welcome screenshot to {welcome_path}")

    # Try to find and click "去创作" button
    try:
        create_btn = page.locator("text=去创作").first
        if create_btn.is_visible(timeout=5000):
            create_btn.click()
            time.sleep(3)
            create_path = f"{output_dir}/ref_create.png"
            page.screenshot(path=create_path, full_page=True)
            print(f"Saved create page screenshot to {create_path}")
        else:
            print("Create button not found")
    except Exception as e:
        print(f"Could not click create button: {e}")

    browser.close()

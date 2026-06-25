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

    # Try to find and click CTA button
    cta_texts = ["立即开始创作", "开始创作", "去创作"]
    clicked = False
    for text in cta_texts:
        try:
            btn = page.locator(f"text={text}").first
            if btn.is_visible(timeout=3000):
                print(f"Found CTA button with text: {text}")
                btn.click()
                time.sleep(3)
                create_path = f"{output_dir}/ref_create.png"
                page.screenshot(path=create_path, full_page=True)
                print(f"Saved create page screenshot to {create_path}")
                clicked = True
                break
        except Exception as e:
            print(f"Text '{text}' not found or clickable: {e}")

    if not clicked:
        print("No CTA button found")

    # Try to click login button
    page.goto(url, wait_until="networkidle", timeout=30000)
    time.sleep(2)
    try:
        login_btn = page.locator("text=登录").first
        if login_btn.is_visible(timeout=3000):
            login_btn.click()
            time.sleep(3)
            login_path = f"{output_dir}/ref_login.png"
            page.screenshot(path=login_path, full_page=True)
            print(f"Saved login page screenshot to {login_path}")
    except Exception as e:
        print(f"Could not capture login: {e}")

    browser.close()

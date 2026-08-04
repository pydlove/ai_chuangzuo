from playwright.sync_api import sync_playwright
import os

BASE = "http://localhost:4173"
OUT = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(OUT, exist_ok=True)


def capture(browser, name, path, viewport, is_mobile=False):
    context = browser.new_context(
        viewport=viewport,
        user_agent=(
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) "
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
            if is_mobile
            else None
        ),
    )
    page = context.new_page()
    page.goto(f"{BASE}{path}", wait_until="networkidle")
    page.wait_for_timeout(1000)
    page.screenshot(path=os.path.join(OUT, name), full_page=True)
    context.close()


with sync_playwright() as p:
    browser = p.chromium.launch()

    # Mobile
    capture(browser, "mobile_login.png", "/login", {"width": 375, "height": 812}, is_mobile=True)
    capture(browser, "mobile_login_register.png", "/login?ref=ABC123", {"width": 375, "height": 812}, is_mobile=True)

    # PC
    capture(browser, "pc_login.png", "/login", {"width": 1280, "height": 800})

    browser.close()

print("Screenshots saved to", OUT)

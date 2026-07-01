import os
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
CONSOLE_URL = f"{BASE}/console"
OUT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(OUT_DIR, exist_ok=True)

def capture(page, name):
    path = os.path.join(OUT_DIR, f"{name}.png")
    page.screenshot(path=path, full_page=False)
    print(f"Saved {path}")

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        page.goto(CONSOLE_URL)
        page.wait_for_timeout(2000)
        print(f"Current URL: {page.url}")

        # Click tutorial button (first non-bell icon button)
        page.locator("button.console-icon-btn:not(.bell-btn)").nth(0).click()
        page.wait_for_timeout(800)
        capture(page, "vue_tutorial_modal_open")

        # Close tutorial via X button
        page.click(".tutorial-modal .ant-modal-close")
        page.wait_for_timeout(500)
        capture(page, "vue_tutorial_modal_closed_x")

        # Click feedback button (second non-bell icon button)
        page.locator("button.console-icon-btn:not(.bell-btn)").nth(1).click()
        page.wait_for_timeout(800)
        capture(page, "vue_feedback_modal_open")

        # Close via X button
        page.click(".feedback-modal .ant-modal-close")
        page.wait_for_timeout(500)
        capture(page, "vue_feedback_modal_closed_x")

        # Click about button (third non-bell icon button)
        page.locator("button.console-icon-btn:not(.bell-btn)").nth(2).click()
        page.wait_for_timeout(800)
        capture(page, "vue_about_modal_open")

        # Close via ESC
        page.keyboard.press("Escape")
        page.wait_for_timeout(500)
        capture(page, "vue_about_modal_closed_esc")

        browser.close()
        print("Vue modal verification completed.")

if __name__ == "__main__":
    main()

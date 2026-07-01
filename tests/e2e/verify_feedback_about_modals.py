import os
import time
from playwright.sync_api import sync_playwright

BASE = "http://localhost:28585"
CONSOLE_URL = f"{BASE}/.superpowers/brainstorm/6491-1782131242/content/console.html"
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
        page.wait_for_timeout(1000)

        # Open feedback modal
        page.click("button[title='反馈']")
        page.wait_for_timeout(500)
        capture(page, "feedback_modal_open")

        # Close via overlay click (click on the top-left corner of the overlay, outside the modal content)
        page.locator("#feedback-modal").click(force=True, position={"x": 10, "y": 10})
        page.wait_for_timeout(500)
        capture(page, "feedback_modal_closed_overlay")

        # Open about modal
        page.click("button[title='关于我们']")
        page.wait_for_timeout(500)
        capture(page, "about_modal_open")

        # Close via ESC
        page.keyboard.press("Escape")
        page.wait_for_timeout(500)
        capture(page, "about_modal_closed_esc")

        # Re-open feedback and close via X button
        page.click("button[title='反馈']")
        page.wait_for_timeout(500)
        page.click("#feedback-modal .modal-close")
        page.wait_for_timeout(500)
        capture(page, "feedback_modal_closed_x")

        browser.close()
        print("All modal verifications completed.")

if __name__ == "__main__":
    main()

"""Verify mobile pull-to-refresh on auth pages."""
import os

from playwright.sync_api import sync_playwright

BASE_URL = os.environ.get("BASE_URL", "http://127.0.0.1:28586")
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


def test_page(page, path, name):
    page.goto(f"{BASE_URL}{path}")
    page.wait_for_load_state("networkidle")
    page.wait_for_selector(".pull-to-refresh", timeout=10000)

    content = page.locator(".pull-content").first
    box = content.bounding_box()
    center_x = int(box["x"] + box["width"] / 2)
    start_y = int(box["y"] + 100)

    def dispatch_touch(event, y):
        page.evaluate(
            """([eventName, x, y, selector]) => {
                const el = document.querySelector(selector);
                const touch = new Touch({
                    identifier: 1,
                    target: el,
                    clientX: x,
                    clientY: y,
                    screenX: x,
                    screenY: y,
                    pageX: x,
                    pageY: y,
                    radiusX: 1,
                    radiusY: 1,
                    rotationAngle: 0,
                    force: 1,
                });
                const ev = new TouchEvent(eventName, {
                    cancelable: true,
                    bubbles: true,
                    touches: eventName === 'touchend' ? [] : [touch],
                    targetTouches: eventName === 'touchend' ? [] : [touch],
                    changedTouches: [touch],
                });
                el.dispatchEvent(ev);
            }""",
            [event, center_x, y, ".pull-content"],
        )

    # Small pull should translate content
    dispatch_touch("touchstart", start_y)
    dispatch_touch("touchmove", start_y + 100)
    page.wait_for_timeout(100)
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, f"ptr-{name}-pulling.png"))
    transform = page.evaluate(
        """() => {
            const el = document.querySelector('.pull-content');
            return window.getComputedStyle(el).transform;
        }"""
    )
    print(f"[{name}] Transform after 100px pull:", transform)
    assert transform != "none", "Content should be translated"

    dispatch_touch("touchend", start_y + 100)
    page.wait_for_timeout(300)

    # Large pull should trigger reload
    dispatch_touch("touchstart", start_y)
    dispatch_touch("touchmove", start_y + 180)
    page.wait_for_timeout(100)
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, f"ptr-{name}-threshold.png"))

    with page.expect_navigation(timeout=5000):
        dispatch_touch("touchend", start_y + 180)
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, f"ptr-{name}-refreshed.png"))
    print(f"[{name}] Pull-to-refresh triggered a page reload.")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(
            viewport={"width": 375, "height": 667},
            has_touch=True,
            is_mobile=True,
            user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1",
        )
        page = context.new_page()

        test_page(page, "/login", "login")
        test_page(page, "/forgot", "forgot")

        browser.close()
        print("Auth page pull-to-refresh verification passed.")


if __name__ == "__main__":
    main()

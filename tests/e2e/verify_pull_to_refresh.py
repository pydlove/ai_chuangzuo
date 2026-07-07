"""Verify mobile pull-to-refresh on /console/mine."""
import os
import sys
import time

from playwright.sync_api import sync_playwright, expect

BASE_URL = os.environ.get("BASE_URL", "http://127.0.0.1:28586")
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


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

        page.goto(f"{BASE_URL}/login")
        page.evaluate("""
            localStorage.setItem('aichuangzuo_access_token', 'test-token');
            localStorage.setItem('aichuangzuo_user_id', 'test-user-id');
        """)

        page.goto(f"{BASE_URL}/console/mine")
        page.wait_for_load_state("networkidle")
        page.wait_for_selector(".pull-to-refresh", timeout=10000)

        # Screenshot: initial state
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "ptr-initial.png"))

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

        # Small pull: should move content but not refresh
        dispatch_touch("touchstart", start_y)
        dispatch_touch("touchmove", start_y + 100)
        page.wait_for_timeout(100)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "ptr-pulling.png"))
        transform = page.evaluate(
            """() => {
                const el = document.querySelector('.pull-content');
                return window.getComputedStyle(el).transform;
            }"""
        )
        print("Transform after 100px pull:", transform)
        assert "matrix" in transform or "translate3d" in transform, "Content should be translated"

        dispatch_touch("touchend", start_y + 100)
        page.wait_for_timeout(300)
        transform_reset = page.evaluate(
            """() => {
                const el = document.querySelector('.pull-content');
                return window.getComputedStyle(el).transform;
            }"""
        )
        print("Transform after release:", transform_reset)
        # Transition may leave a tiny residual value
        if transform_reset == "none":
            reset_y = 0
        else:
            reset_y = float(transform_reset.replace("matrix(1, 0, 0, 1, ", "").replace(")", "").split(", ")[-1])
        assert reset_y < 2, f"Content should reset near 0, got {transform_reset}"

        # Large pull: should trigger a page reload (detected as navigation)
        dispatch_touch("touchstart", start_y)
        dispatch_touch("touchmove", start_y + 180)
        page.wait_for_timeout(100)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "ptr-threshold.png"))
        text = page.locator(".pull-text").first.inner_text()
        print("Indicator text at threshold:", text)
        assert text in ("释放刷新", "刷新中…"), f"Unexpected indicator text: {text}"

        with page.expect_navigation(timeout=5000):
            dispatch_touch("touchend", start_y + 180)
        page.wait_for_load_state("networkidle")
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "ptr-refreshing.png"))
        print("Pull-to-refresh triggered a page reload.")

        browser.close()
        print("Pull-to-refresh verification passed.")


if __name__ == "__main__":
    main()

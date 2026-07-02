"""验证 创作币说明 tooltip 在暗色主题下的样式."""
from playwright.sync_api import sync_playwright
import sys
import os

BASE = "http://localhost:22345"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"

errors = []
fails = []


def assert_eq(label, actual, expected):
    if actual != expected:
        msg = f"[FAIL] {label}: expected {expected!r}, got {actual!r}"
        print(msg)
        fails.append(msg)
    else:
        print(f"[ OK ] {label}: {actual!r}")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 900})
        page = context.new_page()

        page.on("console", lambda m: errors.append(m.text) if m.type == "error" else None)
        page.on("pageerror", lambda e: errors.append(str(e)))

        # ---- 暗色主题 ----
        page.goto(f"{BASE}/console/earnings")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(400)
        page.evaluate("() => { document.body.setAttribute('data-theme', 'dark') }")
        page.wait_for_timeout(200)

        trigger = page.locator(".account-stat-label-tooltip").first
        trigger.wait_for(state="visible", timeout=5000)
        trigger.scroll_into_view_if_needed()

        trigger.hover()
        page.wait_for_selector(".invite-coin-tooltip .ant-tooltip-inner", state="visible", timeout=5000)
        page.wait_for_timeout(400)

        os.makedirs(SCREENSHOT_DIR, exist_ok=True)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "coin_tooltip_dark.png"), full_page=False)

        # 取暗色主题下的计算样式
        inner = page.locator(".invite-coin-tooltip .ant-tooltip-inner").first
        bg = inner.evaluate("el => getComputedStyle(el).backgroundColor")
        color = inner.evaluate("el => getComputedStyle(el).color")
        assert_eq("dark inner background", bg, "rgb(31, 31, 31)")
        assert_eq("dark inner color", color, "rgb(240, 240, 240)")

        title_color = page.locator(".invite-coin-tooltip-title").first.evaluate("el => getComputedStyle(el).color")
        desc_color = page.locator(".invite-coin-tooltip-desc").first.evaluate("el => getComputedStyle(el).color")
        list_color = page.locator(".invite-coin-tooltip-list li").first.evaluate("el => getComputedStyle(el).color")
        assert_eq("dark title color", title_color, "rgb(240, 240, 240)")
        assert_eq("dark desc color", desc_color, "rgb(166, 166, 166)")
        assert_eq("dark list color", list_color, "rgb(166, 166, 166)")

        # ---- 浅色主题对比 ----
        page.evaluate("() => { document.body.setAttribute('data-theme', 'light') }")
        # 鼠标移开再回去
        page.mouse.move(0, 0)
        page.wait_for_timeout(300)
        trigger.hover()
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "coin_tooltip_light.png"), full_page=False)

        inner = page.locator(".invite-coin-tooltip .ant-tooltip-inner").first
        bg = inner.evaluate("el => getComputedStyle(el).backgroundColor")
        color = inner.evaluate("el => getComputedStyle(el).color")
        assert_eq("light inner background", bg, "rgb(255, 255, 255)")
        assert_eq("light inner color", color, "rgb(38, 38, 38)")

        browser.close()

    print(f"\nConsole errors: {len(errors)}")
    for e in errors:
        print(f"  ERROR: {e}")

    if fails:
        print(f"\n{len(fails)} assertion failure(s):")
        for f in fails:
            print(f"  {f}")
        sys.exit(1)

    print("\nVerification complete")


if __name__ == "__main__":
    main()

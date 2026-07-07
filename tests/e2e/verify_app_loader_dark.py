"""验证初始加载页在暗色主题下的样式."""
from playwright.sync_api import sync_playwright
import os
import sys

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

        # 先设置 localStorage 主题为 dark，再刷新
        page.goto(f"{BASE}/")
        page.wait_for_load_state("networkidle")
        page.evaluate("() => { localStorage.setItem('aichuangzuo_theme', 'dark') }")

        # 重新加载，在 loader 消失前截图
        page.goto(f"{BASE}/")
        page.wait_for_timeout(400)

        loader = page.locator("#app-loader").first
        if not loader.is_visible():
            print("[WARN] loader not visible, retrying with slower load")
            page.goto(f"{BASE}/")
            page.wait_for_timeout(200)

        os.makedirs(SCREENSHOT_DIR, exist_ok=True)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "app_loader_dark.png"), full_page=False)

        # 检查 html data-theme 属性
        theme_attr = page.evaluate("() => document.documentElement.getAttribute('data-theme')")
        assert_eq("html data-theme attribute", theme_attr, "dark")

        # 取 loader 背景
        bg = loader.evaluate("el => getComputedStyle(el).backgroundImage")
        print(f"loader backgroundImage: {bg}")
        assert_eq("loader background contains #141414", "rgb(20, 20, 20)" in bg, True)

        slogan_color = page.locator(".loader-slogan").first.evaluate("el => getComputedStyle(el).color")
        assert_eq("loader slogan color", slogan_color, "rgb(166, 166, 166)")

        en_color = page.locator(".loader-en").first.evaluate("el => getComputedStyle(el).color")
        assert_eq("loader en color", en_color, "rgb(115, 115, 115)")

        line_bg = page.locator(".loader-line").first.evaluate("el => getComputedStyle(el).backgroundColor")
        assert_eq("loader line background", line_bg, "rgb(42, 42, 42)")

        # 浅色主题对比
        page.evaluate("() => { localStorage.setItem('aichuangzuo_theme', 'light') }")
        page.goto(f"{BASE}/")
        page.wait_for_timeout(400)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "app_loader_light.png"), full_page=False)

        theme_attr = page.evaluate("() => document.documentElement.getAttribute('data-theme')")
        assert_eq("html data-theme light", theme_attr, "light")

        loader = page.locator("#app-loader").first
        bg = loader.evaluate("el => getComputedStyle(el).backgroundImage")
        print(f"light loader backgroundImage: {bg}")
        assert_eq("light loader background contains #ffffff", "ffffff" in bg or "255, 255, 255" in bg, True)

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

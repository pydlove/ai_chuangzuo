#!/usr/bin/env python3
"""Zoomed screenshot of just the search bar area."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1920, "height": 1080}, device_scale_factor=2)
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate("localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.goto(f"{BASE}/console/works?bust=100", wait_until="networkidle")
        page.wait_for_timeout(1500)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)

        # Default state zoomed
        box = page.evaluate("""() => {
          const el = document.querySelector('.works-search');
          const r = el.getBoundingClientRect();
          return { x: r.x, y: r.y, w: r.width, h: r.height };
        }""")
        print(f"Search box: {box}")
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/zoom_default.png", clip={
            "x": max(0, box["x"] - 10),
            "y": max(0, box["y"] - 10),
            "width": box["w"] + 20,
            "height": box["h"] + 20,
        })

        # Focused state
        page.click(".works-search input")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/zoom_focused.png", clip={
            "x": max(0, box["x"] - 10),
            "y": max(0, box["y"] - 10),
            "width": box["w"] + 20,
            "height": box["h"] + 20,
        })

        # Typed state
        page.keyboard.type("职场")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/zoom_typed.png", clip={
            "x": max(0, box["x"] - 10),
            "y": max(0, box["y"] - 10),
            "width": box["w"] + 20,
            "height": box["h"] + 20,
        })

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
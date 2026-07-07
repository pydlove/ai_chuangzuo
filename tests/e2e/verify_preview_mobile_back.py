#!/usr/bin/env python3
"""Verify mobile preview/export page uses the global mobile-style back header."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4173"
SCREENSHOT_DIR = "/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 375, "height": 812})
        page = ctx.new_page()

        page.goto(f"{BASE}/login")
        page.evaluate("""() => {
          localStorage.setItem('aichuangzuo_access_token', 'dummy')
          localStorage.setItem('aichuangzuo_theme', 'dark')
          localStorage.setItem('aichuangzuo_current_article', JSON.stringify({
            title: '示例文章标题',
            body: '这是一段示例正文内容。\\n\\n第二段内容。',
            wordCount: 20,
            style: '专业严谨',
            completedAt: '2026-07-06 10:00'
          }))
        }""")
        page.goto(f"{BASE}/console/preview", wait_until="networkidle")
        page.wait_for_timeout(1200)

        page.screenshot(path=f"{SCREENSHOT_DIR}/preview_mobile_back.png", full_page=True)

        # The global mobile subpage header should contain a chevron back.
        back = page.query_selector(".mobile-subpage-back")
        title = page.query_selector(".mobile-subpage-title")
        page_back = page.query_selector(".preview-header .back-btn")
        print(f"global mobile back visible: {back.is_visible() if back else False}")
        if back:
            print(f"global back text: {back.inner_text().strip()}")
        if title:
            print(f"global title text: {title.inner_text().strip()}")
        if page_back:
            print(f"page back display: {page_back.evaluate('e => getComputedStyle(e).display')}")
        else:
            print("page back not found")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

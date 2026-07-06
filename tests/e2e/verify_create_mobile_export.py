#!/usr/bin/env python3
"""Verify that clicking 去导出 on mobile Create page navigates to preview subpage."""
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
          localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify([{
            id: 'test-1',
            title: '测试文章',
            status: 'completed',
            progress: 100,
            wordCount: 50,
            completedAt: new Date().toISOString(),
            style: { name: '专业严谨' },
            content: { body: '这是测试正文第一段。\\n\\n这是第二段内容。' }
          }]))
        }""")
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(1200)

        page.screenshot(path=f"{SCREENSHOT_DIR}/create_mobile_before_export.png")

        export_btn = page.query_selector(".queue-export-btn")
        print(f"export button visible: {export_btn.is_visible() if export_btn else False}")
        if export_btn:
            export_btn.click()
            page.wait_for_timeout(1500)

        print(f"current url: {page.url}")
        page.screenshot(path=f"{SCREENSHOT_DIR}/create_mobile_after_export.png", full_page=True)

        modal = page.query_selector(".export-modal")
        print(f"export modal visible: {modal.is_visible() if modal else False}")

        preview_back = page.query_selector(".mobile-subpage-back")
        preview_title = page.query_selector(".mobile-subpage-title")
        print(f"preview mobile back visible: {preview_back.is_visible() if preview_back else False}")
        if preview_title:
            print(f"preview mobile title: {preview_title.inner_text().strip()}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()

import subprocess
import sys

try:
    from playwright.sync_api import sync_playwright, expect
except ImportError:
    print("Installing playwright...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "playwright"])
    subprocess.check_call([sys.executable, "-m", "playwright", "install", "chromium"])
    from playwright.sync_api import sync_playwright, expect


BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"


def test_console_page():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        page.goto(f"{BASE_URL}/console.html")

        # Sidebar
        expect(page.locator(".console-sidebar-brand")).to_have_text("爱创作")
        expect(page.locator(".console-sidebar-item").first).to_have_text("📝 创作")

        # Header center title
        expect(page.locator(".console-header-center")).to_have_text("爱创作")

        # Membership modal
        page.locator(".console-membership-badge").click()
        expect(page.locator("#membership-modal .modal-title")).to_have_text("会员信息")
        page.locator("#membership-modal .modal-close").click()
        expect(page.locator("#membership-modal")).not_to_be_visible()

        # User menu
        page.locator(".console-avatar").click()
        expect(page.locator(".console-user-menu")).to_be_visible()

        # Message modal
        page.evaluate("""
            localStorage.setItem('aichuangzuo_notifications', JSON.stringify([
                { id: '1', type: 'announcement', title: '公告测试', summary: '公告内容', read: false, createdAt: new Date().toISOString() }
            ]));
        """)
        page.reload()
        page.locator(".console-icon-btn[title='消息']").click()
        expect(page.locator("#message-modal .modal-title")).to_have_text("消息中心")
        expect(page.locator("#message-modal-body")).to_contain_text("公告内容")

        # Tutorial modal
        page.locator("#message-modal .modal-close").click()
        page.locator(".console-icon-btn[title='教程']").click()
        expect(page.locator("#tutorial-modal .modal-title")).to_have_text("教程与帮助")

        # Theme toggle
        page.locator("#tutorial-modal .modal-close").click()
        page.evaluate("""() => { localStorage.setItem('aichuangzuo_theme', 'light'); }""")
        page.reload()
        page.locator(".console-icon-btn[title='切换主题']").click()
        theme = page.evaluate("""() => { return document.body.getAttribute('data-theme'); }""")
        assert theme == 'dark', f"Expected dark theme, got {theme}"

        browser.close()


if __name__ == "__main__":
    test_console_page()
    print("Console verification passed.")

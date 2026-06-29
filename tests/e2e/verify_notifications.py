import subprocess
import sys
import time
from pathlib import Path

try:
    from playwright.sync_api import sync_playwright, expect
except ImportError:
    print("Installing playwright...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "playwright"])
    subprocess.check_call([sys.executable, "-m", "playwright", "install", "chromium"])
    from playwright.sync_api import sync_playwright, expect


BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"


def clear_notifications(page):
    page.evaluate("""
        localStorage.removeItem('aichuangzuo_notifications');
        localStorage.removeItem('aichuangzuo_notifications_seeded');
        localStorage.removeItem('aichuangzuo_membership_notified_days');
    """)


def test_notification_badge_and_page():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        # 1. 打开首页并清空历史消息
        page.goto(f"{BASE_URL}/index.html")
        clear_notifications(page)
        page.reload()

        # 2. 铃铛应存在，角标隐藏
        bell = page.locator(".notification-bell").first
        expect(bell).to_be_visible()
        badge = bell.locator(".notification-badge").first
        expect(badge).not_to_be_visible()

        # 3. 手动写入一条未读生成完成消息
        page.evaluate("""
            var notifications = [{
                id: 'test-gen-1',
                type: 'generation',
                title: '文章生成完成',
                summary: '《测试文章》已生成',
                read: false,
                createdAt: new Date().toISOString()
            }];
            localStorage.setItem('aichuangzuo_notifications', JSON.stringify(notifications));
        """)
        page.reload()

        # 4. 角标显示 1
        expect(badge).to_be_visible()
        expect(badge).to_have_text("1")

        # 5. 点击铃铛进入消息中心
        bell.click()
        page.wait_for_url(f"{BASE_URL}/notifications.html")
        expect(page.locator(".notification-page-title")).to_have_text("消息中心")

        # 6. 生成完成 Tab 下应有一条消息
        item = page.locator(".notification-item").first
        expect(item).to_be_visible()
        expect(item.locator(".notification-item-title")).to_have_text("文章生成完成")

        # 7. 消息应已被自动标记为已读（进入页面即已读）
        expect(item).not_to_have_class("unread")

        # 8. 清空本类消息
        page.on("dialog", lambda dialog: dialog.accept())
        page.locator("#clear-btn").click()
        expect(page.locator(".notification-empty")).to_be_visible()

        browser.close()


if __name__ == "__main__":
    test_notification_badge_and_page()
    print("Notification verification passed.")

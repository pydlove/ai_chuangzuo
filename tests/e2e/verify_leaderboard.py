import subprocess
import time
import sys
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[2]
USER_WEB = ROOT / "project" / "user" / "web"
BASE_URL = "http://localhost:4173"
SCREENSHOT_DIR = ROOT / "tests" / "e2e" / "screenshots"


def start_preview_server():
    proc = subprocess.Popen(
        ["npm", "run", "preview", "--", "--port", "4173"],
        cwd=USER_WEB,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    for _ in range(30):
        try:
            import urllib.request
            urllib.request.urlopen(BASE_URL, timeout=1)
            return proc
        except Exception:
            time.sleep(1)
    proc.terminate()
    raise RuntimeError("preview server did not start")


MOCK_COIN = {
    "code": 0,
    "data": {
        "topList": [
            {"userId": "u1", "nickname": "创作达人", "amount": 12580.00, "rank": 1, "isMe": False},
            {"userId": "u2", "nickname": "小李同学", "amount": 9860.50, "rank": 2, "isMe": False},
            {"userId": "u3", "nickname": "张三", "amount": 7420.00, "rank": 3, "isMe": False},
            {"userId": "u4", "nickname": "UserFour", "amount": 5120.00, "rank": 4, "isMe": False},
            {"userId": "u5", "nickname": "UserFive", "amount": 4300.00, "rank": 5, "isMe": False},
            {"userId": "u6", "nickname": "UserSix", "amount": 3600.00, "rank": 6, "isMe": False},
            {"userId": "me", "nickname": "我", "amount": 2100.00, "rank": 8, "isMe": True}
        ],
        "me": {"userId": "me", "nickname": "我", "amount": 2100.00, "rank": 8, "isMe": True}
    }
}


def main():
    server = start_preview_server()
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch()
            context = browser.new_context(viewport={"width": 1280, "height": 900})
            context.add_init_script("""
                localStorage.setItem('aichuangzuo_access_token', 'mock-token');
                localStorage.setItem('aichuangzuo_refresh_token', 'mock-token');
            """)
            page = context.new_page()

            def handle_route(route, request):
                url = request.url
                if "/leaderboards/coin" in url:
                    route.fulfill(status=200, content_type="application/json", body=json.dumps(MOCK_COIN))
                elif "/api/" in url:
                    route.fulfill(status=200, content_type="application/json", body=json.dumps({"code": 0, "data": {}, "message": "ok"}))
                else:
                    route.continue_()

            page.route("**/api/**", handle_route)
            page.goto(f"{BASE_URL}/console/create")
            page.wait_for_load_state("networkidle")
            time.sleep(1)
            page.evaluate("""
                localStorage.setItem('aichuangzuo_access_token', 'mock-token');
                localStorage.setItem('aichuangzuo_refresh_token', 'mock-token');
            """)
            page.locator('.console-sidebar-item:has-text("收益排行榜")').click()
            time.sleep(1)

            # 验证页面标题与规则说明
            assert page.is_visible("text=收益排行榜"), "page title not found"
            assert page.is_visible("text=规则说明"), "rules link not found"
            # 自媒体收入榜与 tab 切换已暂时隐藏
            assert not page.is_visible("text=自媒体收入榜"), "income tab should be hidden"
            assert not page.locator(".leaderboard-tabs").is_visible(), "tab switch should be hidden"
            assert not page.is_visible("text=申报收入"), "submit button should be hidden"

            # 截图：创作币榜
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_coin.png"))

            # 截图：前三名卡片
            top_cards = page.locator(".leaderboard-top-card").count()
            assert top_cards >= 1, "expected top cards"

            # 打开规则弹框
            page.click("text=规则说明")
            time.sleep(0.3)
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_rules.png"))

            # 暗色主题截图
            page.locator(".leaderboard-rules-modal .ant-modal-close").click()
            time.sleep(0.3)
            page.evaluate(
                """() => {
                    localStorage.setItem('aichuangzuo_theme', 'dark');
                    document.body.setAttribute('data-theme', 'dark');
                }"""
            )
            time.sleep(0.3)
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_coin_dark.png"))

            # 暗色规则弹框截图
            page.click("text=规则说明")
            time.sleep(0.3)
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_rules_dark.png"))

            # 验证暗色下关键元素可见且背景色符合预期
            bg = page.evaluate("() => getComputedStyle(document.querySelector('.leaderboard-page')).backgroundColor")
            assert "14" in bg or "20" in bg or "21" in bg, f"unexpected dark background: {bg}"
            section_bg = page.evaluate("() => getComputedStyle(document.querySelector('.leaderboard-section')).backgroundColor")
            assert "14" in section_bg or "20" in section_bg or "21" in section_bg, f"unexpected dark section background: {section_bg}"

            browser.close()
            print("All leaderboard checks passed.")
    finally:
        server.terminate()
        try:
            server.wait(timeout=5)
        except Exception:
            server.kill()


if __name__ == "__main__":
    main()

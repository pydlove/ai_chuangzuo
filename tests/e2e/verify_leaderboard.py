import subprocess
import time
import sys
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


def main():
    server = start_preview_server()
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch()
            page = browser.new_page(viewport={"width": 1280, "height": 900})
            page.goto(f"{BASE_URL}/console/leaderboard")
            page.wait_for_load_state("networkidle")
            time.sleep(0.5)

            # 验证页面标题与标签
            assert page.is_visible("text=收益排行榜"), "page title not found"
            assert page.is_visible("text=创作币榜"), "coin tab not found"
            assert page.is_visible("text=自媒体收入榜"), "income tab not found"
            assert page.is_visible("text=规则说明"), "rules link not found"

            # 截图：创作币榜
            page.click("text=创作币榜")
            time.sleep(0.3)
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_coin.png"))

            # 截图：前三名卡片
            top_cards = page.locator(".leaderboard-top-card").count()
            assert top_cards >= 1, "expected top cards"

            # 切换到自媒体收入榜
            page.locator('.leaderboard-tab >> text=自媒体收入榜').click()
            time.sleep(0.5)
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_income.png"))

            # 打开申报弹框
            page.locator(".leaderboard-submit-btn").click()
            time.sleep(0.3)
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_submit.png"))

            # 验证申报表单元素
            assert page.is_visible("text=所属月份"), "submit month label not found"
            assert page.is_visible("text=收入金额（元）"), "submit amount label not found"
            assert page.is_visible("text=收益截图"), "submit screenshot label not found"

            # 关闭申报弹框并打开规则弹框
            page.keyboard.press("Escape")
            time.sleep(0.3)
            page.click("text=规则说明")
            time.sleep(0.3)
            page.screenshot(path=str(SCREENSHOT_DIR / "leaderboard_rules.png"))

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

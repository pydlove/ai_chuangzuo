import os
import re
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

BASE_URL = os.environ.get("BASE_URL", "http://localhost:22347")
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"


def test_redeem_code():
    SCREENSHOT_DIR.mkdir(exist_ok=True)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        page.goto(f"{BASE_URL}/console/create")
        page.wait_for_selector(".console-layout", timeout=10000)

        # 清理之前的测试数据
        page.evaluate("""() => {
            localStorage.removeItem('aichuangzuo_redeem_codes')
            localStorage.removeItem('aichuangzuo_redeem_history')
            localStorage.removeItem('aichuangzuo_coin_balance')
            localStorage.removeItem('aichuangzuo_membership')
            localStorage.setItem('aichuangzuo_notif_seeded', '1')
        }""")
        page.reload()
        page.wait_for_selector(".console-layout", timeout=10000)

        # 获取当前余额
        def get_coin_balance():
            return page.evaluate("""() => {
                const raw = localStorage.getItem('aichuangzuo_coin_balance')
                return raw ? parseInt(raw, 10) : 0
            }""")

        # 用例 1: 弹框渲染
        redeem_buttons = page.locator("button:has-text('兑换码')")
        expect(redeem_buttons).to_have_count(1)
        redeem_buttons.first.click()
        page.wait_for_selector(".redeem-panel", timeout=5000)
        page.screenshot(path=SCREENSHOT_DIR / "redeem_modal_open.png")

        # 用例 6: 空输入时按钮 disabled
        submit_btn = page.locator(".redeem-submit")
        expect(submit_btn).to_be_disabled()

        # 用例 2: 兑换创作币 COIN100
        page.locator(".redeem-input").fill("COIN100")
        expect(submit_btn).to_be_enabled()
        submit_btn.click()
        page.wait_for_selector(".redeem-status.success", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("兑换成功 +100 创作币")
        page.screenshot(path=SCREENSHOT_DIR / "redeem_success_coin.png")
        page.wait_for_timeout(2200)
        # 弹框关闭
        expect(page.locator(".redeem-panel")).not_to_be_visible()
        page.wait_for_timeout(500)  # 等待 modal 关闭动画完成
        assert get_coin_balance() == 100, f"余额应为 100,实际 {get_coin_balance()}"

        # 用例 5: 重复兑换提示已使用
        redeem_buttons.first.click()
        page.wait_for_selector(".redeem-panel", timeout=5000)
        page.locator(".redeem-input").fill("COIN100")
        submit_btn.click()
        page.wait_for_selector(".redeem-status.error", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("该兑换码已被使用过")

        # 用例 3: 兑换会员 VIP7DAY(同一弹框继续,错误态不会自动关闭)
        page.locator(".redeem-input").fill("VIP7DAY")
        submit_btn.click()
        page.wait_for_selector(".redeem-status.success", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("兑换成功 +7 天专业版会员")
        page.screenshot(path=SCREENSHOT_DIR / "redeem_success_membership.png")
        page.wait_for_timeout(2200)
        expect(page.locator(".redeem-panel")).not_to_be_visible()
        page.wait_for_timeout(500)  # 等待 modal 关闭动画完成

        # 验证 membership 写入
        membership = page.evaluate("""() => {
            const raw = localStorage.getItem('aichuangzuo_membership')
            return raw ? JSON.parse(raw) : null
        }""")
        assert membership and membership.get("level") == "专业版会员", membership
        assert re.match(r"\d{4}-\d{2}-\d{2}", membership.get("expiresAt", "")), membership

        # 用例 4: 无效码
        redeem_buttons.first.click()
        page.locator(".redeem-input").fill("INVALID")
        submit_btn.click()
        page.wait_for_selector(".redeem-status.error", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("兑换码无效或已过期")
        page.screenshot(path=SCREENSHOT_DIR / "redeem_error_invalid.png")

        browser.close()
        print("All redeem code tests passed.")


if __name__ == "__main__":
    test_redeem_code()

import os
import re
from playwright.sync_api import sync_playwright

URL = os.environ.get('APP_URL', 'http://localhost:22345')
SCREENSHOT_DIR = 'tests/e2e/screenshots'
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        # 1. 清空市场相关 localStorage 并设置余额
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(800)
        page.evaluate("""
          () => {
            localStorage.removeItem('aichuangzuo_style_market')
            localStorage.removeItem('aichuangzuo_earnings_records')
            localStorage.setItem('aichuangzuo_coin_balance', '10')
          }
        """)
        page.reload()
        page.wait_for_timeout(500)

        # 2. 创建我的风格
        page.locator('.style-add-card:has-text("新建我的风格")').click()
        page.wait_for_timeout(300)
        inputs = page.locator('.style-editor-input')
        inputs.nth(0).fill('市场测试风格')
        page.locator('.style-editor-textarea').fill('这是一段用于市场测试的风格提示词。')
        inputs.nth(1).fill('公众号情感文')
        page.locator('.style-editor-form button:has-text("保存")').click()
        page.wait_for_timeout(300)

        # 3. 发布并模拟审核通过
        card = page.locator('.style-card:has-text("市场测试风格")')
        card.locator('button:has-text("发布")').click()
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_publish_confirm.png')
        page.locator('.publish-confirm-submit:has-text("确认发布")').click()
        page.wait_for_timeout(500)
        card.locator('button:has-text("通过")').click()
        page.wait_for_timeout(300)
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_cards_redesign.png')

        # 4. 进入市场页
        page.goto(f'{URL}/console/style-market')
        page.wait_for_timeout(800)
        assert '市场测试风格' in page.content()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_list.png')

        # 5. 使用市场风格
        page.locator('button:has-text("使用")').first.click()
        page.wait_for_timeout(800)
        assert '/console/create' in page.url
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_applied.png')

        # 6. 进入收益页
        page.goto(f'{URL}/console/earnings')
        page.wait_for_timeout(800)
        assert '使用「市场测试风格」生成文章' in page.content()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_earnings.png')

        print('风格市场验证通过')
        browser.close()


if __name__ == '__main__':
    main()

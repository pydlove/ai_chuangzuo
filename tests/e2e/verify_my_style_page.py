from playwright.sync_api import sync_playwright

URL = 'http://localhost:22347'  # 根据实际 dev server 端口调整

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        # 1. 访问我的风格页面
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(800)

        assert page.locator('.styles-title').inner_text() == '我的风格'
        assert page.locator('.styles-tab').count() == 3

        # 2. 系统预设风格 tab 显示 8 张卡片
        page.locator('button:has-text("系统预设风格")').click()
        page.wait_for_timeout(300)
        system_cards = page.locator('.styles-content:visible .style-card')
        assert system_cards.count() == 8, f'Expected 8 system styles, got {system_cards.count()}'

        # 3. 我的风格 tab 为空
        page.locator('button:has-text("我的风格")').click()
        page.wait_for_timeout(300)
        assert page.locator('.styles-empty').count() == 1

        # 4. 新建风格
        page.locator('button:has-text("去创建一个")').click()
        page.wait_for_timeout(300)
        inputs = page.locator('.style-editor-input')
        inputs.nth(0).fill('测试风格')
        page.locator('.style-editor-textarea').fill('这是一段测试风格提示词，语气轻松活泼。')
        inputs.nth(1).fill('公众号情感文')
        page.locator('.style-editor-form button:has-text("保存")').click()
        page.wait_for_timeout(300)

        cards = page.locator('.styles-content:visible .style-card')
        assert cards.count() == 1, f'Expected 1 custom style, got {cards.count()}'
        card_text = cards.first.inner_text()
        assert '测试风格' in card_text
        assert '适用：公众号情感文' in card_text

        # 5. 编辑风格
        page.locator('button:has-text("编辑")').click()
        page.wait_for_timeout(300)
        page.locator('.style-editor-textarea').fill('已更新的测试风格提示词。')
        inputs = page.locator('.style-editor-input')
        inputs.nth(1).fill('小红书种草')
        page.locator('.style-editor-form button:has-text("保存")').click()
        page.wait_for_timeout(300)
        card_text = cards.first.inner_text()
        assert '已更新' in card_text
        assert '适用：小红书种草' in card_text

        # 6. 删除风格
        page.locator('button:has-text("删除")').click()
        page.wait_for_timeout(300)
        assert page.locator('.styles-empty').count() == 1

        # 7. 系统预设风格“使用”跳转创作页
        page.locator('button:has-text("系统预设风格")').click()
        page.wait_for_timeout(300)
        page.locator('.styles-content:visible .style-action-btn').filter(has_text='使用').first.click()
        page.wait_for_timeout(500)
        assert '/console/create' in page.url

        page.screenshot(path='tests/e2e/screenshots/my_style_page.png')
        print('我的风格独立页面验证通过')

        browser.close()

if __name__ == '__main__':
    main()

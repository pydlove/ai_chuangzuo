# tests/e2e/verify_style_learning.py
import os
import time
from playwright.sync_api import sync_playwright

URL = os.environ.get('APP_URL', 'http://localhost:22345')
SCREENSHOT_DIR = 'tests/e2e/screenshots'
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

SAMPLE_TEXT = (
    '这是一段用于测试风格学习的示例文本。'
    '它需要超过两百个字符才能触发「开始学习」按钮。'
    + '在这里我们将文本不断重复以满足字数要求。' * 30
)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        # 1. 访问我的风格页面 + 清空 localStorage
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(800)
        page.evaluate("() => localStorage.removeItem('aichuangzuo_learned_styles')")
        page.reload()
        page.wait_for_timeout(500)

        # 2. 验证第三个 tab 存在
        tabs = page.locator('.styles-tab')
        assert tabs.count() == 3, f'期望 3 个 tab，实际 {tabs.count()}'
        tabs.nth(2).click()
        page.wait_for_timeout(300)

        # 3. 验证空状态
        assert page.locator('.learned-empty').count() == 1
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_learning_empty.png')

        # 4. 打开导入对话框
        page.locator('.learned-add-btn').click()
        page.wait_for_timeout(300)
        assert page.locator('.learned-textarea').count() >= 1

        # 5. 验证过短文本按钮禁用
        page.locator('.learned-textarea').first.fill('太短了')
        page.wait_for_timeout(200)
        assert page.locator('.learned-submit-btn').first.is_disabled()

        # 6. 粘贴足够长的文本
        textareas = page.locator('.learned-textarea')
        inputs = page.locator('.learned-input')
        textareas.first.fill(SAMPLE_TEXT)
        # 第一个 .learned-input 是「来源标题」
        inputs.first.fill('测试来源')
        page.wait_for_timeout(200)
        page.locator('.learned-submit-btn').first.click()

        # 7. 等待进度态结束，进入结果页
        page.wait_for_selector('.learned-progress', timeout=2000)
        page.wait_for_selector('.learned-result-title', timeout=5000)
        assert '测试来源' in page.locator('.learned-result-title').inner_text()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_learning_result.png')

        # 8. 输入命名并保存（结果页第二个 .learned-input 是「命名」字段）
        inputs = page.locator('.learned-input')
        inputs.last.fill('我的测试风格')
        # 适用范围：填写
        page.locator('input[placeholder*="公众号情感文"]').fill('公众号情感文')
        page.wait_for_timeout(200)
        page.locator('button:has-text("保存到风格库")').click()
        page.wait_for_timeout(500)

        # 9. 验证第三个 tab 中出现新卡片
        tabs.nth(2).click()
        page.wait_for_timeout(300)
        cards = page.locator('.styles-content:visible .style-card')
        assert cards.count() == 1, f'期望 1 张卡片，实际 {cards.count()}'
        assert '我的测试风格' in cards.first.inner_text()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_learning_card.png')

        # 10. 创作页联动
        page.goto(f'{URL}/console/create')
        page.wait_for_timeout(1500)
        chips = page.locator('.settings-chip')
        chips.nth(2).click()  # 风格 chip
        page.wait_for_timeout(800)
        modal_tabs = page.locator('.ant-modal .style-tab')
        assert modal_tabs.count() == 3
        modal_tabs.nth(2).click()
        page.wait_for_timeout(500)
        assert '我的测试风格' in page.content()

        # 11. 删除卡片
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(500)
        tabs = page.locator('.styles-tab')
        tabs.nth(2).click()
        page.wait_for_timeout(300)
        page.on('dialog', lambda dialog: dialog.accept())
        page.locator('button:has-text("删除")').first.click()
        page.wait_for_timeout(500)
        assert page.locator('.learned-empty').count() == 1

        print('文章风格学习验证通过')
        browser.close()


if __name__ == '__main__':
    main()
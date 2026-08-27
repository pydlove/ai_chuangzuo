from playwright.sync_api import sync_playwright
import os

BASE = os.environ.get('T2I_TEST_BASE', 'http://localhost:28588')


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # 桌面端
        page = browser.new_page(viewport={'width': 1280, 'height': 800})
        page.goto(f'{BASE}/tools/text-to-image')
        page.wait_for_load_state('networkidle')
        page.wait_for_selector('#app-loader', state='detached', timeout=5000)
        assert page.locator('text=文字转图片').first.is_visible(), '页面标题未显示'
        assert page.locator('[contenteditable="true"]').first.is_visible(), '富文本编辑器未显示'
        assert page.locator('button:has-text("下载图片")').first.is_visible(), '下载图片按钮未显示'
        print('text-to-image page desktop OK')
        page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/text_to_image_page.png')

        # 导出功能：使用默认内容点击导出，应触发图片下载
        with page.expect_download() as download_info:
            page.locator('button:has-text("下载图片")').first.click()
        download = download_info.value
        assert download.suggested_filename.endswith('.png'), '导出文件名格式错误'
        print('text-to-image export OK')

        # 移动端：检查返回头
        mobile = browser.new_page(viewport={'width': 390, 'height': 844})
        mobile.goto(f'{BASE}/tools/text-to-image')
        mobile.wait_for_load_state('networkidle')
        mobile.wait_for_selector('#app-loader', state='detached', timeout=5000)
        assert mobile.locator('.t2i-subpage-back').first.is_visible(), '移动端返回按钮未显示'
        assert mobile.locator('.t2i-subpage-title').first.is_visible(), '移动端标题未显示'
        print('text-to-image mobile header OK')
        mobile.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/text_to_image_mobile_header.png')

        browser.close()


if __name__ == '__main__':
    main()

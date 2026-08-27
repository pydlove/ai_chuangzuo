from playwright.sync_api import sync_playwright
import os

BASE = os.environ.get('CUTOUT_TEST_BASE', 'http://localhost:28588')

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # 桌面端
        page = browser.new_page(viewport={'width': 1280, 'height': 800})
        page.goto(f'{BASE}/tools/cutout')
        page.wait_for_load_state('networkidle')
        page.wait_for_selector('#app-loader', state='detached', timeout=5000)
        assert page.locator('text=AI 抠图').first.is_visible(), '页面标题未显示'
        assert page.locator('text=上传图片').first.is_visible(), '上传按钮未显示'
        assert page.locator('text=抠图').first.is_visible(), '抠图按钮未显示'
        assert page.locator('text=下载').first.is_visible(), '下载按钮未显示'
        print('cutout page desktop OK')
        page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/cutout_page.png')

        # 移动端：检查返回头
        mobile = browser.new_page(viewport={'width': 390, 'height': 844})
        mobile.goto(f'{BASE}/tools/cutout')
        mobile.wait_for_load_state('networkidle')
        mobile.wait_for_selector('#app-loader', state='detached', timeout=5000)
        assert mobile.locator('.cutout-subpage-back').first.is_visible(), '移动端返回按钮未显示'
        assert mobile.locator('.cutout-subpage-title').first.is_visible(), '页面标题未显示'
        print('cutout page mobile header OK')
        mobile.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/cutout_mobile_header.png')

        # 桌面端：上传 → 抠图 → 结果检查
        test_image = '/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/downloads/invite-poster-classic-red.png'
        page.locator('.cutout-file-input').set_input_files(test_image)
        page.wait_for_selector('.cutout-canvas-wrap:not(.is-empty)', state='visible', timeout=10000)
        page.locator('button:has-text("抠图")').first.click()
        page.wait_for_selector('.cutout-result-close', state='visible', timeout=10000)
        assert page.locator('canvas.cutout-canvas').nth(1).is_visible(), '结果画布未显示'
        assert page.locator('.cutout-canvas-wrap--result').count() == 1, '结果面板缺少栅格背景容器'
        print('cutout workflow OK')
        page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/cutout_result.png')

        # 再次点击「上传图片」按钮并选择同一张图，应能重新上传并清空上次结果
        with page.expect_file_chooser() as fc_info:
            page.locator('.cutout-panel-head button:has-text("上传图片")').first.click()
        fc_info.value.set_files(test_image)
        page.wait_for_selector('.cutout-result-close', state='hidden', timeout=5000)
        assert page.locator('canvas.cutout-canvas').first.is_visible(), '重新上传后原图未显示'
        print('cutout re-upload OK')

        browser.close()

if __name__ == '__main__':
    main()

from playwright.sync_api import sync_playwright
import os

BASE = os.environ.get('TOOL_PAGES_BASE', 'http://localhost:28588')
PAGES = [
    ('image_compress', '/tools/image-compress'),
    ('qrcode', '/tools/qrcode'),
    ('watermark_remove', '/tools/watermark-remove'),
]


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        for name, path in PAGES:
            desktop = browser.new_page(viewport={'width': 1280, 'height': 800})
            desktop.goto(f'{BASE}{path}')
            desktop.wait_for_load_state('networkidle')
            desktop.wait_for_selector('#app-loader', state='detached', timeout=5000)
            desktop.screenshot(path=f'/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/{name}_desktop.png')
            desktop.close()

            mobile = browser.new_page(viewport={'width': 390, 'height': 844})
            mobile.goto(f'{BASE}{path}')
            mobile.wait_for_load_state('networkidle')
            mobile.wait_for_selector('#app-loader', state='detached', timeout=5000)
            mobile.screenshot(path=f'/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/{name}_mobile.png')
            mobile.close()
            print(f'{name} screenshots OK')
        browser.close()


if __name__ == '__main__':
    main()

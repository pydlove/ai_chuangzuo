from playwright.sync_api import sync_playwright

errors = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1280, 'height': 900})

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    def handle_api(route, request):
        route.fulfill(status=200, content_type='application/json', body='{"code":0,"data":{},"message":"ok"}')

    page.route('**/api/v1/user/**', handle_api)

    page.goto('http://localhost:28587/login')
    page.evaluate("() => { localStorage.setItem('aichuangzuo_access_token', 'mock-token') }")
    page.goto('http://localhost:28587/console/orders')
    page.wait_for_timeout(3000)

    page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/desktop_sidebar_orders.png', full_page=True)
    print('Screenshot saved to /Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/desktop_sidebar_orders.png')

    items = page.locator('.console-sidebar-item').all_inner_texts()
    print(f'Sidebar items: {items}')
    assert any('我的订单' in item for item in items), '我的订单 not found in sidebar'

    print(f'\nConsole errors: {len(errors)}')
    for e in errors:
        print(f'  ERROR: {e}')

    browser.close()

print('\nVerification complete')

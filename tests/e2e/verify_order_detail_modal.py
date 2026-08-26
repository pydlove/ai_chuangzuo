from playwright.sync_api import sync_playwright

errors = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 375, 'height': 812})

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    def handle_api(route, request):
        url = request.url
        if '/orders' in url:
            route.fulfill(
                status=200,
                content_type='application/json',
                body='{"code":0,"data":{"list":[{"id":1,"orderNo":"SUB260826000001","planKey":"pro","planName":"专业版会员","cycle":"year","cycleName":"年付","amount":199.00,"coinAmount":100,"coinDiscount":10.00,"couponDiscount":20.00,"totalAmount":229.00,"status":1,"statusName":"已支付","paidAt":"2026-08-26T10:30:00","createdAt":"2026-08-26T10:25:00"},{"id":2,"orderNo":"SUB260826000002","planKey":"basic","planName":"基础版会员","cycle":"month","cycleName":"月付","amount":29.00,"coinAmount":0,"coinDiscount":0.00,"couponDiscount":0.00,"totalAmount":29.00,"status":0,"statusName":"待支付","paidAt":null,"createdAt":"2026-08-26T11:00:00"}],"total":2,"page":1,"pageSize":100},"message":"ok"}'
            )
            return
        if '/membership/me' in url:
            route.fulfill(status=200, content_type='application/json', body='{"code":0,"data":{"hasMembership":true,"level":"pro","levelName":"专业版","expiresAt":"2026-12-31","cycle":"year"},"message":"ok"}')
            return
        if '/user/me' in url or '/profile' in url:
            route.fulfill(status=200, content_type='application/json', body='{"code":0,"data":{"id":1,"nickname":"测试用户","phone":"13800138000","email":"test@example.com","avatarUrl":"","membershipLevel":"pro","membershipExpireAt":"2026-12-31"},"message":"ok"}')
            return
        route.fulfill(status=200, content_type='application/json', body='{"code":0,"data":{},"message":"ok"}')

    page.route('**/api/v1/user/**', handle_api)

    page.goto('http://localhost:28586/login')
    page.evaluate("() => { localStorage.setItem('aichuangzuo_access_token', 'mock-token') }")
    page.goto('http://localhost:28586/console/orders')
    page.wait_for_timeout(1500)

    page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/order_list_mobile.png', full_page=True)
    print('Screenshot saved to /Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/order_list_mobile.png')

    cards = page.locator('.order-card').all()
    assert len(cards) > 0, 'No order cards found'
    cards[0].click()
    page.wait_for_timeout(500)

    page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/order_detail_modal_mobile.png', full_page=True)
    print('Screenshot saved to /Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/order_detail_modal_mobile.png')

    modal = page.locator('.order-detail-modal')
    assert modal.is_visible(), 'Detail modal is not visible'
    labels = modal.locator('.order-detail__label').all_inner_texts()
    assert '支付名称' in labels
    assert '订单号' in labels
    assert '支付方式' in labels
    print(f'Modal labels: {labels}')

    print(f'\nConsole errors: {len(errors)}')
    for e in errors:
        print(f'  ERROR: {e}')

    browser.close()

print('\nVerification complete')

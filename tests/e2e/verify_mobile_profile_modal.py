import json
from playwright.sync_api import sync_playwright

errors = []
BASE_URL = 'http://localhost:28587'

PROFILE_TEMPLATE = {
    "userId": "U000001",
    "nickname": "测试用户",
    "email": "test@example.com",
    "phone": "13800138000",
    "avatarUrl": "",
    "bio": "",
    "gender": 0,
    "birthday": "",
    "location": "",
    "emailVerified": 1,
    "phoneVerified": 1,
    "inviteCode": "ABC123"
}

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 375, 'height': 812})

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    mock_bio = '这是我的个人简介'

    def handle_api(route, request):
        url = request.url
        if '/me' in url and 'profile' not in url and 'nickname' not in url:
            data = {**PROFILE_TEMPLATE, "bio": mock_bio}
            route.fulfill(status=200, content_type='application/json', body=json.dumps({"code": 0, "data": data, "message": "ok"}))
            return
        route.fulfill(status=200, content_type='application/json', body='{"code":0,"data":{},"message":"ok"}')

    page.route('**/api/v1/user/**', handle_api)

    def load_mine():
        page.goto(f'{BASE_URL}/login')
        page.evaluate("() => { localStorage.setItem('aichuangzuo_access_token', 'mock-token') }")
        page.goto(f'{BASE_URL}/console/mine')
        page.wait_for_timeout(1500)

    # === Case 1: with bio ===
    load_mine()

    page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/mobile_mine_settings_with_bio.png', full_page=True)
    print('Screenshot saved to mobile_mine_settings_with_bio.png')

    bio_text = page.locator('.mine-user-bio').inner_text()
    print(f'With bio - mine page bio: {bio_text}')
    assert '这是我的个人简介' in bio_text, 'Bio not displayed on mine page'
    assert page.locator('.mine-user-id').count() == 1, 'Phone/email line should be visible when bio exists'

    # === Case 2: empty bio ===
    mock_bio = ''
    page.reload()
    page.wait_for_timeout(1500)

    page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/mobile_mine_settings_empty_bio.png', full_page=True)
    print('Screenshot saved to mobile_mine_settings_empty_bio.png')

    bio_text = page.locator('.mine-user-bio').inner_text()
    print(f'Empty bio - mine page bio: {bio_text}')
    assert '点击添加简介' in bio_text, 'Bio placeholder not displayed'
    assert page.locator('.mine-user-id').count() == 0, 'Phone/email line should be hidden when bio is empty'

    # === Settings modal checks ===
    page.locator('.mine-header-icon-btn').nth(1).click()
    page.wait_for_timeout(500)

    page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/mobile_settings_modal.png', full_page=True)
    print('Screenshot saved to mobile_settings_modal.png')

    settings_items = page.locator('.mine-settings-item').all_inner_texts()
    print(f'Settings items: {settings_items}')
    assert any('修改个人信息' in item for item in settings_items), '修改个人信息 not found'

    settings_label = page.locator('.mine-settings-item').filter(has_text='修改个人信息').locator('.mine-settings-label')
    assert settings_label.count() == 1
    styles = settings_label.evaluate('el => ({ whiteSpace: window.getComputedStyle(el).whiteSpace, width: window.getComputedStyle(el).width, minWidth: window.getComputedStyle(el).minWidth })')
    print(f'Label computed styles: white-space={styles["whiteSpace"]}, width={styles["width"]}, min-width={styles["minWidth"]}')
    label_box = settings_label.bounding_box()
    assert label_box['height'] <= 22, f'Label wrapped, height={label_box["height"]}'

    # === Profile modal checks ===
    background_name = page.locator('.mine-user-name').inner_text()
    print(f'Background name before edit: {background_name}')

    page.locator('.mine-settings-item').filter(has_text='修改个人信息').click()
    page.wait_for_timeout(500)

    page.screenshot(path='/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/mobile_profile_modal.png', full_page=True)
    print('Screenshot saved to mobile_profile_modal.png')

    nickname_input = page.locator('.profile-modal .profile-input').first
    nickname_input.fill('新昵称测试')
    page.wait_for_timeout(200)

    background_name_after = page.locator('.mine-user-name').inner_text()
    print(f'Background name after edit: {background_name_after}')
    assert background_name == background_name_after, 'Background nickname changed before save'

    modal_title = page.locator('.profile-modal .ant-modal-title').inner_text()
    assert modal_title == '修改个人信息', f'Unexpected modal title: {modal_title}'

    labels = page.locator('.profile-label').all_inner_texts()
    print(f'Profile labels: {labels}')
    assert '昵称' in labels
    assert '简介' in labels
    assert '性别' in labels
    assert '生日' in labels
    assert '所在地' in labels

    platform_no = page.locator('.profile-platform-no').inner_text()
    assert '平台号' in platform_no, platform_no

    print(f'\nConsole errors: {len(errors)}')
    for e in errors:
        print(f'  ERROR: {e}')

    browser.close()

print('\nVerification complete')

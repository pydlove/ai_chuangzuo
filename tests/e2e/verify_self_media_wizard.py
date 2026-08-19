from playwright.sync_api import sync_playwright
import json
import urllib.request

BASE_URL = 'http://127.0.0.1:22345'
API_URL = 'http://127.0.0.1:25050'
SCREENSHOT_DIR = '/tmp/self_media_wizard'


def login():
    req = urllib.request.Request(
        f'{API_URL}/api/v1/user/auth/login',
        data=json.dumps({'email': 'test_selfmedia@example.com', 'password': 'Test1234!'}).encode(),
        headers={'Content-Type': 'application/json'},
        method='POST'
    )
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
        return data['data']['accessToken']


def main():
    token = login()
    print(f'Got access token: {token[:20]}...')

    errors = []
    network_logs = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})

        page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)

        def log_response(resp):
            if resp.status >= 400:
                network_logs.append(f'{resp.status} {resp.url}')

        page.on('response', log_response)

        # Inject auth state and clear onboarding flag
        page.goto(BASE_URL + '/login')
        page.evaluate(f'''() => {{
            localStorage.setItem('aichuangzuo_access_token', '{token}');
            localStorage.removeItem('aichuangzuo_onboarding_done');
        }}''')

        # Navigate to onboarding
        page.goto(BASE_URL + '/console/onboarding')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1500)
        page.screenshot(path=f'{SCREENSHOT_DIR}_onboarding_load.png', full_page=True)

        # Check wizard title
        title = page.locator('text=制定你的自媒体方案').first
        print(f'Wizard title visible: {title.is_visible() if title.count() > 0 else False}')

        # Select a platform directly (skip AI recommendation since LLM not configured)
        platform_card = page.locator('.platform-card:has-text("小红书")').first
        if platform_card.count() > 0:
            platform_card.click()
            print('Selected 小红书 platform')
        else:
            print('WARN: 小红书 platform card not found')

        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}_onboarding_platform_selected.png', full_page=True)

        # Check next button
        next_btn = page.locator('button:has-text("下一步")').first
        print(f'Next button visible: {next_btn.is_visible() if next_btn.count() > 0 else False}')
        print(f'Next button enabled: {next_btn.is_enabled() if next_btn.count() > 0 else False}')

        if next_btn.count() > 0 and next_btn.is_enabled():
            next_btn.click()
            page.wait_for_timeout(1000)
            page.screenshot(path=f'{SCREENSHOT_DIR}_onboarding_step2.png', full_page=True)
            step2_title = page.locator('text=你更适合哪种变现方式？').first
            print(f'Step 2 title visible: {step2_title.is_visible() if step2_title.count() > 0 else False}')

        # Go back to step 1 and test AI recommendation modal wiring
        prev_btn = page.locator('button:has-text("上一步")').first
        if prev_btn.count() > 0 and prev_btn.is_enabled():
            prev_btn.click()
            page.wait_for_timeout(500)

        ai_btn = page.locator('button:has-text("AI 推荐")').first
        if ai_btn.count() > 0:
            ai_btn.click()
            page.wait_for_timeout(500)
            page.screenshot(path=f'{SCREENSHOT_DIR}_onboarding_recommend_modal.png', full_page=True)
            modal_title = page.locator('text=让 AI 推荐最适合你的平台').first
            print(f'AI recommend modal visible: {modal_title.is_visible() if modal_title.count() > 0 else False}')

            # Fill required fields in modal
            for label, value in [
                ('主业还是副业', '副业'),
                ('每周能投入多少时间', '3 - 10 小时'),
                ('期望月收入达到多少', '月入过万'),
                ('能接受多久不盈利', '3 个月'),
                ('倾向于做哪种内容', '图文笔记'),
                ('目标受众是哪类人', '职场人'),
                ('更符合哪种身份', '职场人'),
                ('是否愿意出镜或做视频', '不想做视频'),
            ]:
                btn = page.locator(f'.recommend-form .form-block:has-text("{label}") .option-group button:has-text("{value}")').first
                if btn.count() > 0:
                    btn.click()
                    page.wait_for_timeout(100)

            page.wait_for_timeout(300)
            page.screenshot(path=f'{SCREENSHOT_DIR}_onboarding_recommend_filled.png', full_page=True)
            recommend_btn = page.locator('button:has-text("获取推荐")').first
            print(f'Get recommend button enabled: {recommend_btn.is_enabled() if recommend_btn.count() > 0 else False}')
            if recommend_btn.count() > 0 and recommend_btn.is_enabled():
                recommend_btn.click()
                page.wait_for_timeout(2000)
                page.screenshot(path=f'{SCREENSHOT_DIR}_onboarding_recommend_result.png', full_page=True)
                error_msg = page.locator('text=平台推荐失败').first
                print(f'Recommend error visible (expected due to no LLM): {error_msg.is_visible() if error_msg.count() > 0 else False}')

        # Navigate to workbench
        page.goto(BASE_URL + '/console/workbench')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1500)
        page.screenshot(path=f'{SCREENSHOT_DIR}_workbench_load.png', full_page=True)

        workbench_title = page.locator('text=工作台').first
        print(f'Workbench title visible: {workbench_title.is_visible() if workbench_title.count() > 0 else False}')

        print(f'\nSelf-media plan API calls:')
        for log in network_logs:
            print(f'  {log}')

        print(f'\nConsole errors: {len(errors)}')
        for e in errors:
            print(f'  ERROR: {e}')

        browser.close()

    print('\nVerification complete')


if __name__ == '__main__':
    main()

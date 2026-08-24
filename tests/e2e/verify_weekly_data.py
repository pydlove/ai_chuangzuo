import re

from playwright.sync_api import sync_playwright
import requests

BASE_API = 'http://localhost:25050'
BASE_WEB = 'http://localhost:22346'
EMAIL = 'test_selfmedia@example.com'
PASSWORD = '123456'

# 1. Login via API to get token
login_res = requests.post(
    f'{BASE_API}/api/v1/user/auth/login',
    json={'email': EMAIL, 'password': PASSWORD}
)
login_res.raise_for_status()
token = login_res.json()['data']['accessToken']
print(f'Logged in, token prefix: {token[:20]}...')

errors = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1280, 'height': 900})

    page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)
    page.on('pageerror', lambda err: errors.append(str(err)))

    # 2. Inject token and open console workbench
    page.goto(f'{BASE_WEB}/login')
    page.evaluate(f"""
        localStorage.setItem('aichuangzuo_access_token', '{token}');
        localStorage.setItem('aichuangzuo_refresh_token', 'dummy');
    """)
    page.goto(f'{BASE_WEB}/console/workbench')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)

    # Debug: screenshot current console state
    page.screenshot(path='/tmp/weekly_data_console.png', full_page=True)
    print(f'Console page title: {page.title()}')
    print(f'本周数据 occurrences: {page.locator("text=本周数据").count()}')

    # Close membership promo modal if present
    dismiss = page.locator('text=我已经知道，不再弹出')
    if dismiss.count() > 0:
        dismiss.first.click()
        page.wait_for_timeout(300)

    # 3. Open weekly data modal
    weekly_btn = page.locator('text=本周数据').first
    weekly_btn.scroll_into_view_if_needed()
    weekly_btn.click()
    page.wait_for_timeout(800)

    # 4. Wait for load and verify empty-state summary
    assert page.locator('text=录入本周数据').count() > 0, 'Weekly data modal not opened'
    summary_text = page.locator('.weekly-data-summary').inner_text()
    print(f'Empty summary: {summary_text!r}')
    assert re.search(r'本周共发布\s*0\s*篇', summary_text), f'Empty state should show 0 articles, got: {summary_text}'
    assert re.search(r'总阅读量\s*0', summary_text), f'Empty state should show 0 reads, got: {summary_text}'

    # 5. Add two rows
    page.locator('text=添加文章').first.click()
    page.locator('.weekly-data-title input').nth(0).fill('UI 测试文章一')
    page.locator('.weekly-data-reads input').nth(0).fill('333')

    page.locator('text=添加文章').first.click()
    page.locator('.weekly-data-title input').nth(1).fill('UI 测试文章二')
    page.locator('.weekly-data-reads input').nth(1).fill('666')

    # Verify summary updates with valid rows only
    summary_text = page.locator('.weekly-data-summary').inner_text()
    print(f'Filled summary before save: {summary_text!r}')
    assert re.search(r'本周共发布\s*2\s*篇', summary_text), f'Should show 2 valid articles, got: {summary_text}'
    assert re.search(r'总阅读量\s*999', summary_text), f'Should show 999 total reads, got: {summary_text}'

    # 6. Save using primary button in modal footer area
    page.screenshot(path='/tmp/weekly_data_modal.png', full_page=False)
    save_btn = page.locator('.weekly-data-modal .weekly-data-actions .ant-btn-primary').first
    save_btn.scroll_into_view_if_needed()
    save_btn.click()
    page.wait_for_timeout(1200)

    # 7. Reopen modal and verify data persisted
    page.locator('text=本周数据').first.click()
    page.wait_for_timeout(800)

    summary_text = page.locator('.weekly-data-summary').inner_text()
    print(f'Summary after reopen: {summary_text!r}')
    assert re.search(r'本周共发布\s*2\s*篇', summary_text), f'Should show 2 articles after reopen, got: {summary_text}'
    assert re.search(r'总阅读量\s*999', summary_text), f'Should show 999 total reads after reopen, got: {summary_text}'

    title_inputs = page.locator('.weekly-data-title input').all()
    read_inputs = page.locator('.weekly-data-reads input').all()
    titles = [t.input_value() for t in title_inputs]
    reads = [r.input_value() for r in read_inputs]
    print(f'Titles after reopen: {titles}')
    print(f'Reads after reopen: {reads}')

    assert 'UI 测试文章一' in titles, 'First title not persisted'
    assert 'UI 测试文章二' in titles, 'Second title not persisted'
    assert '333' in reads, 'First reads not persisted'
    assert '666' in reads, 'Second reads not persisted'

    # 8. Screenshot for manual inspection
    page.screenshot(path='/tmp/weekly_data_verify.png', full_page=False)

    browser.close()

print(f'\nConsole/page errors: {len(errors)}')
for e in errors:
    print(f'  ERROR: {e}')

print('\nWeekly data frontend verification complete')

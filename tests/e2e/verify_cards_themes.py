"""验证 CardsModal 在浅色/暗色主题下的全部下载按钮与菜单栏样式."""
from playwright.sync_api import sync_playwright
import os

APP_URL = os.environ.get('APP_URL', 'http://127.0.0.1:5173')

errors = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={'width': 1280, 'height': 900}, accept_downloads=True)
    page = context.new_page()

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    article = {
        'title': '如何高效管理时间：从混乱到掌控的 5 个方法',
        'body': '时间对每个人来说都是公平的。\n\n【优先级排序】\n先做最重要的事。',
        'completedAt': '2026-06-22',
        'wordCount': 1500,
        'style': '专业严谨'
    }
    queue_item = {
        'id': 12345,
        'title': article['title'],
        'platform': '公众号',
        'wordCount': 1500,
        'style': {'name': '专业严谨'},
        'template': {'name': '公众号标准模板'},
        'status': 'completed',
        'progress': 100,
        'createdAt': '2026-06-22T00:00:00.000Z',
        'completedAt': '2026-06-22T00:05:00.000Z',
        'content': {'title': article['title'], 'body': article['body'], 'style': '专业严谨'}
    }

    # 浅色主题
    page.goto(f'{APP_URL}/')
    page.wait_for_load_state('networkidle')
    page.evaluate(f"() => {{ document.body.setAttribute('data-theme', 'light'); localStorage.setItem('aichuangzuo_current_article', JSON.stringify({article})); localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify([{queue_item}])) }}")
    page.goto(f'{APP_URL}/console/create')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(600)
    page.locator('button:has-text("去导出")').first.click()
    page.wait_for_timeout(600)
    page.locator('.export-float-btn.danger:has-text("生成贴图")').first.click()
    page.wait_for_timeout(800)
    page.screenshot(path='/tmp/cards_modal_light.png', full_page=True)

    # 验证浅色主题：全部下载按钮应为 xiaohongshu 红
    download_btn = page.locator('.cards-modal-download-all').first
    bg = download_btn.evaluate('el => getComputedStyle(el).backgroundColor')
    print(f"[light] download-all background: {bg}")

    page.locator('.cards-modal-close').first.click()
    page.wait_for_timeout(200)

    # 暗色主题
    page.evaluate("() => { document.body.setAttribute('data-theme', 'dark') }")
    page.locator('button:has-text("去导出")').first.click()
    page.wait_for_timeout(600)
    page.locator('.export-float-btn.danger:has-text("生成贴图")').first.click()
    page.wait_for_timeout(800)
    page.screenshot(path='/tmp/cards_modal_dark.png', full_page=True)

    # 验证暗色主题：tab 边框应为红色调
    tab = page.locator('.cards-modal-tab').first
    tab_border = tab.evaluate('el => getComputedStyle(el).borderColor')
    tab_bg = tab.evaluate('el => getComputedStyle(el).backgroundColor')
    print(f"[dark] tab background: {tab_bg}")
    print(f"[dark] tab border-color: {tab_border}")

    print(f"\nConsole errors: {len(errors)}")
    for e in errors:
        print(f"  ERROR: {e}")

    browser.close()

print("\nVerification complete")

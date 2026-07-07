from playwright.sync_api import sync_playwright
import os
import time

APP_URL = os.environ.get('APP_URL', 'http://127.0.0.1:5173')

errors = []
downloads = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={'width': 1280, 'height': 900}, accept_downloads=True)
    page = context.new_page()

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    page.goto(f'{APP_URL}/')
    page.wait_for_load_state('networkidle')
    article = {
        'title': '如何高效管理时间：从混乱到掌控的 5 个方法',
        'body': '时间对每个人来说都是公平的。\n\n【优先级排序】\n先做最重要的事。\n\n【时间块】\n给任务一个容器。',
        'completedAt': '2026-06-22',
        'wordCount': 1500,
        'style': '专业严谨'
    }
    page.evaluate(f"() => {{ localStorage.setItem('aichuangzuo_current_article', JSON.stringify({article})) }}")

    page.goto(f'{APP_URL}/console/preview')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(500)

    # 1. 复制正文
    copy_btn = page.locator('button:has-text("复制正文")').first
    copy_btn.click()
    page.wait_for_timeout(300)
    print("Copy text button clicked successfully")

    # 2. 导出 Word
    with page.expect_download() as download_info:
        page.locator('button:has-text("导出 Word")').first.click()
    download = download_info.value
    download_path = f"/tmp/{download.suggested_filename}"
    download.save_as(download_path)
    print(f"Word download saved: {download_path}, size: {os.path.getsize(download_path)} bytes")

    # 3. 生成贴图
    card_btn = page.locator('.floating-action-bar button:has-text("生成贴图")').first
    print(f"Generate cards button visible: {card_btn.is_visible()}")
    card_btn.click()
    page.wait_for_timeout(1000)
    page.screenshot(path='/tmp/verify_after_card_click.png', full_page=True)

    modal = page.locator('.cards-modal-overlay').first
    print(f"Cards modal visible: {modal.is_visible()}")

    if modal.is_visible():
        page.screenshot(path='/tmp/verify_cards_modal.png', full_page=True)

        # 验证 tab 数量
        tabs = page.locator('.cards-modal-tab').all()
        print(f"Card style tabs: {len(tabs)}")

        # 验证卡片数量（封面 + 2 个内容卡片）
        canvases = page.locator('.cards-modal-canvas').all()
        print(f"Card canvases: {len(canvases)}")

        # 切换公众号风格
        wechat_tab = page.locator('.cards-modal-tab:has-text("公众号")').first
        wechat_tab.click()
        page.wait_for_timeout(500)

        # 下载单张贴图
        with page.expect_download() as download_info:
            canvases[0].click()
        card_download = download_info.value
        card_path = f"/tmp/{card_download.suggested_filename}"
        card_download.save_as(card_path)
        print(f"Card download saved: {card_path}, size: {os.path.getsize(card_path)} bytes")

        # 关闭弹窗
        page.locator('.cards-modal-close').first.click()
        page.wait_for_timeout(200)
        print(f"Cards modal closed: {not modal.is_visible()}")

    print(f"\nConsole errors: {len(errors)}")
    for e in errors:
        print(f"  ERROR: {e}")

    browser.close()

print("\nVerification complete")

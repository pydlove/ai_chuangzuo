"""验证 CreateIndex.vue 的生成贴图弹框复用 CardsModal 组件."""
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

    # 准备：写一篇文章到 localStorage，并放进 generation_queue 模拟已完成任务
    article = {
        'title': '如何高效管理时间：从混乱到掌控的 5 个方法',
        'body': '时间对每个人来说都是公平的。\n\n【优先级排序】\n先做最重要的事。\n\n【时间块】\n给任务一个容器。\n\n【总结】\n坚持才有效果。',
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
    page.evaluate(f"() => {{ localStorage.setItem('aichuangzuo_current_article', JSON.stringify({article})); localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify([{queue_item}])) }}")

    page.goto(f'{APP_URL}/console/create')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(800)

    page.screenshot(path='/tmp/verify_create_initial.png', full_page=True)

    # 1. 点击已完成任务的「去导出」按钮（在右侧生成队列）
    export_btn = page.locator('button:has-text("去导出")').first
    print(f"Export button visible: {export_btn.is_visible()}")
    export_btn.click()
    page.wait_for_timeout(800)
    page.screenshot(path='/tmp/verify_create_export_modal.png', full_page=True)

    # 2. 在导出弹框中点击「生成贴图」按钮
    cards_btn = page.locator('.export-float-btn.danger:has-text("生成贴图")').first
    print(f"Cards button visible: {cards_btn.is_visible()}")
    cards_btn.click()
    page.wait_for_timeout(1200)
    page.screenshot(path='/tmp/verify_create_cards_modal.png', full_page=True)

    # 3. 验证弹框出现
    overlay = page.locator('.cards-modal-overlay').first
    print(f"Cards modal visible: {overlay.is_visible()}")

    if overlay.is_visible():
        # 验证 tabs 数量（应该有 6 个：小红书/公众号/抖音/文艺/极简/商务）
        tabs = page.locator('.cards-modal-tab').all()
        print(f"Card style tabs: {len(tabs)}")

        # 验证 canvas 数量（封面 + 3 个内容卡片）
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
        page.wait_for_timeout(300)

    print(f"\nConsole errors: {len(errors)}")
    for e in errors:
        print(f"  ERROR: {e}")

    browser.close()

print("\nVerification complete")

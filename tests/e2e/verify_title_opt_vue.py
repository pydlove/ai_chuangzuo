from playwright.sync_api import sync_playwright

errors = []

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1280, 'height': 900})

    def on_console(msg):
        if msg.type == 'error':
            errors.append(msg.text)

    page.on('console', on_console)

    # Seed localStorage with a sample article before navigating
    page.goto('http://127.0.0.1:5173/')
    page.wait_for_load_state('networkidle')
    article = {
        'title': '如何高效管理时间：从混乱到掌控的 5 个方法',
        'body': '时间对每个人来说都是公平的。\n\n【优先级排序】\n先做最重要的事。\n\n【时间块】\n给任务一个容器。',
        'completedAt': '2026-06-22',
        'wordCount': 1500,
        'style': '专业严谨'
    }
    page.evaluate(f"() => {{ localStorage.setItem('aichuangzuo_current_article', JSON.stringify({article})) }}")

    # Navigate to preview page
    page.goto('http://127.0.0.1:5173/console/preview')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(500)

    # Screenshot initial state
    page.screenshot(path='/tmp/verify_title_opt_initial.png', full_page=True)

    # Verify article title is rendered
    title = page.locator('.article-title').first
    print(f"Article title visible: {title.is_visible()}")
    print(f"Article title text: {title.inner_text() if title.is_visible() else 'N/A'}")

    # Click AI optimize title button in floating action bar
    btn = page.locator('button:has-text("AI 优化标题")').first
    print(f"Optimize button visible: {btn.is_visible()}")
    btn.click()
    page.wait_for_timeout(500)

    # Screenshot modal
    page.screenshot(path='/tmp/verify_title_opt_modal.png', full_page=True)

    # Verify modal
    modal = page.locator('.title-opt-overlay').first
    print(f"Title opt modal visible: {modal.is_visible()}")

    if modal.is_visible():
        # Verify original title reference
        original = page.locator('.title-opt-original').first
        print(f"Original title ref visible: {original.is_visible()}")
        if original.is_visible():
            print(f"Original title ref text: {original.inner_text()[:100]}")

        # Verify AI recommended titles
        ai_items = page.locator('.title-opt-list').first.locator('.title-opt-item').all()
        print(f"AI recommended title items: {len(ai_items)}")

        # Verify platform tabs
        tabs = page.locator('.title-opt-platform-tab').all()
        print(f"Platform tabs: {len(tabs)}")
        for tab in tabs:
            print(f"  Tab: {tab.inner_text()}")

        # Select second AI title
        if len(ai_items) >= 2:
            target_title = ai_items[1].locator('.title-opt-item-text').first.inner_text()
            print(f"Selecting title: {target_title}")
            ai_items[1].click()
            page.wait_for_timeout(200)

            # Confirm
            confirm = page.locator('.title-opt-btn-confirm').first
            print(f"Confirm button enabled: {not confirm.is_disabled()}")
            confirm.click()
            page.wait_for_timeout(500)

            # Verify title changed
            new_title = page.locator('.article-title').first
            print(f"New article title: {new_title.inner_text()}")
            print(f"Title changed: {new_title.inner_text() == target_title}")

    print(f"\nConsole errors: {len(errors)}")
    for e in errors:
        print(f"  ERROR: {e}")

    browser.close()

print("\nVerification complete")

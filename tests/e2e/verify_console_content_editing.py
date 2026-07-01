from playwright.sync_api import sync_playwright

BASE = 'http://127.0.0.1:22345'


def test_console_content_editing():
    errors = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})

        def on_console(msg):
            if msg.type == 'error':
                errors.append(msg.text)
        page.on('console', on_console)

        article = {
            'id': 'test-edit-001',
            'title': '如何高效管理时间：从混乱到掌控的 5 个方法',
            'body': '时间对每个人来说都是公平的。\n\n【优先级排序】\n先做最重要的事。\n\n- 列出今日最重要的 3 件事\n- 先完成最难的那一件\n\n【时间块】\n给任务一个容器。\n\n> 关键结论：管理时间本质是管理注意力。',
            'completedAt': '2026-06-22',
            'wordCount': 1500,
            'style': '专业严谨'
        }

        queue = [{
            'id': 'test-edit-001',
            'status': 'completed',
            'title': article['title'],
            'platform': '微信公众号',
            'wordCount': article['wordCount'],
            'style': article['style'],
            'completedAt': article['completedAt'],
            'content': {'title': article['title'], 'body': article['body']}
        }]

        # Seed data
        page.goto(BASE + '/')
        page.wait_for_load_state('networkidle')
        page.evaluate(f"""() => {{
            localStorage.setItem('aichuangzuo_current_article', JSON.stringify({article}));
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify({queue}));
        }}""")

        # 1. Open preview
        page.goto(BASE + '/console/preview')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)

        assert page.locator('.article-title:has-text("如何高效管理时间")').count() > 0, 'preview title not rendered'

        # 2. Inline edit
        page.click('button:has-text("编辑正文")')
        page.wait_for_timeout(300)
        assert page.locator('.edit-floating-bar').count() > 0, 'edit floating bar not shown'

        # 3. Modify title
        title_block = page.locator('.editing-body .edit-block.title').first
        title_block.click()
        title_block.fill('用户修改后的标题')
        page.wait_for_timeout(200)

        # 4. Save inline edit
        page.click('.edit-floating-bar button:has-text("保存修改")')
        page.wait_for_timeout(500)

        assert page.locator('.article-title:has-text("用户修改后的标题")').count() > 0, 'inline edited title not applied'

        # 5. Verify queue sync
        saved_queue = page.evaluate("""() => {
            try {
                return JSON.parse(localStorage.getItem('aichuangzuo_generation_queue') || '[]');
            } catch(e) { return []; }
        }""")
        assert any(item['title'] == '用户修改后的标题' for item in saved_queue), 'queue not synced after inline edit'

        # 6. Open standalone edit page
        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)

        assert page.locator('.edit-block-area.title').count() > 0, 'edit page blocks not rendered'

        # 7. Modify title on edit page
        edit_title = page.locator('.edit-block-area.title').first
        edit_title.click()
        edit_title.fill('独立编辑页修改后的标题')
        page.wait_for_timeout(200)

        # 8. Save on edit page
        page.click('.edit-actions .save')
        page.wait_for_timeout(500)

        # 9. Verify back on preview
        assert page.url.endswith('/console/preview'), 'did not redirect to preview after edit page save'
        assert page.locator('.article-title:has-text("独立编辑页修改后的标题")').count() > 0, 'edit page save not reflected'

        page.screenshot(path='/tmp/verify_console_content_editing.png', full_page=True)

        browser.close()
        print('console content editing verification passed')
        if errors:
            print(f'console errors: {len(errors)}')
            for e in errors:
                print(f'  ERROR: {e}')


if __name__ == '__main__':
    test_console_content_editing()

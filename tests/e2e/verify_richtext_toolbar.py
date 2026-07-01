from playwright.sync_api import sync_playwright

BASE = 'http://localhost:22345'


def test_richtext_toolbar():
    errors = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})
        page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)

        article = {
            'id': 'test-richtext-001',
            'title': '富文本工具栏测试',
            'body': '【小标题】\n\n正文段落。\n\n> 引用文本\n\n- 列表一\n- 列表二',
            'completedAt': '2026-07-01',
            'wordCount': 100,
            'style': '专业严谨'
        }
        queue = [{
            'id': 'test-richtext-001',
            'status': 'completed',
            'title': article['title'],
            'platform': '微信公众号',
            'wordCount': 100,
            'style': '专业严谨',
            'completedAt': '2026-07-01',
            'content': {'title': article['title'], 'body': article['body']}
        }]

        page.goto(BASE + '/')
        page.wait_for_load_state('networkidle')
        page.evaluate("""(data) => {
            localStorage.setItem('aichuangzuo_current_article', JSON.stringify(data.article));
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify(data.queue));
        }""", {'article': article, 'queue': queue})

        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)

        editor = page.locator('.edit-editor').first
        assert editor.count() > 0, 'editor not rendered'

        # 操作 1:选中第一个 p(正文段落),加下划线
        editor.evaluate("""(el) => {
            const ps = el.querySelectorAll('p');
            const p = ps[0];
            const range = document.createRange();
            range.selectNodeContents(p);
            const sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(range);
        }""")
        page.click('button[title="下划线"]')
        page.wait_for_timeout(200)

        # 操作 2:点工具栏的"居中"按钮(可见,Ant Design Vue 在中文字符间加空格)
        page.locator('.edit-toolbar .ant-btn').filter(has_text='居').filter(has_text='中').first.click()
        page.wait_for_timeout(200)

        # 保存
        page.click('.edit-actions .save')
        page.wait_for_timeout(500)

        # 验证 1:localStorage 中存在 styleOverrides 字段
        saved = page.evaluate("() => JSON.parse(localStorage.getItem('aichuangzuo_current_article') || '{}')")
        assert 'styleOverrides' in saved, 'styleOverrides not persisted'
        assert saved['styleOverrides']['blocks'] or saved['styleOverrides']['inlines'], \
            f'styleOverrides is empty: {saved["styleOverrides"]}'

        # 验证 2:刷新后重新进入编辑页,样式仍存在
        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)
        reloaded_html = page.locator('.edit-editor').first.evaluate('(el) => el.innerHTML')
        assert '<u>' in reloaded_html, f'underline style not reloaded. html={reloaded_html[:300]}'
        assert 'text-align: center' in reloaded_html or 'text-align:center' in reloaded_html, \
            f'align style not reloaded. html={reloaded_html[:300]}'

        # 验证 3:旧文章(无 styleOverrides)不报错
        page.evaluate("""() => {
            const old = {
                id: 'old-001', title: '旧文章', body: '正文',
                completedAt: '2026-06-01', wordCount: 10, style: '通用'
            };
            localStorage.setItem('aichuangzuo_current_article', JSON.stringify(old));
        }""")
        page.goto(BASE + '/console/edit')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(500)
        assert page.locator('.edit-editor').count() > 0, 'old article failed to load'

        page.screenshot(path='/tmp/verify_richtext_toolbar.png', full_page=True)
        browser.close()
        print('richtext toolbar verification passed')
        if errors:
            print(f'console errors: {len(errors)}')
            for e in errors:
                print(f'  ERROR: {e}')


if __name__ == '__main__':
    test_richtext_toolbar()

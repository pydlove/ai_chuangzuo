from playwright.sync_api import sync_playwright
import time

BASE = 'http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content'


def test_content_editing():
    errors = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        def on_console(msg):
            if msg.type == 'error':
                errors.append(msg.text)
        page.on('console', on_console)

        # 清理旧数据
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)
        page.evaluate("""() => {
            try { localStorage.removeItem('aichuangzuo_article_edits'); } catch(e) {}
        }""")

        # 1. 打开 preview.html 进入编辑态（点击标题直接触发）
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)
        page.click('.preview-title')
        time.sleep(0.3)

        # 2. 修改标题
        title = page.locator('.preview-title').first
        title.fill('用户修改后的标题')

        # 3. 保存
        page.click('.article-edit-fab .save-btn')
        time.sleep(0.3)

        # 4. 验证 localStorage
        saved = page.evaluate("""() => {
            try {
                var raw = localStorage.getItem('aichuangzuo_article_edits');
                var edits = raw ? JSON.parse(raw) : null;
                return edits && edits.blocks.some(function(b) {
                    return b.type === 'title' && b.html.includes('用户修改后的标题');
                });
            } catch(e) { return false; }
        }""")
        assert saved, 'edited title not saved to localStorage'

        # 5. 刷新页面确认保留
        page.reload(wait_until='networkidle')
        time.sleep(0.5)
        assert page.locator('.preview-title:has-text("用户修改后的标题")').count() > 0, 'saved edit not applied after reload'

        # 6. 进入 edit.html 修改并保存
        page.goto(BASE + '/edit.html', wait_until='networkidle')
        time.sleep(0.5)
        area = page.locator('.edit-block-area.title').first
        area.fill('独立编辑页修改后的标题')
        page.click('.edit-actions .save')
        time.sleep(0.5)

        # 7. 返回 preview.html 验证
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)
        assert page.locator('.preview-title:has-text("独立编辑页修改后的标题")').count() > 0, 'edit page save not reflected on preview'

        page.screenshot(path='/tmp/content_editing_verify.png', full_page=True)

        # 清理
        page.evaluate("""() => {
            try { localStorage.removeItem('aichuangzuo_article_edits'); } catch(e) {}
        }""")

        browser.close()
        print('content editing verification passed')
        if errors:
            print(f'console errors: {len(errors)}')
            for e in errors:
                print(f'  ERROR: {e}')


if __name__ == '__main__':
    test_content_editing()

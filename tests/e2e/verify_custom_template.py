from playwright.sync_api import sync_playwright
import time

BASE = 'http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content'

def test_custom_template():
    errors = []
    template_name = '蓝调小红书_' + str(int(time.time()))

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        def on_console(msg):
            if msg.type == 'error':
                errors.append(msg.text)

        page.on('console', on_console)

        # 1. 打开创作页
        page.goto(BASE + '/create.html', wait_until='networkidle')
        time.sleep(0.5)

        # 清理已有的自定义模板，保证测试独立
        page.evaluate("""() => {
            try { localStorage.removeItem('aichuangzuo_custom_templates'); } catch(e) {}
        }""")

        # 2. 打开模板库
        page.click('#screen-create button:has-text("+ 模板库")')
        time.sleep(0.3)

        # 确认弹窗出现
        assert page.locator('#template-lib-modal').count() > 0, 'template library modal not found'

        # 3. 切到「我的模板」
        page.click('#template-lib-modal button:has-text("我的模板")')
        time.sleep(0.3)

        # 4. 点击创建自定义模板
        page.click('#template-lib-modal button:has-text("创建自定义模板")')
        time.sleep(0.3)

        # 确认编辑器出现
        assert page.locator('#custom-template-editor-modal').count() > 0, 'custom template editor not found'

        # 5. 输入名称
        page.fill('#custom-template-name', template_name)

        # 6. 选择商务蓝主题（第二个色块）
        page.click('.custom-theme-btn:nth-child(2)')

        # 7. 选择标题居中
        page.click('.custom-title-btn:has-text("居中")')

        # 8. 选择高亮背景块
        page.click('.custom-highlight-btn:has-text("背景块")')

        # 9. 保存
        page.click('#custom-template-save')
        time.sleep(0.3)

        # 10. 验证 localStorage 已保存
        saved = page.evaluate("""() => {
            try {
                var raw = localStorage.getItem('aichuangzuo_custom_templates');
                var arr = raw ? JSON.parse(raw) : [];
                return arr.some(function(t) { return t.name === '%s'; });
            } catch(e) { return false; }
        }""" % template_name)
        assert saved, 'custom template not saved to localStorage'

        # 11. 重新打开模板库并切到我的模板
        page.click('#screen-create button:has-text("+ 模板库")')
        time.sleep(0.3)
        page.click('#template-lib-modal button:has-text("我的模板")')
        time.sleep(0.3)

        # 12. 确认我的模板列表出现
        row = page.locator('#template-lib-modal .template-lib-row:has-text("%s")' % template_name)
        assert row.count() > 0, 'saved custom template not found in list'

        # 13. 应用该模板
        row.click()
        time.sleep(0.2)
        page.click('#template-lib-modal button:has-text("应用此模板")')
        time.sleep(0.3)

        # 14. 进入预览页
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)

        # 15. 验证预览页使用了自定义模板
        preview_template = page.evaluate("""() => {
            var el = document.querySelector('.article-preview');
            return el ? el.getAttribute('data-template') : null;
        }""")
        assert preview_template and preview_template.startswith('custom_'), 'preview page did not use custom template'

        # 16. 截图留档
        page.screenshot(path='/tmp/custom_template_preview.png', full_page=True)

        # 清理测试数据
        page.evaluate("""() => {
            try { localStorage.removeItem('aichuangzuo_custom_templates'); } catch(e) {}
        }""")

        browser.close()

        print('custom template verification passed')
        if errors:
            print(f'console errors: {len(errors)}')
            for e in errors:
                print(f'  ERROR: {e}')

if __name__ == '__main__':
    test_custom_template()

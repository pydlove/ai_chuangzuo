from playwright.sync_api import sync_playwright

URL = 'http://localhost:22347'

def seed_data(page):
    page.evaluate('''
        () => {
            const queue = Array.from({ length: 7 }, (_, i) => ({
                id: i + 1,
                title: `测试文章 ${i + 1}`,
                platform: '微信公众号',
                wordCount: 1200,
                style: '职场干货',
                template: '默认模板',
                status: 'completed',
                completedAt: new Date(Date.now() - i * 1000).toISOString(),
                content: { title: `测试文章 ${i + 1}`, body: '正文内容' }
            }));
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify(queue));
        }
    ''')

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        page.goto(f'{URL}/console/create')
        seed_data(page)
        page.reload()
        page.wait_for_timeout(500)

        items = page.locator('.queue-panel-item')
        assert items.count() == 5, f'Expected 5 visible queue items, got {items.count()}'

        more = page.locator('.queue-panel-more')
        assert more.count() == 1, 'Expected overflow prompt'
        assert '还有 2 个任务' in more.inner_text()

        page.screenshot(path='tests/e2e/screenshots/create_queue_overflow.png')
        print('创作页生成队列 5 条限制验证通过')

        browser.close()

if __name__ == '__main__':
    main()

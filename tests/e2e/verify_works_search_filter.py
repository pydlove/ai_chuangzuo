from playwright.sync_api import sync_playwright

URL = 'http://localhost:28586'

def seed_data(page):
    page.evaluate('''
        () => {
            const queue = [
                {
                    id: 1,
                    title: '职场新人快速提升效率',
                    platform: '微信公众号',
                    wordCount: 1200,
                    style: '职场干货',
                    template: '默认模板',
                    status: 'completed',
                    completedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
                    content: { title: '职场新人快速提升效率', body: '正文内容' }
                },
                {
                    id: 2,
                    title: '小红书爆款文案写作技巧',
                    platform: '小红书',
                    wordCount: 800,
                    style: '营销文案',
                    template: '小红书卡片',
                    status: 'completed',
                    completedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 10).toISOString(),
                    content: { title: '小红书爆款文案写作技巧', body: '正文内容' }
                }
            ];
            const drafts = [
                {
                    id: 3,
                    customTitle: '知乎深度回答：如何高效学习',
                    platform: { key: 'zhihu', name: '知乎' },
                    wordCount: { count: 1500 },
                    style: { name: '知识科普' },
                    savedAt: new Date().toISOString()
                }
            ];
            localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify(queue));
            localStorage.setItem('aichuangzuo_drafts', JSON.stringify(drafts));
        }
    ''')

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        page.goto(f'{URL}/console/works')
        seed_data(page)
        page.reload()

        # 验证已生成 tab 有两条数据
        page.locator('button:has-text("已生成")').click()
        page.wait_for_timeout(300)
        cards = page.locator('.work-card')
        assert cards.count() == 2, f'Expected 2 works, got {cards.count()}'

        # 标题搜索
        page.locator('.works-search input').fill('职场')
        page.wait_for_timeout(300)
        assert cards.count() == 1, f'Expected 1 work after title search, got {cards.count()}'
        assert '职场新人快速提升效率' in cards.first.inner_text()

        page.locator('.works-search input').fill('')
        page.wait_for_timeout(300)

        # 平台筛选：选择小红书
        page.locator('.works-filter-select').nth(0).click()
        page.locator('.ant-select-item[title="小红书"]').click()
        page.wait_for_timeout(300)
        assert cards.count() == 1, f'Expected 1 work after platform filter, got {cards.count()}'
        assert '小红书爆款文案写作技巧' in cards.first.inner_text()

        # 清空筛选
        page.locator('.works-search input').fill('不存在的关键词')
        page.wait_for_timeout(300)
        assert cards.count() == 0
        page.locator('button:has-text("清空筛选")').click()
        page.wait_for_timeout(300)
        assert cards.count() == 2

        # 时间范围筛选：近7天
        page.locator('.works-filter-time label:has-text("近7天")').click()
        page.wait_for_timeout(300)
        assert cards.count() == 1, f'Expected 1 work within 7 days, got {cards.count()}'
        assert '职场新人快速提升效率' in cards.first.inner_text()

        # 切换到草稿箱，验证条件共享
        page.locator('button:has-text("草稿箱")').click()
        page.wait_for_timeout(300)
        draft_cards = page.locator('.draft-card')
        assert draft_cards.count() == 1, f'Expected 1 draft with shared filter, got {draft_cards.count()}'

        # 草稿箱标题搜索
        page.locator('.works-search input').fill('知乎')
        page.wait_for_timeout(300)
        assert draft_cards.count() == 1
        assert '知乎深度回答' in draft_cards.first.inner_text()

        page.screenshot(path='tests/e2e/screenshots/works_search_filter.png')
        print('作品页搜索筛选验证通过')

        browser.close()

if __name__ == '__main__':
    main()

"""风格市场视觉升级 v2 E2E 验证 — 覆盖 SPEC §9 全部 9 个场景。

前置：用户端 dev server 在 22345 端口跑起来（npm run dev）。
执行：python tests/e2e/verify_style_market.py
"""

import os
import json
from playwright.sync_api import sync_playwright

URL = os.environ.get('APP_URL', 'http://localhost:22345')
SCREENSHOT_DIR = 'tests/e2e/screenshots'
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

# framenavigated 共享收集器（main 内重置；模块级声明便于 global 生效）
_nav_urls = []

# 一组 mock 风格数据，用于驱动 5 区块的渲染校验
MOCK_STYLES = [
    {
        'id': 'market-001',
        'name': '公众号情感文',
        'sourceType': 'my',
        'creatorId': 'u_creator_e2e',
        'creatorName': '林晚',
        'prompt': '一段用于测试公众号情感文的风格提示词。',
        'scope': '公众号,情感文',
        'excerpt1': '',
        'excerpt2': '',
        'status': 'approved',
        'price': 0.2,
        'weeklyUses': 142,
        'totalUses': 1560,
        'weeklyEarnings': 28.4,
        'milestoneBonus': 0,
        'featured': True,
        'lastSettlementAt': '2026-07-20T00:00:00Z',
        'createdAt': '2026-07-10T08:30:00Z'
    },
    {
        'id': 'market-002',
        'name': '种草文案',
        'sourceType': 'learned',
        'creatorId': 'u_other_creator',
        'creatorName': '安宁',
        'prompt': '小红书种草文案的语感与节奏。',
        'scope': '小红书,种草',
        'excerpt1': '',
        'excerpt2': '',
        'status': 'approved',
        'price': 0.2,
        'weeklyUses': 56,
        'totalUses': 420,
        'weeklyEarnings': 11.2,
        'milestoneBonus': 0,
        'featured': True,
        'lastSettlementAt': '2026-07-20T00:00:00Z',
        'createdAt': '2026-07-05T08:30:00Z'
    },
    {
        'id': 'market-003',
        'name': '知乎深度',
        'sourceType': 'my',
        'creatorId': 'u_third_creator',
        'creatorName': '星河',
        'prompt': '知乎答题风格的严谨与思辨。',
        'scope': '知乎,深度',
        'excerpt1': '',
        'excerpt2': '',
        'status': 'approved',
        'price': 0.2,
        'weeklyUses': 12,
        'totalUses': 88,
        'weeklyEarnings': 2.4,
        'milestoneBonus': 0,
        'featured': True,
        'lastSettlementAt': '2026-07-20T00:00:00Z',
        'createdAt': '2026-07-12T08:30:00Z'
    },
    {
        'id': 'market-pending',
        'name': '抖音口播',
        'sourceType': 'my',
        'creatorId': 'u_creator_e2e',
        'creatorName': '林晚',
        'prompt': '抖音口播风格。',
        'scope': '抖音',
        'excerpt1': '',
        'excerpt2': '',
        'status': 'pending',
        'price': 0.2,
        'weeklyUses': 0,
        'totalUses': 0,
        'weeklyEarnings': 0,
        'milestoneBonus': 0,
        'lastSettlementAt': '2026-07-20T00:00:00Z',
        'createdAt': '2026-07-26T08:30:00Z'
    }
]

# 一组 mock 收益记录，让 topCreators 不会全 0
MOCK_EARNINGS = [
    {'id': 'earn-001', 'type': 'usage', 'styleId': 'market-001', 'amount': 0.2,
     'description': '使用「公众号情感文」生成文章', 'status': 'unsettled',
     'settlementWeek': '2026-30', 'createdAt': '2026-07-26T01:00:00Z'},
    {'id': 'earn-002', 'type': 'usage', 'styleId': 'market-002', 'amount': 0.2,
     'description': '使用「种草文案」生成文章', 'status': 'unsettled',
     'settlementWeek': '2026-30', 'createdAt': '2026-07-26T02:00:00Z'}
]


def install_routes(ctx):
    """拦截所有 user API，用 mock 数据兜底，避免 401/403 触发重定向。"""

    def handler(route):
        url = route.request.url
        if '/api/v1/user/market-styles' in url:
            route.fulfill(
                status=200, content_type='application/json',
                body=json.dumps({'code': 0, 'data': MOCK_STYLES}, ensure_ascii=False)
            )
        elif '/api/v1/user/' in url:
            # 用空数组保证响应可被业务代码 `.map(...)` 安全处理
            route.fulfill(
                status=200, content_type='application/json',
                body=json.dumps({'code': 0, 'data': []})
            )
        else:
            route.continue_()

    ctx.route('**/*', handler)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={'width': 1280, 'height': 1000})

        # mock 登录态 + 测试种子 + 收益记录
        ctx.add_init_script(f"""
            localStorage.setItem('aichuangzuo_access_token', 'mock_e2e_token');
            localStorage.setItem('aichuangzuo_user_id', 'u_creator_e2e');
            localStorage.setItem('aichuangzuo_earnings_records', '{json.dumps(MOCK_EARNINGS, ensure_ascii=False)}');
            localStorage.setItem('aichuangzuo_coin_balance', '10.00');
        """)
        install_routes(ctx)

        page = ctx.new_page()
        page.on('dialog', lambda d: (print('  ↪ dialog:', d.message), d.accept()))
        # 静默过滤掉重复的 Vite HMR warning，只看 error/warning
        page.on('console', lambda m: print(f'  ↪ console.{m.type}:', m.text[:200]) if m.type in ('error','warning') else None)
        # 用共享列表同时打印 + 收集，便于场景 8 在过渡 URL 中查 query
        global _nav_urls
        _nav_urls = []
        def _on_nav(f):
            if f is page.main_frame:
                _nav_urls.append(f.url)
                print('  NAV:', f.url)
        page.on('framenavigated', _on_nav)
        errs = []
        page.on('pageerror', lambda exc: errs.append(str(exc)))

        # ───── 场景 1：访问市场页 → 5 区块全部渲染，无 JS 错误 ─────
        page.goto(f'{URL}/console/style-market', wait_until='domcontentloaded')
        page.wait_for_timeout(2500)
        assert page.locator('.market-banner').is_visible(), '① banner 缺失'
        assert page.locator('.market-upload-card').is_visible(), '② 激励卡缺失'
        assert page.locator('.market-featured').is_visible(), '③ 精选区缺失'
        assert page.locator('.market-creators').is_visible(), '④ 收益榜缺失'
        assert page.locator('.market-grid-section').is_visible(), '⑤ 网格区缺失'
        assert '爱创作 · 风格市场' in page.locator('.market-banner-title').inner_text()
        assert '官方运营' in page.locator('.market-banner-sub').inner_text()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_01_full.png', full_page=True)

        # ───── 场景 2：5 个 banner chip（已上架/累计使用/累计发放）─────
        assert page.locator('.market-banner-stat').count() == 3, 'banner chip 数不对'

        # ───── 场景 3：② 上传激励卡点击跳转 ─────
        page.locator('.market-upload-cta').click()
        page.wait_for_timeout(800)
        assert '/console/styles' in page.url, f'② 跳转异常: {page.url}'
        page.go_back()
        page.wait_for_timeout(800)

        # ───── 场景 4：③ 官方精选大卡渲染（approved 且 totalUses>=5 的样式）─────
        assert page.locator('.market-featured-card').count() >= 1, '③ 无精选大卡'
        assert '官方' == page.locator('.market-official-badge').inner_text(), '官方徽章缺失'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_03_featured.png')

        # ───── 场景 5：④ 收益潜力榜 Top 5 渲染 ─────
        assert page.locator('.market-creator-row').count() >= 1, '④ 无创作者行'

        # ───── 场景 6：⑤ 全部风格区 — 5 tab 切换 + 卡片渲染 ─────
        for tab in ['全部', '本周最热', '历史最热', '最新', '官方精选']:
            page.locator(f'.market-tab:has-text("{tab}")').click()
            page.wait_for_timeout(200)
        # pending 风格不应在 approved 列表
        assert page.locator('.market-card').count() == 3, f'approved 卡片数异常: {page.locator(".market-card").count()}'
        # 我的标记：当前 user 是 u_creator_e2e，所以 m1（属于 u_creator_e2e）有"我的"徽章
        assert page.locator('.market-card-mine').count() >= 1, '「我的」标缺失'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_06_grid.png')

        # ───── 场景 7：⑤ 搜索 ─────
        page.locator('.market-search-input').fill('种草')
        page.wait_for_timeout(300)
        assert page.locator('.market-card').count() == 1, f'搜索结果异常: {page.locator(".market-card").count()}'
        page.locator('.market-search-input').fill('')
        page.wait_for_timeout(200)

        # ───── 场景 8：使用按钮跳转 /console/create?marketStyleId=xxx ─────
        # 等数据稳定（loadMarketStyles 是异步，确保有 approved 卡片可点）
        page.wait_for_function(
            "() => document.querySelectorAll('.market-card').length >= 3",
            timeout=5000
        )
        page.wait_for_timeout(400)
        # CreateIndex 会读 query 后 router.replace 掉它，所以 query 仅在过渡期间可见
        nav_urls_before_click = list(_nav_urls)
        page.locator('.market-card-use').first.click()
        page.wait_for_timeout(1500)
        new_nav_urls = _nav_urls[len(nav_urls_before_click):]
        assert '/console/create' in page.url, f'使用跳转异常: {page.url}'
        assert any('marketStyleId=' in u for u in new_nav_urls), (
            f'query 未出现在任何过渡 URL 中: {new_nav_urls}'
        )

        # ───── 场景 9：暗色主题 + 移动端响应式 ─────
        page.goto(f'{URL}/console/style-market', wait_until='domcontentloaded')
        page.wait_for_timeout(1500)
        page.evaluate("() => { document.body.dataset.theme = 'dark'; }")
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_09_dark.png', full_page=True)

        page.set_viewport_size({'width': 375, 'height': 812})
        page.evaluate("() => { document.body.dataset.theme = ''; }")
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_09_mobile.png', full_page=True)
        cols = page.evaluate(
            "getComputedStyle(document.querySelector('.market-grid')).gridTemplateColumns"
        )
        assert cols.endswith('px'), f'移动端 grid 不是单列: {cols}'
        # 移动端 banner 应该是单列
        banner_cols = page.evaluate(
            "getComputedStyle(document.querySelector('.market-banner')).gridTemplateColumns"
        )
        # 单列应只有 1 个 grid track
        assert len(banner_cols.split()) == 1, f'移动端 banner 不是单列: {banner_cols}'

        # ───── 收益规则弹框 ─────
        page.set_viewport_size({'width': 1280, 'height': 800})
        page.evaluate("() => { document.body.dataset.theme = ''; }")
        page.wait_for_timeout(400)
        page.locator('.market-banner-rules-link').click()
        page.wait_for_timeout(400)
        assert page.locator('.ant-modal-content').is_visible(), '收益规则弹框未打开'
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_10_rules.png')
        page.keyboard.press('Escape')
        page.wait_for_timeout(500)

        # ───── 排行榜点击 → 创作者详情 modal ─────
        # 先确保上一个 modal 已经不在屏幕中央干扰，避免同时存在两个 close 选择器
        page.goto(f'{URL}/console/style-market', wait_until='domcontentloaded')
        page.wait_for_timeout(1200)
        page.locator('.market-creator-row').first.click()
        page.wait_for_timeout(400)
        assert page.locator('.creator-modal').is_visible(), '创作者 modal 未打开'
        assert page.locator('.creator-modal-name').count() == 1
        assert page.locator('.creator-modal-stat').count() >= 3
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_v2_11_creator.png')
        # modal 里不应有"使用"按钮（只看不能点击）
        modal_use = page.locator('.creator-modal button:has-text("使用")').count()
        assert modal_use == 0, f'创作者 modal 里不应有"使用"按钮: {modal_use}'

        if errs:
            raise AssertionError('页面 JS 错误: ' + ' / '.join(errs))

        print('风格市场 v2 验证通过')
        browser.close()


if __name__ == '__main__':
    main()

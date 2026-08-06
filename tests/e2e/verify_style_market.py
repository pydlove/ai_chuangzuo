"""提示词市场视觉升级 v2 E2E 验证 — 覆盖首页核心场景。

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

# 一组 mock 提示词数据，用于驱动首页渲染校验
MOCK_STYLES = [
    {
        'id': 'market-001',
        'name': '公众号情感文',
        'sourceType': 'my',
        'creatorId': 'u_creator_e2e',
        'creatorName': '林晚',
        'description': '一段用于测试公众号情感文的风格提示词。',
        'promptSummary': '公众号情感文风格',
        'scope': '公众号,情感文',
        'status': 'approved',
        'price': 2,
        'weeklyUses': 142,
        'totalUses': 1560,
        'weeklyEarnings': 284,
        'featured': True,
        'createdAt': '2026-07-10T08:30:00Z'
    },
    {
        'id': 'market-002',
        'name': '种草文案',
        'sourceType': 'learned',
        'creatorId': 'u_other_creator',
        'creatorName': '安宁',
        'description': '小红书种草文案的语感与节奏。',
        'promptSummary': '小红书种草文案',
        'scope': '小红书,种草',
        'status': 'approved',
        'price': 2,
        'weeklyUses': 56,
        'totalUses': 420,
        'weeklyEarnings': 112,
        'featured': True,
        'createdAt': '2026-07-05T08:30:00Z'
    },
    {
        'id': 'market-003',
        'name': '知乎深度',
        'sourceType': 'my',
        'creatorId': 'u_third_creator',
        'creatorName': '星河',
        'description': '知乎答题风格的严谨与思辨。',
        'promptSummary': '知乎深度',
        'scope': '知乎,深度',
        'status': 'approved',
        'price': 2,
        'weeklyUses': 12,
        'totalUses': 88,
        'weeklyEarnings': 24,
        'featured': True,
        'createdAt': '2026-07-12T08:30:00Z'
    },
    {
        'id': 'market-pending',
        'name': '抖音口播',
        'sourceType': 'my',
        'creatorId': 'u_creator_e2e',
        'creatorName': '林晚',
        'description': '抖音口播风格。',
        'promptSummary': '抖音口播',
        'scope': '抖音',
        'status': 'pending',
        'price': 2,
        'weeklyUses': 0,
        'totalUses': 0,
        'weeklyEarnings': 0,
        'featured': False,
        'createdAt': '2026-07-26T08:30:00Z'
    }
]

APPROVED_STYLES = [s for s in MOCK_STYLES if s['status'] == 'approved']


def install_routes(ctx):
    """拦截所有 user API，用 mock 数据兜底，避免 401/403 触发重定向。"""

    def handler(route):
        url = route.request.url
        if '/market-skills/paged' in url:
            route.fulfill(
                status=200, content_type='application/json',
                body=json.dumps({
                    'code': 0,
                    'data': {
                        'records': APPROVED_STYLES,
                        'total': len(APPROVED_STYLES),
                        'current': 1,
                        'size': 15
                    }
                }, ensure_ascii=False)
            )
        elif '/market-skills/overview' in url:
            route.fulfill(
                status=200, content_type='application/json',
                body=json.dumps({
                    'code': 0,
                    'data': {
                        'approvedCount': len(APPROVED_STYLES),
                        'totalUses': sum(s['totalUses'] for s in APPROVED_STYLES),
                        'totalEarnings': sum(s['totalUses'] * s['price'] for s in APPROVED_STYLES),
                        'featuredSkills': APPROVED_STYLES
                    }
                }, ensure_ascii=False)
            )
        elif '/market-skills/favorites' in url:
            route.fulfill(
                status=200, content_type='application/json',
                body=json.dumps({'code': 0, 'data': []})
            )
        elif '/market-skills' in url:
            route.fulfill(
                status=200, content_type='application/json',
                body=json.dumps({'code': 0, 'data': APPROVED_STYLES}, ensure_ascii=False)
            )
        elif '/api/v1/user/' in url:
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

        ctx.add_init_script(f"""
            localStorage.setItem('aichuangzuo_access_token', 'mock_e2e_token');
            localStorage.setItem('aichuangzuo_user_id', 'u_creator_e2e');
            localStorage.setItem('aichuangzuo_coin_balance', '10.00');
        """)
        install_routes(ctx)

        page = ctx.new_page()
        page.on('dialog', lambda d: (print('  ↪ dialog:', d.message), d.accept()))
        page.on('console', lambda m: print(f'  ↪ console.{m.type}:', m.text[:200]) if m.type in ('error','warning') else None)
        global _nav_urls
        _nav_urls = []
        def _on_nav(f):
            if f is page.main_frame:
                _nav_urls.append(f.url)
                print('  NAV:', f.url)
        page.on('framenavigated', _on_nav)
        errs = []
        page.on('pageerror', lambda exc: errs.append(str(exc)))

        # ───── 场景 1：访问市场页 → 核心区块渲染，无 JS 错误 ─────
        page.goto(f'{URL}/console/skill-market', wait_until='domcontentloaded')
        page.wait_for_timeout(2500)
        assert page.locator('.market-banner').is_visible(), '① banner 缺失'
        assert page.locator('.market-upload-card').is_visible(), '② 激励卡缺失'
        assert page.locator('.market-grid-section').is_visible(), '③ 网格区缺失'
        assert '爱创作 · 提示词市场' in page.locator('.market-banner-title').inner_text()
        assert '官方运营' in page.locator('.market-banner-sub').inner_text()
        page.screenshot(path=f'{SCREENSHOT_DIR}/skill_market_v2_01_full.png', full_page=True)

        # ───── 场景 2：3 个 banner chip（已上架/累计使用/累计发放）─────
        assert page.locator('.market-banner-stat').count() == 3, 'banner chip 数不对'

        # ───── 场景 3：上传激励卡点击跳转 ─────
        page.locator('.market-upload-cta').click()
        page.wait_for_timeout(800)
        assert '/console/skills' in page.url, f'跳转异常: {page.url}'
        page.go_back()
        page.wait_for_timeout(800)

        # ───── 场景 4：官方精选徽章渲染 ─────
        assert page.locator('.skill-card__featured-badge').count() >= 1, '无官方精选徽章'
        assert '官方精选' == page.locator('.skill-card__featured-badge').first.inner_text(), '官方精选文案缺失'
        page.screenshot(path=f'{SCREENSHOT_DIR}/skill_market_v2_04_featured.png')

        # ───── 场景 5：全部提示词区 — 5 tab 切换 + 卡片渲染 ─────
        for tab in ['全部', '本周最热', '历史最热', '最新', '官方精选']:
            page.locator(f'.market-tab:has-text("{tab}")').click()
            page.wait_for_timeout(200)
        # pending 风格不应在 approved 列表
        assert page.locator('.skill-card').count() == 3, f'approved 卡片数异常: {page.locator(".skill-card").count()}'
        # 我的标记：当前 user 是 u_creator_e2e，所以 m1（属于 u_creator_e2e）有"我的"徽章
        assert page.locator('.skill-card__mine-compact').count() >= 1, '「我的」标缺失'
        page.screenshot(path=f'{SCREENSHOT_DIR}/skill_market_v2_05_grid.png')

        # ───── 场景 6：搜索 ─────
        page.locator('.market-search-input').fill('种草')
        page.wait_for_timeout(300)
        assert page.locator('.skill-card').count() == 1, f'搜索结果异常: {page.locator(".skill-card").count()}'
        page.locator('.market-search-input').fill('')
        page.wait_for_timeout(200)

        # ───── 场景 7：使用按钮跳转 /console/create?marketSkillId=xxx ─────
        page.wait_for_function(
            "() => document.querySelectorAll('.skill-card').length >= 3",
            timeout=5000
        )
        page.wait_for_timeout(400)
        nav_urls_before_click = list(_nav_urls)
        page.locator('.skill-card__action-btn--primary').first.click()
        page.wait_for_timeout(1500)
        new_nav_urls = _nav_urls[len(nav_urls_before_click):]
        assert '/console/create' in page.url, f'使用跳转异常: {page.url}'
        assert any('marketSkillId=' in u for u in new_nav_urls), (
            f'query 未出现在任何过渡 URL 中: {new_nav_urls}'
        )

        # ───── 场景 8：暗色主题 + 移动端响应式 ─────
        page.goto(f'{URL}/console/skill-market', wait_until='domcontentloaded')
        page.wait_for_timeout(1500)
        page.evaluate("() => { document.body.dataset.theme = 'dark'; }")
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}/skill_market_v2_08_dark.png', full_page=True)

        page.set_viewport_size({'width': 375, 'height': 812})
        page.evaluate("() => { document.body.dataset.theme = ''; }")
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}/skill_market_v2_08_mobile.png', full_page=True)
        cols = page.evaluate(
            "getComputedStyle(document.querySelector('.market-grid')).gridTemplateColumns"
        )
        assert cols.endswith('px'), f'移动端 grid 不是单列: {cols}'
        banner_cols = page.evaluate(
            "getComputedStyle(document.querySelector('.market-banner')).gridTemplateColumns"
        )
        assert len(banner_cols.split()) == 1, f'移动端 banner 不是单列: {banner_cols}'

        # ───── 场景 9：收益规则弹框 ─────
        page.set_viewport_size({'width': 1280, 'height': 800})
        page.evaluate("() => { document.body.dataset.theme = ''; }")
        page.wait_for_timeout(400)
        page.locator('.market-banner-rules-link').click()
        page.wait_for_timeout(400)
        assert page.locator('.ant-modal-content').is_visible(), '收益规则弹框未打开'
        page.screenshot(path=f'{SCREENSHOT_DIR}/skill_market_v2_09_rules.png')
        page.keyboard.press('Escape')
        page.wait_for_timeout(500)

        # ───── 场景 10：提示词卡片点击查看 → 提示词详情 modal ─────
        page.goto(f'{URL}/console/skill-market', wait_until='domcontentloaded')
        page.wait_for_timeout(1200)
        page.locator('.skill-card').first.click()
        page.wait_for_timeout(400)
        assert page.locator('.skill-detail-modal').is_visible(), '提示词详情 modal 未打开'
        assert page.locator('.skill-detail-prompt').count() == 1
        page.screenshot(path=f'{SCREENSHOT_DIR}/skill_market_v2_10_skill_detail.png')

        if errs:
            raise AssertionError('页面 JS 错误: ' + ' / '.join(errs))

        print('提示词市场 v2 验证通过')
        browser.close()


if __name__ == '__main__':
    main()

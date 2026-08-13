from playwright.sync_api import sync_playwright

BASE = 'http://localhost:22347'
SCREENSHOT = '/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/creation_queue_detail.png'

MOCK_DATA = {
    "code": 0,
    "message": "ok",
    "data": {
        "list": [
            {
                "id": 1,
                "bizNo": "GT202608130001",
                "userId": 10001,
                "userNickname": "测试用户",
                "status": 1,
                "modelConfigDisplay": "默认key/MiniMax",
                "modelConfigId": 1,
                "wordLimitTarget": 1200,
                "retryCount": 0,
                "title": "如何提高自媒体写作效率",
                "description": "请写一篇关于提升自媒体写作效率的实用指南，包含选题、写作、排版、发布等环节。",
                "platform": "微信公众号",
                "skillRef": "default-professional",
                "template": "公众号图文模板",
                "userSkillPrompt": "专业、亲和、有行动清单",
                "failedReason": None,
                "completedAt": None,
                "articleBizNo": None,
                "createdAt": "2026-08-13 10:30:00",
                "waitingSeconds": 12,
                "totalTokens": 3456
            }
        ],
        "total": 1
    }
}

import json

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={'width': 1440, 'height': 900})
    page = context.new_page()

    page.route('**/api/v1/admin/generation/tasks*', lambda route: route.fulfill(
        status=200,
        content_type='application/json',
        body=json.dumps(MOCK_DATA)
    ))

    page.goto(f'{BASE}/login')
    page.evaluate("() => { localStorage.setItem('admin_access_token', JSON.stringify('mock-token')); }")
    page.goto(f'{BASE}/console/creation-queue')
    page.wait_for_selector('text=GT202608130001', timeout=15000)
    page.click('button:has-text(\"详情\")')
    page.wait_for_selector('.detail-body', timeout=15000)
    page.wait_for_timeout(300)
    page.screenshot(path=SCREENSHOT, full_page=False)
    print('screenshot saved to', SCREENSHOT)

    # 验证内部滚动：滚动到底部查看任务信息
    page.locator('.detail-scroll').evaluate('(el) => el.scrollTop = el.scrollHeight')
    page.wait_for_timeout(300)
    page.screenshot(path=SCREENSHOT.replace('.png', '_scrolled.png'), full_page=False)
    print('scrolled screenshot saved')
    browser.close()

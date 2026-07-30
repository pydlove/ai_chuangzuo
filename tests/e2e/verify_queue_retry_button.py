from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={"width": 1280, "height": 900})
    page = context.new_page()

    context.add_init_script("""
      localStorage.setItem('aichuangzuo_access_token', 'verification-token');
      localStorage.setItem('aichuangzuo_user_id', 'verification-user');
    """)

    def handle_route(route):
        url = route.request.url
        if '/benefits/me' in url:
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":0,"data":{"benefits":[{"code":"ai_article_quota","value":10,"remaining":10}]},"message":"ok"}'
            )
        elif '/generation-tasks' in url and route.request.method == 'GET':
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":0,"data":{"list":[{"id":123,"title":"测试文章标题","status":3,"failedReason":"生成超时","progressPct":0,"inputParam":{"title":"测试文章标题","platform":"wechat"},"wordLimitTarget":1500,"createdAt":"2026-07-30T04:00:00Z"}],"total":1},"message":"ok"}'
            )
        else:
            route.continue_()

    page.route("http://localhost:28585/api/v1/user/**", handle_route)

    page.goto("http://localhost:28585/console/create")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1500)

    page.click(".topbar-right .topbar-btn")
    page.wait_for_timeout(800)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/queue_retry_button_theme.png")

    browser.close()
    print("queue retry button screenshot saved")

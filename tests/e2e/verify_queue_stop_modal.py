from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={"width": 1280, "height": 900})
    page = context.new_page()

    # 注入 token + mock 队列接口返回一个生成中任务
    context.add_init_script("""
      localStorage.setItem('aichuangzuo_access_token', 'verification-token');
      localStorage.setItem('aichuangzuo_user_id', 'verification-user');
    """)

    page.route("http://localhost:28585/api/v1/user/generation-tasks**", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body='{"code":0,"data":{"list":[{"id":123,"title":"测试文章标题","status":1,"progressPct":45,"inputParam":{"title":"测试文章标题","platform":"wechat"},"wordLimitTarget":1500,"createdAt":"2026-07-30T04:00:00Z"}],"total":1},"message":"ok"}'
    ))

    page.goto("http://localhost:28585/console/create")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1500)

    # 打开队列抽屉
    page.click(".topbar-right .topbar-btn")
    page.wait_for_timeout(800)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/queue_drawer_open.png")

    # 点击停止按钮
    page.click(".queue-item-stop-btn")
    page.wait_for_timeout(800)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/queue_stop_modal_theme.png")

    browser.close()
    print("queue stop modal screenshots saved")

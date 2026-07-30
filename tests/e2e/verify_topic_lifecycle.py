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
        if '/topics/random' in url:
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":0,"data":[{"id":101,"title":"职场效率提升技巧","summary":"分享几个提升工作效率的方法"},{"id":102,"title":"小红书种草文案","summary":"写一篇种草笔记"}],"message":"ok"}'
            )
        elif '/benefits/me' in url:
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":0,"data":{"benefits":[{"code":"ai_article_quota","value":10,"remaining":10}]},"message":"ok"}'
            )
        elif '/benefits/consume/ai_article_quota' in url:
            route.fulfill(status=200, content_type="application/json", body='{"code":0,"data":true,"message":"ok"}')
        elif '/generation-tasks' in url and route.request.method == 'POST':
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":0,"data":{"id":999},"message":"ok"}'
            )
        elif '/topics/101/used' in url or '/topics/102/used' in url:
            route.fulfill(status=200, content_type="application/json", body='{"code":0,"data":true,"message":"ok"}')
        else:
            route.continue_()

    page.route("http://localhost:28585/api/v1/user/**", handle_route)

    page.goto("http://localhost:28585/console/create")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1500)

    # 点击第一个灵感标题并生成
    page.click(".topic-capsule:nth-of-type(1)")
    page.wait_for_timeout(300)
    page.click(".hero-generate-btn")
    page.wait_for_timeout(1000)
    used_after_generate = page.eval_on_selector(".topic-capsule:nth-of-type(1)", "el => el.classList.contains('used')")
    print("used after generate:", used_after_generate)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/topic_lifecycle_used.png")

    # 通过动态导入直接修改队列状态为 cancelled，触发回滚
    page.evaluate("""async () => {
      try {
        const module = await import('/src/views/console/create/useGenerationQueue.js')
        const { queueList } = module.useGenerationQueue()
        queueList.value = [{
          id: 999,
          title: '职场效率提升技巧',
          platform: 'wechat',
          wordCount: 1500,
          status: 'cancelled',
          progress: 0,
          createdAt: '2026-07-30T04:00:00Z',
          completedAt: null
        }]
      } catch (e) {
        console.error(e)
      }
    }""")
    page.wait_for_timeout(500)
    used_after_cancel = page.eval_on_selector(".topic-capsule:nth-of-type(1)", "el => el.classList.contains('used')")
    print("used after cancel:", used_after_cancel)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/topic_lifecycle_rollback.png")

    # 再模拟重新生成（状态变为 queued/generating），标题应恢复已使用
    page.evaluate("""async () => {
      try {
        const module = await import('/src/views/console/create/useGenerationQueue.js')
        const { queueList } = module.useGenerationQueue()
        queueList.value = [{
          id: 999,
          title: '职场效率提升技巧',
          platform: 'wechat',
          wordCount: 1500,
          status: 'generating',
          progress: 10,
          createdAt: '2026-07-30T04:00:00Z',
          completedAt: null
        }]
      } catch (e) {
        console.error(e)
      }
    }""")
    page.wait_for_timeout(500)
    used_after_retry = page.eval_on_selector(".topic-capsule:nth-of-type(1)", "el => el.classList.contains('used')")
    print("used after retry:", used_after_retry)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/topic_lifecycle_retry.png")

    browser.close()
    print("topic lifecycle screenshots saved")

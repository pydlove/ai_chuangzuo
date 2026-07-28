from playwright.sync_api import sync_playwright
import sys

URL = "http://127.0.0.1:22346/console/creation-queue"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # 拦截 Storage.getItem，让 admin_access_token 始终返回 fake token
    context.add_init_script("""
        const originalGetItem = Storage.prototype.getItem;
        Storage.prototype.getItem = function(key) {
            if (key === 'admin_access_token') {
                return '"fake-token-for-ui-check"';
            }
            return originalGetItem.call(this, key);
        };
    """)

    # 同时拦截列表 API 与预览 API
    page.route("**/api/v1/admin/generation/tasks**", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body='{"code":0,"message":"ok","data":{"list":[{"id":1,"bizNo":"GA20260701","userId":7,"userNickname":"小王","status":2,"wordLimitTarget":1500,"articleBizNo":"A123","totalTokens":1234,"createdAt":"2026-07-01T10:00:00","completedAt":"2026-07-01T10:05:00"}],"total":1,"page":1,"pageSize":20}}'
    ))
    page.route("**/api/v1/admin/generation/tasks/1/article", lambda route: route.fulfill(
        status=200,
        content_type="application/json",
        body='{"code":0,"message":"ok","data":{"bizNo":"A123","title":"测试文章标题","body":"这是正文内容\\n\\n第二段。","description":"文章描述","platform":"wechat","tags":["标签1","标签2"]}}'
    ))

    # 导航到创作队列
    page.goto(URL, wait_until='load')
    page.wait_for_timeout(2000)

    # 如果还在登录页，说明注入未生效
    if '/login' in page.url:
        print("Still on login page, token injection did not work")
        page.screenshot(path='tests/e2e/screenshots/creation_queue_login.png')
        browser.close()
        sys.exit(1)

    # 切换到已完成 tab
    completed_tab = page.get_by_role('tab', name='已完成')
    print(f"completed_tab count={completed_tab.count()}")
    if completed_tab.count() > 0:
        # 通过 JS 点击 tab 内部元素，确保触发 antd tab change
        page.evaluate("""() => {
            const tabs = Array.from(document.querySelectorAll('.ant-tabs-tab'));
            const target = tabs.find(el => el.textContent.includes('已完成'));
            if (target) target.click();
        }""")
        page.wait_for_timeout(2000)

    # 截图
    page.screenshot(path='tests/e2e/screenshots/creation_queue_completed.png', full_page=False)

    # 检查是否有预览/下载按钮
    has_preview = page.locator('button:has-text("预览")').count() > 0
    has_download = page.locator('button:has-text("下载")').count() > 0
    active_tab = page.locator('.ant-tabs-tab-active').inner_text() if page.locator('.ant-tabs-tab-active').count() > 0 else 'none'
    print(f"active_tab={active_tab}, has_preview_button={has_preview}, has_download_button={has_download}, url={page.url}")

    # 点击预览，检查弹窗内容
    page.locator('button:has-text("预览")').first.click()
    page.wait_for_timeout(1000)
    page.screenshot(path='tests/e2e/screenshots/creation_queue_preview_modal.png', full_page=False)
    modal_title = page.locator('.ant-modal-title').inner_text() if page.locator('.ant-modal-title').count() > 0 else ''
    modal_body = page.locator('.preview-body').inner_text() if page.locator('.preview-body').count() > 0 else ''
    print(f"modal_title={modal_title}, modal_body_preview={modal_body[:30]}")

    browser.close()

    if not (has_preview and has_download):
        sys.exit(1)
    if '测试文章标题' not in modal_title or '这是正文内容' not in modal_body:
        print("Preview modal content not rendered")
        sys.exit(1)
    print("UI check passed")

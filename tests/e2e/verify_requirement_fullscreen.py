from playwright.sync_api import sync_playwright
import time

with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={"width": 1280, "height": 900})
    page = context.new_page()

    # 注入 token 绕过登录守卫
    context.add_init_script("""
      localStorage.setItem('aichuangzuo_access_token', 'verification-token');
      localStorage.setItem('aichuangzuo_user_id', 'verification-user');
    """)

    page.goto("http://localhost:28585/console/create")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)

    # 截图：主界面
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/create_requirement_main.png")

    # 点击全屏按钮
    page.click(".hero-textarea-fullscreen")
    page.wait_for_timeout(800)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/create_requirement_fullscreen.png")

    # 输入接近上限的文本
    long_text = "测试观点" * 40  # 160 个字符
    page.fill(".requirement-fullscreen-textarea", long_text)
    page.wait_for_timeout(500)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/create_requirement_typing.png")

    # 尝试超过 200 字
    page.fill(".requirement-fullscreen-textarea", "超" * 250)
    page.wait_for_timeout(500)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/create_requirement_overlimit.png")

    # 保存
    page.click(".requirement-fullscreen-actions .hero-generate-btn")
    page.wait_for_timeout(800)
    page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/create_requirement_saved.png")

    browser.close()
    print("verification screenshots saved")

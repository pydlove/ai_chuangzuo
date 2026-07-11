#!/usr/bin/env python3
"""管理端 - 创作学院 CRUD 流程端到端验证。

前置条件：
- MySQL 启动且 Flyway 已迁移（含 V2.0.0_023__create_learn_tables）
- admin-api 启动（默认 8080），user-api 启动（默认 8081）
- admin-web dev 启动（默认 http://localhost:22346）
- user-web dev 启动（默认 http://localhost:5173/learn；见 learn_browsing.py）
- 已存在管理员账号（环境变量 ADMIN_USER / ADMIN_PASS，默认 admin / admin123）

注意：admin 登录含滑块人机验证；本脚本假定滑块已通过或测试环境已关闭滑块。
若未关闭，需要在 openSliderModal 后实现 .slider-handle 的拖拽逻辑。
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

ADMIN_URL = os.environ.get("ADMIN_URL", "http://localhost:22346")
USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "admin123")

SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "admin_learn"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def login(page):
    page.goto(f"{ADMIN_URL}/login")
    page.fill('input[placeholder="请输入管理员账号"]', ADMIN_USER)
    page.fill('input[placeholder="请输入密码"]', ADMIN_PASS)
    page.click('button:has-text("登录")')
    # 滑块人机验证：若启用需在此处实现拖拽
    page.wait_for_url(lambda u: "/login" not in u, timeout=10_000)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        login(page)
        page.screenshot(path=SCREENSHOTS_DIR / "00-console.png")

        # 1. 分类管理：进入页面
        page.click('text=创作学院')
        page.click('text=分类管理')
        page.wait_for_url("**/console/learn/category")
        page.screenshot(path=SCREENSHOTS_DIR / "01-category-empty.png")

        # 2. 新增顶级分类
        page.click('button:has-text("新增顶级分类")')
        page.locator('.ant-modal input[type="text"]').first.fill('创作技巧')
        page.click('.ant-modal button:has-text("确定")')
        time.sleep(0.6)
        page.screenshot(path=SCREENSHOTS_DIR / "02-category-created.png")
        expect(page.locator('text=创作技巧').first).to_be_visible()

        # 3. 进入文章管理
        page.click('text=文章管理')
        page.wait_for_url("**/console/learn/article")
        page.screenshot(path=SCREENSHOTS_DIR / "03-article-list-empty.png")

        # 4. 新增文章
        page.click('button:has-text("新增文章")')
        page.wait_for_url("**/console/learn/article/edit")
        page.fill('input[maxlength="128"]', '如何写出爆款标题')
        # 分类树选择
        page.click('.ant-tree-select')
        page.click('text=创作技巧')
        # 摘要
        page.fill('textarea', '本文介绍写标题的常见模式')
        # Markdown 内容（mavon-editor textarea）
        page.fill('.v-note-wrapper textarea', '# 标题三大原则\n\n1. 数字\n2. 反差\n3. 痛点')
        # 保存草稿
        page.click('button:has-text("保存草稿")')
        page.wait_for_url("**/console/learn/article/edit/**")
        page.screenshot(path=SCREENSHOTS_DIR / "04-article-draft.png")

        # 5. 发布
        page.click('button:has-text("保存并发布")')
        time.sleep(0.8)
        page.screenshot(path=SCREENSHOTS_DIR / "05-article-published.png")

        # 6. 在用户端验证可见
        page.goto(f"{USER_URL}/learn")
        time.sleep(1.2)
        page.screenshot(path=SCREENSHOTS_DIR / "06-user-learn-tree.png", full_page=True)
        expect(page.locator('text=创作技巧').first).to_be_visible()

        # 7. 进入文章详情
        page.click('text=如何写出爆款标题')
        page.wait_for_url("**/article/**")
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "07-user-article.png", full_page=True)

        ctx.close()
        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)
#!/usr/bin/env python3
"""用户端 - 创作学院浏览端到端验证。

前置条件：
- MySQL 启动且 Flyway 已迁移
- user-api 启动（默认 8081），admin-api 已通过管理端录入至少 1 个已发布分类 + 1 篇已发布文章
- user-web dev 启动（默认 http://localhost:22345）
- 期望数据：admin 端创建顶级分类「创作技巧」并在其下发布一篇 Markdown 文章「如何写出爆款标题」
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # Desktop
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(f"{USER_URL}/learn")
        time.sleep(1.2)
        page.screenshot(path=SCREENSHOTS_DIR / "01-desktop-tree.png", full_page=True)

        # 点击分类
        page.locator('.learn-tree-node').filter(has_text='创作技巧').first.click()
        time.sleep(0.8)
        page.screenshot(path=SCREENSHOTS_DIR / "02-desktop-list.png", full_page=True)

        # 进入文章
        page.click('text=如何写出爆款标题')
        page.wait_for_url("**/article/**")
        time.sleep(0.6)
        page.screenshot(path=SCREENSHOTS_DIR / "03-desktop-article.png", full_page=True)
        expect(page.locator('h1:has-text("如何写出爆款标题")')).to_be_visible()

        # 404 文章（不存在的 id）
        page.goto(f"{USER_URL}/learn/article/99999999")
        time.sleep(0.6)
        page.screenshot(path=SCREENSHOTS_DIR / "04-404.png", full_page=True)
        ctx.close()

        # Mobile：分类抽屉
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        page2.goto(f"{USER_URL}/learn")
        time.sleep(1.2)
        page2.screenshot(path=SCREENSHOTS_DIR / "05-mobile-tree.png", full_page=True)
        page2.click('button.learn-tree-fab')
        time.sleep(0.6)
        page2.screenshot(path=SCREENSHOTS_DIR / "06-mobile-sheet.png", full_page=True)

        ctx2.close()
        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)
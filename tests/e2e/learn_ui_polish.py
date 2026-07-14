#!/usr/bin/env python3
"""用户端 - 创作学院 UI 中度打磨端到端验证。

前置条件：
- user-api 启动（默认 25050）
- user-web dev 启动（默认 http://localhost:22345）
- 已通过管理端录入至少 2 个分类、跨分类的 3 篇已发布文章
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn_ui_polish"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ===== Desktop =====
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 1. Hero 区
        page.goto(f"{USER_URL}/learn")
        time.sleep(1.5)
        expect(page.locator('.learn-hero-title')).to_have_text('创作学院')
        expect(page.locator('.learn-hero-subtitle')).to_be_visible()
        expect(page.locator('.learn-hero-deco-lg')).to_be_visible()
        page.screenshot(path=SCREENSHOTS_DIR / "01-hero.png", full_page=True)

        # 2. 侧边栏图标
        top_nodes = page.locator('.learn-tree-row.top-level')
        assert top_nodes.count() > 0, "no top-level categories"
        first_top = top_nodes.first
        # 顶级分类应有 SVG 图标（Ant Design icon 渲染为 svg）
        svg_count = first_top.locator('svg').count()
        assert svg_count > 0, "top-level category should have icon"
        # 折叠图标应为 › 或 ∨
        caret = first_top.locator('.learn-tree-caret')
        if caret.count() > 0:
            text = caret.inner_text()
            assert text in ('›', '∨'), f"caret should be › or ∨, got {text}"

        # 3. 空状态
        expect(page.locator('.learn-empty-title')).to_have_text('欢迎来到创作学院')
        expect(page.locator('.learn-empty-subtitle')).to_be_visible()
        chips = page.locator('.learn-empty-chip')
        assert chips.count() > 0, "empty state should have quick access chips"

        # 4. 分类列表（点击第一个顶级分类）
        top_nodes.first.click()
        time.sleep(1.0)
        page.screenshot(path=SCREENSHOTS_DIR / "02-category-list.png", full_page=True)
        # 面包屑
        breadcrumb = page.locator('.learn-breadcrumb')
        if breadcrumb.count() > 0:
            expect(breadcrumb).to_be_visible()
        # 文章卡片
        cards = page.locator('.learn-article-card')
        if cards.count() > 0:
            first_card = cards.first
            expect(first_card.locator('.learn-article-card-title')).to_be_visible()

        # 5. 文章详情：面包屑 + 元信息条 + 上下篇 + CTA
        page.goto(f"{USER_URL}/learn/article/3")
        time.sleep(1.5)
        page.screenshot(path=SCREENSHOTS_DIR / "03-article-top.png", full_page=True)

        # 面包屑
        expect(page.locator('.learn-breadcrumb')).to_be_visible()
        # 元信息条
        expect(page.locator('.learn-meta-bar')).to_be_visible()
        expect(page.locator('.learn-meta-item').first).to_be_visible()
        # 阅读时长
        meta_text = page.locator('.learn-meta-bar').inner_text()
        assert '分钟' in meta_text, f"meta bar should contain reading minutes, got: {meta_text}"

        # 滚到底部看上下篇 + CTA
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "04-article-bottom.png", full_page=True)

        # 上下篇卡片
        nav_cards = page.locator('.learn-nav-card')
        assert nav_cards.count() > 0, "article should have prev/next cards"
        # CTA 卡片
        expect(page.locator('.learn-cta-card')).to_be_visible()
        expect(page.locator('.learn-cta-btn')).to_be_visible()
        cta_text = page.locator('.learn-cta-btn').inner_text()
        assert '立即开始创作' in cta_text, f"CTA button text wrong: {cta_text}"

        ctx.close()

        # ===== Mobile =====
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        page2.goto(f"{USER_URL}/learn/article/3")
        time.sleep(1.5)
        page2.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.5)
        page2.screenshot(path=SCREENSHOTS_DIR / "05-mobile-article.png", full_page=True)
        # 移动端 CTA 按钮应全宽（容器内）
        cta_btn = page2.locator('.learn-cta-btn')
        box = cta_btn.bounding_box()
        assert box['width'] > 250, f"mobile CTA button should be wide, got {box['width']}"
        ctx2.close()

        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)

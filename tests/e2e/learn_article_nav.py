#!/usr/bin/env python3
"""用户端 - 创作学院文章底部「上一篇/下一篇」导航端到端验证。

前置条件：
- MySQL 启动且 Flyway 已迁移
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
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn_nav"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def goto_article(page, article_id):
    page.goto(f"{USER_URL}/learn/article/{article_id}")
    time.sleep(0.8)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ---------- Desktop ----------
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 调后端 API 拉全学院阅读链，避免硬编码 id
        page.goto(f"{USER_URL}/learn")
        time.sleep(1.0)
        seq = page.evaluate("""
            async () => {
                const treeRes = await fetch('/api/v1/user/learn/category/tree');
                const tree = (await treeRes.json()).data || [];
                const orderedCatIds = [];
                const walk = nodes => nodes.forEach(n => {
                    orderedCatIds.push(n.id);
                    if (n.children && n.children.length) walk(n.children);
                });
                walk(tree);
                const seq = [];
                for (const cid of orderedCatIds) {
                    const r = await fetch(`/api/v1/user/learn/category/${cid}?page=1&size=100`);
                    const d = (await r.json()).data;
                    if (d && d.articles) d.articles.forEach(a => seq.push(a.id));
                }
                return seq;
            }
        """)
        if len(seq) < 3:
            print(f"FAIL: need at least 3 published articles, got {len(seq)}", file=sys.stderr)
            sys.exit(1)
        first, mid, last = seq[0], seq[len(seq) // 2], seq[-1]
        print(f"reading chain size = {len(seq)}, first={first} mid={mid} last={last}")

        # 1. 首篇：只看到「下一篇」
        goto_article(page, first)
        expect(page.locator('.learn-nav-next')).to_be_visible()
        expect(page.locator('.learn-nav-prev')).to_have_count(0)
        page.screenshot(path=SCREENSHOTS_DIR / "01-first-only-next.png", full_page=True)

        # 2. 中间篇：两个按钮都在；点击「下一篇」跳转并滚顶部
        goto_article(page, mid)
        expect(page.locator('.learn-nav-prev')).to_be_visible()
        expect(page.locator('.learn-nav-next')).to_be_visible()
        page.screenshot(path=SCREENSHOTS_DIR / "02-mid-both.png", full_page=True)

        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.3)
        page.locator('.learn-nav-next').click()
        time.sleep(1.0)
        scroll_y = page.evaluate("window.scrollY")
        assert scroll_y < 50, f"after click next, scrollY should be near 0, got {scroll_y}"
        page.screenshot(path=SCREENSHOTS_DIR / "03-after-click-scroll-top.png", full_page=True)

        # 3. 末篇：只看到「上一篇」
        goto_article(page, last)
        expect(page.locator('.learn-nav-prev')).to_be_visible()
        expect(page.locator('.learn-nav-next')).to_have_count(0)
        page.screenshot(path=SCREENSHOTS_DIR / "04-last-only-prev.png", full_page=True)

        # 4. 跨分类提示：遍历序列，找到至少一篇按钮内显示《分类名》的
        found_cat_hint = False
        for aid in seq:
            goto_article(page, aid)
            if page.locator('.learn-nav-cat').count() > 0:
                found_cat_hint = True
                page.screenshot(path=SCREENSHOTS_DIR / "05-cross-category-hint.png", full_page=True)
                break
        if not found_cat_hint:
            print("WARN: no cross-category hint found (may be expected if all articles are in one category)")

        ctx.close()

        # ---------- Mobile：点击下一篇滚回顶部 ----------
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        goto_article(page2, mid)
        time.sleep(0.5)
        page2.screenshot(path=SCREENSHOTS_DIR / "06-mobile-article.png", full_page=True)

        mobile_nav_cards = page2.locator('.ml-article-nav__card')
        assert mobile_nav_cards.count() == 2, "mobile article nav should have both prev and next cards"

        # 滚动到底部后点击「下一篇」，应回到顶部
        page2.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.3)
        mobile_nav_cards.nth(1).click()
        time.sleep(1.0)
        mobile_scroll_y = page2.evaluate("window.scrollY")
        assert mobile_scroll_y < 50, f"after mobile click next, scrollY should be near 0, got {mobile_scroll_y}"
        page2.screenshot(path=SCREENSHOTS_DIR / "07-mobile-after-click-next-scroll-top.png", full_page=True)

        # 末篇：点击「上一篇」也应回到顶部
        goto_article(page2, last)
        time.sleep(0.5)
        page2.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.3)
        page2.locator('.ml-article-nav__card').first.click()
        time.sleep(1.0)
        mobile_scroll_y_prev = page2.evaluate("window.scrollY")
        assert mobile_scroll_y_prev < 50, f"after mobile click prev, scrollY should be near 0, got {mobile_scroll_y_prev}"
        page2.screenshot(path=SCREENSHOTS_DIR / "08-mobile-after-click-prev-scroll-top.png", full_page=True)

        ctx2.close()
        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)

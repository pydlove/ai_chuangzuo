#!/usr/bin/env python3
"""用户端 - 创作学院 banner + 推荐分类端到端验证。

前置条件：
- admin-api 启动（26060）
- user-api 启动（25050）
- user-web dev 启动（22345）
- 已通过管理端录入至少 1 个 banner、至少 1 个推荐分类
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn_banner"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ===== Desktop =====
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 1. 空状态页：banner 轮播
        page.goto(f"{USER_URL}/learn")
        time.sleep(2.0)

        banner_section = page.locator('.learn-banner-section')
        if banner_section.count() > 0:
            expect(banner_section).to_be_visible()
            imgs = page.locator('.learn-banner-img')
            assert imgs.count() > 0, "banner section should have images"
            page.screenshot(path=SCREENSHOTS_DIR / "01-banner.png", full_page=True)
            print(f"PASS: banner carousel visible with {imgs.count()} images")
        else:
            print("WARN: no banner section (may need to add banners via admin)")

        # 2. 推荐分类
        recommend_section = page.locator('.learn-recommend-section')
        if recommend_section.count() > 0:
            expect(recommend_section).to_be_visible()
            cards = page.locator('.learn-recommend-card')
            assert cards.count() > 0, "recommend section should have cards"
            # 每个卡片应有图标（svg）和名称
            first = cards.first
            assert first.locator('svg').count() > 0, "recommend card should have icon"
            assert first.locator('.learn-recommend-name').inner_text().strip(), "card should have name"
            page.screenshot(path=SCREENSHOTS_DIR / "02-recommend.png", full_page=True)
            print(f"PASS: recommend section visible with {cards.count()} cards")

            # 3. 点击推荐分类卡片 → 跳转分类列表
            first.click()
            time.sleep(1.5)
            assert 'cat=' in page.url, f"should navigate to category, got {page.url}"
            page.screenshot(path=SCREENSHOTS_DIR / "03-category-nav.png", full_page=True)
            print("PASS: recommend card click navigates to category")
        else:
            print("WARN: no recommend section (may need to mark categories as recommended)")

        ctx.close()

        # ===== Mobile =====
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        page2.goto(f"{USER_URL}/learn")
        time.sleep(2.0)
        page2.screenshot(path=SCREENSHOTS_DIR / "04-mobile.png", full_page=True)
        # banner 图片应完整显示（width:100% height:auto，无裁剪）
        if page2.locator('.learn-banner-img').count() > 0:
            box = page2.locator('.learn-banner-img').first.bounding_box()
            assert box['height'] > 0, "banner image should be visible"
            # 确认图片按自然比例缩放（width:100% + height:auto），宽高比应与原图一致
            dims = page2.evaluate("""() => {
                const img = document.querySelector('.learn-banner-img');
                return { natW: img.naturalWidth, natH: img.naturalHeight, w: img.width, h: img.height };
            }""")
            nat_ratio = dims['natW'] / dims['natH']
            rendered_ratio = dims['w'] / dims['h']
            assert abs(nat_ratio - rendered_ratio) < 0.05, \
                f"aspect ratio mismatch: natural={nat_ratio:.2f} rendered={rendered_ratio:.2f}"
            print("PASS: mobile banner fully visible")
        ctx2.close()

        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)

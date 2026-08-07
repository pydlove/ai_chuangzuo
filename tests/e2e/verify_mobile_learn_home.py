#!/usr/bin/env python3
"""用户端 - 移动端首页 /learn banner + 推荐文章卡片验证。

前置条件：
- admin-api 启动（26060）
- user-api 启动（25050）
- user-web dev 启动（22345）
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "mobile_learn_home"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ===== Mobile (390px) =====
        ctx = browser.new_context(viewport={"width": 390, "height": 800})
        page = ctx.new_page()

        page.goto(f"{USER_URL}/learn")
        time.sleep(2.0)
        page.screenshot(path=SCREENSHOTS_DIR / "01-mobile-home-full.png", full_page=True)

        # 1. banner 完整可见
        banner_imgs = page.locator('.ml-banner__slide img')
        if banner_imgs.count() > 0:
            img = banner_imgs.first
            box = img.bounding_box()
            assert box and box['height'] > 0, "banner image should be visible"

            dims = page.evaluate("""() => {
                const img = document.querySelector('.ml-banner__slide img');
                const r = img.getBoundingClientRect();
                return { natW: img.naturalWidth, natH: img.naturalHeight, w: r.width, h: r.height };
            }""")
            # 由于改为 object-fit:contain，整张图应完整可见
            print(f"PASS: banner visible - natural={dims['natW']}x{dims['natH']}, "
                  f"rendered={dims['w']:.1f}x{dims['h']:.1f}")

            # 截图 banner 区域
            page.locator('.ml-banner').screenshot(path=SCREENSHOTS_DIR / "02-banner.png")
        else:
            print("WARN: no banner images rendered")

        # 2. 推荐文章卡片：图片在左（120×73 横向），标题在右
        article_cards = page.locator('.ml-article-card')
        if article_cards.count() > 0:
            print(f"PASS: {article_cards.count()} article cards rendered")

            # 检查第一张卡片布局
            first = article_cards.first
            cover = first.locator('.ml-article-card__cover')
            if cover.count() > 0:
                cover_box = cover.bounding_box()
                title_box = first.locator('.ml-article-card__title').bounding_box()
                assert cover_box and title_box, "card parts should be visible"
                # 图片在左，标题在右：cover.x < title.x
                assert cover_box['x'] < title_box['x'], \
                    f"cover should be on left, got cover.x={cover_box['x']:.1f} title.x={title_box['x']:.1f}"
                # 图片横向比例（约 120:73 ≈ 1.65:1）
                ratio = cover_box['width'] / cover_box['height']
                assert 1.5 < ratio < 1.8, \
                    f"cover ratio should be ~1.65 (445:270), got {ratio:.2f}"
                print(f"PASS: cover ratio {ratio:.2f} (target 1.65), "
                      f"cover={cover_box['width']:.1f}x{cover_box['height']:.1f}")

                # 截图第一张卡片
                first.screenshot(path=SCREENSHOTS_DIR / "03-article-card.png")
            else:
                print("WARN: no cover image on first card")
        else:
            print("WARN: no article cards rendered (may need to mark articles as recommended)")

        ctx.close()
        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)

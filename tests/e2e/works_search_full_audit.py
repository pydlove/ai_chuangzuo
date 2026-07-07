#!/usr/bin/env python3
"""Full audit of works search box in dark mode with data + interaction."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"

WORKS_JS = """
localStorage.setItem('aichuangzuo_theme', 'dark');
localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify([
  {
    id: 'w1',
    title: '职场新人的5个高效习惯',
    prompt: '写一篇职场新人指南',
    status: 'completed',
    progress: 100,
    createdAt: Date.now() - 3600000,
    completedAt: Date.now() - 3500000,
    article: { title: '职场新人的5个高效习惯', summary: '...', body: '正文内容' }
  },
  {
    id: 'w2',
    title: '小红书种草爆款公式',
    prompt: '小红书爆款分析',
    status: 'completed',
    progress: 100,
    createdAt: Date.now() - 86400000,
    completedAt: Date.now() - 86000000,
    article: { title: '小红书种草爆款公式', summary: '...', body: '正文' }
  }
]));
"""

BAD = {"rgb(255, 255, 255)", "rgb(245, 245, 245)", "rgb(250, 250, 250)", "rgb(248, 248, 248)", "rgb(252, 252, 252)"}


def check(page, sels, label):
    print(f"\n--- {label} ---")
    found = 0
    bad = 0
    for sel in sels:
        for i, el in enumerate(page.query_selector_all(sel)):
            found += 1
            bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
            color = el.evaluate("e => getComputedStyle(e).color")
            is_bad = bg in BAD
            marker = "BAD" if is_bad else "ok"
            if is_bad:
                bad += 1
            print(f"  {marker} {sel}[{i}] bg={bg} color={color}")
    print(f">>> {label}: found={found}, bad={bad}")
    return bad


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate(WORKS_JS)
        page.goto(f"{BASE}/console/works", wait_until="networkidle")
        page.wait_for_timeout(800)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_with_data.png", full_page=False)

        sels_default = [
            ".works-filter-bar",
            ".works-search",
            ".works-search .ant-input-affix-wrapper",
            ".works-search input",
            ".works-search .ant-input-prefix",
            ".works-search .ant-input-suffix",
            ".works-search .ant-input-suffix .ant-input-search-icon",
            ".works-search .ant-input-suffix .ant-input-search-button",
            ".works-search .ant-input-group > .ant-input-group-addon",
            ".works-search .ant-input-search",
            ".works-filter-select",
            ".works-filter-select .ant-select-selector",
            ".works-filter-time",
            ".works-filter-time .ant-radio-button-wrapper",
        ]
        check(page, sels_default, "default state")

        # Focus the search
        try:
            search_input = page.query_selector(".works-search input")
            if search_input:
                search_input.focus()
                page.wait_for_timeout(300)
                page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_search_focus.png", full_page=False)
                check(page, [".works-search .ant-input-affix-wrapper-focused", ".works-search .ant-input-affix-wrapper"], "focused state")
                # Type something
                page.keyboard.type("职场")
                page.wait_for_timeout(300)
                page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_search_typing.png", full_page=False)
                check(page, [".works-search input", ".works-search .ant-input-affix-wrapper"], "typing state")
        except Exception as e:
            print(f"focus ERR {e}")

        # Open platform select dropdown
        try:
            sel = page.query_selector(".works-filter-select .ant-select-selector")
            if sel:
                sel.click()
                page.wait_for_timeout(500)
                page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works_dropdown.png", full_page=False)
                check(page, [".ant-select-dropdown", ".ant-select-item"], "platform dropdown")
                page.keyboard.press("Escape")
                page.wait_for_timeout(300)
        except Exception as e:
            print(f"dropdown ERR {e}")

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
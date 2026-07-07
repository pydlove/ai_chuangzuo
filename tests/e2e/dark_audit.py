#!/usr/bin/env python3
"""Dark mode audit: open modals, simulate queue data, inspect computed backgrounds."""
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22347"

# Sample queue items to populate the mini-queue panel.
QUEUE_JS = """
localStorage.setItem('aichuangzuo_theme', 'dark');
localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify([
  {
    id: 'q1',
    title: '小红书爆款标题写作技巧分享',
    prompt: '帮我写一篇小红书爆款标题技巧',
    platform: 'xiaohongshu',
    wordCount: 800,
    styleName: '种草达人',
    templateKey: 'general',
    status: 'completed',
    progress: 100,
    createdAt: Date.now() - 60000,
    completedAt: Date.now() - 30000,
    article: { title: '标题技巧分享', summary: '摘要', body: '内容...' }
  },
  {
    id: 'q2',
    title: '如何在职场保持高效',
    prompt: '写职场高效技巧',
    platform: 'wechat',
    wordCount: 1500,
    styleName: '深度好文',
    templateKey: 'general',
    status: 'generating',
    progress: 45,
    createdAt: Date.now() - 20000
  },
  {
    id: 'q3',
    title: 'AI 写作新趋势',
    prompt: 'AI 写作分析',
    platform: 'toutiao',
    wordCount: 1200,
    styleName: '资讯快报',
    templateKey: 'general',
    status: 'queued',
    progress: 0,
    createdAt: Date.now() - 5000
  }
]));
"""

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
    title: '（未命名草稿）',
    prompt: '一个未完成的草稿',
    status: 'draft',
    createdAt: Date.now() - 7200000
  },
  {
    id: 'w3',
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


def check(page, selectors, label):
    print(f"\n--- {label} ---")
    for sel in selectors:
        try:
            els = page.query_selector_all(sel)
            for i, el in enumerate(els):
                if not el.is_visible():
                    continue
                bg = el.evaluate("e => getComputedStyle(e).backgroundColor")
                color = el.evaluate("e => getComputedStyle(e).color")
                print(f"  {sel}[{i}] bg={bg} color={color}")
        except Exception as e:
            print(f"  {sel} ERR {e}")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # === Create page with queue data ===
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE, wait_until="networkidle")
        page.evaluate(QUEUE_JS)
        page.goto(f"{BASE}/console/create", wait_until="networkidle")
        page.wait_for_timeout(800)
        page.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_create_with_data.png", full_page=True)
        check(page, [".hero-title-input", ".hero-textarea", ".queue-panel-item", ".queue-panel-item.completed", ".queue-panel-item.generating", ".queue-panel-item.queued"], "create / queue panel")

        # Open word count modal — clicks "1500字标准" chip
        try:
            page.click("text=1500", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_wc_modal.png", full_page=False)
            check(page, [".wc-custom-input", ".ant-modal-content", ".ant-modal-header"], "word count modal")
            # Switch to "自定义" tab
            page.click("text=自定义", timeout=2000)
            page.wait_for_timeout(500)
            check(page, [".wc-custom-input"], "word count modal custom")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"wc modal ERR {e}")

        # Open style modal then go to editor
        try:
            page.click("text=年度总结", timeout=2000)
            page.wait_for_timeout(800)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_modal.png", full_page=False)
            check(page, [".ant-modal-content", ".ant-modal-header"], "style modal")
            page.click("text=新建我的风格", timeout=2000)
            page.wait_for_timeout(500)
            page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_style_editor.png", full_page=False)
            check(page, [".style-editor-input", ".style-editor-textarea", ".ant-modal-content"], "style editor")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as e:
            print(f"style modal ERR {e}")
        ctx.close()

        # === Works page ===
        ctx2 = browser.new_context(viewport={"width": 1440, "height": 900})
        page2 = ctx2.new_page()
        page2.goto(BASE, wait_until="networkidle")
        page2.evaluate(WORKS_JS)
        page2.goto(f"{BASE}/console/works", wait_until="networkidle")
        page2.wait_for_timeout(800)
        page2.evaluate("document.body.setAttribute('data-theme', 'dark')")
        page2.wait_for_timeout(500)
        page2.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/dark_works.png", full_page=True)
        check(page2, [".work-card", ".draft-card", ".work-title", ".work-action-btn", ".work-action-btn.primary", ".work-action-btn.danger"], "works page / works tab")
        # Switch to drafts
        try:
            page2.click("text=草稿箱", timeout=2000)
            page2.wait_for_timeout(500)
            check(page2, [".draft-card", ".draft-card .work-title"], "works / drafts tab")
        except Exception as e:
            print(f"drafts click ERR {e}")
        ctx2.close()
        browser.close()


if __name__ == "__main__":
    main()

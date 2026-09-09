#!/usr/bin/env python3
"""验证「小爱帮写」前端流程：我的提示词 → 新建 → 小爱帮写 → 生成 → 表单回填。"""
import jwt, time, uuid, sys
from playwright.sync_api import sync_playwright

BASE = 'http://localhost:22345'
SECRET = 'please-change-this-access-secret-at-least-256-bits-long'

now = int(time.time())
token = jwt.encode({'sub': '3045', 'jti': str(uuid.uuid4()), 'iat': now, 'exp': now + 7200},
                   SECRET, algorithm='HS256')

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    ctx = browser.new_context(viewport={'width': 1440, 'height': 900})
    page = ctx.new_page()
    page.add_init_script(
        f"localStorage.setItem('aichuangzuo_access_token', '{token}');"
        f"localStorage.setItem('aichuangzuo_access_token_expires_at', String(Date.now() + 7200000));"
    )
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))

    page.goto(f'{BASE}/console/skills', wait_until='networkidle')
    page.wait_for_timeout(1500)
    page.screenshot(path='tests/e2e/screenshots/gen1_list.png')

    # 新建我的提示词
    page.locator('.style-add-text', has_text='新建我的提示词').first.click()
    page.wait_for_timeout(800)
    page.screenshot(path='tests/e2e/screenshots/gen2_editor.png')

    # 小爱帮写
    page.locator('.style-editor-ai-btn').click()
    page.wait_for_timeout(1200)
    page.screenshot(path='tests/e2e/screenshots/gen3_dialog.png')

    # 输入描述方向
    page.locator('.generate-skill-modal textarea').fill(
        '知乎平台，程序员分享自己的副业实战经验，要求真实有数据有踩坑复盘，目标读者是想做副业的程序员')
    page.locator('.generate-skill-modal .learned-submit-btn').click()
    page.wait_for_timeout(2000)
    page.screenshot(path='tests/e2e/screenshots/gen4_generating.png')

    # 等待生成完成（AI 调用最长 90s）
    page.wait_for_selector('.generate-skill-modal', state='hidden', timeout=100000)
    page.wait_for_timeout(1000)
    page.screenshot(path='tests/e2e/screenshots/gen5_filled.png')

    # 保存
    page.locator('.save-style-btn').click()
    page.wait_for_timeout(2000)
    page.screenshot(path='tests/e2e/screenshots/gen6_saved.png')

    print('pageerrors:', errors if errors else 'none')
    browser.close()

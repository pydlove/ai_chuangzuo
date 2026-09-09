#!/usr/bin/env python3
"""验证提示词发布额度用完后点击「发布」会弹额度提示（修复静默无反馈）。"""
import jwt, time, uuid, requests
from playwright.sync_api import sync_playwright

BASE = 'http://localhost:22345'
API = 'http://localhost:25050'
SECRET = 'please-change-this-access-secret-at-least-256-bits-long'

now = int(time.time())
token = jwt.encode({'sub': '3045', 'jti': str(uuid.uuid4()), 'iat': now, 'exp': now + 7200},
                   SECRET, algorithm='HS256')

# 先造一个测试提示词，跑完再删
res = requests.post(f'{API}/api/v1/user/skills',
                    headers={'Authorization': f'Bearer {token}'},
                    json={'skillName': '发布额度验证测试', 'prompt': '你是一个测试助手。',
                          'scope': '测试', 'description': '验证发布额度提示'})
print('create skill:', res.status_code, res.json().get('code'))
TEST_BIZ_NO = res.json().get('data')

BENEFITS_RES = {
    'code': 0,
    'data': {
        'planKey': 'pro', 'planName': '专业版', 'expiresAt': '2027-01-01',
        'benefits': [
            {'code': 'skill_market_publish', 'name': '提示词市场上架', 'type': 'quota',
             'value': '3', 'used': 3, 'preUsed': 0, 'remaining': 0},
            {'code': 'skill_ai_generate', 'name': '小爱帮写', 'type': 'boolean', 'value': 'true'},
        ],
    },
}

def intercept_benefits(route):
    route.fulfill(status=200, content_type='application/json', body=str(BENEFITS_RES).replace("'", '"'))

def run(viewport, tag):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport=viewport)
        page = ctx.new_page()
        page.add_init_script(
            f"localStorage.setItem('aichuangzuo_access_token', '{token}');"
            f"localStorage.setItem('aichuangzuo_access_token_expires_at', String(Date.now() + 7200000));"
        )
        page.route('**/benefits/me', intercept_benefits)
        errors = []
        page.on('pageerror', lambda e: errors.append(str(e)))

        page.goto(f'{BASE}/console/skills', wait_until='networkidle')
        page.wait_for_timeout(1500)
        page.screenshot(path=f'tests/e2e/screenshots/publish_block_{tag}_1_list.png')

        # 点击卡片上的「发布」
        page.locator('.action-group__btn--primary', has_text='发布').first.click()
        page.wait_for_timeout(1200)
        page.screenshot(path=f'tests/e2e/screenshots/publish_block_{tag}_2_dialog.png')

        modal = page.locator('.membership-confirm-modal')
        if modal.count() == 0:
            print(f'[{tag}] FAIL: 未弹出额度提示弹窗')
        else:
            print(f'[{tag}] OK 弹窗标题:', modal.locator('.ant-modal-confirm-title').inner_text())
            print(f'[{tag}] OK 弹窗内容:', modal.locator('.ant-modal-confirm-content').inner_text())
        print(f'[{tag}] pageerrors:', errors if errors else 'none')
        browser.close()

run({'width': 1440, 'height': 900}, 'pc')
run({'width': 390, 'height': 844}, 'mobile')

# 清理测试数据
if TEST_BIZ_NO:
    del_res = requests.delete(f'{API}/api/v1/user/skills/{TEST_BIZ_NO}',
                              headers={'Authorization': f'Bearer {token}'})
    print('cleanup delete skill:', del_res.status_code)

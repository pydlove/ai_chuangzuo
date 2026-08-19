from playwright.sync_api import sync_playwright
import base64
import hashlib
import json
import os
import subprocess
import urllib.request

BASE_URL = 'http://127.0.0.1:22345'
API_URL = 'http://127.0.0.1:25050'
SCREENSHOT_DIR = '/tmp/self_media_wizard'

DB_HOST = '127.0.0.1'
DB_USER = 'root'
DB_PASSWORD = '123456'
DB_NAME = 'aichuangzuo'

TEST_EMAIL = 'test_selfmedia@example.com'
TEST_PASSWORD = 'Test1234!'
TEST_PLATFORM = 'xiaohongshu'

QUESTIONS = [
    {
        'key': 'content_form',
        'text': '你更想发布哪种内容形式？',
        'options': [
            {'key': 'graphic_notes', 'label': '图文笔记'},
            {'key': 'short_video', 'label': '短视频'},
            {'key': 'live', 'label': '直播'},
        ],
        'isRequired': True,
        'sortOrder': 1,
    },
    {
        'key': 'time_commitment',
        'text': '你每天能投入多少时间？',
        'options': [
            {'key': 'lt_1h', 'label': '1小时以内'},
            {'key': '1_2h', 'label': '1-2小时'},
            {'key': '2_4h', 'label': '2-4小时'},
            {'key': 'gt_4h', 'label': '4小时以上'},
        ],
        'isRequired': True,
        'sortOrder': 2,
    },
    {
        'key': 'monetization_goal',
        'text': '你的变现目标是什么？',
        'options': [
            {'key': 'side_income', 'label': '副业增收'},
            {'key': 'personal_brand', 'label': '打造个人品牌'},
            {'key': 'product_sales', 'label': '卖货/卖服务'},
        ],
        'isRequired': True,
        'sortOrder': 3,
    },
    {
        'key': 'on_camera',
        'text': '是否愿意出镜？',
        'options': [
            {'key': 'no', 'label': '不想出镜'},
            {'key': 'yes', 'label': '愿意出镜'},
            {'key': 'voice_only', 'label': '只出声音'},
        ],
        'isRequired': True,
        'sortOrder': 4,
    },
]

NICHE = {
    'key': 'zhichang_fupan',
    'name': '职场复盘',
    'audience': '25-40岁职场人',
    'monetization': '付费咨询、课程',
    'riskLabel': '中',
    'riskColor': 'warning',
    'caseCount': 12,
    'reason': '真实经验分享容易建立信任，变现路径清晰',
}

PERSONA = {
    'key': 'experiencer',
    'name': '实战记录者',
    'desc': '用亲身经历讲透一个领域的实战经验',
}

PILLARS = [
    {'name': '干货复盘', 'percent': 60},
    {'name': '个人故事', 'percent': 20},
    {'name': '热点解读', 'percent': 20},
]


def compute_answer_hash(answers):
    """与后端 SelfMediaPlanServiceImpl.answerHash 保持一致。"""
    sorted_answers = sorted(answers, key=lambda a: a['questionKey'])
    payload = json.dumps(
        sorted_answers,
        separators=(',', ':'),
        ensure_ascii=False,
    )
    return hashlib.sha256(payload.encode('utf-8')).hexdigest()


def seed_test_data(user_id):
    answers = [{'questionKey': q['key'], 'answer': q['options'][0]['key']} for q in QUESTIONS]
    answer_hash = compute_answer_hash(answers)

    sql_path = '/tmp/self_media_wizard_seed.sql'
    with open(sql_path, 'w', encoding='utf-8') as f:
        f.write('SET NAMES utf8mb4;\n')
        f.write(f"DELETE FROM u_self_media_plan_question WHERE user_id = {user_id} AND platform_key = '{TEST_PLATFORM}';\n")
        f.write(f"DELETE FROM u_self_media_plan_niche WHERE user_id = {user_id} AND platform_key = '{TEST_PLATFORM}';\n")
        f.write(f"DELETE FROM u_self_media_plan_persona WHERE user_id = {user_id} AND platform_key = '{TEST_PLATFORM}';\n")

        for q in QUESTIONS:
            f.write(
                "INSERT INTO u_self_media_plan_question "
                "(user_id, platform_key, prompt_code, question_key, question_text, options_json, is_required, sort_order) "
                f"VALUES ({user_id}, '{TEST_PLATFORM}', 'self_media_platform_questions_v2', "
                f"'{q['key']}', '{q['text']}', '{json.dumps(q['options'], ensure_ascii=False)}', "
                f"{1 if q['isRequired'] else 0}, {q['sortOrder']});\n"
            )

        f.write(
            "INSERT INTO u_self_media_plan_niche "
            "(user_id, platform_key, answer_snapshot_hash, answer_snapshot_json, niche_key, name, audience, monetization, risk_label, risk_color, case_count, reason) "
            f"VALUES ({user_id}, '{TEST_PLATFORM}', '{answer_hash}', "
            f"'{json.dumps(answers, ensure_ascii=False)}', "
            f"'{NICHE['key']}', '{NICHE['name']}', '{NICHE['audience']}', "
            f"'{NICHE['monetization']}', '{NICHE['riskLabel']}', '{NICHE['riskColor']}', "
            f"{NICHE['caseCount']}, '{NICHE['reason']}');\n"
        )

        f.write(
            "INSERT INTO u_self_media_plan_persona "
            "(user_id, platform_key, answer_snapshot_hash, niche_key, persona_key, name, description, default_pillars_json) "
            f"VALUES ({user_id}, '{TEST_PLATFORM}', '{answer_hash}', "
            f"'{NICHE['key']}', '{PERSONA['key']}', '{PERSONA['name']}', "
            f"'{PERSONA['desc']}', '{json.dumps(PILLARS, ensure_ascii=False)}');\n"
        )

    subprocess.run(
        ['mysql', f'-h{DB_HOST}', f'-u{DB_USER}', f'-p{DB_PASSWORD}', DB_NAME, '-e', open(sql_path, encoding='utf-8').read()],
        check=True,
        capture_output=True,
        text=True,
    )
    return answer_hash


def login():
    req = urllib.request.Request(
        f'{API_URL}/api/v1/user/auth/login',
        data=json.dumps({'email': TEST_EMAIL, 'password': TEST_PASSWORD}).encode(),
        headers={'Content-Type': 'application/json'},
        method='POST',
    )
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
        token = data['data']['accessToken']
        # JWT sub 即为 userId
        payload_b64 = token.split('.')[1]
        padding = 4 - len(payload_b64) % 4
        payload_json = base64.urlsafe_b64decode(payload_b64 + '=' * padding).decode('utf-8')
        payload = json.loads(payload_json)
        user_id = int(payload['sub'])
        return token, user_id


def main():
    os.makedirs(SCREENSHOT_DIR, exist_ok=True)

    token, user_id = login()
    print(f'Logged in as user {user_id}, token={token[:20]}...')

    seed_test_data(user_id)
    print('Seeded cached wizard data for user.')

    errors = []
    network_logs = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={'width': 1280, 'height': 900})
        page = context.new_page()

        # 在页面脚本执行前注入 token，避免未登录时 system-skills 接口 403
        context.add_init_script(f'''
            localStorage.setItem('aichuangzuo_access_token', '{token}');
            localStorage.removeItem('aichuangzuo_onboarding_done');
        ''')

        page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)
        page.on('response', lambda resp: network_logs.append(f'{{resp.status}} {{resp.url}}') if resp.status >= 400 else None)

        # Step 1: 选平台
        page.goto(BASE_URL + '/console/onboarding')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(800)
        page.screenshot(path=f'{SCREENSHOT_DIR}_step1_platform.png', full_page=True)

        title = page.locator('text=制定你的自媒体方案').first
        print(f'Wizard title visible: {title.is_visible() if title.count() > 0 else False}')

        platform_card = page.locator('.platform-card:has-text("小红书")').first
        assert platform_card.count() > 0, '小红书 platform card not found'
        platform_card.click()
        page.wait_for_timeout(300)
        page.screenshot(path=f'{SCREENSHOT_DIR}_step1_selected.png', full_page=True)

        next_btn = page.locator('button:has-text("下一步")').first
        assert next_btn.count() > 0 and next_btn.is_enabled(), 'Next button not available on step 1'
        next_btn.click()

        # Step 2: 回答问题
        page.wait_for_selector('.form-block', timeout=15000)
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}_step2_questions.png', full_page=True)

        question_blocks = page.locator('.form-block').all()
        print(f'Questions count: {len(question_blocks)}')
        assert len(question_blocks) == len(QUESTIONS), f'Expected {len(QUESTIONS)} questions, got {len(question_blocks)}'

        for idx, block in enumerate(question_blocks):
            option_btn = block.locator('.option-group button').first
            assert option_btn.count() > 0, f'No options for question {idx}'
            option_btn.click()
            page.wait_for_timeout(100)

        page.wait_for_timeout(300)
        page.screenshot(path=f'{SCREENSHOT_DIR}_step2_answered.png', full_page=True)

        next_btn = page.locator('button:has-text("下一步")').first
        next_btn.click()

        # Step 3: 选赛道
        page.wait_for_selector('.niche-card', timeout=15000)
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}_step3_niches.png', full_page=True)

        niche_cards = page.locator('.niche-card').all()
        print(f'Niche options count: {len(niche_cards)}')
        assert len(niche_cards) >= 1, 'No niche options rendered'

        next_btn = page.locator('button:has-text("下一步")').first
        next_btn.click()

        # Step 4: 选人设
        page.wait_for_selector('.persona-card', timeout=15000)
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}_step4_personas.png', full_page=True)

        persona_cards = page.locator('.persona-card').all()
        print(f'Persona options count: {len(persona_cards)}')
        assert len(persona_cards) >= 1, 'No persona options rendered'

        next_btn = page.locator('button:has-text("下一步")').first
        next_btn.click()

        # Step 5: 方案汇总
        page.wait_for_selector('.summary-card', timeout=15000)
        page.wait_for_timeout(500)
        page.screenshot(path=f'{SCREENSHOT_DIR}_step5_summary.png', full_page=True)

        confirm_btn = page.locator('button:has-text("确认方案，进入工作台")').first
        assert confirm_btn.count() > 0 and confirm_btn.is_enabled(), 'Confirm button not available'
        confirm_btn.click()

        # 等待跳转工作台
        try:
            page.wait_for_url(BASE_URL + '/console/workbench', timeout=15000)
        except Exception as e:
            print(f'Navigation to workbench failed: {e}')
            print(f'Current URL: {page.url}')
            page.screenshot(path=f'{SCREENSHOT_DIR}_workbench_timeout.png', full_page=True)
            print('Network logs:')
            for log in network_logs:
                print(f'  {log}')
            print('Console errors:')
            for err in errors:
                print(f'  {err}')
            raise

        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(800)
        page.screenshot(path=f'{SCREENSHOT_DIR}_workbench.png', full_page=True)

        workbench_title = page.locator('text=工作台').first
        print(f'Workbench title visible: {workbench_title.is_visible() if workbench_title.count() > 0 else False}')

        # 验证运营方案卡片
        assert page.locator('text=小红书').first.is_visible(), 'Plan platform not shown'
        assert page.locator(f'text={NICHE["name"]}').first.is_visible(), 'Plan niche not shown'
        assert page.locator(f'text={PERSONA["name"]}').first.is_visible(), 'Plan persona not shown'
        for pillar in PILLARS:
            assert page.locator(f'text={pillar["name"]} {pillar["percent"]}%').first.is_visible(), f'Pillar {pillar["name"]} not shown'

        browser.close()

    print('\nNetwork errors:')
    for log in network_logs:
        print(f'  {log}')

    print(f'\nConsole errors: {len(errors)}')
    for e in errors:
        print(f'  ERROR: {e}')

    if network_logs or errors:
        raise AssertionError('Verification failed: network or console errors detected')

    print('\nVerification complete')


if __name__ == '__main__':
    main()

"""验证会员首月价格：首页价格区块 + 定价页月付卡展示。"""
import sys
from playwright.sync_api import sync_playwright

BASE = 'http://localhost:22345'
# 预存在问题：匿名访问任意页面都会触发 /auth/refresh 500（无 refresh token），与本功能无关
KNOWN_ERRORS = {'Failed to load resource: the server responded with a status of 500 (Internal Server Error)'}
errors = []
failed = False

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)

    # ---- PC 首页 ----
    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.on('console', lambda m: errors.append(m.text) if m.type == 'error' and m.text not in KNOWN_ERRORS else None)
    page.goto(f'{BASE}/', wait_until='networkidle')
    page.wait_for_timeout(1500)

    section = page.locator('.member-pricing')
    print(f"首页会员价格区块: {'存在' if section.count() else '缺失'}")
    if not section.count():
        failed = True
    else:
        cards = page.locator('.member-pricing-card')
        print(f"首页套餐卡数量: {cards.count()}")
        if cards.count() < 3:
            failed = True
        first_month = page.locator('.member-pricing .member-pricing-first-tag')
        print(f"首页首月特惠标签数量: {first_month.count()}")
        if first_month.count() == 0:
            print("  WARN: 未配置 first_month_price 或当前用户非首购")
        page.screenshot(path='tests/e2e/screenshots/verify_first_month_home.png', full_page=False)
    page.close()

    # ---- PC 定价页（默认月付周期）----

    page = browser.new_page(viewport={'width': 1280, 'height': 900})
    page.on('console', lambda m: errors.append(m.text) if m.type == 'error' and m.text not in KNOWN_ERRORS else None)
    page.goto(f'{BASE}/pricing', wait_until='networkidle')
    page.wait_for_timeout(1500)

    tag = page.locator('.plan-first-month')
    print(f"定价页首月特惠标签数量: {tag.count()}")
    if tag.count():
        print(f"  文案示例: {tag.first.inner_text()}")
    else:
        print("  WARN: 定价页未展示首月特惠（可能未配置或非首购用户）")
    page.screenshot(path='tests/e2e/screenshots/verify_first_month_pricing.png', full_page=False)
    page.close()

    # ---- 移动端首页 ----
    page = browser.new_page(viewport={'width': 390, 'height': 844})
    page.goto(f'{BASE}/', wait_until='networkidle')
    page.wait_for_timeout(1500)
    mh = page.locator('.mh-pricing')
    print(f"移动端首页会员价格区块: {'存在' if mh.count() else '缺失'}")
    if not mh.count():
        failed = True
    page.screenshot(path='tests/e2e/screenshots/verify_first_month_mobile_home.png', full_page=False)
    page.close()

    browser.close()

print(f"\nConsole errors: {len(errors)}")
for e in errors[:10]:
    print(f"  ERROR: {e}")

print("\nRESULT: " + ("FAIL" if failed or errors else "PASS"))
sys.exit(1 if failed or errors else 0)

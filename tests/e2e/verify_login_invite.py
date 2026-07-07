#!/usr/bin/env python3
"""Vue 控制台登录页邀请码端到端验证。

覆盖场景：
  1) 无 ref 直接打开 → 输入框为空，无 banner
  2) 带 ref=ABC123 打开 → 输入框预填，banner 显示
  4) 手动填邀请码注册 → 跳 /console + coin_balance=5
  5) 填自己的邀请码 → message.warning 拦截
  7) URL 带与自身不同的 ref → 正常注册 + 发币

用法：
  cd project/user/web && npm run dev   # 启动 Vue dev server（默认 :22345）
  python3 tests/e2e/verify_login_invite.py
  # 或指定端口
  BASE_URL=http://localhost:22345 python3 tests/e2e/verify_login_invite.py
"""
import os
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = os.environ.get("BASE_URL", "http://localhost:22345")
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def open_login(page, ref=None):
    url = f"{BASE_URL}/login" + (f"?ref={ref}" if ref else "")
    page.goto(url)
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(500)


def switch_to_register(page):
    """点击「注册」tab。"""
    page.locator("button.auth-tab", has_text="注册").click()
    page.wait_for_timeout(300)


def main():
    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        try:
            # ========== 场景 1: 无 ref 直接打开 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            page.evaluate("""() => {
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_invite_code')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page)
            # 默认 tab 应为「登录」（无 ref）
            default_tab = page.locator("button.auth-tab.active").inner_text()
            switch_to_register(page)
            invite_val = page.locator('input[placeholder="如没有可留空"]').input_value()
            banner_count = page.locator(".invite-banner").count()
            banner_visible = banner_count > 0 and page.locator(".invite-banner").is_visible()
            ok = invite_val == "" and not banner_visible and default_tab == "登录"
            results.append(("场景1 无ref默认登录tab+输入框为空+banner隐藏", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_no_ref.png"))
            ctx.close()

            # ========== 场景 2: 带 ref=ABC123 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            page.evaluate("""() => {
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page, ref="ABC123")
            # 默认 tab 应为「注册」（带 ref）
            default_tab = page.locator("button.auth-tab.active").inner_text()
            invite_val = page.locator('input[placeholder="如没有可留空"]').input_value()
            banner_visible = page.locator(".invite-banner").count() > 0 and \
                             page.locator(".invite-banner").is_visible()
            ok = invite_val == "ABC123" and banner_visible and default_tab == "注册"
            results.append(("场景2 ref默认注册tab+自动填充+banner显示", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_auto_fill.png"))
            ctx.close()

            # ========== 场景 5: 自邀请拦截 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            # 模拟已在 ConsoleLayout 生成自己的码
            page.evaluate("""() => {
              localStorage.setItem('aichuangzuo_invite_code', 'K7P2QX')
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page, ref="K7P2QX")
            # 带 ref 默认就是注册 tab，无需 switch_to_register
            # 直接点注册按钮（限定在「注册」字样的 submit-btn）
            page.locator("button.submit-btn", has_text="注册").click()
            page.wait_for_timeout(800)
            toast_visible = page.locator("text=不能填写自己的邀请码").is_visible()
            current_url = page.url
            ok = toast_visible and "/login" in current_url
            results.append(("场景5 自邀请拦截", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_self_block.png"))
            ctx.close()

            # ========== 场景 4: 手动填邀请码注册 + 发币 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            page.evaluate("""() => {
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
              localStorage.removeItem('aichuangzuo_invite_code')
            }""")
            open_login(page)
            switch_to_register(page)
            page.locator('input[placeholder="如没有可留空"]').fill("ABC123")
            page.locator("button.submit-btn", has_text="注册").click()
            page.wait_for_timeout(800)
            current_url = page.url
            coin_balance = page.evaluate(
                "() => parseInt(localStorage.getItem('aichuangzuo_coin_balance') || '0', 10)"
            )
            ok = "/console" in current_url and coin_balance == 5
            results.append(("场景4 手动填邀请码+发币", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_manual.png"))
            ctx.close()

            # ========== 场景 7: URL 带与自身不同的 ref 正常注册 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            page.evaluate("""() => {
              localStorage.setItem('aichuangzuo_invite_code', 'SELFCODE')
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page, ref="EXTREF9")
            # 带 ref 默认就是注册 tab，无需 switch_to_register
            page.locator("button.submit-btn", has_text="注册").click()
            page.wait_for_timeout(800)
            current_url = page.url
            coin_balance = page.evaluate(
                "() => parseInt(localStorage.getItem('aichuangzuo_coin_balance') || '0', 10)"
            )
            ok = "/console" in current_url and coin_balance == 5
            results.append(("场景7 外部ref正常注册+发币", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_normal.png"))
            ctx.close()

        finally:
            browser.close()

    # 输出结果
    print("\n=== Vue 注册邀请码验证结果 ===")
    all_ok = True
    for name, ok in results:
        status = "✓ PASS" if ok else "✗ FAIL"
        print(f"{status}  {name}")
        if not ok:
            all_ok = False
    print()
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
#!/usr/bin/env python3
"""注册页邀请码端到端验证。

覆盖场景：
  1) 无 ref 直接打开 → 输入框为空，banner 不显示
  2) 带 ref=ABC123 打开 → 输入框预填，banner 显示
  3) PC 端编辑输入框 → 移动端同步
  5) 已有自己邀请码的情况下填同样的码 → toast 拦截
  7) URL 带与自己不同的 ref → 正常注册

用法：
  ./scripts/local/start.sh          # 启动原型服务器
  python3 tests/e2e/verify_register_invite.py
"""
import os
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = os.environ.get(
    "BASE_URL",
    "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/login.html",
)
INVITE_URL = BASE_URL.rsplit("/", 1)[0] + "/invite.html"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def open_login(page, ref=None):
    url = BASE_URL + (f"?ref={ref}" if ref else "")
    page.goto(url)
    page.wait_for_load_state("domcontentloaded")
    page.wait_for_timeout(300)


def switch_to_register(page):
    """点击 PC 与移动端两边的注册 tab。"""
    tabs = page.locator(".auth-tab", has_text="注册")
    count = tabs.count()
    for i in range(count):
        tabs.nth(i).click()
    page.wait_for_timeout(200)


def main():
    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        try:
            # ========== 场景 1: 无 ref 直接打开 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            open_login(page)
            switch_to_register(page)
            pc_val = page.locator("#pc-invite-code-input").input_value()
            mobile_val = page.locator("#mobile-invite-code-input").input_value()
            banner_visible = page.locator("#pc-invite-banner").is_visible()
            ok = pc_val == "" and mobile_val == "" and not banner_visible
            results.append(("场景1 无ref输入框为空+banner隐藏", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_no_ref.png"))
            ctx.close()

            # ========== 场景 2: 带 ref=ABC123 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            open_login(page, ref="ABC123")
            switch_to_register(page)
            pc_val = page.locator("#pc-invite-code-input").input_value()
            mobile_val = page.locator("#mobile-invite-code-input").input_value()
            banner_visible = page.locator("#pc-invite-banner").is_visible()
            ok = pc_val == "ABC123" and mobile_val == "ABC123" and banner_visible
            results.append(("场景2 ref自动填充+banner显示", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_auto_fill.png"))
            ctx.close()

            # ========== 场景 3: PC ↔ mobile 双向同步 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            # 用一个短的初始 ref，避免 fill 受 maxlength 截断后值不变
            open_login(page, ref="AB")
            switch_to_register(page)
            pc_input = page.locator("#pc-invite-code-input")
            pc_input.fill("ABCDEF")
            page.wait_for_timeout(150)
            mobile_val = page.locator("#mobile-invite-code-input").input_value()
            ok = mobile_val == "ABCDEF"
            results.append(("场景3 PC→mobile同步", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_sync.png"))
            ctx.close()

            # ========== 场景 5: 自邀请拦截 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            # 先访问 invite.html 触发 getInviteCode() 生成自己的码
            page.goto(INVITE_URL)
            page.wait_for_load_state("domcontentloaded")
            page.wait_for_timeout(400)
            self_code = page.evaluate(
                "() => localStorage.getItem('aichuangzuo_invite_code')"
            )
            assert self_code, "自己的邀请码未生成"

            # 回 login.html 填自己的码
            open_login(page)
            switch_to_register(page)
            page.locator("#pc-invite-code-input").fill(self_code)
            # 点击注册按钮
            page.locator("#pc-register button.mock-button").click()
            page.wait_for_timeout(600)
            toast_visible = page.locator("text=不能填写自己的邀请码").is_visible()
            current_url = page.url
            ok = toast_visible and "login.html" in current_url
            results.append(("场景5 自邀请拦截", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_self_block.png"))
            ctx.close()

            # ========== 场景 7: 带与自身不同的 ref 正常注册 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(INVITE_URL)
            page.wait_for_load_state("domcontentloaded")
            page.wait_for_timeout(400)
            self_code = page.evaluate(
                "() => localStorage.getItem('aichuangzuo_invite_code')"
            )
            other_ref = "ZZZZZZ" if self_code != "ZZZZZZ" else "YYYYYY"

            open_login(page, ref=other_ref)
            switch_to_register(page)
            page.locator("#pc-register button.mock-button").click()
            page.wait_for_timeout(800)
            current_url = page.url
            coin_balance = page.evaluate(
                "() => parseInt(localStorage.getItem('aichuangzuo_coin_balance') || '0', 10)"
            )
            # awardNewUserCoins 发放 5 创作币后会 removeItem ref，
            # 所以通过 coin_balance == 5 验证 ref 确实被消费。
            ok = "create.html" in current_url and coin_balance == 5
            results.append(("场景7 外部ref正常注册", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_normal.png"))
            ctx.close()

        finally:
            browser.close()

    # 输出结果
    print("\n=== 注册邀请码验证结果 ===")
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
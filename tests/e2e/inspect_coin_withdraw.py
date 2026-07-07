from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        # Step 1: open the invite modal and find the "去提现" button
        page.goto(f"{BASE}/console/preview")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        page.click(".console-invite-btn")
        page.wait_for_selector(".invite-panel", timeout=5000)
        page.wait_for_timeout(500)

        # The new "去提现" button should be visible next to the 创作币余额
        go_btn = page.locator(".invite-stat-go-withdraw")
        print(f"去提现 button visible: {go_btn.is_visible()}")
        print(f"去提现 button text: {go_btn.text_content()}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_with_go_to_withdraw.png", full_page=False)
        print("Saved: invite_with_go_to_withdraw.png")

        # Step 2: navigate to /console/coin directly
        page.goto(f"{BASE}/console/coin")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # Should land on the WithdrawIndex page
        title_visible = page.locator(".coin-page-title").is_visible()
        print(f"withdraw page title visible: {title_visible}")
        print(f"page title text: {page.locator('.coin-page-title').text_content()}")

        # Should show 实名认证 status (initially unverified since fresh localStorage state)
        auth_status = page.locator(".coin-auth-status").text_content()
        print(f"initial auth status: {auth_status}")

        # Check stat cards
        balance = page.locator(".coin-stat-card").nth(0).locator(".coin-stat-value").text_content()
        withdrawn = page.locator(".coin-stat-card").nth(1).locator(".coin-stat-value").text_content()
        earned = page.locator(".coin-stat-card").nth(2).locator(".coin-stat-value").text_content()
        print(f"balance: {balance}, withdrawn: {withdrawn}, earned: {earned}")

        # Eligibility tag should be "暂不可提现" (no auth)
        elig_tag = page.locator(".coin-eligibility-tag").text_content()
        print(f"eligibility tag: {elig_tag}")

        # Apply button text should indicate disabled state
        apply_btn = page.locator(".coin-withdraw-btn")
        print(f"apply button text: {apply_btn.text_content()}")
        print(f"apply button disabled: {apply_btn.is_disabled()}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/coin_page_initial.png", full_page=False)
        print("Saved: coin_page_initial.png")

        # Step 3: fill in 实名认证
        page.fill(".coin-form-field:nth-child(1) .coin-form-input", "张三")
        page.fill(".coin-form-field:nth-child(2) .coin-form-input", "11010519491231002X")

        submit_auth = page.locator(".coin-form-actions .invite-btn-primary")
        print(f"submit real-name btn enabled: {submit_auth.is_enabled()}")
        submit_auth.click()
        page.wait_for_timeout(500)

        # After submit, should show "已认证" status and auth display rows
        auth_after = page.locator(".coin-auth-status").text_content()
        print(f"auth status after submit: {auth_after}")
        display_visible = page.locator(".coin-auth-display").is_visible()
        print(f"auth display visible: {display_visible}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/coin_page_authenticated.png", full_page=False)
        print("Saved: coin_page_authenticated.png")

        # Step 4: set coin balance high enough to test withdraw via console
        # (simulate giving balance through dev tools)
        page.evaluate("localStorage.setItem('aichuangzuo_coin_balance', '150')")
        page.reload()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        balance2 = page.locator(".coin-stat-card").nth(0).locator(".coin-stat-value").text_content()
        elig_tag2 = page.locator(".coin-eligibility-tag").text_content()
        apply_btn2 = page.locator(".coin-withdraw-btn")
        print(f"after balance set: balance={balance2}, eligibility={elig_tag2}, btn enabled={apply_btn2.is_enabled()}, btn text={apply_btn2.text_content()}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/coin_page_ready.png", full_page=False)
        print("Saved: coin_page_ready.png")

        # Step 5: open apply modal, fill, submit
        apply_btn2.click()
        page.wait_for_selector(".coin-apply-panel", timeout=5000)
        page.fill(".coin-apply-input[type='number']", "100")
        page.fill(".coin-apply-item:nth-child(4) .coin-apply-input", "13800138000")
        page.wait_for_timeout(300)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/coin_apply_modal.png", full_page=False)
        print("Saved: coin_apply_modal.png")

        submit_apply = page.locator(".coin-apply-actions .invite-btn-primary")
        print(f"submit apply enabled: {submit_apply.is_enabled()}")
        submit_apply.click()
        page.wait_for_timeout(800)

        # Records should now show one entry
        record_rows = page.locator(".coin-records-row").count()
        print(f"withdraw records rows: {record_rows}")
        balance3 = page.locator(".coin-stat-card").nth(0).locator(".coin-stat-value").text_content()
        withdrawn3 = page.locator(".coin-stat-card").nth(1).locator(".coin-stat-value").text_content()
        print(f"after withdraw: balance={balance3}, total withdrawn={withdrawn3}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/coin_page_after_withdraw.png", full_page=False)
        print("Saved: coin_page_after_withdraw.png")

        browser.close()
        print("Done.")


if __name__ == "__main__":
    main()
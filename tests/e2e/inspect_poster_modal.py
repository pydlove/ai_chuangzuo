from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        # Step 1: open invite modal from preview
        page.goto(f"{BASE}/console/preview")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        page.click(".console-invite-btn")
        page.wait_for_selector(".invite-panel", timeout=5000)
        page.wait_for_timeout(500)

        # Verify: 创作币返利 text should have bold + underline
        rule_underline = page.locator(".invite-rule-underline")
        font_weight = rule_underline.evaluate("el => getComputedStyle(el).fontWeight")
        text_decoration = rule_underline.evaluate("el => getComputedStyle(el).textDecorationLine")
        print(f"rule underline font-weight: {font_weight} (expect 700)")
        print(f"rule underline text-decoration: {text_decoration} (expect underline)")

        # Verify: 满 100 可提现 button should be removed
        link_actions_text = page.locator(".invite-link-actions").text_content()
        print(f"link actions text: {link_actions_text}")
        has_withdraw_btn = page.locator(".invite-link-actions .invite-btn-primary").count()
        print(f"withdraw button count in link actions: {has_withdraw_btn} (expect 0)")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/invite_v2_after_fixes.png", full_page=False)
        print("Saved: invite_v2_after_fixes.png")

        # Step 2: open poster style modal
        page.click(".invite-link-actions .invite-btn-secondary:nth-child(2)")  # 下载海报 button
        page.wait_for_selector(".poster-panel", timeout=5000)
        page.wait_for_timeout(500)

        cards = page.locator(".poster-card").count()
        print(f"poster style cards: {cards} (expect 4)")

        active_count = page.locator(".poster-card.active").count()
        print(f"default active card count: {active_count} (expect 1)")

        selected_name = page.locator(".poster-card.active .poster-card-name").text_content()
        print(f"default selected: {selected_name}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/poster_modal_default.png", full_page=False)
        print("Saved: poster_modal_default.png")

        # Step 3: click "深色高级" (3rd card)
        page.click(".poster-card:nth-child(3)")
        page.wait_for_timeout(300)
        active_after = page.locator(".poster-card.active .poster-card-name").text_content()
        print(f"after click 3rd card, active: {active_after}")

        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/poster_modal_dark_selected.png", full_page=False)
        print("Saved: poster_modal_dark_selected.png")

        # Step 4: click download
        download_btn = page.locator(".poster-actions .invite-btn-primary")
        print(f"download button text: {download_btn.text_content()}")

        with page.expect_download(timeout=5000) as download_info:
            download_btn.click()
        download = download_info.value
        print(f"download triggered: {download.suggested_filename}")

        page.wait_for_timeout(500)
        page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/poster_modal_after_download.png", full_page=False)
        print("Saved: poster_modal_after_download.png")

        # Step 5: close modal and verify
        page.click(".poster-actions .invite-btn-secondary")
        page.wait_for_timeout(300)
        modal_visible = page.locator(".poster-panel").count()
        print(f"poster panel after close: {modal_visible} (expect 0)")

        # Step 6: close invite modal too, verify back to preview
        page.locator(".ant-modal-close").first.click()
        page.wait_for_timeout(500)

        # Step 7: test the "返回邀请有礼" flow goes to /console/create
        page.click(".console-invite-btn")
        page.wait_for_selector(".invite-panel", timeout=5000)
        page.wait_for_timeout(300)
        page.click(".invite-stat-go-withdraw")
        page.wait_for_url(f"{BASE}/console/coin", timeout=5000)
        page.wait_for_timeout(500)
        page.click(".coin-page-header .invite-btn-secondary")
        page.wait_for_timeout(800)

        url_after_back = page.url
        print(f"URL after 返回邀请有礼: {url_after_back}")
        modal_open = page.locator(".invite-panel").is_visible()
        print(f"invite modal re-opened: {modal_open}")

        # Close invite modal — should now be on /console/create
        page.locator(".ant-modal-close").first.click()
        page.wait_for_timeout(500)
        final_url = page.url
        print(f"final URL after closing modal: {final_url}")

        browser.close()
        print("Done.")


if __name__ == "__main__":
    main()
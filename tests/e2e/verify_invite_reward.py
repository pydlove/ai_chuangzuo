import re
from playwright.sync_api import sync_playwright, expect

BASE = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"


def test_invite_page_loads(page):
    page.goto(f"{BASE}/invite.html")
    expect(page.locator(".invite-header")).to_contain_text("邀请有礼")
    code = page.locator("#invite-code-display").text_content()
    assert re.match(r"^[A-Z0-9]{6}$", code), f"invite code invalid: {code}"


def test_simulate_friend_register(page):
    page.goto(f"{BASE}/invite.html")
    page.fill("#simulate-friend-email", "test-friend@example.com")
    page.click("button[onclick*=\"simulateInviteRegister\"]")
    expect(page.locator("#invite-stat-count")).to_contain_text("1")
    expect(page.locator("#invite-friend-list")).to_contain_text("test-friend@example.com")


def test_login_ref_banner(page):
    page.goto(f"{BASE}/login.html?ref=ABCDEF")
    page.click("button[onclick*=\"switchAuth('pc', 'register')\"]")
    expect(page.locator("#pc-invite-banner")).to_be_visible()
    expect(page.locator("#pc-invite-banner")).to_contain_text("5 个创作币")


def test_pricing_coin_discount(page):
    page.goto(f"{BASE}/pricing.html")
    page.evaluate("""
        localStorage.setItem('aichuangzuo_coin_balance', '20');
    """)
    page.reload()
    expect(page.locator(".coin-balance").first).to_contain_text("20")
    page.locator(".coin-discount-input").first.fill("5")
    expect(page.locator(".final-amount").first).not_to_contain_text("29.9")


if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context()
        page = context.new_page()

        test_invite_page_loads(page)
        test_simulate_friend_register(page)
        test_login_ref_banner(page)
        test_pricing_coin_discount(page)

        browser.close()
        print("All invite reward checks passed.")

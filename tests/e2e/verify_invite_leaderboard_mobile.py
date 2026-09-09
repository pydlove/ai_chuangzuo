"""验证邀请排行榜领奖台在超长昵称下不被挤压（移动端 390px 视口）。"""
import sys
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"

MOCK_DATA = {
    "code": 0,
    "data": {
        "month": "all",
        "topList": [
            {
                "userId": 101,
                "nickname": "一杯冰美式",
                "avatarUrl": None,
                "memberLevel": None,
                "amount": 409.50,
                "inviteCount": 8,
                "rank": 1,
                "isMe": False,
            },
            {
                "userId": 102,
                "nickname": "姚太阳",
                "avatarUrl": None,
                "memberLevel": None,
                "amount": 350.10,
                "inviteCount": 10,
                "rank": 2,
                "isMe": False,
            },
            {
                "userId": 103,
                "nickname": "我姓周，却不能护你周全！",
                "avatarUrl": None,
                "memberLevel": None,
                "amount": 219.80,
                "inviteCount": 6,
                "rank": 3,
                "isMe": False,
            },
        ],
        "me": None,
    },
}


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        page = ctx.new_page()

        page.add_init_script(
            "localStorage.setItem('aichuangzuo_access_token', 'dummy-token');"
            "localStorage.setItem('aichuangzuo_access_token_expires_at',"
            " String(Date.now() + 10 * 365 * 24 * 3600 * 1000));"
        )

        def handle_api(route):
            if "leaderboards/invite" in route.request.url:
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body=__import__("json").dumps(MOCK_DATA),
                )
            else:
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body=__import__("json").dumps({"code": 0, "data": None}),
                )

        page.route("**/api/v1/user/**", handle_api)

        page.goto(f"{BASE}/console/invite-leaderboard", wait_until="networkidle")
        page.wait_for_selector(".invite-rank-podium", timeout=10000)
        page.wait_for_timeout(500)

        result = page.evaluate(
            """() => {
              const cards = [...document.querySelectorAll('.invite-rank-podium .podium-card')];
              return cards.map(c => {
                const name = c.querySelector('.podium-card__name');
                const style = getComputedStyle(name);
                return {
                  width: c.getBoundingClientRect().width,
                  clipped: name.scrollWidth > name.clientWidth,
                  ellipsis: style.textOverflow === 'ellipsis',
                };
              });
            }"""
        )
        print("card metrics:", result)
        widths = [r["width"] for r in result]
        assert len(widths) == 3, f"expected 3 podium cards, got {len(widths)}"
        assert max(widths) - min(widths) < 1, f"podium cards squeezed: widths={widths}"
        assert all(r["ellipsis"] for r in result), "nickname ellipsis style missing"
        assert result[2]["clipped"], "long nickname should be clipped to card width"

        page.screenshot(path="tests/e2e/screenshots/invite_leaderboard_mobile.png", full_page=False)
        print("PASS: podium cards equal width, long nickname clipped with ellipsis")
        browser.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print("FAIL:", e)
        sys.exit(1)

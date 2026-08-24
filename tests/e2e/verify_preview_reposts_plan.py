#!/usr/bin/env python3
"""Verify the article preview page renders the reposts plan on the article."""
import re
import time
import uuid
import jwt
from playwright.sync_api import sync_playwright

USER_SECRET = "please-change-this-access-secret-at-least-256-bits-long"
USER_WEB = "http://localhost:22345"


def make_token(user_id: int) -> str:
    now = int(time.time())
    payload = {
        "sub": str(user_id),
        "jti": str(uuid.uuid4()),
        "iat": now,
        "exp": now + 7200,
    }
    return jwt.encode(payload, USER_SECRET, algorithm="HS256")


def main():
    user_id = 900000000 + int(time.time() * 1000) % 100000000
    token = make_token(user_id)
    publish_called = {"count": 0}

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 900})
        page = context.new_page()

        page.goto(USER_WEB)
        page.wait_for_load_state("networkidle")
        page.evaluate(f"localStorage.setItem('aichuangzuo_access_token', '{token}')")

        def handle_route(route):
            url = route.request.url
            if "self-media-plans/actions/publish-plan" in url:
                publish_called["count"] += 1
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body='{"code":0,"message":"success","data":{"mainPlatform":{"platform":"小红书","publishTime":"每晚 19:30-20:30","reason":"职场人群晚间活跃高峰"},"coldStart":{"immediateActions":["发布后立即完整阅读1遍","自己点赞、收藏并评论"],"duration":"发布后 30 分钟内","sharingTips":"可分享到2-3个相关微信群或朋友圈，引导前5个互动"},"reposts":[{"platform":"公众号","publishTime":"次日 07:30","title":"35+被裁员后，自由职业转型的3个真实方法","tags":["职场转型","自由职业","副业"],"imageSuggestions":"封面用真人出镜+痛点文字，配3-5张方法步骤图"}]}}',
                )
                return
            if re.search(r"/articles/[^/]+$", url):
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body='{"code":0,"message":"success","data":{"bizNo":"T202608200001","title":"35+ 被裁员后，我用这 3 个方法半年内转型自由职业","body":"正文内容","wordCount":1200,"completedAt":"2026-08-20T10:00:00","platform":"xiaohongshu","skillName":"专业严谨","template":"xiaohongshu_default","description":"描述","tags":["标签1","标签2"]}}',
                )
                return
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":0,"message":"success","data":null}',
            )

        context.route(re.compile(r"/api/v1/user/"), handle_route)

        page.goto(f"{USER_WEB}/console/preview/T202608200001")
        page.wait_for_selector(".preview-article", timeout=15000)
        page.wait_for_selector("text=冷启动策略", timeout=10000)
        page.wait_for_selector("text=发布后 30 分钟内", timeout=10000)
        page.wait_for_selector("text=自己点赞、收藏并评论", timeout=10000)
        page.wait_for_selector("text=一文多发方案", timeout=10000)
        page.wait_for_selector("text=公众号", timeout=10000)
        page.wait_for_selector("text=次日 07:30", timeout=10000)
        page.wait_for_selector("text=35+被裁员后", timeout=10000)
        page.wait_for_selector("text=职场转型", timeout=10000)
        page.wait_for_selector("text=封面用真人出镜", timeout=10000)

        assert publish_called["count"] == 1, f"Expected 1 publish plan call, got {publish_called['count']}"

        print("Test passed: reposts plan rendered on article preview page")

        browser.close()


if __name__ == "__main__":
    main()

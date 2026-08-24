#!/usr/bin/env python3
"""Verify the publish guide and per-article reposts/article-view buttons on workbench."""
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
    request_body = {}

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
                body = route.request.post_data
                if body:
                    import json

                    try:
                        request_body.update(json.loads(body))
                    except Exception:
                        pass
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body='{"code":0,"message":"success","data":{"mainPlatform":{"platform":"小红书","publishTime":"每晚 19:30-20:30","reason":"职场人群晚间活跃高峰，完播率更高"},"coldStart":{"immediateActions":["发布后立即完整阅读1遍","自己点赞、收藏并评论","浏览同领域5条笔记并互动"],"duration":"发布后 30 分钟内","sharingTips":"可分享到2-3个相关微信群或朋友圈，引导前5个互动，不要刷屏"},"reposts":[{"platform":"公众号","publishTime":"次日 07:30","title":"35+被裁员后，自由职业转型的3个真实方法","tags":["职场转型","自由职业","副业","35+","经验分享"],"imageSuggestions":"封面用真人出镜+痛点文字，配3-5张方法步骤图"},{"platform":"知乎","publishTime":"周二 21:00","title":"35岁以后被裁员，如何半年内转型自由职业？","tags":["职业规划","自由职业","中年转型","副业","个人成长"],"imageSuggestions":"用信息图展示3个方法，配1张对比图"},{"platform":"头条号","publishTime":"次日 12:00","title":"被裁员后，我用这3个方法半年转型自由职业","tags":["职场","创业","自由职业","转型","干货"],"imageSuggestions":"封面用数字3+痛点标题，配2-3张实景图"}]}}',
                )
                return
            if "self-media-plans/current" in url:
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body='{"code":0,"message":"success","data":{"platformKey":"xiaohongshu","platformName":"小红书","nicheKey":"zhichang","nicheName":"职场转型","personaKey":"shizhan","personaName":"实战派博主","goalKey":"zhangfen","goalName":"涨粉","pillars":[{"name":"干货复盘","percent":60},{"name":"个人故事","percent":20},{"name":"热点解读","percent":20}]}}',
                )
                return
            if "generation-tasks" in url:
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body='{"code":0,"message":"success","data":{"list":[{"id":1001,"bizNo":"T202608210001","title":"35+ 被裁员后，我用这 3 个方法半年内转型自由职业","status":2,"inputParam":{"platform":"xiaohongshu"},"progressPct":100,"createdAt":"2026-08-21T10:00:00"}],"total":1,"page":1,"pageSize":20}}',
                )
                return
            if re.search(r"/articles/[^/]+$", url):
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body='{"code":0,"message":"success","data":{"bizNo":"T202608210001","title":"35+ 被裁员后，我用这 3 个方法半年内转型自由职业","body":"这是文章正文内容\\n\\n第一段介绍背景。\\n\\n第二段给出第一个方法。\\n\\n第三段总结。","wordCount":1200,"completedAt":"2026-08-21T10:00:00","platform":"xiaohongshu","skillName":"专业严谨","template":"xiaohongshu_default"}}',
                )
                return
            route.fulfill(
                status=200,
                content_type="application/json",
                body='{"code":0,"message":"success","data":null}',
            )

        context.route(re.compile(r"/api/v1/user/"), handle_route)

        page.goto(f"{USER_WEB}/console/workbench")
        page.wait_for_selector(".shortcut-card", timeout=15000)
        page.wait_for_timeout(1000)

        # Dismiss any onboarding modal that may appear
        page.evaluate("""() => {
          document.querySelectorAll('.ant-modal-wrap').forEach(w => {
            if (getComputedStyle(w).display !== 'none') w.remove();
          });
          document.querySelectorAll('.ant-modal-mask').forEach(m => m.remove());
        }""")

        # Wait for generation record to render
        page.wait_for_selector(".generation-item", timeout=15000)
        page.wait_for_selector("text=一文多发", timeout=10000)
        page.wait_for_selector("text=查看", timeout=10000)

        # Test 1: global "如何发布" button opens publish guide modal
        page.locator(".how-publish-btn").click()
        page.wait_for_selector("text=建议发布时间", timeout=10000)
        page.wait_for_selector("text=每晚 19:30-20:30", timeout=10000)
        page.wait_for_selector("text=冷启动策略", timeout=10000)
        page.wait_for_selector("text=发布后 30 分钟内", timeout=10000)
        page.wait_for_selector("text=自己点赞、收藏并评论", timeout=10000)

        # Close publish guide modal
        page.locator(".publish-modal .ant-modal-close").click()
        page.wait_for_timeout(300)

        # Test 2: per-article "一文多发" button opens reposts modal
        page.locator(".repost-btn").first.click()
        page.wait_for_selector("text=一文多发方案", timeout=10000)
        page.wait_for_selector("text=公众号", timeout=10000)
        page.wait_for_selector("text=次日 07:30", timeout=10000)
        page.wait_for_selector("text=35+被裁员后", timeout=10000)

        # Close reposts modal
        page.locator(".reposts-modal .ant-modal-close").click()
        page.wait_for_timeout(300)

        # Test 3: per-article "查看" button opens article view modal
        page.locator(".view-article-btn").first.click()
        page.wait_for_selector("text=查看文章", timeout=10000)
        page.wait_for_selector("text=35+ 被裁员后，我用这 3 个方法半年内转型自由职业", timeout=10000)
        page.wait_for_selector("text=这是文章正文内容", timeout=10000)

        assert publish_called["count"] >= 1, f"Expected at least 1 publish plan call, got {publish_called['count']}"
        assert request_body.get("articleTitle"), f"Expected articleTitle, got {request_body}"
        assert request_body.get("mainPlatform") == "小红书", f"Expected mainPlatform 小红书, got {request_body}"

        print(
            f"Test passed: publish guide/reposts/article-view rendered, publish plan called with "
            f"title={request_body['articleTitle']}, platform={request_body['mainPlatform']}"
        )

        browser.close()


if __name__ == "__main__":
    main()

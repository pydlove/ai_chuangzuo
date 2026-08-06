"""
控制台移动端 app-style 验证

覆盖：
- 移动端 (375×812)：侧边栏隐藏，header 隐藏，底部 TabBar 显示 4 个 tab（创作/活动/消息/我的）
- 创作页使用独立 MobileCreate 布局
- 活动页包含约稿中心和提示词市场入口
- 创作学院控制台页独立移动端样式，支持分类切换、文章详情与目录浮钮
- 桌面端 (1280×800) 回归：侧边栏 + header 仍可见，TabBar 隐藏
"""

from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:4173"

AUTH_SCRIPT = """
  localStorage.setItem('aichuangzuo_access_token', 'fake-token-for-testing');
  localStorage.setItem('aichuangzuo_refresh_token', 'fake-refresh');
  localStorage.setItem('aichuangzuo_membership', JSON.stringify({level:'专业版会员', expiresAt:'2026-12-31'}));
  localStorage.setItem('aichuangzuo_coin_balance', '888');
"""


def _mock_api(page):
    """Mock 所有 /api 请求，避免 fake token 触发 401 跳转登录页。

    根据常见列表/对象接口返回最小合法结构，防止前端 .map 空对象报错。
    """
    import json

    def payload_for(url):
        path = url.replace(f"{BASE_URL}/api", "")
        # 列表类接口
        if "/skills" in path or "/export-templates" in path:
            return {"code": 0, "data": [], "message": "ok"}
        # 消息/动态
        if "/messages" in path:
            return {
                "code": 0,
                "data": [
                    {
                        "id": 1,
                        "type": "announcement",
                        "title": "约稿中心上线",
                        "summary": "官方任务投稿已开放，快来参与获取创作币奖励。",
                        "content": "官方任务投稿已开放，挑选合适的任务，使用你在爱创作中生成完成的文章参与投稿。稿件采纳后，奖励全额发放。",
                        "read": False,
                        "createdAt": "2026-08-03T10:00:00",
                    },
                    {
                        "id": 2,
                        "type": "generation",
                        "title": "文章生成完成",
                        "summary": "你提交的文章《如何高效管理时间》已生成完成。",
                        "content": "",
                        "read": False,
                        "createdAt": "2026-08-03T09:30:00",
                    },
                    {
                        "id": 3,
                        "type": "membership",
                        "title": "会员权益提醒",
                        "summary": "你的专业版会员将于 7 天后到期。",
                        "content": "你的专业版会员将于 7 天后到期，请及时续费以继续享受无限次生成等权益。",
                        "read": True,
                        "createdAt": "2026-08-02T14:20:00",
                    },
                ],
                "message": "ok",
            }
        # 约稿任务详情
        if re.search(r"/commission/tasks/\d+$", path):
            return {
                "code": 0,
                "data": {
                    "task": {
                        "id": 1,
                        "taskId": 1,
                        "title": "小红书爆款笔记约稿",
                        "description": "征集小红书平台爆款笔记，要求内容真实、排版精美，适合年轻女性用户群体。\\n\\n投稿要求：\\n1. 文章内容需与任务主题相关\\n2. 字数在 300-800 字之间\\n3. 必须为原创且在爱创作平台生成",
                        "status": 0,
                        "rewardCoin": 88,
                        "minWordCount": 300,
                        "maxWordCount": 800,
                        "adoptedCount": 2,
                        "neededCount": 10,
                        "submissionCount": 24,
                        "deadlineAt": "2026-08-10T23:59:59",
                        "selectionDeadlineAt": "2026-08-12T23:59:59",
                    },
                    "mySubmission": None,
                    "submitters": [
                        {"submitterId": "u_1", "nickname": "投稿人A"},
                        {"submitterId": "u_2", "nickname": "投稿人B"},
                        {"submitterId": "u_3", "nickname": "投稿人C"},
                    ],
                    "adopters": [
                        {"submitterId": "u_4", "nickname": "中稿人X"},
                        {"submitterId": "u_5", "nickname": "中稿人Y"},
                    ],
                },
                "message": "ok",
            }
        # 约稿任务列表：前端用 total
        if "/commission/tasks" in path:
            return {
                "code": 0,
                "data": {
                    "list": [
                        {
                            "id": 1,
                            "taskId": 1,
                            "title": "小红书爆款笔记约稿",
                            "description": "征集小红书平台爆款笔记，要求内容真实、排版精美，适合年轻女性用户群体。",
                            "status": 0,
                            "rewardCoin": 88,
                            "minWordCount": 300,
                            "maxWordCount": 800,
                            "adoptedCount": 2,
                            "neededCount": 10,
                            "submissionCount": 24,
                            "deadlineAt": "2026-08-10T23:59:59",
                            "selectionDeadlineAt": "2026-08-12T23:59:59",
                        },
                        {
                            "id": 2,
                            "taskId": 2,
                            "title": "职场效率提升干货",
                            "description": "围绕职场效率、时间管理、沟通技巧等方向，输出有实操价值的干货文章。",
                            "status": 1,
                            "rewardCoin": 66,
                            "minWordCount": 500,
                            "maxWordCount": 1500,
                            "adoptedCount": 1,
                            "neededCount": 5,
                            "submissionCount": 12,
                            "deadlineAt": "2026-08-05T23:59:59",
                            "selectionDeadlineAt": "2026-08-08T23:59:59",
                        },
                    ],
                    "total": 2,
                },
                "message": "ok",
            }
        # 我的投稿
        if "/commission/submissions/mine" in path:
            return {"code": 0, "data": {"list": [], "total": 0}, "message": "ok"}
        # 市场概览
        if "/market-skills/overview" in path:
            return {
                "code": 0,
                "data": {
                    "approvedCount": 12,
                    "totalUses": 3456,
                    "totalEarnings": 6912.0,
                    "featuredSkills": [],
                },
                "message": "ok",
            }
        # 市场提示词分页列表
        if "/market-skills/paged" in path:
            return {
                "code": 0,
                "data": {
                    "list": [
                        {
                            "id": "s1",
                            "name": "小红书爆款笔记",
                            "description": "小红书风格爆款笔记提示词，帮你快速生成吸引人的标题和正文。",
                            "promptSummary": "小红书风格爆款笔记",
                            "creatorId": "u_1",
                            "creatorName": "创作达人",
                            "featured": True,
                            "scope": "小红书",
                            "weeklyUses": 60,
                            "totalUses": 300,
                            "weeklyEarnings": 120,
                            "status": "approved",
                            "createdAt": "2026-07-01T10:00:00",
                        },
                        {
                            "id": "s2",
                            "name": "职场干货",
                            "description": "职场效率与沟通技巧提示词，输出有实操价值的干货文章。",
                            "promptSummary": "职场效率与沟通技巧",
                            "creatorId": "u_2",
                            "creatorName": "职场写手",
                            "featured": False,
                            "scope": "公众号",
                            "weeklyUses": 40,
                            "totalUses": 200,
                            "weeklyEarnings": 80,
                            "status": "approved",
                            "createdAt": "2026-07-05T10:00:00",
                        },
                    ],
                    "total": 2,
                },
                "message": "ok",
            }
        # 收藏 ID 列表
        if "/market-skills/favorites" in path:
            return {"code": 0, "data": [], "message": "ok"}
        # 灵感选题
        if "/topics/random" in path:
            return {
                "code": 0,
                "data": [
                    {"id": 1, "title": "如何高效管理时间", "summary": "掌握时间管理技巧，提升工作效率"},
                    {"id": 2, "title": "小红书爆款笔记怎么写", "summary": "从选题到排版的小红书运营技巧"},
                    {"id": 3, "title": "公众号选题灵感", "summary": "找到适合公众号的内容方向"},
                    {"id": 4, "title": "短视频脚本模板", "summary": "快速产出短视频口播脚本"},
                    {"id": 5, "title": "职场沟通避坑指南", "summary": "提升职场表达与协作效率"},
                    {"id": 6, "title": "AI 写作提示词技巧", "summary": "用好提示词让 AI 更懂你"},
                ],
                "message": "ok",
            }
        # 创作学院
        if "/learn/category/tree" in path:
            return {
                "code": 0,
                "data": [
                    {"id": 1, "name": "账号定位", "total": 2, "articles": None},
                    {"id": 2, "name": "爆款方法", "total": 1, "articles": None},
                ],
                "message": "ok",
            }
        if "/learn/article/recommended" in path:
            return {
                "code": 0,
                "data": [
                    {
                        "id": 101,
                        "title": "新手如何找准账号定位",
                        "summary": "从兴趣、能力、市场三个维度找到适合自己的内容方向。",
                        "content": "账号定位是自媒体的第一步。",
                        "categoryId": 1,
                        "categoryName": "账号定位",
                        "coverImageUrl": "",
                        "publishedAt": "2026-07-20T10:00:00",
                    },
                    {
                        "id": 102,
                        "title": "爆款标题的 5 个公式",
                        "summary": "学会这 5 个公式，让标题点击率翻倍。",
                        "content": "标题决定打开率。",
                        "categoryId": 2,
                        "categoryName": "爆款方法",
                        "coverImageUrl": "",
                        "publishedAt": "2026-07-22T10:00:00",
                    },
                ],
                "message": "ok",
            }
        if re.search(r"/learn/category/\d+", path):
            return {
                "code": 0,
                "data": {
                    "id": 1,
                    "name": "账号定位",
                    "total": 2,
                    "articles": [
                        {
                            "id": 101,
                            "title": "新手如何找准账号定位",
                            "summary": "从兴趣、能力、市场三个维度找到适合自己的内容方向。",
                            "content": "账号定位是自媒体的第一步。",
                            "categoryId": 1,
                            "coverImageUrl": "",
                            "publishedAt": "2026-07-20T10:00:00",
                        },
                        {
                            "id": 103,
                            "title": "目标用户画像怎么画",
                            "summary": "用三个问题快速锁定你的核心读者。",
                            "content": "用户画像越清晰，内容越精准。",
                            "categoryId": 1,
                            "coverImageUrl": "",
                            "publishedAt": "2026-07-21T10:00:00",
                        },
                    ],
                },
                "message": "ok",
            }
        if re.search(r"/learn/article/\d+", path):
            return {
                "code": 0,
                "data": {
                    "id": 101,
                    "title": "新手如何找准账号定位",
                    "summary": "从兴趣、能力、市场三个维度找到适合自己的内容方向。",
                    "contentType": "markdown",
                    "content": """## 为什么账号定位很重要

定位决定了你后续所有的内容方向。

### 兴趣维度

选择你真正愿意长期输出的领域。

### 能力维度

评估你目前能稳定生产的内容形式。

## 总结

找到三者的交集，就是你的账号定位。""",
                    "categoryId": 1,
                    "categoryName": "账号定位",
                    "coverImageUrl": "",
                    "publishedAt": "2026-07-20T10:00:00",
                    "updatedAt": "2026-07-20T10:00:00",
                    "prevArticle": None,
                    "nextArticle": {
                        "id": 103,
                        "title": "目标用户画像怎么画",
                        "categoryName": "账号定位",
                    },
                },
                "message": "ok",
            }
        if "/learn/banner" in path:
            return {"code": 0, "data": [], "message": "ok"}
        # 其余按对象兜底
        return {"code": 0, "data": {}, "message": "ok"}

    def handle(route, request):
        if request.url.startswith(f"{BASE_URL}/api/"):
            route.fulfill(
                status=200,
                content_type="application/json",
                body=json.dumps(payload_for(request.url))
            )
        else:
            route.continue_()
    page.route("**/*", handle)


def _auth_context(browser, **kwargs):
    context = browser.new_context(**kwargs)
    context.add_init_script(AUTH_SCRIPT)
    return context


def test_console_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        OUT = "tests/e2e/screenshots"

        # 桌面端回归
        desktop = _auth_context(browser, viewport={"width": 1280, "height": 800})
        page = desktop.new_page()
        _mock_api(page)
        page.goto(f"{BASE_URL}/console/create", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(1000)

        expect(page.locator(".console-sidebar")).to_be_visible()
        expect(page.locator(".console-header")).to_be_visible()
        expect(page.locator(".console-tabbar")).not_to_be_visible()
        page.screenshot(path=f"{OUT}/console_desktop_create.png", full_page=True)
        desktop.close()

        # 移动端
        mobile = _auth_context(
            browser,
            viewport={"width": 375, "height": 812},
            bypass_csp=True,
        )
        page = mobile.new_page()
        _mock_api(page)
        page.goto(f"{BASE_URL}/console/mine", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(1000)

        expect(page.locator(".console-sidebar")).not_to_be_visible()
        expect(page.locator(".console-header")).not_to_be_visible()
        expect(page.locator(".console-tabbar")).to_be_visible()

        tab_items = page.locator(".console-tabbar-item")
        expect(tab_items).to_have_count(4)
        labels = [tab_items.nth(i).locator(".console-tabbar-label").inner_text() for i in range(4)]
        assert labels == ["创作", "活动", "消息", "我的"], f"TabBar 顺序错: {labels}"

        # 切到活动页
        tab_items.nth(1).click()
        page.wait_for_timeout(800)
        assert re.search(r".*/console/activities$", page.url), f"未跳转到活动页: {page.url}"
        expect(page.locator(".activities-page")).to_be_visible()
        expect(page.locator(".activity-card--commission")).to_be_visible()
        expect(page.locator(".activity-card--market")).to_be_visible()
        expect(page.locator(".activity-card--academy")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_activities.png", full_page=True)

        # TabBar 高亮
        activities_tab = page.locator(".console-tabbar-item").nth(1)
        expect(activities_tab).to_have_class(re.compile(r"\bactive\b"))

        # 活动页创作学院应进入控制台创作学院，而非宣传页
        page.locator(".activity-card--academy").click()
        page.wait_for_timeout(800)
        assert re.search(r"/console/learn$", page.url), f"活动页创作学院跳转错: {page.url}"
        expect(page.locator(".console-learn-page")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_activities_academy.png", full_page=True)
        page.goto(f"{BASE_URL}/console/activities", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(600)

        # 从活动页进入提示词市场
        page.locator(".activity-card--market").click()
        page.wait_for_timeout(800)
        assert re.search(r"/console/skill-market$", page.url), f"未跳转到提示词市场: {page.url}"
        expect(page.locator(".market-page")).to_be_visible()
        expect(page.locator(".market-banner")).to_be_visible()
        expect(page.locator(".market-upload-card")).to_be_visible()
        expect(page.locator(".market-grid-section")).to_be_visible()
        expect(page.locator(".skill-card")).to_have_count(2)
        page.screenshot(path=f"{OUT}/console_mobile_skill_market.png", full_page=True)

        # 从活动页进入约稿中心
        page.goto(f"{BASE_URL}/console/activities", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(600)
        page.locator(".activity-card--commission").click()
        page.wait_for_timeout(800)
        assert re.search(r"/console/commission$", page.url), f"未跳转到约稿中心: {page.url}"
        expect(page.locator(".commission-page")).to_be_visible()
        expect(page.locator(".commission-stats")).to_be_visible()
        expect(page.locator(".commission-rules")).to_be_visible()
        expect(page.locator(".commission-tabs")).to_be_visible()
        expect(page.locator(".task-card")).to_have_count(2)
        page.screenshot(path=f"{OUT}/console_mobile_commission.png", full_page=True)

        # 点击任务卡片进入约稿详情
        page.locator(".task-card").first.click()
        page.wait_for_timeout(800)
        assert re.search(r"/console/commission/\d+$", page.url), f"未跳转到约稿详情: {page.url}"
        expect(page.locator(".detail-page")).to_be_visible()
        expect(page.locator(".content-panel")).to_be_visible()
        expect(page.locator(".action-panel")).to_be_visible()
        expect(page.locator(".reward-card")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_commission_detail.png", full_page=True)

        # 约稿中心是子页面，TabBar 隐藏，直接跳转到消息页
        page.goto(f"{BASE_URL}/console/messages", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(800)
        assert re.search(r"/console/messages$", page.url), f"未跳转到消息页: {page.url}"
        expect(page.locator(".messages-page")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_messages.png", full_page=True)

        # 切到我的页
        page.locator(".console-tabbar-item").nth(3).click()
        page.wait_for_timeout(600)
        assert re.search(r"/console/mine$", page.url), f"未跳转到我的页: {page.url}"
        expect(page.locator(".mine-page")).to_be_visible()
        expect(page.locator(".mine-user-card")).to_be_visible()
        expect(page.locator(".mine-user-name")).to_contain_text("爱创作用户")
        expect(page.locator(".mine-stats-card")).to_be_visible()
        expect(page.locator(".mine-stat-value")).to_have_count(3)
        expect(page.locator(".mine-grid")).to_be_visible()
        expect(page.locator(".mine-grid-item")).to_have_count(8)
        expect(page.locator(".mine-grid-item").first.locator(".mine-grid-label")).to_contain_text("我的作品")
        grid_labels = [page.locator(".mine-grid-label").nth(i).inner_text() for i in range(8)]
        assert grid_labels == ["我的作品", "我的账户", "我的权益", "邀请有礼", "我的提示词", "提示词市场", "热搜榜", "兑换码"], f"功能顺序错: {grid_labels}"
        expect(page.locator(".mine-block")).to_have_count(4)
        expect(page.locator(".mine-logout")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_mine.png", full_page=True)

        # 创作学院（控制台独立页面）
        page.goto(f"{BASE_URL}/console/learn", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(800)
        assert re.search(r"/console/learn$", page.url), f"未跳转到创作学院: {page.url}"
        expect(page.locator(".console-learn-page")).to_be_visible()
        expect(page.locator(".category-tabs-bar")).to_be_visible()
        expect(page.locator(".article-card").first).to_be_visible()
        expect(page.locator(".article-list .article-card")).to_have_count(2)
        page.screenshot(path=f"{OUT}/console_mobile_learn.png", full_page=True)

        # 切换分类查看分类详情
        tabs = page.locator(".category-tab")
        expect(tabs).to_have_count(3)  # 全部 + 2 个分类
        tabs.nth(1).click()
        page.wait_for_timeout(800)
        assert re.search(r"/console/learn\?cat=", page.url), f"未切换到分类: {page.url}"
        expect(page.locator(".category-detail-head")).to_be_visible()
        expect(page.locator(".article-card")).to_have_count(2)
        page.screenshot(path=f"{OUT}/console_mobile_learn_category.png", full_page=True)

        # 进入文章详情
        page.locator(".article-card").first.click()
        page.wait_for_timeout(800)
        assert re.search(r"/console/learn/article/\d+$", page.url), f"未跳转到文章详情: {page.url}"
        expect(page.locator(".learn-content")).to_be_visible()
        expect(page.locator(".mobile-toc-btn")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_learn_article.png", full_page=True)

        # 打开移动端目录抽屉
        page.locator(".mobile-toc-btn").click()
        page.wait_for_timeout(600)
        expect(page.locator(".mobile-toc-item")).to_have_count(4)
        page.screenshot(path=f"{OUT}/console_mobile_learn_toc.png", full_page=True)
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)

        # 文章详情是子页面，TabBar 隐藏，直接跳转到创作页
        page.goto(f"{BASE_URL}/console/create", wait_until="domcontentloaded", timeout=20000)
        page.wait_for_load_state("networkidle", timeout=10000)
        page.wait_for_timeout(600)
        assert re.search(r"/console/create$", page.url), f"未跳转到创作页: {page.url}"
        expect(page.locator(".mobile-create")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_create.png", full_page=True)

        # 点击一个快速开始灵感胶囊
        capsules = page.locator(".topic-capsule--mobile")
        expect(capsules).to_have_count(3)
        first_capsule = capsules.first
        expect(first_capsule).to_be_visible(timeout=5000)
        first_capsule.click()
        page.wait_for_timeout(400)
        expect(page.locator(".mc-title-input")).to_have_value("如何高效管理时间")
        page.screenshot(path=f"{OUT}/console_mobile_topic_clicked.png", full_page=True)

        # 打开字数设置弹框并截图
        page.locator(".mc-quick-item").nth(1).click()
        page.wait_for_timeout(600)
        expect(page.locator(".word-count-modal")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_wordcount.png", full_page=True)
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)

        # 打开提示词弹框并截图
        page.locator(".mc-quick-item").nth(2).click()
        page.wait_for_timeout(600)
        expect(page.locator(".style-modal")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_skill.png", full_page=True)
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)

        # 打开模板弹框并截图
        page.locator(".mc-quick-item").nth(3).click()
        page.wait_for_timeout(600)
        expect(page.locator(".template-modal")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_template.png", full_page=True)
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)

        # 打开扩写全屏编辑器
        page.locator(".mc-expand-btn").click()
        page.wait_for_timeout(500)
        expect(page.locator(".mc-fullscreen")).to_be_visible()
        page.screenshot(path=f"{OUT}/console_mobile_create_fullscreen.png", full_page=True)
        page.locator(".mc-fullscreen__save").click()
        page.wait_for_timeout(300)

        # 无横向溢出
        body_w = page.evaluate("document.documentElement.scrollWidth")
        assert body_w <= 377, f"横向溢出: body={body_w}"

        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_console_mobile()
    print("Console mobile verification passed.")

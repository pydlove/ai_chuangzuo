#!/usr/bin/env python3
"""验证自媒体账号功能：我的页入口 + 新页面移动端/PC 端渲染 + 弹框。"""
import sys
from playwright.sync_api import sync_playwright

BASE = "http://localhost:22345"
SHOT_DIR = "tests/e2e/screenshots"


def mock_api(page):
    def handler(route):
        pathname = route.request.url.split("?", 1)[0].replace("http://localhost:22345", "")
        if not pathname.startswith("/api/"):
            route.continue_()
            return
        route.fulfill(
            status=200,
            content_type="application/json",
            body='{"code":0,"data":[]}',
        )
    page.route("**/*", handler)


def login(page):
    page.goto(f"{BASE}/login", wait_until="networkidle")
    page.evaluate(
        "localStorage.setItem('aichuangzuo_access_token','e2e-fake-token');"
        "localStorage.setItem('aichuangzuo_refresh_token','e2e-fake-refresh');"
        "localStorage.setItem('aichuangzuo_user_id','e2e-user');"
        "localStorage.setItem('aichuangzuo_membership','{}');"
    )


def enter_console(page, path):
    page.goto(f"{BASE}{path}", wait_until="networkidle")
    page.locator("#app-loader").wait_for(state="detached", timeout=10000)
    page.wait_for_timeout(1200)


def main():
    errors = []
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ---- 移动端 ----
        ctx = browser.new_context(viewport={"width": 390, "height": 844}, is_mobile=True,
                                  has_touch=True, device_scale_factor=2)
        page = ctx.new_page()
        page.on("pageerror", lambda e: errors.append(f"mobile pageerror: {e}"))
        mock_api(page)
        login(page)

        enter_console(page, "/console/mine")
        item = page.locator(".mine-hot-service-item", has_text="自媒体账号")
        if item.count() == 0:
            errors.append("mine page: 自媒体账号入口不存在")
        else:
            img = item.locator("img")
            src = img.get_attribute("src") if img.count() else ""
            from urllib.parse import unquote
            if "自媒体账号icon-v1.png" not in unquote(src or ""):
                errors.append(f"mine page: icon 不对 -> {src}")
        if page.locator(".mine-hot-service-item", has_text="账号检测").count() > 0:
            errors.append("mine page: 账号检测入口仍然存在")
        page.screenshot(path=f"{SHOT_DIR}/sma-mine-mobile.png", full_page=True)

        # 进入二级页
        if item.count():
            item.first.click()
            page.wait_for_timeout(1200)
            if "/console/selfmedia-accounts" not in page.url:
                errors.append(f"跳转失败: {page.url}")
            page.screenshot(path=f"{SHOT_DIR}/sma-page-mobile-empty.png", full_page=True)

            # 新增账号弹框
            page.locator(".sma-mobile-add-btn").click()
            page.wait_for_timeout(600)
            page.screenshot(path=f"{SHOT_DIR}/sma-form-mobile.png", full_page=True)
            page.locator(".sma-form__control .ant-select-selector").click()
            page.wait_for_timeout(400)
            page.locator(".ant-select-item-option", has_text="小红书").click()
            page.locator("input[placeholder='例如：爱创作的小爱']").fill("爱创作的小爱")
            page.locator("input[placeholder*='选填，例如抖音号']").fill("aichuangzuo2026")
            page.locator("input[placeholder='选填，账号主页链接']").fill("https://xhs.example.com/user/123")
            page.locator(".ant-modal-footer button", has_text="保").first.click()
            page.wait_for_timeout(800)
            page.screenshot(path=f"{SHOT_DIR}/sma-page-mobile-list.png", full_page=True)
            card = page.locator(".sma-card")
            if card.count() == 0:
                errors.append("新增后移动端卡片列表为空")
            elif "小红书" not in card.first.inner_text():
                errors.append("卡片未显示平台名")
        ctx.close()

        # ---- PC 端（同一浏览器上下文需重新设置 token，但 localStorage 按 origin 共享：新 context 不共享）----
        ctx2 = browser.new_context(viewport={"width": 1440, "height": 900}, device_scale_factor=1)
        page2 = ctx2.new_page()
        page2.on("pageerror", lambda e: errors.append(f"pc pageerror: {e}"))
        mock_api(page2)
        login(page2)
        enter_console(page2, "/console/selfmedia-accounts")
        page2.screenshot(path=f"{SHOT_DIR}/sma-page-pc.png", full_page=True)
        if page2.locator(".sma-page__action").count() == 0:
            errors.append("PC 端缺少新增按钮")
        # PC 端独立新增一条（localStorage 不跨 context 共享）
        page2.locator(".sma-page__action").click()
        page2.wait_for_timeout(500)
        page2.locator(".sma-form__control .ant-select-selector").click()
        page2.wait_for_timeout(400)
        page2.locator(".ant-select-item-option", has_text="微信公众号").click()
        page2.locator("input[placeholder='例如：爱创作的小爱']").fill("小爱公众号")
        page2.locator(".ant-modal-footer button", has_text="保").first.click()
        page2.wait_for_timeout(800)
        page2.screenshot(path=f"{SHOT_DIR}/sma-page-pc-list.png", full_page=True)
        rows = page2.locator(".sma-table tbody tr.ant-table-row")
        if rows.count() == 0:
            errors.append("PC 端表格没有数据行")
        else:
            text = rows.first.inner_text()
            if "微信公众号" not in text or "小爱公众号" not in text:
                errors.append(f"PC 表格行内容不对: {text[:120]}")
        sidebar = page2.locator(".console-sidebar-item", has_text="自媒体账号")
        if sidebar.count() == 0:
            errors.append("PC 侧边栏缺少自媒体账号菜单")
        else:
            # 展开「我的」分组后应可见
            if not sidebar.first.is_visible():
                group = page2.locator(".console-sidebar-group-title")
                if group.count():
                    group.first.click()
                    page2.wait_for_timeout(400)
                if not sidebar.first.is_visible():
                    errors.append("PC 侧边栏自媒体账号菜单不可见（我的分组未展开？）")
        ctx2.close()
        browser.close()

    if errors:
        print("FAIL")
        for e in errors:
            print(" -", e)
        sys.exit(1)
    print("PASS")


if __name__ == "__main__":
    main()

"""
约稿中心快速验证脚本:
- 直接打开 /console/commission 列表页
- 打开详情页、发布页
- 切换演示账号看不同视角
- 截图保存到 tests/e2e/screenshots/commission_*.png
"""
import asyncio
import os
import sys
from pathlib import Path

from playwright.async_api import async_playwright

BASE = "http://127.0.0.1:5193"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


async def shot(page, name):
    out = SCREENSHOT_DIR / f"commission_{name}.png"
    await page.screenshot(path=str(out), full_page=False)
    print(f"  saved {out.name}")


async def wait_list(page):
    """等待约稿列表页真正挂载,并等开屏 loader(#app-loader,最短展示 1.2s 后淡出移除)消失"""
    await page.wait_for_selector(".commission-title", timeout=15000)
    await page.wait_for_selector("#app-loader", state="detached", timeout=5000)
    await page.wait_for_timeout(200)


async def mock_api(ctx):
    """后端未启动:拦截所有用户端接口,返回空的成功包,避免 401 拦截器把 token 清掉跳登录。
    约稿功能本身走 localStorage,不依赖这些接口。"""
    async def obj_handler(route):
        await route.fulfill(
            status=200,
            content_type="application/json",
            body='{"code":0,"message":"ok","data":{"records":[],"list":[],"total":0,"friends":[]}}',
        )

    async def list_handler(route):
        await route.fulfill(
            status=200,
            content_type="application/json",
            body='{"code":0,"message":"ok","data":[]}',
        )

    await ctx.route("**/api/v1/user/**", obj_handler)
    # 消息列表期望 data 是数组(res.data || []),单独返回数组;后注册的优先匹配。
    await ctx.route("**/api/v1/user/messages", list_handler)


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        ctx = await browser.new_context(viewport={"width": 1440, "height": 900})
        await mock_api(ctx)
        # 提前注入 token 跳过登录
        await ctx.add_init_script("""
            localStorage.setItem('aichuangzuo_access_token', 'demo-token');
        """)
        page = await ctx.new_page()

        # 捕获 console 错误
        errors = []
        page.on("pageerror", lambda e: errors.append(str(e)))
        page.on("console", lambda msg: msg.type == "error" and errors.append(msg.text))

        print("→ 列表页(默认柠檬不酸)")
        await page.goto(f"{BASE}/console/commission", wait_until="domcontentloaded")
        await wait_list(page)
        await shot(page, "01_list_user1")

        print("→ 切到墨鱼写作(任务 1 的发布者)")
        await page.evaluate("""
            localStorage.setItem('aichuangzuo_commission_current_user', 'demo-user-2');
        """)
        await page.reload(wait_until="domcontentloaded")
        await wait_list(page)
        await shot(page, "02_list_user2_publisher")

        print("→ 我发布的 Tab")
        await page.click("text=我发布的")
        await page.wait_for_timeout(400)
        await shot(page, "03_my_published")

        print("→ 点开第一个任务(征集小红书爆款)")
        await page.click("text=征集 5 篇小红书爆款养生选题")
        await page.wait_for_url("**/console/commission/**", timeout=5000)
        await page.wait_for_timeout(800)
        await shot(page, "04_detail_publisher_view")

        print("→ 切回柠檬不酸(投稿人视角),打开我的投递任务")
        await page.evaluate("""
            localStorage.setItem('aichuangzuo_commission_current_user', 'demo-user-1');
        """)
        await page.goto(f"{BASE}/console/commission", wait_until="domcontentloaded")
        await wait_list(page)
        await page.click("text=我投稿的")
        await page.wait_for_timeout(400)
        await shot(page, "05_my_submitted")
        await page.click("text=招募知乎带货短文")
        await page.wait_for_timeout(800)
        await shot(page, "06_detail_submitter_view")

        print("→ 发布页")
        await page.goto(f"{BASE}/console/commission/publish", wait_until="domcontentloaded")
        await page.wait_for_selector(".publish-title", timeout=15000)
        await page.wait_for_selector("#app-loader", state="detached", timeout=5000)
        await page.wait_for_timeout(300)
        await shot(page, "07_publish_form")

        print("→ 移动端")
        await ctx.close()
        ctx2 = await browser.new_context(viewport={"width": 375, "height": 800})
        await mock_api(ctx2)
        await ctx2.add_init_script("""
            localStorage.setItem('aichuangzuo_access_token', 'demo-token');
        """)
        page2 = await ctx2.new_page()
        await page2.goto(f"{BASE}/console/commission", wait_until="domcontentloaded")
        await wait_list(page2)
        await shot(page2, "08_mobile_list")
        await page2.goto(f"{BASE}/console/commission/publish", wait_until="domcontentloaded")
        await page2.wait_for_selector(".publish-title", timeout=15000)
        await page2.wait_for_selector("#app-loader", state="detached", timeout=5000)
        await page2.wait_for_timeout(300)
        await shot(page2, "09_mobile_publish")

        await browser.close()

        if errors:
            print("\n❌ Page errors:")
            for e in errors:
                print(f"  - {e}")
            sys.exit(1)
        print("\n✅ 所有页面无 JS 错误")


if __name__ == "__main__":
    asyncio.run(main())
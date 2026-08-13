import asyncio
from playwright.async_api import async_playwright

ADMIN_URL = "http://localhost:22347"
ACCESS_TOKEN = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwianRpIjoiMjU4MGI4NGMtYWQ3NC00OTI4LThiM2EtNWM2MTFiNDhiZTk4IiwiaWF0IjoxNzg2NDU3NjczLCJleHAiOjE3ODY0NjQ4NzN9.8_k2Cz2WnkTigoV6-VttXvwjD03qbMeTWrI_ewFRfqyLuewVC-6HX9cdH4vob6sV"
USER_INFO = '{"id":1,"username":"admin","realName":"超级管理员","avatarUrl":null}'

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(viewport={"width": 1440, "height": 900})

        # 直接注入登录态，绕过滑块
        await page.goto(f"{ADMIN_URL}/login")
        await page.evaluate(f"""
            localStorage.setItem('admin_access_token', JSON.stringify('{ACCESS_TOKEN}'));
        """)

        # 访问定时任务页面
        await page.goto(f"{ADMIN_URL}/console/scheduled-tasks")
        await page.wait_for_timeout(2500)
        print("定时任务页面URL:", page.url)

        # 截图
        await page.screenshot(path="/Users/panyong/aio_project/ai_chuangzuo/tests/e2e/screenshots/scheduled_task_view.png", full_page=True)
        print("页面截图已保存")

        # 验证页面上有任务列表
        await page.wait_for_selector('text=每日热搜抓取', timeout=10000)
        await page.wait_for_selector('text=待生效会员激活', timeout=10000)
        await page.wait_for_selector('text=系统设置', timeout=10000)
        print("页面包含管理端和用户端任务")

        await browser.close()

if __name__ == "__main__":
    asyncio.run(main())

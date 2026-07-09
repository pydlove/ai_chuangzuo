"""验证:
  1. /pricing 页面三个套餐卡 + 对比表都有"队列最多 N 个任务"
  2. 免费用户 → 提交任务被 message.warning 拦截
  3. 基础版 → 只能有 1 个 generating 任务,第 2 个被拦截
  4. 专业版 → 最多 5 个
  5. 旗舰版 → 最多 10 个
  6. 过期会员 → 降级为 free,被拦截
"""
import asyncio
from playwright.async_api import async_playwright

USER_WEB = "http://localhost:22345"


async def set_membership(page, level=None, expired=False):
    """level: None 清空,'基础版'/'专业版'/'旗舰版';expired 把 expiresAt 设为过去"""
    if level is None:
        await page.evaluate("() => localStorage.removeItem('aichuangzuo_membership')")
        return
    import datetime
    if expired:
        iso = (datetime.datetime.now() - datetime.timedelta(days=1)).isoformat() + "Z"
    else:
        iso = (datetime.datetime.now() + datetime.timedelta(days=30)).isoformat() + "Z"
    await page.evaluate(
        f"() => localStorage.setItem('aichuangzuo_membership', JSON.stringify({{ level: '{level}', expiresAt: '{iso}' }}))"
    )


async def clear_queue(page):
    await page.evaluate("() => localStorage.removeItem('aichuangzuo_generation_queue')")


async def fill_form_and_submit(page, title):
    """填表 + 点生成文章。返回 warning 文本(若没有则 None)"""
    # 标题:input.hero-title-input
    title_input = page.locator('input.hero-title-input')
    await title_input.fill(title)
    # 补充要求:textarea.hero-textarea
    textarea = page.locator('textarea.hero-textarea')
    await textarea.fill('这是测试补充要求,验证队列上限')
    # 点生成文章
    btn = page.locator('button.hero-generate-btn')
    await btn.click()
    await page.wait_for_timeout(400)
    warn_text = None
    warn = page.locator('.ant-message-warning')
    if await warn.count() > 0:
        warn_text = (await warn.first.inner_text()).strip()
        await page.wait_for_timeout(2200)  # 等 message 自动消失
    return warn_text


async def count_generating(page):
    return await page.evaluate("""() => {
        const raw = localStorage.getItem('aichuangzuo_generation_queue');
        if (!raw) return 0;
        try {
            const list = JSON.parse(raw);
            return list.filter(x => x.status === 'generating').length;
        } catch { return 0; }
    }""")


async def inject_n_generating(page, n):
    """直接往 localStorage 注入 n 个 generating 任务,模拟队列已满"""
    import json, time
    items = []
    now = int(time.time() * 1000)
    for i in range(n):
        items.append({
            "id": now - i,
            "title": f"占位任务 {i+1}",
            "platform": "未选择",
            "wordCount": 1500,
            "style": "未选择",
            "template": "未选择",
            "status": "generating",
            "progress": 30,
            "createdAt": "2026-07-09T00:00:00.000Z",
            "completedAt": None
        })
    await page.evaluate(
        f"() => localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify({json.dumps(items)}))"
    )


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        ctx = await browser.new_context(viewport={"width": 1440, "height": 900})
        page = await ctx.new_page()

        # ============ 1. /pricing 页面展示 ============
        await page.goto(f"{USER_WEB}/pricing", wait_until="networkidle")
        await page.wait_for_timeout(500)
        await page.screenshot(path="/tmp/pricing_with_queue.png", full_page=True)

        # 三个套餐卡的 features 文本
        cards = await page.locator('.plan-card, [class*="plan-"]').count()
        # 退而求其次: 用 features 容器类
        body_text = await page.locator('body').inner_text()
        assert '队列最多 1 个任务' in body_text, "缺 基础版 1 个任务"
        assert '队列最多 5 个任务' in body_text, "缺 专业版 5 个任务"
        assert '队列最多 10 个任务' in body_text, "缺 旗舰版 10 个任务"
        assert '队列任务数' in body_text, "缺 对比表行"
        print("[1] /pricing: 三档 + 对比表都显示 ✓")

        # ============ 2. 免费用户被拦截 ============
        await page.goto(f"{USER_WEB}/login", wait_until="networkidle")
        await set_membership(page, level=None)
        await page.goto(f"{USER_WEB}/console/create", wait_until="networkidle")
        await page.wait_for_timeout(800)
        await clear_queue(page)
        await page.reload()
        await page.wait_for_timeout(500)

        warn = await fill_form_and_submit(page, "免费用户测试标题")
        assert warn and '免费用户' in warn, f"免费用户应被拦截,实际 warn={warn!r}"
        print(f"[2] 免费用户: {warn[:50]}... ✓")

        # ============ 3. 基础版 → 第 1 个成功,第 2 个被拦截 ============
        await set_membership(page, level='基础版')
        await page.reload()
        await page.wait_for_timeout(500)
        await clear_queue(page)

        warn = await fill_form_and_submit(page, "基础版任务 1")
        # 没有 warning 就是成功加进去了
        if warn and '队列已满' in warn:
            # 也可能: 我们先清空了队列,这里不应该被拦
            raise AssertionError(f"基础版第 1 个应成功,实际被拦: {warn!r}")
        await page.wait_for_timeout(300)
        n = await count_generating(page)
        assert n == 1, f"基础版第 1 个后应 1 个 generating,实际 {n}"
        print(f"[3.1] 基础版第 1 个任务: 成功入队 (n={n}) ✓")

        # 第 2 个: 注入 1 个 generating(因为我们刚才的已经 finished by mock? 不,它在 setInterval)
        # 简单点:用注入的方式确保有 1 个
        await inject_n_generating(page, 1)
        await page.wait_for_timeout(200)
        n = await count_generating(page)
        assert n == 1, f"注入 1 个后应 = 1,实际 {n}"
        # 但 localStorage 注入的不会被 setInterval 推进,所以再点会成功加一个变成 2
        # 所以我换成: 让真实任务把队列塞到 1 个 generating,然后立刻尝试加第 2 个
        # 重新清空 + 只用注入方式
        await clear_queue(page)
        await inject_n_generating(page, 1)  # 1 个 generating(等于上限)
        await page.reload()
        await page.wait_for_timeout(800)

        warn = await fill_form_and_submit(page, "基础版任务 2(应被拦)")
        assert warn and ('队列已满' in warn or '基础版' in warn), f"基础版第 2 个应被拦,实际 warn={warn!r}"
        print(f"[3.2] 基础版第 2 个: {warn[:60]}... ✓")

        # ============ 4. 专业版 → 5 个允许,第 6 个被拦 ============
        await set_membership(page, level='专业版')
        await clear_queue(page)
        await inject_n_generating(page, 5)
        await page.reload()
        await page.wait_for_timeout(800)

        n = await count_generating(page)
        assert n == 5, f"专业版注入 5 个后应 = 5,实际 {n}"
        warn = await fill_form_and_submit(page, "专业版任务 6(应被拦)")
        assert warn and '队列已满' in warn, f"专业版第 6 个应被拦,实际 warn={warn!r}"
        print(f"[4] 专业版第 6 个: {warn[:60]}... ✓")

        # ============ 5. 旗舰版 → 10 个允许,第 11 个被拦 ============
        await set_membership(page, level='旗舰版')
        await clear_queue(page)
        await inject_n_generating(page, 10)
        await page.reload()
        await page.wait_for_timeout(800)

        n = await count_generating(page)
        assert n == 10, f"旗舰版注入 10 个后应 = 10,实际 {n}"
        warn = await fill_form_and_submit(page, "旗舰版任务 11(应被拦)")
        assert warn and '队列已满' in warn, f"旗舰版第 11 个应被拦,实际 warn={warn!r}"
        print(f"[5] 旗舰版第 11 个: {warn[:60]}... ✓")

        # ============ 6. 过期会员 → 降级为 free,被拦 ============
        await set_membership(page, level='旗舰版', expired=True)
        await clear_queue(page)
        await page.reload()
        await page.wait_for_timeout(800)

        warn = await fill_form_and_submit(page, "过期会员测试(应被拦)")
        assert warn and '免费用户' in warn, f"过期应降级为 free 并被拦,实际 warn={warn!r}"
        print(f"[6] 过期会员: {warn[:50]}... ✓")

        await page.screenshot(path="/tmp/queue_block_free.png", full_page=True)

        await browser.close()
        print("\nALL PASS")


asyncio.run(main())

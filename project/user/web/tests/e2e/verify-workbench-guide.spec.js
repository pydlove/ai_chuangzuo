import { test, expect } from '@playwright/test'

const BASE_URL = process.env.GUIDE_BASE_URL || 'http://localhost:4173'

function mockApi(route) {
  return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: {} }) })
}

async function setupPage(page, { mobile = false } = {}) {
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      return mockApi(route)
    }
    route.continue()
  })

  await page.setViewportSize(mobile ? { width: 390, height: 844 } : { width: 1440, height: 900 })
  await page.goto(BASE_URL + '/console/workbench')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    // 避免运营方案弹窗抢占，单独验证向导
    localStorage.setItem('aichuangzuo_selfmedia_plan_modal_dismissed', '1')
    localStorage.removeItem('aichuangzuo_workbench_guide_done')
  })
  await page.reload()
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
}

test.describe('workbench guide (desktop)', () => {
  test('auto pops on first visit, steps advance, and closes persistently', async ({ page }) => {
    await setupPage(page)

    // 首次进入自动弹出向导
    await expect(page.locator('.driver-popover')).toBeVisible({ timeout: 10000 })
    await expect(page.locator('.driver-popover-title').first()).toHaveText('工作台')
    await page.screenshot({ path: 'tests/e2e/screenshots/guide-desktop-step1.png' })

    // 依次走完所有步骤
    for (let i = 0; i < 12; i++) {
      const next = page.locator('.driver-popover-next-btn')
      if (!(await next.isVisible().catch(() => false))) break
      const label = await next.textContent()
      if (label === '完成') break
      await next.click()
      await page.waitForTimeout(300)
    }
    await page.screenshot({ path: 'tests/e2e/screenshots/guide-desktop-mid.png' })

    // 点关闭 → 记录已看
    await page.locator('.driver-popover-close-btn').click()
    await expect(page.locator('.driver-popover')).toBeHidden()
    const done = await page.evaluate(() => localStorage.getItem('aichuangzuo_workbench_guide_done'))
    expect(done).toBe('1')

    // 刷新不再自动弹
    await page.reload()
    await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
    await page.waitForTimeout(800)
    await expect(page.locator('.driver-popover')).toBeHidden()

    // 手动入口可重看
    await page.locator('.guide-entry-btn').click()
    await expect(page.locator('.driver-popover')).toBeVisible()
    await page.screenshot({ path: 'tests/e2e/screenshots/guide-desktop-reopen.png' })
  })
})

test.describe('workbench guide (mobile)', () => {
  test('mobile steps highlight create / feature bar / tabbar', async ({ page }) => {
    await setupPage(page, { mobile: true })

    await expect(page.locator('.driver-popover')).toBeVisible({ timeout: 10000 })
    await expect(page.locator('.driver-popover-title').first()).toHaveText('开始今日创作')
    await page.screenshot({ path: 'tests/e2e/screenshots/guide-mobile-step1.png' })

    // 走到第二步：功能栏
    await page.locator('.driver-popover-next-btn').click()
    await page.waitForTimeout(300)
    await expect(page.locator('.driver-popover-title')).toHaveText('约稿中心 / 提示词市场')
    await page.screenshot({ path: 'tests/e2e/screenshots/guide-mobile-step2.png' })

    // 最后一步是底部导航
    for (let i = 0; i < 5; i++) {
      const next = page.locator('.driver-popover-next-btn')
      const label = await next.textContent().catch(() => '完成')
      if (label === '完成') break
      await next.click()
      await page.waitForTimeout(300)
    }
    await expect(page.locator('.driver-popover-title')).toHaveText('底部导航')
    await page.screenshot({ path: 'tests/e2e/screenshots/guide-mobile-last.png' })
  })
})

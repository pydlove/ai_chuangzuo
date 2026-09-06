import { test, expect } from '@playwright/test'

const BASE_URL = process.env.GUIDE_BASE_URL || 'http://localhost:4173'

function mockApi(route) {
  return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: {} }) })
}

test('guide uses theme red color', async ({ page }) => {
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      return mockApi(route)
    }
    route.continue()
  })
  await page.setViewportSize({ width: 1440, height: 900 })
  await page.goto(BASE_URL + '/console/workbench')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_selfmedia_plan_modal_dismissed', '1')
    localStorage.removeItem('aichuangzuo_workbench_guide_done')
  })
  await page.reload()
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })

  await expect(page.locator('.driver-popover')).toBeVisible({ timeout: 10000 })
  // 前进两步到「提示词市场」
  await page.locator('.driver-popover-next-btn').click()
  await page.waitForTimeout(200)
  await page.locator('.driver-popover-next-btn').click()
  await page.waitForTimeout(400)
  await expect(page.locator('.driver-popover-title')).toHaveText('提示词市场')

  // 标题和主按钮应为主题红 #FF2442
  const titleColor = await page.locator('.driver-popover-title').evaluate((el) => getComputedStyle(el).color)
  const nextBg = await page.locator('.driver-popover-next-btn').evaluate((el) => getComputedStyle(el).backgroundColor)
  expect(titleColor).toBe('rgb(255, 36, 66)')
  expect(nextBg).toBe('rgb(255, 36, 66)')

  await page.screenshot({ path: 'tests/e2e/screenshots/guide-theme-red.png' })
})

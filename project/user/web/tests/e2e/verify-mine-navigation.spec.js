import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:4173'

function mockApi(route) {
  return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: {} }) })
}

test.beforeEach(async ({ page }) => {
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      return mockApi(route)
    }
    route.continue()
  })

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(BASE_URL + '/login')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
  })
})

test('mine page stat cards navigate to correct pages', async ({ page }) => {
  await page.goto(BASE_URL + '/console/mine')
  // 等待应用加载层消失，MineIndex 的 stat 卡片才能被点击
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(300)

  // 本月已生成 → 我的作品
  await page.locator('.mine-stat-item', { hasText: '本月已生成' }).click()
  await expect(page).toHaveURL(/\/console\/works/)

  await page.goto(BASE_URL + '/console/mine')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(300)

  // 创作币余额 → 我的账户
  await page.locator('.mine-stat-item', { hasText: '创作币余额' }).click()
  await expect(page).toHaveURL(/\/console\/earnings/)

  await page.goto(BASE_URL + '/console/mine')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(300)

  // 已邀请 → 邀请有礼
  await page.locator('.mine-stat-item', { hasText: '已邀请' }).click()
  await expect(page).toHaveURL(/\/console\/invite/)
})

test('mine page membership badge navigates to benefits', async ({ page }) => {
  await page.goto(BASE_URL + '/console/mine')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(300)

  await page.locator('.mine-user-vip').click()
  await expect(page).toHaveURL(/\/console\/benefits/)
})

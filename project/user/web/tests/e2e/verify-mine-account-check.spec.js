import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:4173'

function mockApi(route, body = { code: 0, data: {} }) {
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(body)
  })
}

test.beforeEach(async ({ page }) => {
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (!url.pathname.startsWith('/api/')) {
      return route.continue()
    }
    if (url.pathname.includes('/self-media-plans/current')) {
      return mockApi(route, {
        code: 0,
        data: {
          platformKey: 'xiaohongshu',
          platformName: '小红书',
          nicheName: '35+ 职场转型',
          personaName: '实战记录者',
          pillars: [
            { name: '干货复盘', percent: 60 },
            { name: '个人故事', percent: 20 },
            { name: '热点解读', percent: 20 }
          ]
        }
      })
    }
    if (url.pathname.includes('/self-media/nickname/check')) {
      return mockApi(route, {
        code: 0,
        data: { fit: true, reason: '昵称与定位契合', suggestions: [] }
      })
    }
    if (url.pathname.includes('/self-media/nickname/recommend')) {
      return mockApi(route, {
        code: 0,
        data: {
          options: [{ nickname: '职场转型小助手', bio: '分享真实职场转型经验' }]
        }
      })
    }
    return mockApi(route)
  })

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(BASE_URL + '/login')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_refresh_token', 'mock-refresh-token')
  })
})

test('mine page replaces prompt market with account check entry', async ({ page }) => {
  await page.goto(BASE_URL + '/console/mine')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })

  await expect(page.locator('.mine-grid-label', { hasText: '账号检测' })).toBeVisible()
  await expect(page.locator('.mine-grid-label', { hasText: '提示词市场' })).toHaveCount(0)
})

test('account check modal opens and fits mobile screen', async ({ page }) => {
  await page.goto(BASE_URL + '/console/mine')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })

  await page.locator('.mine-grid-label', { hasText: '账号检测' }).click()

  const modal = page.locator('.account-check-modal')
  await expect(modal).toBeVisible()
  await expect(modal.locator('.ant-modal-title')).toHaveText('平台账号检测')
  await expect(modal.getByPlaceholder('输入你的账号昵称')).toBeVisible()
  await expect(modal.getByRole('button', { name: '检测名称' })).toBeVisible()
})

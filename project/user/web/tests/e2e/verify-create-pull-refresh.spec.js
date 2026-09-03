import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:5173'

function mockResponse(body) {
  return { status: 200, contentType: 'application/json', body: JSON.stringify(body) }
}

function emptyResponse() {
  return mockResponse({ code: 0, data: {} })
}

const mockBenefits = {
  code: 0,
  data: {
    planKey: 'pro',
    planName: '专业版',
    expiresAt: '2026-12-31',
    benefits: [
      { code: 'ai_article_quota', name: 'AI 文章生成', type: 'quota', value: '100', used: 32, remaining: 68 }
    ]
  }
}

const mockMembership = {
  code: 0,
  data: { hasMembership: true, level: 'pro', levelName: '专业版', cycle: 'month', expiresAt: '2026-12-31' }
}

test.beforeEach(async ({ page }) => {
  await page.route('http://localhost:5173/api/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/user/benefits/me') return route.fulfill(mockResponse(mockBenefits))
    if (url.pathname === '/api/v1/user/membership/me') return route.fulfill(mockResponse(mockMembership))
    return route.fulfill(emptyResponse())
  })
  await page.setViewportSize({ width: 390, height: 844 })
})

test('mobile free create page chips allow vertical touch through', async ({ page }) => {
  page.on('console', (msg) => console.log('[console]', msg.type(), msg.text()))
  page.on('pageerror', (err) => console.log('[pageerror]', err.message))

  await page.goto(BASE_URL + '/')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_refresh_token', 'mock-refresh-token')
  })
  await page.goto(BASE_URL + '/console/create/free')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(800)

  const chips = page.locator('.free-create-chips')
  await expect(chips).toBeVisible()
  const touchAction = await chips.evaluate((el) => window.getComputedStyle(el).touchAction)
  expect(touchAction).toBe('pan-x')

  await page.screenshot({ path: 'tests/e2e/screenshots/mobile_free_create_chips.png', fullPage: false })
})

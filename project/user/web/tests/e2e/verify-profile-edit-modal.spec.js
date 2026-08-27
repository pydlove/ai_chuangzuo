import { test } from '@playwright/test'

const BASE_URL = 'http://localhost:22345'

function mockResponse(body) {
  return { status: 200, contentType: 'application/json', body: JSON.stringify(body) }
}

function emptyResponse() {
  return mockResponse({ code: 0, data: {} })
}

const mockProfile = {
  code: 0,
  data: {
    userId: 'U001234',
    nickname: '测试用户',
    avatarUrl: '',
    bio: '',
    gender: 0,
    birthday: '',
    location: '',
    occupation: ''
  }
}

test.beforeEach(async ({ page }) => {
  await page.route('http://localhost:22345/api/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/user/me') return route.fulfill(mockResponse(mockProfile))
    return route.fulfill(emptyResponse())
  })
  await page.setViewportSize({ width: 1280, height: 800 })
})

test('open profile edit modal screenshot', async ({ page }) => {
  page.on('console', (msg) => console.log('[console]', msg.type(), msg.text()))
  page.on('pageerror', (err) => console.log('[pageerror]', err.message))

  await page.goto(BASE_URL + '/')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_refresh_token', 'mock-refresh-token')
  })
  await page.setViewportSize({ width: 1280, height: 1200 })
  await page.goto(BASE_URL + '/console/create')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(800)

  // 点击右上角用户头像打开用户中心
  await page.locator('.console-avatar').first().click()
  await page.waitForTimeout(300)
  // 点击编辑资料
  await page.locator('.user-action').filter({ hasText: '编辑资料' }).click()
  await page.waitForTimeout(800)

  // 点击生日选择器展开日期面板
  await page.locator('.profile-edit-birthday-picker input').click()
  await page.waitForTimeout(500)

  await page.screenshot({ path: 'tests/e2e/screenshots/profile_edit_modal.png', fullPage: false })
})

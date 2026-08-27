import { test } from '@playwright/test'

const BASE_URL = 'http://localhost:22345'

function mockResponse(body) {
  return { status: 200, contentType: 'application/json', body: JSON.stringify(body) }
}

function emptyResponse() {
  return mockResponse({ code: 0, data: {} })
}

test.beforeEach(async ({ page }) => {
  await page.route('http://localhost:22345/api/**', async (route) => {
    const url = new URL(route.request().url())
    console.log('[mock api]', url.pathname)
    return route.fulfill(emptyResponse())
  })
  await page.setViewportSize({ width: 390, height: 844 })
})

test('mobile works search box screenshot', async ({ page }) => {
  page.on('console', (msg) => console.log('[console]', msg.type(), msg.text()))
  page.on('pageerror', (err) => console.log('[pageerror]', err.message))

  await page.goto(BASE_URL + '/')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_refresh_token', 'mock-refresh-token')
  })
  await page.goto(BASE_URL + '/console/works')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'tests/e2e/screenshots/mobile_works_search.png', fullPage: false })
})

import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:4173'

function mockApi(route, url) {
  if (url.includes('/api/v1/user/messages')) {
    if (url.match(/\/messages\/\d+\/read/)) {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: null }) })
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          {
            id: 1,
            type: 'membership',
            title: '您的会员即将到期',
            summary: '您的会员将于 3 天后到期，请及时续费',
            content: '您的会员将于 3 天后到期，请及时续费\n\n点击「我的会员」查看详情并续费。',
            link: '/me/membership',
            read: false,
            createdAt: '2026-08-10T08:00:00Z'
          }
        ]
      })
    })
  }
  return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: {} }) })
}

async function setupPage(page, viewport = { width: 390, height: 844 }) {
  await page.setViewportSize(viewport)
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      return mockApi(route, route.request().url())
    }
    route.continue()
  })

  await page.goto(BASE_URL + '/login')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
  })
}

test('message center redirects /me/membership to /console/benefits', async ({ page }) => {
  await setupPage(page)
  await page.goto(BASE_URL + '/console/messages')
  await page.waitForTimeout(600)

  await page.locator('.message-card').first().click()
  await page.waitForTimeout(300)

  await expect(page).toHaveURL(BASE_URL + '/console/benefits')
})

test('header bell dropdown redirects /me/membership to /console/benefits', async ({ page }) => {
  await setupPage(page, { width: 1280, height: 800 })
  await page.goto(BASE_URL + '/console/create')
  await page.waitForTimeout(600)

  await page.locator('.bell-btn').click()
  await page.waitForTimeout(300)

  await page.locator('.notif-item').first().click()
  await page.waitForTimeout(300)

  await expect(page).toHaveURL(BASE_URL + '/console/benefits')
})

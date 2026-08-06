import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:4173'

function mockApi(route, url) {
  if (url.includes('/api/v1/user/messages')) {
    // 全部已读
    if (url.includes('/read-all')) {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: null }) })
    }
    // 单条已读：/messages/{id}/read
    if (url.match(/\/messages\/\d+\/read/)) {
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: null }) })
    }
    // 消息列表
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          {
            id: 1,
            type: 'announcement',
            title: '系统公告一',
            summary: '这是第一条未读公告',
            content: '公告详情一',
            read: false,
            createdAt: '2026-08-05T08:00:00Z'
          },
          {
            id: 2,
            type: 'announcement',
            title: '系统公告二',
            summary: '这是第二条未读公告',
            content: '公告详情二',
            read: false,
            createdAt: '2026-08-05T09:00:00Z'
          }
        ]
      })
    })
  }
  return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: {} }) })
}

test.beforeEach(async ({ page }) => {
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      return mockApi(route, route.request().url())
    }
    route.continue()
  })

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(BASE_URL + '/login')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
  })
})

test('mobile message tabbar badge decreases after reading a message', async ({ page }) => {
  await page.goto(BASE_URL + '/console/messages')
  await page.waitForTimeout(600)

  const badge = page.locator('.console-tabbar-item[href="/console/messages"] .console-tabbar-badge')
  await expect(badge).toHaveText('2')

  // 点击第一条消息，打开详情弹框并标记已读
  await page.locator('.message-card').first().click()
  await page.waitForTimeout(300)

  await expect(badge).toHaveText('1')
  await page.screenshot({ path: 'tests/e2e/screenshots/console_mobile_message_badge_after_read.png' })
})

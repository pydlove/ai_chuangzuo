import { test } from '@playwright/test'

const empty = { list: [], total: 0 }
const emptyObj = {}

const mockOrders = {
  code: 0,
  data: {
    list: [
      {
        id: 1,
        orderNo: 'SUB260901783301',
        status: 1,
        statusName: '已支付',
        planKey: 'flagship',
        planName: '旗舰版',
        cycle: 'monthly',
        cycleName: '月卡',
        amount: 0.01,
        coinAmount: 0,
        couponDiscount: 0,
        thirdPartyTradeId: 'XHP2026090116260001',
        createdAt: '2026-09-01T16:26:00',
        paidAt: '2026-09-01T16:26:30'
      }
    ],
    total: 1,
    page: 1,
    pageSize: 100
  }
}

const mockMembership = {
  planKey: 'pro',
  planName: '专业版',
  expiresAt: '2026-12-31',
  status: 1,
  aiArticleQuota: 100,
  aiArticleUsed: 10,
  exportWord: true,
  copyText: true,
  aiTitleOptimize: true,
  onlineEdit: true
}

const mockNewcomerOffer = {
  eligible: false,
  discount: 0
}

const mockSkills = {
  list: [],
  total: 0
}

const mockUser = {
  id: 1,
  nickname: '测试用户',
  avatar: '',
  phone: '13800138000'
}

const mockBenefits = {
  benefits: []
}

const mockMessages = {
  list: [],
  total: 0,
  unreadCount: 0
}

const mockExportTemplates = {
  list: [],
  total: 0
}

test('mobile order page layout screenshot', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
  })

  await page.route('**/api/v1/user/**', async (route) => {
    const url = route.request().url()
    if (url.includes('/orders')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockOrders) })
    } else if (url.includes('/membership/me')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockMembership) })
    } else if (url.includes('/plans/newcomer-offer')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockNewcomerOffer) })
    } else if (url.includes('/skills/system-skills')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockSkills) })
    } else if (url.includes('/user/me')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockUser) })
    } else if (url.includes('/benefits/me')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockBenefits) })
    } else if (url.includes('/messages/unread-count')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ unreadCount: 0 }) })
    } else if (url.includes('/messages')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockMessages) })
    } else if (url.includes('/export-templates')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockExportTemplates) })
    } else if (url.includes('/articles/monthly-count')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ count: 0 }) })
    } else if (url.includes('/account/invite-stats')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(emptyObj) })
    } else if (url.includes('/account/summary')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(emptyObj) })
    } else if (url.includes('/generation-tasks')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(empty) })
    } else if (url.includes('/account/withdrawals')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(empty) })
    } else if (url.includes('/self-media-plans/current')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(emptyObj) })
    } else {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(emptyObj) })
    }
  })

  page.on('console', (msg) => {
    console.log('CONSOLE:', msg.type(), msg.text())
  })
  page.on('pageerror', (err) => {
    console.log('PAGE ERROR:', err.message)
  })

  await page.goto('/console/orders')
  await page.setViewportSize({ width: 375, height: 812 })
  await page.waitForTimeout(2000)

  await page.screenshot({ path: 'tests/e2e/screenshots/mobile-order-layout.png', fullPage: false })

  // Click 全部 tab
  const allTab = page.locator('.tabs__tab').filter({ hasText: '全部' })
  await allTab.click()
  await page.waitForTimeout(1000)
  await page.screenshot({ path: 'tests/e2e/screenshots/mobile-order-layout-all.png', fullPage: false })

  // Open order detail modal
  await page.locator('.order-card').first().click()
  await page.waitForTimeout(1000)
  await page.screenshot({ path: 'tests/e2e/screenshots/mobile-order-detail.png', fullPage: false })

  const hasCard = await page.locator('.order-card').count()
  console.log('ORDER CARD COUNT:', hasCard)
})
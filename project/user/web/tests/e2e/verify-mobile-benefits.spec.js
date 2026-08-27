import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:22345'

const mockBenefits = {
  code: 0,
  data: {
    planKey: 'pro',
    planName: '专业版',
    expiresAt: '2026-12-31',
    benefits: [
      { code: 'ai_article_quota', name: 'AI 文章生成', type: 'quota', value: '100', used: 32, remaining: 68 },
      { code: 'plan_adjust_quota', name: '计划调整', type: 'quota', value: '50', used: 5, remaining: 45 },
      { code: 'export_word', name: '导出 Word', type: 'boolean', value: 'true' },
      { code: 'copy_text', name: '复制正文', type: 'boolean', value: 'true' },
      { code: 'ai_title_optimize', name: '标题优化', type: 'boolean', value: 'true' },
      { code: 'online_edit', name: '在线编辑', type: 'boolean', value: 'true' }
    ]
  }
}

const mockPlans = {
  code: 0,
  data: {
    plans: [
      {
        key: 'basic',
        name: '基础版',
        recommended: false,
        monthly: { original: 59, current: 39, articles: '每月 30 篇', savings: null },
        quarter: { original: 177, current: 99, articles: '每季 90 篇', savings: 78 },
        year: { original: 708, current: 299, articles: '每年 365 篇', savings: 409 }
      },
      {
        key: 'pro',
        name: '专业版',
        recommended: true,
        monthly: { original: 99, current: 69, articles: '每月 100 篇', savings: null },
        quarter: { original: 297, current: 189, articles: '每季 300 篇', savings: 108 },
        year: { original: 1188, current: 599, articles: '每年 1200 篇', savings: 589 }
      },
      {
        key: 'flagship',
        name: '旗舰版',
        recommended: false,
        monthly: { original: 199, current: 129, articles: '每月 不限量', savings: null },
        quarter: { original: 597, current: 349, articles: '每季 不限量', savings: 248 },
        year: { original: 2388, current: 999, articles: '每年 不限量', savings: 1389 }
      }
    ],
    compareRows: [
      { code: 'ai_article_quota', label: 'AI 文章生成', basic: { value: '30篇/月' }, pro: { value: '100篇/月' }, flagship: { value: '不限量' } },
      { code: 'plan_adjust_quota', label: '计划调整', basic: { value: '10次/月' }, pro: { value: '50次/月' }, flagship: { value: '不限量' } },
      { code: 'export_word', label: '导出 Word', basic: { value: true }, pro: { value: true }, flagship: { value: true } },
      { code: 'copy_text', label: '复制正文', basic: { value: true }, pro: { value: true }, flagship: { value: true } },
      { code: 'ai_topic', label: 'AI 选题', basic: { value: false }, pro: { value: true }, flagship: { value: true } },
      { code: 'ai_title_optimize', label: '标题优化', basic: { value: false }, pro: { value: true }, flagship: { value: true } },
      { code: 'online_edit', label: '在线编辑', basic: { value: false }, pro: { value: true }, flagship: { value: true } },
      { code: 'skill_custom', label: '我的提示词', basic: { value: '3个' }, pro: { value: '10个' }, flagship: { value: '不限量' } },
      { code: 'skill_learn_analyze', label: '爆文学习', basic: { value: false }, pro: { value: '10次/月' }, flagship: { value: '不限量' } },
      { code: 'sticker_quota', label: '生成卡片', basic: { value: '10张/月' }, pro: { value: '50张/月' }, flagship: { value: '不限量' } },
      { code: 'history_days', label: '历史记录', basic: { value: '30天' }, pro: { value: '90天' }, flagship: { value: '永久' } },
      { code: 'queue_priority', label: '队列优先', basic: { value: false }, pro: { value: true }, flagship: { value: true } }
    ]
  }
}

const mockMembership = {
  code: 0,
  data: {
    hasMembership: true,
    level: 'pro',
    levelName: '专业版',
    cycle: 'month',
    expiresAt: '2026-12-31'
  }
}

const mockNewcomer = {
  code: 0,
  data: {
    eligible: false
  }
}

const mockInvite = {
  code: 0,
  data: {
    inviteCode: 'MOCK1234',
    invitedCount: 0,
    inviteCoinEarned: 0,
    coinEarned: 0,
    coinBalance: 100,
    friends: []
  }
}

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
    if (url.pathname === '/api/v1/user/benefits/me') return route.fulfill(mockResponse(mockBenefits))
    if (url.pathname === '/api/v1/user/plans') return route.fulfill(mockResponse(mockPlans))
    if (url.pathname === '/api/v1/user/membership/me') return route.fulfill(mockResponse(mockMembership))
    if (url.pathname === '/api/v1/user/plans/newcomer-offer') return route.fulfill(mockResponse(mockNewcomer))
    if (url.pathname === '/api/v1/user/account/invite-stats') return route.fulfill(mockResponse(mockInvite))
    // 其它 API 统一空返回，避免 401/403 触发登出
    return route.fulfill(emptyResponse())
  })

  await page.setViewportSize({ width: 390, height: 844 })
})

test('mobile benefits page renders VIP style', async ({ page }) => {
  page.on('console', (msg) => console.log('[console]', msg.type(), msg.text()))
  page.on('pageerror', (err) => console.log('[pageerror]', err.message))

  await page.goto(BASE_URL + '/')
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_refresh_token', 'mock-refresh-token')
  })
  await page.goto(BASE_URL + '/console/benefits')
  await page.locator('#app-loader').waitFor({ state: 'detached', timeout: 10000 })
  await page.waitForTimeout(800)

  console.log('current url:', page.url())
  console.log('token:', await page.evaluate(() => localStorage.getItem('aichuangzuo_access_token')))

  // 等待核心元素渲染
  await expect(page.locator('.mb-vip-card')).toBeVisible()
  await expect(page.locator('.mb-quick-benefits')).toBeVisible()
  await expect(page.locator('.mb-compare')).toBeVisible()
  await expect(page.locator('.mb-footer-action')).toBeVisible()

  await page.screenshot({ path: 'tests/e2e/screenshots/mobile_benefits_vip.png', fullPage: false })
})

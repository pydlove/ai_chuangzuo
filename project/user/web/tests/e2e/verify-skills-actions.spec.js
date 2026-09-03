import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:22345'

function mockApi(route, url) {
  const pathname = new URL(url).pathname
  if (pathname === '/api/v1/user/skills' || pathname.endsWith('/skills')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          {
            bizNo: 'skill-001',
            name: '测试已上架',
            desc: '已上架的测试提示词',
            prompt: '这是一个已上架的测试提示词内容',
            scope: '小红书,抖音',
            count: 5,
            marketStatus: 'published',
            status: 'approved'
          },
          {
            bizNo: 'skill-002',
            name: '测试未发布',
            desc: '未发布的测试提示词',
            prompt: '这是一个未发布的测试提示词内容',
            scope: '公众号',
            count: 3,
            marketStatus: null,
            status: 'approved'
          },
          {
            bizNo: 'skill-003',
            name: '测试已打回',
            desc: '已打回的测试提示词',
            prompt: '这是一个已打回的测试提示词内容',
            scope: '知乎',
            count: 1,
            marketStatus: 'rejected',
            status: 'approved'
          }
        ]
      })
    })
  }
  if (pathname.includes('/skills/system-skills')) {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) })
  }
  if (pathname.includes('/skills/learned')) {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) })
  }
  if (pathname.includes('/skills/favorite')) {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) })
  }
  if (pathname.includes('/market-skills/my-submissions')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          { id: 'skill-001', status: 'approved' },
          { id: 'skill-003', status: 'rejected' }
        ]
      })
    })
  }
  if (pathname.includes('/benefits/me')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          planKey: 'pro',
          planName: '专业版',
          benefits: [
            { code: 'skill_custom', value: '100', remaining: 100 },
            { code: 'skill_learn_analyze', value: '10', remaining: 10 },
            { code: 'skill_market_publish', value: '5', remaining: 5 }
          ]
        }
      })
    })
  }
  return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: {} }) })
}

test('desktop my skills cards hide use button and show unpublish', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_user_id', 'mock-user-id')
  })
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      return mockApi(route, route.request().url())
    }
    route.continue()
  })
  await page.setViewportSize({ width: 1280, height: 800 })
  await page.goto(BASE_URL + '/console/skills')
  await page.waitForTimeout(1500)
  await page.screenshot({ path: '/tmp/verify-skills-desktop-debug.png', fullPage: true })

  // 已上架卡片不应有“使用”按钮，应有“下架”
  const publishedCard = page.locator('.styles-grid .skill-card').filter({ hasText: '这是一个已上架的测试提示词内容' })
  await expect(publishedCard).toContainText('下架')
  await expect(publishedCard).not.toContainText('使用')

  // 未发布卡片不应有“使用”按钮
  const unpublishedCard = page.locator('.styles-grid .skill-card').filter({ hasText: '这是一个未发布的测试提示词内容' })
  await expect(unpublishedCard).not.toContainText('使用')

  // 点击卡片查看，详情弹窗内也不应有“使用”按钮
  await publishedCard.getByText('查看').click()
  await page.waitForTimeout(300)
  const detailModal = page.locator('.skill-detail-modal')
  await expect(detailModal).toBeVisible()
  await expect(detailModal.locator('button:has-text("使用")')).toHaveCount(0)
  await page.screenshot({ path: '/tmp/verify-skills-detail.png' })

  await page.screenshot({ path: '/tmp/verify-skills-desktop.png', fullPage: true })
})

test('mobile my skills cards expose unpublish action', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_user_id', 'mock-user-id')
  })
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      return mockApi(route, route.request().url())
    }
    route.continue()
  })
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(BASE_URL + '/console/skills')
  await page.waitForTimeout(1500)
  await page.screenshot({ path: '/tmp/verify-skills-mobile-debug.png', fullPage: true })

  // 已上架卡片在手机端应显示“下架”
  const publishedCard = page.locator('.styles-grid .skill-card').filter({ hasText: '这是一个已上架的测试提示词内容' })
  await expect(publishedCard).toContainText('下架')

  await page.screenshot({ path: '/tmp/verify-skills-mobile.png', fullPage: true })
})

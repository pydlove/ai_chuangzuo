import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:4173'

function mockApi(route, url) {
  if (url.includes('/api/v1/user/learn/category/tree')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          { id: 1, name: '内容定位', children: [] },
          { id: 2, name: '平台运营', children: [] },
          { id: 3, name: '爆款方法', children: [] }
        ]
      })
    })
  }
  if (url.includes('/api/v1/user/learn/article/recommended')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          {
            id: 101,
            title: '如何找到账号定位',
            summary: '从兴趣、能力和市场需求三个维度，找到适合你的自媒体内容定位。',
            categoryName: '内容定位',
            content: 'a'.repeat(1200),
            publishedAt: '2026-08-01T00:00:00Z'
          },
          {
            id: 102,
            title: '小红书标题的 10 个公式',
            summary: '总结小红书爆款标题的常用结构，帮你快速写出高点击标题。',
            categoryName: '爆款方法',
            content: 'b'.repeat(900),
            publishedAt: '2026-08-02T00:00:00Z'
          },
          {
            id: 103,
            title: '抖音算法推荐机制解析',
            summary: '理解抖音推荐逻辑，让你的内容更容易进入更大的流量池。',
            categoryName: '平台运营',
            content: 'c'.repeat(1500),
            publishedAt: '2026-08-03T00:00:00Z'
          }
        ]
      })
    })
  }
  if (url.includes('/api/v1/user/learn/category/1')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          id: 1,
          name: '内容定位',
          total: 2,
          articles: [
            { id: 101, title: '如何找到账号定位', summary: '从兴趣、能力和市场需求三个维度...', content: 'x'.repeat(1200), publishedAt: '2026-08-01T00:00:00Z' },
            { id: 104, title: '账号人设打造的 5 个步骤', summary: '让观众记住你的关键方法。', content: 'y'.repeat(800), publishedAt: '2026-08-02T00:00:00Z' }
          ]
        }
      })
    })
  }
  if (url.includes('/api/v1/user/learn/article/101')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          id: 101,
          title: '如何找到账号定位',
          summary: '从兴趣、能力和市场需求三个维度，找到适合你的自媒体内容定位。',
          categoryId: 1,
          categoryName: '内容定位',
          contentType: 'markdown',
          content: '# 如何找到账号定位\n\n找到账号定位是自媒体创作的第一步。\n\n## 兴趣驱动\n\n选择你真正感兴趣的方向。\n\n## 能力匹配\n\n评估你能持续输出的内容。\n\n## 市场需求\n\n验证这个方向是否有受众。',
          publishedAt: '2026-08-01T00:00:00Z',
          updatedAt: '2026-08-01T00:00:00Z'
        }
      })
    })
  }
  if (url.includes('/api/v1/user/skills/system-skills')) {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) })
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

  await page.goto(BASE_URL + '/console/learn')
  await page.waitForTimeout(500)
  await page.evaluate(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
  })
})

test('console learn home renders recommended articles', async ({ page }) => {
  await page.goto(BASE_URL + '/console/learn')
  await expect(page.locator('.console-learn-title')).toContainText('创作学院')
  await expect(page.locator('.category-tab.active')).toContainText('全部')
  await expect(page.locator('.article-card')).toHaveCount(3)
  await page.screenshot({ path: 'tests/e2e/screenshots/console-learn-home.png', fullPage: true })
})

test('console learn category detail renders articles', async ({ page }) => {
  await page.goto(BASE_URL + '/console/learn?cat=1')
  await expect(page.locator('.category-detail-title')).toContainText('内容定位')
  await expect(page.locator('.article-card')).toHaveCount(2)
  await page.screenshot({ path: 'tests/e2e/screenshots/console-learn-category.png', fullPage: true })
})

test('console learn article detail renders content', async ({ page }) => {
  await page.goto(BASE_URL + '/console/learn/article/101')
  await expect(page.locator('.learn-content-title')).toContainText('如何找到账号定位')
  await page.screenshot({ path: 'tests/e2e/screenshots/console-learn-article.png', fullPage: true })
})

test('console learn mobile view', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(BASE_URL + '/console/learn')
  await expect(page.locator('.article-card')).toHaveCount(3)
  await page.screenshot({ path: 'tests/e2e/screenshots/console-learn-mobile.png', fullPage: true })
})

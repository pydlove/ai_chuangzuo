import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:4173'

function mockApi(route, url) {
  const pathname = new URL(url).pathname
  if (pathname.includes('/skills/analyze')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          prompt: '请模仿以下风格撰写自媒体文章：语气亲切自然，多用 emoji，开头用疑问句吸引注意，正文分点说明，结尾引导互动。',
          description: '小红书种草笔记风格',
          excerpt1: '姐妹们，这款真的绝了！',
          excerpt2: '用了两周，皮肤明显变好了～'
        }
      })
    })
  }
  if (pathname === '/api/v1/user/skills' || pathname.endsWith('/skills')) {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) })
  }
  if (pathname.includes('/skills/system-skills')) {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) })
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

test.beforeEach(async ({ page }) => {
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
  await page.goto(BASE_URL + '/console/skills')
  await page.waitForTimeout(800)
})

test('learn skill result shows preview and edit button', async ({ page }) => {
  await page.getByText('学习的提示词', { exact: true }).click()
  await page.waitForTimeout(300)
  await page.getByText('学习新提示词', { exact: true }).click()
  await page.fill('.learned-textarea', 'a'.repeat(250))
  await page.click('.learned-pane .learned-submit-btn')
  await page.waitForTimeout(1200)

  await expect(page.locator('.learned-prompt-preview')).toBeVisible()
  await expect(page.locator('.learned-prompt-edit-btn')).toBeVisible()
  await page.screenshot({ path: 'tests/e2e/screenshots/learn-skill-preview.png', fullPage: true })
})

test('save validation shows message and scrolls to error', async ({ page }) => {
  await page.getByText('学习的提示词', { exact: true }).click()
  await page.waitForTimeout(300)
  await page.getByText('学习新提示词', { exact: true }).click()
  await page.fill('.learned-textarea', 'b'.repeat(250))
  await page.click('.learned-pane .learned-submit-btn')
  await page.waitForTimeout(1200)

  await page.fill('#learned-name-field .learned-input', '测试提示词')
  await page.click('.learned-result-actions .learned-submit-btn:has-text("保存到提示词")')
  await page.waitForTimeout(1200)

  const scopeError = page.locator('#learned-scope-field .learned-error:has-text("请填写适用范围")')
  await expect(scopeError).toBeVisible()
  await page.screenshot({ path: 'tests/e2e/screenshots/learn-skill-validation.png', fullPage: true })
})

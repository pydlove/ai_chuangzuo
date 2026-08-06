import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:5180'

const mySkills = [
  { bizNo: 'my-1', skillName: '我的测试提示词', description: '我的', prompt: 'prompt-my', scope: '小红书', useCount: 0, auditStatus: null, sourceType: 1 }
]

const learnedSkills = [
  { bizNo: 'learned-1', skillName: '学习的测试提示词', description: '学习的', prompt: 'prompt-learned', scope: '小红书', useCount: 0, createdAt: '2026-08-01T00:00:00Z', auditStatus: null, sourceType: 2 }
]

const systemSkills = [
  { bizNo: 'sys-1', name: '系统测试提示词', description: '系统的', promptSummary: 'prompt-sys-summary', prompt: 'prompt-sys', scope: '通用' }
]

let updateCallCount = 0

function mockApi(route, url) {
  const pathname = new URL(url).pathname
  const searchParams = new URL(url).searchParams
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
    const sourceType = searchParams.get('sourceType') || '1'
    const data = sourceType === '2' ? learnedSkills : mySkills
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data }) })
  }
  if (pathname.includes('/skills/system-skills')) {
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: systemSkills }) })
  }
  if (pathname.match(/\/skills\/[^/]+$/u) && route.request().method() === 'PUT') {
    updateCallCount++
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: {} }) })
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
  updateCallCount = 0
  page.on('console', msg => console.log('[BROWSER]', msg.type(), msg.text()))
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

test('edit learned skill with duplicate my skill name', async ({ page }) => {
  await page.getByText('学习的提示词', { exact: true }).click()
  await page.waitForTimeout(500)

  await page.locator('.skill-card', { hasText: '学习的测试提示词' }).locator('.skill-card__action-btn:has-text("编辑")').click()
  await page.waitForTimeout(500)

  await page.fill('#learned-name-field .learned-input', '我的测试提示词')
  await page.locator('#learned-scope-field .style-scope-tag-input').fill('小红书')
  await page.keyboard.press('Enter')
  await page.waitForTimeout(200)

  await page.click('.learned-result-actions .learned-submit-btn:has-text("保存到提示词")')
  await page.waitForTimeout(1000)

  console.log('Update API call count:', updateCallCount)
  expect(updateCallCount).toBe(0)
  await expect(page.locator('#learned-name-field .learned-error:has-text("该提示词名称已存在")')).toBeVisible()
})

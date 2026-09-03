import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:22345'

async function setupAuthAndMocks(page) {
  await page.addInitScript(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-token')
    localStorage.setItem('aichuangzuo_user_id', 'mock-user-id')
  })
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      const pathname = url.pathname
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
              ...Array.from({ length: 25 }, (_, i) => ({
                bizNo: `skill-filler-${i}`,
                name: `填充提示词 ${i}`,
                desc: '填充描述',
                prompt: `填充内容 ${i} `.repeat(10),
                scope: '小红书',
                count: i,
                marketStatus: null,
                status: 'approved'
              }))
            ]
          })
        })
      }
      if (pathname.includes('/skills/system-skills') || pathname.includes('/skills/learned') || pathname.includes('/skills/favorite')) {
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) })
      }
      if (pathname.includes('/market-skills/my-submissions')) {
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [{ id: 'skill-001', status: 'approved' }] }) })
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
    route.continue()
  })
}

async function canScroll(page, selector) {
  const el = await page.locator(selector).first()
  const scrollHeight = await el.evaluate(e => e.scrollHeight)
  const clientHeight = await el.evaluate(e => e.clientHeight)
  if (scrollHeight <= clientHeight) return { canScroll: false, reason: 'no overflow' }
  await el.evaluate(e => { e.scrollTop = 100 })
  const scrollTop = await el.evaluate(e => e.scrollTop)
  return { canScroll: scrollTop > 0, scrollTop, scrollHeight, clientHeight }
}

test('mobile console skills page scrolling', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await setupAuthAndMocks(page)
  await page.goto(BASE_URL + '/console/skills')
  await page.waitForTimeout(1500)
  await page.screenshot({ path: '/tmp/scroll-skills-top.png', fullPage: false })

  // .styles-index 是实际的滚动容器
  const stylesIndexResult = await canScroll(page, '.styles-index')
  expect(stylesIndexResult.canScroll, `.styles-index should scroll: ${JSON.stringify(stylesIndexResult)}`).toBe(true)

  // body 不应该滚动，避免触发浏览器下拉刷新
  await page.evaluate(() => { window.scrollTo(0, 200) })
  const scrollY = await page.evaluate(() => window.scrollY)
  expect(scrollY).toBe(0)

  await page.screenshot({ path: '/tmp/scroll-skills-bottom.png', fullPage: false })
})

test('mobile home page scrolling', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await setupAuthAndMocks(page)
  await page.goto(BASE_URL + '/')
  await page.waitForTimeout(1500)
  await page.screenshot({ path: '/tmp/scroll-home-top.png', fullPage: false })

  await page.evaluate(() => { window.scrollTo(0, 200) })
  const scrollY = await page.evaluate(() => window.scrollY)
  console.log('window scrollY:', scrollY)
  await page.screenshot({ path: '/tmp/scroll-home-bottom.png', fullPage: false })
})

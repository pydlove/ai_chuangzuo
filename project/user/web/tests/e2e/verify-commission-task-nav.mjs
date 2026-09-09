// 临时验证脚本：约稿详情页 上一个/下一个任务 导航
// 运行：node tests/e2e/verify-commission-task-nav.mjs
import { chromium } from 'playwright'

const BASE_URL = 'http://localhost:22345'

// 约稿大厅列表：创建时间倒序（最新在前）
const tasks = [
  { id: 4, title: '最新任务：AI 工具盘点', status: 0, rewardCoin: 500, minWordCount: 800, maxWordCount: 1500, adoptedCount: 0, neededCount: 3, submissionCount: 2, deadlineAt: '2026-09-20T12:00:00', createdAt: '2026-09-08T01:00:00' },
  { id: 3, title: '中秋家宴文案征集', status: 0, rewardCoin: 300, minWordCount: 500, maxWordCount: 1000, adoptedCount: 1, neededCount: 2, submissionCount: 5, deadlineAt: '2026-09-18T12:00:00', createdAt: '2026-09-07T01:00:00' },
  { id: 2, title: '职场效率提升干货', status: 1, rewardCoin: 200, minWordCount: 1000, maxWordCount: 2000, adoptedCount: 0, neededCount: 1, submissionCount: 4, deadlineAt: '2026-09-10T12:00:00', selectionDeadlineAt: '2026-09-15T12:00:00', createdAt: '2026-09-06T01:00:00' },
  { id: 1, title: '最早任务：新手起号指南', status: 2, rewardCoin: 100, minWordCount: null, maxWordCount: null, adoptedCount: 2, neededCount: 2, submissionCount: 8, deadlineAt: '2026-09-01T12:00:00', createdAt: '2026-09-05T01:00:00' }
]

function detailOf(id) {
  const t = tasks.find(x => x.id === Number(id))
  return {
    task: { ...t, description: `【任务${id}】这是任务说明文字，用于验证上一个/下一个任务导航。` },
    mySubmission: null,
    submitters: [],
    adopters: [],
    submissionCount: t.submissionCount
  }
}

async function setupPage(page, mobile) {
  await page.route('**/api/v1/**', (route) => {
    const url = new URL(route.request().url())
    const ok = (data) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data }) })
    const m = url.pathname.match(/^\/api\/v1\/user\/commission\/tasks(?:\/(\d+))?$/)
    if (m) {
      if (m[1]) return ok(detailOf(m[1]))
      return ok({ records: tasks, total: tasks.length })
    }
    if (url.pathname === '/api/v1/user/commission/submissions/mine') return ok({ records: [], total: 0 })
    return ok({})
  })
  await page.setViewportSize(mobile ? { width: 390, height: 844 } : { width: 1440, height: 900 })
  await page.addInitScript(() => {
    localStorage.setItem('aichuangzuo_access_token', 'mock-header.eyJleHAiOiA5OTk5OTk5OTk5fQ.mock-sig')
    localStorage.setItem('aichuangzuo_access_token_expires_at', String(Date.now() + 86400000))
    localStorage.setItem('aichuangzuo_selfmedia_plan_modal_dismissed', '1')
  })
}

function assert(cond, msg) {
  if (!cond) throw new Error('断言失败: ' + msg)
  console.log('  ✓', msg)
}

const browser = await chromium.launch()

// ---------- 桌面端 ----------
{
  console.log('[桌面端] /console/commission/2')
  const page = await browser.newPage()
  await setupPage(page, false)
  await page.goto(`${BASE_URL}/console/commission/2`)
  await page.waitForSelector('.task-nav', { timeout: 10000 })

  const nav = page.locator('.task-nav')
  assert(await nav.isVisible(), '任务导航区块可见')

  const prev = nav.locator('.task-nav-btn').first()
  const next = nav.locator('.task-nav-btn--next')
  assert((await prev.locator('.task-nav-label').textContent()) === '上一个任务', '左侧标签为「上一个任务」')
  assert((await prev.locator('.task-nav-title').textContent()).includes('中秋家宴文案征集'), '上一个任务显示更新任务的名称')
  assert((await next.locator('.task-nav-label').textContent()) === '下一个任务', '右侧标签为「下一个任务」')
  assert((await next.locator('.task-nav-title').textContent()).includes('最早任务：新手起号指南'), '下一个任务显示更早任务的名称')

  // 点击「下一个任务」→ 跳到任务 1
  await next.click()
  await page.waitForURL('**/console/commission/1', { timeout: 5000 })
  await page.waitForFunction(() => document.querySelector('.content-head h1')?.textContent?.includes('最早任务'), null, { timeout: 5000 })
  assert(true, '点击「下一个任务」跳转到任务 1 并刷新详情')

  // 任务 1 是最早的：下一个禁用，显示占位文案
  const nextDisabled = page.locator('.task-nav-btn--next')
  assert(await nextDisabled.isDisabled(), '最早的任务「下一个任务」按钮禁用')
  assert((await nextDisabled.locator('.task-nav-title').textContent()).includes('没有更新的任务了'), '禁用态显示占位文案')
  assert((await page.locator('.task-nav-btn').first().locator('.task-nav-title').textContent()).includes('职场效率提升干货'), '任务 1 的「上一个任务」为任务 2')

  await page.screenshot({ path: 'tests/e2e/screenshots/commission-task-nav-desktop.png', fullPage: true })
  await page.close()
}

// ---------- 手机端 ----------
{
  console.log('[手机端] /console/commission/3')
  const page = await browser.newPage()
  await setupPage(page, true)
  await page.goto(`${BASE_URL}/console/commission/3`)
  await page.waitForSelector('.task-nav', { timeout: 10000 })
  assert(await page.locator('.task-nav').isVisible(), '手机端任务导航区块可见')

  const columns = await page.locator('.task-nav').evaluate((el) => getComputedStyle(el).gridTemplateColumns.split(' ').length)
  assert(columns === 1, '手机端导航竖排单列展示')

  const prevTitle = await page.locator('.task-nav-btn').first().locator('.task-nav-title').textContent()
  const nextTitle = await page.locator('.task-nav-btn--next').locator('.task-nav-title').textContent()
  assert(prevTitle.includes('最新任务'), '手机端「上一个任务」为最新任务')
  assert(nextTitle.includes('职场效率提升干货'), '手机端「下一个任务」为任务 2')

  // 点击「下一个任务」→ 任务 2
  await page.locator('.task-nav-btn--next').click()
  await page.waitForURL('**/console/commission/2', { timeout: 5000 })
  await page.waitForFunction(() => document.querySelector('.content-head h1')?.textContent?.includes('职场效率提升干货'), null, { timeout: 5000 })
  assert(true, '手机端点击「下一个任务」跳转成功')

  await page.screenshot({ path: 'tests/e2e/screenshots/commission-task-nav-mobile.png', fullPage: true })
  await page.close()
}

await browser.close()
console.log('\n全部断言通过')

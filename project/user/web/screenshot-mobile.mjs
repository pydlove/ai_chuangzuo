import { chromium, devices } from '@playwright/test'

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({ ...devices['iPhone 12'] })
const page = await context.newPage()

await page.route('http://127.0.0.1:4173/api/**', async route => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, data: {}, message: 'ok' })
  })
})

try {
  await page.goto('http://127.0.0.1:4173/login', { waitUntil: 'networkidle' })
  await page.evaluate(() => localStorage.setItem('aichuangzuo_access_token', 'fake'))
  await page.goto('http://127.0.0.1:4173/console/create/free', { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)
  await page.click('text=选择导出模板')
  await page.waitForTimeout(1000)
  await page.screenshot({ path: 'mobile-template-modal.png', fullPage: false })
  console.log('Screenshot saved to mobile-template-modal.png')
} catch (e) {
  console.log('Error:', e.message)
} finally {
  await browser.close()
}

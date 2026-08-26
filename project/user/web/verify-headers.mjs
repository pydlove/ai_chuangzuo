import { chromium, devices } from 'playwright'
import fs from 'fs'
import path from 'path'

const url = process.argv[2] || 'http://localhost:22346'
const outputDir = '/tmp/verify-headers-screenshots'
fs.mkdirSync(outputDir, { recursive: true })

const browser = await chromium.launch()
const context = await browser.newContext({
  viewport: { width: 390, height: 844 },
  userAgent: devices['iPhone 13'].userAgent
})
const page = await context.newPage()

await page.addInitScript((token) => {
  localStorage.setItem('aichuangzuo_access_token', token)
  localStorage.setItem('aichuangzuo_user_id', '3358')
}, process.env.TOKEN)

for (const target of ['/console/account-check', '/console/weekly-data']) {
  await page.goto(`${url}${target}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  const name = target.replace('/console/', '')
  await page.screenshot({ path: path.join(outputDir, `${name}.png`), fullPage: true })
  console.log(target, '=>', await page.url())
}

await browser.close()
console.log('screenshots saved to', outputDir)

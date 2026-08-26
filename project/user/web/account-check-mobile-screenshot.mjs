import { chromium, devices } from 'playwright'
import fs from 'fs'
import path from 'path'

const url = process.argv[2] || 'http://localhost:22346'
const outputDir = '/tmp/account-check-screenshots'
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

await page.goto(`${url}/console/account-check`, { waitUntil: 'networkidle' })
await page.waitForTimeout(1500)
await page.screenshot({ path: path.join(outputDir, '10-account-check-logged-in.png'), fullPage: true })
console.log('current url:', await page.url())

await browser.close()
console.log('screenshots saved to', outputDir)

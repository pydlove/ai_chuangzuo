# 兑换码功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在控制台顶部新增一个「🎟️ 兑换码」胶囊按钮,点击打开极简弹框,输入码后兑换创作币或会员天数。

**Architecture:** 全部逻辑集中在 `ConsoleLayout.vue`(与邀请有礼同源),新增一个 `a-modal` 弹框、头部按钮、状态与兑换逻辑;会员数据存储从 string 升级为 `{ level, expiresAt }` 对象并兼容旧值;预置测试码硬编码于常量对象;新增 e2e 脚本验证 6 条用例。

**Tech Stack:** Vue 3 + `<script setup>` + Ant Design Vue(`a-modal`/`a-tooltip`/`message`) + Vite + Playwright(localStorage 模拟数据)

## Global Constraints

- 只在用户端控制台范围实现,不引入后端或新页面路由
- 不增加一级侧边栏菜单项
- 弹框只保留输入区,不展示兑换历史/规则(历史仅持久化到 localStorage 作接口预留)
- 预置码与模拟数据性质一致,硬编码在前端
- 颜色/圆角复用现有 `.console-invite-btn` 红色胶囊风格
- 遵循现有 `localStorage` 键前缀 `aichuangzuo_`

---

## File Structure

| 文件 | 责任 |
|---|---|
| `project/user/web/src/views/console/ConsoleLayout.vue` | 新增 🎟️ 按钮、兑换弹框、兑换逻辑、会员数据结构迁移、所有相关样式 |
| `tests/e2e/verify_redeem_code.py` | 端到端测试脚本,覆盖弹框渲染、成功/失败/重复兑换、空输入禁用 |

---

### Task 1: 添加头部 🎟️ 兑换码按钮

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue:343-349` (🎁 邀请有礼按钮下方)

**Interfaces:**
- Consumes: 无
- Produces: `openRedeemModal()` 函数(下一步实现)

- [ ] **Step 1: 在邀请有礼按钮后插入兑换码按钮**

在 `ConsoleLayout.vue` 中找到 `<!-- 邀请有礼按钮 -->` 注释下方的按钮,紧跟其后添加:

```vue
<!-- 兑换码按钮 -->
<a-tooltip title="兑换码">
  <button class="console-icon-btn console-invite-btn" @click="openRedeemModal">
    <span style="font-size: 16px;">🎟️</span>
    <span>兑换码</span>
  </button>
</a-tooltip>
```

- [ ] **Step 2: 启动 dev server 验证按钮可见**

运行:

```bash
cd project/user/web
npm run dev
```

浏览器打开 `http://localhost:5173/console/create`,确认顶部右侧在 🎁 邀请有礼 右侧出现 🎟️ 兑换码 胶囊按钮。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(redeem): 在控制台头部添加兑换码入口按钮

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 添加兑换码弹框骨架

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue` (在邀请有礼 modal 结束后新增)

**Interfaces:**
- Consumes: `redeemVisible` ref
- Produces: 弹框 UI 结构

- [ ] **Step 1: 在邀请有礼 modal 后添加 redeem modal**

在 `ConsoleLayout.vue` 中 `</a-modal>` (海报样式选择弹框之后或邀请有礼弹框之后,推荐放在邀请有礼 modal 结束标签之后)插入:

```vue
<!-- 兑换码弹框 -->
<a-modal
  v-model:open="redeemVisible"
  :footer="null"
  :width="420"
  centered
  class="redeem-modal"
>
  <div class="redeem-panel">
    <div class="redeem-header">
      <span class="redeem-title">🎟️ 兑换码</span>
      <span class="redeem-subtitle">输入兑换码兑换奖励</span>
    </div>

    <input
      ref="redeemInputRef"
      v-model="redeemCode"
      class="redeem-input"
      placeholder="请输入兑换码"
      maxlength="32"
      @keydown.enter="submitRedeem"
    />

    <div v-if="redeemStatus" :class="['redeem-status', redeemStatus.type]">
      {{ redeemStatus.message }}
    </div>

    <button
      class="invite-btn invite-btn-primary redeem-submit"
      :disabled="!canSubmitRedeem"
      @click="submitRedeem"
    >
      {{ redeemLoading ? '兑换中...' : '立即兑换' }}
    </button>
  </div>
</a-modal>
```

- [ ] **Step 2: 声明基础状态变量**

在 `<script setup>` 顶部(邀请有礼相关常量之后或附近)添加:

```javascript
// ---------- 兑换码 ----------
const REDEEM_USED_KEY = 'aichuangzuo_redeem_codes'
const REDEEM_HISTORY_KEY = 'aichuangzuo_redeem_history'

const redeemVisible = ref(false)
const redeemCode = ref('')
const redeemLoading = ref(false)
const redeemStatus = ref(null)
const redeemInputRef = ref(null)

const canSubmitRedeem = computed(() => {
  return redeemCode.value.trim().length >= 6 && !redeemLoading.value
})
```

- [ ] **Step 3: 添加打开/关闭弹框函数**

在 `script setup` 中添加入口函数:

```javascript
const openRedeemModal = () => {
  redeemVisible.value = true
  redeemCode.value = ''
  redeemStatus.value = null
  nextTick(() => {
    redeemInputRef.value?.focus()
  })
}
```

需要在 `<script setup>` 顶部引入 `nextTick`:

```javascript
import { ref, computed, reactive, onMounted, watch, nextTick } from 'vue'
```

- [ ] **Step 4: 启动 dev server 验证弹框打开**

点击 🎟️ 兑换码 按钮,确认弹框出现,包含标题、输入框、状态位、立即兑换按钮。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(redeem): 添加兑换码弹框骨架与基础状态

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 实现兑换逻辑与预置码

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`

**Interfaces:**
- Consumes: `redeemCode`, `coinBalance`, `setCoinBalance`, membership state/functions
- Produces: `submitRedeem`, `getRedeemedCodes`, `saveRedeemedCode`, `redeemCodeImpl`, `extendMembership` functions

- [ ] **Step 1: 定义预置兑换码常量**

在 `script setup` 中兑换码状态变量之后添加:

```javascript
const REDEEM_PRESETS = {
  COIN100: { type: 'coin', reward: 100 },
  COIN500: { type: 'coin', reward: 500 },
  VIP7DAY: { type: 'membership', reward: 7, level: '专业版会员' },
  VIP30DAY: { type: 'membership', reward: 30, level: '专业版会员' }
}
```

- [ ] **Step 2: 添加已使用码读写函数**

```javascript
const getRedeemedCodes = () => {
  try {
    const raw = localStorage.getItem(REDEEM_USED_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const saveRedeemedCode = (code) => {
  const codes = getRedeemedCodes()
  if (!codes.includes(code)) {
    codes.push(code)
    localStorage.setItem(REDEEM_USED_KEY, JSON.stringify(codes))
  }
}

const appendRedeemHistory = (record) => {
  try {
    const raw = localStorage.getItem(REDEEM_HISTORY_KEY)
    const history = raw ? JSON.parse(raw) : []
    history.unshift(record)
    localStorage.setItem(REDEEM_HISTORY_KEY, JSON.stringify(history))
  } catch {
    // ignore
  }
}
```

- [ ] **Step 3: 改造会员数据加载逻辑**

替换现有 `loadMembership` 为:

```javascript
const loadMembership = () => {
  const raw = localStorage.getItem(MEMBERSHIP_KEY)
  if (!raw) {
    hasMembership.value = false
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      membershipLevel.value = parsed.level || '年会员'
      membershipExpiry.value = parsed.expiresAt || ''
      hasMembership.value = true
      return
    }
  } catch {
    // 旧格式 string,迁移
  }
  // 旧 string 格式
  membershipLevel.value = raw
  const fallbackExpiry = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  membershipExpiry.value = fallbackExpiry
  hasMembership.value = true
  // 写入新格式
  localStorage.setItem(MEMBERSHIP_KEY, JSON.stringify({
    level: membershipLevel.value,
    expiresAt: membershipExpiry.value
  }))
}
```

在 `membershipLevel` ref 附近新增 `membershipExpiry` ref:

```javascript
const membershipLevel = ref('年会员')
const membershipExpiry = ref('')
```

- [ ] **Step 4: 添加会员续期函数**

```javascript
const extendMembership = (days, level) => {
  const now = new Date()
  const currentExpiry = membershipExpiry.value ? new Date(membershipExpiry.value) : now
  const base = currentExpiry > now ? currentExpiry : now
  const newExpiry = new Date(base.getTime() + days * 24 * 60 * 60 * 1000)
  const isoDate = newExpiry.toISOString().split('T')[0]

  membershipLevel.value = level || membershipLevel.value || '年会员'
  membershipExpiry.value = isoDate
  hasMembership.value = true

  localStorage.setItem(MEMBERSHIP_KEY, JSON.stringify({
    level: membershipLevel.value,
    expiresAt: isoDate
  }))
}
```

- [ ] **Step 5: 实现提交兑换函数**

```javascript
const submitRedeem = () => {
  const code = redeemCode.value.trim().toUpperCase()
  if (code.length < 6) {
    redeemStatus.value = { type: 'error', message: '兑换码格式不正确' }
    return
  }
  if (redeemLoading.value) return

  redeemLoading.value = true
  redeemStatus.value = null

  // 模拟网络请求
  setTimeout(() => {
    if (getRedeemedCodes().includes(code)) {
      redeemStatus.value = { type: 'error', message: '该兑换码已被使用过' }
      redeemLoading.value = false
      return
    }

    const preset = REDEEM_PRESETS[code]
    if (!preset) {
      redeemStatus.value = { type: 'error', message: '兑换码无效或已过期' }
      redeemLoading.value = false
      return
    }

    saveRedeemedCode(code)
    appendRedeemHistory({
      code,
      type: preset.type,
      reward: preset.reward,
      redeemedAt: new Date().toISOString()
    })

    if (preset.type === 'coin') {
      addCoin(preset.reward, `兑换码 ${code}`)
      redeemStatus.value = { type: 'success', message: `✅ 兑换成功 +${preset.reward} 创作币` }
    } else if (preset.type === 'membership') {
      extendMembership(preset.reward, preset.level)
      redeemStatus.value = { type: 'success', message: `✅ 兑换成功 +${preset.reward} 天${preset.level}` }
    }

    redeemLoading.value = false
    setTimeout(() => {
      redeemVisible.value = false
      redeemCode.value = ''
      redeemStatus.value = null
    }, 2000)
  }, 400)
}
```

- [ ] **Step 6: 更新用户中心有效期显示**

在 `ConsoleLayout.vue` 用户中心下拉中,把硬编码的:

```vue
<div class="membership-expiry" v-if="hasMembership">有效期至 2026-12-31</div>
```

替换为:

```vue
<div class="membership-expiry" v-if="hasMembership">有效期至 {{ membershipExpiry }}</div>
```

- [ ] **Step 7: 手动验证兑换功能**

打开控制台 → 点击 🎟️ 兑换码 → 输入 `COIN100` → 点击立即兑换 → 观察:
- 按钮文案变成「兑换中...」
- 2 秒后顶部显示绿色成功提示
- 弹框自动关闭
- 顶部创作币余额增加 100(可在 🎁 邀请有礼 弹框中查看)
- 再次输入 `COIN100` 提示「该兑换码已被使用过」
- 输入 `INVALID` 提示「兑换码无效或已过期」
- 输入 `VIP7DAY` 后顶部会员 badge 显示「专业版会员」且有效期延长 7 天

- [ ] **Step 8: Commit**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(redeem): 实现兑换码兑换逻辑与会员数据升级

- 预置 COIN100/COIN500/VIP7DAY/VIP30DAY 四组测试码
- 创作币兑换直接累加余额
- 会员数据从 string 升级为 { level, expiresAt },兼容旧值
- 支持会员天数在剩余有效期上累加
- 兑换成功后 2 秒自动关闭弹框

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 添加弹框样式

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue` (在 `<style scoped>` 末尾或邀请有礼样式附近添加)

**Interfaces:**
- Consumes: 无
- Produces: `.redeem-*` CSS classes

- [ ] **Step 1: 添加 redeem 样式**

在 `<style scoped>` 末尾(暗色主题媒体查询之前)追加:

```css
/* ========== 兑换码弹框 ========== */
.redeem-modal .ant-modal-body {
  padding: 0;
}

.redeem-panel {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
}

.redeem-header {
  text-align: center;
  margin-bottom: 20px;
}

.redeem-title {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.redeem-subtitle {
  display: block;
  font-size: 13px;
  color: #8c8c8c;
}

.redeem-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 10px;
  font-size: 15px;
  color: #1a1a1a;
  letter-spacing: 1px;
  text-transform: uppercase;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.redeem-input::placeholder {
  text-transform: none;
  color: #bfbfbf;
}

.redeem-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.redeem-status {
  margin-top: 14px;
  font-size: 13px;
  text-align: center;
  min-height: 20px;
}

.redeem-status.error {
  color: #ff4d4f;
}

.redeem-status.success {
  color: #07c160;
}

.redeem-submit {
  width: 100%;
  margin-top: 14px;
  padding: 12px;
  font-size: 15px;
}
```

- [ ] **Step 2: 添加暗色主题样式**

在 `<style scoped>` 的暗色主题区追加:

```css
body[data-theme="dark"] .redeem-panel {
  background: #1f1f1f;
}

body[data-theme="dark"] .redeem-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .redeem-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .redeem-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .redeem-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .redeem-status.error {
  color: #ff7875;
}

body[data-theme="dark"] .redeem-status.success {
  color: #10b981;
}
```

- [ ] **Step 3: 验证视觉**

在 light / dark 主题下分别打开兑换码弹框,确认:
- 输入框、标题、按钮、状态文字颜色正常
- 成功提示为绿色,错误提示为红色
- 按钮与邀请有礼按钮视觉一致

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(redeem): 添加兑换码弹框 light/dark 样式

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: 端到端测试

**Files:**
- Create: `tests/e2e/verify_redeem_code.py`

**Interfaces:**
- Consumes: 页面 UI 元素(classes `.console-invite-btn`, `.redeem-*`)
- Produces: 测试脚本 + 截图

- [ ] **Step 1: 创建测试脚本**

```python
import re
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

BASE_URL = "http://localhost:5173"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"


def test_redeem_code():
    SCREENSHOT_DIR.mkdir(exist_ok=True)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        # 清理之前的测试数据
        page.evaluate("""() => {
            localStorage.removeItem('aichuangzuo_redeem_codes')
            localStorage.removeItem('aichuangzuo_redeem_history')
            localStorage.removeItem('aichuangzuo_coin_balance')
            localStorage.removeItem('aichuangzuo_membership')
            localStorage.setItem('aichuangzuo_notif_seeded', '1')
        }""")

        page.goto(f"{BASE_URL}/console/create")
        page.wait_for_selector(".console-layout", timeout=10000)

        # 获取当前余额
        def get_coin_balance():
            return page.evaluate("""() => {
                const raw = localStorage.getItem('aichuangzuo_coin_balance')
                return raw ? parseInt(raw, 10) : 0
            }""")

        # 用例 1: 弹框渲染
        redeem_buttons = page.locator("button:has-text('兑换码')")
        expect(redeem_buttons).to_have_count(1)
        redeem_buttons.first.click()
        page.wait_for_selector(".redeem-panel", timeout=5000)
        page.screenshot(path=SCREENSHOT_DIR / "redeem_modal_open.png")

        # 用例 6: 空输入时按钮 disabled
        submit_btn = page.locator(".redeem-submit")
        expect(submit_btn).to_be_disabled()

        # 用例 2: 兑换创作币 COIN100
        page.locator(".redeem-input").fill("COIN100")
        expect(submit_btn).to_be_enabled()
        submit_btn.click()
        page.wait_for_selector(".redeem-status.success", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("兑换成功 +100 创作币")
        page.screenshot(path=SCREENSHOT_DIR / "redeem_success_coin.png")
        page.wait_for_timeout(2200)
        # 弹框关闭
        expect(page.locator(".redeem-panel")).not_to_be_visible()
        assert get_coin_balance() == 100, f"余额应为 100,实际 {get_coin_balance()}"

        # 用例 5: 重复兑换提示已使用
        redeem_buttons.first.click()
        page.locator(".redeem-input").fill("COIN100")
        submit_btn.click()
        page.wait_for_selector(".redeem-status.error", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("该兑换码已被使用过")
        page.keyboard.press("Escape")

        # 用例 3: 兑换会员 VIP7DAY
        redeem_buttons.first.click()
        page.locator(".redeem-input").fill("VIP7DAY")
        submit_btn.click()
        page.wait_for_selector(".redeem-status.success", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("兑换成功 +7 天专业版会员")
        page.screenshot(path=SCREENSHOT_DIR / "redeem_success_membership.png")
        page.wait_for_timeout(2200)

        # 验证 membership 写入
        membership = page.evaluate("""() => {
            const raw = localStorage.getItem('aichuangzuo_membership')
            return raw ? JSON.parse(raw) : null
        }""")
        assert membership and membership.get("level") == "专业版会员", membership
        assert re.match(r"\d{4}-\d{2}-\d{2}", membership.get("expiresAt", "")), membership

        # 用例 4: 无效码
        redeem_buttons.first.click()
        page.locator(".redeem-input").fill("INVALID")
        submit_btn.click()
        page.wait_for_selector(".redeem-status.error", timeout=5000)
        expect(page.locator(".redeem-status")).to_contain_text("兑换码无效或已过期")
        page.screenshot(path=SCREENSHOT_DIR / "redeem_error_invalid.png")

        browser.close()
        print("All redeem code tests passed.")


if __name__ == "__main__":
    test_redeem_code()
```

- [ ] **Step 2: 运行测试**

确保 dev server 仍在运行(端口 5173),然后执行:

```bash
python3 tests/e2e/verify_redeem_code.py
```

预期输出:

```
All redeem code tests passed.
```

并生成 4 张截图:
- `tests/e2e/screenshots/redeem_modal_open.png`
- `tests/e2e/screenshots/redeem_success_coin.png`
- `tests/e2e/screenshots/redeem_success_membership.png`
- `tests/e2e/screenshots/redeem_error_invalid.png`

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_redeem_code.py tests/e2e/screenshots/redeem_*.png
git commit -m "test(e2e): 添加兑换码功能端到端验证

覆盖弹框渲染、创作币兑换、会员兑换、重复兑换、无效码、空输入禁用。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Self-Review

### Spec Coverage

| Spec 要求 | 实现任务 |
|---|---|
| 头部 🎟️ 按钮与邀请有礼并列 | Task 1 |
| 同款红色胶囊样式 | Task 1 + Task 4 |
| 极简弹框只保留输入区 | Task 2 |
| 自动 trim + 转大写 | Task 3 Step 5 |
| Enter 提交 | Task 2 Step 1 |
| 空输入禁用 + 格式校验 | Task 2 Step 2 + Task 3 Step 5 |
| 预置测试码 | Task 3 Step 1 |
| 创作币奖励 | Task 3 Step 5 |
| 会员天数奖励 | Task 3 Step 4 + Step 5 |
| 已使用码去重 | Task 3 Step 2 + Step 5 |
| 成功/错误状态位 | Task 2 Step 1 + Task 3 Step 5 + Task 4 |
| 2 秒后自动关闭 | Task 3 Step 5 |
| 会员数据升级 + 兼容旧值 | Task 3 Step 3 |
| localStorage 持久化 | Task 3 Step 2 + Step 3 + Step 4 |
| e2e 测试 6 条用例 | Task 5 |

### Placeholder Scan

- 无 TBD/TODO
- 所有步骤包含可运行代码或命令
- 所有文件路径为精确路径

### Type Consistency

- `redeemStatus` 为 `{ type: 'success'|'error', message: string } | null`
- `REDEEM_PRESETS` 键大写,与 `submitRedeem` 中 `toUpperCase()` 一致
- `membershipExpiry` 为 `YYYY-MM-DD` 字符串,与显示位置一致
- `getRedeemedCodes` / `saveRedeemedCode` 与测试脚本中 localStorage 键一致

### 已知边界

- 测试脚本会清除 `aichuangzuo_redeem_codes`、`aichuangzuo_redeem_history`、`aichuangzuo_coin_balance`、`aichuangzuo_membership`,避免历史状态干扰
- 测试要求 dev server 在 `localhost:5173` 运行,与现有其他 e2e 脚本一致

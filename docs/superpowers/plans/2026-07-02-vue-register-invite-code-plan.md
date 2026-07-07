# Vue 控制台注册页邀请码实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Vue 控制台 `Login.vue` 注册表单新增可选邀请码输入字段；URL `?ref=` 自动填充；手动填写优先；防自邀请校验；注册成功后发 5 创作币。

**Architecture:** 工具函数封装在 `composables/useInviteCode.js`（与 `useStyles.js` 同风格）；`Login.vue` 在 template 加邀请 banner + 输入框，在 script 加自邀请校验与发币；Playwright 脚本覆盖关键场景。

**Tech Stack:** Vue 3 + `<script setup>` Composition API，Ant Design Vue（`message` 组件），Vite，Playwright（Python，e2e 验证）。

**Spec:** `docs/superpowers/specs/2026-07-02-vue-register-invite-code-design.md`

## Global Constraints

- 不引入新依赖；不修改 `ConsoleLayout.vue`、`WithdrawIndex.vue`、`router/index.js`。
- localStorage key 沿用现有：`aichuangzuo_invite_code`（ConsoleLayout.vue 用）、`aichuangzuo_coin_balance`（WithdrawIndex.vue 用）。**新增** `aichuangzuo_invite_ref`。
- 邀请码生成规则：6 位 `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`（去除 I、L、O、0、1），与 `ConsoleLayout.vue` 中 `generateInviteCode()` 完全一致。
- Toast / 提示用 Ant Design Vue `message.warning(...)` / `message.success(...)`，与现有 ConsoleLayout.vue 一致。
- 字号、间距、圆角沿用 Login.vue 现有 `.form-item` / `.form-input` / `.form-label` 风格，不引入新 CSS 变量。

---

## Task 1: 创建 composable `useInviteCode.js`

**Files:**
- Create: `project/user/web/src/composables/useInviteCode.js`

**背景:** Vue 端需要邀请码工具：生成 / 读取自己的码、URL ref 解析、localStorage 同步、消费 ref 发币。封装在 composable 中便于 Login.vue 调用。

- [ ] **Step 1: 创建 `useInviteCode.js`**

新建文件 `project/user/web/src/composables/useInviteCode.js`，写入：

```js
const INVITE_CODE_KEY = 'aichuangzuo_invite_code'
const INVITE_REF_KEY = 'aichuangzuo_invite_ref'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const COIN_BONUS_NEW_USER = 5

const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

function generateInviteCode() {
  let code = ''
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

/** 获取或生成当前用户的邀请码（持久化在 localStorage）。 */
export function getInviteCode() {
  let code = localStorage.getItem(INVITE_CODE_KEY)
  if (!code) {
    code = generateInviteCode()
    localStorage.setItem(INVITE_CODE_KEY, code)
  }
  return code
}

/** 读取邀请链接 ?ref= 参数；返回 trim+uppercase 后的码。 */
export function getRefFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const ref = params.get('ref')
  return ref ? ref.trim().toUpperCase() : ''
}

/** 把 ref 写入 localStorage；空值则清除残留。 */
export function setStoredRef(ref) {
  if (ref) {
    localStorage.setItem(INVITE_REF_KEY, ref)
  } else {
    localStorage.removeItem(INVITE_REF_KEY)
  }
}

/** 读取持久化的 ref。 */
export function getStoredRef() {
  const raw = localStorage.getItem(INVITE_REF_KEY)
  return raw ? raw.trim().toUpperCase() : ''
}

/** 注册完成时调用：消费 ref，给当前用户发 5 创作币。返回发放数量。 */
export function awardNewUserCoins() {
  const ref = getStoredRef()
  if (!ref) return 0
  const raw = localStorage.getItem(COIN_BALANCE_KEY)
  const balance = raw ? parseInt(raw, 10) : 0
  localStorage.setItem(COIN_BALANCE_KEY, String(balance + COIN_BONUS_NEW_USER))
  localStorage.removeItem(INVITE_REF_KEY)
  return COIN_BONUS_NEW_USER
}
```

- [ ] **Step 2: 手动冒烟测试**

启动 Vue 控制台 dev server（端口参见 `project/user/web/package.json`）：
```bash
cd project/user/web && npm run dev
```

浏览器打开 dev server URL，DevTools Console 执行：

```js
// 模拟从 ConsoleLayout 生成的码
localStorage.setItem('aichuangzuo_invite_code', 'K7P2QX')
```

Console 验证（应返回 `'K7P2QX'`）：
```js
await import('/src/composables/useInviteCode.js').then(m => m.getInviteCode())
```

进一步验证（应返回 0，因为没设 ref）：
```js
await import('/src/composables/useInviteCode.js').then(m => m.awardNewUserCoins())
```

设了 ref 后再调：
```js
localStorage.setItem('aichuangzuo_invite_ref', 'ABC123')
await import('/src/composables/useInviteCode.js').then(m => m.awardNewUserCoins())
// 期望返回 5；aichuangzuo_coin_balance 增加 5；aichuangzuo_invite_ref 被清除
```

- [ ] **Step 3: 提交**

```bash
git add project/user/web/src/composables/useInviteCode.js
git commit -m "feat(invite): 新建 useInviteCode composable 封装邀请码工具"
```

---

## Task 2: Login.vue UI 改动（template + style）

**Files:**
- Modify: `project/user/web/src/views/Login.vue:84-139`（注册表单 template）
- Modify: `project/user/web/src/views/Login.vue:218-484`（style scoped）

**背景:** 在注册表单加 invite banner + 邀请码输入框，并加少量配套样式。

- [ ] **Step 1: 在 template 插入 invite banner 与输入框**

打开 `project/user/web/src/views/Login.vue`，定位到「确认密码」段（`<label class="form-label">确认密码</label>` 所在 `<div class="form-item">`）的结束 `</div>` 之后、`<button class="submit-btn" @click="handleRegister">注册</button>` 之前，插入：

```vue
        <!-- 邀请 banner（仅 ref 存在时显示） -->
        <a-alert
          v-if="showInviteBanner"
          message="你收到了好友的邀请，注册并完成邮箱验证后可获得 5 个创作币。"
          type="success"
          show-icon
          class="invite-banner"
        />

        <div class="form-item">
          <label class="form-label">
            邀请码 <span class="form-label-optional">（选填）</span>
          </label>
          <input
            v-model="registerForm.inviteCode"
            type="text"
            class="form-input"
            placeholder="如没有可留空"
            maxlength="6"
          />
        </div>
```

> 注：原文件中 `<div class="form-item">` 等使用 2 空格缩进；插入的代码块需保持 8 空格缩进（与「确认密码」段平级，位于注册 form 内）。

- [ ] **Step 2: 追加配套样式**

在 `<style scoped>` 内（任意位置，建议紧邻 `.form-item` 之后），追加：

```css
.invite-banner {
  margin-bottom: 16px;
  border-radius: 8px;
}

.form-label-optional {
  color: #8c8c8c;
  font-weight: 400;
}
```

- [ ] **Step 3: 视觉验证**

启动 dev server，浏览器打开 `/login`，点击「注册」tab。

预期：
- 「确认密码」与「注册」按钮之间出现「邀请码（选填）」输入框，灰色「（选填）」标识。
- 顶部不显示 banner（因为还没接 onMounted 逻辑，下一 task 才实现）。

DevTools Console 验证：
```js
document.querySelector('input[placeholder="如没有可留空"]') !== null
```
预期：`true`。

- [ ] **Step 4: 提交**

```bash
git add project/user/web/src/views/Login.vue
git commit -m "feat(login): 注册表单新增邀请码输入框与 banner"
```

---

## Task 3: Login.vue 逻辑改动（script）

**Files:**
- Modify: `project/user/web/src/views/Login.vue:150-216`（script setup）

**背景:** 加响应式状态、onMounted 自动填充、handleRegister 自邀请校验与发币。

- [ ] **Step 1: 追加 imports**

打开 `<script setup>` 段，把：

```js
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
```

替换为：

```js
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getInviteCode, getRefFromUrl, getStoredRef, setStoredRef, awardNewUserCoins } from '@/composables/useInviteCode'
```

- [ ] **Step 2: `registerForm` 加字段**

定位到：

```js
const registerForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})
```

替换为：

```js
const registerForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  inviteCode: ''
})
```

- [ ] **Step 3: 新增响应式状态**

在 `const activeTab = ref('login')` 之后插入：

```js
const showInviteBanner = ref(false)
```

- [ ] **Step 4: 改造 `handleRegister`**

定位到：

```js
const handleRegister = () => {
  // TODO: 调用注册接口
  console.log('注册', registerForm)
}
```

替换为：

```js
const handleRegister = async () => {
  const inviteCode = registerForm.inviteCode.trim().toUpperCase()

  // 1. 自邀请校验
  const selfCode = getInviteCode()
  if (inviteCode && inviteCode === selfCode) {
    message.warning('不能填写自己的邀请码')
    return
  }

  // 2. 输入框是唯一真值；空字符串显式清除残留 ref
  setStoredRef(inviteCode)

  // TODO: 调用注册接口
  console.log('注册', registerForm)

  // 3. 注册成功后发放创作币并提示
  const coins = awardNewUserCoins()
  if (coins > 0) {
    message.success(`注册成功，邀请奖励 +${coins} 创作币`)
  }

  router.push('/console')
}
```

- [ ] **Step 5: 加 `onMounted` 钩子**

定位到 `<script setup>` 末尾、`generateCaptcha()` 调用之后，插入：

```js
onMounted(() => {
  const ref = getRefFromUrl()
  if (ref) {
    setStoredRef(ref)
    registerForm.inviteCode = ref
    showInviteBanner.value = true
  } else if (getStoredRef()) {
    // localStorage 残留 ref（用户刷新页面），banner 仍显示
    showInviteBanner.value = true
  }
})
```

完整 `<script setup>` 末尾部分大致为：

```js
const handleRegister = async () => {
  // ... 如 step 4
}

// 初始化验证码
generateCaptcha()

onMounted(() => {
  // ... 如 step 5
})
```

- [ ] **Step 6: 手动验证（场景 2 — URL 自动填充）**

清空 localStorage 中相关 key：
```js
localStorage.removeItem('aichuangzuo_invite_ref')
localStorage.removeItem('aichuangzuo_invite_code')
localStorage.removeItem('aichuangzuo_coin_balance')
```

浏览器访问：
```
http://<dev-host>/login?ref=ABC123
```

点击「注册」tab。

预期：
- 顶部出现绿色 banner「你收到了好友的邀请，注册并完成邮箱验证后可获得 5 个创作币。」
- 邀请码输入框预填 `ABC123`。

- [ ] **Step 7: 手动验证（场景 5 — 自邀请拦截）**

Console 设一个自己的邀请码（模拟已在 ConsoleLayout 生成）：
```js
localStorage.setItem('aichuangzuo_invite_code', 'K7P2QX')
```

刷新 `/login?ref=K7P2QX`，点击「注册」tab。邀请码输入框应预填 `K7P2QX`。点击「注册」按钮。

预期：
- 顶部弹 message「不能填写自己的邀请码」（橙色 warning）。
- URL 不跳转（仍停留在 `/login`）。

- [ ] **Step 8: 手动验证（场景 7 — 外部 ref 正常注册并发币）**

清空 ref 后访问：
```
/login?ref=ZZZZZZ
```

点击「注册」tab，点击「注册」按钮。

预期：
- 顶部 message「注册成功，邀请奖励 +5 创作币」（绿色 success）。
- URL 跳转到 `/console`。
- Console 验证：
  ```js
  localStorage.getItem('aichuangzuo_coin_balance')  // "5"
  localStorage.getItem('aichuangzuo_invite_ref')     // null
  ```

- [ ] **Step 9: 提交**

```bash
git add project/user/web/src/views/Login.vue
git commit -m "feat(login): 邀请码 URL 自动填充 + 自邀请校验 + 发币"
```

---

## Task 4: Playwright 验证脚本

**Files:**
- Create: `tests/e2e/verify_login_invite.py`

**背景:** 自动化覆盖场景 1/2/4/5/7。dev server 端口从 env `BASE_URL` 读取，默认参考现有 `verify_redeem_code.py` 用 `http://localhost:22347`（用户可覆盖）。

- [ ] **Step 1: 创建 Playwright 脚本**

新建 `tests/e2e/verify_login_invite.py`：

```python
#!/usr/bin/env python3
"""Vue 控制台登录页邀请码端到端验证。

覆盖场景：
  1) 无 ref 直接打开 → 输入框为空，无 banner
  2) 带 ref=ABC123 打开 → 输入框预填，banner 显示
  4) 手动填邀请码注册 → 跳 /console + coin_balance=5
  5) 填自己的邀请码 → message.warning 拦截
  7) URL 带与自身不同的 ref → 正常注册 + 发币

用法：
  cd project/user/web && npm run dev
  python3 tests/e2e/verify_login_invite.py
"""
import os
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

BASE_URL = os.environ.get("BASE_URL", "http://localhost:22347")
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def open_login(page, ref=None):
    url = f"{BASE_URL}/login" + (f"?ref={ref}" if ref else "")
    page.goto(url)
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(500)


def switch_to_register(page):
    """点击「注册」tab。"""
    page.locator("button.auth-tab", has_text="注册").click()
    page.wait_for_timeout(300)


def main():
    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        try:
            # ========== 场景 1: 无 ref 直接打开 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            # 清空邀请相关 localStorage
            page.evaluate("""() => {
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_invite_code')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page)
            switch_to_register(page)
            invite_val = page.locator('input[placeholder="如没有可留空"]').input_value()
            banner_visible = page.locator(".invite-banner").count() > 0 and \
                             page.locator(".invite-banner").is_visible()
            ok = invite_val == "" and not banner_visible
            results.append(("场景1 无ref输入框为空+banner隐藏", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_no_ref.png"))
            ctx.close()

            # ========== 场景 2: 带 ref=ABC123 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.evaluate("""() => {
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page, ref="ABC123")
            switch_to_register(page)
            invite_val = page.locator('input[placeholder="如没有可留空"]').input_value()
            banner_visible = page.locator(".invite-banner").is_visible()
            ok = invite_val == "ABC123" and banner_visible
            results.append(("场景2 ref自动填充+banner显示", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_auto_fill.png"))
            ctx.close()

            # ========== 场景 5: 自邀请拦截 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            # 先模拟已在 ConsoleLayout 生成自己的码
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            page.evaluate("""() => {
              localStorage.setItem('aichuangzuo_invite_code', 'K7P2QX')
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page, ref="K7P2QX")
            switch_to_register(page)
            # 直接点注册（输入框已预填自己的码）
            page.locator("button.submit-btn").click()
            page.wait_for_timeout(800)
            # ant-design-vue message 默认 3s 后消失，800ms 仍在
            toast_visible = page.locator("text=不能填写自己的邀请码").is_visible()
            current_url = page.url
            ok = toast_visible and "/login" in current_url
            results.append(("场景5 自邀请拦截", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_self_block.png"))
            ctx.close()

            # ========== 场景 4: 手动填邀请码注册 + 发币 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            page.evaluate("""() => {
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
              localStorage.removeItem('aichuangzuo_invite_code')
            }""")
            open_login(page)
            switch_to_register(page)
            page.locator('input[placeholder="如没有可留空"]').fill("ABC123")
            page.locator("button.submit-btn").click()
            page.wait_for_timeout(800)
            current_url = page.url
            coin_balance = page.evaluate(
                "() => parseInt(localStorage.getItem('aichuangzuo_coin_balance') || '0', 10)"
            )
            ok = "/console" in current_url and coin_balance == 5
            results.append(("场景4 手动填邀请码+发币", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_manual.png"))
            ctx.close()

            # ========== 场景 7: URL 带与自身不同的 ref 正常注册 ==========
            ctx = browser.new_context(viewport={"width": 1440, "height": 900})
            page = ctx.new_page()
            page.goto(BASE_URL + "/login")
            page.wait_for_load_state("networkidle")
            page.evaluate("""() => {
              localStorage.setItem('aichuangzuo_invite_code', 'SELFCODE')
              localStorage.removeItem('aichuangzuo_invite_ref')
              localStorage.removeItem('aichuangzuo_coin_balance')
            }""")
            open_login(page, ref="EXTREF9")
            switch_to_register(page)
            page.locator("button.submit-btn").click()
            page.wait_for_timeout(800)
            current_url = page.url
            coin_balance = page.evaluate(
                "() => parseInt(localStorage.getItem('aichuangzuo_coin_balance') || '0', 10)"
            )
            ok = "/console" in current_url and coin_balance == 5
            results.append(("场景7 外部ref正常注册+发币", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "login_invite_normal.png"))
            ctx.close()

        finally:
            browser.close()

    # 输出结果
    print("\n=== Vue 注册邀请码验证结果 ===")
    all_ok = True
    for name, ok in results:
        status = "✓ PASS" if ok else "✗ FAIL"
        print(f"{status}  {name}")
        if not ok:
            all_ok = False
    print()
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 启动 Vue dev server**

```bash
cd project/user/web && npm run dev
```

预期：dev server 监听端口（默认 vite 是 5173，但项目可能配了别的）。如监听在非 22347，需通过 `BASE_URL=http://localhost:xxxx python3 tests/e2e/verify_login_invite.py` 覆盖。

确认端口：
```bash
# 在另一终端
curl -s http://localhost:22347/login -o /dev/null -w "%{http_code}"
# 或
curl -s http://localhost:5173/login -o /dev/null -w "%{http_code}"
```

选能返回 200 的端口。

- [ ] **Step 3: 运行 Playwright 验证脚本**

```bash
python3 tests/e2e/verify_login_invite.py
```

预期：所有场景输出 `✓ PASS`，退出码 `0`。

如失败，按失败场景对照 spec 验证要点定位：
- 场景 1 失败 → 检查 Task 2 步骤 1 的 template 插入、Task 3 步骤 5 的 onMounted
- 场景 2 失败 → 检查 Task 3 步骤 5 的 onMounted + setStoredRef
- 场景 4 失败 → 检查 Task 3 步骤 4 的 handleRegister + setStoredRef + awardNewUserCoins
- 场景 5 失败 → 检查 Task 3 步骤 4 的自邀请校验
- 场景 7 失败 → 检查 Task 3 步骤 4 中 `awardNewUserCoins()` 调用

- [ ] **Step 4: 检查截图**

打开 `tests/e2e/screenshots/login_invite_*.png`：
- `login_invite_no_ref.png`：邀请码输入框为空，无 banner。
- `login_invite_auto_fill.png`：输入框预填 `ABC123`，顶部 banner 显示。
- `login_invite_self_block.png`：warning toast「不能填写自己的邀请码」可见，未跳转。
- `login_invite_manual.png`：跳转后落在 `/console`（或在过渡画面）。
- `login_invite_normal.png`：同上。

- [ ] **Step 5: 提交**

```bash
git add tests/e2e/verify_login_invite.py tests/e2e/screenshots/login_invite_*.png
git commit -m "test(e2e): Vue 控制台注册邀请码端到端验证"
```

---

## 回归检查

完成后手动 / 跑现有 e2e 验证：
- `tests/e2e/verify_redeem_code.py`（兑换码功能）继续通过。
- `tests/e2e/verify_invite_reward.py`（原型邀请奖励）继续通过。
- `ConsoleLayout.vue` 的邀请有礼弹框手动点开一次，验证自己的邀请码生成、复制、链接复制、海报下载流程未受影响。
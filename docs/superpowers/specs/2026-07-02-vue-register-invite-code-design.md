# Vue 控制台注册页邀请码设计

## 背景

爱创作 Vue 控制台（`project/user/web/`）已有「邀请有礼」模块（详见 `ConsoleLayout.vue`）：
- 老用户可以查看自己的邀请码、邀请链接、生成海报。
- 邀请链接格式：`${origin}/login?ref=${inviteCode}`。

但 Vue 端 `Login.vue` 注册表单没有邀请码输入字段，被邀请人**只能通过点击链接**完成注册（无法手动填写），也无法自邀请拦截。

之前在原型 HTML（`.superpowers/brainstorm/6491-1782131242/content/login.html`）已实现等价功能，详见 `2026-07-02-register-invite-code-design.md`。本文档把同样设计落到 Vue 控制台。

## 目标

1. Vue 注册页提供可选的「邀请码」输入字段，非必填。
2. 通过 `/login?ref=INVITE_CODE` 进入时，输入框自动填充邀请码。
3. 手动填写的邀请码优先于 URL 参数。
4. 拦截自邀请：用户填入的邀请码不能等于自己的邀请码。
5. 注册成功后发放 5 创作币（仅在被邀请时）。
6. 不影响现有的 ConsoleLayout.vue 邀请有礼逻辑。

## 非目标

- 不做邀请码格式实时校验（非必填字段严格校验会劝退用户）。
- 不做后端账号体系下的「已注册用户补绑」逻辑。
- 不改动 ConsoleLayout.vue 的「邀请有礼」弹框。
- 不改动 WithdrawIndex.vue 的提现逻辑。
- 不引入新的依赖。

## 设计

### 1. 新建 composable `useInviteCode.js`

**文件**：`project/user/web/src/composables/useInviteCode.js`（新建）。

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

/** 注册完成时调用：消费 ref，给当前用户发 5 创作币。 */
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

**要点**：
- 工具函数风格（不是 composable hook），与现有 `composables/useStyles.js` 风格一致。
- `INVITE_CODE_KEY` 复用现有 key，与 `ConsoleLayout.vue` 共用。
- `COIN_BALANCE_KEY` 复用现有 key，与 `WithdrawIndex.vue` 共用。
- **新增** `INVITE_REF_KEY = 'aichuangzuo_invite_ref'`：Vue 端之前没有这个 key。

### 2. Login.vue UI 改动

**文件**：`project/user/web/src/views/Login.vue`

#### 2.1 Template 改动

在 `<div v-show="activeTab === 'register'" class="auth-form">` 内、「确认密码」段（`<div class="form-item">` 含 label 「确认密码」）之后、`<button class="submit-btn" @click="handleRegister">注册</button>` 之前，插入：

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

#### 2.2 Style 追加

在 `<style scoped>` 内（任意位置）追加：

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

#### 2.3 Script 改动

**新增 imports**：

```js
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getInviteCode, getRefFromUrl, setStoredRef, awardNewUserCoins } from '@/composables/useInviteCode'
```

**`registerForm` 新增字段**：

```js
const registerForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  inviteCode: ''   // 新增
})
```

**新增响应式状态**：

```js
const showInviteBanner = ref(false)
```

**`onMounted` 钩子**（在 `<script setup>` 末尾、`generateCaptcha()` 之后）：

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

### 3. handleRegister 自邀请校验 + 发币

**位置**：`project/user/web/src/views/Login.vue` 的 `handleRegister` 函数。

**当前实现**：

```js
const handleRegister = () => {
  // TODO: 调用注册接口
  console.log('注册', registerForm)
}
```

**改造后**：

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

**关键点**：

1. **顺序保证**：必须先做自邀请校验，再写 `localStorage`，否则会把自邀请码错误写入。
2. **空值 OK**：邀请码为空时跳过自邀请校验；`setStoredRef('')` 显式 `removeItem` 清除 URL 阶段写入的残留 ref。
3. **手动优先**：用户在输入框中填的码覆盖 URL ref。
4. **message.warning 复用**：用 Ant Design Vue 现有的 `message` API，与 `ConsoleLayout.vue` 一致。
5. **错误处理**：prototype 端 `handleRegister` 没有真实接口调用；这里同步保留 `console.log` 占位。

### 4. 数据流

```
用户访问 /login?ref=ABC123
   ↓
Login.vue onMounted 触发
   ↓
getRefFromUrl() → "ABC123"
setStoredRef("ABC123") 写入 localStorage.aichuangzuo_invite_ref
registerForm.inviteCode = "ABC123"
showInviteBanner = true
   ↓
用户在输入框编辑（v-model 双向绑定）
   ↓
点击「注册」→ handleRegister
   ↓
读取 registerForm.inviteCode（手动优先）
   ↓
若等于自己的邀请码（getInviteCode）→ message.warning 拦截
   ↓
否则 setStoredRef(邀请码) 同步 localStorage（有值写入 / 空值清除）
   ↓
awardNewUserCoins() 读取 localStorage，发 5 创作币并 removeItem
   ↓
router.push('/console')
```

## 边界情况与风险

| 场景 | 处理方案 |
|---|---|
| URL 不带 ref、用户不填邀请码 | 正常注册流程，不发 5 创作币 |
| URL 带 ref、用户手动改成另一个码 | 手动码覆盖，写入 localStorage，发币时使用手动码 |
| URL 带 ref、用户清空输入框 | 输入框视为明确「不填」；`setStoredRef('')` 显式 `removeItem`，本次注册不会发币 |
| URL 带 ref、用户填自己的码 | message.warning 拦截 |
| localStorage 中残留旧 ref（刷新页面） | banner 显示但输入框留空，让用户决定 |
| 邀请码大小写不一致 | `toUpperCase()` 统一比较 |
| `aichuangzuo_coin_balance` 不存在 | `parseInt` 默认值 0，安全 |
| 用户未先访问 ConsoleLayout 即在 Login 注册 | `getInviteCode()` 自动生成并持久化 |
| 用户刷新页面导致 Vue 组件重新挂载 | `onMounted` 每次都跑；`setStoredRef` 幂等 |

## 验证要点

### 手动验证场景

| # | 场景 | 预期结果 |
|---|---|---|
| 1 | 打开 `/login`（无 ref），切换到注册 tab | 邀请码输入框为空，无 banner |
| 2 | 打开 `/login?ref=ABC123`，切换到注册 tab | 输入框预填 `ABC123`，banner 显示 |
| 3 | 场景 2 后，修改输入框为 `XYZ999` | 实时更新（v-model） |
| 4 | 无 ref 打开 `/login`，手动填 `ABC123`，点注册 | 跳转到 `/console`；`localStorage.aichuangzuo_invite_ref = "ABC123"`；`aichuangzuo_coin_balance` 增加 5 |
| 5 | 已经登录过（`aichuangzuo_invite_code = K7P2QX`），注册输入框填 `K7P2QX`，点注册 | 弹 message「不能填写自己的邀请码」，不跳转 |
| 6 | 已经登录过，注册输入框留空，点注册 | 正常跳转，不发 5 创作币 |
| 7 | 已经登录过，URL `?ref=ZZZZZZ`（与自己的码不同），点注册 | 正常跳转，发 5 创作币 |

### Playwright 脚本

参考 `tests/e2e/verify_redeem_code.py` 风格，新建 `tests/e2e/verify_login_invite.py`，覆盖场景 1/2/4/5/7，截图保存到 `tests/e2e/screenshots/`。

### 回归检查

- `ConsoleLayout.vue` 的「邀请有礼」弹框（自己的邀请码生成、链接、海报）不受影响。
- `WithdrawIndex.vue` 的提现逻辑（依赖 `aichuangzuo_coin_balance`）不受影响。
- Vue 应用其他路由（`/forgot`、`/pricing`、`/console/*`）不受影响。
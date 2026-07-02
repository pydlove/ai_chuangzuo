# 注册页邀请码设计

## 背景

爱创作已经上线「邀请有礼」功能（详见 `2026-07-01-invite-reward-design.md`），老用户可以生成邀请码 / 邀请链接 / 海报，邀请新用户完成注册。被邀请人通过邀请链接或邀请码注册后，双方获得奖励。

但当前注册页（`login.html`）只有「邀请 banner」展示，没有邀请码输入字段：

- 用户必须通过邀请链接才能被归因，无法手动填写邀请码。
- 已经看过海报、扫了二维码但链接被截断 / 丢失的用户，无法补填邀请码。
- 自邀请（用户填自己的邀请码）无法拦截。

## 目标

1. 注册页提供可选的「邀请码」输入字段，非必填。
2. 通过 `?ref=INVITE_CODE` 形式的邀请链接进入时，输入框自动填充邀请码。
3. 手动填写的邀请码优先于 URL 参数，符合现有 spec 中「以手动填写为准」的规定。
4. 拦截自邀请：用户填入的邀请码不能等于自己的邀请码。
5. 不影响现有的「邀请 banner」展示与奖励发放链路。

## 非目标

- 不做邀请码格式实时校验（非必填字段严格校验会劝退用户）。
- 不做后端账号体系下的「已注册用户补绑」逻辑（prototype 无后端）。
- 不改动现有 `invite.html`（邀请有礼主页面）。
- 不改动奖励规则、阶梯奖励、创作币流水等已有逻辑。

## 设计

### 1. UI 改动（`login.html`）

**位置**：注册表单的「确认密码」段之后、「注册」按钮之前。PC 与移动端都要新增。

**PC 端**（在 `<div class="auth-form" id="pc-register">` 内，「确认密码」段之后、注册按钮之前插入）：

```html
<div style="margin-bottom: 24px;">
  <label style="display: block; margin-bottom: 6px; font-size: 14px; color: #262626; font-weight: 500;">
    邀请码 <span style="color: #8c8c8c; font-weight: 400;">（选填）</span>
  </label>
  <input id="pc-invite-code-input" class="mock-input" placeholder="如没有可留空" maxlength="6"
         style="width: 100%; padding: 12px; border: 1px solid #d9d9d9; border-radius: 8px; color: #1a1a1a; text-transform: uppercase;"/>
</div>
```

**移动端**（在 `<div class="auth-form" id="mobile-register">` 内同位置插入，`id="mobile-invite-code-input"`），字号 / padding 略小，与其他移动端字段保持一致（参考「邮箱验证码」段落的 13px / 10px 风格）。

样式要点：

- `text-transform: uppercase`：用户小写输入也能正常显示。
- `maxlength="6"`：与现有 `generateInviteCode()` 生成的 6 位规则一致。
- `(选填)` 灰色标识，避免误以为是必填。

### 2. URL 自动填充逻辑

**入口**：当前 `login.html` 内联 `<script>`（DOMContentLoaded 监听器）。

**现有行为**：仅展示 banner 并写入 `localStorage.aichuangzuo_invite_ref`。

**扩展**：在原有 `if (ref)` 分支末尾追加：

```js
var pcInput = document.getElementById('pc-invite-code-input');
var mobileInput = document.getElementById('mobile-invite-code-input');
if (pcInput) pcInput.value = ref.toUpperCase();
if (mobileInput) mobileInput.value = ref.toUpperCase();
bindInviteCodeSync();
```

**双视图同步**：由于 PC 和移动端 mockup 在同一页面共存，新增 `bindInviteCodeSync()` 让两侧输入框双向跟随：

```js
function bindInviteCodeSync() {
  var pcInput = document.getElementById('pc-invite-code-input');
  var mobileInput = document.getElementById('mobile-invite-code-input');
  if (!pcInput || !mobileInput) return;
  function sync(from, to) {
    return function() { to.value = from.value.toUpperCase(); };
  }
  pcInput.addEventListener('input', sync(pcInput, mobileInput));
  mobileInput.addEventListener('input', sync(mobileInput, pcInput));
}
```

**残留 ref 处理**：URL 不带 `?ref=` 但 `localStorage.aichuangzuo_invite_ref` 仍残留时（如刷新页面），banner 由 `showInviteBannerIfRef()` 处理；输入框保持为空，让用户决定是否手动填写，避免把无关 ref 自动填到无关用户头上。

### 3. 防自邀请校验

**触发时机**：用户点击「注册」按钮的瞬间。

**实现位置**：`shared.js` 中的 `simulateAuth` 函数。

**新增工具函数**（与现有 `getInviteCode()` 等保持模块风格，挂在 IIFE 内）：

```js
function getRegisterInviteCode() {
  var el = document.getElementById('pc-invite-code-input')
        || document.getElementById('mobile-invite-code-input');
  return el ? el.value.trim().toUpperCase() : '';
}
```

**`simulateAuth` 改造**（保持原函数签名，只在 register 分支前置校验）：

```js
function simulateAuth(type) {
  if (type === 'register') {
    var inputCode = getRegisterInviteCode();
    var selfCode = (typeof getInviteCode === 'function') ? getInviteCode() : '';
    if (inputCode && selfCode && inputCode === selfCode) {
      showToast('不能填写自己的邀请码');
      return;
    }
    // 手动输入框是唯一的真值；空字符串表示明确不填，清除残留 ref
    if (inputCode) {
      localStorage.setItem('aichuangzuo_invite_ref', inputCode);
    } else {
      localStorage.removeItem('aichuangzuo_invite_ref');
    }
    isLoggedIn = true;
    sessionStorage.setItem('isLoggedIn', 'true');
    awardNewUserCoins();
    location.href = 'create.html';
    return;
  }
  isLoggedIn = true;
  sessionStorage.setItem('isLoggedIn', 'true');
  location.href = 'create.html';
}
```

**关键点**：

1. **手动优先**：用户在输入框中手动填的码覆盖 URL 传入的 ref。
2. **空值 OK**：输入框为空时跳过自邀请校验；同步 `localStorage` 时显式 `removeItem` 清除 URL 阶段写入的残留 ref，保证「手动输入框是唯一真值」语义。
3. **Toast 复用**：`shared.js` 已有 `showToast()`，无需新增。
4. **顺序保证**：必须先做自邀请校验，再同步 `localStorage`，否则会把自邀请码错误写入。

### 4. 数据流

```
用户访问 /login.html?ref=ABC123
   ↓
DOMContentLoaded 触发
   ↓
读取 URL 的 ref，写入 localStorage.aichuangzuo_invite_ref
   ↓
显示 invite-banner（PC + mobile）
   ↓
自动填入两个邀请码输入框
   ↓
绑定 PC ↔ mobile 输入框双向同步
   ↓
用户在任一输入框编辑（可能清空、修改、保留）
   ↓
点击「注册」→ simulateAuth('register')
   ↓
读取输入框当前值（手动优先）
   ↓
若等于自己的邀请码 → toast 阻止
   ↓
否则按输入框值同步 localStorage：有值则写入，空则清除（输入框是唯一真值）
   ↓
awardNewUserCoins() 读取 localStorage 发币 / 不发币
   ↓
跳转 create.html
```

## 边界情况与风险

| 场景 | 处理方案 |
|---|---|
| URL 不带 ref、用户不填邀请码 | 正常注册流程，不发 5 创作币 |
| URL 带 ref、用户手动改成另一个码 | 手动码覆盖，写入 localStorage，发币时使用手动码 |
| URL 带 ref、用户清空输入框 | 手动输入框视为明确「不填邀请码」；`simulateAuth` 显式 `removeItem` 清除残留 ref，本次注册不会发币（符合 spec 中「以手动填写为准」） |
| URL 带 ref、用户填自己的码 | toast 拦截，不允许提交 |
| 已注册用户回访 login 页重新填邀请码 | prototype 无后端账号体系，本设计不覆盖此场景 |
| 用户在 PC 输入、在移动端视图看 | 双向同步保证一致 |
| `localStorage` 中残留旧 ref | banner 显示但输入框留空，让用户决定 |
| 邀请码大小写不一致 | `toUpperCase()` 统一比较 |

## 验证要点

### 手动验证场景

| # | 场景 | 预期结果 |
|---|---|---|
| 1 | 直接打开 `login.html`，切换到注册 tab | 「邀请码（选填）」输入框为空，banner 不显示 |
| 2 | 打开 `login.html?ref=ABC123`，切换到注册 tab | 输入框预填 `ABC123`，banner 显示「你收到了好友的邀请…」 |
| 3 | 场景 2 后，在 PC 视图输入框修改为 `XYZ999`，查看移动端 | 移动端输入框同步显示 `XYZ999` |
| 4 | 不带 ref 打开 `login.html`，手动填 `ABC123`，点注册 | 进入创作页；`localStorage.aichuangzuo_invite_ref = "ABC123"` |
| 5 | 已经登录过的浏览器（`aichuangzuo_invite_code = K7P2QX`），注册输入框填 `K7P2QX`，点注册 | 弹 toast「不能填写自己的邀请码」，不跳转 |
| 6 | 已经登录过的浏览器，注册输入框留空，点注册 | 正常进入创作页，不发 5 创作币 |
| 7 | 已经登录过的浏览器，URL 带 `?ref=ZZZZZZ`（与自己的码不同），点注册 | 正常进入创作页，发 5 创作币 |

### Playwright 脚本

参考现有 `tests/e2e/verify_*.py` 风格，新增 `tests/e2e/verify_register_invite.py`，覆盖场景 1、2、3、5、7，截图保存到 `tests/e2e/screenshots/`。

### 回归检查

- `simulateAuth('login')` 不受影响。
- `invite.html`（邀请有礼主页面）的链接生成、海报、提现逻辑不受影响。
- `awardNewUserCoins()` 仅依赖 `localStorage.aichuangzuo_invite_ref` 的存在性，行为不变。
- `simulateInviteRegister()`（模拟好友注册）的奖励链路不受影响。
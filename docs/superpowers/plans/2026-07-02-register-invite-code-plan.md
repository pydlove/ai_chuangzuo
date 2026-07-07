# 注册页邀请码实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `login.html` 注册表单新增可选邀请码输入字段；URL `?ref=` 自动填充；手动填写优先；防自邀请校验。

**Architecture:** 纯前端改动。`login.html` 加 UI + 内联脚本扩展；`shared.js` 在 `simulateAuth('register')` 前置自邀请校验并把输入框作为唯一真值同步到 `localStorage`；Playwright 脚本覆盖关键场景。

**Tech Stack:** 原生 HTML / CSS / JavaScript（无构建），Playwright（Python，e2e 验证）。

**Spec:** `docs/superpowers/specs/2026-07-02-register-invite-code-design.md`

## Global Constraints

- 原型纯前端，不引入构建系统、不引入后端。
- 品牌色 `#07c160`；圆角 8px；输入框 1px `#d9d9d9` 边框——与现有表单字段完全一致。
- PC 端字号 14px / padding 12px；移动端字号 13px / padding 10px（参考「邮箱验证码」段落）。
- `localStorage` key 沿用现有：`aichuangzuo_invite_ref`、`aichuangzuo_invite_code`、`aichuangzuo_coin_balance`。
- 邀请码生成规则：6 位 `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`（去除易混淆字符 I、L、O、0、1），与 `shared.js` 中 `generateInviteCode()` 一致。

---

## Task 1: 在 PC / 移动端注册表单新增邀请码输入字段

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/login.html:96-98`（PC 端注册表单「确认密码」之后）

**背景:** 当前 PC 端注册表单在「确认密码」之后直接是「注册」按钮，移动端同位置（line 161）。需要在这两处分别插入「邀请码（选填）」输入框。

- [ ] **Step 1: 在 PC 端注册表单插入邀请码输入字段**

打开 `.superpowers/brainstorm/6491-1782131242/content/login.html`，定位到 `id="pc-register"` 的 `<div>` 内部，「确认密码」段（`<input class="mock-input" placeholder="再次输入密码"... type="password"/>`）之后、`<button class="mock-button" onclick="simulateAuth('register')"...>注册</button>` 之前，插入：

```html
<div style="margin-bottom: 24px;">
  <label style="display: block; margin-bottom: 6px; font-size: 14px; color: #262626; font-weight: 500;">
    邀请码 <span style="color: #8c8c8c; font-weight: 400;">（选填）</span>
  </label>
  <input id="pc-invite-code-input" class="mock-input" placeholder="如没有可留空" maxlength="6"
         style="width: 100%; padding: 12px; border: 1px solid #d9d9d9; border-radius: 8px; color: #1a1a1a; text-transform: uppercase;"/>
</div>
```

- [ ] **Step 2: 在移动端注册表单插入邀请码输入字段**

定位到 `id="mobile-register"` 的 `<div>` 内部，「确认密码」段之后、注册按钮之前，插入：

```html
<div style="margin-bottom: 20px;">
  <label style="display: block; margin-bottom: 6px; font-size: 13px; color: #262626; font-weight: 500;">
    邀请码 <span style="color: #8c8c8c; font-weight: 400;">（选填）</span>
  </label>
  <input id="mobile-invite-code-input" class="mock-input" placeholder="如没有可留空" maxlength="6"
         style="width: 100%; padding: 10px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; color: #1a1a1a; text-transform: uppercase;"/>
</div>
```

注意移动端用 `font-size: 13px`（label）、`padding: 10px`、`border-radius: 6px`、`font-size: 14px`（input），与同表单其他字段一致。

- [ ] **Step 3: 手动视觉验证**

启动本地服务器（如已运行则跳过）：
```bash
./scripts/local/start.sh
```

浏览器打开 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/login.html`，点击「注册」tab。预期：
- PC 端视图：「确认密码」字段下方出现「邀请码（选填）」输入框，灰色 `(选填)` 标识。
- 移动端视图：同样位置出现输入框，字号略小。

打开 DevTools Console 执行：
```js
document.getElementById('pc-invite-code-input') !== null &&
document.getElementById('mobile-invite-code-input') !== null
```
预期：返回 `true`。

- [ ] **Step 4: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/login.html
git commit -m "feat(login): 注册表单新增邀请码输入字段"
```

---

## Task 2: URL 自动填充与 PC/移动端输入框双向同步

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/login.html:170-184`（DOMContentLoaded 监听器）

**背景:** 当前 DOMContentLoaded 监听器读取 URL `?ref=` 后只展示 banner 并写入 localStorage。需扩展为：写入输入框 + 绑定 PC ↔ mobile 双向同步。

- [ ] **Step 1: 扩展 DOMContentLoaded 监听器**

打开 `login.html`，定位到内联 `<script>` 中的 DOMContentLoaded 监听器（在 `<script src="shared.js?v=9"></script>` 之后）。当前实现：

```js
document.addEventListener('DOMContentLoaded', function() {
  if (typeof updateNotificationBadge === 'function') {
    updateNotificationBadge();
  }
  var params = new URLSearchParams(location.search);
  var ref = params.get('ref');
  if (ref) {
    var pc = document.getElementById('pc-invite-banner');
    var mobile = document.getElementById('mobile-invite-banner');
    if (pc) pc.style.display = 'flex';
    if (mobile) mobile.style.display = 'flex';
    localStorage.setItem('aichuangzuo_invite_ref', ref);
  }
});
```

在 `if (ref) { ... }` 块内部、`localStorage.setItem(...)` 之后追加：

```js
    var pcInput = document.getElementById('pc-invite-code-input');
    var mobileInput = document.getElementById('mobile-invite-code-input');
    if (pcInput) pcInput.value = ref.toUpperCase();
    if (mobileInput) mobileInput.value = ref.toUpperCase();
    bindInviteCodeSync();
```

并在监听器外、内联 `<script>` 标签内（同级）新增 helper：

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

最终内联 `<script>` 完整内容应大致为：

```js
<script>
  document.addEventListener('DOMContentLoaded', function() {
    if (typeof updateNotificationBadge === 'function') {
      updateNotificationBadge();
    }
    var params = new URLSearchParams(location.search);
    var ref = params.get('ref');
    if (ref) {
      var pc = document.getElementById('pc-invite-banner');
      var mobile = document.getElementById('mobile-invite-banner');
      if (pc) pc.style.display = 'flex';
      if (mobile) mobile.style.display = 'flex';
      localStorage.setItem('aichuangzuo_invite_ref', ref);
      var pcInput = document.getElementById('pc-invite-code-input');
      var mobileInput = document.getElementById('mobile-invite-code-input');
      if (pcInput) pcInput.value = ref.toUpperCase();
      if (mobileInput) mobileInput.value = ref.toUpperCase();
      bindInviteCodeSync();
    } else {
      bindInviteCodeSync();
    }
  });

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
</script>
```

> 注：`else` 分支仍调用 `bindInviteCodeSync()`，让不带 `?ref=` 的访问也能享受 PC ↔ mobile 双向同步（例如用户在 PC 输入，移动端 mockup 同时更新）。

- [ ] **Step 2: 手动视觉验证（场景 2）**

浏览器打开：
```
http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/login.html?ref=ABC123
```

点击「注册」tab。预期：
- PC 端输入框值：`ABC123`
- 移动端输入框值：`ABC123`
- 顶部 invite-banner 显示。

DevTools Console 验证：
```js
document.getElementById('pc-invite-code-input').value === 'ABC123' &&
document.getElementById('mobile-invite-code-input').value === 'ABC123'
```
预期：`true`。

- [ ] **Step 3: 手动视觉验证（场景 3 — 双向同步）**

在 PC 端输入框追加输入 `XYZ`，预期移动端输入框实时显示 `ABC123XYZ`。反之亦然。

- [ ] **Step 4: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/login.html
git commit -m "feat(login): 邀请码 URL 自动填充与 PC/移动端同步"
```

---

## Task 3: shared.js 防自邀请校验 + 输入框为唯一真值

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js:17-24`（`simulateAuth` 函数）
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js:4987-5016`（邀请 IIFE 末尾）

**背景:** 当前 `simulateAuth(type)` 在 register 分支只调用 `awardNewUserCoins()`，没有读取输入框。需新增 `getRegisterInviteCode()` helper，改造 `simulateAuth`：
1. 读取输入框值（手动优先）。
2. 与自己的邀请码比对，相同则 toast 拦截。
3. 把输入框值同步到 `localStorage.aichuangzuo_invite_ref`（空字符串则 removeItem）。

- [ ] **Step 1: 新增 `getRegisterInviteCode` helper**

打开 `shared.js`，定位到 line 4987 附近（`window.awardNewUserCoins = function() {...}` 之后的 `})();` 之前），插入：

```js
    window.getRegisterInviteCode = function() {
      var el = document.getElementById('pc-invite-code-input')
            || document.getElementById('mobile-invite-code-input');
      return el ? el.value.trim().toUpperCase() : '';
    };
```

> 注：与 IIFE 内 `getInviteCode`、`awardNewUserCoins` 等风格一致，挂在 `window` 上。

- [ ] **Step 2: 改造 `simulateAuth` 函数**

定位到 line 17：

```js
function simulateAuth(type) {
  isLoggedIn = true;
  sessionStorage.setItem('isLoggedIn', 'true');
  if (type === 'register') {
    awardNewUserCoins();
  }
  location.href='create.html';
}
```

替换为：

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

- [ ] **Step 3: 手动视觉验证（场景 5 — 自邀请拦截）**

浏览器打开 `login.html`（无 ref），先访问 `invite.html` 让浏览器生成自己的邀请码（首次访问会触发 `getInviteCode()`，写入 `localStorage.aichuangzuo_invite_code`）。记下显示的 6 位码（如 `K7P2QX`）。

回到 `login.html`，点击「注册」tab。在邀请码输入框填入刚才记下的码。点击「注册」。

预期：
- 弹出 toast「不能填写自己的邀请码」。
- URL 不跳转（仍停留在 `login.html`）。

- [ ] **Step 4: 手动视觉验证（场景 4 — 手动覆盖生效）**

无 `?ref=` 打开 `login.html`，点击「注册」tab。在邀请码输入框填 `ABC123`，点击「注册」。

预期：跳转到 `create.html`。DevTools Console 验证：
```js
localStorage.getItem('aichuangzuo_invite_ref') === 'ABC123'
```
预期：`true`。

- [ ] **Step 5: 手动视觉验证（场景 6 — 空值不污染 localStorage）**

清空 localStorage 后访问 `invite.html`，记下自己的邀请码（假设为 `K7P2QX`）。回到 `login.html`，注册邀请码输入框留空，点击「注册」。

预期：跳转到 `create.html`。DevTools Console 验证：
```js
localStorage.getItem('aichuangzuo_invite_ref')
```
预期：`null`（注册时被显式 `removeItem` 清除）。

- [ ] **Step 6: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(login): 注册邀请码自邀请校验与输入框为唯一真值"
```

---

## Task 4: Playwright 验证脚本

**Files:**
- Create: `tests/e2e/verify_register_invite.py`

**背景:** 项目使用 ad-hoc Playwright 脚本做端到端验证，参考 `tests/e2e/verify_redeem_code.py` 的写法。

- [ ] **Step 1: 创建 Playwright 脚本**

创建 `tests/e2e/verify_register_invite.py`：

```python
#!/usr/bin/env python3
"""注册页邀请码端到端验证。

覆盖场景：
  1) 无 ref 直接打开 → 输入框为空，banner 不显示
  2) 带 ref=ABC123 打开 → 输入框预填，banner 显示
  3) PC 端编辑输入框 → 移动端同步
  5) 已有自己邀请码的情况下填同样的码 → toast 拦截
  7) URL 带与自己不同的 ref → 正常注册
"""
import os
import sys
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/login.html"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def open_login(page, ref=None):
    url = BASE_URL + (f"?ref={ref}" if ref else "")
    page.goto(url)
    # 等待 DOMContentLoaded 脚本执行完毕
    page.wait_for_load_state("domcontentloaded")
    page.wait_for_timeout(300)


def switch_to_register(page, which="pc"):
    selector = f"#{which}-register"
    # 切换 register tab
    page.locator(f".auth-tab", has_text="注册").first.click()
    page.wait_for_timeout(200)


def main():
    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        try:
            # ========== 场景 1: 无 ref 直接打开 ==========
            ctx = browser.new_context()
            page = ctx.new_page()
            open_login(page)
            switch_to_register(page, "pc")
            switch_to_register(page, "mobile")
            pc_val = page.locator("#pc-invite-code-input").input_value()
            mobile_val = page.locator("#mobile-invite-code-input").input_value()
            banner_visible = page.locator("#pc-invite-banner").is_visible()
            ok = pc_val == "" and mobile_val == "" and not banner_visible
            results.append(("场景1 无ref输入框为空+banner隐藏", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_no_ref.png"))
            ctx.close()

            # ========== 场景 2: 带 ref=ABC123 ==========
            ctx = browser.new_context()
            page = ctx.new_page()
            open_login(page, ref="ABC123")
            switch_to_register(page, "pc")
            pc_val = page.locator("#pc-invite-code-input").input_value()
            mobile_val = page.locator("#mobile-invite-code-input").input_value()
            banner_visible = page.locator("#pc-invite-banner").is_visible()
            ok = pc_val == "ABC123" and mobile_val == "ABC123" and banner_visible
            results.append(("场景2 ref自动填充+banner显示", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_auto_fill.png"))
            ctx.close()

            # ========== 场景 3: PC ↔ mobile 双向同步 ==========
            ctx = browser.new_context()
            page = ctx.new_page()
            open_login(page, ref="ABC123")
            switch_to_register(page, "pc")
            pc_input = page.locator("#pc-invite-code-input")
            pc_input.fill("ABC123XYZ")
            page.wait_for_timeout(100)
            mobile_val = page.locator("#mobile-invite-code-input").input_value()
            ok = mobile_val == "ABC123XYZ"
            results.append(("场景3 PC→mobile同步", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_sync.png"))
            ctx.close()

            # ========== 场景 5: 自邀请拦截 ==========
            # 先访问 invite.html 触发 getInviteCode() 生成自己码
            ctx = browser.new_context()
            page = ctx.new_page()
            page.goto(BASE_URL.rsplit("/", 1)[0] + "/invite.html")
            page.wait_for_load_state("domcontentloaded")
            page.wait_for_timeout(300)
            self_code = page.evaluate("() => localStorage.getItem('aichuangzuo_invite_code')")
            assert self_code, "自己的邀请码未生成"

            # 回 login.html 填自己的码
            open_login(page)
            switch_to_register(page, "pc")
            page.locator("#pc-invite-code-input").fill(self_code)
            # 点击注册
            page.locator("#pc-register button.mock-button").click()
            page.wait_for_timeout(500)
            # 应弹 toast，未跳转
            toast_visible = page.locator("text=不能填写自己的邀请码").is_visible()
            current_url = page.url
            ok = toast_visible and "login.html" in current_url
            results.append(("场景5 自邀请拦截", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_self_block.png"))
            ctx.close()

            # ========== 场景 7: 带与自身不同的 ref 正常注册 ==========
            ctx = browser.new_context()
            page = ctx.new_page()
            # 先生成自己的码
            page.goto(BASE_URL.rsplit("/", 1)[0] + "/invite.html")
            page.wait_for_load_state("domcontentloaded")
            page.wait_for_timeout(300)
            self_code = page.evaluate("() => localStorage.getItem('aichuangzuo_invite_code')")
            other_ref = "ZZZZZZ" if self_code != "ZZZZZZ" else "YYYYYY"

            open_login(page, ref=other_ref)
            switch_to_register(page, "pc")
            page.locator("#pc-invite-code-input").click()  # 触发 input 事件以确保值就位
            page.locator("#pc-register button.mock-button").click()
            page.wait_for_timeout(800)
            current_url = page.url
            final_ref = page.evaluate("() => localStorage.getItem('aichuangzuo_invite_ref')")
            ok = "create.html" in current_url and final_ref == other_ref
            results.append(("场景7 外部ref正常注册", ok))
            page.screenshot(path=str(SCREENSHOT_DIR / "register_invite_normal.png"))
            ctx.close()

        finally:
            browser.close()

    # 输出结果
    print("\n=== 验证结果 ===")
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

- [ ] **Step 2: 启动原型服务器**

```bash
./scripts/local/start.sh
```

如已运行则跳过。预期：服务器监听 `http://localhost:28585`。

- [ ] **Step 3: 运行 Playwright 验证脚本**

```bash
python3 tests/e2e/verify_register_invite.py
```

预期：所有场景输出 `✓ PASS`，退出码 `0`。

如失败，按失败场景对照 spec 验证要点定位问题：
- 场景 1 失败 → 检查 Task 1 步骤 2 / 步骤 3 是否正确插入输入框
- 场景 2 失败 → 检查 Task 2 步骤 1 的 DOMContentLoaded 扩展
- 场景 3 失败 → 检查 Task 2 步骤 1 的 `bindInviteCodeSync()` 是否绑定
- 场景 5 失败 → 检查 Task 3 步骤 2 的 `simulateAuth` 改造
- 场景 7 失败 → 检查 Task 3 步骤 2 中 `localStorage.setItem` 调用

- [ ] **Step 4: 检查截图**

打开 `tests/e2e/screenshots/register_invite_*.png`：
- `register_invite_no_ref.png`：邀请码输入框为空，无 banner。
- `register_invite_auto_fill.png`：两个输入框都是 `ABC123`，顶部 banner 显示。
- `register_invite_sync.png`：两个输入框都是 `ABC123XYZ`。
- `register_invite_self_block.png`：toast「不能填写自己的邀请码」可见，未跳转。
- `register_invite_normal.png`：跳转后落在 `create.html`（或在过渡画面）。

- [ ] **Step 5: 提交**

```bash
git add tests/e2e/verify_register_invite.py tests/e2e/screenshots/register_invite_*.png
git commit -m "test(e2e): 注册页邀请码端到端验证"
```

---

## 回归检查

完成后跑一遍现有邀请流程，确认未破坏：
- 手动访问 `invite.html`：自己的邀请码、链接、海报照常显示。
- `tests/e2e/` 现有邀请相关脚本（如有）继续通过。
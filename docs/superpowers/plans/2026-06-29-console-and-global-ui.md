# 控制台与全局 UI 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增控制台页面并统一全站 Footer、关于我们弹框、黑夜/白天模式切换。

**Architecture:** 使用 CSS 变量管理主题色，`shared.js` 提供全局 Footer、关于我们、反馈、教程、会员、消息弹框函数；新增 `console.html` 作为控制台，包含左侧边栏和顶部 Header；所有页面通过 `shared.js` 自动注入 Footer。

**Tech Stack:** 纯前端 HTML/CSS/JS，无构建工具，使用 `localStorage` 持久化主题和消息数据。

## Global Constraints

- 保持原型前端-only，不引入后端或构建步骤。
- 首页 / 会员页保持宣传网站形式不变，仅加 Footer。
- 全站支持黑夜 / 白天模式，状态持久化到 `localStorage`。
- 全站可打开「关于我们」弹框。
- 不改造现有功能页为 SPA 或嵌入 iframe。
- 不实现真实的反馈提交后端、微信扫码、拨打电话等交互。

---

## File Structure

- **Create**
  - `.superpowers/brainstorm/6491-1782131242/content/console.html` — 控制台页面
  - `tests/e2e/verify_console.py` — 控制台 e2e 验证脚本
- **Modify**
  - `.superpowers/brainstorm/6491-1782131242/content/shared.css` — CSS 变量、弹框样式、控制台布局、Footer 样式
  - `.superpowers/brainstorm/6491-1782131242/content/shared.js` — 主题切换、全局 Footer 注入、各类弹框函数
  - `.superpowers/brainstorm/6491-1782131242/content/index.html` — 确保加载 shared.js 后自动注入 Footer
  - `.superpowers/brainstorm/6491-1782131242/content/pricing.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/login.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/forgot.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/create.html` — 同上（已有内部铃铛，无需改动）
  - `.superpowers/brainstorm/6491-1782131242/content/loading.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/preview.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/works.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/settings.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/order.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/payment.html` — 同上
  - `.superpowers/brainstorm/6491-1782131242/content/notifications.html` — 更新为消息弹框的「查看全部」落地页（可选）

---

### Task 1: CSS theme variables and dark mode base

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Produces CSS custom properties:
  - `--bg-color`
  - `--text-color`
  - `--text-secondary`
  - `--border-color`
  - `--card-bg`
  - `--header-bg`
  - `--sidebar-bg`
  - `--sidebar-hover`
  - `--primary-color` (#07c160)
- Produces dark overrides under `body[data-theme="dark"]`

- [ ] **Step 1: Add CSS variables to shared.css**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.css` 文件最开头（或紧跟现有重置样式后）添加：

```css
:root {
  --bg-color: #f8f9fa;
  --text-color: #1a1a1a;
  --text-secondary: #595959;
  --border-color: #eee;
  --card-bg: #ffffff;
  --header-bg: #ffffff;
  --sidebar-bg: #ffffff;
  --sidebar-hover: #f6ffed;
  --primary-color: #07c160;
}

body[data-theme="dark"] {
  --bg-color: #141414;
  --text-color: #e0e0e0;
  --text-secondary: #a6a6a6;
  --border-color: #303030;
  --card-bg: #1f1f1f;
  --header-bg: #1f1f1f;
  --sidebar-bg: #1f1f1f;
  --sidebar-hover: #1a2e1a;
  --primary-color: #10b981;
}
```

- [ ] **Step 2: Apply variables to body**

在同一文件中找到 `body` 样式，如果没有则添加：

```css
body {
  background-color: var(--bg-color);
  color: var(--text-color);
}
```

- [ ] **Step 3: Verify in browser**

启动服务器：

```bash
./scripts/local/start.sh
```

访问 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/index.html`，打开控制台，手动给 `body` 加 `data-theme="dark"`，确认背景色变深。

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(theme): add CSS variables and dark mode base"
```

---

### Task 2: Theme toggle API in shared.js

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Produces:
  - `getTheme()` → `'light' | 'dark'`
  - `setTheme(theme)` → void
  - `toggleTheme()` → void
  - `initTheme()` → void

- [ ] **Step 1: Append theme API near notification API**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.js` 的通知 API 区域后添加：

```javascript
  // ===================== 主题切换 =====================
  var THEME_KEY = 'aichuangzuo_theme';

  function getTheme() {
    return localStorage.getItem(THEME_KEY) || 'light';
  }

  function setTheme(theme) {
    if (theme !== 'light' && theme !== 'dark') return;
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem(THEME_KEY, theme);
  }

  function toggleTheme() {
    var next = getTheme() === 'light' ? 'dark' : 'light';
    setTheme(next);
  }

  function initTheme() {
    setTheme(getTheme());
  }
```

- [ ] **Step 2: Call initTheme on DOMContentLoaded**

找到 `document.addEventListener('DOMContentLoaded', function() { ... });`，在末尾添加：

```javascript
    initTheme();
```

- [ ] **Step 3: Verify with Playwright**

```bash
python3 - <<'PY'
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto(f"{BASE_URL}/index.html")
    page.evaluate("""() => { localStorage.removeItem('aichuangzuo_theme'); }""")
    page.reload()
    theme = page.evaluate("""() => { return document.body.getAttribute('data-theme'); }""")
    assert theme == 'light', f"Expected light, got {theme}"
    page.evaluate("""() => { toggleTheme(); }""")
    theme = page.evaluate("""() => { return document.body.getAttribute('data-theme'); }""")
    assert theme == 'dark', f"Expected dark, got {theme}"
    stored = page.evaluate("""() => { return localStorage.getItem('aichuangzuo_theme'); }""")
    assert stored == 'dark', f"Expected dark in storage, got {stored}"
    browser.close()
    print("Theme API verification passed.")
PY
```

Expected output: `Theme API verification passed.`

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(theme): add theme toggle API"
```

---

### Task 3: Global Footer

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Produces:
  - `renderGlobalFooter()` → void

- [ ] **Step 1: Add footer styles to shared.css**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.css` 文件末尾追加：

```css
/* 全局 Footer */
.global-footer {
  background: var(--card-bg);
  border-top: 1px solid var(--border-color);
  padding: 16px 24px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
}
.global-footer a {
  color: var(--primary-color);
  cursor: pointer;
  text-decoration: none;
}
.global-footer a:hover {
  text-decoration: underline;
}
```

- [ ] **Step 2: Add footer render function to shared.js**

在主题 API 区域后添加：

```javascript
  // ===================== 全局 Footer =====================
  function renderGlobalFooter() {
    if (document.querySelector('.global-footer')) return;
    var footer = document.createElement('div');
    footer.className = 'global-footer';
    footer.innerHTML = '&copy; 2026 爱创作 · All Rights Reserved | 备案号：京ICP备XXXXXXXX号 | <a onclick="openAboutModal()">关于我们</a>';
    document.body.appendChild(footer);
  }
```

- [ ] **Step 3: Call renderGlobalFooter on DOMContentLoaded**

在 `DOMContentLoaded` 监听器末尾添加：

```javascript
    renderGlobalFooter();
```

- [ ] **Step 4: Verify with Playwright**

```bash
python3 - <<'PY'
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto(f"{BASE_URL}/index.html")
    footer = page.locator(".global-footer").first
    assert footer.is_visible(), "Footer not rendered"
    text = footer.text_content()
    assert "爱创作" in text, f"Footer text missing 爱创作: {text}"
    browser.close()
    print("Footer verification passed.")
PY
```

- [ ] **Step 5: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js \
        .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(ui): add global footer to all pages"
```

---

### Task 4: About us modal

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Produces:
  - `openAboutModal()` → void
  - `closeAboutModal()` → void

- [ ] **Step 1: Add modal styles to shared.css**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.css` 末尾追加：

```css
/* 通用弹框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10020;
  padding: 24px;
}
.modal-content {
  background: var(--card-bg);
  border-radius: 12px;
  max-width: 560px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}
.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-color);
}
.modal-close {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
}
.modal-body {
  padding: 20px;
  color: var(--text-color);
  line-height: 1.7;
  font-size: 14px;
}
.modal-footer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
}
.modal-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  color: var(--text-color);
  font-size: 13px;
  cursor: pointer;
}
.modal-action-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}
```

- [ ] **Step 2: Add openAboutModal to shared.js**

在 footer 区域后添加：

```javascript
  // ===================== 关于我们弹框 =====================
  function openAboutModal() {
    if (document.getElementById('about-modal')) return;
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'about-modal';
    overlay.innerHTML = '<div class="modal-content" onclick="event.stopPropagation()">' +
      '<div class="modal-header"><div class="modal-title">关于我们</div><button class="modal-close" onclick="closeAboutModal()">×</button></div>' +
      '<div class="modal-body">' +
        '<div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">' +
          '<div style="width:48px;height:48px;border-radius:50%;background:#07c160;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700;font-size:20px;">爱</div>' +
          '<div><div style="font-weight:700;font-size:16px;">爱创作</div><div style="color:var(--text-secondary);font-size:13px;">创作者灵感旅程中的同行者</div></div>' +
        '</div>' +
        '<p>爱创作希望成为创作者灵感旅程中的同行者。我们希望让写作不再被“文笔”所限制，哪怕不擅长表达的人，也能把脑海里的想法顺利写出来。AI 在这里不是替代者，而是帮助作者整理思路、激发灵感、拓展想象的辅助工具。</p>' +
        '<p>我们珍惜每一位作者投入在作品里的情绪、时间与热爱，也尊重原创应有的价值。对于利用 AI 进行搬运、洗稿、恶意拼接内容等行为，爱创作强烈反对。技术的发展不该消解创作，而应让真正的创意被更多人看见。</p>' +
        '<p>平台中的 AI 生成结果，仅作为创作过程中的参考与辅助内容，相关输出由模型自动生成，并不代表平台立场或价值观点。</p>' +
      '</div>' +
      '<div class="modal-footer-actions">' +
        '<button class="modal-action-btn" onclick="showToast('用户协议占位')">📄 用户协议</button>' +
        '<button class="modal-action-btn" onclick="showToast('隐私政策占位')">🛡️ 隐私政策</button>' +
        '<button class="modal-action-btn" onclick="showToast('关注微信占位')">💬 关注微信</button>' +
        '<button class="modal-action-btn" onclick="showToast('联系电话占位')">📞 联系电话</button>' +
        '<button class="modal-action-btn" onclick="showToast('举报占位')">🚩 举报</button>' +
      '</div>' +
      '<div style="text-align:center;padding-bottom:16px;color:var(--text-secondary);font-size:12px;">&copy; 2026 爱创作 · All Rights Reserved</div>' +
    '</div>';
    overlay.onclick = closeAboutModal;
    document.body.appendChild(overlay);
  }

  function closeAboutModal() {
    var el = document.getElementById('about-modal');
    if (el) el.remove();
  }
```

- [ ] **Step 3: Verify with Playwright**

```bash
python3 - <<'PY'
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto(f"{BASE_URL}/index.html")
    page.evaluate("""() => { openAboutModal(); }""")
    modal = page.locator("#about-modal").first
    assert modal.is_visible(), "About modal not visible"
    title = page.locator("#about-modal .modal-title").text_content()
    assert title == "关于我们", f"Unexpected title: {title}"
    browser.close()
    print("About modal verification passed.")
PY
```

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js \
        .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(ui): add about us modal"
```

---

### Task 5: Console page skeleton

**Files:**
- Create: `.superpowers/brainstorm/6491-1782131242/content/console.html`

**Interfaces:**
- Produces: `console.html` with sidebar + main content area

- [ ] **Step 1: Create console.html**

创建 `.superpowers/brainstorm/6491-1782131242/content/console.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>控制台 - 爱创作</title>
<link rel="stylesheet" href="shared.css">
<style>
  body { margin: 0; min-height: 100vh; }
</style>
</head>
<body>
<div class="console-layout">
  <!-- 侧边栏 -->
  <aside class="console-sidebar">
    <div class="console-sidebar-brand">爱创作</div>
    <nav class="console-sidebar-nav">
      <a class="console-sidebar-item active" href="create.html" data-key="create">
        <span>📝</span> 创作
      </a>
      <a class="console-sidebar-item" href="loading.html" data-key="queue">
        <span>⏳</span> 生成队列
      </a>
      <a class="console-sidebar-item" href="preview.html" data-key="preview">
        <span>👁</span> 预览导出
      </a>
      <a class="console-sidebar-item" href="works.html" data-key="works">
        <span>📚</span> 我的作品
      </a>
    </nav>
  </aside>

  <!-- 主区域 -->
  <div class="console-main">
    <header class="console-header">
      <div class="console-header-left">
        <div class="console-avatar" onclick="toggleUserMenu()">U</div>
        <div class="console-membership-badge" onclick="openMembershipModal()">会员</div>
        <div class="console-user-menu" id="console-user-menu">
          <a href="settings.html">个人中心</a>
          <a href="login.html">退出登录</a>
        </div>
      </div>
      <div class="console-header-center">爱创作</div>
      <div class="console-header-right">
        <button class="console-icon-btn" onclick="openMessageModal()" title="消息">🔔<span class="notification-badge"></span></button>
        <button class="console-icon-btn" onclick="openTutorialModal()" title="教程">📖</button>
        <button class="console-icon-btn" onclick="toggleTheme()" title="切换主题">🌙</button>
        <button class="console-icon-btn" onclick="openFeedbackModal()" title="反馈">💬</button>
        <button class="console-icon-btn" onclick="openAboutModal()" title="关于我们">ℹ️</button>
      </div>
    </header>

    <div class="console-content">
      <div class="console-welcome">
        <h1>欢迎回来 👋</h1>
        <p>从左侧菜单开始创作，或查看生成队列和作品。</p>
        <div class="console-quick-actions">
          <a href="create.html" class="console-btn-primary">开始创作</a>
          <a href="works.html" class="console-btn-secondary">查看作品</a>
        </div>
      </div>
    </div>
  </div>
</div>

<script src="shared.js"></script>
<script>
  function toggleUserMenu() {
    var menu = document.getElementById('console-user-menu');
    menu.classList.toggle('visible');
  }

  document.addEventListener('click', function(e) {
    var menu = document.getElementById('console-user-menu');
    var avatar = document.querySelector('.console-avatar');
    if (menu && avatar && !menu.contains(e.target) && !avatar.contains(e.target)) {
      menu.classList.remove('visible');
    }
  });

  document.addEventListener('DOMContentLoaded', function() {
    initTheme();
    updateNotificationBadge();
  });
</script>
</body>
</html>
```

- [ ] **Step 2: Verify console page loads**

访问 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/console.html`，确认页面显示侧边栏、Header 和欢迎内容。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/console.html
git commit -m "feat(console): add console page skeleton"
```

---

### Task 6: Console layout and header styles

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Produces CSS classes:
  - `.console-layout`
  - `.console-sidebar`
  - `.console-header`
  - `.console-content`
  - `.console-icon-btn`

- [ ] **Step 1: Append console styles to shared.css**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.css` 末尾追加：

```css
/* 控制台布局 */
.console-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-color);
}
.console-sidebar {
  width: 200px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border-color);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
.console-sidebar-brand {
  padding: 16px 20px;
  font-weight: 700;
  font-size: 18px;
  color: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
}
.console-sidebar-nav {
  display: flex;
  flex-direction: column;
  padding: 12px;
  gap: 4px;
}
.console-sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--text-color);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}
.console-sidebar-item:hover {
  background: var(--sidebar-hover);
}
.console-sidebar-item.active {
  background: var(--sidebar-hover);
  color: var(--primary-color);
  font-weight: 600;
}
.console-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.console-header {
  height: 56px;
  background: var(--header-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: relative;
}
.console-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}
.console-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-color);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.console-membership-badge {
  padding: 4px 10px;
  border-radius: 12px;
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
  font-size: 12px;
  cursor: pointer;
}
body[data-theme="dark"] .console-membership-badge {
  background: #2b2111;
  border-color: #594214;
}
.console-user-menu {
  position: absolute;
  top: 44px;
  left: 0;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  display: none;
  flex-direction: column;
  min-width: 120px;
  z-index: 100;
}
.console-user-menu.visible {
  display: flex;
}
.console-user-menu a {
  padding: 10px 14px;
  color: var(--text-color);
  text-decoration: none;
  font-size: 13px;
}
.console-user-menu a:hover {
  background: var(--sidebar-hover);
}
.console-header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-weight: 700;
  font-size: 16px;
  color: var(--text-color);
}
.console-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.console-icon-btn {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-color);
}
.console-icon-btn:hover {
  background: var(--sidebar-hover);
}
.console-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
.console-welcome h1 {
  font-size: 24px;
  margin-bottom: 8px;
  color: var(--text-color);
}
.console-welcome p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}
.console-quick-actions {
  display: flex;
  gap: 12px;
}
.console-btn-primary,
.console-btn-secondary {
  padding: 10px 20px;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}
.console-btn-primary {
  background: var(--primary-color);
  color: #fff;
}
.console-btn-secondary {
  background: var(--card-bg);
  color: var(--text-color);
  border: 1px solid var(--border-color);
}
```

- [ ] **Step 2: Verify console styling**

刷新 `console.html`，确认侧边栏、Header、按钮样式正确。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(console): add console layout and header styles"
```

---

### Task 7: Membership modal

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Produces:
  - `openMembershipModal()` → void
  - `closeMembershipModal()` → void
  - `getMembershipInfo()` → object

- [ ] **Step 1: Add membership modal functions**

在关于我们弹框代码后添加：

```javascript
  // ===================== 会员信息弹框 =====================
  function getMembershipInfo() {
    var defaults = { level: '免费版', expiry: '未开通', quota: '10 / 10 篇' };
    try {
      var raw = localStorage.getItem('aichuangzuo_membership');
      return raw ? JSON.parse(raw) : defaults;
    } catch (e) {
      return defaults;
    }
  }

  function openMembershipModal() {
    if (document.getElementById('membership-modal')) return;
    var info = getMembershipInfo();
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'membership-modal';
    overlay.innerHTML = '<div class="modal-content" style="max-width:360px;" onclick="event.stopPropagation()">' +
      '<div class="modal-header"><div class="modal-title">会员信息</div><button class="modal-close" onclick="closeMembershipModal()">×</button></div>' +
      '<div class="modal-body" style="text-align:center;">' +
        '<div style="font-size:42px;margin-bottom:12px;">👑</div>' +
        '<div style="font-size:18px;font-weight:700;margin-bottom:8px;">' + info.level + '</div>' +
        '<div style="color:var(--text-secondary);font-size:13px;margin-bottom:6px;">到期时间：' + info.expiry + '</div>' +
        '<div style="color:var(--text-secondary);font-size:13px;margin-bottom:20px;">本月剩余额度：' + info.quota + '</div>' +
        '<button onclick="location.href=\'pricing.html\'" style="padding:10px 24px;background:#07c160;color:#fff;border:none;border-radius:8px;font-size:14px;cursor:pointer;">立即续费</button>' +
      '</div>' +
    '</div>';
    overlay.onclick = closeMembershipModal;
    document.body.appendChild(overlay);
  }

  function closeMembershipModal() {
    var el = document.getElementById('membership-modal');
    if (el) el.remove();
  }
```

- [ ] **Step 2: Verify with Playwright**

```bash
python3 - <<'PY'
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto(f"{BASE_URL}/console.html")
    page.evaluate("""() => { openMembershipModal(); }""")
    title = page.locator("#membership-modal .modal-title").text_content()
    assert title == "会员信息", f"Unexpected title: {title}"
    browser.close()
    print("Membership modal verification passed.")
PY
```

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(console): add membership modal"
```

---

### Task 8: Message bell modal

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Consumes: `getNotifications()`, `markCategoryRead()`, `clearCategory()`, `updateNotificationBadge()`
- Produces:
  - `openMessageModal()` → void
  - `closeMessageModal()` → void
  - `renderMessageTab(type)` → void

- [ ] **Step 1: Refactor notification type mapping**

在消息弹框代码前，确保现有通知 API 支持三种分类。在 `.superpowers/brainstorm/6491-1782131242/content/shared.js` 中，把现有 `NOTIFICATION_TYPES` 调整为：

```javascript
  var NOTIFICATION_TYPES = ['announcement', 'message', 'history'];
```

如果之前已经实现了 `generation/membership/feature/promotion` 四类，需要新增一个映射函数，把旧分类映射到新的三类：

```javascript
  function normalizeNotificationType(type) {
    var map = {
      generation: 'history',
      membership: 'message',
      feature: 'announcement',
      promotion: 'announcement'
    };
    return map[type] || type;
  }
```

- [ ] **Step 2: Add message modal functions**

在会员弹框代码后添加：

```javascript
  // ===================== 消息铃铛弹框 =====================
  var MESSAGE_TYPE_LABELS = {
    announcement: '公告',
    message: '站内信',
    history: '历史记录'
  };
  var currentMessageType = 'announcement';

  function openMessageModal() {
    if (document.getElementById('message-modal')) return;
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'message-modal';
    overlay.style.alignItems = 'flex-start';
    overlay.style.paddingTop = '60px';
    overlay.innerHTML = '<div class="modal-content" style="max-width:420px;" onclick="event.stopPropagation()">' +
      '<div class="modal-header">' +
        '<div class="modal-title">消息中心</div>' +
        '<button class="modal-close" onclick="closeMessageModal()">×</button>' +
      '</div>' +
      '<div style="display:flex;gap:8px;padding:12px 16px;border-bottom:1px solid var(--border-color);">' +
        '<button class="msg-tab active" data-type="announcement" onclick="switchMessageTab(\'announcement\')">公告</button>' +
        '<button class="msg-tab" data-type="message" onclick="switchMessageTab(\'message\')">站内信</button>' +
        '<button class="msg-tab" data-type="history" onclick="switchMessageTab(\'history\')">历史记录</button>' +
      '</div>' +
      '<div class="modal-body" id="message-modal-body" style="min-height:200px;"></div>' +
      '<div style="display:flex;justify-content:flex-end;gap:8px;padding:12px 16px;border-top:1px solid var(--border-color);">' +
        '<button onclick="markMessageCategoryRead()" style="padding:6px 12px;border:1px solid var(--border-color);background:var(--card-bg);color:var(--text-color);border-radius:6px;font-size:13px;cursor:pointer;">全部已读</button>' +
        '<button onclick="clearMessageCategory()" style="padding:6px 12px;border:1px solid #ff4d4f;background:#fff;color:#ff4d4f;border-radius:6px;font-size:13px;cursor:pointer;">清空</button>' +
      '</div>' +
    '</div>';
    overlay.onclick = closeMessageModal;
    document.body.appendChild(overlay);
    switchMessageTab('announcement');
  }

  function closeMessageModal() {
    var el = document.getElementById('message-modal');
    if (el) el.remove();
  }

  function switchMessageTab(type) {
    currentMessageType = type;
    document.querySelectorAll('#message-modal .msg-tab').forEach(function(btn) {
      btn.classList.toggle('active', btn.dataset.type === type);
      btn.style.cssText = btn.dataset.type === type
        ? 'padding:6px 12px;border-radius:6px;border:none;background:#f6ffed;color:#07c160;font-size:13px;cursor:pointer;'
        : 'padding:6px 12px;border-radius:6px;border:none;background:transparent;color:var(--text-secondary);font-size:13px;cursor:pointer;';
    });
    markCategoryRead(type);
    renderMessageTab(type);
  }

  function renderMessageTab(type) {
    var body = document.getElementById('message-modal-body');
    if (!body) return;
    var notifications = getNotifications().filter(function(n) {
      return normalizeNotificationType(n.type) === type;
    });
    notifications.sort(function(a, b) { return new Date(b.createdAt) - new Date(a.createdAt); });

    if (notifications.length === 0) {
      body.innerHTML = '<div style="text-align:center;padding:40px 0;color:var(--text-secondary);font-size:13px;">暂无 ' + MESSAGE_TYPE_LABELS[type] + '</div>';
      return;
    }

    body.innerHTML = notifications.map(function(n) {
      return '<div style="padding:12px 0;border-bottom:1px solid var(--border-color);">' +
        '<div style="font-weight:' + (n.read ? '500' : '700') + ';font-size:14px;margin-bottom:4px;color:var(--text-color);">' + escapeHtml(n.title) + '</div>' +
        '<div style="font-size:13px;color:var(--text-secondary);margin-bottom:4px;">' + escapeHtml(n.summary) + '</div>' +
        '<div style="font-size:12px;color:#8c8c8c;">' + formatMessageTime(n.createdAt) + '</div>' +
      '</div>';
    }).join('');
  }

  function markMessageCategoryRead() {
    markCategoryRead(currentMessageType);
    renderMessageTab(currentMessageType);
  }

  function clearMessageCategory() {
    if (!confirm('确定清空「' + MESSAGE_TYPE_LABELS[currentMessageType] + '」吗？')) return;
    clearCategory(currentMessageType);
    renderMessageTab(currentMessageType);
  }

  function formatMessageTime(iso) {
    var date = new Date(iso);
    return date.getFullYear() + '-' + String(date.getMonth()+1).padStart(2,'0') + '-' + String(date.getDate()).padStart(2,'0') + ' ' + String(date.getHours()).padStart(2,'0') + ':' + String(date.getMinutes()).padStart(2,'0');
  }

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
```

- [ ] **Step 3: Add message tab base styles**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.css` 末尾追加：

```css
.msg-tab {
  padding: 6px 12px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
}
.msg-tab.active {
  background: var(--sidebar-hover);
  color: var(--primary-color);
}
```

- [ ] **Step 4: Verify with Playwright**

```bash
python3 - <<'PY'
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto(f"{BASE_URL}/console.html")
    page.evaluate("""() => {
        localStorage.setItem('aichuangzuo_notifications', JSON.stringify([
            { id: '1', type: 'announcement', title: '新功能', summary: '标题优化器上线', read: false, createdAt: new Date().toISOString() }
        ]));
        openMessageModal();
    }""")
    title = page.locator("#message-modal .modal-title").text_content()
    assert title == "消息中心", f"Unexpected title: {title}"
    item = page.locator("#message-modal-body").text_content()
    assert "标题优化器上线" in item, f"Message content missing: {item}"
    browser.close()
    print("Message modal verification passed.")
PY
```

- [ ] **Step 5: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js \
        .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(console): add message bell modal with three tabs"
```

---

### Task 9: Tutorial modal

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Produces:
  - `openTutorialModal()` → void
  - `closeTutorialModal()` → void

- [ ] **Step 1: Add tutorial modal functions**

在消息弹框代码后添加：

```javascript
  // ===================== 教程弹框 =====================
  function openTutorialModal() {
    if (document.getElementById('tutorial-modal')) return;
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'tutorial-modal';
    overlay.innerHTML = '<div class="modal-content" style="max-width:420px;" onclick="event.stopPropagation()">' +
      '<div class="modal-header"><div class="modal-title">教程与帮助</div><button class="modal-close" onclick="closeTutorialModal()">×</button></div>' +
      '<div class="modal-body" style="padding:0;">' +
        '<div onclick="showToast(\'帮助文档占位\')" style="padding:16px 20px;border-bottom:1px solid var(--border-color);cursor:pointer;">' +
          '<div style="font-weight:600;font-size:15px;margin-bottom:4px;color:var(--text-color);">📄 帮助 / 文档</div>' +
          '<div style="font-size:13px;color:var(--text-secondary);">从基础到专业技巧的快速指南，助你充分利用爱创作的功能。</div>' +
        '</div>' +
        '<div onclick="showToast(\'B站视频占位\')" style="padding:16px 20px;border-bottom:1px solid var(--border-color);cursor:pointer;">' +
          '<div style="font-weight:600;font-size:15px;margin-bottom:4px;color:var(--text-color);">▶️ 观看 B 站</div>' +
          '<div style="font-size:13px;color:var(--text-secondary);">视频教程、定期直播和课程，适合学习者。直播中设有在线答疑时间！</div>' +
        '</div>' +
        '<div onclick="showToast(\'微信交流群占位\')" style="padding:16px 20px;cursor:pointer;">' +
          '<div style="font-weight:600;font-size:15px;margin-bottom:4px;color:var(--text-color);">💬 加入微信交流群</div>' +
          '<div style="font-size:13px;color:var(--text-secondary);">一个充满活力的作者网络，提供帮助的渠道，分享创作技巧、经验和最佳实践。</div>' +
        '</div>' +
      '</div>' +
    '</div>';
    overlay.onclick = closeTutorialModal;
    document.body.appendChild(overlay);
  }

  function closeTutorialModal() {
    var el = document.getElementById('tutorial-modal');
    if (el) el.remove();
  }
```

- [ ] **Step 2: Verify**

访问 `console.html`，点击「教程」按钮，确认弹框出现。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(console): add tutorial modal"
```

---

### Task 10: Feedback modal

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Produces:
  - `openFeedbackModal()` → void
  - `closeFeedbackModal()` → void

- [ ] **Step 1: Add feedback modal functions**

在教程弹框代码后添加：

```javascript
  // ===================== 反馈弹框 =====================
  function openFeedbackModal() {
    if (document.getElementById('feedback-modal')) return;
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'feedback-modal';
    overlay.innerHTML = '<div class="modal-content" style="max-width:420px;" onclick="event.stopPropagation()">' +
      '<div class="modal-header"><div class="modal-title">意见反馈</div><button class="modal-close" onclick="closeFeedbackModal()">×</button></div>' +
      '<div class="modal-body">' +
        '<div style="margin-bottom:12px;">' +
          '<label style="display:block;font-size:13px;color:var(--text-secondary);margin-bottom:6px;">反馈类型</label>' +
          '<select id="feedback-type" style="width:100%;padding:8px;border:1px solid var(--border-color);border-radius:6px;background:var(--card-bg);color:var(--text-color);">' +
            '<option>功能建议</option><option>问题反馈</option><option>其他</option>' +
          '</select>' +
        '</div>' +
        '<div style="margin-bottom:16px;">' +
          '<label style="display:block;font-size:13px;color:var(--text-secondary);margin-bottom:6px;">反馈内容</label>' +
          '<textarea id="feedback-content" rows="4" style="width:100%;padding:8px;border:1px solid var(--border-color);border-radius:6px;background:var(--card-bg);color:var(--text-color);resize:vertical;"></textarea>' +
        '</div>' +
        '<button onclick="submitFeedback()" style="width:100%;padding:10px;background:#07c160;color:#fff;border:none;border-radius:8px;font-size:14px;cursor:pointer;">提交反馈</button>' +
      '</div>' +
    '</div>';
    overlay.onclick = closeFeedbackModal;
    document.body.appendChild(overlay);
  }

  function closeFeedbackModal() {
    var el = document.getElementById('feedback-modal');
    if (el) el.remove();
  }

  function submitFeedback() {
    var type = document.getElementById('feedback-type').value;
    var content = document.getElementById('feedback-content').value;
    if (!content.trim()) {
      showToast('请填写反馈内容');
      return;
    }
    console.log('Feedback:', type, content);
    showToast('反馈已收到，感谢你的建议');
    closeFeedbackModal();
  }
```

- [ ] **Step 2: Verify**

访问 `console.html`，点击「反馈」按钮，填写内容提交，确认出现 Toast。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(console): add feedback modal"
```

---

### Task 11: Console e2e test

**Files:**
- Create: `tests/e2e/verify_console.py`

**Interfaces:**
- No code interfaces; verifies UI behavior.

- [ ] **Step 1: Create test script**

创建 `tests/e2e/verify_console.py`：

```python
import subprocess
import sys

try:
    from playwright.sync_api import sync_playwright, expect
except ImportError:
    print("Installing playwright...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "playwright"])
    subprocess.check_call([sys.executable, "-m", "playwright", "install", "chromium"])
    from playwright.sync_api import sync_playwright, expect


BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"


def test_console_page():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        page.goto(f"{BASE_URL}/console.html")

        # Sidebar
        expect(page.locator(".console-sidebar-brand")).to_have_text("爱创作")
        expect(page.locator(".console-sidebar-item").first).to_have_text("📝 创作")

        # Header center title
        expect(page.locator(".console-header-center")).to_have_text("爱创作")

        # Membership modal
        page.locator(".console-membership-badge").click()
        expect(page.locator("#membership-modal .modal-title")).to_have_text("会员信息")
        page.locator("#membership-modal .modal-close").click()
        expect(page.locator("#membership-modal")).not_to_be_visible()

        # User menu
        page.locator(".console-avatar").click()
        expect(page.locator(".console-user-menu")).to_be_visible()

        # Message modal
        page.evaluate("""
            localStorage.setItem('aichuangzuo_notifications', JSON.stringify([
                { id: '1', type: 'announcement', title: '公告测试', summary: '公告内容', read: false, createdAt: new Date().toISOString() }
            ]));
        """)
        page.reload()
        page.locator(".console-icon-btn[title='消息']").click()
        expect(page.locator("#message-modal .modal-title")).to_have_text("消息中心")
        expect(page.locator("#message-modal-body")).to_contain_text("公告内容")

        # Tutorial modal
        page.locator("#message-modal .modal-close").click()
        page.locator(".console-icon-btn[title='教程']").click()
        expect(page.locator("#tutorial-modal .modal-title")).to_have_text("教程与帮助")

        # Theme toggle
        page.locator("#tutorial-modal .modal-close").click()
        page.evaluate("""() => { localStorage.setItem('aichuangzuo_theme', 'light'); }""")
        page.reload()
        page.locator(".console-icon-btn[title='切换主题']").click()
        theme = page.evaluate("""() => { return document.body.getAttribute('data-theme'); }""")
        assert theme == 'dark', f"Expected dark theme, got {theme}"

        browser.close()


if __name__ == "__main__":
    test_console_page()
    print("Console verification passed.")
```

- [ ] **Step 2: Run the test**

```bash
python3 tests/e2e/verify_console.py
```

Expected: `Console verification passed.`

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_console.py
git commit -m "test(console): add console e2e verification"
```

---

### Task 12: Manual verification and screenshots

**Files:**
- No file changes.

- [ ] **Step 1: Verify console in browser**

启动服务器：

```bash
./scripts/local/start.sh
```

访问 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/console.html`

依次检查：
1. 左侧边栏四项菜单显示正常。
2. Header 左侧头像点击出现「个人中心」「退出登录」。
3. Header 「会员」点击弹出会员信息，有「立即续费」按钮。
4. Header 铃铛点击弹出消息浮层，能切换公告 / 站内信 / 历史记录。
5. Header 教程、反馈、关于我们弹框都能打开。
6. 昼夜切换按钮能切换主题，刷新后保持。

- [ ] **Step 2: Verify global footer**

访问 `index.html`、`pricing.html`、`login.html`，确认底部出现统一 Footer，点击「关于我们」打开弹框。

- [ ] **Step 3: Capture reference screenshots**

用 Playwright 或其他截图工具保存控制台页面、消息弹框、关于我们弹框的截图，放入 `tests/e2e/screenshots/` 供后续回归对比。

---

## Self-Review

**1. Spec coverage:**
- 控制台页面：✓ Task 5
- 左侧边栏：✓ Task 5/6
- Header 头像/会员/消息/教程/昼夜/反馈/关于我们：✓ Task 6/7/8/9/10
- 消息铃铛三类弹框：✓ Task 8
- 会员信息弹框：✓ Task 7
- 关于我们弹框：✓ Task 4
- 全站 Footer：✓ Task 3
- 昼夜模式：✓ Task 1/2

**2. Placeholder scan：** 无 TBD/TODO。

**3. Type consistency：** `openMessageModal` / `closeMessageModal` / `switchMessageTab` 等命名一致；`getTheme` / `setTheme` / `toggleTheme` 一致。

**4. Gap：** 无。

# 消息通知中心实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在爱创作原型中新增一个站内消息中心，支持生成完成、会员提醒、新功能、优惠活动四类消息的历史记录查看。

**Architecture:** 使用 `localStorage` 持久化消息数据；在 `shared.js` 中新增通知读写与角标更新 API；新增 `notifications.html` 作为消息中心页；在所有现有页面导航栏加入铃铛入口；在生成队列完成和页面加载时触发消息写入。

**Tech Stack:** 纯前端 HTML/CSS/JS，无构建工具，无后端，使用 `localStorage`。

## Global Constraints

- 保持原型前端-only，不引入后端或构建步骤。
- 消息数据持久化使用 `localStorage`，key 统一为 `aichuangzuo_` 前缀。
- 复用现有 `shared.js` 的 `showToast` 作为即时反馈。
- 复用 `settings.html` 中已有的通知开关；关闭某类通知后不再写入该类消息。
- 不实现 Web Push / 短信 / 邮件推送、单条删除、手动已读切换、消息搜索。
- 移动端铃铛入口暂不在顶部导航展示，通过个人中心进入。

---

## File Structure

- **Create**
  - `.superpowers/brainstorm/6491-1782131242/content/notifications.html` — 消息中心页面，含四类 Tab、消息列表、清空按钮。
  - `tests/e2e/verify_notifications.py` — Playwright 验证脚本。
- **Modify**
  - `.superpowers/brainstorm/6491-1782131242/content/shared.js` — 新增通知数据模型、读写 API、角标更新、生成完成通知触发。
  - `.superpowers/brainstorm/6491-1782131242/content/shared.css` — 新增铃铛、角标、Tab、消息列表、空状态样式。
  - `.superpowers/brainstorm/6491-1782131242/content/index.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/login.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/create.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/loading.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/preview.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/works.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/pricing.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/settings.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/order.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/payment.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/forgot.html` — 导航栏加铃铛。
  - `.superpowers/brainstorm/6491-1782131242/content/edit.html` — 导航栏加铃铛。

---

### Task 1: Add notification core API to shared.js

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Produces:
  - `NOTIFICATION_KEY = 'aichuangzuo_notifications'`
  - `NOTIFICATION_TYPES = ['generation', 'membership', 'feature', 'promotion']`
  - `getNotifications()` → returns `Array<Notification>`
  - `saveNotifications(notifications)` → void
  - `addNotification(type, title, summary)` → `Notification | null`
  - `markCategoryRead(type)` → void
  - `clearCategory(type)` → void
  - `getUnreadCount()` → number
  - `updateNotificationBadge()` → void
  - `isNotificationEnabled(type)` → boolean

- [ ] **Step 1: Append notification API near the end of shared.js before the DOMContentLoaded listener**

打开 `.superpowers/brainstorm/6491-1782131242/content/shared.js`，在文件末尾（`document.addEventListener('DOMContentLoaded'...` 之前）插入以下代码：

```javascript
  // ===================== 消息通知中心 =====================
  var NOTIFICATION_KEY = 'aichuangzuo_notifications';
  var NOTIFICATION_TYPES = ['generation', 'membership', 'feature', 'promotion'];

  function getNotifications() {
    try {
      var raw = localStorage.getItem(NOTIFICATION_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch (e) {
      return [];
    }
  }

  function saveNotifications(notifications) {
    localStorage.setItem(NOTIFICATION_KEY, JSON.stringify(notifications));
  }

  function isNotificationEnabled(type) {
    var key = 'aichuangzuo_notification_settings';
    var defaults = { generation: true, membership: true, feature: true, promotion: true };
    try {
      var raw = localStorage.getItem(key);
      var settings = raw ? JSON.parse(raw) : defaults;
      return settings[type] !== false;
    } catch (e) {
      return defaults[type] !== false;
    }
  }

  function addNotification(type, title, summary) {
    if (!NOTIFICATION_TYPES.includes(type)) return null;
    if (!isNotificationEnabled(type)) return null;
    var notifications = getNotifications();
    var notification = {
      id: generateId(),
      type: type,
      title: title || '',
      summary: summary || '',
      read: false,
      createdAt: new Date().toISOString()
    };
    notifications.unshift(notification);
    // 最多保留 100 条
    if (notifications.length > 100) {
      notifications = notifications.slice(0, 100);
    }
    saveNotifications(notifications);
    updateNotificationBadge();
    return notification;
  }

  function markCategoryRead(type) {
    var notifications = getNotifications();
    var changed = false;
    notifications.forEach(function(n) {
      if (n.type === type && !n.read) {
        n.read = true;
        changed = true;
      }
    });
    if (changed) {
      saveNotifications(notifications);
      updateNotificationBadge();
    }
  }

  function clearCategory(type) {
    var notifications = getNotifications().filter(function(n) { return n.type !== type; });
    saveNotifications(notifications);
    updateNotificationBadge();
  }

  function getUnreadCount() {
    return getNotifications().filter(function(n) { return !n.read; }).length;
  }

  function updateNotificationBadge() {
    var count = getUnreadCount();
    document.querySelectorAll('.notification-badge').forEach(function(el) {
      el.textContent = count > 99 ? '99+' : String(count);
      el.style.display = count > 0 ? 'flex' : 'none';
    });
  }
```

- [ ] **Step 2: Verify with browser console**

在浏览器打开任意原型页面，按 F12 打开控制台，依次执行：

```javascript
addNotification('generation', '测试标题', '测试摘要');
getNotifications();
getUnreadCount();
```

Expected: `getNotifications()` 返回包含一条未读消息的数组，`getUnreadCount()` 返回 1。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(notification): add notification core API and badge helpers"
```

---

### Task 2: Add notification styles to shared.css

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Produces CSS classes:
  - `.notification-bell` — 铃铛按钮容器
  - `.notification-bell-icon` — 铃铛 SVG 图标
  - `.notification-badge` — 红色数字角标
  - `.notification-page` / `.notification-tabs` / `.notification-tab` — 消息中心页和 Tab
  - `.notification-item` / `.notification-item.unread` — 消息项
  - `.notification-empty` — 空状态

- [ ] **Step 1: Append styles to shared.css**

打开 `.superpowers/brainstorm/6491-1782131242/content/shared.css`，在文件末尾追加：

```css
/* 消息通知中心 */
.notification-bell {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  background: transparent;
  border: none;
  margin-left: auto;
  flex-shrink: 0;
}
.notification-bell:hover {
  background: #f6ffed;
}
.notification-bell-icon {
  width: 18px;
  height: 18px;
  color: #595959;
}
.notification-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: none;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}
.notification-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 24px;
}
.notification-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.notification-page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}
.notification-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}
.notification-tab {
  padding: 10px 16px;
  border: none;
  background: transparent;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.notification-tab.active {
  color: #07c160;
  border-bottom-color: #07c160;
  font-weight: 600;
}
.notification-clear-btn {
  padding: 6px 14px;
  border: 1px solid #ff4d4f;
  background: #fff;
  color: #ff4d4f;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}
.notification-clear-btn:hover {
  background: #fff1f0;
}
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.notification-item {
  position: relative;
  padding: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.notification-item:hover {
  border-color: #b7eb8f;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.notification-item.unread {
  background: #f6ffed;
  border-left: 3px solid #07c160;
}
.notification-item-title {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 6px;
}
.notification-item.unread .notification-item-title {
  font-weight: 700;
}
.notification-item-summary {
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
  margin-bottom: 8px;
}
.notification-item-time {
  font-size: 12px;
  color: #8c8c8c;
}
.notification-empty {
  text-align: center;
  padding: 60px 24px;
  color: #8c8c8c;
  font-size: 14px;
}
.notification-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}
```

- [ ] **Step 2: Verify with browser**

打开任意页面，确认样式文件加载没有报错，铃铛样式类可用。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(notification): add notification center styles"
```

---

### Task 3: Create notifications.html page

**Files:**
- Create: `.superpowers/brainstorm/6491-1782131242/content/notifications.html`

**Interfaces:**
- Consumes from shared.js:
  - `getNotifications()`
  - `markCategoryRead(type)`
  - `clearCategory(type)`
  - `updateNotificationBadge()`
- Produces inline functions:
  - `renderNotifications(type)`
  - `switchTab(type)`
  - `handleClear(type)`

- [ ] **Step 1: Create notifications.html**

创建 `.superpowers/brainstorm/6491-1782131242/content/notifications.html`，内容如下：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>消息中心 - 爱创作</title>
<link rel="stylesheet" href="shared.css">
<style>
  body { background: #f8f9fa; min-height: 100vh; }
</style>
</head>
<body>
<div class="prototype-frame">
  <div class="prototype-nav">
    <button onclick="location.href='index.html'">01 首页</button>
    <button onclick="location.href='login.html'">02 登录/注册</button>
    <button onclick="location.href='create.html'">03 创作页</button>
    <button onclick="location.href='loading.html'">04 生成队列</button>
    <button onclick="location.href='preview.html'">05 预览/导出</button>
    <button onclick="location.href='works.html'">06 我的作品</button>
    <button onclick="location.href='pricing.html'">07 会员/购买</button>
    <button onclick="location.href='settings.html'">08 个人中心</button>
    <button onclick="location.href='order.html'">09 确认订单</button>
    <button onclick="location.href='payment.html'">10 扫码支付</button>
    <button class="notification-bell" onclick="location.href='notifications.html'" title="消息中心">
      <svg class="notification-bell-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
      </svg>
      <span class="notification-badge"></span>
    </button>
  </div>
  <div class="prototype-screen active">
    <div class="notification-page">
      <div class="notification-page-header">
        <div class="notification-page-title">消息中心</div>
        <button class="notification-clear-btn" id="clear-btn" onclick="handleClear(currentType)">清空本类消息</button>
      </div>
      <div class="notification-tabs">
        <button class="notification-tab active" data-type="generation" onclick="switchTab('generation')">生成完成</button>
        <button class="notification-tab" data-type="membership" onclick="switchTab('membership')">会员提醒</button>
        <button class="notification-tab" data-type="feature" onclick="switchTab('feature')">新功能</button>
        <button class="notification-tab" data-type="promotion" onclick="switchTab('promotion')">优惠活动</button>
      </div>
      <div class="notification-list" id="notification-list"></div>
    </div>
  </div>
</div>
<script src="shared.js"></script>
<script>
  var TYPE_LABELS = {
    generation: '生成完成',
    membership: '会员提醒',
    feature: '新功能',
    promotion: '优惠活动'
  };
  var currentType = 'generation';

  function formatTime(iso) {
    var date = new Date(iso);
    var now = new Date();
    var diff = Math.floor((now - date) / 1000);
    if (diff < 60) return '刚刚';
    if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前';
    if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前';
    if (diff < 604800) return Math.floor(diff / 86400) + ' 天前';
    return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
  }

  function renderNotifications(type) {
    var list = document.getElementById('notification-list');
    var notifications = getNotifications().filter(function(n) { return n.type === type; });
    notifications.sort(function(a, b) { return new Date(b.createdAt) - new Date(a.createdAt); });

    if (notifications.length === 0) {
      list.innerHTML = '<div class="notification-empty"><div class="notification-empty-icon">📭</div>暂无 ' + TYPE_LABELS[type] + ' 消息</div>';
      return;
    }

    list.innerHTML = notifications.map(function(n) {
      return '<div class="notification-item ' + (n.read ? '' : 'unread') + '" onclick="handleItemClick(\'' + n.id + '\')">' +
        '<div class="notification-item-title">' + escapeHtml(n.title) + '</div>' +
        '<div class="notification-item-summary">' + escapeHtml(n.summary) + '</div>' +
        '<div class="notification-item-time">' + formatTime(n.createdAt) + '</div>' +
      '</div>';
    }).join('');
  }

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function switchTab(type) {
    currentType = type;
    document.querySelectorAll('.notification-tab').forEach(function(btn) {
      btn.classList.toggle('active', btn.dataset.type === type);
    });
    markCategoryRead(type);
    renderNotifications(type);
  }

  function handleClear(type) {
    if (!confirm('确定要清空「' + TYPE_LABELS[type] + '」下的所有消息吗？')) return;
    clearCategory(type);
    renderNotifications(type);
    showToast('已清空');
  }

  function handleItemClick(id) {
    var notifications = getNotifications();
    var n = notifications.find(function(item) { return item.id === id; });
    if (!n) return;
    if (!n.read) {
      n.read = true;
      saveNotifications(notifications);
      updateNotificationBadge();
      renderNotifications(currentType);
    }
    if (n.type === 'generation') {
      location.href = 'works.html';
    } else if (n.type === 'membership') {
      location.href = 'pricing.html';
    }
  }

  document.addEventListener('DOMContentLoaded', function() {
    updateNotificationBadge();
    switchTab('generation');
  });
</script>
</body>
</html>
```

- [ ] **Step 2: Verify page renders**

启动本地服务器：

```bash
./scripts/local/start.sh
```

访问 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/notifications.html`

Expected: 页面显示四个 Tab，当前「生成完成」Tab 无消息时显示「暂无 生成完成 消息」。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/notifications.html
git commit -m "feat(notification): add notification center page"
```

---

### Task 4: Add bell entry to all page navs

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/index.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/login.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/create.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/loading.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/preview.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/works.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/pricing.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/settings.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/order.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/payment.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/forgot.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/edit.html`

**Interfaces:**
- Consumes from shared.js: `updateNotificationBadge()`

- [ ] **Step 1: Insert bell HTML into each page's `.prototype-nav`**

对每个页面，找到 `<div class="prototype-nav">` 的结束标签 `</div>`，在前面插入：

```html
    <button class="notification-bell" onclick="location.href='notifications.html'" title="消息中心">
      <svg class="notification-bell-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
      </svg>
      <span class="notification-badge"></span>
    </button>
```

- [ ] **Step 2: Initialize badge on page load**

对每个页面，在已有的 `DOMContentLoaded` 监听器末尾或 `<script>` 末尾添加：

```javascript
  if (typeof updateNotificationBadge === 'function') {
    updateNotificationBadge();
  }
```

如果页面没有 `DOMContentLoaded` 监听器，则在 `</body>` 前添加：

```html
<script>
  document.addEventListener('DOMContentLoaded', function() {
    if (typeof updateNotificationBadge === 'function') {
      updateNotificationBadge();
    }
  });
</script>
```

- [ ] **Step 3: Verify one page and apply to the rest**

先修改 `index.html`，刷新浏览器，确认导航栏右侧出现铃铛图标。然后依次修改其余 11 个页面。

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/index.html \
        .superpowers/brainstorm/6491-1782131242/content/login.html \
        .superpowers/brainstorm/6491-1782131242/content/create.html \
        .superpowers/brainstorm/6491-1782131242/content/loading.html \
        .superpowers/brainstorm/6491-1782131242/content/preview.html \
        .superpowers/brainstorm/6491-1782131242/content/works.html \
        .superpowers/brainstorm/6491-1782131242/content/pricing.html \
        .superpowers/brainstorm/6491-1782131242/content/settings.html \
        .superpowers/brainstorm/6491-1782131242/content/order.html \
        .superpowers/brainstorm/6491-1782131242/content/payment.html \
        .superpowers/brainstorm/6491-1782131242/content/forgot.html \
        .superpowers/brainstorm/6491-1782131242/content/edit.html
git commit -m "feat(notification): add notification bell to all page navs"
```

---

### Task 5: Wire generation completion notification

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Consumes: `addNotification(type, title, summary)` from Task 1

- [ ] **Step 1: Add notification when task completes**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.js` 中找到 `processGenerationQueue` 函数，定位到以下代码块：

```javascript
          task.progress = 100;
          task.status = 'completed';
          task.completedAt = new Date().toISOString();
          changed = true;
          showGenerationToast('《' + task.title + '》生成完成', 'success');
```

在其后插入：

```javascript
          if (typeof addNotification === 'function') {
            addNotification('generation', '文章生成完成', '《' + task.title + '》已生成，点击预览');
          }
```

- [ ] **Step 2: Verify**

1. 打开 `create.html`，提交一个生成任务。
2. 等待任务完成（或在 `loading.html` 观察）。
3. 打开 `notifications.html` 或查看任意页面铃铛角标。

Expected: 任务完成后，铃铛角标显示 1；消息中心「生成完成」Tab 出现一条消息。

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(notification): trigger notification on generation completion"
```

---

### Task 6: Wire membership, feature, and promotion notifications

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/notifications.html`

**Interfaces:**
- Consumes: `addNotification(type, title, summary)`
- Produces: `seedNotificationsOnLoad()`

- [ ] **Step 1: Add seed helper to shared.js**

在 `.superpowers/brainstorm/6491-1782131242/content/shared.js` 的通知 API 区域追加：

```javascript
  function seedNotificationsOnLoad() {
    var SEED_KEY = 'aichuangzuo_notifications_seeded';
    if (localStorage.getItem(SEED_KEY)) return;

    addNotification('feature', '新功能上线：标题优化器', '预览页新增 AI 标题优化，一键生成多平台爆款标题');
    addNotification('promotion', '限时优惠：年会员 7 折', '即日起至月底，年会员低至 199 元，点击了解详情');

    var membershipExpiry = localStorage.getItem('aichuangzuo_membership_expiry');
    if (!membershipExpiry) {
      // 模拟一个 30 天后到期的会员，用于演示提醒
      var expiry = new Date();
      expiry.setDate(expiry.getDate() + 30);
      localStorage.setItem('aichuangzuo_membership_expiry', expiry.toISOString());
    }

    localStorage.setItem(SEED_KEY, 'true');
  }

  function checkMembershipNotifications() {
    var raw = localStorage.getItem('aichuangzuo_membership_expiry');
    if (!raw) return;
    var expiry = new Date(raw);
    var now = new Date();
    var days = Math.ceil((expiry - now) / (1000 * 60 * 60 * 24));

    var CHECK_KEY = 'aichuangzuo_membership_notified_days';
    var notified = parseInt(localStorage.getItem(CHECK_KEY) || '999', 10);

    if (days <= 7 && days > 3 && notified > 7) {
      addNotification('membership', '会员即将到期', '您的会员将在 7 天后到期，续费可继续享受 AI 创作权益');
      localStorage.setItem(CHECK_KEY, '7');
    } else if (days <= 3 && days > 0 && notified > 3) {
      addNotification('membership', '会员即将到期', '您的会员将在 3 天内到期，请及时续费');
      localStorage.setItem(CHECK_KEY, '3');
    } else if (days <= 0 && notified > 0) {
      addNotification('membership', '会员已到期', '您的会员已到期，续费后可恢复全部功能');
      localStorage.setItem(CHECK_KEY, '0');
    }
  }
```

- [ ] **Step 2: Call seed helper on notifications page load**

在 `.superpowers/brainstorm/6491-1782131242/content/notifications.html` 的 `DOMContentLoaded` 监听器中，在 `switchTab('generation')` 之前添加：

```javascript
    seedNotificationsOnLoad();
    checkMembershipNotifications();
```

- [ ] **Step 3: Verify**

1. 清除 localStorage 中的 `aichuangzuo_notifications_seeded`。
2. 刷新 `notifications.html`。

Expected: 「新功能」「优惠活动」Tab 各出现一条消息；「会员提醒」Tab 出现 7 天后到期的提醒。

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js \
        .superpowers/brainstorm/6491-1782131242/content/notifications.html
git commit -m "feat(notification): seed membership, feature and promotion notifications"
```

---

### Task 7: Write notification e2e test

**Files:**
- Create: `tests/e2e/verify_notifications.py`

**Interfaces:**
- No code interfaces; verifies UI behavior.

- [ ] **Step 1: Create test script**

创建 `tests/e2e/verify_notifications.py`，内容如下：

```python
import subprocess
import sys
import time
from pathlib import Path

try:
    from playwright.sync_api import sync_playwright, expect
except ImportError:
    print("Installing playwright...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "playwright"])
    subprocess.check_call([sys.executable, "-m", "playwright", "install", "chromium"])
    from playwright.sync_api import sync_playwright, expect


BASE_URL = "http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content"


def clear_notifications(page):
    page.evaluate("""
        localStorage.removeItem('aichuangzuo_notifications');
        localStorage.removeItem('aichuangzuo_notifications_seeded');
        localStorage.removeItem('aichuangzuo_membership_notified_days');
    """)


def test_notification_badge_and_page():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()

        # 1. 打开首页并清空历史消息
        page.goto(f"{BASE_URL}/index.html")
        clear_notifications(page)
        page.reload()

        # 2. 铃铛应存在，角标隐藏
        bell = page.locator(".notification-bell").first
        expect(bell).to_be_visible()
        badge = bell.locator(".notification-badge").first
        expect(badge).not_to_be_visible()

        # 3. 手动写入一条未读生成完成消息
        page.evaluate("""
            var notifications = [{
                id: 'test-gen-1',
                type: 'generation',
                title: '文章生成完成',
                summary: '《测试文章》已生成',
                read: false,
                createdAt: new Date().toISOString()
            }];
            localStorage.setItem('aichuangzuo_notifications', JSON.stringify(notifications));
        """)
        page.reload()

        # 4. 角标显示 1
        expect(badge).to_be_visible()
        expect(badge).to_have_text("1")

        # 5. 点击铃铛进入消息中心
        bell.click()
        page.wait_for_url(f"{BASE_URL}/notifications.html")
        expect(page.locator(".notification-page-title")).to_have_text("消息中心")

        # 6. 生成完成 Tab 下应有一条未读消息
        item = page.locator(".notification-item.unread").first
        expect(item).to_be_visible()
        expect(item.locator(".notification-item-title")).to_have_text("文章生成完成")

        # 7. 切换 Tab 后未读标记应消失（自动已读）
        page.locator(".notification-tab[data-type='membership']").click()
        page.locator(".notification-tab[data-type='generation']").click()
        expect(page.locator(".notification-item.unread")).not_to_be_visible()

        # 8. 清空本类消息
        page.locator("#clear-btn").click()
        page.on("dialog", lambda dialog: dialog.accept())
        page.locator("#clear-btn").click()
        expect(page.locator(".notification-empty")).to_be_visible()

        browser.close()


if __name__ == "__main__":
    test_notification_badge_and_page()
    print("Notification verification passed.")
```

- [ ] **Step 2: Run the test**

确保本地服务器已启动：

```bash
./scripts/local/start.sh
```

在另一个终端运行：

```bash
python3 tests/e2e/verify_notifications.py
```

Expected: 输出 `Notification verification passed.`

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_notifications.py
git commit -m "test(notification): add notification center e2e verification"
```

---

### Task 8: Manual browser verification

**Files:**
- No file changes.

- [ ] **Step 1: Start the server**

```bash
./scripts/local/start.sh
```

- [ ] **Step 2: Verify the golden path**

1. 访问 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/create.html`
2. 填写主题并提交生成任务。
3. 等待任务完成（约几秒）。
4. 观察顶部导航铃铛：角标从 0 变为 1。
5. 点击铃铛进入消息中心。
6. 确认「生成完成」Tab 有一条未读消息，标题为「文章生成完成」。
7. 切换到「会员提醒」「新功能」「优惠活动」Tab，确认各有预设消息。
8. 切换回「生成完成」，确认未读标记消失。
9. 点击「清空本类消息」，确认列表为空。

- [ ] **Step 3: Verify edge cases**

1. 刷新页面后消息不丢失。
2. 在 `settings.html` 关闭「生成完成通知」，再次生成文章，确认不再产生新消息。
3. 在移动端宽度下，页面布局不崩溃（允许横向滚动）。

- [ ] **Step 4: Update progress ledger**

如有需要，更新 `.superpowers/sdd/progress.md`，记录消息通知中心已完成。

---

## Self-Review

**1. Spec coverage:**
- 四类消息支持：✓ Task 1 数据模型 + Task 5/6 触发时机
- 历史记录查看：✓ Task 3 notifications.html
- 顶部铃铛入口：✓ Task 4
- 自动已读 + 一键清空：✓ Task 3
- 与现有通知开关集成：✓ Task 1 `isNotificationEnabled`
- 生成完成与队列打通：✓ Task 5

**2. Placeholder scan:** 无 TBD、TODO、模糊描述。

**3. Type consistency:** `addNotification` / `getNotifications` / `clearCategory` / `markCategoryRead` 名称与用法在各 Task 中一致。

**4. Gap:** 无。

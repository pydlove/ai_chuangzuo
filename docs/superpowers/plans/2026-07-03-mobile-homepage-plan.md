# 首页移动端适配实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为用户端首页 `Home.vue` 添加手机端适配，解决顶部导航挤压问题，实现汉堡抽屉菜单与各区块响应式布局。

**Architecture:** 在单一文件 `project/user/web/src/views/Home.vue` 内通过 `@media (max-width: 768px)` 媒体查询覆盖桌面样式；新增抽屉组件状态与 DOM，复用现有主题切换逻辑；使用 Playwright 脚本在 375px/1280px 视口下验证布局与交互。

**Tech Stack:** Vue 3 + Vite + Ant Design Vue + CSS3 Media Queries + Playwright (Python)

**Implementation Notes:** 桌面端保持原有 `.nav-links` flex 结构，通过 `.nav-link-desktop` / `.theme-toggle-desktop` 类控制手机端隐藏；抽屉使用 `transform: translateX(100%)` 滑入滑出，配合 `mobileMenuOpen` 状态。

## Global Constraints

- 只修改 `project/user/web/src/views/Home.vue`，不新增路由、不改动其他页面。
- 断点统一使用 `@media (max-width: 768px)`，与项目内现有移动端适配保持一致。
- 手机端头部保留 Logo + 「开始创作」CTA + 汉堡按钮，导航链接与主题切换移入抽屉。
- 抽屉宽度 260px，从右侧滑入，打开时禁止底层页面滚动。
- 所有新增/覆盖样式必须同步支持 `body[data-theme="dark"]`。
- 不引入第三方响应式库或新 UI 组件。
- 验证方式：运行 `npm run dev` 启动 Vite 开发服务器（默认 `http://127.0.0.1:5173/`），再运行 Playwright 脚本。

---

## File Structure

| 文件 | 职责 |
|---|---|
| `project/user/web/src/views/Home.vue` | 首页组件，新增抽屉 DOM、响应式状态、移动端样式与黑暗主题覆盖 |
| `tests/e2e/verify_home_mobile.py` | 新增 Playwright 验证脚本：检查手机端汉堡按钮、抽屉开关、桌面端导航展示、各区块无溢出 |
| `tests/e2e/screenshots/home_mobile_*.png` | 验证脚本生成的参考截图（按需保留） |

---

## Task 1: 创建 Playwright 验证脚本（先写失败测试）

**Files:**
- Create: `tests/e2e/verify_home_mobile.py`

**Interfaces:**
- Consumes: 无
- Produces: 一个可独立运行的验证脚本，后续任务依赖其断言通过

- [ ] **Step 1: 编写失败测试脚本**

在 `tests/e2e/verify_home_mobile.py` 写入以下内容：

```python
from playwright.sync_api import sync_playwright, expect

BASE_URL = "http://127.0.0.1:5173/"


def test_home_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        # 1. 汉堡按钮可见
        menu_btn = page.locator(".mobile-menu-toggle")
        expect(menu_btn).to_be_visible()

        # 2. 桌面导航链接在手机端不可见
        desktop_link = page.locator(".nav-link").first
        expect(desktop_link).not_to_be_visible()

        # 3. 打开抽屉
        menu_btn.click()
        drawer = page.locator(".mobile-drawer")
        expect(drawer).to_have_class(/open/)

        # 4. 抽屉内存在导航链接
        expect(page.locator(".mobile-drawer a:has-text('首页')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('会员')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('玩法指南')")).to_be_visible()

        # 5. 点击链接后抽屉关闭
        page.locator(".mobile-drawer a:has-text('玩法指南')").click()
        page.wait_for_timeout(300)
        expect(page).to_have_url(f"{BASE_URL}guide")
        expect(page.locator(".mobile-drawer")).not_to_have_class(/open/)

        # 6. 截图保存
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.screenshot(path="tests/e2e/screenshots/home_mobile_375.png", full_page=True)

        mobile.close()

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 800})
        page = desktop.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        # 7. 桌面导航链接可见，汉堡按钮不可见
        expect(page.locator(".nav-link").first).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).not_to_be_visible()

        page.screenshot(path="tests/e2e/screenshots/home_desktop_1280.png", full_page=True)

        desktop.close()
        browser.close()


if __name__ == "__main__":
    test_home_mobile()
    print("Home mobile verification passed.")
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd project/user/web && npm run dev &
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_home_mobile.py
```

Expected: 失败，提示 `.mobile-menu-toggle` 未找到。

- [ ] **Step 3: 提交测试脚本**

```bash
git add tests/e2e/verify_home_mobile.py
git commit -m "$(cat <<'EOF'
test(user): 添加首页移动端 Playwright 验证脚本

覆盖手机端汉堡按钮、抽屉开关、链接跳转及桌面端导航展示。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: 实现手机端头部抽屉（Markup + State）

**Files:**
- Modify: `project/user/web/src/views/Home.vue`（template 与 script）

**Interfaces:**
- Consumes: 现有 `toggleTheme` 函数、`currentTheme` ref
- Produces: `mobileMenuOpen` ref，`.mobile-menu-toggle`、`.mobile-drawer`、`.nav-link-desktop`、`.theme-toggle-desktop` 等 DOM/类名

- [ ] **Step 1: 引入 `watch` 与 `onUnmounted**

在 `<script setup>` 顶部将 `ref, onMounted` 改为：

```js
import { ref, onMounted, onUnmounted, watch } from 'vue'
```

- [ ] **Step 2: 新增抽屉状态与 body 滚动控制**

在 `currentTheme` ref 下方添加：

```js
const mobileMenuOpen = ref(false)

watch(mobileMenuOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onUnmounted(() => {
  document.body.style.overflow = ''
})
```

- [ ] **Step 3: 改造顶部导航模板**

将现有 `<header class="home-nav">...</header>` 替换为：

```vue
<header class="home-nav">
  <div class="nav-brand">
    <img
      src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
      alt="爱创作"
      class="nav-logo"
    />
    <span class="nav-brand-name">爱创作</span>
  </div>
  <div class="nav-links">
    <router-link to="/" class="nav-link nav-link-desktop active">首页</router-link>
    <router-link to="/pricing" class="nav-link nav-link-desktop">会员</router-link>
    <router-link to="/guide" class="nav-link nav-link-desktop">玩法指南</router-link>
    <button
      class="theme-toggle theme-toggle-desktop"
      :title="currentTheme === 'light' ? '切换深色主题' : '切换浅色主题'"
      @click="toggleTheme"
    >
        <svg
          v-if="currentTheme === 'light'"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>
        <svg
          v-else
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="5" />
          <line x1="12" y1="1" x2="12" y2="3" />
          <line x1="12" y1="21" x2="12" y2="23" />
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
          <line x1="1" y1="12" x2="3" y2="12" />
          <line x1="21" y1="12" x2="23" y2="12" />
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
          <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
        </svg>
    </button>
    <router-link to="/login" class="nav-cta">开始创作</router-link>
    <button
      class="mobile-menu-toggle"
      aria-label="打开菜单"
      @click="mobileMenuOpen = !mobileMenuOpen"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="3" y1="6" x2="21" y2="6" />
        <line x1="3" y1="12" x2="21" y2="12" />
        <line x1="3" y1="18" x2="21" y2="18" />
      </svg>
    </button>
  </div>
</header>

<!-- 移动端抽屉 -->
<div
  v-if="mobileMenuOpen"
  class="mobile-drawer-backdrop"
  @click="mobileMenuOpen = false"
/>
<div :class="['mobile-drawer', { open: mobileMenuOpen }]">
  <div class="mobile-drawer-header">
    <span class="mobile-drawer-title">菜单</span>
    <button
      class="mobile-drawer-close"
      aria-label="关闭菜单"
      @click="mobileMenuOpen = false"
    >
      ×
    </button>
  </div>
  <nav class="mobile-drawer-nav">
    <router-link to="/" class="mobile-drawer-link" @click="mobileMenuOpen = false">首页</router-link>
    <router-link to="/pricing" class="mobile-drawer-link" @click="mobileMenuOpen = false">会员</router-link>
    <router-link to="/guide" class="mobile-drawer-link" @click="mobileMenuOpen = false">玩法指南</router-link>
  </nav>
  <div class="mobile-drawer-footer">
    <button class="mobile-drawer-theme" @click="toggleTheme">
      <svg
        v-if="currentTheme === 'light'"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
      </svg>
      <svg
        v-else
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="12" r="5" />
        <line x1="12" y1="1" x2="12" y2="3" />
        <line x1="12" y1="21" x2="12" y2="23" />
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
        <line x1="1" y1="12" x2="3" y2="12" />
        <line x1="21" y1="12" x2="23" y2="12" />
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
      </svg>
      <span>{{ currentTheme === 'light' ? '切换深色主题' : '切换浅色主题' }}</span>
    </button>
  </div>
</div>
```

- [ ] **Step 4: 运行测试确认头部相关断言通过**

```bash
python3 tests/e2e/verify_home_mobile.py
```

Expected: 通过汉堡按钮可见、桌面链接隐藏、抽屉打开、链接存在、跳转后抽屉关闭的断言。此时抽屉尚未有动画样式，但功能已可用。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 首页头部新增手机端抽屉菜单 DOM 与状态

引入 mobileMenuOpen 控制抽屉，导航链接与主题切换移入抽屉，
顶部保留 Logo、CTA 与汉堡按钮。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: 实现手机端头部与抽屉样式

**Files:**
- Modify: `project/user/web/src/views/Home.vue`（style 部分）

**Interfaces:**
- Consumes: Task 2 中新增的 `.mobile-menu-toggle`、`.mobile-drawer`、`.nav-link-desktop`、`.theme-toggle-desktop` 等类名
- Produces: 视觉正确的手机端头部与抽屉，黑暗主题兼容

- [ ] **Step 1: 在 `<style scoped>` 末尾添加以下样式块**

```css
/* ========== 手机端导航与抽屉 ========== */

/* 桌面导航链接容器 */
.nav-link-desktop,
.theme-toggle-desktop {
  display: inline-flex;
}

/* 汉堡按钮 */
.mobile-menu-toggle {
  display: none;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mobile-menu-toggle:hover {
  background: #FFF5F7;
  color: #FF2442;
}

.mobile-menu-toggle svg {
  width: 20px;
  height: 20px;
}

/* 抽屉遮罩 */
.mobile-drawer-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
}

/* 抽屉面板 */
.mobile-drawer {
  display: none;
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 260px;
  background: #fff;
  z-index: 100;
  transform: translateX(100%);
  transition: transform 0.25s ease;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.1);
  flex-direction: column;
}

.mobile-drawer.open {
  transform: translateX(0);
}

.mobile-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.mobile-drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.mobile-drawer-close {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mobile-drawer-close:hover {
  background: #f5f5f5;
  color: #FF2442;
}

.mobile-drawer-nav {
  flex: 1;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-drawer-link {
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  transition: all 0.2s;
}

.mobile-drawer-link:hover,
.mobile-drawer-link.active {
  background: #FFF5F7;
  color: #FF2442;
}

.mobile-drawer-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.mobile-drawer-theme {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #fff;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.mobile-drawer-theme:hover {
  border-color: #FFCBD4;
  color: #FF2442;
  background: #FFF5F7;
}

.mobile-drawer-theme svg {
  width: 18px;
  height: 18px;
}

/* ========== 媒体查询：手机端 ≤768px ========== */

@media (max-width: 768px) {
  .home-nav {
    padding: 12px 16px;
  }

  .nav-logo {
    height: 28px;
  }

  .nav-brand-name {
    font-size: 16px;
  }

  .nav-links {
    gap: 12px;
  }

  .nav-link-desktop,
  .theme-toggle-desktop {
    display: none;
  }

  .nav-cta {
    padding: 10px 20px;
    font-size: 13px;
    border-radius: 20px;
  }

  .mobile-menu-toggle {
    display: flex;
  }

  .mobile-drawer-backdrop {
    display: block;
  }

  .mobile-drawer {
    display: flex;
  }
}
```

- [ ] **Step 2: 在样式文件末尾追加黑暗主题覆盖**

```css
/* ========== 手机端抽屉黑暗主题 ========== */

body[data-theme="dark"] .mobile-menu-toggle {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-menu-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer {
  background: #1f1f1f;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .mobile-drawer-header,
body[data-theme="dark"] .mobile-drawer-footer {
  border-color: #303030;
}

body[data-theme="dark"] .mobile-drawer-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .mobile-drawer-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-drawer-close:hover {
  background: #2a2a2a;
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer-link {
  color: #e0e0e0;
}

body[data-theme="dark"] .mobile-drawer-link:hover,
body[data-theme="dark"] .mobile-drawer-link.active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-drawer-theme {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-drawer-theme:hover {
  border-color: rgba(255, 77, 111, 0.5);
  color: #ff4d6f;
  background: rgba(255, 36, 66, 0.15);
}
```

- [ ] **Step 3: 运行测试确认抽屉交互通过**

```bash
python3 tests/e2e/verify_home_mobile.py
```

Expected: 通过抽屉打开、链接存在、跳转后抽屉关闭的断言。

- [ ] **Step 4: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 首页手机端头部抽屉样式与黑暗主题

添加汉堡按钮、抽屉面板、遮罩、关闭与主题切换样式，
覆盖浅色/黑暗主题。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: 实现内容区块响应式样式

**Files:**
- Modify: `project/user/web/src/views/Home.vue`（style 部分，在 Task 3 的 `@media` 块内继续追加）

**Interfaces:**
- Consumes: 现有 `.hero`、`.stats`、`.features`、`.steps`、`.cta-section`、`.home-footer` 类名
- Produces: 手机端下各区块正确的字号、间距与布局

- [ ] **Step 1: 在 Task 3 的 `@media (max-width: 768px)` 块内追加内容区样式**

```css
@media (max-width: 768px) {
  /* ... Task 3 的导航样式保留 ... */

  /* Hero */
  .hero {
    padding: 48px 20px 40px;
  }

  .hero-badge {
    font-size: 12px;
    padding: 5px 12px;
    margin-bottom: 16px;
  }

  .hero-title {
    font-size: 28px;
    line-height: 1.25;
    margin-bottom: 16px;
  }

  .hero-desc {
    font-size: 16px;
    margin-bottom: 24px;
  }

  .hero-btn {
    padding: 14px 32px;
    border-radius: 24px;
    font-size: 16px;
  }

  .hero-checkmarks {
    flex-direction: column;
    gap: 10px;
    margin-top: 24px;
  }

  .check-item {
    justify-content: center;
  }

  .hero-guide-link {
    margin-top: 24px;
  }

  /* 数据区 */
  .stats {
    padding: 32px 20px;
  }

  .stats-inner {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px 12px;
  }

  .stat-num {
    font-size: 28px;
  }

  .stat-label {
    font-size: 13px;
  }

  /* 特色功能 */
  .features {
    padding: 40px 20px;
  }

  .features-header {
    margin-bottom: 32px;
  }

  .features-title {
    font-size: 24px;
  }

  .features-subtitle {
    font-size: 14px;
  }

  .features-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .feature-card {
    padding: 20px;
    border-radius: 12px;
  }

  .feature-icon {
    width: 40px;
    height: 40px;
    margin-bottom: 14px;
  }

  .feature-icon svg {
    width: 20px;
    height: 20px;
  }

  .feature-name {
    font-size: 15px;
  }

  .feature-desc {
    font-size: 13px;
  }

  /* 使用步骤 */
  .steps {
    padding: 40px 20px;
  }

  .steps-title {
    font-size: 24px;
  }

  .steps-subtitle {
    font-size: 14px;
    margin-bottom: 32px;
  }

  .steps-list {
    flex-direction: column;
    gap: 16px;
  }

  .step-item {
    padding: 20px;
  }

  /* 最终 CTA */
  .cta-section {
    padding: 40px 20px;
  }

  .cta-title {
    font-size: 22px;
  }

  .cta-desc {
    font-size: 14px;
    margin-bottom: 24px;
  }

  /* 底部 */
  .home-footer {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    padding: 16px 20px;
  }

  .home-footer span + span::before {
    display: none;
  }
}
```

- [ ] **Step 2: 确认黑暗主题无需额外覆盖**

已在 Task 3 中为 `.mobile-drawer` 系列添加黑暗主题样式。内容区（Hero、stats、features、steps、CTA、footer）的桌面黑暗主题选择器（如 `body[data-theme="dark"] .hero`）在手机端同样生效，因为手机端样式只覆盖字号、间距与布局，不覆盖颜色。若实测发现颜色异常，再单独补充。

- [ ] **Step 3: 运行完整测试并检查截图**

```bash
python3 tests/e2e/verify_home_mobile.py
```

Expected: 所有断言通过，`tests/e2e/screenshots/home_mobile_375.png` 与 `home_desktop_1280.png` 无挤压、截断或重叠。

- [ ] **Step 4: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 首页 Hero、数据、功能、步骤、CTA 与底部响应式样式

在 768px 断点下调整字号、间距、网格与排列方式，保持桌面端与
黑暗主题不变。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: 最终验证与收尾

**Files:**
- Modify: 无（仅运行验证与 review）
- 可能新增： `tests/e2e/screenshots/home_mobile_375.png`、`tests/e2e/screenshots/home_desktop_1280.png`

**Interfaces:**
- Consumes: 前序任务完成的页面与脚本
- Produces: 通过的验证结果与参考截图

- [ ] **Step 1: 启动开发服务器**

```bash
cd project/user/web && npm run dev
```

确认终端显示 `Local: http://127.0.0.1:5173/`。

- [ ] **Step 2: 运行验证脚本**

在另一个终端执行：

```bash
python3 tests/e2e/verify_home_mobile.py
```

Expected output:

```
Home mobile verification passed.
```

- [ ] **Step 3: 人工抽查截图**

打开以下文件检查：

- `tests/e2e/screenshots/home_mobile_375.png`：顶部应只显示 Logo + CTA + 汉堡按钮；Hero 标题字号合理；功能卡片单列；步骤垂直排列。
- `tests/e2e/screenshots/home_desktop_1280.png`：应与改造前一致，顶部显示完整导航链接，无汉堡按钮。

- [ ] **Step 4: 提交截图（如保留）**

```bash
git add tests/e2e/screenshots/home_mobile_375.png tests/e2e/screenshots/home_desktop_1280.png
git commit -m "$(cat <<'EOF'
test(user): 添加首页移动端验证截图

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 5: 更新进度文档（如适用）**

若 `.superpowers/sdd/progress.md` 中有首页移动端相关任务条目，标记为完成。

---

## Self-Review Checklist

### Spec Coverage

| Spec 要求 | 对应任务 |
|---|---|
| 手机端头部不挤压 | Task 2 + Task 3 |
| 汉堡抽屉菜单 | Task 2 + Task 3 |
| 抽屉内链接跳转并关闭 | Task 2 |
| 主题切换在抽屉内 | Task 2 + Task 3 |
| Hero/数据/功能/步骤/CTA/底部响应式 | Task 4 |
| 黑暗主题兼容 | Task 3 + Task 4 |
| 桌面端保持不变 | Task 2 + Task 3 + Task 4 |
| 验收标准可验证 | Task 1 + Task 5 |

### Placeholder Scan

- 无 TBD/TODO。
- 所有步骤包含实际代码或命令。
- 所有文件路径为绝对路径或从仓库根目录出发的相对路径。

### Type Consistency

- `mobileMenuOpen` 类型为 `Ref<boolean>`，在模板、watch、点击事件与 `onUnmounted` 中一致使用。
- `.nav-link-desktop`、`.theme-toggle-desktop`、`.mobile-menu-toggle`、`.mobile-drawer`、`.open` 类名在模板、样式、测试脚本中一致。
- `toggleTheme` 与 `currentTheme` 复用现有实现，无签名变更。

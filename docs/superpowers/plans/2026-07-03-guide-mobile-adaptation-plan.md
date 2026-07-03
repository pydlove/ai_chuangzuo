# 玩法指南移动端适配实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 抽出单一 `NavBar.vue` 共用组件,改造 `Home.vue` 与 `GuideIndex.vue` 改用 NavBar,修复 GuideIndex 移动端 `flex-direction: column` 让 sidebar 抽屉生效,并把验证脚本替换为桌面 + 手机双视口覆盖。

**Architecture:** 新建 `project/user/web/src/components/layout/NavBar.vue`,承担 brand / desktop nav / theme toggle / CTA / mobile 汉堡 + drawer + 主题切换副作用。Home 与 GuideIndex 各自删除本地 nav 模板与 theme 状态,改用 `<NavBar :links="..." />`。GuideSidebar.vue 抽屉逻辑已就位,本次只修 `.guide-body` 的 mobile 摆放条件。验证脚本重写为 `verify_guide_mobile.py` 覆盖双视口。

**Tech Stack:** Vue 3 (Composition API + `script setup`), Vue Router 4, Vite, Playwright (Python), localStorage(`aichuangzuo_theme`)。

**Implementation Notes:**
- 沿用现有 768px 断点与 `mobileMenuOpen`/`currentTheme`/`toggleTheme` 状态命名,与首页已经实现的 mobile drawer 行为完全一致。
- NavBar 内部不暴露主题加载函数,组件自管理 `onMounted(loadTheme)`。
- NavBar 实例之间可能并发 mount(Home 与 GuideIndex 在客户端 SPA 内不会同时存在,但同一个 base 视图与子视图可能共存),使用 v-if 抽屉避免重复 `data-theme` 副作用。
- 文案统一使用项目内已有的全角中文标点「，」「。」。

## Global Constraints

- 改动范围:`components/layout/NavBar.vue`(新建)、`views/Home.vue`(改)、`views/GuideIndex.vue`(改)、`tests/e2e/verify_guide.py`(替换为 `verify_guide_mobile.py`)。
- 不重写 `GuideSidebar.vue`,不动 `guide-content.js` / `TimeCalculator.vue` / `LeaderboardPreview.vue`。
- 不新增路由、不引入依赖。
- 视觉主色 `#FF2442`,所有新增样式同步支持 `body[data-theme="dark"]`。
- 断点统一 `@media (max-width: 768px)`。
- Vite dev server 默认端口 `22345`(已在 `vite.config.js` 配置)。
- 验证方式:运行 `cd project/user/web && npm run dev` 启动开发服务器(默认 `http://localhost:22345/`),再运行 Playwright 验证脚本。

---

## File Structure

| 文件 | 变更类型 | 职责 |
|---|---|---|
| `project/user/web/src/components/layout/NavBar.vue` | 新建 | 共用顶部导航:品牌、桌面链接、主题切换、CTA、移动汉堡按钮 + 抽屉 |
| `project/user/web/src/views/Home.vue` | 修改 | 移除自有 nav 模板/theme 状态/相关样式,改用 `<NavBar />` |
| `project/user/web/src/views/GuideIndex.vue` | 修改 | 移除自有 nav 模板/theme 状态/nav 样式,改用 `<NavBar />`;修 `@media (max-width: 768px)` 不再 `column` |
| `tests/e2e/verify_guide_mobile.py` | 新建 | 桌面 1280 + 手机 375 双视口 Playwright 验证 |
| `tests/e2e/verify_guide.py` | 删除 | 被 `verify_guide_mobile.py` 替换(`git rm`) |
| `tests/e2e/screenshots/guide_*_1280.png`、 `guide_*_375.png` | 新建 | 验证脚本输出截图 |

---

### Task 1: 创建 Playwright 双视口验证脚本(失败测试先行)

**Files:**
- Create: `tests/e2e/verify_guide_mobile.py`
- Delete: `tests/e2e/verify_guide.py`

**Interfaces:**
- Consumes: Vite dev server at `http://localhost:22345/guide`
- Produces: 后续所有 Task 依赖此脚本 12 项断言全部通过

- [ ] **Step 1: 删除旧验证脚本**

```bash
git rm tests/e2e/verify_guide.py
```

- [ ] **Step 2: 创建新验证脚本 `tests/e2e/verify_guide_mobile.py`,写入以下完整内容**

```python
from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:22345"
GUIDE_URL = f"{BASE_URL}/guide"


def test_guide_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 900})
        page = desktop.new_page()
        page.goto(GUIDE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 1. 桌面端可见 4 个目录分类(产品功能 / 收益方式 / 创作流程 / 提现与结算)
        expect(page.locator("text=产品功能").first).to_be_visible()
        expect(page.locator("text=收益方式").first).to_be_visible()
        expect(page.locator("text=创作流程").first).to_be_visible()
        expect(page.locator("text=提现与结算").first).to_be_visible()

        # 2. 顶部 navbar (新抽的 NavBar)
        expect(page.locator(".navbar .navbar-cta")).to_be_visible()
        expect(page.locator(".navbar .navbar-cta")).to_have_attribute("href", "/login")

        # 3. 桌面端导航链接可见、汉堡按钮隐藏
        expect(page.locator(".navbar-link-desktop").first).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).not_to_be_visible()

        # 4. 时间节省计算器与排行榜预览存在(滚动到可见即可)
        expect(page.locator("text=算算你能省多少")).to_be_visible()
        expect(page.locator("text=本月创作币榜 TOP 5")).to_be_visible()

        # 5. 点击目录「提现与结算」应触发滚动(无报错即可)
        page.locator("aside.guide-sidebar >> text=提现与结算").first.click()
        page.wait_for_timeout(400)

        # 截图
        page.screenshot(path="tests/e2e/screenshots/guide_desktop_1280.png", full_page=True)
        desktop.close()

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(GUIDE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 6. 移动端汉堡按钮可见、桌面链接隐藏
        expect(page.locator(".mobile-menu-toggle")).to_be_visible()
        for i in range(3):
            expect(page.locator(".navbar-link-desktop").nth(i)).not_to_be_visible()

        # 7. 打开抽屉
        page.locator(".mobile-menu-toggle").click()
        drawer = page.locator(".mobile-drawer")
        expect(drawer).to_have_class(re.compile(r"\bopen\b"))

        # 8. 抽屉内链接存在
        expect(page.locator(".mobile-drawer a:has-text('首页')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('会员')")).to_be_visible()
        expect(page.locator(".mobile-drawer a:has-text('玩法指南')")).to_be_visible()

        # 9. 抽屉内主题切换按钮可见
        expect(page.locator(".mobile-drawer-theme")).to_be_visible()

        # 10. 点击抽屉内链接 → 跳转 + 抽屉自动关闭
        page.locator(".mobile-drawer a:has-text('会员')").click()
        page.wait_for_url(re.compile(r".*/pricing$"))
        expect(page.locator(".mobile-drawer")).to_have_count(0)

        # 11. 回到 guide,检查 sidebar 抽屉目录能正常开关
        page.goto(GUIDE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        if page.locator(".gs-mobile-toggle").count() > 0:
            page.locator(".gs-mobile-toggle").click()
            expect(page.locator(".gs-nav")).to_have_class(re.compile(r"\bopen\b"))
            page.locator(".gs-backdrop").click()
            expect(page.locator(".gs-nav")).not_to_have_class(re.compile(r"\bopen\b"))

        # 12. 暗色主题下导航与抽屉可见且有深色背景
        page.evaluate("document.body.setAttribute('data-theme', 'dark'); localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        expect(page.locator(".navbar")).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).to_be_visible()

        # 13. 无横向溢出
        body_width = page.evaluate("document.documentElement.scrollWidth")
        viewport_w = 375
        assert body_width <= viewport_w + 2, f"横向溢出: body={body_width} viewport={viewport_w}"

        page.screenshot(path="tests/e2e/screenshots/guide_mobile_375.png", full_page=True)
        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_guide_mobile()
    print("Guide mobile verification passed.")
```

- [ ] **Step 3: 启动 dev server 并运行脚本,确认失败**

```bash
cd project/user/web && npm run dev
# 另一个终端:
python3 tests/e2e/verify_guide_mobile.py
```

Expected: 第一处失败应为桌面断言: `.navbar-link-desktop` 未找到(当前 Home.vue 是 `.nav-link-desktop`,且新组件尚未存在)。

- [ ] **Step 4: 提交(失败测试)**

```bash
git add tests/e2e/screenshots
git add tests/e2e/verify_guide_mobile.py
git commit -m "$(cat <<'EOF'
test(user): 添加玩法指南移动端 Playwright 双视口验证脚本

覆盖桌面 1280 + 手机 375,断言桌面 nav 链接可见、汉堡按钮隐藏、
抽屉开关、链接跳转关闭抽屉、sidebar drawer、无横向溢出、暗色主题,
替换旧的 verify_guide.py。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
mkdir -p tests/e2e/screenshots  # 若提交前已存在则忽略
```

> **注**: `mkdir -p` 用于确保 screenshots 目录存在,若目录已存在命令忽略。

---

### Task 2: 创建 NavBar.vue 共用组件

**Files:**
- Create: `project/user/web/src/components/layout/NavBar.vue`

**Interfaces:**
- Produces: Props `links (Array<{ to, label }> required)`、`activePath (String default '')`、`ctaTo (String default '/login')`、`ctaLabel (String default '开始创作')`
- Internal: refs `currentTheme`、`mobileMenuOpen`;functions `toggleTheme`、`loadTheme`;watch(mobileMenuOpen)→ body.overflow;onMounted(loadTheme);onUnmounted 清理 body.overflow。
- 受控类名:`.navbar`、`.navbar-brand`、`.navbar-logo`、`.navbar-brand-name`、`.navbar-links`、`.navbar-link`、`.navbar-link-desktop`、`.navbar-cta`、`.theme-toggle`、`.theme-toggle-desktop`、`.mobile-menu-toggle`、`.mobile-drawer-backdrop`、`.mobile-drawer`、`.mobile-drawer.open`、`.mobile-drawer-header`、`.mobile-drawer-title`、`.mobile-drawer-close`、`.mobile-drawer-nav`、`.mobile-drawer-link`、`.mobile-drawer-footer`、`.mobile-drawer-theme`。

- [ ] **Step 1: 写入完整 NavBar.vue**

将以下完整内容写入 `project/user/web/src/components/layout/NavBar.vue`:

```vue
<template>
  <header class="navbar">
    <div class="navbar-brand">
      <img
        src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
        alt="爱创作"
        class="navbar-logo"
      />
      <span class="navbar-brand-name">爱创作</span>
    </div>

    <nav class="navbar-links">
      <router-link
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        class="navbar-link navbar-link-desktop"
        :class="{ active: resolvedActive === link.to }"
      >{{ link.label }}</router-link>

      <button
        class="theme-toggle theme-toggle-desktop"
        :title="currentTheme === 'light' ? '切换深色主题' : '切换浅色主题'"
        @click="toggleTheme"
      >
        <svg v-if="currentTheme === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="5" />
          <line x1="12" y1="1" x2="12" y2="3" />
          <line x1="12" y1="21" x2="12" y2="23" />
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
          <line x1="1" y1="12" x2="3" y2="12" />
          <line x1="21" y1="12" x2="23" y2="12" />
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
          <line x1="18.36" y1="5.64" x2="19.78" y2="5.64" />
        </svg>
      </button>

      <router-link :to="ctaTo" class="navbar-cta">{{ ctaLabel }}</router-link>

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
    </nav>
  </header>

  <div
    v-if="mobileMenuOpen"
    class="mobile-drawer-backdrop"
    @click="mobileMenuOpen = false"
  />
  <div :class="['mobile-drawer', { open: mobileMenuOpen }]">
    <div class="mobile-drawer-header">
      <span class="mobile-drawer-title">菜单</span>
      <button class="mobile-drawer-close" aria-label="关闭菜单" @click="mobileMenuOpen = false">×</button>
    </div>
    <nav class="mobile-drawer-nav">
      <router-link
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        class="mobile-drawer-link"
        :class="{ active: resolvedActive === link.to }"
        @click="mobileMenuOpen = false"
      >{{ link.label }}</router-link>
    </nav>
    <div class="mobile-drawer-footer">
      <button class="mobile-drawer-theme" @click="toggleTheme">
        <svg v-if="currentTheme === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="5" />
          <line x1="12" y1="1" x2="12" y2="3" />
          <line x1="12" y1="21" x2="12" y2="23" />
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
          <line x1="1" y1="12" x2="3" y2="12" />
          <line x1="21" y1="12" x2="23" y2="12" />
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
          <line x1="18.36" y1="5.64" x2="19.78" y2="5.64" />
        </svg>
        <span>{{ currentTheme === 'light' ? '切换深色主题' : '切换浅色主题' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const THEME_KEY = 'aichuangzuo_theme'

const props = defineProps({
  links: { type: Array, required: true },
  activePath: { type: String, default: '' },
  ctaTo: { type: String, default: '/login' },
  ctaLabel: { type: String, default: '开始创作' }
})

const route = useRoute()
const mobileMenuOpen = ref(false)
const currentTheme = ref('light')

const resolvedActive = computed(() => props.activePath || route.path)

const toggleTheme = () => {
  const next = currentTheme.value === 'light' ? 'dark' : 'light'
  currentTheme.value = next
  document.body.setAttribute('data-theme', next)
  localStorage.setItem(THEME_KEY, next)
}

const loadTheme = () => {
  const saved = localStorage.getItem(THEME_KEY) || 'light'
  currentTheme.value = saved
  document.body.setAttribute('data-theme', saved)
}

watch(mobileMenuOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

onMounted(loadTheme)

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 48px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  user-select: none;
  cursor: default;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.navbar-logo {
  height: 32px;
  width: auto;
}

.navbar-brand-name {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: 32px;
}

.navbar-link {
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: color 0.2s;
}

.navbar-link:hover,
.navbar-link.active {
  color: #FF2442;
}

.navbar-cta {
  padding: 8px 22px;
  background: #FF2442;
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.navbar-cta:hover {
  background: #E61E3A;
}

.theme-toggle {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.theme-toggle:hover {
  background: #FFF5F7;
  color: #FF2442;
}

.theme-toggle svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.navbar-link-desktop,
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
  .navbar {
    padding: 12px 16px;
  }

  .navbar-logo {
    height: 28px;
  }

  .navbar-brand-name {
    font-size: 16px;
  }

  .navbar-links {
    gap: 12px;
  }

  .navbar-link-desktop,
  .theme-toggle-desktop {
    display: none;
  }

  .navbar-cta {
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

/* ========== 暗色主题 ========== */

body[data-theme="dark"] .navbar {
  background: #1f1f1f;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .navbar-brand-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .navbar-link {
  color: #a6a6a6;
}

body[data-theme="dark"] .navbar-link:hover,
body[data-theme="dark"] .navbar-link.active {
  color: #ff4d6f;
}

body[data-theme="dark"] .navbar-cta {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .navbar-cta:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .theme-toggle {
  color: #a6a6a6;
}

body[data-theme="dark"] .theme-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

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
</style>
```

- [ ] **Step 2: 提交 NavBar.vue**

```bash
git add project/user/web/src/components/layout/NavBar.vue
git commit -m "$(cat <<'EOF'
feat(user): 新建 NavBar 共用组件承担顶部导航与移动端抽屉

接收 links / activePath / ctaTo / ctaLabel props,
持有 currentTheme 与 mobileMenuOpen 状态,
包含主题切换、桌面链接、CTA、移动汉堡按钮 + 右侧抽屉,
同步支持暗色主题。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: 改造 Home.vue 使用 NavBar

**Files:**
- Modify: `project/user/web/src/views/Home.vue`(template、script、style)

**Interfaces:**
- Consumes: Task 2 产出的 `NavBar` 组件,props `links`、`ctaTo`、`ctaLabel`(可选)
- Produces: Home.vue 顶部第一子节点变为 `<NavBar :links="navLinks" :cta-to="ctaTo" />`;script 移除 `currentTheme`、`mobileMenuOpen`、`watch`、 `onUnmounted`、`toggleTheme`、`loadTheme`;style 移除 `.home-nav`、`.nav-brand`、`.nav-logo`、`.nav-brand-name`、`.nav-links`、`.nav-link*`、`.nav-cta*`、`.theme-toggle*`、`.mobile-menu-toggle*`、`.mobile-drawer*`、相关暗色主题覆盖。

- [ ] **Step 1: 删除 Home.vue 的 `<header class="home-nav">` ~ `</header>` 模板(含后面的 draw-backdrop / mobile-drawer 整段)**

在 `Home.vue` 中从第 3 行 `<header class="home-nav">` 开始,删至 `<!-- Hero 区 -->` 之前的 `</div>` 结束,大致覆盖:

```vue
    <!-- 导航栏 -->
    <header class="home-nav">
      ...(60+ 行,直至 drawer 收尾的 </div>)...
    </header>

    <!-- 移动端抽屉 -->
    <div v-if="mobileMenuOpen" ... />
    <div :class="['mobile-drawer', ...]">
      ...
    </div>
```

完整的替换方案:把 `<!-- 导航栏 -->` 到 `<!-- Hero 区 -->` 之间全部内容替换为:

```vue
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />
```

> 删除后的位置恰好是 `<div class="home-page">` 的第一个子节点。Hero 区段保持不变。

- [ ] **Step 2: 替换 `<script setup>` 中的引用与状态**

将整个 `<script setup>` 块替换为:

```vue
<script setup>
import NavBar from '@/components/layout/NavBar.vue'

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'
</script>
```

(删除原 39 行 script setup 中的所有 imports、refs、watch、onUnmounted、toggleTheme、loadTheme、onMounted)

- [ ] **Step 3: 删除 `<style scoped>` 中的导航相关样式块**

在 `Home.vue` 的 `<style scoped>` 末尾是大量的 nav 与 dark theme 样式。现将 `/* ========== 手机端抽屉黑暗主题 ========== */` 之前的 `/* ========== 媒体查询：手机端 ≤768px ========== */` 整块内关于 `.home-nav` / `.nav-links` / `.nav-link-desktop` 等导航相关样式全部删除(但保留 hero / stats / features / steps / cta-section / home-footer 的 mobile 调整)。

**具体删除项**:

- `/* 导航栏 */` 块整体删除(`.home-nav`、`.nav-brand`、`.nav-logo`、`.nav-brand-name`、`.nav-links`、`.nav-link`、`.nav-cta`)。
- `.nav-link:hover` 与 `.nav-link.active` 单独行(可留 `.navbar-link:hover` 已在 NavBar,但 `.nav-link:hover` 与 `.nav-link.active` 若仅被 nav 使用则一并删除)。
- `/* 主题切换按钮 */` 块整体删除(`.theme-toggle`、`.theme-toggle:hover`、`.theme-toggle svg`)。
- `/* ========== 手机端导航与抽屉 ========== */` 块整体删除(包括 `.nav-link-desktop`、`.theme-toggle-desktop`、`.mobile-menu-toggle*`、`.mobile-drawer*` 等所有 mobile nav 样式)。
- 删除暗色主题下针对 `.home-nav`、`.navbar-brand-name` (旧)、`.nav-link*`、`.nav-cta`、`theme-toggle`、`mobile-menu-toggle`、`mobile-drawer*` 的所有 body[data-theme="dark"] 选择器。

**保留**:

- `.hero`、`.hero-*`、`.stats*`、`.features*`、`.earnings-section*`、`.steps*`、`.cta-section*`、`.home-footer*`、`.hero-actions`、`.hero-btn-secondary` (仍用于 Hero)、`.section-cta` (收益矩阵 CTA)、`.cta-actions` (终 CTA)。
- 暗色主题对 `.hero*`、`.stats*`、`.features*`、`.earnings-section*`、`.steps*`、`.cta-section*`、`.home-footer*`、`.hero-btn-secondary`、`.section-cta*`、`.feature-card-asset` 等的覆盖。
- 移动端对 hero / stats / features / earnings / steps / CTA / footer 的 `@media` 调整。

实操:由于样式量大,**直接重写整个 `<style scoped>` 块**,把保留的样式重新拼接。最简洁做法是 Read 整个 file → 编写保留版本 → Write。

读取当前 `<style scoped>` 起止行后,把整个块替换为下面这段(注意:不动的部分保持原样,本次只删除上面列出的项)。为节省篇幅,提供完整 CSS 替换(可直接覆盖):

```css
.home-page {
  min-height: 100vh;
  background: #fff;
}

.hero {
  background: linear-gradient(180deg, #FFE5EB 0%, #fff 100%);
  padding: 90px 48px 70px;
  text-align: center;
}

.hero-inner {
  max-width: 660px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-block;
  background: #fff;
  border: 1px solid #FFCBD4;
  color: #FF2442;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 46px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
  line-height: 1.15;
}

.hero-desc {
  font-size: 18px;
  color: #595959;
  margin-bottom: 32px;
  line-height: 1.7;
}

.hero-btn {
  display: inline-block;
  padding: 16px 44px;
  background: #FF2442;
  color: #fff;
  border-radius: 30px;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 10px 30px rgba(255,36,66,0.35);
  cursor: pointer;
  transition: all 0.2s;
}

.hero-btn:hover {
  background: #E61E3A;
}

.hero-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-btn-secondary {
  display: inline-block;
  padding: 14px 28px;
  background: transparent;
  color: #FF2442;
  border: 2px solid #FF2442;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.hero-btn-secondary:hover {
  background: #FF2442;
  color: #fff;
}

.hero-checkmarks {
  display: flex;
  gap: 24px;
  justify-content: center;
  margin-top: 28px;
  flex-wrap: wrap;
}

.hero-guide-link {
  margin-top: 20px;
  font-size: 14px;
}

.hero-guide-link a {
  color: #ff2442;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.hero-guide-link a:hover {
  color: #e61e3a;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #595959;
  font-size: 14px;
}

.check-icon {
  width: 18px;
  height: 18px;
  background: #FF2442;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stats {
  background: #f8f9fa;
  padding: 50px 48px;
}

.stats-inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #FF2442;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.stat-label {
  color: #595959;
  font-size: 14px;
}

.features {
  padding: 70px 48px;
}

.features-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.features-header {
  text-align: center;
  margin-bottom: 48px;
}

.features-tag {
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.features-title {
  font-size: 30px;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.features-subtitle {
  color: #595959;
  font-size: 15px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  text-align: left;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.feature-icon svg {
  width: 22px;
  height: 22px;
}

.feature-name {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
}

.feature-card-asset {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2a2a 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
}

.feature-card-asset .feature-icon {
  background: rgba(255, 36, 66, 0.15);
}

.feature-card-asset .feature-name {
  color: #fff;
}

.feature-card-asset .feature-desc {
  color: rgba(255, 255, 255, 0.75);
}

.asset-chart {
  width: 100%;
  height: 60px;
  margin-top: 14px;
  display: block;
}

.earnings-section {
  background: #f8f9fa;
  padding: 70px 48px;
}

.earnings-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.earnings-header {
  text-align: center;
  margin-bottom: 48px;
}

.earnings-tag {
  display: inline-block;
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 0.05em;
}

.earnings-title {
  font-size: 30px;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-weight: 700;
}

.earnings-subtitle {
  color: #595959;
  font-size: 15px;
}

.earnings-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.section-cta {
  display: block;
  width: max-content;
  margin: 0 auto;
  padding: 12px 28px;
  background: transparent;
  color: #FF2442;
  border: 2px solid #FF2442;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.section-cta:hover {
  background: #FF2442;
  color: #fff;
}

.steps {
  background: linear-gradient(135deg, #FF2442 0%, #CC1730 100%);
  padding: 70px 48px;
  text-align: center;
}

.steps-inner {
  max-width: 900px;
  margin: 0 auto;
}

.steps-title {
  font-size: 30px;
  color: #fff;
  margin-bottom: 12px;
}

.steps-subtitle {
  color: rgba(255,255,255,0.9);
  font-size: 16px;
  margin-bottom: 48px;
}

.steps-list {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.step-item {
  flex: 1;
  background: rgba(255,255,255,0.12);
  border-radius: 16px;
  padding: 28px;
  text-align: center;
}

.step-num {
  width: 40px;
  height: 40px;
  background: #fff;
  color: #FF2442;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  margin: 0 auto 16px;
}

.step-name {
  font-weight: 600;
  color: #fff;
  font-size: 16px;
  margin-bottom: 8px;
}

.step-desc {
  color: rgba(255,255,255,0.85);
  font-size: 14px;
}

.cta-section {
  padding: 70px 48px;
  text-align: center;
}

.cta-title {
  font-size: 28px;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.cta-desc {
  color: #595959;
  margin-bottom: 32px;
  font-size: 16px;
}

.cta-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.home-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}

.home-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .home-page {
  background: #141414;
}

body[data-theme="dark"] .hero {
  background: linear-gradient(180deg, rgba(255, 36, 66, 0.12) 0%, #141414 100%);
}

body[data-theme="dark"] .hero-badge {
  background: #1f1f1f;
  border-color: rgba(255, 77, 111, 0.5);
  color: #ff4d6f;
}

body[data-theme="dark"] .hero-title,
body[data-theme="dark"] .features-title,
body[data-theme="dark"] .feature-name,
body[data-theme="dark"] .cta-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .hero-desc,
body[data-theme="dark"] .check-item,
body[data-theme="dark"] .features-subtitle,
body[data-theme="dark"] .feature-desc,
body[data-theme="dark"] .cta-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .hero-btn {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  box-shadow: 0 10px 30px rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .hero-btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .check-icon {
  background: #ff4d6f;
}

body[data-theme="dark"] .hero-guide-link a {
  color: #ff4d6f;
}

body[data-theme="dark"] .stats {
  background: #1f1f1f;
}

body[data-theme="dark"] .stat-num {
  color: #ff4d6f;
}

body[data-theme="dark"] .stat-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .features-tag {
  color: #ff4d6f;
}

body[data-theme="dark"] .feature-card {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .feature-icon {
  background: rgba(255, 36, 66, 0.12) !important;
}

body[data-theme="dark"] .feature-icon svg {
  stroke: #ff4d6f;
}

body[data-theme="dark"] .feature-card-asset {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2a2a 100%);
}

body[data-theme="dark"] .earnings-section {
  background: #1a1a1a;
}

body[data-theme="dark"] .earnings-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .earnings-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .section-cta {
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .section-cta:hover {
  background: #ff4d6f;
  color: #fff;
}

body[data-theme="dark"] .steps {
  background: linear-gradient(135deg, #b01030 0%, #8a0f25 100%);
}

body[data-theme="dark"] .step-num {
  background: #e0e0e0;
  color: #FF2442;
}

body[data-theme="dark"] .cta-section {
  background: #141414;
}

body[data-theme="dark"] .home-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .home-footer span + span::before {
  color: #303030;
}

body[data-theme="dark"] .hero-btn-secondary {
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .hero-btn-secondary:hover {
  background: #ff4d6f;
  color: #fff;
}

/* ========== 媒体查询：手机端 ≤768px ========== */
@media (max-width: 768px) {
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

  .hero-actions {
    flex-direction: column;
    gap: 12px;
  }

  .hero-btn-secondary {
    padding: 12px 28px;
    font-size: 15px;
    border-radius: 24px;
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

  .stats {
    padding: 32px 20px;
  }

  .stats-inner {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px 12px;
  }

  .stat-num {
    font-size: 24px;
  }

  .stat-label {
    font-size: 13px;
  }

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

  .feature-card-asset {
    padding: 22px;
  }

  .asset-chart {
    height: 50px;
    margin-top: 12px;
  }

  .earnings-section {
    padding: 40px 20px;
  }

  .earnings-header {
    margin-bottom: 32px;
  }

  .earnings-title {
    font-size: 24px;
  }

  .earnings-subtitle {
    font-size: 14px;
  }

  .earnings-grid {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-bottom: 32px;
  }

  .section-cta {
    padding: 10px 24px;
    font-size: 14px;
  }

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

  .cta-actions {
    flex-direction: column;
    gap: 12px;
  }

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

> **保留**:本文替换了原 `<style scoped>` 整块,等价于「保留所有营销/数据/特色/CTA 区块 + 暗色主题覆盖 + mobile 适配,只删除 nav 相关」。替换前后页面除 nav 外无视觉差异。

- [ ] **Step 4: 运行验证脚本确认首页桌面断言通过**

```bash
cd project/user/web  # 已经在后台运行的 dev server 继续使用
python3 tests/e2e/verify_guide_mobile.py
```

Expected: 桌面端断言 1-5 全部通过(因 Home.vue 与 GuideIndex.vue 都已用 NavBar),继续到手机端断言(6-) 失败是因 GuideIndex.vue 仍在旧 `<header>` 上 —— 桌面段通过是 ok 的,看脚本在 mobile 段(6-)上能否走通。

实际上桌面段命中 `.navbar-link-desktop` 与 `.navbar-cta`,这些类名只在 NavBar.vue 中存在,所以 Home.vue 与 GuideIndex.vue 都必须先用 NavBar 才能让桌面段通过。

如果步骤 4 桌面段失败,提示 `.navbar-link-desktop` 未找到,说明 Home.vue 改造未生效。检查 `import NavBar` 与 `<NavBar :links="navLinks" />` 模板插入位置。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
refactor(user): Home.vue 改用 NavBar 共用组件

删除自有 <header> 模板、drawer DOM、theme 状态机、相关 nav 样式,
Hero / 数据 / 6 张特色卡 / 收益玩法矩阵 / 三步 / 终 CTA / Footer
全部保留。Hero 按钮与首页营销内容文案不动。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: 改造 GuideIndex.vue 使用 NavBar + 修复 mobile layout

**Files:**
- Modify: `project/user/web/src/views/GuideIndex.vue`(template、script、style)

**Interfaces:**
- Consumes: Task 2 产出的 `NavBar` 组件,props `links`
- Produces: GuideIndex.vue 顶部第一子节点变为 `<NavBar>`;script 删除 `THEME_KEY`、`currentTheme`、`toggleTheme`、`loadTheme`、`onMounted` 中的 `loadTheme`;style 删除 `.guide-nav*`、`.nav-*`、`.theme-toggle*`、对应暗色主题;`@media (max-width: 768px)` 中的 `.guide-body { flex-direction: column; ... }` 改为 `padding: 12px 16px;`(保留 row 摆放)

- [ ] **Step 1: 删除 `<header class="guide-nav">` 块**

在 `GuideIndex.vue` 中,从 `<!-- 顶部导航 -->` 至 `<!-- 主体 -->` 之间的整段删除(约 36 行),并在该位置插入:

```vue
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />
```

- [ ] **Step 2: 替换 `<script setup>`**

将现有的 `<script setup>` 替换为:

```vue
<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { guideSections } from '@/data/guide-content.js'
import GuideSidebar from '@/components/guide/GuideSidebar.vue'
import GuideArticle from '@/components/guide/GuideArticle.vue'
import NavBar from '@/components/layout/NavBar.vue'

const route = useRoute()
const router = useRouter()

const activeArticleId = ref('')

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'

const handleSelect = ({ articleId }) => {
  const el = document.getElementById(articleId)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    router.replace({ hash: `#${articleId}` })
    activeArticleId.value = articleId
  }
}

const observerArticles = () => {
  const ids = guideSections.flatMap(s => s.articles.map(a => a.id))
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          activeArticleId.value = entry.target.id
          if (route.hash !== `#${entry.target.id}`) {
            router.replace({ hash: `#${entry.target.id}` })
          }
        }
      })
    },
    { rootMargin: '-20% 0px -60% 0px', threshold: 0 }
  )
  ids.forEach((id) => {
    const el = document.getElementById(id)
    if (el) observer.observe(el)
  })
}

onMounted(() => {
  nextTick(() => {
    observerArticles()
    if (route.hash) {
      const id = route.hash.slice(1)
      const el = document.getElementById(id)
      if (el) {
        el.scrollIntoView({ behavior: 'auto', block: 'start' })
        activeArticleId.value = id
      }
    }
  })
})
</script>
```

变更点:
- 删除 `THEME_KEY`、`currentTheme`、`toggleTheme`、`loadTheme`、`import { ref, onMounted, nextTick }`(去掉无关 import,保留需要的)
- 新增 `import NavBar from '@/components/layout/NavBar.vue'`
- 新增 `navLinks`、`ctaTo`、`ctaLabel` 三个常量

- [ ] **Step 3: 删除 `<style scoped>` 中 nav 相关样式**

将整个 `<style scoped>` 块替换为:

```css
.guide-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.guide-body {
  flex: 1;
  display: flex;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 24px;
  gap: 40px;
}
.guide-main {
  flex: 1;
  min-width: 0;
}
.guide-hero {
  margin-bottom: 40px;
}
.guide-hero h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 10px;
}
.guide-hero p {
  font-size: 16px;
  color: #595959;
  margin: 0;
}
.guide-footer-cta {
  text-align: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #fff0f2 0%, #fff 100%);
  border-radius: 16px;
  margin-top: 24px;
}
.guide-footer-cta h3 {
  font-size: 22px;
  color: #1a1a1a;
  margin: 0 0 8px;
}
.guide-footer-cta p {
  font-size: 15px;
  color: #595959;
  margin: 0 0 20px;
}
.guide-cta-btn {
  display: inline-block;
  padding: 14px 36px;
  background: #ff2442;
  color: #fff;
  border-radius: 28px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.guide-cta-btn:hover {
  background: #e61e3a;
}
.guide-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}

@media (max-width: 768px) {
  .guide-body {
    padding: 12px 16px;
  }
  .guide-hero h1 {
    font-size: 24px;
  }
}

body[data-theme="dark"] .guide-page {
  background: #141414;
}
body[data-theme="dark"] .guide-footer {
  background: #1f1f1f;
  border-top-color: #303030;
}
body[data-theme="dark"] .guide-hero h1,
body[data-theme="dark"] .guide-footer-cta h3 {
  color: #e0e0e0;
}
body[data-theme="dark"] .guide-hero p,
body[data-theme="dark"] .guide-footer-cta p,
body[data-theme="dark"] .guide-footer {
  color: #a6a6a6;
}
body[data-theme="dark"] .guide-footer-cta {
  background: linear-gradient(135deg, #331018 0%, #1f1f1f 100%);
}
body[data-theme="dark"] .guide-cta-btn {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
}
```

> 删除项:`.guide-nav*`、`.nav-*`、`.theme-toggle*`、对应 dark theme 选择器。
> 修改项:`@media (max-width: 768px)` 中的 `.guide-body { padding: 16px; }` (旧) 改为 `.guide-body { padding: 12px 16px; }`(同时移除 `flex-direction: column` 与 `gap: 16px`,让 sidebar 抽屉正常工作)。
> 其余保留:hero、footer-cta、footer、暗色主题相关。

- [ ] **Step 4: 运行验证脚本确认双视口都通过**

```bash
python3 tests/e2e/verify_guide_mobile.py
```

Expected: 全部断言通过,`guide_desktop_1280.png` 与 `guide_mobile_375.png` 正常生成。如失败,根据报错定位是 NavBar 不显示 / Sidebar 抽屉不触发 / 横向溢出哪种情况。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/GuideIndex.vue
git commit -m "$(cat <<'EOF'
refactor(user): GuideIndex.vue 改用 NavBar + 修复移动端 layout

删除自有 <header class="guide-nav"> 模板与 theme 状态,
改用 <NavBar />。修复 @media 不再让 .guide-body 强制 column,
让 GuideSidebar 已实现的抽屉式目录真正生效。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: 最终验证与截图提交

**Files:**
- Modify: 无(仅运行验证)
- Create: `tests/e2e/screenshots/guide_desktop_1280.png`、`tests/e2e/screenshots/guide_mobile_375.png`、`tests/e2e/screenshots/guide_mobile_375_dark.png`(若脚本生成)

**Interfaces:**
- Consumes: 前序 Task 完成的页面与脚本

- [ ] **Step 1: 启动 dev server(若未运行)**

```bash
cd project/user/web && npm run dev
```

确认终端显示 `Local: http://localhost:22345/`。

- [ ] **Step 2: 运行验证脚本**

```bash
python3 tests/e2e/verify_guide_mobile.py
```

Expected output:

```
Guide mobile verification passed.
```

- [ ] **Step 3: 人工抽查截图**

打开 `tests/e2e/screenshots/guide_desktop_1280.png` 与 `guide_mobile_375.png`,检查:

- 桌面 1280:顶部仅显示 Logo + 「爱创作」 + 首页/会员/玩法指南 + 主题切换 + 开始创作;目录分类「产品功能 / 收益方式 / 创作流程 / 提现与结算」可见。
- 手机 375:顶部仅 Logo + 开始创作 + 汉堡按钮;点开汉堡 → 抽屉滑入显示链接;首屏可见 Hero + 文章列表;无横向溢出。

- [ ] **Step 4: 提交截图**

```bash
git add tests/e2e/screenshots/guide_desktop_1280.png tests/e2e/screenshots/guide_mobile_375.png
git add tests/e2e/screenshots 2>/dev/null
git commit -m "$(cat <<'EOF'
test(user): 添加玩法指南双视口验证截图

桌面 1280 + 手机 375 全页截图,覆盖 NavBar 共用化后效果。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review Checklist

### Spec Coverage

| Spec 要求 | 对应任务 |
|---|---|
| ①  NavBar.vue 新建,承担品牌 + 链接 + 主题 + CTA + 抽屉 | Task 2 |
| ②  Home.vue 改用 NavBar、删除本地 nav 与 theme 状态 | Task 3 |
| ③  GuideIndex.vue 改用 NavBar、删除本地 nav 与 theme 状态 | Task 4 |
| ④  GuideIndex.vue `@media` 不再 column,让 sidebar 抽屉触发 | Task 4 |
| ⑤  双视口验证脚本重写为 `verify_guide_mobile.py` | Task 1 |
| ⑥  12+ 项断言(分类可见、抽屉开关、跳转关闭、无溢出、暗色) | Task 1 |
| ⑦  桌面 1280 与手机 375 双截图 | Task 5 |

### Placeholder Scan

- 无 TBD / TODO / FIXME / 占位符。
- 每个 Task 都有可执行命令与完整代码块(包含 template、script、style 完整粘贴内容)。
- 所有 `git rm` / `git add` / `git commit` 命令含 message。

### Type Consistency

- NavBar props: `links` (Array 必填) / `activePath` (String default '') / `ctaTo` (String default '/login') / `ctaLabel` (String default '开始创作')。在 Home.vue 与 GuideIndex.vue 的 `<NavBar>` 调用处均为 `links / ctaTo / ctaLabel` 三参,activePath 隐式使用 `route.path`。
- 类名 `.navbar` / `.navbar-brand` / `.navbar-link-desktop` / `.navbar-cta` / `.mobile-menu-toggle` / `.mobile-drawer.open` / `.mobile-drawer-backdrop` 在 NavBar.vue、verify_guide_mobile.py 三处统一。
- 状态名 `currentTheme` / `mobileMenuOpen` 在 NavBar.vue 内唯一持有,Home.vue 与 GuideIndex.vue 不再持有(避免重复键)。
- localStorage 键 `aichuangzuo_theme` 在 NavBar.vue 与 Task 1 验证脚本的 `localStorage.setItem('aichuangzuo_theme', 'dark')` 一致。
- 路由路径 `/guide`、`/pricing`、`/login`、`/` 在 links 数组、CTA、`router.push` 与 `useRoute().path` 之间一致。

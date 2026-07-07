# 玩法指南移动端适配设计

**日期**: 2026-07-03
**状态**: 已确认，待实现
**关联文件**:
- `project/user/web/src/components/layout/NavBar.vue`(新建)
- `project/user/web/src/views/Home.vue`(修改)
- `project/user/web/src/views/GuideIndex.vue`(修改)
- `tests/e2e/verify_guide.py`(替换为 `verify_guide_mobile.py`)

---

## 1. 功能概述

对爱创作用户端「玩法指南」(`/guide`)页面与首页做手机端适配,核心动作:

1. **抽出公共 NavBar 组件**,统一承担首页与玩法指南页面的顶部导航(含主题切换、移动端汉堡按钮 + 抽屉),消除两边重复实现。
2. **修复 GuideIndex 现有 `@media (max-width: 768px)` 错误**(强制 `flex-direction: column` 让 sidebar 上下堆叠),让 GuideSidebar 已实现的抽屉式目录真正生效。
3. **重写验证脚本**,覆盖桌面 + 手机双视口,断言抽屉开关、链接跳转、无横向溢出、暗色主题。

> 玩法指南的桌面排版与内容(`useLeaderboard`、`TimeCalculator`、`guide-content.js` 等)已实装,本次不动。

---

## 2. 设计决策

- **改造范围**: 1 个新组件 + 2 个视图改造 + 1 个验证脚本重写。不动 GuideSidebar.vue(已实现抽屉)、不动其他视图、不新增路由、不引入依赖。
- **断点**: `@media (max-width: 768px)`,与首页、玩法指南已有的 768 断点保持一致。
- **NavBar 抽象粒度**: 单一 `NavBar.vue` 承担「品牌 + 桌面链接 + 主题切换 + CTA + 移动汉堡 + 抽屉」全部职责。NavBar 内部维护主题状态、抽屉状态、body overflow 副作用。
- **主题切换复用**: 用 `aichuangzuo_theme` localStorage 键,与首页完全一致。
- **页面不重复持有主题**: Home 与 GuideIndex 都改为「不持有主题状态」,改由 NavBar 唯一持有。这避免重复 key 与重复 `document.body[data-theme]` 写入。
- **不重写 GuideSidebar**: 它的抽屉式目录(`.gs-mobile-toggle` + `.gs-nav.open` + `.gs-backdrop`)已经按 spec 工作,本次只修 `.guide-body` 在 mobile 下的摆放,让抽屉触发条件成立。

---

## 3. 整体架构

```
项目结构
├ components/layout/NavBar.vue     新建
├ views/Home.vue                   改用 <NavBar />
├ views/GuideIndex.vue             改用 <NavBar /> + 修 layout @media
└ tests/e2e/verify_guide_mobile.py 替换 verify_guide.py

NavBar.vue 单一职责
├ Brand(Logo + "爱创作")
├ links(由 props.links 渲染水平 nav)
├ Theme Toggle
├ CTA(由 props.ctaTo/ctaLabel 渲染)
├ Hamburger Button(仅 ≤768px 显示)
└ Drawer Panel
   ├ Nav Links(点击 → 关闭 + 跳转)
   └ Theme Toggle(与顶部同步)
```

---

## 4. NavBar 组件详细设计

### 4.1 Props

| 名称 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `links` | `Array<{ to: string, label: string }>` | 必填 | 桌面与移动抽屉中显示的导航链接 |
| `activePath` | `string` | `''` | 显式指定当前 active 路径;空则用 `useRoute().path` |
| `ctaTo` | `string` | `'/login'` | CTA 按钮跳转 |
| `ctaLabel` | `string` | `'开始创作'` | CTA 按钮文案 |

### 4.2 Template 结构

```vue
<template>
  <header class="navbar">
    <div class="navbar-brand">
      <img src="..." alt="爱创作" class="navbar-logo" />
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

      <button class="theme-toggle theme-toggle-desktop" :title="..." @click="toggleTheme">
        <!-- 月亮/太阳 SVG -->
      </button>

      <router-link :to="ctaTo" class="navbar-cta">{{ ctaLabel }}</router-link>

      <button class="mobile-menu-toggle" aria-label="打开菜单" @click="mobileMenuOpen = !mobileMenuOpen">
        <!-- 汉堡 SVG(3 条线) -->
      </button>
    </nav>
  </header>

  <!-- 移动端抽屉 -->
  <div v-if="mobileMenuOpen" class="mobile-drawer-backdrop" @click="mobileMenuOpen = false" />
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
        <!-- 主题 SVG + 切换文案 -->
      </button>
    </div>
  </div>
</template>
```

### 4.3 Script

```vue
<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({ /* ... */ })

const route = useRoute()
const mobileMenuOpen = ref(false)
const currentTheme = ref('light')

const THEME_KEY = 'aichuangzuo_theme'

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
  // 不主动移除 data-theme(避免影响其他 unmounted 视图)
})
</script>
```

> **注**: `onUnmounted` 不主动移除 `data-theme`,因为该属性是页面级而非组件级;切换路由时 `data-theme` 应保持。

### 4.4 Style(关键规则)

| 元素 | 桌面 | 移动(≤768px) |
|---|---|---|
| `<header class="navbar">` | `padding: 14px 48px` | `padding: 12px 16px` |
| `.navbar-logo` | height 32px | height 28px |
| `.navbar-brand-name` | font-size 18px | font-size 16px |
| `.navbar-links` gap | 32px | 12px |
| `.navbar-link-desktop` 与 `.theme-toggle-desktop` | display: inline-flex | display: none |
| `.mobile-menu-toggle` | display: none | display: flex,32×32 |
| `.navbar-cta` | 8px 22px,font-size 14px | 10px 20px,font-size 13px |
| `.mobile-drawer` | display: none | display: flex; width 260px; `position: fixed; right: 0; top: 0; bottom: 0; background: #fff/1f1f1f; transform: translateX(100%); transition: transform 0.25s ease` |
| `.mobile-drawer.open` | — | `transform: translateX(0)` |
| `.mobile-drawer-backdrop` | display: none | display: block,`position: fixed; inset: 0; background: rgba(0,0,0,0.3); z-index: 99` |
| 抽屉 z-index | — | 99(蒙层)/ 100(面板) |

> 暗色主题覆盖: `.mobile-drawer { background: #1f1f1f; box-shadow: -2px 0 12px rgba(0,0,0,0.5); }`、`.mobile-drawer-title/.mobile-drawer-link { color: #e0e0e0; }` 等;沿用现有首页 dark theme 选择器。

---

## 5. Home.vue 改造要点

### 5.1 Template

- **删除** `<header class="home-nav">...` ~ `</header>`(包含抽屉)全部模板(约 60+ 行)。
- **新增**:`<NavBar :links="navLinks" />`(放在模板最顶,作为第一子节点)。

### 5.2 Script

- **删除** `currentTheme`、`mobileMenuOpen`、`watch(mobileMenuOpen, ...)`、`onUnmounted(...)`、`toggleTheme`、`loadTheme`、`import { ref, onMounted, onUnmounted, watch }` 中 `onUnmounted` 与 `watch`(若仍需则保留,但本视图都不用)。
- **新增**: `import NavBar from '@/components/layout/NavBar.vue'` 与 `const navLinks = [{ to: '/', label: '首页' }, { to: '/pricing', label: '会员' }, { to: '/guide', label: '玩法指南' }]`。

### 5.3 Style

- **删除** 所有顶部导航相关样式: `.home-nav`、`.nav-brand`、`.nav-logo`、`.nav-brand-name`、`.nav-links`、`.nav-link*`、`.nav-cta*`、`.theme-toggle*`(普通与暗色)、`.hero-actions`、`.hero-btn-secondary`(若 CTA 改用 NavBar 的 primary CTA 后该按钮逻辑不一定需要,本次保留——见 §8)。
- **保留**: Hero、数据、特色功能、收益玩法矩阵、三步、终 CTA、Footer 全部样式;以及现有首页的暗色主题覆盖(只删除 nav 相关的覆盖)。

> Hero 主 CTA (`<router-link class="hero-btn">`)、次 CTA (`<router-link class="hero-btn-secondary">`) 由视图保留,不内化进 NavBar,因为它们文案与视觉是关键营销元素。

---

## 6. GuideIndex.vue 改造要点

### 6.1 Template

- **删除** `<header class="guide-nav">...` ~ `</header>` 全部模板(约 36 行)。
- **新增** `<NavBar :links="navLinks" />` 作为最顶子节点。
- **保留**: GuideSidebar、GuideArticle、Hero、`guide-footer-cta`、Footer。

### 6.2 Script

- **删除**:`THEME_KEY`、`currentTheme`、`toggleTheme`、`loadTheme`、`onMounted` 中的 `loadTheme()` 调用(主题改由 NavBar 加载)。
- **保留**:`activeArticleId`、`observerArticles`、`handleSelect`、URL hash 同步。
- **调整**: `onMounted` 仍调用 `nextTick(() => { observerArticles(); ... })`,不调 `loadTheme`。

### 6.3 Style

- **删除**: `.guide-nav`、`.nav-brand`、`.nav-logo`、`.nav-brand-name`、`.nav-links`、`.nav-link*`、`.nav-cta*`、`.theme-toggle*`(普通与暗色)、`.guide-footer span + span::before`、暗色主题对 `.guide-nav`、`.nav-*` 的覆盖。
- **修改** `@media (max-width: 768px)`:
  - 移除 `flex-direction: column` 与 `gap: 16px`(让 sidebar 在 desktop 横向 flex 布局生效;GuideSidebar 自己的 mobile drawer `.gs-nav` 会接管)。
  - 改为:`.guide-body { padding: 12px 16px; }`(保留移动端 padding)。
  - 保留:`.guide-hero h1 { font-size: 24px; }`(hero 字号)。
- **保留**: `.guide-body`、`.guide-main`、`.guide-hero*`、`.guide-footer-cta*`、`.guide-cta-btn*`、`.guide-footer`、所有 sidebar 相关的 (GuideSidebar 组件自己带)、暗色主题对 `.guide-page`、`.guide-footer-cta`、`hero h1` 等的覆盖。

---

## 7. 视觉规范

沿用现有设计系统:

- **主色**:`#FF2442`
- **暗色主题背景**:`#141414`(页面)、`#1f1f1f`(容器)、`#e0e0e0`/`#a6a6a6`(文字)
- **字体**: 沿用现有系统字体栈
- **圆角**: navbar 与 drawer 不带圆角;CTA 按钮 20px
- **过渡**: drawer 0.25s ease
- **断点**: 768px
- **z-index**: drawer 蒙层 99,drawer 面板 100

---

## 8. 验收标准

- [ ] `NavBar.vue` 文件创建,包含 brand + links + theme + cta + 移动端汉堡 + drawer。
- [ ] Home.vue 顶部导航由 `<NavBar>` 渲染,原 nav 模板与样式不再存在。
- [ ] Home.vue 在 dev 模式下不持 `currentTheme` / `mobileMenuOpen` ref(仅 NavBar 持有)。
- [ ] GuideIndex.vue 顶部导航由 `<NavBar>` 渲染。
- [ ] GuideIndex.vue `@media (max-width: 768px)` 不再把 `.guide-body` 强制成 column。
- [ ] 手机 375px 视口下,GuideIndex 顶部汉堡按钮可见,`.mobile-drawer.open` 抽屉滑入。
- [ ] 抽屉内链接点击 → 路由跳转 + 抽屉自动关闭。
- [ ] 桌面 1280px 与手机 375px 视口下均无横向溢出(`scrollWidth ≤ viewport + 2`)。
- [ ] 暗色主题(`body[data-theme="dark"]`)下 NavBar 与 drawer 颜色与原首页一致。
- [ ] `tests/e2e/verify_guide.py`(原文件)被替换为 `verify_guide_mobile.py`,覆盖 §4.1-4.12 全部断言。
- [ ] 截图保存至 `tests/e2e/screenshots/guide_desktop_*.png` 与 `guide_mobile_*.png`。

---

## 9. 不做的范围

- 不重写 GuideSidebar 或其抽屉逻辑(已就绪)。
- 不动 ConsoleLayout、EarningsIndex、LeaderboardIndex 等其他视图。
- 不引入新依赖、新路由或新组件库。
- 不修复或重写 `guide-content.js`、`TimeCalculator.vue`、`LeaderboardPreview.vue`(现有即可)。
- 不抽公共 Footer,本次仅抽象 NavBar。
- 不抽出 useTheme composable(本次受限于抽象一次只做一个组件,且后续 dashboard 内多个视图都加 NavBar 时再做)。

---

## 10. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-07-03 | v1.0 | 初稿:抽 NavBar 共用组件,修 GuideIndex mobile layout, 重写 verify 脚本覆盖双视口 | - |

# 会员页面移动端适配设计

**日期**: 2026-07-04
**状态**: 已确认,待实现
**关联文件**:
- `project/user/web/src/views/Pricing.vue`(主改造)
- `project/user/web/src/views/Login.vue`(抽 NavBar)
- `project/user/web/src/views/Forgot.vue`(抽 NavBar)
- `tests/e2e/verify_pricing_mobile.py`(新建)

---

## 1. 功能概述

会员(Pricing)页面 + 顺带将 Login / Forgot 的导航抽到共用 NavBar 组件,完成:
1. **Pricing 抽 NavBar**:删除自有 header / theme 状态,改用 `<NavBar />`。
2. **Pricing mobile 适配**:
   - 标题区 + 周期切换按钮 + 「查看完整权益对比」链接:缩小 padding 与字号。
   - **3 张定价卡**:mobile 下从 `grid-template-columns: repeat(3, 1fr)` 改为单列堆叠,recommended 卡保留红框 + 热门标签。
   - **权益对比表**:mobile 下保持 4 列表结构(权益/基础/专业/旗舰),但用 `overflow-x: auto` 容器包裹,允许**横向滚动**,表格最小宽度 480px 维持可读性。
   - **底部 footer**:横向 footer span 改为纵向堆叠。
3. **Login / Forgot 抽 NavBar**:Login 与 Forgot 表单本身本次不动,只删自有 nav 模板与 theme 状态,改用 NavBar。
4. **新建 `verify_pricing_mobile.py`**:覆盖双视口 + 抽屉开关 + 卡片单列 + 对比表横向滚动 + 无溢出 + 暗色主题。

> 不动 ConsoleLayout、EarningsIndex、LeaderboardIndex、其他 dashboard 视图;不改 Login / Forgot 表单本身;不引入新依赖、不新增路由。

---

## 2. 设计决策

- **断点**: `@media (max-width: 768px)`,与 Home / Guide 一致。
- **NavBar 复用**:沿用 Task 2 已建好的 `components/layout/NavBar.vue`,三个视图(Pricing / Login / Forgot)各自只引入 NavBar、不持有 theme 与 drawer 状态。
- **对比表横向滚动**:不用 JS 切换 tab、不重构表格;CSS 仅加 `.compare-table-wrap { overflow-x: auto }` + 给表格 `min-width: 480px`。优点:不破坏桌面布局,实现成本最低。
- **Pricing 卡片 mobile 单列**:`grid-template-columns: 1fr`,gap 缩到 16px,卡片内 padding 28→20px,字号按比例缩。
- **CTA 在 NavBar 中**:与首页保持一致,导航上的「开始创作」按钮是主要 CTA,卡片的「立即订阅」按钮在卡片内独立存在,移动端也保留(不替代 nav CTA)。
- **本次不抽出 Footer 组件**。

---

## 3. 整体架构

```
项目结构改动
├ views/Pricing.vue     改用 <NavBar /> + mobile 适配
├ views/Login.vue       改用 <NavBar />
├ views/Forgot.vue      改用 <NavBar />
└ tests/e2e/verify_pricing_mobile.py  新建(双视口)
```

---

## 4. Pricing.vue 详细设计

### 4.1 Template 改动

```vue
<template>
  <div class="pricing-page">
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <div class="pricing-body">
      <div class="pricing-content">
        <h1 class="pricing-title">...</h1>
        <p class="pricing-subtitle">...</p>
        <div class="billing-toggle">...</div>
        <div class="compare-link">...</div>
        <div class="pricing-cards">
          <div v-for="plan in plans" :key="plan.key" class="pricing-card">...</div>
        </div>
      </div>

      <!-- 对比表外层加横向滚动容器 -->
      <div class="compare-table-wrap">
        <div id="pricing-compare" class="compare-section">
          ...
          <table class="compare-table">...</table>
        </div>
      </div>
    </div>

    <footer class="pricing-footer">...</footer>
  </div>
</template>
```

### 4.2 Script 改动

删除 `currentTheme`、`THEME_KEY`、`toggleTheme`、`loadTheme`、`onMounted` 中的 `loadTheme()`。新增:

```js
const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'
```

import 移除 `onMounted`,新增 `import NavBar from '@/components/layout/NavBar.vue'`。

### 4.3 Style 改动

**删除项**:
- `.pricing-nav`、`.nav-brand`、`.nav-logo`、`.nav-brand-name`、`.nav-links`、`.nav-link*`、`.nav-cta*`、`.theme-toggle*` 与对应 dark theme 选择器(`.pricing-nav`、`nav-brand-name`、`nav-link`、`nav-cta`、`theme-toggle`)。

**修改项**:
- `.pricing-body { padding: 40px 24px }` → mobile `padding: 24px 16px`。
- `.pricing-title { font-size: 28px }` → mobile `font-size: 22px`。
- `.pricing-cards { grid-template-columns: repeat(3, 1fr); gap: 24px }` → mobile `grid-template-columns: 1fr; gap: 16px`。
- `.pricing-card { padding: 28px }` → mobile `padding: 20px`。
- `.compare-section { padding: 32px }` → mobile `padding: 20px 16px`。
- `.compare-table { font-size: 14px }` → mobile 表格保持 14px(横向滚动避免字号过小)。
- 新增 `.compare-table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; width: 100%; }`。
- 给 `.compare-table` mobile 加 `min-width: 480px`,确保滚动有意义。
- `.pricing-footer` mobile `flex-direction: column; gap: 4px;`。
- `.pricing-footer span + span::before { display: none }` mobile。

**保留项**:
- 桌面所有原样式不动。
- 所有暗色主题针对 `.pricing-page` / `.pricing-card` / `.compare-*` / `.plan-*` / `.pricing-title` / `.pricing-subtitle` / `.billing-toggle` / `.pricing-footer` 等的覆盖保留。
- `.pricing-card:hover`、`.recommended` 边框等桌面效果保留。

---

## 5. Login.vue / Forgot.vue 改动

### 5.1 Template

删除 `<header class="login-nav">...</header>`(共 ~50 行含抽屉 / theme 按钮 SVG),替换为:

```vue
<NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />
```

注意 Login.vue 的 brand 有 `@click="$router.push('/')"` 行为 —— NavBar 的 brand 区域是纯图片+文字,点击不会跳转。**本次接受此差异**(品牌区在 NavBar 中不可点击跳转,符合当前 Home/Guide 已建立的统一行为)。

### 5.2 Script

- 删除 `THEME_KEY`、`currentTheme`、`toggleTheme`、`loadTheme`、import 中的 `onMounted`(若仍需则保留)。
- 删除 `onMounted(loadTheme)` 调用。
- 新增 `import NavBar from '@/components/layout/NavBar.vue'`。
- 新增 `navLinks` / `ctaTo` / `ctaLabel` 常量。

### 5.3 Style

删除所有 `.login-nav` / `.forgot-nav`、`.nav-brand` / `.nav-logo` / `.nav-brand-name` / `.nav-links` / `.nav-link*` / `.theme-toggle*`、对应 dark theme 选择器。

表单本身(.login-card / .forgot-card / .form-* / .auth-tabs / 滑块 / 弹窗)的样式**不动**。本次不为表单本身做 mobile 适配(超出范围)。

---

## 6. verify_pricing_mobile.py 设计

BASE_URL = `http://localhost:22345`,断言覆盖:

### 桌面 1280
1. 标题文案「每天 3 分钟,AI 帮你写完一篇文章」
2. `.navbar .navbar-cta` 存在且 href=/login
3. 3 张定价卡(`.pricing-card`)全部可见
4. 对比表 4 列 × N 行存在(行数 ≥ 10)
5. 周期切换按钮(`.toggle-btn`)有 3 个,active 默认「月度」
6. 点击「查看完整权益对比」可滚动到对比表
7. 切换到「年度」周期,价格更新(¥59.9 → ¥503.2)

### 手机 375
8. 移动端汉堡按钮可见、桌面链接隐藏
9. 打开抽屉 → 抽屉内链接 + 主题按钮可见
10. 点击抽屉内「玩法指南」跳转 + 抽屉关闭
11. 回到 pricing,3 张定价卡均为单列(每张宽度 ≈ viewport - 32 = 343)
12. 对比表 `.compare-table-wrap` 横向滚动:scrollWidth > clientWidth,scrollLeft=200 后可见
13. 无横向溢出(documentElement.scrollWidth ≤ 377)
14. 暗色主题下 body[data-theme="dark"] 时 .navbar 与卡片背景变深

### 截图
- `tests/e2e/screenshots/pricing_desktop_1280.png`
- `tests/e2e/screenshots/pricing_mobile_375.png`
- `tests/e2e/screenshots/pricing_mobile_375_sheet.png`

---

## 7. 验收标准

- [ ] Pricing.vue 顶部 nav 改为 `<NavBar />`,本地 header 模板与 theme 状态删除
- [ ] Pricing.vue mobile 3 张卡单列,对比表横向可滚动,无 overflow
- [ ] Login.vue / Forgot.vue 顶部 nav 改为 `<NavBar />`,本地 header 与 theme 状态删除
- [ ] 三处脚本 `import { ref, onMounted, onUnmounted, watch }` 内的 `currentTheme` / `THEME_KEY` 全部移除
- [ ] 双视口 14+ 项断言全部通过
- [ ] 截图保存 desktop + mobile 双版本

---

## 8. 不做的范围

- 不重写 Login / Forgot 表单布局(超出范围)
- 不动 ConsoleLayout、EarningsIndex、LeaderboardIndex 等其他视图
- 不抽出 Footer 组件
- 不引入新依赖、新路由、新组件
- 不做 Pricing 后端逻辑、对接支付接口等
- 不改 GuideSidebar、Home、Guide 已就绪的 NavBar

---

## 9. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-07-04 | v1.0 | 初稿:Pricing mobile + Login/Forgot 抽 NavBar | - |
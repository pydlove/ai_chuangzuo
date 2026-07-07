# 会员页面移动端适配实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pricing.vue 抽 NavBar + mobile 卡片单列 + 对比表横向滚动;Login.vue / Forgot.vue 仅抽 NavBar;新建 verify_pricing_mobile.py 覆盖双视口 14+ 断言。

**Architecture:** 三个视图(Pricing / Login / Forgot)各删除自有 `<header class="*-nav">` 模板、theme 状态、相关 nav 样式,改用共用 `<NavBar />`。Pricing 额外做卡片 mobile 单列与对比表 `overflow-x:auto` 容器。验证脚本重写覆盖双视口。

**Tech Stack:** Vue 3 Composition API + `script setup`, Vue Router 4, Vite, Playwright (Python)。

**Implementation Notes:**
- NavBar 组件已建好(`components/layout/NavBar.vue`),无需改动。
- Pricing `<style scoped>` 中要保留大部分原营销/卡片/对比表样式,只删 nav 相关与加上 mobile `@media` 块。
- Login/Forgot 只删 nav,不重写表单与背景。

---

## File Structure

| 文件 | 变更类型 | 职责 |
|---|---|---|
| `project/user/web/src/views/Pricing.vue` | 改 | 抽 NavBar + 卡片 mobile 单列 + 对比表横向滚动 |
| `project/user/web/src/views/Login.vue` | 改 | 仅抽 NavBar |
| `project/user/web/src/views/Forgot.vue` | 改 | 仅抽 NavBar |
| `tests/e2e/verify_pricing_mobile.py` | 新建 | 双视口 + 抽屉 + 卡片单列 + 对比表滚动 + 无溢出 + 暗色 |
| `tests/e2e/screenshots/pricing_*_1280.png`、`pricing_*_375.png`、`pricing_mobile_375_sheet.png` | 新建 | 截图 |

---

## Global Constraints

- 改动范围:3 个 Vue 视图 + 1 个验证脚本。不动 NavBar.vue、GuideSidebar.vue、ConsoleLayout、其他 dashboard 视图。
- 不引入新依赖、新路由、新组件。
- 沿用 `aichuangzuo_theme` localStorage 键与 768px 断点。
- 视觉主色 `#FF2442`,新增 mobile 样式同步支持 `body[data-theme="dark"]`。
- Vite dev server 默认端口 `22345`。

---

### Task 1: 创建 verify_pricing_mobile.py(失败测试先行)

**Files:**
- Create: `tests/e2e/verify_pricing_mobile.py`

**Interfaces:**
- Consumes: Vite dev server at `http://localhost:22345/pricing`
- Produces: 14+ 断言覆盖桌面 + 手机双视口

- [ ] **Step 1: 创建验证脚本**

将以下完整内容写入 `tests/e2e/verify_pricing_mobile.py`:

```python
from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://localhost:22345"
PRICING_URL = f"{BASE_URL}/pricing"


def test_pricing_mobile():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 900})
        page = desktop.new_page()
        page.goto(PRICING_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 1. 标题文案
        expect(page.locator(".pricing-title")).to_have_text("每天 3 分钟，AI 帮你写完一篇文章")

        # 2. NavBar 顶部导航
        expect(page.locator(".navbar .navbar-cta")).to_be_visible()
        expect(page.locator(".navbar .navbar-cta")).to_have_attribute("href", "/login")
        expect(page.locator(".navbar-link-desktop").first).to_be_visible()
        expect(page.locator(".mobile-menu-toggle")).not_to_be_visible()

        # 3. 3 张定价卡
        expect(page.locator(".pricing-card")).to_have_count(3)
        # 3 张卡均为可见且宽度大致相等
        cards = page.locator(".pricing-card").all()
        for c in cards:
            expect(c).to_be_visible()

        # 4. 对比表行数 ≥ 10
        rows = page.locator(".compare-table tbody tr").count()
        assert rows >= 10, f"对比表行数应 ≥ 10: 实际 {rows}"

        # 5. 周期切换按钮 3 个
        expect(page.locator(".toggle-btn")).to_have_count(3)
        expect(page.locator(".toggle-btn.active")).to_have_text("月度")

        # 6. 切换到「年度」周期,价格更新(专业版应显示 ¥503.2)
        page.locator(".toggle-btn:has-text('年度')").click()
        page.wait_for_timeout(300)
        pro_card = page.locator(".pricing-card.recommended")
        expect(pro_card.locator(".plan-price")).to_contain_text("503.2")

        page.screenshot(path="tests/e2e/screenshots/pricing_desktop_1280.png", full_page=True)
        desktop.close()

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(PRICING_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 7. 汉堡按钮可见、桌面链接隐藏
        expect(page.locator(".mobile-menu-toggle")).to_be_visible()
        for i in range(3):
            expect(page.locator(".navbar-link-desktop").nth(i)).not_to_be_visible()

        # 8. 打开抽屉
        page.locator(".mobile-menu-toggle").click()
        sheet = page.locator(".mobile-drawer")
        expect(sheet).to_have_class(re.compile(r"\bopen\b"))
        expect(page.locator(".mobile-drawer a:has-text('玩法指南')")).to_be_visible()
        page.screenshot(path="tests/e2e/screenshots/pricing_mobile_375_sheet.png")

        # 9. 点击抽屉内「玩法指南」跳转 + 抽屉关闭
        page.mouse.click(187, 100)  # 点击 sheet 顶部之上,触发 backdrop 关闭
        expect(sheet).not_to_have_class(re.compile(r"\bopen\b"))
        page.locator(".mobile-menu-toggle").click()
        page.locator(".mobile-drawer a:has-text('玩法指南')").click()
        page.wait_for_url(re.compile(r".*/guide$"))

        # 10. 回到 pricing,检查 3 张定价卡为单列(每张宽度 ≈ viewport)
        page.goto(PRICING_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        card_widths = page.evaluate("""
() => {
  const cards = Array.from(document.querySelectorAll('.pricing-card'));
  return cards.map(c => c.getBoundingClientRect().width);
}
""")
        viewport_w = 375
        # 移动端单列,每张卡应接近 viewport 宽(扣除 padding ≈ 343)
        for w in card_widths:
            assert w > 300, f"移动端单列卡宽应接近 viewport: 实际 {w}"

        # 11. 对比表横向可滚动
        wrap = page.locator(".compare-table-wrap")
        if wrap.count() > 0:
            scroll_state = wrap.evaluate("el => ({scrollW: el.scrollWidth, clientW: el.clientWidth})")
            if scroll_state["scrollW"] > scroll_state["clientW"]:
                wrap.evaluate("el => { el.scrollLeft = 200 }")
                page.wait_for_timeout(200)
                assert wrap.evaluate("el => el.scrollLeft") > 0, "对比表应可横向滚动"
                wrap.evaluate("el => { el.scrollLeft = 0 }")

        # 12. 暗色主题
        page.evaluate("document.body.setAttribute('data-theme', 'dark'); localStorage.setItem('aichuangzuo_theme', 'dark')")
        page.reload()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)
        expect(page.locator(".navbar")).to_be_visible()
        expect(page.locator(".pricing-card")).to_have_count(3)

        # 13. 无横向溢出
        body_width = page.evaluate("document.documentElement.scrollWidth")
        assert body_width <= viewport_w + 2, f"横向溢出: body={body_width} viewport={viewport_w}"

        page.screenshot(path="tests/e2e/screenshots/pricing_mobile_375.png", full_page=True)
        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_pricing_mobile()
    print("Pricing mobile verification passed.")
```

- [ ] **Step 2: 运行脚本,确认失败**

```bash
python3 tests/e2e/verify_pricing_mobile.py 2>&1 | tail -10
```

Expected: 桌面 `.navbar .navbar-cta` 未找到而失败。

- [ ] **Step 3: 提交失败测试**

```bash
git add tests/e2e/verify_pricing_mobile.py
git commit -m "$(cat <<'EOF'
test(user): 添加会员页面移动端 Playwright 双视口验证脚本

覆盖桌面 1280 + 手机 375,断言标题/卡片/对比表/抽屉/周期切换、
手机 3 张卡单列、对比表横向可滚动、无溢出、暗色主题。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: 改造 Pricing.vue 使用 NavBar + mobile 适配

**Files:**
- Modify: `project/user/web/src/views/Pricing.vue`(template、script、style)

**Interfaces:**
- Consumes: Task 2 已建好的 `NavBar` 组件
- Produces: 顶部 nav 改 `<NavBar />`;script 删除 theme 状态;style 删除 `.pricing-nav*` / `.nav-*` / `.theme-toggle*` / 对应 dark theme;新增 mobile `@media` 块:卡片单列、对比表 `overflow-x:auto` 容器、footer 纵向

- [ ] **Step 1: 替换 Pricing.vue 顶部 header 模板**

定位第 4-55 行(`<!-- 导航栏 -->` 到 `</header>`),替换为:

```vue
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />
```

- [ ] **Step 2: 替换 Pricing.vue 的 `<script setup>`**

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

const activeCycle = ref('month')
const cycles = [
  { key: 'month', label: '月度' },
  { key: 'quarter', label: '季度' },
  { key: 'year', label: '年度' }
]

const getPeriodLabel = () => {
  return activeCycle.value === 'month' ? '月'
    : activeCycle.value === 'quarter' ? '季' : '年'
}

const plans = [
  // ...保留原 plans 数据...
]

const getPrice = (plan) => {
  const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
  const cycle = plan[keyMap[activeCycle.value]]
  return { original: cycle.original, current: cycle.current }
}

const getArticles = (plan) => {
  const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
  return plan[keyMap[activeCycle.value]].articles
}

const getSavings = (plan) => {
  const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
  return plan[keyMap[activeCycle.value]].savings || null
}

const compareRows = [
  // ...保留原 compareRows 数据...
]

const getCell = (row, col) => {
  const val = row[col]
  if (val === true) return '<span style="color:#FF2442;font-weight:600;">✓</span>'
  if (val === false) return '<span style="color:#FF2442;font-weight:600;">✗</span>'
  return `<span style="font-weight:500;">${val}</span>`
}

const scrollToCompare = () => {
  document.getElementById('pricing-compare')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>
```

> 注:`plans` / `compareRows` 数据保持原状不动,只在 script 顶部加 NavBar import 与 `navLinks / ctaTo / ctaLabel` 常量,删除 `THEME_KEY / currentTheme / toggleTheme / loadTheme / onMounted(loadTheme)`。

实操:**直接 Read 整个 `<script setup>` 块 → 修改(加 import、加常量、删 theme 状态与 onMounted)→ 保留 plans / compareRows / 价格/文章/储蓄 helper 不动**。

- [ ] **Step 3: 给对比表外层加横向滚动容器**

定位原 `<div id="pricing-compare" class="compare-section">` 这一行,在外层包一层 `<div class="compare-table-wrap">`:

```vue
      <!-- 对比表横向滚动容器 -->
      <div class="compare-table-wrap">
        <div id="pricing-compare" class="compare-section">
          <div class="compare-header">
            <h2>功能权益对比</h2>
            <span class="compare-hint">✓ 包含 · ✗ 不包含</span>
          </div>
          <table class="compare-table">
            ...
          </table>
        </div>
      </div>
```

- [ ] **Step 4: 删除 `<style scoped>` 中 nav 相关样式**

删除以下块:
- `.pricing-nav`、`.nav-brand`、`.nav-logo`、`.nav-brand-name`、`.nav-links`、`.nav-link`、`.nav-link:hover/active`、`.nav-cta`、`.nav-cta:hover`、`.theme-toggle`、`.theme-toggle:hover`、`.theme-toggle svg`(原 312-371 行附近 + 680-704 行)。
- 对应 dark theme 选择器:`.pricing-nav`、`.nav-brand-name`、`.nav-link`、`.nav-link:hover/active`、`.nav-cta`、`.nav-cta:hover`、`.theme-toggle`、`.theme-toggle:hover`。

- [ ] **Step 5: 新增 mobile 适配样式**

在 `<style scoped>` 末尾(暗色主题块之前)新增:

```css
/* ========== 媒体查询：手机端 ≤768px ========== */
@media (max-width: 768px) {
  .pricing-body {
    padding: 24px 16px;
  }
  .pricing-title {
    font-size: 22px;
  }
  .pricing-subtitle {
    font-size: 14px;
    margin-bottom: 20px;
  }
  .billing-toggle {
    margin-bottom: 8px;
  }
  .toggle-btn {
    padding: 6px 14px;
    font-size: 13px;
  }
  .toggle-badge {
    font-size: 11px;
  }
  .compare-link {
    margin-bottom: 20px;
  }
  .pricing-cards {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-bottom: 32px;
  }
  .pricing-card {
    padding: 20px;
  }
  .plan-name {
    font-size: 16px;
  }
  .plan-price {
    font-size: 26px;
  }
  .compare-table-wrap {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    width: 100%;
    margin: 0 -16px;
    padding: 0 16px;
  }
  .compare-section {
    padding: 20px 16px;
    min-width: 0;
  }
  .compare-table {
    min-width: 480px;
  }
  .compare-header h2 {
    font-size: 18px;
  }
  .compare-table th,
  .compare-table td {
    padding: 10px 8px;
    font-size: 13px;
  }
  .pricing-footer {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    padding: 16px 20px;
  }
  .pricing-footer span + span::before {
    display: none;
  }
}
```

- [ ] **Step 6: 运行 verify_pricing_mobile.py 确认通过**

```bash
python3 tests/e2e/verify_pricing_mobile.py 2>&1 | tail -5
```

Expected: 全部 14+ 断言通过。

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/views/Pricing.vue
git commit -m "$(cat <<'EOF'
refactor(user): Pricing.vue 改用 NavBar + mobile 卡片单列与对比表横向滚动

删除自有 <header> 与 theme 状态;mobile 下 3 张定价卡改为单列,
对比表外加 .compare-table-wrap 横向滚动容器(表格 min-width 480px),
footer 改为纵向堆叠。同步保留所有暗色主题对 pricing-card /
plan-* / compare-* / pricing-footer 的覆盖。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Login.vue 抽 NavBar

**Files:**
- Modify: `project/user/web/src/views/Login.vue`(template、script、style)

**Interfaces:**
- Produces: 顶部 nav 改 `<NavBar />`;script 删除 theme 状态与 onMounted(loadTheme);style 删除 `.login-nav` / `.nav-*` / `.theme-toggle*` 与对应 dark theme

- [ ] **Step 1: 替换 Login.vue 顶部 header**

定位第 10-60 行的 `<header class="login-nav">...</header>` 整段,替换为:

```vue
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />
```

- [ ] **Step 2: 修改 script**

定位 import 行,把 `import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'` 改为 `import { ref, reactive, watch, onBeforeUnmount } from 'vue'`(若仍在使用其他生命钩子则保留),新增:

```js
import NavBar from '@/components/layout/NavBar.vue'

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'
```

删除 `THEME_KEY`、`currentTheme`、`toggleTheme`、`loadTheme`、`onMounted(() => { loadTheme() })`(整个 onMounted 若仅用于 theme 则一并删除)。

> 注意:Login.vue 有大量表单/弹窗状态(`loginForm`、`registerForm`、`activeTab`、`sliderModalPassed` 等),**不要删除这些**,只删 theme 相关。

- [ ] **Step 3: 删除 nav 相关样式**

定位 `.login-nav`、`.nav-brand`、`.nav-logo`、`.nav-brand-name`、`.nav-links`、`.nav-link*`、`.theme-toggle*` 与对应 dark theme 选择器,全部删除。

> 注意:Login.vue 的 `<style scoped>` 中表单/卡片/弹窗样式不要动。

- [ ] **Step 4: 验证 Login 页面没有 console error**

```bash
python3 -c "
from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    desktop = browser.new_context(viewport={'width': 1280, 'height': 900})
    page = desktop.new_page()
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.on('console', lambda m: errors.append(m.text) if m.type == 'error' else None)
    page.goto('http://localhost:22345/login', wait_until='domcontentloaded')
    page.wait_for_timeout(2000)
    print('errors:', errors)
    print('navbar visible:', page.locator('.navbar').is_visible())
    browser.close()
"
```

Expected: `errors: []`,navbar 可见。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/Login.vue
git commit -m "$(cat <<'EOF'
refactor(user): Login.vue 改用 NavBar 共用组件

删除自有 <header class="login-nav"> 模板与 theme 状态,
改用 <NavBar />。表单、滑块、登录注册逻辑不动。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Forgot.vue 抽 NavBar

**Files:**
- Modify: `project/user/web/src/views/Forgot.vue`(template、script、style)

**Interfaces:**
- 同 Task 3,但作用于 Forgot.vue

- [ ] **Step 1: 替换 Forgot.vue 顶部 header**

定位 `<header class="forgot-nav">...</header>` 整段,替换为:

```vue
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />
```

- [ ] **Step 2: 修改 script**

按 Task 3 同样的方式:删 theme 状态与 onMounted(loadTheme),新增 NavBar import 与 navLinks / ctaTo / ctaLabel。

- [ ] **Step 3: 删除 nav 相关样式**

按 Task 3 同样的方式:删除 `.forgot-nav`、`.nav-*`、`.theme-toggle*` 与对应 dark theme。表单/卡片样式不动。

- [ ] **Step 4: 验证 Forgot 页面没有 console error**

```bash
python3 -c "
from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    desktop = browser.new_context(viewport={'width': 1280, 'height': 900})
    page = desktop.new_page()
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.on('console', lambda m: errors.append(m.text) if m.type == 'error' else None)
    page.goto('http://localhost:22345/forgot', wait_until='domcontentloaded')
    page.wait_for_timeout(2000)
    print('errors:', errors)
    print('navbar visible:', page.locator('.navbar').is_visible())
    browser.close()
"
```

Expected: `errors: []`,navbar 可见。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/Forgot.vue
git commit -m "$(cat <<'EOF'
refactor(user): Forgot.vue 改用 NavBar 共用组件

删除自有 <header class="forgot-nav"> 模板与 theme 状态,
改用 <NavBar />。表单、忘记密码流程逻辑不动。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: 最终验证 + 现有测试无回归

**Files:**
- 无(仅运行验证)
- Create: 截图(若脚本未自动生成)

**Interfaces:**
- 跑完三个验证脚本,确保没有破坏既有的 Home / Guide 验证

- [ ] **Step 1: 跑 verify_home_marketing.py**

```bash
python3 tests/e2e/verify_home_marketing.py 2>&1 | tail -3
```

Expected: `Home marketing verification passed.`

- [ ] **Step 2: 跑 verify_guide_mobile.py**

```bash
python3 tests/e2e/verify_guide_mobile.py 2>&1 | tail -3
```

Expected: `Guide mobile verification passed.`

- [ ] **Step 3: 跑 verify_pricing_mobile.py**

```bash
python3 tests/e2e/verify_pricing_mobile.py 2>&1 | tail -3
```

Expected: `Pricing mobile verification passed.`

- [ ] **Step 4: 提交截图(若未自动入仓)**

```bash
git add tests/e2e/screenshots/pricing_desktop_1280.png tests/e2e/screenshots/pricing_mobile_375.png tests/e2e/screenshots/pricing_mobile_375_sheet.png
git commit -m "$(cat <<'EOF'
test(user): 添加会员页面双视口验证截图

桌面 1280 + 手机 375 全页截图 + 抽屉打开状态,覆盖
NavBar 共用化后定价页的 mobile 适配效果。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review Checklist

### Spec Coverage

| Spec 要求 | 对应任务 |
|---|---|
| Pricing 抽 NavBar | Task 2 |
| Pricing mobile 卡片单列 | Task 2 Step 5 |
| Pricing mobile 对比表横向滚动 | Task 2 Step 3 + 5 |
| Login 抽 NavBar | Task 3 |
| Forgot 抽 NavBar | Task 4 |
| 新建 verify_pricing_mobile.py 双视口 | Task 1 + Task 5 |
| 截图 desktop + mobile + sheet | Task 5 |

### Placeholder Scan

- 无 TBD / TODO / FIXME。
- 每个 Task 含可执行命令与完整代码块。

### Type Consistency

- NavBar props 在三处调用一致:`links / ctaTo / ctaLabel`。
- 状态名:`currentTheme` / `mobileMenuOpen` 只在 NavBar 内部持有。
- localStorage 键 `aichuangzuo_theme` 一致。
- 路由路径 `/`、`/pricing`、`/guide`、`/login` 在 navLinks 与 CTA 一致。
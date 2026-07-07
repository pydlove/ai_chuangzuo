# 首页营销重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `project/user/web/src/views/Home.vue` 中把首页从「AI 写作工具」叙事升级为「自媒体账号资产 + 多元变现」叙事,新增「收益玩法矩阵」与 6 张特色卡(含差异化的「账号资产复利」卡),重写 Hero、数据、三步与终 CTA,并保证移动端与暗色主题兼容。

**Architecture:** 单文件 `Home.vue` 三段式修改 —— template 增加/重写 6 个区块(导航已存在),script 不新增状态,style 在 `@media (max-width: 768px)` 与 `body[data-theme="dark"]` 选择器下补充新样式。验证用 Playwright 脚本断言文案与 DOM 结构,并在双视口下截图。

**Tech Stack:** Vue 3 + Vite + CSS3 Media Queries + Playwright (Python)

**Implementation Notes:**
- 沿用现有 768px 断点与抽屉状态 `mobileMenuOpen`、`currentTheme`、`toggleTheme`,不新增响应式状态。
- 新增板块统一沿用现有类名风格 (`.xxx-section` / `.xxx-inner` / `.xxx-grid` / `.xxx-card`),便于响应式与暗色主题覆盖。
- 第 6 张特色卡「账号资产复利」使用差异化视觉:深色背景 + 内嵌静态 SVG 增长曲线。
- 文案口吻: Hero 与终 CTA 激情,中段理性。占位数字 `¥360 万+` / `12 万+` / `6 大主流` / `3 分钟` 在 Task 9 实施期可调整。

## Global Constraints

- 只修改 `project/user/web/src/views/Home.vue`,不新增路由、不改动其他页面。
- 视觉主色 `#FF2442`,沿用现有色阶,所有新增/覆盖样式同步支持 `body[data-theme="dark"]`。
- 手机端断点统一 `@media (max-width: 768px)`,与 `2026-07-03-mobile-homepage-plan` 保持一致。
- 不引入第三方营销/响应式库,继续使用内联 SVG。
- 顶部导航、抽屉逻辑、暗色主题切换、移动端适配等已存在实现全部保留,只在新样式中扩展。
- 验证方式: 运行 `cd project/user/web && npm run dev` 启动 Vite 开发服务器 (默认 `http://127.0.0.1:5173/`),再运行 Playwright 验证脚本。

---

## File Structure

| 文件 | 变更类型 | 职责 |
|---|---|---|
| `project/user/web/src/views/Home.vue` | 修改 | 首页模板(6 个区块)、script、style |
| `tests/e2e/verify_home_marketing.py` | 新建 | Playwright 端到端验证脚本:文案断言、卡片数量、CTA 链接、新板块可见性、双视口截图 |
| `tests/e2e/screenshots/home_marketing_*.png` | 新建 | 验证脚本输出(桌面 1280 + 手机 375) |

---

### Task 1: 创建 Playwright 营销验证脚本(失败测试先行)

**Files:**
- Create: `tests/e2e/verify_home_marketing.py`

**Interfaces:**
- Consumes: 现有 Vite dev server at `http://127.0.0.1:5173/`
- Produces: 后续任务依赖此脚本的断言全部通过

- [ ] **Step 1: 编写初始失败测试**

在 `tests/e2e/verify_home_marketing.py` 写入以下内容:

```python
from playwright.sync_api import sync_playwright, expect
import re

BASE_URL = "http://127.0.0.1:5173/"


def test_home_marketing():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # --- 桌面端视口 ---
        desktop = browser.new_context(viewport={"width": 1280, "height": 900})
        page = desktop.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 1. Hero 重写文案
        expect(page.locator(".hero-title")).to_have_text("会增值的自媒体账号,从第一篇文章开始")
        expect(page.locator(".hero-btn")).to_be_visible()
        expect(page.locator(".hero-btn-secondary")).to_have_attribute("href", "/guide")

        # 2. 数据区 4 个数字与标签
        nums = page.locator(".stat-num").all_text_inner_texts()
        assert any("360 万" in n for n in nums), f"未找到 360 万 数字: {nums}"
        assert any("12 万" in n for n in nums), f"未找到 12 万 数字: {nums}"
        assert any("6" in n for n in nums), f"未找到 6 数字: {nums}"
        assert any("3" in n for n in nums), f"未找到 3 数字: {nums}"

        # 3. 特色功能 6 张卡
        feature_cards = page.locator(".features .feature-card")
        expect(feature_cards).to_have_count(6)

        # 4. 收益玩法矩阵 (NEW 板块)
        expect(page.locator(".earnings-section")).to_be_visible()
        earnings_cards = page.locator(".earnings-section .feature-card")
        expect(earnings_cards).to_have_count(4)
        expect(page.locator(".earnings-section .section-cta")).to_have_attribute("href", "/guide")

        # 5. 三步化简
        expect(page.locator(".steps-title")).to_have_text("3 步起一个会增值的账号")
        step_items = page.locator(".steps-list .step-item")
        expect(step_items).to_have_count(3)

        # 6. 终 CTA 主+次双按钮
        expect(page.locator(".cta-section .cta-title")).to_have_text("现在起号,3 个月后看复利")
        expect(page.locator(".cta-section .hero-btn")).to_be_visible()
        expect(page.locator(".cta-section .hero-btn-secondary")).to_have_attribute("href", "/guide")

        # 截图
        page.screenshot(path="tests/e2e/screenshots/home_marketing_1280.png", full_page=True)

        desktop.close()

        # --- 手机端视口 ---
        mobile = browser.new_context(viewport={"width": 375, "height": 812})
        page = mobile.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(800)

        # 7. 手机端:6 张特色卡单列、收益玩法 4 张单列
        expect(feature_cards.first).to_be_visible()
        expect(earnings_cards.first).to_be_visible()

        # 8. 手机端无内容溢出(横向滚动)
        body_width = page.evaluate("document.documentElement.scrollWidth")
        viewport_w = 375
        assert body_width <= viewport_w + 2, f"横向溢出: body={body_width} viewport={viewport_w}"

        # 9. 终 CTA 在手机端主按钮可见
        expect(page.locator(".cta-section .hero-btn")).to_be_visible()

        page.screenshot(path="tests/e2e/screenshots/home_marketing_375.png", full_page=True)
        mobile.close()
        browser.close()


if __name__ == "__main__":
    test_home_marketing()
    print("Home marketing verification passed.")
```

- [ ] **Step 2: 创建截图输出目录**

```bash
mkdir -p tests/e2e/screenshots
```

- [ ] **Step 3: 启动 dev server 并运行脚本,确认失败**

```bash
cd project/user/web && npm run dev &
sleep 4
python3 tests/e2e/verify_home_marketing.py
```

Expected: 失败。第一处失败应为 `Hero 重写文案` 断言(当前标题仍为 `3 分钟写一篇能变现的自媒体文章`)。`Ctrl+C` 关闭 dev server。

- [ ] **Step 4: 提交失败测试**

```bash
git add tests/e2e/verify_home_marketing.py
git commit -m "$(cat <<'EOF'
test(user): 添加首页营销重构 Playwright 验证脚本

覆盖 Hero 文案、数据数字、6 张特色卡、新增收益玩法矩阵、
3 步标题、终 CTA 双按钮及双视口截图与移动端无溢出。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: 重写 Hero 区(模板 + 文案 + 主+次 CTA)

**Files:**
- Modify: `project/user/web/src/views/Home.vue`(template 第 127-145 行区域)

**Interfaces:**
- Consumes: 现有 `<section class="hero">` 与 `.hero-btn`、`.hero-checkmarks`、`.hero-guide-link`
- Produces: 新标题文案、新副标题、新徽章、新 3 checkmarks、主 + 次 CTA(次按钮新增 `.hero-btn-secondary` 类)

- [ ] **Step 1: 替换 `<section class="hero">` 全段**

将 `Home.vue` 第 127-145 行的 Hero 整段替换为:

```vue
<!-- Hero 区 -->
<section class="hero">
  <div class="hero-inner">
    <div class="hero-badge">AI 写作助手 · 多平台变现 · 账号长期增值</div>
    <h1 class="hero-title">会增值的自媒体账号,从第一篇文章开始</h1>
    <p class="hero-desc">
      3 分钟产出一篇能直接发的文章,平台内多重赚钱机制,
      让你的自媒体账号像滚雪球一样,越做越大、越来越值钱。
    </p>
    <div class="hero-actions">
      <router-link to="/login" class="hero-btn">立即开始创作</router-link>
      <router-link to="/guide" class="hero-btn-secondary">看看能赚多少钱</router-link>
    </div>
    <div class="hero-checkmarks">
      <span class="check-item"><span class="check-icon">✓</span>单篇 3 分钟成稿</span>
      <span class="check-item"><span class="check-icon">✓</span>多平台一稿多发变现</span>
      <span class="check-item"><span class="check-icon">✓</span>账号越久越值钱</span>
    </div>
    <div class="hero-guide-link">
      <router-link to="/guide">不知道怎么变现?先看看玩法指南 →</router-link>
    </div>
  </div>
</section>
```

- [ ] **Step 2: 添加 `.hero-actions` 与 `.hero-btn-secondary` 桌面样式**

将以下 CSS 块追加到 `<style scoped>` 中的 `.hero-guide-link` 样式之后 (约第 442 行 `.hero-guide-link a:hover` 之后):

```css
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
```

- [ ] **Step 3: 添加暗色主题下次按钮样式**

在文件末尾 (现有 `body[data-theme="dark"]` 选择器之后) 追加:

```css
body[data-theme="dark"] .hero-btn-secondary {
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .hero-btn-secondary:hover {
  background: #ff4d6f;
  color: #fff;
}
```

- [ ] **Step 4: 运行 Playwright 验证脚本,确认前 3 项断言通过**

```bash
cd project/user/web && npm run dev &
sleep 4
python3 tests/e2e/verify_home_marketing.py
```

Expected: 步骤 1-2 (Hero 文案 + 数据断言) 通过。失败停在步骤 3 (6 张特色卡) 之前。`Ctrl+C` 关闭 dev server。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 重写首页 Hero 区文案与主+次双按钮

标题改为「会增值的自媒体账号,从第一篇文章开始」,
新增 outline 风格「看看能赚多少钱」次按钮,同步支持暗色主题。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: 升级数据区(占位数字 + 收益叙事标签)

**Files:**
- Modify: `project/user/web/src/views/Home.vue`(template 第 147-167 行,style 中 `.stats` 已存在)

**Interfaces:**
- Consumes: 现有 `.stats` / `.stats-inner` / `.stat-item` / `.stat-num` / `.stat-label`
- Produces: 4 个新 `.stat-num` 数字与 `.stat-label` 文案

- [ ] **Step 1: 替换数据区 4 个 item 文本**

将 `Home.vue` 第 150-165 行的 4 个 `.stat-item` 替换为:

```vue
<div class="stats-inner">
  <div class="stat-item">
    <div class="stat-num">¥ 360 万 +</div>
    <div class="stat-label">累计为创作者带来收益</div>
  </div>
  <div class="stat-item">
    <div class="stat-num">12 万 +</div>
    <div class="stat-label">累计注册账号</div>
  </div>
  <div class="stat-item">
    <div class="stat-num">6 大主流</div>
    <div class="stat-label">已覆盖变现平台</div>
  </div>
  <div class="stat-item">
    <div class="stat-num">3 分钟</div>
    <div class="stat-label">平均成稿时间</div>
  </div>
</div>
```

- [ ] **Step 2: 调整 `.stat-num` 字号以适应 `¥` 长字符串(桌面)**

在 `<style scoped>` 中找到现有 `.stat-num` 样式(约第 485 行),将其改为:

```css
.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #FF2442;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}
```

- [ ] **Step 3: 在 `@media (max-width: 768px)` 内同步缩放**

找到 `mobile` 块内的 `.stat-num` 规则(约第 1075 行),将其改为:

```css
.stat-num {
  font-size: 24px;
}
```

- [ ] **Step 4: 运行验证脚本确认前 4 项断言通过**

```bash
cd project/user/web && npm run dev &
sleep 4
python3 tests/e2e/verify_home_marketing.py
```

Expected: 数据区 4 个数字断言通过,继续到特色卡断言(步骤 3)失败。`Ctrl+C` 关闭。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 首页数据区升级为收益+规模双驱动占位

数字改为 ¥360 万+ / 12 万+ / 6 大主流 / 3 分钟,
叙事从工具能力转向「创作者赚到了钱」。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: 特色功能区扩展为 6 张卡(含「账号资产复利」差异化视觉)

**Files:**
- Modify: `project/user/web/src/views/Home.vue`(template 第 169-222 行,style 中 `.features-grid` 与新增 `.feature-card-asset`)

**Interfaces:**
- Consumes: 现有 4 个 `.feature-card`
- Produces: 共 6 张卡 (新增第 5 张「持续变现」与第 6 张「账号资产复利」+ 深色背景 + 增长曲线 SVG),CSS 类 `.feature-card-asset` 与 `.asset-chart`

- [ ] **Step 1: 替换 section features 整段**

将 `Home.vue` 第 169-222 行的 features 段替换为:

```vue
<!-- 特色功能 -->
<section class="features">
  <div class="features-inner">
    <div class="features-header">
      <div class="features-tag">为什么选择爱创作</div>
      <h2 class="features-title">把时间变成账号资产</h2>
      <p class="features-subtitle">不教你写文案,只帮你把内容变成账号流量、持续收益和长期复利</p>
    </div>
    <div class="features-grid">
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="feature-name">3 分钟成稿</div>
        <div class="feature-desc">输入写作方向,AI 自动完成标题、结构、正文。告别 3 小时憋一篇文。</div>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
          </svg>
        </div>
        <div class="feature-name">一稿多发跨平台</div>
        <div class="feature-desc">一次创作,公众号、小红书、抖音、百家号、头条、知乎全部适配,一份内容赚 N 份收益。</div>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </div>
        <div class="feature-name">爆款结构</div>
        <div class="feature-desc">内置高打开率标题、钩子开头、金句结尾,不用懂写作也能产出爆款。</div>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
        </div>
        <div class="feature-name">导出即发布</div>
        <div class="feature-desc">生成后预览、微调、导出 Word,复制到任何平台直接发布,快速变现。</div>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="feature-name">持续变现</div>
        <div class="feature-desc">创作币奖励、邀请好友返利、月榜奖金、外部自媒体收入申报……不是写一篇赚一篇,是越写越能赚。</div>
      </div>
      <div class="feature-card feature-card-asset">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
            <polyline points="17 6 23 6 23 12"/>
          </svg>
        </div>
        <div class="feature-name">账号资产复利</div>
        <div class="feature-desc">自媒体账号像滚雪球 —— 粉丝、内容沉淀、平台权重,会随时间持续累加。早一天起号,早一天开始滚雪球。</div>
        <svg class="asset-chart" viewBox="0 0 240 60" preserveAspectRatio="none">
          <defs>
            <linearGradient id="asset-grad" x1="0" x2="1" y1="0" y2="0">
              <stop offset="0%" stop-color="#FF2442" stop-opacity="0.2"/>
              <stop offset="100%" stop-color="#FF2442" stop-opacity="1"/>
            </linearGradient>
          </defs>
          <polyline points="0,52 30,46 60,42 90,34 120,28 150,20 180,14 210,8 240,4" fill="none" stroke="url(#asset-grad)" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
          <circle cx="240" cy="4" r="3.5" fill="#FF2442"/>
          <text x="234" y="14" fill="#FF2442" font-size="9" font-weight="700">↑</text>
        </svg>
      </div>
    </div>
  </div>
</section>
```

- [ ] **Step 2: 修改 `.features-grid` 桌面布局为 3 列 × 2 行**

找到 `<style scoped>` 中的 `.features-grid` (约第 530 行),替换为:

```css
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
```

> **注**: spec 提到桌面 6 列 vs 3 列 × 2 行的二选一。本计划采用 `3×2` 以保证卡片可读性;若实际宽度允许,后续可调整为 6 列。

- [ ] **Step 3: 添加第 6 张卡差异化样式**

在 `.feature-desc` 样式之后 (约第 570 行),追加:

```css
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
```

- [ ] **Step 4: 浅色主题下卡片描述颜色保持,无需额外覆盖**

`body[data-theme="dark"] .feature-card` 已存在 (约第 794 行) 在桌面+移动端均生效,无需新增。如未来启用浅色资产卡,可另立 PR 切换。

- [ ] **Step 5: 运行验证脚本确认 6 张卡断言通过**

```bash
cd project/user/web && npm run dev &
sleep 4
python3 tests/e2e/verify_home_marketing.py
```

Expected: 步骤 3 (`.features .feature-card` 数量 = 6) 通过,继续到步骤 4 (`.earnings-section` 不存在) 失败。`Ctrl+C` 关闭。

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 首页特色功能扩展为 6 张卡,新增持续变现与账号资产复利

第 6 张「账号资产复利」使用深色背景 + 静态 SVG 增长曲线,
与另外 5 张形成视觉差异化;桌面布局改为 3 列 × 2 行。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: 新增「收益玩法矩阵」板块

**Files:**
- Modify: `project/user/web/src/views/Home.vue`(template,在 features 与 steps 之间插入;style 同步新增)

**Interfaces:**
- Consumes: 现有 `.feature-card` 复用样式
- Produces: 新板块 `.earnings-section` + `.earnings-grid` + `.section-cta`,4 张玩法卡片,深色 outline 风格 CTA,板块 `#f8f9fa` 灰底

- [ ] **Step 1: 在 `</section>` (features 收尾) 与 `<section class="steps">` 之间插入新区块**

找到文件中 `</section>` 紧接 `<section class="steps">` 的位置,将它们之间替换为:

```vue
<!-- 收益玩法矩阵 -->
<section class="earnings-section">
  <div class="earnings-inner">
    <div class="earnings-header">
      <div class="earnings-tag">4 种变现路径</div>
      <h2 class="earnings-title">边写边赚</h2>
      <p class="earnings-subtitle">平台内赚创作币 + 返利 + 奖金,平台外赚自媒体收入</p>
    </div>
    <div class="earnings-grid">
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="6" x2="12" y2="12"/>
            <line x1="12" y1="12" x2="16" y2="14"/>
          </svg>
        </div>
        <div class="feature-name">创作币奖励</div>
        <div class="feature-desc">完成任务、活动、上榜,1 元 = 1 创作币。抵扣会员购买、满 100 可提现到支付宝。</div>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="feature-name">邀请好友返利</div>
        <div class="feature-desc">邀请 3 人 → 3 天会员;好友首单 10% 返利。老带新,你赚会员天数和创作币。</div>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </div>
        <div class="feature-name">排行榜奖金</div>
        <div class="feature-desc">创作币榜、自媒体收入榜,月榜 TOP10 各奖 100 创作币 —— 写得好就上榜。</div>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff1f0;">
          <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <line x1="3" y1="9" x2="21" y2="9"/>
            <line x1="9" y1="21" x2="9" y2="9"/>
          </svg>
        </div>
        <div class="feature-name">自媒体收入申报</div>
        <div class="feature-desc">公众号、小红书、抖音、百家号、头条、知乎 收益申报,记录你的自媒体收入轨迹。</div>
      </div>
    </div>
    <router-link to="/guide" class="section-cta">查看完整玩法 · 看看别人赚了多少 →</router-link>
  </div>
</section>

<!-- 使用步骤 -->
```

- [ ] **Step 2: 添加 `earnings-section` 相关样式**

在 `<style scoped>` 中 `/* 使用步骤 */` 注释之前 (约第 572 行),追加:

```css
/* 收益玩法矩阵 */
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
  display: inline-block;
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

.earnings-header,
.earnings-grid,
.section-cta {
  text-align: center;
}

.section-cta {
  /* 居中 wrapper */
}

.earnings-inner > .section-cta {
  display: block;
  width: max-content;
  margin: 0 auto;
}
```

- [ ] **Step 3: 添加暗色主题覆盖**

在末尾的暗色主题块 `body[data-theme="dark"] .cta-section` 之前,追加:

```css
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
```

- [ ] **Step 4: 运行验证脚本确认收益玩法矩阵断言通过**

```bash
cd project/user/web && npm run dev &
sleep 4
python3 tests/e2e/verify_home_marketing.py
```

Expected: 步骤 4 (.earnings-section 可见 + 4 张卡 + section-cta href=/guide) 通过,继续到步骤 5 (`.steps-title` 文案) 失败。`Ctrl+C` 关闭。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 首页新增「收益玩法矩阵」板块

4 张玩法卡:创作币奖励 / 邀请好友返利 / 排行榜奖金 / 自媒体收入申报,
灰底背景 + outline 风格 CTA 引导跳转玩法指南,同步支持暗色主题。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: 三步开始化简 + 终 CTA 改写(激情收尾)

**Files:**
- Modify: `project/user/web/src/views/Home.vue`(template 的 steps 段与 cta-section 段)

**Interfaces:**
- Consumes: 现有 `.steps`、`.steps-list`、`.step-item`、`.cta-section`、`.cta-title`、`.hero-btn`
- Produces: `.steps-title` 与 3 步骤文案;`.cta-title` / `.cta-desc` 与主+次双按钮 (复用 `.hero-btn-secondary`)

- [ ] **Step 1: 替换三步开始文案**

将 `Home.vue` 中 steps 整段 (约第 225-247 行) 替换为:

```vue
<!-- 使用步骤 -->
<section class="steps">
  <div class="steps-inner">
    <h2 class="steps-title">3 步起一个会增值的账号</h2>
    <p class="steps-subtitle">1 分钟注册,3 分钟第一篇,写不动也能保持账号在涨</p>
    <div class="steps-list">
      <div class="step-item">
        <div class="step-num">1</div>
        <div class="step-name">注册账号</div>
        <div class="step-desc">1 分钟(免费)</div>
      </div>
      <div class="step-item">
        <div class="step-num">2</div>
        <div class="step-name">输入主题</div>
        <div class="step-desc">1 句话(零门槛)</div>
      </div>
      <div class="step-item">
        <div class="step-num">3</div>
        <div class="step-name">AI 产出</div>
        <div class="step-desc">3 分钟可发(一篇成品)</div>
      </div>
    </div>
  </div>
</section>
```

- [ ] **Step 2: 替换终 CTA 段**

将 cta-section 整段 (约第 250-254 行) 替换为:

```vue
<!-- 最终 CTA -->
<section class="cta-section">
  <h2 class="cta-title">现在起号,3 个月后看复利</h2>
  <p class="cta-desc">
    内容慢慢写,账号先到位 ——<br />
    等你准备好赚钱时,雪球已经在滚。
  </p>
  <div class="cta-actions">
    <router-link to="/login" class="hero-btn">立即开始创作</router-link>
    <router-link to="/guide" class="hero-btn-secondary">查看玩法指南</router-link>
  </div>
</section>
```

- [ ] **Step 3: 添加 `.cta-actions` 样式以保证两个按钮水平居中**

在 `.cta-desc` 样式(约第 648 行)之后追加:

```css
.cta-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
```

- [ ] **Step 4: 移动端 `.cta-actions` 同步**

在 `@media (max-width: 768px)` 块内 `.cta-desc` 之后追加:

```css
.cta-actions {
  gap: 12px;
}
```

- [ ] **Step 5: 运行验证脚本确认所有断言通过**

```bash
cd project/user/web && npm run dev &
sleep 4
python3 tests/e2e/verify_home_marketing.py
```

Expected: 所有断言通过,生成两张截图。`Ctrl+C` 关闭 dev server。

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 三步开始化简为「起号」+ 终 CTA 改写为激情收尾

三步骤叙述简化为「注册账号 1 分钟 / 输入主题 1 句话 / AI 产出 3 分钟」;
终 CTA 标题改为「现在起号,3 个月后看复利」,主+次双按钮。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: 新增板块与卡片的移动端响应式适配

**Files:**
- Modify: `project/user/web/src/views/Home.vue`(style 中 `@media (max-width: 768px)` 块)

**Interfaces:**
- Consumes: 现有手机端 nav 与区块样式
- Produces: `.hero-actions`、`.earnings-section`、`.earnings-grid`、`.section-cta`、`.feature-card-asset`、`.cta-actions` 的手机端样式

- [ ] **Step 1: 在 `@media (max-width: 768px)` 内补充新元素样式**

找到 `@media (max-width: 768px)` 块,在 `/* Hero */` 之前的部分 (即 `.mobile-drawer { display: flex; }` 之后) 追加:

```css
/* Hero - 主+次双按钮 */
.hero-actions {
  flex-direction: column;
  gap: 12px;
}

.hero-btn-secondary {
  padding: 12px 28px;
  font-size: 15px;
  border-radius: 24px;
}
```

- [ ] **Step 2: 在 `/* 特色功能 */` 段落内追加第 6 张差异化卡的移动端样式**

在现有的 `.feature-desc { font-size: 13px; }` 之后 (即特色功能块的末尾) 追加:

```css
.feature-card-asset {
  padding: 22px;
}

.asset-chart {
  height: 50px;
  margin-top: 12px;
}
```

- [ ] **Step 3: 在 `/* 使用步骤 */` 块之前追加收益玩法矩阵手机样式**

在 `.features-grid { grid-template-columns: 1fr; gap: 16px; }` 之后 (即特色功能收尾) 追加:

```css
/* 收益玩法矩阵 */
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
```

- [ ] **Step 4: 在最终 CTA 手机样式中追加双按钮紧凑间距**

在现有 `.cta-section { padding: 40px 20px; }` 之后追加:

```css
.cta-actions {
  flex-direction: column;
  gap: 12px;
}
```

- [ ] **Step 5: 添加 `.earnings-section` 与 `.feature-card-asset` 暗色主题增强**

在文件末尾的暗色主题块内,追加 (位于现有 `body[data-theme="dark"] .feature-card` 块附近):

```css
body[data-theme="dark"] .feature-card-asset {
  background: linear-gradient(135deg, #1f1f1f 0%, #2a2a2a 100%);
}
```

- [ ] **Step 6: 运行验证脚本确认手机端断言与截图无溢出**

```bash
cd project/user/web && npm run dev &
sleep 4
python3 tests/e2e/verify_home_marketing.py
```

Expected: 全部断言通过,`home_marketing_375.png` 无内容溢出、6 张卡单列、收益玩法 4 张单列。`Ctrl+C` 关闭。

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "$(cat <<'EOF'
feat(user): 首页新元素移动端响应式与暗色主题覆盖

hero-actions / earnings-section / feature-card-asset / cta-actions
等在 768px 断点下调整布局与字号,保持桌面端不变。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: 最终验证与参考截图提交

**Files:**
- Modify: 无(仅运行验证)
- Create: `tests/e2e/screenshots/home_marketing_1280.png`、`tests/e2e/screenshots/home_marketing_375.png`

**Interfaces:**
- Consumes: 前序任务完成的页面与脚本
- Produces: 通过的验证结果与参考截图

- [ ] **Step 1: 启动 dev server**

```bash
cd project/user/web && npm run dev
```

确认终端显示 `Local: http://127.0.0.1:5173/`。

- [ ] **Step 2: 运行验证脚本**

在另一个终端执行:

```bash
python3 tests/e2e/verify_home_marketing.py
```

Expected output:

```
Home marketing verification passed.
```

- [ ] **Step 3: 人工抽查截图**

打开以下文件检查:

- `tests/e2e/screenshots/home_marketing_1280.png`:Hero 主标题正确;数据区 4 个新数字正确;6 张特色卡 3 列 × 2 行布局,第 6 张为深色背景含增长曲线;新增「收益玩法矩阵」4 张卡可见;三步化简文案正确;终 CTA 双按钮。
- `tests/e2e/screenshots/home_marketing_375.png`:导航保留汉堡按钮;6 张特色卡单列;收益玩法 4 张卡单列;无横向溢出。

- [ ] **Step 4: 提交截图**

```bash
git add tests/e2e/screenshots/home_marketing_1280.png tests/e2e/screenshots/home_marketing_375.png
git commit -m "$(cat <<'EOF'
test(user): 添加首页营销重构验证截图

桌面 1280 + 手机 375 双视口全页截图。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 5: 更新进度文档(如适用)**

若 `.superpowers/sdd/progress.md` 中存在首页营销相关任务,标记为完成。

---

## Self-Review Checklist

### Spec Coverage

| Spec 要求 | 对应任务 |
|---|---|
| ①  顶部导航保留(含玩法指南入口) | Task 1 (步骤 7-9 验证) — 模板未改 |
| ②  Hero 重写:新标题/副标题/徽章/主+次 CTA/新 checkmarks | Task 2 |
| ③  数据区升级:¥360万+ / 12万+ / 6大主流 / 3分钟 | Task 3 |
| ④  特色功能 6 张卡(含第 6 张差异化视觉) | Task 4 |
| ⑤  收益玩法矩阵新板块(4 张卡 + outline CTA) | Task 5 |
| ⑥  三步化简为「3 步起号」 | Task 6 |
| ⑦  终 CTA:「现在起号,3 个月后看复利」+ 主+次按钮 | Task 6 |
| ⑧  移动端 768px 断点适配新元素 | Task 7 |
| ⑨  暗色主题同步覆盖新元素 | Task 2、3、5、7 |
| ⑩  验证脚本覆盖文案/卡片数/双视口/无溢出 | Task 1 + Task 8 |

### Placeholder Scan

- 无 TBD / TODO / FIXME / XXX / `?`。
- 所有插入的 template / style 都给出完整代码,无"参考上文"或"类似处理"。
- 所有命令含 cd/预期输出。

### Type Consistency

- 类名一致: `.hero-btn-secondary`、`.feature-card-asset`、`.asset-chart`、`.earnings-section`、`.earnings-inner`、`.earnings-tag`、`.earnings-title`、`.earnings-subtitle`、`.earnings-grid`、`.section-cta`、`.cta-actions`、`.stats-inner`、`.steps-list`、`.step-item` 在 template、style、验证脚本中三处统一。
- 路由链接: 主 CTA → `/login`,次 CTA / 玩法引导 → `/guide`,验证脚本以 `href` 断言匹配。
- 数字占位 `¥360万+` / `12万+` / `6大主流` / `3分钟` 在 spec、template、验证脚本三处统一为同一字符串(允许空格差异,通过 `in` 匹配)。

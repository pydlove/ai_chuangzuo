# 首页 Redesign 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 根据最新产品定位「自媒体运营流水线」，重新设计用户端 PC 与移动端首页，将核心叙事从「AI 快速成稿 + 账号增值」升级为「先定位、再创作、持续运营变现」。

**Architecture:** 仅改造现有 `Home.vue` 与 `MobileHome.vue` 的模板结构、文案、样式和路由指向；复用现有组件（NavBar、滚动动画、暗色主题）与视觉体系；新增 E2E 测试验证首页关键文案与 CTA 链接。

**Tech Stack:** Vue 3 + Vue Router 4 + Vite + Ant Design Vue（如需要）+ Playwright（E2E 测试）

## Global Constraints

- 不新增路由、页面或后端接口。
- 不引入新的第三方依赖或组件库。
- 主色保持 `#FF2442`，沿用现有暗色主题 CSS 变量。
- 响应式断点保持 `768px`。
- 所有 CTA 主按钮指向 `/console/onboarding`（未登录用户会被登录流程拦截）。
- 文案需与 `docs/superpowers/specs/2026-08-25-homepage-redesign-design.md` 保持一致。
- 改完后必须运行 E2E 测试并通过；需同步检查暗色主题下的渲染。

---

## File Structure

| 文件 | 操作 | 说明 |
|---|---|---|
| `project/user/web/src/components/layout/NavBar.vue` | 修改 | 将首页 NavBar 的 CTA 文案从「开始创作」改为「免费制定方案」，链接改为 `/console/onboarding` |
| `project/user/web/src/views/Home.vue` | 大幅修改 | PC 端首页模板、文案、样式：Hero、痛点区、流水线、对比区、收益矩阵、三步起号、终 CTA |
| `project/user/web/src/views/MobileHome.vue` | 大幅修改 | 移动端首页同步改造 |
| `tests/e2e/verify_home_redesign.py` | 新增 | 验证首页关键文案、链接、板块存在性 |

---

## Task 1: 更新 NavBar CTA

**Files:**
- Modify: `project/user/web/src/components/layout/NavBar.vue`

**Interfaces:**
- Consumes: 现有 `ctaTo`、`ctaLabel` props 或内部状态
- Produces: 首页 NavBar 的 CTA 文案和链接更新

- [ ] **Step 1: 定位 NavBar CTA 配置**

在 `NavBar.vue` 中找到首页使用的 CTA 配置。如果 CTA 是通过 props 传入的（如 `Home.vue` 中 `:cta-to="ctaTo" :cta-label="ctaLabel"`），则修改 `Home.vue` 中的 `ctaTo` 和 `ctaLabel`；如果 NavBar 内部写死，则修改 NavBar 内部。

当前 `Home.vue` 中：
```js
const ctaTo = '/console/workbench'
const ctaLabel = '开始创作'
```

改为：
```js
const ctaTo = '/console/onboarding'
const ctaLabel = '免费制定方案'
```

- [ ] **Step 2: 验证 NavBar 渲染**

启动本地 dev server：
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
npm run dev
```

访问 `http://localhost:5173/`（或项目实际端口），确认顶部 NavBar 右侧 CTA 显示为「免费制定方案」，点击后地址栏变为 `/console/onboarding`。

- [ ] **Step 3: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/web/src/views/Home.vue
git commit -m "feat(home): 首页 NavBar CTA 指向自媒体方案制定"
```

---

## Task 2: 重写 PC 端 Hero 区

**Files:**
- Modify: `project/user/web/src/views/Home.vue:7-61`

**Interfaces:**
- Consumes: 现有 `banners`、`activeBannerIndex`、轮播逻辑
- Produces: 新定位 Hero 文案与 CTA

- [ ] **Step 1: 修改 Hero 文案与 CTA**

将 `Home.vue` 中 Hero 区（约第 7-61 行）替换为：

```vue
    <!-- Hero 区 -->
    <section class="hero">
      <div class="hero-deco hero-deco-1" :style="{ transform: `translateY(${scrollY * 0.15}px)` }"></div>
      <div class="hero-deco hero-deco-2" :style="{ transform: `translateY(${scrollY * 0.08}px)` }"></div>
      <div class="hero-deco hero-deco-3" :style="{ transform: `translateY(${scrollY * 0.12}px)` }"></div>
      <div class="hero-inner">
        <div class="hero-text">
          <div class="hero-badge">
            <span class="hero-badge-dot"></span>
            AI 驱动的自媒体运营流水线
          </div>
          <h1 class="hero-title">普通人做自媒体，<br />先从定位开始</h1>
          <p class="hero-desc">
            不知道写什么、账号做不起来、有专业却不会变现？<br />
            爱创作把复杂的自媒体工作拆成一套可执行的 AI 辅助流程：<br />
            选平台、定赛道、做人设、持续选题、生成文章、发布复盘，一步不落。
          </p>
          <div class="hero-actions">
            <router-link to="/console/onboarding" class="hero-btn">免费制定我的自媒体方案</router-link>
            <router-link to="/guide" class="hero-btn-secondary">看看别人怎么变现</router-link>
          </div>
          <div class="hero-checkmarks">
            <span class="check-item"><span class="check-icon">✓</span>先定位，再创作</span>
            <span class="check-item"><span class="check-icon">✓</span>低粉高赞 + 蓝海赛道</span>
            <span class="check-item"><span class="check-icon">✓</span>注入个人素材，降低同质化</span>
            <span class="check-item"><span class="check-icon">✓</span>不止生成文章，更给运营策略</span>
          </div>
          <div class="hero-guide-link">
            <router-link to="/guide">不知道怎么开始？先看看玩法指南 →</router-link>
          </div>
        </div>
        <div class="hero-visual">
          <img src="/assets/images/小爱-v1.png" alt="AI 顾问小爱" class="hero-mascot" />
          <div class="hero-visual-caption">你的专属自媒体顾问 · 小爱</div>
        </div>
      </div>
    </section>
```

注意：如果右侧不想用吉祥物图片，可保留现有 `hero-banner-carousel` 轮播，但需移除轮播卡片中的「查看详情」文案或保持原样。本计划推荐用吉祥物替换轮播以强化「顾问」定位；若保留轮播，需确保轮播逻辑继续可用。

- [ ] **Step 2: 添加/调整 Hero 右侧视觉样式**

在 `<style scoped>` 中新增 `.hero-visual`、`.hero-mascot`、`.hero-visual-caption` 样式：

```css
.hero-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}
.hero-mascot {
  max-width: 320px;
  width: 100%;
  height: auto;
  filter: drop-shadow(0 16px 32px rgba(255, 36, 66, 0.18));
  animation: float 4s ease-in-out infinite;
}
.hero-visual-caption {
  margin-top: 16px;
  font-size: 14px;
  color: #595959;
  background: rgba(255,255,255,0.8);
  padding: 6px 16px;
  border-radius: 20px;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
```

同时调整 `.hero-inner` 的 `grid-template-columns` 为 `1.3fr 1fr` 或保持 `1.45fr 1fr`。

- [ ] **Step 3: 浏览器验证**

刷新首页，确认：
- 主标题为「普通人做自媒体，先从定位开始」
- 主 CTA 为「免费制定我的自媒体方案」
- 四个 checkmarks 文案正确
- 右侧显示小爱吉祥物

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "feat(home): PC 端 Hero 区按新定位重写"
```

---

## Task 3: 替换「特色功能」为「三类用户痛点区」

**Files:**
- Modify: `project/user/web/src/views/Home.vue:81-165`

**Interfaces:**
- Consumes: 现有 `.feature-card`、`.features-grid`、`.section-tag` 样式
- Produces: 三类用户痛点区 HTML 结构与文案

- [ ] **Step 1: 替换「为什么选择爱创作」板块**

将第 81-165 行的 `features` section 替换为：

```vue
    <!-- 三类用户痛点区 -->
    <section class="features">
      <div class="features-inner">
        <div class="features-header reveal" data-reveal-delay="0">
          <div class="section-tag">你是否也遇到这些问题？</div>
          <h2 class="features-title">不同的人卡在自媒体的不同阶段</h2>
          <p class="features-subtitle">爱创作给的是对应阶段的解决方案，不是一篇万能模板。</p>
        </div>
        <div class="features-grid pain-grid">
          <div class="feature-card pain-card reveal" data-reveal-delay="100">
            <div class="feature-icon pain-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
            <div class="pain-label">完全没做过自媒体</div>
            <div class="feature-name">打开编辑器就发呆，不知道写什么</div>
            <div class="feature-desc">没有方向、没有选题、不知道自己适合什么平台。</div>
            <div class="pain-solution">AI 根据你的背景推荐平台、赛道、人设和今日选题。</div>
            <a href="#pipeline" class="pain-link">这说的就是我 →</a>
          </div>
          <div class="feature-card pain-card reveal" data-reveal-delay="200">
            <div class="feature-icon pain-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
            <div class="pain-label">做过但做不起来</div>
            <div class="feature-name">发了不少却没人看，追热点还被限流</div>
            <div class="feature-desc">内容同质化、不懂平台规则、账号权重低。</div>
            <div class="pain-solution">基于低粉高赞数据推荐蓝海细分赛道，避开同质化。</div>
            <a href="#pipeline" class="pain-link">这说的就是我 →</a>
          </div>
          <div class="feature-card pain-card reveal" data-reveal-delay="300">
            <div class="feature-icon pain-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
              </svg>
            </div>
            <div class="pain-label">有专业经验 / 产品</div>
            <div class="feature-name">有专业知识却不会包装，变不了现</div>
            <div class="feature-desc">有素材但不会选题、不会写标题、不会引流。</div>
            <div class="pain-solution">把人设拆解成内容支柱，持续输出、引流、成交。</div>
            <a href="#pipeline" class="pain-link">这说的就是我 →</a>
          </div>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: 添加痛点区样式**

在 `<style scoped>` 中新增/覆盖：

```css
.pain-grid {
  grid-template-columns: repeat(3, 1fr);
}
.pain-card {
  display: flex;
  flex-direction: column;
}
.pain-label {
  display: inline-block;
  width: max-content;
  font-size: 12px;
  font-weight: 600;
  color: #FF2442;
  background: #FFF0F2;
  padding: 4px 10px;
  border-radius: 12px;
  margin-bottom: 12px;
}
.pain-solution {
  margin-top: auto;
  padding-top: 16px;
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
  line-height: 1.6;
  border-top: 1px dashed #f0f0f0;
}
.pain-link {
  margin-top: 12px;
  font-size: 14px;
  color: #FF2442;
  text-decoration: none;
  font-weight: 500;
}
.pain-link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .pain-grid { grid-template-columns: 1fr; }
}
```

- [ ] **Step 3: 浏览器验证**

刷新首页，确认：
- 区标题为「你是否也遇到这些问题？」
- 三张卡片分别对应新手、重新定位者、垂直经验者
- 每张卡片包含痛点 + 解决方案 + 「这说的就是我」链接

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "feat(home): PC 端新增三类用户痛点区"
```

---

## Task 4: 新增自媒体运营流水线区

**Files:**
- Modify: `project/user/web/src/views/Home.vue`（在痛点区之后插入新 section）

**Interfaces:**
- Consumes: 现有 `.section-tag`、卡片样式
- Produces: 5 步流水线 HTML 结构与样式

- [ ] **Step 1: 在痛点区后插入流水线 section**

在痛点区 `</section>` 后插入：

```vue
    <!-- 自媒体运营流水线 -->
    <section id="pipeline" class="pipeline-section">
      <div class="pipeline-inner">
        <div class="pipeline-header reveal" data-reveal-delay="0">
          <div class="section-tag">自媒体运营流水线</div>
          <h2 class="pipeline-title">不是帮你写一篇文章，<br />而是帮你建立一套可持续的运营方案</h2>
          <p class="pipeline-subtitle">从 0 到 1，再到持续变现，爱创作把每一步都变成固定动作。</p>
        </div>
        <div class="pipeline-track reveal" data-reveal-delay="100">
          <div class="pipeline-step">
            <div class="pipeline-num">1</div>
            <div class="pipeline-name">制定方案</div>
            <div class="pipeline-desc">选平台、定目标、选赛道、做人设</div>
          </div>
          <div class="pipeline-arrow">→</div>
          <div class="pipeline-step">
            <div class="pipeline-num">2</div>
            <div class="pipeline-name">每日选题</div>
            <div class="pipeline-desc">AI 基于方案推荐低粉高赞选题 + 差异化角度</div>
          </div>
          <div class="pipeline-arrow">→</div>
          <div class="pipeline-step">
            <div class="pipeline-num">3</div>
            <div class="pipeline-name">生成文章</div>
            <div class="pipeline-desc">注入个人素材，按平台特性生成可发内容</div>
          </div>
          <div class="pipeline-arrow">→</div>
          <div class="pipeline-step">
            <div class="pipeline-num">4</div>
            <div class="pipeline-name">发布运营</div>
            <div class="pipeline-desc">标题优化、发布时间、发布策略建议</div>
          </div>
          <div class="pipeline-arrow">→</div>
          <div class="pipeline-step">
            <div class="pipeline-num">5</div>
            <div class="pipeline-name">运营复盘</div>
            <div class="pipeline-desc">数据追踪、方案迭代、持续推荐</div>
          </div>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: 添加流水线样式**

在 `<style scoped>` 中新增：

```css
.pipeline-section {
  background: #f8f9fa;
  padding: 80px 48px;
}
.pipeline-inner { max-width: 1100px; margin: 0 auto; }
.pipeline-header { text-align: center; margin-bottom: 56px; }
.pipeline-title {
  font-size: 32px;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-weight: 700;
}
.pipeline-subtitle { color: #595959; font-size: 15px; }
.pipeline-track {
  display: flex;
  align-items: stretch;
  justify-content: center;
  gap: 12px;
}
.pipeline-step {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  padding: 28px 20px;
  text-align: center;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: all 0.3s ease;
}
.pipeline-step:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(255, 36, 66, 0.12);
  border-color: #FFCBD4;
}
.pipeline-num {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #FF4D6F, #FF2442);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  margin: 0 auto 16px;
}
.pipeline-name {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.pipeline-desc { font-size: 13px; color: #595959; line-height: 1.6; }
.pipeline-arrow {
  display: flex;
  align-items: center;
  color: #FF2442;
  font-size: 20px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .pipeline-section { padding: 50px 20px; }
  .pipeline-title { font-size: 24px; }
  .pipeline-track {
    flex-direction: column;
    gap: 16px;
  }
  .pipeline-arrow {
    transform: rotate(90deg);
    justify-content: center;
    padding: 4px 0;
  }
}
```

- [ ] **Step 3: 浏览器验证**

刷新首页，确认：
- 流水线区标题正确
- 5 个步骤水平排列（桌面）
- 步骤之间箭头连接
- 移动端步骤垂直堆叠，箭头旋转

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "feat(home): PC 端新增自媒体运营流水线区"
```

---

## Task 5: 新增前后对比区

**Files:**
- Modify: `project/user/web/src/views/Home.vue`（在流水线区之后、收益矩阵之前插入）

**Interfaces:**
- Consumes: 现有卡片样式
- Produces: 双列对比 HTML 结构

- [ ] **Step 1: 插入对比区 section**

在流水线区 `</section>` 后、收益矩阵 `<section class="earnings-section">` 前插入：

```vue
    <!-- 前后对比区 -->
    <section class="compare-section">
      <div class="compare-inner">
        <div class="compare-header reveal" data-reveal-delay="0">
          <div class="section-tag">传统做法 vs 爱创作</div>
          <h2 class="compare-title">差的不只是工具，是整套方法</h2>
        </div>
        <div class="compare-grid reveal" data-reveal-delay="100">
          <div class="compare-col compare-col--old">
            <h3 class="compare-col-title">传统做法</h3>
            <ul class="compare-list">
              <li>打开空白编辑器，选题靠拍脑袋</li>
              <li>追热点，内容和别人雷同</li>
              <li>AI 生成大路货，被平台判低创作度</li>
              <li>写完就发，不懂平台规则</li>
              <li>有流量却不知道怎么赚钱</li>
              <li>发几篇没反馈就放弃</li>
            </ul>
          </div>
          <div class="compare-col compare-col--new">
            <h3 class="compare-col-title">爱创作</h3>
            <ul class="compare-list">
              <li>先制定方案，AI 每天推荐选题</li>
              <li>基于低粉高赞 + 蓝海赛道，差异化选题</li>
              <li>强制注入个人素材，降低同质化</li>
              <li>按平台特性生成，给发布策略</li>
              <li>从定位阶段就规划变现路径</li>
              <li>持续推荐、迭代定位</li>
            </ul>
          </div>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: 添加对比区样式**

```css
.compare-section {
  background: #fff;
  padding: 80px 48px;
}
.compare-inner { max-width: 900px; margin: 0 auto; }
.compare-header { text-align: center; margin-bottom: 48px; }
.compare-title {
  font-size: 32px;
  color: #1a1a1a;
  font-weight: 700;
}
.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.compare-col {
  border-radius: 20px;
  padding: 36px;
}
.compare-col--old {
  background: #f8f9fa;
  border: 1px solid #f0f0f0;
}
.compare-col--new {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  box-shadow: 0 12px 36px rgba(255, 36, 66, 0.2);
}
.compare-col-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
}
.compare-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.compare-list li {
  padding: 12px 0;
  border-bottom: 1px dashed rgba(0,0,0,0.08);
  font-size: 15px;
  line-height: 1.6;
}
.compare-col--new .compare-list li {
  border-bottom-color: rgba(255,255,255,0.2);
}
.compare-list li:last-child { border-bottom: none; }

@media (max-width: 768px) {
  .compare-section { padding: 50px 20px; }
  .compare-title { font-size: 24px; }
  .compare-grid { grid-template-columns: 1fr; }
}
```

- [ ] **Step 3: 浏览器验证**

确认：
- 标题为「传统做法 vs 爱创作」
- 左右两列对比正确
- 右侧爱创作列为品牌红渐变背景

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "feat(home): PC 端新增前后对比区"
```

---

## Task 6: 调整收益玩法矩阵文案与标签

**Files:**
- Modify: `project/user/web/src/views/Home.vue:earnings-section`

**Interfaces:**
- Consumes: 现有 `.earnings-section`、`.earnings-grid`、`.feature-card` 样式
- Produces: 新副标题 + 对应环节标签

- [ ] **Step 1: 修改收益区标题与卡片**

将收益区标题改为：
```vue
          <div class="section-tag">4 种变现路径</div>
          <h2 class="earnings-title">边写边赚</h2>
          <p class="earnings-subtitle">流水线每个环节都能产生收益</p>
```

在四张收益卡片中，每张顶部增加一个环节标签：

```vue
          <div class="feature-card reveal" data-reveal-delay="100">
            <div class="earn-stage-tag">每日选题 / 生成文章</div>
            <div class="feature-icon">...icon...</div>
            <div class="feature-name">创作币奖励</div>
            <div class="feature-desc">完成任务、活动、上榜，创作币可抵扣会员或提现。</div>
          </div>
```

其余三张卡片分别加上：
- 邀请好友返利 → `拉新激励`
- 排行榜奖金 → `运营复盘 / 数据成长`
- 自媒体收入申报 → `变现闭环`

- [ ] **Step 2: 添加环节标签样式**

```css
.earn-stage-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #FF2442;
  background: #FFF0F2;
  padding: 3px 10px;
  border-radius: 10px;
  margin-bottom: 14px;
}
```

- [ ] **Step 3: 浏览器验证**

确认副标题和环节标签正确显示。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "feat(home): 收益玩法矩阵按流水线环节重新包装"
```

---

## Task 7: 调整三步起号与终 CTA 文案

**Files:**
- Modify: `project/user/web/src/views/Home.vue:steps section` 和 `cta-section`

**Interfaces:**
- Consumes: 现有 `.steps`、`.cta-section` 样式
- Produces: 新步骤文案与 CTA 链接

- [ ] **Step 1: 修改三步起号**

将 steps section 替换为：

```vue
    <!-- 使用步骤 -->
    <section class="steps">
      <div class="steps-deco steps-deco-1"></div>
      <div class="steps-deco steps-deco-2"></div>
      <div class="steps-inner">
        <h2 class="steps-title reveal" data-reveal-delay="0">3 步建立你的自媒体运营方案</h2>
        <p class="steps-subtitle reveal" data-reveal-delay="80">1 分钟填写问卷，3 分钟拿到方案，然后每天按推荐选题创作。</p>
        <div class="steps-list">
          <div class="step-item reveal" data-reveal-delay="160">
            <div class="step-num">1</div>
            <div class="step-name">填写问卷</div>
            <div class="step-desc">1 分钟，告诉 AI 你的背景和投入时间</div>
          </div>
          <div class="step-item reveal" data-reveal-delay="280">
            <div class="step-num">2</div>
            <div class="step-name">制定方案</div>
            <div class="step-desc">AI 推荐平台、赛道、人设和内容支柱</div>
          </div>
          <div class="step-item reveal" data-reveal-delay="400">
            <div class="step-num">3</div>
            <div class="step-name">开始创作</div>
            <div class="step-desc">每天按推荐选题生成文章，持续运营</div>
          </div>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: 修改终 CTA**

将 cta-section 替换为：

```vue
    <!-- 最终 CTA -->
    <section class="cta-section">
      <div class="cta-card reveal" data-reveal-delay="0">
        <h2 class="cta-title">现在定位，3 个月后看复利</h2>
        <p class="cta-desc">
          账号不是写出来的，是运营出来的。<br />
          先定位，再创作，越早开始，雪球滚得越大。
        </p>
        <div class="cta-actions">
          <router-link to="/console/onboarding" class="hero-btn">免费制定我的自媒体方案</router-link>
          <router-link to="/guide" class="hero-btn-secondary">查看玩法指南</router-link>
        </div>
      </div>
    </section>
```

- [ ] **Step 3: 浏览器验证**

确认三步起号和终 CTA 文案、链接正确。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/Home.vue
git commit -m "feat(home): PC 端三步起号与终 CTA 按新定位调整"
```

---

## Task 8: 同步改造 MobileHome.vue

**Files:**
- Modify: `project/user/web/src/views/MobileHome.vue`

**Interfaces:**
- Consumes: 现有移动端结构与样式
- Produces: 与新定位一致的移动端首页

- [ ] **Step 1: 更新移动端 Hero**

将 MobileHome 的 Hero 区（第 53-88 行）替换为：

```vue
    <!-- Hero -->
    <section class="mh-hero">
      <div class="mh-hero__badge">
        <span class="mh-hero__badge-dot"></span>
        AI 驱动的自媒体运营流水线
      </div>
      <h1 class="mh-hero__title">普通人做自媒体，先从定位开始</h1>
      <p class="mh-hero__desc">
        不知道写什么、账号做不起来、有专业却不会变现？爱创作把复杂的自媒体工作拆成一套可执行的 AI 辅助流程。
      </p>
      <div class="mh-hero__actions">
        <router-link to="/console/onboarding" class="mh-btn mh-btn--primary">免费制定方案</router-link>
        <router-link to="/guide" class="mh-btn mh-btn--secondary">看看别人怎么变现</router-link>
      </div>

      <div v-if="banners.length" class="mh-hero__carousel">
        ...保留现有轮播...
      </div>
    </section>
```

- [ ] **Step 2: 替换移动端「为什么选择」为痛点区**

将 MobileHome 的「为什么选择爱创作」section（约第 106-169 行）替换为简化的三类痛点卡片：

```vue
    <!-- 三类痛点 -->
    <section class="mh-section">
      <div class="mh-section__tag">你是否也遇到这些问题？</div>
      <h2 class="mh-section__title">不同阶段的解决方案</h2>
      <p class="mh-section__subtitle">爱创作给的是对应阶段的解决方案，不是一篇万能模板。</p>

      <div class="mh-pain-list">
        <div class="mh-pain-card">
          <div class="mh-pain-card__tag">完全没做过自媒体</div>
          <div class="mh-pain-card__title">打开编辑器就发呆</div>
          <div class="mh-pain-card__desc">AI 根据你的背景推荐平台、赛道、人设和今日选题。</div>
        </div>
        <div class="mh-pain-card">
          <div class="mh-pain-card__tag">做过但做不起来</div>
          <div class="mh-pain-card__title">发了不少却没人看</div>
          <div class="mh-pain-card__desc">基于低粉高赞数据推荐蓝海细分赛道，避开同质化。</div>
        </div>
        <div class="mh-pain-card">
          <div class="mh-pain-card__tag">有专业经验 / 产品</div>
          <div class="mh-pain-card__title">有专业知识却不会变现</div>
          <div class="mh-pain-card__desc">把人设拆解成内容支柱，持续输出、引流、成交。</div>
        </div>
      </div>
    </section>
```

- [ ] **Step 3: 新增移动端流水线区**

在痛点区后、收益玩法前插入：

```vue
    <!-- 流水线 -->
    <section class="mh-section mh-section--pipeline">
      <div class="mh-section__tag">自媒体运营流水线</div>
      <h2 class="mh-section__title">建立一套可持续的运营方案</h2>
      <p class="mh-section__subtitle">从 0 到 1，再到持续变现，每一步都是固定动作。</p>

      <div class="mh-pipeline">
        <div class="mh-pipeline__item"><span>1</span>制定方案</div>
        <div class="mh-pipeline__item"><span>2</span>每日选题</div>
        <div class="mh-pipeline__item"><span>3</span>生成文章</div>
        <div class="mh-pipeline__item"><span>4</span>发布运营</div>
        <div class="mh-pipeline__item"><span>5</span>运营复盘</div>
      </div>
    </section>
```

- [ ] **Step 4: 调整移动端三步起号与 CTA**

将 MobileHome 的步骤区替换为：

```vue
    <!-- 使用步骤 -->
    <section class="mh-section mh-section--steps">
      <h2 class="mh-section__title">3 步建立你的自媒体运营方案</h2>
      <p class="mh-section__subtitle">1 分钟填写问卷，3 分钟拿到方案</p>

      <div class="mh-steps">
        <div class="mh-step">
          <div class="mh-step__num">1</div>
          <div class="mh-step__name">填写问卷</div>
          <div class="mh-step__desc">1 分钟</div>
        </div>
        <div class="mh-step">
          <div class="mh-step__num">2</div>
          <div class="mh-step__name">制定方案</div>
          <div class="mh-step__desc">AI 推荐</div>
        </div>
        <div class="mh-step">
          <div class="mh-step__num">3</div>
          <div class="mh-step__name">开始创作</div>
          <div class="mh-step__desc">持续运营</div>
        </div>
      </div>
    </section>
```

终 CTA 区替换为：

```vue
    <!-- 最终 CTA -->
    <section class="mh-cta">
      <h2 class="mh-cta__title">现在定位，3 个月后看复利</h2>
      <p class="mh-cta__desc">账号不是写出来的，是运营出来的。先定位，再创作。</p>
      <router-link to="/console/onboarding" class="mh-btn mh-btn--primary">免费制定方案</router-link>
      <router-link to="/guide" class="mh-btn mh-btn--secondary">查看玩法指南</router-link>
    </section>
```

- [ ] **Step 5: 添加移动端新样式**

在 `MobileHome.vue` 的 `<style>` 中新增：

```css
.mh-pain-list { display: flex; flex-direction: column; gap: 16px; }
.mh-pain-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.mh-pain-card__tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #FF2442;
  background: #FFF0F2;
  padding: 3px 10px;
  border-radius: 10px;
  margin-bottom: 10px;
}
.mh-pain-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.mh-pain-card__desc { font-size: 13px; color: #595959; line-height: 1.6; }

.mh-section--pipeline { background: #f8f9fa; }
.mh-pipeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mh-pipeline__item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  border: 1px solid #f0f0f0;
}
.mh-pipeline__item span {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #FF4D6F, #FF2442);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}
```

- [ ] **Step 6: 移动端浏览器验证**

使用浏览器 DevTools 切换到移动视口（≤768px），确认：
- Hero 文案和 CTA 正确
- 痛点区三张卡片垂直堆叠
- 流水线 5 步垂直排列
- 三步起号和终 CTA 文案正确

- [ ] **Step 7: Commit**

```bash
git add project/user/web/src/views/MobileHome.vue
git commit -m "feat(home): 移动端首页按新定位同步改造"
```

---

## Task 9: 编写 E2E 测试

**Files:**
- Create: `tests/e2e/verify_home_redesign.py`

**Interfaces:**
- Consumes: 首页各板块文案与链接
- Produces: 测试报告与截图

- [ ] **Step 1: 创建测试文件**

```python
# tests/e2e/verify_home_redesign.py
import re
from playwright.sync_api import sync_playwright, expect

BASE_URL = "http://localhost:5173"


def test_home_hero_redesign():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        page.goto(BASE_URL)

        # Hero 主标题
        hero_title = page.locator(".hero-title")
        expect(hero_title).to_contain_text("普通人做自媒体")
        expect(hero_title).to_contain_text("先从定位开始")

        # Hero 主 CTA
        primary_cta = page.locator(".hero-actions .hero-btn").first
        expect(primary_cta).to_contain_text("免费制定我的自媒体方案")
        expect(primary_cta).to_have_attribute("href", "/console/onboarding")

        # Hero 次 CTA
        secondary_cta = page.locator(".hero-actions .hero-btn-secondary").first
        expect(secondary_cta).to_contain_text("看看别人怎么变现")

        # Checkmarks
        checkmarks = page.locator(".hero-checkmarks")
        expect(checkmarks).to_contain_text("先定位，再创作")
        expect(checkmarks).to_contain_text("低粉高赞 + 蓝海赛道")

        browser.close()


def test_home_pain_points_section():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        page.goto(BASE_URL)

        section = page.locator(".features")
        expect(section).to_contain_text("你是否也遇到这些问题？")
        expect(section).to_contain_text("打开编辑器就发呆")
        expect(section).to_contain_text("发了不少却没人看")
        expect(section).to_contain_text("有专业知识却不会包装")

        browser.close()


def test_home_pipeline_section():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        page.goto(BASE_URL)

        section = page.locator(".pipeline-section")
        expect(section).to_contain_text("自媒体运营流水线")
        expect(section).to_contain_text("制定方案")
        expect(section).to_contain_text("每日选题")
        expect(section).to_contain_text("生成文章")
        expect(section).to_contain_text("发布运营")
        expect(section).to_contain_text("运营复盘")

        browser.close()


def test_home_compare_section():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        page.goto(BASE_URL)

        section = page.locator(".compare-section")
        expect(section).to_contain_text("传统做法")
        expect(section).to_contain_text("爱创作")
        expect(section).to_contain_text("打开空白编辑器，选题靠拍脑袋")
        expect(section).to_contain_text("先制定方案，AI 每天推荐选题")

        browser.close()


def test_home_steps_and_final_cta():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        page.goto(BASE_URL)

        # 三步起号
        steps = page.locator(".steps")
        expect(steps).to_contain_text("3 步建立你的自媒体运营方案")
        expect(steps).to_contain_text("填写问卷")
        expect(steps).to_contain_text("制定方案")
        expect(steps).to_contain_text("开始创作")

        # 终 CTA
        cta = page.locator(".cta-section")
        expect(cta).to_contain_text("现在定位，3 个月后看复利")
        final_cta = cta.locator(".hero-btn").first
        expect(final_cta).to_contain_text("免费制定我的自媒体方案")
        expect(final_cta).to_have_attribute("href", "/console/onboarding")

        browser.close()


if __name__ == "__main__":
    test_home_hero_redesign()
    test_home_pain_points_section()
    test_home_pipeline_section()
    test_home_compare_section()
    test_home_steps_and_final_cta()
    print("All homepage redesign assertions passed.")
```

- [ ] **Step 2: 运行测试**

确保本地 dev server 已启动，然后运行：

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_home_redesign.py
```

预期输出：`All homepage redesign assertions passed.`

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_home_redesign.py
git commit -m "test(e2e): 新增首页 redesign 验证脚本"
```

---

## Task 10: 暗色主题与细节检查

**Files:**
- Modify: `project/user/web/src/views/Home.vue`、`project/user/web/src/views/MobileHome.vue`（按需微调）

- [ ] **Step 1: 检查暗色主题**

在浏览器中切换到暗色主题，确认：
- Hero 区文字可读
- 痛点区卡片背景正确
- 流水线区背景与卡片对比度合适
- 对比区左侧卡片文字可读
- 收益矩阵卡片背景正确
- 三步起号区数字徽章颜色正确

- [ ] **Step 2: 修复暗色主题问题**

如果发现某元素在暗色主题下颜色不对，在对应的 `body[data-theme="dark"]` 规则中追加修复。例如：

```css
body[data-theme="dark"] .pipeline-section { background: #1a1a1a; }
body[data-theme="dark"] .pipeline-step {
  background: #1f1f1f;
  border-color: #2a2a2a;
}
body[data-theme="dark"] .pipeline-name { color: #e0e0e0; }
body[data-theme="dark"] .pipeline-desc { color: #a6a6a6; }
body[data-theme="dark"] .compare-col--old {
  background: #1f1f1f;
  border-color: #2a2a2a;
}
body[data-theme="dark"] .compare-title { color: #e0e0e0; }
body[data-theme="dark"] .compare-col--old .compare-col-title { color: #e0e0e0; }
body[data-theme="dark"] .compare-list li { border-bottom-color: #303030; color: #a6a6a6; }
```

- [ ] **Step 3: 最终 Commit**

```bash
git add project/user/web/src/views/Home.vue project/user/web/src/views/MobileHome.vue
git commit -m "fix(home): 首页 redesign 暗色主题适配"
```

---

## Self-Review Checklist

- [ ] **Spec coverage:** Hero、痛点区、流水线、对比区、收益矩阵、三步起号、终 CTA、移动端、NavBar CTA、E2E 测试、暗色主题均有对应任务。
- [ ] **Placeholder scan:** 计划中没有 TBD/TODO，所有代码片段为实际可替换内容。
- [ ] **Type consistency:** 不涉及复杂类型，仅 Vue 模板与 CSS。
- [ ] **Scope check:** 任务集中在前端首页改造，不触及后端或其他页面。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-25-homepage-redesign.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints for review

Which approach?

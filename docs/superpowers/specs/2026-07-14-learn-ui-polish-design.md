# 创作学院 UI 中度打磨设计

**日期**: 2026-07-14
**状态**: 已确认，待实现
**关联文件**:
- `project/user/web/src/views/LearnIndex.vue`（Hero + 面包屑数据 + 空状态）
- `project/user/web/src/components/learn/LearnSidebarNode.vue`（图标 + 层级指示）
- `project/user/web/src/components/learn/LearnContent.vue`（面包屑 + 元信息条 + 上下篇卡片 + CTA + 空状态 + 分类列表卡片）

---

## 1. 功能概述

对创作学院（用户端 `/learn`）做一轮**中度 UI 打磨**，不改动页面骨架与后端接口，只针对现有视觉问题做 6 处定向优化：

1. 新增 Hero 区（品牌仪式感）
2. 侧边栏图标与层级指示升级
3. 文章详情新增面包屑 + 元信息条
4. 上下篇导航卡片与 CTA 升级
5. 空状态插画化 + 分类列表文章卡片化

**明确不做**：不改后端 API、不改 markdown 渲染样式、不引新依赖、不做暗色主题、不动 console 端。

---

## 2. 设计决策

- **图标库**：复用已安装的 `@ant-design/icons-vue` v7（console 端 4 个页面在用），零新增依赖。线性描边风格，避免 emoji 的"卡通感"。
- **品牌色**：严格遵循 `docs/design/design-system.md` 中的 `#FF2442` 主色体系，浅红背景用 `#FFF5F7`，hover 红 `#e61e3a`。
- **圆角**：卡片 `radius-xl (12px)`，小元素 `radius-lg (8px)`，胶囊 `radius-full (9999px)`。
- **阅读时长估算**：前端按 `content.length / 300` 估算（中文约 300 字/分钟），不新增后端字段。
- **Hero 副标题**：静态文案 `从 0 到 1 的自媒体实战指南`，不显示篇数（避免 N 次请求）。
- **移动端**：所有改动在 `< 992px` 断点下正常堆叠，不新增独立移动端组件。

---

## 3. Hero 区

### 3.1 位置与尺寸

- 位置：`NavBar` 之下、左右两栏布局之上，跨全宽
- 高度：PC `140px`，移动端 `100px`
- 底部与内容区间距 `24px`，无分割线

### 3.2 视觉

```
┌────────────────────────────────────────────────────────┐
│  [淡红渐变背景 + 右上角装饰几何]                          │
│                                                          │
│   创作学院                                                │
│   从 0 到 1 的自媒体实战指南                              │
│                                                          │
└────────────────────────────────────────────────────────┘
```

- **背景**：`linear-gradient(180deg, #FFF5F7 0%, #FFFFFF 100%)`
- **装饰**：右上角两个绝对定位的圆（大圆 `200px` / 小圆 `80px`），背景 `#FFE8EC`，`border-radius: 50%`，`overflow: hidden` 裁切
- **主标题**：`创作学院`，32px / 700 / `#1a1a1a`
- **副标题**：`从 0 到 1 的自媒体实战指南`，14px / `#8c8c8c`，margin-top `8px`

### 3.3 实现位置

`LearnIndex.vue` 模板中，`NavBar` 之后、`.learn-body` 之前插入：

```vue
<header class="learn-hero">
  <div class="learn-hero-deco learn-hero-deco-lg"></div>
  <div class="learn-hero-deco learn-hero-deco-sm"></div>
  <div class="learn-hero-inner">
    <h1 class="learn-hero-title">创作学院</h1>
    <p class="learn-hero-subtitle">从 0 到 1 的自媒体实战指南</p>
  </div>
</header>
```

---

## 4. 侧边栏升级

### 4.1 图标映射

前端硬编码映射表（新建 `project/user/web/src/components/learn/learnCategoryIcons.js`）：

```js
import {
  AimOutlined, RocketOutlined, ThunderboltOutlined,
  RiseOutlined, IdcardOutlined, WalletOutlined
} from '@ant-design/icons-vue'

export const CATEGORY_ICONS = {
  '内容定位': AimOutlined,
  '平台运营技巧': RocketOutlined,
  '爆款方法论': ThunderboltOutlined,
  '涨粉与流量增长': RiseOutlined,
  'IP 打造与人设': IdcardOutlined,
  '变现路径': WalletOutlined
}
```

- 顶级分类渲染图标，子级不渲染
- 找不到映射 → 不渲染图标
- 图标 16px，默认 `#8c8c8c`，hover/选中变 `#FF2442`

### 4.2 折叠图标

- `+/−` → `›/∨`（右尖括号 / 下尖括号）
- 12px / `#bfbfbf`

### 4.3 层级指示

- 子级分类左侧加 `border-left: 2px solid #f0f0f0`，从顶级延伸
- 顶级分类之间 `margin-top: 4px`

### 4.4 选中态

- 保留现有浅红背景 `#FFF5F7` + 红色左边线
- 左边线加粗到 `3px`
- label 前加 `4px` 红色实心圆点（`●`）

### 4.5 hover 态

- 保留浅红背景
- 额外加 `border-left: 3px solid #ffb3c1`（比选中态浅的红色）

---

## 5. 面包屑 + 文章元信息条

### 5.1 面包屑

**位置**：文章标题区最上方。

**格式**：`创作学院 › 父分类 › 当前分类`

**数据来源**：`currentArticle.categoryId` → 反查 `categoryTree` 拼出完整路径。`LearnIndex.vue` 新增 `currentCategoryPath` computed，传入 `LearnContent.vue`。

**样式**：12px / `#8c8c8c` / 分隔符 `›` / 每段 hover 变 `#FF2442` / 可点击跳 `/learn?cat=<id>`

**移动端**：超出省略号，只保留最后两段（`... › 父分类 › 当前分类`）

### 5.2 元信息条

**位置**：文章标题 + summary 之下、正文之上。

**内容**：`📅 发布日期 · ⏱ 阅读时长 · 📝 字数 · #分类标签`

| 项 | 数据来源 | 格式 |
|---|---|---|
| 发布日期 | `article.publishedAt \|\| article.updatedAt` | `YYYY-MM-DD` |
| 阅读时长 | `Math.ceil(content.length / 300)` | `约 N 分钟` |
| 字数 | `content.length` | `N 字` |
| 分类标签 | `currentCategoryName` | chip：浅红背景 `#FFF5F7` + 红字 `#FF2442` + `radius-full` + padding `2px 10px` |

**图标**：`CalendarOutlined` / `ClockCircleOutlined` / `FileTextOutlined` / `TagOutlined`（14px / `#8c8c8c`）

**样式**：12px / `#8c8c8c` / 间距 `12px` / 上下 padding `12px 0` / 底部 `border-bottom: 1px solid #f0f0f0`

**移动端**：flex-wrap 换行

---

## 6. 上下篇卡片 + CTA 升级

### 6.1 上下篇卡片

**结构**（每张卡片从上到下）：
1. 方向行：`← 上一篇` / `下一篇 →`（12px / `#8c8c8c`）
2. 分类 chip：`TagOutlined` + 分类名，浅红背景 + 红字 + `radius-full`，**跨分类才显示**
3. 标题：15px / 600 / `#1a1a1a`，最多 2 行省略号

**视觉**：
- 默认：白底 + `1px solid #eee` + `radius-xl (12px)` + padding `16px 20px`
- hover：`border-color: #FF2442` + `box-shadow: 0 4px 12px rgba(255, 36, 66, 0.12)` + `transform: translateY(-2px)` + 标题变红
- 过渡：`transition: all 0.2s ease`
- 上一篇左对齐，下一篇右对齐
- 移动端上下堆叠（现状已是）

### 6.2 CTA 卡片

**位置**：上下篇卡片之下、footer 之上。

**视觉**：
- 背景：`linear-gradient(135deg, #FFF5F7 0%, #FFE8EC 100%)`
- 圆角：`radius-xl (12px)`
- padding：`24px 28px`

**结构**（横向 flex）：
- 左侧：`BulbOutlined` 图标（32px / `#FF2442`）
- 中间：主标题 `想把自己的账号也做成这样？`（16px / 600 / `#1a1a1a`）+ 副标题 `用 AI 一分钟生成你的第一篇`（13px / `#8c8c8c`）
- 右侧：红色实心按钮 `立即开始创作 →`（背景 `#FF2442` / 白字 / `radius-full` / padding `10px 24px` / hover `#e61e3a`）

**移动端**：纵向堆叠，按钮全宽。

---

## 7. 空状态 + 分类列表

### 7.1 空状态（学院首页未选分类）

**位置**：`.learn-main` 内居中。

**结构**：
1. 装饰图标：`ReadOutlined`（64px / `#FFE8EC`）
2. 主标题：`欢迎来到创作学院`（20px / 600 / `#1a1a1a`）
3. 副标题：`从左侧选择一个分类开始学习`（14px / `#8c8c8c`）
4. 快捷入口：前 4 个顶级分类 chip（图标 + 分类名），白底 + `1px solid #eee` + `radius-full` + padding `8px 16px`，hover 红边红字

**样式**：`display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; gap: 12px;`

### 7.2 分类列表页

**面包屑**：与文章详情页一致（`创作学院 › 父分类 › 当前分类`）。

**分类标题区**：标题（24px / 700）+ `本分类下共 N 篇文章`（13px / `#8c8c8c`）。

**文章条目卡片化**：
- 每篇文章：白底 + `1px solid #eee` + `radius-lg (8px)` + padding `16px 20px` + margin-bottom `12px`
- 卡片内：标题（16px / 600）+ 摘要（14px / `#595959`，最多 2 行省略）+ 元信息行（📅 日期 · ⏱ 阅读时长）
- hover：`border-color: #FF2442` + `box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08)` + 标题变红

**空分类**：保留 `该分类下暂无已发布文章`，样式改成居中提示。

---

## 8. 文件变更清单

| 文件 | 变更 |
|---|---|
| `project/user/web/src/views/LearnIndex.vue` | 新增 Hero 区、新增 `currentCategoryPath` computed、空状态快捷入口、移动端样式 |
| `project/user/web/src/components/learn/LearnSidebarNode.vue` | 图标映射、`›/∨` 折叠图标、层级指示线、选中态圆点、hover 左边线 |
| `project/user/web/src/components/learn/LearnContent.vue` | 面包屑、元信息条、上下篇卡片升级、CTA 卡片、空状态、分类列表卡片 |
| `project/user/web/src/components/learn/learnCategoryIcons.js` | 新建：分类图标映射表 |
| `tests/e2e/learn_ui_polish.py` | 新建：e2e 验证脚本 |

---

## 9. 测试

### 9.1 e2e（Playwright）

`tests/e2e/learn_ui_polish.py` 覆盖：

1. Hero 区：标题、副标题、装饰图形存在
2. 侧边栏：顶级分类有 SVG 图标（`svg` 元素存在）、折叠图标是 `›/∨`、选中态有红色圆点
3. 面包屑：文章详情页显示 `创作学院 › ... › ...`，点击跳转正确
4. 元信息条：日期、阅读时长、字数、分类 chip 都在
5. 上下篇卡片：hover 后 computed style 有 `translateY` 与阴影
6. CTA 卡片：渐变背景、按钮存在且可点击
7. 空状态：装饰图标、主标题、快捷入口 chip 存在
8. 分类列表：文章条目是卡片样式（有 `border-radius`）、有面包屑
9. 移动端：390px 视口下所有改动正常堆叠

### 9.2 手测清单

- 首页 → 创作学院 → Hero 渲染正常
- 点击分类 → 面包屑正确、文章卡片 hover 有反馈
- 进入文章 → 面包屑、元信息条、上下篇卡片、CTA 卡片都正常
- 点击上下篇 → 跳转正确，页面滚到顶部
- 移动端：分类抽屉正常、文章页堆叠正常、CTA 按钮全宽

---

## 10. 提交拆分

| Commit | 范围 |
|---|---|
| `feat(user-web): 创作学院 Hero 区 + 侧边栏图标升级` | `LearnIndex.vue` Hero、`LearnSidebarNode.vue` 图标 + 层级指示、`learnCategoryIcons.js` |
| `feat(user-web): 创作学院面包屑 + 文章元信息条` | `LearnContent.vue` 面包屑 + 元信息条、`LearnIndex.vue` 分类路径 |
| `feat(user-web): 创作学院上下篇卡片 + CTA 升级` | `LearnContent.vue` 上下篇卡片 + CTA 卡片 |
| `feat(user-web): 创作学院空状态 + 分类列表卡片化` | `LearnContent.vue` 空状态 + 分类列表卡片 |
| `test(e2e): 创作学院 UI 打磨验证脚本` | `tests/e2e/learn_ui_polish.py` |

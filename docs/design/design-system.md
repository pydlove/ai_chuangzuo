# 设计系统规范

> 本文档定义爱创作（AI Creation）项目用户端与管理端的 UI 设计系统，覆盖品牌色、圆角、字号、间距、阴影、按钮、搜索框、列表等核心视觉规范。主色调采用品牌绿 `#07c160`。

---

## 1. 品牌色

主色采用品牌绿，定义为 `--color-primary`：

| 名称 | 颜色值 | 用途 |
|---|---|---|
| `primary` | `#07c160` | 主按钮、强调文字、选中状态、链接 |
| `primary-hover` | `#06ad56` | 主按钮 hover |
| `primary-active` | `#059a4c` | 主按钮 active |
| `primary-light` | `#f6ffed` | 主色淡背景（hover、选中态、徽标底色） |
| `primary-bg` | `#f6ffed` | 极淡背景（高亮区域） |

---

## 2. 中性色

| 名称 | 颜色值 | 用途 |
|---|---|---|
| `text-primary` | `#1a1a1a` | 标题、主要文字 |
| `text-regular` | `#262626` | 正文 |
| `text-secondary` | `#595959` | 次要文字、描述 |
| `text-placeholder` | `#8c8c8c` | 占位符、辅助文字 |
| `border-default` | `#d9d9d9` | 默认边框 |
| `border-light` | `#eee` | 浅边框、分割线 |
| `bg-page` | `#f8f9fa` | 页面背景 |
| `bg-card` | `#ffffff` | 卡片/容器背景 |
| `bg-hover` | `#f5f5f5` | hover 背景 |

---

## 3. 功能色

| 名称 | 颜色值 | 用途 |
|---|---|---|
| `success` | `#07c160` | 成功状态 |
| `warning` | `#fa8c16` | 警告状态 |
| `error` | `#ff4d4f` | 错误状态 |
| `info` | `#1989fa` | 信息提示 |

---

## 4. 圆角

统一使用 4 档圆角：

| 名称 | 值 | 用途 |
|---|---|---|
| `radius-sm` | `4px` | 标签、小徽标 |
| `radius-md` | `6px` | 图标按钮、小控件 |
| `radius-lg` | `8px` | 按钮、输入框、小卡片 |
| `radius-xl` | `12px` | 卡片、面板、会员徽章 |
| `radius-full` | `9999px` | 头像、胶囊按钮、搜索框 |

---

## 5. 字体

```css
font-family: -apple-system, BlinkMacSystemFont, "PingFang SC",
             "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", sans-serif;
```

| 名称 | 字号 / 行高 | 用途 |
|---|---|---|
| `font-h1` | `24px` | 页面大标题 |
| `font-h2` | `20px` | 模块标题 |
| `font-h3` | `18px` | 卡片标题 |
| `font-body` | `14px` | 正文（默认） |
| `font-small` | `12px` | 辅助文字、说明 |
| `font-caption` | `11px` | 极小提示 |

字重：`400` 常规、`500` 中等、`600` 半粗、`700` 加粗。

---

## 6. 间距

8 倍数体系：`4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48`

| 名称 | 值 | 用途 |
|---|---|---|
| `space-xs` | `4px` | 文字与图标 |
| `space-sm` | `8px` | 紧凑间距 |
| `space-md` | `16px` | 通用间距 |
| `space-lg` | `24px` | 模块间距 |
| `space-xl` | `32px` | 区块间距 |

---

## 7. 阴影

| 名称 | 值 | 用途 |
|---|---|---|
| `shadow-sm` | `0 1px 2px rgba(0,0,0,0.04)` | 轻微悬浮 |
| `shadow-sm2` | `0 2px 12px rgba(0,0,0,0.06)` | 卡片阴影 |
| `shadow-md` | `0 4px 12px rgba(0,0,0,0.10)` | 弹窗、下拉 |
| `shadow-lg` | `0 8px 32px rgba(0,0,0,0.15)` | 大弹窗 |

---

## 8. 按钮

| 类型 | 样式 |
|---|---|
| 主按钮 | 背景 `primary`、文字白、圆角 `8px`、高度 `40px`、hover 变 `primary-hover` |
| 次按钮 | 背景白、边框 `border-default`、文字 `text-primary`、hover 背景 `bg-hover` |
| 文字按钮 | 无背景无边框、文字 `primary`、hover 文字 `primary-hover` |
| 危险按钮 | 背景 `error`、文字白 |
| 禁用 | 背景 `#F5F5F5`、文字 `#BFBFBF`、不可点击 |

尺寸：

| 尺寸 | 高度 | 圆角 | 字号 |
|---|---|---|---|
| `sm` | `28px` | `4px` | `12px` |
| `md` | `36px` | `6px` | `14px` |
| `lg` | `40px` | `8px` | `14px` |

---

## 9. 搜索框

### 9.1 默认样式（胶囊形）

```text
高度：40px
圆角：9999px
背景：#F5F5F5
内边距：0 16px
图标：左侧 16×16 搜索图标，颜色 #999
占位符：#8c8c8c
```

### 9.2 状态

| 状态 | 样式 |
|---|---|
| 默认 | 背景 `#F5F5F5`，无边框 |
| Hover | 背景 `#EEEEEE` |
| Focus | 背景白，边框 `1px solid primary` |
| 禁用 | 背景 `#FAFAFA`，文字 `#BFBFBF` |

### 9.3 尺寸

| 尺寸 | 高度 | 字号 | 图标 |
|---|---|---|---|
| `sm` | `32px` | `12px` | `14px` |
| `md` | `40px` | `14px` | `16px` |
| `lg` | `48px` | `16px` | `18px` |

### 9.4 变体

- **带按钮搜索框**：右侧带主色按钮，高度与输入框一致。
- **带下拉搜索框**：左侧带分类下拉，下拉与输入框视觉一体。
- **移动端搜索框**：占满宽度，高度 `36px`。

---

## 10. 列表

### 10.1 通用规则

```text
背景：白色
内边距：16px 水平，12px 垂直
高度：自适应内容，最小 56px
分割线：1px solid border-light
最后一项：无分割线
```

### 10.2 状态

| 状态 | 样式 |
|---|---|
| 默认 | 背景白 |
| Hover | 背景 `primary-light`（`#f6ffed`） |
| Active/选中 | 背景 `primary-light`，左侧 3px 主色条 |
| 禁用 | 文字 `text-placeholder` |

### 10.3 类型

#### 文本列表（设置项、菜单）

```
┌─────────────────────────────────┐
│ 标题                  右侧说明 ›
└─────────────────────────────────┘
```

#### 图标 + 文本列表

```
┌─────────────────────────────────┐
│ [图标] 标题            右侧内容 ›
└─────────────────────────────────┘
```

#### 卡片列表（文章、模板）

```
┌─────────────────────────────────┐
│ [封面图]                         │
│ 标题文字                         │
│ 描述文字             元信息  ›  │
└─────────────────────────────────┘
```

- 圆角：`12px`
- 间距：卡片间距 `12px`
- 阴影：`shadow-sm`，hover `shadow-md`

#### 数据列表（表格行）

- 行高：`48px`
- 表头：`text-secondary`，背景 `#FAFAFA`，加粗
- 斑马纹：可选用 `bg-page` 间隔
- Hover：`bg-hover`

---

## 11. 控制台布局规范

### 侧边栏

| 元素 | 规范 |
|---|---|
| 宽度 | `200px` |
| 背景 | `bg-card` |
| 品牌区高度 | `56px`，文字 `primary`，字号 `18px`，字重 `700`，内边距 `0 20px` |
| 导航项高度 | `36px`（padding `10px 12px`） |
| 导航项圆角 | `8px` |
| 导航项默认 | 文字 `text-primary`，背景透明 |
| 导航项 hover | 背景 `primary-light` |
| 导航项激活 | 背景 `primary-light`，文字 `primary`，字重 `600` |
| 图标大小 | `18px` |

### 顶部栏

| 元素 | 规范 |
|---|---|
| 高度 | `56px` |
| 背景 | `bg-card` |
| 内边距 | `0 20px` |
| 头像 | 宽高 `32px`，圆角 `50%`，背景 `primary`，文字白，字重 `600` |
| 会员徽章 | 背景 `#fff7e6`，文字 `#fa8c16`，边框 `#ffd591`，圆角 `12px` |
| 图标按钮 | 宽高 `32px`，圆角 `6px`，hover 背景 `primary-light` |
| 标题文字 | 居中，字重 `700`，字号 `16px` |

### 内容区

| 元素 | 规范 |
|---|---|
| 内边距 | `24px` |
| 背景 | `bg-page` |

### 底部

| 元素 | 规范 |
|---|---|
| 内边距 | `16px 24px` |
| 背景 | `bg-card` |
| 文字颜色 | `text-secondary`，`13px` |
| 分割线 | 上边框 `border-light` |

---

## 12. CSS 变量汇总

```css
:root {
  /* 品牌色 */
  --color-primary: #07c160;
  --color-primary-hover: #06ad56;
  --color-primary-active: #059a4c;
  --color-primary-light: #f6ffed;
  --color-primary-bg: #f6ffed;

  /* 中性色 */
  --color-text-primary: #1a1a1a;
  --color-text-regular: #262626;
  --color-text-secondary: #595959;
  --color-text-placeholder: #8c8c8c;
  --color-border-default: #d9d9d9;
  --color-border-light: #eee;
  --color-bg-page: #f8f9fa;
  --color-bg-card: #ffffff;
  --color-bg-hover: #f5f5f5;

  /* 功能色 */
  --color-success: #07c160;
  --color-warning: #fa8c16;
  --color-error: #ff4d4f;
  --color-info: #1989fa;

  /* 圆角 */
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 8px;
  --radius-xl: 12px;
  --radius-full: 9999px;

  /* 间距 */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;

  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-sm2: 0 2px 12px rgba(0, 0, 0, 0.06);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.10);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.15);

  /* 字号 */
  --font-h1: 24px;
  --font-h2: 20px;
  --font-h3: 18px;
  --font-body: 14px;
  --font-small: 12px;
  --font-caption: 11px;
}
```

---

## 13. 禁止事项

- 禁止使用品牌绿以外的强彩色作为主操作色。
- 禁止大段使用纯黑（`#000`），用 `text-primary` 替代。
- 禁止圆角超过 `16px`（弹窗除外），保持视觉统一。
- 禁止在阴影中使用非 `0,0,0` 色相的 RGBA。
- 禁止使用非规范字号，所有文字使用 `font-*` 变量。

---

## 14. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-30 | v1.0 | 初稿：品牌绿 `#07c160`、圆角、字体、间距、按钮、搜索框、列表、控制台布局 | - |
| 2026-06-30 | v1.1 | 更新主色为绿色体系，调整圆角档位，增加控制台布局规范章节 | - |

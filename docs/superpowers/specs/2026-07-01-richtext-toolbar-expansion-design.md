# 编辑内容页富文本工具栏扩展设计

## 背景

`/console/edit` 页面（`project/user/web/src/views/console/EditIndex.vue`）当前工具栏仅提供 7 个操作项：标题、引用、列表、加粗、斜体、清除格式。

无法满足编辑诉求：
- 缺少常用行内样式（下划线、删除线、行内代码、颜色、背景、字号、字体）。
- 缺少段落级排版（对齐、缩进、行高、有序列表）。
- 样式无法持久化，刷新或再次打开文章后丢失，不利于二次编辑与导出。

## 目标

- 在 `/console/edit` 工具栏新增一整套行内文字样式与段落对齐操作。
- 新增样式独立存储，与现有 `body` 纯文本字段解耦，刷新/二次编辑后样式仍可还原。
- 为后续“导出 Word/小红书卡片”等下游消费富文本样式留出接入点。

## 方案

采用“补充样式、独立存储”方案：

1. 工具栏在现有按钮组基础上扩展为三段：**行内样式**、**段落对齐**、**列表/缩进**。
2. 文章新增 `styleOverrides` 字段，按 block 索引与行内文本区间记录样式，**不改动** `body` 纯文本语法。
3. 保存时从 `contentEditable` 的 DOM 拆出纯文本 + 样式，存到 localStorage。
4. 加载时按 `body` 重建 DOM，再按 `styleOverrides` 把样式贴回对应位置。
5. 保持现有 `contentEditable` + `document.execCommand` 方案，不引入富文本库。

## 架构

### 新增/修改文件

| 文件 | 变更 |
|------|------|
| `project/user/web/src/views/console/EditIndex.vue` | 工具栏 UI 扩展、状态/事件处理、保存/加载逻辑调整 |
| `project/user/web/src/utils/articleBlocks.js` | 新增 `bodyToHtmlWithStyles` / `htmlToBodyWithStyles` / `applyStyleOverrides` / `extractStyleOverrides` |
| `project/user/web/src/utils/articleStorage.js` | 适配 `styleOverrides` 字段读写（保持向后兼容） |

不动：`articleStorage.js` 中既有 `body` 字段语义、生成队列、预览页、生成流程、导出（后续单独消费）。

### 数据模型

```js
article = {
  id, title, createdAt, updatedAt,
  body: '【小标题】\n> 引用\n正文...',  // 纯文本语法，保持不变
  styleOverrides: {                       // 新增；旧文章无此字段视为空对象
    blocks: {
      // key 为 block 在解析顺序中的索引（数字字符串）
      '0': { align: 'center', lineHeight: '2', indent: 1 },
      '2': { color: '#ff2442', fontSize: 'large' }
    },
    inlines: [
      // [blockIndex, startOffset, endOffset) 在纯文本 block 内的字符区间
      { block: 1, start: 0, end: 4, styles: { bold: true, color: '#ff2442' } }
    ]
  }
}
```

支持的样式键：

- 块级：`align`（`left`/`center`/`right`/`justify`）、`lineHeight`（`1.5`/`1.75`/`2.0`）、`indent`（0/1/2）、`fontSize`（`xs`/`sm`/`base`/`lg`/`xl`）、`fontFamily`（`system`/`serif`/`sans`/`kai`）。
- 行内：`bold`、`italic`、`underline`、`strike`、`code`、`color`（hex）、`backgroundColor`（hex）。

## 组件设计

### 工具栏布局

工具栏分三段，使用 Ant Design Vue 组件：

```
┌─────────────────────────────────────────────────────────────────────┐
│  行内样式                                                          │
│  [B] [I] [U] [S] [代码] | 字体色 ▾  背景色 ▾  字号 ▾  字体 ▾  │清除│
├─────────────────────────────────────────────────────────────────────┤
│  段落                                                              │
│  [≡] [≣] [≢] [☰]  行高 ▾  缩进 [+] [-]                            │
├─────────────────────────────────────────────────────────────────────┤
│  列表                                                              │
│  [• 无序] [1. 有序]   [H2 小标题] [❝ 引用]                          │
└─────────────────────────────────────────────────────────────────────┘
```

实现细节：

- 使用 `a-button-group` 分组；颜色用 `a-color-picker` 触发，预设 8 色（黑/灰/红/橙/黄/绿/蓝/紫）+ 自定义。
- 字号、字体、行高、缩进使用 `a-dropdown` + `a-menu`。
- 工具栏首屏宽度不足时，行内样式部分项折叠到「更多」下拉菜单（断点 ≤ 1024px 时启用）。
- 工具栏中已激活的样式按钮显示高亮状态（与现有 `.active` 状态保持一致）。

### 状态管理

在 `EditIndex.vue` 中新增：

- `activeFormats`：`Set<string>`，当前光标处已激活的样式键（用于按钮高亮）。
- `styleOverridesRef`：与 `bodyHtml` 同步的响应式副本，编辑过程中实时更新；保存时序列化进 `article.styleOverrides`。
- 工具栏操作：
  - 行内类：聚焦编辑器 → `document.execCommand` 切样式 → 重新计算光标处的 inline 区间 → 写入 `styleOverrides`。
  - 块级类：定位光标所在 block 索引 → 写入 `styleOverrides.blocks[blockIdx]` → 重渲染该 block 样式。

### 粘贴与撤销

- `onPaste` 保持现有强制纯文本行为（`insertText`），不引入富文本粘贴，避免外部样式污染 `styleOverrides`。
- 不引入自定义历史栈，依赖浏览器原生 `contenteditable` 撤销（`Ctrl/Cmd+Z`、`Ctrl/Cmd+Shift+Z`）。

## 数据流

### 加载文章

1. `onMounted` 从 `loadCurrentArticle()` 拿到 `article`。
2. `styleOverridesRef` 初始化为 `article.styleOverrides || { blocks: {}, inlines: [] }`。
3. `bodyToHtmlWithStyles(article.body, styleOverridesRef)` 生成带样式的 HTML 注入编辑器。

### 用户编辑

1. 用户点击工具栏 → 直接修改 `contentEditable` DOM → 记录增量到 `styleOverridesRef`。
2. `selectionchange` 事件触发 `updateActiveFormats()`，刷新按钮高亮。

### 保存

1. 用户点击保存。
2. `htmlToBodyWithStyles(editorRef.innerHTML)` 同时产出：
   - 纯文本 `body`（与现有 `serializeBlocksToArticle` 等价，不带样式信息）。
   - `styleOverrides`（块级 + 行内）。
3. 写入 `aichuangzuo_current_article` 与生成队列同步（与现有逻辑一致）。
4. 提示成功，跳转回 `/console/preview`。

## `articleBlocks.js` 函数签名

```js
// 在 body 纯文本语法的 HTML 之上叠加 styleOverrides，返回编辑器可用的 HTML
bodyToHtmlWithStyles(body, styleOverrides) => string

// 把编辑器 DOM 拆成 body + styleOverrides
htmlToBodyWithStyles(html) => { body: string, styleOverrides: { blocks, inlines } }

// 仅用 styleOverrides 装饰既有 HTML（用于重渲染）
applyStyleOverrides(html, styleOverrides) => string
```

实现要点：

- `htmlToBodyWithStyles` 在现有 `htmlToBody` 基础上扩展：
  - 块级样式读取：读取每个顶层 block 元素的 `style`/`align` 属性，归一化后写入 `blocks[blockIdx]`。
  - 行内样式读取：递归遍历 inline 元素（`<b>`、`<i>`、`<u>`、`<s>`、`<code>`、`<span style="color|background-color|font-size|font-family">`），记录起止 offset 与样式键。
- `bodyToHtmlWithStyles` 在 `bodyToHtml` 基础上扩展：
  - 块级样式：渲染每个 block 时按 `blocks[idx]` 设置对应 inline style。
  - 行内样式：先渲染 block 文本，再用一次轻量算法把 `inlines` 中的区间套上 `<span>` 或对应标签。
- `applyStyleOverrides` 单独抽出来，便于预览/导出时复用。

## 错误处理

- 标题为空：复用现有校验，提示“标题不能为空”。
- `localStorage` 写入失败：复用现有提示。
- `styleOverrides` 字段缺失或格式异常：降级为空对象，编辑功能仍可用。
- 工具栏颜色/字号选择取消：保留原状，不写入 `styleOverrides`。

## 测试

1. **单元/手工测试**（`tests/e2e/` 新增脚本 `verify_richtext_toolbar.py`）：
   - 进入 `/console/edit` 编辑一篇文章。
   - 依次操作：下划线、删除线、字体颜色、字号、对齐、行高、缩进、有序列表。
   - 保存 → 刷新 → 重新进入 `/console/edit`，验证样式仍存在。
   - 同步验证 `aichuangzuo_current_article.styleOverrides` 字段已写入。
2. **回归**：
   - 旧文章（无 `styleOverrides` 字段）打开不报错，工具栏可用。
   - 现有标题/引用/列表/加粗/斜体功能不受影响。
   - 纯文本导出（`copyArticleText`）和 Word 导出（`exportWord`）现有行为不破坏。
3. **手动验证**：
   - 工具栏按钮在浅色/深色主题下视觉一致。
   - 移动端尺寸（≤ 768px）下工具栏可用，必要时横向滚动。

## 范围

- 在：编辑页工具栏、`articleBlocks.js` 富文本支持、localStorage 字段扩展。
- 不在（留待后续）：Word/小红书卡片/分享图等下游消费 `styleOverrides`、编辑器自定义字体上传、移动端专属工具栏布局、协同编辑。

## 待办

- [ ] `articleBlocks.js` 新增 `bodyToHtmlWithStyles` / `htmlToBodyWithStyles` / `applyStyleOverrides`。
- [ ] `EditIndex.vue` 工具栏 UI 三段式扩展与按钮高亮。
- [ ] `EditIndex.vue` 行内/块级样式写回逻辑与 `selectionchange` 联动。
- [ ] `articleStorage.js` 兼容 `styleOverrides` 字段（缺失则视为空对象）。
- [ ] 编写 `verify_richtext_toolbar.py` 端到端脚本并跑通。
- [ ] 手动验证深色主题与移动端尺寸。

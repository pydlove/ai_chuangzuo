# 意见反馈 / 关于我们弹框设计

## 背景

`console.html` 顶部 header 中已有「反馈」和「关于我们」两个图标入口，点击后调用 `shared.js` 中的 `openFeedbackModal()` 和 `openAboutModal()`。本设计明确这两个弹框应采用与「消息铃铛」弹框一致的实现模式，并确保居中显示、可手动关闭。

## 目标

1. 两个弹框均在屏幕正中间弹出。
2. 用户可通过多种方式手动关闭弹框：
   - 点击右上角 `×` 按钮
   - 点击黑色半透明遮罩区域
   - 按键盘 `ESC` 键
3. 实现方式与现有 `openMessageModal()` 保持一致，便于维护。

## 设计

### 架构

沿用现有 modal 系统：

- `shared.css` 已定义 `.modal-overlay`（fixed 全屏遮罩、flex 居中）和 `.modal-content`（白底卡片）。
- `shared.js` 中通过 JS 动态创建 overlay 并 `appendChild` 到 `document.body`。

### 弹框结构

```text
.modal-overlay            (fixed 全屏遮罩，默认 flex 居中)
  └── .modal-content      (白底圆角卡片)
        ├── .modal-header (标题 + × 关闭按钮)
        ├── .modal-body   (表单或介绍内容)
        └── (可选) 底部操作区
```

### 行为

- **居中**：不添加 `align-items: flex-start` 或 `padding-top` 等顶部对齐覆盖，使用 `.modal-overlay` 默认的 `align-items: center; justify-content: center`。
- **关闭**：
  - `×` 按钮调用对应 `closeXxxModal()` 函数。
  - 点击遮罩（`overlay.onclick`）关闭。
  - 按 `ESC` 关闭当前最上层弹框（通过全局 `keydown` 监听实现）。
- **互斥**：如果弹框已存在（通过 id 判断），不再重复创建。

### 内容

- **意见反馈**：保留现有表单，包括反馈类型下拉、反馈内容文本框、提交按钮。提交后显示 toast 并关闭弹框。
- **关于我们**：保留现有品牌介绍文案和底部快捷按钮（用户协议、隐私政策、关注微信、联系电话、举报）。

## 改动范围

- `shared.js`：
  - 调整 `openFeedbackModal()` 和 `openAboutModal()`，确保结构与 `openMessageModal()` 一致。
  - 补充全局 `ESC` 关闭弹框监听。
- `shared.css`：如有需要，加固 `.modal-overlay` 的居中样式，避免被 `console.html` 的特殊布局覆盖。
- `console.html`：不改动入口图标。

## 验证

1. 启动本地预览服务器： `./scripts/local/start.sh`
2. 打开 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/console.html`
3. 分别点击 header 中的「反馈」和「关于我们」图标：
   - 弹框出现在屏幕正中间。
   - 点击 `×`、点击遮罩、按 `ESC` 均可关闭弹框。
   - 弹框内容保持现有文案和表单。

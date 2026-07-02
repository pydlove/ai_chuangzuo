# 控制台文章手动编辑设计

## 背景

原型 `.superpowers/brainstorm/6491-1782131242/content/` 中已存在文章手动编辑能力：

- `preview.html` 支持点击“进入编辑模式”或点击正文元素进入 inline 编辑。
- `edit.html` 提供独立的分块编辑页，将文章拆为标题、小标题、段落、列表项、重点高亮等 block。

本设计将该能力移植到 Vue 控制台（`project/user/web/src/views/console/`）。

## 目标

- 在控制台预览页支持 inline 编辑生成后的文章。
- 新增 `/console/edit` 独立分块编辑页。
- 编辑结果持久化到 `aichuangzuo_current_article`，并同步回生成队列对应作品。

## 方案

采用“完全复刻原型”方案：

1. `PreviewIndex.vue` 增加“编辑正文”入口，支持整页进入编辑态及点击某一段快速编辑。
2. 新增 `EditIndex.vue` 作为 `/console/edit` 路由，提供分块编辑页。
3. 新增 `articleBlocks.js` 纯工具函数，负责 `body` 文本与 block 数组的互转。
4. 保存时同步更新生成队列，保证“我的作品”中打开的是修改后的版本。

## 架构

### 新增/修改文件

| 文件 | 变更 |
|------|------|
| `project/user/web/src/router/index.js` | 新增 `/console/edit` 子路由 |
| `project/user/web/src/views/console/EditIndex.vue` | 新增独立分块编辑页 |
| `project/user/web/src/views/console/PreviewIndex.vue` | 增加编辑入口、inline 编辑态、保存/取消条 |
| `project/user/web/src/utils/articleBlocks.js` | 新增：body 与 blocks 互转 |
| `project/user/web/src/utils/articleStorage.js` | 新增/复用：读写当前文章并同步生成队列 |

### 数据流

1. `WorksIndex` 点击作品 → 将 `work.content` 写入 `aichuangzuo_current_article`。
2. `PreviewIndex` 加载并展示。
3. 用户进入编辑态 → 记录快照，调用 `parseBodyToBlocks` 得到 blocks。
4. 用户保存 → `serializeBlocksToArticle` 生成新 `title` + `body` → 更新 `aichuangzuo_current_article` → 同步更新生成队列中同 `id` 项。
5. 用户取消 → 用快照还原。

## 组件设计

### EditIndex.vue

- 顶部：返回预览按钮 + “编辑内容”标题。
- 主体：按顺序渲染 blocks，每个 block 包含类型标签和 `contenteditable` 编辑区。
  - 标题：大号加粗。
  - 小标题：中等加粗。
  - 正文段落：常规样式。
  - 列表项：保持列表前缀样式。
  - 重点高亮：浅绿背景 + 左侧绿色边框。
- 底部固定胶囊条：取消 / 保存修改。
- 粘贴强制纯文本。

### PreviewIndex.vue

- 头部右侧新增“编辑正文”按钮。
- 进入编辑态后：
  - 文章区域各 block 变为 `contenteditable`。
  - 底部显示保存/取消条。
  - 原浮动操作栏隐藏。
- 支持直接点击某一段进入编辑态。
- 已修改 block 可显示轻微视觉标记。

## Block 解析规则

`parseBodyToBlocks(title, body)`：

- `title` → `type: 'title'`
- 行匹配 `^【([^】]+)】` → `type: 'heading'`
- 行首匹配 `^[-•]\s` 或 `^\d+\.\s` → `type: 'list-item'`
- 行首匹配 `^>\s` → `type: 'highlight'`
- 其余非空行 → `type: 'paragraph'`

`serializeBlocksToArticle(blocks)`：

- 找到 `type: 'title'` 的 block 作为 `title`。
- 其余 block 按类型还原为文本行：
  - heading → `【内容】`
  - list-item → 保留原有列表前缀（若丢失则默认 `- `）
  - highlight → `> 内容`
  - paragraph → 原样
- 用 `\n\n` 连接段落类 block，列表项内部用 `\n` 连接，最终生成 `body`。

## 错误处理

- 标题为空：提示“标题不能为空”，禁止保存。
- `localStorage` 写入失败：提示用户检查浏览器存储权限。
- `/console/edit` 无文章数据：显示空状态，引导用户去创作。

## 测试

新增 Playwright 端到端脚本：

1. 构造或生成一篇已完成作品，进入预览页。
2. 点击“编辑正文”，修改标题和某段正文。
3. 保存，验证预览页内容已更新。
4. 返回“我的作品”，重新打开该作品，验证修改已持久化。
5. 进入 `/console/edit`，验证分块编辑页可正常保存并返回预览页。

## 待办

- [ ] 实现 `articleBlocks.js` 解析/序列化函数。
- [ ] 实现 `articleStorage.js` 读写与队列同步。
- [ ] 实现 `EditIndex.vue`。
- [ ] 修改 `PreviewIndex.vue` 增加 inline 编辑能力。
- [ ] 修改 `router/index.js` 注册 `/console/edit`。
- [ ] 编写并运行端到端测试脚本。

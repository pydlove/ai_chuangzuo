# 我的风格独立菜单设计

## 背景与目标

创作页（`/console/create`）已有风格库弹框，支持在“系统预设”和“我的风格”两个 tab 之间切换并创建自定义风格。但自定义风格只能在创作页弹框内管理，无法提前设计好、在多处复用。

**目标：** 在控制台侧边栏新增“我的风格”独立入口，让用户可以提前设计并管理自己的风格；创作页的风格弹框保持不变，但与其共享同一份状态，实现多入口管理。

## 设计原则

1. **多入口一致**：侧边栏页面与创作页弹框使用同一套数据和 CRUD 逻辑。
2. **弹框不动**：创作页现有风格弹框的模板与交互尽量不动，只调整数据源引用。
3. **前端状态先行**：当前继续使用内存状态，不写入 localStorage，便于后续直接替换为后端 API。
4. **体验一致**：独立页面的风格卡片、编辑器子视图与创作页弹框保持相同视觉与交互。

## 架构

```text
ConsoleLayout.vue
  └── 侧边栏新增「我的风格」→ /console/styles

router/index.js
  └── 新增 /console/styles → StylesIndex.vue

project/user/web/src/
  ├── composables/useStyles.js   # 新增：共享风格状态与 CRUD
  ├── views/console/StylesIndex.vue   # 新增：独立风格管理页
  └── views/console/CreateIndex.vue   # 修改：风格弹框改用 useStyles
```

## 数据模型

风格对象沿用现有结构：

```ts
interface Style {
  name: string          // 风格名称，最多 20 字
  desc: string          // 简短描述
  prompt: string        // 风格提示词，最多 1000 字
  promptSummary?: string // 提示词摘要（系统预设使用）
  count?: number        // 使用次数（我的风格可选）
}
```

`useStyles.js` 导出：

- `systemStyles`：只读系统预设列表。
- `myStyles`：`ref<Style[]>`，用户自定义风格数组。
- `currentStyle`：`ref<Style>`，当前在创作页应用的风格。
- `createStyle(style)`：新建自定义风格。
- `updateStyle(oldName, style)`：更新自定义风格。
- `deleteStyle(name)`：删除自定义风格。
- `applyStyle(style)`：将指定风格设为 `currentStyle`。

## 页面设计

### 路由与入口

- 新路由：`/console/styles`，名称 `ConsoleStyles`，组件 `StylesIndex.vue`。
- 侧边栏 `navItems` 新增：`{ path: '/console/styles', label: '我的风格', icon: SmileOutlined }`（图标可选用 Ant Design Vue 中合适的图标，如 SmileOutlined、BulbOutlined 等）。

### StylesIndex.vue 结构

1. **页面标题区**
   - 标题：我的风格
   - 副标题：提前设计你的专属写作风格，创作时一键选用

2. **Tab 栏**
   - 我的风格（左侧）
   - 系统预设（右侧）

3. **我的风格 tab**
   - 卡片网格展示所有自定义风格。
   - 每个卡片显示：名称、描述、提示词前 60 字摘要、使用次数。
   - 提示词摘要由 `prompt` 截取前 60 字生成，点击“查看完整提示词”可展开。
   - 卡片操作：编辑、删除、使用（跳转创作页并应用该风格）。
   - 首个卡片为“新建我的风格”占位卡片，点击进入编辑器。
   - 空状态：使用 Ant Design `a-empty`，提示“还没有自定义风格，去创建一个吧”。

4. **系统预设 tab**
   - 卡片网格展示所有系统预设风格。
   - 每个卡片显示：名称、描述、提示词摘要（使用 `promptSummary`）。
   - 卡片操作：查看完整提示词、使用（跳转创作页并应用该风格）。
   - 系统预设只读，不可编辑或删除。

5. **编辑器子视图**
   - 点击“新建”或“编辑”后，tab 内容区切换为编辑器子视图，不叠加新弹框。
   - 编辑器字段：
     - 风格名称（输入框，必填，最多 20 字）
     - 风格提示词（文本域，必填，最多 1000 字）
     - 快速填充模板（横向卡片列表，点击填充提示词）
   - 操作：返回列表、保存。

### 创作页弹框调整

- 将 `systemStyles`、`myStyles`、`currentStyle` 及相关 CRUD 方法替换为从 `useStyles` 引入。
- 模板、样式、交互保持不变，仅数据源引用调整。

## 交互流程

### 在独立页面新建风格

1. 用户点击侧边栏“我的风格”。
2. 页面默认展示“我的风格”tab。
3. 用户点击“新建我的风格”卡片。
4. 内容区切换为编辑器子视图。
5. 用户填写名称和提示词，可点击快速模板填充。
6. 点击保存后返回列表，新风格出现在卡片网格中。
7. 点击该风格的“使用”按钮，跳转至 `/console/create`，创作页顶部风格 chip 显示该风格名称。

### 在创作页弹框新建风格

1. 用户在创作页点击风格 chip，打开风格库弹框。
2. 切换至“我的风格”tab，点击“新建我的风格”。
3. 弹框内容区切换为编辑器子视图（现有逻辑）。
4. 保存后返回弹框列表，数据同步到共享状态。
5. 在独立页面“我的风格”中也能看到新风格。

## 校验规则

- 风格名称必填，最多 20 字。
- 风格提示词必填，最多 1000 字。
- 同一风格名称不能重复（不区分大小写）。
- 保存按钮在以上任一规则不满足时禁用，并在字段下方显示错误提示。

## 测试计划

新增端到端测试脚本 `tests/e2e/verify_my_style_page.py`：

1. 访问 `/console/styles`，验证页面标题与两个 tab 存在。
2. 切换 tab，验证系统预设和我的风格列表渲染。
3. 在“我的风格”tab 新建一个风格，验证保存后出现在列表中。
4. 点击编辑，修改提示词并保存，验证列表更新。
5. 点击删除，验证该风格从列表移除。
6. 点击“使用”，验证跳转至 `/console/create` 且顶部风格 chip 显示对应名称。
7. 在创作页弹框新建风格，返回独立页面刷新（或重新进入），验证数据一致。

## 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `project/user/web/src/views/console/StylesIndex.vue` | 新增 | 我的风格独立管理页面 |
| `project/user/web/src/composables/useStyles.js` | 新增 | 共享风格状态与 CRUD |
| `project/user/web/src/router/index.js` | 修改 | 新增 `/console/styles` 路由 |
| `project/user/web/src/views/console/ConsoleLayout.vue` | 修改 | 侧边栏新增“我的风格”入口 |
| `project/user/web/src/views/console/CreateIndex.vue` | 修改 | 风格弹框改用 `useStyles` |
| `tests/e2e/verify_my_style_page.py` | 新增 | 端到端验证脚本 |

## 后续扩展

- 待后端就绪后，将 `useStyles.js` 中的内存数据替换为 API 调用（列表、新建、更新、删除）。
- 可考虑在风格卡片展示更多元信息，如最近使用时间、生成文章数等。

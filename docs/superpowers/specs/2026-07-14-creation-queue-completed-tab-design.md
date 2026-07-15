# 创作队列增加「已完成」tab

## 背景

管理端「创作队列」页面当前只有 3 个 tab：执行中 / 排队中 / 未执行。任务状态 2（已完成）在列表中无法查看，运营/管理员无法追踪已生成的文章任务。

## 目标

在创作队列页面增加「已完成」tab，展示 status=2 的任务，列布局与其他 tab 保持一致，但已完成任务不需要操作按钮。

## 方案

### 后端

无需改动。任务列表接口 `/api/v1/admin/generation/tasks` 已支持按 `status` 查询，status=2 对应已完成任务。

### 前端

1. **Tab 定义**（`project/admin/web/src/composables/useCreationQueue.js`）
   - `STATUS_TAB` 增加 `completed: 2`。

2. **页面 tab 栏**（`project/admin/web/src/views/CreationQueueView.vue`）
   - 增加 `<a-tab-pane key="completed" tab="已完成" />`。
   - 页面描述文案从"3 个 tab"改为"4 个 tab"。

3. **操作列渲染**
   - 在 `actions` slot 中判断 `activeTabKey === 'completed'` 时不渲染任何操作按钮。

## 验收标准

- 页面出现「已完成」tab，点击后加载 status=2 的任务列表。
- 已完成任务的列表列与执行中/排队中/未执行保持一致。
- 已完成任务的操作列为空。
- 切换 tab、搜索、分页、自动刷新对已完成 tab 同样生效。

## 范围外

- 已完成任务不增加「查看文章」「重新生成」等操作按钮。
- 不调整后端接口。

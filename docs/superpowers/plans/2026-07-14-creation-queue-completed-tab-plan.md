# 创作队列增加「已完成」tab 实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在管理端创作队列页面增加「已完成」tab，用于查看 status=2 的任务列表。

**架构：** 纯前端改动。任务列表接口已支持按 status 查询，只需在前端 composable 和页面模板中注册 completed 状态，并在操作列对 completed tab 隐藏按钮。

**Tech Stack:** Vue 3 + Ant Design Vue + Vite

## Global Constraints

- 不改动后端接口。
- 已完成 tab 不显示操作按钮。
- 列布局与现有 tab 保持一致。

---

### Task 1: 在 composable 中注册 completed 状态

**Files:**
- Modify: `project/admin/web/src/composables/useCreationQueue.js:24-28`

**Interfaces:**
- Consumes: 无。
- Produces: `STATUS_TAB` 增加 `completed: 2`，`activeStatus` 默认值不变（仍为 processing）。

- [ ] **Step 1: 修改 STATUS_TAB 对象**

```javascript
const STATUS_TAB = {
  processing: 1,
  queued: 0,
  failed: 3,
  completed: 2
}
```

- [ ] **Step 2: 更新文件头部注释**

将注释从 `3 个 tab` 改为 `4 个 tab`，status 说明增加 `2=completed`。

```javascript
/**
 * 创作队列 composable。
 *
 * <p>提供 4 个 tab 共享的列表状态 + 5s 自动刷新。
 * status: 0=queued 1=processing 2=completed 3=failed
 */
```

- [ ] **Step 3: 手动验证**

在浏览器控制台或临时打印确认 `STATUS_TAB.completed === 2`。

- [ ] **Step 4: Commit**

```bash
git add project/admin/web/src/composables/useCreationQueue.js
git commit -m "feat(admin-web): 创作队列 composable 增加 completed 状态"
```

---

### Task 2: 在页面中添加 tab 并隐藏已完成任务的操作按钮

**Files:**
- Modify: `project/admin/web/src/views/CreationQueueView.vue:12-16` 与 `66-93`

**Interfaces:**
- Consumes: `activeTabKey` computed（已存在，基于 `activeStatus.value`）。
- Produces: 页面渲染 4 个 tab；completed tab 操作列无按钮。

- [ ] **Step 1: 添加 completed tab**

在 `<a-tabs>` 中新增：

```vue
<a-tab-pane key="completed" tab="已完成" />
```

- [ ] **Step 2: 更新页面描述**

将 `3 个 tab 分别对应` 改为 `4 个 tab 分别对应`，并增加 `已完成（completed）`。

```vue
<p class="page-desc">
  展示用户提交的创作任务，4 个 tab 分别对应：执行中（processing）/ 排队中（queued）/ 已完成（completed）/ 未执行（failed）。
  每 5 秒自动刷新；点表格里的操作可手动重试 / 释放 lease / 标记失败。
</p>
```

- [ ] **Step 3: 隐藏 completed tab 的操作按钮**

在 `actions` slot 的最外层判断：

```vue
<template v-else-if="column.key === 'actions'">
  <a-space v-if="activeTabKey !== 'completed'">
    <!-- 原有按钮保持不变 -->
  </a-space>
  <span v-else>-</span>
</template>
```

保留原有 `a-space` 内的所有按钮不动，仅在最外层用 `v-if="activeTabKey !== 'completed'"` 控制显示， completed 时显示 `-`。

- [ ] **Step 4: 本地验证**

启动前端 dev server：

```bash
cd project/admin/web
npm run dev
```

打开创作队列页面，确认：
1. 出现「已完成」tab。
2. 点击后加载列表（如本地无 completed 数据，可 mock 或连测试环境）。
3. 已完成任务的操作列显示 `-`。
4. 其他 tab 操作列按钮正常显示。

- [ ] **Step 5: Commit**

```bash
git add project/admin/web/src/views/CreationQueueView.vue
git commit -m "feat(admin-web): 创作队列增加已完成 tab"
```

---

### Task 3: 回归验证

**Files:**
- 无文件修改。

- [ ] **Step 1: 运行前端类型检查/构建**

```bash
cd project/admin/web
npm run build
```

预期：构建成功，无新增类型错误。

- [ ] **Step 2: 端到端手动验证**

1. 切换 4 个 tab，确认每个 tab 都能正确加载对应 status 的数据。
2. 在已完成 tab 中测试分页、搜索、自动刷新。
3. 确认执行中/排队中/未执行 tab 的操作按钮行为未变。

- [ ] **Step 3: Commit（如有构建产物更新）**

仅当需要提交 `dist` 构建产物时：

```bash
git add project/admin/web/dist
git commit -m "chore(admin-web): 更新创作队列 completed tab 构建产物"
```

---

## Self-Review

- **Spec coverage:** 已完成 tab 增加（Task 1+2）、不显示操作按钮（Task 2 Step 3）、列布局一致（Task 2 Step 1-2）、后端无改动（Global Constraints）均已覆盖。
- **Placeholder scan:** 无 TBD/TODO，所有步骤含具体代码。
- **Type一致性:** `STATUS_TAB.completed` 为 number 2，与后端 status 定义一致；`activeTabKey` 为 string，与 `a-tab-pane key` 一致。

# 执行过程抽屉自动刷新

## 背景

创作队列「执行过程」抽屉打开后只调用一次 `/call-logs/by-task/{id}/grouped` 拉取 AI 调用日志，任务执行中不会自动更新。配合上一轮已实现的「每个 stage 完成后增量落库」，前端需要轮询才能展示阶段推进。

## 目标

抽屉打开期间若任务未完成（status 0/1），每 5 秒刷新一次日志；任务变为已完成（status 2）后停止轮询；关闭抽屉清理定时器。

## 方案

### 后端

**接口响应结构调整**

`GenerationCallLogController.listByTaskGrouped` 返回结构由 `Map<Integer, List<GenerationCallLogVO>>` 改为包含任务状态的包装对象：

```json
{
  "taskStatus": 1,
  "grouped": { "2": [...], "3": [...] }
}
```

**改动点**

1. 新增 VO `GenerationCallLogGroupedVO`：`taskStatus` + `grouped` 两个字段。
2. `GenerationCallLogService.queryByTaskIdGrouped` 改为返回 `GenerationCallLogGroupedVO`。从 `ctx.getTask()`（调用方传入）或新增的 `GenerationTask` 查询拿到任务状态。考虑到 service 不一定持有 task，改为接收 `taskId` 入参并由 service 查库拿 status。
3. `GenerationCallLogController` 同步改为返回新 VO。
4. 现有测试 `GenerationCallLogServiceTest` 中 `queryByTaskIdGrouped_shouldBucketByStageIndex` 需要改为 mock task 查询并断言新结构。

### 前端

1. **API 层**（`project/admin/web/src/api/creationQueue.js`）：`getGenerationCallLogsGrouped` 返回类型改为 `{ taskStatus, grouped }`。

2. **抽屉逻辑**（`project/admin/web/src/views/CreationQueueView.vue`）：
   - 打开抽屉时记录 `taskStatus`，若 `!== 2` 则启动 `setInterval` 每 5 秒调一次。
   - 每次回调更新 `callLogDrawer.grouped`；若返回的 `taskStatus === 2`，`clearInterval` 停止轮询。
   - 抽屉关闭（`callLogDrawer.open = false`）时清理定时器。
   - 复用现有 `document.visibilityState === 'visible'` 逻辑：页面不可见时跳过轮询，避免后台标签页浪费请求。
   - 用户在抽屉内交互（展开 collapse、点击）不影响轮询。

## 验收标准

- 抽屉打开后，每 5 秒自动刷新一次日志（页面可见时）。
- 任务完成后，下一次轮询检测到 `taskStatus=2`，停止轮询。
- 关闭抽屉，定时器被清理，不存在后台轮询。
- 任务未完成时查看抽屉能看到 stage 2、3 等逐步出现的日志。
- 后端响应结构变更不破坏其他接口。

## 范围外

- 不做 WebSocket/SSE 推送。
- 不在前端展示「正在刷新」指示器。
- 不改动其他日志查询接口。
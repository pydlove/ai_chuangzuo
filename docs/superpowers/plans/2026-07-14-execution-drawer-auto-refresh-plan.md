# 执行过程抽屉自动刷新实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在创作队列「执行过程」抽屉打开期间，对未完成的任务每 5 秒刷新一次日志；任务完成后停止轮询。

**架构：** 后端修改 `/call-logs/by-task/{id}/grouped` 响应结构为 `{taskStatus, grouped}`；前端抽屉根据 `taskStatus` 决定是否启动/停止 5 秒轮询。

**Tech Stack:** Spring Boot + MyBatis-Plus + Vue 3 + Ant Design Vue

## Global Constraints

- 仅 admin 端 `GenerationCallLogController.listByTaskGrouped` 接口响应结构变更，其他接口不变。
- 抽屉轮询频率固定 5 秒，与页面级 `startAutoRefresh` 一致。
- 抽屉在页面不可见时跳过本轮轮询，复用既有 `document.visibilityState` 逻辑。
- 后端改动需同步更新 `GenerationCallLogServiceTest`。

---

### Task 1: 新增包装 VO 与 service 查询任务状态

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/GenerationCallLogGroupedVO.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogService.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogServiceTest.java`

**Interfaces:**
- Consumes: `taskId: Long`
- Produces: `queryByTaskIdGrouped(Long taskId)` 返回 `GenerationCallLogGroupedVO { taskStatus, grouped }`

- [ ] **Step 1: 写失败的测试（先红）**

在 `GenerationCallLogServiceTest` 中替换现有 `queryByTaskIdGrouped_shouldBucketByStageIndex`，并新增断言：

```java
@Mock
private com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper taskMapper;

@Test
void queryByTaskIdGrouped_shouldIncludeTaskStatus() {
    com.aichuangzuo.shared.entity.GenerationTask task = new com.aichuangzuo.shared.entity.GenerationTask();
    task.setId(100L);
    task.setStatus(1);
    when(taskMapper.selectById(100L)).thenReturn(task);

    GenerationCallLog r1 = new GenerationCallLog();
    r1.setTaskId(100L); r1.setStageIndex(2); r1.setStageName("outline");
    r1.setSuccess(1); r1.setDurationMs(1000); r1.setCalledAt(LocalDateTime.now());
    when(mapper.selectByTaskId(100L)).thenReturn(List.of(r1));

    GenerationCallLogGroupedVO vo = service.queryByTaskIdGrouped(100L);

    assertEquals(1, vo.getTaskStatus());
    assertNotNull(vo.getGrouped());
    assertEquals(1, vo.getGrouped().get(2).size());
}

@Test
void queryByTaskIdGrouped_shouldDefaultTaskStatusToNullWhenTaskMissing() {
    when(taskMapper.selectById(999L)).thenReturn(null);
    when(mapper.selectByTaskId(999L)).thenReturn(List.of());

    GenerationCallLogGroupedVO vo = service.queryByTaskIdGrouped(999L);

    assertNull(vo.getTaskStatus());
    assertNotNull(vo.getGrouped());
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api
mvn test -Dtest=GenerationCallLogServiceTest -q
```

预期：编译失败（`GenerationCallLogGroupedVO` 不存在）或断言失败。

- [ ] **Step 3: 创建 VO**

`project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/GenerationCallLogGroupedVO.java`：

```java
package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 创作任务 AI 调用日志的「按 stage 分组」查询结果包装。
 * 顺带返回任务当前 status，方便前端决定是否继续轮询。
 */
@Data
public class GenerationCallLogGroupedVO {

    /** 任务当前 status（0/1/2/3）；任务不存在时为 null。 */
    private Integer taskStatus;

    /** key=stageIndex, value=该 stage 的所有 attempt 记录。 */
    private Map<Integer, List<GenerationCallLogVO>> grouped;
}
```

- [ ] **Step 4: 修改 service**

`GenerationCallLogService.java`：

1. 增加 import：

```java
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogGroupedVO;
import com.aichuangzuo.shared.entity.GenerationTask;
```

2. 增加字段：

```java
private final GenerationTaskMapper taskMapper;
```

3. 替换 `queryByTaskIdGrouped`：

```java
public GenerationCallLogGroupedVO queryByTaskIdGrouped(Long taskId) {
    List<GenerationCallLogVO> all = queryByTaskId(taskId);
    Map<Integer, List<GenerationCallLogVO>> grouped = new java.util.TreeMap<>();
    for (GenerationCallLogVO v : all) {
        grouped.computeIfAbsent(v.getStageIndex(), k -> new ArrayList<>()).add(v);
    }
    GenerationCallLogGroupedVO vo = new GenerationCallLogGroupedVO();
    vo.setGrouped(grouped);
    GenerationTask task = taskMapper.selectById(taskId);
    vo.setTaskStatus(task == null ? null : task.getStatus());
    return vo;
}
```

- [ ] **Step 5: 跑测试确认通过**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api
mvn test -Dtest=GenerationCallLogServiceTest -q
```

预期：所有测试通过（含原有 5 个 + 新增 2 个）。

- [ ] **Step 6: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/GenerationCallLogGroupedVO.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogService.java \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogServiceTest.java
git commit -m "feat(admin-api): call-logs grouped 接口返回任务状态"
```

---

### Task 2: 修改 controller 返回新 VO

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/controller/GenerationCallLogController.java`

**Interfaces:**
- Consumes: 现有 service 调用。
- Produces: `listByTaskGrouped` 返回 `Result<GenerationCallLogGroupedVO>`。

- [ ] **Step 1: 修改 controller**

```java
package com.aichuangzuo.admin.modules.generation.controller;

import com.aichuangzuo.admin.modules.generation.service.GenerationCallLogService;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogGroupedVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-创作调用日志")
@RestController
@RequestMapping("/api/v1/admin/generation/call-logs")
@RequiredArgsConstructor
public class GenerationCallLogController {

    private final GenerationCallLogService service;

    @GetMapping("/by-task/{taskId}")
    public Result<java.util.List<com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogVO>> listByTask(@PathVariable Long taskId) {
        return Result.success(service.queryByTaskId(taskId));
    }

    /**
     * 查某任务的全部调用日志（按 stage 分组），并附带任务当前 status。
     * 适合前端「执行过程」抽屉轮询使用：taskStatus=2 时停止刷新。
     */
    @GetMapping("/by-task/{taskId}/grouped")
    public Result<GenerationCallLogGroupedVO> listByTaskGrouped(@PathVariable Long taskId) {
        return Result.success(service.queryByTaskIdGrouped(taskId));
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api
mvn compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 3: 跑 generation 模块全部测试**

```bash
mvn test -Dtest="com.aichuangzuo.admin.modules.generation.**" -q
```

预期：全部通过。

- [ ] **Step 4: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/controller/GenerationCallLogController.java
git commit -m "feat(admin-api): grouped 接口返回结构改为 wrapper"
```

---

### Task 3: 前端 API 层适配新结构

**Files:**
- Modify: `project/admin/web/src/api/creationQueue.js`

**Interfaces:**
- Consumes: 后端新接口 `/call-logs/by-task/{taskId}/grouped`。
- Produces: `getGenerationCallLogsGrouped(taskId)` 返回 `{ taskStatus, grouped }`。

- [ ] **Step 1: 更新 API 注释与返回类型**

```javascript
/**
 * 查某任务的 AI 调用日志（按 stage 分组）+ 任务当前 status。
 * 返回 { taskStatus, grouped }，taskStatus=2 时前端停止轮询。
 */
export const getGenerationCallLogsGrouped = (taskId) =>
  request.get(`/api/v1/admin/generation/call-logs/by-task/${taskId}/grouped`).then((res) => res.data)
```

（注：实际返回会经过 `request` 拦截器解包，业务拿到的就是 `{ taskStatus, grouped }`。原有代码未声明返回类型，无需额外改动。）

- [ ] **Step 2: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/web/src/api/creationQueue.js
git commit -m "docs(admin-web): 更新 grouped 接口注释"
```

---

### Task 4: 前端抽屉增加轮询

**Files:**
- Modify: `project/admin/web/src/views/CreationQueueView.vue`

**Interfaces:**
- Consumes: `getGenerationCallLogsGrouped(taskId)`。
- Produces: 抽屉打开后启动 5s 轮询；taskStatus=2 或抽屉关闭时停止。

- [ ] **Step 1: 增加定时器状态**

在 `<script setup>` 顶部 import 中补充：

```javascript
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue'
```

并将 `callLogDrawer` reactive 扩展：

```javascript
const callLogDrawer = reactive({
  open: false,
  loading: false,
  task: null,
  grouped: {},
  taskStatus: null,
  pollTimer: null
})

const POLL_INTERVAL_MS = 5000
```

- [ ] **Step 2: 重构 openCallLogs 并增加轮询**

替换 `openCallLogs`：

```javascript
const fetchCallLogs = async () => {
  if (!callLogDrawer.task) return
  try {
    const data = await getGenerationCallLogsGrouped(callLogDrawer.task.id)
    callLogDrawer.grouped = data?.grouped || {}
    callLogDrawer.taskStatus = data?.taskStatus ?? null
    if (callLogDrawer.taskStatus === 2) {
      stopPoll()
    }
  } catch (e) {
    message.error(e.message || '加载调用日志失败')
  }
}

const startPoll = () => {
  stopPoll()
  callLogDrawer.pollTimer = setInterval(() => {
    if (document.visibilityState !== 'visible') return
    if (!callLogDrawer.open || callLogDrawer.taskStatus === 2) {
      stopPoll()
      return
    }
    fetchCallLogs()
  }, POLL_INTERVAL_MS)
}

const stopPoll = () => {
  if (callLogDrawer.pollTimer) {
    clearInterval(callLogDrawer.pollTimer)
    callLogDrawer.pollTimer = null
  }
}

const openCallLogs = async (record) => {
  callLogDrawer.task = record
  callLogDrawer.grouped = {}
  callLogDrawer.taskStatus = record.status ?? null
  callLogDrawer.open = true
  callLogDrawer.loading = true
  try {
    await fetchCallLogs()
    if (callLogDrawer.taskStatus !== 2) {
      startPoll()
    }
  } finally {
    callLogDrawer.loading = false
  }
}
```

- [ ] **Step 3: 抽屉关闭时停止轮询**

模板中 `a-drawer` 增加 `@close="onDrawerClose"`：

```vue
<a-drawer
  v-model:open="callLogDrawer.open"
  title="执行过程"
  width="600"
  :body-style="{ paddingTop: '8px' }"
  @close="onDrawerClose"
>
```

在 `<script setup>` 中增加：

```javascript
const onDrawerClose = () => {
  stopPoll()
}
```

- [ ] **Step 4: 组件卸载时兜底清理**

在 `onMounted` 后追加：

```javascript
onBeforeUnmount(() => {
  stopPoll()
})
```

- [ ] **Step 5: 构建验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/web
npm run build
```

预期：构建成功，无新增错误。

- [ ] **Step 6: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/web/src/views/CreationQueueView.vue
git commit -m "feat(admin-web): 执行过程抽屉打开后自动轮询"
```

---

### Task 5: 端到端验证

**Files:**
- 无文件修改。

- [ ] **Step 1: 手动验证**

启动 admin-api 与 admin-web dev server，提交一个创作任务：

```bash
# 启动 admin-api（按项目实际命令）
# 启动 admin-web
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/web && npm run dev
```

1. 在创作队列中点开一个执行中任务的「执行过程」抽屉，确认每 5 秒日志自动更新。
2. 等待任务完成后，确认抽屉停止轮询（DevTools Network 标签页不再有 `/grouped` 请求）。
3. 关闭抽屉后再打开另一个已完成的任务，确认不会启动轮询。
4. 抽屉打开时切换浏览器 tab 到其他标签页 30 秒，切回来确认无明显「积压请求」。

- [ ] **Step 2: 更新 dist 构建产物（如已部署）**

如 admin-web 通过 dist 部署：

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/web
npm run build
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/web/dist
git commit -m "chore(admin-web): 更新抽屉轮询构建产物"
```

---

## Self-Review

- **Spec 覆盖：** 后端 wrapper VO（Task 1）、service 携带 status（Task 1）、controller 返回新结构（Task 2）、测试更新（Task 1）、前端 API 注释（Task 3）、前端轮询逻辑（Task 4）、关闭时清理（Task 4）、页面不可见跳过（Task 4）均已覆盖。
- **Placeholder scan：** 无 TBD/TODO，所有步骤含具体代码。
- **Type 一致性：** `GenerationCallLogGroupedVO.taskStatus` 为 `Integer`，与 `GenerationTask.status` 类型一致；前端 `callLogDrawer.taskStatus` 也为 `Number|null`。
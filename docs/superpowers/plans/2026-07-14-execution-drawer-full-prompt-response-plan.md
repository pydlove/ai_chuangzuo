# 执行过程抽屉展示完整 prompt 与 AI 返回 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让管理端「创作队列-执行过程」抽屉显示 AI 阶段实际发送给模型的完整 userMsg（变量已替换）和 AI 完整返回，不再是模板与 200 字截断。

**Architecture:** 在 `AiCallRecord` 增加两个字段记录每次 attempt 的完整 userMsg 和 response；`DefaultAiGateway.call()` 在每次尝试中捕获当前实际传给 AI 的 userMsg（包含重试时注入的错误前缀）和 AI 返回的完整 content；DB 表把 `user_msg_preview`/`response_preview` 改成 `user_msg`/`response_content`（TEXT 列），前端读新字段并去除高度限制。

**Tech Stack:** Spring Boot + MyBatis-Plus + JDK 17（admin-api），Vue 3 + Ant Design Vue（admin-web），Flyway 迁移。

## Global Constraints

- 后端模块在 `project/admin/api/`，前端模块在 `project/admin/web/`。
- 数据库迁移文件名 `V2.0.0_031__*.sql`，按 Flyway 版本号顺序递增。
- 中文注释用一句话总结字段用途，与现有 V2.0.0_029 风格一致。
- MyBatis 列名用 snake_case；实体字段 camelCase 走 MyBatis-Plus 默认映射（无 `@TableField` 显式映射时按驼峰转下划线）。
- 前端不需要写测试脚本（CLAUDE.md 项目约定：仅后端有单元测试）。
- 任务全部完成后跑 `cd project/admin/api && mvn -q test -Dtest=GenerationCallLogServiceTest` 验证后端单测通过。

---

### Task 1: 数据库迁移 V2.0.0_031

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_031__add_full_prompt_response_to_call_log.sql`

**Interfaces:**
- 替代旧列：`a_generation_call_log.user_msg_preview`/`response_preview` → `user_msg`/`response_content`，类型 VARCHAR(256) → TEXT。
- 不影响其他列与索引。

- [ ] **Step 1: 写迁移文件**

```sql
-- 创作 AI 调用日志：把截断 preview 列升级为全文列，存储实际发给 AI 的 userMsg 和 AI 完整返回
ALTER TABLE a_generation_call_log
    ADD COLUMN user_msg TEXT NULL COMMENT '本次尝试完整 userMsg（变量已替换）' AFTER error,
    ADD COLUMN response_content TEXT NULL COMMENT 'AI 完整返回（成功时）' AFTER user_msg,
    DROP COLUMN user_msg_preview,
    DROP COLUMN response_preview;
```

- [ ] **Step 2: 验证文件位置**

```bash
ls project/admin/api/src/main/resources/db/migration/V2.0.0_031__add_full_prompt_response_to_call_log.sql
```

Expected: 路径打印，无 error。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_031__add_full_prompt_response_to_call_log.sql
git commit -m "feat(admin-api): add full user_msg/response_content columns to a_generation_call_log"
```

---

### Task 2: `AiCallRecord` 增加完整 userMsg / responseContent 字段

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/AiCallRecord.java`

**Interfaces:**
- 新增 `String userMsg`：本次 attempt 实际发送给 AI 的 userMsg（变量已替换；含重试注入的错误前缀）。
- 新增 `String responseContent`：AI 完整返回内容（成功时），失败时为 null。

- [ ] **Step 1: 编辑 `AiCallRecord.java`**

在 `private boolean success;` 之后、`private int attempt;` 之前新增：

```java
    /** 本次尝试实际发送给 AI 的完整 userMsg（含变量替换结果；重试时包含注入的错误上下文）。 */
    private String userMsg;
    /** AI 完整返回内容（成功时）；失败时为 null。 */
    private String responseContent;
```

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/AiCallRecord.java
git commit -m "feat(admin-api): add userMsg/responseContent to AiCallRecord"
```

---

### Task 3: `DefaultAiGateway.call()` 写入 userMsg / responseContent

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGateway.java`

**Interfaces:**
- 每次循环创建 `AiCallRecord rec` 后立即设置：
  - `rec.setUserMsg(currentUserMsg)` —— 全文 userMsg（含重试注入的前缀）。
  - 若 `err == null`：`rec.setResponseContent(content)`；否则保持 null。
- 不在此处截断，不影响重试 / 退避 / budget 逻辑。

- [ ] **Step 1: 编辑 `DefaultAiGateway.java` 留痕块**

找到 `// 3. 留痕` 注释下的 `AiCallRecord rec = new AiCallRecord(); ... ctx.getAiCallHistory().add(rec);` 块，在 `rec.setAttempt(attempt);` 这一行后增加：

```java
            rec.setUserMsg(currentUserMsg);
```

然后在 `rec.setError(...)` 那一行后增加：

```java
            if (err == null) {
                rec.setResponseContent(content);
            }
```

完整块应当形如：

```java
            // 3. 留痕
            AiCallRecord rec = new AiCallRecord();
            rec.setStageIndex(currentStageIndex(ctx));
            rec.setStepName(currentStepName(ctx));
            rec.setCalledAt(java.time.LocalDateTime.now());
            rec.setDurationMs(attemptDuration);
            rec.setSuccess(err == null);
            rec.setError(err == null ? null : err.getClass().getSimpleName() + ":" + err.getMessage());
            rec.setAttempt(attempt);
            rec.setUserMsg(currentUserMsg);
            if (err == null) {
                rec.setResponseContent(content);
            }
            ctx.getAiCallHistory().add(rec);
```

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGateway.java
git commit -m "feat(admin-api): capture full userMsg/responseContent per AI attempt"
```

---

### Task 4: `GenerationCallLog` 实体字段改名

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/entity/GenerationCallLog.java`

**Interfaces:**
- 字段 `userMsgPreview` → `userMsg`，`responsePreview` → `responseContent`。
- 注释同步更新。
- 保留 `error`、`durationMs` 等其他字段不动。

- [ ] **Step 1: 编辑实体**

把：

```java
    /** userMsg 前 200 字。 */
    private String userMsgPreview;

    /** AI 返回前 200 字（成功时）。 */
    private String responsePreview;
```

改为：

```java
    /** 本次尝试完整 userMsg（变量已替换）。 */
    private String userMsg;

    /** AI 完整返回内容（成功时）。 */
    private String responseContent;
```

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS（MyBatis-Plus 默认驼峰转下划线：userMsg → user_msg / responseContent → response_content）。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/entity/GenerationCallLog.java
git commit -m "refactor(admin-api): rename call log preview fields to userMsg/responseContent"
```

---

### Task 5: `GenerationCallLogVO` 字段改名

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/GenerationCallLogVO.java`

**Interfaces:**
- `userMsgPreview` → `userMsg`，`responsePreview` → `responseContent`，类型 `String`。

- [ ] **Step 1: 编辑 VO**

把：

```java
    private String userMsgPreview;
    private String responsePreview;
```

改为：

```java
    private String userMsg;
    private String responseContent;
```

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/GenerationCallLogVO.java
git commit -m "refactor(admin-api): rename VO preview fields to userMsg/responseContent"
```

---

### Task 6: `GenerationCallLogMapper.xml` 列名更新

**Files:**
- Modify: `project/admin/api/src/main/resources/mapper/GenerationCallLogMapper.xml`

**Interfaces:**
- `<insert id="batchInsert">`：字段列与 `#{}` 参数改为 `user_msg`/`response_content`。
- `<select id="selectByTaskId">`：SELECT 改为 `user_msg`/`response_content`。
- `<delete id="deleteByTaskId">` 保持不变。

- [ ] **Step 1: 编辑 mapper XML**

将 `<insert id="batchInsert">` 整段替换为：

```xml
    <insert id="batchInsert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO a_generation_call_log
            (task_id, stage_index, stage_name, attempt, success, error, duration_ms,
             called_at, user_msg, response_content, tenant_id)
        VALUES
        <foreach collection="list" item="it" separator=",">
            (#{it.taskId}, #{it.stageIndex}, #{it.stageName}, #{it.attempt}, #{it.success},
             #{it.error}, #{it.durationMs}, #{it.calledAt}, #{it.userMsg},
             #{it.responseContent}, #{it.tenantId})
        </foreach>
    </insert>
```

将 `<select id="selectByTaskId" ...>` 整段替换为：

```xml
    <select id="selectByTaskId" resultType="com.aichuangzuo.admin.modules.generation.entity.GenerationCallLog">
        SELECT id, task_id, stage_index, stage_name, attempt, success, error,
               duration_ms, called_at, user_msg, response_content,
               tenant_id, created_at
        FROM a_generation_call_log
        WHERE task_id = #{taskId}
        ORDER BY stage_index ASC, attempt ASC, id ASC
    </select>
```

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/resources/mapper/GenerationCallLogMapper.xml
git commit -m "refactor(admin-api): mapper XML columns to user_msg/response_content"
```

---

### Task 7: `GenerationCallLogService.persistAll` 直写新字段，删除旧 preview 提取方法

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogService.java`

**Interfaces:**
- `persistAll` 中 `row.setUserMsgPreview(...)` / `row.setResponsePreview(...)` → `row.setUserMsg(rec.getUserMsg())` / `row.setResponseContent(rec.getResponseContent())`。
- 删除私有方法 `extractUserMsgPreview` 与 `extractResponse`（CLAUDE.md：不用的代码必须删）。
- 删除 `PREVIEW_LEN = 200` 常量（已无人使用）。
- 保留 `truncate(String, int)`：仍用于 `error` 字段截断 500 字。
- `import` 中删除不再需要的（实际无 import 变化，BeanUtils/Mapper/VO 等仍需要）。

- [ ] **Step 1: 删除 `PREVIEW_LEN` 常量**

把：

```java
    private static final int PREVIEW_LEN = 200;
```

整行删除。

- [ ] **Step 2: 改写 `persistAll` 内字段赋值**

把：

```java
            row.setUserMsgPreview(extractUserMsgPreview(ctx, rec));
            row.setResponsePreview(rec.isSuccess()
                    ? truncate(rec.getStepName() + ":" + extractResponse(ctx, rec), PREVIEW_LEN)
                    : null);
```

改为：

```java
            row.setUserMsg(rec.getUserMsg());
            row.setResponseContent(rec.getResponseContent());
```

- [ ] **Step 3: 删除两个私有方法**

删除整个 `extractUserMsgPreview(GenerationContext ctx, AiCallRecord rec)` 方法体（从注释 + 方法签名到闭合 `}`）。

删除整个 `extractResponse(GenerationContext ctx, AiCallRecord rec)` 方法体（从方法签名到闭合 `}`）。

- [ ] **Step 4: 编译验证**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogService.java
git commit -m "refactor(admin-api): persist full userMsg/responseContent, drop preview extraction"
```

---

### Task 8: 更新 `GenerationCallLogServiceTest` 断言新字段

**Files:**
- Modify: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogServiceTest.java`

**Interfaces:**
- `rec()` 辅助方法增加 `setUserMsg(...)` / `setResponseContent(...)` 设置。
- `persistAll_shouldBatchInsertAllHistoryRecords`：每个 rec 设全文 userMsg / responseContent（成功时），断言 rows 中新字段被正确填充。

- [ ] **Step 1: 编辑测试 `rec()` 辅助方法**

把：

```java
    private AiCallRecord rec(int stageIdx, String stepName, int attempt, boolean success, String error, long ms) {
        AiCallRecord r = new AiCallRecord();
        r.setStageIndex(stageIdx);
        r.setStepName(stepName);
        r.setAttempt(attempt);
        r.setSuccess(success);
        r.setError(error);
        r.setDurationMs(ms);
        r.setCalledAt(LocalDateTime.now());
        return r;
    }
```

改为：

```java
    private AiCallRecord rec(int stageIdx, String stepName, int attempt, boolean success, String error, long ms) {
        AiCallRecord r = new AiCallRecord();
        r.setStageIndex(stageIdx);
        r.setStepName(stepName);
        r.setAttempt(attempt);
        r.setSuccess(success);
        r.setError(error);
        r.setDurationMs(ms);
        r.setCalledAt(LocalDateTime.now());
        r.setUserMsg("[stage " + stageIdx + " " + stepName + "] user msg attempt " + attempt);
        if (success) {
            r.setResponseContent("response content for " + stepName + " attempt " + attempt);
        }
        return r;
    }
```

- [ ] **Step 2: 在 `persistAll_shouldBatchInsertAllHistoryRecords` 中增加字段断言**

在该测试 `verify(mapper).batchInsert(captor.capture());` 之后、`rows.get(0).getTaskId()` 之前增加：

```java
        assertEquals("[stage 2 outline] user msg attempt 1", rows.get(0).getUserMsg());
        assertEquals("response content for outline attempt 1", rows.get(0).getResponseContent());
        // 失败的 attempt 只设 userMsg，responseContent 保持 null
        assertEquals("[stage 4 draft] user msg attempt 1", rows.get(1).getUserMsg());
        assertNull(rows.get(1).getResponseContent());
        // 第 3 行是重试成功，userMsg 可能是注入错误后的版本（这里 helper 没模拟，保持原始）
        assertEquals("[stage 4 draft] user msg attempt 2", rows.get(2).getUserMsg());
        assertEquals("response content for draft attempt 2", rows.get(2).getResponseContent());
```

- [ ] **Step 3: 运行测试**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q test -Dtest=GenerationCallLogServiceTest
```

Expected: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0。

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationCallLogServiceTest.java
git commit -m "test(admin-api): assert userMsg/responseContent populated in call log service"
```

---

### Task 9: 前端 `CreationQueueView.vue` 切换新字段 + 去除高度限制

**Files:**
- Modify: `project/admin/web/src/views/CreationQueueView.vue`

**Interfaces:**
- 模板中 `log.userMsgPreview` / `log.responsePreview` → `log.userMsg` / `log.responseContent`。
- `.attempt-preview` 的 `max-height: 160px` 改为 `max-height: 600px`（保留 `overflow-y: auto`，让抽屉本身接管过长内容的滚动；用户已选「去高度限制」）。

- [ ] **Step 1: 编辑模板的字段引用**

把：

```vue
                    <div class="attempt-label">发送 prompt（预览）</div>
                    <pre class="attempt-preview">{{ log.userMsgPreview || '-' }}</pre>
                    <div class="attempt-label">AI 返回（预览）</div>
                    <pre class="attempt-preview">{{ log.responsePreview || '-' }}</pre>
```

改为：

```vue
                    <div class="attempt-label">发送 prompt</div>
                    <pre class="attempt-preview">{{ log.userMsg || '-' }}</pre>
                    <div class="attempt-label">AI 返回</div>
                    <pre class="attempt-preview">{{ log.responseContent || '-' }}</pre>
```

- [ ] **Step 2: 编辑 `.attempt-preview` 样式**

把：

```css
.attempt-preview {
  ...
  max-height: 160px;
  overflow-y: auto;
}
```

中 `max-height: 160px;` 改为 `max-height: 600px;`。

- [ ] **Step 3: 提交**

```bash
git add project/admin/web/src/views/CreationQueueView.vue
git commit -m "feat(admin-web): display full userMsg/responseContent in execution drawer"
```

---

### Task 10: 最终验证

**Files:** （无文件改动）

- [ ] **Step 1: 跑后端单测**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q test -Dtest=GenerationCallLogServiceTest
```

Expected: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0。

- [ ] **Step 2: 全工程编译**

```bash
cd project/admin/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn -q -DskipTests compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 前端类型检查（如果装了 vue-tsc/lint 工具）**

如未配置则跳过此步；本项目无强制前端测试流程。

- [ ] **Step 4: 手动 sanity**

- 数据库新列已存在：`SHOW COLUMNS FROM a_generation_call_log;` 包含 `user_msg TEXT` 与 `response_content TEXT`，无 `user_msg_preview`/`response_preview`。
- 启动 admin-api，提交一个创作任务，打开执行过程抽屉，应能看到每个 attempt 的完整 prompt（含真实变量值）与完整 AI 返回（不被截断）。

---

## Self-Review Notes

- 类型一致：`AiCallRecord.userMsg`/`responseContent` 在 Task 2 定义；Task 3 在 gateway 内 set；Task 4 实体字段 userMsg/responseContent；Task 5 VO 同名；Task 6 mapper 列名 user_msg/response_content（驼峰转下划线）；Task 7 service 直接 `rec.getUserMsg()`/`rec.getResponseContent()`。所有命名一致。
- 覆盖 spec 全部验收点：完整 userMsg（变量替换）、完整 AI 返回、不截断、失败 attempt responseContent 为 null、DB 新列名匹配、前端字段引用更新。
- 不留 dead code：`extractUserMsgPreview`/`extractResponse`/`PREVIEW_LEN` 全部删除，符合 CLAUDE.md「不用的代码必须删」。
- 前端无过时引用：`grep -RIn userMsgPreview\|responsePreview project/admin/web/` 应无输出。
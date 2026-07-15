# 执行过程抽屉展示完整 prompt 与 AI 返回

## 背景

创作队列「执行过程」抽屉当前显示的「发送 prompt（预览）」与「AI 返回（预览）」存在两个问题：

1. **prompt 预览显示的是模板，不是实际发送给 AI 的内容**：当前 `userMsgPreview` 由 `GenerationCallLogService.extractUserMsgPreview` 生成，取的是 `ctx.stageAiPrompt(stageIndex)`（prompt 模板，含有未替换的 `${var}` 占位符），不是 step 真正拼好变量后传给 AI 的 userMsg。
2. **预览被截断到 200 字符**，且 AI 返回预览拼接了 stepName 前缀（`stepName + ":" + extractResponse(ctx, rec)`），既不全也丑。

## 目标

执行过程抽屉展示：

- 「发送 prompt」= 实际发送给 AI 的完整 userMsg（变量已替换），全文不截断。
- 「AI 返回」= AI 返回的完整内容，全文不截断，等宽格式化展示。

## 方案

### 数据库迁移 `V2.0.0_031__add_full_prompt_response_to_call_log.sql`

```sql
ALTER TABLE a_generation_call_log
    ADD COLUMN user_msg TEXT NULL COMMENT '本次尝试完整 userMsg（变量已替换）' AFTER error,
    ADD COLUMN response_content TEXT NULL COMMENT 'AI 完整返回（成功时）' AFTER user_msg,
    DROP COLUMN user_msg_preview,
    DROP COLUMN response_preview;
```

### 后端

**`AiCallRecord`（pipeline 内存留痕）**

新增字段：

```java
/** 本次尝试实际发送给 AI 的完整 userMsg（含变量替换结果，含重试时注入的错误上下文）。 */
private String userMsg;
/** AI 完整返回内容（成功时）。失败时为 null。 */
private String responseContent;
```

**`DefaultAiGateway.call()`**

每次创建 `AiCallRecord` 时：

```java
rec.setUserMsg(currentUserMsg);                    // 全文
// ...调用 AI...
if (err == null) {
    rec.setResponseContent(content);               // 全文
}
```

不要在此处截断。

**`GenerationCallLog` 实体 + `GenerationCallLogVO`**

将字段 `userMsgPreview` / `responsePreview` 重命名为 `userMsg` / `responseContent`，类型保持 `String`（MyBatis 映射到 TEXT）。

**`GenerationCallLogMapper.xml`**

`<insert id="batchInsert">` 的字段列表与 `#{}` 参数改为 `user_msg` / `response_content`，去掉 preview 列。
`<select id="selectByTaskId">` 的 SELECT 与 resultType 同步更新。

**`GenerationCallLogService.persistAll`**

直接将 `rec.getUserMsg()` / `rec.getResponseContent()` 写入对应字段，不再调用 `extractUserMsgPreview` / `extractResponse` / `truncate`。

删除 `extractUserMsgPreview` / `extractResponse` 两个私有方法（已无人调用，符合 CLAUDE.md 的「不用的代码必须删」）。

**`GenerationCallLogServiceTest`**

更新 mock 行为，断言新字段被正确填充，并删除对旧 preview 提取方法的间接断言（如有）。

### 前端

**`project/admin/web/src/views/CreationQueueView.vue`**

模板中两处预览渲染：

```vue
<pre class="attempt-preview">{{ log.userMsgPreview || '-' }}</pre>
<pre class="attempt-preview">{{ log.responsePreview || '-' }}</pre>
```

改为：

```vue
<pre class="attempt-preview">{{ log.userMsg || '-' }}</pre>
<pre class="attempt-preview">{{ log.responseContent || '-' }}</pre>
```

样式：移除 `.attempt-preview` 的 `max-height: 160px`，改为更高的 `max-height: 600px`（或保留 `overflow-y: auto`，去除硬限制让抽屉自身滚动承接更长内容）。等宽字体、`pre-wrap`、背景色、边框保持。

## 验收标准

- 执行过程抽屉中「发送 prompt」显示完整 userMsg，变量已替换为真实值。
- 「AI 返回」显示完整 AI 响应，未截断。
- 重试注入错误上下文的 prompt 也能完整展示（第 2/3 次 attempt 各自的 userMsg）。
- 失败的 attempt 仅显示 userMsg，不显示 responseContent（为 null）。
- 数据库表不再有 `user_msg_preview` / `response_preview` 列，新列为 `user_msg` / `response_content`。
- 后端单元测试通过。

## 范围外

- 不做 prompt/response 的语法高亮或折叠展开。
- 不限制单个 prompt 的最大长度。
- 不做敏感字段脱敏（后续如需要再单独处理）。
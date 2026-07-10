# 创作模板阶段 3：用户端模板选择 + 任务侧消费版本

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地设计文档 2026-07-09-de-ai-flavor-writing-pipeline-design.md 5.16.1 阶段 3：用户在创作页选模板，生成任务创建时锁定模板版本，runtime 按锁定版本跑流水线而不是直接读 `enabled=1`。

**Architecture:**

- **现状**：阶段 2 落地后，`t_prompt_template` 有状态机和版本字段，`t_prompt_template_version` 保存每次发布快照。但 `u_generation_task` 只有 `prompt_template_id`，没有版本字段；worker 通过 `PipelineTemplateResolver.findEnabled()` 直接拿最新已发布的模板。
- **改造点**：
  1. **schema**：`u_generation_task` 加 `prompt_template_version` 列，任务创建时快照。
  2. **user-api**：提交任务时接收 `templateId`（可选，不传则用默认），把当前 `latestPublishedVersion` 快照到 `prompt_template_version`。
  3. **user-api**：新增只读接口 `GET /prompt-templates`，让前端列出已发布模板供选择。
  4. **admin-api**：改造 `PipelineTemplateResolver` 接收 `(templateId, templateVersion)` 参数而不是 `findEnabled()`。
  5. **user-web**：创作页加模板下拉选择器，默认选中内置模板。
- **向后兼容**：老任务（无 `prompt_template_version` 字段）走 fallback —— worker 解析时如果任务记录没有版本字段，用 `enabled=1` 的最新版。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8, Flyway, Vue 3, Ant Design Vue, Playwright.

## 全局约束

- `prompt_template_version` 加在 `u_generation_task` 上是 NULLABLE，老数据（已有任务）填 NULL，runtime 走 fallback。
- 不破坏已有 18 个迁移；新增序号 `V2.0.0_019`。
- 阶段 1 / 2 的 16 个单测不动；新增 stage 3 测试。
- `u_generation_task.prompt_template_id`（V2.0.0_009 已有）继续保留语义为「任务引用的模板」，新增 `prompt_template_version` 表示「锁定的版本号」。

---

## 现状盘点（实施前必读）

| 已完成项 | 文件 / 位置 |
|----------|-------------|
| `t_prompt_template` 状态机 + `latest_published_version` | V2.0.0_018 |
| `t_prompt_template_version` 快照表 | V2.0.0_018 |
| `t_prompt_template_stage` 12 阶段 | V2.0.0_014 |
| `PipelineTemplateResolver` 读 `enabled=1` | 阶段 1 |
| `GenerationTask.promptTemplateId` 字段 | V2.0.0_009 |
| 16 个单测 | 阶段 1+2 |

| 缺口 | 本计划要补 |
|------|------------|
| `u_generation_task` 无版本列 | Task 1：V2.0.0_019 加列 |
| `PipelineTemplateResolver` 只读 enabled | Task 2：接 `(templateId, version)` |
| user-api 无模板列表端点 | Task 3：`GET /prompt-templates` |
| 任务提交不传/不锁版本 | Task 4：DTO + Service |
| user-web 无模板选择器 | Task 5：前端下拉 |
| 无 stage 3 单测 / E2E | Task 6+7 |

---

## File Structure

| 文件 | 职责 |
|------|------|
| `project/admin/api/src/main/resources/db/migration/V2.0.0_019__add_template_version_to_generation_task.sql` | **新增** 给 `u_generation_task` 加 `prompt_template_version` 列 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/PipelineTemplateResolver.java` | **修改** 接 `(templateId, version)` 参数 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationExecutor.java` | **修改** 调用新版 resolver |
| `project/shared/src/main/java/com/aichuangzuo/shared/entity/GenerationTask.java` | **修改** 加 `promptTemplateVersion` 字段 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/PromptTemplateQueryService.java` | **新增** user 端只读 service |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/controller/PromptTemplateQueryController.java` | **新增** user 端 `GET /prompt-templates` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/GenerationTaskService.java` | **修改** 提交时快照版本 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/dto/request/SubmitRequest.java` | **修改** 加可选 `templateId` |
| `project/user/web/src/views/console/CreateIndex.vue` | **修改** 加模板下拉 |
| `project/user/web/src/api/generation.js` | **修改** 加 `listTemplates` |
| `project/admin/api/src/test/.../service/Stage3PipelineResolverTest.java` | **新增** resolver 版本读取测试 |
| `project/user/api/src/test/.../service/PromptTemplateQueryServiceTest.java` | **新增** user 端列表测试 |
| `tests/e2e/verify_user_template_selection.py` | **新增** E2E：选非默认模板 → 任务锁定版本 |

---

## Task 1：V2.0.0_019 加版本列

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_019__add_template_version_to_generation_task.sql`

- [ ] **Step 1.1：写迁移**

```sql
-- 阶段 3：生成任务锁定模板版本
-- 设计文档：docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.15.6
-- 老任务（无 version）保持 NULL，runtime 走 fallback（用 enabled=1 的最新版）

SET NAMES utf8mb4;

ALTER TABLE u_generation_task
    ADD COLUMN prompt_template_version INT UNSIGNED DEFAULT NULL
        COMMENT '任务创建时锁定的模板版本号；NULL=fallback 到 enabled=1';
```

- [ ] **Step 1.2：起 admin-api 自动跑迁移**

```bash
mvn -pl admin/api -am spring-boot:run
```

观察日志：`Successfully applied 19 migrations`，查：

```sql
DESC u_generation_task;
-- 应看到 prompt_template_version INT UNSIGNED DEFAULT NULL
```

- [ ] **Step 1.3：提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_019__add_template_version_to_generation_task.sql
git commit -m "feat(creative-template): add prompt_template_version column to generation task"
```

---

## Task 2：resolver 接 `(templateId, version)` 参数

**Files:**
- Modify: `project/admin/api/.../pipeline/PipelineTemplateResolver.java`
- Modify: `project/admin/api/.../service/GenerationExecutor.java`

- [ ] **Step 2.1：实体加字段**

`GenerationTask.java` 加：

```java
private Integer promptTemplateVersion;
```

- [ ] **Step 2.2：resolver 重构**

把 `resolveInto(GenerationContext ctx)` 改成 `resolveInto(GenerationContext ctx, Long templateId, Integer templateVersion)`：

```java
public void resolveInto(GenerationContext ctx, Long templateId, Integer templateVersion) {
    PromptTemplate template;
    if (templateId != null && templateVersion != null) {
        // 优先按任务锁定的 (templateId, version) 取
        template = mapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
        }
        // 从 version 表拿快照，没有则从 stage 表取
        PromptTemplateVersion v = versionMapper.selectByTemplateId(templateId).stream()
                .filter(x -> x.getVersion() != null && x.getVersion().equals(templateVersion))
                .findFirst()
                .orElse(null);
        if (v != null) {
            ctx.setConfigJsonSnapshot(v.getConfigJson());
        }
        // stage 表照常读（用于实际跑）
    } else {
        // fallback：找当前 enabled=1
        template = templateService.findEnabled()
                .orElseThrow(() -> new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_ENABLED));
    }
    ctx.setTemplate(template);
    // ... 原 stage 装载逻辑
}
```

- [ ] **Step 2.3：GenerationContext 加 configJsonSnapshot**

```java
private String configJsonSnapshot;  // 来自 version 表，仅供回溯/调试
```

- [ ] **Step 2.4：GenerationExecutor 传版本**

```java
Long templateId = task.getPromptTemplateId();
Integer templateVersion = task.getPromptTemplateVersion();
templateResolver.resolveInto(ctx, templateId, templateVersion);
```

- [ ] **Step 2.5：编译**

```bash
mvn -pl shared -am compile -DskipTests
mvn -pl admin/api -am compile -DskipTests
```

- [ ] **Step 2.6：提交**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/entity/GenerationTask.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/PipelineTemplateResolver.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationContext.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationExecutor.java
git commit -m "feat(creative-template): pipeline resolver accepts (templateId, version) locking"
```

---

## Task 3：user 端只读接口

**Files:**
- Create: `project/user/api/.../service/PromptTemplateQueryService.java`
- Create: `project/user/api/.../vo/PromptTemplatePublicVO.java`
- Create: `project/user/api/.../controller/PromptTemplateQueryController.java`

- [ ] **Step 3.1：DTO/VO**

`PromptTemplatePublicVO.java`（仅暴露必要字段，不含 prompt 全文）：

```java
@Data
public class PromptTemplatePublicVO {
    private Long id;
    private String name;
    private String remark;
    private Integer latestPublishedVersion;
    private Boolean isBuiltin;
}
```

- [ ] **Step 3.2：Service**

```java
@Service
@RequiredArgsConstructor
public class PromptTemplateQueryService {
    private final PromptTemplateMapper mapper;

    public List<PromptTemplatePublicVO> listPublished() {
        return mapper.selectPublished().stream().map(t -> {
            PromptTemplatePublicVO vo = new PromptTemplatePublicVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setRemark(t.getRemark());
            vo.setLatestPublishedVersion(t.getLatestPublishedVersion());
            vo.setIsBuiltin(t.getId() != null && t.getId() == CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
            return vo;
        }).toList();
    }

    public PromptTemplatePublicVO detail(Long id) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null || t.getTemplateStatus() == null
                || t.getTemplateStatus() != TemplateStatus.PUBLISHED.code) {
            throw new BusinessException(...);
        }
        // ...
    }
}
```

`PromptTemplateMapper.selectPublished()` 新方法（admin 端 mapper 镜像）：

```java
@Select("SELECT * FROM t_prompt_template WHERE template_status = 1 AND is_deleted = 0 ORDER BY id ASC")
List<PromptTemplate> selectPublished();
```

- [ ] **Step 3.3：Controller**

```java
@RestController
@RequestMapping("/api/v1/user/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateQueryController {
    private final PromptTemplateQueryService service;

    @GetMapping
    public Result<List<PromptTemplatePublicVO>> list() {
        return Result.success(service.listPublished());
    }

    @GetMapping("/{id}")
    public Result<PromptTemplatePublicVO> detail(@PathVariable Long id) {
        return Result.success(service.detail(id));
    }
}
```

- [ ] **Step 3.4：编译**

```bash
mvn -pl user/api -am compile -DskipTests
```

- [ ] **Step 3.5：提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/PromptTemplateQueryService.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/controller/PromptTemplateQueryController.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/vo/PromptTemplatePublicVO.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/PromptTemplateMapper.java \
        project/admin/api/src/main/resources/mapper/PromptTemplateMapper.xml
git commit -m "feat(creative-template): add user-side readonly template list/detail endpoints"
```

---

## Task 4：任务提交时快照版本

**Files:**
- Modify: `project/user/api/.../dto/request/SubmitRequest.java`
- Modify: `project/user/api/.../service/GenerationTaskService.java`

- [ ] **Step 4.1：DTO 加可选 templateId**

```java
private Long templateId;  // 可选，不传走默认（id=1）
```

- [ ] **Step 4.2：submit() 快照版本**

```java
@Transactional
public Long submit(SubmitRequest req, Long userId) {
    // 解析模板：优先用 req.templateId，不传则用默认（id=1）
    Long templateId = req.getTemplateId() != null ? req.getTemplateId() : 1L;
    PromptTemplate template = templateMapper.selectById(templateId);
    if (template == null || template.getTemplateStatus() != TemplateStatus.PUBLISHED.code) {
        throw new BusinessException(UserGenerationErrorCode.TEMPLATE_NOT_AVAILABLE);
    }
    Integer lockedVersion = template.getLatestPublishedVersion();
    if (lockedVersion == null) {
        throw new BusinessException(UserGenerationErrorCode.TEMPLATE_NOT_PUBLISHED);
    }

    // 写任务（含锁定的版本号）
    GenerationTask task = new GenerationTask();
    // ... 现存字段
    task.setPromptTemplateId(templateId);
    task.setPromptTemplateVersion(lockedVersion);
    taskMapper.insert(task);
    return task.getId();
}
```

- [ ] **Step 4.3：错误码**

在 `UserGenerationErrorCode` 加：

```java
TEMPLATE_NOT_AVAILABLE(114008, "所选模板不可用"),
TEMPLATE_NOT_PUBLISHED(114009, "所选模板未发布");
```

- [ ] **Step 4.4：编译**

```bash
mvn -pl user/api -am compile -DskipTests
```

- [ ] **Step 4.5：提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/dto/request/SubmitRequest.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/GenerationTaskService.java \
        project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserGenerationErrorCode.java
git commit -m "feat(creative-template): user task submission locks template version"
```

---

## Task 5：user-web 创作页加模板下拉

**Files:**
- Modify: `project/user/web/src/views/console/CreateIndex.vue`
- Modify: `project/user/web/src/api/generation.js`

- [ ] **Step 5.1：API 加 listTemplates**

```js
export function listPromptTemplates() {
  return request.get('/api/v1/user/prompt-templates').then((res) => res.data)
}
```

- [ ] **Step 5.2：CreateIndex 加下拉**

```vue
<a-form-item label="创作模板">
  <a-select v-model:value="form.templateId" placeholder="选择创作模板">
    <a-select-option
      v-for="t in templateOptions"
      :key="t.id"
      :value="t.id"
    >
      {{ t.name }}
      <span v-if="t.isBuiltin" style="color: #07c160; margin-left: 4px;">(内置)</span>
      <span v-if="t.latestPublishedVersion" style="color: #8c8c8c; margin-left: 4px;">v{{ t.latestPublishedVersion }}</span>
    </a-select-option>
  </a-select>
</a-form-item>
```

初始化：

```js
const templateOptions = ref([])
onMounted(async () => {
  templateOptions.value = await listPromptTemplates()
  form.value.templateId = templateOptions.value.find(t => t.isBuiltin)?.id ?? templateOptions.value[0]?.id
})
```

提交时带上 `templateId: form.value.templateId`。

- [ ] **Step 5.3：编译**

```bash
cd project/user/web && npx vite build
```

- [ ] **Step 5.4：提交**

```bash
git add project/user/web/src/views/console/CreateIndex.vue \
        project/user/web/src/api/generation.js
git commit -m "feat(creative-template): user creation page shows template selector"
```

---

## Task 6：阶段 3 单测

**Files:**
- Create: `project/admin/api/src/test/.../pipeline/Stage3PipelineResolverTest.java`
- Create: `project/user/api/src/test/.../service/PromptTemplateQueryServiceTest.java`

- [ ] **Step 6.1：admin 端 resolver 测试**

```java
@Test
void resolveInto_shouldUseLockedVersion() {
    Long tid = 2L;
    Integer ver = 5;
    PromptTemplate tpl = sampleTemplate(tid);
    tpl.setTemplateStatus(TemplateStatus.PUBLISHED.code);
    tpl.setLatestPublishedVersion(ver);
    when(templateMapper.selectById(tid)).thenReturn(tpl);
    PromptTemplateVersion v = new PromptTemplateVersion();
    v.setTemplateId(tid);
    v.setVersion(ver);
    v.setConfigJson("{\"stages\":[]}");
    v.setVersionStatus(TemplateStatus.PUBLISHED.code);
    when(versionMapper.selectByTemplateId(tid)).thenReturn(List.of(v));

    GenerationContext ctx = new GenerationContext();
    resolver.resolveInto(ctx, tid, ver);

    assertEquals("{\"stages\":[]}", ctx.getConfigJsonSnapshot());
    assertEquals(tid, ctx.getTemplate().getId());
}

@Test
void resolveInto_shouldFallbackToEnabledWhenNoVersion() {
    when(templateService.findEnabled()).thenReturn(Optional.of(sampleTemplate(1L)));
    GenerationContext ctx = new GenerationContext();
    resolver.resolveInto(ctx, null, null);
    assertEquals(1L, ctx.getTemplate().getId());
}
```

- [ ] **Step 6.2：user 端 service 测试**

```java
@Test
void listPublished_shouldFilterToStatusPublished() {
    when(mapper.selectPublished()).thenReturn(List.of(samplePublished(1L), samplePublished(2L)));
    List<PromptTemplatePublicVO> list = service.listPublished();
    assertEquals(2, list.size());
    assertEquals(1L, list.get(0).getId());
}

@Test
void detail_shouldThrowForDraftTemplate() {
    PromptTemplate t = samplePublished(5L);
    t.setTemplateStatus(TemplateStatus.DRAFT.code);
    when(mapper.selectById(5L)).thenReturn(t);
    assertThrows(BusinessException.class, () -> service.detail(5L));
}
```

- [ ] **Step 6.3：跑**

```bash
mvn -pl admin/api -am test -Dtest=Stage3PipelineResolverTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -pl user/api -am test -Dtest=PromptTemplateQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

- [ ] **Step 6.4：提交**

```bash
git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/Stage3PipelineResolverTest.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/generation/service/PromptTemplateQueryServiceTest.java
git commit -m "test(creative-template): add stage 3 unit tests"
```

---

## Task 7：E2E 验证选模板 + 版本锁定

**Files:**
- Create: `tests/e2e/verify_user_template_selection.py`

- [ ] **Step 7.1：脚本骨架**

1. admin 登录 → 创建自定义模板 v1 → 验证 user 端 `GET /prompt-templates` 能看到
2. user 登录 → 创作页 GET `/prompt-templates` 列出 → 选自定义模板提交任务
3. 查 `a_generation_task` 记录：`prompt_template_id=自定义, prompt_template_version=1`
4. admin 端重新发布自定义模板 → v2
5. user 再提交一条任务 → v2 也被锁定
6. 老任务（无 version）走 fallback：worker 解析时用 enabled=1

- [ ] **Step 7.2：跑**

```bash
./scripts/local/start-all.sh
python3 tests/e2e/verify_user_template_selection.py
```

- [ ] **Step 7.3：提交**

```bash
git add tests/e2e/verify_user_template_selection.py
git commit -m "test(creative-template): add user template selection E2E"
```

---

## Self-Review

**1. Spec coverage（设计文档 5.16.1 阶段 3 + §5.15.4 + §5.15.6 验收）：**

| 验收项 | 覆盖 Task |
|--------|-----------|
| §5.15.4 用户端只读接口 | Task 3 |
| §5.15.6 生成任务加列 | Task 1 |
| 任务提交锁定版本 | Task 4 |
| runtime 按锁定版本跑 | Task 2 |
| user-web 模板下拉 | Task 5 |
| 兼容性 fallback | Task 2（resolveInto 接 nullable）+ Task 4（templateId 可选） |

**2. 不破坏阶段 1 / 2：**

- V2.0.0_019 是纯 ALTER + NULLABLE DEFAULT，老任务不需迁移。
- `prompt_template_version` NULL 时 resolver 走 fallback（找 enabled=1），与阶段 1 的默认模板兼容。
- 阶段 1/2 的 16 个单测不动。

**3. 范围控制（不做）：**

- 不实现模板收藏/标签（设计文档 §5.13 提到但未必要）
- 不实现 user 自定义模板（仍只读）
- 不实现版本 diff / 回滚（设计文档 §5.14 Q3 待讨论）

**4. 可回滚：**

- V2.0.0_019 单列删除即可回滚
- 改回 resolver 调用方（恢复 findEnabled 调用）是 1 行 revert

**5. 风险：**

- 老任务的 `prompt_template_id` 已有但可能不准确（运行时没用到）；本次只新增 `prompt_template_version` 列，不动 `prompt_template_id` 已有值。
- user 端 mapper 镜像时需要保证 schema 可见性：admin/user 共享同一 MySQL，admin-api 持有 migration 即可。

阶段 3 完成可进入阶段 4（自定义模板生效：user 端可看到非内置模板的差异生效）和阶段 5（示例模板库发布）。
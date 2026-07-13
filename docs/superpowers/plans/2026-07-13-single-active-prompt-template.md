# 创作模板单一生效 + 用户端创作直连 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 管理端任意时刻只有 1 条已发布创作模板（发布=唯一生效），用户端创作页点"生成文章"直接按该模板 12 阶段执行，移除 /console/ai-generate 页及全部死代码。

**Architecture:** 发布事务内先全表下线再发布目标，保证 `t_prompt_template.template_status=1` 最多 1 条；删除旧的 `enabled` 双轨字段。用户端提交生成不再传 templateId，user-api 自动锁定唯一已发布模板最新版本写入任务；admin-api worker 按锁定版本快照执行（现有逻辑），fallback 从 enabled=1 改为唯一 PUBLISHED。

**Tech Stack:** JDK 17 + Spring Boot + MyBatis-Plus + Flyway（admin-api / user-api 双模块共享库）、Vue 3 + Ant Design Vue + Vite（admin-web / user-web）、JUnit 5 + Mockito。

**设计文档：** `docs/superpowers/specs/2026-07-13-single-active-prompt-template-design.md`

## Global Constraints

- 仓库根：`/Users/panyong/aio_project/ai_chuangzuo`，所有 git 命令先 `cd` 到根目录。
- JDK 17；MyBatis-Plus LambdaQueryWrapper/LambdaUpdateWrapper；Lombok。
- Flyway 迁移只放 `project/admin/api/src/main/resources/db/migration/`，下一个版本号 `V2.0.0_024`。
- 提交信息格式：`type(scope): 中文描述`（参考 `git log --oneline -5`）。
- **不用的代码开发结束后必须删掉**：删除的接口/字段/函数需 grep 确认无调用方。
- Mockito 单测不需要数据库；跑全量测试套件时需 `MYSQL_USERNAME=root MYSQL_PASSWORD=123456` 且先清理 `hot_search_daily` 当日数据（预存测试隔离问题，与本需求无关）。
- 阶段表 `t_prompt_template_stage.enabled`（阶段级开关）**保留不动**；本次只删模板级 `t_prompt_template.enabled`。

---

### Task 1: admin-api 发布唯一化 + 删除 enabled 双轨（后端全量）

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_024__drop_template_enabled_and_enforce_single_published.sql`
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateServicePublishTest.java`
- Modify: `project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplate.java`
- Modify: `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminGenerationErrorCode.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/PromptTemplateMapper.java`
- Modify: `project/admin/api/src/main/resources/mapper/PromptTemplateMapper.xml`（若不在此路径，`find project/admin/api/src/main/resources -name PromptTemplateMapper.xml` 定位）
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateService.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/controller/PromptTemplateAdminController.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/PipelineTemplateResolver.java:61-66`
- Modify: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/PipelineTemplateResolverTest.java`

**Interfaces:**
- Produces: `PromptTemplateService.findPublished(): Optional<PromptTemplate>`；`AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_PUBLISHED`；`publish` 新语义：事务内先下线其他已发布模板及其已发布版本，再发布目标。

- [ ] **Step 1: 写发布唯一性失败测试**

创建 `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateServicePublishTest.java`：

```java
package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateStageMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateVersionMapper;
import com.aichuangzuo.shared.creative.TemplateStatus;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServicePublishTest {

    @Mock
    private PromptTemplateMapper mapper;

    @Mock
    private PromptTemplateStageMapper stageMapper;

    @Mock
    private PromptTemplateVersionMapper versionMapper;

    @InjectMocks
    private PromptTemplateService service;

    @Test
    void publish_shouldOfflineOtherPublishedTemplatesAndVersions() {
        PromptTemplate t = new PromptTemplate();
        t.setId(2L);
        t.setTemplateStatus(TemplateStatus.DRAFT.code);
        when(mapper.selectById(2L)).thenReturn(t);
        when(stageMapper.selectByTemplateId(2L)).thenReturn(List.of());
        when(versionMapper.selectByTemplateId(2L)).thenReturn(List.of());

        Long version = service.publish(2L, "note", 1L);

        assertEquals(1L, version);
        // 先下线其他已发布模板 + 其他模板的已发布版本（update(null, wrapper) 各一次）
        verify(mapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(versionMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        // 目标模板置为已发布 v1
        ArgumentCaptor<PromptTemplate> captor = ArgumentCaptor.forClass(PromptTemplate.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(TemplateStatus.PUBLISHED.code, captor.getValue().getTemplateStatus());
        assertEquals(1, captor.getValue().getLatestPublishedVersion());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api && mvn -q test -Dtest=PromptTemplateServicePublishTest
```

Expected: 测试失败（publish 当前不会调 `mapper.update(null, wrapper)`，verify 不通过）。

- [ ] **Step 3: 改 publish —— 事务内先下线其他已发布模板**

修改 `PromptTemplateService.publish`（当前 L191-235）。在 `// 1. 快照 12 阶段到 version 表` 这一行之前插入第 0 步：

```java
        // 0. 唯一生效：先把其他已发布模板及其已发布版本置为已下线
        LambdaUpdateWrapper<PromptTemplate> offlineOthers = Wrappers.lambdaUpdate(PromptTemplate.class)
                .eq(PromptTemplate::getTemplateStatus, TemplateStatus.PUBLISHED.code)
                .ne(PromptTemplate::getId, id)
                .set(PromptTemplate::getTemplateStatus, TemplateStatus.OFFLINE.code);
        mapper.update(null, offlineOthers);
        LambdaUpdateWrapper<PromptTemplateVersion> offlineOtherVersions = Wrappers.lambdaUpdate(PromptTemplateVersion.class)
                .eq(PromptTemplateVersion::getVersionStatus, TemplateStatus.PUBLISHED.code)
                .ne(PromptTemplateVersion::getTemplateId, id)
                .set(PromptTemplateVersion::getVersionStatus, TemplateStatus.OFFLINE.code);
        versionMapper.update(null, offlineOtherVersions);
```

- [ ] **Step 4: 跑测试确认通过**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api && mvn -q test -Dtest=PromptTemplateServicePublishTest
```

Expected: `Tests run: 1, Failures: 0, Errors: 0`。

- [ ] **Step 5: 改 resolver 测试（先红）**

`PipelineTemplateResolverTest.java` 做 4 处替换：
- `when(templateService.findEnabled()).thenReturn(Optional.of(enabled));` → `findPublished()`（L71）
- `verify(templateService).findEnabled();` → `findPublished()`（L80）
- `when(templateService.findEnabled()).thenReturn(Optional.empty());` → `findPublished()`（L135）
- `AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_ENABLED.getCode()` → `AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_PUBLISHED.getCode()`（L141）

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api && mvn -q test -Dtest=PipelineTemplateResolverTest
```

Expected: 编译失败（`findPublished` / `PROMPT_TEMPLATE_NO_PUBLISHED` 尚不存在）。

- [ ] **Step 6: 实现 findPublished + 错误码 + resolver fallback**

`PromptTemplateService.java`：删除 `findEnabled()`（L55-58），替换为：

```java
    public Optional<PromptTemplate> findPublished() {
        List<PromptTemplate> list = mapper.selectPublished();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
```

`AdminGenerationErrorCode.java`：
- 删除 `PROMPT_TEMPLATE_ENABLED_DUPLICATE(308003, "已有启用的提示词模板"),`（L14，已确认零调用方）；
- `PROMPT_TEMPLATE_NO_ENABLED(308010, "当前没有启用的提示词模板"),`（L21）→ `PROMPT_TEMPLATE_NO_PUBLISHED(308010, "当前没有已发布的提示词模板"),`。

`PipelineTemplateResolver.java` L61-66 改为：

```java
        } else {
            // fallback：找当前唯一已发布
            template = templateService.findPublished()
                    .orElseThrow(() -> new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_PUBLISHED));
            log.debug("resolved template id={} (fallback to published)", template.getId());
        }
```

类注释 L24 的 `找当前 enabled=1` 改为 `找当前唯一已发布（template_status=1）`。

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api && mvn -q test -Dtest=PipelineTemplateResolverTest
```

Expected: 全部通过。

- [ ] **Step 7: 删 enabled —— entity / mapper / service / controller**

`project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplate.java`：删除 `enabled` 字段及其注释（L38-39）；类注释 L22 改为：

```java
 * <p>运行时约束：全表最多 1 条 template_status=PUBLISHED，由发布事务保证。
```

`PromptTemplateMapper.java`：删除 `selectEnabled()` 方法及注释（保留 `selectPublished()`）。

`PromptTemplateMapper.xml`：先 `cat` 确认只有 selectEnabled 一个 statement，然后整个文件删除。

`PromptTemplateService.java`：
- 删除 `enable()`（L143-159）与 `disable()`（L161-171）整个方法；
- `create()`：删 `t.setEnabled(0);`；
- `update()`：`BeanUtils.copyProperties(req, exist, "id", "enabled", "stages")` → `BeanUtils.copyProperties(req, exist, "id", "stages")`；
- `offline()`：删 `t.setEnabled(0);`，方法注释改为 `下线模板：把状态置为 OFFLINE。`；
- `publish()` 第 3 步：删 `t.setEnabled(1);`；
- `clone()`：copyProperties 排除列表去掉 `"enabled", `，删 `copy.setEnabled(0);`；
- 类注释（L41-44）改为：`Admin 端-提示词模板服务：CRUD + 发布/下线 + 12 阶段配置管理。<p>生效策略：全表最多 1 条 PUBLISHED，由发布事务保证。`

`PromptTemplateAdminController.java`：删除 `enable`（L62-67）与 `disable`（L69-74）两个端点方法。

- [ ] **Step 8: 写迁移脚本**

创建 `project/admin/api/src/main/resources/db/migration/V2.0.0_024__drop_template_enabled_and_enforce_single_published.sql`：

```sql
-- 创作模板单一生效：数据修正 + 删 enabled 双轨字段
-- 设计文档：docs/superpowers/specs/2026-07-13-single-active-prompt-template-design.md

SET NAMES utf8mb4;

-- 1. 数据修正：只保留 id 最小的已发布模板，其余置为已下线
UPDATE t_prompt_template
SET template_status = 2
WHERE template_status = 1
  AND is_deleted = 0
  AND id != (SELECT min_id FROM (
      SELECT MIN(id) AS min_id FROM t_prompt_template
      WHERE template_status = 1 AND is_deleted = 0
  ) tmp);

-- 2. 被下线的模板，其已发布版本行同步置 OFFLINE（与 service.offline 行为一致）
UPDATE t_prompt_template_version v
    JOIN t_prompt_template t ON t.id = v.template_id
SET v.version_status = 2
WHERE t.template_status = 2
  AND v.version_status = 1
  AND v.is_deleted = 0;

-- 3. 删 enabled 双轨字段
ALTER TABLE t_prompt_template DROP COLUMN enabled;
```

- [ ] **Step 9: 编译 + 全量 generation 模块测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api && mvn -q test -Dtest='PromptTemplate*,Pipeline*'
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api && mvn -q compile
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && mvn -q compile
```

Expected: 全部通过/成功。user-api 若报 `getEnabled/setEnabled` 错误，按报错位置删除对应调用（预期没有，user-api 只用 templateStatus）。

- [ ] **Step 10: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && git add project/admin/api project/shared && git commit -m "feat(admin-api): 发布=唯一生效，删除 enabled 双轨"
```

---

### Task 2: admin-web 清理 enable/disable + 发布确认文案

**Files:**
- Modify: `project/admin/web/src/api/promptTemplate.js`
- Modify: `project/admin/web/src/composables/usePromptTemplate.js`
- Modify: `project/admin/web/src/views/PromptTemplateListView.vue`

**Interfaces:** 无（纯前端清理 + 文案）。

- [ ] **Step 1: 删 api 层 enable/disable**

`project/admin/web/src/api/promptTemplate.js`：删除 `enableTemplate`（L19-21）与 `disableTemplate`（L23-25）两个函数。

- [ ] **Step 2: 删 composable 里的死代码**

`project/admin/web/src/composables/usePromptTemplate.js`：
- 删除 import 中的 `enableTemplate,`（L7）与 `disableTemplate,`（L8）；
- 删除 `handleEnable` 与 `handleDisable` 两个函数（L87-97）；
- 删除 return 对象中的 `handleEnable,` 与 `handleDisable,` 两行。

删完验证：`grep -n "enable\|disable" project/admin/web/src/composables/usePromptTemplate.js project/admin/web/src/api/promptTemplate.js` → 无输出。

- [ ] **Step 3: 发布确认文案 + 生效中 tag**

`PromptTemplateListView.vue`：
- 状态列（L37-44）已发布 tag 改为"生效中"：

```html
          <template v-if="column.key === 'templateStatus'">
            <a-tag v-if="record.templateStatus === 1" color="green">生效中</a-tag>
            <a-tag v-else-if="record.templateStatus === 2" color="default">{{ record.templateStatusLabel }}</a-tag>
            <a-tag v-else color="orange">{{ record.templateStatusLabel || '草稿' }}</a-tag>
```

- `onPublish`（L142-158）Modal 的 `content` 改为：

```js
    content: `发布后将自动下线当前已发布模板。将创建版本 v${(record.latestPublishedVersion || 0) + 1}。`,
```

- [ ] **Step 4: 构建验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/web && npm run build 2>&1 | tail -5
```

Expected: 构建成功。

- [ ] **Step 5: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && git add project/admin/web && git commit -m "refactor(admin-web): 删 enable/disable 死代码，发布提示唯一生效"
```

---

### Task 3: user-api 提交锁定唯一已发布模板 + 删模板查询链

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/dto/request/GenerationSubmitRequest.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/GenerationTaskService.java:85-98,111`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/mapper/UserPromptTemplateMapper.java`
- Modify: `project/user/api/src/test/java/com/aichuangzuo/user/modules/generation/service/GenerationTaskServiceTest.java`
- Delete: `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/controller/PromptTemplateQueryController.java`
- Delete: `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/PromptTemplateQueryService.java`
- Delete: `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/vo/PromptTemplatePublicVO.java`
- Delete: `project/user/api/src/test/java/com/aichuangzuo/user/modules/generation/service/PromptTemplateQueryServiceTest.java`

**Interfaces:**
- Produces: `POST /api/v1/user/generation-tasks` 请求体不再有 `templateId` 字段（Task 4 前端对应）。

- [ ] **Step 1: 改测试（先红）—— stubCommonFlow 改用 selectPublished + 两个新测试**

`GenerationTaskServiceTest.java`：
- `stubCommonFlow` 中 `when(promptTemplateMapper.selectById(any())).thenReturn(tpl);` → `when(promptTemplateMapper.selectPublished()).thenReturn(List.of(tpl));`（缺 `import java.util.List;` 则补）；
- 类内补两个测试：

```java
    @Test
    void submit_shouldLockUniquePublishedTemplate() {
        Long userId = 9L;
        stubCommonFlow(userId);
        PromptTemplate tpl = new PromptTemplate();
        tpl.setId(7L);
        tpl.setTemplateStatus(com.aichuangzuo.shared.creative.TemplateStatus.PUBLISHED.code);
        tpl.setLatestPublishedVersion(3);
        when(promptTemplateMapper.selectPublished()).thenReturn(List.of(tpl));

        service.submit(sampleRequest(""), userId);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getPromptTemplateId());
        assertEquals(3, captor.getValue().getPromptTemplateVersion());
    }

    @Test
    void submit_shouldFailWhenNoPublishedTemplate() {
        Long userId = 10L;
        when(benefitResolver.ratePerMinute(userId)).thenReturn(5);
        when(activeModelConfigMapper.selectActiveId()).thenReturn(10L);
        when(coinRecordService.getBalance(userId)).thenReturn(BigDecimal.TEN);
        when(promptTemplateMapper.selectPublished()).thenReturn(List.of());

        com.aichuangzuo.shared.exception.BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.aichuangzuo.shared.exception.BusinessException.class,
                        () -> service.submit(sampleRequest(""), userId));
        assertEquals(
                com.aichuangzuo.shared.enums.error.UserGenerationErrorCode
                        .GENERATION_TEMPLATE_DISABLED.getCode(),
                e.getCode());
    }
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && mvn -q test -Dtest=GenerationTaskServiceTest
```

Expected: 失败（submit 当前查 selectById，stub 不命中 → NPE 或断言失败）。

- [ ] **Step 3: 改 submit 模板解析 + DTO 去 templateId**

`GenerationTaskService.java` 第 4 步（L85-98）整体替换为：

```java
        // 4. 锁定唯一已发布模板（task 锁定版本）
        List<PromptTemplate> published = promptTemplateMapper.selectPublished();
        if (published.isEmpty()) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED);
        }
        PromptTemplate template = published.get(0);
        Integer lockedVersion = template.getLatestPublishedVersion();
        if (lockedVersion == null) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED);
        }
```

第 5 步入队段：`task.setPromptTemplateId(requestedTemplateId);` → `task.setPromptTemplateId(template.getId());`；log 语句里的 `requestedTemplateId` → `template.getId()`。
删除不再使用的 import：`CreativeTemplateConstants`、`TemplateStatus`（`mvn compile` 报未使用警告不影响构建，手动删）。

`GenerationSubmitRequest.java`：删除文件末尾的 `templateId` 字段及其注释。

`UserPromptTemplateMapper.java`：删除 `selectById` 方法及 `@Param` import。

- [ ] **Step 4: 删查询链 4 文件**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && git rm \
  project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/controller/PromptTemplateQueryController.java \
  project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/PromptTemplateQueryService.java \
  project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/vo/PromptTemplatePublicVO.java \
  project/user/api/src/test/java/com/aichuangzuo/user/modules/generation/service/PromptTemplateQueryServiceTest.java
```

- [ ] **Step 5: 跑测试确认通过**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && mvn -q test -Dtest=GenerationTaskServiceTest
```

Expected: 全部通过。

- [ ] **Step 6: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && git add project/user/api && git commit -m "feat(user-api): 提交生成锁定唯一已发布模板，删模板查询链"
```

---

### Task 4: user-web 创作页直连提交 + 移除 ai-generate 页

**Files:**
- Delete: `project/user/web/src/views/console/GenerationQueueIndex.vue`
- Delete: `tests/e2e/verify_generation_queue.py`
- Modify: `project/user/web/src/router/index.js:57-61`
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue:1254`（+ L1176 import）
- Modify: `project/user/web/src/views/console/CreateIndex.vue`（L105、L144、L559、L570-589、L1153-1186、队列项点击）
- Modify: `project/user/web/src/api/generation.js`

**Interfaces:**
- Consumes: `POST /generation-tasks`（Task 3，无 templateId）；`submitGeneration(data)` 已存在于 `api/generation.js`。

- [ ] **Step 1: 删页面与路由与菜单**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && git rm project/user/web/src/views/console/GenerationQueueIndex.vue tests/e2e/verify_generation_queue.py
```

`router/index.js`：删除 `path: 'ai-generate'` 路由块（L57-61，含 name/component 共 5 行）。

`ConsoleLayout.vue`：删除 navItems 中 `{ path: '/console/ai-generate', label: 'AI 创作', icon: RobotOutlined },`（L1254）；从 `@ant-design/icons-vue` 的 import 列表中删除 `RobotOutlined`（L1176）。删后 `grep -n "RobotOutlined" project/user/web/src/views/console/ConsoleLayout.vue` 应无输出。

- [ ] **Step 2: CreateIndex —— 删模板加载死代码**

- L559 import 改为：`import { listGenerationTasks, submitGeneration } from '@/api/generation.js'`；
- 删除 L570-585：`// 阶段 3：可用的创作模板（默认 = 内置模板）` 注释、`availableTemplates` / `selectedTemplateId` 两个 ref、`loadAvailableTemplates` 函数；
- L589 `await Promise.all([loadSystemStyles(), loadAvailableTemplates()])` → `await loadSystemStyles()`。

- [ ] **Step 3: CreateIndex —— handleGenerate 改直接提交**

L1153-1186 整个 `handleGenerate` 替换为：

```js
const handleGenerate = async () => {
  if (!customTitle.value.trim()) {
    message.warning('请输入文章标题')
    return
  }
  if (!customRequirement.value.trim()) {
    message.warning('请补充你的核心观点和要求')
    return
  }

  const platformValue = typeof currentPlatform.value === 'object'
    ? (currentPlatform.value?.key || '')
    : (currentPlatform.value || '')
  const styleValue = typeof currentStyle.value === 'object'
    ? (currentStyle.value?.id || currentStyle.value?.name || '')
    : (currentStyle.value || '')
  const wordCountValue = typeof currentWordCount.value === 'object'
    ? (currentWordCount.value?.count || 800)
    : (Number(currentWordCount.value) || 800)

  try {
    await submitGeneration({
      title: customTitle.value,
      description: customRequirement.value,
      platform: platformValue,
      styleRef: styleValue,
      wordCount: wordCountValue
    })
    message.success('已加入生成队列')
    loadMiniQueue()
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  }
}
```

- [ ] **Step 4: CreateIndex —— 队列面板链接与完成项点击**

- L105：`@click="router.push('/console/ai-generate')"` → `@click="router.push('/console/works')"`；
- L144：文案 `去生成队列查看 →` → `去我的作品查看 →`，`router.push('/console/ai-generate')` → `router.push('/console/works')`；
- 队列项根 div（`v-for="item in miniQueueList.slice(0, 5)"`）加完成态点击：

```html
        <div
          v-for="item in miniQueueList.slice(0, 5)"
          :key="item.id"
          :class="['queue-panel-item', item.status]"
          :style="item.status === 'completed' ? 'cursor: pointer' : ''"
          @click="item.status === 'completed' && router.push('/console/works')"
        >
```

- [ ] **Step 5: api/generation.js 删 listPromptTemplates**

删除文件末尾的 `listPromptTemplates` 函数及注释。删后验证：`grep -rn "listPromptTemplates" project/user/web/src` → 无输出。

- [ ] **Step 6: 构建 + 残留检查**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && npm run build 2>&1 | tail -5
cd /Users/panyong/aio_project/ai_chuangzuo && grep -rn "ai-generate" project/user/web/src tests/e2e
```

Expected: 构建成功；grep 无输出。

- [ ] **Step 7: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && git add project/user/web tests/e2e && git commit -m "feat(user-web): 创作页直接提交生成，移除 AI 创作页"
```

---

### Task 5: 全仓验证

**Files:** 无改动，纯验证。

- [ ] **Step 1: grep 清零**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
grep -rn "ai-generate" project/user/web/src tests/e2e
grep -rn "listPromptTemplates\|PromptTemplateQuery\|PromptTemplatePublicVO" project/user --include="*.java" --include="*.js" --include="*.vue" | grep -v target | grep -v dist
grep -rn "selectEnabled\|findEnabled\|enableTemplate\|disableTemplate\|PROMPT_TEMPLATE_ENABLED_DUPLICATE\|PROMPT_TEMPLATE_NO_ENABLED" project --include="*.java" --include="*.js" --include="*.vue" | grep -v target | grep -v dist
grep -n "enabled" project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplate.java
```

Expected: 全部无输出（最后一条只查模板 entity；阶段 entity `PromptTemplateStage.enabled` 保留，不在此列）。

- [ ] **Step 2: 两端后端测试套件**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo && mysql -uroot -p123456 -h127.0.0.1 aichuangzuo -e "DELETE FROM hot_search_daily WHERE snapshot_date=CURDATE();"
cd project/admin/api && MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -q test 2>&1 | tail -3
cd ../user/api && MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -q test 2>&1 | tail -3
```

Expected: 两端 `Failures: 0, Errors: 0`。

- [ ] **Step 3: 两端前端构建**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && npm run build 2>&1 | tail -3
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/web && npm run build 2>&1 | tail -3
```

Expected: 都成功。

- [ ] **Step 4: 运行时冒烟（手动，可选）**

起 `scripts/local/user-full-stack/start.sh` 与 `scripts/local/admin-full-stack/start.sh`：
1. 管理端发布另一个模板 → `SELECT id, template_status FROM t_prompt_template WHERE template_status=1` 只剩新发布的；
2. 用户端创作页填标题点"生成文章" → 右侧"生成队列"面板出现任务并推进到完成；
3. 点完成条目 → 跳"我的作品"，新文章在列表中。

- [ ] **Step 5: 收尾**

按 superpowers:finishing-a-development-branch 流程：报告验证结果，询问推送/保持现状。

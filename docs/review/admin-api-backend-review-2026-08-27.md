# 管理端后端 Java 代码评审报告

**评审日期**：2026-08-27  
**评审范围**：`project/admin/api/` 管理端后端源码及配置  
**评审依据**：`CLAUDE.md`、`docs/architecture/security-conventions.md`、`docs/architecture/mysql-table-conventions.md`、`docs/architecture/java-code-style-conventions.md` 等文档。  
**说明**：本次评审针对当前工作区中未提交的 `main` 分支改动，未关联到 GitHub Pull Request。

---

## 一、高危 / 部署阻塞问题

### 1. 用户端表结构变更放在管理端 Flyway 迁移目录

**位置**：
- `project/admin/api/src/main/resources/db/migration/V2.0.0_098__add_self_media_question_other_option.sql:3-5`
- `project/user/api/src/main/resources/db/migration/V1.0.0_087__add_self_media_plan_ai_generated_tables.sql`（历史建表位置）

**问题**：
迁移脚本 `V2.0.0_098` 对 `u_self_media_plan_question` 表执行 `ALTER TABLE`，但 `u_` 前缀表属于用户端，其建表脚本位于 `project/user/api/src/main/resources/db/migration/`。当前用户端 Java 代码（`SelfMediaPlanQuestion.java`、`QuestionVO.java`、`SelfMediaPlanServiceImpl.java`）已同步新增 `allow_other` / `other_max_length` 字段，但对应的 DDL 却放在管理端迁移目录。

两端的 Flyway 分别使用独立的 schema history 表（`admin_flyway_schema_history` 与用户端默认表），因此：
- 若只部署用户端 API，或用户端 API 先于管理端启动，`u_self_media_plan_question` 表没有新列，导致 MyBatis-Plus 插入/查询报错。
- 破坏“用户端表由用户端迁移维护”的约定，造成跨模块耦合。

**规范依据**：`docs/architecture/mysql-table-conventions.md` 第 2.2 节、第 8.1 节；`CLAUDE.md` 中“数据库表按端拆分”的目录约定。

**建议修复**：
将该迁移脚本移动到 `project/user/api/src/main/resources/db/migration/`，并相应调整版本号（当前用户端最新为 `V1.0.0_116__repair_user_coupon_used_status.sql`，可新增 `V1.0.0_117__add_self_media_question_other_option.sql`）。管理端若需访问该表数据，应依赖用户端迁移已完成，而不是反向修改用户端表结构。

---

## 二、中危 / 功能正确性问题

### 2. `professional_qualification` 的 “其他” 填写内容未传入下游 prompt

**位置**：
- `project/admin/api/src/main/resources/db/migration/V2.0.0_097__update_self_media_plan_prompts_to_agent_framework.sql:35`（questions prompt 定义 `other_cert` 选项）
- 同文件第 122-123 行、第 189 行（niche/persona prompt 要求检查 other 填写内容）
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service/impl/SelfMediaPlanServiceImpl.java:275-289`（`buildQuestionsAnswersJson` 只传入选项 key/label）
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto/QuestionAnswerDTO.java:6-9`（DTO 无 other 文本字段）

**问题**：
`V097` 的 questions prompt 要求 `professional_qualification` 提供“其他（请填写）”选项，并设置 `otherMaxLength=100`。同文件的 niche/persona prompt 明确声明会依据 “other 填写内容” 判断是否可推荐需要资质的赛道/人设。然而当前 `QuestionAnswerDTO` 只有 `questionKey` 和 `answer`（选项 key/label），`buildQuestionsAnswersJson` 也只把选项 key/label 作为 `answer` 传给模型，**用户的自由填写内容丢失**。模型因此无法执行 prompt 中要求的资质校验，可能导致：
- 过度保守：所有选“其他”的用户都被认为无资质；
- 或过度宽松：无法识别用户填写的是法律/医疗/财经等需要资质的领域。

**建议修复**：
- 在 `QuestionAnswerDTO` 中新增 `otherText` 字段；
- 前端提交答案时，对于 `allowOther=true` 且用户选了“其他”的问题，携带填写内容；
- `buildQuestionsAnswersJson` 将 `otherText` 一并拼入 `nicheQuestionAnswersJson` / `personaQuestionAnswersJson`，例如增加 `otherText` 字段或在 `answer` 中同时包含 key 与填写内容。

---

### 3. 更新 prompt 后未使已缓存的旧问题失效

**位置**：
- `project/admin/api/src/main/resources/db/migration/V2.0.0_097__update_self_media_plan_prompts_to_agent_framework.sql`（整份 prompt 更新）
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service/impl/SelfMediaPlanServiceImpl.java:108-112`

**问题**：
用户端 `getOrGeneratePlatformQuestions` 以 `promptCode == PROMPT_PLATFORM_QUESTIONS` 作为缓存命中条件。`V097` 更新了同一 `prompt_code` 下的问题数量（由 4-6 个变为 8-9 个）、新增了 `professional_qualification` 核心问题以及 `allowOther` 字段，但缓存 key 未变。已生成过旧版问题的用户会命中缓存，继续拿到旧格式（缺少 `professional_qualification`、`allowOther` 等），导致前端展示与后端实体不一致，并影响后续 niche/persona 推荐。

**建议修复**：
- 方案 A：将本次 prompt 升级视为新版本，修改 `PROMPT_PLATFORM_QUESTIONS` 的 prompt_code（例如 `self_media_platform_questions_v3`），使旧缓存自然失效；
- 方案 B：在 `V097` 中追加一条 `DELETE FROM u_self_media_plan_question WHERE prompt_code = 'self_media_platform_questions_v2';` 以清理旧缓存（需评估数据影响，仅清除问题缓存，不影响已生成的 niche/persona）。

---

### 4. 新增布尔列未使用 `TINYINT UNSIGNED`

**位置**：
- `project/admin/api/src/main/resources/db/migration/V2.0.0_098__add_self_media_question_other_option.sql:4`

**问题**：
```sql
ADD COLUMN allow_other TINYINT NOT NULL DEFAULT 0 COMMENT '...'
```
同表原布尔列 `is_required`、`is_deleted` 均为 `TINYINT UNSIGNED`（见 `V1.0.0_087__add_self_media_plan_ai_generated_tables.sql`），而 `allow_other` 使用了 `TINYINT`，与表内既有约定和 `mysql-table-conventions.md` 第 3.4 节“布尔：`TINYINT UNSIGNED`”不一致。

**建议修复**：
```sql
ADD COLUMN allow_other TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否支持其他选项填写：0否 1是'
```

---

## 三、低危 / 规范与可维护性问题

### 5. 大数据迁移脚本缺少 `SET NAMES utf8mb4;`

**位置**：
- `project/admin/api/src/main/resources/db/migration/V2.0.0_097__update_self_media_plan_prompts_to_agent_framework.sql:1`

**问题**：
该脚本包含大量中文字符、Markdown、JSON 示例，但脚本开头未指定 `SET NAMES utf8mb4;`。`mysql-table-conventions.md` 第 8.3 节明确要求“脚本开头指定字符集：SET NAMES utf8mb4;”。在连接字符集配置不当的环境（如默认 latin1）中执行，可能导致中文 prompt 内容乱码或截断。

**建议修复**：
在脚本第一行添加：
```sql
SET NAMES utf8mb4;
```

---

### 6. `server.compression` 配置在 `application.yml` 与 `application-prod.yml` 重复

**位置**：
- `project/admin/api/src/main/resources/application.yml:6-9`
- `project/admin/api/src/main/resources/application-prod.yml:2-5`

**问题**：
两个配置文件中的 `server.compression` 块内容完全一致。`application-prod.yml` 继承 `application.yml`，若值相同则无需重复定义。重复配置会增加后续修改时遗漏的风险。

**建议修复**：
保留 `application.yml` 中的压缩配置，删除 `application-prod.yml` 中的同名配置，除非生产环境需要不同参数。

---

## 四、已确认无需重复处理的问题

以下问题已在 `docs/review/user-api-backend-review-2026-08-27.md` 中讨论，本次 admin 端改动已做对应修复或属于已知折中方案，不再作为新问题重复提出：

1. **JWT Secret 启动校验**：`JwtUtil.validateSecrets()` 已新增，校验 access/refresh secret 长度不少于 32 字节。✅
2. **长占位符默认值**：`application.yml` 中保留 `${ADMIN_JWT_ACCESS_SECRET:please-change-this-admin-access-secret-at-least-256-bits-long}` 等长默认值，是为避免旧部署未配环境变量时启动失败；生产环境仍需通过环境变量覆盖。⚠️ 已知折中。

---

## 五、修复优先级建议

| 优先级 | 问题 | 涉及文件 |
|---|---|---|
| P0 | 迁移脚本放错模块 | `V2.0.0_098__add_self_media_question_other_option.sql` |
| P1 | “其他”填写内容未传入模型 | `QuestionAnswerDTO.java`、`SelfMediaPlanServiceImpl.java`、prompt |
| P1 | prompt 更新未清缓存 | `V2.0.0_097...sql`、`SelfMediaPlanServiceImpl.java` |
| P2 | `allow_other` 列类型应为 `TINYINT UNSIGNED` | `V2.0.0_098__add_self_media_question_other_option.sql` |
| P2 | 迁移脚本缺少 `SET NAMES utf8mb4;` | `V2.0.0_097__update_self_media_plan_prompts_to_agent_framework.sql` |
| P3 | 压缩配置重复 | `application.yml`、`application-prod.yml` |

---

*评审结束*

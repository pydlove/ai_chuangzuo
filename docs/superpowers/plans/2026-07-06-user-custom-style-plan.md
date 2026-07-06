# 用户自定义风格功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成用户端「我的风格」中自定义风格的完整前后端实现，将 `myStyles` 从 `localStorage` 迁移到 MySQL。

**Architecture:** 后端在 `modules/style` 下新增 `u_user_style` 表的 CRUD；前端新增 `api/style.js`，改造 `useStyles.js` 和 `StylesIndex.vue` 走后端接口。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus 3.5.8 + Spring Security 6 + JUnit 5 + Vue 3 + Axios + Ant Design Vue。

## Global Constraints

- **DB 不动旧表**：通过 Flyway 新增 `u_user_style` 表，不修改现有表。
- **错误码**：新建 `modules/style/enums/StyleErrorCode.java`，不混用 `UserAuthErrorCode`。
- **鉴权**：新接口走 `/api/v1/user/styles`，由 JWT 过滤器写入 `SecurityUserContext`。
- **数据隔离**：所有查询必须带 `user_id`，禁止跨用户读取。
- **Javadoc**：新增 service/controller/DTO/mapper 方法补 Javadoc。
- **测试**：service 层写 `@SpringBootTest @Transactional` 集成测试；前端 `vite build` 通过。
- **清理**：开发过程中不用的 mock/临时代码及时删除。

---

## Task 1: 数据库迁移脚本

**Files:**
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_005__create_user_style_table.sql`

**Interfaces:**
- Produces: `u_user_style` 表结构，供后端 Entity/Mapper 使用。

- [ ] **Step 1: 编写 Flyway 迁移脚本**

```sql
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_user_style (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号，对外暴露',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    style_name VARCHAR(64) NOT NULL COMMENT '风格名称',
    prompt TEXT NOT NULL COMMENT '风格提示词，写入生成请求',
    scope VARCHAR(256) DEFAULT NULL COMMENT '适用范围标签，逗号分隔',
    source_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '来源类型：1-自定义，2-学习',
    use_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计使用次数',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_style_biz_no (biz_no),
    UNIQUE KEY uk_u_user_style_user_id_name (user_id, style_name),
    KEY idx_u_user_style_user_id_source (user_id, source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户风格表';
```

- [ ] **Step 2: 本地验证脚本可执行**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api flyway:info
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api flyway:migrate
```

Expected: 迁移成功，`u_user_style` 表创建。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/resources/db/migration/V1.0.0_005__create_user_style_table.sql
git commit -m "feat(style): 创建用户风格表 u_user_style"
```

---

## Task 2: 后端实体与 Mapper

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/entity/UserStyle.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/mapper/UserStyleMapper.java`
- Create: `project/user/api/src/main/resources/mapper/style/UserStyleMapper.xml`（如需要自定义 SQL）

**Interfaces:**
- Consumes: `u_user_style` 表结构。
- Produces: `UserStyle` 实体、`UserStyleMapper` 接口，供 Service 使用。

- [ ] **Step 1: 编写 UserStyle 实体**

```java
package com.aichuangzuo.user.modules.style.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("u_user_style")
public class UserStyle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizNo;
    private Long userId;
    private String styleName;
    private String prompt;
    private String scope;
    private Integer sourceType;
    private Integer useCount;
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 编写 UserStyleMapper**

```java
package com.aichuangzuo.user.modules.style.mapper;

import com.aichuangzuo.user.modules.style.entity.UserStyle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserStyleMapper extends BaseMapper<UserStyle> {
    @Select("SELECT * FROM u_user_style WHERE user_id = #{userId} AND source_type = #{sourceType} AND is_deleted = 0 ORDER BY updated_at DESC")
    List<UserStyle> selectByUserIdAndSourceType(@Param("userId") Long userId, @Param("sourceType") Integer sourceType);
}
```

- [ ] **Step 3: 编译验证**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api compile -DskipTests
```

Expected: 编译通过。

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/style/entity/UserStyle.java project/user/api/src/main/java/com/aichuangzuo/user/modules/style/mapper/UserStyleMapper.java
git commit -m "feat(style): 添加 UserStyle 实体与 Mapper"
```

---

## Task 3: 后端 DTO、VO 与错误码

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/dto/request/CreateStyleRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/dto/request/UpdateStyleRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/vo/UserStyleVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/enums/StyleErrorCode.java`

**Interfaces:**
- Consumes: 前端请求参数、业务校验结果。
- Produces: `CreateStyleRequest`、`UpdateStyleRequest`、`UserStyleVO`、`StyleErrorCode`。

- [ ] **Step 1: 编写 CreateStyleRequest**

```java
package com.aichuangzuo.user.modules.style.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStyleRequest {
    @NotBlank(message = "风格名称不能为空")
    @Size(min = 1, max = 20, message = "风格名称长度需为 1-20 字符")
    private String styleName;

    @NotBlank(message = "风格提示词不能为空")
    @Size(min = 1, max = 1000, message = "风格提示词长度需为 1-1000 字符")
    private String prompt;

    @Size(max = 256, message = "适用范围过长")
    private String scope;
}
```

- [ ] **Step 2: 编写 UpdateStyleRequest**

与 `CreateStyleRequest` 同结构，单独文件便于后续扩展。

- [ ] **Step 3: 编写 UserStyleVO**

```java
package com.aichuangzuo.user.modules.style.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserStyleVO {
    private String bizNo;
    private String styleName;
    private String prompt;
    private String scope;
    private Integer sourceType;
    private Integer useCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 编写 StyleErrorCode**

```java
package com.aichuangzuo.user.modules.style.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum StyleErrorCode implements ErrorCode {
    STYLE_NAME_EXISTS(112001, "风格名称已存在"),
    STYLE_NOT_FOUND(112002, "风格不存在或无权访问"),
    STYLE_NAME_EMPTY(112003, "风格名称不能为空"),
    STYLE_PROMPT_EMPTY(112004, "风格提示词不能为空"),
    STYLE_SCOPE_TOO_LONG(112005, "适用范围标签过多或过长");

    private final int code;
    private final String message;

    StyleErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/style/dto/ project/user/api/src/main/java/com/aichuangzuo/user/modules/style/vo/ project/user/api/src/main/java/com/aichuangzuo/user/modules/style/enums/
git commit -m "feat(style): 添加风格 DTO、VO、错误码"
```

---

## Task 4: 后端 Service 实现

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/UserStyleService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/impl/UserStyleServiceImpl.java`

**Interfaces:**
- Consumes: `UserStyleMapper`、`CreateStyleRequest`、`UpdateStyleRequest`。
- Produces: `UserStyleVO` 列表、CRUD 操作。

- [ ] **Step 1: 编写 UserStyleService 接口**

```java
package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.user.modules.style.dto.request.CreateStyleRequest;
import com.aichuangzuo.user.modules.style.dto.request.UpdateStyleRequest;
import com.aichuangzuo.user.modules.style.vo.UserStyleVO;

import java.util.List;

public interface UserStyleService {
    List<UserStyleVO> listMyStyles(Integer sourceType);
    UserStyleVO createStyle(CreateStyleRequest request);
    UserStyleVO updateStyle(String bizNo, UpdateStyleRequest request);
    void deleteStyle(String bizNo);
}
```

- [ ] **Step 2: 编写 UserStyleServiceImpl**

实现要点：
- 从 `SecurityUserContext` 取当前用户 ID。
- 创建时生成 `bizNo`（可用 `IdUtil.getSnowflakeNextIdStr` 或类似工具）。
- 校验名称唯一性（同一用户下）。
- 校验 `scope` 标签数量（最多 3 个）和单个长度（最多 8 字符）。
- 更新/删除时校验所有权。
- 使用 MapStruct 或手动转换 `UserStyle` → `UserStyleVO`。

- [ ] **Step 3: 运行 Service 集成测试（先写测试见 Task 6）**

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/
git commit -m "feat(style): 实现 UserStyleService CRUD 与校验"
```

---

## Task 5: 后端 Controller

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/controller/UserStyleController.java`

**Interfaces:**
- Consumes: `UserStyleService`。
- Produces: REST API `/api/v1/user/styles`。

- [ ] **Step 1: 编写 UserStyleController**

```java
package com.aichuangzuo.user.modules.style.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.style.dto.request.CreateStyleRequest;
import com.aichuangzuo.user.modules.style.dto.request.UpdateStyleRequest;
import com.aichuangzuo.user.modules.style.service.UserStyleService;
import com.aichuangzuo.user.modules.style.vo.UserStyleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户风格")
@RestController
@RequestMapping("/api/v1/user/styles")
@RequiredArgsConstructor
public class UserStyleController {
    private final UserStyleService userStyleService;

    @Operation(summary = "获取我的风格列表")
    @GetMapping
    public Result<List<UserStyleVO>> listMyStyles(
            @RequestParam(name = "sourceType", required = false, defaultValue = "1") Integer sourceType) {
        return Result.success(userStyleService.listMyStyles(sourceType));
    }

    @Operation(summary = "创建风格")
    @PostMapping
    public Result<UserStyleVO> createStyle(@Valid @RequestBody CreateStyleRequest request) {
        return Result.success(userStyleService.createStyle(request));
    }

    @Operation(summary = "修改风格")
    @PutMapping("/{bizNo}")
    public Result<UserStyleVO> updateStyle(
            @PathVariable String bizNo,
            @Valid @RequestBody UpdateStyleRequest request) {
        return Result.success(userStyleService.updateStyle(bizNo, request));
    }

    @Operation(summary = "删除风格")
    @DeleteMapping("/{bizNo}")
    public Result<Void> deleteStyle(@PathVariable String bizNo) {
        userStyleService.deleteStyle(bizNo);
        return Result.success();
    }
}
```

- [ ] **Step 2: 编译并启动验证**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api compile -DskipTests
```

Expected: 编译通过。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/style/controller/UserStyleController.java
git commit -m "feat(style): 添加 UserStyleController 风格 CRUD 接口"
```

---

## Task 6: 后端 Service 测试

**Files:**
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/style/service/UserStyleServiceTest.java`

**Interfaces:**
- Consumes: `UserStyleService`、`UserStyleMapper`。
- Produces: 6 个测试用例全部通过。

- [ ] **Step 1: 编写测试用例**

覆盖：
1. 创建自定义风格成功
2. 同用户下风格名重复失败
3. 查询列表只返回当前用户数据
4. 修改风格成功
5. 修改不存在的风格失败
6. 删除风格成功（逻辑删除）
7. 越权修改/删除其他用户风格失败

- [ ] **Step 2: 运行测试**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api test -Dtest=UserStyleServiceTest
```

Expected: 全部通过。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/test/java/com/aichuangzuo/user/modules/style/service/UserStyleServiceTest.java
git commit -m "test(style): 添加 UserStyleService 集成测试"
```

---

## Task 7: 前端 API 层

**Files:**
- Create: `project/user/web/src/api/style.js`

**Interfaces:**
- Consumes: `/api/v1/user/styles` 接口。
- Produces: `getMyStyles`、`createStyle`、`updateStyle`、`deleteStyle` 函数。

- [ ] **Step 1: 编写 api/style.js**

```javascript
import { api } from '@/api/auth'

export function getMyStyles(sourceType = 1) {
  return api.get('/styles', { params: { sourceType } })
}

export function createStyle(data) {
  return api.post('/styles', data)
}

export function updateStyle(bizNo, data) {
  return api.put(`/styles/${bizNo}`, data)
}

export function deleteStyle(bizNo) {
  return api.delete(`/styles/${bizNo}`)
}
```

- [ ] **Step 2: Commit**

```bash
git add project/user/web/src/api/style.js
git commit -m "feat(style): 添加前端风格 API 封装"
```

---

## Task 8: 前端 useStyles 改造

**Files:**
- Modify: `project/user/web/src/composables/useStyles.js`

**Interfaces:**
- Consumes: `getMyStyles`、`createStyle`、`updateStyle`、`deleteStyle`。
- Produces: `myStyles` 从后端加载，`addCustomStyle` / `updateCustomStyle` / `removeCustomStyle` 调 API。

- [ ] **Step 1: 移除 myStyles 的 localStorage 持久化**

将 `myStyles` 初始化为 `ref([])`，移除相关 localStorage 读写。

- [ ] **Step 2: 新增 loadMyStyles**

```javascript
import { getMyStyles, createStyle, updateStyle, deleteStyle } from '@/api/style'

export async function loadMyStyles() {
  const res = await getMyStyles()
  myStyles.value = res.data || []
}
```

- [ ] **Step 3: 改造 addCustomStyle / updateCustomStyle / removeCustomStyle**

改为异步调用 API，成功后刷新列表。

```javascript
export async function addCustomStyle(style) {
  await createStyle({
    styleName: style.name.trim(),
    prompt: style.prompt.trim(),
    scope: (style.scope || '').trim()
  })
  await loadMyStyles()
}

export async function updateCustomStyle(oldName, style) {
  const target = myStyles.value.find(s => s.name === oldName)
  if (!target) return
  await updateStyle(target.bizNo, {
    styleName: style.name.trim(),
    prompt: style.prompt.trim(),
    scope: (style.scope || '').trim()
  })
  await loadMyStyles()
}

export async function removeCustomStyle(name) {
  const target = myStyles.value.find(s => s.name === name)
  if (!target) return
  await deleteStyle(target.bizNo)
  await loadMyStyles()
}
```

- [ ] **Step 4: 保持 currentStyle、systemStyles、learnedStyles 不变**

`currentStyle` 仍兼容系统预设和用户风格；`learnedStyles` 仍走 localStorage。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/composables/useStyles.js
git commit -m "feat(style): myStyles 从 localStorage 切换到后端 API"
```

---

## Task 9: 前端 StylesIndex 集成

**Files:**
- Modify: `project/user/web/src/views/console/StylesIndex.vue`

**Interfaces:**
- Consumes: `loadMyStyles`。
- Produces: 页面加载时拉取列表，操作后刷新。

- [ ] **Step 1: 页面加载时调用 loadMyStyles**

```javascript
import { loadMyStyles } from '@/composables/useStyles.js'

onMounted(() => {
  loadMyStyles()
})
```

- [ ] **Step 2: 改造 saveStyle / deleteStyle / saveLearnedResult**

`saveStyle` 改为 `await addCustomStyle(...)` 或 `await updateCustomStyle(...)`，成功后 `editorMode = false`。
`deleteStyle` 改为 `await removeCustomStyle(...)`。

- [ ] **Step 3: 处理 myStyles 数据结构变化**

后端返回字段为 `styleName`、`bizNo`、`scope`、`prompt`、`useCount` 等。前端 `v-for` 中 `s.name` 需要改为 `s.styleName`，并确保 `useStyle` 等方法兼容。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/StylesIndex.vue
git commit -m "feat(style): StylesIndex 对接后端风格接口"
```

---

## Task 10: 联调与验证

- [ ] **Step 1: 后端全量测试通过**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api test
```

Expected: 全部通过。

- [ ] **Step 2: 前端构建通过**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
./node_modules/.bin/vite build
```

Expected: `✓ built`。

- [ ] **Step 3: 手动 E2E 验证**

- 启动全栈：`/Users/panyong/aio_project/ai_chuangzuo/scripts/local/user-full-stack/start.sh`
- 登录后进入「我的风格」
- 新建一个风格 → 列表刷新
- 编辑风格 → 保存成功
- 删除风格 → 列表移除
- 刷新页面 → 数据仍在

- [ ] **Step 4: Commit 任何修复**

---

## Task 11: 清理与收尾

- [ ] **Step 1: 检查无用代码**

确认 `useStyles.js` 中没有遗留的 `myStyles` localStorage 读写代码。

- [ ] **Step 2: 运行 lint/构建**

重复 Task 10 的测试和构建命令。

- [ ] **Step 3: 提交最终版本**

```bash
git status
git add ...
git commit -m "feat(style): 完成用户自定义风格前后端完整功能"
```

- [ ] **Step 4: 使用 finishing-a-development-branch skill 收尾**

开发完成后调用 `superpowers:finishing-a-development-branch` 进行分支收尾。

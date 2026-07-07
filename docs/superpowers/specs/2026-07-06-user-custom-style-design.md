# 用户自定义风格功能设计文档

> 日期：2026-07-06  
> 范围：用户端「我的风格」中自定义风格的完整前后端实现（学习的风格、风格市场、收藏等后续迭代）。

---

## 1. 目标

让登录用户可以在控制台创建、编辑、删除、查看自己的写作风格，数据持久化到 MySQL，替代当前 `useStyles.js` 中 `myStyles` 的 `localStorage` 存储。

## 2. 非目标

- 本期不实现「学习的风格」后端 AI 分析（保留前端 mock）。
- 本期不实现风格市场、发布、审核、收藏、收益结算。
- 系统预设风格保持前端硬编码。

## 3. 数据库设计

### 3.1 表：`u_user_style`

复用同一张表承载后续「自定义」和「学习」两种来源，通过 `source_type` 区分。

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

### 3.2 设计说明

- `biz_no` 用于前后端交互，避免暴露自增 ID。
- `user_id + style_name` 唯一，确保同一用户下风格名不重复。
- `source_type` 默认 1（自定义），为后续学习风格接入预留。
- 逻辑删除，保留 `use_count` 等业务数据。

## 4. 后端设计

### 4.1 模块位置

按 `docs/architecture/java-package-conventions.md`，放在 `com.aichuangzuo.user.modules.style` 下。

### 4.2 错误码

新建 `modules/style/enums/StyleErrorCode.java`，实现 `ErrorCode` 接口：

| 错误码 | 说明 |
|---|---|
| 112001 | 风格名称已存在 |
| 112002 | 风格不存在或无权访问 |
| 112003 | 风格名称不能为空 |
| 112004 | 风格提示词不能为空 |

### 4.3 接口

基路径：`/api/v1/user/styles`

#### 4.3.1 获取当前用户风格列表

```
GET /api/v1/user/styles?sourceType=1
```

- `sourceType` 可选，默认 1（自定义）。
- 返回当前登录用户的未删除风格列表，按 `updated_at` 倒序。

响应：

```json
{
  "code": 0,
  "data": [
    {
      "bizNo": "S592...",
      "styleName": "我的小红书风",
      "prompt": "...",
      "scope": "小红书,种草,短文案",
      "sourceType": 1,
      "useCount": 0,
      "createdAt": "2026-07-06T10:00:00.000",
      "updatedAt": "2026-07-06T10:00:00.000"
    }
  ]
}
```

#### 4.3.2 创建风格

```
POST /api/v1/user/styles
```

请求体：

```json
{
  "styleName": "我的小红书风",
  "prompt": "你是一位擅长小红书种草的写手...",
  "scope": "小红书,种草"
}
```

校验：
- `styleName` 1-20 字符，当前用户唯一
- `prompt` 1-1000 字符
- `scope` 最多 3 个标签，每个标签 1-8 字符

#### 4.3.3 修改风格

```
PUT /api/v1/user/styles/{bizNo}
```

请求体同创建，但只能修改当前用户自己的风格。

#### 4.3.4 删除风格

```
DELETE /api/v1/user/styles/{bizNo}
```

逻辑删除，仅允许删除当前用户自己的风格。

### 4.4 主要类

| 类 | 职责 |
|---|---|
| `UserStyle` | MyBatis-Plus 实体 |
| `UserStyleMapper` | 数据访问 |
| `UserStyleService` / `UserStyleServiceImpl` | 业务逻辑、校验、CRUD |
| `UserStyleController` | REST 接口 |
| `CreateStyleRequest` / `UpdateStyleRequest` | 请求 DTO（Jakarta Validation） |
| `UserStyleVO` | 响应视图对象 |
| `StyleErrorCode` | 模块错误码 |

## 5. 前端设计

### 5.1 新增 API 层

`project/user/web/src/api/style.js`：

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

### 5.2 改造 `useStyles.js`

- `systemStyles`：保持硬编码系统预设。
- `myStyles`：从 `ref([])` 改为异步加载，移除 localStorage 读写。
- `addCustomStyle` / `updateCustomStyle` / `removeCustomStyle`：改为调 API 并刷新列表。
- `currentStyle`：继续作为当前选中风格，兼容系统预设和用户风格。
- `learnedStyles`：本期不动，仍走 localStorage mock。

### 5.3 改造 `StylesIndex.vue`

- 页面 `onMounted` 调 `loadMyStyles()`。
- 新建/编辑/删除成功后重新加载列表并给出 `message.success`。
- 保持现有 UI 和交互不变。

## 6. 测试

### 6.1 后端测试

- `UserStyleServiceTest`：覆盖创建成功、名称重复、越权修改、删除、列表查询。
- 使用 `@SpringBootTest @Transactional` 集成测试。

### 6.2 前端测试

- `vite build` 通过。
- 手动验证：新建风格、编辑、删除、列表刷新。

## 7. 后续扩展

- 学习的风格：复用 `u_user_style` 表，`source_type=2`，新增 AI 分析接口。
- 风格市场：新增 `u_user_style_market` 表，关联 `u_user_style`，走审核流程。
- 收藏：新增 `u_user_style_favorite` 关联表。

## 8. 文件清单

**后端**
- `project/user/api/src/main/resources/db/migration/V1.0.0_005__create_user_style_table.sql`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/entity/UserStyle.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/mapper/UserStyleMapper.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/UserStyleService.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/impl/UserStyleServiceImpl.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/dto/request/CreateStyleRequest.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/dto/request/UpdateStyleRequest.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/vo/UserStyleVO.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/controller/UserStyleController.java`
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/enums/StyleErrorCode.java`
- `project/user/api/src/test/java/com/aichuangzuo/user/modules/style/service/UserStyleServiceTest.java`

**前端**
- `project/user/web/src/api/style.js`
- `project/user/web/src/composables/useStyles.js`
- `project/user/web/src/views/console/StylesIndex.vue`

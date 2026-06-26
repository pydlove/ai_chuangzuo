# MySQL 表结构生成规范

> 本文档定义爱创作（AI Creation）项目 MySQL 8.x 数据库的建表规范，适用于用户端（C 端）与管理端（B 端/管理后台）全部业务表。所有表结构变更通过 Flyway 脚本执行。

---

## 1. 基础约束

| 项 | 规范 |
|---|---|
| 数据库版本 | MySQL 8.x |
| 默认存储引擎 | InnoDB |
| 默认字符集 | `utf8mb4` |
| 默认排序规则 | `utf8mb4_unicode_ci` |
| 时区 | 应用层统一使用 UTC+8（`Asia/Shanghai`），数据库字段类型使用 `DATETIME(3)`，不依赖数据库 `TIMESTAMP` 自动转换 |
| 迁移工具 | Flyway，所有 DDL/DML 变更必须写成版本化脚本 |
| 命名语言 | 全小写英文，单词间用下划线 `_` 分隔，禁止拼音、缩写（约定俗成的 id/ok/url 除外） |
| 行格式 | `DYNAMIC`（MySQL 8 默认），无需显式指定 |

---

## 2. 命名规范

### 2.1 数据库/Schema

- 单一数据库部署：`aichuangzuo`
- 若后续拆分：
  - 用户端业务：`aichuangzuo_user`
  - 管理端业务：`aichuangzuo_admin`
  - 公共/配置：`aichuangzuo_common`

### 2.2 表名

格式：`<端标识>_<业务模块>_<实体名>`

| 端 | 前缀 | 示例 |
|---|---|---|
| 用户端 | `u_` | `u_user`, `u_generation_task`, `u_article` |
| 管理端 | `a_` | `a_admin_user`, `a_role`, `a_system_config` |
| 公共/配置 | `c_` | `c_region`, `c_dict` |

说明：
- 端前缀用于在单一数据库中快速区分表归属，避免跨端误操作。
- 表名单数形式，如 `u_user` 而非 `u_users`。
- 关联表使用 `u_<实体A>_<实体B>_rel` 或 `u_<实体>_<关系>`，如 `u_user_role_rel`。

### 2.3 字段名

- 全小写，下划线分隔，如 `created_at`。
- 主键固定为 `id`，类型 `BIGINT UNSIGNED AUTO_INCREMENT`。
- 外键字段名：`{关联表实体}_id`，如 `user_id`、`article_id`。
- 布尔字段：`is_{形容词}`，如 `is_deleted`、`is_published`、`is_active`。
- 时间字段：`{动作}_at`，如 `created_at`、`updated_at`、`deleted_at`、`published_at`。
- 操作人字段：`{动作}_by`，如 `created_by`、`updated_by`。
- 枚举/状态字段：`{业务}_status`，如 `task_status`、`audit_status`；或 `{业务}_type`，如 `article_type`。
- 金额字段：使用 `DECIMAL(19,4)`，字段名以 `_amount` 结尾，如 `pay_amount`。
- JSON 字段：以 `_json` 或 `_extra` 结尾，如 `template_config_json`、`ext_json`。

### 2.4 索引名

| 索引类型 | 命名规则 | 示例 |
|---|---|---|
| 主键 | `pk_表名` | `pk_u_user` |
| 唯一索引 | `uk_表名_字段` | `uk_u_user_phone` |
| 普通索引 | `idx_表名_字段` | `idx_u_user_status` |
| 组合索引 | `idx_表名_字段1_字段2` | `idx_u_article_user_id_status` |
| 全文索引 | `ft_表名_字段` | `ft_u_article_content` |

说明：表名在索引名中可省略前缀以控制长度，但需保证可读性。

---

## 3. 字段设计通用规范

### 3.1 主键

```sql
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
PRIMARY KEY (id)
```

- 所有表必须包含自增主键 `id`。
- 禁止使用业务字段作为主键，禁止 UUID 作为主键（如需对外暴露唯一标识，使用单独的 `biz_no` 或 `uuid` 字段）。

### 3.2 审计字段（所有表通用）

```sql
created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID，0表示系统或未知',
updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID，0表示系统或未知',
deleted_at DATETIME(3) DEFAULT NULL COMMENT '删除时间，NULL表示未删除',
is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
```

- `created_by` / `updated_by` 存储用户 ID 或管理员 ID，根据表归属区分语义。
- 逻辑删除统一使用 `is_deleted` + `deleted_at`，物理删除仅允许在数据合规清理脚本中执行。

### 3.3 乐观锁字段

```sql
version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'
```

- 并发更新频繁的核心表必须添加，如订单、任务、额度、库存类表。
- MyBatis-Plus 默认使用 `version` 字段作为乐观锁。

### 3.4 数据类型选择

| 场景 | 推荐类型 | 说明 |
|---|---|---|
| 主键/外键/人ID | `BIGINT UNSIGNED` | 预留足够增长空间 |
| 自增计数 | `BIGINT UNSIGNED` | 避免溢出 |
| 状态/类型枚举 | `TINYINT UNSIGNED` | 0-255 足够覆盖大多数业务枚举 |
| 布尔 | `TINYINT UNSIGNED` | 0/1，禁止使用 `BOOLEAN` 别名 |
| 小整数计数 | `INT UNSIGNED` | 如阅读数、点赞数 |
| 大整数计数 | `BIGINT UNSIGNED` | 如文章字数、文件字节数 |
| 金额 | `DECIMAL(19,4)` | 4 位小数满足大多数场景 |
| 字符串（短） | `VARCHAR(32/64/128/256)` | 按实际长度分档，禁止无限 `VARCHAR(5000)` |
| 字符串（中） | `VARCHAR(1024/2048)` | 如 URL、摘要 |
| 长文本 | `TEXT` / `LONGTEXT` | 文章内容、日志、JSON |
| JSON 结构化数据 | `JSON` | MySQL 8 支持 JSON 类型，优先使用 |
| 时间 | `DATETIME(3)` | 毫秒精度，避免 `TIMESTAMP` 的 2038 年问题 |
| IP 地址 | `VARCHAR(45)` | 兼容 IPv6 |
| 手机号 | `VARCHAR(20)` | 国际号码可能超过 11 位 |
| 邮箱 | `VARCHAR(128)` | |
| 枚举集合 | `SET` 或 `VARCHAR` | 优先在应用层用 `TINYINT` 位运算或关联表表达，避免 `SET` 类型 |

### 3.5 NULL 与默认值

- 核心业务字段必须有 `NOT NULL` 约束，并设置合理默认值。
- 文本/JSON 等可空字段使用 `DEFAULT NULL`。
- 禁止对字符串字段使用 `DEFAULT ''` 作为无值语义，应明确使用 `NULL` 或业务默认值。
- 状态字段必须设置默认值，且默认值对应“初始/正常”状态。

### 3.6 注释

- 每个表必须有 `COMMENT`。
- 每个字段必须有 `COMMENT`，说明字段含义、取值范围、特殊值。
- 枚举字段注释必须列出全部枚举值，例如：
  ```sql
  task_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '任务状态：0-排队中，1-生成中，2-已完成，3-失败，4-已取消',
  ```

---

## 4. 用户端（C 端）表特殊规范

用户端表面向终端创作者，核心关注高并发写入、数据隔离、隐私合规。

### 4.1 强制字段

除 3.2 通用审计字段外，用户端业务表必须包含：

```sql
user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示默认租户',
```

- 所有用户数据查询必须带 `user_id` 过滤，禁止跨用户读取。
- `tenant_id` 为未来多租户扩展预留，单租户阶段固定为 0。

### 4.2 业务编号字段

```sql
biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号，对外暴露',
```

- 用户端核心实体建议生成业务编号，如订单号、任务号、文章编号，避免直接暴露自增 ID。
- 业务编号加唯一索引。

### 4.3 隐私与敏感字段

- 手机号、邮箱、真实姓名等敏感信息单独存储，查询权限由管理端接口控制。
- 用户密码、Token、API Key 等必须加密存储，禁止明文落库。
- 日志表中记录用户操作时，敏感参数需脱敏。

### 4.4 分库分表预留

- 用户端大表（如文章表、生成任务表、操作日志表）在设计时预留分片键：
  - 默认按 `user_id` 分片。
  - 分片键必须参与主查询条件或唯一索引前缀。

### 4.5 额度与计费字段

```sql
credit_used INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次消耗额度',
credit_refunded INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已退回额度',
```

- 涉及额度扣减/退回的表必须记录明细，便于对账。

---

## 5. 管理端（B 端/后台）表特殊规范

管理端表面向内部运营与系统配置，核心关注权限控制、审计追踪、配置化管理。

### 5.1 强制字段

除 3.2 通用审计字段外，管理端业务表视情况包含：

```sql
admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '关联管理员ID',
role_id BIGINT UNSIGNED NOT NULL COMMENT '关联角色ID',
```

- 管理操作必须记录操作人 `created_by` / `updated_by`。

### 5.2 权限相关表

```sql
-- 管理员表
CREATE TABLE a_admin_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(256) NOT NULL COMMENT '密码哈希',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    last_login_at DATETIME(3) DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_admin_user_username (username)
) COMMENT='管理员账号表';

-- 角色表
CREATE TABLE a_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(256) DEFAULT NULL COMMENT '描述',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_role_code (role_code)
) COMMENT='角色表';

-- 管理员-角色关联表
CREATE TABLE a_admin_user_role_rel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_admin_user_role_rel (admin_user_id, role_id)
) COMMENT='管理员角色关联表';

-- 权限表
CREATE TABLE a_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    resource_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '资源类型：1-菜单，2-按钮，3-接口',
    parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限ID，0表示顶级',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_permission_code (permission_code)
) COMMENT='权限表';

-- 角色-权限关联表
CREATE TABLE a_role_permission_rel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_role_permission_rel (role_id, permission_id)
) COMMENT='角色权限关联表';
```

### 5.3 系统配置表

```sql
CREATE TABLE a_system_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_group VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '配置分组',
    description VARCHAR(256) DEFAULT NULL COMMENT '配置说明',
    is_sensitive TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否敏感：0-否，1-是',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_system_config_key_group (config_key, config_group)
) COMMENT='系统配置表';
```

### 5.4 操作日志表

```sql
CREATE TABLE a_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    admin_user_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作管理员ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(256) NOT NULL COMMENT '操作描述',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(2048) DEFAULT NULL COMMENT '请求URL',
    request_params TEXT DEFAULT NULL COMMENT '请求参数（敏感信息脱敏）',
    response_result TEXT DEFAULT NULL COMMENT '响应结果',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    cost_time_ms INT UNSIGNED DEFAULT NULL COMMENT '耗时毫秒',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_a_operation_log_admin_user_id (admin_user_id),
    KEY idx_a_operation_log_created_at (created_at)
) COMMENT='管理端操作日志表';
```

---

## 6. 用户端与管理端对比表

| 维度 | 用户端（C 端） | 管理端（B 端） |
|---|---|---|
| 表名前缀 | `u_` | `a_` |
| 核心数据归属 | `user_id` | `admin_user_id` / `role_id` |
| 数据隔离 | 按用户隔离，禁止跨用户读取 | 按角色权限隔离 |
| 租户字段 | `tenant_id` 预留 | 通常不需要 |
| 业务编号 | 核心实体使用 `biz_no` | 视需要 |
| 敏感信息 | 加密存储，接口脱敏 | 严格权限控制 |
| 并发关注 | 高并发写入、额度扣减 | 配置变更、审计追踪 |
| 分片键 | `user_id` | 通常不分片 |
| 日志重点 | 用户行为、生成任务 | 管理操作、权限变更 |

---

## 7. 索引设计规范

### 7.1 通用原则

- 每个表必须有主键索引。
- 查询条件中的字段、排序字段、关联字段必须建立索引。
- 唯一约束业务语义上必须唯一的地方使用唯一索引。
- 组合索引遵循最左前缀原则，将区分度高的字段放在前面。
- 单表索引数量建议不超过 5 个，避免写入性能下降。
- 索引字段尽量不使用函数或类型转换。

### 7.2 禁止行为

- 禁止对 `TEXT` / `LONGTEXT` / `BLOB` 类型字段直接创建普通索引（可建前缀索引或全文索引）。
- 禁止创建冗余索引（如已有 `(a,b)`，则 `(a)` 冗余）。
- 禁止在频繁更新的字段上创建过多索引。

### 7.3 示例

```sql
-- 用户文章表
CREATE TABLE u_article (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '文章编号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    title VARCHAR(256) NOT NULL COMMENT '文章标题',
    content LONGTEXT COMMENT '文章内容',
    word_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '字数',
    article_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-草稿，1-已发布',
    template_code VARCHAR(64) DEFAULT NULL COMMENT '使用模板编码',
    style_code VARCHAR(64) DEFAULT NULL COMMENT '使用风格编码',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_article_biz_no (biz_no),
    KEY idx_u_article_user_id_status (user_id, article_status),
    KEY idx_u_article_created_at (created_at)
) COMMENT='用户文章表';
```

---

## 8. Flyway 迁移脚本规范

### 8.1 脚本位置

```
project/user/api/src/main/resources/db/migration/
project/admin/api/src/main/resources/db/migration/
```

### 8.2 脚本命名

- 版本号：`V{版本号}__{描述}.sql`
- 版本号格式：`主版本.次版本.补丁号_序号`，例如 `V1.0.0_001__create_user_table.sql`
- 回滚脚本：`U{版本号}__{描述}.sql`（可选，视团队要求）

### 8.3 脚本内容

- 每个脚本只做一件事：创建一张表、修改一张表、或一批关联变更。
- 脚本开头指定字符集：
  ```sql
  SET NAMES utf8mb4;
  ```
- 新建表脚本必须包含 `IF NOT EXISTS`。
- 修改表脚本必须包含 `IF EXISTS` 判断（ Flyway 不支持原生 `IF EXISTS` 时，通过存储过程或拆分脚本处理）。
- 禁止直接修改已发布表中的字段含义或删除已有字段；如需变更，使用新增字段 + 数据迁移 + 废弃旧字段的方式。
- 每个脚本末尾可加入权限/初始化数据插入。

### 8.4 示例

```sql
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '用户唯一编号',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    password_hash VARCHAR(256) DEFAULT NULL COMMENT '密码哈希',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    user_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_biz_no (biz_no),
    UNIQUE KEY uk_u_user_phone (phone),
    KEY idx_u_user_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

---

## 9. 本项目示例表清单

基于现有原型功能，建议优先建立以下核心表：

### 9.1 用户端

| 表名 | 说明 |
|---|---|
| `u_user` | 用户基础信息 |
| `u_user_profile` | 用户扩展资料 |
| `u_generation_task` | 文章生成任务 |
| `u_article` | 生成完成的文章 |
| `u_article_version` | 文章版本历史 |
| `u_template_usage` | 用户模板使用记录 |
| `u_style_preset` | 用户自定义风格 |
| `u_credit_account` | 用户额度账户 |
| `u_credit_transaction` | 额度流水 |
| `u_order` | 充值/购买订单 |
| `u_payment` | 支付记录 |
| `u_user_login_log` | 用户登录日志 |
| `u_user_operation_log` | 用户操作日志 |

### 9.2 管理端

| 表名 | 说明 |
|---|---|
| `a_admin_user` | 管理员账号 |
| `a_role` | 角色 |
| `a_permission` | 权限 |
| `a_admin_user_role_rel` | 管理员角色关联 |
| `a_role_permission_rel` | 角色权限关联 |
| `a_system_config` | 系统配置 |
| `a_operation_log` | 管理操作日志 |
| `a_global_template` | 全局文章模板 |
| `a_global_style` | 全局写作风格 |
| `a_model_config` | AI 模型配置 |
| `a_price_package` | 定价套餐 |

---

## 10. 禁止事项

- 禁止在业务表中存储图片/BLOB 二进制数据，文件路径存于数据库，文件内容存于本地磁盘或后续 OSS。
- 禁止在数据库中做复杂计算或业务逻辑（应放在应用层）。
- 禁止使用触发器、存储过程、事件调度器实现业务逻辑。
- 禁止在大表上无索引查询。
- 禁止修改已发布字段的数据类型导致数据截断。
- 禁止将管理端与用户端数据混放在同一张表中（除非是纯配置字典表）。

---

## 11. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：区分用户端与管理端，定义字段、索引、Flyway 规范 | - |


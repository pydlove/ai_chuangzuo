# 管理端系统设置 - 模型配置设计

## 背景

管理控制台需要增加「系统设置」一级菜单，其下新增「模型配置」二级菜单，用于管理 AI 大模型厂商接入参数。当前项目需要支持 Kimi 与 MiniMax 两个厂商，未来可扩展更多厂商。

## 目标

- 在管理端提供模型配置的新增、编辑、删除、查看能力。
- 支持从厂商接口拉取可用模型列表。
- 支持测试连接，验证 baseUrl + apiKey 可用性。
- 全局仅启用一个配置，启用后系统使用该配置调用模型。
- apiKey 入库加密存储，接口不返回明文。

## 非目标

- 不在界面上单独选择 API 格式，格式由厂商类型派生。
- 本次不实现用户端真实调用模型（仅完成配置管理，为后续调用做准备）。
- 不实现复杂的厂商扩展插件机制。

## 方案选型

采用 **按厂商管理单条配置** 方案：

- 每个厂商（`kimi`、`minimax`）在表中最多一条配置。
- `provider_type` 同时作为唯一键和厂商类型标识。
- 优点：语义清晰、默认配置逻辑简单、接口路径直观。

## 数据库设计

### 表 `a_model_config`

```sql
CREATE TABLE IF NOT EXISTS a_model_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    provider_type VARCHAR(64) NOT NULL COMMENT '厂商类型：kimi / minimax',
    base_url VARCHAR(512) NOT NULL COMMENT 'API 基础地址',
    api_key_encrypted VARCHAR(512) NOT NULL COMMENT '加密后的 API Key',
    model_code VARCHAR(128) NOT NULL COMMENT '模型编码',
    model_name VARCHAR(128) DEFAULT NULL COMMENT '模型显示名',
    is_active TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否启用：0-否，1-是（全局唯一）',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_model_config_provider_type (provider_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型配置表';
```

### 关键约束

- `provider_type` 唯一，保证一厂商一条配置。
- `is_active = 1` 全局最多一条；激活某配置时，其他配置自动置为 0。
- `api_key_encrypted` 使用 AES 对称加密，密钥由环境变量 `ADMIN_MODEL_API_KEY_SECRET` 提供。

## API 接口设计

### 1. 查询模型配置列表

```
GET /api/v1/admin/model-configs
```

**响应数据**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "providerType": "kimi",
      "providerName": "Kimi",
      "baseUrl": "https://api.moonshot.cn",
      "modelCode": "moonshot-v1-8k",
      "modelName": "Moonshot V1 8K",
      "isActive": 1
    },
    {
      "id": 2,
      "providerType": "minimax",
      "providerName": "MiniMax",
      "baseUrl": "",
      "modelCode": "",
      "isActive": 0
    }
  ]
}
```

列表返回所有支持的厂商（包含未配置的占位项），便于前端展示。

### 2. 查看模型配置详情

```
GET /api/v1/admin/model-configs/{providerType}
```

**响应数据**：与列表项一致，`apiKey` 字段不返回（或返回脱敏/空）。

### 3. 保存/更新模型配置

```
PUT /api/v1/admin/model-configs/{providerType}
```

**请求体**

```json
{
  "baseUrl": "https://api.moonshot.cn",
  "apiKey": "sk-xxxxxxxx",
  "modelCode": "moonshot-v1-8k",
  "modelName": "Moonshot V1 8K",
  "isActive": 0
}
```

- 若该厂商配置不存在则创建，存在则更新（包括已逻辑删除的记录，保存时恢复）。
- `apiKey` 入库前加密。
- `isActive` 为 1 时，自动将其他配置置为 0。

### 4. 删除模型配置

```
DELETE /api/v1/admin/model-configs/{providerType}
```

逻辑删除，置 `is_deleted = 1`。

### 5. 拉取模型列表

```
POST /api/v1/admin/model-configs/{providerType}/actions/fetch-models
```

**请求体**

```json
{
  "baseUrl": "https://api.moonshot.cn",
  "apiKey": "sk-xxxxxxxx"
}
```

**响应数据**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "modelCode": "moonshot-v1-8k", "modelName": "Moonshot V1 8K" },
    { "modelCode": "moonshot-v1-32k", "modelName": "Moonshot V1 32K" }
  ]
}
```

- Kimi：调用 `/v1/models`。
- MiniMax：若厂商不支持模型列表接口，返回内置常用模型列表兜底。

### 6. 测试连接

```
POST /api/v1/admin/model-configs/{providerType}/actions/test-connection
```

**请求体**：同 fetch-models。

**响应数据**

```json
{
  "code": 0,
  "message": "success",
  "data": { "success": true }
}
```

- Kimi：调用 `/v1/models` 验证 key 有效。
- MiniMax：调用最小请求验证（如 models 接口或简短 chat completion）。

### 7. 启用/停用配置

```
POST /api/v1/admin/model-configs/{providerType}/actions/toggle-active
```

**请求体**

```json
{
  "isActive": 1
}
```

- 启用时，其他配置自动停用。
- 停用时，允许全局暂时无启用配置。

## API 格式说明

界面不选择 API 格式，由 `provider_type` 决定：

| 厂商类型 | API 格式 |
|---------|---------|
| `kimi` | OpenAI 兼容格式（`/v1/chat/completions`） |
| `minimax` | MiniMax 原生格式（`/v1/text/chatcompletion_v2`） |

后续如需支持 OpenAI 兼容的自定义厂商，可扩展 `provider_type` 为 `custom-openai`。

## 后端结构

```
project/admin/api/src/main/java/com/aichuangzuo/admin/modules/system/
├── controller/
│   └── ModelConfigController.java
├── service/
│   ├── ModelConfigService.java
│   └── impl/
│       └── ModelConfigServiceImpl.java
├── entity/
│   └── ModelConfig.java
├── mapper/
│   └── ModelConfigMapper.java
├── dto/
│   └── request/
│       ├── ModelConfigSaveRequest.java
│       └── ModelConfigActiveRequest.java
│   └── ModelConfigDTO.java
└── vo/
    ├── ModelConfigVO.java
    └── ModelOptionVO.java
```

新增工具/配置：

- `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/`
  - `AiProvider.java`：厂商类型枚举。
  - `AiProviderClient.java`：厂商调用抽象。
  - `KimiProviderClient.java`：Kimi 实现。
  - `MinimaxProviderClient.java`：MiniMax 实现。
- `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/crypto/AesUtil.java`：AES 加解密。
- `project/admin/api/src/main/resources/application.yml`：新增 `admin.model.api-key-secret`。

新增错误码（`AdminSystemErrorCode`，24xxxx 范围）：

| 错误码 | 含义 |
|--------|------|
| 240001 | 厂商类型不支持 |
| 240002 | 模型配置不存在 |
| 240003 | API Key 加密失败 |
| 240004 | 拉取模型列表失败 |
| 240005 | 连接测试失败 |
| 240006 | 全局已有启用配置 |

## 前端结构

### 菜单调整

`project/admin/web/src/layouts/AdminLayout.vue` 菜单改为嵌套结构：

```
系统设置（一级）
  └── 模型配置（二级）
```

使用 Ant Design Vue 的 `a-sub-menu` 包裹 `a-menu-item`。

### 新增页面

- `project/admin/web/src/views/ModelConfigView.vue`：模型配置管理页。
- `project/admin/web/src/composables/useModelConfig.js`：模型配置状态与操作。
- `project/admin/web/src/api/modelConfig.js`：接口调用。

### 页面交互

每个厂商一个卡片：

- 厂商名称（Kimi / MiniMax）
- baseUrl 输入框
- apiKey 输入框（密码型）
- 模型选择：下拉框 + 「获取模型」按钮
- 操作：「保存」「启用」「停用」「测试连接」「删除」

列表中显示当前启用状态；未配置的厂商显示「去配置」按钮。

## 权限控制

- 所有 `/api/v1/admin/model-configs/**` 接口仅允许 `SUPER_ADMIN` 访问。
- 复用已有的 `AdminUserPermissionService.isSuperAdmin()` 校验。

## 安全与合规

- `apiKey` 必须 AES 加密后落库。
- 查询接口不返回 `apiKey` 明文；编辑时如需回填，可返回脱敏形式（如 `sk-****xxxx`）或不返回由用户重新填写。
- 建议 fetch-models / test-connection 的请求体中 `apiKey` 使用明文（用户刚输入的），不读取数据库解密后的 key。

## 风险与假设

- Kimi 与 MiniMax 的模型列表接口可能变化，需要兜底逻辑。
- 当前项目没有真实调用模型的代码，模型配置管理完成后，需后续任务接入用户端生成流程。
- AES 加密密钥丢失将导致已保存的 apiKey 无法解密，生产环境需妥善管理 `ADMIN_MODEL_API_KEY_SECRET`。

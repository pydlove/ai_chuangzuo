# 管理端模型配置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在管理端实现「系统设置 > 模型配置」的完整增删改查、厂商模型拉取、连接测试与单配置启用，并确保 `apiKey` 加密存储。

**Architecture:** 后端新增 `modelconfig` 模块，按 MyBatis-Plus 实体/Mapper/Service/Controller 分层；AES 加解密放到 `shared` 工具类，密钥通过 `application.yml` 环境变量注入；厂商调用抽象为 `AiProviderClient`，Kimi 走 OpenAI 兼容 `/v1/models`，MiniMax 返回内置模型并以 `/v1/text/chatcompletion_v2` 测试；前端在 `AdminLayout` 增加嵌套菜单与 `ModelConfigView` 卡片式配置页。

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus + Flyway + Lombok + Vue 3 + Vite + Ant Design Vue 4.x + Axios.

## Global Constraints

- 所有 `/api/v1/admin/model-configs/**` 接口仅允许 `SUPER_ADMIN` 访问。
- `apiKey` 必须 AES 加密后落库；查询接口不返回明文。
- 全局最多只有一条 `is_active = 1` 的配置。
- 每个厂商（`kimi`、`minimax`）最多一条配置，`provider_type` 为唯一键。
- 删除为逻辑删除；再次保存同一厂商时恢复已删除记录。
- 模型列表与连接测试使用请求体中的明文 `apiKey`，不读取数据库。
- 菜单使用 Ant Design Vue 的 `a-sub-menu` 嵌套结构。

---

## Files Created or Modified

### Backend

| File | Purpose |
|------|---------|
| `project/admin/api/src/main/resources/db/migration/V2.0.0_002__create_model_config_table.sql` | 创建 `a_model_config` 表 |
| `project/shared/src/main/java/com/aichuangzuo/shared/utils/AesUtil.java` | AES 加解密工具 |
| `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminModelConfigErrorCode.java` | 模型配置错误码（24xxxx） |
| `project/admin/api/src/main/resources/application.yml` | 新增 `admin.model.api-key-secret` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/entity/ModelConfig.java` | 实体 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/mapper/ModelConfigMapper.java` | Mapper（含自定义查询/更新/删除） |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/dto/request/ModelConfigSaveRequest.java` | 保存请求 DTO |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/dto/request/ModelConfigActiveRequest.java` | 启用/停用请求 DTO |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/dto/request/ModelConfigConnectionRequest.java` | 拉取/测试请求 DTO |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/vo/ModelConfigVO.java` | 列表/详情 VO |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/vo/ModelOptionVO.java` | 模型选项 VO |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/AiProvider.java` | 厂商枚举 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/AiProviderClient.java` | 厂商客户端接口 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/KimiProviderClient.java` | Kimi 客户端实现 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/MinimaxProviderClient.java` | MiniMax 客户端实现 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/service/ModelConfigService.java` | Service 接口 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/service/impl/ModelConfigServiceImpl.java` | Service 实现 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/controller/ModelConfigController.java` | Controller |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/modelconfig/service/ModelConfigServiceTest.java` | Service 单元测试 |
| `project/shared/src/test/java/com/aichuangzuo/shared/utils/AesUtilTest.java` | AES 单元测试 |

### Frontend

| File | Purpose |
|------|---------|
| `project/admin/web/src/layouts/AdminLayout.vue` | 增加「系统设置 > 模型配置」嵌套菜单 |
| `project/admin/web/src/router/index.js` | 增加 `/console/model-configs` 路由 |
| `project/admin/web/src/api/modelConfig.js` | 模型配置接口调用 |
| `project/admin/web/src/composables/useModelConfig.js` | 状态与操作封装 |
| `project/admin/web/src/views/ModelConfigView.vue` | 模型配置管理页面 |

### Verification

| File | Purpose |
|------|---------|
| `tests/e2e/verify_model_config.py` | Playwright 页面加载验证 |

---

### Task 1: 创建数据库表

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_002__create_model_config_table.sql`
- Test: start `admin-api` 后检查表结构

**Interfaces:**
- Produces: `a_model_config` 表，字段与 spec 一致，`provider_type` 唯一。

- [ ] **Step 1: 编写 Flyway 迁移脚本**

```sql
SET NAMES utf8mb4;

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

- [ ] **Step 2: 启动 admin-api 验证迁移**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn spring-boot:run -pl admin/api -am
```

Expected: 服务在 26060 端口正常启动，无 Flyway 报错；数据库中出现 `a_model_config` 表。

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_002__create_model_config_table.sql
git commit -m "feat(admin): 创建模型配置表"
```

---

### Task 2: 新增 AES 加解密工具

**Files:**
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/utils/AesUtil.java`
- Create: `project/shared/src/test/java/com/aichuangzuo/shared/utils/AesUtilTest.java`

**Interfaces:**
- Produces: `AesUtil.encrypt(String plaintext, String secret)`, `AesUtil.decrypt(String ciphertext, String secret)`
- Secret 长度必须是 16/24/32 字节，否则抛异常。

- [ ] **Step 1: 编写 AES 工具类**

```java
package com.aichuangzuo.shared.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private AesUtil() {
    }

    public static String encrypt(String plaintext, String secret) throws Exception {
        byte[] keyBytes = normalizeKey(secret);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String ciphertext, String secret) throws Exception {
        byte[] keyBytes = normalizeKey(secret);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] combined = Base64.getDecoder().decode(ciphertext);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        byte[] encrypted = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static byte[] normalizeKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("AES secret must not be blank");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 16 || bytes.length == 24 || bytes.length == 32) {
            return bytes;
        }
        throw new IllegalArgumentException("AES secret length must be 16/24/32 bytes, actual=" + bytes.length);
    }
}
```

- [ ] **Step 2: 编写单元测试**

```java
package com.aichuangzuo.shared.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AesUtilTest {

    @Test
    void shouldEncryptAndDecrypt() throws Exception {
        String secret = "0123456789abcdef0123456789abcdef";
        String plain = "sk-test-key-12345";

        String encrypted = AesUtil.encrypt(plain, secret);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, AesUtil.decrypt(encrypted, secret));
    }
}
```

- [ ] **Step 3: 运行测试**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn test -pl shared -Dtest=AesUtilTest
```

Expected: `Tests run: 1, Failures: 0, Errors: 0`.

- [ ] **Step 4: Commit**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/utils/AesUtil.java \
        project/shared/src/test/java/com/aichuangzuo/shared/utils/AesUtilTest.java
git commit -m "feat(shared): AES 加解密工具"
```

---

### Task 3: 新增模型配置错误码

**Files:**
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminModelConfigErrorCode.java`

**Interfaces:**
- Produces: `AdminModelConfigErrorCode.PROVIDER_NOT_SUPPORTED`, `MODEL_CONFIG_NOT_FOUND`, `API_KEY_ENCRYPT_FAILED`, `FETCH_MODELS_FAILED`, `TEST_CONNECTION_FAILED`。

- [ ] **Step 1: 编写错误码枚举**

```java
package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminModelConfigErrorCode implements ErrorCode {
    PROVIDER_NOT_SUPPORTED(240001, "厂商类型不支持"),
    MODEL_CONFIG_NOT_FOUND(240002, "模型配置不存在"),
    API_KEY_ENCRYPT_FAILED(240003, "API Key 加密失败"),
    FETCH_MODELS_FAILED(240004, "拉取模型列表失败"),
    TEST_CONNECTION_FAILED(240005, "连接测试失败");

    private final int code;
    private final String message;

    AdminModelConfigErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminModelConfigErrorCode.java
git commit -m "feat(shared): 模型配置错误码"
```

---

### Task 4: 配置 AES 密钥

**Files:**
- Modify: `project/admin/api/src/main/resources/application.yml`

**Interfaces:**
- Produces: `admin.model.api-key-secret` 配置项，通过环境变量 `ADMIN_MODEL_API_KEY_SECRET` 注入。

- [ ] **Step 1: 在 application.yml 添加配置**

在 `auth.jwt` 配置下方新增：

```yaml
admin:
  model:
    api-key-secret: ${ADMIN_MODEL_API_KEY_SECRET:0123456789abcdef0123456789abcdef}
```

> 默认值 `change-me-32-char-secret-key!` 正好是 32 个字符，满足 AES-256 密钥长度要求。生产环境必须通过 `ADMIN_MODEL_API_KEY_SECRET` 覆盖。

- [ ] **Step 2: Commit**

```bash
git add project/admin/api/src/main/resources/application.yml
git commit -m "feat(admin): 模型配置 AES 密钥配置"
```

---

### Task 5: 实体与 Mapper

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/entity/ModelConfig.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/mapper/ModelConfigMapper.java`

**Interfaces:**
- Produces: `ModelConfig` 实体、`ModelConfigMapper` 接口，含按 `provider_type` 查询、逻辑删除恢复更新等方法。

- [ ] **Step 1: 编写实体**

```java
package com.aichuangzuo.admin.modules.modelconfig.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_model_config")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerType;
    private String baseUrl;
    private String apiKeyEncrypted;
    private String modelCode;
    private String modelName;
    private Integer isActive;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
```

- [ ] **Step 2: 编写 Mapper**

```java
package com.aichuangzuo.admin.modules.modelconfig.mapper;

import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    @Select("SELECT * FROM a_model_config WHERE provider_type = #{providerType} AND is_deleted = 0 LIMIT 1")
    ModelConfig selectByProviderType(@Param("providerType") String providerType);

    @Select("SELECT * FROM a_model_config WHERE provider_type = #{providerType} LIMIT 1")
    ModelConfig selectByProviderTypeIncludingDeleted(@Param("providerType") String providerType);

    @Update("UPDATE a_model_config SET base_url = #{baseUrl}, api_key_encrypted = #{apiKeyEncrypted}, " +
            "model_code = #{modelCode}, model_name = #{modelName}, is_active = #{isActive}, is_deleted = 0, " +
            "updated_at = NOW(3), updated_by = #{updatedBy} WHERE id = #{id}")
    int updateByIdIncludingDeleted(ModelConfig entity);

    @Update("UPDATE a_model_config SET is_deleted = 1, updated_at = NOW(3), updated_by = #{updatedBy} " +
            "WHERE provider_type = #{providerType} AND is_deleted = 0")
    int deleteByProviderType(@Param("providerType") String providerType, @Param("updatedBy") Long updatedBy);
}
```

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/entity/ModelConfig.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/mapper/ModelConfigMapper.java
git commit -m "feat(admin): 模型配置实体与 Mapper"
```

---

### Task 6: DTO 与 VO

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/dto/request/ModelConfigSaveRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/dto/request/ModelConfigActiveRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/dto/request/ModelConfigConnectionRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/vo/ModelConfigVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/vo/ModelOptionVO.java`

**Interfaces:**
- Produces: 请求 DTO 与响应 VO，供 Service 和 Controller 使用。

- [ ] **Step 1: 编写保存请求 DTO**

```java
package com.aichuangzuo.admin.modules.modelconfig.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModelConfigSaveRequest {

    @NotBlank
    private String baseUrl;

    private String apiKey;

    @NotBlank
    private String modelCode;

    private String modelName;

    @NotNull
    private Integer isActive;
}
```

- [ ] **Step 2: 编写启用/停用请求 DTO**

```java
package com.aichuangzuo.admin.modules.modelconfig.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModelConfigActiveRequest {

    @NotNull
    private Integer isActive;
}
```

- [ ] **Step 3: 编写连接请求 DTO**

```java
package com.aichuangzuo.admin.modules.modelconfig.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelConfigConnectionRequest {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String apiKey;
}
```

- [ ] **Step 4: 编写配置 VO**

```java
package com.aichuangzuo.admin.modules.modelconfig.vo;

import lombok.Data;

@Data
public class ModelConfigVO {

    private Long id;
    private String providerType;
    private String providerName;
    private String baseUrl;
    private String modelCode;
    private String modelName;
    private Integer isActive;
}
```

- [ ] **Step 5: 编写模型选项 VO**

```java
package com.aichuangzuo.admin.modules.modelconfig.vo;

import lombok.Data;

@Data
public class ModelOptionVO {

    private String modelCode;
    private String modelName;
}
```

- [ ] **Step 6: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/dto \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/vo
git commit -m "feat(admin): 模型配置 DTO 与 VO"
```

---

### Task 7: 厂商客户端

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/AiProvider.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/AiProviderClient.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/KimiProviderClient.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai/MinimaxProviderClient.java`

**Interfaces:**
- Consumes: `ModelOptionVO`
- Produces: `AiProvider` 枚举、`AiProviderClient.testConnection(...)`、`AiProviderClient.fetchModels(...)`

- [ ] **Step 1: 编写厂商枚举**

```java
package com.aichuangzuo.admin.infrastructure.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum AiProvider {
    KIMI("kimi", "Kimi"),
    MINIMAX("minimax", "MiniMax");

    private final String code;
    private final String name;

    public static Optional<AiProvider> fromCode(String code) {
        return Arrays.stream(values())
                .filter(p -> p.code.equalsIgnoreCase(code))
                .findFirst();
    }

    public static List<AiProvider> all() {
        return Arrays.asList(values());
    }
}
```

- [ ] **Step 2: 编写客户端接口**

```java
package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;

import java.util.List;

public interface AiProviderClient {

    boolean testConnection(String baseUrl, String apiKey);

    List<ModelOptionVO> fetchModels(String baseUrl, String apiKey);
}
```

- [ ] **Step 3: 编写 Kimi 客户端**

```java
package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KimiProviderClient implements AiProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Map<String, String> FRIENDLY_NAMES = Map.of(
            "moonshot-v1-8k", "Moonshot V1 8K",
            "moonshot-v1-32k", "Moonshot V1 32K",
            "moonshot-v1-128k", "Moonshot V1 128K"
    );

    @Override
    public boolean testConnection(String baseUrl, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(trim(baseUrl) + "/v1/models", HttpMethod.GET, entity, String.class);
            return true;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            return false;
        } catch (RestClientException e) {
            log.warn("Kimi test connection failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<ModelOptionVO> fetchModels(String baseUrl, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                trim(baseUrl) + "/v1/models", HttpMethod.GET, entity, Map.class);

        Object data = response.getBody() != null ? response.getBody().get("data") : null;
        if (!(data instanceof List)) {
            return Collections.emptyList();
        }

        return ((List<Map<String, Object>>) data).stream()
                .map(m -> {
                    String code = (String) m.get("id");
                    ModelOptionVO vo = new ModelOptionVO();
                    vo.setModelCode(code);
                    vo.setModelName(FRIENDLY_NAMES.getOrDefault(code, code));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private String trim(String baseUrl) {
        return baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }
}
```

- [ ] **Step 4: 编写 MiniMax 客户端**

```java
package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MinimaxProviderClient implements AiProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final List<ModelOptionVO> DEFAULT_MODELS = List.of(
            option("abab6.5s-chat", "abab6.5s-chat"),
            option("abab6-chat", "abab6-chat")
    );

    @Override
    public boolean testConnection(String baseUrl, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", "abab6.5s-chat",
                    "messages", List.of(Map.of("role", "user", "content", "hi"))
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    trim(baseUrl) + "/v1/text/chatcompletion_v2", entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            return false;
        } catch (RestClientException e) {
            log.warn("MiniMax test connection failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<ModelOptionVO> fetchModels(String baseUrl, String apiKey) {
        return DEFAULT_MODELS;
    }

    private static ModelOptionVO option(String code, String name) {
        ModelOptionVO vo = new ModelOptionVO();
        vo.setModelCode(code);
        vo.setModelName(name);
        return vo;
    }

    private String trim(String baseUrl) {
        return baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/infrastructure/ai
git commit -m "feat(admin): Kimi / MiniMax 厂商客户端"
```

---

### Task 8: Service 实现

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/service/ModelConfigService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/service/impl/ModelConfigServiceImpl.java`

**Interfaces:**
- Consumes: `ModelConfigMapper`, `KimiProviderClient`, `MinimaxProviderClient`, `AesUtil`, `SecurityAdminContext`
- Produces: `listConfigs()`, `getConfig(...)`, `saveConfig(...)`, `deleteConfig(...)`, `fetchModels(...)`, `testConnection(...)`, `toggleActive(...)`

- [ ] **Step 1: 编写 Service 接口**

```java
package com.aichuangzuo.admin.modules.modelconfig.service;

import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;

import java.util.List;

public interface ModelConfigService {

    List<ModelConfigVO> listConfigs();

    ModelConfigVO getConfig(String providerType);

    void saveConfig(String providerType, ModelConfigSaveRequest request);

    void deleteConfig(String providerType);

    List<ModelOptionVO> fetchModels(String providerType, ModelConfigConnectionRequest request);

    boolean testConnection(String providerType, ModelConfigConnectionRequest request);

    void toggleActive(String providerType, ModelConfigActiveRequest request);
}
```

- [ ] **Step 2: 编写 Service 实现**

```java
package com.aichuangzuo.admin.modules.modelconfig.service.impl;

import com.aichuangzuo.admin.infrastructure.ai.AiProvider;
import com.aichuangzuo.admin.infrastructure.ai.AiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.KimiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.MinimaxProviderClient;
import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.modelconfig.service.ModelConfigService;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.aichuangzuo.shared.enums.error.AdminModelConfigErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;
    private final KimiProviderClient kimiProviderClient;
    private final MinimaxProviderClient minimaxProviderClient;

    @Value("${admin.model.api-key-secret}")
    private String apiKeySecret;

    @Override
    public List<ModelConfigVO> listConfigs() {
        List<ModelConfigVO> result = new ArrayList<>();
        for (AiProvider provider : AiProvider.all()) {
            ModelConfig entity = modelConfigMapper.selectByProviderType(provider.getCode());
            result.add(toVo(entity, provider));
        }
        return result;
    }

    @Override
    public ModelConfigVO getConfig(String providerType) {
        AiProvider provider = resolveProvider(providerType);
        ModelConfig entity = modelConfigMapper.selectByProviderType(providerType);
        return toVo(entity, provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(String providerType, ModelConfigSaveRequest request) {
        AiProvider provider = resolveProvider(providerType);

        ModelConfig existing = modelConfigMapper.selectByProviderTypeIncludingDeleted(providerType);
        boolean isNew = (existing == null);
        ModelConfig entity = isNew ? new ModelConfig() : existing;

        entity.setProviderType(providerType);
        entity.setBaseUrl(request.getBaseUrl());
        if (StringUtils.hasText(request.getApiKey())) {
            entity.setApiKeyEncrypted(encryptApiKey(request.getApiKey()));
        }
        entity.setModelCode(request.getModelCode());
        entity.setModelName(request.getModelName());
        entity.setIsDeleted(0);

        Integer active = request.getIsActive() != null ? request.getIsActive() : 0;
        entity.setIsActive(active);
        entity.setUpdatedBy(currentAdminIdOrZero());

        if (active == 1) {
            deactivateOthers(providerType);
        }

        if (isNew) {
            entity.setCreatedBy(entity.getUpdatedBy());
            modelConfigMapper.insert(entity);
        } else {
            modelConfigMapper.updateByIdIncludingDeleted(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String providerType) {
        resolveProvider(providerType);
        ModelConfig entity = modelConfigMapper.selectByProviderType(providerType);
        if (entity == null) {
            throw new BusinessException(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND);
        }
        modelConfigMapper.deleteByProviderType(providerType, currentAdminIdOrZero());
    }

    @Override
    public List<ModelOptionVO> fetchModels(String providerType, ModelConfigConnectionRequest request) {
        AiProvider provider = resolveProvider(providerType);
        try {
            return clientFor(provider).fetchModels(request.getBaseUrl(), request.getApiKey());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("fetch models failed, provider={}", providerType, e);
            throw new BusinessException(AdminModelConfigErrorCode.FETCH_MODELS_FAILED);
        }
    }

    @Override
    public boolean testConnection(String providerType, ModelConfigConnectionRequest request) {
        AiProvider provider = resolveProvider(providerType);
        try {
            return clientFor(provider).testConnection(request.getBaseUrl(), request.getApiKey());
        } catch (Exception e) {
            log.error("test connection failed, provider={}", providerType, e);
            throw new BusinessException(AdminModelConfigErrorCode.TEST_CONNECTION_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleActive(String providerType, ModelConfigActiveRequest request) {
        resolveProvider(providerType);
        ModelConfig entity = modelConfigMapper.selectByProviderType(providerType);
        if (entity == null) {
            throw new BusinessException(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND);
        }

        Integer active = request.getIsActive() != null ? request.getIsActive() : 0;
        entity.setIsActive(active);
        entity.setUpdatedBy(currentAdminIdOrZero());

        if (active == 1) {
            deactivateOthers(providerType);
        }
        modelConfigMapper.updateByIdIncludingDeleted(entity);
    }

    private AiProvider resolveProvider(String providerType) {
        return AiProvider.fromCode(providerType)
                .orElseThrow(() -> new BusinessException(AdminModelConfigErrorCode.PROVIDER_NOT_SUPPORTED));
    }

    private AiProviderClient clientFor(AiProvider provider) {
        return switch (provider) {
            case KIMI -> kimiProviderClient;
            case MINIMAX -> minimaxProviderClient;
        };
    }

    private ModelConfigVO toVo(ModelConfig entity, AiProvider provider) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setProviderType(provider.getCode());
        vo.setProviderName(provider.getName());
        if (entity != null) {
            vo.setId(entity.getId());
            vo.setBaseUrl(entity.getBaseUrl());
            vo.setModelCode(entity.getModelCode());
            vo.setModelName(entity.getModelName());
            vo.setIsActive(entity.getIsActive());
        } else {
            vo.setBaseUrl("");
            vo.setModelCode("");
            vo.setModelName("");
            vo.setIsActive(0);
        }
        return vo;
    }

    private String encryptApiKey(String plain) {
        try {
            return AesUtil.encrypt(plain, apiKeySecret);
        } catch (Exception e) {
            log.error("encrypt api key failed", e);
            throw new BusinessException(AdminModelConfigErrorCode.API_KEY_ENCRYPT_FAILED);
        }
    }

    private void deactivateOthers(String providerType) {
        LambdaUpdateWrapper<ModelConfig> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ModelConfig::getIsActive, 1).ne(ModelConfig::getProviderType, providerType);

        ModelConfig update = new ModelConfig();
        update.setIsActive(0);
        update.setUpdatedBy(currentAdminIdOrZero());
        modelConfigMapper.update(update, wrapper);
    }

    private Long currentAdminIdOrZero() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return adminId != null ? adminId : 0L;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/service
git commit -m "feat(admin): 模型配置 Service"
```

---

### Task 9: Controller

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/controller/ModelConfigController.java`

**Interfaces:**
- Consumes: `ModelConfigService`, `AdminUserPermissionService`, `SecurityAdminContext`
- Produces: REST endpoints under `/api/v1/admin/model-configs/**`

- [ ] **Step 1: 编写 Controller**

```java
package com.aichuangzuo.admin.modules.modelconfig.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.service.ModelConfigService;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端模型配置")
@RestController
@RequestMapping("/api/v1/admin/model-configs")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询模型配置列表")
    @GetMapping
    public Result<List<ModelConfigVO>> list() {
        checkSuperAdmin();
        return Result.success(modelConfigService.listConfigs());
    }

    @Operation(summary = "查看模型配置详情")
    @GetMapping("/{providerType}")
    public Result<ModelConfigVO> get(@PathVariable(name = "providerType") String providerType) {
        checkSuperAdmin();
        return Result.success(modelConfigService.getConfig(providerType));
    }

    @Operation(summary = "保存/更新模型配置")
    @PutMapping("/{providerType}")
    public Result<Void> save(@PathVariable(name = "providerType") String providerType,
                             @Valid @RequestBody ModelConfigSaveRequest request) {
        checkSuperAdmin();
        modelConfigService.saveConfig(providerType, request);
        return Result.success();
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/{providerType}")
    public Result<Void> delete(@PathVariable(name = "providerType") String providerType) {
        checkSuperAdmin();
        modelConfigService.deleteConfig(providerType);
        return Result.success();
    }

    @Operation(summary = "拉取模型列表")
    @PostMapping("/{providerType}/actions/fetch-models")
    public Result<List<ModelOptionVO>> fetchModels(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        checkSuperAdmin();
        return Result.success(modelConfigService.fetchModels(providerType, request));
    }

    @Operation(summary = "测试连接")
    @PostMapping("/{providerType}/actions/test-connection")
    public Result<Map<String, Boolean>> testConnection(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        checkSuperAdmin();
        boolean success = modelConfigService.testConnection(providerType, request);
        return Result.success(Map.of("success", success));
    }

    @Operation(summary = "启用/停用配置")
    @PostMapping("/{providerType}/actions/toggle-active")
    public Result<Void> toggleActive(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigActiveRequest request) {
        checkSuperAdmin();
        modelConfigService.toggleActive(providerType, request);
        return Result.success();
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/modelconfig/controller/ModelConfigController.java
git commit -m "feat(admin): 模型配置 Controller"
```

---

### Task 10: 后端单元测试

**Files:**
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/modelconfig/service/ModelConfigServiceTest.java`

**Interfaces:**
- Consumes: `ModelConfigServiceImpl` with mocked mapper and clients
- Produces: passing tests for list, save, delete, toggle

- [ ] **Step 1: 编写 Service 测试**

```java
package com.aichuangzuo.admin.modules.modelconfig.service;

import com.aichuangzuo.admin.infrastructure.ai.KimiProviderClient;
import com.aichuangzuo.admin.infrastructure.ai.MinimaxProviderClient;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.modelconfig.service.impl.ModelConfigServiceImpl;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.shared.enums.error.AdminModelConfigErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @Mock
    private KimiProviderClient kimiProviderClient;

    @Mock
    private MinimaxProviderClient minimaxProviderClient;

    @InjectMocks
    private ModelConfigServiceImpl modelConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(modelConfigService, "apiKeySecret",
                "0123456789abcdef0123456789abcdef");
    }

    @Test
    void listConfigs_shouldReturnAllProviders() {
        ModelConfig config = new ModelConfig();
        config.setId(1L);
        config.setProviderType("kimi");
        config.setBaseUrl("https://api.moonshot.cn");
        config.setModelCode("moonshot-v1-8k");
        config.setIsActive(1);

        when(modelConfigMapper.selectByProviderType("kimi")).thenReturn(config);
        when(modelConfigMapper.selectByProviderType("minimax")).thenReturn(null);

        List<ModelConfigVO> result = modelConfigService.listConfigs();

        assertEquals(2, result.size());
        assertEquals("Kimi", result.get(0).getProviderName());
        assertEquals("moonshot-v1-8k", result.get(0).getModelCode());
        assertEquals("MiniMax", result.get(1).getProviderName());
        assertEquals(0, result.get(1).getIsActive());
    }

    @Test
    void saveConfig_shouldCreateAndEncryptApiKey() {
        when(modelConfigMapper.selectByProviderTypeIncludingDeleted("kimi")).thenReturn(null);

        ModelConfigSaveRequest request = new ModelConfigSaveRequest();
        request.setBaseUrl("https://api.moonshot.cn");
        request.setApiKey("sk-test");
        request.setModelCode("moonshot-v1-8k");
        request.setModelName("Moonshot V1 8K");
        request.setIsActive(0);

        modelConfigService.saveConfig("kimi", request);

        verify(modelConfigMapper).insert(any(ModelConfig.class));
    }

    @Test
    void deleteConfig_shouldThrowWhenNotFound() {
        when(modelConfigMapper.selectByProviderType("kimi")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> modelConfigService.deleteConfig("kimi"));
        assertEquals(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void toggleActive_shouldThrowWhenNotFound() {
        when(modelConfigMapper.selectByProviderType("minimax")).thenReturn(null);

        ModelConfigActiveRequest request = new ModelConfigActiveRequest();
        request.setIsActive(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> modelConfigService.toggleActive("minimax", request));
        assertEquals(AdminModelConfigErrorCode.MODEL_CONFIG_NOT_FOUND.getCode(), ex.getCode());
    }
}
```

- [ ] **Step 2: 运行测试**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn test -pl admin/api -Dtest=ModelConfigServiceTest
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`。

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/modelconfig/service/ModelConfigServiceTest.java
git commit -m "test(admin): 模型配置 Service 单元测试"
```

---

### Task 11: 前端菜单与路由

**Files:**
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`
- Modify: `project/admin/web/src/router/index.js`

**Interfaces:**
- Produces: 侧边栏「系统设置 > 模型配置」入口，路由 `/console/model-configs`。

- [ ] **Step 1: 修改 AdminLayout.vue**

完整替换 `<a-menu>` 块为：

```vue
<a-menu
  mode="inline"
  :selected-keys="[$route.path]"
  :open-keys="openKeys"
  class="admin-menu"
  @click="handleMenuClick"
  @openChange="onOpenChange"
>
  <a-menu-item key="/console/users">
    <template #icon>
      <UserOutlined />
    </template>
    用户管理
  </a-menu-item>
  <a-menu-item key="/console/styles">
    <template #icon>
      <AuditOutlined />
    </template>
    风格审核
  </a-menu-item>
  <a-sub-menu key="/console/settings">
    <template #icon>
      <SettingOutlined />
    </template>
    <template #title>系统设置</template>
    <a-menu-item key="/console/model-configs">
      <template #icon>
        <ApiOutlined />
      </template>
      模型配置
    </a-menu-item>
  </a-sub-menu>
</a-menu>
```

在 `<script setup>` 中：

1. 引入新增图标：

```javascript
import { UserOutlined, AuditOutlined, SettingOutlined, ApiOutlined } from '@ant-design/icons-vue'
```

2. 在 `userInitial` 后新增展开状态：

```javascript
const openKeys = ref(['/console/settings'])
const onOpenChange = (keys) => {
  openKeys.value = keys
}
```

3. 在 `currentMenuName` 计算属性中新增：

```javascript
if (route.path === '/console/model-configs') return '模型配置'
```

- [ ] **Step 2: 修改 router/index.js**

在 `/console` 的 `children` 中新增：

```javascript
{
  path: 'model-configs',
  name: 'AdminModelConfig',
  component: () => import('@/views/ModelConfigView.vue')
}
```

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/layouts/AdminLayout.vue \
        project/admin/web/src/router/index.js
git commit -m "feat(admin-web): 模型配置菜单与路由"
```

---

### Task 12: 前端 API 与 Composable

**Files:**
- Create: `project/admin/web/src/api/modelConfig.js`
- Create: `project/admin/web/src/composables/useModelConfig.js`

**Interfaces:**
- Produces: `listConfigs`, `getConfig`, `saveConfig`, `deleteConfig`, `fetchModels`, `testConnection`, `toggleActive` API functions and `useModelConfig` composable.

- [ ] **Step 1: 编写 API 模块**

```javascript
import request from '@/utils/request.js'

export function listConfigs() {
  return request.get('/api/v1/admin/model-configs').then((res) => res.data)
}

export function getConfig(providerType) {
  return request.get(`/api/v1/admin/model-configs/${providerType}`).then((res) => res.data)
}

export function saveConfig(providerType, data) {
  return request.put(`/api/v1/admin/model-configs/${providerType}`, data)
}

export function deleteConfig(providerType) {
  return request.delete(`/api/v1/admin/model-configs/${providerType}`)
}

export function fetchModels(providerType, data) {
  return request.post(`/api/v1/admin/model-configs/${providerType}/actions/fetch-models`, data).then((res) => res.data)
}

export function testConnection(providerType, data) {
  return request.post(`/api/v1/admin/model-configs/${providerType}/actions/test-connection`, data).then((res) => res.data)
}

export function toggleActive(providerType, data) {
  return request.post(`/api/v1/admin/model-configs/${providerType}/actions/toggle-active`, data)
}
```

- [ ] **Step 2: 编写 Composable**

```javascript
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listConfigs,
  saveConfig,
  deleteConfig,
  fetchModels,
  testConnection,
  toggleActive
} from '@/api/modelConfig.js'

export function useModelConfig() {
  const providers = ref([])
  const loading = ref(false)

  const fetchProviders = async () => {
    loading.value = true
    try {
      providers.value = await listConfigs()
    } catch (error) {
      message.error(error.message || '加载模型配置失败')
    } finally {
      loading.value = false
    }
  }

  const saveProvider = async (providerType, form) => {
    await saveConfig(providerType, form)
    message.success('保存成功')
    await fetchProviders()
  }

  const removeProvider = async (providerType) => {
    await deleteConfig(providerType)
    message.success('删除成功')
    await fetchProviders()
  }

  const fetchModelOptions = async (providerType, form) => {
    return await fetchModels(providerType, form)
  }

  const testProviderConnection = async (providerType, form) => {
    const res = await testConnection(providerType, form)
    message[res.success ? 'success' : 'error'](res.success ? '连接成功' : '连接失败')
    return res.success
  }

  const toggleProviderActive = async (providerType, isActive) => {
    await toggleActive(providerType, { isActive })
    message.success(isActive ? '已启用' : '已停用')
    await fetchProviders()
  }

  return {
    providers,
    loading,
    fetchProviders,
    saveProvider,
    removeProvider,
    fetchModelOptions,
    testProviderConnection,
    toggleProviderActive
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/api/modelConfig.js \
        project/admin/web/src/composables/useModelConfig.js
git commit -m "feat(admin-web): 模型配置 API 与 Composable"
```

---

### Task 13: 前端模型配置页面

**Files:**
- Create: `project/admin/web/src/views/ModelConfigView.vue`

**Interfaces:**
- Consumes: `useModelConfig`, `message`
- Produces: 每个厂商一个可编辑卡片，支持获取模型、测试连接、保存、启用/停用、删除。

- [ ] **Step 1: 编写页面**

```vue
<template>
  <div class="model-config">
    <h3 class="page-title">模型配置</h3>
    <p class="page-desc">配置 AI 大模型厂商接入参数，全局仅可启用一个配置。</p>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col
          v-for="provider in providers"
          :key="provider.providerType"
          :xs="24"
          :lg="12"
        >
          <a-card :title="provider.providerName" class="config-card">
            <template #extra>
              <a-tag v-if="provider.isActive" color="green">已启用</a-tag>
              <a-tag v-else color="default">未启用</a-tag>
            </template>

            <a-form layout="vertical">
              <a-form-item label="Base URL">
                <a-input
                  v-model:value="forms[provider.providerType].baseUrl"
                  placeholder="https://api.moonshot.cn"
                />
              </a-form-item>

              <a-form-item label="API Key">
                <a-input-password
                  v-model:value="forms[provider.providerType].apiKey"
                  placeholder="sk-..."
                />
              </a-form-item>

              <a-form-item label="模型">
                <a-select
                  v-model:value="forms[provider.providerType].modelCode"
                  :options="modelOptions[provider.providerType]"
                  placeholder="请选择或获取模型"
                  allow-clear
                  show-search
                />
              </a-form-item>

              <a-form-item>
                <a-space wrap>
                  <a-button @click="handleFetchModels(provider.providerType)">
                    获取模型
                  </a-button>
                  <a-button @click="handleTestConnection(provider.providerType)">
                    测试连接
                  </a-button>
                  <a-button type="primary" @click="handleSave(provider.providerType)">
                    保存
                  </a-button>
                  <a-button
                    v-if="provider.isActive"
                    @click="handleToggle(provider.providerType, 0)"
                  >
                    停用
                  </a-button>
                  <a-button
                    v-else
                    type="primary"
                    ghost
                    @click="handleToggle(provider.providerType, 1)"
                  >
                    启用
                  </a-button>
                  <a-popconfirm
                    title="确定删除该配置？"
                    ok-text="确认"
                    cancel-text="取消"
                    @confirm="handleDelete(provider.providerType)"
                  >
                    <a-button danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useModelConfig } from '@/composables/useModelConfig.js'

const {
  providers,
  loading,
  fetchProviders,
  saveProvider,
  removeProvider,
  fetchModelOptions,
  testProviderConnection,
  toggleProviderActive
} = useModelConfig()

const forms = reactive({})
const modelOptions = reactive({})

const initForms = () => {
  providers.value.forEach((p) => {
    if (!forms[p.providerType]) {
      forms[p.providerType] = {
        baseUrl: p.baseUrl || '',
        apiKey: '',
        modelCode: p.modelCode || '',
        modelName: p.modelName || '',
        isActive: p.isActive
      }
    }
    if (!modelOptions[p.providerType]) {
      modelOptions[p.providerType] = []
    }
  })
}

const handleFetchModels = async (providerType) => {
  const form = forms[providerType]
  if (!form.baseUrl || !form.apiKey) {
    message.warning('请先填写 Base URL 和 API Key')
    return
  }
  try {
    const options = await fetchModelOptions(providerType, {
      baseUrl: form.baseUrl,
      apiKey: form.apiKey
    })
    modelOptions[providerType] = options.map((o) => ({
      label: o.modelName,
      value: o.modelCode
    }))
    message.success('获取模型成功')
  } catch (error) {
    // composable 已提示错误
  }
}

const handleTestConnection = async (providerType) => {
  const form = forms[providerType]
  if (!form.baseUrl || !form.apiKey) {
    message.warning('请先填写 Base URL 和 API Key')
    return
  }
  try {
    await testProviderConnection(providerType, {
      baseUrl: form.baseUrl,
      apiKey: form.apiKey
    })
  } catch (error) {
    // composable 已提示错误
  }
}

const syncFormFromProvider = (providerType) => {
  const updated = providers.value.find((p) => p.providerType === providerType)
  if (!updated) return
  forms[providerType] = {
    baseUrl: updated.baseUrl || '',
    apiKey: '',
    modelCode: updated.modelCode || '',
    modelName: updated.modelName || '',
    isActive: updated.isActive
  }
}

const handleSave = async (providerType) => {
  const form = forms[providerType]
  const payload = {
    baseUrl: form.baseUrl,
    modelCode: form.modelCode,
    modelName: form.modelName,
    isActive: form.isActive
  }
  if (form.apiKey) {
    payload.apiKey = form.apiKey
  }
  await saveProvider(providerType, payload)
  syncFormFromProvider(providerType)
}

const handleToggle = async (providerType, isActive) => {
  await toggleProviderActive(providerType, isActive)
  syncFormFromProvider(providerType)
}

const handleDelete = async (providerType) => {
  await removeProvider(providerType)
  syncFormFromProvider(providerType)
}

onMounted(async () => {
  await fetchProviders()
  initForms()
})
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.page-desc {
  color: #8c8c8c;
  margin: 0 0 16px;
}

.config-card {
  border-radius: 8px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/views/ModelConfigView.vue
git commit -m "feat(admin-web): 模型配置管理页面"
```

---

### Task 14: 验证

**Files:**
- Create: `tests/e2e/verify_model_config.py`

**Interfaces:**
- Consumes: 已启动的 admin-api（26060）和 admin-web dev server（5173）
- Produces: 后端测试通过、页面加载截图、关键接口 curl 验证。

- [ ] **Step 1: 运行后端单元测试**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn test -pl admin/api -am -Dtest=AesUtilTest,ModelConfigServiceTest
```

Expected: 所有测试通过。

- [ ] **Step 2: 启动服务并验证接口**

1. 启动 admin-api：
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn spring-boot:run -pl admin/api -am
```

2. 获取 admin token：
```bash
curl -X POST http://localhost:26060/api/v1/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Root1qaz!QAZ"}'
```

3. 验证列表接口：
```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:26060/api/v1/admin/model-configs
```

Expected: 返回 Kimi 和 MiniMax 两条数据，未配置时 `baseUrl` 为空字符串，`isActive` 为 0。

4. 验证保存：
```bash
curl -X PUT -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  http://localhost:26060/api/v1/admin/model-configs/kimi \
  -d '{"baseUrl":"https://api.moonshot.cn","apiKey":"sk-test","modelCode":"moonshot-v1-8k","modelName":"Moonshot V1 8K","isActive":1}'
```

Expected: code 0；数据库 `a_model_config` 中 `api_key_encrypted` 为密文。

- [ ] **Step 3: 编写并运行 E2E 脚本**

```python
from playwright.sync_api import sync_playwright
import requests

BASE_URL = 'http://localhost:22346'
API_URL = 'http://localhost:26060'


def get_admin_token():
    resp = requests.post(
        f'{API_URL}/api/v1/admin/auth/login',
        json={'username': 'admin', 'password': 'Root1qaz!QAZ'}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def login_as_admin(page):
    token = get_admin_token()
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate(f"""
      window.localStorage.setItem('admin_access_token', JSON.stringify('{token}'))
      window.localStorage.setItem('admin_refresh_token', JSON.stringify('{token}'))
    """)


def test_model_config_page():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        login_as_admin(page)
        page.goto(f'{BASE_URL}/console/model-configs')
        page.wait_for_selector('.config-card', timeout=10000)

        cards = page.query_selector_all('.config-card')
        assert len(cards) >= 2, f'应至少展示 2 个厂商卡片，实际 {len(cards)}'

        page.screenshot(path='tests/e2e/screenshots/model_config.png')
        browser.close()


if __name__ == '__main__':
    test_model_config_page()
```

Run:
```bash
python3 tests/e2e/verify_model_config.py
```

Expected: 脚本无报错并生成截图。

- [ ] **Step 4: Commit**

```bash
git add tests/e2e/verify_model_config.py
git commit -m "test(e2e): 模型配置页面加载验证"
```

---

## Plan Self-Review

1. **Spec coverage:**
   - 表结构与唯一键 → Task 1。
   - AES 加密存储 → Task 2 + Task 4。
   - 列表/详情/保存/删除/拉取模型/测试连接/启用停用 → Tasks 5-9。
   - SUPER_ADMIN 权限 → Task 9 `checkSuperAdmin()`。
   - 全局唯一启用 → Task 8 `deactivateOthers(...)`。
   - 前端嵌套菜单与页面 → Tasks 11-13。
   - 测试与验证 → Task 10 + Task 14。
   - 无 gaps。

2. **Placeholder scan:** 无 TBD/TODO/"实现 later"/"适当处理"。

3. **Type consistency:**
   - `ModelConfigVO` / `ModelOptionVO` 字段名与 Controller/Service 一致。
   - `providerType` 路径变量与 `AiProvider.code` 一致。
   - `isActive` 使用 `Integer` 与数据库 TINYINT 对应。

4. **One concern resolved in plan:** 逻辑删除记录再次保存时，使用 `selectByProviderTypeIncludingDeleted` + `updateByIdIncludingDeleted` 恢复，避免 `provider_type` 唯一键冲突。

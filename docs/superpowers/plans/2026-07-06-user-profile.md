# 我的/个人中心 实施计划 (2026-07-06)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `user/api` 新建 `modules/user`，提供 4 个 endpoint（GET /me、PUT /me/nickname、PUT /me/email、PUT /me/password）让前端弹框和 MineIndex 拿到真实用户数据并支持修改；前端用 `useUserProfile` composable 接管 `consoleActions` 中的硬编码字段；新代码全配 Javadoc，老的 `User` 实体和 `EmailCodeServiceImpl#validateEmailCode` 顺手补注释。

**Architecture:** 后端新建 `modules/user`(controller / service 接口 / service impl / vo / dto/request / converter)，复用 `auth/UserMapper` 和 `auth/EmailCodeService`；前端在 `api/user.js` + `composables/useUserProfile.js` 拉取，ConsoleLayout onMounted 触发，弹框保存按钮改调真实 API。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus 3.5.8 + Spring Security 6 + MapStruct 1.5.5 + JUnit 5 + Vue 3 + Axios + Ant Design Vue。

## Global Constraints

- **DB 不动**：u_user 表已有本轮需要的所有字段；不新增表、不改表结构、不新增字段
- **不破坏现有功能**：登录/注册/Forgot/Reset 流程不改；`SecurityConfig` 中 `/api/v1/user/auth/**` 放行规则保留；新 `/api/v1/user/me/**` 走 `.anyRequest().authenticated()` 走 JWT
- **范围严格限定**：4 个 endpoint + 1 个 VO + 3 个 Request DTO；coinBalance / inviteStats / membershipLevel / membershipExpiry / hasMembership 本轮保持 ref 占位
- **错误码沿用 `UserAuthErrorCode`**：复用已有 `EMAIL_ALREADY_EXISTS(111006)` / `PASSWORD_NOT_MATCH(111007)` / `PASSWORD_FORMAT_ERROR(111008)` / `EMAIL_CODE_ERROR(111003)`；只新增 `EMAIL_SAME_AS_OLD(111015)` 和 `PASSWORD_INCORRECT(111016)`
- **Javadoc 范围**：本轮新建的 controller/service/impl/vo/dto 全写类级和方法级 Javadoc；会调用的既有类（`EmailCodeServiceImpl#validateEmailCode`、`UserMapper#existsByEmail`、`User` 实体 8 个非自明字段）顺手补；其他老代码不动避免范围蔓延
- **垃圾代码随写随删**：调试用 console.log、临时 mock、注释掉的旧逻辑、本轮不用的占位字段都不留到下次提交
- **前端 axios baseURL**：是 `/api/v1/user`，所以前端 url 写 `/me`、`/me/nickname` 等相对路径
- **测试隔离**：新增测试沿用 `@SpringBootTest @Transactional` + `@MockBean` 模式；不引入新依赖

## File Structure

**后端新增/修改：**
- 修改：`project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java`（+2 个枚举值）
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/entity/User.java`（字段级注释）
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mapper/UserMapper.java`（+ `existsByEmail`）
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/vo/UserProfileVO.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/UpdateNicknameRequest.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/UpdateEmailRequest.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/ChangePasswordRequest.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/converter/UserConverter.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/UserProfileService.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/user/controller/UserProfileController.java`
- 新增：`project/user/api/src/test/java/com/aichuangzuo/user/modules/user/mapper/UserMapperExistsByEmailTest.java`
- 新增：`project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceGetTest.java`
- 新增：`project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceNicknameTest.java`
- 新增：`project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceEmailTest.java`
- 新增：`project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServicePasswordTest.java`
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java`（给 `validateEmailCode` 加方法级 Javadoc）

**前端新增/修改：**
- 新增：`project/user/web/src/api/user.js`
- 新增：`project/user/web/src/composables/useUserProfile.js`
- 修改：`project/user/web/src/views/console/ConsoleLayout.vue`（onMounted 拉数据 + provide 改真实 + 弹框保存改 API）

---

### Task 1: 错误码新增 + User 实体字段注释

**Files:**
- Modify: `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/entity/User.java`

**Consumes:** 无

**Produces:** 2 个新错误码可用；User 实体 8 个字段有 `/** ... */` 行内注释

- [ ] **Step 1: 在 `UserAuthErrorCode` 新增 2 个错误码**

打开 `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java`，在最后一个枚举值（`EMAIL_SEND_FAILED`）之后添加：

```java
    EMAIL_SAME_AS_OLD(111015, "新邮箱与原邮箱相同"),
    PASSWORD_INCORRECT(111016, "原密码错误"),
```

- [ ] **Step 2: 给 `User` 实体 8 个字段加行内注释**

打开 `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/entity/User.java`，把整个类替换为：

```java
package com.aichuangzuo.user.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 u_user。
 * 字段命名严格沿用表 V1.0.0_001 迁移脚本；本类只承载持久化结构，
 * 业务校验在 service 层（如昵称长度、邮箱格式等），不在这里用注解约束。
 */
@Getter
@Setter
@TableName("u_user")
public class User {
    /** 主键ID；雪花之外也兼容 AUTO_INCREMENT，由 MyBatis-Plus 自动填充。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户对外暴露的业务编号，如 U000123；注册时生成，不可变。 */
    private String bizNo;
    /** 用户昵称；1-20 字符，service 层校验。 */
    private String nickname;
    /** 登录邮箱；唯一约束由 uk_u_user_email 保证；改邮箱时同时清空旧 email 验证码缓存。 */
    private String email;
    /** BCrypt 加密后的密码哈希；原始密码不存。 */
    private String passwordHash;
    /** 头像 URL；可为空，UI 层空时回退到昵称首字母。 */
    private String avatarUrl;
    /** 个人邀请码 6 位；唯一约束由 uk_u_user_invite_code 保证；注册时生成。 */
    private String inviteCode;
    /** 用户状态：0-禁用 / 1-正常；禁用时 JwtAuthenticationFilter 仍能解析 token 但登录接口拒绝。 */
    private Integer userStatus;
    /** 邮箱是否验证：0-否 / 1-是；改邮箱成功后置 1。 */
    private Integer emailVerified;
    /** 租户ID；当前统一为 0，预留多租户扩展。 */
    private Long tenantId;
    /** 逻辑删除标记：0-未删 / 1-已删；所有查询自动追加 is_deleted=0 条件。 */
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

- [ ] **Step 3: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project && mvn -pl shared,user/api -am compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/entity/User.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): 错误码 111015/111016 + User 实体字段注释"
```

---

### Task 2: UserMapper#existsByEmail（含 TDD 测试）

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mapper/UserMapper.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/mapper/UserMapperExistsByEmailTest.java`

**Consumes:** User 实体

**Produces:** `UserMapper#existsByEmail(String email, Long excludeUserId)` 存在；含 1 个测试文件验证"排除自己后能查到他人"和"排除自己能正确判定"

- [ ] **Step 1: 写测试**

新建 `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/mapper/UserMapperExistsByEmailTest.java`：

```java
package com.aichuangzuo.user.modules.user.mapper;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class UserMapperExistsByEmailTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldReturnTrueWhenEmailExistsForAnotherUser() {
        User u1 = newUser("exists-a@test.com");
        User u2 = newUser("exists-b@test.com");
        userMapper.insert(u1);
        userMapper.insert(u2);
        // u1 视角看 u2 的邮箱：应该存在
        assertTrue(userMapper.existsByEmail(u2.getEmail(), u1.getId()));
    }

    @Test
    void shouldReturnFalseWhenEmailBelongsToExcludedUser() {
        User u1 = newUser("self@test.com");
        userMapper.insert(u1);
        // 排除自己后，自己邮箱不再算"冲突"
        assertFalse(userMapper.existsByEmail(u1.getEmail(), u1.getId()));
    }

    @Test
    void shouldReturnFalseWhenEmailNotRegistered() {
        assertFalse(userMapper.existsByEmail("nobody@test.com", 0L));
    }

    private User newUser(String email) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname("u");
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(0);
        return u;
    }
}
```

- [ ] **Step 2: 运行测试，预期失败**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project && mvn -pl user/api test -Dtest=UserMapperExistsByEmailTest -q
```

Expected: 编译错误 `cannot find symbol method existsByEmail(...)` 或测试 FAIL

- [ ] **Step 3: 实现 `existsByEmail`**

打开 `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mapper/UserMapper.java`，整个文件替换为：

```java
package com.aichuangzuo.user.modules.auth.mapper;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按邮箱精确查询未被软删的用户。
     *
     * @param email 邮箱（已规范化为小写）
     * @return 命中的 User；无则 null
     */
    @Select("SELECT * FROM u_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    User selectByEmail(String email);

    /**
     * 按邀请码精确查询未被软删的用户。
     *
     * @param inviteCode 6 位邀请码
     * @return 命中的 User；无则 null
     */
    @Select("SELECT * FROM u_user WHERE invite_code = #{inviteCode} AND is_deleted = 0 LIMIT 1")
    User selectByInviteCode(String inviteCode);

    /**
     * 仅更新密码哈希；不触碰其他字段。
     *
     * @param id   用户主键
     * @param hash 新密码的 BCrypt 哈希
     * @return 受影响行数；通常为 1
     */
    @Update("UPDATE u_user SET password_hash = #{hash}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updatePassword(@Param("id") Long id, @Param("hash") String hash);

    /**
     * 判断指定邮箱是否已被他人占用（用于改邮箱时校验冲突）。
     *
     * <p>典型调用：{@code existsByEmail(newEmail, currentUserId)} —
     * 如果返回 true，说明新邮箱已被另一个用户注册，不允许改。
     *
     * <p>传 {@code excludeUserId = 0L} 表示不过滤任何用户，等价于"该邮箱是否被任何账号占用"。
     *
     * @param email         待校验的邮箱
     * @param excludeUserId 排除的用户主键（通常是当前登录用户自己）
     * @return true 表示已被他人占用
     */
    @Select("SELECT COUNT(*) > 0 FROM u_user WHERE email = #{email} AND is_deleted = 0 AND id <> #{excludeUserId}")
    boolean existsByEmail(@Param("email") String email, @Param("excludeUserId") Long excludeUserId);
}
```

- [ ] **Step 4: 运行测试，预期通过**

```bash
mvn -pl user/api test -Dtest=UserMapperExistsByEmailTest -q
```

Expected: Tests run: 3, Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mapper/UserMapper.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/user/mapper/UserMapperExistsByEmailTest.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): UserMapper#existsByEmail + 测试"
```

---

### Task 3: VO + Converter + 3 个 Request DTO（无业务逻辑）

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/vo/UserProfileVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/converter/UserConverter.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/UpdateNicknameRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/UpdateEmailRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/ChangePasswordRequest.java`

**Consumes:** User 实体、MapStruct 1.5.5

**Produces:** 5 个数据类，全部带 Javadoc

- [ ] **Step 1: 创建 `UserProfileVO`**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/vo/UserProfileVO.java`：

```java
package com.aichuangzuo.user.modules.user.vo;

import lombok.Data;

/**
 * 当前登录用户的个人资料视图。
 *
 * <p>对应 GET /api/v1/user/me 和所有更新接口的成功响应。
 *
 * <p>字段说明：
 * <ul>
 *   <li>userId - 业务编号（u_user.biz_no），前端展示用，不暴露数据库主键</li>
 *   <li>nickname - 昵称，可能为空（前端需有兜底）</li>
 *   <li>email - 当前邮箱；前端展示通常脱敏但此处原样返回，由调用方决定</li>
 *   <li>avatarUrl - 头像 URL；空时前端用昵称首字母兜底</li>
 *   <li>emailVerified - 0/1；改邮箱成功后置 1</li>
 * </ul>
 */
@Data
public class UserProfileVO {
    private String userId;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Integer emailVerified;
}
```

- [ ] **Step 2: 创建 `UserConverter`**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/converter/UserConverter.java`：

```java
package com.aichuangzuo.user.modules.user.converter;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import org.mapstruct.Mapper;

/**
 * User 实体 → UserProfileVO 的转换器。
 *
 * <p>纯字段映射，不做脱敏、不做默认值填充 —— 这些由调用方按需处理。
 */
@Mapper(componentModel = "spring")
public interface UserConverter {
    /**
     * 将 User 实体映射为视图对象。
     *
     * @param user 实体（已通过 selectById / 鉴权过滤器加载）
     * @return 视图对象；user 为 null 时返回 null（MapStruct 默认行为）
     */
    UserProfileVO toProfileVO(User user);
}
```

- [ ] **Step 3: 创建 `UpdateNicknameRequest`**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/UpdateNicknameRequest.java`：

```java
package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改昵称请求体。
 *
 * <p>校验规则：trim 后 1-20 字符。空字符串和超长由 Bean Validation 拦截，
 * 业务层还会再 trim 一次（防御 setNickname(" ") 的边缘情况）。
 */
@Data
public class UpdateNicknameRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称长度不能超过 20 个字符")
    private String nickname;
}
```

- [ ] **Step 4: 创建 `UpdateEmailRequest`**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/UpdateEmailRequest.java`：

```java
package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改邮箱请求体。
 *
 * <p>改邮箱需要新邮箱收到验证码，因此除了邮箱格式还要求 6 位验证码。
 * 新邮箱与旧邮箱相同 / 邮箱已被他人注册 / 验证码错误都由 service 层校验。
 */
@Data
public class UpdateEmailRequest {

    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String newEmail;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码为 6 位")
    private String emailCode;
}
```

- [ ] **Step 5: 创建 `ChangePasswordRequest`**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/request/ChangePasswordRequest.java`：

```java
package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求体（已登录状态）。
 *
 * <p>与公开的 reset-password 不同：这里必须提供原密码。
 *
 * <p>长度校验统一放在 service 层（≥6 位且 ≤20 位），不在注解里写死，
 * 方便后续调整密码强度策略时只改 service。
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
```

- [ ] **Step 6: 编译验证**

```bash
mvn -pl user/api -am compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/user/vo/ \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/user/converter/ \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/user/dto/
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): VO/Converter/Request DTO"
```

---

### Task 4: UserProfileService 接口 + EmailCodeServiceImpl#validateEmailCode Javadoc

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/UserProfileService.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java`（只动 Javadoc）

**Consumes:** UserProfileVO、3 个 Request DTO

**Produces:** 接口定义 + 复用方法注释

- [ ] **Step 1: 创建 `UserProfileService` 接口**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/UserProfileService.java`：

```java
package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;

/**
 * 用户个人资料服务：查询与修改当前登录用户的基本信息。
 *
 * <p>所有方法都依赖 {@code SecurityUserContext.getCurrentUserId()} 拿到当前用户，
 * 不接受外部传入 userId，避免越权。
 */
public interface UserProfileService {

    /**
     * 查询当前登录用户的个人资料。
     *
     * @return UserProfileVO
     * @throws com.aichuangzuo.shared.exception.BusinessException USER_NOT_FOUND 当用户不存在或已被删除
     */
    UserProfileVO getMyProfile();

    /**
     * 修改昵称。
     *
     * @param request 新昵称请求（已通过 Bean Validation）
     * @return 更新后的 UserProfileVO
     * @throws com.aichuangzuo.shared.exception.BusinessException USER_NOT_FOUND
     */
    UserProfileVO updateNickname(UpdateNicknameRequest request);

    /**
     * 修改邮箱。需要新邮箱已收到验证码。
     *
     * @param request 新邮箱 + 6 位验证码（已通过 Bean Validation）
     * @return 更新后的 UserProfileVO（email_verified 置 1）
     * @throws com.aichuangzuo.shared.exception.BusinessException EMAIL_CODE_ERROR / EMAIL_ALREADY_EXISTS / EMAIL_SAME_AS_OLD / USER_NOT_FOUND
     */
    UserProfileVO updateEmail(UpdateEmailRequest request);

    /**
     * 修改密码。需要原密码校验通过。
     *
     * <p>成功后不会自动签发新 token —— 客户端继续使用旧 access token，
     * 下次 token 过期时通过 refresh-token 流程拿到新 token。
     *
     * @param request 旧/新/确认密码
     * @throws com.aichuangzuo.shared.exception.BusinessException PASSWORD_INCORRECT / PASSWORD_FORMAT_ERROR / PASSWORD_NOT_MATCH / USER_NOT_FOUND
     */
    void changePassword(ChangePasswordRequest request);
}
```

- [ ] **Step 2: 给 `EmailCodeServiceImpl#validateEmailCode` 加方法级 Javadoc**

打开 `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java`，把 `validateEmailCode` 方法（第 62-71 行）替换为：

```java
    /**
     * 校验邮箱验证码：命中缓存则返回 true 并失效该验证码（防复用），否则返回 false。
     *
     * <p>调用方应根据返回值抛 {@code EMAIL_CODE_ERROR(111003)} 业务异常。
     *
     * <p>注意：本方法不会清掉"发送次数计数"（24h ≤10 次），仅清验证码本身。
     *
     * @param email     接收验证码的邮箱
     * @param emailCode 用户提交的 6 位验证码
     * @return true 表示校验通过（验证码已失效，不可再用）
     */
    @Override
    public boolean validateEmailCode(String email, String emailCode) {
        String key = EMAIL_CODE_PREFIX + email;
        String cachedCode = cacheUtil.get(key);
        if (cachedCode == null) {
            return false;
        }
        cacheUtil.delete(key);
        return cachedCode.equalsIgnoreCase(emailCode);
    }
```

- [ ] **Step 3: 编译验证**

```bash
mvn -pl user/api -am compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/UserProfileService.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): UserProfileService 接口 + validateEmailCode 注释"
```

---

### Task 5: UserProfileServiceImpl#getMyProfile（含 TDD）

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceGetTest.java`

**Consumes:** UserProfileService 接口、UserConverter、UserMapper、SecurityUserContext

**Produces:** UserProfileServiceImpl 骨架 + getMyProfile 方法 + 1 个测试

- [ ] **Step 1: 写测试**

新建 `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceGetTest.java`：

```java
package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServiceGetTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserMapper userMapper;

    @AfterEach
    void clear() {
        SecurityUserContext.clear();
    }

    @Test
    void shouldReturnProfileForCurrentUser() {
        User u = newUser("get-ok@test.com", "Nicky");
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        UserProfileVO vo = userProfileService.getMyProfile();

        assertNotNull(vo);
        assertEquals(u.getBizNo(), vo.getUserId());
        assertEquals("Nicky", vo.getNickname());
        assertEquals("get-ok@test.com", vo.getEmail());
        assertEquals(1, vo.getEmailVerified());
    }

    @Test
    void shouldThrowWhenUserMissing() {
        SecurityUserContext.setCurrentUserId(99999999L);
        assertThrows(BusinessException.class, () -> userProfileService.getMyProfile());
    }

    private User newUser(String email, String nickname) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname(nickname);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(1);
        return u;
    }
}
```

- [ ] **Step 2: 运行测试，预期编译失败**

```bash
mvn -pl user/api test -Dtest=UserProfileServiceGetTest -q
```

Expected: 编译错误 `cannot find symbol class UserProfileService`

- [ ] **Step 3: 实现 Impl 骨架 + getMyProfile**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java`：

```java
package com.aichuangzuo.user.modules.user.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import com.aichuangzuo.user.modules.user.converter.UserConverter;
import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.service.UserProfileService;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户个人资料服务实现。
 *
 * <p>所有方法都从 {@link SecurityUserContext} 拿当前用户 ID，
 * 然后通过 {@link UserMapper} 加载实体做修改。
 *
 * <p>依赖：
 * <ul>
 *   <li>{@link UserMapper} - u_user 表读写</li>
 *   <li>{@link EmailCodeService} - 改邮箱时校验验证码</li>
 *   <li>{@link PasswordEncoder} - 改密码时 BCrypt 加解密</li>
 *   <li>{@link UserConverter} - User → UserProfileVO 映射</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final EmailCodeService emailCodeService;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;

    @Override
    public UserProfileVO getMyProfile() {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        return userConverter.toProfileVO(user);
    }

    @Override
    public UserProfileVO updateNickname(UpdateNicknameRequest request) {
        // 由 Task 6 实现
        throw new UnsupportedOperationException("see Task 6");
    }

    @Override
    public UserProfileVO updateEmail(UpdateEmailRequest request) {
        // 由 Task 7 实现
        throw new UnsupportedOperationException("see Task 7");
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        // 由 Task 8 实现
        throw new UnsupportedOperationException("see Task 8");
    }
}
```

- [ ] **Step 4: 运行测试，预期通过**

```bash
mvn -pl user/api test -Dtest=UserProfileServiceGetTest -q
```

Expected: Tests run: 2, Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceGetTest.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): getMyProfile 实现 + 测试"
```

---

### Task 6: updateNickname 实现（含 TDD）

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceNicknameTest.java`

**Consumes:** User 实体可写字段

**Produces:** 3 个测试（成功 / 空字符串被 Bean Validation 拦截前的 trim 防御 / 用户不存在）

- [ ] **Step 1: 写测试**

新建 `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceNicknameTest.java`：

```java
package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServiceNicknameTest {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserMapper userMapper;

    @AfterEach
    void clear() { SecurityUserContext.clear(); }

    @Test
    void shouldUpdateNicknameSuccessfully() {
        User u = newUser("nick-ok@test.com", "old");
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("  new name  ");
        UserProfileVO vo = userProfileService.updateNickname(req);

        assertEquals("new name", vo.getNickname()); // trim 后存储
        User refreshed = userMapper.selectById(u.getId());
        assertEquals("new name", refreshed.getNickname());
    }

    @Test
    void shouldThrowWhenUserMissing() {
        SecurityUserContext.setCurrentUserId(99999999L);
        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("x");
        assertThrows(BusinessException.class, () -> userProfileService.updateNickname(req));
    }

    private User newUser(String email, String nickname) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname(nickname);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(1);
        return u;
    }
}
```

- [ ] **Step 2: 运行测试，预期失败**

```bash
mvn -pl user/api test -Dtest=UserProfileServiceNicknameTest -q
```

Expected: 测试 FAIL with `UnsupportedOperationException("see Task 6")`

- [ ] **Step 3: 实现 updateNickname**

打开 `UserProfileServiceImpl.java`，把 `updateNickname` 方法替换为：

```java
    /**
     * 修改当前用户的昵称。会 trim 后再写库，避免前后空格污染展示。
     *
     * @param request 新昵称（已通过 Bean Validation，1-20 字符）
     * @return 更新后的视图对象
     * @throws BusinessException USER_NOT_FOUND
     */
    @Override
    public UserProfileVO updateNickname(UpdateNicknameRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        user.setNickname(request.getNickname().trim());
        userMapper.updateById(user);
        log.info("昵称已修改 userId={}", userId);
        return userConverter.toProfileVO(user);
    }
```

- [ ] **Step 4: 运行测试，预期通过**

```bash
mvn -pl user/api test -Dtest=UserProfileServiceNicknameTest -q
```

Expected: Tests run: 2, Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceNicknameTest.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): updateNickname 实现 + 测试"
```

---

### Task 7: updateEmail 实现（含 TDD）

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceEmailTest.java`

**Consumes:** EmailCodeService#validateEmailCode、UserMapper#existsByEmail

**Produces:** 4 个测试（成功 / 验证码错 / 邮箱被他人占用 / 新邮箱与旧邮箱相同）

- [ ] **Step 1: 写测试**

新建 `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceEmailTest.java`：

```java
package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServiceEmailTest {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserMapper userMapper;
    @MockBean private EmailCodeService emailCodeService;

    @AfterEach
    void clear() { SecurityUserContext.clear(); }

    @Test
    void shouldUpdateEmailSuccessfully() {
        User u = newUser("email-old@test.com", 1); // emailVerified=1
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());
        when(emailCodeService.validateEmailCode("email-new@test.com", "000000")).thenReturn(true);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("email-new@test.com");
        req.setEmailCode("000000");
        UserProfileVO vo = userProfileService.updateEmail(req);

        assertEquals("email-new@test.com", vo.getEmail());
        assertEquals(1, vo.getEmailVerified());
        User refreshed = userMapper.selectById(u.getId());
        assertEquals("email-new@test.com", refreshed.getEmail());
    }

    @Test
    void shouldThrowWhenCodeInvalid() {
        User u = newUser("email-a@test.com", 1);
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(false);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("email-b@test.com");
        req.setEmailCode("111111");
        assertThrows(BusinessException.class, () -> userProfileService.updateEmail(req));
    }

    @Test
    void shouldThrowWhenNewEmailAlreadyTaken() {
        User me = newUser("me@test.com", 1);
        User other = newUser("taken@test.com", 1);
        userMapper.insert(me);
        userMapper.insert(other);
        SecurityUserContext.setCurrentUserId(me.getId());
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("taken@test.com");
        req.setEmailCode("000000");
        assertThrows(BusinessException.class, () -> userProfileService.updateEmail(req));
    }

    @Test
    void shouldThrowWhenNewEmailSameAsOld() {
        User u = newUser("same@test.com", 1);
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("same@test.com");
        req.setEmailCode("000000");
        assertThrows(BusinessException.class, () -> userProfileService.updateEmail(req));
    }

    private User newUser(String email, int verified) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname("u");
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(verified);
        return u;
    }
}
```

- [ ] **Step 2: 运行测试，预期失败**

```bash
mvn -pl user/api test -Dtest=UserProfileServiceEmailTest -q
```

Expected: 4 个测试都 FAIL with `UnsupportedOperationException("see Task 7")`

- [ ] **Step 3: 实现 updateEmail**

打开 `UserProfileServiceImpl.java`，把 `updateEmail` 方法替换为：

```java
    /**
     * 修改当前用户的邮箱。
     *
     * <p>流程：
     * <ol>
     *   <li>校验新邮箱收到的验证码（一次性，验证后失效）</li>
     *   <li>不允许新邮箱与旧邮箱相同</li>
     *   <li>新邮箱不能已被他人注册</li>
     *   <li>写入新邮箱并把 email_verified 置 1</li>
     * </ol>
     *
     * @param request 新邮箱 + 6 位验证码
     * @return 更新后的视图对象
     * @throws BusinessException EMAIL_CODE_ERROR / EMAIL_SAME_AS_OLD / EMAIL_ALREADY_EXISTS / USER_NOT_FOUND
     */
    @Override
    public UserProfileVO updateEmail(UpdateEmailRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        String newEmail = request.getNewEmail().trim().toLowerCase();

        if (!emailCodeService.validateEmailCode(newEmail, request.getEmailCode())) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_CODE_ERROR);
        }
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_SAME_AS_OLD);
        }
        if (userMapper.existsByEmail(newEmail, userId)) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_ALREADY_EXISTS);
        }
        user.setEmail(newEmail);
        user.setEmailVerified(1);
        userMapper.updateById(user);
        log.info("邮箱已修改 userId={}, newEmail={}", userId, newEmail);
        return userConverter.toProfileVO(user);
    }
```

- [ ] **Step 4: 运行测试，预期通过**

```bash
mvn -pl user/api test -Dtest=UserProfileServiceEmailTest -q
```

Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServiceEmailTest.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): updateEmail 实现 + 测试"
```

---

### Task 8: changePassword 实现（含 TDD）

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServicePasswordTest.java`

**Consumes:** PasswordEncoder（BCrypt-12，已在 SecurityConfig 注册）

**Produces:** 4 个测试（成功 / 旧密码错 / 新密码太弱 / 两次不一致）

- [ ] **Step 1: 写测试**

新建 `project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServicePasswordTest.java`：

```java
package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServicePasswordTest {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void clear() { SecurityUserContext.clear(); }

    @Test
    void shouldChangePasswordSuccessfully() {
        User u = newUserWithHash("pwd-ok@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old123");
        req.setNewPassword("new123456");
        req.setConfirmPassword("new123456");
        userProfileService.changePassword(req);

        User refreshed = userMapper.selectById(u.getId());
        assertTrue(passwordEncoder.matches("new123456", refreshed.getPasswordHash()));
    }

    @Test
    void shouldThrowWhenOldPasswordWrong() {
        User u = newUserWithHash("pwd-wrong@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("WRONG");
        req.setNewPassword("new123456");
        req.setConfirmPassword("new123456");
        assertThrows(BusinessException.class, () -> userProfileService.changePassword(req));
    }

    @Test
    void shouldThrowWhenNewPasswordTooWeak() {
        User u = newUserWithHash("pwd-weak@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old123");
        req.setNewPassword("123");  // < 6 位
        req.setConfirmPassword("123");
        assertThrows(BusinessException.class, () -> userProfileService.changePassword(req));
    }

    @Test
    void shouldThrowWhenConfirmMismatch() {
        User u = newUserWithHash("pwd-mm@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old123");
        req.setNewPassword("new123456");
        req.setConfirmPassword("different");
        assertThrows(BusinessException.class, () -> userProfileService.changePassword(req));
    }

    private User newUserWithHash(String email, String hash) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname("u");
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(1);
        return u;
    }
}
```

- [ ] **Step 2: 运行测试，预期失败**

```bash
mvn -pl user/api test -Dtest=UserProfileServicePasswordTest -q
```

Expected: 4 个测试都 FAIL with `UnsupportedOperationException("see Task 8")`

- [ ] **Step 3: 实现 changePassword**

打开 `UserProfileServiceImpl.java`，把 `changePassword` 方法替换为：

```java
    /** 密码长度下限。service 层集中校验，方便调整时只改一处。 */
    private static final int MIN_PASSWORD_LENGTH = 6;
    /** 密码长度上限；与 register 接口保持一致。 */
    private static final int MAX_PASSWORD_LENGTH = 20;

    /**
     * 修改当前用户的密码。需要原密码校验通过，新密码长度 ≥6 且 ≤20，新密码两次一致。
     *
     * <p>成功后仅更新密码字段，不签发新 token —— 客户端继续使用旧 access token。
     *
     * @param request 旧/新/确认密码
     * @throws BusinessException PASSWORD_INCORRECT / PASSWORD_FORMAT_ERROR / PASSWORD_NOT_MATCH / USER_NOT_FOUND
     */
    @Override
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_INCORRECT);
        }
        String newPwd = request.getNewPassword();
        if (newPwd.length() < MIN_PASSWORD_LENGTH || newPwd.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_FORMAT_ERROR);
        }
        if (!newPwd.equals(request.getConfirmPassword())) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_NOT_MATCH);
        }
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        log.info("密码已修改 userId={}", userId);
    }
```

- [ ] **Step 4: 运行测试，预期通过**

```bash
mvn -pl user/api test -Dtest=UserProfileServicePasswordTest -q
```

Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 5: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/UserProfileServiceImpl.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/user/service/UserProfileServicePasswordTest.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): changePassword 实现 + 测试"
```

---

### Task 9: UserProfileController

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/controller/UserProfileController.java`

**Consumes:** UserProfileService、3 个 Request DTO

**Produces:** 4 个 REST endpoint，含 Swagger 注解和 Javadoc

- [ ] **Step 1: 创建 Controller**

新建 `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/controller/UserProfileController.java`：

```java
package com.aichuangzuo.user.modules.user.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.service.UserProfileService;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人资料 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/me，鉴权由 SecurityConfig 的
 * {@code .anyRequest().authenticated()} 统一拦截，
 * 所有方法依赖 JwtAuthenticationFilter 把 userId 写入 SecurityUserContext。
 */
@Tag(name = "用户个人资料")
@RestController
@RequestMapping("/api/v1/user/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 获取当前登录用户的个人资料。
     *
     * @return UserProfileVO
     */
    @Operation(summary = "获取我的个人资料")
    @GetMapping
    public Result<UserProfileVO> getMyProfile() {
        return Result.success(userProfileService.getMyProfile());
    }

    /**
     * 修改昵称。
     *
     * @param request 新昵称（1-20 字符）
     * @return 更新后的 UserProfileVO
     */
    @Operation(summary = "修改昵称")
    @PutMapping("/nickname")
    public Result<UserProfileVO> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        return Result.success(userProfileService.updateNickname(request));
    }

    /**
     * 修改邮箱。需要新邮箱已收到验证码。
     *
     * @param request 新邮箱 + 6 位验证码
     * @return 更新后的 UserProfileVO（email_verified 置 1）
     */
    @Operation(summary = "修改邮箱")
    @PutMapping("/email")
    public Result<UserProfileVO> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        return Result.success(userProfileService.updateEmail(request));
    }

    /**
     * 修改密码。需要原密码校验通过。
     *
     * @param request 旧/新/确认密码
     * @return 成功响应（无 data）
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(request);
        return Result.success();
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl user/api -am compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 跑全部 user 模块测试，确保回归**

```bash
mvn -pl user/api test -q
```

Expected: 全部测试 PASS（之前的 auth 模块测试 + 本轮新加的 4 个测试文件 = 至少 13 个测试全绿）

- [ ] **Step 4: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/user/controller/UserProfileController.java
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(user): UserProfileController 4 个 endpoint"
```

---

### Task 10: 前端 api/user.js

**Files:**
- Create: `project/user/web/src/api/user.js`

**Consumes:** Axios 实例（与 auth.js 共用 baseURL `/api/v1/user`）

**Produces:** 4 个 API 调用函数

- [ ] **Step 1: 创建 `api/user.js`**

新建 `project/user/web/src/api/user.js`：

```javascript
import axios from 'axios'

// 复用 auth.js 的 axios 配置（baseURL = '/api/v1/user'，含 JWT 拦截器 + 业务错误转 reject）
// 这里单独引入 axios 仅为了保证文件可独立使用；调用方拿到的也是 Promise<data>
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1/user',
  timeout: 10000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('aichuangzuo_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 获取当前登录用户的个人资料。
 * @returns {Promise<{userId:string, nickname:string, email:string, avatarUrl:string|null, emailVerified:number}>}
 */
export function getMyProfile() {
  return api.get('/me')
}

/**
 * 修改昵称。
 * @param {string} nickname 新昵称，1-20 字符
 */
export function updateNickname(nickname) {
  return api.put('/me/nickname', { nickname })
}

/**
 * 修改邮箱（需要新邮箱收到的验证码）。
 * @param {string} newEmail
 * @param {string} emailCode 6 位验证码
 */
export function updateEmail(newEmail, emailCode) {
  return api.put('/me/email', { newEmail, emailCode })
}

/**
 * 修改密码（已登录状态，需原密码）。
 * @param {{oldPassword:string, newPassword:string, confirmPassword:string}} payload
 */
export function changePassword(payload) {
  return api.put('/me/password', payload)
}
```

- [ ] **Step 2: 提交**

```bash
git add project/user/web/src/api/user.js
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(web): api/user.js 4 个 endpoint"
```

---

### Task 11: 前端 useUserProfile composable

**Files:**
- Create: `project/user/web/src/composables/useUserProfile.js`

**Consumes:** api/user.js 4 个函数、ant-design-vue message

**Produces:** 单例式 composable（4 个函数 + 1 个 ref）

- [ ] **Step 1: 创建 composable**

新建 `project/user/web/src/composables/useUserProfile.js`：

```javascript
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getMyProfile,
  updateNickname,
  updateEmail,
  changePassword
} from '@/api/user'

// 模块级 ref：单例模式，整个 console 共享一份 profile。
// 不再额外包装 store，因为只有 ConsoleLayout 一处使用。
const profile = ref(null)
const loading = ref(false)

/** 从异常负载里取出可读 message；兼容多种错误结构。 */
function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

/**
 * 用户个人资料 composable。
 * 提供全局共享的 profile 引用 + 4 个修改方法（成功本地更新，失败弹 message）。
 */
export function useUserProfile() {

  async function loadProfile() {
    loading.value = true
    try {
      const res = await getMyProfile()
      profile.value = res.data || res  // auth.js 拦截器已剥外层，res 即 data
    } catch (e) {
      // 401 时拦截器会跳登录页，其他错误静默不打扰
      console.warn('loadProfile 失败', errMsg(e))
    } finally {
      loading.value = false
    }
  }

  async function saveNickname(nickname) {
    await updateNickname(nickname)
    if (profile.value) profile.value.nickname = nickname.trim()
    message.success('昵称已更新')
  }

  async function saveEmail(newEmail, emailCode) {
    await updateEmail(newEmail, emailCode)
    await loadProfile()  // 服务端把 email_verified 置 1，重新拉一份
    message.success('邮箱已更新')
  }

  async function savePassword(payload) {
    await changePassword(payload)
    message.success('密码已修改，请重新登录')
    // 密码已改 → 强制下次走 refresh-token 流程；前端只提示，不立即清 token
  }

  return {
    profile,
    loading,
    loadProfile,
    saveNickname,
    saveEmail,
    savePassword
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add project/user/web/src/composables/useUserProfile.js
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(web): useUserProfile composable"
```

---

### Task 12: ConsoleLayout.vue 集成（接真实数据 + 替换 console.log）

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`

**Consumes:** useUserProfile、ant-design-vue message

**Produces:**
- `onMounted` 调 `loadProfile`
- `provide('consoleActions')` 中的 `profileForm`/`emailForm` 改为派生自 `profile`
- 弹框"用户ID 88886666"和"本月已生成 12 篇"硬编码替换为真实
- profileModal 的 `handleProfileSubmit` 改成 `saveNickname` 调用
- emailModal 的 `handleEmailSubmit` 改成 `saveEmail` 调用
- 旧的 `console.log` 全部删除

- [ ] **Step 1: import useUserProfile 和 save* 函数**

打开 `ConsoleLayout.vue`，找到现有的 `import` 块（位于 script setup 顶部），在合适位置加：

```javascript
import { useUserProfile } from '@/composables/useUserProfile'
```

- [ ] **Step 2: 拿到 composable 实例**

在 setup 内（紧跟 `const router = useRouter()` 之后）加：

```javascript
const userProfile = useUserProfile()
```

- [ ] **Step 3: 把硬编码的 profileForm/emailForm 初始化改为派生**

找到 `const profileForm = reactive({...})`（约第 1145 行），把它替换为：

```javascript
const profileForm = reactive({
  get nickname() { return userProfile.profile.value?.nickname ?? '' },
  set nickname(v) { userProfile.profile.value && (userProfile.profile.value.nickname = v) }
})
```

找到 `const emailForm = reactive({...})`（紧随其后），替换为：

```javascript
const emailForm = reactive({
  get email() { return userProfile.profile.value?.email ?? '' },
  set email(v) { userProfile.profile.value && (userProfile.profile.value.email = v) },
  code: ''
})
```

> 用 getter/setter 而非 `computed().value`：computed 在 reactive 里只读，而这两个表单字段需要双向绑定。

- [ ] **Step 4: 把 "用户ID 88886666" 和 "本月已生成 12 篇" 硬编码替换**

模板中找到：
```html
<span class="user-row-value">88886666</span>
```
替换为：
```html
<span class="user-row-value">{{ userProfile.profile.value?.userId || '—' }}</span>
```

模板中找到：
```html
<span class="user-row-value">12 篇</span>
```
替换为：
```html
<span class="user-row-value">{{ monthlyWorks }} 篇</span>
```

⚠️ `monthlyWorks` 来自 MineIndex.vue 的 localStorage 计算；本文件还没有该变量。在 setup 末尾加：

```javascript
// 本月已生成：复用 MineIndex 的 localStorage 算法
const monthlyWorks = ref(0)
function readMonthlyWorks() {
  try {
    const raw = localStorage.getItem('aichuangzuo_generation_queue')
    if (!raw) return 0
    const list = JSON.parse(raw)
    if (!Array.isArray(list)) return 0
    const now = new Date()
    const ymKey = `${now.getFullYear()}-${now.getMonth()}`
    return list.filter((item) => {
      if (item.status !== 'completed') return false
      const t = item.completedAt || item.updatedAt || item.createdAt
      if (!t) return false
      const d = new Date(t)
      return `${d.getFullYear()}-${d.getMonth()}` === ymKey
    }).length
  } catch { return 0 }
}
monthlyWorks.value = readMonthlyWorks()
```

- [ ] **Step 5: 替换 profileModal 的 handleProfileSubmit（删 console.log）**

找到 `const handleProfileSubmit = () => { ... console.log('修改昵称', ...) ... }`（约第 1182 行），整个函数替换为：

```javascript
const handleProfileSubmit = async () => {
  if (!profileForm.nickname.trim()) {
    message.warning('昵称不能为空')
    return
  }
  try {
    await userProfile.saveNickname(profileForm.nickname)
    profileVisible.value = false
  } catch (e) {
    message.error(e.message || '昵称保存失败')
  }
}
```

- [ ] **Step 6: 替换 emailModal 的 handleEmailSubmit（删 console.log）**

找到 `const handleEmailSubmit = () => { ... console.log('修改邮箱', ...) ... }`（约第 1188 行），整个函数替换为：

```javascript
const handleEmailSubmit = async () => {
  if (!emailForm.email.trim() || !emailForm.code.trim()) {
    message.warning('邮箱和验证码不能为空')
    return
  }
  try {
    await userProfile.saveEmail(emailForm.email, emailForm.code)
    emailVisible.value = false
    emailForm.code = ''
  } catch (e) {
    message.error(e.message || '邮箱保存失败')
  }
}
```

- [ ] **Step 7: onMounted 触发 loadProfile**

找到 `onMounted(...)` 钩子（如果已有），在内部追加：

```javascript
userProfile.loadProfile()
```

如果文件里目前没有 `onMounted`，在 setup 末尾加：

```javascript
import { onMounted } from 'vue'
// ...
onMounted(() => {
  userProfile.loadProfile()
})
```

> 注意：`import` 语句应合并到文件顶部的 import 块，不要新建第二个 import。

- [ ] **Step 8: 验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && grep -n "console.log" src/views/console/ConsoleLayout.vue | grep -E "修改昵称|修改邮箱"
```

Expected: 无匹配（旧的 console.log 已删干净）

```bash
grep -nE "88886666|>12 篇<" src/views/console/ConsoleLayout.vue
```

Expected: 无匹配（硬编码已替换）

- [ ] **Step 9: 提交**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue
git -c user.email=panyong@aicloud.com -c user.name=panyong commit -m "feat(web): ConsoleLayout 接入真实用户数据 + 弹框改调 API"
```

---

### Task 13: E2E 验证（curl + 手测）

**Files:** 无（只验证）

- [ ] **Step 1: 启动后端 + 前端**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/scripts/local/user-full-stack && ./start.sh
```

Expected: 后端 PID 启动、端口 25050 监听；前端端口 22345

- [ ] **Step 2: 用 curl 测 4 个 endpoint**

先登录拿 token：
```bash
curl -sS -X POST http://localhost:25050/api/v1/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"py_world@163.com","password":"123456"}' | tee /tmp/login.json
```

Expected: `{"code":0,"data":{"accessToken":"...","user":{...}}}`

提取 token：
```bash
TOKEN=$(python3 -c "import json; print(json.load(open('/tmp/login.json'))['data']['accessToken'])")
```

a. **GET /me**：
```bash
curl -sS http://localhost:25050/api/v1/user/me -H "Authorization: Bearer $TOKEN"
```

Expected: 返回 UserProfileVO，`email=py_world@163.com`

b. **PUT /me/nickname**：
```bash
curl -sS -X PUT http://localhost:25050/api/v1/user/me/nickname \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"测试昵称"}'
```

Expected: `{"code":0,"data":{"nickname":"测试昵称",...}}`

c. **PUT /me/email**（跳过，需要真实邮箱收验证码，本步骤标为可选）：
```bash
# 1) 触发发码到 new@example.com
curl -sS -X POST http://localhost:25050/api/v1/user/auth/email-codes \
  -H "Content-Type: application/json" \
  -d '{"email":"new@example.com","captchaKey":"x","captchaCode":"TEST12"}'
# 2) 由于没有真实邮箱拿到验证码，本轮跳过端到端验证；service 层 4 个单元测试已覆盖
```

d. **PUT /me/password**：
```bash
curl -sS -X PUT http://localhost:25050/api/v1/user/me/password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"123456","newPassword":"abcdef","confirmPassword":"abcdef"}'
```

Expected: `{"code":0,"data":null}`

e. **改密后旧密码登录失败**：
```bash
curl -sS -X POST http://localhost:25050/api/v1/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"py_world@163.com","password":"123456"}'
```

Expected: `code=111004`（账号或密码错误）

f. **新密码登录成功**：
```bash
curl -sS -X POST http://localhost:25050/api/v1/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"py_world@163.com","password":"abcdef"}'
```

Expected: `code=0`

g. **改回原密码**（避免影响后续开发）：
```bash
TOKEN2=$(python3 -c "import json,sys; print(json.load(open('/tmp/login.json'))['data']['accessToken'])" 2>/dev/null || echo "")
NEW_TOKEN=$(curl -sS -X POST http://localhost:25050/api/v1/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"py_world@163.com","password":"abcdef"}' | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['accessToken'])")
curl -sS -X PUT http://localhost:25050/api/v1/user/me/password \
  -H "Authorization: Bearer $NEW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"abcdef","newPassword":"123456","confirmPassword":"123456"}'
```

Expected: `{"code":0,"data":null}`

- [ ] **Step 3: 浏览器手测**

1. 访问 http://localhost:22345/login（局域网 IP 同理）
2. 登录 py_world@163.com / 123456
3. 进入 console
4. 点 header 头像 → 弹框里"用户ID"显示真实 bizNo（如 U000xxx），"本月已生成"显示 0
5. 点"昵称"行 → 弹框 → 改昵称 → 保存 → 弹框关闭，header 头像派生字符更新
6. （改邮箱跳过，邮箱收码本机不便演示）
7. 点"修改密码" → 旧 123456 / 新 xxx / 确认 xxx → 保存 → 提示成功
8. 登出 → 用新密码登录成功 → 用旧密码登录失败
9. 移动端：访问 /console/mine（或切到移动 tab），用户卡显示真实昵称/邮箱

- [ ] **Step 4: 全测试回归**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project && mvn -pl user/api test -q
```

Expected: 全绿（auth 模块原有测试 + user 模块新加 14 个测试）

- [ ] **Step 5: 提交（如有自动产生的 lock 文件）**

```bash
git status
```

如果有未跟踪的 .pid / logs 文件，应已在 .gitignore 中，跳过。

---

## 验证清单（review 时勾选）

- [ ] 后端：4 个 endpoint 在 Swagger UI（http://localhost:25050/doc.html）可见
- [ ] 后端：`mvn test` 全绿
- [ ] 后端：错误码 111015 / 111016 在 UserAuthErrorCode 中存在
- [ ] 后端：User 实体字段注释完整
- [ ] 后端：UserProfileController / Service / Impl / VO / DTO / Converter 都有 Javadoc
- [ ] 后端：EmailCodeServiceImpl#validateEmailCode 有方法级 Javadoc
- [ ] 前端：api/user.js 4 个函数存在
- [ ] 前端：useUserProfile composable 4 个函数 + profile ref
- [ ] 前端：ConsoleLayout 弹框用户ID/本月已生成显示真实/派生
- [ ] 前端：handleProfileSubmit / handleEmailSubmit 无 console.log，调 save* API
- [ ] 手测：登录 → 改昵称 → 弹框同步更新 → 改密码 → 旧密码失败/新密码成功
- [ ] 无残留调试代码：grep "console.log" 在提交中无新增
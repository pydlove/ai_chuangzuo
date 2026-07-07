# 管理端注册用户管理实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 admin/api 中实现注册用户列表、详情、状态切换、重置密码接口，并替换 admin/web 前端 mock 实现完成联调。

**Architecture:** admin/api 直接查询共用 MySQL 中的 `u_user` 与 `u_user_login_log` 表；接口层校验当前管理员是否拥有 `SUPER_ADMIN` 角色；Service 层完成分页查询、状态映射、密码加密与最近登录时间补全。

**Tech Stack:** Spring Boot 3.2.5, MyBatis-Plus 3.5.8, Spring Security (BCrypt), Vue 3 + Vite + Axios, JUnit 5 + Mockito

## Global Constraints

- JDK 17
- 不新增外部中间件
- admin/api 与 user/api 共用同一 MySQL 实例 `aichuangzuo`
- 用户表结构以 `u_user` 迁移脚本为准
- 错误码沿用项目枚举规范
- 所有 admin 接口以 `/api/v1/admin` 为前缀
- 单元测试需命中真实逻辑，不绕过校验

---

## 文件结构

新增文件：

- `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminUserErrorCode.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/entity/PlatformUser.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/entity/PlatformUserLoginLog.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/mapper/PlatformUserMapper.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/mapper/PlatformUserLoginLogMapper.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/mapper/RoleMapper.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/mapper/AdminUserRoleRelMapper.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/service/AdminUserPermissionService.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/service/impl/AdminUserPermissionServiceImpl.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/dto/request/AdminUserStatusRequest.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/AdminUserVO.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/AdminUserPageVO.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/AdminUserResetPasswordVO.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/service/AdminUserService.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/service/impl/AdminUserServiceImpl.java`
- `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/controller/AdminUserController.java`
- `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/user/service/AdminUserServiceTest.java`

修改文件：

- `project/admin/web/src/api/user.js`

---

### Task 1: 定义用户管理错误码

**Files:**
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminUserErrorCode.java`

**Interfaces:**
- Produces: `AdminUserErrorCode` 枚举，实现 `ErrorCode`

- [ ] **Step 1: 创建错误码枚举**

```java
package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminUserErrorCode implements ErrorCode {
    USER_NOT_FOUND(220001, "用户不存在"),
    NO_PERMISSION(220002, "无权限访问"),
    STATUS_INVALID(220003, "状态参数非法");

    private final int code;
    private final String message;

    AdminUserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: 编译 shared 模块**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/shared
mvn clean install -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminUserErrorCode.java
git commit -m "feat(shared): add admin user management error codes"
```

---

### Task 2: 创建用户端实体（admin 模块视角）

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/entity/PlatformUser.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/entity/PlatformUserLoginLog.java`

**Interfaces:**
- Produces: `PlatformUser` 映射 `u_user`，`PlatformUserLoginLog` 映射 `u_user_login_log`

- [ ] **Step 1: 创建 PlatformUser 实体**

```java
package com.aichuangzuo.admin.modules.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user")
public class PlatformUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizNo;
    private String nickname;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    private String inviteCode;
    private Integer userStatus;
    private Integer emailVerified;
    private Long tenantId;
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

- [ ] **Step 2: 创建 PlatformUserLoginLog 实体**

```java
package com.aichuangzuo.admin.modules.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user_login_log")
public class PlatformUserLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer loginType;
    private String clientIp;
    private String userAgent;
    private Integer loginStatus;
    private String failReason;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/entity/
git commit -m "feat(admin): add platform user and login log entities"
```

---

### Task 3: 创建 Mapper 并支持最近登录时间查询

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/mapper/PlatformUserMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/mapper/PlatformUserLoginLogMapper.java`

**Interfaces:**
- Consumes: `PlatformUser`, `PlatformUserLoginLog`
- Produces: `selectLastLoginAtByUserId(Long userId) -> LocalDateTime`

- [ ] **Step 1: 创建 PlatformUserMapper**

```java
package com.aichuangzuo.admin.modules.user.mapper;

import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlatformUserMapper extends BaseMapper<PlatformUser> {
}
```

- [ ] **Step 2: 创建 PlatformUserLoginLogMapper**

```java
package com.aichuangzuo.admin.modules.user.mapper;

import com.aichuangzuo.admin.modules.user.entity.PlatformUserLoginLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface PlatformUserLoginLogMapper extends BaseMapper<PlatformUserLoginLog> {

    @Select("SELECT created_at FROM u_user_login_log WHERE user_id = #{userId} AND login_status = 1 ORDER BY created_at DESC LIMIT 1")
    LocalDateTime selectLastLoginAtByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/mapper/
git commit -m "feat(admin): add platform user mappers"
```

---

### Task 4: 实现 SUPER_ADMIN 权限校验

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/mapper/RoleMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/mapper/AdminUserRoleRelMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/service/AdminUserPermissionService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/service/impl/AdminUserPermissionServiceImpl.java`

**Interfaces:**
- Consumes: `a_role`, `a_admin_user_role_rel`
- Produces: `boolean isSuperAdmin(Long adminUserId)`

- [ ] **Step 1: 创建 RoleMapper**

```java
package com.aichuangzuo.admin.modules.auth.mapper;

import com.aichuangzuo.admin.modules.auth.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT * FROM a_role WHERE role_code = #{roleCode} AND is_deleted = 0 LIMIT 1")
    Role selectByRoleCode(@Param("roleCode") String roleCode);
}
```

- [ ] **Step 2: 创建 AdminUserRoleRelMapper**

```java
package com.aichuangzuo.admin.modules.auth.mapper;

import com.aichuangzuo.admin.modules.auth.entity.AdminUserRoleRel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserRoleRelMapper extends BaseMapper<AdminUserRoleRel> {

    @Select("SELECT COUNT(*) > 0 FROM a_admin_user_role_rel WHERE admin_user_id = #{adminUserId} AND role_id = #{roleId}")
    boolean existsByAdminUserIdAndRoleId(@Param("adminUserId") Long adminUserId, @Param("roleId") Long roleId);
}
```

- [ ] **Step 3: 创建权限服务接口与实现**

```java
package com.aichuangzuo.admin.modules.auth.service;

public interface AdminUserPermissionService {
    boolean isSuperAdmin(Long adminUserId);
}
```

```java
package com.aichuangzuo.admin.modules.auth.service.impl;

import com.aichuangzuo.admin.modules.auth.entity.Role;
import com.aichuangzuo.admin.modules.auth.mapper.AdminUserRoleRelMapper;
import com.aichuangzuo.admin.modules.auth.mapper.RoleMapper;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserPermissionServiceImpl implements AdminUserPermissionService {

    private final RoleMapper roleMapper;
    private final AdminUserRoleRelMapper adminUserRoleRelMapper;

    @Override
    public boolean isSuperAdmin(Long adminUserId) {
        if (adminUserId == null) {
            return false;
        }
        Role role = roleMapper.selectByRoleCode("SUPER_ADMIN");
        if (role == null) {
            return false;
        }
        return adminUserRoleRelMapper.existsByAdminUserIdAndRoleId(adminUserId, role.getId());
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/mapper/RoleMapper.java project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/mapper/AdminUserRoleRelMapper.java project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/service/ project/admin/api/src/main/java/com/aichuangzuo/admin/modules/auth/service/impl/
git commit -m "feat(admin): add SUPER_ADMIN permission check"
```

---

### Task 5: 创建 DTO/VO

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/dto/request/AdminUserStatusRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/AdminUserVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/AdminUserPageVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/AdminUserResetPasswordVO.java`

**Interfaces:**
- Produces: `AdminUserStatusRequest`, `AdminUserVO`, `AdminUserPageVO`, `AdminUserResetPasswordVO`

- [ ] **Step 1: 创建 AdminUserStatusRequest**

```java
package com.aichuangzuo.admin.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserStatusRequest {
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "enabled|disabled", message = "状态只能是 enabled 或 disabled")
    private String status;
}
```

- [ ] **Step 2: 创建 AdminUserVO**

```java
package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String account;
    private String email;
    private String nickname;
    private String status;
    private String inviteCode;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
```

- [ ] **Step 3: 创建 AdminUserPageVO**

```java
package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserPageVO {
    private List<AdminUserVO> list;
    private long total;
}
```

- [ ] **Step 4: 创建 AdminUserResetPasswordVO**

```java
package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

@Data
public class AdminUserResetPasswordVO {
    private String newPassword;
}
```

- [ ] **Step 5: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/dto/ project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/
git commit -m "feat(admin): add admin user management DTOs and VOs"
```

---

### Task 6: 实现 AdminUserService

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/service/AdminUserService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/service/impl/AdminUserServiceImpl.java`

**Interfaces:**
- Consumes: `PlatformUserMapper`, `PlatformUserLoginLogMapper`, `PasswordEncoder`
- Produces: `AdminUserPageVO listUsers(...)`, `AdminUserVO getUser(Long)`, `void updateStatus(Long, AdminUserStatusRequest)`, `AdminUserResetPasswordVO resetPassword(Long)`

- [ ] **Step 1: 创建 AdminUserService 接口**

```java
package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;

public interface AdminUserService {
    AdminUserPageVO listUsers(String keyword, int page, int pageSize);
    AdminUserVO getUser(Long id);
    void updateStatus(Long id, AdminUserStatusRequest request);
    AdminUserResetPasswordVO resetPassword(Long id);
}
```

- [ ] **Step 2: 创建 AdminUserServiceImpl 实现**

```java
package com.aichuangzuo.admin.modules.user.service.impl;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final PlatformUserMapper platformUserMapper;
    private final PlatformUserLoginLogMapper platformUserLoginLogMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String RESET_PASSWORD = "adc123456";

    @Override
    public AdminUserPageVO listUsers(String keyword, int page, int pageSize) {
        Page<PlatformUser> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformUser::getIsDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PlatformUser::getEmail, kw)
                    .or()
                    .like(PlatformUser::getNickname, kw)
                    .or()
                    .like(PlatformUser::getInviteCode, kw));
        }
        wrapper.orderByDesc(PlatformUser::getCreatedAt);
        Page<PlatformUser> result = platformUserMapper.selectPage(pageParam, wrapper);
        List<AdminUserVO> list = result.getRecords().stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());
        AdminUserPageVO vo = new AdminUserPageVO();
        vo.setList(list);
        vo.setTotal(result.getTotal());
        return vo;
    }

    @Override
    public AdminUserVO getUser(Long id) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        return toAdminUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, AdminUserStatusRequest request) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        int status = "enabled".equals(request.getStatus()) ? 1 : 0;
        user.setUserStatus(status);
        platformUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResetPasswordVO resetPassword(Long id) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        user.setPasswordHash(passwordEncoder.encode(RESET_PASSWORD));
        platformUserMapper.updateById(user);
        AdminUserResetPasswordVO vo = new AdminUserResetPasswordVO();
        vo.setNewPassword(RESET_PASSWORD);
        return vo;
    }

    private AdminUserVO toAdminUserVO(PlatformUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setAccount(user.getEmail());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getUserStatus() == 1 ? "enabled" : "disabled");
        vo.setInviteCode(user.getInviteCode());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(platformUserLoginLogMapper.selectLastLoginAtByUserId(user.getId()));
        return vo;
    }
}
```

- [ ] **Step 3: 编写 Service 单元测试**

Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/user/service/AdminUserServiceTest.java`

```java
package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.service.impl.AdminUserServiceImpl;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Mock
    private PlatformUserLoginLogMapper platformUserLoginLogMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void listUsers_shouldReturnPage() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setNickname("test");
        user.setInviteCode("ABC123");
        user.setUserStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsDeleted(0);

        Page<PlatformUser> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(user));
        page.setTotal(1);

        when(platformUserMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());

        AdminUserPageVO result = adminUserService.listUsers("", 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("user@example.com", result.getList().get(0).getAccount());
        assertEquals("enabled", result.getList().get(0).getStatus());
    }

    @Test
    void getUser_shouldThrowWhenNotFound() {
        when(platformUserMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.getUser(999L));
        assertEquals(AdminUserErrorCode.USER_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void resetPassword_shouldReturnFixedPassword() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setIsDeleted(0);
        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("adc123456")).thenReturn("hashed");

        AdminUserResetPasswordVO result = adminUserService.resetPassword(1L);

        assertEquals("adc123456", result.getNewPassword());
        verify(platformUserMapper).updateById(user);
        assertEquals("hashed", user.getPasswordHash());
    }
}
```

- [ ] **Step 4: 运行测试**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api
mvn test -Dtest=AdminUserServiceTest
```

Expected: Tests run: 3, Failures: 0

- [ ] **Step 5: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/service/ project/admin/api/src/test/java/com/aichuangzuo/admin/modules/user/service/
git commit -m "feat(admin): implement admin user service with tests"
```

---

### Task 7: 实现 AdminUserController

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/controller/AdminUserController.java`

**Interfaces:**
- Consumes: `AdminUserService`, `AdminUserPermissionService`, `SecurityAdminContext`
- Produces: REST endpoints `/api/v1/admin/users/**`

- [ ] **Step 1: 创建 Controller**

```java
package com.aichuangzuo.admin.modules.user.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端用户管理")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询用户列表")
    @GetMapping
    public Result<AdminUserPageVO> listUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        checkSuperAdmin();
        return Result.success(adminUserService.listUsers(keyword, page, pageSize));
    }

    @Operation(summary = "查看用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> getUser(@PathVariable Long id) {
        checkSuperAdmin();
        return Result.success(adminUserService.getUser(id));
    }

    @Operation(summary = "修改用户状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody AdminUserStatusRequest request) {
        checkSuperAdmin();
        adminUserService.updateStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public Result<AdminUserResetPasswordVO> resetPassword(@PathVariable Long id) {
        checkSuperAdmin();
        return Result.success(adminUserService.resetPassword(id));
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
```

- [ ] **Step 2: 编译 admin-api**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/controller/AdminUserController.java
git commit -m "feat(admin): add admin user management controller"
```

---

### Task 8: 替换前端 mock API

**Files:**
- Modify: `project/admin/web/src/api/user.js`

**Interfaces:**
- Consumes: `request` (axios wrapper)
- Produces: `listUsers`, `getUser`, `updateUserStatus`, `resetUserPassword`

- [ ] **Step 1: 重写 user.js**

```javascript
import request from '@/utils/request.js'

export function listUsers(params = {}) {
  return request.get('/api/v1/admin/users', { params }).then((res) => res.data)
}

export function getUser(id) {
  return request.get(`/api/v1/admin/users/${id}`).then((res) => res.data)
}

export function updateUserStatus(id, status) {
  return request.patch(`/api/v1/admin/users/${id}/status`, { status })
}

export function resetUserPassword(id) {
  return request.post(`/api/v1/admin/users/${id}/reset-password`).then((res) => res.data)
}
```

- [ ] **Step 2: Commit**

```bash
git add project/admin/web/src/api/user.js
git commit -m "feat(admin-web): replace mock user api with real endpoints"
```

---

### Task 9: 联调验证

**Files:**
- 无代码改动，使用脚本与 curl 验证

- [ ] **Step 1: 启动 admin 全栈**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo
bash scripts/local/admin-full-stack/start.sh
```

Expected: 后端启动在 26060，前端启动在 22346。

- [ ] **Step 2: 登录获取 token**

Run:
```bash
curl -s -X POST http://localhost:26060/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Root1qaz!QAZ"}'
```

Expected: 返回 `code: 0`，`data.accessToken` 非空。

- [ ] **Step 3: 测试用户列表接口**

Replace `<TOKEN>` with the access token:

```bash
curl -s "http://localhost:26060/api/v1/admin/users?page=1&pageSize=10" \
  -H "Authorization: Bearer <TOKEN>"
```

Expected: `code: 0`，`data.list` 为用户数组，`data.total >= 0`。

- [ ] **Step 4: 测试状态切换与重置密码**

```bash
curl -s -X PATCH "http://localhost:26060/api/v1/admin/users/1/status" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"status":"disabled"}'

curl -s -X POST "http://localhost:26060/api/v1/admin/users/1/reset-password" \
  -H "Authorization: Bearer <TOKEN>"
```

Expected: 均返回 `code: 0`；重置密码返回 `data.newPassword = "adc123456"`。

- [ ] **Step 5: 浏览器验证前端页面**

Open http://localhost:22346/login，使用 admin/Root1qaz!QAZ 登录，进入"用户管理"页面，确认：
- 表格加载出真实用户数据
- 搜索、分页正常
- 禁用/启用按钮可切换状态
- 重置密码弹框显示 `adc123456`
- 查看详情抽屉正常展示

- [ ] **Step 6: 停止服务**

Run:
```bash
bash scripts/local/admin-full-stack/stop.sh
```

- [ ] **Step 7: Commit 验证截图/日志（可选）**

如有新增测试截图或脚本，按项目规范提交。

---

## Self-Review

**1. Spec coverage:**
- 列表查询：Task 6 listUsers ✅
- 用户详情：Task 6 getUser + Task 7 GET /{id} ✅
- 状态切换：Task 6 updateStatus + Task 7 PATCH /{id}/status ✅
- 重置密码：Task 6 resetPassword + Task 7 POST /{id}/reset-password ✅
- SUPER_ADMIN 权限：Task 4 + Task 7 checkSuperAdmin ✅
- 前端联调：Task 8 ✅
- 搜索字段（邮箱/昵称/邀请码）：Task 6 listUsers wrapper ✅

**2. Placeholder scan:**
- 无 TBD/TODO/"后续"/"适当"等模糊描述。
- 所有代码块包含完整可运行代码。

**3. Type consistency:**
- `listUsers` 返回 `AdminUserPageVO`，前端 `.then((res) => res.data)` 解出 `AdminUserPageVO`。
- `status` 字符串 `enabled`/`disabled` 在 Service 内映射为 `1`/`0`。
- `account` 字段由 `email` 填充。
- 无命名冲突。

**4. Risk note:**
- `SecurityAdminContext` 在请求结束后由 `JwtAuthenticationFilter` 清理，Controller 中读取安全。
- 重置密码明文返回给管理员，与需求一致。

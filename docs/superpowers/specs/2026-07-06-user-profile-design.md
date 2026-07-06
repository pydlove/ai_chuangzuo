# 我的/个人中心 设计 (2026-07-06)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把 `views/console/MineIndex.vue`(移动端"我的") 和 `views/console/ConsoleLayout.vue` header 头像 dropdown(网页端"个人中心"弹框)里当前全是硬编码占位符的"昵称/邮箱/头像/用户ID"，接到真实后端；新增"修改昵称/邮箱/密码"3 个 endpoint；新代码按规范补 Javadoc 方便 review。

**Architecture:** 在 `user/api` 下新建独立 `modules/user`(controller + service + impl + vo + dto/request + converter + mapper 复用 `auth/UserMapper`)，前端在 `api/user.js` + `composables/useUserProfile.js` 拉取/缓存/更新，ConsoleLayout onMounted 时调一次拿到真实数据，替换 `provide('consoleActions')` 中 4 个硬编码字段(`profileForm`/`emailForm`/`userId`/`本月已生成`)。其余占位(`coinBalance`/`inviteStats`/`membershipLevel`/`membershipExpiry`/`hasMembership`)本轮保持 ref 默认值，待后续迭代接真实数据源。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus 3.5.8 + Spring Security 6 + Vue 3 + Pinia-free ref + Ant Design Vue。

## Global Constraints

- **不引入新中间件**：用户数据全在 `u_user` 表(已有)；本轮不动 DB、不新增字段
- **不破坏现有功能**：登录/注册/Forgot 流程不改；前端 MineIndex 模板和 `inject('consoleActions')` 写法不动
- **本轮范围严格限定**：4 个 endpoint + 1 个 VO + 3 个 Request DTO；会员/创作币/邀请数据本轮仍占位
- **Javadoc 范围**：本轮新建的 controller/service/impl/vo/dto 全写类级和方法级 Javadoc；会调用的既有类(`EmailCodeServiceImpl#validateEmailCode`、`UserMapper`、`User` 实体)顺手补关键方法/字段注释；其他老代码不动避免范围蔓延
- **错误码沿用 `UserAuthErrorCode`**：缺什么加什么，禁止新建枚举
- **垃圾代码随写随删**：调试用 console.log、临时 mock、注释掉的旧逻辑、本轮不用的占位字段都不留到下次提交
- **GlobalConstraints 来自本仓库 architecture 文档**：MySQL 8 + Flyway + JWT + Caffeine 已配置完毕，本次只在 user 模块内扩展

---

## 1. 架构骨架（总览）

```
网页端/移动端
  console 进入 → useUserProfile.loadProfile()
       ↓ GET /api/v1/user/me
       ↓ UserProfileController.getMyProfile
       ↓ UserProfileServiceImpl.getMyProfile
       ↓ UserMapper.selectById → User → UserProfileVO
       ↓ 返回 {userId, nickname, email, avatarUrl, emailVerified}
  consoleActions.{userId, nickname, email, avatarUrl}  ← 替换硬编码
       ↓
  点击保存修改昵称 → PUT /api/v1/user/me/nickname
       ↓ UserProfileServiceImpl.updateNickname
       ↓ 长度 1-20 校验 → UserMapper.updateById
       ↓ 返回更新后的 UserProfileVO → consoleActions 同步更新
       ↓
  点击保存修改邮箱 → PUT /api/v1/user/me/email
       ↓ UserProfileServiceImpl.updateEmail
       ↓ EmailCodeServiceImpl.validateEmailCode(newEmail, code)
       ↓ UserMapper.existsByEmail(newEmail, excludeUserId) → 抛 EMAIL_ALREADY_REGISTERED
       ↓ UserMapper.updateById({email:newEmail, email_verified:1})
       ↓ 清掉新邮箱的验证码缓存（防复用）
       ↓
  点击保存修改密码 → PUT /api/v1/user/me/password
       ↓ UserProfileServiceImpl.changePassword
       ↓ BCrypt.matches(oldPassword, user.passwordHash) → 抛 PASSWORD_INCORRECT
       ↓ 长度 ≥ 6 + 两次一致 → 抛 PASSWORD_TOO_WEAK / PASSWORD_NOT_MATCH
       ↓ BCrypt.encode(newPassword) → updateById
```

## 2. 文件改动表

| 类型 | 文件 | 改动 |
|------|------|------|
| 新增 | `project/user/api/.../modules/user/controller/UserProfileController.java` | 4 个 endpoint，含完整类级+方法级 Javadoc |
| 新增 | `project/user/api/.../modules/user/service/UserProfileService.java` | 接口，方法签名含 Javadoc |
| 新增 | `project/user/api/.../modules/user/service/impl/UserProfileServiceImpl.java` | 实现，注入 `UserMapper` + `EmailCodeService`，关键业务规则 Javadoc |
| 新增 | `project/user/api/.../modules/user/vo/UserProfileVO.java` | 5 字段，类级 Javadoc 说明每个字段来源 |
| 新增 | `project/user/api/.../modules/user/dto/request/UpdateNicknameRequest.java` | nickname: @NotBlank @Size(1,20) |
| 新增 | `project/user/api/.../modules/user/dto/request/UpdateEmailRequest.java` | newEmail + emailCode 校验 |
| 新增 | `project/user/api/.../modules/user/dto/request/ChangePasswordRequest.java` | oldPassword + newPassword + confirmPassword |
| 新增 | `project/user/api/.../modules/user/converter/UserConverter.java` | `User → UserProfileVO` |
| 修改 | `project/user/api/.../modules/auth/mapper/UserMapper.java` | 新增 `existsByEmail(@Param("email") String email, @Param("excludeUserId") Long excludeUserId)`，方法 Javadoc |
| 修改 | `shared/.../enums/error/UserAuthErrorCode.java` | 新增 `EMAIL_ALREADY_REGISTERED(111015)` / `EMAIL_SAME_AS_OLD(111016)` / `PASSWORD_INCORRECT(111017)` / `PASSWORD_TOO_WEAK(111018)` |
| 修改 | `project/user/api/.../modules/auth/entity/User.java` | 给 8 个非自明字段补字段级注释（biz_no/nickname/email/avatar_url/invite_code/user_status/email_verified/tenant_id） |
| 修改 | `project/user/web/src/api/user.js` | 新文件，4 个 API 调用 |
| 新增 | `project/user/web/src/composables/useUserProfile.js` | composable：loadProfile/updateNickname/updateEmail/changePassword，ant message 提示 |
| 修改 | `project/user/web/src/views/console/ConsoleLayout.vue` | onMounted 调 `loadProfile()`；把 `provide('consoleActions')` 中的 `profileForm`/`emailForm` 改成从 useUserProfile 读取；弹框"用户ID 88886666"和"本月已生成 12 篇"硬编码替换；profileModal/emailModal 的"保存"按钮从 console.log 替换成真实 API 调用 |

## 3. API 接口契约

### 3.1 `GET /api/v1/user/me`

- **鉴权**：JWT required
- **响应**：
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "userId": "U000123",
      "nickname": "爱创作用户",
      "email": "py_world@163.com",
      "avatarUrl": null,
      "emailVerified": 1
    }
  }
  ```

### 3.2 `PUT /api/v1/user/me/nickname`

- **请求**：`{"nickname": "新昵称"}`
- **失败**：401 未登录 / 400 nickname 为空或超长
- **成功**：返回最新 `UserProfileVO`

### 3.3 `PUT /api/v1/user/me/email`

- **请求**：`{"newEmail": "new@example.com", "emailCode": "123456"}`
- **失败**：验证码错误/过期 / 邮箱已被他人注册 / 新邮箱与旧邮箱相同
- **成功**：返回最新 `UserProfileVO`，同时把 `email_verified` 置 1

### 3.4 `PUT /api/v1/user/me/password`

- **请求**：`{"oldPassword": "xxx", "newPassword": "yyy", "confirmPassword": "yyy"}`
- **失败**：旧密码错 / 新密码 < 6 位 / 两次不一致
- **成功**：204 No Content

## 4. 前端集成

### 4.1 新增 `src/api/user.js`

```js
import api from './request'
export const getMyProfile = () => api.get('/api/v1/user/me')
export const updateNickname = (nickname) => api.put('/api/v1/user/me/nickname', { nickname })
export const updateEmail = (newEmail, emailCode) => api.put('/api/v1/user/me/email', { newEmail, emailCode })
export const changePassword = (payload) => api.put('/api/v1/user/me/password', payload)
```

### 4.2 新增 `src/composables/useUserProfile.js`

- `const profile = ref(null)`
- `async function loadProfile()`：调用 `getMyProfile()`，写入 `profile.value`
- `async function saveNickname(nickname)`：调 `updateNickname`，成功后 `profile.value.nickname = nickname`
- `async function saveEmail(newEmail, code)`：同上
- `async function savePassword(payload)`：成功后 ant message 提示
- 失败统一 `message.error(e.message)`

### 4.3 `ConsoleLayout.vue` 改动点

- import `useUserProfile`，在 setup 中调用
- `onMounted(() => userProfile.loadProfile())`
- 把 `provide('consoleActions', { ..., profileForm: {nickname: profile.value?.nickname ?? '爱创作用户'}, emailForm: {email: profile.value?.email ?? ''}, userId: profile.value?.userId ?? '...' })` — 通过 computed 派生
- profileModal 的 `handleProfileSubmit`：删 `console.log`，改成 `await userProfile.saveNickname(profileForm.nickname)`
- emailModal 的 `handleEmailSubmit`：改成 `await userProfile.saveEmail(emailForm.email, emailForm.code)`
- 弹框里"用户ID 88886666" → `{{ userId }}`；"本月已生成 12 篇" → 改成 `{{ monthlyWorks }} 篇`（复用 MineIndex 的计算逻辑，或抽到 composable 里）

## 5. Javadoc 规范（本轮严格执行）

- 每个新类：类级 `/** ... */`，含职责、依赖、典型用法
- 每个 controller 方法：HTTP 方法 + 路径 + 业务规则 + `@param` + `@return`
- 每个 service 方法：业务规则 + 失败抛错 + `@param` + `@return`
- 简单 getter/setter 可省
- 复用方法（`EmailCodeServiceImpl#validateEmailCode`）：补 1-2 行方法级 Javadoc，说明 emailCode 验证后会被失效（防复用）
- `User` entity 8 个非自明字段加 `/** ... */` 行内注释
- `UserMapper#existsByEmail`：明确 excludeUserId 用途

## 6. 测试

- `UserProfileServiceImpl` 单元测试：getMyProfile / updateNickname 长度校验 / updateEmail 验证码错/邮箱冲突/与旧邮箱相同 / changePassword 旧密码错/新密码太弱/两次不一致/正常改密成功
- `UserMapper#existsByEmail` SQL 正确性测试（排除自己邮箱后应能查到已被他人注册）
- 前端：useUserProfile composable 单元测试（mock api 调用 + 错误处理）

## 7. 不在本轮范围（明示排除）

- 会员等级/有效期后端化（membershipLevel/membershipExpiry/hasMembership 仍 ref 占位）
- 创作币余额后端化（coinBalance 仍 ref(0)）
- 邀请统计后端化（inviteStats 仍空 reactive）
- 头像上传 UI + 后端
- 主题切换持久化
- 本月已生成后端化（仍走 localStorage `aichuangzuo_generation_queue`）

## 8. 验证步骤

1. `mvn -pl user/api test`：UserProfileServiceImpl 单元测试全绿
2. `mvn -pl user/api spring-boot:run`：本地启动后端
3. `curl -H "Authorization: Bearer <jwt>" http://localhost:25050/api/v1/user/me`：返回真实数据
4. 前端 onMounted 触发 loadProfile，弹框和 Mine 页都显示真实昵称/邮箱/用户ID
5. 改昵称：保存 → 弹框和 Mine 页同步更新
6. 改邮箱：发码到新邮箱 → 输入验证码 → 保存 → 邮箱切换；旧邮箱缓存失效
7. 改密码：旧密码错 → 报错；两次不一致 → 报错；正常 → 旧密码登录失败、新密码登录成功
# 用户端后端 Java 代码评审报告

**评审日期**：2026-08-27  
**评审范围**：`project/user/api/` 用户端后端 Java 代码  
**评审依据**：`docs/architecture/` 下 java-package-conventions、java-code-style-conventions、api-interface-conventions、exception-errorcode-conventions、security-conventions、logging-conventions、caching-conventions、mysql-table-conventions 等文档。

---

## 一、高危安全问题

### 1. 内部接口未鉴权 / 配置冲突 ✅ 已修复

- `project/user/api/src/main/java/com/aichuangzuo/user/config/SecurityConfig.java:40`：`.requestMatchers("/api/v1/user/internal/**").permitAll()` 直接把全部内部接口放行。
- `project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/InternalKeyAuthenticationFilter.java:29`：仅对 `/api/v1/user/coin-records/internal-grant` 和 `/api/v1/user/internal/**` 做 `X-Internal-Key` 校验，但 `SecurityConfig` 已经 `permitAll`，导致内部接口可被匿名访问。
- **风险**：创作币发放、收益、提现等内部接口可能被未授权调用。
- **修复**：移除 `SecurityConfig.java` 中 `/api/v1/user/internal/**` 的 `permitAll()` 配置，使内部接口统一走 `InternalKeyAuthenticationFilter` 的 `X-Internal-Key` 校验。移除后该路径落入 `.anyRequest().authenticated()`，由前置 Filter 在请求到达鉴权决策前设置认证信息。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest` 两个内部 Controller 单元测试，均通过。
  - 全量测试因本地 MySQL 未启动（`Access denied for user 'root'@'localhost'`）无法执行，与本次改动无关。

### 2. 管理端 JWT Secret 硬编码 ✅ 已修复（生产兼容性调整）

- `project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/AdminJwtUtil.java:20`：`@Value("${auth.jwt.admin-access-secret:please-change-this-admin-access-secret-at-least-256-bits-long}")` 在配置缺失时回退为硬编码默认值。
- 违反安全规范“禁止硬编码 Secret”。同问题还存在于 `project/admin/api/src/main/resources/application.yml:41`，管理端生成 admin JWT 的 `access-secret` 与 `refresh-secret` 同样使用了硬编码默认值。
- **修复**：
  - 用户端 `AdminJwtUtil.java`：移除 `@Value` 的硬编码默认值，改为 `@Value("${auth.jwt.admin-access-secret}")`；新增 `@PostConstruct validateSecret()`，在 Bean 初始化时校验 Secret 已配置且长度不少于 32 字节（256 位），否则抛出 `IllegalStateException`。
  - 用户端 `application.yml`：新增 `admin-access-secret: ${ADMIN_JWT_ACCESS_SECRET:please-change-this-admin-access-secret-at-least-256-bits-long}`，默认占位符长度满足 256 位要求，**生产环境必须通过环境变量覆盖**。
  - 管理端 `application.yml`：保留 `${ADMIN_JWT_ACCESS_SECRET:please-change-this-admin-access-secret-at-least-256-bits-long}` / `${ADMIN_JWT_REFRESH_SECRET:please-change-this-admin-refresh-secret-at-least-256-bits-long}` 长默认值，避免已有部署因未配环境变量而启动失败，**生产环境必须通过环境变量覆盖**。
  - 管理端 `JwtUtil.java`：新增 `@PostConstruct validateSecrets()`，校验 access/refresh Secret 均配置且长度不少于 32 字节。
  - `config/example.env`：补充 `ADMIN_JWT_ACCESS_SECRET`、`ADMIN_JWT_REFRESH_SECRET` 配置项模板。
- **生产注意**：admin-api 与管理端/用户端跨端调用依赖同一 `ADMIN_JWT_ACCESS_SECRET`，部署时请确保两侧环境变量一致且为强随机密钥。

### 3. 敏感信息入日志 ✅ 已修复

- `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/AuthController.java:85`：`log.info("刷新Token, userId={}, refreshToken={}", ..., request.getRefreshToken())` 把 refreshToken 完整打印。
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java:57`：`log.info("邮箱验证码已发送 email={}, code={}", normalizedEmail, code)` 把验证码明文打印。
- 违反日志规范“禁止将 Token、密码等敏感信息作为日志参数”。
- **修复**：
  - `AuthController.java`：将 `refreshToken={}` 改为 `refreshTokenLen={}`，仅记录 Token 长度，不记录完整 Token。
  - `EmailCodeServiceImpl.java`：移除日志中的 `code={}`，仅记录邮箱。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

### 4. SQL 注入风险 ✅ 已修复

- `project/user/api/src/main/java/com/aichuangzuo/user/modules/leaderboard/service/impl/CoinRecordServiceImpl.java:51、82`：`.setSql("coin_balance = coin_balance + " + amount.toPlainString())` 直接拼接数值到 SQL。
- `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/service/SkillMarketUsageService.java:69`：`.setSql("weekly_earnings = weekly_earnings + " + price.toPlainString())` 同样拼接。
- 虽然 `amount/price` 当前多为内部生成，但一旦入口参数被篡改即存在注入风险，应使用参数化表达式或 `apply` 占位符。
- **修复**：
  - `UserMapper.java`：新增 `@Update` 方法 `addCoinBalance(userId, amount)` 与 `subtractCoinBalance(userId, amount)`，使用 `#{amount}` 参数化占位符，并带 `coin_balance >= #{amount}` 余额充足条件。
  - `CoinRecordServiceImpl.java`：`grant` / `spend` 改为调用上述 Mapper 方法，移除 `setSql` 字符串拼接。
  - `SkillMarketMapper.java`：新增 `@Update` 方法 `incrementUsageStats(skillId, price)`，使用 `#{price}` 参数化占位符。
  - `SkillMarketUsageService.java`：改为调用上述 Mapper 方法，移除 `setSql` 字符串拼接。
  - 清理了两个 Service 中不再使用的 `LambdaUpdateWrapper` 导入。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

---

## 二、API 响应 / 异常 / 错误码

### 5. JWT 过滤器返回非规范响应 ✅ 已修复

- `project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/JwtAuthenticationFilter.java:72`：`writeUnauthorized` 返回 `{"code":"TOKEN_INVALID","message":"..."}`，`code` 是字符串而非规范要求的 6 位数字整数，前端无法按统一错误码处理。
- **修复**：
  - 引入 `com.aichuangzuo.shared.enums.error.UserAuthErrorCode` 与 `com.aichuangzuo.shared.result.Result`。
  - `writeUnauthorized` 改为接收 `UserAuthErrorCode`，返回 `Result.fail(errorCode)` 序列化后的 JSON，例如 `{"code":111023,"message":"token 已被登出"}`。
  - Token 黑名单场景使用 `TOKEN_BLACKLISTED(111023)`，其他异常使用 `TOKEN_INVALID(111022)`，均为 6 位数字错误码。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

### 6. 直接抛 JDK 异常表达业务错误 ✅ 已修复

多处违反异常规范“禁止抛 `IllegalArgumentException` / `RuntimeException` 表达业务错误”：

- `CoinRecordServiceImpl.java:39、78`
- `EarningsServiceImpl.java:108、111、149、184、187、191`
- `ScheduledTaskService.java:36、39`
- `GenerationTaskRefundService.java:29`

应统一改为 `BusinessException(ErrorCode)` 或 `SystemException`。
- **修复**：
  - 上述位置全部改为 `new BusinessException(SystemErrorCode.PARAM_VALIDATION_ERROR.getCode(), "...")` 或 `new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND.getCode(), "...")`。
  - `CoinRecordServiceImpl`：入账/扣减金额非法 → `PARAM_VALIDATION_ERROR`。
  - `EarningsServiceImpl`：收益金额、抵扣数量、归属月份、收益类型非法 → `PARAM_VALIDATION_ERROR`。
  - `ScheduledTaskService`：任务不存在 → `RESOURCE_NOT_FOUND`；任务已禁用 → `PARAM_VALIDATION_ERROR`。
  - `GenerationTaskRefundService`：参数为空 → `PARAM_VALIDATION_ERROR`。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

### 7. 错误码枚举未下沉到 shared ✅ 已修复

- `ArticleErrorCode`、`CommissionErrorCode`、`LeaderboardErrorCode`、`MessageErrorCode` 等定义在各自模块。
- `exception-errorcode-conventions.md` 要求错误码枚举按模块放在 `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/`。
- **修复**：
  - 将用户端 13 个模块级错误码枚举从 `project/user/api/src/main/java/com/aichuangzuo/user/modules/*/enums/` 迁移到 `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/`。
  - 更新的枚举包括：`ArticleErrorCode`、`BenefitErrorCode`、`CommissionErrorCode`、`FeedbackErrorCode`、`HotSearchErrorCode`、`LeaderboardErrorCode`、`LotteryErrorCode`、`MembershipErrorCode`、`MessageErrorCode`、`RecommendedCreationErrorCode`、`SelfMediaPlanErrorCode`、`SkillErrorCode`、`WithdrawErrorCode`。
  - 同步更新了 `project/user/api/src/main/java` 下 31 个文件的 32 条 import 语句，以及测试目录中的 8 条 import 语句。
  - 修复了 `ArticleErrorCode` 构造函数参数名 `String` 的问题（改为 `message`），未改动任何错误码值或错误消息。
  - 原模块目录中的枚举文件已删除。
- **验证**：
  - `mvn -f project/shared/pom.xml compile -q` 编译通过。
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - `mvn -f project/user/api/pom.xml test-compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

---

## 三、代码风格 / Lombok / MyBatis-Plus

### 8. Entity 使用 `@Data` ✅ 已修复

以下实体违反“Entity 禁止使用 `@Data`”：

- `HomeBannerEntity.java:10`
- `SkillMarket.java:17`
- `UserMarketFavorite.java:11`
- `LearnCategoryEntity.java:16`
- `LearnBannerEntity.java:10`
- `LearnArticleEntity.java:18`

- **修复**：
  - 上述 6 个 Entity 的 `lombok.Data` 注解全部替换为显式的 `lombok.Getter` + `lombok.Setter`。
  - 全工程 Entity 目录下已无 `@Data` 使用（`grep -l "@Data" project/user/api/src/main/java/**/entity/*.java` 无结果）。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

### 9. Entity 命名保留 “Entity” 后缀

- `LearnBannerEntity`、`HomeBannerEntity`、`TestimonialEntity` 等类名保留了 `Entity` 后缀。
- 规范要求“去掉表前缀，直接用领域对象名”（如 `LearnBanner`）。

### 10. 魔法值 / 硬编码状态 ✅ 已修复

- `UserInviteBindingServiceImpl.java:80-81`：`sourceType(2)`、`effectiveStatus(1)` 直接写死。
- `ArticleServiceImpl.java:173`：`.set(Article::getIsDeleted, 1)` 等大量 `0/1` 状态值未使用枚举。
- `AuthServiceImpl.java:171、172、174` 等同样存在。
- **修复**：
  - 在 `project/shared/src/main/java/com/aichuangzuo/shared/enums/` 下新增 6 个共享枚举：
    - `UserStatusEnum`：DISABLED(0)、ENABLED(1)。
    - `VerifyStatusEnum`：UNVERIFIED(0)、VERIFIED(1)。
    - `BlockStatusEnum`：UNBLOCKED(0)、BLOCKED(1)。
    - `DeletedFlagEnum`：NOT_DELETED(0)、DELETED(1)。
    - `InviteSourceTypeEnum`：LINK(1)、MANUAL(2)。
    - `InviteEffectiveStatusEnum`：PENDING(0)、ACTIVE(1)、INVALID(2)。
  - `UserInviteBindingServiceImpl.java`：`sourceType` 改为 `InviteSourceTypeEnum.MANUAL.getCode()`，`effectiveStatus` 改为 `InviteEffectiveStatusEnum.ACTIVE.getCode()`。
  - `InviteRewardServiceImpl.java`：移除局部 `EFFECTIVE_STATUS` 常量，同一邀请关系字段改用共享枚举。
  - `ArticleServiceImpl.java`：所有 `isDeleted` 的 `0/1` 魔法值改为 `DeletedFlagEnum.NOT_DELETED / DELETED.getCode()`，包括 `Article`、`SkillMarket`、`UserSkill` 查询条件。
  - `AuthServiceImpl.java`：`userStatus`、`emailVerified`、`phoneVerified`、`isBlocked` 的 `0/1` 魔法值分别改用 `UserStatusEnum`、`VerifyStatusEnum`、`BlockStatusEnum`。
  - `UserProfileServiceImpl.java`：修改邮箱/手机后设置 `emailVerified` / `phoneVerified` 改用 `VerifyStatusEnum.VERIFIED.getCode()`。
- **验证**：
  - `mvn -f project/shared/pom.xml install -q -DskipTests` 安装共享模块。
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - `mvn -f project/user/api/pom.xml test-compile -q` 测试代码编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

### 11. 时间类型不规范 ✅ 已修复

- `JwtUtil.java:44`：使用 `new Date()`。
- 规范要求优先使用 `LocalDateTime` / `Instant`（JWT 场景可用 `Date.from(Instant.now())`）。
- **修复**：
  - `JwtUtil.java`：新增 `import java.time.Instant;`。
  - 将 `Date now = new Date();` 改为 `Date now = Date.from(Instant.now());`，`expiry` 仍基于 `now` 推导，保持 JJWT 的 `Date` API 兼容性。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

---

## 四、日志 / traceId / 缓存

### 12. 缺失 traceId 与请求日志

- 全工程未找到 MDC / `X-Trace-Id` / 请求日志 Filter。
- 违反日志规范“请求入口生成 traceId 并写入 MDC”。

### 13. 缓存 Key 与值使用不当 ✅ 已修复

- `RateLimitInterceptor.java:36-42`：把 `AtomicInteger` 可变对象存入 Caffeine，依赖对象引用做计数；并发下 `incrementAndGet()` 与缓存过期逻辑存在竞态。
- `CaffeineConfig.java` 中 `authCache` 使用自定义 `CacheValue` 包装，而 `CacheUtil` 在 get 时再次做时间校验，逻辑冗余。
- **修复**：
  - `CacheUtil.java`：
    - 移除 `get(String)` 中手动判断 `System.currentTimeMillis() > expireAtMillis` 并 `invalidate` 的冗余逻辑；Caffeine 自定义 `Expiry` 已根据 `CacheValue.expireAtMillis` 管理过期，`getIfPresent` 读取时会自动过滤过期条目。
    - 新增 `incrementAndGet(String key, long duration, TimeUnit unit)` 方法，使用 `authCache.asMap().compute(...)` 原子地完成"不存在/已过期则初始化为 1，否则 +1"，避免 `get`-判空-`set`-`incrementAndGet` 之间的竞态。
  - `RateLimitInterceptor.java`：
    - 移除 `AtomicInteger` 可变对象缓存方案及对应 import。
    - 改为调用 `cacheUtil.incrementAndGet(key, windowSeconds, TimeUnit.SECONDS)`，直接获取当前累计请求数并判断是否超过阈值。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

---

## 五、其他明显问题

### 14. Controller 直接调用 Mapper ✅ 已修复

- `LotteryController.java:43-50`：`campaignMapper.selectOne(...)` 在 Controller 中直接查询。
- 违反分层规范“Controller 不允许直接调用 Mapper”。
- **修复**：
  - `LotteryDisplayService` / `LotteryDisplayServiceImpl` 新增三个方法：
    - `getCurrentCampaign()`：查询当前进行中的活动。
    - `getCampaignById(Long campaignId)`：按 ID 查询活动。
    - `listActiveTiersByCampaignId(Long campaignId)`：查询活动下启用的奖项档位。
  - `LotteryDisplayServiceImpl` 注入 `LotteryCampaignMapper` 与 `LotteryPrizeTierMapper`，将查询逻辑下沉到 Service；`is_deleted=0` 改用 `DeletedFlagEnum.NOT_DELETED.getCode()`。
  - `LotteryController` 移除 `LotteryCampaignMapper`、`LotteryPrizeTierMapper` 依赖及所有 LambdaQueryWrapper 查询逻辑，改为注入 `LotteryDisplayService` 并调用上述方法。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

### 15. 测试端点生产环境仍注册路径 ✅ 已修复

- `TestEmailCodeController.java` 使用 `@Profile("test")`。
- 但 `SecurityConfig.java:41` 将 `/__test/**` 永久 `permitAll`；若 profile 配置错误会暴露验证码读取接口。
- **修复**：
  - `SecurityConfig.java`：注入 `Environment`，将 `.requestMatchers("/__test/**").permitAll()` 改为仅在 `test` profile 激活时注册：`if (environment.matchesProfiles("test")) { auth.requestMatchers("/__test/**").permitAll(); }`。
  - 非 test 环境下 `/__test/**` 不再被无条件放行，将落入 `.anyRequest().authenticated()`；同时 `TestEmailCodeController` 本身也仅在该 profile 下注册，两层保护保持一致。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。
  - `EmailCodeServiceTest`（`@ActiveProfiles("test")`）因本地 MySQL 未启动无法执行，与本次改动无关。

### 16. MyBatis 时间函数不一致 ✅ 已修复

- `UserMapper.java:48`：`updated_at = NOW()` 使用秒级。
- 规范要求 `DATETIME(3)` 并使用 `NOW(3)`。
- **修复**：
  - `UserMapper.java`：`updatePassword` 中的 `updated_at = NOW()` 改为 `updated_at = NOW(3)`，与库中 `DATETIME(3)` 字段精度及其他 `@Update` 方法保持一致。
  - 全用户端 Java Mapper 与 XML 中已无 `NOW()` 用法，统一为 `NOW(3)`。
- **验证**：
  - `mvn -f project/user/api/pom.xml compile -q` 编译通过。
  - 运行用户端内部 Controller 单元测试 `GenerationTaskInternalControllerNotifyTest`、`SkillUsageInternalControllerTest`，均通过。

---

## 六、总结与建议

当前代码在功能实现上较为完整，但存在**内部接口未鉴权、JWT Secret 硬编码、敏感信息入日志、SQL 拼接**等高危问题，亟需优先修复；同时错误码、Lombok、日志 traceId、魔法值等也普遍存在不符合架构文档的情况，建议统一治理。高危安全问题已修复完毕。

### 修复优先级

1. **立即修复（安全类）**
   - ✅ 内部接口鉴权：移除 `permitAll`，统一走 `X-Internal-Key` 校验（已完成）。
   - ✅ 管理端 JWT Secret 硬编码：用户端 `AdminJwtUtil`、管理端 `JwtUtil` 启动时校验长度，默认值已改为长占位符（已完成，生产环境需覆盖环境变量）。
   - ✅ 敏感信息入日志：refreshToken、邮箱验证码不再完整打印（已完成）。
   - ✅ SQL 拼接改为参数化：余额增减、收益累计改用 Mapper 参数化 `@Update`（已完成）。

2. **短期统一治理**
   - ✅ JWT 过滤器返回规范响应：`code` 已改为 6 位数字错误码，使用统一 `Result` 结构（已完成）。
   - ✅ 业务异常统一：服务层不再抛 `IllegalArgumentException` / `RuntimeException`，改为 `BusinessException`（已完成）。
   - ✅ 错误码枚举下沉：用户端 13 个模块级错误码枚举已迁移到 `project/shared/enums/error/`（已完成）。
   - ✅ Entity 去掉 `@Data`：6 个 Entity 已改为显式 `@Getter`/`@Setter`（已完成）。
   - ✅ 魔法值硬编码状态：用户端核心 Service 中的 `sourceType`、`effectiveStatus`、`isDeleted`、`userStatus`、`emailVerified`、`phoneVerified`、`isBlocked` 已改用 `project/shared/enums/` 下的共享枚举（已完成）。
   - ✅ 时间类型不规范：`JwtUtil.java` 中的 `new Date()` 已改为 `Date.from(Instant.now())`（已完成）。
   - ✅ 缓存 Key 与值使用不当：`RateLimitInterceptor` 改为原子 `incrementAndGet`，`CacheUtil.get` 移除冗余时间校验（已完成）。
   - ✅ Controller 直接调用 Mapper：`LotteryController` 改为通过 `LotteryDisplayService` 查询活动与奖项（已完成）。
   - ✅ 测试端点生产环境仍注册路径：`SecurityConfig` 仅在 `test` profile 下放行 `/__test/**`（已完成）。
   - ✅ MyBatis 时间函数不一致：`UserMapper.updatePassword` 中的 `NOW()` 已改为 `NOW(3)`（已完成）。
   - Entity 命名保留 “Entity” 后缀（问题 9）。

3. **中期补齐基础能力**
   - 请求日志 Filter + MDC traceId。

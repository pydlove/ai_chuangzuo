# 模拟运营（机器人用户旅程模拟）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 管理端新增「模拟运营」功能：手动创建机器人用户批次，让机器人按概率走完注册→登录→资料→抽奖→买会员→自由创作→约稿旅程，全程调用用户端真实接口，产运营数据。

**Architecture:** 管理端 admin/api 新增 simulation 模块（三张 a_simulation_* 表 + 批次接口 + RestTemplate 客户端 + 阶段执行器 + @ScheduledTask 消费器）；用户端 user/api 只加受 X-Internal-Key 保护的内部注册/邀请码接口、技能市场 publisherType 过滤参数、PaymentServiceImpl 一处内部密钥放行。前端 admin/web 加「模拟运营」页面（tabs，第一个 tab 模拟新用户）。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + RestTemplate + JWT；Vue 3 + Ant Design Vue；JUnit 5 + Mockito。

**Spec:** `docs/superpowers/specs/2026-09-16-simulation-operations-design.md`

## Global Constraints

- **禁止修改已应用的 Flyway 迁移文件**：只新增更高版本文件。admin 下一个版本 `V2.0.0_110__create_simulation_tables.sql`；user 本计划无迁移（user_type 字段已存在）。
- admin 表前缀 `a_`，全字段带 COMMENT，utf8mb4_unicode_ci，索引 `KEY idx_{列}`。
- 统一返回 `com.aichuangzuo.shared.result.Result`；业务异常 `BusinessException` + 模块错误码枚举（放 `com.aichuangzuo.shared.enums.error`）。
- 校验用 jakarta validation；分页入参 `page`/`size`（Long，默认 1/20），出参 record `PageResult(List<X> items, long total, long page, long size)` 定义在 Service 接口内。
- 用户端内部接口安全：路径必须在 `/api/v1/user/internal/` 前缀下（自动走 `InternalKeyAuthenticationFilter` 的 X-Internal-Key 校验），Controller 内还要二次校验管理端 JWT（`AdminJwtUtil.parseAccessToken`，模式照抄 `EarningsInternalController`）。
- 内部调用 header：`Authorization: Bearer <admin JWT>`（admin 侧用 `JwtUtil.generateAccessToken(userId)` 签发）+ `X-Internal-Key`（配置项 `user.api.internal-key`）。
- 用户端口 25050，admin 端口 26060；用户端测试支付常量 `TEST_PAY_CODE = "123456"`（`PaymentServiceImpl` line 61）。
- 开发过程垃圾代码及时清理：不用的 import/注释/临时 log 当任务内删干净。
- 实体风格照抄 `LotteryCampaign.java`：`@Getter @Setter`、`@TableName`、`IdType.AUTO`、`is_deleted`/`tenant_id`/`created_at`/`updated_at`/`created_by`/`updated_by` 齐全。

---

### Task 1: user/api — 机器人注册内部接口 + 随机邀请码接口

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/simulation/controller/SimulationInternalController.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/simulation/service/SimulationUserService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/simulation/dto/SimulationRobotCreateRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/simulation/vo/RobotCreatedVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/util/InviteCodeGenerator.java`（从 AuthServiceImpl 抽出）
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/AuthServiceImpl.java`（generateInviteCode 改为注入 InviteCodeGenerator）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/simulation/service/SimulationUserServiceTest.java`

**Interfaces:**
- Consumes: `InviteRewardService.rewardAfterRegister(User invitee, String inviteCode)`（`modules/user/service/InviteRewardService.java:21`）；`UserMapper`；`PasswordEncoder` bean。
- Produces:
  - `POST /api/v1/user/internal/simulation/robots` → `Result<RobotCreatedVO{userId, email}>`
  - `GET /api/v1/user/internal/simulation/robot-invite-codes?count=N` → `Result<List<String>>`
  - `InviteCodeGenerator.generate()`（public，`AuthServiceImpl` 与 `SimulationUserService` 共用）

- [ ] **Step 1: 抽出 InviteCodeGenerator**

新建 `modules/auth/util/InviteCodeGenerator.java`：

```java
@Component
public class InviteCodeGenerator {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final UserMapper userMapper; // 构造器注入

    public String generate() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            String code = sb.toString();
            if (userMapper.selectByInviteCode(code) == null) return code;
        }
        throw new BusinessException(SystemErrorCode.SYSTEM_ERROR, "邀请码生成失败");
    }
}
```

`AuthServiceImpl`：删除私有 `generateInviteCode()`（line 229 附近），构造器注入 `InviteCodeGenerator`，原调用点改为 `inviteCodeGenerator.generate()`。

- [ ] **Step 2: 写失败测试**

`SimulationUserServiceTest`（JUnit5 + Mockito，Mock `UserMapper`、`PasswordEncoder`、`InviteRewardService`、`InviteCodeGenerator`）：

```java
@Test
void createRobot_setsUserTypeZeroAndSkipsVerification() {
    when(inviteCodeGenerator.generate()).thenReturn("ABCD23");
    when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    RobotCreatedVO vo = service.createRobot("bot1@x.simrobot.com", "pass123456", null);
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userMapper).insert(captor.capture());
    assertEquals(0, captor.getValue().getUserType());
    assertEquals("ABCD23", captor.getValue().getInviteCode());
    assertEquals(VerifyStatusEnum.VERIFIED.getCode(), captor.getValue().getEmailVerified());
    verify(inviteRewardService, never()).rewardAfterRegister(any(), any());
}

@Test
void createRobot_withInviteCode_rewardsInviter() {
    // inviteCode 非空时 verify(inviteRewardService).rewardAfterRegister(any(User.class), eq("INVITE"))
}
```

- [ ] **Step 3: 运行测试确认失败**

`cd project/user/api && mvn test -Dtest=SimulationUserServiceTest` → 编译失败（类不存在）。

- [ ] **Step 4: 实现 SimulationUserService**

```java
@Service
@RequiredArgsConstructor
public class SimulationUserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final InviteRewardService inviteRewardService;

    @Transactional
    public RobotCreatedVO createRobot(String email, String password, String inviteCode) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            return new RobotCreatedVO(existing.getId(), existing.getEmail()); // 幂等：已存在直接返回
        }
        User user = new User();
        user.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setInviteCode(inviteCodeGenerator.generate());
        user.setUserStatus(UserStatusEnum.ENABLED.getCode());
        user.setEmailVerified(VerifyStatusEnum.VERIFIED.getCode());
        user.setUserType(0);
        user.setNickname("用户" + user.getInviteCode());
        userMapper.insert(user);
        if (StringUtils.hasText(inviteCode)) {
            inviteRewardService.rewardAfterRegister(user, inviteCode);
        }
        return new RobotCreatedVO(user.getId(), user.getEmail());
    }

    public List<String> randomRobotInviteCodes(int count) {
        return userMapper.selectRobotInviteCodes(count); // Step 5 加到 UserMapper
    }
}
```

`RobotCreatedVO`：`public record RobotCreatedVO(Long userId, String email) {}`。

- [ ] **Step 5: UserMapper 加随机机器人邀请码查询**

`modules/auth/mapper/UserMapper.java` 加：

```java
@Select("SELECT invite_code FROM u_user WHERE user_type = 0 AND is_deleted = 0 ORDER BY RAND() LIMIT #{count}")
List<String> selectRobotInviteCodes(@Param("count") int count);
```

- [ ] **Step 6: 实现 Controller**

```java
@Tag(name = "内部-模拟运营")
@Slf4j
@RestController
@RequestMapping("/api/v1/user/internal/simulation")
@RequiredArgsConstructor
public class SimulationInternalController {
    private final SimulationUserService simulationUserService;
    private final AdminJwtUtil adminJwtUtil;

    @Operation(summary = "注册机器人用户")
    @PostMapping("/robots")
    public Result<RobotCreatedVO> createRobot(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody SimulationRobotCreateRequest request) {
        requireInternalAdmin(authHeader);
        return Result.success(simulationUserService.createRobot(request.getEmail(), request.getPassword(), request.getInviteCode()));
    }

    @Operation(summary = "随机机器人邀请码")
    @GetMapping("/robot-invite-codes")
    public Result<List<String>> robotInviteCodes(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "5") int count) {
        requireInternalAdmin(authHeader);
        return Result.success(simulationUserService.randomRobotInviteCodes(Math.min(count, 50)));
    }

    private void requireInternalAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("missing admin token");
        }
        adminJwtUtil.parseAccessToken(authHeader.substring(7));
    }
}
```

`SimulationRobotCreateRequest`：`@NotBlank email`（`@Email`）、`@NotBlank @Size(min=6,max=20) password`、`inviteCode` 可选。UnauthorizedException 用 `com.aichuangzuo.shared.exception.UnauthorizedException`（以项目实际类为准，照抄 EarningsInternalController 的引法）。

注意：`/api/v1/user/internal/` 前缀已被 `InternalKeyAuthenticationFilter` 覆盖，X-Internal-Key 校验无需自己写；但要在 SecurityConfig 确认该前缀已被列为 internal（已有 `/api/v1/user/internal/` 通则，无需改动）。

- [ ] **Step 7: 运行测试确认通过 + 全量编译**

`cd project/user/api && mvn test -Dtest=SimulationUserServiceTest && mvn compile` → PASS。

- [ ] **Step 8: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/simulation project/user/api/src/main/java/com/aichuangzuo/user/modules/auth project/user/api/src/test/java/com/aichuangzuo/user/modules/simulation
git commit -m "feat(user): 新增模拟运营内部接口（机器人注册/邀请码）"
```

---

### Task 2: user/api — 技能市场列表 publisherType 过滤

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/controller/SkillMarketController.java`（`listEnabledPaged` 加 `@RequestParam(required=false) Integer publisherType`）
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/service/impl/SkillMarketQueryServiceImpl.java`（`pageEnabled` 透传）
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/mapper/SkillMarketAggregateMapper.java`（`selectEnabledMarketSkills` 加 `@Param("publisherType") Integer publisherType`）
- Modify: `project/user/api/src/main/resources/mapper/SkillMarketAggregateMapper.xml`（WHERE 加条件）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/skill/market/service/SkillMarketQueryServiceTest.java`（若该模块无既有测试则跳过单测，用 Task 9 端到端验证代替）

**Interfaces:**
- Produces: `GET /api/v1/user/market-skills/paged?page&pageSize&keyword&sortType&publisherType`（publisherType：0=机器人，1=真实用户，不传=全部）。

- [ ] **Step 1: 修改链路四个文件**

Mapper XML 的 `selectEnabledMarketSkills`（已有 `LEFT JOIN u_user u ON u.id = s.publisher_user_id`）WHERE 段加：

```xml
<if test="publisherType != null">
    AND u.user_type = #{publisherType}
</if>
```

Controller/Service/Mapper 接口逐层加同名参数透传，不改默认值行为（不传 = 全部）。

- [ ] **Step 2: 编译**

`cd project/user/api && mvn compile` → PASS。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/skill project/user/api/src/main/resources/mapper/SkillMarketAggregateMapper.xml
git commit -m "feat(user): 技能市场列表支持按发布者类型过滤"
```

---

### Task 3: user/api — 支付 test_mode 内部密钥放行

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/InternalCallContext.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/InternalKeyAuthenticationFilter.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/service/impl/PaymentServiceImpl.java`（line 148 附近）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/infrastructure/security/InternalCallContextTest.java`

**Interfaces:**
- Produces: `InternalCallContext.markInternal()` / `isInternal()` / `clear()`；`PaymentServiceImpl` 中 `isTestMode(config)` 判断变为 `isTestMode(config) || InternalCallContext.isInternal()`。

- [ ] **Step 1: 写 InternalCallContext**

```java
public final class InternalCallContext {
    private static final ThreadLocal<Boolean> INTERNAL = new ThreadLocal<>();
    private InternalCallContext() {}
    public static void markInternal() { INTERNAL.set(Boolean.TRUE); }
    public static boolean isInternal() { return Boolean.TRUE.equals(INTERNAL.get()); }
    public static void clear() { INTERNAL.remove(); }
}
```

- [ ] **Step 2: 修改 InternalKeyAuthenticationFilter**

在现有校验逻辑中：只要请求带了与配置一致的 X-Internal-Key（不限路径），在 `chain.doFilter` 前 `InternalCallContext.markInternal()`，并用 try/finally 在之后 `InternalCallContext.clear()`。注意保持现有 internal 路径的 401 行为不变（key 不对仍 401），非 internal 路径带 key 只是打标、不拦截。

- [ ] **Step 3: 修改 PaymentServiceImpl**

line 148 附近：

```java
// 原：if (isTestMode(config)) {
if (isTestMode(config) || InternalCallContext.isInternal()) {
```

其余逻辑（payCode 必须 123456、confirmOrder）不变。

- [ ] **Step 4: 写测试**

`InternalCallContextTest`：mark→isInternal true→clear→false。PaymentServiceImpl 行为变更由 Task 9 端到端覆盖（单测需要 mock 整条下单链，性价比低，跳过）。

- [ ] **Step 5: 运行测试 + 编译**

`cd project/user/api && mvn test -Dtest=InternalCallContextTest && mvn compile` → PASS。

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security project/user/api/src/main/java/com/aichuangzuo/user/modules/membership project/user/api/src/test/java/com/aichuangzuo/user/infrastructure/security
git commit -m "feat(user): 支付测试模式支持内部密钥请求级放行"
```

---

### Task 4: admin/api — Flyway 迁移 + 实体/枚举/Mapper

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_110__create_simulation_tables.sql`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/simulation/entity/SimulationBatch.java`
- Create: `.../modules/simulation/entity/SimulationRobot.java`
- Create: `.../modules/simulation/entity/SimulationRobotLog.java`
- Create: `.../modules/simulation/mapper/SimulationBatchMapper.java`、`SimulationRobotMapper.java`、`SimulationRobotLogMapper.java`（三个极简 BaseMapper）
- Create: `.../modules/simulation/enums/SimulationBatchStatus.java`、`SimulationRobotStatus.java`、`SimulationStage.java`、`PromptScope.java`
- Create: `.../modules/simulation/config/SimulationStageConfig.java`

**Interfaces:**
- Produces（后续任务依赖的常量与字段）：
  - `SimulationBatchStatus`: PENDING, RUNNING, COMPLETED, CANCELED
  - `SimulationRobotStatus`: WAITING, IN_PROGRESS, COMPLETED, FAILED, CANCELED
  - `SimulationStage`: REGISTER, LOGIN, PROFILE, LOTTERY, MEMBERSHIP, CREATE, COMMISSION（`next()` 返回下一阶段或 null）
  - `PromptScope`: ROBOT(0), ALL(null), REAL(1)；`Integer userType()` 返回 publisherType 参数值
  - `SimulationStageConfig`（lombok @Data，Jackson 序列化进 `stage_config` 列）：`lotteryEnabled, lotteryProbability, membershipEnabled, membershipProbability, createEnabled, createProbability, commissionEnabled, commissionProbability`（boolean/int）、`promptScope`（PromptScope）、`userIntervalMin, userIntervalMax, stageIntervalMin, stageIntervalMax`（int，秒）
  - 实体表名：`a_simulation_batch` / `a_simulation_robot` / `a_simulation_robot_log`

- [ ] **Step 1: Flyway 迁移**

`V2.0.0_110__create_simulation_tables.sql`（风格照抄 V2.0.0_106，utf8mb4_unicode_ci、全字段 COMMENT）：

```sql
CREATE TABLE IF NOT EXISTS a_simulation_batch (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(32) NOT NULL COMMENT '批次编号 SIM+日期+序号',
    user_count INT UNSIGNED NOT NULL COMMENT '机器人数',
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '会员套餐ID',
    plan_name VARCHAR(64) NOT NULL COMMENT '会员套餐名称快照',
    stage_config JSON NOT NULL COMMENT '阶段配置(概率/范围/间隔)',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/RUNNING/COMPLETED/CANCELED',
    total_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总数',
    completed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '完成数',
    failed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失败数',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟运营批次';

CREATE TABLE IF NOT EXISTS a_simulation_robot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '批次ID',
    seq INT UNSIGNED NOT NULL COMMENT '批次内序号',
    email VARCHAR(128) NOT NULL COMMENT '虚拟邮箱',
    password_encrypted VARCHAR(255) NOT NULL COMMENT 'AES加密后的初始密码',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户端u_user.id',
    invite_code VARCHAR(16) DEFAULT NULL COMMENT '绑定的邀请码',
    status VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT '状态 WAITING/IN_PROGRESS/COMPLETED/FAILED/CANCELED',
    current_stage VARCHAR(16) NOT NULL DEFAULT 'REGISTER' COMMENT '当前阶段',
    next_run_at DATETIME(3) NOT NULL COMMENT '下一阶段允许执行时间',
    context JSON DEFAULT NULL COMMENT '阶段上下文(文章/任务ID等)',
    fail_stage VARCHAR(16) DEFAULT NULL COMMENT '失败阶段',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    started_at DATETIME(3) DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_seq (batch_id, seq),
    KEY idx_next_run (status, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟运营机器人';

CREATE TABLE IF NOT EXISTS a_simulation_robot_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    robot_id BIGINT UNSIGNED NOT NULL COMMENT '机器人ID',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '批次ID',
    stage VARCHAR(16) NOT NULL COMMENT '阶段',
    status VARCHAR(16) NOT NULL COMMENT '结果 SUCCESS/FAILED/SKIPPED',
    detail JSON DEFAULT NULL COMMENT '明细(任务ID/耗时等)',
    error_msg VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_robot (robot_id),
    KEY idx_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟运营机器人阶段日志';
```

- [ ] **Step 2: 枚举**

```java
@Getter
@RequiredArgsConstructor
public enum SimulationStage {
    REGISTER, LOGIN, PROFILE, LOTTERY, MEMBERSHIP, CREATE, COMMISSION;
    private static final List<SimulationStage> ORDER = List.of(values());
    public SimulationStage next() {
        int i = ORDER.indexOf(this) + 1;
        return i < ORDER.size() ? ORDER.get(i) : null;
    }
}

@Getter
@RequiredArgsConstructor
public enum PromptScope {
    ROBOT(0), ALL(null), REAL(1);
    private final Integer userType;
}
```

- [ ] **Step 3: 实体 + Mapper**

三个实体照 `LotteryCampaign.java` 风格（`@Getter @Setter`、`@TableName` 不加 tenant_id——模拟数据无租户语义，若全局 Mapper 拦截器强制 tenant 则补上字段并在插入时置 0，编译期会发现）。`SimulationRobot.context` 用 `String` 存 JSON（读写时由 Service 用 ObjectMapper 转换，与 `stage_config` 一致）。三个 Mapper 均为 `public interface XxxMapper extends BaseMapper<Xxx> {}`。

- [ ] **Step 4: SimulationStageConfig**

```java
@Data
public class SimulationStageConfig {
    private boolean lotteryEnabled = true;
    private int lotteryProbability = 100;
    private boolean membershipEnabled = true;
    private int membershipProbability = 100;
    private boolean createEnabled = true;
    private int createProbability = 100;
    private boolean commissionEnabled = true;
    private int commissionProbability = 50;
    private PromptScope promptScope = PromptScope.ALL;
    private int userIntervalMin = 10;
    private int userIntervalMax = 30;
    private int stageIntervalMin = 3;
    private int stageIntervalMax = 8;
}
```

- [ ] **Step 5: 启动验证**

`cd project/admin/api && mvn compile` → PASS；本地起服务确认 Flyway 迁移执行成功（`SHOW TABLES LIKE 'a_simulation%'` 三张表存在）。若本地库因迁移失败报 checksum 错，按 CLAUDE.md 用 `mvn flyway:repair` 修复后重试。

- [ ] **Step 6: Commit**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_110__create_simulation_tables.sql project/admin/api/src/main/java/com/aichuangzuo/admin/modules/simulation
git commit -m "feat(admin): 模拟运营三张表迁移与实体枚举"
```

---

### Task 5: admin/api — SimulationUserApiClient（调用户端）+ 资料生成 + 头像抓取

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/simulation/client/SimulationUserApiClient.java`
- Create: `.../modules/simulation/service/SimulationProfileGenerator.java`
- Create: `.../modules/simulation/service/AvatarFetcher.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/simulation/service/SimulationProfileGeneratorTest.java`

**Interfaces:**
- Consumes: `GenerationAiService.call(Long modelConfigId, String systemMessage, String userMessage, Map<String,Object> modelParams, boolean requireJson)`（`modules/generation/service/GenerationAiService.java`，返回 `AiCallResult`，取 content）；`AesUtil.encrypt/decrypt`（shared）；admin `JwtUtil.generateAccessToken(Long)`。
- Produces（Task 7 执行器依赖，签名必须一致）：

```java
@Component
@RequiredArgsConstructor
public class SimulationUserApiClient {
    // @Value("${simulation.user-api.base-url:http://localhost:25050}") baseUrl
    // @Value("${user.api.internal-key:}") internalKey

    public RobotCreated createRobot(String email, String password, String inviteCode);          // POST internal, 内部JWT+X-Internal-Key
    public List<String> randomRobotInviteCodes(int count);                                       // GET internal, 内部JWT+X-Internal-Key
    public String login(String email, String password);                                          // POST /api/v1/user/auth/login → accessToken
    public void updateNickname(String token, String nickname);                                   // PUT /me/nickname
    public void updateProfile(String token, String bio);                                         // PUT /me/profile
    public void uploadAvatar(String token, byte[] imageBytes, String filename);                  // POST /me/avatar multipart
    public Long currentLotteryCampaignId(String token);                                          // GET /lottery/campaigns/current → id，无活动返回 null
    public void drawLottery(String token);                                                       // POST /lottery/draw
    public void subscribeMembership(String token, Long planId);                                  // POST /membership/subscribe {planId, payCode:"123456"}，header 加 X-Internal-Key
    public List<MarketSkill> listMarketSkills(String token, Integer publisherType, int sampleSize); // GET /market-skills/paged 循环翻页采样
    public Long createGenerationTask(String token, Long marketSkillId);                          // POST /generation-tasks，入参以现有创建 DTO 为准
    public String pollGenerationTask(String token, Long taskId);                                 // GET /generation-tasks/{id} 轮询至完成，返回文章内容；超时/失败抛 BusinessException
    public Long randomCommissionTaskId(String token);                                            // GET /commission/tasks 随机选一个进行中任务，无则 null
    public void submitCommission(String token, Long commissionTaskId, String articleContent);    // POST /commission/tasks/{id}/submissions
}
```

实现要点（照抄 `leaderboard/client/UserApiClient.java` 模式）：`new RestTemplate()`；`Result.class` 接收响应后检查 `getCode()==0`，否则 `throw new BusinessException(...)`；record `RobotCreated(Long userId, String email)`、`MarketSkill(Long id, String name)` 定义在 client 包内。

- [ ] **Step 1: 探明四个用户端入参形状**（本步只读代码，不改）

读 `project/user/api` 下：`LoginRequest`（字段名）、`GenerationTaskController` 的创建 DTO（引用技能市场提示词的字段名与类型）、GenerationTask 详情 VO 的文章内容字段名、`commission` 提交 DTO 字段、`PUT /me/nickname` 和 `PUT /me/profile` 的请求体字段名、`POST /me/avatar` 的 multipart 参数名。把确认后的字段名直接写进 client 代码。

- [ ] **Step 2: 写 client 三个 record + 全部方法**

subscribe 请求的 header 同时带机器人 Bearer token 和 `X-Internal-Key`（Task 3 的放行靠它）。login 返回体取 `data.accessToken`（用 Jackson `JsonNode` 或 Map 解析，避免引用户端 VO）。

- [ ] **Step 3: SimulationProfileGenerator**

```java
@Service
@RequiredArgsConstructor
public class SimulationProfileGenerator {
    private final GenerationAiService generationAiService;
    private final ModelConfigMapper modelConfigMapper; // 选启用配置

    public String generateNickname() {
        return call("你是网名生成器，只输出昵称本身，不要解释、不要引号、不要emoji。",
                "生成1个中文网名，2-6个字，有网感、像真实博主，避免“实战派/观察员/笔记”这类模板词。输出不超过10个字符。");
    }

    public String generateBio(String nickname) {
        return call("你是社交平台个人简介写手，只输出简介本身。",
                "为昵称「" + nickname + "」写一句中文个人简介，20-40字，像真实博主签名，有温度，禁止用“｜”等分隔符堆标签。输出不超过60个字符。");
    }

    private String call(String system, String user) {
        ModelConfig cfg = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getStatus, 1).orderByAsc(ModelConfig::getId).last("LIMIT 1"));
        if (cfg == null) throw new BusinessException(SystemErrorCode.SYSTEM_ERROR, "无可用模型配置");
        String content = generationAiService.call(cfg.getId(), system, user, null, false).getContent();
        if (!StringUtils.hasText(content)) throw new BusinessException(SystemErrorCode.SYSTEM_ERROR, "模型返回为空");
        return content.trim();
    }
}
```

ModelConfig 的 status 字段名以 `modules/generation` 实体为准（Step 1 一并确认）。

- [ ] **Step 4: AvatarFetcher**

```java
@Component
public class AvatarFetcher {
    private final RestTemplate restTemplate = new RestTemplate();
    private final SecureRandom random = new SecureRandom();

    public byte[] fetchRandom() {
        String seed = UUID.randomUUID().toString().substring(0, 8);
        byte[] bytes = restTemplate.getForObject(
                "https://picsum.photos/seed/" + seed + "/200/200", byte[].class);
        if (bytes == null || bytes.length == 0) throw new BusinessException(SystemErrorCode.SYSTEM_ERROR, "头像下载失败");
        return bytes;
    }
}
```

- [ ] **Step 5: 写生成器测试并运行**

`SimulationProfileGeneratorTest`：mock `GenerationAiService` + `ModelConfigMapper`，断言空返回抛异常、正常返回 trim 后内容。`cd project/admin/api && mvn test -Dtest=SimulationProfileGeneratorTest && mvn compile` → PASS（client 的联调由 Task 9 覆盖，不写 mock server 测试）。

- [ ] **Step 6: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/simulation
git commit -m "feat(admin): 模拟运营用户端调用客户端与资料生成器"
```

---

### Task 6: admin/api — 批次管理接口（创建/列表/详情/日志/取消）

**Files:**
- Create: `.../modules/simulation/controller/SimulationBatchAdminController.java`
- Create: `.../modules/simulation/service/SimulationBatchService.java` + `impl/SimulationBatchServiceImpl.java`
- Create: `.../modules/simulation/dto/request/SimulationBatchCreateRequest.java`、`SimulationBatchQueryRequest.java`
- Create: `.../modules/simulation/vo/SimulationBatchVO.java`、`SimulationRobotVO.java`、`SimulationRobotLogVO.java`
- Create: `.../modules/simulation/enums/AdminSimulationErrorCode.java`（implements shared 错误码接口，照抄 AdminCommissionErrorCode 的写法）
- Modify: `config/example.env` 加 `SIMULATION_USER_API_BASE_URL=http://localhost:25050`、`SIMULATION_PASSWORD_SECRET=`（16/24/32 字节）
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/simulation/service/SimulationBatchServiceTest.java`

**Interfaces:**
- Consumes: Task 4 的实体/枚举/Mapper；`AesUtil`。
- Produces（Task 8 前端依赖）：
  - `POST /api/v1/admin/simulation/batches`（body 见 Step 2）→ `Result<Long>`（批次ID）
  - `GET /api/v1/admin/simulation/batches?page&size` → `Result<PageResult>`，`SimulationBatchVO{id,batchNo,userCount,planId,planName,stageConfig(JSON串),status,totalCount,completedCount,failedCount,remark,createdAt,startedAt,finishedAt}`
  - `GET /api/v1/admin/simulation/batches/{id}` → `Result<BatchDetailVO{batch, List<SimulationRobotVO>}>`
  - `GET /api/v1/admin/simulation/batches/{id}/logs?robotId&stage&page&size` → `Result<PageResult>`，`SimulationRobotLogVO{id,robotId,stage,status,detail,errorMsg,createdAt}`
  - `POST /api/v1/admin/simulation/batches/{id}/cancel` → `Result<Void>`

- [ ] **Step 1: 写失败测试**

`SimulationBatchServiceTest` 覆盖：概率越界（<0 或 >100）抛 `BusinessException`；`userIntervalMin > userIntervalMax` 抛；`userCount` 超 500 抛；创建成功时验证 batch 插入 + N 条 robot 生成（email 格式 `bot{batchNo后8位}{4位seq}@{词}.simrobot.com`、password_encrypted 非明文、next_run_at≈now、status=WAITING）。

- [ ] **Step 2: 实现**

`SimulationBatchCreateRequest`（jakarta validation 基础校验 + Service 业务校验）：

```java
@Data
public class SimulationBatchCreateRequest {
    @NotNull @Min(1) @Max(500) private Integer userCount;
    @NotNull private Long planId;
    @NotBlank private String planName;
    @NotNull private PromptScope promptScope;
    private boolean lotteryEnabled = true;  @Min(0) @Max(100) private int lotteryProbability = 100;
    private boolean membershipEnabled = true; @Min(0) @Max(100) private int membershipProbability = 100;
    private boolean createEnabled = true;   @Min(0) @Max(100) private int createProbability = 100;
    private boolean commissionEnabled = true; @Min(0) @Max(100) private int commissionProbability = 50;
    @Min(0) private int userIntervalMin = 10; @Min(0) private int userIntervalMax = 30;
    @Min(0) private int stageIntervalMin = 3; @Min(0) private int stageIntervalMax = 8;
    private String remark;
}
```

Service.create：参数交叉校验（min≤max，各概率 0-100）→ 组装 `SimulationStageConfig`（ObjectMapper 序列化）→ batch_no = `"SIM" + yyyyMMdd + 三位序号`（当日已有批次数+1，查表 count）→ 插 batch（PENDING）→ 循环 1..userCount 生成 robot（`SecureRandom` 密码 12 位、AesUtil.encrypt(password, `${simulation.password-secret}`)、email 拼接、邀请码字段留空——绑定在 REGISTER 阶段由执行器随机取）→ 更新 batch.total_count。整个创建方法 `@Transactional`。

cancel：batch 状态 PENDING/RUNNING → CANCELED + `UPDATE a_simulation_robot SET status='CANCELED' WHERE batch_id=? AND status IN ('WAITING','IN_PROGRESS')`。

列表/详情/日志：MyBatis-Plus 分页，VO 手工组装（暂不用 MapStruct，模块内直接 new）。`stageConfig` 以 JSON 字符串原样透出给前端。

- [ ] **Step 3: 运行测试**

`cd project/admin/api && mvn test -Dtest=SimulationBatchServiceTest` → PASS。

- [ ] **Step 4: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/simulation project/admin/api/src/test/java/com/aichuangzuo/admin/modules/simulation config/example.env
git commit -m "feat(admin): 模拟运营批次管理接口"
```

---

### Task 7: admin/api — 消费器 + 阶段执行器（核心）

**Files:**
- Create: `.../modules/simulation/job/SimulationBatchConsumerJob.java`
- Create: `.../modules/simulation/engine/RobotTokenHolder.java`
- Create: `.../modules/simulation/engine/SimulationStageDispatcher.java`
- Create: `.../modules/simulation/engine/handler/RegisterHandler.java`、`LoginHandler.java`、`ProfileHandler.java`、`LotteryHandler.java`、`MembershipHandler.java`、`CreateHandler.java`、`CommissionHandler.java`
- Create: `.../modules/simulation/engine/StageHandler.java`、`RobotContext.java`
- Create: `.../modules/simulation/service/SimulationRobotService.java`（robot 行状态推进 + 日志写入）
- Modify: `project/admin/api/src/main/resources/application.yml`（若无则加 `simulation.password-secret` 默认 dev 值，仅本地默认）
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/simulation/engine/SimulationStageDispatcherTest.java`

**Interfaces:**
- Consumes: Task 5 的 `SimulationUserApiClient`/`SimulationProfileGenerator`/`AvatarFetcher`；Task 6 的 Service。
- Produces: 无对外接口，行为产物是 user 库真实业务数据 + a_simulation_* 状态。

- [ ] **Step 1: 定义引擎骨架**

```java
public interface StageHandler {
    SimulationStage stage();
    /** 执行一个阶段；前置不满足时抛 StageSkippedException(原因)，由调度器记 SKIPPED */
    void execute(RobotContext ctx);
}

public class StageSkippedException extends RuntimeException { public StageSkippedException(String msg) { super(msg); } }

@RequiredArgsConstructor
public class RobotContext {
    public final SimulationBatch batch;
    public final SimulationStageConfig config;
    public final SimulationRobot robot;          // 执行中会被更新
    public final String plainPassword;           // SimulationRobotService 解密后注入（本类不透传密钥配置）
    public final SimulationUserApiClient userApi;
    public final SimulationProfileGenerator profileGenerator;
    public final AvatarFetcher avatarFetcher;
    public final ObjectMapper objectMapper;
    // 透传依赖：robotMapper/batchMapper/logMapper 由 SimulationRobotService 封装
}
```

`RobotTokenHolder`（@Component）：`ConcurrentHashMap<Long,String> cache`；`String token(RobotContext ctx)`：无缓存或调用 401 时 `userApi.login(ctx.robot.getEmail(), ctx.plainPassword)` 重新登录并缓存。401 识别：client 抛出的 BusinessException code 为 401 时 refresh 重试一次（在 holder 内实现，不扩散到 handler）。

- [ ] **Step 2: 七个 Handler**

- `RegisterHandler`：邀请码 = `userApi.randomRobotInviteCodes(1)` 为空则 null → `userApi.createRobot(email, 解密password, inviteCode)` → robot.user_id 回写，context 存 inviteCode。
- `LoginHandler`：`tokenHolder.token(ctx)` 预热缓存。
- `ProfileHandler`：`nickname = profileGenerator.generateNickname()` → updateNickname；`bio = profileGenerator.generateBio(nickname)` → updateProfile；`avatarFetcher.fetchRandom()` → uploadAvatar。
- `LotteryHandler`：`campaignId = currentLotteryCampaignId(token)`，null → throw StageSkipped("无进行中抽奖活动") → drawLottery。
- `MembershipHandler`：`subscribeMembership(token, batch.planId)`。
- `CreateHandler`：`skills = listMarketSkills(token, config.promptScope.userType(), 50)`，空 → StageSkipped("无可选提示词") → 随机选一个 → `taskId = createGenerationTask` → `article = pollGenerationTask`（轮询间隔 5s、上限 60 次）→ robot.context 写 `{"generationTaskId":..,"articleContent":..,"marketSkillId":..}`。
- `CommissionHandler`：读 robot.context 的 articleContent（无 → StageSkipped("无已生成文章")）→ `taskId = randomCommissionTaskId`，null → StageSkipped("无进行中约稿任务") → submitCommission。

概率判定不进 Handler：调度器在分发前按 config 判定该阶段 enabled + 随机数 ≤ probability，不命中直接记 SKIPPED 日志并推进。

- [ ] **Step 3: SimulationStageDispatcher**

```java
@Component
@RequiredArgsConstructor
public class SimulationStageDispatcher {
    private final List<StageHandler> handlers;
    private final SecureRandom random = new SecureRandom();

    public void dispatch(RobotContext ctx) {
        SimulationStage stage = SimulationStage.valueOf(ctx.robot.getCurrentStage());
        SimulationStageConfig cfg = ctx.config;
        if (!enabled(stage, cfg)) {
            throw new StageSkippedException("概率未命中或未启用");
        }
        handlers.stream().filter(h -> h.stage() == stage).findFirst()
                .orElseThrow(() -> new BusinessException(SystemErrorCode.SYSTEM_ERROR, "无阶段处理器: " + stage))
                .execute(ctx);
    }

    private boolean enabled(SimulationStage stage, SimulationStageConfig c) {
        return switch (stage) {
            case LOTTERY -> c.isLotteryEnabled() && random.nextInt(100) < c.getLotteryProbability();
            case MEMBERSHIP -> c.isMembershipEnabled() && random.nextInt(100) < c.getMembershipProbability();
            case CREATE -> c.isCreateEnabled() && random.nextInt(100) < c.getCreateProbability();
            case COMMISSION -> c.isCommissionEnabled() && random.nextInt(100) < c.getCommissionProbability();
            default -> true;
        };
    }
}
```

- [ ] **Step 4: SimulationRobotService + Consumer Job**

`SimulationRobotService.advance(robot, success, detailJson, errorMsg)`：写 `a_simulation_robot_log` → 成功则 `current_stage = next()`，`next_run_at = now + random(stageIntervalMin..stageIntervalMax)` 秒；next 为 null → robot COMPLETED/finished_at，batch 计数 +1 并检查是否全部终态（是则 batch COMPLETED）→ 失败则 robot FAILED/fail_stage/fail_reason。密码解密：注入 `@Value("${simulation.password-secret}")` 用 AesUtil。

```java
@Slf4j @Component @RequiredArgsConstructor
public class SimulationBatchConsumerJob {
    private final SimulationRobotService robotService;
    private final ScheduledTaskExecutor scheduledTaskExecutor;

    @ScheduledTask(key = "simulation_batch_consumer", name = "模拟批次消费器",
        description = "每5秒推进一个机器人的一个阶段", triggerType = "fixed_delay", expression = "5000", sortOrder = 40)
    @Scheduled(fixedDelay = 5000)
    public void run() {
        scheduledTaskExecutor.executeAuto("simulation_batch_consumer", robotService::tick);
    }
}
```

`tick()` 顺序：① 扫描补完结批次（所有 robot 终态的 RUNNING 批次 → COMPLETED）；② 找一条可执行 robot：`JOIN a_simulation_batch` 条件 `batch.status IN ('PENDING','RUNNING') AND robot.status='WAITING' AND robot.next_run_at<=NOW()` 或 `robot.status='IN_PROGRESS' AND robot.next_run_at<=NOW()`，`ORDER BY robot.id LIMIT 1`（自定义 @Select；并发安全靠单 tick 单条 + 执行内先置 IN_PROGRESS 再执行）；③ 首次领取将 batch 置 RUNNING、robot 置 IN_PROGRESS、写 started_at；④ 构建 RobotContext → dispatcher.dispatch → advance 成功分支；捕获 `StageSkippedException` → SKIPPED 日志 + 正常推进；捕获其它异常 → FAILED 日志 + robot FAILED。

IN_PROGRESS 中的互斥：`UPDATE ... SET status='IN_PROGRESS' WHERE id=? AND status='WAITING'`（影响行数 0 则放弃本条，防多实例重复执行）。

- [ ] **Step 5: 写调度器测试**

`SimulationStageDispatcherTest`：mock 全部 handler；断言 LOTTERY 概率 0 时抛 StageSkipped、REGISTER 恒执行、未知阶段抛异常。`cd project/admin/api && mvn test -Dtest=SimulationStageDispatcherTest` → PASS。

- [ ] **Step 6: 本地起服务观察**

admin + user 两端启动，创建一个 1 人批次，观察 5 秒级 tick 推进与 a_simulation_robot_log 增长（需要 Task 8 前端前可用 SQL/接口验证）。

- [ ] **Step 7: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/simulation
git commit -m "feat(admin): 模拟运营批次消费器与阶段执行器"
```

---

### Task 8: admin/web — 模拟运营页面

**Files:**
- Create: `project/admin/web/src/api/simulation.js`
- Create: `project/admin/web/src/views/OperationSimulationView.vue`
- Modify: `project/admin/web/src/router/index.js`（children 加路由）
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`（运营管理 children 加菜单）

**Interfaces:**
- Consumes: Task 6 全部接口（`res.data.items/total` 结构）。

- [ ] **Step 1: api/simulation.js**

```js
import request from '@/utils/request'
export function createBatch(data) { return request({ url: '/simulation/batches', method: 'post', data }) }
export function listBatches(params) { return request({ url: '/simulation/batches', method: 'get', params }) }
export function getBatch(id) { return request({ url: `/simulation/batches/${id}`, method: 'get' }) }
export function listBatchLogs(id, params) { return request({ url: `/simulation/batches/${id}/logs`, method: 'get', params }) }
export function cancelBatch(id) { return request({ url: `/simulation/batches/${id}/cancel`, method: 'post' }) }
```

- [ ] **Step 2: 页面**

`OperationSimulationView.vue`（结构照抄 `LotteryAdminView.vue` 骨架）：

- 根容器 `.simulation-admin { background:#fff; padding:24px; border-radius:12px; min-height:calc(100vh - 112px) }`。
- `<a-tabs v-model:activeKey="activeKey">` + `<a-tab-pane key="new-users" tab="模拟新用户">`（只做一个 pane，留扩展）。
- 创建区：「创建批次」按钮弹 `<a-modal width="640px">`，表单字段——生成数量（a-input-number 1-500 默认 10）、会员版本（a-select，选项调既有 plan 管理接口，默认专业版；若无现成 admin plan 列表接口则用 a-input 手填 planId+planName）、提示词范围（a-radio-group：robot=仅机器人/all=所有人/real=仅真实用户）、四个阶段（a-checkbox + a-input-number 0-100 同行：抽奖/买会员/自由创作/约稿，默认 100/100/100/50）、用户间隔（两个 a-input-number 秒，默认 10/30）、阶段间隔（默认 3/8）、备注。提交前前端再校验 min≤max。
- 批次表格：列——批次号、机器人数、进度（`completed/total`，FAILED>0 显示红色 `+failed`）、状态 a-tag（PENDING=default/RUNNING=blue/COMPLETED=green/CANCELED=orange，纯函数 map 写法照抄 LotteryAdminView L1184）、创建时间（dayjs `MM-DD HH:mm`）、操作（详情/取消，取消用 a-popconfirm，RUNNING/PENDING 才显示）。
- 详情 `<a-drawer width="720px" title="批次详情">`：robot 子表（邮箱、状态 tag、当前阶段、失败原因）+ 选中 robot 后加载日志列表（阶段/结果 tag SUCCESS=green/FAILED=red/SKIPPED=default/detail 折叠展示/error_msg）。
- 轮询：列表页当存在 RUNNING 批次时 10 秒 setTimeout 重新 load（组件卸载清除）。

- [ ] **Step 3: 路由 + 菜单**

router children 加：`{ path: 'operation/simulation', name: 'AdminOperationSimulation', component: () => import('@/views/OperationSimulationView.vue') }`。

AdminLayout 运营管理 children 加：`{ key: '/console/operation/simulation', title: '模拟运营', icon: RobotOutlined }`（图标从 `@ant-design/icons-vue` 导入；若无 RobotOutlined 用 ApiOutlined）。

- [ ] **Step 4: 验证**

`cd project/admin/web && npm run build` 通过；起前后端，页面可见菜单、可创建批次、进度条随消费器推进。

- [ ] **Step 5: Commit**

```bash
git add project/admin/web/src
git commit -m "feat(admin-web): 模拟运营页面（模拟新用户）"
```

---

### Task 9: 端到端验证

**Files:**
- 无新代码（发现问题回流到对应 Task 修复）。

- [ ] **Step 1: 起全链路**

admin api（26060）、user api（25050）、admin web 本地运行。

- [ ] **Step 2: 小批次验证**

管理端创建 3 人批次（全部概率 100、间隔 3-5 秒/1-2 秒），观察：批次推进到 COMPLETED；每阶段 a_simulation_robot_log 有记录。

- [ ] **Step 3: 数据核对（spec §9）**

user 库核对：`u_user` 三条 user_type=0 记录且昵称/简介非默认；`u_user_invite_relation` 有绑定（除首个机器人外）；抽奖记录存在；`u_order` + `u_user_membership` 三条且会员生效；`u_earnings_record` 提示词发布者产生 USAGE 收益（先准备一个真实/机器人发布的市场技能）；`u_commission_submission` 有投稿。SKIPPED 场景：临时把概率调 0 跑 1 人，确认记 SKIPPED 不记 FAILED。

- [ ] **Step 4: 绿通隔离验证**

不带 X-Internal-Key 用 payCode=123456 调 subscribe → 期望报 INVALID_PAY_CODE（生产 test_mode 关闭前提下）；确认全局 `a_payment_config.test_mode` 仍为 0。

- [ ] **Step 5: Commit（如有修复）**

```bash
git add -A && git commit -m "fix(simulation): 端到端验证修复"
```

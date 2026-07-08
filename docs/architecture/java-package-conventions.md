# Java 项目分包规范

> 本文档定义爱创作（AI Creation）项目后端 Java 代码的分包规范，适用于用户端 API（`project/user/api`）、管理端 API（`project/admin/api`）以及公共模块（`project/shared`）。

---

## 1. 总体原则

- **按业务端隔离**：用户端与管理端代码彻底分开，不共享 `Controller` / `Service` / `Mapper`。
- **模块内按领域分包**，领域内再按技术层分包。避免纯 `controller/service/mapper` 全局平铺导致后期爆炸。
- **共享代码下沉**：仅当用户端与管理端真正共用时，才放入 `project/shared/`。
- **依赖方向**：`api` → `shared`，同端模块之间可横向依赖，禁止用户端依赖管理端代码，禁止管理端依赖用户端代码。
- **包命名**：`com.aichuangzuo.{端}.{模块/分层}`。
- **Entity 命名**：去掉表名前缀 `u_` / `a_`，直接用领域对象名（如表 `u_article` 对应实体 `Article`）。

---

## 2. Maven 模块结构

```text
project/
├── pom.xml                       # 父 POM，统一依赖版本
├── shared/                       # 公共模块，被 user/api 与 admin/api 依赖
│   └── src/main/java/com/aichuangzuo/shared/
├── user/api/                     # 用户端后端
│   └── src/main/java/com/aichuangzuo/user/
└── admin/api/                    # 管理端后端
    └── src/main/java/com/aichuangzuo/admin/
```

---

## 3. 用户端分包：`com.aichuangzuo.user`

```text
com.aichuangzuo.user/
├── UserApiApplication.java
├── config/                       # 全局配置
│   ├── SecurityConfig.java
│   ├── MybatisPlusConfig.java
│   ├── CaffeineConfig.java
│   ├── WebMvcConfig.java
│   ├── JwtConfig.java
│   └── SchedulerConfig.java
├── common/                       # 本端公共，不跨端
│   ├── constant/
│   ├── util/
│   ├── exception/
│   └── result/
├── infrastructure/               # 基础设施
│   ├── persistence/              # 持久化相关
│   │   ├── handler/              # 类型处理器、字段填充策略
│   │   └── repository/           # 可选：仓储封装
│   ├── cache/                    # Caffeine 封装
│   ├── storage/                  # 本地文件存储
│   ├── client/                   # LLM / 第三方 API 调用
│   └── security/                 # JWT、当前用户上下文
├── modules/                      # 业务模块
│   ├── auth/
│   │   ├── controller/
│   │   ├── service/
│   │   │   └── impl/
│   │   ├── mapper/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── vo/
│   │   ├── converter/            # MapStruct
│   │   └── constant/
│   ├── user/
│   ├── article/
│   ├── generation/
│   ├── template/
│   ├── style/
│   ├── credit/
│   └── billing/                  # 订单 + 支付合并模块
└── job/                          # 定时任务
    ├── GenerationTaskConsumer.java
    ├── CreditResetJob.java
    └── StatCleanupJob.java
```

### 3.1 用户端模块说明

| 模块 | 职责 |
|---|---|
| `auth` | 注册、登录、JWT 刷新、登出、找回密码 |
| `user` | 用户信息、资料、头像、账号设置 |
| `article` | 文章 CRUD、版本、预览、导出 |
| `generation` | 生成任务提交、取消、状态轮询 |
| `template` | 用户模板收藏/使用记录 |
| `style` | 用户自定义风格预设 |
| `credit` | 额度账户、额度流水、月度重置 |
| `billing` | 订单 + 支付（含回调） |

---

## 4. 管理端分包：`com.aichuangzuo.admin`

结构与用户端对称，业务模块不同：

```text
com.aichuangzuo.admin/
├── AdminApiApplication.java
├── config/
├── common/
├── infrastructure/
│   ├── persistence/
│   ├── cache/
│   ├── storage/
│   ├── client/
│   └── security/
├── modules/
│   ├── auth/                     # 管理员登录
│   ├── adminuser/                # 管理员账号
│   ├── role/
│   ├── permission/
│   ├── systemconfig/
│   ├── operationlog/
│   ├── globaltemplate/
│   ├── globalstyle/
│   ├── modelconfig/
│   ├── pricepackage/
│   ├── user/                     # C 端用户管理（只读/审核）
│   ├── article/                  # C 端内容管理
│   ├── generation/               # 生成任务监控
│   └── dashboard/                # 统计数据
└── job/
```

---

## 5. 公共模块分包：`com.aichuangzuo.shared`

```text
com.aichuangzuo.shared/
├── constants/                    # 全局常量
├── enums/                        # 全局枚举
├── models/                       # 跨端通用 POJO
├── dto/                          # 跨端通用 DTO
├── result/                       # 统一响应封装
├── exception/                    # 通用异常基类
├── utils/                        # 通用工具类
└── validator/                    # 通用校验注解
```

### 5.1 什么应该放 shared

- 全局响应码、统一 `Result<T>`
- 全局异常基类
- 跨端共用的枚举（如 `ArticleStatus`、`TaskStatus`、`PlatformType`）
- 工具类（日期、JSON、加密、分页包装）
- 跨端引用的 DTO（极少，通常不跨端）

### 5.2 什么不应该放 shared

- 用户端或管理端专属常量/枚举
- 与 Spring Security 强耦合的类
- 业务 Service / Mapper / Controller

---

## 6. 业务模块内部分层规范

每个业务模块内部统一：

```text
com.aichuangzuo.user.modules.article/
├── controller/                   # 仅暴露 HTTP 入口
├── service/                      # 业务接口
│   └── impl/                     # 业务实现
├── mapper/                       # MyBatis-Plus Mapper
├── entity/                       # MyBatis-Plus Entity
├── dto/                          # 入参对象
│   ├── request/
│   └── response/
├── vo/                           # 出参视图对象
├── converter/                    # MapStruct 转换器
└── constant/                     # 模块常量
```

### 6.1 命名规范

| 类型 | 命名 | 示例 |
|---|---|---|
| Controller | `{Module}Controller` | `ArticleController` |
| Service 接口 | `{Module}Service` | `ArticleService` |
| Service 实现 | `{Module}ServiceImpl` | `ArticleServiceImpl` |
| Mapper | `{Module}Mapper` | `ArticleMapper` |
| Entity | 去掉表前缀后的驼峰名 | 表 `u_article` → `Article` |
| DTO | `{Action}{Module}Request` / `...Response` | `CreateArticleRequest` |
| VO | `{Module}VO` / `{Module}DetailVO` | `ArticleVO` |
| Converter | `{Module}Converter` | `ArticleConverter` |
| Constant | `{Module}Constants` | `ArticleConstants` |

### 6.2 Entity 与表名映射示例

| 表名 | 端 | Entity 类名 | 所在包 |
|---|---|---|---|
| `u_user` | 用户端 | `User` | `com.aichuangzuo.user.modules.user.entity` |
| `u_article` | 用户端 | `Article` | `com.aichuangzuo.user.modules.article.entity` |
| `u_generation_task` | 用户端 | `GenerationTask` | `com.aichuangzuo.user.modules.generation.entity` |
| `a_admin_user` | 管理端 | `AdminUser` | `com.aichuangzuo.admin.modules.adminuser.entity` |
| `a_system_config` | 管理端 | `SystemConfig` | `com.aichuangzuo.admin.modules.systemconfig.entity` |

---

## 7. 依赖与调用规则

```text
Controller
    ↓
Service
    ↓
Mapper / Entity
    ↓
shared / infrastructure
```

- `Controller` 不允许直接调用 `Mapper`。
- `Service` 之间允许同层调用，优先通过编排 Service 解耦；复杂跨模块场景可考虑事件机制。
- 禁止跨端 Service 直接调用；管理端需要读取用户端数据时，通过用户端暴露的 Internal API 或复用 `shared` 中定义的只读 DTO。
- 核心配置类统一放 `config/`，模块级配置可放模块内 `config/`。
- 定时任务统一放 `job/`，禁止在 `Service` 里写 `@Scheduled`。

---

## 8. 关键配置类位置

| 配置 | 位置 |
|---|---|
| Spring Security / JWT | `com.aichuangzuo.user.config.SecurityConfig` |
| MyBatis-Plus | `com.aichuangzuo.user.config.MybatisPlusConfig` |
| 字段自动填充 | `com.aichuangzuo.user.infrastructure.persistence.handler.*` |
| Caffeine | `com.aichuangzuo.user.config.CaffeineConfig` |
| 文件存储 | `com.aichuangzuo.user.infrastructure.storage.*` |
| LLM 调用客户端 | `com.aichuangzuo.user.infrastructure.client.*` |
| 生成任务消费 | `com.aichuangzuo.user.job.GenerationTaskConsumer` |

---

## 9. 禁止事项

- 禁止在 `shared` 模块中引入 Spring Web / Spring Security 依赖。
- 禁止用户端和管理端代码互相导入。
- 禁止纯 `controller/service/mapper/entity` 在根目录平铺。
- 禁止在 `entity` 中写业务逻辑。
- 禁止在 `Service` 中直接使用 `HttpServletRequest`。
- 禁止工具类依赖业务模块。
- 禁止 Entity 类名保留 `u_` / `a_` 表前缀。

### 9.1 例外：领域聚合根下允许二级模块

当某个业务领域由多个强内聚的子领域组成，且它们共用同一张聚合根表或同一组上下游概念时，允许在该领域目录下再按子领域分包。例如：

```text
com.aichuangzuo.admin.modules.style/
├── entity/UserStyleAggregate.java        # 聚合根实体（表 u_user_style）
├── preset/                               # 平台预设风格
│   ├── controller/、service/、mapper/、dto/、vo/、enums/
├── market/                               # 风格广场
│   └── ...
└── review/                               # 风格审核
    └── ...
```

规则：

- 仅当父目录名 + 子目录名组合仍是清晰的领域命名空间（如 `style.preset` / `style.market` / `style.review`）时才允许嵌套。
- 嵌套层级最多两级：`modules/<领域>/<子领域>/<技术层>/`。
- 同一聚合根的 Entity 放在领域根下的 `entity/`，子领域不重复声明。
- 不得用嵌套逃避 §9 的「禁止纯技术层平铺」——子领域内部仍必须按 §6 分技术层。

---

## 10. 示例：文章模块完整包结构

```text
com.aichuangzuo.user.modules.article/
├── ArticleController.java
├── service/
│   ├── ArticleService.java
│   └── impl/
│       └── ArticleServiceImpl.java
├── mapper/
│   └── ArticleMapper.java
├── entity/
│   └── Article.java
├── dto/
│   ├── request/
│   │   ├── CreateArticleRequest.java
│   │   ├── UpdateArticleRequest.java
│   │   └── ArticlePageRequest.java
│   └── response/
│       └── ArticlePageResponse.java
├── vo/
│   ├── ArticleVO.java
│   └── ArticleDetailVO.java
├── converter/
│   └── ArticleConverter.java
└── constant/
    └── ArticleConstants.java
```

---

## 11. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：用户端/管理端/公共模块分包，订单与支付合并为 billing，Entity 去掉表前缀 | - |

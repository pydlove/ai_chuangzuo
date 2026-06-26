# 技术选型规范

> 本文档固化爱创作（AI Creation）项目的技术栈，适用于后续所有开发工作。

## 约束前提

- 服务器资源有限，**不安装 Redis、不安装独立消息队列**等额外中间件。
- 除 MySQL 数据库外，不依赖需要独立进程的外部服务。
- 优先采用可直接运行在 JVM 或应用进程内的轻量级方案。

## 技术栈总览

| 层级 | 选型 |
|---|---|
| 后端框架 | Spring Boot + Spring + MyBatis-Plus |
| JDK 版本 | 17 |
| 数据库 | MySQL 8.x |
| 数据库迁移 | Flyway |
| 缓存 | Caffeine（JVM 内存缓存） |
| 异步任务队列 | MySQL 任务表 + Spring Scheduler |
| 认证授权 | Spring Security + JWT |
| 限流 | Bucket4j / Guava RateLimiter（本地限流） |
| 文件存储 | 服务器本地磁盘 + Nginx 静态资源代理 |
| 分布式锁 | 暂不需要，单机使用 JDK 锁 |
| 前端框架 | Vue 3 |
| 前端状态管理 | Pinia |
| 前端路由 | Vue Router 4 |
| UI 组件库 | Ant Design Vue |
| HTTP 客户端 | Axios |
| 构建工具 | Maven（后端）/ Vite（前端） |
| 部署方式 | jar 包 + Nginx 反向代理 |

## 后端详细说明

### Spring Boot + Spring + MyBatis-Plus
- Spring Boot 作为基础框架，Spring Security 负责认证与权限。
- MyBatis-Plus 在 MyBatis 基础上减少样板代码，自带分页、字段填充、逻辑删除。

### JDK 17
- 使用 LTS 版本，支持记录类、模式匹配、增强的 Switch 等新特性。

### MySQL + Flyway
- MySQL 作为唯一持久化数据库。
- Flyway 管理数据库版本迁移，所有表结构变更必须通过 Flyway 脚本执行。

### Caffeine 缓存
- 替代 Redis，用于缓存模板列表、用户信息、AI 模型配置等读多写少的数据。
- 仅限单机部署；后续如需集群部署，再评估是否引入 Redis。

### MySQL 任务队列表
- 文章生成等耗时操作采用异步处理。
- 通过 `generation_task` 类似表存储任务状态，Spring Scheduler 定时轮询消费。
- 优点：持久化、可重试、重启不丢任务、无需额外中间件。

### JWT 认证
- 采用无状态 JWT，不依赖 Session 或 Redis 存储登录状态。
- Token 由前端保存并在请求头中携带。

### 本地限流
- 使用 Bucket4j 或 Guava RateLimiter 实现基于内存的限流。
- 按用户或 IP 维度限制 AI 调用频次。

## 前端详细说明

### Vue 3 + Vite
- 使用 Vue 3 Composition API 开发。
- Vite 作为构建工具，启动快、热更新效率高。

### Pinia
- 全局状态管理，替代 Vuex。

### Vue Router 4
- 单页应用路由管理。

### Ant Design Vue
- 主 UI 组件库，覆盖后台管理系统所需组件。
- 如后续需要更好的移动端体验，可叠加 Vant。

### Axios
- HTTP 请求封装，统一处理请求拦截（Token、Loading）、响应拦截（错误提示）。

## AI 调用层

- 使用 OpenFeign 或 WebClient 调用 LLM API。
- 文章生成采用异步任务模式，结果落库后由前端轮询获取。
- 如需要实时流式输出效果，后端可通过 `SseEmitter` 推送生成进度或片段。
- API Key 等敏感配置通过环境变量或配置中心管理，禁止硬编码。

## 文件存储

- 开发及初期生产环境使用服务器本地磁盘存储图片等静态资源。
- 通过 Nginx 配置静态资源访问路径。
- 后续如迁移至 OSS，需保证访问 URL 结构兼容。

## 不允许引入的组件

在明确批准前，以下中间件或服务不得引入：

- Redis
- RabbitMQ / RocketMQ / Kafka
- Elasticsearch
- MongoDB
- 独立 MinIO / OSS 服务

## 变更流程

1. 如需调整本规范，需先在本文档中记录变更理由。
2. 涉及新增中间件或服务时，必须评估服务器资源并经过确认。
3. 变更后同步更新相关设计文档和部署脚本。

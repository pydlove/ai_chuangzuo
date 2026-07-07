# 缓存规范

> 本文档定义爱创作（AI Creation）项目本地缓存（Caffeine）的使用规范，与 `docs/architecture/tech-stack.md` 中"不引入 Redis"的约束保持一致。

---

## 1. 缓存框架

- 统一使用 **Caffeine** 作为 JVM 本地缓存。
- 禁止引入 Redis、Ehcache、Guava Cache 等其他缓存中间件。
- 缓存配置集中放在 `com.aichuangzuo.user.config.CaffeineConfig`（用户端）和 `com.aichuangzuo.admin.config.CaffeineConfig`（管理端）。

---

## 2. 使用场景

以下数据适合缓存：

| 场景 | 示例 | 原因 |
|---|---|---|
| 读多写少 | 模板列表、风格列表、系统配置 | 变更频率低，查询频繁 |
| 计算成本高 | 用户额度汇总、文章字数统计 | 避免重复计算 |
| 频繁访问的单条记录 | 当前用户信息 | 减少数据库查询 |
| 临时黑名单 | JWT 失效 Token | 单机内存即可满足 |

以下数据禁止缓存：

- 实时性要求高的数据（如订单状态、任务状态）。
- 用户隐私数据（如手机号、密码哈希）。
- 大数据量列表（如文章全文、操作日志）。

---

## 3. 缓存 Key 命名

统一格式：

```text
{端}:{模块}:{业务标识}:{唯一参数}
```

示例：

```text
user:config:templates
user:config:styles
user:user:info:10001
user:credit:summary:10001
admin:config:system
admin:permission:role:1
```

规则：

- 全小写，单词间用 `:` 分隔。
- 用户相关缓存必须包含 `userId`。
- 禁止在 Key 中拼接敏感信息。

---

## 4. 过期时间策略

| 数据类型 | 建议 TTL | 说明 |
|---|---|---|
| 系统配置 | 10 分钟 | 变更不频繁，但需及时生效 |
| 模板/风格列表 | 30 分钟 | 读多写少 |
| 用户信息 | 5 分钟 | 平衡一致性与性能 |
| 额度汇总 | 1 分钟 | 对一致性要求较高 |
| JWT 黑名单 | 与 Token 剩余有效期一致 | 动态 TTL |

- 缓存配置必须显式设置 `maximumSize` 和 `expireAfterWrite` / `expireAfterAccess`。
- 禁止设置无限大小或无限期缓存。

---

## 5. 缓存更新与失效

### 5.1 写后失效

数据变更时主动删除缓存：

```java
public void updateTemplate(Template template) {
    templateMapper.updateById(template);
    caffeineCache.evict("user:config:templates");
}
```

### 5.2 查询时回源

```java
@Cacheable(value = "templates", key = "'user:config:templates'")
public List<TemplateVO> listTemplates() {
    return templateMapper.selectList();
}
```

### 5.3 更新时刷新

```java
@CachePut(value = "userInfo", key = "'user:user:info:' + #userId")
public UserVO updateUserInfo(Long userId, UpdateUserRequest request) {
    // 更新并返回最新数据
}
```

### 5.4 批量失效

模块配置变更时，可按前缀批量清除：

```java
caffeineCache.invalidate("user:config:");
```

---

## 6. 缓存问题防护

### 6.1 缓存穿透

- 查询结果为空时，缓存空值或空对象，过期时间较短（如 1 分钟）。
- 对非法参数做前置校验。

### 6.2 缓存击穿

- 热点数据设置随机 TTL 偏移（如 5 分钟 ± 30 秒）。
- 高并发访问时使用同步加载或互斥锁。

### 6.3 缓存雪崩

- 避免大量缓存同时过期，设置随机偏移量。
- 核心数据配置永不过期 + 异步刷新。

---

## 7. 本地缓存局限性

- **单机限制**：Caffeine 缓存不能跨 JVM 共享，集群部署时每个节点独立缓存。
- **一致性风险**：数据变更后，其他节点缓存不会立即失效。重要数据需缩短 TTL 或采用发布订阅机制同步（本阶段不使用 Redis Pub/Sub，可通过数据库版本号 + 短 TTL 缓解）。
- **内存限制**：单机缓存大小有限，禁止缓存大对象或大数据集。

---

## 8. 示例配置

```java
@Configuration
public class CaffeineConfig {

    @Bean("templateCache")
    public Cache<String, Object> templateCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }

    @Bean("userCache")
    public Cache<String, Object> userCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }
}
```

---

## 9. 禁止事项

- 禁止使用缓存存储敏感信息。
- 禁止缓存未设置过期时间的数据。
- 禁止在缓存中存储大对象（如文章全文、日志列表）。
- 禁止使用用户输入直接拼接缓存 Key。
- 禁止在缓存未命中时直接返回错误，必须回源数据库。

---

## 10. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：Caffeine 使用场景、Key 命名、TTL、失效策略、缓存问题防护 | - |

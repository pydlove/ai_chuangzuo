# Java 代码风格规范

> 本文档定义爱创作（AI Creation）项目 Java 后端代码的基础风格，保持简短、可执行。

---

## 1. 基础规则

- 遵循《阿里巴巴 Java 开发手册》作为基线。
- JDK 17，使用 `var` 可接受，但不得滥用。
- 每行不超过 120 字符。
- 缩进 4 空格，禁止 Tab。
- 类、方法、字段必须写 `/** */` 文档注释，说明职责与特殊逻辑。

---

## 2. 命名

| 类型 | 规则 | 示例 |
|---|---|---|
| 类名 | 大驼峰，名词 | `ArticleService` |
| 接口 | 大驼峰，名词，不加 `I` 前缀 | `ArticleRepository` |
| 方法 | 小驼峰，动词开头 | `createArticle` |
| 变量 | 小驼峰 | `articleTitle` |
| 常量 | 全大写 + 下划线 | `MAX_PAGE_SIZE` |
| 布尔 | `is` / `has` / `can` 开头 | `isPublished` |
| 集合 | 复数形式 | `articles`、`userIds` |

---

## 3. Lombok

允许：

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Slf4j
```

禁止：

- Entity 上使用 `@Data`（生成的 `toString` / `equals` 可能引发懒加载问题）。
- Entity 上使用 `@AllArgsConstructor`（破坏 JPA/MyBatis 无参构造约定）。

---

## 4. MapStruct

- 转换器类名：`{Module}Converter`。
- 注入方式：`@Mapper(componentModel = "spring")`。
- 转换方向：
  - `Request` → `Entity`
  - `Entity` → `VO`
  - `Entity` ↔ `DTO` 跨层时使用
- 禁止在 Converter 中写复杂业务逻辑。

---

## 5. MyBatis-Plus

- Service 继承 `ServiceImpl`，自定义接口继承 `IService`。
- 简单 CRUD 用 BaseMapper / IService API。
- 复杂查询用 `LambdaQueryWrapper`，超过 3 个条件建议抽方法。
- 复杂 SQL 写在 XML 中，禁止在代码里拼接长 SQL 字符串。

---

## 6. Stream / Optional

- 允许使用 Stream，但链式操作不超过 3 层。
- `Optional` 仅用于返回值，禁止用于字段和参数。
- 优先使用 `orElseThrow()`，避免裸 `.get()`。

---

## 7. 异常与日志

- 业务异常用 `BusinessException(ErrorCode)`。
- 日志使用 `@Slf4j`。
- 日志占位符 `{}`，禁止字符串拼接。
- 禁止 `e.printStackTrace()`。

---

## 8. 单元测试

- 框架：JUnit 5 + Mockito。
- 测试类命名：`{ClassName}Test`。
- 方法命名：`should{DoSomething}When{Condition}` 或中文 `创建文章成功()`。
- 覆盖核心 Service 方法，不强制追求 100% 覆盖率。

---

## 9. 禁止事项

- 禁止使用 `魔法值`，必须定义常量或枚举。
- 禁止 `new Date()`，使用 `LocalDateTime` / `Instant`。
- 禁止返回 `null` 集合，返回空集合 `Collections.emptyList()`。
- 禁止在循环中做数据库查询或远程调用。
- 禁止大段注释掉的代码提交。
- 禁止工具类实例化，必须私有构造 + `final` 类。

---

## 10. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：命名、Lombok、MapStruct、MyBatis-Plus、Stream/Optional、测试、禁止事项 | - |

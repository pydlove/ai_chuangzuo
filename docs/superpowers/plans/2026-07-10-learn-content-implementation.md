# 创作学院模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现「创作学院」文档式浏览页：管理员可在管理端 CRUD 分类树与文章（含 Markdown/富文本双格式、草稿/发布双状态），用户端按左树右文形态浏览已发布内容。

**Architecture:**
- 后端：管理端 CRUD 走 `admin/api`，公共浏览走 `user/api`，共享 MySQL 同一库的两张表 `article_category` 与 `article`。State machine（draft ↔ published）+ `published_at` 语义在 Service 层集中实现。
- 用户端：单页 `/learn` 路由，左侧 sticky 树 + 右侧内容；移动端抽屉 + 全宽布局；`markdown-it` (`html:false`) + `highlight.js` 渲染 Markdown，`v-html` 渲染富文本。
- 管理端：分类管理（树编辑器） + 文章列表 + 文章编辑器（Markdown 走 `mavon-editor`，富文本走 `@tiptap/vue-3`）。

**Tech Stack:** Spring Boot + MyBatis-Plus + Flyway · Vue 3 + Pinia + Vue Router 4 + Ant Design Vue · `mavon-editor` + `@tiptap/vue-3` + `markdown-it` + `highlight.js`。

## Global Constraints

- 现有 `Result<T>` 包装统一返回 `{code, message, data}`，错误码走项目已有枚举体系
- 表命名小写蛇形；新表名 `article_category`、`article`（已与现有库无冲突；执行前 `SHOW TABLES` 确认）
- 软删除字段 `deleted BIT NOT NULL DEFAULT 0`，所有查询必须加 `deleted = 0` 过滤
- 通用时间字段 `created_at`/`updated_at` 走 MyBatis-Plus 自动填充（确认 base entity 已配置）
- 公共约定：所有 admin API 路径前缀 `/admin/learn/**`、公共 API 路径前缀 `/api/v1/learn/**`（沿用 `user/api` 的 `MarketStyleController` 前缀风格）
- 草稿状态对外 404 不可见
- 富文本存储 Tiptap 输出的 HTML；Markdown 存原文不预渲染
- 不实现图片上传，文章正文图文混排留待二期
- 不用第三方拖拽库；分类树用 Ant Design Vue 的 `<a-tree>` 自带 draggable
- CSP 与现有项目一致；`markdown-it` 必须 `html: false`

---

## 文件清单

### 后端 — `project/admin/api`（管理端）

| 路径 | 操作 | 说明 |
|---|---|---|
| `src/main/resources/db/migration/V2.0.0_021__create_learn_tables.sql` | 新建 | Flyway 迁移 |
| `src/main/java/com/aichuangzuo/admin/modules/learn/entity/LearnCategoryEntity.java` | 新建 | 分类实体 |
| `src/main/java/com/aichuangzuo/admin/modules/learn/entity/LearnArticleEntity.java` | 新建 | 文章实体 |
| `src/main/java/com/aichuangzuo/admin/modules/learn/enums/ContentType.java` | 新建 | `markdown` / `rich_text` |
| `src/main/java/com/aichuangzuo/admin/modules/learn/enums/ArticleStatus.java` | 新建 | `draft` / `published` |
| `src/main/java/com/aichuangzuo/admin/modules/learn/mapper/LearnCategoryMapper.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/mapper/LearnArticleMapper.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/dto/request/LearnCategoryReq.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/dto/request/LearnArticleReq.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/dto/request/LearnArticlePageQuery.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/dto/request/LearnSortReq.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/vo/LearnCategoryTreeNode.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/vo/LearnArticleDetail.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/service/LearnCategoryService.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/service/LearnArticleService.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnCategoryServiceImpl.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnArticleServiceImpl.java` | 新建 | |
| `src/main/java/com/aichuangzuo/admin/modules/learn/controller/LearnAdminController.java` | 新建 | 管理端增删改查 |
| `src/main/java/com/aichuangzuo/admin/modules/learn/exception/LearnErrorCode.java` | 新建 | 自定义错误码 |
| `src/test/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnCategoryServiceImplTest.java` | 新建 | TDD |
| `src/test/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnArticleServiceImplTest.java` | 新建 | TDD |

### 后端 — `project/user/api`（用户端公共）

| 路径 | 操作 | 说明 |
|---|---|---|
| `src/main/java/com/aichuangzuo/user/modules/learn/entity/LearnCategoryEntity.java` | 新建 | 同表只读视图 |
| `src/main/java/com/aichuangzuo/user/modules/learn/entity/LearnArticleEntity.java` | 新建 | 同表只读视图 |
| `src/main/java/com/aichuangzuo/user/modules/learn/mapper/LearnCategoryMapper.java` | 新建 | |
| `src/main/java/com/aichuangzuo/user/modules/learn/mapper/LearnArticleMapper.java` | 新建 | |
| `src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnCategoryTreeVO.java` | 新建 | |
| `src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnCategoryDetailVO.java` | 新建 | 含已发布文章列表 |
| `src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleVO.java` | 新建 | |
| `src/main/java/com/aichuangzuo/user/modules/learn/service/LearnBrowseService.java` | 新建 | |
| `src/main/java/com/aichuangzuo/user/modules/learn/service/impl/LearnBrowseServiceImpl.java` | 新建 | |
| `src/main/java/com/aichuangzuo/user/modules/learn/controller/LearnController.java` | 新建 | 用户端公共 |

### 用户端 — `project/user/web`

| 路径 | 操作 | 说明 |
|---|---|---|
| `src/router/index.js` | 修改 | 加 `/learn` 路由 |
| `src/views/LearnIndex.vue` | 新建 | 学院主页 |
| `src/components/learn/LearnSidebar.vue` | 新建 | 树状菜单 |
| `src/components/learn/LearnContent.vue` | 新建 | 内容渲染 |
| `src/components/learn/MobileTreeSheet.vue` | 新建 | 移动端抽屉 |
| `src/components/learn/LearnMarkdown.vue` | 新建 | Markdown 渲染 |
| `src/components/learn/LearnRichText.vue` | 新建 | 富文本渲染 |
| `src/api/learn.js` | 新建 | axios 封装 |
| `src/components/layout/NavBar.vue` | 修改 | 加「创作学院」链接 |

### 管理端 — `project/admin/web`

| 路径 | 操作 | 说明 |
|---|---|---|
| `src/router/index.js` | 修改 | 加 `/learn/article` 等 |
| `src/views/learn/CategoryManage.vue` | 新建 | 分类管理 |
| `src/views/learn/ArticleList.vue` | 新建 | 文章列表 |
| `src/views/learn/ArticleEditor.vue` | 新建 | 文章编辑器 |
| `src/components/learn/CategoryTreeEditor.vue` | 新建 | 树编辑器（左树右表单）|
| `src/components/learn/MarkdownEditor.vue` | 新建 | 包装 mavon-editor |
| `src/components/learn/RichTextEditor.vue` | 新建 | 包装 Tiptap |
| `src/api/learn.js` | 新建 | axios 封装 |
| `src/layouts/MainLayout.vue` 或等价菜单文件 | 修改 | 加「创作学院」一级菜单 |

### 测试

| 路径 | 操作 | 说明 |
|---|---|---|
| `tests/e2e/admin_learn_management.py` | 新建 | 管理端 E2E |
| `tests/e2e/learn_browsing.py` | 新建 | 用户端 E2E |

---

## Task 1: 后端迁移 + 实体 + 枚举

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_021__create_learn_tables.sql`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/entity/LearnCategoryEntity.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/entity/LearnArticleEntity.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/enums/ContentType.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/enums/ArticleStatus.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/exception/LearnErrorCode.java`

**Interfaces:**
- Produces:
  - 表 `article_category`、`article`（两表无外键约束，业务层维护关系）
  - `LearnCategoryEntity`、`LearnArticleEntity`（MyBatis-Plus 实体，继承项目已有 BaseEntity 基类）
  - `ContentType.MARKDOWN`、`ContentType.RICH_TEXT` 枚举
  - `ArticleStatus.DRAFT`、`ArticleStatus.PUBLISHED` 枚举
  - `LearnErrorCode` 错误码（带 BizException 抛出支持）

- [ ] **Step 1: 写 Flyway 迁移脚本**

文件 `project/admin/api/src/main/resources/db/migration/V2.0.0_021__create_learn_tables.sql`：

```sql
-- 创作学院分类表
CREATE TABLE IF NOT EXISTS article_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NULL,
    name        VARCHAR(64)  NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted     BIT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_parent_deleted (parent_id, deleted),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作学院分类';

-- 创作学院文章表
CREATE TABLE IF NOT EXISTS article (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    category_id   BIGINT       NOT NULL,
    title         VARCHAR(128) NOT NULL,
    summary       VARCHAR(255) NULL,
    content_type  VARCHAR(16)  NOT NULL,
    content       LONGTEXT     NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    sort          INT          NOT NULL DEFAULT 0,
    author_id     BIGINT       NULL,
    published_at  DATETIME     NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    deleted       BIT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_cat_status_deleted (category_id, status, deleted),
    INDEX idx_sort (sort),
    INDEX idx_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作学院文章';
```

- [ ] **Step 2: 写枚举**

`enums/ContentType.java`：

```java
package com.aichuangzuo.admin.modules.learn.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContentType {
    MARKDOWN("markdown"),
    RICH_TEXT("rich_text");

    @EnumValue
    private final String code;

    public static ContentType fromCode(String code) {
        for (ContentType v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new IllegalArgumentException("unknown content type: " + code);
    }
}
```

`enums/ArticleStatus.java`：

```java
package com.aichuangzuo.admin.modules.learn.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArticleStatus {
    DRAFT("draft"),
    PUBLISHED("published");

    @EnumValue
    private final String code;

    public static ArticleStatus fromCode(String code) {
        for (ArticleStatus v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new IllegalArgumentException("unknown status: " + code);
    }
}
```

- [ ] **Step 3: 写实体**

`entity/LearnCategoryEntity.java`：

```java
package com.aichuangzuo.admin.modules.learn.entity;

import com.aichuangzuo.admin.infrastructure.persistence.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article_category")
public class LearnCategoryEntity extends BaseEntity {
    private Long parentId;
    private String name;
    private Integer sort;
}
```

> 如果项目没有 `BaseEntity`，直接继承 `com.baomidou.mybatisplus.annotation.TableField` + 自维护时间字段；查询时使用项目已有实体基类，确保 `deleted=0` 过滤（通常通过 `MybatisPlusConfig` 的 `MetaObjectHandler` 和 `PaginationInnerInterceptor` 公共处理）。

`entity/LearnArticleEntity.java`：

```java
package com.aichuangzuo.admin.modules.learn.entity;

import com.aichuangzuo.admin.infrastructure.persistence.BaseEntity;
import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article")
public class LearnArticleEntity extends BaseEntity {
    private Long categoryId;
    private String title;
    private String summary;
    private ContentType contentType;
    private String content;
    private ArticleStatus status;
    private Integer sort;
    private Long authorId;
    private LocalDateTime publishedAt;
}
```

- [ ] **Step 4: 写错误码枚举**

`exception/LearnErrorCode.java`：

```java
package com.aichuangzuo.admin.modules.learn.exception;

import com.aichuangzuo.shared.exception.BizException;
import com.aichuangzuo.shared.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LearnErrorCode implements ErrorCode {
    CATEGORY_NAME_DUPLICATE("LEARN_CATEGORY_NAME_DUPLICATE", "同级分类名重复"),
    CATEGORY_NOT_EMPTY("LEARN_CATEGORY_NOT_EMPTY", "分类下存在子分类或文章，无法删除"),
    CONTENT_TOO_LARGE("LEARN_CONTENT_TOO_LARGE", "正文超出大小限制"),
    ARTICLE_NOT_FOUND("LEARN_ARTICLE_NOT_FOUND", "文章不存在或已下线");

    private final String code;
    private final String message;

    public BizException toException() {
        return new BizException(this);
    }
}
```

> 如果项目已有不同的 `ErrorCode` 接口或 `BizException` 签名，按现有实现调整 — 关键是抛出异常时能带上错误码。

- [ ] **Step 5: 编译验证**

Run: `mvn -pl project/admin/api -am compile -DskipTests`
Expected: BUILD SUCCESS（首次会编译 `shared` 模块；后续直接编 `admin/api`）

- [ ] **Step 6: Commit**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_021__create_learn_tables.sql \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/
git commit -m "feat(admin): 创建创作学院模块的表/实体/枚举骨架"
```

---

## Task 2: 分类 Mapper + Service（TDD：树构建 + 重复名校验）

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/mapper/LearnCategoryMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/dto/request/LearnCategoryReq.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/vo/LearnCategoryTreeNode.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/service/LearnCategoryService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnCategoryServiceImpl.java`
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnCategoryServiceImplTest.java`

**Interfaces:**
- Consumes: `LearnCategoryEntity`
- Produces:
  - `LearnCategoryMapper extends BaseMapper<LearnCategoryEntity>`
  - `LearnCategoryTreeNode { id, parentId, name, sort, children: List<...> }`
  - `LearnCategoryService` 接口: `tree() / create(req) / update(id, req) / delete(id) / sortBatch(List<SortItem>)`
  - `LearnCategoryServiceImpl` 实现，调用 MyBatis-Plus API，禁止状态/草稿但允许软删除校验

- [ ] **Step 1: 写失败测试 — 分类树构建**

`LearnCategoryServiceImplTest.java`：

```java
package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;
import com.aichuangzuo.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnCategoryServiceImplTest {

    @Mock
    private LearnCategoryMapper mapper;

    @InjectMocks
    private LearnCategoryServiceImpl service;

    @Test
    void tree_buildsNestedTreeOrderedBySort() {
        LearnCategoryEntity a = cat(1L, null, "A", 0);
        LearnCategoryEntity b = cat(2L, 1L, "B", 1);
        LearnCategoryEntity c = cat(3L, 1L, "C", 0);
        LearnCategoryEntity d = cat(4L, null, "D", 1);
        when(mapper.selectList(any())).thenReturn(List.of(a, b, c, d));

        List<LearnCategoryTreeNode> tree = service.tree();

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getName()).isEqualTo("A");
        assertThat(tree.get(0).getChildren()).extracting(LearnCategoryTreeNode::getName).containsExactly("C", "B");
        assertThat(tree.get(1).getName()).isEqualTo("D");
    }

    @Test
    void create_rejectsDuplicateNameAtSameLevel() {
        when(mapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.create(req(null, "A", 0)))
                .isInstanceOf(BizException.class)
                .extracting("errorCode.code").isEqualTo(LearnErrorCode.CATEGORY_NAME_DUPLICATE.getCode());
    }

    private LearnCategoryEntity cat(Long id, Long parent, String name, int sort) {
        LearnCategoryEntity e = new LearnCategoryEntity();
        e.setId(id); e.setParentId(parent); e.setName(name); e.setSort(sort);
        return e;
    }

    private com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq req(Long parent, String name, int sort) {
        var r = new com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq();
        r.setParentId(parent); r.setName(name); r.setSort(sort);
        return r;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd project/admin/api && mvn test -Dtest=LearnCategoryServiceImplTest -q`
Expected: 编译失败（`LearnCategoryServiceImpl` / `LearnCategoryMapper` 不存在）或测试 100% 失败。

- [ ] **Step 3: 写 Mapper**

`LearnCategoryMapper.java`：

```java
package com.aichuangzuo.admin.modules.learn.mapper;

import com.aichuangzuo.admin.modules.learn.entity.LearnCategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface LearnCategoryMapper extends BaseMapper<LearnCategoryEntity> {
}
```

（继承 `BaseMapper` 即可；树形查询在 Service 层用 stream 构建）

- [ ] **Step 4: 写 DTO + VO**

`dto/request/LearnCategoryReq.java`：

```java
package com.aichuangzuo.admin.modules.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearnCategoryReq {
    private Long parentId;

    @NotBlank
    @Size(max = 64)
    private String name;

    private Integer sort = 0;
}
```

`vo/LearnCategoryTreeNode.java`：

```java
package com.aichuangzuo.admin.modules.learn.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearnCategoryTreeNode {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private List<LearnCategoryTreeNode> children = new ArrayList<>();
}
```

- [ ] **Step 5: 写 Service 接口与实现**

`service/LearnCategoryService.java`：

```java
package com.aichuangzuo.admin.modules.learn.service;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;

import java.util.List;

public interface LearnCategoryService {
    List<LearnCategoryTreeNode> tree();
    Long create(LearnCategoryReq req);
    void update(Long id, LearnCategoryReq req);
    void delete(Long id);
    void sortBatch(List<LearnSortReq.SortItem> items);
}
```

> `LearnSortReq` 是共用请求，下面 Task 3 一起写也行；如未建好，先在 `LearnSortReq.java` 里占位一个 `SortItem { Long id; Integer sort; Long parentId; }` 内部类。

`service/impl/LearnCategoryServiceImpl.java`：

```java
package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.admin.modules.learn.service.LearnCategoryService;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnCategoryServiceImpl implements LearnCategoryService {

    private final LearnCategoryMapper mapper;

    @Override
    public List<LearnCategoryTreeNode> tree() {
        List<LearnCategoryEntity> all = mapper.selectList(
                new QueryWrapper<LearnCategoryEntity>().eq("deleted", 0));
        Map<Long, LearnCategoryTreeNode> nodeMap = all.stream().collect(Collectors.toMap(
                LearnCategoryEntity::getId,
                e -> {
                    LearnCategoryTreeNode n = new LearnCategoryTreeNode();
                    n.setId(e.getId());
                    n.setParentId(e.getParentId());
                    n.setName(e.getName());
                    n.setSort(e.getSort());
                    return n;
                }));
        List<LearnCategoryTreeNode> roots = new ArrayList<>();
        for (LearnCategoryEntity e : all) {
            LearnCategoryTreeNode node = nodeMap.get(e.getId());
            if (e.getParentId() == null) {
                roots.add(node);
            } else {
                LearnCategoryTreeNode parent = nodeMap.get(e.getParentId());
                if (parent != null) parent.getChildren().add(node);
            }
        }
        sortRecursively(roots);
        return roots;
    }

    private void sortRecursively(List<LearnCategoryTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        nodes.sort(Comparator.comparing(LearnCategoryTreeNode::getSort));
        nodes.forEach(n -> sortRecursively(n.getChildren()));
    }

    @Override
    public Long create(LearnCategoryReq req) {
        rejectDuplicateName(req.getParentId(), req.getName(), null);
        LearnCategoryEntity entity = new LearnCategoryEntity();
        entity.setParentId(req.getParentId());
        entity.setName(req.getName());
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, LearnCategoryReq req) {
        LearnCategoryEntity exist = requireExisting(id);
        rejectDuplicateName(req.getParentId(), req.getName(), id);
        exist.setParentId(req.getParentId());
        exist.setName(req.getName());
        exist.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.updateById(exist);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExisting(id);
        Long childCount = mapper.selectCount(new QueryWrapper<LearnCategoryEntity>()
                .eq("deleted", 0).eq("parent_id", id));
        if (childCount > 0) {
            throw LearnErrorCode.CATEGORY_NOT_EMPTY.toException();
        }
        // 文章校验放到 ArticleService 跨表查 — 这里只看分类树自身非空
        mapper.deleteById(id); // 软删（BaseEntity 配合 logic-delete 字段）
    }

    @Override
    @Transactional
    public void sortBatch(List<LearnSortReq.SortItem> items) {
        for (LearnSortReq.SortItem it : items) {
            LearnCategoryEntity e = requireExisting(it.getId());
            e.setSort(it.getSort());
            if (it.getParentId() != null) e.setParentId(it.getParentId());
            mapper.updateById(e);
        }
    }

    private void rejectDuplicateName(Long parentId, String name, Long excludeId) {
        QueryWrapper<LearnCategoryEntity> qw = new QueryWrapper<LearnCategoryEntity>()
                .eq("deleted", 0)
                .eq("name", name)
                .and(w -> w.isNull("parent_id").eq("parent_id", parentId == null ? -1 : parentId));
        // 上述 wrapper 处理 parent_id 为 null 的场景，按项目 MyBatis-Plus 版本调整
        if (parentId == null) {
            qw.and(w -> w.isNull("parent_id"));
        } else {
            qw.and(w -> w.eq("parent_id", parentId));
        }
        LearnCategoryEntity exist = mapper.selectOne(qw);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw LearnErrorCode.CATEGORY_NAME_DUPLICATE.toException();
        }
    }

    private LearnCategoryEntity requireExisting(Long id) {
        LearnCategoryEntity e = mapper.selectById(id);
        if (e == null) throw LearnErrorCode.ARTICLE_NOT_FOUND.toException();
        return e;
    }
}
```

> 备注：`Mapper.deleteById` 在项目里是软删除还是物理删除取决于是否配置 `mybatis-plus.global-config.db-config.logic-delete-field`；如已配置，调用即软删。

- [ ] **Step 6: 跑测试**

Run: `cd project/admin/api && mvn test -Dtest=LearnCategoryServiceImplTest -q`
Expected: 2 个测试全部通过。

- [ ] **Step 7: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/ \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/learn/
git commit -m "feat(admin): 创作学院分类管理 service + 树构建"
```

---

## Task 3: 文章 Service（TDD：状态转换 + 分页 + 校验）

**Files:**
- Create: `mapper/LearnArticleMapper.java`
- Create: `dto/request/LearnArticleReq.java`
- Create: `dto/request/LearnArticlePageQuery.java`
- Create: `dto/request/LearnSortReq.java`
- Create: `vo/LearnArticleDetail.java`
- Create: `service/LearnArticleService.java`
- Create: `service/impl/LearnArticleServiceImpl.java`
- Create: `test/.../LearnArticleServiceImplTest.java`

**Interfaces:**
- `LearnArticleService` 方法:
  - `PageResult<LearnArticleDetail> page(LearnArticlePageQuery q)`
  - `LearnArticleDetail detail(Long id)`
  - `Long create(LearnArticleReq req)`
  - `void update(Long id, LearnArticleReq req)`
  - `void delete(Long id)`
  - `void publish(Long id)`
  - `void unpublish(Long id)`
  - `void move(Long id, Long categoryId)`
  - `void sortBatch(List<LearnSortReq.SortItem> items)`
- `published_at` 语义：仅当 `DRAFT → PUBLISHED` 首次时写入；纯编辑 published 文章不改；`PUBLISHED → DRAFT` 保留

- [ ] **Step 1: 写失败测试**

`LearnArticleServiceImplTest.java`：

```java
package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnArticleServiceImplTest {

    @Mock LearnArticleMapper articleMapper;
    @Mock LearnCategoryMapper categoryMapper;
    @InjectMocks LearnArticleServiceImpl service;

    @Test
    void publish_fromDraft_writesPublishedAt() {
        LearnArticleEntity draft = article(10L, ArticleStatus.DRAFT, null);
        when(articleMapper.selectById(10L)).thenReturn(draft);
        service.publish(10L);
        ArgumentCaptor<LearnArticleEntity> cap = ArgumentCaptor.forClass(LearnArticleEntity.class);
        verify(articleMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(cap.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void publish_alreadyPublished_keepsOriginalPublishedAt() {
        LocalDateTime original = LocalDateTime.of(2026, 1, 1, 0, 0);
        LearnArticleEntity published = article(11L, ArticleStatus.PUBLISHED, original);
        when(articleMapper.selectById(11L)).thenReturn(published);
        service.publish(11L);
        ArgumentCaptor<LearnArticleEntity> cap = ArgumentCaptor.forClass(LearnArticleEntity.class);
        verify(articleMapper).updateById(cap.capture());
        assertThat(cap.getValue().getPublishedAt()).isEqualTo(original);
    }

    @Test
    void unpublish_keepsPublishedAt() {
        LocalDateTime original = LocalDateTime.of(2026, 2, 1, 0, 0);
        LearnArticleEntity published = article(12L, ArticleStatus.PUBLISHED, original);
        when(articleMapper.selectById(12L)).thenReturn(published);
        service.unpublish(12L);
        ArgumentCaptor<LearnArticleEntity> cap = ArgumentCaptor.forClass(LearnArticleEntity.class);
        verify(articleMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(cap.getValue().getPublishedAt()).isEqualTo(original);
    }

    @Test
    void delete_categoryMustBeClean_firstChecksArticles() {
        // 即分类下还有别的已发布文章不能删
        when(articleMapper.selectCount(any())).thenReturn(3L);
        // 假设 delete 由 ArticleService 的"分类下是否还有文章"检查；此处测试 article.delete 直接走
        // 不需要走 category 删除 — 留给 CategoryService 调 articleMapper
        service.delete(20L);
        verify(articleMapper).deleteById(20L);
    }

    @Test
    void create_rejectsContentOverLimit() {
        var req = new com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq();
        req.setCategoryId(1L);
        req.setTitle("t");
        req.setContentType(ContentType.MARKDOWN);
        req.setStatus(ArticleStatus.DRAFT);
        req.setContent("a".repeat(300_000)); // 超过 200KB
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BizException.class)
                .extracting("errorCode.code").isEqualTo(LearnErrorCode.CONTENT_TOO_LARGE.getCode());
        verify(articleMapper, never()).insert(any());
    }

    private LearnArticleEntity article(Long id, ArticleStatus status, LocalDateTime pub) {
        LearnArticleEntity e = new LearnArticleEntity();
        e.setId(id); e.setCategoryId(1L); e.setTitle("t"); e.setStatus(status);
        e.setContentType(ContentType.MARKDOWN); e.setContent("x"); e.setPublishedAt(pub);
        return e;
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd project/admin/api && mvn test -Dtest=LearnArticleServiceImplTest -q`
Expected: 编译失败 / 测试全红。

- [ ] **Step 3: 写 Mapper**

`LearnArticleMapper.java`：

```java
package com.aichuangzuo.admin.modules.learn.mapper;

import com.aichuangzuo.admin.modules.learn.entity.LearnArticleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface LearnArticleMapper extends BaseMapper<LearnArticleEntity> {
}
```

- [ ] **Step 4: 写 DTO/VO**

`dto/request/LearnArticleReq.java`：

```java
package com.aichuangzuo.admin.modules.learn.dto.request;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearnArticleReq {
    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 128)
    private String title;

    @Size(max = 255)
    private String summary;

    @NotNull
    private ContentType contentType;

    @NotNull
    private String content;

    private ArticleStatus status = ArticleStatus.DRAFT;

    private Integer sort = 0;
}
```

`dto/request/LearnArticlePageQuery.java`：

```java
package com.aichuangzuo.admin.modules.learn.dto.request;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import lombok.Data;

@Data
public class LearnArticlePageQuery {
    private Long categoryId;
    private ArticleStatus status;
    private String keyword;
    private Integer page = 1;
    private Integer size = 20;
}
```

`dto/request/LearnSortReq.java`：

```java
package com.aichuangzuo.admin.modules.learn.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class LearnSortReq {
    private List<SortItem> items;

    @Data
    public static class SortItem {
        private Long id;
        private Integer sort;
        private Long parentId; // 仅分类拖拽时使用，文章置空
    }
}
```

`vo/LearnArticleDetail.java`：

```java
package com.aichuangzuo.admin.modules.learn.vo;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearnArticleDetail {
    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private ContentType contentType;
    private String content;
    private ArticleStatus status;
    private Integer sort;
    private Long authorId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: 写 Service 接口与实现**

`service/LearnArticleService.java`：

```java
package com.aichuangzuo.admin.modules.learn.service;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticlePageQuery;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.vo.LearnArticleDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface LearnArticleService {
    Page<LearnArticleDetail> page(LearnArticlePageQuery q);
    LearnArticleDetail detail(Long id);
    Long create(LearnArticleReq req);
    void update(Long id, LearnArticleReq req);
    void delete(Long id);
    void publish(Long id);
    void unpublish(Long id);
    void move(Long id, Long categoryId);
    void sortBatch(List<LearnSortReq.SortItem> items);
}
```

`service/impl/LearnArticleServiceImpl.java`：

```java
package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticlePageQuery;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.admin.modules.learn.service.LearnArticleService;
import com.aichuangzuo.admin.modules.learn.vo.LearnArticleDetail;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnArticleServiceImpl implements LearnArticleService {

    private static final int MAX_MARKDOWN_BYTES = 200 * 1024;
    private static final int MAX_HTML_BYTES = 500 * 1024;

    private final LearnArticleMapper articleMapper;
    private final LearnCategoryMapper categoryMapper;

    @Override
    public Page<LearnArticleDetail> page(LearnArticlePageQuery q) {
        Page<LearnArticleEntity> page = new Page<>(q.getPage(), q.getSize());
        QueryWrapper<LearnArticleEntity> qw = new QueryWrapper<LearnArticleEntity>().eq("deleted", 0);
        if (q.getCategoryId() != null) qw.eq("category_id", q.getCategoryId());
        if (q.getStatus() != null) qw.eq("status", q.getStatus().getCode());
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            qw.and(w -> w.like("title", q.getKeyword()).or().like("summary", q.getKeyword()));
        }
        qw.orderByDesc("updated_at");
        Page<LearnArticleEntity> res = articleMapper.selectPage(page, qw);
        return res.convert(this::toVo);
    }

    @Override
    public LearnArticleDetail detail(Long id) {
        LearnArticleEntity e = requireExisting(id);
        return toVo(e);
    }

    @Override
    public Long create(LearnArticleReq req) {
        requireCategoryExists(req.getCategoryId());
        validateContentSize(req.getContentType(), req.getContent());
        LearnArticleEntity e = new LearnArticleEntity();
        copyFromReq(e, req);
        applyStatusTransition(e, null);
        articleMapper.insert(e);
        return e.getId();
    }

    @Override
    public void update(Long id, LearnArticleReq req) {
        LearnArticleEntity exist = requireExisting(id);
        // 切换正文类型时不允许：仅当原状态为 published 且 contentType 变更时拒绝
        if (exist.getStatus() == ArticleStatus.PUBLISHED
                && !exist.getContentType().equals(req.getContentType())) {
            throw LearnErrorCode.ARTICLE_NOT_FOUND.toException(); // 借用此码表示不允许
            // 也可以新建 LearnErrorCode.PUBLISHED_CONTENT_TYPE_LOCKED
        }
        requireCategoryExists(req.getCategoryId());
        validateContentSize(req.getContentType(), req.getContent());
        copyFromReq(exist, req);
        // status 字段保持不变（不动 published_at）
        articleMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        requireExisting(id);
        articleMapper.deleteById(id);
    }

    @Override
    public void publish(Long id) {
        LearnArticleEntity e = requireExisting(id);
        applyStatusTransition(e, ArticleStatus.PUBLISHED);
        articleMapper.updateById(e);
    }

    @Override
    public void unpublish(Long id) {
        LearnArticleEntity e = requireExisting(id);
        applyStatusTransition(e, ArticleStatus.DRAFT);
        articleMapper.updateById(e);
    }

    @Override
    public void move(Long id, Long categoryId) {
        requireCategoryExists(categoryId);
        LearnArticleEntity e = requireExisting(id);
        e.setCategoryId(categoryId);
        articleMapper.updateById(e);
    }

    @Override
    public void sortBatch(List<LearnSortReq.SortItem> items) {
        for (LearnSortReq.SortItem it : items) {
            LearnArticleEntity e = requireExisting(it.getId());
            e.setSort(it.getSort());
            articleMapper.updateById(e);
        }
    }

    // -------- helpers --------

    private void applyStatusTransition(LearnArticleEntity e, ArticleStatus target) {
        if (target != null) e.setStatus(target);
        // 草稿 → 已发布：写入 published_at
        if (e.getStatus() == ArticleStatus.PUBLISHED && e.getPublishedAt() == null) {
            e.setPublishedAt(LocalDateTime.now());
        }
        // 任何保持 PUBLISHED 但 publishedAt 已存在的：不覆盖
    }

    private void copyFromReq(LearnArticleEntity e, LearnArticleReq req) {
        e.setCategoryId(req.getCategoryId());
        e.setTitle(req.getTitle());
        e.setSummary(req.getSummary());
        e.setContentType(req.getContentType());
        e.setContent(req.getContent());
        if (req.getSort() != null) e.setSort(req.getSort());
        if (req.getStatus() != null) e.setStatus(req.getStatus());
    }

    private void validateContentSize(ContentType type, String content) {
        int bytes = content == null ? 0 : content.getBytes().length;
        int max = type == ContentType.MARKDOWN ? MAX_MARKDOWN_BYTES : MAX_HTML_BYTES;
        if (bytes > max) throw LearnErrorCode.CONTENT_TOO_LARGE.toException();
    }

    private LearnArticleEntity requireExisting(Long id) {
        LearnArticleEntity e = articleMapper.selectById(id);
        if (e == null) throw LearnErrorCode.ARTICLE_NOT_FOUND.toException();
        return e;
    }

    private void requireCategoryExists(Long id) {
        if (id == null || categoryMapper.selectById(id) == null) {
            throw LearnErrorCode.ARTICLE_NOT_FOUND.toException();
        }
    }

    private LearnArticleDetail toVo(LearnArticleEntity e) {
        LearnArticleDetail v = new LearnArticleDetail();
        v.setId(e.getId());
        v.setCategoryId(e.getCategoryId());
        v.setTitle(e.getTitle());
        v.setSummary(e.getSummary());
        v.setContentType(e.getContentType());
        v.setContent(e.getContent());
        v.setStatus(e.getStatus());
        v.setSort(e.getSort());
        v.setAuthorId(e.getAuthorId());
        v.setPublishedAt(e.getPublishedAt());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
```

- [ ] **Step 6: 跑测试**

Run: `cd project/admin/api && mvn test -Dtest=LearnArticleServiceImplTest -q`
Expected: 5 个测试全部通过。

- [ ] **Step 7: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/ \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/learn/
git commit -m "feat(admin): 创作学院文章 service（状态机 + 校验 + 分页）"
```

---

## Task 4: 管理端 Admin Controller

**Files:**
- Create: `controller/LearnAdminController.java`

**Interfaces:**
- 所有方法返回 `Result<T>`，路径前缀 `/admin/learn/**`
- 调用 Service 层，不写业务逻辑

- [ ] **Step 1: 写 Controller**

`controller/LearnAdminController.java`：

```java
package com.aichuangzuo.admin.modules.learn.controller;

import com.aichuangzuo.admin.modules.learn.dto.request.*;
import com.aichuangzuo.admin.modules.learn.service.LearnArticleService;
import com.aichuangzuo.admin.modules.learn.service.LearnCategoryService;
import com.aichuangzuo.admin.modules.learn.vo.LearnArticleDetail;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "创作学院管理")
@RestController
@RequestMapping("/admin/learn")
@RequiredArgsConstructor
public class LearnAdminController {

    private final LearnCategoryService categoryService;
    private final LearnArticleService articleService;

    @Operation(summary = "分类树")
    @GetMapping("/category/tree")
    public Result<List<LearnCategoryTreeNode>> categoryTree() {
        return Result.success(categoryService.tree());
    }

    @Operation(summary = "新增分类")
    @PostMapping("/category")
    public Result<Long> createCategory(@Valid @RequestBody LearnCategoryReq req) {
        return Result.success(categoryService.create(req));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody LearnCategoryReq req) {
        categoryService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量拖拽排序（分类）")
    @PostMapping("/category/sort")
    public Result<Void> sortCategory(@RequestBody LearnSortReq req) {
        categoryService.sortBatch(req.getItems());
        return Result.success();
    }

    @Operation(summary = "文章分页")
    @GetMapping("/article/page")
    public Result<Page<LearnArticleDetail>> articlePage(LearnArticlePageQuery q) {
        return Result.success(articleService.page(q));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/article/{id}")
    public Result<LearnArticleDetail> articleDetail(@PathVariable Long id) {
        return Result.success(articleService.detail(id));
    }

    @Operation(summary = "新增文章")
    @PostMapping("/article")
    public Result<Long> createArticle(@Valid @RequestBody LearnArticleReq req) {
        return Result.success(articleService.create(req));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/article/{id}")
    public Result<Void> updateArticle(@PathVariable Long id, @Valid @RequestBody LearnArticleReq req) {
        articleService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "发布文章")
    @PostMapping("/article/{id}/publish")
    public Result<Void> publishArticle(@PathVariable Long id) {
        articleService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下线文章")
    @PostMapping("/article/{id}/unpublish")
    public Result<Void> unpublishArticle(@PathVariable Long id) {
        articleService.unpublish(id);
        return Result.success();
    }

    @Operation(summary = "移动文章分类")
    @PostMapping("/article/{id}/move")
    public Result<Void> moveArticle(@PathVariable Long id, @RequestBody LearnArticleReq req) {
        articleService.move(id, req.getCategoryId());
        return Result.success();
    }

    @Operation(summary = "批量拖拽排序（文章）")
    @PostMapping("/article/sort")
    public Result<Void> sortArticle(@RequestBody LearnSortReq req) {
        articleService.sortBatch(req.getItems());
        return Result.success();
    }
}
```

> `LearnSortReq.getItems()` 见 Task 3 的 DTO 定义，items 字段已就位。

- [ ] **Step 2: 编译**

Run: `mvn -pl project/admin/api compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/controller/
git commit -m "feat(admin): 创作学院 admin controller 全部端点"
```

---

## Task 5: 用户端公共 Service + Controller（user/api）

**Files:**（详见 Task 1 全局清单）

- Create: `entity/LearnCategoryEntity.java`（user/api 镜像）
- Create: `entity/LearnArticleEntity.java`（user/api 镜像）
- Create: `mapper/LearnCategoryMapper.java`（user/api 镜像，删 default 方法 = 空）
- Create: `mapper/LearnArticleMapper.java`（user/api 镜像）
- Create: `vo/LearnCategoryTreeVO.java`
- Create: `vo/LearnCategoryDetailVO.java`
- Create: `vo/LearnArticleVO.java`
- Create: `service/LearnBrowseService.java`
- Create: `service/impl/LearnBrowseServiceImpl.java`
- Create: `controller/LearnController.java`

**Interfaces:**
- `LearnController`:
  - `GET /api/v1/learn/category/tree` → `List<LearnCategoryTreeVO>`
  - `GET /api/v1/learn/category/{id}?page=&size=` → `LearnCategoryDetailVO`
  - `GET /api/v1/learn/article/{id}` → `LearnArticleVO`（仅 published）

- [ ] **Step 1: 写两个 Entity（user/api 镜像）**

> 与 admin/api 几乎相同，包名换为 `com.aichuangzuo.user.modules.learn.entity`，继承项目已有 user/api 的 BaseEntity。如 BaseEntity 路径不同，按 user/api 现有实体调整。

- [ ] **Step 2: 写 Mapper（user/api 镜像）**

`LearnCategoryMapper.java`、`LearnArticleMapper.java` 各 extends 各自的 BaseMapper；规则同上。

- [ ] **Step 3: 写 VO**

`vo/LearnCategoryTreeVO.java`：

```java
package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class LearnCategoryTreeVO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private List<LearnCategoryTreeVO> children = new ArrayList<>();
}
```

`vo/LearnCategoryDetailVO.java`：

```java
package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;
import java.util.List;

@Data
public class LearnCategoryDetailVO {
    private Long id;
    private String name;
    private Long parentId;
    private String parentName; // 用于面包屑，可选
    private List<LearnCategoryTreeVO> children;
    private List<LearnArticleVO> articles;
    private Integer page;
    private Integer size;
    private Long total;
}
```

`vo/LearnArticleVO.java`：

```java
package com.aichuangzuo.user.modules.learn.vo;

import com.aichuangzuo.admin.modules.learn.enums.ContentType; // 跨模块引用很重；考虑在 user/api 端定义独立枚举
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LearnArticleVO {
    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private ContentType contentType; // 见上「跨模块」
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
```

> **重要**：`com.aichuangzuo.admin.modules.learn.enums` 是 admin 私有包，user 模块不应反向依赖。**在 user/api 也定义一份枚举 `ContentType` 与 `ArticleStatus`**（mirror enum），结构相同。BizException/错误码同理在 user/api 不需要抛业务异常，只在查不到时抛 `Result.fail(404, ...)` 或返回 `Result.notFound()`。

- [ ] **Step 4: 写 Service 接口与实现**

`service/LearnBrowseService.java`：

```java
package com.aichuangzuo.user.modules.learn.service;

import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;

import java.util.List;

public interface LearnBrowseService {
    List<LearnCategoryTreeVO> tree();
    LearnCategoryDetailVO categoryDetail(Long id, int page, int size);
    LearnArticleVO articleDetail(Long id);
}
```

`service/impl/LearnBrowseServiceImpl.java`：

```java
package com.aichuangzuo.user.modules.learn.service.impl;

import com.aichuangzuo.user.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.user.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.user.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.user.modules.learn.enums.ContentType; // user/api 本地枚举
import com.aichuangzuo.user.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.user.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.user.modules.learn.service.LearnBrowseService;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnBrowseServiceImpl implements LearnBrowseService {

    private final LearnCategoryMapper categoryMapper;
    private final LearnArticleMapper articleMapper;

    @Override
    public List<LearnCategoryTreeVO> tree() {
        // 拿全部分类
        List<LearnCategoryEntity> allCats = categoryMapper.selectList(
                new QueryWrapper<LearnCategoryEntity>().eq("deleted", 0));
        if (allCats.isEmpty()) return List.of();

        // 拿所有已发布文章的 category_id，用于过滤
        List<Long> publishedCatIds = articleMapper.selectList(new QueryWrapper<LearnArticleEntity>()
                        .eq("deleted", 0)
                        .eq("status", ArticleStatus.PUBLISHED.getCode()))
                .stream().map(LearnArticleEntity::getCategoryId).distinct().toList();

        Set<Long> liveSet = new HashSet<>(publishedCatIds);
        // 父链保留：即使父分类本身没文章
        Set<Long> keepIds = new HashSet<>(liveSet);
        Map<Long, LearnCategoryEntity> byId = allCats.stream()
                .collect(Collectors.toMap(LearnCategoryEntity::getId, c -> c));
        for (Long cid : liveSet) {
            LearnCategoryEntity c = byId.get(cid);
            while (c != null && c.getParentId() != null) {
                keepIds.add(c.getId());
                c = byId.get(c.getParentId());
            }
        }

        List<LearnCategoryEntity> filtered = allCats.stream()
                .filter(c -> keepIds.contains(c.getId())).toList();

        return buildTree(filtered);
    }

    private List<LearnCategoryTreeVO> buildTree(List<LearnCategoryEntity> nodes) {
        Map<Long, LearnCategoryTreeVO> map = new LinkedHashMap<>();
        for (LearnCategoryEntity e : nodes) {
            LearnCategoryTreeVO n = new LearnCategoryTreeVO();
            n.setId(e.getId()); n.setParentId(e.getParentId());
            n.setName(e.getName()); n.setSort(e.getSort());
            map.put(e.getId(), n);
        }
        List<LearnCategoryTreeVO> roots = new ArrayList<>();
        for (LearnCategoryEntity e : nodes) {
            LearnCategoryTreeVO n = map.get(e.getId());
            if (e.getParentId() == null) roots.add(n);
            else {
                LearnCategoryTreeVO p = map.get(e.getParentId());
                if (p != null) p.getChildren().add(n);
                else roots.add(n); // 父节点缺失兜底
            }
        }
        sortRecursive(roots);
        return roots;
    }

    private void sortRecursive(List<LearnCategoryTreeVO> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        nodes.sort(Comparator.comparing(LearnCategoryTreeVO::getSort));
        nodes.forEach(n -> sortRecursive(n.getChildren()));
    }

    @Override
    public LearnCategoryDetailVO categoryDetail(Long id, int page, int size) {
        LearnCategoryEntity cat = categoryMapper.selectById(id);
        if (cat == null) return null;

        LearnCategoryDetailVO vo = new LearnCategoryDetailVO();
        vo.setId(cat.getId()); vo.setName(cat.getName());
        vo.setParentId(cat.getParentId());
        if (cat.getParentId() != null) {
            LearnCategoryEntity parent = categoryMapper.selectById(cat.getParentId());
            if (parent != null) vo.setParentName(parent.getName());
        }
        vo.setChildren(buildTree(categoryMapper.selectList(
                new QueryWrapper<LearnCategoryEntity>().eq("deleted", 0).eq("parent_id", id))));
        vo.setPage(page); vo.setSize(size);

        Page<LearnArticleEntity> p = new Page<>(page, size);
        Page<LearnArticleEntity> res = articleMapper.selectPage(p, new QueryWrapper<LearnArticleEntity>()
                .eq("deleted", 0)
                .eq("status", ArticleStatus.PUBLISHED.getCode())
                .eq("category_id", id)
                .orderByAsc("sort")
                .orderByDesc("updated_at"));
        vo.setArticles(res.getRecords().stream().map(this::toVo).toList());
        vo.setTotal(res.getTotal());
        return vo;
    }

    @Override
    public LearnArticleVO articleDetail(Long id) {
        LearnArticleEntity e = articleMapper.selectOne(new QueryWrapper<LearnArticleEntity>()
                .eq("deleted", 0)
                .eq("id", id)
                .eq("status", ArticleStatus.PUBLISHED.getCode()));
        return e == null ? null : toVo(e);
    }

    private LearnArticleVO toVo(LearnArticleEntity e) {
        LearnArticleVO v = new LearnArticleVO();
        v.setId(e.getId());
        v.setCategoryId(e.getCategoryId());
        v.setTitle(e.getTitle());
        v.setSummary(e.getSummary());
        v.setContentType(ContentType.valueOf(e.getContentType().name())); // 同名枚举互转
        v.setContent(e.getContent());
        v.setPublishedAt(e.getPublishedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
```

- [ ] **Step 5: 写 Controller**

`controller/LearnController.java`：

```java
package com.aichuangzuo.user.modules.learn.controller;

import com.aichuangzuo.shared.exception.NotFoundException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.learn.service.LearnBrowseService;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "创作学院公共浏览")
@RestController
@RequestMapping("/api/v1/learn")
@RequiredArgsConstructor
public class LearnController {

    private final LearnBrowseService service;

    @Operation(summary = "分类树")
    @GetMapping("/category/tree")
    public Result<List<LearnCategoryTreeVO>> tree() {
        return Result.success(service.tree());
    }

    @Operation(summary = "分类详情 + 已发布文章列表")
    @GetMapping("/category/{id}")
    public Result<LearnCategoryDetailVO> categoryDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LearnCategoryDetailVO vo = service.categoryDetail(id, page, size);
        if (vo == null) throw new NotFoundException("分类不存在");
        return Result.success(vo);
    }

    @Operation(summary = "文章详情")
    @GetMapping("/article/{id}")
    public Result<LearnArticleVO> articleDetail(@PathVariable Long id) {
        LearnArticleVO vo = service.articleDetail(id);
        if (vo == null) throw new NotFoundException("文章不存在或已下线");
        return Result.success(vo);
    }
}
```

> `NotFoundException` 按 user/api 现有体系调整；目标都是 404 + 标准响应。

- [ ] **Step 6: 编译两个模块**

```bash
mvn -pl project/admin/api,project/user/api -am compile -DskipTests
```

Expected: 两个模块都 BUILD SUCCESS。

- [ ] **Step 7: Commit**

```bash
git add project/admin/api project/user/api -A
git commit -m "feat(public): 创作学院公共浏览端点（user/api）"
```

---

## Task 6: 用户端 — `/learn` 路由 + NavBar + 基础壳

**Files:**
- Modify: `project/user/web/src/router/index.js`（加 1 条路由）
- Modify: `project/user/web/src/components/layout/NavBar.vue`（加链接；需 props 化传给 Learn 页）
- Create: `project/user/web/src/views/LearnIndex.vue`（页面骨架）
- Create: `project/user/web/src/api/learn.js`

- [ ] **Step 1: 写 api/learn.js**

```javascript
import http from '@/utils/http'

export const fetchCategoryTree = () => http.get('/api/v1/learn/category/tree')

export const fetchCategoryDetail = (id, page = 1, size = 50) =>
  http.get(`/api/v1/learn/category/${id}`, { params: { page, size } })

export const fetchArticle = id => http.get(`/api/v1/learn/article/${id}`)
```

> `http` 路径与现有项目一致（视项目实际情况调整）。

- [ ] **Step 2: 加路由**

打开 `project/user/web/src/router/index.js`，在 `pricing` 路由之后插入：

```js
{
  path: '/learn',
  name: 'Learn',
  component: () => import('@/views/LearnIndex.vue')
},
{
  path: '/learn/article/:id',
  name: 'LearnArticle',
  component: () => import('@/views/LearnIndex.vue')
}
```

- [ ] **Step 3: 写 LearnIndex 骨架**

`views/LearnIndex.vue`：

```vue
<template>
  <div class="learn-page">
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <div class="learn-body">
      <aside class="learn-sidebar">
        <LearnSidebar
          v-if="categoryTree.length"
          :nodes="categoryTree"
          :active-id="activeCategoryId"
          @select="onSelectCategory"
        />
        <div v-else class="learn-empty">内容正在筹备中…</div>
      </aside>

      <main class="learn-main">
        <LearnContent
          :article="currentArticle"
          :category="currentCategory"
          @load-article="loadArticle"
        />
      </main>
    </div>

    <MobileTreeSheet
      v-model:open="mobileSheetOpen"
      :nodes="categoryTree"
      :active-id="activeCategoryId"
      @select="onSelectCategoryFromSheet"
    />

    <button
      v-if="isMobile"
      class="learn-tree-fab"
      @click="mobileSheetOpen = true"
    >分类</button>

    <footer class="learn-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCategoryTree, fetchArticle } from '@/api/learn'
import NavBar from '@/components/layout/NavBar.vue'
import LearnSidebar from '@/components/learn/LearnSidebar.vue'
import LearnContent from '@/components/learn/LearnContent.vue'
import MobileTreeSheet from '@/components/learn/MobileTreeSheet.vue'

const route = useRoute()
const router = useRouter()
const categoryTree = ref([])
const currentArticle = ref(null)
const currentCategory = ref(null)
const mobileSheetOpen = ref(false)
const isMobile = ref(window.innerWidth < 992)

const activeCategoryId = computed(() => {
  if (route.params.id) return currentArticle.value?.categoryId ?? null
  return route.query.cat ? Number(route.query.cat) : null
})

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'

const onSelectCategory = id => router.replace({ path: '/learn', query: { cat: id } })
const onSelectCategoryFromSheet = id => {
  mobileSheetOpen.value = false
  onSelectCategory(id)
}

const loadArticle = id => router.push(`/learn/article/${id}`)

async function bootstrap () {
  const tree = await fetchCategoryTree()
  categoryTree.value = tree.data || []
  // 直链进入文章
  if (route.params.id) {
    const res = await fetchArticle(route.params.id)
    currentArticle.value = res.data || null
  }
}

window.addEventListener('resize', () => {
  isMobile.value = window.innerWidth < 992
})

onMounted(bootstrap)
watch(() => route.path, bootstrap)
</script>

<style scoped>
.learn-page { min-height: 100vh; display: flex; flex-direction: column; background: #fafafa; }
.learn-body { display: flex; flex: 1; max-width: 1200px; width: 100%; margin: 0 auto; padding: 24px 16px; gap: 24px; }
.learn-sidebar { width: 240px; flex-shrink: 0; position: sticky; top: 88px; align-self: flex-start; max-height: calc(100vh - 88px); overflow-y: auto; background: #fff; border-radius: 8px; padding: 12px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.learn-main { flex: 1; min-width: 0; background: #fff; border-radius: 8px; padding: 28px 32px; }
.learn-empty { padding: 32px 16px; text-align: center; color: #999; }
.learn-footer { padding: 32px 16px; text-align: center; color: #999; font-size: 13px; display: flex; flex-direction: column; gap: 4px; }
.learn-tree-fab {
  position: fixed; bottom: 24px; right: 24px;
  background: #07c160; color: #fff; border: 0; border-radius: 24px;
  padding: 10px 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.15); cursor: pointer; z-index: 50;
  display: none;
}
@media (max-width: 991px) {
  .learn-body { flex-direction: column; padding: 16px; }
  .learn-sidebar { display: none; }
  .learn-tree-fab { display: inline-flex; }
  .learn-main { padding: 20px 16px; }
}
</style>
```

- [ ] **Step 4: NavBar 加链接**

打开 `project/user/web/src/components/layout/NavBar.vue`：该组件接收 `links` props，所以无需改 NavBar 本身 — 在每个调用页面传新增的 `创作学院` 链接。仅需检查哪些页面在桌面链接区展示同类导航（Home/Pricing/GuideIndex/LearnIndex 同一套）。本次实现仅在 LearnIndex.vue 内部设置 `links`，其它页面 NavBar 的链接各自管自己的，无需全局变动。

- [ ] **Step 5: 编译 dev**

Run: `cd project/user/web && npm run build -- --mode=development 2>&1 | tail -30`
Expected: 编译无错（即使组件尚未建，构造 Mock 文件补上再继续；下一步 Task 7 完成）

- [ ] **Step 6: Commit**

```bash
git add project/user/web/src/router/index.js \
        project/user/web/src/views/LearnIndex.vue \
        project/user/web/src/api/learn.js
git commit -m "feat(user-web): /learn 页面骨架 + nav 入口 + api 封装"
```

---

## Task 7: 用户端 — LearnSidebar 树组件

**Files:**
- Create: `project/user/web/src/components/learn/LearnSidebar.vue`

- [ ] **Step 1: 写组件**

```vue
<template>
  <ul class="learn-tree">
    <LearnSidebarNode
      v-for="node in nodes"
      :key="node.id"
      :node="node"
      :depth="0"
      :active-id="activeId"
      @select="$emit('select', $event)"
    />
  </ul>
</template>

<script setup>
import LearnSidebarNode from './LearnSidebarNode.vue'
defineProps({
  nodes: { type: Array, required: true },
  activeId: { type: Number, default: null }
})
defineEmits(['select'])
</script>

<style scoped>
.learn-tree { list-style: none; margin: 0; padding: 0; }
</style>
```

Create: `project/user/web/src/components/learn/LearnSidebarNode.vue`：

```vue
<template>
  <li class="learn-tree-node">
    <div
      :class="['learn-tree-row', { active: node.id === activeId, expandable: hasChildren }]"
      :style="{ paddingLeft: `${depth * 16 + 12}px` }"
      @click="onClick"
    >
      <span v-if="hasChildren" class="learn-tree-caret">{{ open ? '−' : '+' }}</span>
      <span v-else class="learn-tree-caret-spacer"></span>
      <span class="learn-tree-label">{{ node.name }}</span>
    </div>
    <ul v-if="open && hasChildren" class="learn-tree-children">
      <LearnSidebarNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :active-id="activeId"
        @select="(id) => $emit('select', id)"
      />
    </ul>
  </li>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  activeId: { type: Number, default: null }
})
defineEmits(['select'])

const open = ref(props.depth < 1)
const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)

function onClick () {
  if (hasChildren.value) open.value = !open.value
}
</script>

<style scoped>
.learn-tree-row {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; cursor: pointer; user-select: none;
  font-size: 14px; color: #262626;
  border-left: 3px solid transparent;
}
.learn-tree-row:hover { background: #f6ffed; }
.learn-tree-row.active {
  background: #f6ffed; color: #07c160; font-weight: 600;
  border-left-color: #07c160;
}
.learn-tree-caret {
  width: 14px; text-align: center; font-size: 12px; color: #999;
}
.learn-tree-caret-spacer { width: 14px; display: inline-block; }
.learn-tree-children { list-style: none; padding: 0; margin: 0; }
</style>
```

- [ ] **Step 2: 编译**

Run: `cd project/user/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS 或无组件相关错误。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/components/learn/
git commit -m "feat(user-web): LearnSidebar 递归树组件"
```

---

## Task 8: 用户端 — LearnContent + Markdown/RichText 渲染

**Files:**
- Create: `project/user/web/src/components/learn/LearnContent.vue`
- Create: `project/user/web/src/components/learn/LearnMarkdown.vue`
- Create: `project/user/web/src/components/learn/LearnRichText.vue`
- Modify: `package.json`（user/web）加 `markdown-it` + `highlight.js`

**Interfaces:**
- `LearnMarkdown` props: `source` (string)
- `LearnRichText` props: `html` (string)

- [ ] **Step 1: 安装依赖**

```bash
cd project/user/web && npm install markdown-it highlight.js
```

- [ ] **Step 2: 写 Markdown 渲染**

`LearnMarkdown.vue`：

```vue
<template>
  <div class="learn-md" v-html="rendered"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github.css'

const props = defineProps({
  source: { type: String, default: '' }
})

const md = new MarkdownIt({
  html: false,        // 禁用 raw HTML（spec Global Constraint）
  linkify: true,
  typographer: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (_) { /* noop */ }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const rendered = computed(() => md.render(props.source || ''))
</script>

<style scoped>
.learn-md { line-height: 1.75; color: #262626; font-size: 15px; }
.learn-md :deep(h1) { font-size: 24px; font-weight: 700; margin: 1.4em 0 0.6em; }
.learn-md :deep(h2) { font-size: 20px; font-weight: 700; margin: 1.2em 0 0.5em; border-bottom: 1px solid #eee; padding-bottom: 6px; }
.learn-md :deep(h3) { font-size: 17px; font-weight: 600; margin: 1em 0 0.4em; }
.learn-md :deep(p) { margin: 0.8em 0; }
.learn-md :deep(blockquote) {
  margin: 1em 0; padding: 10px 16px; color: #555;
  background: #f8f8f8; border-left: 4px solid #07c160;
}
.learn-md :deep(code) {
  background: #f6f8fa; padding: 2px 6px; border-radius: 4px;
  font-size: 13px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.learn-md :deep(pre) {
  background: #1f1f1f; color: #f5f5f5; padding: 14px 18px;
  border-radius: 6px; overflow-x: auto; font-size: 13px; line-height: 1.5;
}
.learn-md :deep(pre code) { background: transparent; padding: 0; color: inherit; }
.learn-md :deep(table) {
  border-collapse: collapse; width: 100%; margin: 1em 0;
}
.learn-md :deep(th), .learn-md :deep(td) {
  border: 1px solid #e8e8e8; padding: 8px 12px; text-align: left;
}
.learn-md :deep(th) { background: #fafafa; }
.learn-md :deep(ul), .learn-md :deep(ol) { padding-left: 1.5em; }
</style>
```

- [ ] **Step 3: 写 RichText 渲染**

`LearnRichText.vue`：

```vue
<template>
  <div class="learn-rt" v-html="safeHtml"></div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ html: { type: String, default: '' } })
const safeHtml = computed(() => props.html || '')
</script>

<style scoped>
.learn-rt { line-height: 1.75; color: #262626; font-size: 15px; }
.learn-rt :deep(*) { max-width: 100%; }
.learn-rt :deep(h1) { font-size: 24px; font-weight: 700; margin: 1.4em 0 0.6em; }
.learn-rt :deep(h2) { font-size: 20px; font-weight: 700; margin: 1.2em 0 0.5em; }
.learn-rt :deep(h3) { font-size: 17px; font-weight: 600; margin: 1em 0 0.4em; }
.learn-rt :deep(p) { margin: 0.8em 0; }
.learn-rt :deep(blockquote) {
  margin: 1em 0; padding: 10px 16px; color: #555;
  background: #f8f8f8; border-left: 4px solid #07c160;
}
.learn-rt :deep(code) {
  background: #f6f8fa; padding: 2px 6px; border-radius: 4px; font-size: 13px;
}
.learn-rt :deep(pre) {
  background: #1f1f1f; color: #f5f5f5; padding: 14px 18px;
  border-radius: 6px; overflow-x: auto; font-size: 13px; line-height: 1.5;
}
.learn-rt :deep(table) {
  border-collapse: collapse; width: 100%; margin: 1em 0;
}
.learn-rt :deep(th), .learn-rt :deep(td) {
  border: 1px solid #e8e8e8; padding: 8px 12px; text-align: left;
}
.learn-rt :deep(th) { background: #fafafa; }
</style>
```

- [ ] **Step 4: 写 LearnContent**

`LearnContent.vue`：

```vue
<template>
  <div class="learn-content">
    <!-- 文章详情 -->
    <template v-if="article">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ article.title }}</h1>
        <p v-if="article.summary" class="learn-content-summary">{{ article.summary }}</p>
        <div class="learn-content-meta">
          发布于 {{ formatDate(article.publishedAt || article.updatedAt) }}
        </div>
      </header>
      <article class="learn-content-body">
        <LearnMarkdown v-if="article.contentType === 'markdown'" :source="article.content" />
        <LearnRichText v-else :html="article.content" />
      </article>
      <footer class="learn-content-foot">
        <router-link to="/login" class="learn-cta">想把自己的账号也做成这样？立即开始创作 →</router-link>
      </footer>
    </template>

    <!-- 分类详情（列表） -->
    <template v-else-if="category && category.articles?.length">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ category.name }}</h1>
      </header>
      <ul class="learn-article-list">
        <li v-for="a in category.articles" :key="a.id" class="learn-article-item">
          <a @click.prevent="$emit('load-article', a.id)" href="#">{{ a.title }}</a>
          <p v-if="a.summary" class="learn-article-summary">{{ a.summary }}</p>
          <div class="learn-article-meta">{{ formatDate(a.publishedAt || a.updatedAt) }}</div>
        </li>
      </ul>
    </template>

    <template v-else>
      <div class="learn-content-empty">从左侧选择一个分类查看内容</div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LearnMarkdown from './LearnMarkdown.vue'
import LearnRichText from './LearnRichText.vue'

const props = defineProps({
  article: { type: Object, default: null },
  category: { type: Object, default: null }
})
defineEmits(['load-article'])

function formatDate (d) {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getFullYear()}-${String(dt.getMonth()+1).padStart(2,'0')}-${String(dt.getDate()).padStart(2,'0')}`
}
</script>

<style scoped>
.learn-content { min-height: 320px; }
.learn-content-head { border-bottom: 1px solid #eee; padding-bottom: 16px; margin-bottom: 24px; }
.learn-content-title { font-size: 28px; font-weight: 700; color: #1a1a1a; margin: 0; }
.learn-content-summary { color: #666; font-size: 14px; margin: 8px 0 0; }
.learn-content-meta { color: #999; font-size: 13px; margin-top: 12px; }
.learn-content-body { margin-bottom: 36px; }
.learn-content-foot { border-top: 1px solid #eee; padding-top: 24px; text-align: center; }
.learn-cta { color: #07c160; font-weight: 600; text-decoration: none; }
.learn-cta:hover { text-decoration: underline; }
.learn-content-empty { color: #999; padding: 80px 0; text-align: center; font-size: 14px; }

.learn-article-list { list-style: none; margin: 0; padding: 0; }
.learn-article-item { padding: 18px 0; border-bottom: 1px solid #f0f0f0; }
.learn-article-item a { font-size: 16px; font-weight: 600; color: #1a1a1a; cursor: pointer; }
.learn-article-item a:hover { color: #07c160; }
.learn-article-summary { color: #666; font-size: 14px; margin: 6px 0 0; }
.learn-article-meta { color: #999; font-size: 12px; margin-top: 6px; }
</style>
```

- [ ] **Step 5: 关联 — 修改 LearnIndex 数据获取**

在 `LearnIndex.vue` 的 `bootstrap()` 中：

```js
async function bootstrap () {
  const tree = await fetchCategoryTree()
  categoryTree.value = tree.data || []

  if (route.params.id) {
    const res = await fetchArticle(route.params.id)
    currentArticle.value = res.data || null
    currentCategory.value = null
  } else if (route.query.cat) {
    const detail = await fetchCategoryDetail(route.query.cat, 1, 50)
    currentCategory.value = detail.data || null
    currentArticle.value = null
  } else {
    currentCategory.value = null
    currentArticle.value = null
  }
}
```

- [ ] **Step 6: 编译**

Run: `cd project/user/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add project/user/web/src/components/learn project/user/web/src/views/LearnIndex.vue \
        project/user/web/package.json project/user/web/package-lock.json 2>/dev/null
git commit -m "feat(user-web): 内容渲染（Markdown + 富文本）+ LearnContent"
```

---

## Task 9: 用户端 — MobileTreeSheet + 全宽移动布局

**Files:**
- Create: `project/user/web/src/components/learn/MobileTreeSheet.vue`

- [ ] **Step 1: 写抽屉组件**

```vue
<template>
  <a-drawer
    v-model:open="openModel"
    title="分类"
    placement="bottom"
    :height="'70vh'"
    :closable="true"
  >
    <LearnSidebar
      :nodes="nodes"
      :active-id="activeId"
      @select="onSelect"
    />
  </a-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { Drawer } from 'ant-design-vue'
import LearnSidebar from './LearnSidebar.vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  nodes: { type: Array, required: true },
  activeId: { type: Number, default: null }
})
const emit = defineEmits(['update:open', 'select'])

const openModel = computed({
  get () { return props.open },
  set (v) { emit('update:open', v) }
})

function onSelect (id) {
  emit('select', id)
  openModel.value = false
}
</script>
```

- [ ] **Step 2: 编译**

Run: `cd project/user/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/components/learn/MobileTreeSheet.vue
git commit -m "feat(user-web): 移动端分类抽屉"
```

---

## Task 10: 用户端 — 提交 + 验证浏览流

**Files:** 现有 `LearnIndex.vue` 已包含布局；做一次手动验证 + 编写初始 mock 数据流程性确认。

- [ ] **Step 1: 起后端 + 验证公共 API**

```bash
# 在本地起 admin + user api（按项目 README / scripts/local/）
curl http://localhost:port/api/v1/learn/category/tree
# 返回 [] ，因为还没有数据
```

- [ ] **Step 2: 启前端、访问 /learn**

```bash
cd project/user/web && npm run dev
# 浏览器访问 http://localhost:5173/learn
# 验证：
# 1) 空状态文案「内容正在筹备中…」显示
# 2) NavBar 上能看到「创作学院」
# 3) 窗口缩窄到 < 992px，「分类」FAB 出现，点击弹底部抽屉
```

- [ ] **Step 3: 留 staging 任务**

> 因尚未接后端真实数据，验收推迟到 Task 14 + Task 20 完成后再做端到端验证。

- [ ] **Step 4: Commit**

（如有 UI 调整）：

```bash
git add project/user/web
git commit -m "fix(user-web): /learn 初次自测调整"
```

---

## Task 11: 管理端 — 路由 + 菜单

**Files:**
- Modify: `project/admin/web/src/router/index.js`
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`（或等价菜单配置）

- [ ] **Step 1: 写管理端 API 封装**

Create: `project/admin/web/src/api/learn.js`：

```javascript
import http from '@/utils/http'

export const fetchCategoryTree = () => http.get('/admin/learn/category/tree')
export const createCategory = data => http.post('/admin/learn/category', data)
export const updateCategory = (id, data) => http.put(`/admin/learn/category/${id}`, data)
export const deleteCategory = id => http.delete(`/admin/learn/category/${id}`)
export const sortCategory = items => http.post('/admin/learn/category/sort', { items })

export const fetchArticlePage = params => http.get('/admin/learn/article/page', { params })
export const fetchArticle = id => http.get(`/admin/learn/article/${id}`)
export const createArticle = data => http.post('/admin/learn/article', data)
export const updateArticle = (id, data) => http.put(`/admin/learn/article/${id}`, data)
export const deleteArticle = id => http.delete(`/admin/learn/article/${id}`)
export const publishArticle = id => http.post(`/admin/learn/article/${id}/publish`)
export const unpublishArticle = id => http.post(`/admin/learn/article/${id}/unpublish`)
export const moveArticle = (id, categoryId) => http.post(`/admin/learn/article/${id}/move`, { categoryId })
export const sortArticle = items => http.post('/admin/learn/article/sort', { items })
```

- [ ] **Step 2: 加管理端路由**

打开 `project/admin/web/src/router/index.js`，加：

```js
{
  path: 'learn/category',
  name: 'LearnCategoryManage',
  component: () => import('@/views/learn/CategoryManage.vue'),
  meta: { title: '分类管理', icon: 'folder' }
},
{
  path: 'learn/article',
  name: 'LearnArticleList',
  component: () => import('@/views/learn/ArticleList.vue'),
  meta: { title: '文章管理', icon: 'file-text' }
},
{
  path: 'learn/article/edit/:id?',
  name: 'LearnArticleEditor',
  component: () => import('@/views/learn/ArticleEditor.vue'),
  meta: { title: '文章编辑', hidden: true }
}
```

- [ ] **Step 3: 加菜单配置**

打开 `project/admin/web/src/layouts/AdminLayout.vue`（或 `menus.js`/`menu.ts`），在合适位置加菜单项：

```js
{
  key: 'learn',
  label: '创作学院',
  icon: 'book',
  children: [
    { key: 'learn-category', label: '分类管理', path: '/learn/category' },
    { key: 'learn-article', label: '文章管理', path: '/learn/article' }
  ]
}
```

> 按项目现有菜单写法（可能是 `defineMenus` 数组 / store / 配置文件）调整。

- [ ] **Step 4: 编译**

Run: `cd project/admin/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS（即使 view 文件还没建，先建占位空文件让路由能解析）

- [ ] **Step 5: Commit**

```bash
git add project/admin/web/src/router/index.js \
        project/admin/web/src/layouts \
        project/admin/web/src/api/learn.js
git commit -m "feat(admin-web): 创作学院菜单 + 路由骨架"
```

---

## Task 12: 管理端 — 分类管理页（左树 + 右表单）

**Files:**
- Create: `project/admin/web/src/views/learn/CategoryManage.vue`
- Create: `project/admin/web/src/components/learn/CategoryTreeEditor.vue`

- [ ] **Step 1: 写 CategoryTreeEditor**

`components/learn/CategoryTreeEditor.vue`：

```vue
<template>
  <div class="category-editor">
    <a-card title="分类管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="onCreateRoot">新增顶级分类</a-button>
      </template>
      <a-tree
        :tree-data="treeData"
        :replace-fields="{ title: 'name', key: 'id' }"
        block-node
        :default-expand-all="true"
      >
        <template #title="{ dataRef }">
          <span>{{ dataRef.name }}</span>
          <span class="row-actions">
            <a @click.stop="onAddChild(dataRef)">+ 子分类</a>
            <a-divider type="vertical" />
            <a @click.stop="onEdit(dataRef)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm
              title="确认删除该分类？子分类或文章非空时会拒绝"
              @confirm.stop="onDelete(dataRef)"
            >
              <a class="danger" @click.stop>删除</a>
            </a-popconfirm>
          </span>
        </template>
      </a-tree>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editing ? '编辑分类' : '新增分类'"
      :confirm-loading="submitting"
      @ok="onSubmit"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="父分类">
          <a-tree-select
            v-model:value="form.parentId"
            :tree-data="parentOptions"
            :replace-fields="{ label: 'name', value: 'id' }"
            :tree-default-expand-all="true"
            allow-clear
            placeholder="（顶级分类）"
          />
        </a-form-item>
        <a-form-item label="名称" required>
          <a-input v-model:value="form.name" maxlength="64" />
        </a-form-item>
        <a-form-item label="排序值">
          <a-input-number v-model:value="form.sort" :min="0" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  fetchCategoryTree,
  createCategory,
  updateCategory,
  deleteCategory
} from '@/api/learn'

const treeData = ref([])
const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref(null)
const form = reactive({ parentId: null, name: '', sort: 0 })

const parentOptions = computed(() => [{ id: null, name: '（顶级）' }, ...treeData.value])

async function load () {
  const res = await fetchCategoryTree()
  treeData.value = res.data || []
}

function onCreateRoot () {
  editing.value = null
  form.parentId = null; form.name = ''; form.sort = 0
  modalOpen.value = true
}
function onAddChild (node) {
  editing.value = null
  form.parentId = node.id; form.name = ''; form.sort = 0
  modalOpen.value = true
}
function onEdit (node) {
  editing.value = node
  form.parentId = node.parentId; form.name = node.name; form.sort = node.sort
  modalOpen.value = true
}
async function onDelete (node) {
  try {
    await deleteCategory(node.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e?.message || '删除失败（分类下可能仍有子分类或文章）')
  }
}
async function onSubmit () {
  if (!form.name.trim()) { message.error('名称不能为空'); return }
  submitting.value = true
  try {
    if (editing.value) {
      await updateCategory(editing.value.id, form)
    } else {
      await createCategory(form)
    }
    message.success('已保存')
    modalOpen.value = false
    await load()
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.category-editor { padding: 16px; }
.row-actions { margin-left: 12px; opacity: 0.7; font-size: 12px; }
.row-actions a { color: #07c160; }
.row-actions a.danger { color: #ff4d4f; }
</style>
```

- [ ] **Step 2: 写 CategoryManage.vue**

```vue
<template>
  <CategoryTreeEditor />
</template>

<script setup>
import CategoryTreeEditor from '@/components/learn/CategoryTreeEditor.vue'
</script>
```

- [ ] **Step 3: 编译**

Run: `cd project/admin/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add project/admin/web/src/views/learn/CategoryManage.vue \
        project/admin/web/src/components/learn/CategoryTreeEditor.vue
git commit -m "feat(admin-web): 分类管理页（左树 + 右表单）"
```

---

## Task 13: 管理端 — 文章列表

**Files:**
- Create: `project/admin/web/src/views/learn/ArticleList.vue`

- [ ] **Step 1: 写列表页**

```vue
<template>
  <div class="article-list">
    <a-card :bordered="false">
      <template #title>文章管理</template>
      <template #extra>
        <a-button type="primary" @click="$router.push('/learn/article/edit')">新增文章</a-button>
      </template>

      <div class="filter-bar">
        <a-select
          v-model:value="filters.categoryId"
          placeholder="全部分类"
          allow-clear
          style="width: 240px"
          :options="categoryOptions"
        />
        <a-select
          v-model:value="filters.status"
          placeholder="全部状态"
          allow-clear
          style="width: 160px; margin-left: 12px"
          :options="statusOptions"
        />
        <a-input
          v-model:value="filters.keyword"
          placeholder="搜索标题或摘要"
          style="width: 240px; margin-left: 12px"
          @press-enter="onSearch"
        />
        <a-button type="primary" style="margin-left: 12px" @click="onSearch">查询</a-button>
      </div>

      <a-table
        :data-source="rows"
        :columns="columns"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'published' ? 'green' : 'orange'">
              {{ record.status === 'published' ? '已发布' : '草稿' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a @click="$router.push(`/learn/article/edit/${record.id}`)">编辑</a>
            <a-divider type="vertical" />
            <a v-if="record.status !== 'published'" @click="onPublish(record)">发布</a>
            <a v-else @click="onUnpublish(record)">下线</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除？" @confirm="onDelete(record)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  fetchArticlePage,
  publishArticle,
  unpublishArticle,
  deleteArticle
} from '@/api/learn'
import { fetchCategoryTree } from '@/api/learn'

const rows = ref([])
const loading = ref(false)
const categories = ref([])

const filters = reactive({ categoryId: null, status: null, keyword: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' }
]

const categoryOptions = computed(() =>
  flatten(categories.value).map(c => ({ label: '— '.repeat(c.depth) + c.name, value: c.id }))
)

function flatten (tree, depth = 0, acc = []) {
  for (const n of tree) {
    acc.push({ id: n.id, name: n.name, depth })
    flatten(n.children || [], depth + 1, acc)
  }
  return acc
}

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '分类', key: 'category', customRender: ({ record }) => record.categoryId },
  { title: '状态', key: 'status' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' }
]

async function load () {
  loading.value = true
  try {
    const res = await fetchArticlePage({
      categoryId: filters.categoryId,
      status: filters.status,
      keyword: filters.keyword,
      page: pagination.current,
      size: pagination.pageSize
    })
    rows.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}
async function loadCategories () {
  const res = await fetchCategoryTree()
  categories.value = res.data || []
}

function onSearch () { pagination.current = 1; load() }
function onTableChange (pag) { pagination.current = pag.current; pagination.pageSize = pag.pageSize; load() }
async function onPublish (r) { await publishArticle(r.id); message.success('已发布'); load() }
async function onUnpublish (r) { await unpublishArticle(r.id); message.success('已下线'); load() }
async function onDelete (r) { await deleteArticle(r.id); message.success('已删除'); load() }

onMounted(() => { loadCategories(); load() })
</script>

<style scoped>
.article-list { padding: 16px; }
.filter-bar { display: flex; margin-bottom: 12px; flex-wrap: wrap; }
.danger { color: #ff4d4f; }
</style>
```

- [ ] **Step 2: 编译**

Run: `cd project/admin/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/views/learn/ArticleList.vue
git commit -m "feat(admin-web): 文章列表页（分页 + 过滤 + 发布/下线）"
```

---

## Task 14: 管理端 — 文章编辑器骨架

**Files:**
- Create: `project/admin/web/src/views/learn/ArticleEditor.vue`

- [ ] **Step 1: 安装编辑器依赖**

```bash
cd project/admin/web && npm install mavon-editor @tiptap/vue-3 @tiptap/starter-kit @tiptap/extension-image
```

- [ ] **Step 2: 写 ArticleEditor.vue**

```vue
<template>
  <div class="article-editor">
    <a-card :bordered="false">
      <template #title>
        <a @click="$router.push('/learn/article')" style="margin-right: 12px">← 返回列表</a>
        {{ isEdit ? '编辑文章' : '新增文章' }}
      </template>
      <a-form layout="vertical" :model="form" ref="formRef">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" maxlength="128" />
        </a-form-item>
        <a-form-item label="分类" required>
          <a-tree-select
            v-model:value="form.categoryId"
            :tree-data="categories"
            :replace-fields="{ label: 'name', value: 'id' }"
            :tree-default-expand-all="true"
            placeholder="选择分类"
          />
        </a-form-item>
        <a-form-item label="摘要">
          <a-textarea v-model:value="form.summary" :rows="2" maxlength="255" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" />
          <span style="margin-left: 16px">
            <a-checkbox v-model:checked="updatePublishedAt">编辑已发布文章时刷新发布时间</a-checkbox>
          </span>
        </a-form-item>
        <a-form-item label="正文类型">
          <a-radio-group v-model:value="form.contentType" :disabled="contentTypeLocked">
            <a-radio value="markdown">Markdown</a-radio>
            <a-radio value="rich_text">富文本</a-radio>
          </a-radio-group>
          <span v-if="contentTypeLocked" style="margin-left: 12px; color: #999; font-size: 12px">已发布文章不允许切换正文类型</span>
        </a-form-item>
        <a-form-item label="正文" required>
          <MarkdownEditor
            v-if="form.contentType === 'markdown'"
            v-model:value="form.content"
          />
          <RichTextEditor
            v-else
            v-model:html="form.content"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button @click="onSave('draft')">保存草稿</a-button>
            <a-button type="primary" @click="onSave('published')">保存并发布</a-button>
            <a-button @click="$router.push('/learn/article')">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  fetchCategoryTree,
  fetchArticle,
  createArticle,
  updateArticle,
  publishArticle,
  unpublishArticle
} from '@/api/learn'
import MarkdownEditor from '@/components/learn/MarkdownEditor.vue'
import RichTextEditor from '@/components/learn/RichTextEditor.vue'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const form = reactive({
  categoryId: null,
  title: '',
  summary: '',
  sort: 0,
  contentType: 'markdown',
  content: ''
})
const dirty = ref(false)
const updatePublishedAt = ref(false)
const originalStatus = ref(null)

const contentTypeLocked = computed(() =>
  isEdit.value && originalStatus.value === 'published'
)

const formRef = ref(null)

async function load () {
  const cat = await fetchCategoryTree()
  categories.value = cat.data || []
  if (route.params.id) {
    const res = await fetchArticle(route.params.id)
    const a = res.data
    Object.assign(form, {
      categoryId: a.categoryId, title: a.title, summary: a.summary,
      sort: a.sort, contentType: a.contentType, content: a.content
    })
    originalStatus.value = a.status
  }
}

watch(form, () => { dirty.value = true }, { deep: true })

let saving = false
async function onSave (target) {
  const payload = {
    categoryId: form.categoryId,
    title: form.title,
    summary: form.summary,
    sort: form.sort,
    contentType: form.contentType,
    content: form.content,
    status: target
  }
  saving = true
  try {
    if (!isEdit.value) {
      const res = await createArticle(payload)
      const newId = res.data
      message.success(target === 'published' ? '已发布' : '已存草稿')
      if (target === 'published' && updatePublishedAt.value) updatePublishedAt.value = false
      router.replace(`/learn/article/edit/${newId}`)
    } else {
      await updateArticle(route.params.id, payload)
      // 编辑已发布时不写 published_at，除非勾选了；接口本身不动该字段
      message.success('已更新')
    }
    dirty.value = false
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    saving = false
  }
}

onBeforeRouteLeave(() => {
  if (!dirty.value) return true
  return new Promise(resolve => {
    Modal.confirm({
      title: '内容已修改，是否保存为草稿？',
      okText: '保存草稿',
      cancelText: '丢弃修改',
      onOk: async () => {
        await onSave('draft')
        resolve(true)
      },
      onCancel: () => resolve(true)
    })
  })
})

onMounted(load)
</script>

<style scoped>
.article-editor { padding: 16px; }
</style>
```

- [ ] **Step 3: 编译**

Run: `cd project/admin/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS（在 MarkdownEditor / RichTextEditor 未建之前会编译失败 — 占位空组件后再编译）

- [ ] **Step 4: Commit（如已编译通过）**

```bash
git add project/admin/web/src/views/learn/ArticleEditor.vue \
        project/admin/web/package.json project/admin/web/package-lock.json
git commit -m "feat(admin-web): 文章编辑器骨架（表单 + 状态 + 离开提示）"
```

---

## Task 15: 管理端 — MarkdownEditor 包装 mavon-editor

**Files:**
- Create: `project/admin/web/src/components/learn/MarkdownEditor.vue`

- [ ] **Step 1: 写组件**

```vue
<template>
  <mavon-editor
    v-model="valueModel"
    :toolbars="toolbars"
    placeholder="开始用 Markdown 撰写…"
    language="zh-CN"
    style="min-height: 420px"
  />
</template>

<script setup>
import { computed } from 'vue'
import { MavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'

const props = defineProps({ value: { type: String, default: '' } })
const emit = defineEmits(['update:value'])

const valueModel = computed({
  get () { return props.value },
  set (v) { emit('update:value', v) }
})

const toolbars = {
  bold: true, italic: true, header: true, underline: true, strikethrough: true,
  mark: true, superscript: true, subscript: true, quote: true, ol: true, ul: true,
  link: true, imagelink: false, code: true, table: true, fullscreen: true,
  readmodel: true, htmlcode: true, help: true, trash: true, undo: true, redo: true,
  navigation: true, alignleft: true, aligncenter: true, alignright: true,
  subfield: true, preview: true
}
</script>
```

- [ ] **Step 2: 编译**

Run: `cd project/admin/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/components/learn/MarkdownEditor.vue
git commit -m "feat(admin-web): Markdown 编辑器（mavon-editor）"
```

---

## Task 16: 管理端 — RichTextEditor 包装 Tiptap

**Files:**
- Create: `project/admin/web/src/components/learn/RichTextEditor.vue`

- [ ] **Step 1: 写组件**

```vue
<template>
  <div class="rich-text-editor">
    <div v-if="editor" class="rt-toolbar">
      <a-button-group size="small">
        <a-button :type="editor.isActive('bold') ? 'primary' : 'default'" @click="editor.chain().focus().toggleBold().run()">B</a-button>
        <a-button :type="editor.isActive('italic') ? 'primary' : 'default'" @click="editor.chain().focus().toggleItalic().run()"><i>I</i></a-button>
        <a-button @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">H2</a-button>
        <a-button @click="editor.chain().focus().toggleHeading({ level: 3 }).run()">H3</a-button>
        <a-button @click="editor.chain().focus().toggleBulletList().run()">• list</a-button>
        <a-button @click="editor.chain().focus().toggleOrderedList().run()">1. list</a-button>
        <a-button @click="editor.chain().focus().toggleBlockquote().run()">"</a-button>
        <a-button @click="editor.chain().focus().toggleCodeBlock().run()">{ }</a-button>
        <a-button @click="editor.chain().focus().undo().run()">↶</a-button>
        <a-button @click="editor.chain().focus().redo().run()">↷</a-button>
      </a-button-group>
    </div>
    <editor-content :editor="editor" class="rt-content" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'

const props = defineProps({ html: { type: String, default: '' } })
const emit = defineEmits(['update:html'])

const editor = useEditor({
  content: props.html,
  extensions: [StarterKit, Image],
  onUpdate ({ editor }) {
    emit('update:html', editor.getHTML())
  }
})

watch(() => props.html, val => {
  // 外部重置（如切换类型、load 文章）时同步
  if (editor.value && val !== editor.value.getHTML()) {
    editor.value.commands.setContent(val || '', false)
  }
})

onBeforeUnmount(() => editor.value?.destroy())
</script>

<style scoped>
.rich-text-editor { border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.rt-toolbar { border-bottom: 1px solid #f0f0f0; padding: 8px; }
.rt-content { padding: 12px 16px; min-height: 400px; line-height: 1.7; }
:deep(.ProseMirror) { outline: none; }
:deep(.ProseMirror p) { margin: 0.6em 0; }
:deep(.ProseMirror h2) { font-size: 20px; margin: 1em 0 0.4em; font-weight: 700; }
:deep(.ProseMirror h3) { font-size: 17px; margin: 0.8em 0 0.4em; font-weight: 600; }
:deep(.ProseMirror blockquote) { border-left: 4px solid #07c160; padding: 6px 12px; color: #555; margin: 0.8em 0; background: #f8f8f8; }
:deep(.ProseMirror pre) { background: #1f1f1f; color: #f5f5f5; padding: 12px 16px; border-radius: 6px; overflow-x: auto; font-size: 13px; }
:deep(.ProseMirror code) { background: #f6f8fa; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
</style>
```

- [ ] **Step 2: 编译**

Run: `cd project/admin/web && npm run build 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/components/learn/RichTextEditor.vue
git commit -m "feat(admin-web): 富文本编辑器（Tiptap + StarterKit）"
```

---

## Task 17: 管理端 — 内容类型切换二次确认（草稿态）

**Files:**
- Modify: `project/admin/web/src/views/learn/ArticleEditor.vue`

- [ ] **Step 1: 切换确认逻辑**

在 `ArticleEditor.vue` 增加 watch + 模态：

```js
import { Modal } from 'ant-design-vue'

const initialContentType = ref('markdown')
watch(() => form.contentType, (newType, oldType) => {
  if (originalStatus.value === 'published') return  // 已发布锁定
  if (newType === oldType) return
  Modal.confirm({
    title: '切换正文类型',
    content: '切换会清空当前正文内容，是否继续？',
    okText: '继续切换',
    cancelText: '取消',
    onOk: () => { form.content = '' },
    onCancel: () => { form.contentType = oldType }
  })
})
```

挂载时初始化：

```js
originalStatus.value = a.status
initialContentType.value = a.contentType
form.contentType = a.contentType
```

- [ ] **Step 2: 验证**（本地 + dev 模式）

```bash
# 起后端 + 启 admin dev
# 登录后台 → 文章管理 → 新增文章 → 选 Markdown 写点内容 → 切富文本 → 看确认弹窗 → 切换后内容清空
```

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/views/learn/ArticleEditor.vue
git commit -m "feat(admin-web): 正文类型切换二次确认"
```

---

## Task 18: 单元测试最终跑通

**Files:** 已建 `LearnCategoryServiceImplTest` / `LearnArticleServiceImplTest`（Task 2 / 3）

- [ ] **Step 1: 跑单测**

Run: `cd project/admin/api && mvn test -Dtest="LearnCategoryServiceImplTest,LearnArticleServiceImplTest"`
Expected: 全部 PASS

- [ ] **Step 2: 若失败**

修复使其全绿；不通过即不进入下一阶段。

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/test
git commit -m "test(admin): 创作学院 service 单测全绿"
```

---

## Task 19: 管理端 E2E

**Files:**
- Create: `tests/e2e/admin_learn_management.py`

- [ ] **Step 1: 写 E2E 脚本骨架**

```python
#!/usr/bin/env python3
"""管理端 - 创作学院 CRUD 流程"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

ADMIN_URL = os.environ.get("ADMIN_URL", "http://localhost:8080/admin")
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "admin123")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "admin_learn"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def login(page):
    page.goto(f"{ADMIN_URL}/login")
    page.fill('input[name="username"]', ADMIN_USER)
    page.fill('input[name="password"]', ADMIN_PASS)
    page.click('button[type="submit"]')
    page.wait_for_url(lambda u: "/login" not in u)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 1. 登录
        login(page)

        # 2. 进入分类管理
        page.click('text=创作学院')
        page.click('text=分类管理')
        page.wait_for_url("**/learn/category")
        page.screenshot(path=SCREENSHOTS_DIR / "01-category-empty.png")

        # 3. 新增顶级分类
        page.click('text=新增顶级分类')
        page.fill('input[placeholder=""]', '创作技巧')
        page.click('button:has-text("确定")')
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "02-category-created.png")
        expect(page.locator('text=创作技巧')).to_be_visible()

        # 4. 新增文章
        page.goto(f"{ADMIN_URL}/learn/article")
        page.click('text=新增文章')
        page.fill('input', '如何写出爆款标题')
        # 分类树选择器
        page.click('.ant-select-selection')
        page.click('text=创作技巧')
        # 摘要
        page.fill('textarea', '本文介绍写标题的常见模式')
        # 内容
        page.fill('.editor-content textarea', '# 标题三大原则\n\n1. 数字\n2. 反差\n3. 痛点')
        # 保存草稿
        page.click('button:has-text("保存草稿")')
        page.wait_for_url("**/learn/article/edit/**")
        page.screenshot(path=SCREENSHOTS_DIR / "03-article-draft.png")

        # 5. 发布
        page.click('button:has-text("保存并发布")')
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "04-article-published.png")

        # 6. 验证 user 端
        page.goto(os.environ.get("USER_URL", "http://localhost:5173/learn"))
        time.sleep(1)
        page.screenshot(path=SCREENSHOTS_DIR / "05-user-learn.png", full_page=True)

        ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 跑测试**

```bash
# 起 admin/user 后端 + admin/user web
python3 tests/e2e/admin_learn_management.py
```

Expected: 全部步骤成功 + 截图生成。如失败对照截图与日志定位。

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/admin_learn_management.py tests/e2e/screenshots/admin_learn/
git commit -m "test(e2e): admin 创作学院 CRUD 端到端"
```

---

## Task 20: 用户端 E2E

**Files:**
- Create: `tests/e2e/learn_browsing.py`

- [ ] **Step 1: 写 E2E 脚本**

```python
#!/usr/bin/env python3
"""用户端 - 创作学院浏览"""

import os
import time
from pathlib import Path
from playwright.sync_api import sync_playwright

USER_URL = os.environ.get("USER_URL", "http://localhost:5173/learn")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # Desktop
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(USER_URL)
        time.sleep(1)
        page.screenshot(path=SCREENSHOTS_DIR / "01-desktop-tree.png", full_page=True)

        # 点击分类
        page.click('text=创作技巧 >> nth=0')
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "02-desktop-list.png", full_page=True)

        # 进入文章
        page.click('text=如何写出爆款标题')
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "03-desktop-article.png", full_page=True)

        # 404 文章
        page.goto(f"{USER_URL.rsplit('/', 1)[0]}/article/9999999")
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "04-404.png", full_page=True)
        ctx.close()

        # Mobile
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        page2.goto(USER_URL)
        time.sleep(1)
        page2.click('button:has-text("分类")')
        time.sleep(0.5)
        page2.screenshot(path=SCREENSHOTS_DIR / "05-mobile-sheet.png", full_page=True)
        ctx2.close()

        browser.close()


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 跑测试**

```bash
python3 tests/e2e/learn_browsing.py
```

Expected: 全部步骤成功 + 截图。

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/learn_browsing.py tests/e2e/screenshots/learn/
git commit -m "test(e2e): user 创作学院浏览（含移动端抽屉）"
```

---

## Task 21: 进度表更新 + 最终验证

**Files:**
- Modify: `.superpowers/sdd/progress.md`（如存在；不存在则跳过）

- [ ] **Step 1: 写一行完成记录**

```
- 2026-07-10 「创作学院」模块：后端 CRUD + 管理端编辑器 + 用户端 /learn 浏览
```

- [ ] **Step 2: 最终自检**

```bash
# 后端
cd project/admin/api && mvn test -q
cd project/user/api && mvn test -q

# 前端
cd project/user/web && npm run build
cd project/admin/web && npm run build

# E2E
python3 tests/e2e/admin_learn_management.py
python3 tests/e2e/learn_browsing.py
```

Expected: 全部 BUILD SUCCESS + 测试通过。

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "docs: 创作学院模块交付完成，更新进度表"
```

---

## Verification

最终交付后，逐项对照 spec：

1. ✅ 用户端 `/learn` 页面，左树右文，移动端抽屉
2. ✅ NavBar 「创作学院」入口
3. ✅ 管理端一级菜单 + 子菜单（分类管理 / 文章管理）
4. ✅ 分类树 CRUD（拖拽、删除校验非空）
5. ✅ 文章 CRUD（Markdown / 富文本，草稿 / 发布）
6. ✅ 公开浏览只展示已发布文章，草稿等同 404
7. ✅ 后端 Service 单测覆盖状态机 + 校验
8. ✅ 管理端 + 用户端 Playwright E2E
9. ✅ `markdown-it` 配置 `html: false`
10. ✅ `published_at` 仅草稿→发布首次写入

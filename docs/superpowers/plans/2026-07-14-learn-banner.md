# 创作学院 Banner + 推荐分类 实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 管理端可配置 banner（图片 URL + 跳转链接 + 排序）和分类推荐开关；用户端空状态页展示 banner 轮播 + 推荐分类卡片。

**Architecture:** Admin 端新增 banner CRUD（entity → mapper → service → controller），分类管理加 `is_recommended` 字段；User 端新增 banner 只读接口，分类树 VO 加推荐标记；前端管理端加 banner 管理页 + 分类推荐开关，用户端空状态页重新设计为 banner 轮播 + 推荐分类卡片。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + Vue 3 + Ant Design Vue（a-carousel）+ Playwright (e2e)。

## Global Constraints

- 不新增任何中间件或外部依赖。
- banner 只填图片 URL，不做图片上传。
- 管理端 entity 继承 `BaseEntity`（自动审计字段 + 软删除）；用户端 entity 不继承，手动声明字段。
- 错误码段位 `2700xx`，新增从 `270007` 开始。
- Flyway 迁移号：`V2.0.0_025`（admin）。
- 品牌色 `#FF2442`，圆角卡片 `8px`。
- 移动端断点 `< 992px`。
- 环境变量：`MYSQL_USERNAME=root`, `MYSQL_PASSWORD=123456`, `JASYPT_ENCRYPTOR_PASSWORD=MySecretKey2024`。
- 端口：admin-api 25051，user-api 25050，admin-web 22346，user-web 22345。

---

## 文件结构

| 操作 | 路径 | 责任 |
|---|---|---|
| 新建 | `project/admin/api/.../db/migration/V2.0.0_025__create_learn_banner_and_category_recommended.sql` | 建 banner 表 + 分类加推荐字段 |
| 新建 | `project/admin/api/.../learn/entity/LearnBannerEntity.java` | banner 实体（extends BaseEntity） |
| 新建 | `project/admin/api/.../learn/mapper/LearnBannerMapper.java` | banner mapper |
| 新建 | `project/admin/api/.../learn/dto/request/LearnBannerReq.java` | banner 请求 DTO |
| 新建 | `project/admin/api/.../learn/vo/LearnBannerVO.java` | banner VO |
| 新建 | `project/admin/api/.../learn/service/LearnBannerService.java` | banner service 接口 |
| 新建 | `project/admin/api/.../learn/service/impl/LearnBannerServiceImpl.java` | banner service 实现 |
| 修改 | `project/admin/api/.../learn/controller/LearnAdminController.java` | 新增 banner CRUD 端点 |
| 修改 | `project/admin/api/.../learn/dto/request/LearnCategoryReq.java` | 加 isRecommended |
| 修改 | `project/admin/api/.../learn/entity/LearnCategoryEntity.java` | 加 isRecommended |
| 修改 | `project/admin/api/.../learn/vo/LearnCategoryTreeNode.java` | 加 isRecommended |
| 修改 | `project/admin/api/.../learn/service/impl/LearnCategoryServiceImpl.java` | update() 处理 isRecommended |
| 修改 | `project/admin/api/.../learn/exception/LearnErrorCode.java` | 加 BANNER_NOT_FOUND |
| 新建 | `project/user/api/.../learn/entity/LearnBannerEntity.java` | user 端 banner 只读实体 |
| 新建 | `project/user/api/.../learn/mapper/LearnBannerMapper.java` | user 端 banner mapper |
| 新建 | `project/user/api/.../learn/vo/LearnBannerVO.java` | user 端 banner VO |
| 修改 | `project/user/api/.../learn/controller/LearnController.java` | 新增 banner 端点 |
| 修改 | `project/user/api/.../learn/service/LearnBrowseService.java` | 加 banners() 方法 |
| 修改 | `project/user/api/.../learn/service/impl/LearnBrowseServiceImpl.java` | 实现 banners() + tree() 加 isRecommended |
| 修改 | `project/user/api/.../learn/entity/LearnCategoryEntity.java` | 加 isRecommended |
| 修改 | `project/user/api/.../learn/vo/LearnCategoryTreeVO.java` | 加 isRecommended |
| 新建 | `project/admin/web/src/views/LearnBannerView.vue` | banner 管理页 |
| 修改 | `project/admin/web/src/api/learn.js` | 加 banner API |
| 修改 | `project/admin/web/src/router/index.js` | 加 banner 路由 |
| 修改 | `project/admin/web/src/layouts/AdminLayout.vue` | 加 banner 菜单 |
| 修改 | `project/admin/web/src/components/learn/CategoryTreeEditor.vue` | 加推荐开关 |
| 修改 | `project/user/web/src/api/learn.js` | 加 banner API |
| 修改 | `project/user/web/src/views/LearnIndex.vue` | 空状态页 banner 轮播 + 推荐分类 |
| 新建 | `tests/e2e/learn_banner.py` | e2e 验证脚本 |

---

### Task 1: Flyway 迁移

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_025__create_learn_banner_and_category_recommended.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- 创作学院 banner 表 + 分类推荐字段
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS t_learn_banner (
    id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    image_url     VARCHAR(512)     NOT NULL                COMMENT '图片 URL',
    link_url      VARCHAR(512)     NOT NULL DEFAULT ''     COMMENT '点击跳转链接',
    sort          INT              NOT NULL DEFAULT 0      COMMENT '排序权重，小在前',
    is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '是否删除：0-否，1-是',
    created_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    updated_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作学院 banner';

ALTER TABLE t_article_category
    ADD COLUMN is_recommended TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否推荐 0=否 1=是' AFTER sort;
```

- [ ] **Step 2: 验证迁移**

启动 admin-api，确认 Flyway 迁移成功：

```bash
cd project/admin/api
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 JASYPT_ENCRYPTOR_PASSWORD=MySecretKey2024 \
  mvn spring-boot:run -Dspring-boot.run.profiles=local &
# 等启动完成后检查日志中是否有：
# "Migrating schema ... to version 2.0.0 025"
# 或直接查数据库：
mysql -uroot -p123456 aichuangzuo -e "DESC t_learn_banner; DESC t_article_category;"
```

预期：`t_learn_banner` 表存在且有 6 个业务字段；`t_article_category` 有 `is_recommended` 列。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_025__create_learn_banner_and_category_recommended.sql
git commit -m "feat(admin-api): 创作学院 banner 表 + 分类推荐字段迁移

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: Admin API — Banner CRUD

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/entity/LearnBannerEntity.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/mapper/LearnBannerMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/dto/request/LearnBannerReq.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/vo/LearnBannerVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/service/LearnBannerService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnBannerServiceImpl.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/controller/LearnAdminController.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/exception/LearnErrorCode.java`

**Interfaces:**
- Consumes: `BaseEntity`（`project/shared/.../entity/BaseEntity.java`）、`Result`、`BusinessException`、`ErrorCode`
- Produces:
  - `LearnBannerEntity`（`id/imageUrl/linkUrl/sort` + BaseEntity 审计字段）
  - `LearnBannerMapper extends BaseMapper<LearnBannerEntity>`
  - `LearnBannerReq`（`imageUrl/linkUrl/sort`）
  - `LearnBannerVO`（`id/imageUrl/linkUrl/sort/createdAt/updatedAt`）
  - `LearnBannerService`（`list()/create(req)/update(id, req)/delete(id)`）
  - Controller 端点：`GET/POST /banner`、`PUT/DELETE /banner/{id}`

- [ ] **Step 1: 新建 `LearnBannerEntity.java`**

```java
package com.aichuangzuo.admin.modules.learn.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_learn_banner")
public class LearnBannerEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 图片 URL */
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 排序权重，小在前 */
    private Integer sort;
}
```

- [ ] **Step 2: 新建 `LearnBannerMapper.java`**

```java
package com.aichuangzuo.admin.modules.learn.mapper;

import com.aichuangzuo.admin.modules.learn.entity.LearnBannerEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearnBannerMapper extends BaseMapper<LearnBannerEntity> {
}
```

- [ ] **Step 3: 新建 `LearnBannerReq.java`**

```java
package com.aichuangzuo.admin.modules.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearnBannerReq {

    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 512)
    private String imageUrl;

    @Size(max = 512)
    private String linkUrl = "";

    @NotNull
    private Integer sort = 0;
}
```

- [ ] **Step 4: 新建 `LearnBannerVO.java`**

```java
package com.aichuangzuo.admin.modules.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearnBannerVO {
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: 新建 `LearnBannerService.java`**

```java
package com.aichuangzuo.admin.modules.learn.service;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnBannerReq;
import com.aichuangzuo.admin.modules.learn.vo.LearnBannerVO;

import java.util.List;

public interface LearnBannerService {

    List<LearnBannerVO> list();

    Long create(LearnBannerReq req);

    void update(Long id, LearnBannerReq req);

    void delete(Long id);
}
```

- [ ] **Step 6: 新建 `LearnBannerServiceImpl.java`**

```java
package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnBannerReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnBannerEntity;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnBannerMapper;
import com.aichuangzuo.admin.modules.learn.service.LearnBannerService;
import com.aichuangzuo.admin.modules.learn.vo.LearnBannerVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnBannerServiceImpl implements LearnBannerService {

    private final LearnBannerMapper mapper;

    @Override
    public List<LearnBannerVO> list() {
        return mapper.selectList(new QueryWrapper<LearnBannerEntity>().orderByAsc("sort"))
                .stream().map(this::toVo).toList();
    }

    @Override
    public Long create(LearnBannerReq req) {
        LearnBannerEntity e = new LearnBannerEntity();
        e.setImageUrl(req.getImageUrl());
        e.setLinkUrl(req.getLinkUrl() != null ? req.getLinkUrl() : "");
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.insert(e);
        return e.getId();
    }

    @Override
    public void update(Long id, LearnBannerReq req) {
        LearnBannerEntity e = requireExisting(id);
        e.setImageUrl(req.getImageUrl());
        e.setLinkUrl(req.getLinkUrl() != null ? req.getLinkUrl() : "");
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.updateById(e);
    }

    @Override
    public void delete(Long id) {
        requireExisting(id);
        mapper.deleteById(id);
    }

    private LearnBannerEntity requireExisting(Long id) {
        LearnBannerEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(LearnErrorCode.BANNER_NOT_FOUND);
        }
        return e;
    }

    private LearnBannerVO toVo(LearnBannerEntity e) {
        LearnBannerVO v = new LearnBannerVO();
        v.setId(e.getId());
        v.setImageUrl(e.getImageUrl());
        v.setLinkUrl(e.getLinkUrl());
        v.setSort(e.getSort());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
```

- [ ] **Step 7: 修改 `LearnErrorCode.java` — 新增 BANNER_NOT_FOUND**

在 `CATEGORY_NOT_FOUND(270006, "分类不存在");` 之后改为：

```java
    CATEGORY_NOT_FOUND(270006, "分类不存在"),
    BANNER_NOT_FOUND(270007, "Banner 不存在");
```

（注意把原来 `CATEGORY_NOT_FOUND` 末尾的分号改为逗号。）

- [ ] **Step 8: 修改 `LearnAdminController.java` — 注入 bannerService + 新增端点**

在字段声明区加：

```java
    private final LearnBannerService bannerService;
```

在文件末尾（`}` 之前）加：

```java
    // ---------- Banner ----------

    @Operation(summary = "Banner 列表")
    @GetMapping("/banner")
    public Result<List<LearnBannerVO>> bannerList() {
        return Result.success(bannerService.list());
    }

    @Operation(summary = "新增 Banner")
    @PostMapping("/banner")
    public Result<Long> createBanner(@Valid @RequestBody LearnBannerReq req) {
        return Result.success(bannerService.create(req));
    }

    @Operation(summary = "更新 Banner")
    @PutMapping("/banner/{id}")
    public Result<Void> updateBanner(@PathVariable Long id, @Valid @RequestBody LearnBannerReq req) {
        bannerService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除 Banner")
    @DeleteMapping("/banner/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.success();
    }
```

同时在 import 区加：

```java
import com.aichuangzuo.admin.modules.learn.dto.request.LearnBannerReq;
import com.aichuangzuo.admin.modules.learn.service.LearnBannerService;
import com.aichuangzuo.admin.modules.learn.vo.LearnBannerVO;
```

- [ ] **Step 9: 编译验证**

```bash
cd project/admin/api
mvn compile -q
```

预期：编译成功，无错误。

- [ ] **Step 10: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/
git commit -m "feat(admin-api): 创作学院 banner CRUD 接口

- LearnBannerEntity / Mapper / Service / Controller
- 4 个端点：GET/POST /banner、PUT/DELETE /banner/{id}
- 错误码 BANNER_NOT_FOUND(270007)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: Admin API — 分类推荐字段

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/entity/LearnCategoryEntity.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/dto/request/LearnCategoryReq.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/vo/LearnCategoryTreeNode.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/service/impl/LearnCategoryServiceImpl.java`

**Interfaces:**
- Consumes: 现有 `LearnCategoryEntity`、`LearnCategoryReq`、`LearnCategoryTreeNode`
- Produces: 分类 entity/req/VO 均加 `isRecommended` 字段

- [ ] **Step 1: 修改 `LearnCategoryEntity.java` — 加 isRecommended**

在 `private Integer sort;` 之后加：

```java
    /** 是否推荐：0=否 1=是 */
    private Integer isRecommended;
```

- [ ] **Step 2: 修改 `LearnCategoryReq.java` — 加 isRecommended**

在 `private Integer sort = 0;` 之后加：

```java
    /** 是否推荐：0=否 1=是 */
    private Integer isRecommended = 0;
```

- [ ] **Step 3: 修改 `LearnCategoryTreeNode.java` — 加 isRecommended**

在 `private Integer sort;` 之后加：

```java
    private Integer isRecommended;
```

- [ ] **Step 4: 修改 `LearnCategoryServiceImpl.java` — tree() 和 update() 处理 isRecommended**

在 `tree()` 方法的 VO 构建块中，在 `n.setSort(e.getSort());` 之后加：

```java
                    n.setIsRecommended(e.getIsRecommended());
```

在 `update()` 方法的 `exist.setSort(...)` 之后加：

```java
        exist.setIsRecommended(req.getIsRecommended() != null ? req.getIsRecommended() : 0);
```

- [ ] **Step 5: 编译验证**

```bash
cd project/admin/api
mvn compile -q
```

- [ ] **Step 6: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/learn/
git commit -m "feat(admin-api): 创作学院分类加推荐字段

- LearnCategoryEntity/Req/TreeNode 加 isRecommended
- tree() 返回推荐标记，update() 处理推荐开关

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: User API — Banner 只读 + 分类树推荐标记

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/entity/LearnBannerEntity.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/mapper/LearnBannerMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnBannerVO.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/entity/LearnCategoryEntity.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnCategoryTreeVO.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/service/LearnBrowseService.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/service/impl/LearnBrowseServiceImpl.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/controller/LearnController.java`

**Interfaces:**
- Consumes: Task 1 的 `t_learn_banner` 表、Task 3 的 `is_recommended` 列
- Produces:
  - `LearnBannerEntity`（user 端只读，`id/imageUrl/linkUrl/sort`）
  - `LearnBannerVO`（`id/imageUrl/linkUrl`）
  - `LearnBrowseService.banners()` → `List<LearnBannerVO>`
  - `LearnCategoryTreeVO.isRecommended`
  - Controller 端点：`GET /banner`

- [ ] **Step 1: 新建 user 端 `LearnBannerEntity.java`**

```java
package com.aichuangzuo.user.modules.learn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_learn_banner")
public class LearnBannerEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageUrl;

    private String linkUrl;

    private Integer sort;

    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
```

- [ ] **Step 2: 新建 user 端 `LearnBannerMapper.java`**

```java
package com.aichuangzuo.user.modules.learn.mapper;

import com.aichuangzuo.user.modules.learn.entity.LearnBannerEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearnBannerMapper extends BaseMapper<LearnBannerEntity> {
}
```

- [ ] **Step 3: 新建 user 端 `LearnBannerVO.java`**

```java
package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;

@Data
public class LearnBannerVO {
    private Long id;
    private String imageUrl;
    private String linkUrl;
}
```

- [ ] **Step 4: 修改 user 端 `LearnCategoryEntity.java` — 加 isRecommended**

在 `private Integer sort;` 之后加：

```java
    private Integer isRecommended;
```

- [ ] **Step 5: 修改 user 端 `LearnCategoryTreeVO.java` — 加 isRecommended**

在 `private Integer sort;` 之后加：

```java
    private Integer isRecommended;
```

- [ ] **Step 6: 修改 `LearnBrowseService.java` — 加 banners() 方法**

在 `articleDetail` 方法声明之后加：

```java
    /** Banner 列表（所有未删除，按 sort ASC） */
    List<LearnBannerVO> banners();
```

同时在 import 区加：

```java
import com.aichuangzuo.user.modules.learn.vo.LearnBannerVO;
```

- [ ] **Step 7: 修改 `LearnBrowseServiceImpl.java` — 注入 bannerMapper + 实现 banners() + tree() 加 isRecommended**

在字段声明区加：

```java
    private final LearnBannerMapper bannerMapper;
```

在 `buildTree()` 方法的 VO 构建块中，在 `n.setSort(e.getSort());` 之后加：

```java
            n.setIsRecommended(e.getIsRecommended());
```

在 `articleDetail()` 方法之后加：

```java
    @Override
    public List<LearnBannerVO> banners() {
        return bannerMapper.selectList(new QueryWrapper<LearnBannerEntity>().orderByAsc("sort"))
                .stream().map(e -> {
                    LearnBannerVO v = new LearnBannerVO();
                    v.setId(e.getId());
                    v.setImageUrl(e.getImageUrl());
                    v.setLinkUrl(e.getLinkUrl());
                    return v;
                }).toList();
    }
```

同时在 import 区加：

```java
import com.aichuangzuo.user.modules.learn.entity.LearnBannerEntity;
import com.aichuangzuo.user.modules.learn.mapper.LearnBannerMapper;
import com.aichuangzuo.user.modules.learn.vo.LearnBannerVO;
```

- [ ] **Step 8: 修改 `LearnController.java` — 新增 banner 端点**

在 `articleDetail()` 方法之后加：

```java
    @Operation(summary = "Banner 列表")
    @GetMapping("/banner")
    public Result<List<LearnBannerVO>> banners() {
        return Result.success(service.banners());
    }
```

同时在 import 区加：

```java
import com.aichuangzuo.user.modules.learn.vo.LearnBannerVO;
```

- [ ] **Step 9: 编译验证**

```bash
cd project/user/api
mvn compile -q
```

- [ ] **Step 10: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/
git commit -m "feat(user-api): 创作学院 banner 只读接口 + 分类树加推荐标记

- GET /api/v1/user/learn/banner 返回所有未删除 banner
- 分类树 VO 加 isRecommended 字段

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: Admin Web — Banner 管理页

**Files:**
- Create: `project/admin/web/src/views/LearnBannerView.vue`
- Modify: `project/admin/web/src/api/learn.js`
- Modify: `project/admin/web/src/router/index.js`
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`

**Interfaces:**
- Consumes: Task 2 的 admin banner CRUD API
- Produces: `/console/learn/banner` 路由 + 侧边栏菜单项

- [ ] **Step 1: 修改 `api/learn.js` — 加 banner API**

在文件末尾加：

```js
// ---------- Banner ----------
export function fetchBanners() {
  return request.get(`${BASE}/banner`).then((res) => res.data || [])
}
export function createBanner(data) {
  return request.post(`${BASE}/banner`, data).then((res) => res.data)
}
export function updateBanner(id, data) {
  return request.put(`${BASE}/banner/${id}`, data)
}
export function deleteBanner(id) {
  return request.delete(`${BASE}/banner/${id}`)
}
```

- [ ] **Step 2: 新建 `LearnBannerView.vue`**

```vue
<template>
  <div class="banner-view">
    <a-card title="Banner 管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="onCreate">新增 Banner</a-button>
      </template>
      <a-table :columns="columns" :data-source="banners" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'imageUrl'">
            <img :src="record.imageUrl" style="width: 120px; height: 60px; object-fit: cover; border-radius: 4px;" />
          </template>
          <template v-else-if="column.key === 'linkUrl'">
            <span>{{ record.linkUrl || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="onEdit(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该 Banner？" @confirm="onDelete(record)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editing ? '编辑 Banner' : '新增 Banner'"
      :confirm-loading="submitting"
      @ok="onSubmit"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="图片 URL" required>
          <a-input v-model:value="form.imageUrl" placeholder="https://..." />
        </a-form-item>
        <a-form-item label="跳转链接">
          <a-input v-model:value="form.linkUrl" placeholder="（可选）https://..." />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { fetchBanners, createBanner, updateBanner, deleteBanner } from '@/api/learn'

const banners = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref(null)
const form = reactive({ imageUrl: '', linkUrl: '', sort: 0 })

const columns = [
  { title: '预览', key: 'imageUrl', width: 140 },
  { title: '图片 URL', dataIndex: 'imageUrl', key: 'imageUrl', ellipsis: true },
  { title: '跳转链接', key: 'linkUrl', ellipsis: true },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '操作', key: 'action', width: 120 }
]

async function load() {
  loading.value = true
  try {
    banners.value = await fetchBanners()
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onCreate() {
  editing.value = null
  form.imageUrl = ''; form.linkUrl = ''; form.sort = 0
  modalOpen.value = true
}

function onEdit(record) {
  editing.value = record
  form.imageUrl = record.imageUrl
  form.linkUrl = record.linkUrl || ''
  form.sort = record.sort
  modalOpen.value = true
}

async function onDelete(record) {
  try {
    await deleteBanner(record.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

async function onSubmit() {
  if (!form.imageUrl.trim()) { message.error('图片 URL 不能为空'); return }
  submitting.value = true
  try {
    if (editing.value) {
      await updateBanner(editing.value.id, form)
    } else {
      await createBanner(form)
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
.banner-view { padding: 0; }
.danger { color: #ff4d4f; }
</style>
```

- [ ] **Step 3: 修改 `router/index.js` — 加 banner 路由**

在 `learn/article/edit/:id?` 路由之后加：

```js
      {
        path: 'learn/banner',
        name: 'AdminLearnBanner',
        component: () => import('@/views/LearnBannerView.vue')
      }
```

- [ ] **Step 4: 修改 `AdminLayout.vue` — 加 banner 菜单项**

在 `<a-menu-item key="/console/learn/article">文章管理</a-menu-item>` 之后加：

```vue
          <a-menu-item key="/console/learn/banner">Banner 管理</a-menu-item>
```

- [ ] **Step 5: 验证**

启动 admin-api 和 admin-web，访问 `http://localhost:22346/console/learn/banner`，确认：
- 页面加载正常
- 点击「新增 Banner」弹框正常
- 填写图片 URL + 排序，保存后表格刷新显示

- [ ] **Step 6: 提交**

```bash
git add project/admin/web/src/views/LearnBannerView.vue \
        project/admin/web/src/api/learn.js \
        project/admin/web/src/router/index.js \
        project/admin/web/src/layouts/AdminLayout.vue
git commit -m "feat(admin-web): 创作学院 banner 管理页

- 表格展示 banner 列表（预览图 + URL + 链接 + 排序）
- 新增/编辑弹框（图片 URL + 跳转链接 + 排序）
- 删除带确认

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: Admin Web — 分类推荐开关

**Files:**
- Modify: `project/admin/web/src/components/learn/CategoryTreeEditor.vue`

**Interfaces:**
- Consumes: Task 3 的分类推荐 API（`updateCategory` 支持 `isRecommended`）
- Produces: 分类树每个节点旁有推荐开关

- [ ] **Step 1: 修改 `CategoryTreeEditor.vue` — 模板加推荐开关**

在 `<template #title="{ dataRef }">` 的 `<span>{{ dataRef.name }}</span>` 之后、`<span class="row-actions">` 之前加：

```vue
          <a-tag v-if="dataRef.isRecommended" color="red" style="margin-left: 8px; font-size: 11px;">推荐</a-tag>
```

在 `<span class="row-actions">` 内的 `<a @click.stop="onEdit(dataRef)">编辑</a>` 之后加：

```vue
            <a-divider type="vertical" />
            <a @click.stop="onToggleRecommend(dataRef)">
              {{ dataRef.isRecommended ? '取消推荐' : '推荐' }}
            </a>
```

- [ ] **Step 2: 修改 `CategoryTreeEditor.vue` — script 加 onToggleRecommend**

在 `onDelete` 函数之后加：

```js
async function onToggleRecommend(node) {
  try {
    await updateCategory(node.id, {
      parentId: node.parentId,
      name: node.name,
      sort: node.sort,
      isRecommended: node.isRecommended ? 0 : 1
    })
    message.success(node.isRecommended ? '已取消推荐' : '已设为推荐')
    await load()
  } catch (e) {
    message.error(e?.message || '操作失败')
  }
}
```

- [ ] **Step 3: 验证**

在管理端分类管理页，点击某个分类的「推荐」，确认：
- tag 标签出现/消失
- 刷新页面后状态保持

- [ ] **Step 4: 提交**

```bash
git add project/admin/web/src/components/learn/CategoryTreeEditor.vue
git commit -m "feat(admin-web): 创作学院分类管理加推荐开关

- 分类节点旁显示推荐 tag
- 点击推荐/取消推荐即时保存

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: User Web — Banner 轮播 + 推荐分类

**Files:**
- Modify: `project/user/web/src/api/learn.js`
- Modify: `project/user/web/src/views/LearnIndex.vue`

**Interfaces:**
- Consumes: Task 4 的 user banner API + 分类树 `isRecommended` 字段
- Produces: 空状态页 banner 轮播 + 推荐分类卡片

- [ ] **Step 1: 修改 `api/learn.js` — 加 banner API**

在文件末尾加：

```js
/**
 * 创作学院 - Banner 列表。
 * @returns {Promise<{code:number, data:Array, message:string}>}
 */
export function fetchBanners() {
  return request.get('/learn/banner')
}
```

- [ ] **Step 2: 修改 `LearnIndex.vue` — 模板：空状态页重新设计**

把 `<main class="learn-main">` 内的 `<LearnContent>` 调用替换为：

```vue
      <main class="learn-main">
        <!-- 空状态页：banner + 推荐分类 -->
        <template v-if="isEmptyState">
          <!-- Banner 轮播 -->
          <div v-if="banners.length" class="learn-banner-section">
            <a-carousel autoplay :dots="true" dot-position="bottom" class="learn-banner-carousel">
              <div v-for="b in banners" :key="b.id" class="learn-banner-slide">
                <a v-if="b.linkUrl" :href="b.linkUrl" target="_blank" rel="noopener">
                  <img :src="b.imageUrl" :alt="'banner-' + b.id" class="learn-banner-img" />
                </a>
                <img v-else :src="b.imageUrl" :alt="'banner-' + b.id" class="learn-banner-img" />
              </div>
            </a-carousel>
          </div>

          <!-- 推荐分类 -->
          <div v-if="recommendedCategories.length" class="learn-recommend-section">
            <h2 class="learn-recommend-title">推荐分类</h2>
            <div class="learn-recommend-grid">
              <a
                v-for="cat in recommendedCategories"
                :key="cat.id"
                class="learn-recommend-card"
                @click.prevent="onSelectCategory(cat.id)"
                href="#"
              >
                <component
                  v-if="getCategoryIcon(cat.name)"
                  :is="getCategoryIcon(cat.name)"
                  class="learn-recommend-icon"
                />
                <span class="learn-recommend-name">{{ cat.name }}</span>
              </a>
            </div>
          </div>

          <!-- 兜底空状态 -->
          <div v-if="!banners.length && !recommendedCategories.length" class="learn-content-empty">
            <ReadOutlined class="learn-empty-icon" />
            <div class="learn-empty-title">欢迎来到创作学院</div>
            <div class="learn-empty-subtitle">从左侧选择一个分类开始学习</div>
          </div>
        </template>

        <!-- 非空状态：文章详情 / 分类列表 -->
        <LearnContent
          v-else
          :article="currentArticle"
          :category="currentCategory"
          :current-category-name="currentCategoryName"
          :category-path="currentCategoryPath"
          :top-categories="topCategories"
          @load-article="loadArticle"
          @select-category="onSelectCategory"
        />
      </main>
```

- [ ] **Step 3: 修改 `LearnIndex.vue` — script：新增数据与逻辑**

在 import 区加：

```js
import { fetchBanners } from '@/api/learn'
import { CATEGORY_ICONS } from '@/components/learn/learnCategoryIcons'
import { ReadOutlined } from '@ant-design/icons-vue'
```

在 `topCategories` computed 之后加：

```js
// 是否为空状态页（未选文章、未选分类）
const isEmptyState = computed(() => !route.params.id && !route.query.cat)

// Banner 列表
const banners = ref([])

// 推荐分类（从分类树过滤 isRecommended === 1 的顶级分类）
const recommendedCategories = computed(() =>
  categoryTree.value.filter(c => c.isRecommended === 1)
)

function getCategoryIcon(name) {
  return CATEGORY_ICONS[name] || null
}
```

在 `bootstrap()` 函数的 `try` 块开头加 banner 加载（在 `fetchCategoryTree` 之后）：

```js
  // 加载 banner（仅空状态页需要）
  if (!route.params.id && !route.query.cat) {
    try {
      const bannerRes = await fetchBanners()
      banners.value = bannerRes.data || []
    } catch (e) {
      banners.value = []
    }
  }
```

- [ ] **Step 4: 修改 `LearnIndex.vue` — 样式**

在 `<style scoped>` 中追加：

```css
/* Banner 轮播 */
.learn-banner-section { margin-bottom: 24px; }
.learn-banner-carousel { border-radius: 12px; overflow: hidden; }
.learn-banner-slide { height: 280px; }
.learn-banner-img { width: 100%; height: 280px; object-fit: cover; display: block; }

/* 推荐分类 */
.learn-recommend-section { margin-bottom: 24px; }
.learn-recommend-title {
  font-size: 16px; font-weight: 600; color: #1a1a1a; margin: 0 0 12px;
}
.learn-recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}
.learn-recommend-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
  text-decoration: none;
  color: #1a1a1a;
  cursor: pointer;
  transition: all 0.2s ease;
}
.learn-recommend-card:hover {
  border-color: #FF2442;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08);
}
.learn-recommend-icon { font-size: 32px; color: #FF2442; margin-bottom: 8px; }
.learn-recommend-name { font-size: 14px; font-weight: 600; }

@media (max-width: 991px) {
  .learn-banner-slide,
  .learn-banner-img { height: 160px; }
}
```

- [ ] **Step 5: 截图验证**

确保 admin-api 已通过 Task 5 的页面录入至少 1 个 banner，且至少 1 个分类标记为推荐。

```bash
python3 -c "
from playwright.sync_api import sync_playwright
import time
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={'width':1440,'height':900})
    pg.goto('http://localhost:22345/learn')
    time.sleep(2.0)
    pg.screenshot(path='tests/e2e/screenshots/task7-banner-recommend.png', full_page=True)
    b.close()
    print('OK')
"
```

预期：截图中空状态页显示 banner 轮播 + 推荐分类卡片。

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/api/learn.js \
        project/user/web/src/views/LearnIndex.vue
git commit -m "feat(user-web): 创作学院空状态页 banner 轮播 + 推荐分类

- Banner 轮播：a-carousel 自动轮播，支持跳转链接
- 推荐分类：图标 + 名称卡片网格，点击跳分类列表
- 无 banner 无推荐时保留兜底空状态

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 8: e2e 验证脚本

**Files:**
- Create: `tests/e2e/learn_banner.py`

**Interfaces:**
- Consumes: 运行中的 admin-api（25051）、user-api（25050）、user-web（22345）

- [ ] **Step 1: 编写脚本**

```python
#!/usr/bin/env python3
"""用户端 - 创作学院 banner + 推荐分类端到端验证。

前置条件：
- admin-api 启动（25051）
- user-api 启动（25050）
- user-web dev 启动（22345）
- 已通过管理端录入至少 1 个 banner、至少 1 个推荐分类
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn_banner"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ===== Desktop =====
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 1. 空状态页：banner 轮播
        page.goto(f"{USER_URL}/learn")
        time.sleep(2.0)

        banner_section = page.locator('.learn-banner-section')
        if banner_section.count() > 0:
            expect(banner_section).to_be_visible()
            imgs = page.locator('.learn-banner-img')
            assert imgs.count() > 0, "banner section should have images"
            page.screenshot(path=SCREENSHOTS_DIR / "01-banner.png", full_page=True)
            print(f"PASS: banner carousel visible with {imgs.count()} images")
        else:
            print("WARN: no banner section (may need to add banners via admin)")

        # 2. 推荐分类
        recommend_section = page.locator('.learn-recommend-section')
        if recommend_section.count() > 0:
            expect(recommend_section).to_be_visible()
            cards = page.locator('.learn-recommend-card')
            assert cards.count() > 0, "recommend section should have cards"
            # 每个卡片应有图标（svg）和名称
            first = cards.first
            assert first.locator('svg').count() > 0, "recommend card should have icon"
            assert first.locator('.learn-recommend-name').inner_text().strip(), "card should have name"
            page.screenshot(path=SCREENSHOTS_DIR / "02-recommend.png", full_page=True)
            print(f"PASS: recommend section visible with {cards.count()} cards")

            # 3. 点击推荐分类卡片 → 跳转分类列表
            first.click()
            time.sleep(1.5)
            assert 'cat=' in page.url, f"should navigate to category, got {page.url}"
            page.screenshot(path=SCREENSHOTS_DIR / "03-category-nav.png", full_page=True)
            print("PASS: recommend card click navigates to category")
        else:
            print("WARN: no recommend section (may need to mark categories as recommended)")

        # 4. 兜底空状态（无 banner 无推荐时）
        # 如果有 banner 或推荐，兜底不显示——跳过

        ctx.close()

        # ===== Mobile =====
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        page2.goto(f"{USER_URL}/learn")
        time.sleep(2.0)
        page2.screenshot(path=SCREENSHOTS_DIR / "04-mobile.png", full_page=True)
        # banner 图片高度应为 160px
        if page2.locator('.learn-banner-img').count() > 0:
            box = page2.locator('.learn-banner-img').first.bounding_box()
            assert box['height'] <= 170, f"mobile banner height should be ~160px, got {box['height']}"
            print("PASS: mobile banner height correct")
        ctx2.close()

        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)
```

- [ ] **Step 2: 运行脚本**

确保 admin-api、user-api、user-web 都已启动，且已通过管理端录入至少 1 个 banner 和 1 个推荐分类。

```bash
python3 tests/e2e/learn_banner.py
```

预期：输出 PASS/WARN 信息，截图保存到 `tests/e2e/screenshots/learn_banner/`。

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/learn_banner.py
git commit -m "test(e2e): 创作学院 banner + 推荐分类验证脚本

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 自检结果

- **Spec 覆盖**：
  - `t_learn_banner` 建表 → Task 1
  - `t_article_category` 加 `is_recommended` → Task 1
  - Admin banner CRUD（entity/mapper/DTO/VO/service/controller/错误码） → Task 2
  - Admin 分类推荐字段（entity/req/VO/service） → Task 3
  - User banner 只读（entity/mapper/VO/service/controller） → Task 4
  - User 分类树推荐标记 → Task 4
  - Admin banner 管理页 → Task 5
  - Admin 分类推荐开关 → Task 6
  - User 端 banner 轮播 + 推荐分类 → Task 7
  - e2e → Task 8
- **占位符扫描**：无 TBD/TODO，所有代码块完整。
- **类型一致性**：
  - `LearnBannerReq`（`imageUrl/linkUrl/sort`）在 Task 2 定义，Task 5 前端 form 字段一致
  - `LearnBannerVO`（admin: `id/imageUrl/linkUrl/sort/createdAt/updatedAt`；user: `id/imageUrl/linkUrl`）在 Task 2/4 分别定义
  - `isRecommended` 在 Task 3（admin entity/req/VO）和 Task 4（user entity/VO）命名一致
  - `LearnBannerService.list()/create()/update()/delete()` 在 Task 2 定义，Task 2 controller 调用一致
  - `LearnBrowseService.banners()` 在 Task 4 定义，Task 4 controller 调用一致

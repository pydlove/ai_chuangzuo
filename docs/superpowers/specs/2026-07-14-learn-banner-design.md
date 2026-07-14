# 创作学院 Banner + 推荐分类 设计

**日期**: 2026-07-14
**状态**: 已确认，待实现
**关联文件**:
- `project/admin/api/.../modules/learn/`（banner CRUD + 分类推荐字段）
- `project/user/api/.../modules/learn/`（banner 只读 + 分类树加推荐标记）
- `project/admin/web/src/views/`（banner 管理页）
- `project/admin/web/src/components/learn/CategoryTreeEditor.vue`（推荐开关）
- `project/user/web/src/views/LearnIndex.vue`（空状态页重新设计）

---

## 1. 功能概述

为创作学院用户端空状态页（`/learn` 未选分类时）增加两个能力：

1. **Banner 轮播**——管理端配置图片 URL + 可选跳转链接，用户端自动轮播
2. **推荐分类**——管理端分类管理加「是否推荐」开关，推荐分类以图标卡片形式展示在欢迎页

**明确不做**：不做图片上传（只填 URL）、不做 banner 生效时间、不做点击统计、不做暗色主题。

---

## 2. 数据库变更

### 2.1 新建 `t_learn_banner`

Flyway 迁移：`V2.0.0_025__create_learn_banner_and_category_recommended.sql`

```sql
CREATE TABLE IF NOT EXISTS t_learn_banner (
    id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    image_url     VARCHAR(512)     NOT NULL                COMMENT '图片 URL',
    link_url      VARCHAR(512)     NOT NULL DEFAULT ''     COMMENT '点击跳转链接',
    sort          INT              NOT NULL DEFAULT 0      COMMENT '排序权重，小在前',
    is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    updated_by    BIGINT UNSIGNED  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作学院 banner';
```

### 2.2 `t_article_category` 加字段

```sql
ALTER TABLE t_article_category
    ADD COLUMN is_recommended TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否推荐 0=否 1=是' AFTER sort;
```

---

## 3. 后端 API

### 3.1 Admin 端

复用 `LearnAdminController`（`/api/v1/admin/learn`），新增 4 个端点：

| 方法 | 路径 | 请求体 | 响应 | 说明 |
|---|---|---|---|---|
| GET | `/banner` | - | `Result<List<LearnBannerVO>>` | banner 列表，按 sort ASC |
| POST | `/banner` | `LearnBannerReq` | `Result<LearnBannerVO>` | 新增 banner |
| PUT | `/banner/{id}` | `LearnBannerReq` | `Result<LearnBannerVO>` | 编辑 banner |
| DELETE | `/banner/{id}` | - | `Result<Void>` | 删除 banner |

**`LearnBannerReq`**：
```java
@Data
public class LearnBannerReq {
    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 512)
    private String imageUrl;

    @Size(max = 512)
    private String linkUrl;

    @NotNull
    private Integer sort;
}
```

**`LearnBannerVO`**：
```java
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

**Entity**：`LearnBannerEntity extends BaseEntity`，`@TableName("t_learn_banner")`。

**Service**：`LearnBannerService` 接口 + `LearnBannerServiceImpl`，标准 CRUD。

**错误码**：`LearnErrorCode` 新增 `BANNER_NOT_FOUND(270007, "Banner 不存在")`。

**分类推荐**：`LearnCategoryReq` 加 `private Integer isRecommended;`（0/1），`LearnCategoryServiceImpl.update()` 中处理。

### 3.2 User 端

`LearnController`（`/api/v1/user/learn`）新增 1 个端点：

| 方法 | 路径 | 响应 | 说明 |
|---|---|---|---|
| GET | `/banner` | `Result<List<LearnBannerVO>>` | 所有未删除的 banner，按 sort ASC |

**User 端 `LearnBannerVO`**：
```java
@Data
public class LearnBannerVO {
    private Long id;
    private String imageUrl;
    private String linkUrl;
}
```

**分类树 VO**：`LearnCategoryTreeVO` 加 `private Integer isRecommended;`，前端过滤推荐分类。

---

## 4. 管理端页面

### 4.1 Banner 管理页

**路由**：`/console/learn/banner`
**文件**：`project/admin/web/src/views/LearnBannerView.vue`

**布局**：
- 顶部：「新增 Banner」按钮
- 表格列：缩略图（80x45）、图片 URL（省略号）、跳转链接（省略号）、排序、创建时间、操作（编辑/删除）
- 新增/编辑：a-modal 弹框
  - 图片 URL：a-input，必填
  - 跳转链接：a-input，选填
  - 排序：a-input-number，默认 0

**侧边栏**：`AdminLayout.vue` 的「创作学院」sub-menu 下新增「Banner 管理」菜单项。

**API 客户端**：`project/admin/web/src/api/learn.js` 新增：
```js
export const fetchBanners = () => request.get('/api/v1/admin/learn/banner').then(r => r.data)
export const createBanner = (data) => request.post('/api/v1/admin/learn/banner', data).then(r => r.data)
export const updateBanner = (id, data) => request.put(`/api/v1/admin/learn/banner/${id}`, data).then(r => r.data)
export const deleteBanner = (id) => request.delete(`/api/v1/admin/learn/banner/${id}`).then(r => r.data)
```

### 4.2 分类管理推荐开关

**文件**：`project/admin/web/src/components/learn/CategoryTreeEditor.vue`

在每个分类节点的操作区加一个 `a-switch`（推荐/不推荐），切换时调 `updateCategory(id, { isRecommended })`。

---

## 5. 用户端页面

### 5.1 空状态页重新设计

**文件**：`project/user/web/src/views/LearnIndex.vue`

**布局**（从上到下）：

1. **Banner 轮播区**（有 banner 时显示）：
   - `a-carousel` 组件，`autoplay`，`dot-position="bottom"`
   - 高度 280px，圆角 12px，`overflow: hidden`
   - 每张 banner：`img` 全宽填充（`object-fit: cover`）
   - 有 `linkUrl` 的 banner 包裹 `<a>` 标签可点击跳转
   - 移动端高度 160px

2. **推荐分类区**（有推荐分类时显示）：
   - 标题：「推荐分类」，14px / 600 / `#1a1a1a`
   - 卡片网格：`display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 12px;`
   - 每张卡片：白底 + `1px solid #eee` + `radius-lg (8px)` + padding `16px` + 居中
     - 图标：复用 `CATEGORY_ICONS` 映射，32px / `#FF2442`
     - 名称：14px / 600 / `#1a1a1a`，margin-top `8px`
   - hover：`border-color: #FF2442` + `box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08)`
   - 点击跳 `/learn?cat=<id>`

3. **空状态兜底**（无 banner 且无推荐分类时）：
   - 保留现有 `ReadOutlined` + 欢迎文案

**API 客户端**：`project/user/web/src/api/learn.js` 新增：
```js
export const fetchBanners = () => request.get('/learn/banner').then(r => r.data)
```

**`LearnIndex.vue` 新增**：
- `banners` ref：空状态页加载 banner 列表
- `recommendedCategories` computed：从 `categoryTree` 过滤 `isRecommended === 1` 的顶级分类
- 空状态页模板替换为 banner + 推荐分类 + 兜底

### 5.2 空状态页布局

空状态页（`!route.params.id && !route.query.cat`）的主内容区（`.learn-main`）从上到下：

1. **Banner 轮播**（有 banner 时显示）
2. **推荐分类**（有推荐分类时显示）
3. **兜底空状态**（两者都没有时显示现有 ReadOutlined + 欢迎文案）

Hero 区（`创作学院` 标题 + 副标题）在所有 `/learn` 页面保留，作为页面统一头部。文章详情页和分类列表页的主内容区不受影响。

---

## 6. 文件变更清单

| 操作 | 路径 | 变更 |
|---|---|---|
| 新建 | `project/admin/api/.../db/migration/V2.0.0_025__create_learn_banner_and_category_recommended.sql` | 建 banner 表 + 分类加推荐字段 |
| 新建 | `project/admin/api/.../modules/learn/entity/LearnBannerEntity.java` | banner 实体 |
| 新建 | `project/admin/api/.../modules/learn/mapper/LearnBannerMapper.java` | banner mapper |
| 新建 | `project/admin/api/.../modules/learn/dto/request/LearnBannerReq.java` | banner 请求 DTO |
| 新建 | `project/admin/api/.../modules/learn/vo/LearnBannerVO.java` | banner VO |
| 新建 | `project/admin/api/.../modules/learn/service/LearnBannerService.java` | banner service 接口 |
| 新建 | `project/admin/api/.../modules/learn/service/impl/LearnBannerServiceImpl.java` | banner service 实现 |
| 修改 | `project/admin/api/.../modules/learn/controller/LearnAdminController.java` | 新增 banner CRUD 端点 |
| 修改 | `project/admin/api/.../modules/learn/dto/request/LearnCategoryReq.java` | 加 isRecommended |
| 修改 | `project/admin/api/.../modules/learn/exception/LearnErrorCode.java` | 加 BANNER_NOT_FOUND |
| 新建 | `project/user/api/.../modules/learn/entity/LearnBannerEntity.java` | user 端 banner 只读实体 |
| 新建 | `project/user/api/.../modules/learn/mapper/LearnBannerMapper.java` | user 端 banner mapper |
| 新建 | `project/user/api/.../modules/learn/vo/LearnBannerVO.java` | user 端 banner VO |
| 修改 | `project/user/api/.../modules/learn/controller/LearnController.java` | 新增 banner 端点 |
| 修改 | `project/user/api/.../modules/learn/service/LearnBrowseService.java` | 加 banners() 方法 |
| 修改 | `project/user/api/.../modules/learn/service/impl/LearnBrowseServiceImpl.java` | 实现 banners() |
| 修改 | `project/user/api/.../modules/learn/vo/LearnCategoryTreeVO.java` | 加 isRecommended |
| 新建 | `project/admin/web/src/views/LearnBannerView.vue` | banner 管理页 |
| 修改 | `project/admin/web/src/api/learn.js` | 加 banner API |
| 修改 | `project/admin/web/src/router/index.js` | 加 banner 路由 |
| 修改 | `project/admin/web/src/layouts/AdminLayout.vue` | 加 banner 菜单 |
| 修改 | `project/admin/web/src/components/learn/CategoryTreeEditor.vue` | 加推荐开关 |
| 修改 | `project/user/web/src/api/learn.js` | 加 banner API |
| 修改 | `project/user/web/src/views/LearnIndex.vue` | 空状态页重新设计 |

---

## 7. 测试

### 7.1 后端测试

- `LearnBannerServiceImplTest`（admin）：CRUD 正常路径 + banner 不存在异常
- 复用现有 `@SpringBootTest + @Transactional + @Rollback` 模式

### 7.2 e2e 验证

`tests/e2e/learn_banner.py`：
1. Admin 新增 banner → 用户端空状态页显示轮播
2. Admin 标记分类为推荐 → 用户端显示推荐分类卡片
3. 点击推荐分类卡片 → 跳转分类列表
4. 点击有链接的 banner → 跳转正确
5. 无 banner 无推荐 → 显示兜底空状态

---

## 8. 提交拆分

| Commit | 范围 |
|---|---|
| `feat(admin-api): 创作学院 banner 表 + 分类推荐字段迁移` | Flyway SQL |
| `feat(admin-api): 创作学院 banner CRUD 接口` | admin entity/mapper/service/controller/DTO/VO/错误码 |
| `feat(admin-api): 创作学院分类加推荐字段` | LearnCategoryReq + LearnCategoryServiceImpl |
| `feat(user-api): 创作学院 banner 只读接口 + 分类树加推荐标记` | user entity/mapper/VO/service/controller |
| `feat(admin-web): 创作学院 banner 管理页` | 视图 + 路由 + 侧边栏 + API |
| `feat(admin-web): 创作学院分类管理加推荐开关` | CategoryTreeEditor |
| `feat(user-web): 创作学院空状态页 banner 轮播 + 推荐分类` | LearnIndex.vue + API |
| `test(e2e): 创作学院 banner + 推荐分类验证` | e2e 脚本 |

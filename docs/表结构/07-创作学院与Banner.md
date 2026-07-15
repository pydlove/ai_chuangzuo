# 创作学院与首页 Banner

本域共 4 张表，覆盖创作学院的文章、分类、Banner，以及 C 端首页 Banner。

## t_article — 学院文章表

**用途**：存储创作学院的文章内容，由管理端维护、用户端只读，支持 Markdown 与富文本两种正文格式。

| 字段 | 类型 | 允许空 | 默认值 | 含义 |
| --- | --- | --- | --- | --- |
| id | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| category_id | BIGINT UNSIGNED | 否 | — | 所属分类 id，关联 `t_article_category.id` |
| title | VARCHAR(128) | 否 | — | 文章标题 |
| summary | VARCHAR(255) | 是 | NULL | 文章摘要 |
| cover_image_url | VARCHAR(512) | 否 | '' | 封面图 URL |
| content_type | VARCHAR(16) | 否 | — | 正文类型：`markdown`=Markdown；`rich_text`=富文本 |
| content | LONGTEXT | 否 | — | 正文内容 |
| status | VARCHAR(16) | 否 | — | 文章状态：`draft`=草稿；`published`=已发布 |
| sort | INT | 否 | 0 | 排序权重，升序展示 |
| author_id | BIGINT UNSIGNED | 是 | NULL | 作者 id（管理端用户 id） |
| published_at | DATETIME(3) | 是 | NULL | 发布时间 |
| is_deleted | TINYINT UNSIGNED | 否 | 0 | 是否删除：0=否，1=是（逻辑删除） |
| created_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| updated_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |
| created_by | BIGINT UNSIGNED | 否 | 0 | 创建人 id |
| updated_by | BIGINT UNSIGNED | 否 | 0 | 更新人 id |

**索引**：
- PRIMARY KEY (`id`)
- INDEX `idx_cat_status_deleted` (`category_id`, `status`, `is_deleted`)
- INDEX `idx_sort` (`sort`)
- INDEX `idx_status_deleted` (`status`, `is_deleted`)

**说明**：
- `category_id` 关联 `t_article_category.id`，文章挂在某个分类下。
- `content_type` 决定 `content` 字段的解析方式，由前端按 Markdown 或富文本渲染。
- `status` 仅 `published` 状态的文章对用户端可见；`draft` 仅管理端可见。
- 软删除通过 `is_deleted` 实现，MyBatis-Plus `@TableLogic` 自动过滤。

## t_article_category — 学院分类表

**用途**：创作学院文章的分类，支持二级（父子）结构，管理端可标记"推荐分类"用于用户端首页/侧边栏展示。

| 字段 | 类型 | 允许空 | 默认值 | 含义 |
| --- | --- | --- | --- | --- |
| id | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| parent_id | BIGINT UNSIGNED | 是 | NULL | 父分类 id；NULL 表示顶级分类 |
| name | VARCHAR(64) | 否 | — | 分类名 |
| sort | INT | 否 | 0 | 排序值，升序展示 |
| is_recommended | TINYINT UNSIGNED | 否 | 0 | 是否推荐：0=否，1=是 |
| is_deleted | TINYINT UNSIGNED | 否 | 0 | 是否删除：0=否，1=是（逻辑删除） |
| created_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| updated_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |
| created_by | BIGINT UNSIGNED | 否 | 0 | 创建人 id |
| updated_by | BIGINT UNSIGNED | 否 | 0 | 更新人 id |

**索引**：
- PRIMARY KEY (`id`)
- INDEX `idx_parent_deleted` (`parent_id`, `is_deleted`)
- INDEX `idx_sort` (`sort`)

**说明**：
- `parent_id` 自关联 `t_article_category.id`，构成两级分类树；NULL 为顶级。
- `is_recommended=1` 的分类会在用户端创作学院首页推荐位展示。
- 被 `t_article.category_id` 引用；删除分类前需先处理其下文章。

## t_learn_banner — 学院 Banner 表

**用途**：创作学院页面顶部轮播 Banner，由管理端配置，用户端按排序读取展示。

| 字段 | 类型 | 允许空 | 默认值 | 含义 |
| --- | --- | --- | --- | --- |
| id | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| image_url | VARCHAR(512) | 否 | — | 图片 URL |
| link_url | VARCHAR(512) | 否 | '' | 点击跳转链接；空串表示不跳转 |
| sort | INT | 否 | 0 | 排序权重，小在前 |
| is_deleted | TINYINT UNSIGNED | 否 | 0 | 是否删除：0=否，1=是（逻辑删除） |
| created_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| updated_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |
| created_by | BIGINT UNSIGNED | 否 | 0 | 创建人 id |
| updated_by | BIGINT UNSIGNED | 否 | 0 | 更新人 id |

**索引**：
- PRIMARY KEY (`id`)
- INDEX `idx_sort` (`sort`)

**说明**：
- 仅用于创作学院模块的 Banner 位，与 C 端首页 Banner（`a_home_banner`）相互独立。
- `link_url` 既可以是站内路径，也可以是站外 URL，由前端按字符串处理。

## a_home_banner — 首页 Banner 表

**用途**：C 端首页顶部轮播 Banner，由管理端配置。

| 字段 | 类型 | 允许空 | 默认值 | 含义 |
| --- | --- | --- | --- | --- |
| id | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键 |
| image_url | VARCHAR(512) | 否 | — | 图片 URL |
| link_url | VARCHAR(512) | 否 | '' | 点击跳转链接；空串表示不跳转 |
| sort | INT | 否 | 0 | 排序权重，小在前 |
| is_deleted | TINYINT UNSIGNED | 否 | 0 | 是否删除：0=否，1=是（逻辑删除） |
| created_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) | 创建时间 |
| updated_at | DATETIME(3) | 否 | CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) | 更新时间 |
| created_by | BIGINT UNSIGNED | 否 | 0 | 创建人 id |
| updated_by | BIGINT UNSIGNED | 否 | 0 | 更新人 id |

**索引**：
- PRIMARY KEY (`id`)
- INDEX `idx_a_home_banner_sort` (`sort`)

**说明**：
- 表前缀为 `a_` 是因为该 Banner 完全由管理端配置，但服务于 C 端首页展示，用户端通过只读接口获取。
- 与 `t_learn_banner` 字段结构一致，但用途不同：本表用于 App/H5 首页，`t_learn_banner` 用于创作学院页。

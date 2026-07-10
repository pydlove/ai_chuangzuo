# 创作学院模块设计（2026-07-10）

## 一、目标与范围

为用户端新增一个文档式浏览页「创作学院」，路径 `/learn`。管理员可在管理端自由定义分类树、录入和发布文章；用户端按文档站形态浏览已发布的文章。一次性闭环交付：后端 + 管理端 + 用户端。

不在本期范围内：i18n 国际化、文章版本回滚、性能压测、CMS 富文本清洗（DOMPurify）。

## 二、命名与位置

- 用户端页面：**创作学院**，路径 `/learn`
- 用户端入口：在现有 NavBar 上加一项「创作学院」，与「首页」「会员」「玩法指南」并列
- 管理端一级菜单：「创作学院」；下挂两个子菜单：「分类管理」「文章管理」

## 三、数据模型

新增两张表，使用现有命名约定（snake_case、`id` 主键、`created_at`/`updated_at`/`deleted`，Flyway 迁移）。

### 3.1 `article_category`（文章分类）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint AUTO_INCREMENT PK | 主键 |
| parent_id | bigint NULL | 父分类 id，null 表示一级 |
| name | varchar(64) NOT NULL | 分类名 |
| sort | int NOT NULL DEFAULT 0 | 排序值，升序展示 |
| created_at | datetime NOT NULL | |
| updated_at | datetime NOT NULL | |
| deleted | bit NOT NULL DEFAULT 0 | 软删除 |

索引：`(parent_id, deleted)`、`(sort)`

### 3.2 `article`（文章）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint AUTO_INCREMENT PK | 主键 |
| category_id | bigint NOT NULL | 所属分类 |
| title | varchar(128) NOT NULL | 文章标题 |
| summary | varchar(255) NULL | 列表展示摘要 |
| content_type | varchar(16) NOT NULL | `markdown` 或 `rich_text` |
| content | longtext NOT NULL | Markdown 原文 / 富文本 HTML |
| status | varchar(16) NOT NULL | `draft` 或 `published` |
| sort | int NOT NULL DEFAULT 0 | 同分类下排序 |
| author_id | bigint NULL | 创建者管理用户 id，可空 |
| published_at | datetime NULL | 最近一次发布时间 |
| created_at | datetime NOT NULL | |
| updated_at | datetime NOT NULL | |
| deleted | bit NOT NULL DEFAULT 0 | 软删除 |

索引：`(category_id, status, deleted)`、`(sort)`、`(status, deleted)`

### 3.3 关键约定

- Markdown 内容保存原文（不预渲染），确保再编辑不丢信息
- 富文本存储 Tiptap 输出的 HTML；JSON 备选方案不在本期
- `published_at` 仅在「草稿→发布」时写入；纯编辑已发布文章不改这个字段
- 删除使用软删除；用户端永远只查 `deleted=0 AND status='published'`
- 树形深度不限（一期），二期再视情况加限制

## 四、API 接口

统一走项目现有约定：`{code, message, data}` 响应包装 + 标准错误码。

### 4.1 管理端（需管理员 JWT，路径前缀 `/admin/learn`）

**分类管理**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/learn/category/tree` | 完整分类树 |
| POST | `/admin/learn/category` | body: `{parentId, name, sort}` |
| PUT | `/admin/learn/category/{id}` | body: `{name, sort, parentId}` |
| DELETE | `/admin/learn/category/{id}` | 子分类或文章非空时拒绝 |

**文章管理**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/learn/article/page` | 分页；query: `categoryId?, status?, keyword?, page, size` |
| GET | `/admin/learn/article/{id}` | 含 content |
| POST | `/admin/learn/article` | body: `{categoryId, title, summary, contentType, content, status, sort}` |
| PUT | `/admin/learn/article/{id}` | 同上 body |
| DELETE | `/admin/learn/article/{id}` | 软删除 |
| POST | `/admin/learn/article/{id}/publish` | 草稿→已发布，首次写 `published_at` |
| POST | `/admin/learn/article/{id}/unpublish` | 已发布→草稿，保留 `published_at` |
| POST | `/admin/learn/article/{id}/move` | body: `{categoryId}` |
| POST | `/admin/learn/article/sort` | body: `[{id, sort}, ...]` |
| POST | `/admin/learn/category/sort` | 拖拽改顺序；body: `[{id, sort, parentId}, ...]` |

### 4.2 用户端（公开，路径前缀 `/learn`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/learn/category/tree` | 只返回有已发布文章的分类节点 |
| GET | `/learn/category/{id}` | 分类详情 + 子分类 + 已发布文章列表（支持分页 `page,size`） |
| GET | `/learn/article/{id}` | 详情，仅 `status=published` 可见 |

### 4.3 要点

- 所有用户端接口仅返回 `status='published' AND deleted=0` 数据，草稿等同 404 对外不可见
- 详情接口返回 content（按 `contentType` 渲染）+ `updatedAt` + `categoryId` + 父级链（面包屑）
- 管理端列表用现有 `PageQuery` / `PageResult` 包装
- 删除分类若子节点或文章非空 → 拒绝（错误码 `LEARN_CATEGORY_NOT_EMPTY`）
- 同级分类名重名 → 拒绝（`LEARN_CATEGORY_NAME_DUPLICATE`）

## 五、管理端 UI

沿用现有 `admin/web` 的 Vue 3 + Ant Design Vue。

### 5.1 菜单

一级菜单 **创作学院**，子菜单 **分类管理** / **文章管理**。

### 5.2 分类管理页（左树 + 右表单）

- 左侧树形列表，节点右侧 `+ / 编辑 / 删除` 小图标
- 顶栏「+ 新增顶级分类」按钮
- 节点可拖拽改顺序，调用 `/admin/learn/category/sort` 批量更新
- 右侧编辑表单：分类名、排序值；保存 / 取消
- 删除弹窗显示子节点/文章数量提示

### 5.3 文章列表页（标准表格页）

- 顶部筛选：分类树选择器、状态下拉、关键字输入、时间排序
- 表格列：标题、所属分类、状态、排序、最近更新时间、创建时间、操作
- 操作列：编辑、发布、下线、删除、移动到其他分类
- 右上角「+ 新增文章」

### 5.4 文章编辑器

布局：

```
← 返回列表 │ 新增/编辑文章
─────────────────────────────────
标题                [____________________________]
分类                [树选择器]
摘要                [____________________________]
排序                [___]  状态 [草稿 / 已发布]
─────────────────────────────────
正文类型            ( ) Markdown   ( ) 富文本
[              编辑器主体             ]
─────────────────────────────────
[保存草稿] [保存并发布] [取消]
```

- 草稿状态下可在两种类型间切换（弹确认「会清空当前正文」）
- 已发布文章**不允许切换**正文类型

**编辑器选型**：
- Markdown: `mavon-editor`
- 富文本: `@tiptap/vue-3` + `StarterKit` + `Image`

**保存语义**：
- 「保存草稿」→ status=draft
- 「保存并发布」→ status=published，首次发布写 `published_at`
- 默认不刷新 `published_at`；如需刷新提供勾选项「更新发布时间」
- `beforeRouteLeave` 拦截 dirty 编辑，给「保存为草稿」提示

### 5.5 兜底

- 保存按钮防抖 1 秒
- 删除前 ID 前缀二次确认

## 六、用户端 UI（`/learn`）

### 6.1 PC 布局（≥992px）

```
NavBar（已有，加「创作学院」）
────────────────────────────────────
┌──────────┬───────────────────────┐
│ 分类树   │ 面包屑 > 标题 > 摘要   │
│ 240px    │ 时间                  │
│ sticky   │ ─────                 │
│          │ 正文（Markdown/HTML） │
└──────────┴───────────────────────┘
Footer（已有）
```

### 6.2 移动布局（<992px）

- 顶栏下加「分类」按钮 → 底部抽屉显示树
- 内容区全宽，正文字号略放大

### 6.3 交互

- 树数据源：`GET /learn/category/tree`
- 选中节点高亮色 = 品牌绿 `#07c160`，二级缩进 16px、三级 32px
- URL 同步：`/learn?cat=12`、`/learn/article/:id`
- 树为空（无已发布文章）：占位文案「内容正在筹备中…」
- 正文渲染：
  - `contentType='markdown'` → `markdown-it`（**关闭 `html` 选项**禁用 raw HTML）+ `highlight.js`
  - `contentType='rich_text'` → `v-html`
- 文末：上一篇 / 下一篇（同分类内）
- 文末 CTA：「想把自己的账号也做成这样？立即开始创作 →」→ `/login`

### 6.4 组件结构

- `views/LearnIndex.vue`
- `components/learn/LearnSidebar.vue`
- `components/learn/LearnContent.vue`
- `components/learn/MobileTreeSheet.vue`
- `api/learn.js`

## 七、错误处理与边界

### 7.1 错误码

| 场景 | 错误码 |
|---|---|
| 分类下有文章/子分类 | `LEARN_CATEGORY_NOT_EMPTY` |
| 同级分类名重名 | `LEARN_CATEGORY_NAME_DUPLICATE` |
| 正文超 200 KB（Markdown）/ 500 KB（富文本）| `LEARN_CONTENT_TOO_LARGE` |
| 草稿/发布状态转换 | 走单独接口，幂等返回 200 |

### 7.2 安全

- 富文本 HTML 接收后不解析、不清洗；CSP 与现有项目一致
- Markdown 渲染端 `markdown-it` 配置 `html: false`，禁用 raw HTML
- 代码块用 `highlight.js`，白名单常规语言

### 7.3 一致性

- 分类删除前校验非空，不级联
- 编辑器互改冲突暂不处理（一期不引入乐观锁，按冲突率后期补）

### 7.4 用户端兜底

- API 5xx → 内容区「加载失败，点击重试」
- API 404 → 「该文章已下线或被删除」+「返回学院」CTA
- 移动端抽屉快速连点去重

## 八、测试

### 8.1 后端 Service 单元测试

- 分类树构建（含 sort、父子关系）
- 同级分类名重名校验
- 删除分类有子节点/文章抛业务异常
- 文章分页过滤（分类/状态/关键字）
- 状态转换：`draft → published` 写 `published_at`、`published → draft` 保留 `published_at`
- 状态转换幂等：草稿再点发布不报错、已发布再点发布不刷新时间
- 软删除查询不可见
- Markdown / 富文本两种 `content_type` 存取正确

### 8.2 后端 Controller 接口测试

`@WebMvcTest` 或 `@SpringBootTest`：鉴权、入参校验、状态码；happy path 即可。

### 8.3 管理端 Playwright E2E

`tests/e2e/admin_learn_management.py`：
- 登录 → 分类管理 → 新增顶级分类 → 改名 → 删除
- 一级下加二级分类 → 验证树形
- 文章管理：新增 → 分类 → 写 Markdown → 保存草稿 → 详情校验
- 内容类型切换草稿确认弹窗
- 发布/下线 状态正确
- 删除二次确认
- 移动文章到其他分类，用户端也可见

### 8.4 用户端 Playwright E2E

`tests/e2e/learn_browsing.py`：
- 访问 `/learn` → 树加载 → 点节点 → 内容展示
- 直接访问 `/learn/article/{id}` 内容渲染
- Markdown 渲染（含代码块、表格）
- 富文本渲染
- 移动视口下分类抽屉
- 404 文章路径兜底
- 树为空时占位文案

## 九、实现路径

1. **后端基础**：Flyway 迁移、Entity、Mapper、Service、Controller（管理端 + 公开）
2. **用户端浏览页**：路由、视图、组件、api、NavBar 入口
3. **管理端 UI**：路由、分类管理、列表、编辑器；菜单注入
4. **编辑器接入**：`mavon-editor` + Tiptap；图片上传暂不实现（先做纯文本与表格代码块的渲染，图文混排功能留给二期）
5. **测试 + 验收**：单测、E2E 跑通后更新进度表

### 文件清单

**后端（`project/shared/api` 或 `project/admin/api`，按现有 admin/user 划分规则确定）**
- `db/migration/V{x}__create_learn_tables.sql`
- `entity/{LearnCategory, LearnArticle}.java`
- `dto/learn/{LearnCategoryReq, LearnCategoryTreeNode, LearnArticleReq, LearnArticleDetail, LearnArticlePageQuery}.java`
- `mapper/{LearnCategoryMapper, LearnArticleMapper}.java`
- `service/{LearnCategoryService, LearnArticleService}.java` + Impl
- `controller/admin/LearnAdminController.java`
- `controller/learn/LearnPublicController.java`

**用户端（`project/user/web`）**
- `src/router/index.js`
- `src/views/LearnIndex.vue`
- `src/components/learn/{LearnSidebar, LearnContent, MobileTreeSheet}.vue`
- `src/api/learn.js`
- `src/components/layout/NavBar.vue`（加「创作学院」链接）

**管理端（`project/admin/web`）**
- `src/router/index.js`
- `src/views/learn/{CategoryManage, ArticleList, ArticleEditor}.vue`
- `src/api/learn.js`
- `src/components/learn/{CategoryTreeEditor}.vue`
- `src/components/learn/{MarkdownEditor, RichTextEditor}.vue`
- 菜单配置文件

**测试**
- 后端：`service/impl/LearnServiceImplTest.java` 等
- E2E：`tests/e2e/{admin_learn_management, learn_browsing}.py`

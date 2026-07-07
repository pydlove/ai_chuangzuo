# 作品页搜索 + 筛选设计

## 背景

`WorksIndex.vue`（我的作品页）需要支持按标题关键词、平台、风格、时间范围搜索和筛选，并在「已生成」与「草稿箱」两个 tab 之间共享筛选条件。

## 目标

- 在「我的作品」标题行增加搜索框和筛选条件。
- 两个 tab（已生成 / 草稿箱）都受同一组条件过滤。
- 筛选为纯前端实现，基于 `localStorage` 中的队列和草稿数据。
- 接入「已生成」tab 的真实数据源，使其不再是空模拟数据。

## 页面结构

```
[我的作品]  [搜索框 🔍]  [平台 ▾]  [风格 ▾]  [时间 ▾]          [已生成 | 草稿箱]
```

- 搜索框：`a-input-search`，placeholder 为「搜索标题关键词」，实时过滤。
- 平台筛选：`a-select mode="multiple"`，选项来自创作页的平台列表。
- 风格筛选：`a-select mode="multiple"`，选项来自系统预设风格列表。
- 时间筛选：`a-radio-group`，选项为「全部 / 近7天 / 近30天 / 近90天」。
- Tabs 保持在右侧不变。

## 数据接入

### 已生成

从 `localStorage` 的 `aichuangzuo_generation_queue` 读取 `status === 'completed'` 的项，字段与 `QueueIndex.vue` 的 `loadWorks` 保持一致：

- `id`
- `title`
- `platform`（字符串，如「公众号」）
- `wordCount`
- `style`（字符串，如「职场干货」）
- `template`
- `completedAt`（ISO 字符串）
- `content`

### 草稿箱

保持从 `aichuangzuo_drafts` 读取，字段结构为：

- `id`
- `customTitle`（作为标题）
- `platform`（对象，含 `name`）
- `wordCount`
- `style`（对象，含 `name`）
- `savedAt`（ISO 字符串）

## 数据标准化

渲染前将两类数据统一为：

```js
{
  id,
  title,
  platformName,   // 字符串
  styleName,      // 字符串
  date,           // Date 对象
  raw             // 原始数据，保留用于后续操作
}
```

## 筛选逻辑

所有筛选条件使用 `ref` 维护，在 tab 切换时保留：

- `searchKeyword`：标题忽略大小写包含。
- `selectedPlatforms`：多选，满足任一即命中。
- `selectedStyles`：多选，满足任一即命中。
- `timeRange`：「全部」不过滤；其余选项计算 `date` 与当前时间的天数差。

## 空状态

- 无数据：保持现有「还没有生成的文章」/「草稿箱是空的」提示。
- 筛选后无结果：新增「未找到匹配的作品」提示，并提供「清空筛选」按钮一键重置所有条件。

## 依赖

- 复用项目中已引入的 Ant Design Vue 组件：`a-input-search`、`a-select`、`a-radio-group`。
- 平台选项和风格选项与 `CreateIndex.vue` 中的定义对齐，避免硬编码。

## 测试

新增 Playwright 脚本 `tests/e2e/verify_works_search_filter.py`：

1. 预置一条已完成的生成队列数据和一条草稿数据。
2. 访问 `/console/works`。
3. 验证标题关键词搜索、平台多选筛选、时间范围筛选后结果正确。
4. 验证「清空筛选」按钮能恢复全部结果。

## 后续工作

本设计通过后将进入 `writing-plans` 阶段，输出具体实现步骤。

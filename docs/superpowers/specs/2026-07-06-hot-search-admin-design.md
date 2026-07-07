# 管理端热搜管理设计

## 背景

管理端已有热搜抓取与定时任务（admin-api 端，每天 02:00 跑），但缺少后台管理界面与 CRUD 接口。管理控制台需要「热度榜」顶级菜单，下设三个子页：平台管理、今日榜单、抓取配置。同步时间（cron）目前硬编码在 `application.yml`，需要迁移到 DB 支持后台修改后立即生效。

## 目标

- **平台 CRUD**：增删改查热搜平台（编码、名称、图标、排序、启停），含"启用平台被引用则禁止删除"约束。
- **每日榜单 CRUD**：按平台+日期过滤分页查询，支持手动新增/编辑/删除条目（rank、标题、热度值、URL、搜索量、快照日期）。
- **抓取配置 CRUD**：cron 表达式、抓取总开关、topN、连接/读取超时时间，落库后立即生效。
- **手动触发**：一键同步抓取所有启用平台，返回每平台结果。
- **上次抓取摘要**：抓取时间、各平台成功/失败条数。

## 非目标

- 不引入新抓取器或修改现有抓取实现。
- 不修改用户端只读接口。
- 不实现多租户/权限细分（仅 SUPER_ADMIN）。

## 数据库

### 新增 `hot_search_config`（单行配置）

迁移 `V2.0.0_003__create_hot_search_config_table.sql`：

```sql
CREATE TABLE IF NOT EXISTS hot_search_config (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    cron VARCHAR(64) NOT NULL,
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1,
    top_n INT NOT NULL DEFAULT 50,
    connect_timeout_millis INT NOT NULL DEFAULT 5000,
    read_timeout_millis INT NOT NULL DEFAULT 10000,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热搜抓取配置';
```

启动时若 `id=1` 不存在则插入默认值（cron=`0 0 2 * * ?`，enabled=1，top_n=50）。

### 复用现有表

- `hot_search_platform`（5 字段 + 审计）— 平台 CRUD 直接操作。
- `hot_search_daily`（含唯一键 `platform_code+snapshot_date+rank_num`）— 榜单 CRUD 直接操作。

## 架构

### 后端改造

- **新增** `HotSearchConfig` 实体、Mapper、Service。
- **改造** `HotSearchCrawlJob`：
  - 去掉 `@Scheduled(cron=...)`，改注入 `ThreadPoolTaskScheduler`。
  - 启动时按 DB 配置注册 cron Trigger。
  - 新增 `reschedule()` 方法：先 cancel 旧 Trigger，再按当前 DB cron 注册新 Trigger。
- **保留** `HotSearchProperties`（yml）作为启动默认值；启动同步逻辑：若 DB 有记录则用 DB 值覆盖 yml。
- **新增** `HotSearchPlatformAdminService` + `HotSearchDailyAdminService` + `HotSearchConfigService`。
- **改造** `HotSearchCrawlController`：增加平台/每日/配置的 CRUD 接口；保留并增强 `POST /crawl` 返回每平台结果。

### 关键模式：动态定时

```java
// HotSearchCrawlJob 改造示意
public void reschedule() {
    if (scheduledFuture != null) scheduledFuture.cancel(false);
    HotSearchConfig cfg = configService.getOrInit();
    if (cfg.getEnabled() == 1) {
        scheduledFuture = taskScheduler.schedule(this::crawl,
            new CronTrigger(cfg.getCron()));
    }
}

@PostConstruct
public void initSchedule() {
    configService.syncFromProperties();  // DB 覆盖 yml
    reschedule();
}
```

## REST 接口

所有接口 `/api/v1/admin/hot-search/**`，均需 SUPER_ADMIN。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/platforms` | 平台列表（全量） |
| POST | `/platforms` | 新增 |
| PUT | `/platforms/{id}` | 修改 |
| DELETE | `/platforms/{id}` | 删除（有 daily 数据则 409） |
| GET | `/daily?platform=&date=&page=&size=` | 分页 |
| POST | `/daily` | 新增（手动补录） |
| PUT | `/daily/{id}` | 修改 |
| DELETE | `/daily/{id}` | 删除 |
| POST | `/daily/{id}/re-crawl` | 重抓该平台当日 |
| GET | `/config` | 读配置 + 上次抓取摘要 |
| PUT | `/config` | 写配置（落库 + reschedule） |
| POST | `/crawl` | 同步全量抓取，返回每平台结果 |
| GET | `/crawl/last-run` | 上次抓取摘要 |

**关键 VO**：`PlatformCrawlResult { platformCode, platformName, success, fetched, error }`、`CrawlResultVO { results, startedAt, finishedAt }`、`LastRunVO { lastRunAt, totalFetched, successCount, failCount, results }`、`HotSearchDailyAdminVO { id, platformCode, platformName, rankNum, title, hotValue, url, searchCount, snapshotDate }`。

## 错误码

新增 `AdminHotSearchErrorCode`（25xxxx 系列，复用现有 `ErrorCode` 模式）：

| 码 | 含义 |
|---|---|
| 250001 | 平台不存在 |
| 250002 | 平台编码已存在 |
| 250003 | 平台存在榜单数据，禁止删除 |
| 250004 | 每日榜单条目不存在 |
| 250005 | 配置不存在 |
| 250006 | cron 表达式非法 |
| 250007 | 手动抓取失败 |

## 后端结构

```
project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/
├── controller/
│   └── HotSearchAdminController.java
├── service/
│   ├── HotSearchPlatformAdminService.java
│   ├── HotSearchDailyAdminService.java
│   ├── HotSearchConfigService.java
│   └── impl/...
├── entity/
│   ├── HotSearchPlatform.java (已有)
│   ├── HotSearchDaily.java (已有)
│   └── HotSearchConfig.java
├── mapper/
│   └── HotSearchConfigMapper.java
├── enums/
│   └── AdminHotSearchErrorCode.java
└── dto/request/
    ├── HotSearchPlatformRequest.java
    ├── HotSearchDailyRequest.java
    └── HotSearchConfigRequest.java
```

改造：`job/HotSearchCrawlJob.java`（去掉静态 cron）。

## 前端结构

```
project/admin/web/src/
├── api/hotSearch.js
├── composables/useHotSearch.js
└── views/
    ├── HotSearchPlatformView.vue
    ├── HotSearchDailyView.vue
    └── HotSearchConfigView.vue
```

- 顶级菜单「热度榜」(`FireOutlined`)，三个子页：
  1. `/console/hot-search/platforms` 平台列表（表格 + 新增/编辑弹框 + 启停 + 删除）
  2. `/console/hot-search/daily` 每日榜单（平台+日期过滤的表格 + CRUD + 重抓按钮）
  3. `/console/hot-search/config` 抓取配置（表单 + 保存 + "立即抓取一次" + 上次抓取摘要卡片）

复用 `ModelConfigView.vue` 的 `request.js` / `useXxx.js` 模式。

## 测试

- **后端**：Service 层 `@SpringBootTest` 集成测试 — 平台 CRUD、每日 CRUD（新增重复 rank 报错）、配置保存触发 reschedule（验证 Trigger 替换）。
- **前端 E2E**：`tests/e2e/verify_hot_search_admin.py` — 登录 admin → 访问三个页面 → 列表渲染 → 保存配置 → 触发抓取。

## 风险

- jsoup 抓取各平台公开页面可能因反爬失败（已有 warn 日志和空列表兜底，不影响其他平台）。
- cron 表达式校验：保存前用 `CronExpression.parse()` 验证，无效则返回 250006。
- 同步全量抓取可能耗时 30-60s，UI 需 loading 态。

## 自审

1. **Spec coverage**：覆盖 CRUD 三个对象、同步时间动态生效、手动触发、上次摘要、权限、错误码。
2. **Placeholder scan**：无 TBD/TODO。
3. **Internal consistency**：DB → Entity → Service → Controller → 前端字段一致（`platformCode`、`rankNum`、`snapshotDate`、`cron`、`enabled`、`topN`、`connectTimeoutMillis`、`readTimeoutMillis`）。
4. **Scope**：单一管理端模块，单一实施计划可完成。
5. **Ambiguity**：手动抓取=同步阻塞；每日 CRUD=增改全权（用户在问答中确认）。
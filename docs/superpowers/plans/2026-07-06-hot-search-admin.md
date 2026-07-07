# 管理端热搜管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在管理端实现热搜的全面管理：平台 CRUD、每日榜单 CRUD、抓取配置（含 cron 运行时生效）、手动同步抓取、上次抓取摘要；以及对应的管理端前端三个页面。

**Architecture:** 后端新增 `hot_search_config` 单行配置表；改造 `HotSearchCrawlJob` 去掉 `@Scheduled` 静态 cron，改用 `ThreadPoolTaskScheduler` + `CronTrigger` 在启动和保存配置后动态注册/重建 Trigger；保留 `HotSearchProperties` (yml) 作为启动默认值；新增平台/每日/配置三类 CRUD 接口；手动抓取同步阻塞并返回每平台结果。前端顶级菜单「热度榜」下设平台管理、今日榜单、抓取配置三个子页，复用现有 ModelConfigView 模式。

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus + Flyway + Spring TaskScheduler + jsoup + Vue 3 + Ant Design Vue 4 + Axios + Playwright (E2E)。

## Global Constraints

- **DB 命名**：snake_case，表名 `hot_search_*`；与现有 `hot_search_platform` / `hot_search_daily` 一致。
- **错误码段**：25xxxx（新增 `AdminHotSearchErrorCode`）。
- **模块包路径**：`com.aichuangzuo.admin.modules.hotsearch`。
- **DTO/VO**：Lombok `@Data`；请求参数校验用 Jakarta Validation。
- **测试**：Service 层 `@SpringBootTest @Transactional` 集成测试。
- **前端 API**：复用 `src/utils/request.js`（与 modelConfig 风格一致）。
- **权限**：所有 `/api/v1/admin/hot-search/**` 接口需 SUPER_ADMIN，复用 `AdminUserPermissionService.isSuperAdmin()`。
- **动态定时**：使用 `ThreadPoolTaskScheduler` + `CronTrigger`，启动时和保存配置后 cancel 旧 Trigger 再注册新 Trigger；yml 保留为默认值（启动时若 DB 无记录则 INSERT 默认行）。
- **不引入新中间件**。

---

### Task 1: 创建 `hot_search_config` 表迁移

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_003__create_hot_search_config_table.sql`

**Interfaces:**
- Produces: 表 `hot_search_config`（单行，id=1）。

- [ ] **Step 1: 编写迁移**

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

INSERT IGNORE INTO hot_search_config (id, cron, enabled, top_n, connect_timeout_millis, read_timeout_millis)
VALUES (1, '0 0 2 * * ?', 1, 50, 5000, 10000);
```

- [ ] **Step 2: 运行 admin-api 触发迁移**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api spring-boot:run -DskipTests
```

启动后 Ctrl+C 停掉。

Expected: Flyway 日志显示 `Successfully applied 1 migration to schema "aichuangzuo"`。

- [ ] **Step 3: 验证表已创建**

```bash
MYSQL_PASSWORD=123456 mysql -uroot -e "USE aichuangzuo; SHOW CREATE TABLE hot_search_config\G; SELECT * FROM hot_search_config;"
```

Expected: 表结构正确，默认行存在。

- [ ] **Step 4: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/resources/db/migration/V2.0.0_003__create_hot_search_config_table.sql
git commit -m "feat(admin-hotsearch): 创建抓取配置表"
```

---

### Task 2: 错误码枚举

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/enums/AdminHotSearchErrorCode.java`

**Interfaces:**
- Produces: `AdminHotSearchErrorCode` 枚举（实现 `com.aichuangzuo.shared.result.ErrorCode`）。

- [ ] **Step 1: 编写枚举**

```java
package com.aichuangzuo.admin.modules.hotsearch.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 热搜模块管理端错误码。
 *
 * <p>错误码段：25xxxx
 */
@Getter
public enum AdminHotSearchErrorCode implements ErrorCode {

    PLATFORM_NOT_FOUND(250001, "热搜平台不存在"),
    PLATFORM_CODE_DUPLICATED(250002, "平台编码已存在"),
    PLATFORM_IN_USE(250003, "平台存在榜单数据，禁止删除"),
    DAILY_NOT_FOUND(250004, "每日榜单条目不存在"),
    DAILY_RANK_DUPLICATED(250005, "同日同平台存在相同排名"),
    CONFIG_NOT_FOUND(250006, "抓取配置不存在"),
    INVALID_CRON(250007, "cron 表达式非法"),
    CRAWL_FAILED(250008, "手动抓取失败");

    private final int code;
    private final String message;

    AdminHotSearchErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/enums/AdminHotSearchErrorCode.java
git commit -m "feat(admin-hotsearch): 错误码枚举"
```

---

### Task 3: `HotSearchConfig` 实体与 Mapper

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/entity/HotSearchConfig.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/mapper/HotSearchConfigMapper.java`

**Interfaces:**
- Produces: `HotSearchConfig` 实体（`@TableName("hot_search_config")`）；`HotSearchConfigMapper extends BaseMapper<HotSearchConfig>`。

- [ ] **Step 1: 创建实体**

```java
package com.aichuangzuo.admin.modules.hotsearch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 热搜抓取配置，对应表 hot_search_config。
 * 单行表，固定 id=1。
 */
@Getter
@Setter
@TableName("hot_search_config")
public class HotSearchConfig {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String cron;

    private Integer enabled;

    private Integer topN;

    private Integer connectTimeoutMillis;

    private Integer readTimeoutMillis;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
```

- [ ] **Step 2: 创建 Mapper**

```java
package com.aichuangzuo.admin.modules.hotsearch.mapper;

import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotSearchConfigMapper extends BaseMapper<HotSearchConfig> {
}
```

- [ ] **Step 3: 编译并 Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/entity/HotSearchConfig.java project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/mapper/HotSearchConfigMapper.java
git commit -m "feat(admin-hotsearch): Config 实体与 Mapper"
```

---

### Task 4: 抓取结果 VO 与请求 DTO

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/vo/PlatformCrawlResultVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/vo/CrawlResultVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/vo/LastRunVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/vo/HotSearchDailyAdminVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/dto/request/HotSearchPlatformRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/dto/request/HotSearchDailyRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/dto/request/HotSearchConfigRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/dto/request/HotSearchDailyQueryRequest.java`

**Interfaces:**
- Produces: 3 个 VO + 4 个 Request DTO。

- [ ] **Step 1: 抓取结果 VO**

`PlatformCrawlResultVO.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.vo;
import lombok.Data;
@Data
public class PlatformCrawlResultVO {
    private String platformCode;
    private String platformName;
    private boolean success;
    private int fetched;
    private String error;
}
```

`CrawlResultVO.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.vo;
import lombok.Data;
import java.time.Instant;
import java.util.List;
@Data
public class CrawlResultVO {
    private List<PlatformCrawlResultVO> results;
    private Instant startedAt;
    private Instant finishedAt;
}
```

`LastRunVO.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.vo;
import lombok.Data;
import java.time.Instant;
import java.util.List;
@Data
public class LastRunVO {
    private Instant lastRunAt;
    private int totalFetched;
    private int successCount;
    private int failCount;
    private List<PlatformCrawlResultVO> results;
}
```

`HotSearchDailyAdminVO.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.vo;
import lombok.Data;
import java.time.LocalDate;
@Data
public class HotSearchDailyAdminVO {
    private Long id;
    private String platformCode;
    private String platformName;
    private Integer rankNum;
    private String title;
    private String hotValue;
    private String url;
    private Long searchCount;
    private LocalDate snapshotDate;
}
```

- [ ] **Step 2: 请求 DTO**

`HotSearchPlatformRequest.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class HotSearchPlatformRequest {
    @NotBlank
    @Size(max = 32)
    private String code;
    @NotBlank
    @Size(max = 64)
    private String name;
    @Size(max = 255)
    private String icon;
    private Integer sortOrder;
    private Integer enabled;
}
```

`HotSearchDailyRequest.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
@Data
public class HotSearchDailyRequest {
    @NotBlank
    private String platformCode;
    @NotNull
    private Integer rankNum;
    @NotBlank
    @Size(max = 512)
    private String title;
    @Size(max = 64)
    private String hotValue;
    @Size(max = 1024)
    private String url;
    private Long searchCount;
    @NotNull
    private LocalDate snapshotDate;
}
```

`HotSearchConfigRequest.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
@Data
public class HotSearchConfigRequest {
    @NotBlank
    private String cron;
    @NotNull
    private Integer enabled;
    @NotNull
    @Min(1)
    private Integer topN;
    @NotNull
    @Min(100)
    private Integer connectTimeoutMillis;
    @NotNull
    @Min(100)
    private Integer readTimeoutMillis;
}
```

`HotSearchDailyQueryRequest.java`:
```java
package com.aichuangzuo.admin.modules.hotsearch.dto.request;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
@Data
public class HotSearchDailyQueryRequest {
    private String platform;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private Long page = 1L;
    private Long size = 20L;
}
```

- [ ] **Step 3: 编译并 Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/vo/ project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/dto/
git commit -m "feat(admin-hotsearch): 抓取结果 VO 与请求 DTO"
```

---

### Task 5: `HotSearchCrawlJob` 改造（动态 cron + 返回结果 + 记录上次摘要）

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/job/HotSearchCrawlJob.java`

**Interfaces:**
- Consumes: `HotSearchPlatformMapper`, `HotSearchDailyMapper`, `List<HotSearchFetcher>`, `HotSearchProperties`, `HotSearchConfigService`（下一 Task）。
- Produces:
  - `CrawlResultVO crawlAll()` — 同步抓取所有启用平台，返回每平台结果。
  - `void reschedule()` — 重建 Trigger。
  - `LastRunVO getLastRun()` — 上次抓取摘要。
  - `void crawl()` — 定时触发入口（无返回值）。

- [ ] **Step 1: 重写 `HotSearchCrawlJob`**

完整替换文件内容：

```java
package com.aichuangzuo.admin.modules.hotsearch.job;

import com.aichuangzuo.admin.modules.hotsearch.crawler.HotSearchFetcher;
import com.aichuangzuo.admin.modules.hotsearch.crawler.HotSearchItem;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchConfigService;
import com.aichuangzuo.admin.modules.hotsearch.vo.CrawlResultVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.LastRunVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.PlatformCrawlResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotSearchCrawlJob {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;
    private final List<HotSearchFetcher> fetchers;
    private final HotSearchConfigService configService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private volatile ScheduledFuture<?> scheduledFuture;
    private volatile LastRunVO lastRun = new LastRunVO();

    @PostConstruct
    public void init() {
        configService.syncFromProperties();
        reschedule();
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledFuture != null) scheduledFuture.cancel(false);
    }

    /**
     * 根据当前 DB 配置重建定时 Trigger。
     */
    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        var cfg = configService.getConfig();
        if (cfg.getEnabled() != null && cfg.getEnabled() == 1) {
            try {
                new CronTrigger(cfg.getCron()); // 校验
                scheduledFuture = taskScheduler.schedule(this::crawl, new CronTrigger(cfg.getCron()));
                log.info("热搜定时抓取已注册，cron={}", cfg.getCron());
            } catch (Exception e) {
                log.warn("热搜 cron 表达式非法，注册失败: {}", cfg.getCron());
            }
        } else {
            log.info("热搜定时抓取已停用");
        }
    }

    /**
     * 定时任务入口。
     */
    public void crawl() {
        try {
            crawlAll();
        } catch (Exception e) {
            log.error("定时抓取异常", e);
        }
    }

    /**
     * 同步抓取所有启用平台，返回每平台结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public CrawlResultVO crawlAll() {
        Instant startedAt = Instant.now();
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<HotSearchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotSearchPlatform::getEnabled, 1)
                .orderByAsc(HotSearchPlatform::getSortOrder);
        List<HotSearchPlatform> platforms = platformMapper.selectList(wrapper);

        List<PlatformCrawlResultVO> results = new ArrayList<>();
        for (HotSearchPlatform platform : platforms) {
            results.add(crawlPlatform(platform, today));
        }

        CrawlResultVO result = new CrawlResultVO();
        result.setResults(results);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());

        updateLastRun(results);
        log.info("热搜抓取完成，平台数={}", results.size());
        return result;
    }

    /**
     * 重抓指定平台当日。
     */
    @Transactional(rollbackFor = Exception.class)
    public CrawlResultVO recrawlPlatform(String platformCode) {
        Instant startedAt = Instant.now();
        LocalDate today = LocalDate.now();
        HotSearchPlatform platform = platformMapper.selectOne(
                new LambdaQueryWrapper<HotSearchPlatform>().eq(HotSearchPlatform::getCode, platformCode));
        if (platform == null) {
            throw new com.aichuangzuo.shared.exception.BusinessException(
                    com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        List<PlatformCrawlResultVO> results = List.of(crawlPlatform(platform, today));
        CrawlResultVO result = new CrawlResultVO();
        result.setResults(results);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());
        updateLastRun(results);
        return result;
    }

    public LastRunVO getLastRun() {
        return lastRun;
    }

    private PlatformCrawlResultVO crawlPlatform(HotSearchPlatform platform, LocalDate date) {
        PlatformCrawlResultVO vo = new PlatformCrawlResultVO();
        vo.setPlatformCode(platform.getCode());
        vo.setPlatformName(platform.getName());

        HotSearchFetcher fetcher = fetchers.stream()
                .filter(f -> f.supports(platform))
                .findFirst()
                .orElse(null);
        if (fetcher == null) {
            vo.setSuccess(false);
            vo.setFetched(0);
            vo.setError("无抓取器");
            log.warn("平台 [{}] 无可用抓取器", platform.getCode());
            return vo;
        }

        try {
            List<HotSearchItem> items = fetcher.fetch(platform);
            if (items == null || items.isEmpty()) {
                vo.setSuccess(false);
                vo.setFetched(0);
                vo.setError("未抓取到数据");
                log.warn("平台 [{}] 未抓取到数据", platform.getCode());
                return vo;
            }
            // 删除旧数据
            dailyMapper.delete(new LambdaQueryWrapper<HotSearchDaily>()
                    .eq(HotSearchDaily::getPlatformCode, platform.getCode())
                    .eq(HotSearchDaily::getSnapshotDate, date));
            // 写入新数据
            for (HotSearchItem item : items) {
                HotSearchDaily daily = new HotSearchDaily();
                daily.setPlatformCode(platform.getCode());
                daily.setRankNum(item.getRank());
                daily.setTitle(item.getTitle());
                daily.setHotValue(item.getHotValue());
                daily.setUrl(item.getUrl());
                daily.setSearchCount(item.getSearchCount());
                daily.setSnapshotDate(date);
                dailyMapper.insert(daily);
            }
            vo.setSuccess(true);
            vo.setFetched(items.size());
            log.info("平台 [{}] 抓取完成，写入 {} 条", platform.getCode(), items.size());
            return vo;
        } catch (Exception e) {
            vo.setSuccess(false);
            vo.setFetched(0);
            vo.setError(e.getMessage());
            log.warn("平台 [{}] 抓取失败: {}", platform.getCode(), e.getMessage());
            return vo;
        }
    }

    private void updateLastRun(List<PlatformCrawlResultVO> results) {
        LastRunVO run = new LastRunVO();
        run.setLastRunAt(Instant.now());
        run.setResults(results);
        run.setSuccessCount((int) results.stream().filter(PlatformCrawlResultVO::isSuccess).count());
        run.setFailCount(results.size() - run.getSuccessCount());
        run.setTotalFetched(results.stream().mapToInt(PlatformCrawlResultVO::getFetched).sum());
        this.lastRun = run;
    }
}
```

- [ ] **Step 2: 创建 `ThreadPoolTaskScheduler` 配置 Bean**

Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/config/HotSearchSchedulerConfig.java`

```java
package com.aichuangzuo.admin.modules.hotsearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class HotSearchSchedulerConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler hotSearchTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("hot-search-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
```

Expected: BUILD SUCCESS（可能因 `HotSearchConfigService` 尚未存在而失败 → 在 Task 6 后再编译；这里如果失败属正常）。

- [ ] **Step 4: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/job/HotSearchCrawlJob.java project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/config/HotSearchSchedulerConfig.java
git commit -m "refactor(admin-hotsearch): 改造 CrawlJob 支持动态 cron 与结果聚合"
```

---

### Task 6: `HotSearchConfigService`（含启动初始化与 reschedule 协调）

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/HotSearchConfigService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/impl/HotSearchConfigServiceImpl.java`

**Interfaces:**
- Produces:
  - `HotSearchConfig getConfig()` — 读取单行配置（不存在则抛 CONFIG_NOT_FOUND）。
  - `HotSearchConfig saveConfig(HotSearchConfigRequest req, Long updatedBy)` — 校验 cron，写库，调用 `HotSearchCrawlJob.reschedule()`。
  - `void syncFromProperties()` — 启动时若 DB 行不存在则 INSERT（来自 yml 默认）。

- [ ] **Step 1: Service 接口**

```java
package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;

public interface HotSearchConfigService {
    HotSearchConfig getConfig();

    HotSearchConfig saveConfig(HotSearchConfigRequest request, Long updatedBy);

    void syncFromProperties();
}
```

- [ ] **Step 2: 实现**

```java
package com.aichuangzuo.admin.modules.hotsearch.service.impl;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.job.HotSearchCrawlJob;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchConfigMapper;
import com.aichuangzuo.admin.modules.hotsearch.properties.HotSearchProperties;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchConfigService;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotSearchConfigServiceImpl implements HotSearchConfigService {

    private static final long CONFIG_ID = 1L;

    private final HotSearchConfigMapper configMapper;
    private final HotSearchProperties properties;
    private final HotSearchCrawlJob crawlJob;

    @Override
    public HotSearchConfig getConfig() {
        HotSearchConfig cfg = configMapper.selectById(CONFIG_ID);
        if (cfg == null) {
            throw new BusinessException(AdminHotSearchErrorCode.CONFIG_NOT_FOUND);
        }
        return cfg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchConfig saveConfig(HotSearchConfigRequest req, Long updatedBy) {
        try {
            new CronTrigger(req.getCron());
        } catch (Exception e) {
            throw new BusinessException(AdminHotSearchErrorCode.INVALID_CRON);
        }

        HotSearchConfig existing = configMapper.selectById(CONFIG_ID);
        HotSearchConfig entity = existing == null ? new HotSearchConfig() : existing;
        entity.setId(CONFIG_ID);
        entity.setCron(req.getCron());
        entity.setEnabled(req.getEnabled());
        entity.setTopN(req.getTopN());
        entity.setConnectTimeoutMillis(req.getConnectTimeoutMillis());
        entity.setReadTimeoutMillis(req.getReadTimeoutMillis());
        entity.setUpdatedBy(updatedBy == null ? 0L : updatedBy);

        if (existing == null) {
            entity.setCreatedAt(LocalDateTime.now());
            configMapper.insert(entity);
        } else {
            configMapper.updateById(entity);
        }

        // 重建定时 Trigger
        crawlJob.reschedule();
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromProperties() {
        HotSearchConfig existing = configMapper.selectById(CONFIG_ID);
        if (existing != null) {
            return;
        }
        HotSearchConfig entity = new HotSearchConfig();
        entity.setId(CONFIG_ID);
        entity.setCron(properties.getCron() != null ? properties.getCron() : "0 0 2 * * ?");
        entity.setEnabled(properties.isCrawlEnabled() ? 1 : 0);
        entity.setTopN(properties.getTopN() != null ? properties.getTopN() : 50);
        entity.setConnectTimeoutMillis(properties.getConnectTimeoutMillis() != null ? properties.getConnectTimeoutMillis() : 5000);
        entity.setReadTimeoutMillis(properties.getReadTimeoutMillis() != null ? properties.getReadTimeoutMillis() : 10000);
        entity.setUpdatedBy(0L);
        configMapper.insert(entity);
        log.info("热搜配置默认值已写入 DB");
    }
}
```

- [ ] **Step 3: 编译并 Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/
git commit -m "feat(admin-hotsearch): Config Service"
```

---

### Task 7: 平台 CRUD Service

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/HotSearchPlatformAdminService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/impl/HotSearchPlatformAdminServiceImpl.java`

**Interfaces:**
- Produces:
  - `List<HotSearchPlatform> listAll()`
  - `HotSearchPlatform create(HotSearchPlatformRequest)`
  - `HotSearchPlatform update(Long id, HotSearchPlatformRequest)`
  - `void delete(Long id)` — 若 `hot_search_daily` 有引用则抛 PLATFORM_IN_USE。

- [ ] **Step 1: 接口**

```java
package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import java.util.List;

public interface HotSearchPlatformAdminService {
    List<HotSearchPlatform> listAll();
    HotSearchPlatform create(HotSearchPlatformRequest req);
    HotSearchPlatform update(Long id, HotSearchPlatformRequest req);
    void delete(Long id);
}
```

- [ ] **Step 2: 实现**

```java
package com.aichuangzuo.admin.modules.hotsearch.service.impl;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchPlatformAdminService;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotSearchPlatformAdminServiceImpl implements HotSearchPlatformAdminService {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;

    @Override
    public List<HotSearchPlatform> listAll() {
        return platformMapper.selectList(
                new LambdaQueryWrapper<HotSearchPlatform>().orderByAsc(HotSearchPlatform::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchPlatform create(HotSearchPlatformRequest req) {
        Long exist = platformMapper.selectCount(
                new LambdaQueryWrapper<HotSearchPlatform>().eq(HotSearchPlatform::getCode, req.getCode()));
        if (exist != null && exist > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_CODE_DUPLICATED);
        }
        HotSearchPlatform p = new HotSearchPlatform();
        p.setCode(req.getCode());
        p.setName(req.getName());
        p.setIcon(req.getIcon());
        p.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        p.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        platformMapper.insert(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchPlatform update(Long id, HotSearchPlatformRequest req) {
        HotSearchPlatform p = platformMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        p.setName(req.getName());
        p.setIcon(req.getIcon());
        p.setSortOrder(req.getSortOrder() == null ? p.getSortOrder() : req.getSortOrder());
        p.setEnabled(req.getEnabled() == null ? p.getEnabled() : req.getEnabled());
        p.setUpdatedAt(LocalDateTime.now());
        platformMapper.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HotSearchPlatform p = platformMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        Long refCount = dailyMapper.selectCount(
                new LambdaQueryWrapper<com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily>()
                        .eq(com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily::getPlatformCode, p.getCode()));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_IN_USE);
        }
        platformMapper.deleteById(id);
    }
}
```

- [ ] **Step 3: 编译并 Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/HotSearchPlatformAdminService.java project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/impl/HotSearchPlatformAdminServiceImpl.java
git commit -m "feat(admin-hotsearch): 平台 CRUD Service"
```

---

### Task 8: 每日榜单 CRUD Service

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/HotSearchDailyAdminService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/impl/HotSearchDailyAdminServiceImpl.java`

**Interfaces:**
- Produces:
  - `PageResult<HotSearchDailyAdminVO> list(HotSearchDailyQueryRequest)` — 分页。
  - `HotSearchDailyAdminVO get(Long id)`
  - `HotSearchDailyAdminVO create(HotSearchDailyRequest)`
  - `HotSearchDailyAdminVO update(Long id, HotSearchDailyRequest)`
  - `void delete(Long id)`

- [ ] **Step 1: 接口**

```java
package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyRequest;
import com.aichuangzuo.admin.modules.hotsearch.vo.HotSearchDailyAdminVO;
import java.util.List;

public interface HotSearchDailyAdminService {
    PageResult list(HotSearchDailyQueryRequest request);
    HotSearchDailyAdminVO get(Long id);
    HotSearchDailyAdminVO create(HotSearchDailyRequest req);
    HotSearchDailyAdminVO update(Long id, HotSearchDailyRequest req);
    void delete(Long id);

    record PageResult(List<HotSearchDailyAdminVO> items, long total, long page, long size) {}
}
```

- [ ] **Step 2: 实现**

```java
package com.aichuangzuo.admin.modules.hotsearch.service.impl;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchDailyAdminService;
import com.aichuangzuo.admin.modules.hotsearch.vo.HotSearchDailyAdminVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotSearchDailyAdminServiceImpl implements HotSearchDailyAdminService {

    private final HotSearchDailyMapper dailyMapper;
    private final HotSearchPlatformMapper platformMapper;

    @Override
    public PageResult list(HotSearchDailyQueryRequest req) {
        LambdaQueryWrapper<HotSearchDaily> wrapper = new LambdaQueryWrapper<>();
        if (req.getPlatform() != null && !req.getPlatform().isBlank()) {
            wrapper.eq(HotSearchDaily::getPlatformCode, req.getPlatform());
        }
        if (req.getDate() != null) {
            wrapper.eq(HotSearchDaily::getSnapshotDate, req.getDate());
        }
        wrapper.orderByDesc(HotSearchDaily::getSnapshotDate)
                .orderByAsc(HotSearchDaily::getPlatformCode)
                .orderByAsc(HotSearchDaily::getRankNum);

        Page<HotSearchDaily> page = dailyMapper.selectPage(
                Page.of(req.getPage(), req.getSize()), wrapper);

        Map<String, String> platformNames = platformMapper.selectList(null).stream()
                .collect(Collectors.toMap(HotSearchPlatform::getCode, HotSearchPlatform::getName, (a, b) -> a));
        List<HotSearchDailyAdminVO> items = page.getRecords().stream().map(d -> toVO(d, platformNames)).toList();
        return new PageResult(items, page.getTotal(), req.getPage(), req.getSize());
    }

    @Override
    public HotSearchDailyAdminVO get(Long id) {
        HotSearchDaily d = dailyMapper.selectById(id);
        if (d == null) throw new BusinessException(AdminHotSearchErrorCode.DAILY_NOT_FOUND);
        return toVO(d, platformNameMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchDailyAdminVO create(HotSearchDailyRequest req) {
        // 校验平台存在
        HotSearchPlatform p = platformMapper.selectOne(
                new LambdaQueryWrapper<HotSearchPlatform>().eq(HotSearchPlatform::getCode, req.getPlatformCode()));
        if (p == null) throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);

        // 校验 (platformCode, snapshotDate, rankNum) 唯一
        Long dup = dailyMapper.selectCount(new LambdaQueryWrapper<HotSearchDaily>()
                .eq(HotSearchDaily::getPlatformCode, req.getPlatformCode())
                .eq(HotSearchDaily::getSnapshotDate, req.getSnapshotDate())
                .eq(HotSearchDaily::getRankNum, req.getRankNum()));
        if (dup != null && dup > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.DAILY_RANK_DUPLICATED);
        }

        HotSearchDaily d = new HotSearchDaily();
        d.setPlatformCode(req.getPlatformCode());
        d.setRankNum(req.getRankNum());
        d.setTitle(req.getTitle());
        d.setHotValue(req.getHotValue());
        d.setUrl(req.getUrl());
        d.setSearchCount(req.getSearchCount());
        d.setSnapshotDate(req.getSnapshotDate());
        d.setCreatedAt(LocalDateTime.now());
        dailyMapper.insert(d);
        return toVO(d, platformNameMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchDailyAdminVO update(Long id, HotSearchDailyRequest req) {
        HotSearchDaily d = dailyMapper.selectById(id);
        if (d == null) throw new BusinessException(AdminHotSearchErrorCode.DAILY_NOT_FOUND);

        // 唯一约束：排除自身后检查
        Long dup = dailyMapper.selectCount(new LambdaQueryWrapper<HotSearchDaily>()
                .eq(HotSearchDaily::getPlatformCode, req.getPlatformCode())
                .eq(HotSearchDaily::getSnapshotDate, req.getSnapshotDate())
                .eq(HotSearchDaily::getRankNum, req.getRankNum())
                .ne(HotSearchDaily::getId, id));
        if (dup != null && dup > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.DAILY_RANK_DUPLICATED);
        }

        d.setRankNum(req.getRankNum());
        d.setTitle(req.getTitle());
        d.setHotValue(req.getHotValue());
        d.setUrl(req.getUrl());
        d.setSearchCount(req.getSearchCount());
        d.setSnapshotDate(req.getSnapshotDate());
        dailyMapper.updateById(d);
        return toVO(d, platformNameMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (dailyMapper.selectById(id) == null) {
            throw new BusinessException(AdminHotSearchErrorCode.DAILY_NOT_FOUND);
        }
        dailyMapper.deleteById(id);
    }

    private Map<String, String> platformNameMap() {
        return platformMapper.selectList(null).stream()
                .collect(Collectors.toMap(HotSearchPlatform::getCode, HotSearchPlatform::getName, (a, b) -> a));
    }

    private HotSearchDailyAdminVO toVO(HotSearchDaily d, Map<String, String> names) {
        HotSearchDailyAdminVO vo = new HotSearchDailyAdminVO();
        vo.setId(d.getId());
        vo.setPlatformCode(d.getPlatformCode());
        vo.setPlatformName(names.getOrDefault(d.getPlatformCode(), d.getPlatformCode()));
        vo.setRankNum(d.getRankNum());
        vo.setTitle(d.getTitle());
        vo.setHotValue(d.getHotValue());
        vo.setUrl(d.getUrl());
        vo.setSearchCount(d.getSearchCount());
        vo.setSnapshotDate(d.getSnapshotDate());
        return vo;
    }
}
```

- [ ] **Step 3: 编译并 Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/HotSearchDailyAdminService.java project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/service/impl/HotSearchDailyAdminServiceImpl.java
git commit -m "feat(admin-hotsearch): 每日榜单 CRUD Service"
```

---

### Task 9: Admin Controller（合并所有接口）

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/controller/HotSearchCrawlController.java`
  (重命名为 `HotSearchAdminController.java`，或保留文件名；为减少改动保留文件名并扩展）

**Interfaces:**
- Produces: 13 个 endpoint（详见 spec §REST 接口）。

- [ ] **Step 1: 重写 `HotSearchCrawlController` 为 `HotSearchAdminController`**

Delete the file then Create new:
`project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/controller/HotSearchAdminController.java`

```java
package com.aichuangzuo.admin.modules.hotsearch.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.*;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;
import com.aichuangzuo.admin.modules.hotsearch.job.HotSearchCrawlJob;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchConfigService;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchDailyAdminService;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchPlatformAdminService;
import com.aichuangzuo.admin.modules.hotsearch.vo.CrawlResultVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.HotSearchDailyAdminVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.LastRunVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端热搜管理")
@RestController
@RequestMapping("/api/v1/admin/hot-search")
@RequiredArgsConstructor
public class HotSearchAdminController {

    private final HotSearchPlatformAdminService platformService;
    private final HotSearchDailyAdminService dailyService;
    private final HotSearchConfigService configService;
    private final HotSearchCrawlJob crawlJob;
    private final AdminUserPermissionService permissionService;

    // ===== 平台 =====
    @GetMapping("/platforms")
    public Result<List<HotSearchPlatform>> listPlatforms() {
        checkSuperAdmin();
        return Result.success(platformService.listAll());
    }

    @PostMapping("/platforms")
    public Result<HotSearchPlatform> createPlatform(@Valid @RequestBody HotSearchPlatformRequest req) {
        checkSuperAdmin();
        return Result.success(platformService.create(req));
    }

    @PutMapping("/platforms/{id}")
    public Result<HotSearchPlatform> updatePlatform(@PathVariable Long id, @Valid @RequestBody HotSearchPlatformRequest req) {
        checkSuperAdmin();
        return Result.success(platformService.update(id, req));
    }

    @DeleteMapping("/platforms/{id}")
    public Result<Void> deletePlatform(@PathVariable Long id) {
        checkSuperAdmin();
        platformService.delete(id);
        return Result.success();
    }

    // ===== 每日榜单 =====
    @GetMapping("/daily")
    public Result<HotSearchDailyAdminService.PageResult> listDaily(HotSearchDailyQueryRequest req) {
        checkSuperAdmin();
        return Result.success(dailyService.list(req));
    }

    @PostMapping("/daily")
    public Result<HotSearchDailyAdminVO> createDaily(@Valid @RequestBody HotSearchDailyRequest req) {
        checkSuperAdmin();
        return Result.success(dailyService.create(req));
    }

    @PutMapping("/daily/{id}")
    public Result<HotSearchDailyAdminVO> updateDaily(@PathVariable Long id, @Valid @RequestBody HotSearchDailyRequest req) {
        checkSuperAdmin();
        return Result.success(dailyService.update(id, req));
    }

    @DeleteMapping("/daily/{id}")
    public Result<Void> deleteDaily(@PathVariable Long id) {
        checkSuperAdmin();
        dailyService.delete(id);
        return Result.success();
    }

    @PostMapping("/daily/{id}/re-crawl")
    public Result<CrawlResultVO> recrawlDaily(@PathVariable Long id) {
        checkSuperAdmin();
        HotSearchDailyAdminVO vo = dailyService.get(id);
        return Result.success(crawlJob.recrawlPlatform(vo.getPlatformCode()));
    }

    // ===== 配置 =====
    @GetMapping("/config")
    public Result<HotSearchConfig> getConfig() {
        checkSuperAdmin();
        HotSearchConfig cfg = configService.getConfig();
        return Result.success(cfg);
    }

    @PutMapping("/config")
    public Result<HotSearchConfig> saveConfig(@Valid @RequestBody HotSearchConfigRequest req) {
        checkSuperAdmin();
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(configService.saveConfig(req, adminId));
    }

    // ===== 手动抓取 & 摘要 =====
    @PostMapping("/crawl")
    public Result<CrawlResultVO> crawlNow() {
        checkSuperAdmin();
        return Result.success(crawlJob.crawlAll());
    }

    @GetMapping("/crawl/last-run")
    public Result<LastRunVO> lastRun() {
        checkSuperAdmin();
        return Result.success(crawlJob.getLastRun());
    }

    private void checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
```

- [ ] **Step 2: 删除旧的 `HotSearchCrawlController`**

```bash
rm /Users/panyong/aio_project/ai_chuangzuo/project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/controller/HotSearchCrawlController.java
```

- [ ] **Step 3: 编译并 Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api -am compile -DskipTests -q
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/hotsearch/controller/
git commit -m "feat(admin-hotsearch): 管理 Controller 整合所有接口"
```

---

### Task 10: 启动验证 + 修复循环依赖

**Files:**
- 可能需要调整 `HotSearchCrawlJob` / `HotSearchConfigServiceImpl` 之间的依赖。

- [ ] **Step 1: 启动 admin-api**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api
MYSQL_PASSWORD=123456 mvn spring-boot:run -DskipTests > /tmp/admin-api-hotsearch.log 2>&1 &
```

等待 10 秒。

- [ ] **Step 2: 检查启动日志**

```bash
grep -E "Started AdminApiApplication|APPLICATION FAILED|UnsatisfiedDependencyException|cron|热搜" /tmp/admin-api-hotsearch.log | tail -20
```

Expected: `Started AdminApiApplication in X.X seconds`，且包含 `热搜配置默认值已写入 DB` 和 `热搜定时抓取已注册，cron=0 0 2 * * ?`。

如果出现循环依赖（`HotSearchCrawlJob` 依赖 `HotSearchConfigService`，`HotSearchConfigService` 依赖 `HotSearchCrawlJob`），解法：在 `HotSearchCrawlJob` 中改为**懒加载** `HotSearchConfigService`（注入 `ObjectProvider<HotSearchConfigService>`），或在 `HotSearchConfigServiceImpl` 中将 `crawlJob.reschedule()` 调用改为通过 `ApplicationContext` 查找。

- [ ] **Step 3: 验证 controller 接口可用**

```bash
TOKEN=$(curl -sS -X POST http://localhost:26060/api/v1/admin/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Root1qaz!QAZ"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
curl -sS http://localhost:26060/api/v1/admin/hot-search/config -H "Authorization: Bearer $TOKEN"
```

Expected: 返回 `{"code":0,"data":{"id":1,"cron":"0 0 2 * * ?","enabled":1,...}}`。

- [ ] **Step 4: 停止 admin-api**

```bash
PID=$(lsof -ti:26060); kill -9 $PID 2>/dev/null
```

---

### Task 11: 后端集成测试

**Files:**
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/hotsearch/service/HotSearchConfigServiceTest.java`
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/hotsearch/service/HotSearchPlatformAdminServiceTest.java`

- [ ] **Step 1: `HotSearchConfigServiceTest`**

```java
package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchConfigMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class HotSearchConfigServiceTest {

    @Autowired
    private HotSearchConfigService configService;

    @Autowired
    private HotSearchConfigMapper configMapper;

    @Test
    void shouldReadDefaultConfig() {
        assertNotNull(configService.getConfig());
    }

    @Test
    void shouldRejectInvalidCron() {
        HotSearchConfigRequest req = new HotSearchConfigRequest();
        req.setCron("not-a-cron");
        req.setEnabled(1);
        req.setTopN(50);
        req.setConnectTimeoutMillis(5000);
        req.setReadTimeoutMillis(10000);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> configService.saveConfig(req, 1L));
        assertEquals(AdminHotSearchErrorCode.INVALID_CRON.getCode(), ex.getCode());
    }

    @Test
    void shouldSaveValidConfig() {
        HotSearchConfigRequest req = new HotSearchConfigRequest();
        req.setCron("0 0 3 * * ?");
        req.setEnabled(1);
        req.setTopN(30);
        req.setConnectTimeoutMillis(5000);
        req.setReadTimeoutMillis(10000);
        configService.saveConfig(req, 1L);
        assertEquals("0 0 3 * * ?", configService.getConfig().getCron());
    }
}
```

- [ ] **Step 2: `HotSearchPlatformAdminServiceTest`**

```java
package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class HotSearchPlatformAdminServiceTest {

    @Autowired
    private HotSearchPlatformAdminService service;

    @Test
    void shouldCreateAndDelete() {
        HotSearchPlatformRequest req = new HotSearchPlatformRequest();
        req.setCode("test_plat_" + System.currentTimeMillis());
        req.setName("Test");
        req.setEnabled(1);
        var created = service.create(req);
        assertNotNull(created.getId());
        service.delete(created.getId());
    }

    @Test
    void shouldRejectDuplicateCode() {
        HotSearchPlatformRequest req = new HotSearchPlatformRequest();
        req.setCode("douyin"); // 已存在
        req.setName("Dup");
        req.setEnabled(1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminHotSearchErrorCode.PLATFORM_CODE_DUPLICATED.getCode(), ex.getCode());
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl admin/api test -Dtest='HotSearchConfigServiceTest,HotSearchPlatformAdminServiceTest'
```

Expected: Tests run: 5, Failures: 0, Errors: 0。

- [ ] **Step 4: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/hotsearch/service/
git commit -m "test(admin-hotsearch): Config 与 Platform Service 集成测试"
```

---

### Task 12: 前端 API、Composable、路由与菜单

**Files:**
- Create: `project/admin/web/src/api/hotSearch.js`
- Create: `project/admin/web/src/composables/useHotSearch.js`
- Modify: `project/admin/web/src/router/index.js`
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`

- [ ] **Step 1: API 文件**

```javascript
import request from '@/utils/request.js'

export function listPlatforms() {
  return request.get('/api/v1/admin/hot-search/platforms').then((res) => res.data)
}
export function createPlatform(data) {
  return request.post('/api/v1/admin/hot-search/platforms', data)
}
export function updatePlatform(id, data) {
  return request.put(`/api/v1/admin/hot-search/platforms/${id}`, data)
}
export function deletePlatform(id) {
  return request.delete(`/api/v1/admin/hot-search/platforms/${id}`)
}

export function listDaily(params) {
  return request.get('/api/v1/admin/hot-search/daily', { params }).then((res) => res.data)
}
export function createDaily(data) {
  return request.post('/api/v1/admin/hot-search/daily', data)
}
export function updateDaily(id, data) {
  return request.put(`/api/v1/admin/hot-search/daily/${id}`, data)
}
export function deleteDaily(id) {
  return request.delete(`/api/v1/admin/hot-search/daily/${id}`)
}
export function recrawlDaily(id) {
  return request.post(`/api/v1/admin/hot-search/daily/${id}/re-crawl`).then((res) => res.data)
}

export function getConfig() {
  return request.get('/api/v1/admin/hot-search/config').then((res) => res.data)
}
export function saveConfig(data) {
  return request.put('/api/v1/admin/hot-search/config', data)
}
export function crawlNow() {
  return request.post('/api/v1/admin/hot-search/crawl').then((res) => res.data)
}
export function getLastRun() {
  return request.get('/api/v1/admin/hot-search/crawl/last-run').then((res) => res.data)
}
```

- [ ] **Step 2: Composable**

```javascript
import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import * as api from '@/api/hotSearch.js'

export function useHotSearch() {
  const state = reactive({
    platforms: [],
    daily: { items: [], total: 0, page: 1, size: 20 },
    config: { id: 1, cron: '', enabled: 1, topN: 50, connectTimeoutMillis: 5000, readTimeoutMillis: 10000 },
    lastRun: { lastRunAt: null, totalFetched: 0, successCount: 0, failCount: 0, results: [] },
    loading: false
  })

  const fetchPlatforms = async () => {
    state.loading = true
    try {
      state.platforms = await api.listPlatforms()
    } finally { state.loading = false }
  }
  const fetchDaily = async (params) => {
    state.loading = true
    try {
      state.daily = await api.listDaily(params || {})
    } finally { state.loading = false }
  }
  const fetchConfig = async () => {
    state.config = await api.getConfig()
  }
  const fetchLastRun = async () => {
    state.lastRun = await api.getLastRun()
  }

  return {
    state,
    fetchPlatforms, fetchDaily, fetchConfig, fetchLastRun,
    savePlatform: async (data) => { await api.createPlatform(data); message.success('已新增') },
    updatePlatform: async (id, data) => { await api.updatePlatform(id, data); message.success('已更新') },
    removePlatform: async (id) => { await api.deletePlatform(id); message.success('已删除') },
    saveDaily: async (data) => { await api.createDaily(data); message.success('已新增') },
    updateDaily: async (id, data) => { await api.updateDaily(id, data); message.success('已更新') },
    removeDaily: async (id) => { await api.deleteDaily(id); message.success('已删除') },
    saveConfig: async (data) => { await api.saveConfig(data); message.success('已保存，定时任务已重建') },
    crawlNow: async () => { const r = await api.crawlNow(); message.success('抓取完成'); return r },
    recrawlDaily: async (id) => { const r = await api.recrawlDaily(id); message.success('重抓完成'); return r }
  }
}
```

- [ ] **Step 3: 路由**

Modify `project/admin/web/src/router/index.js`，在 `children` 数组添加：

```javascript
{
  path: 'hot-search/platforms',
  name: 'AdminHotSearchPlatforms',
  component: () => import('@/views/HotSearchPlatformView.vue')
},
{
  path: 'hot-search/daily',
  name: 'AdminHotSearchDaily',
  component: () => import('@/views/HotSearchDailyView.vue')
},
{
  path: 'hot-search/config',
  name: 'AdminHotSearchConfig',
  component: () => import('@/views/HotSearchConfigView.vue')
}
```

- [ ] **Step 4: 菜单**

Modify `project/admin/web/src/layouts/AdminLayout.vue`：

1. 在 `import { ... } from '@ant-design/icons-vue'` 增加 `FireOutlined`。
2. 在 `<a-menu>` 内、`用户管理` 之前插入顶级菜单项：

```html
<a-sub-menu key="/console/hot-search">
  <template #icon><FireOutlined /></template>
  <template #title>热度榜</template>
  <a-menu-item key="/console/hot-search/platforms">平台管理</a-menu-item>
  <a-menu-item key="/console/hot-search/daily">今日榜单</a-menu-item>
  <a-menu-item key="/console/hot-search/config">抓取配置</a-menu-item>
</a-sub-menu>
```

3. `openKeys` 初始值加上 `'/console/hot-search'`。
4. `currentMenuName` 计算属性增加三条分支返回「平台管理 / 今日榜单 / 抓取配置」。

- [ ] **Step 5: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/web/src/api/hotSearch.js project/admin/web/src/composables/useHotSearch.js project/admin/web/src/router/index.js project/admin/web/src/layouts/AdminLayout.vue
git commit -m "feat(admin-web): 热搜管理 API、Composable、路由、菜单"
```

---

### Task 13: 三个前端页面

**Files:**
- Create: `project/admin/web/src/views/HotSearchPlatformView.vue`
- Create: `project/admin/web/src/views/HotSearchDailyView.vue`
- Create: `project/admin/web/src/views/HotSearchConfigView.vue`

**约定：** 三个页面均用 `<a-card>` + `<a-table>` + `<a-modal>`，与 ModelConfigView 风格保持一致；表格列固定高度；表单校验用 `a-form` rules；删除前 `<a-popconfirm>`。

- [ ] **Step 1: `HotSearchPlatformView.vue`**

```vue
<template>
  <div class="hot-search-platforms">
    <a-card title="热搜平台管理">
      <template #extra>
        <a-button type="primary" @click="openCreate">新增平台</a-button>
      </template>
      <a-table
        :data-source="state.platforms"
        :columns="columns"
        :loading="state.loading"
        row-key="id"
        :pagination="{ pageSize: 20 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-tag :color="record.enabled === 1 ? 'green' : 'default'">
              {{ record.enabled === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="editing.id ? '编辑平台' : '新增平台'" @ok="handleSubmit">
      <a-form layout="vertical" :model="editing">
        <a-form-item label="编码" :required="true">
          <a-input v-model:value="editing.code" :disabled="!!editing.id" placeholder="如 weibo" />
        </a-form-item>
        <a-form-item label="名称" :required="true">
          <a-input v-model:value="editing.name" />
        </a-form-item>
        <a-form-item label="图标 URL">
          <a-input v-model:value="editing.icon" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="editing.sortOrder" :min="0" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="enabledBool" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch.js'

const { state, fetchPlatforms, savePlatform, updatePlatform, removePlatform } = useHotSearch()

const columns = [
  { title: '编码', dataIndex: 'code', key: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '图标', dataIndex: 'icon', key: 'icon' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'enabled', width: 100 },
  { title: '操作', key: 'action', width: 180 }
]

const modalOpen = ref(false)
const editing = reactive({ id: null, code: '', name: '', icon: '', sortOrder: 0, enabled: 1 })
const enabledBool = computed({
  get: () => editing.enabled === 1,
  set: (v) => (editing.enabled = v ? 1 : 0)
})

const openCreate = () => {
  editing.id = null
  editing.code = ''
  editing.name = ''
  editing.icon = ''
  editing.sortOrder = 0
  editing.enabled = 1
  modalOpen.value = true
}
const openEdit = (r) => {
  Object.assign(editing, r)
  modalOpen.value = true
}
const handleSubmit = async () => {
  if (!editing.code || !editing.name) {
    message.warning('请填写编码和名称')
    return
  }
  const payload = { ...editing }
  delete payload.id
  if (editing.id) {
    await updatePlatform(editing.id, payload)
  } else {
    await savePlatform(payload)
  }
  modalOpen.value = false
  fetchPlatforms()
}
const handleDelete = async (id) => {
  await removePlatform(id)
  fetchPlatforms()
}

onMounted(fetchPlatforms)
</script>

<style scoped>.hot-search-platforms { padding: 0; }</style>
```

- [ ] **Step 2: `HotSearchDailyView.vue`**

```vue
<template>
  <div class="hot-search-daily">
    <a-card title="每日榜单管理">
      <a-form layout="inline" :model="query" class="filter-bar">
        <a-form-item label="平台">
          <a-select v-model:value="query.platform" allow-clear style="width: 160px" placeholder="全部">
            <a-select-option v-for="p in state.platforms" :key="p.code" :value="p.code">
              {{ p.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="日期">
          <a-date-picker v-model:value="dateObj" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="reload">查询</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="openCreate">新增条目</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :data-source="state.daily.items"
        :columns="columns"
        :loading="state.loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
              <a-popconfirm title="将重新抓取该平台当日数据，确认？" @confirm="handleRecrawl(record)">
                <a-button size="small">重抓</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="editing.id ? '编辑条目' : '新增条目'" @ok="handleSubmit">
      <a-form layout="vertical" :model="editing">
        <a-form-item label="平台" :required="true">
          <a-select v-model:value="editing.platformCode" :disabled="!!editing.id">
            <a-select-option v-for="p in state.platforms" :key="p.code" :value="p.code">
              {{ p.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="排名" :required="true">
          <a-input-number v-model:value="editing.rankNum" :min="1" />
        </a-form-item>
        <a-form-item label="标题" :required="true">
          <a-input v-model:value="editing.title" />
        </a-form-item>
        <a-form-item label="热度值">
          <a-input v-model:value="editing.hotValue" />
        </a-form-item>
        <a-form-item label="URL">
          <a-input v-model:value="editing.url" />
        </a-form-item>
        <a-form-item label="搜索量">
          <a-input-number v-model:value="editing.searchCount" :min="0" />
        </a-form-item>
        <a-form-item label="快照日期" :required="true">
          <a-date-picker v-model:value="editing.snapshotDate" value-format="YYYY-MM-DD" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch.js'

const { state, fetchPlatforms, fetchDaily, saveDaily, updateDaily, removeDaily, recrawlDaily } = useHotSearch()

const columns = [
  { title: '日期', dataIndex: 'snapshotDate', key: 'snapshotDate', width: 120 },
  { title: '平台', dataIndex: 'platformName', key: 'platformName', width: 120 },
  { title: '排名', dataIndex: 'rankNum', key: 'rankNum', width: 80 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '热度', dataIndex: 'hotValue', key: 'hotValue', width: 100 },
  { title: '操作', key: 'action', width: 240 }
]

const query = reactive({ platform: '', date: '', page: 1, size: 20 })
const dateObj = ref('')
watch(dateObj, (v) => (query.date = v || ''))

const pagination = computed(() => ({
  current: Number(state.daily.page) || 1,
  pageSize: Number(state.daily.size) || 20,
  total: Number(state.daily.total) || 0,
  showSizeChanger: true
}))

const reload = () => fetchDaily({ ...query })
const handleTableChange = (p) => {
  query.page = p.current
  query.size = p.pageSize
  fetchDaily({ ...query })
}

const modalOpen = ref(false)
const editing = reactive({
  id: null, platformCode: '', rankNum: 1, title: '', hotValue: '', url: '', searchCount: null, snapshotDate: ''
})
const openCreate = () => {
  editing.id = null
  editing.platformCode = state.platforms[0]?.code || ''
  editing.rankNum = 1
  editing.title = ''
  editing.hotValue = ''
  editing.url = ''
  editing.searchCount = null
  editing.snapshotDate = new Date().toISOString().slice(0, 10)
  modalOpen.value = true
}
const openEdit = (r) => Object.assign(editing, r)
const handleSubmit = async () => {
  if (!editing.platformCode || !editing.title || !editing.snapshotDate) {
    message.warning('请填写平台、标题、日期')
    return
  }
  const payload = { ...editing }
  delete payload.id
  if (editing.id) await updateDaily(editing.id, payload)
  else await saveDaily(payload)
  modalOpen.value = false
  reload()
}
const handleDelete = async (id) => { await removeDaily(id); reload() }
const handleRecrawl = async (record) => { await recrawlDaily(record.id); reload() }

onMounted(async () => {
  await fetchPlatforms()
  await reload()
})
</script>

<style scoped>
.hot-search-daily .filter-bar { margin-bottom: 16px; }
</style>
```

- [ ] **Step 3: `HotSearchConfigView.vue`**

```vue
<template>
  <div class="hot-search-config">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="14">
        <a-card title="抓取配置">
          <a-form layout="vertical" :model="form" :rules="rules" ref="formRef">
            <a-form-item label="cron 表达式（如 0 0 2 * * ?）" name="cron">
              <a-input v-model:value="form.cron" placeholder="0 0 2 * * ?" />
            </a-form-item>
            <a-form-item label="启用定时抓取" name="enabled">
              <a-switch v-model:checked="enabledBool" />
            </a-form-item>
            <a-form-item label="每个平台前 N 条" name="topN">
              <a-input-number v-model:value="form.topN" :min="1" :max="200" />
            </a-form-item>
            <a-form-item label="连接超时 (ms)" name="connectTimeoutMillis">
              <a-input-number v-model:value="form.connectTimeoutMillis" :min="100" />
            </a-form-item>
            <a-form-item label="读取超时 (ms)" name="readTimeoutMillis">
              <a-input-number v-model:value="form.readTimeoutMillis" :min="100" />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSave">保存配置</a-button>
                <a-button @click="handleCrawlNow" :loading="crawling">立即抓取一次</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="上次抓取摘要">
          <template v-if="state.lastRun.lastRunAt">
            <p>抓取时间：{{ formatTime(state.lastRun.lastRunAt) }}</p>
            <p>总条数：{{ state.lastRun.totalFetched }}</p>
            <p>成功：<a-tag color="green">{{ state.lastRun.successCount }}</a-tag> 失败：<a-tag color="red">{{ state.lastRun.failCount }}</a-tag></p>
            <a-divider />
            <a-list
              :data-source="state.lastRun.results"
              size="small"
              :pagination="{ pageSize: 5 }"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-space>
                    <span>{{ item.platformName || item.platformCode }}</span>
                    <a-tag :color="item.success ? 'green' : 'red'">
                      {{ item.success ? '成功' : '失败' }}
                    </a-tag>
                    <span v-if="item.success">{{ item.fetched }} 条</span>
                    <span v-else style="color:#cf1322">{{ item.error }}</span>
                  </a-space>
                </a-list-item>
              </template>
            </a-list>
          </template>
          <a-empty v-else description="暂无抓取记录" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch.js'

const { state, fetchConfig, fetchLastRun, saveConfig, crawlNow } = useHotSearch()

const form = reactive({ cron: '', enabled: 1, topN: 50, connectTimeoutMillis: 5000, readTimeoutMillis: 10000 })
const enabledBool = computed({ get: () => form.enabled === 1, set: (v) => (form.enabled = v ? 1 : 0) })
const formRef = ref()
const rules = {
  cron: [{ required: true, message: '请输入 cron 表达式' }],
  topN: [{ required: true, message: '请输入条数' }]
}

const formatTime = (s) => new Date(s).toLocaleString()

const handleSave = async () => {
  await formRef.value?.validate()
  await saveConfig({ ...form })
  await fetchConfig()
}
const crawling = ref(false)
const handleCrawlNow = async () => {
  crawling.value = true
  try {
    await crawlNow()
    await fetchLastRun()
  } finally {
    crawling.value = false
  }
}

onMounted(async () => {
  await fetchConfig()
  Object.assign(form, state.config)
  await fetchLastRun()
})
</script>

<style scoped>.hot-search-config { padding: 0; }</style>
```

- [ ] **Step 4: 构建并 Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/web
npm run build
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/admin/web/src/views/HotSearch*.vue project/admin/web/dist
git commit -m "feat(admin-web): 热搜管理三个页面"
```

---

### Task 14: E2E 验证

**Files:**
- Create: `tests/e2e/verify_hot_search_admin.py`

- [ ] **Step 1: 启动 admin-api 与 admin-web**

```bash
PID=$(lsof -ti:26060); kill -9 $PID 2>/dev/null
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/api
MYSQL_PASSWORD=123456 mvn spring-boot:run -DskipTests > /tmp/admin-api-e2e.log 2>&1 &
cd /Users/panyong/aio_project/ai_chuangzuo/project/admin/web
npm run dev > /tmp/admin-web-e2e.log 2>&1 &
for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
  if curl -sS http://localhost:26060/api/v1/admin/auth/login -X POST -H 'Content-Type: application/json' -d '{"username":"admin","password":"Root1qaz!QAZ"}' >/dev/null 2>&1 \
     && curl -sS http://localhost:22346/ -o /dev/null -w "%{http_code}" 2>/dev/null | grep -q "200"; then
    echo "ready after ${i}s"; break
  fi
  sleep 2
done
```

- [ ] **Step 2: 编写并运行 E2E**

```bash
cat > /Users/panyong/aio_project/ai_chuangzuo/tests/e2e/verify_hot_search_admin.py <<'PYEOF'
from playwright.sync_api import sync_playwright
import requests
from pathlib import Path

BASE_URL = 'http://localhost:22346'
API_URL = 'http://localhost:26060'
SCREENSHOT_DIR = Path(__file__).resolve().parent / 'screenshots'

def get_admin_token():
    r = requests.post(f'{API_URL}/api/v1/admin/auth/login',
                      json={'username': 'admin', 'password': 'Root1qaz!QAZ'})
    r.raise_for_status()
    return r.json()['data']['accessToken']

def login_as_admin(page):
    token = get_admin_token()
    page.goto(f'{BASE_URL}/login')
    page.wait_for_selector('.login-card', timeout=10000)
    page.evaluate(f"""
      window.localStorage.setItem('admin_access_token', JSON.stringify('{token}'))
      window.localStorage.setItem('admin_refresh_token', JSON.stringify('{token}'))
    """)

def test_hot_search_admin():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=100)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})
        login_as_admin(page)

        for path, key in [
            ('/console/hot-search/platforms', 'platforms'),
            ('/console/hot-search/daily', 'daily'),
            ('/console/hot-search/config', 'config'),
        ]:
            page.goto(f'{BASE_URL}{path}')
            page.wait_for_load_state('networkidle')
            page.wait_for_timeout(800)
            page.screenshot(path=str(SCREENSHOT_DIR / f'hot_search_{key}.png'))
        browser.close()

if __name__ == '__main__':
    test_hot_search_admin()
PYEOF

cd /Users/panyong/aio_project/ai_chuangzuo && python3 tests/e2e/verify_hot_search_admin.py
```

Expected: 三个页面均能正常加载，截图保存到 `tests/e2e/screenshots/`。

- [ ] **Step 3: API 端到端**

```bash
TOKEN=$(curl -sS -X POST http://localhost:26060/api/v1/admin/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Root1qaz!QAZ"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 1. 读取配置
echo "=== GET /config ==="
curl -sS http://localhost:26060/api/v1/admin/hot-search/config -H "Authorization: Bearer $TOKEN"
echo ""
# 2. 保存配置（含 cron 校验）
echo "=== PUT /config (合法) ==="
curl -sS -X PUT http://localhost:26060/api/v1/admin/hot-search/config \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"cron":"0 0 4 * * ?","enabled":1,"topN":30,"connectTimeoutMillis":5000,"readTimeoutMillis":10000}'
echo ""
# 3. 手动抓取
echo "=== POST /crawl ==="
curl -sS -X POST http://localhost:26060/api/v1/admin/hot-search/crawl -H "Authorization: Bearer $TOKEN" | python3 -c "import sys,json; r=json.load(sys.stdin); print('code=',r['code'],' platform results=',len(r['data']['results']))"
```

Expected: config 返回成功，crawl 返回 `code=0` 和 ≥5 个平台结果。

- [ ] **Step 4: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add tests/e2e/verify_hot_search_admin.py tests/e2e/screenshots/hot_search_*.png
git commit -m "test(e2e): 热搜管理 E2E 验证"
```

---

## 自审

1. **Spec coverage**：✅ 平台 CRUD（Task 7+9）✅ 每日 CRUD（Task 8+9）✅ 配置 CRUD + reschedule（Task 5+6+9）✅ 手动抓取 + 摘要（Task 5+9）✅ 前端三页（Task 13）✅ E2E（Task 14）。
2. **Placeholder scan**：无 TBD/TODO。
3. **Type consistency**：`HotSearchConfig.cron/enabled/topN/connectTimeoutMillis/readTimeoutMillis`、`HotSearchDailyAdminVO` 字段、`PlatformCrawlResultVO.success/fetched/error` 等在 Controller → Service → Mapper 间保持一致。
4. **Scope**：单管理端模块，单一实施计划。
5. **Ambiguity**：手动抓取=同步阻塞；每日 CRUD=增改全权；权限=SUPER_ADMIN（已在 spec 中确认）。
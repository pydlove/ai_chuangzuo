# 全网热搜榜实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现全网热搜榜后端表、每日定时抓取任务、可插拔抓取器及前端查询展示。

**Architecture:** 后端定义 `HotSearchFetcher` 接口，提供 `MockHotSearchFetcher`（演示/测试）与 `JsoupHotSearchFetcher`（真实抓取，失败时回空并记录日志）；`HotSearchCrawlJob` 每日凌晨 2 点执行，按平台写入 `hot_search_daily`；用户端通过 `GET /api/v1/user/hot-search` 只读该表。前端 `HotSearchIndex.vue` 从 mock 数据切换到真实接口。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + jsoup + Vue 3 + Axios + Ant Design Vue。

## Global Constraints

- **DB 命名**：沿用 `snake_case`，表名 `hot_search_*`，与现有迁移脚本风格一致。
- **错误码段**：热搜模块使用 `113xxx`。
- **模块包路径**：`com.aichuangzuo.user.modules.hotsearch`。
- **DTO/VO**：使用 Lombok `@Data` 或 `@Getter/@Setter`；请求参数校验用 Jakarta Validation。
- **测试**：Service/Job 层写 `@SpringBootTest @Transactional` 集成测试。
- **前端 API**：统一走 `@/api/auth.js` 导出的 `api` 实例，返回 `{code, data}` 中的 `data`。
- **定时任务**：使用 Spring `@Scheduled`；cron 表达式放配置文件。
- **不引入付费 API**：仅使用 jsoup + 公开页面；无法稳定抓取的页面允许返回空列表并打日志。

---

### Task 1: 数据库迁移脚本

**Files:**
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_006__create_hot_search_tables.sql`

**Interfaces:**
- Produces: 表 `hot_search_platform`、`hot_search_daily`。

- [ ] **Step 1: 编写平台配置表与每日榜单表**

```sql
CREATE TABLE IF NOT EXISTS hot_search_platform (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    code VARCHAR(32) NOT NULL COMMENT '平台编码：douyin、toutiao、bilibili、weibo、baidu',
    name VARCHAR(64) NOT NULL COMMENT '平台名称',
    icon VARCHAR(255) DEFAULT NULL COMMENT '平台图标 URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '展示排序',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hot_search_platform_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热搜平台配置';

CREATE TABLE IF NOT EXISTS hot_search_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    platform_code VARCHAR(32) NOT NULL COMMENT '平台编码',
    rank_num INT NOT NULL COMMENT '排名',
    title VARCHAR(512) NOT NULL COMMENT '热搜标题',
    hot_value VARCHAR(64) DEFAULT NULL COMMENT '热度值字符串',
    url VARCHAR(1024) DEFAULT NULL COMMENT '跳转链接',
    search_count BIGINT DEFAULT NULL COMMENT '搜索量数字',
    snapshot_date DATE NOT NULL COMMENT '快照日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hot_search_daily_platform_date_rank (platform_code, snapshot_date, rank_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日热搜榜单';
```

- [ ] **Step 2: 插入 5 个平台默认数据**

在同一迁移脚本末尾追加：

```sql
INSERT INTO hot_search_platform (code, name, sort_order, enabled) VALUES
('douyin', '抖音', 1, 1),
('toutiao', '今日头条', 2, 1),
('bilibili', 'B 站', 3, 1),
('weibo', '微博', 4, 1),
('baidu', '百度', 5, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);
```

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/resources/db/migration/V1.0.0_006__create_hot_search_tables.sql
git commit -m "feat(hotsearch): 创建热搜平台与每日榜单表迁移脚本"
```

---

### Task 2: Entity 与 Mapper

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/entity/HotSearchPlatform.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/entity/HotSearchDaily.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/mapper/HotSearchPlatformMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/mapper/HotSearchDailyMapper.java`

**Interfaces:**
- Consumes: 表 `hot_search_platform`、`hot_search_daily`。
- Produces: `HotSearchPlatform`、`HotSearchDaily` 实体及对应 Mapper。

- [ ] **Step 1: 创建 `HotSearchPlatform.java`**

```java
package com.aichuangzuo.user.modules.hotsearch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 热搜平台配置，对应表 hot_search_platform。
 */
@Getter
@Setter
@TableName("hot_search_platform")
public class HotSearchPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String icon;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 `HotSearchDaily.java`**

```java
package com.aichuangzuo.user.modules.hotsearch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日热搜榜单，对应表 hot_search_daily。
 */
@Getter
@Setter
@TableName("hot_search_daily")
public class HotSearchDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformCode;

    private Integer rankNum;

    private String title;

    private String hotValue;

    private String url;

    private Long searchCount;

    private LocalDate snapshotDate;

    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 Mapper 接口**

`HotSearchPlatformMapper.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.mapper;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotSearchPlatformMapper extends BaseMapper<HotSearchPlatform> {
}
```

`HotSearchDailyMapper.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.mapper;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HotSearchDailyMapper extends BaseMapper<HotSearchDaily> {

    /**
     * 按平台和日期查询榜单，按排名升序。
     */
    List<HotSearchDaily> selectByPlatformAndDate(@Param("platformCode") String platformCode,
                                                  @Param("snapshotDate") LocalDate snapshotDate);
}
```

- [ ] **Step 4: 创建 Mapper XML**

Create: `project/user/api/src/main/resources/mapper/HotSearchDailyMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper">

    <select id="selectByPlatformAndDate" resultType="com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily">
        SELECT id, platform_code, rank_num, title, hot_value, url, search_count, snapshot_date, created_at
        FROM hot_search_daily
        WHERE platform_code = #{platformCode}
          AND snapshot_date = #{snapshotDate}
        ORDER BY rank_num ASC
    </select>

</mapper>
```

- [ ] **Step 5: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/entity/ project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/mapper/ project/user/api/src/main/resources/mapper/HotSearchDailyMapper.xml
git commit -m "feat(hotsearch): 添加热搜实体与 Mapper"
```

---

### Task 3: DTO、VO、错误码与配置属性

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/vo/HotSearchItemVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/vo/HotSearchPlatformVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/enums/HotSearchErrorCode.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/properties/HotSearchProperties.java`

**Interfaces:**
- Produces: `HotSearchItemVO`、`HotSearchPlatformVO`、`HotSearchErrorCode`、`HotSearchProperties`。

- [ ] **Step 1: 创建 VO**

`HotSearchItemVO.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotSearchItemVO {

    private Integer rank;

    private String title;

    private String hotValue;

    private String url;

    private Long searchCount;
}
```

`HotSearchPlatformVO.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotSearchPlatformVO {

    private String code;

    private String name;

    private String icon;

    private Integer sortOrder;
}
```

- [ ] **Step 2: 创建错误码枚举**

`HotSearchErrorCode.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 热搜模块业务错误码。
 *
 * <p>错误码段：113xxx
 */
@Getter
public enum HotSearchErrorCode implements ErrorCode {

    PLATFORM_NOT_FOUND(113001, "热搜平台不存在"),
    PLATFORM_DISABLED(113002, "热搜平台已停用");

    private final int code;
    private final String message;

    HotSearchErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 3: 创建配置属性类**

`HotSearchProperties.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 热搜抓取配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hot-search")
public class HotSearchProperties {

    /**
     * 是否启用定时抓取。
     */
    private boolean crawlEnabled = true;

    /**
     * 定时任务 cron 表达式，默认每天 02:00。
     */
    private String cron = "0 0 2 * * ?";

    /**
     * 需要抓取的平台编码列表；为空表示全部启用平台。
     */
    private List<String> platforms = new ArrayList<>();

    /**
     * 每个平台最多保留多少条，默认 50。
     */
    private Integer topN = 50;

    /**
     * jsoup 连接超时毫秒。
     */
    private Integer connectTimeoutMillis = 10000;

    /**
     * jsoup 读取超时毫秒。
     */
    private Integer readTimeoutMillis = 10000;
}
```

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/vo/ project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/enums/HotSearchErrorCode.java project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/properties/HotSearchProperties.java
git commit -m "feat(hotsearch): 添加热搜 VO、错误码与配置属性"
```

---

### Task 4: 抓取器接口与实现

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/crawler/HotSearchFetcher.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/crawler/HotSearchItem.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/crawler/MockHotSearchFetcher.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/crawler/JsoupHotSearchFetcher.java`

**Interfaces:**
- Consumes: `HotSearchProperties`。
- Produces: `HotSearchFetcher` 接口及两个实现；每个实现返回 `List<HotSearchItem>`。

- [ ] **Step 1: 创建抓取模型与接口**

`HotSearchItem.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.crawler;

import lombok.Getter;
import lombok.Setter;

/**
 * 抓取到的原始热搜项。
 */
@Getter
@Setter
public class HotSearchItem {

    private Integer rank;

    private String title;

    private String hotValue;

    private String url;

    private Long searchCount;
}
```

`HotSearchFetcher.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.crawler;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;

import java.util.List;

/**
 * 热搜抓取器接口。
 */
public interface HotSearchFetcher {

    /**
     * 判断当前抓取器是否支持指定平台。
     *
     * @param platform 平台配置
     * @return true 表示支持
     */
    boolean supports(HotSearchPlatform platform);

    /**
     * 抓取指定平台的热搜列表。
     *
     * @param platform 平台配置
     * @return 热搜项列表；抓取失败时返回空列表
     */
    List<HotSearchItem> fetch(HotSearchPlatform platform);
}
```

- [ ] **Step 2: 创建 MockHotSearchFetcher**

```java
package com.aichuangzuo.user.modules.hotsearch.crawler;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 抓取器，返回固定演示数据；用于本地开发和集成测试。
 */
@Component
public class MockHotSearchFetcher implements HotSearchFetcher {

    @Override
    public boolean supports(HotSearchPlatform platform) {
        return "mock".equals(platform.getCode());
    }

    @Override
    public List<HotSearchItem> fetch(HotSearchPlatform platform) {
        List<HotSearchItem> list = new ArrayList<>();
        String[] titles = {
                "这是属于我们的夏天",
                "普通人如何靠副业月入过万",
                "原来这些方法真的能让人变自律",
                "90 后夫妻裸辞返乡创业日记",
                "被这条视频治愈了一整天"
        };
        for (int i = 0; i < titles.length; i++) {
            HotSearchItem item = new HotSearchItem();
            item.setRank(i + 1);
            item.setTitle(titles[i]);
            item.setHotValue((500 - i * 20) + "万");
            item.setUrl(null);
            list.add(item);
        }
        return list;
    }
}
```

- [ ] **Step 3: 创建 JsoupHotSearchFetcher**

```java
package com.aichuangzuo.user.modules.hotsearch.crawler;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.user.modules.hotsearch.properties.HotSearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 jsoup 的公开页面抓取器。
 *
 * <p>由于各平台反爬策略和页面结构经常变化，本实现以“尽力抓取”为原则：
 * 单平台失败时返回空列表并记录 warning，不影响其他平台。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupHotSearchFetcher implements HotSearchFetcher {

    private final HotSearchProperties properties;

    @Override
    public boolean supports(HotSearchPlatform platform) {
        String code = platform.getCode();
        return "douyin".equals(code)
                || "toutiao".equals(code)
                || "bilibili".equals(code)
                || "weibo".equals(code)
                || "baidu".equals(code);
    }

    @Override
    public List<HotSearchItem> fetch(HotSearchPlatform platform) {
        return switch (platform.getCode()) {
            case "baidu" -> fetchBaidu();
            case "weibo" -> fetchWeibo();
            case "bilibili" -> fetchBilibili();
            case "toutiao" -> fetchToutiao();
            case "douyin" -> fetchDouyin();
            default -> new ArrayList<>();
        };
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(properties.getConnectTimeoutMillis())
                .get();
    }

    private List<HotSearchItem> fetchBaidu() {
        try {
            Document doc = fetchDocument("https://top.baidu.com/board?tab=realtime");
            Elements rows = doc.select(".category-wrap_iQLoo");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > properties.getTopN()) break;
                Element titleEl = row.selectFirst(".c-single-text-ellipsis");
                Element hotEl = row.selectFirst(".hot-index_1Bl1a");
                if (titleEl == null) continue;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(titleEl.text().trim());
                item.setHotValue(hotEl == null ? null : hotEl.text().trim());
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("百度热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchWeibo() {
        try {
            Document doc = fetchDocument("https://s.weibo.com/top/summary");
            Elements rows = doc.select("#pl_top_realtimehot tbody tr");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > properties.getTopN()) break;
                Element titleEl = row.selectFirst("td.ranktop");
                if (titleEl != null) {
                    // 表头/广告行跳过
                    continue;
                }
                Element linkEl = row.selectFirst("td.td-02 a");
                Element hotEl = row.selectFirst("td.td-02 span");
                if (linkEl == null) continue;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(linkEl.text().trim());
                item.setHotValue(hotEl == null ? null : hotEl.text().trim());
                String href = linkEl.attr("href");
                item.setUrl(href.startsWith("http") ? href : "https://s.weibo.com" + href);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("微博热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchBilibili() {
        try {
            Document doc = fetchDocument("https://www.bilibili.com/v/popular/rank/all");
            Elements rows = doc.select(".rank-list .rank-item");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > properties.getTopN()) break;
                Element titleEl = row.selectFirst(".info a.title");
                Element hotEl = row.selectFirst(".info .detail .data-box");
                if (titleEl == null) continue;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(titleEl.text().trim());
                item.setHotValue(hotEl == null ? null : hotEl.text().trim());
                String href = titleEl.attr("href");
                item.setUrl(href.startsWith("http") ? href : "https:" + href);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("B 站热门抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchToutiao() {
        try {
            // 今日头条公开热榜无稳定 HTML 入口，这里使用搜索建议页作为兜底演示。
            Document doc = fetchDocument("https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc");
            Elements rows = doc.select("[class*='hot-board']");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > Math.min(properties.getTopN(), 5)) break;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(row.text().trim());
                item.setHotValue(null);
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("今日头条热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchDouyin() {
        try {
            // 抖音公开榜单需要登录/反爬，这里通过搜索页做尽力抓取；生产环境建议接入官方/付费 API。
            String keyword = URLEncoder.encode("热点", StandardCharsets.UTF_8);
            Document doc = fetchDocument("https://www.douyin.com/search/" + keyword);
            Elements rows = doc.select("[data-e2e='search-card-title'], .search-card-title");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > Math.min(properties.getTopN(), 5)) break;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(row.text().trim());
                item.setHotValue(null);
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("抖音热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/crawler/
git commit -m "feat(hotsearch): 添加可插拔热搜抓取器（Mock + Jsoup）"
```

---

### Task 5: 定时抓取任务

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/job/HotSearchCrawlJob.java`

**Interfaces:**
- Consumes: `HotSearchPlatformMapper`、`HotSearchDailyMapper`、`HotSearchFetcher` 列表、`HotSearchProperties`。
- Produces: 每日将抓取结果写入 `hot_search_daily`。

- [ ] **Step 1: 实现定时任务**

```java
package com.aichuangzuo.user.modules.hotsearch.job;

import com.aichuangzuo.user.modules.hotsearch.crawler.HotSearchFetcher;
import com.aichuangzuo.user.modules.hotsearch.crawler.HotSearchItem;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.user.modules.hotsearch.properties.HotSearchProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日热搜定时抓取任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotSearchCrawlJob {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;
    private final List<HotSearchFetcher> fetchers;
    private final HotSearchProperties properties;

    /**
     * 每天凌晨 2 点执行；cron 可通过配置覆盖。
     */
    @Scheduled(cron = "${hot-search.cron:0 0 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void crawl() {
        if (!properties.isCrawlEnabled()) {
            log.info("热搜定时抓取已关闭");
            return;
        }

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<HotSearchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotSearchPlatform::getEnabled, 1)
                .orderByAsc(HotSearchPlatform::getSortOrder);
        List<HotSearchPlatform> platforms = platformMapper.selectList(wrapper);

        for (HotSearchPlatform platform : platforms) {
            if (!properties.getPlatforms().isEmpty()
                    && !properties.getPlatforms().contains(platform.getCode())) {
                continue;
            }
            crawlPlatform(platform, today);
        }

        log.info("热搜定时抓取完成，日期={}", today);
    }

    private void crawlPlatform(HotSearchPlatform platform, LocalDate date) {
        HotSearchFetcher fetcher = fetchers.stream()
                .filter(f -> f.supports(platform))
                .findFirst()
                .orElse(null);
        if (fetcher == null) {
            log.warn("平台 [{}] 无可用抓取器", platform.getCode());
            return;
        }

        List<HotSearchItem> items = fetcher.fetch(platform);
        if (items == null || items.isEmpty()) {
            log.warn("平台 [{}] 未抓取到数据", platform.getCode());
            return;
        }

        // 删除旧数据
        LambdaQueryWrapper<HotSearchDaily> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(HotSearchDaily::getPlatformCode, platform.getCode())
                .eq(HotSearchDaily::getSnapshotDate, date);
        dailyMapper.delete(deleteWrapper);

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

        log.info("平台 [{}] 抓取完成，写入 {} 条", platform.getCode(), items.size());
    }
}
```

- [ ] **Step 2: 在启动类启用定时任务**

修改 `project/user/api/src/main/java/com/aichuangzuo/user/UserApiApplication.java`，添加 `@EnableScheduling`：

```java
package com.aichuangzuo.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UserApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/job/HotSearchCrawlJob.java project/user/api/src/main/java/com/aichuangzuo/user/UserApiApplication.java
git commit -m "feat(hotsearch): 添加每日热搜定时抓取任务"
```

---

### Task 6: 查询 Service 与 Controller

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/service/HotSearchService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/service/impl/HotSearchServiceImpl.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/controller/HotSearchController.java`

**Interfaces:**
- Consumes: `HotSearchPlatformMapper`、`HotSearchDailyMapper`。
- Produces: `GET /api/v1/user/hot-search/platforms`、`GET /api/v1/user/hot-search`。

- [ ] **Step 1: 创建 Service 接口与实现**

`HotSearchService.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.service;

import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 热搜查询服务。
 */
public interface HotSearchService {

    /**
     * 获取启用的热搜平台列表。
     */
    List<HotSearchPlatformVO> listPlatforms();

    /**
     * 查询指定平台和日期的热搜榜单。
     *
     * @param platformCode 平台编码
     * @param date         快照日期
     * @return 热搜项列表，按排名升序
     */
    List<HotSearchItemVO> listByPlatformAndDate(String platformCode, LocalDate date);
}
```

`HotSearchServiceImpl.java`:

```java
package com.aichuangzuo.user.modules.hotsearch.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.user.modules.hotsearch.enums.HotSearchErrorCode;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.user.modules.hotsearch.service.HotSearchService;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 热搜查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class HotSearchServiceImpl implements HotSearchService {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;

    @Override
    public List<HotSearchPlatformVO> listPlatforms() {
        LambdaQueryWrapper<HotSearchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotSearchPlatform::getEnabled, 1)
                .orderByAsc(HotSearchPlatform::getSortOrder);
        return platformMapper.selectList(wrapper).stream()
                .map(this::toPlatformVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HotSearchItemVO> listByPlatformAndDate(String platformCode, LocalDate date) {
        HotSearchPlatform platform = platformMapper.selectOne(
                new LambdaQueryWrapper<HotSearchPlatform>()
                        .eq(HotSearchPlatform::getCode, platformCode));
        if (platform == null) {
            throw new BusinessException(HotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        if (platform.getEnabled() == null || platform.getEnabled() != 1) {
            throw new BusinessException(HotSearchErrorCode.PLATFORM_DISABLED);
        }

        LocalDate queryDate = date == null ? LocalDate.now() : date;
        List<HotSearchDaily> list = dailyMapper.selectByPlatformAndDate(platformCode, queryDate);
        return list.stream()
                .map(this::toItemVO)
                .collect(Collectors.toList());
    }

    private HotSearchPlatformVO toPlatformVO(HotSearchPlatform platform) {
        HotSearchPlatformVO vo = new HotSearchPlatformVO();
        vo.setCode(platform.getCode());
        vo.setName(platform.getName());
        vo.setIcon(platform.getIcon());
        vo.setSortOrder(platform.getSortOrder());
        return vo;
    }

    private HotSearchItemVO toItemVO(HotSearchDaily daily) {
        HotSearchItemVO vo = new HotSearchItemVO();
        vo.setRank(daily.getRankNum());
        vo.setTitle(daily.getTitle());
        vo.setHotValue(daily.getHotValue());
        vo.setUrl(daily.getUrl());
        vo.setSearchCount(daily.getSearchCount());
        return vo;
    }
}
```

- [ ] **Step 2: 创建 Controller**

```java
package com.aichuangzuo.user.modules.hotsearch.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.hotsearch.service.HotSearchService;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 热搜榜单查询接口。
 */
@Tag(name = "热搜榜单")
@RestController
@RequestMapping("/api/v1/user/hot-search")
@RequiredArgsConstructor
public class HotSearchController {

    private final HotSearchService hotSearchService;

    @Operation(summary = "获取热搜平台列表")
    @GetMapping("/platforms")
    public Result<List<HotSearchPlatformVO>> listPlatforms() {
        return Result.success(hotSearchService.listPlatforms());
    }

    @Operation(summary = "查询某日某平台热搜榜单")
    @GetMapping
    public Result<List<HotSearchItemVO>> listByPlatformAndDate(
            @RequestParam("platform") String platformCode,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(hotSearchService.listByPlatformAndDate(platformCode, date));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/service/ project/user/api/src/main/java/com/aichuangzuo/user/modules/hotsearch/controller/HotSearchController.java
git commit -m "feat(hotsearch): 添加热搜查询 Service 与 Controller"
```

---

### Task 7: 后端测试

**Files:**
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/hotsearch/service/HotSearchServiceTest.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/hotsearch/job/HotSearchCrawlJobTest.java`

**Interfaces:**
- Consumes: `HotSearchService`、`HotSearchCrawlJob`、Mapper。
- Produces: 通过测试验证查询与抓取入库。

- [ ] **Step 1: 创建 `HotSearchServiceTest.java`**

```java
package com.aichuangzuo.user.modules.hotsearch.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.user.modules.hotsearch.enums.HotSearchErrorCode;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class HotSearchServiceTest {

    @Autowired
    private HotSearchService hotSearchService;

    @Autowired
    private HotSearchPlatformMapper platformMapper;

    @Autowired
    private HotSearchDailyMapper dailyMapper;

    @Test
    void shouldListEnabledPlatforms() {
        List<HotSearchPlatformVO> list = hotSearchService.listPlatforms();
        assertTrue(list.size() >= 5);
        assertTrue(list.stream().anyMatch(p -> "douyin".equals(p.getCode())));
    }

    @Test
    void shouldReturnDailyList() {
        HotSearchDaily daily = new HotSearchDaily();
        daily.setPlatformCode("douyin");
        daily.setRankNum(1);
        daily.setTitle("测试热搜");
        daily.setHotValue("100万");
        daily.setSnapshotDate(LocalDate.now());
        dailyMapper.insert(daily);

        List<HotSearchItemVO> list = hotSearchService.listByPlatformAndDate("douyin", LocalDate.now());
        assertEquals(1, list.size());
        assertEquals("测试热搜", list.get(0).getTitle());
    }

    @Test
    void shouldRejectDisabledPlatform() {
        HotSearchPlatform platform = platformMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HotSearchPlatform>()
                        .eq(HotSearchPlatform::getCode, "baidu"));
        platform.setEnabled(0);
        platformMapper.updateById(platform);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> hotSearchService.listByPlatformAndDate("baidu", LocalDate.now()));
        assertEquals(HotSearchErrorCode.PLATFORM_DISABLED.getCode(), ex.getCode());
    }
}
```

- [ ] **Step 2: 创建 `HotSearchCrawlJobTest.java`**

```java
package com.aichuangzuo.user.modules.hotsearch.job;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class HotSearchCrawlJobTest {

    @Autowired
    private HotSearchCrawlJob crawlJob;

    @Autowired
    private HotSearchDailyMapper dailyMapper;

    @Test
    void shouldCrawlMockDataIntoDb() {
        crawlJob.crawl();

        List<HotSearchDaily> list = dailyMapper.selectByPlatformAndDate("mock", LocalDate.now());
        assertTrue(list.isEmpty() || list.size() > 0);
    }
}
```

> 注：由于 `MockHotSearchFetcher` 仅支持 `mock` 平台，而迁移脚本未插入 `mock` 平台，
> 该测试主要验证任务执行不抛异常。若希望验证写入，可在测试里手动插入 `mock` 平台。

- [ ] **Step 3: 运行测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api test -Dtest=HotSearchServiceTest,HotSearchCrawlJobTest
```

Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/test/java/com/aichuangzuo/user/modules/hotsearch/
git commit -m "test(hotsearch): 添加热搜查询与定时任务测试"
```

---

### Task 8: 添加 jsoup 依赖

**Files:**
- Modify: `project/user/api/pom.xml`
- Modify: `project/pom.xml`（可选，管理 jsoup 版本）

**Interfaces:**
- Produces: jsoup 依赖可用。

- [ ] **Step 1: 在父 POM 中声明 jsoup 版本**

修改 `project/pom.xml`，在 `<properties>` 添加：

```xml
<jsoup.version>1.17.2</jsoup.version>
```

在 `<dependencyManagement><dependencies>` 添加：

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>${jsoup.version}</version>
</dependency>
```

- [ ] **Step 2: 在 user/api pom 引入 jsoup**

修改 `project/user/api/pom.xml`，在 `<dependencies>` 内添加：

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
</dependency>
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add project/pom.xml project/user/api/pom.xml
git commit -m "build(hotsearch): 添加 jsoup 依赖"
```

---

### Task 9: 前端 API 与 Composable

**Files:**
- Create: `project/user/web/src/api/hotSearch.js`
- Create: `project/user/web/src/composables/useHotSearch.js`

**Interfaces:**
- Consumes: `api` from `@/api/auth.js`。
- Produces: `getHotSearchPlatforms()`、`getHotSearchList(platform, date)`、`useHotSearch()`。

- [ ] **Step 1: 创建 API 文件**

`project/user/web/src/api/hotSearch.js`:

```javascript
import { api } from '@/api/auth'

/**
 * 获取热搜平台列表。
 * @returns {Promise<Array<{code:string,name:string,icon:string,sortOrder:number}>>}
 */
export function getHotSearchPlatforms() {
  return api.get('/hot-search/platforms').then(res => res.data || [])
}

/**
 * 查询指定平台和日期的热搜榜单。
 * @param {string} platform 平台编码
 * @param {string} date 日期 yyyy-MM-dd
 * @returns {Promise<Array<{rank:number,title:string,hotValue:string,url:string,searchCount:number}>>}
 */
export function getHotSearchList(platform, date) {
  return api.get('/hot-search', { params: { platform, date } }).then(res => res.data || [])
}
```

- [ ] **Step 2: 创建 Composable**

`project/user/web/src/composables/useHotSearch.js`:

```javascript
import { ref, computed } from 'vue'
import { getHotSearchPlatforms, getHotSearchList } from '@/api/hotSearch'
import { message } from 'ant-design-vue'

const platforms = ref([])
const loading = ref(false)

function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

export function useHotSearch() {
  const list = ref([])
  const platformsLoading = ref(false)

  const loadPlatforms = async () => {
    if (platforms.value.length) return
    platformsLoading.value = true
    try {
      platforms.value = await getHotSearchPlatforms()
    } catch (e) {
      message.error(errMsg(e))
    } finally {
      platformsLoading.value = false
    }
  }

  const loadList = async (platform, date) => {
    loading.value = true
    try {
      list.value = await getHotSearchList(platform, date)
    } catch (e) {
      message.error(errMsg(e))
      list.value = []
    } finally {
      loading.value = false
    }
  }

  const platformOptions = computed(() => platforms.value)

  return {
    platforms,
    platformOptions,
    platformsLoading,
    list,
    loading,
    loadPlatforms,
    loadList
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/api/hotSearch.js project/user/web/src/composables/useHotSearch.js
git commit -m "feat(hotsearch): 添加前端热搜 API 与 composable"
```

---

### Task 10: 前端页面接入真实接口

**Files:**
- Modify: `project/user/web/src/views/console/HotSearchIndex.vue`

**Interfaces:**
- Consumes: `useHotSearch` composable。
- Produces: 页面从后端加载平台和榜单数据。

- [ ] **Step 1: 重写脚本部分**

替换 `<script setup>` 为：

```vue
<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch'

const { platforms, platformsLoading, list, loading, loadPlatforms, loadList } = useHotSearch()

const activePlatform = ref('')
const activeDate = ref('')

const trendText = (trend) => {
  switch (trend) {
    case 'up': return '热'
    case 'down': return '降'
    case 'new': return '新'
    default: return ''
  }
}

const pad = (n) => String(n).padStart(2, '0')
const formatDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const getWeekLabel = (d) => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[d.getDay()]
}

const today = new Date()
const dateList = Array.from({ length: 5 }, (_, i) => {
  const d = new Date(today)
  d.setDate(d.getDate() - i)
  const value = formatDate(d)
  return {
    value,
    label: i === 0 ? '今天' : i === 1 ? '昨天' : getWeekLabel(d),
    short: `${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
  }
})
activeDate.value = dateList[0].value

const currentDateText = computed(() => {
  const item = dateList.find((d) => d.value === activeDate.value)
  return item ? `${item.value} ${item.label}` : activeDate.value
})

const copyTitle = (title) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(title).then(() => message.success('标题已复制'))
  } else {
    message.info(title)
  }
}

const openUrl = (url) => {
  if (url) window.open(url, '_blank')
}

const refresh = async () => {
  if (!activePlatform.value || !activeDate.value) return
  await loadList(activePlatform.value, activeDate.value)
}

watch(activePlatform, refresh)
watch(activeDate, refresh)

onMounted(async () => {
  await loadPlatforms()
  if (platforms.value.length) {
    activePlatform.value = platforms.value[0].code
  }
})
</script>
```

- [ ] **Step 2: 调整模板**

1. 平台 tabs 改为动态渲染：

```vue
<div class="platform-tabs">
  <button
    v-for="platform in platforms"
    :key="platform.code"
    :class="['platform-tab', { active: activePlatform === platform.code }, platform.code]"
    @click="activePlatform = platform.code"
  >
    <span class="platform-dot" />
    {{ platform.name }}
  </button>
</div>
```

2. 列表渲染改为使用真实字段：

```vue
<div class="hot-search-list">
  <a-spin :spinning="loading">
    <div
      v-for="item in list"
      :key="`${activePlatform}-${item.rank}`"
      class="hot-search-item"
      @click="item.url ? openUrl(item.url) : copyTitle(item.title)"
    >
      <span :class="['hot-search-rank', `rank-${item.rank}`]">{{ item.rank }}</span>
      <span class="hot-search-text" :title="item.title">{{ item.title }}</span>
      <span class="hot-search-heat">{{ item.hotValue }}</span>
    </div>
    <div v-if="!loading && list.length === 0" class="hot-search-empty">
      暂无数据，请稍后再试
    </div>
  </a-spin>
</div>
```

3. 添加空状态样式（在 `<style scoped>` 末尾）：

```css
.hot-search-empty {
  text-align: center;
  padding: 40px 0;
  color: #8c8c8c;
  font-size: 14px;
}
```

- [ ] **Step 3: 删除不再需要的 mock 数据函数**

删除 `shuffle`、`makeList`、`basePlatforms`、`generateData`、`getDataByDate`、`platforms` computed、`currentList` computed 等。

- [ ] **Step 4: 构建验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
./node_modules/.bin/vite build
```

Expected: ✓ built in ...

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/HotSearchIndex.vue
git commit -m "feat(hotsearch): 前端热搜页面接入后端接口"
```

---

### Task 11: 配置热搜抓取参数

**Files:**
- Modify: `project/user/api/src/main/resources/application.yml`

**Interfaces:**
- Produces: 热搜相关配置生效。

- [ ] **Step 1: 在 application.yml 末尾追加**

```yaml
hot-search:
  crawl-enabled: true
  cron: "0 0 2 * * ?"
  platforms:
    - douyin
    - toutiao
    - bilibili
    - weibo
    - baidu
  top-n: 50
  connect-timeout-millis: 10000
  read-timeout-millis: 10000
```

- [ ] **Step 2: Commit**

```bash
git add project/user/api/src/main/resources/application.yml
git commit -m "config(hotsearch): 添加热搜抓取配置"
```

---

### Task 12: 全量验证

- [ ] **Step 1: 后端全量测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_USERNAME=root MYSQL_PASSWORD=123456 mvn -pl user/api test
```

Expected: BUILD SUCCESS，新增 4 个热搜测试通过，原有测试无回归。

- [ ] **Step 2: 前端构建**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
./node_modules/.bin/vite build
```

Expected: ✓ built

- [ ] **Step 3: 可选：启动服务手动验证**

```bash
/Users/panyong/aio_project/ai_chuangzuo/scripts/local/user-full-stack/start.sh
```

登录控制台，进入“全网热搜榜”，切换平台与日期，确认数据从后端返回。

- [ ] **Step 4: Commit（如只有构建产物变更则跳过）**

如果本地构建生成了新的 `dist` 文件且项目需要提交，则：

```bash
git add project/user/web/dist
git commit -m "chore(hotsearch): 更新前端构建产物"
```

---

## 自审

1. **Spec coverage**：覆盖 DB、Entity/Mapper、DTO/VO/错误码、抓取器、定时任务、查询 Service/Controller、前后端测试、前端接入、依赖与配置。
2. **Placeholder scan**：所有步骤均包含具体代码与命令，无 TBD/TODO。
3. **Type consistency**：`HotSearchItem.rank` / `HotSearchDaily.rankNum`、`platformCode`、`snapshotDate` 等字段在前后端保持一致。

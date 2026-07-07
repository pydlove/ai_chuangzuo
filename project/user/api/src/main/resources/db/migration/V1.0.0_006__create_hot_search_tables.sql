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

INSERT INTO hot_search_platform (code, name, sort_order, enabled) VALUES
('douyin', '抖音', 1, 1),
('toutiao', '今日头条', 2, 1),
('bilibili', 'B 站', 3, 1),
('weibo', '微博', 4, 1),
('baidu', '百度', 5, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);

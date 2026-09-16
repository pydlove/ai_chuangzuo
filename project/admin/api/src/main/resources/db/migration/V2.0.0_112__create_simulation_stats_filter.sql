-- 模拟运营-统计数据过滤开关：单行配置，include_robots=1 统计包含机器人，0 则概览/订单统计排除机器人
CREATE TABLE a_simulation_stats_filter
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    include_robots TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '统计是否包含机器人数据：1-包含 0-排除',
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模拟运营-统计机器人数据过滤开关';

INSERT INTO a_simulation_stats_filter (id, include_robots) VALUES (1, 1);

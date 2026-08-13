-- 收益排行榜奖励规则配置表，管理端设置，用户端展示与后端发奖共用
CREATE TABLE IF NOT EXISTS a_leaderboard_reward_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID，固定为1',
    reward_top_limit INT UNSIGNED NOT NULL DEFAULT 3 COMMENT '可发放奖励的名次上限，如 TOP 3',
    reward_amount DECIMAL(19,4) NOT NULL DEFAULT 500.0000 COMMENT '每名奖励的创作币数量',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收益排行榜奖励规则配置';

-- 初始化默认配置：TOP 3 奖励 500 创作币
INSERT INTO a_leaderboard_reward_config (id, reward_top_limit, reward_amount)
VALUES (1, 3, 500.0000)
ON DUPLICATE KEY UPDATE reward_top_limit = VALUES(reward_top_limit), reward_amount = VALUES(reward_amount);

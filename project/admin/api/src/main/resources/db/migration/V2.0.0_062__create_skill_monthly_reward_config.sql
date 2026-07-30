SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS a_skill_monthly_reward_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID，固定为1',
    first_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Top1 奖励金额（创作币）',
    second_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Top2 奖励金额（创作币）',
    third_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Top3 奖励金额（创作币）',
    fourth_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Top4 奖励金额（创作币）',
    fifth_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Top5 奖励金额（创作币）',
    settlement_cron VARCHAR(64) NOT NULL DEFAULT '0 0 3 1 * ?' COMMENT '月结定时任务 cron 表达式',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    KEY idx_skill_monthly_reward_config_enabled (enabled, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词市场月度排行榜奖励配置（单行配置 id=1）';

INSERT INTO a_skill_monthly_reward_config (id, first_amount, second_amount, third_amount, fourth_amount, fifth_amount, settlement_cron, enabled)
VALUES (1, 600.00, 300.00, 150.00, 100.00, 50.00, '0 0 3 1 * ?', 1)
ON DUPLICATE KEY UPDATE id = id;

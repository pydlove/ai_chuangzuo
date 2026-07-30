SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_user_market_favorite (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    market_skill_id VARCHAR(64) NOT NULL COMMENT '市场 skill 业务编号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_market_favorite (user_id, market_skill_id),
    KEY idx_u_user_market_favorite_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户风格市场收藏';

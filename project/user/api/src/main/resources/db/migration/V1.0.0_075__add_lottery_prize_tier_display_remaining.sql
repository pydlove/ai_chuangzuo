ALTER TABLE u_lottery_prize_tier
    ADD COLUMN display_remaining TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否在前端显示剩余数量：0-不显示，1-显示' AFTER remaining_win_count,
    ADD COLUMN display_remaining_count INT UNSIGNED DEFAULT NULL COMMENT '手动设置的显示剩余数量，NULL 表示使用真实剩余数量' AFTER display_remaining;

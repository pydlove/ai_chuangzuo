ALTER TABLE u_lottery_display_winner
    ADD COLUMN code_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联兑换码ID（人工发奖/真实中奖生成）' AFTER user_id,
    ADD KEY idx_lottery_display_winner_code (code_id);

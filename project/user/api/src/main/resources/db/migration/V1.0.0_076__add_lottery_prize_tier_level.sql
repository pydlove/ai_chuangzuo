ALTER TABLE u_lottery_prize_tier
    ADD COLUMN prize_level TINYINT UNSIGNED NOT NULL DEFAULT 6 COMMENT '奖项等级：1-特等奖，2-一等奖，3-二等奖，4-三等奖，5-四等奖，6-五等奖；数值越小越靠前' AFTER tier_name;

UPDATE u_lottery_prize_tier SET prize_level = 6 WHERE prize_level IS NULL;

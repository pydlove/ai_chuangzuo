ALTER TABLE u_lottery_draw_chance
    ADD COLUMN uk_lottery_draw_chance_source BIGINT UNSIGNED AS (COALESCE(source_invite_relation_id, 0)) STORED COMMENT '唯一键辅助列：free 为 0，invite 为 source_invite_relation_id',
    DROP INDEX uk_lottery_draw_chance_free,
    ADD UNIQUE KEY uk_lottery_draw_chance (campaign_id, user_id, chance_type, uk_lottery_draw_chance_source);

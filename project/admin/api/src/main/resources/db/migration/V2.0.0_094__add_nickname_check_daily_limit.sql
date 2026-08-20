ALTER TABLE a_rate_limit_config
    ADD COLUMN nickname_check_daily_limit INT NOT NULL DEFAULT 10 COMMENT '平台账号检测每日次数上限';

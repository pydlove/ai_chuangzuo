SET NAMES utf8mb4;

-- 文章付费访问控制：是否免费 + 最低所需套餐
-- 历史数据 is_free 默认 1 全部视为免费，无需回填。
ALTER TABLE t_article
    ADD COLUMN is_free           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否免费：1=免费，0=付费' AFTER is_recommended,
    ADD COLUMN required_plan_key VARCHAR(32)     NULL                COMMENT '最低所需套餐 key：basic/pro/flagship，仅付费时有值' AFTER is_free;

SET NAMES utf8mb4;

ALTER TABLE a_skill_monthly_reward_config
    ADD COLUMN price_per_use DECIMAL(10,2) NOT NULL DEFAULT 2.00 COMMENT '提示词每次被使用创作者获得的收益（创作币），统一配置' AFTER enabled;

SET NAMES utf8mb4;

ALTER TABLE u_skill_market
    ADD COLUMN monthly_uses INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本月使用次数（结算用）' AFTER weekly_earnings,
    ADD COLUMN monthly_earnings DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本月基础收益（创作币）' AFTER monthly_uses,
    ADD COLUMN leaderboard_reward DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本月已发放排行榜奖励（创作币）' AFTER monthly_earnings;

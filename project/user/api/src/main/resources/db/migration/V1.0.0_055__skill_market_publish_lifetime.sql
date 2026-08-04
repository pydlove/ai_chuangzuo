SET NAMES utf8mb4;

-- =============================================================
-- 2026-08-03: 提示词市场发布额度从「月度额度」改为「永久额度」。
-- 1) u_benefit 中 skill_market_publish 类型改为 lifetime，文案去掉"每月"；
-- 2) 合并历史月度用量记录为一条 lifetime 记录（按用户汇总）。
-- =============================================================

-- 1. 更新权益定义：永久额度、提示词语义
UPDATE u_benefit
SET type = 'lifetime',
    name = '发布到提示词市场',
    description = '可发布到提示词市场的提示词数量上限（永久有效）',
    display_label = '发布提示词',
    card_value_tpl = '可发布 {value} 个提示词'
WHERE code = 'skill_market_publish';

-- 2. 合并已有月度用量为永久用量（按用户汇总 used_count / pre_used_count）
CREATE TEMPORARY TABLE tmp_skill_market_publish_lifetime_usage AS
SELECT user_id,
       benefit_code,
       COALESCE(SUM(used_count), 0) AS total_used,
       COALESCE(SUM(pre_used_count), 0) AS total_pre
FROM u_benefit_usage
WHERE benefit_code = 'skill_market_publish'
GROUP BY user_id, benefit_code;

DELETE FROM u_benefit_usage WHERE benefit_code = 'skill_market_publish';

INSERT INTO u_benefit_usage (user_id, benefit_code, period, used_count, pre_used_count, tenant_id)
SELECT user_id,
       benefit_code,
       'lifetime',
       total_used,
       total_pre,
       0
FROM tmp_skill_market_publish_lifetime_usage
WHERE total_used > 0 OR total_pre > 0;

DROP TEMPORARY TABLE tmp_skill_market_publish_lifetime_usage;

-- 3. 更新列注释，说明 period 也支持 lifetime
ALTER TABLE u_benefit_usage MODIFY COLUMN period VARCHAR(16) NOT NULL COMMENT '周期标识：月度格式 yyyy-MM，lifetime 类型权益为 lifetime';
ALTER TABLE u_benefit MODIFY COLUMN type VARCHAR(16) NOT NULL COMMENT '类型：boolean/quota/tier/lifetime';

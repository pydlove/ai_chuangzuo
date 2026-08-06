SET NAMES utf8mb4;

-- =============================================================
-- 2026-08-05: 提示词市场增加审核通过时间，用于正确展示「发布时间」。
-- =============================================================

ALTER TABLE u_skill_market
    ADD COLUMN approved_at DATETIME(3) DEFAULT NULL COMMENT '审核通过时间' AFTER audit_status;

-- 历史已上架记录：用最后更新时间回填（最接近审核通过时刻的可用信息）。
UPDATE u_skill_market
SET approved_at = updated_at
WHERE audit_status = 1
  AND approved_at IS NULL;

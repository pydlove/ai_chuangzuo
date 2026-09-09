SET NAMES utf8mb4;

-- =============================================================
-- 2026-09-08: 提示词审核脏数据清理——把「待审核但无待审核市场记录」的 u_user_skill 回写为草稿（audit_status=3）。
--
-- 背景：audit_status 只有 0/1/2 三态，草稿与待审核同为 0。用户取消发布（下架）后
-- u_user_skill 仍残留 0，管理端提示词审核列表会把这些已撤销记录（以及从未提交的草稿）
-- 当待审核展示。本次起草稿态为 3（见用户端代码调整），此处一次性清理存量。
--
-- 判定：不存在 is_deleted=0 且 audit_status=0 的 u_skill_market 记录，即用户已撤销/从未提交。
-- 真实待审核提交有对应市场待审核记录，不受影响。
-- =============================================================

UPDATE u_user_skill s
SET s.audit_status = 3
WHERE s.audit_status = 0
  AND s.is_deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM u_skill_market m
    WHERE m.biz_no = s.biz_no
      AND m.is_deleted = 0
      AND m.audit_status = 0
  );

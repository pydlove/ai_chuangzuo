SET NAMES utf8mb4;

-- 自媒体方案 AI 缓存表改为物理删除，清理历史软删除记录，避免唯一键冲突
DELETE FROM u_self_media_plan_question WHERE is_deleted = 1;
DELETE FROM u_self_media_plan_niche WHERE is_deleted = 1;
DELETE FROM u_self_media_plan_persona WHERE is_deleted = 1;

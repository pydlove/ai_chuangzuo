SET NAMES utf8mb4;

-- 修复：u_recommended_creation_session 存在按 user_id 的唯一键，
-- 而业务使用逻辑删除会导致已删除行仍然占用唯一键，无法重新插入新会话。
-- 清理历史软删除数据，后续代码改为物理删除。
DELETE FROM u_recommended_creation_session WHERE is_deleted = 1;

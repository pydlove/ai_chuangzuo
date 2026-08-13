SET NAMES utf8mb4;

-- 管理端预设提示词支持「基于模版创建」：把结构化信息（角色、受众、写作要求、语气、禁区）存为 JSON。
-- 实际喂给 AI 的拼接后文本仍保存在 prompt 列，保证用户端生成逻辑无需改动。
ALTER TABLE u_user_skill
    ADD COLUMN prompt_extra JSON DEFAULT NULL
        COMMENT '结构化提示词 JSON（templateBased/role/audience/requirements/tone/restrictions）' AFTER prompt;

ALTER TABLE u_recommended_creation_session
    MODIFY COLUMN prompt TEXT DEFAULT NULL COMMENT '用户选中的创作提示词';

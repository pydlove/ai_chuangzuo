ALTER TABLE u_recommended_creation_session
    ADD COLUMN skill_ref VARCHAR(64) NULL COMMENT '选中的提示词市场 skill 业务编号，用于结算创作币收益' AFTER prompt;

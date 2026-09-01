SET NAMES utf8mb4;

ALTER TABLE u_self_media_plan_question
    ADD COLUMN allow_other TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持其他选项填写：0否 1是' AFTER sort_order,
    ADD COLUMN other_max_length INT NOT NULL DEFAULT 0 COMMENT '其他选项最大输入长度，allow_other=1时生效' AFTER allow_other;

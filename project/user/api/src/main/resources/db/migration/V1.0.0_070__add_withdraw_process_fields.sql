SET NAMES utf8mb4;

ALTER TABLE u_withdraw_request
    ADD COLUMN processed_at DATETIME(3) DEFAULT NULL COMMENT '处理时间' AFTER status,
    ADD COLUMN processed_by BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID（管理员）' AFTER processed_at,
    ADD COLUMN result_remark VARCHAR(255) DEFAULT NULL COMMENT '处理结果备注（如拒绝原因）' AFTER processed_by;

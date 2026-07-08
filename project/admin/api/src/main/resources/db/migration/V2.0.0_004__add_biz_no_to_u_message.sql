-- 管理端-消息管理：为 u_message 增加 biz_no（批次号）
--
-- 用途：一次发布（公告/优惠活动指定人）会扇出 N 行 u_message（每行一个 target_user_id），
--      admin 端需要按 biz_no 聚合展示与统计已读；广播（scope=1）单行独立 biz_no。
--      已有数据（V1.0.0_016 写入的 3 条种子）回填 'LEGACY{id}' 作为兼容。

ALTER TABLE u_message
    ADD COLUMN biz_no VARCHAR(32) NOT NULL DEFAULT '' COMMENT '批次号：广播单行/指定人多行共享' AFTER id;

-- 历史数据回填：默认 '' 改为 'LEGACY{id}'，让老数据也能在 admin 端聚合展示
UPDATE u_message SET biz_no = CONCAT('LEGACY', id) WHERE biz_no = '';

-- 索引：按 biz_no 聚合查询；按 (msg_type, biz_no) 列表过滤
CREATE INDEX idx_message_biz_no ON u_message (biz_no);
CREATE INDEX idx_message_msg_type_biz_no ON u_message (msg_type, biz_no);

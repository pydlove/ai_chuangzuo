-- 模拟运营：a_simulation_batch 补 started_at/finished_at（实体已有字段，批次启动/完结时间）
ALTER TABLE a_simulation_batch
    ADD COLUMN started_at DATETIME(3) DEFAULT NULL COMMENT '批次开始时间' AFTER updated_by,
    ADD COLUMN finished_at DATETIME(3) DEFAULT NULL COMMENT '批次完结时间' AFTER started_at;

-- 模拟运营-批次类型：ROBOT_JOURNEY 机器人旅程 / FREE_CREATE 模拟生成文章（存量批次默认旅程）
ALTER TABLE a_simulation_batch
    ADD COLUMN batch_type VARCHAR(32) NOT NULL DEFAULT 'ROBOT_JOURNEY'
        COMMENT '批次类型：ROBOT_JOURNEY-机器人旅程 / FREE_CREATE-模拟生成文章' AFTER cycle;

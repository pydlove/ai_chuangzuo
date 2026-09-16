-- 模拟运营-机器人执行日志明细列改为字符串，用于展示阶段结果（奖项/文章标题/提示词/约稿任务等）
ALTER TABLE a_simulation_robot_log
    MODIFY COLUMN detail VARCHAR(512) DEFAULT NULL COMMENT '阶段明细（奖项/文章标题/提示词/约稿任务等）';

SET NAMES utf8mb4;

-- AI 生成标题异步任务表。
-- 同步 generate 接口最坏要等数十秒阻塞 admin 弹框，改为入队 + 后台轮询 worker + 状态查询。
-- 状态机：0=QUEUED, 1=PROCESSING, 2=COMPLETED, 3=FAILED（与 t_generation_task 对齐）。

CREATE TABLE IF NOT EXISTS t_topic_title_task (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    status          TINYINT UNSIGNED NOT NULL DEFAULT 0
                        COMMENT '0=queued,1=processing,2=completed,3=failed',
    count           INT              NOT NULL                COMMENT '请求生成数量',
    direction       VARCHAR(1024)    NULL                    COMMENT '方向提示词（可选）',
    generated_count INT              NOT NULL DEFAULT 0      COMMENT '实际入库条数',
    failed_reason   VARCHAR(512)     NULL                    COMMENT '失败原因（status=3 时填写）',
    created_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    started_at      DATETIME(3)      NULL,
    completed_at    DATETIME(3)      NULL,
    PRIMARY KEY (id),
    INDEX idx_topic_title_task_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成标题异步任务';
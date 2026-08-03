ALTER TABLE u_benefit_usage ADD COLUMN pre_used_count INT NOT NULL DEFAULT 0 COMMENT '预扣额度数' AFTER used_count;

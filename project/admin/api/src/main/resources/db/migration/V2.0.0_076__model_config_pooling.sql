SET NAMES utf8mb4;

-- AI 模型配置池化：每个厂商支持多 key，新增唯一名称与全局优先级字段

-- 1. 新增配置名称字段（先 nullable，回填数据后再改为 NOT NULL）
ALTER TABLE a_model_config ADD COLUMN name VARCHAR(128) NULL COMMENT '配置唯一名称，手动填写';

-- 2. 为历史数据回填默认名称（基于厂商类型），保证 NOT NULL 前无空值
UPDATE a_model_config SET name = CONCAT('默认-', provider_type) WHERE name IS NULL OR name = '';

-- 3. 名称改为非空
ALTER TABLE a_model_config MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '配置唯一名称，手动填写';

-- 4. 新增优先级字段：数字越小越优先
ALTER TABLE a_model_config ADD COLUMN priority INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '优先级，数字越小越优先';

-- 5. 删除厂商类型唯一约束，允许一个厂商下存在多条配置
ALTER TABLE a_model_config DROP INDEX uk_a_model_config_provider_type;

-- 6. 新增查询索引
ALTER TABLE a_model_config ADD INDEX idx_a_model_config_provider_type (provider_type);
ALTER TABLE a_model_config ADD INDEX idx_a_model_config_priority (priority);
ALTER TABLE a_model_config ADD INDEX idx_a_model_config_active_priority (is_active, priority);

-- 7. 配置名称全局唯一
ALTER TABLE a_model_config ADD UNIQUE KEY uk_a_model_config_name (name);

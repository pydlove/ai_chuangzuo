-- ============================================================
-- 业务域：AI 创作与生成
-- 表数量：15
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_article` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号，对外暴露',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID（u_user.id）',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作品标题',
  `body` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作品正文',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布描述（AI 生成摘要）',
  `tags_json` json DEFAULT NULL COMMENT '推荐标签 JSON 数组',
  `optimized_titles_json` json DEFAULT NULL COMMENT 'AI 优化标题缓存，形如 {"wechat":["标题1","标题2"],...}',
  `ai_detect_report` json DEFAULT NULL COMMENT 'AI 检测报告（人工/疑似/AI 三段占比与建议）',
  `style_overrides` json DEFAULT NULL COMMENT '编辑器内联样式覆盖，用于 Preview/Edit 还原',
  `platform` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布平台 key：wechat / xiaohongshu / toutiao / baijiahao / douyin / zhihu / general',
  `skill` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '写作风格名称',
  `template` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '导出模板名称',
  `word_count` int unsigned NOT NULL DEFAULT '0' COMMENT '正文字数（去空白字符）',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '生成完成时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `task_id` bigint unsigned DEFAULT NULL COMMENT '关联生成任务ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_article_biz_no` (`biz_no`),
  UNIQUE KEY `uk_u_article_task_id` (`task_id`),
  KEY `idx_u_article_user_deleted` (`user_id`,`is_deleted`),
  KEY `idx_u_article_completed` (`user_id`,`completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_draft` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号，对外暴露',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID（u_user.id）',
  `custom_title` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '草稿标题（用户自定义）',
  `custom_requirement` text COLLATE utf8mb4_unicode_ci COMMENT '草稿需求描述（用户自定义）',
  `platform` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布平台 key',
  `word_count` int unsigned NOT NULL DEFAULT '0' COMMENT '目标字数',
  `skill` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '写作风格名称',
  `template` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '导出模板名称',
  `create_mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'guided' COMMENT '创建模式：guided-引导模式，minimal-熟手模式',
  `saved_at` datetime(3) NOT NULL COMMENT '草稿保存时间（用户最近一次编辑的时间）',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_draft_biz_no` (`biz_no`),
  KEY `idx_u_draft_user_deleted` (`user_id`,`is_deleted`),
  KEY `idx_u_draft_saved` (`user_id`,`saved_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_weekly_article` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `week_start_date` date NOT NULL COMMENT '所在周的周一日期',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `reads` int unsigned NOT NULL DEFAULT '0' COMMENT '阅读量',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_u_weekly_article_user_week` (`user_id`,`week_start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_generation_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号（对外暴露，如 GA20260709xxxx）',
  `target_user_id` bigint unsigned NOT NULL COMMENT '发起任务的用户ID',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-queued，1-processing，2-completed，3-failed',
  `model_config_id` bigint unsigned NOT NULL COMMENT '使用的AI模型配置ID',
  `prompt_template_id` bigint unsigned NOT NULL COMMENT '使用的提示词模板ID（提交时快照）',
  `input_param` json NOT NULL COMMENT '输入参数：title/description/platform/wordCount/styleRef/toneTags',
  `word_limit_target` int unsigned NOT NULL DEFAULT '3000' COMMENT '用户要求字数（≤3000）',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '已重试次数',
  `locked_at` datetime(3) DEFAULT NULL COMMENT 'worker 锁定（开始处理）时间',
  `locked_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'worker 实例ID，当前单实例固定 worker-1',
  `lease_until` datetime(3) DEFAULT NULL COMMENT 'lease 超时时刻（locked_at + 5分钟）',
  `failed_reason` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后一次失败原因',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间（成功或最终失败）',
  `article_biz_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '完成后关联的 u_article.biz_no',
  `retention_days` int DEFAULT NULL COMMENT '保留天数：基础 30，pro/旗舰 null=永久',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID（=0）',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `prompt_template_version` int unsigned DEFAULT NULL COMMENT '任务创建时锁定的模板版本号；NULL=fallback 到 enabled=1 的最新版',
  `progress_pct` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '任务进度 0-100；worker 每阶段结束后写回；user 端轮询可见',
  `plan_priority` tinyint NOT NULL DEFAULT '0' COMMENT '任务提交时用户套餐优先级：0-免费/基础版，1-专业版，2-旗舰版',
  `skill_ref` varchar(64) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (json_unquote(json_extract(`input_param`,_utf8mb4'$.skillRef'))) VIRTUAL COMMENT 'input_param.skillRef 虚拟列，用于索引与统计',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_gt_biz_no` (`biz_no`),
  KEY `idx_a_gt_user_created` (`target_user_id`,`created_at`),
  KEY `idx_a_gt_status_lease` (`status`,`lease_until`),
  KEY `idx_a_gt_article_biz_no` (`article_biz_no`),
  KEY `idx_a_gt_status_priority_created` (`status`,`plan_priority`,`created_at`),
  KEY `idx_a_generation_task_skill_ref` (`skill_ref`),
  KEY `idx_a_generation_task_skill_ref_completed` (`skill_ref`,`completed_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_generation_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '归档表自增主键',
  `task_id` bigint unsigned NOT NULL COMMENT '原 a_generation_task.id',
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号',
  `target_user_id` bigint unsigned NOT NULL COMMENT '发起任务的用户ID',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务结束时已确定的 title（成功或失败状态都有）',
  `input_param` json NOT NULL COMMENT '输入参数 JSON',
  `status` tinyint unsigned NOT NULL COMMENT '终态：2-completed，3-failed',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '累计重试次数',
  `failed_reason` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因（status=failed 时）',
  `word_limit_target` int unsigned NOT NULL COMMENT '用户要求字数',
  `model_config_id` bigint unsigned NOT NULL COMMENT '使用的AI模型配置ID',
  `prompt_template_id` bigint unsigned NOT NULL COMMENT '使用的提示词模板ID',
  `created_at` datetime(3) NOT NULL COMMENT '原任务创建时间',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `article_biz_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '完成后关联的 u_article.biz_no',
  `duration_ms` bigint DEFAULT NULL COMMENT '处理耗时（毫秒）',
  `archived_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入档时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_gh_task_id` (`task_id`),
  KEY `idx_a_gh_user_archived` (`target_user_id`,`archived_at`),
  KEY `idx_a_gh_article_biz_no` (`article_biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_generation_call_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint unsigned NOT NULL COMMENT '所属任务 ID（a_generation_task.id）',
  `stage_index` int unsigned NOT NULL COMMENT '阶段序号 1-12',
  `stage_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '阶段稳定标识符：outline / draft / ...',
  `attempt` int unsigned NOT NULL DEFAULT '1' COMMENT '第几次尝试（1=首次，2=第 1 次重试，...）',
  `success` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否成功',
  `error` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因（成功时为 null）',
  `user_msg` text COLLATE utf8mb4_unicode_ci COMMENT '本次尝试完整 userMsg（变量已替换）',
  `response_content` text COLLATE utf8mb4_unicode_ci COMMENT 'AI 完整返回（成功时）',
  `duration_ms` int unsigned NOT NULL DEFAULT '0' COMMENT '本次尝试耗时（ms）',
  `prompt_tokens` int unsigned DEFAULT NULL COMMENT 'prompt tokens（成功时有值）',
  `completion_tokens` int unsigned DEFAULT NULL COMMENT 'completion tokens（成功时有值）',
  `total_tokens` int unsigned DEFAULT NULL COMMENT '总 tokens = prompt + completion',
  `called_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '调用开始时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID（=0）',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_task` (`task_id`,`called_at`),
  KEY `idx_log_stage` (`task_id`,`stage_index`,`attempt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_generation_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键（固定 1）',
  `pool_size` int unsigned NOT NULL DEFAULT '2' COMMENT 'worker 线程池大小（1-10，重启生效）',
  `claim_batch_size` int unsigned NOT NULL DEFAULT '1' COMMENT '每轮单次拉取任务数（1-10）',
  `lease_minutes` int unsigned NOT NULL DEFAULT '5' COMMENT 'lease 持续分钟（1-60）',
  `poll_interval_ms` int unsigned NOT NULL DEFAULT '500' COMMENT '空轮询间隔 ms（100-5000）',
  `retention_cron` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0 0 3 * * ?' COMMENT '归档定时任务 cron 表达式',
  `worker_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'worker-1' COMMENT 'worker 实例 ID（单实例约定）',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID（=0）',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `llm_retry_max_attempts` int unsigned NOT NULL DEFAULT '3' COMMENT '单 stage AI 调用最大尝试次数（含首次；1-10）',
  `llm_retry_base_delay_ms` int unsigned NOT NULL DEFAULT '500' COMMENT '首次重试前等待 ms（100-10000）',
  `llm_retry_backoff_multiplier` int unsigned NOT NULL DEFAULT '2' COMMENT '指数退避倍数（1-5：第 N 次重试睡 baseDelay * multiplier^(N-1)）',
  `default_temperature` decimal(3,2) NOT NULL DEFAULT '0.70' COMMENT 'AI temperature 默认值（0.00-2.00；stage model_params 可覆盖）',
  `default_max_tokens` int unsigned NOT NULL DEFAULT '32768' COMMENT 'AI max_tokens 默认值（1-128000；MiniMax-M3 推理模型 reasoning 也吃此预算，润色类 stage 至少给到 32k）',
  `default_top_p` decimal(3,2) NOT NULL DEFAULT '1.00' COMMENT 'AI top_p 默认值（0.00-1.00；stage model_params 可覆盖）',
  `ai_read_timeout_seconds` int unsigned NOT NULL DEFAULT '180' COMMENT 'AI 调用读取超时（秒）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_model_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '厂商类型：kimi / minimax',
  `base_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API 基础地址',
  `api_key_encrypted` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密后的 API Key',
  `model_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型编码',
  `model_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型显示名',
  `is_active` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否启用：0-否，1-是（全局唯一）',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置唯一名称，手动填写',
  `priority` int unsigned NOT NULL DEFAULT '0' COMMENT '优先级，数字越小越优先',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_model_config_name` (`name`),
  KEY `idx_a_model_config_provider_type` (`provider_type`),
  KEY `idx_a_model_config_priority` (`priority`),
  KEY `idx_a_model_config_active_priority` (`is_active`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_provider_model` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '厂商类型：kimi / minimax',
  `model_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型编码',
  `model_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型显示名',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_provider_model_provider_type_model_code` (`provider_type`,`model_code`),
  KEY `idx_a_provider_model_provider_type` (`provider_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `c_ai_prompt` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '唯一编码',
  `prompt_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '显示名称',
  `module` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '归属端：admin / user',
  `category` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务分类',
  `system_role` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '系统角色 / AI 身份设定',
  `user_prompt` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户提示词主体',
  `variable_schema` json DEFAULT NULL COMMENT '变量元数据：[{name, required, description, example}]',
  `version` int unsigned NOT NULL DEFAULT '0',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_c_ai_prompt_code` (`prompt_code`),
  KEY `idx_module_category` (`module`,`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_prompt_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称（管理后台显示）',
  `base_content` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '① 基础内容（去 AI 味）',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `template_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '模板状态：0-草稿，1-已发布，2-已下线',
  `latest_published_version` int unsigned DEFAULT NULL COMMENT '当前最新已发布版本号，未发布则为 NULL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_prompt_template_stage` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint unsigned NOT NULL COMMENT '所属模板 ID（t_prompt_template.id）',
  `stage_index` tinyint unsigned NOT NULL COMMENT '阶段序号 1-12（设计文档固定 12 阶段）',
  `stage_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ai_prompt / rule_config / passthrough',
  `stage_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '阶段稳定标识符：outline / material_list / draft / rhythm_detect / ...',
  `ai_prompt` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '仅 stage_type=ai_prompt 有值：可编辑的 AI 提示词模板',
  `rule_config` json DEFAULT NULL COMMENT '仅 stage_type=rule_config 有值：规则参数（JSON）',
  `enabled` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '该 stage 是否启用（admin 可单 stage 关停）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID（=0）',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `model_params` json DEFAULT NULL COMMENT 'AI 阶段可配参数：temperature / max_tokens / top_p 等；NULL=用全局默认',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_stage` (`template_id`,`stage_index`),
  KEY `idx_stage_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_prompt_template_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint unsigned NOT NULL COMMENT '所属模板ID',
  `version` int unsigned NOT NULL COMMENT '版本号，从 1 开始自增',
  `version_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '版本状态：0-草稿，1-已发布，2-已下线',
  `config_json` json NOT NULL COMMENT '12 阶段配置完整快照',
  `change_note` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '本次发布变更说明',
  `published_at` datetime(3) DEFAULT NULL COMMENT '发布时间',
  `published_by` bigint unsigned DEFAULT NULL COMMENT '发布人ID',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_t_pt_version` (`template_id`,`version`),
  KEY `idx_t_pt_version_status` (`version_status`),
  KEY `idx_t_pt_version_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_skill_analyze_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID，固定为1',
  `daily_attempt_limit` int unsigned NOT NULL DEFAULT '5' COMMENT '每个用户每天最多可进行 AI 提示词分析的次数',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_skill_analyze_daily` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `attempt_date` date NOT NULL COMMENT '分析日期',
  `attempt_count` int unsigned NOT NULL DEFAULT '0' COMMENT '当日已分析次数',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_skill_analyze_daily_user_date` (`user_id`,`attempt_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 提示词配置表：管理后台统一维护，用户端/管理端运行时读取
CREATE TABLE IF NOT EXISTS c_ai_prompt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    prompt_code VARCHAR(64) NOT NULL COMMENT '唯一编码',
    prompt_name VARCHAR(128) NOT NULL COMMENT '显示名称',
    module VARCHAR(32) NOT NULL COMMENT '归属端：admin / user',
    category VARCHAR(64) DEFAULT NULL COMMENT '业务分类',
    system_role MEDIUMTEXT COMMENT '系统角色 / AI 身份设定',
    user_prompt MEDIUMTEXT NOT NULL COMMENT '用户提示词主体',
    variable_schema JSON DEFAULT NULL COMMENT '变量元数据：[{name, required, description, example}]',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_c_ai_prompt_code (prompt_code),
    KEY idx_module_category (module, category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 提示词配置表';

-- 管理端：爆款标题/选题生成
INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'topic_title_v1',
    '爆款标题/选题生成',
    'admin',
    'topic_title',
    '你是自媒体爆款标题策划。',
    '请生成 {{count}} 条自媒体选题标题，每条包含标题和描述（写作指引）。\n\n生成方向：{{direction}}\n\n支持平台及规则约束（标题与描述必须同时满足）：\n- 微信公众号：禁止诱导分享/关注/转发、低俗、谣言、侵权、虚假宣传、标题党。\n- 小红书：禁止夸张营销、诱导点赞收藏、虚假体验、违禁词、过度美化/对比。\n- 今日头条：禁止标题党、低俗、谣言、侵权、广告法违禁词、无资质医疗/财经建议。\n- 知乎：禁止诱导关注、编故事、不友善、低质营销、无来源事实断言。\n- 百家号：禁止标题党、低俗、抄袭、广告法违禁词、虚假权威背书。\n- 抖音图文：禁止诱导互动（如“双击 666”）、低俗、虚假内容、侵权、未成年人不良引导。\n通用禁区：严禁使用“最”“第一”“绝对”“国家级”等无法证实的极限词；严禁制造焦虑、歧视、攻击、泄露隐私；严禁承诺收益、疗效等无法验证的结果。\n\n标题多样性要求（避免同质化）：\n- 每条标题必须从不同角度切入，避免同义反复或只换关键词。\n- 句式要交错使用：问题型、反差型、场景型、观点型、方法型、故事型、数据型等。\n- 情绪表达要有差异，避免连续使用“震惊”“绝了”“后悔没早点”等同一套爆款模板。\n- 同一生成批次中，任意两条标题的开头 5 个字不能完全相同。\n\n描述要求（必须是写作指引，不是简单总结）：\n- 说明这篇文章大致怎么写，给出 2-5 个核心观点或写作要点。\n- 格式示例：围绕以下观点创作，1、xxx；2、xxxxx；3、xxxx。\n- 每个要点要指出：本部分论证什么、从什么角度展开、给读者带来什么价值。\n- 不要只写“介绍方法”“分析原因”这类空泛说明。\n\n格式要求：\n- 标题 ≤30 字，描述 ≤300 字。\n- 标题和描述中如需引用词语，一律使用中文双引号“”，不要使用单引号。\n\n输出 JSON 结构：\n{"titles": [{"title": "标题文字", "summary": "围绕以下观点创作，1、...；2、...；3、..."}]}\n\n最终输出要求（覆盖以上所有说明，必须严格遵守）：\n1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。\n2. 不要用 ```json 或任何代码围栏包裹。\n3. 第一个字符必须是 {，最后一个字符必须是 }。\n4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。',
    '[{"name":"count","required":true,"description":"生成数量","example":"10"},{"name":"direction","required":true,"description":"生成方向","example":"职场、情感、生活、AI 等热门自媒体赛道"}]',
    1,
    0,
    '管理端 AI 批量生成选题标题'
);

-- 用户端：标题优化
INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'title_optimize_v1',
    '标题优化',
    'user',
    'title_optimize',
    '你是一位资深新媒体标题策划专家，深谙各内容平台的推荐机制与用户点击心理。你只输出合法 JSON。',
    '请根据文章标题和正文，为 7 个平台分别拟定 2 条优化标题。\n\n【文章标题】\n{{title}}\n\n【文章正文】\n{{bodyExcerpt}}\n\n【平台与风格要求】\n- wechat（公众号）：引发共鸣或好奇，可带数字/悬念，避免标题党词汇堆砌，30 字以内。\n- xiaohongshu（小红书）：口语化、带 emoji，突出获得感或身份代入，20 字以内。\n- toutiao（今日头条）：信息量大、冲击力强，可适度悬念，30 字以内。\n- baijiahao（百家号）：正式稳重、突出价值点与专业性，30 字以内。\n- zhihu（知乎）：以问句或深度观点句呈现，强调逻辑与干货，35 字以内。\n- douyin（抖音图文）：短平快、情绪强、钩子前置，20 字以内。\n- bilibili（B站专栏）：年轻化、有梗但不低俗，突出兴趣点，30 字以内。\n\n【硬性要求】\n1. 每个平台恰好 2 条标题，风格不可雷同：一条偏痛点/利益驱动，一条偏好奇/情绪驱动。\n2. 标题必须忠于正文内容，不得虚构正文不存在的事实、数据或承诺。\n3. 不得使用“震惊”“不看后悔”等低俗标题党词汇。\n4. 输出 JSON 结构：{"titles":{"wechat":["...","..."],"xiaohongshu":["...","..."],"toutiao":["...","..."],"baijiahao":["...","..."],"zhihu":["...","..."],"douyin":["...","..."],"bilibili":["...","..."]}}\n\n最终输出要求（覆盖以上所有说明，必须严格遵守）：\n  1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。\n  2. 不要用 ```json 或任何代码围栏包裹。\n  3. 第一个字符必须是 {，最后一个字符必须是 }。\n  4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。',
    '[{"name":"title","required":true,"description":"文章标题","example":"原文标题"},{"name":"bodyExcerpt","required":true,"description":"正文摘要","example":"文章正文前 1500 字"}]',
    1,
    0,
    '用户端 AI 标题优化'
);

-- 用户端：文风分析
INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'skill_analyze_v1',
    '文风分析',
    'user',
    'skill_analyze',
    '你是一位资深的中文文体分析师，擅长拆解中文自媒体文章的写作风格，并把风格特征提炼成可直接指导 AI 写作的提示词。',
    '请分析以下参考文章的写作风格，完成两件事：\n\n【文章正文】\n{{text}}\n\n【任务】\n1. 从【语气】【词汇】【句式】【结构】四个维度拆解风格特征。每条特征必须具体、可模仿，禁止空泛形容（不要写「语言优美」，要写「多用15字以内短句，段间留白多」这类可执行描述）。\n2. 从原文中逐字摘录 2 个最能代表该风格的片段。\n\n【输出 JSON 结构】\n{"excerpt1":"原文中最能代表风格的连续片段，不超过120字，必须逐字摘自原文","excerpt2":"另一个代表性片段，不超过80字，必须逐字摘自原文，且不与excerpt1重复","description":"用一句话描述这个提示词适合写什么、风格是什么，不超过100字，不要出现英文双引号","prompt":"不超过1200字的风格提示词"}\n\n其中 prompt 字段严格使用以下模板：\n你是一位中文写手，请模仿以下参考文章的写作风格：\n\n【语气】（人称视角、情感温度、与读者的距离感，1-2句）\n【词汇】（书面/口语倾向、网络用语与语气词的使用习惯，1-2句）\n【句式】（句子长短与节奏、标点习惯、常用修辞，1-2句）\n【结构】（开头方式、段落组织、结尾处理，1-2句）\n\n请在生成新内容时严格遵循以上风格特征。\n\n最终输出要求（覆盖以上所有说明，必须严格遵守）：\n  1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。\n  2. 不要用 ```json 或任何代码围栏包裹。\n  3. 第一个字符必须是 {，最后一个字符必须是 }。\n  4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。\n  5. prompt 字段中若需引用示例词语，必须使用中文直角引号「」，严禁使用英文双引号 "，避免破坏 JSON 格式。',
    '[{"name":"text","required":true,"description":"参考文章正文","example":"不超过1000字的参考文章"}]',
    1,
    0,
    '用户端 AI 文风分析'
);

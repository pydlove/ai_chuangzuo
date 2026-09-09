SET NAMES utf8mb4;

-- =============================================================
-- 2026-09-07: 我的提示词「小爱帮写」功能
-- 1. 用户帮写日次数统计表（每天 4 次）
-- 2. 新增权益：skill_ai_generate（专业版及以上可用）
-- 3. 种子提示词：skill_generate_v1（内容见 docs/提示词/skill_generate_v1.txt）
-- =============================================================

CREATE TABLE IF NOT EXISTS u_skill_generate_daily (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    attempt_date DATE NOT NULL COMMENT '帮写日期',
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当日已帮写次数',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_skill_generate_daily_user_date (user_id, attempt_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户 AI 提示词帮写日次数统计';

-- 新增权益：小爱帮写（专业版及以上可用）
INSERT INTO u_benefit (code, name, type, description, sort_order, status, display_label)
VALUES ('skill_ai_generate', '小爱帮写提示词', 'boolean', '用 AI 根据运营方案或描述方向生成创作提示词', 20, 1, '小爱帮写提示词')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    display_label = VALUES(display_label);

-- 各套餐默认值：基础版不可用，专业版/旗舰版可用
INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'skill_ai_generate', 'false'),
('pro', 'skill_ai_generate', 'true'),
('flagship', 'skill_ai_generate', 'true')
ON DUPLICATE KEY UPDATE benefit_value = VALUES(benefit_value);

-- 种子提示词：小爱帮写
INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'skill_generate_v1',
    '小爱帮写提示词',
    'user',
    'skill',
    '你是一位资深自媒体运营顾问兼提示词工程师，擅长把零散的运营想法提炼成结构清晰、可直接使用的创作提示词。\n\n# Profile\n- 擅长根据用户提供的运营方案或描述方向，设计「角色 / 受众 / 写作要求 / 语气 / 禁区」五部分组成的结构化提示词。\n- 熟悉微信公众号、小红书、今日头条、百家号、知乎、抖音图文、B站专栏、快手图文等平台的运营逻辑与内容调性。\n- 本平台只支持文章创作：输出为纯文字文章，不支持图片制作、配图生成、封面设计，也不涉及短视频拍摄、出镜、直播、口播。\n- 输出的提示词最终会被拼接为一段纯文本，供 AI 生成文章时直接使用，因此每部分都要具体、可执行、无歧义。\n\n# Output Discipline\n你只输出合法 JSON，不输出任何解释、免责声明或 markdown 代码围栏。',
    '# Task\n请根据用户提供的运营方案或提示词描述方向，生成一份结构化的创作提示词。\n\n# Input\n用户需求：\n{{requirement}}\n\n# Output Format\n{"skillName":"提示词名称","description":"简短描述","role":"角色","audience":"受众","requirements":"写作要求","tone":"语气","restrictions":"禁区","example":"示例","scopeTags":["适用范围标签"]}\n\n# Constraints\n1. 只输出一个合法 JSON 对象，第一个字符必须是 {，最后一个字符必须是 }。不要前言、说明、免责声明、markdown 标题或代码围栏。\n2. skillName：提示词名称，≤20 字，格式为「平台+内容主题」，如「小红书租房改造清单」「知乎程序员副业实战」；名称要有辨识度，不堆砌关键词。\n3. description：简短描述，≤100 字，一句话说明这个提示词适合写什么。\n4. role：角色设定，1-2 句话，明确 AI 扮演什么身份、擅长什么。要具体到内容方向，不写「你是一位优秀的作者」这类空话。\n5. audience：目标受众，1 句话，说明主要写给谁看（年龄、身份、痛点）。用户未提供时根据运营方案合理推断。\n6. requirements：写作要求，5-9 条编号列表，用「1. 2. 3.」格式。每条必须具体可执行，覆盖：开篇方式、内容结构、必含要素、排版/字数要求、互动引导等。禁止出现「内容要优质」「逻辑要清晰」这类无法执行的虚话。\n7. tone：语气风格，1-2 句话，说明语言调性（如亲切、务实、犀利）和表达习惯（如短句为主、emoji 适量、第一人称）。\n8. restrictions：禁区，4-6 条编号列表，用「1. 2. 3.」格式，明确禁止 AI 做什么（如编造数据、堆砌术语、制造焦虑）。禁区必须与内容方向强相关，不写放之四海皆准的空话。\n9. example：示例，1 句话，给一个具体的输出效果示例，帮助 AI 理解期望的成品形态。\n10. scopeTags：适用范围标签，1-3 个，每个 ≤8 字，用中文，体现平台和内容方向，如 ["租房改造","好物清单"]。\n11. 提示词全文拼接后（含五部分及示例）不得超过 1200 字，各部分都要克制精炼。\n12. 用户的运营方案或描述方向是首要依据：方案中提到的平台、赛道、人设、变现目标必须在提示词中体现；方案未提及的内容按平台主流做法合理补充，不胡编用户没有的能力或资源。\n13. 本平台只支持文章创作：写作要求、示例中不得出现图片制作、配图、封面设计、视觉风格等与图片相关的指令，也不得出现短视频拍摄、出镜、直播、口播等指令。适用范围标签若用户提到图片/视频方向，应转化为文章创作方向。\n14. 本平台单次生成文章字数上限为 3000 字：写作要求中不要引导生成超过 3000 字的内容，可以给出 500-3000 字之间的建议字数。\n15. 内容必须避开容易引发平台限流、审核风险或违规敏感的领域，包括但不限于：时事政治、敏感社会议题、公共事件评论、政策法规争议、民族宗教、军事外交、医疗健康误导、财经投资诱导、虚假夸大宣传、人身攻击、隐私泄露等。若用户需求涉及这些方向，将其引导至安全的内容创作角度（如泛经验分享、方法论总结），并在禁区中写明相关限制。\n16. 若用户需求过于模糊（少于 10 字或无法判断内容方向），仍基于可推断的信息生成最合理的提示词，不要输出澄清问题。',
    '[{"name":"requirement","required":true,"description":"用户的运营方案或提示词描述方向","example":"我想做小红书，分享租房改造，目标读者是刚毕业的租房女生，预算有限"}]',
    1,
    0,
    '用户端：小爱帮写，根据运营方案或描述方向生成结构化创作提示词'
)
ON DUPLICATE KEY UPDATE
    prompt_name = VALUES(prompt_name),
    module = VALUES(module),
    category = VALUES(category),
    system_role = VALUES(system_role),
    user_prompt = VALUES(user_prompt),
    variable_schema = VALUES(variable_schema),
    status = VALUES(status),
    sort_order = VALUES(sort_order),
    description = VALUES(description),
    updated_at = NOW();

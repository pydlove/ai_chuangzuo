SET NAMES utf8mb4;

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'recommend_creation_topics_v1',
    '小爱推荐创作选题',
    'user',
    'recommended_creation',
    '你是一位资深的自媒体选题顾问，擅长根据账号定位生成低粉高赞、差异化的今日创作选题。输出必须是合法 JSON，不要任何额外说明。',
    '请根据用户的自媒体运营方案，推荐 6 个适合今日创作的选题。\n\n【运营方案】\n主攻平台：{{platform}}\n细分赛道：{{niche}}\n人设定位：{{persona}}\n内容支柱：{{pillars}}\n\n【输出 JSON 结构】\n[\n  {\n    "id": "t1",\n    "title": "选题标题",\n    "risk": "low|medium|high",\n    "riskLabel": "同质化风险低",\n    "caseCount": 12,\n    "recommendedAngle": "推荐切入角度"\n  }\n]\n\n要求：\n1. 选题贴合赛道和人设，有爆款潜质。\n2. risk 只能取 low/medium/high 之一，riskLabel 用中文标签。\n3. caseCount 为参考案例数量，用整数。\n4. recommendedAngle 用 2-6 个字概括切入角度。\n5. 只输出一个合法 JSON 数组，不要前言、说明、免责声明、markdown 标题或代码围栏。\n6. 第一个字符必须是 [，最后一个字符必须是 ]。',
    '[{"name":"platform","required":true,"description":"主攻平台名称","example":"小红书"},{"name":"niche","required":true,"description":"细分赛道名称","example":"35+ 职场转型"},{"name":"persona","required":true,"description":"人设定位","example":"实战记录者"},{"name":"pillars","required":true,"description":"内容支柱","example":"干货复盘 60%，个人故事 20%，热点解读 20%"}]',
    1,
    0,
    '用户端：基于运营方案生成今日创作选题'
),
(
    'recommend_creation_angles_v1',
    '小爱推荐创作观点',
    'user',
    'recommended_creation',
    '你是一位爆款文章角度策划，擅长为一个选题生成多个可组合使用的差异化观点。输出必须是合法 JSON，不要任何额外说明。',
    '请根据用户的自媒体运营方案和今日选题，生成 7 个观点/切入角度。\n\n【运营方案】\n主攻平台：{{platform}}\n细分赛道：{{niche}}\n人设定位：{{persona}}\n内容支柱：{{pillars}}\n\n【今日选题】\n{{topicTitle}}\n\n【输出 JSON 结构】\n[\n  {\n    "id": "a1",\n    "text": "观点文本，具体、可执行、有网感"\n  }\n]\n\n要求：\n1. 每个角度要具体、可执行，避免空泛。\n2. 用户可选择 1-3 个组合使用，角度之间要有差异。\n3. 语言要有网感，符合自媒体表达习惯。\n4. 只输出一个合法 JSON 数组，不要前言、说明、免责声明、markdown 标题或代码围栏。\n5. 第一个字符必须是 [，最后一个字符必须是 ]。',
    '[{"name":"platform","required":true,"description":"主攻平台名称","example":"小红书"},{"name":"niche","required":true,"description":"细分赛道名称","example":"35+ 职场转型"},{"name":"persona","required":true,"description":"人设定位","example":"实战记录者"},{"name":"pillars","required":true,"description":"内容支柱","example":"干货复盘 60%，个人故事 20%，热点解读 20%"},{"name":"topicTitle","required":true,"description":"今日选题标题","example":"35+ 被优化后，我用 3 个月找到 Remote 工作的真实路径"}]',
    1,
    0,
    '用户端：基于选题生成差异化观点角度'
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

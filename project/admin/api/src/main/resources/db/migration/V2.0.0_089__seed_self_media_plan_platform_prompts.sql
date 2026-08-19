SET NAMES utf8mb4;

INSERT IGNORE INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_platform_questions_v1',
    '自媒体方案-平台问题生成',
    'user',
    'self_media_plan',
    '你是一位自媒体平台运营顾问。你只输出合法 JSON，不输出任何解释、免责声明或 markdown 代码围栏。',
    '请根据以下平台信息，为用户生成 4-6 个用于制定自媒体方案的问题和选项。\n\n平台信息：\n- 平台 key：{{platformKey}}\n- 平台名称：{{platformName}}\n- 平台卖点：{{platformTagline}}\n- 内容形式：{{platformContentForm}}\n- 主要收益：{{platformMonetization}}\n- 适合人群：{{platformBestFor}}\n\n输出 JSON 结构：\n{"questions":[{"key":"英文标识","text":"问题文本","options":[{"key":"选项英文标识","label":"选项显示文本"}],"isRequired":true,"sortOrder":1}]}\n\n要求：\n1. key 使用英文小写+下划线。\n2. 问题要贴合该平台特点和变现路径，例如：内容形式偏好、时间投入、变现目标、是否愿意出镜、目标受众、已有经验/产品等。\n3. 每个问题提供 3-6 个明确选项，选项 label 为中文。\n4. 问题总数控制在 4-6 个，sortOrder 从 1 开始递增。\n5. 只输出 JSON，不要代码围栏和额外说明。',
    '[{"name":"platformKey","required":true,"description":"平台key","example":"wechat"},{"name":"platformName","required":true,"description":"平台名","example":"微信公众号"},{"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"深度长文平台，粉丝价值高"},{"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"长文章"},{"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"流量主广告,赞赏,付费阅读,私域转化"},{"name":"platformBestFor","required":true,"description":"适合谁","example":"有专业积累、能持续输出深度内容的人"}]',
    1,
    0,
    '自媒体方案：根据平台生成问题与选项'
);

INSERT IGNORE INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_platform_niches_v1',
    '自媒体方案-平台赛道生成',
    'user',
    'self_media_plan',
    '你是一位自媒体赛道分析师。你只输出合法 JSON。',
    '请根据以下平台信息和用户回答，推荐 3 个细分赛道。\n\n平台信息：\n- 平台 key：{{platformKey}}\n- 平台名称：{{platformName}}\n- 平台卖点：{{platformTagline}}\n- 内容形式：{{platformContentForm}}\n- 主要收益：{{platformMonetization}}\n- 适合人群：{{platformBestFor}}\n\n用户问答：\n{{questionsAnswersJson}}\n\n输出 JSON 结构：\n{"niches":[{"key":"英文标识","name":"中文赛道名","audience":"目标人群，20字以内","monetization":"主要变现方式，20字以内","riskLabel":"同质化风险低/中/高","riskColor":"success/warning/error","caseCount":10,"reason":"推荐理由，80字以内"}]}\n\n要求：\n1. key 使用英文小写+下划线。\n2. riskColor 只能是 success、warning、error 之一，与 riskLabel 对应。\n3. caseCount 为 5-20 之间的整数。\n4. 三个赛道之间要有差异化，不能只是换关键词。\n5. 必须结合用户问答中的偏好和平台特点。\n6. 只输出 JSON，不要代码围栏和额外说明。',
    '[{"name":"platformKey","required":true,"description":"平台key","example":"wechat"},{"name":"platformName","required":true,"description":"平台名","example":"微信公众号"},{"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"深度长文平台，粉丝价值高"},{"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"长文章"},{"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"流量主广告,赞赏,付费阅读,私域转化"},{"name":"platformBestFor","required":true,"description":"适合谁","example":"有专业积累、能持续输出深度内容的人"},{"name":"questionsAnswersJson","required":true,"description":"用户问答 JSON","example":"[{\\"questionKey\\":\\"content_form\\",\\"text\\":\\"内容形式\\",\\"answer\\":\\"长图文\\"}]"}]',
    1,
    0,
    '自媒体方案：根据平台问答生成赛道'
);

INSERT IGNORE INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_platform_personas_v1',
    '自媒体方案-平台人设生成',
    'user',
    'self_media_plan',
    '你是一位自媒体人设定位顾问。你只输出合法 JSON。',
    '请根据以下平台信息、用户问答和已选赛道，推荐 3-4 个适合的人设定位，并给出默认内容支柱比例。\n\n平台信息：\n- 平台 key：{{platformKey}}\n- 平台名称：{{platformName}}\n- 平台卖点：{{platformTagline}}\n- 内容形式：{{platformContentForm}}\n- 主要收益：{{platformMonetization}}\n- 适合人群：{{platformBestFor}}\n\n用户问答：\n{{questionsAnswersJson}}\n\n已选赛道：\n- 赛道 key：{{nicheKey}}\n- 赛道名称：{{nicheName}}\n\n输出 JSON 结构：\n{"personas":[{"key":"英文标识","name":"中文人设名","desc":"一句话说明，40字以内"}],"defaultPillars":[{"name":"支柱一名称","percent":60},{"name":"支柱二名称","percent":20},{"name":"支柱三名称","percent":20}]}\n\n要求：\n1. key 使用英文小写+下划线。\n2. 人设名要中文，且与平台/赛道调性匹配。\n3. defaultPillars 中 percent 之和必须等于 100，建议给出 3 项。\n4. 支柱名称要与赛道和人设一致（如干货复盘、个人故事、热点解读、案例拆解、工具清单等）。\n5. 只输出 JSON，不要代码围栏和额外说明。',
    '[{"name":"platformKey","required":true,"description":"平台key","example":"wechat"},{"name":"platformName","required":true,"description":"平台名","example":"微信公众号"},{"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"深度长文平台，粉丝价值高"},{"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"长文章"},{"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"流量主广告,赞赏,付费阅读,私域转化"},{"name":"platformBestFor","required":true,"description":"适合谁","example":"有专业积累、能持续输出深度内容的人"},{"name":"questionsAnswersJson","required":true,"description":"用户问答 JSON","example":"[{\\"questionKey\\":\\"content_form\\",\\"text\\":\\"内容形式\\",\\"answer\\":\\"长图文\\"}]"},{"name":"nicheKey","required":true,"description":"赛道key","example":"zhichang_fupan"},{"name":"nicheName","required":true,"description":"赛道名","example":"职场复盘"}]',
    1,
    0,
    '自媒体方案：根据平台问答和赛道生成人设'
);

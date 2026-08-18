SET NAMES utf8mb4;

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_recommend_platform_v1',
    '自媒体方案-平台推荐',
    'user',
    'self_media_plan',
    '你是一位自媒体平台匹配顾问。你只输出合法 JSON，不输出任何解释、免责声明或 markdown 代码围栏。',
    '请根据用户画像，从以下平台中推荐最合适的一个平台，并说明理由。\n\n可选平台（JSON 数组）：\n{{platformsJson}}\n\n用户画像：\n- 主业/副业：{{workType}}\n- 每周投入时间：{{timePerWeek}}\n- 期望月收入：{{incomeGoal}}\n- 可接受不盈利周期：{{breakEvenPeriod}}\n- 倾向内容形式：{{contentType}}\n- 目标受众：{{audience}}\n- 身份标签：{{identity}}\n- 是否愿意出镜/做视频：{{onCamera}}\n- 补充说明：{{note}}\n\n要求：\n1. platformKey 必须从可选平台的 platformKey 中选取。\n2. reason 用 1-2 句话说明匹配原因，控制在 120 字以内。\n3. 只输出 JSON：{"platformKey":"...","reason":"..."}。\n4. 不要代码围栏，不要任何额外文字。',
    '[{"name":"workType","required":true,"description":"主业/副业/想转主业/不明确","example":"副业"},{"name":"timePerWeek","required":true,"description":"每周投入时间","example":"3 - 10 小时"},{"name":"incomeGoal","required":true,"description":"期望月收入","example":"月入过万"},{"name":"breakEvenPeriod","required":true,"description":"可接受不盈利周期","example":"3 个月"},{"name":"contentType","required":true,"description":"倾向内容形式","example":"图文笔记"},{"name":"audience","required":true,"description":"目标受众","example":"职场人"},{"name":"identity","required":true,"description":"身份标签","example":"职场人"},{"name":"onCamera","required":true,"description":"是否愿意出镜/做视频","example":"不想做视频"},{"name":"note","required":false,"description":"补充说明","example":""},{"name":"platformsJson","required":true,"description":"当前启用平台列表 JSON","example":"[{\"platformKey\":\"xiaohongshu\",\"platformName\":\"小红书\",\"tagline\":\"...\"}]"}]',
    1,
    0,
    '自媒体方案：根据问卷推荐平台'
);

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_recommend_goals_v1',
    '自媒体方案-目标推荐',
    'user',
    'self_media_plan',
    '你是一位自媒体变现路径规划师。你只输出合法 JSON。',
    '请为以下用户推荐 3-5 个适合该平台的运营目标（变现方向）。\n\n平台信息：\n- 平台：{{platformName}}（{{platformKey}}）\n- 卖点：{{platformTagline}}\n- 内容形式：{{platformContentForm}}\n- 主要收益：{{platformMonetization}}\n- 适合人群：{{platformBestFor}}\n\n用户背景：\n- 职业/经验领域：{{background}}\n- 主业/副业：{{workType}}\n- 每周投入时间：{{timePerWeek}}\n- 期望月收入：{{incomeGoal}}\n- 可接受不盈利周期：{{breakEvenPeriod}}\n- 倾向内容形式：{{contentType}}\n- 目标受众：{{audience}}\n- 身份标签：{{identity}}\n- 是否出镜/做视频：{{onCamera}}\n- 补充说明：{{note}}\n\n输出 JSON 结构：\n{"goals":[{"key":"英文标识","name":"中文目标名称","description":"一句话说明为什么适合，不超过60字"}]}\n\n要求：\n1. key 使用英文小写+下划线，如 knowledge、experience、product。\n2. name 必须是中文，直观可懂。\n3. 推荐的选项必须贴合平台变现路径和用户背景。\n4. 只输出 JSON，不要代码围栏和额外说明。',
    '[{"name":"platformKey","required":true,"description":"平台key","example":"xiaohongshu"},{"name":"platformName","required":true,"description":"平台名","example":"小红书"},{"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"图文种草社区，女性用户多"},{"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"图文笔记,短视频"},{"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"品牌广告,带货分佣,私域引流"},{"name":"platformBestFor","required":true,"description":"适合谁","example":"有生活经验、愿意分享好物/干货的人"},{"name":"background","required":false,"description":"职业/经验领域","example":"职场/管理"},{"name":"workType","required":true,"description":"主业/副业/想转主业/不明确","example":"副业"},{"name":"timePerWeek","required":true,"description":"每周投入时间","example":"3 - 10 小时"},{"name":"incomeGoal","required":true,"description":"期望月收入","example":"月入过万"},{"name":"breakEvenPeriod","required":true,"description":"可接受不盈利周期","example":"3 个月"},{"name":"contentType","required":true,"description":"倾向内容形式","example":"图文笔记"},{"name":"audience","required":true,"description":"目标受众","example":"职场人"},{"name":"identity","required":true,"description":"身份标签","example":"职场人"},{"name":"onCamera","required":true,"description":"是否愿意出镜/做视频","example":"不想做视频"},{"name":"note","required":false,"description":"补充说明","example":""}]',
    1,
    0,
    '自媒体方案：根据平台推荐运营目标'
);

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_recommend_niches_v1',
    '自媒体方案-赛道推荐',
    'user',
    'self_media_plan',
    '你是一位自媒体赛道分析师。你只输出合法 JSON。',
    '请基于已选的平台和目标，推荐 3 个细分赛道。\n\n已选信息：\n- 平台：{{platformKey}}\n- 目标：{{goal}}\n- 职业/经验领域：{{background}}\n- 是否有可变现产品/服务：{{hasProduct}}\n- 产品/服务描述：{{productDesc}}\n\n用户画像：\n- 主业/副业：{{workType}}\n- 每周投入时间：{{timePerWeek}}\n- 期望月收入：{{incomeGoal}}\n- 可接受不盈利周期：{{breakEvenPeriod}}\n- 倾向内容形式：{{contentType}}\n- 目标受众：{{audience}}\n- 身份标签：{{identity}}\n- 是否出镜/做视频：{{onCamera}}\n- 补充说明：{{note}}\n\n输出 JSON 结构：\n{"niches":[{"key":"英文标识","name":"中文赛道名","audience":"目标人群，20字以内","monetization":"主要变现方式，20字以内","riskLabel":"同质化风险低/中/高","riskColor":"success/warning/error","caseCount":10,"reason":"推荐理由，80字以内"}]}\n\n要求：\n1. key 使用英文小写+下划线。\n2. riskColor 只能是 success、warning、error 之一，与 riskLabel 对应。\n3. caseCount 为 5-20 之间的整数，用于展示「近7天低粉高赞案例」。\n4. 三个赛道之间要有差异化，不能只是换关键词。\n5. 只输出 JSON，不要代码围栏和额外说明。',
    '[{"name":"platformKey","required":true,"description":"平台key","example":"xiaohongshu"},{"name":"platformName","required":true,"description":"平台名","example":"小红书"},{"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"图文种草社区，女性用户多"},{"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"图文笔记,短视频"},{"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"品牌广告,带货分佣,私域引流"},{"name":"platformBestFor","required":true,"description":"适合谁","example":"有生活经验、愿意分享好物/干货的人"},{"name":"background","required":false,"description":"职业/经验领域","example":"职场/管理"},{"name":"goal","required":true,"description":"已选运营目标","example":"靠生活经验变现"},{"name":"hasProduct","required":true,"description":"是否有可变现产品/服务","example":"没有"},{"name":"productDesc","required":false,"description":"产品/服务描述","example":""},{"name":"workType","required":true,"description":"主业/副业/想转主业/不明确","example":"副业"},{"name":"timePerWeek","required":true,"description":"每周投入时间","example":"3 - 10 小时"},{"name":"incomeGoal","required":true,"description":"期望月收入","example":"月入过万"},{"name":"breakEvenPeriod","required":true,"description":"可接受不盈利周期","example":"3 个月"},{"name":"contentType","required":true,"description":"倾向内容形式","example":"图文笔记"},{"name":"audience","required":true,"description":"目标受众","example":"职场人"},{"name":"identity","required":true,"description":"身份标签","example":"职场人"},{"name":"onCamera","required":true,"description":"是否愿意出镜/做视频","example":"不想做视频"},{"name":"note","required":false,"description":"补充说明","example":""}]',
    1,
    0,
    '自媒体方案：根据平台与目标推荐细分赛道'
);

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_recommend_personas_v1',
    '自媒体方案-人设推荐',
    'user',
    'self_media_plan',
    '你是一位自媒体人设定位顾问。你只输出合法 JSON。',
    '请为以下用户推荐 3-4 个适合的人设定位，并给出默认内容支柱比例。\n\n已选信息：\n- 平台：{{platformKey}}\n- 目标：{{goal}}\n- 职业/经验领域：{{background}}\n- 细分赛道：{{nicheName}}（{{nicheKey}}）\n\n用户画像：\n- 主业/副业：{{workType}}\n- 每周投入时间：{{timePerWeek}}\n- 期望月收入：{{incomeGoal}}\n- 可接受不盈利周期：{{breakEvenPeriod}}\n- 倾向内容形式：{{contentType}}\n- 目标受众：{{audience}}\n- 身份标签：{{identity}}\n- 是否出镜/做视频：{{onCamera}}\n- 补充说明：{{note}}\n\n输出 JSON 结构：\n{"personas":[{"key":"英文标识","name":"中文人设名","desc":"一句话说明，40字以内"}],"defaultPillars":[{"name":"支柱一名称","percent":60},{"name":"支柱二名称","percent":20},{"name":"支柱三名称","percent":20}]}\n\n要求：\n1. key 使用英文小写+下划线。\n2. 人设名要中文，且与平台/赛道调性匹配。\n3. defaultPillars 中 percent 之和必须等于 100，建议给出 3 项。\n4. 支柱名称要与赛道和人设一致（如干货复盘、个人故事、热点解读、案例拆解、工具清单等）。\n5. 只输出 JSON，不要代码围栏和额外说明。',
    '[{"name":"platformKey","required":true,"description":"平台key","example":"xiaohongshu"},{"name":"platformName","required":true,"description":"平台名","example":"小红书"},{"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"图文种草社区，女性用户多"},{"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"图文笔记,短视频"},{"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"品牌广告,带货分佣,私域引流"},{"name":"platformBestFor","required":true,"description":"适合谁","example":"有生活经验、愿意分享好物/干货的人"},{"name":"background","required":false,"description":"职业/经验领域","example":"职场/管理"},{"name":"goal","required":true,"description":"已选运营目标","example":"靠生活经验变现"},{"name":"hasProduct","required":true,"description":"是否有可变现产品/服务","example":"没有"},{"name":"productDesc","required":false,"description":"产品/服务描述","example":""},{"name":"nicheKey","required":true,"description":"赛道key","example":"zhichangzhuanxing"},{"name":"nicheName","required":true,"description":"赛道名","example":"35+ 职场转型"},{"name":"workType","required":true,"description":"主业/副业/想转主业/不明确","example":"副业"},{"name":"timePerWeek","required":true,"description":"每周投入时间","example":"3 - 10 小时"},{"name":"incomeGoal","required":true,"description":"期望月收入","example":"月入过万"},{"name":"breakEvenPeriod","required":true,"description":"可接受不盈利周期","example":"3 个月"},{"name":"contentType","required":true,"description":"倾向内容形式","example":"图文笔记"},{"name":"audience","required":true,"description":"目标受众","example":"职场人"},{"name":"identity","required":true,"description":"身份标签","example":"职场人"},{"name":"onCamera","required":true,"description":"是否愿意出镜/做视频","example":"不想做视频"},{"name":"note","required":false,"description":"补充说明","example":""}]',
    1,
    0,
    '自媒体方案：根据赛道推荐人设与内容支柱'
);

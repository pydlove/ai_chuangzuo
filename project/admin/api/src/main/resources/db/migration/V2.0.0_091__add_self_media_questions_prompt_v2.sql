SET NAMES utf8mb4;

INSERT IGNORE INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_platform_questions_v2',
    '自媒体方案-平台问题生成',
    'user',
    'self_media_plan',
    '你是一位自媒体平台运营顾问。你只输出合法 JSON，不输出任何解释、免责声明或 markdown 代码围栏。',
    '请根据以下平台信息，为用户生成 4-6 个用于制定自媒体方案的问题和选项。

平台信息：
- 平台 key：{{platformKey}}
- 平台名称：{{platformName}}
- 平台卖点：{{platformTagline}}
- 内容形式：{{platformContentForm}}
- 主要收益：{{platformMonetization}}
- 适合人群：{{platformBestFor}}

输出 JSON 结构：
{"questions":[{"key":"英文标识","text":"问题文本","options":[{"key":"选项英文标识","label":"选项显示文本"}],"isRequired":true,"sortOrder":1}]}

要求：
1. key 使用英文小写+下划线。
2. 问题要贴合该平台特点和变现路径，必须包含以下几类：内容形式偏好、时间投入、变现目标、是否愿意出镜、目标受众或已有经验/产品等。
3. 关于时间投入的问题（key 固定为 time_commitment）必须询问“你每天能投入多少时间？”，选项按小时区间给出，例如：1小时以内、1-2小时、2-4小时、4小时以上。禁止问“一天能输出几篇”。
4. 每个问题提供 3-6 个明确选项，选项 label 为中文。
5. 问题总数控制在 4-6 个，sortOrder 从 1 开始递增。
6. 只输出 JSON，不要代码围栏和额外说明。',
    '[{"name":"platformKey","required":true,"description":"平台key","example":"wechat"},{"name":"platformName","required":true,"description":"平台名","example":"微信公众号"},{"name":"platformTagline","required":true,"description":"平台一句话卖点","example":"深度长文平台，粉丝价值高"},{"name":"platformContentForm","required":true,"description":"内容形式，逗号分隔","example":"长文章"},{"name":"platformMonetization","required":true,"description":"主要收益，逗号分隔","example":"流量主广告,赞赏,付费阅读,私域转化"},{"name":"platformBestFor","required":true,"description":"适合谁","example":"有专业积累、能持续输出深度内容的人"}]',
    1,
    0,
    '自媒体方案：根据平台生成问题与选项（v2，时间投入改为每日可用小时）'
);

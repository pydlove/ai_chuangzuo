SET NAMES utf8mb4;

INSERT IGNORE INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'platform_account_recommend_v1',
    '自媒体方案-账号昵称推荐',
    'user',
    'platform_account',
    '你是一位资深的自媒体账号定位顾问，擅长根据平台特点和账号定位为用户生成合适的账号昵称和简介。你只输出合法 JSON。',
    '请根据用户的自媒体定位，为其生成一个合适的账号昵称和一段账号简介（bio）。

【平台】
{{platform}}

【自媒体定位摘要】
{{positioning}}

【输出 JSON 结构】
{"nickname": "推荐昵称", "bio": "账号简介，控制在 80 字以内"}

要求：
1. 昵称必须与 platform 和 positioning 高度相关，让用户一眼知道账号价值。
2. 昵称控制在 12 个汉字或 24 个字符以内，易读易记、无歧义、避免生僻字和敏感词。
3. 简介说明账号价值、内容方向或人设特点，符合该平台调性。
4. 只输出一个合法 JSON 对象，不要任何前言、说明、免责声明、markdown 标题或代码围栏。
5. 第一个字符必须是 {，最后一个字符必须是 }。',
    '[{"name":"platform","required":true,"description":"平台显示名","example":"小红书"},{"name":"positioning","required":true,"description":"自媒体定位摘要，包含平台/赛道/人设/内容支柱","example":"平台：小红书；赛道：35+ 职场转型；人设：实战派转型顾问；内容支柱：转型复盘 60%，工具方法 20%，案例解读 20%。"}]',
    1,
    0,
    '自媒体方案：根据定位推荐账号昵称和简介'
);

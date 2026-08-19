SET NAMES utf8mb4;

INSERT IGNORE INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'platform_account_check_v1',
    '自媒体方案-账号昵称检测',
    'user',
    'platform_account',
    '你是一位资深的自媒体账号命名顾问，擅长根据平台特点和账号定位判断昵称是否合适，并给出更贴切的替代方案。你只输出合法 JSON。',
    '请根据用户的自媒体定位，判断以下昵称是否与该定位契合；如果不契合，请给出 3 个更合适的昵称建议。

【平台】
{{platform}}

【自媒体定位摘要】
{{positioning}}

【待检测昵称】
{{nickname}}

【判定维度】
1. 易读易记：是否简洁、朗朗上口、无歧义、无过长英文/数字混用。
2. 贴合定位：是否与平台、赛道、人设、内容支柱高度相关，让用户一眼知道账号价值。
3. 平台调性：是否符合该平台的命名习惯（小红书偏亲切/有身份感，公众号偏专业/IP 感，抖音偏短平快/有情绪，知乎偏真实/专家感，今日头条/百家号偏领域关键词，B 站偏兴趣/圈层感）。
4. 辨识度：是否有一定独特性，避免过于泛化或与已有大号重名风险。

【输出 JSON 结构】
{"fit": true/false, "reason": "判定理由，控制在 120 字以内", "suggestions": ["建议昵称 1", "建议昵称 2", "建议昵称 3"]}

要求：
1. fit=true 表示昵称契合定位，reason 说明契合点；fit=false 表示不够契合，reason 说明问题所在，并必须在 suggestions 给出 3 个建议昵称。
2. suggestions 中的昵称必须与 platform 和 positioning 高度相关，不要泛泛而谈。
3. 建议昵称控制在 12 个汉字或 24 个字符以内，避免生僻字和敏感词。
4. 只输出一个合法 JSON 对象，不要任何前言、说明、免责声明、markdown 标题或代码围栏。
5. 第一个字符必须是 {，最后一个字符必须是 }。',
    '[{"name":"platform","required":true,"description":"平台显示名","example":"小红书"},{"name":"positioning","required":true,"description":"自媒体定位摘要，包含平台/赛道/人设/内容支柱","example":"平台：小红书；赛道：35+ 职场转型；人设：实战派转型顾问；内容支柱：转型复盘 60%，工具方法 20%，案例解读 20%。"},{"name":"nickname","required":true,"description":"待检测的账号昵称","example":"阿伟的日常"}]',
    1,
    0,
    '自媒体方案：根据定位检测账号昵称并给出建议'
);


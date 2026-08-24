SET NAMES utf8mb4;

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'publish_plan_guide_v1',
    '发布计划建议',
    'user',
    'publish_plan',
    '你是一位熟悉国内自媒体平台算法规律和内容分发策略的运营顾问。请根据用户提供的运营方案、文章标题和主攻平台，给出具体可执行的发布计划，包括主平台最佳发布时间，以及适合一文多发的其他平台。输出必须是合法 JSON，不要任何额外说明。',
    '请根据用户的自媒体运营方案和文章信息，生成一份具体的发文计划。

【运营方案】
主攻平台：{{platformName}}
赛道：{{nicheName}}
人设：{{personaName}}
内容支柱：{{contentPillars}}

【本次内容】
文章标题：{{articleTitle}}
主发平台：{{mainPlatform}}

【可选平台】
微信公众号、小红书、今日头条、百家号、知乎、抖音、B站。主发平台已确定，一文多发请从剩余平台中选择 2-4 个最契合本内容定位的平台。

【输出 JSON 结构】
{
  "mainPlatform": {
    "platform": "主发平台名称",
    "publishTime": "具体发布时间，如 今晚 20:00 或 明天 07:30",
    "reason": "选择该时间和平台的理由，60 字以内"
  },
  "reposts": [
    {
      "platform": "平台名称",
      "publishTime": "具体发布时间，如 明天 09:00",
      "title": "适合该平台的标题，可直接复制发布，30 字以内",
      "tags": ["标签1", "标签2", "标签3"],
      "imageSuggestions": "该平台需要补充的配图/封面建议，如封面形式、配图数量、视觉风格，60 字以内"
    }
  ]
}

要求：
1. publishTime 必须给出具体、可操作的时间点，而不是泛泛的“上午/晚上”。
2. 主平台发布时间要结合该平台的流量高峰和冷启动效率，给出最近一个合适的时段。
3. 一文多发平台要选择与主平台内容形式互补、受众重合度高、且适合本次标题/赛道的平台；不要硬凑不相关平台。
4. 各平台发布时间要考虑用户阅读习惯和平台审核/推荐周期，形成时间差（例如主平台首发后 30 分钟到 2 小时内同步其他平台）。
5. reposts 中的 title 是为该平台专门拟定的发布标题，不要修改正文内容，只给标题。
6. tags 给 3-5 个该平台热度高且与内容相关的标签，不要加 # 符号。
7. imageSuggestions 只说明需要补充什么样的封面/配图，不要要求修改正文内容。
8. 一文多发方案只给出标题、标签、发布时间和配图建议，不需要让修改文章什么内容。
9. 只输出一个合法 JSON 对象，不要前言、说明、免责声明、markdown 标题或代码围栏。
10. 第一个字符必须是 {，最后一个字符必须是 }。',
    '[{"name":"platformName","required":true,"description":"主攻平台名称","example":"小红书"},{"name":"nicheName","required":true,"description":"赛道名称","example":"职场转型"},{"name":"personaName","required":true,"description":"人设名称","example":"实战派转型顾问"},{"name":"contentPillars","required":true,"description":"内容支柱","example":"转型复盘 60%，工具方法 20%，案例解读 20%"},{"name":"articleTitle","required":true,"description":"文章标题","example":"35+ 被裁员后，我用这 3 个方法半年内转型自由职业"},{"name":"mainPlatform","required":true,"description":"主发平台","example":"小红书"}]',
    1,
    0,
    '用户端：根据运营方案生成文章发布计划（一文多发给出标题、标签、发布时间和配图建议）'
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

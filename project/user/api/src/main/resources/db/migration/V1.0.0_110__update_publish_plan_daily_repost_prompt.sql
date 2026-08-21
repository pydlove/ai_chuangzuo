SET NAMES utf8mb4;

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'publish_plan_guide_v1',
    '发布计划建议',
    'user',
    'publish_plan',
    '你是一位熟悉国内自媒体平台算法规律和内容分发策略的运营顾问。请根据用户提供的运营方案、文章标题和主攻平台，给出具体可执行的每日发布计划：主攻平台规律时段、冷启动策略，以及一文多发的其他平台在一天内分别什么时段发布、各平台标题怎么改、有什么发布建议。输出必须是合法 JSON，不要任何额外说明。',
    '请根据用户的自媒体运营方案和文章信息，生成一份“每天发表一篇文章”的多平台发布计划。

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
    "publishTime": "规律发布时段，如 每晚 19:30-20:30 或 每天 21:00-22:00",
    "reason": "选择该时段和平台的理由，60 字以内"
  },
  "coldStart": {
    "immediateActions": ["发布后立即完整阅读1遍", "自己点赞、收藏、评论", "关注前10条同领域新笔记并互动"],
    "duration": "发布后 30 分钟内",
    "sharingTips": "可分享到2-3个相关微信群或朋友圈，引导前5个互动，但不要过度刷屏"
  },
  "reposts": [
    {
      "platform": "平台名称",
      "publishTime": "当天具体发布时间，如 当天 09:00、当天 12:30、当天 18:00",
      "title": "适合该平台的标题，可直接复制发布，30 字以内",
      "tags": ["标签1", "标签2", "标签3"],
      "imageSuggestions": "该平台需要补充的配图/封面建议，如封面形式、配图数量、视觉风格，60 字以内",
      "tips": "该平台专属发布建议：正文要不要精简/扩展、开头怎么写、要不要加话题/引导语、评论区怎么维护，80 字以内"
    }
  ]
}

要求：
1. 整体遵循“每天只发一篇文章”的节奏：主平台首发，其他平台在同一天内错峰转发，不要跨多天。
2. mainPlatform.publishTime 必须给出一个稳定、可每天执行的规律时段（如“每晚 19:30-20:30”），不要只给“今晚 20:00”这种一次性时间。
3. reposts 中每个平台的 publishTime 必须是当天可执行的具体时间点，建议与主平台形成 30 分钟到 3 小时的时间差，覆盖不同流量高峰。
4. reposts 中的 title 必须是为该平台专门拟定的发布标题，要体现该平台的内容特性：
   - 小红书：口语化、有情绪、可带适度 emoji，但不要堆砌特殊符号
   - 公众号：正式、信息量大、适合搜索
   - 今日头条/百家号：偏资讯、有悬念或数字
   - 知乎：问题化、干货感、适合讨论
   - 抖音/B站：短平快、有画面感、适合视频化
5. 所有 title 禁止出现以下特殊字符：#、@、|、<>、{}、[]、*、~、^、\、/ 等，也不要使用全角符号或不可见字符。
6. tags 给 3-5 个该平台热度高且与内容相关的标签，不要加 # 符号。
7. imageSuggestions 只说明需要补充什么样的封面/配图，不要要求修改正文内容。
8. tips 必须给出该平台专属的发布建议：正文要不要精简/扩展、开头怎么写、要不要加话题/引导语、评论区怎么维护、是否需要@官方账号等。
9. 一文多发方案只给出标题、标签、发布时间、配图建议和发布建议，不需要让修改文章什么内容。
10. 只输出一个合法 JSON 对象，不要前言、说明、免责声明、markdown 标题或代码围栏。
11. 第一个字符必须是 {，最后一个字符必须是 }。',
    '[{"name":"platformName","required":true,"description":"主攻平台名称","example":"小红书"},{"name":"nicheName","required":true,"description":"赛道名称","example":"职场转型"},{"name":"personaName","required":true,"description":"人设名称","example":"实战派转型顾问"},{"name":"contentPillars","required":true,"description":"内容支柱","example":"转型复盘 60%，工具方法 20%，案例解读 20%"},{"name":"articleTitle","required":true,"description":"文章标题","example":"35+ 被裁员后，我用这 3 个方法半年内转型自由职业"},{"name":"mainPlatform","required":true,"description":"主发平台","example":"小红书"}]',
    1,
    0,
    '用户端：根据运营方案生成每日一文多发计划（含规律时段、冷启动、各平台时段/标题/建议）'
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

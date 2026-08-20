SET NAMES utf8mb4;

UPDATE c_ai_prompt
SET user_prompt = '请根据用户的自媒体定位，为其生成 3 个合适的账号昵称方案，每个方案包含昵称和账号简介（bio）。

【平台】
{{platform}}

【自媒体定位摘要】
{{positioning}}

【输出 JSON 结构】
{"options": [{"nickname": "推荐昵称1", "bio": "账号简介1，控制在 80 字以内"}, {"nickname": "推荐昵称2", "bio": "账号简介2"}, {"nickname": "推荐昵称3", "bio": "账号简介3"}]}

要求：
1. 三个昵称必须与 platform 和 positioning 高度相关，让用户一眼知道账号价值，且风格/侧重点有所差异。
2. 每个昵称控制在 12 个汉字或 24 个字符以内，易读易记、无歧义、避免生僻字和敏感词。
3. 每个简介说明账号价值、内容方向或人设特点，符合该平台调性。
4. 只输出一个合法 JSON 对象，不要任何前言、说明、免责声明、markdown 标题或代码围栏。
5. 第一个字符必须是 {，最后一个字符必须是 }。'
WHERE prompt_code = 'platform_account_recommend_v1';

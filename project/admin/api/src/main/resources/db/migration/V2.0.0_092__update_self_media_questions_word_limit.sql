SET NAMES utf8mb4;

UPDATE c_ai_prompt
SET user_prompt = '请根据以下平台信息，为用户生成 4-6 个用于制定自媒体方案的问题和选项。\n\n平台信息：\n- 平台 key：{{platformKey}}\n- 平台名称：{{platformName}}\n- 平台卖点：{{platformTagline}}\n- 内容形式：{{platformContentForm}}\n- 主要收益：{{platformMonetization}}\n- 适合人群：{{platformBestFor}}\n\n输出 JSON 结构：\n{"questions":[{"key":"英文标识","text":"问题文本","options":[{"key":"选项英文标识","label":"选项显示文本"}],"isRequired":true,"sortOrder":1}]}\n\n要求：\n1. key 使用英文小写+下划线。\n2. 问题要贴合该平台特点和变现路径，必须包含以下几类：内容形式偏好、时间投入、变现目标、是否愿意出镜、目标受众或已有经验/产品等。\n3. 关于时间投入的问题（key 固定为 time_commitment）必须询问“你每天能投入多少时间？”，选项按小时区间给出，例如：1小时以内、1-2小时、2-4小时、4小时以上。禁止问“一天能输出几篇”。\n4. 本平台单次生成文章字数上限为 3000 字，生成的问题和选项中不要引导用户选择超过 3000 字的内容篇幅，也不要询问用户希望的文章字数。\n5. 每个问题提供 3-6 个明确选项，选项 label 为中文。\n6. 问题总数控制在 4-6 个，sortOrder 从 1 开始递增。\n7. 只输出 JSON，不要代码围栏和额外说明。',
    description = '自媒体方案：根据平台生成问题与选项（v2，时间投入改为每日可用小时，且限制不提超过3000字）'
WHERE prompt_code = 'self_media_platform_questions_v2';

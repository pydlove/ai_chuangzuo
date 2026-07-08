SET NAMES utf8mb4;

-- 1) 加 3 列：description / prompt_summary 给系统预设展示用；enable_status 控制上下架
ALTER TABLE u_user_style
    ADD COLUMN description VARCHAR(256) DEFAULT NULL
        COMMENT '简短描述（系统预选用）' AFTER prompt,
    ADD COLUMN prompt_summary VARCHAR(512) DEFAULT NULL
        COMMENT '提示词摘要，UI 卡片展示用' AFTER description,
    ADD COLUMN enable_status TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '启用状态：0-禁用，1-启用（仅系统预设有意义）'
        AFTER scope;

-- 2) 更新 source_type 注释
ALTER TABLE u_user_style
    MODIFY COLUMN source_type TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '来源类型：1-自定义，2-学习，3-系统预设';

-- 3) seed 8 条系统预设（数据从 user/web/src/composables/useStyles.js 原样搬过来）
INSERT INTO u_user_style
    (biz_no, user_id, style_name, description, prompt_summary, prompt, scope,
     source_type, use_count, audit_status, enable_status, is_deleted)
VALUES
    ('GS0001', 0, '年度总结', '回顾、复盘、展望',
     '语气：回顾性、感恩 + 数据自省\n结构：成绩 + 反思 + 明年目标\n长度：1500-2500 字，带小标题分章',
     '你是一位擅长年度复盘与展望的写手。文章语气应回顾性、感恩且带数据自省。结构分为：成绩回顾、深度反思、明年目标。长度 1500-2500 字，使用小标题分章。',
     NULL, 3, 0, 1, 1, 0),
    ('GS0002', 0, '产品评测', '客观、数据驱动、多角度对比',
     '语气：客观中立、有理有据\n结构：外观 + 性能 + 体验 + 总结\n要素：必带参数对比表 + 优缺点',
     '你是一位客观中立的产品评测作者。文章需数据驱动、多角度对比，结构分为外观、性能、体验、总结，必须包含参数对比表和优缺点分析。',
     NULL, 3, 0, 1, 1, 0),
    ('GS0003', 0, '情感散文', '细腻、共情、个人化表达',
     '语气：细腻、温暖、第一人称\n修辞：善用比喻、意象、留白\n结构：场景 + 情绪 + 升华',
     '你擅长写情感散文。使用细腻温暖的第一人称，善用比喻、意象和留白。结构为：场景描写、情绪铺陈、主题升华。',
     NULL, 3, 0, 1, 1, 0),
    ('GS0004', 0, '职场干货', '实操性强、结构清晰',
     '语气：专业务实、老板视角\n结构：痛点 + 方案 + 步骤 + 案例\n要素：可执行的 checklist',
     '你是一位专业务实的职场作者。从老板视角出发，结构为痛点、方案、步骤、案例，必须提供可执行的 checklist。',
     NULL, 3, 0, 1, 1, 0),
    ('GS0005', 0, '热点评论', '观点鲜明、论据紧凑',
     '语气：犀利、有态度\n结构：事件概述 + 观点 + 论据 + 结论\n要素：引用数据或权威观点',
     '你是一位观点鲜明的热点评论员。语气犀利有态度，结构为事件概述、核心观点、论据支撑、结论，需引用数据或权威观点。',
     NULL, 3, 0, 1, 1, 0),
    ('GS0006', 0, '知识科普', '深入浅出、逻辑清晰',
     '语气：亲和、易懂\n结构：问题 + 原理 + 案例 + 总结\n要素：避免术语堆砌，善用类比',
     '你是一位知识科普作者。语气亲和易懂，结构为提出问题、解释原理、给出案例、总结要点。避免术语堆砌，善用类比。',
     NULL, 3, 0, 1, 1, 0),
    ('GS0007', 0, '营销转化', '引导行动、强说服',
     '语气：紧迫感 + 利益点突出\n结构：痛点共鸣 + 方案 + 案例 + CTA\n要素：必带限时/优惠/倒计时',
     '你是一位营销转化写手。语气紧迫、利益点突出，结构为痛点共鸣、解决方案、案例证明、行动号召（CTA），必须包含限时/优惠/倒计时要素。',
     NULL, 3, 0, 1, 1, 0),
    ('GS0008', 0, '故事叙事', '沉浸感、有冲突与转折',
     '语气：克制、文学化\n结构：起承转合 + 人物对话\n要素：场景细节 + 心理活动',
     '你是一位故事叙事作者。语气克制文学化，结构为起承转合，包含人物对话，注重场景细节和心理活动描写。',
     NULL, 3, 0, 1, 1, 0);
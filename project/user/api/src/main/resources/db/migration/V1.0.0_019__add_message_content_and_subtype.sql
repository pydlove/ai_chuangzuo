-- 消息表新增 content(完整正文)与 sub_type(子类型)字段
-- 拆成两条 ALTER:第一段只加列(INSTANT,只改 metadata),第二段加索引(INPLACE,轻量锁表)
-- 注意:不能合并执行,合并后整体会落到 COPY 算法,锁整张表

ALTER TABLE u_message
    ADD COLUMN content MEDIUMTEXT NULL COMMENT '完整正文,summary 是列表摘要,弹框用',
    ADD COLUMN sub_type VARCHAR(32) NULL COMMENT '子类型,如 membership.subscribed / membership.expiring';

ALTER TABLE u_message
    ADD KEY idx_message_subtype (sub_type);

-- 回填 3 类广播的 content(每条 1-3KB 富文本样例,验证长文滚动)
UPDATE u_message SET content = '尊敬的用户:\n\n为提供更稳定的服务,爱创作将于本月 30 日 22:00-23:00 进行系统维护。\n\n维护期间以下功能将暂时不可用:\n1. 创作币提现\n2. 新用户注册\n3. 订单支付\n\n已提交的内容生成任务将正常完成,不受影响。维护结束后所有功能将自动恢复,无需任何额外操作。\n\n如您在使用过程中遇到任何问题,可通过以下方式联系我们:\n- 在线客服(工作日 9:00-18:00)\n- 微信公众号:爱创作助手\n- 邮箱:support@aichuangzuo.com\n\n感谢您对爱创作的支持与理解,我们将继续努力为您提供更优质的服务。' WHERE id = 1;

UPDATE u_message SET content = '亲爱的小红薯们:\n\n期待已久的功能终于上线啦!在预览页右上角,您将看到新增的「标题优化器」按钮。\n\n【核心功能】\n- AI 一键生成多平台爆款标题\n- 同时给出 5-8 个备选方案,覆盖不同风格\n- 支持小红书、抖音、公众号、知乎、B 站等主流平台\n- 实时显示预估点击率与平台推荐指数\n\n【使用方式】\n1. 打开任意作品预览页\n2. 鼠标悬停到标题右侧的魔法棒图标\n3. 选择目标平台,AI 将自动生成候选标题\n4. 一键替换或微调后保存\n\n【小贴士】\n- 不同平台的爆款公式差异很大,建议针对每个平台单独优化\n- 标题字数建议控制在 18-25 字\n- 加入数字、情感词、疑问句可显著提升点击率\n\n立即体验吧,让您的作品获得更多曝光!' WHERE id = 2;

UPDATE u_message SET content = '【年会员限时 7 折】\n\n活动时间:即日起至本月底 23:59\n\n【限时特惠】\n原价 299 元/年,现仅需 199 元/年,平均每天不到 0.55 元!\n\n【年会员专享权益】\n✓ 无限次 AI 内容生成(免费版每月仅 20 次)\n✓ 多平台爆款标题优化\n✓ 优先体验新功能(每月至少 1 个新功能)\n✓ 专属客服 1V1 答疑\n✓ 创作币每月自动到账 100 枚\n✓ 高级模板库全部解锁\n✓ 导出无水印高清大图\n\n【活动规则】\n1. 仅限新购或续费用户参与\n2. 每人限购 1 次,不可叠加其他优惠\n3. 购买后立即生效,有效期自购买日起 365 天\n4. 活动结束后将恢复原价,不再另行通知\n\n【立即开通】\n点击右上角「开通会员」按钮 → 选择「年会员」→ 完成支付\n\n机不可失,时不再来!' WHERE id = 3;

-- 新增 1 条 membership.expiring 广播(用于演示续订弹框)
INSERT INTO u_message (msg_type, scope, sub_type, target_user_id, title, summary, content, link_url, created_at) VALUES
('membership', 1, 'expiring', NULL, '您的会员即将到期', '续订后可继续享受全部会员权益',
 '亲爱的用户:\n\n您的会员有效期即将结束,部分专属权益可能受到影响。\n\n【续订后可继续享受】\n1. 无限次 AI 内容生成(免费版每月限 20 次)\n2. 多平台爆款标题优化器\n3. 高级模板库全部解锁(共 30+ 套)\n4. 创作币每月自动到账 100 枚\n5. 优先体验新功能\n6. 专属客服 1V1 答疑\n\n【续订福利】\n即日起至月底,年会员续费立享 8 折,仅需 239 元,平均每天不到 0.66 元。\n\n【特别提醒】\n到期后您仍可正常使用免费版功能,但创作币发放、多平台优化等会员权益将自动停止。建议您提前续订,避免影响创作节奏。\n\n如有疑问,可通过在线客服或微信公众号「爱创作助手」联系我们。\n\n期待与您继续同行!',
 NULL, DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 2 MINUTE));

SET NAMES utf8mb4;

ALTER TABLE c_platform
    ADD COLUMN tagline VARCHAR(256) DEFAULT NULL COMMENT '一句话卖点，如「图文种草社区，女性用户多，适合分享生活经验」',
    ADD COLUMN content_form_json JSON DEFAULT NULL COMMENT '内容形式，如 ["图文笔记","短视频"]',
    ADD COLUMN monetization_json JSON DEFAULT NULL COMMENT '主要收益，如 ["品牌广告","带货分佣"]',
    ADD COLUMN threshold VARCHAR(256) DEFAULT NULL COMMENT '变现门槛',
    ADD COLUMN best_for VARCHAR(256) DEFAULT NULL COMMENT '适合谁',
    ADD COLUMN reason VARCHAR(512) DEFAULT NULL COMMENT '提示/推荐理由',
    ADD COLUMN monetization_ease VARCHAR(32) DEFAULT NULL COMMENT '变现难度，如 中等',
    ADD COLUMN time_to_income VARCHAR(32) DEFAULT NULL COMMENT '预计周期，如 2-4个月',
    ADD COLUMN income_range VARCHAR(64) DEFAULT NULL COMMENT '收入空间，如 几千~几万/月',
    ADD COLUMN difficulty VARCHAR(16) DEFAULT NULL COMMENT '运营难度，如 中';

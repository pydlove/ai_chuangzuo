 SET NAMES utf8mb4;
 
 CREATE TABLE IF NOT EXISTS c_platform (
     id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
     platform_key VARCHAR(32) NOT NULL COMMENT '平台唯一键，如 wechat / xiaohongshu',
     platform_name VARCHAR(64) NOT NULL COMMENT '平台显示名，如 公众号',
     description VARCHAR(256) DEFAULT NULL COMMENT '平台简介',
     recommend_words INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '推荐字数',
     trait VARCHAR(512) DEFAULT NULL COMMENT '平台风格/特征描述',
     word_count_presets_json JSON DEFAULT NULL COMMENT '平台专属字数档位配置 JSON',
     sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
     status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
     is_default TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认选中：0-否，1-是',
     icon_url VARCHAR(512) DEFAULT NULL COMMENT '平台图标 URL',
     is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
     created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
     created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID，0表示系统或未知',
     updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
     updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID，0表示系统或未知',
     deleted_at DATETIME(3) DEFAULT NULL COMMENT '删除时间，NULL表示未删除',
     PRIMARY KEY (id),
     UNIQUE KEY uk_c_platform_platform_key (platform_key),
     KEY idx_c_platform_status_sort_order (status, sort_order)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自媒体平台配置表';
 
 -- 种子数据：与当前前端默认平台保持一致
 INSERT INTO c_platform (platform_key, platform_name, description, recommend_words, trait, word_count_presets_json, sort_order, status, is_default, icon_url)
 VALUES
 ('wechat', '公众号', '深度长文，适合专业内容输出', 1500, '长文深度阅读，段落完整，适合观点输出',
  JSON_ARRAY(
   JSON_OBJECT('count', 500, 'label', '短讯 / 快讯'),
   JSON_OBJECT('count', 800, 'label', '早报 / 简评'),
   JSON_OBJECT('count', 1500, 'label', '标准深度文'),
   JSON_OBJECT('count', 2500, 'label', '专题报道'),
   JSON_OBJECT('count', 3000, 'label', '行业研究（上限）')
  ), 10, 1, 1, NULL),
 ('xiaohongshu', '小红书', '轻松图文，种草安利效果好', 800, '短段落多 emoji，自动带话题标签',
  JSON_ARRAY(
   JSON_OBJECT('count', 300, 'label', '标题种草'),
   JSON_OBJECT('count', 500, 'label', '图文分享'),
   JSON_OBJECT('count', 800, 'label', '详细测评'),
   JSON_OBJECT('count', 1200, 'label', '步骤拆解教程')
  ), 20, 1, 0, NULL),
 ('toutiao', '今日头条', '算法分发，热点资讯类内容', 800, '算法友好，热点资讯提示词',
  JSON_ARRAY(
   JSON_OBJECT('count', 400, 'label', '热点快讯'),
   JSON_OBJECT('count', 800, 'label', '事件报道'),
   JSON_OBJECT('count', 1500, 'label', '专题分析'),
   JSON_OBJECT('count', 2000, 'label', '观点长文')
  ), 30, 1, 0, NULL),
 ('baijiahao', '百家号', '多平台分发，SEO友好', 1500, 'SEO 友好，知识科普调性',
  JSON_ARRAY(
   JSON_OBJECT('count', 500, 'label', '短科普'),
   JSON_OBJECT('count', 1000, 'label', '知识科普'),
   JSON_OBJECT('count', 1500, 'label', '生活攻略'),
   JSON_OBJECT('count', 2000, 'label', '人文叙事'),
   JSON_OBJECT('count', 2500, 'label', '行业洞察')
  ), 40, 1, 0, NULL),
 ('douyin', '抖音图文', '短视频+图文，流量大', 300, '图配文短文案，金句为主',
  JSON_ARRAY(
   JSON_OBJECT('count', 150, 'label', '封面金句'),
   JSON_OBJECT('count', 300, 'label', '图配文'),
   JSON_OBJECT('count', 600, 'label', '情感短篇')
  ), 50, 1, 0, NULL),
 ('zhihu', '知乎', '深度问答，专业知识分享', 1500, '专业问答体，逻辑严谨',
  JSON_ARRAY(
   JSON_OBJECT('count', 500, 'label', '短文'),
   JSON_OBJECT('count', 1000, 'label', '中等'),
   JSON_OBJECT('count', 1500, 'label', '标准'),
   JSON_OBJECT('count', 2500, 'label', '长文')
  ), 60, 1, 0, NULL),
 ('bilibili', 'B站', '专栏图文，年轻兴趣社区', 1500, '专栏图文，年轻社区语气',
  JSON_ARRAY(
   JSON_OBJECT('count', 500, 'label', '短篇动态'),
   JSON_OBJECT('count', 800, 'label', '动态短文'),
   JSON_OBJECT('count', 1500, 'label', '科普专栏'),
   JSON_OBJECT('count', 2500, 'label', '深度评测'),
   JSON_OBJECT('count', 3000, 'label', '连载长文')
  ), 70, 1, 0, NULL)
 ON DUPLICATE KEY UPDATE
     platform_name = VALUES(platform_name),
     description = VALUES(description),
     recommend_words = VALUES(recommend_words),
     trait = VALUES(trait),
     word_count_presets_json = VALUES(word_count_presets_json),
     sort_order = VALUES(sort_order),
     status = VALUES(status),
     is_default = VALUES(is_default),
     icon_url = VALUES(icon_url);

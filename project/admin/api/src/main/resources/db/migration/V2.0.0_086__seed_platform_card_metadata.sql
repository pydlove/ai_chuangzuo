SET NAMES utf8mb4;

UPDATE c_platform
SET tagline = '图文种草社区，女性用户多，适合分享生活经验',
    content_form_json = JSON_ARRAY('图文笔记', '短视频'),
    monetization_json = JSON_ARRAY('品牌广告', '带货分佣', '私域引流'),
    threshold = '0粉可带货，1000粉可接蒲公英商单',
    best_for = '有生活经验、愿意分享好物/干货的人',
    reason = '种草转化率高，起号相对快，但对封面和标题要求高',
    monetization_ease = '中等',
    time_to_income = '2-4个月',
    income_range = '几千~几万/月',
    difficulty = '中'
WHERE platform_key = 'xiaohongshu';

UPDATE c_platform
SET tagline = '深度长文平台，粉丝价值高，适合做私域沉淀',
    content_form_json = JSON_ARRAY('长文章'),
    monetization_json = JSON_ARRAY('流量主广告', '赞赏', '付费阅读', '私域转化'),
    threshold = '100粉丝可开通流量主',
    best_for = '有专业积累、能持续输出深度内容的人',
    reason = '粉丝粘性最强，变现路径稳定，但需要长期坚持',
    monetization_ease = '较慢',
    time_to_income = '6个月以上',
    income_range = '几千~几万/月',
    difficulty = '高'
WHERE platform_key = 'wechat';

UPDATE c_platform
SET tagline = '算法推荐资讯平台，流量大，变现门槛低',
    content_form_json = JSON_ARRAY('文章', '微头条', '视频'),
    monetization_json = JSON_ARRAY('流量分成', '青云计划', '带货'),
    threshold = '0粉即可参与流量分成',
    best_for = '想快速获得流量、擅长追热点的人',
    reason = '发文即有推荐流量，新手容易看到正反馈',
    monetization_ease = '容易',
    time_to_income = '1-2个月',
    income_range = '几百~几千/月',
    difficulty = '低'
WHERE platform_key = 'toutiao';

UPDATE c_platform
SET tagline = '百度生态内容平台，搜索流量长尾稳定',
    content_form_json = JSON_ARRAY('文章', '短视频', '动态'),
    monetization_json = JSON_ARRAY('广告分成', '带货', '付费专栏'),
    threshold = '转正后（持续发文）可开广告分成',
    best_for = '擅长图文、希望内容长期被搜索到的人',
    reason = '百度搜索能带来长期被动流量，适合干货类内容',
    monetization_ease = '容易',
    time_to_income = '2-3个月',
    income_range = '几百~几千/月',
    difficulty = '低'
WHERE platform_key = 'baijiahao';

UPDATE c_platform
SET tagline = '专业问答社区，长尾流量强，适合建立专业信任',
    content_form_json = JSON_ARRAY('问答', '文章', '视频'),
    monetization_json = JSON_ARRAY('好物推荐', '付费咨询', '品牌任务', '盐选专栏'),
    threshold = '创作者等级4级可开好物推荐',
    best_for = '有专业知识、能解答具体问题的人',
    reason = '一个问题可能持续带来流量，适合专业型 IP',
    monetization_ease = '中等',
    time_to_income = '3-6个月',
    income_range = '几千~几万/月',
    difficulty = '中'
WHERE platform_key = 'zhihu';

UPDATE c_platform
SET tagline = '短视频头部平台，流量天花板高，适合快速起量',
    content_form_json = JSON_ARRAY('短视频', '直播'),
    monetization_json = JSON_ARRAY('广告分成', '带货', '直播打赏', '本地生活'),
    threshold = '0粉可开橱窗，1000粉可直播带货',
    best_for = '愿意出镜、能做短视频的人',
    reason = '流量最大，但竞争激烈，对视频生产能力要求高',
    monetization_ease = '容易',
    time_to_income = '1-3个月',
    income_range = '几千~几十万/月',
    difficulty = '高'
WHERE platform_key = 'douyin';

UPDATE c_platform
SET tagline = '年轻兴趣社区，专栏+视频结合，适合圈层内容',
    content_form_json = JSON_ARRAY('专栏图文', '视频'),
    monetization_json = JSON_ARRAY('充电计划', '广告分成', '带货', '商单'),
    threshold = '创作激励需电磁力等级',
    best_for = '熟悉年轻文化、能持续产出内容的人',
    reason = '社区氛围好，适合兴趣圈层和系列化内容',
    monetization_ease = '中等',
    time_to_income = '3-6个月',
    income_range = '几百~几万/月',
    difficulty = '中'
WHERE platform_key = 'bilibili';

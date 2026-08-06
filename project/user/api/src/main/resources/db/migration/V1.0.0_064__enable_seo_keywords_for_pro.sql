SET NAMES utf8mb4;

-- SEO 关键词建议权益调整为专业版支持：basic 保持关闭，pro 开启，flagship 保持开启。
UPDATE u_plan_benefit SET benefit_value = 'true' WHERE plan_key = 'pro' AND benefit_code = 'seo_keywords';

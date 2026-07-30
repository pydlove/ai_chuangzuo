SET NAMES utf8mb4;

-- 小红书系列导出模板的默认末尾签名原先带平台名/品牌名（#小红书 #爱创作），
-- 用户反馈内置签名不应出现平台名。清空签名，由管理端按需自行配置。
UPDATE a_export_template
   SET signature_text = NULL,
       updated_at     = CURRENT_TIMESTAMP(3)
 WHERE platform       = 'xiaohongshu'
   AND signature_text = '#小红书 #爱创作'
   AND is_deleted     = 0;

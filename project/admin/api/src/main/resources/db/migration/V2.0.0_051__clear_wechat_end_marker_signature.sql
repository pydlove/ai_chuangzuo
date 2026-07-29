SET NAMES utf8mb4;

-- 去掉 5 个 wechat 模板的「— 完 —」末尾签名。
-- 这些签名会让 stage 12 (ExportRenderStep) 在正文末尾追加结束标记，破坏正文自然收束。
-- 仅清空 signature_text，不动 signature_position（默认 'end' 不变），方便后续需要时再加。
-- 其他平台的品牌署名（(本文由爱创作生成) / #小红书 #爱创作 / —— 来自爱创作 ——）保留。

UPDATE a_export_template
   SET signature_text = NULL,
       updated_at = CURRENT_TIMESTAMP(3)
 WHERE template_key IN ('wechat', 'wechat-minimal', 'wechat-dialogue', 'wechat-brand', 'wechat-infographic')
   AND signature_text = '— 完 —';
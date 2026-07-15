/**
 * 导出模板注册表：创作页弹框和预览页共用。
 * 包含 30 个模板的元信息 + 视觉预设 + 平台签名。
 */

export const allTemplates = [
  { key: 'wechat', name: '公众号标准模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '简洁专业，适合深度长文' },
  { key: 'business', name: '商业财经模板', bgColor: '#fff', textColor: '#003a8c', desc: '专业严谨，适合商业分析' },
  { key: 'marketing', name: '营销推广模板', bgColor: '#fff', textColor: '#cf1322', desc: '吸引眼球，适合营销内容' },
  { key: 'academic', name: '学术论文模板', bgColor: '#fafaf5', textColor: '#1a1a1a', desc: '严谨规范，适合学术写作' },
  { key: 'toutiao', name: '头条号标准模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '算法友好，适合热点资讯' },
  { key: 'xiaohongshu', name: '小红书爆款模板', bgColor: '#fff', textColor: '#ff2442', desc: '种草安利，适合小红书风格' },
  { key: 'baijiahao', name: '百家号模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '多平台分发，适合资讯类' },
  { key: 'story', name: '故事叙事模板', bgColor: '#faf5ef', textColor: '#8b5e34', desc: '沉浸叙事，适合故事类内容' },
  { key: 'magazine', name: '杂志风模板', bgColor: '#fafafa', textColor: '#1a1a1a', desc: '精致排版，适合生活方式' },
  { key: 'card', name: '卡片式模板', bgColor: '#f5f5f5', textColor: '#1a1a1a', desc: '模块化展示，信息清晰' },
  { key: 'checklist', name: '清单攻略模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '步骤清晰，适合教程攻略' },
  { key: 'dark', name: '深色模式模板', bgColor: '#1a1a1a', textColor: '#fff', desc: '科技感强，适合技术内容' },
  { key: 'wechat-minimal', name: '公众号简约模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '简约留白，适合文艺内容' },
  { key: 'wechat-dialogue', name: '公众号对话模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '对话形式，适合访谈采访' },
  { key: 'wechat-brand', name: '品牌故事模板', bgColor: '#fff', textColor: '#07c160', desc: '品牌调性，适合企业宣传' },
  { key: 'wechat-infographic', name: '信息图模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '数据可视化，适合信息图解' },
  { key: 'xiaohongshu-list', name: '小红书清单模板', bgColor: '#fff', textColor: '#ff2442', desc: '清单形式，适合好物推荐' },
  { key: 'xiaohongshu-review', name: '小红书测评模板', bgColor: '#fff', textColor: '#ff2442', desc: '测评对比，适合产品体验' },
  { key: 'xiaohongshu-tutorial', name: '小红书教程模板', bgColor: '#fff', textColor: '#ff2442', desc: '步骤详细，适合教程分享' },
  { key: 'xiaohongshu-emotion', name: '小红书情感模板', bgColor: '#fff', textColor: '#ff2442', desc: '情感共鸣，适合成长分享' },
  { key: 'toutiao-news', name: '头条新闻模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '新闻体，适合资讯报道' },
  { key: 'toutiao-depth', name: '头条深度模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '深度分析，适合专题报道' },
  { key: 'toutiao-hot', name: '头条热点模板', bgColor: '#fff', textColor: '#ff6600', desc: '热点追踪，适合爆款文章' },
  { key: 'toutiao-qa', name: '头条问答模板', bgColor: '#fff', textColor: '#ff6600', desc: '问答形式，适合知识解答' },
  { key: 'baijiahao-science', name: '百家号科普模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '知识科普，适合科学讲解' },
  { key: 'baijiahao-history', name: '百家号历史模板', bgColor: '#fafafa', textColor: '#1a1a1a', desc: '人文历史，适合故事讲述' },
  { key: 'baijiahao-guide', name: '百家号攻略模板', bgColor: '#fff', textColor: '#1677ff', desc: '实用攻略，适合生活指南' },
  { key: 'douyin-graphic', name: '抖音图文金句模板', bgColor: '#f5f5f5', textColor: '#1a1a1a', desc: '金句卡片，适合短视频配套' },
  { key: 'douyin-quote', name: '抖音图文语录模板', bgColor: '#f5f5f5', textColor: '#1a1a1a', desc: '语录风格，适合励志文案' },
  { key: 'zhihu-answer', name: '知乎回答模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '专业回答，适合知识分享' }
]

export const templateLargeStyles = {
  wechat:    { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutBg: '#f6ffed', calloutBorder: '4px solid #07c160', calloutColor: '#262626' },
  business:  { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#003a8c', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d6e4ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#003a8c', headingSize: '16px', headingBorder: '3px solid #003a8c', headingPl: '10px', calloutBg: '#f0f5ff', calloutBorder: '3px solid #1677ff', calloutColor: '#003a8c' },
  marketing: { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#cf1322', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#ffccc7', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#cf1322', headingSize: '17px', headingBorder: '3px solid #cf1322', headingPl: '10px', calloutVariant: 'cta' },
  academic:  { bg: '#fafaf5', font: 'Georgia, "Songti SC", serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.9', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutBg: '#f5f5f0', calloutBorder: 'none', calloutColor: '#595959', numbered: true },
  toutiao:   { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  xiaohongshu:{ bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
  baijiahao: { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1677ff', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '2px solid #1677ff', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
  story:     { bg: '#faf5ef', font: 'Georgia, "Songti SC", serif', titleColor: '#8b5e34', titleSize: '22px', metaColor: '#a89378', metaBorder: '#e8dccb', bodyColor: '#3a2e22', bodySize: '15px', bodyLine: '1.95', headingColor: '#8b5e34', headingSize: '16px', headingBorder: '3px solid #d4a373', headingPl: '10px', calloutBg: '#f0e6d8', calloutBorder: 'none', calloutColor: '#8b5e34' },
  magazine:  { bg: '#fafafa', font: 'Georgia, "Songti SC", serif', titleColor: '#1a1a1a', titleSize: '26px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#595959', bodySize: '14px', bodyLine: '1.95', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '17px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#f5f5f5', calloutBorder: 'none', calloutColor: '#262626' },
  card:      { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '20px', metaColor: '#8c8c8c', metaBorder: '#e8e8e8', bodyColor: '#262626', bodySize: '13px', bodyLine: '1.7', headingColor: '#07c160', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
  checklist: { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#07c160', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutVariant: 'checklist' },
  dark:      { bg: '#1a1a1a', font: '-apple-system, sans-serif', titleColor: '#fff', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#333', bodyColor: '#d9d9d9', bodySize: '14px', bodyLine: '1.85', headingColor: '#95de64', headingSize: '16px', headingBorder: '3px solid #95de64', headingPl: '10px', calloutBg: '#262626', calloutBorder: 'none', calloutColor: '#95de64' },
  'wechat-minimal': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.9', headingColor: '#1a1a1a', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fafafa', calloutBorder: '1px solid #e8e8e8', calloutColor: '#595959' },
  'wechat-dialogue': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '20px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#07c160', headingSize: '14px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
  'wechat-brand': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#07c160', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#d9f7be', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#07c160', headingSize: '16px', headingBorder: '4px solid #07c160', headingPl: '10px', calloutBg: '#f6ffed', calloutBorder: '3px solid #07c160', calloutColor: '#07c160' },
  'wechat-infographic': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.75', headingColor: '#07c160', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
  'xiaohongshu-list': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'checklist' },
  'xiaohongshu-review': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff6600', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'xiaohongshu-tutorial': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
  'xiaohongshu-emotion': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.9', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fff0f3', calloutBorder: '1px solid #ffd1d9', calloutColor: '#262626' },
  'toutiao-news': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'toutiao-depth': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.85', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '2px solid #ff6600', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'toutiao-hot': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff6600', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutVariant: 'cta' },
  'toutiao-qa': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff6600', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff6600', headingSize: '16px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'baijiahao-science': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1677ff', headingSize: '16px', headingBorder: '4px solid #1677ff', headingPl: '10px', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
  'baijiahao-history': { bg: '#fafafa', font: 'Georgia, "Songti SC", serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.9', headingColor: '#1677ff', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '1px solid #1677ff', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
  'baijiahao-guide': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1677ff', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.75', headingColor: '#1677ff', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
  'douyin-graphic': { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '26px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#1a1a1a', bodySize: '15px', bodyLine: '1.75', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#fff', calloutBorder: '1px solid #d9d9d9', calloutColor: '#1a1a1a' },
  'douyin-quote': { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#1a1a1a', bodySize: '16px', bodyLine: '1.65', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#1a1a1a', calloutBorder: 'none', calloutColor: '#fff' },
  'zhihu-answer': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#0066ff', headingSize: '16px', headingBorder: '4px solid #0066ff', headingPl: '10px', calloutBg: '#f0f5ff', calloutBorder: '3px solid #0066ff', calloutColor: '#0066ff' }
}

/** 各平台 body 尾部/头部签名（与 ExportRenderStep 一致） */
const PLATFORM_SIGNATURES = {
  wechat: { position: 'end', text: '— 完 —' },
  xiaohongshu: { position: 'end', text: '#小红书 #爱创作' },
  toutiao: { position: 'end', text: '（本文由爱创作生成）' },
  zhihu: { position: 'start', text: '> 本文由爱创作 AI 生成。' },
  baijiahao: { position: 'end', text: '—— 来自爱创作 ——' }
}

const PLATFORM_PREFIXES = ['xiaohongshu', 'baijiahao', 'toutiao', 'wechat', 'zhihu', 'douyin']

/** 从模板 key 提取平台前缀，如 'xiaohongshu-list' → 'xiaohongshu'，'business' → 'general' */
export function getTemplatePlatform(key) {
  if (!key) return 'general'
  for (const p of PLATFORM_PREFIXES) {
    if (key === p || key.startsWith(p + '-')) return p
  }
  return 'general'
}

/** 取模板视觉预设，fallback 到 wechat */
export function getTemplateStyle(key) {
  return templateLargeStyles[key] || templateLargeStyles.wechat
}

/** 取模板元信息 */
export function getTemplateMeta(key) {
  return allTemplates.find(t => t.key === key) || null
}

/** 取模板对应平台的签名 */
export function getTemplateSignature(key) {
  const platform = getTemplatePlatform(key)
  return PLATFORM_SIGNATURES[platform] || null
}

/** 所有已知签名文本（用于从 body 中剥离） */
export const ALL_SIGNATURE_TEXTS = Object.values(PLATFORM_SIGNATURES).map(s => s.text)

/** 创作页弹框右侧的大图预览 HTML */
export function buildLargePreview(t) {
  const s = templateLargeStyles[t.key] || templateLargeStyles.wechat
  const titleAlign = s.titleAlign || 'left'
  const metaAlign = s.metaAlign || 'left'
  const bodyAlign = s.bodyAlign || 'left'
  const headingAlign = s.headingAlign || 'left'
  const headingPl = s.headingPl || '0'
  const headingBorderBottom = s.headingBorderBottom || 'none'

  let calloutHtml
  if (s.calloutVariant === 'cta') {
    calloutHtml = '<div style="background: #fff; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 6px; margin-top: 14px; border: 2px solid #cf1322; text-align: center;"><strong style="color: #cf1322;">限时优惠</strong> · 立即行动 · 别错过</div>'
  } else if (s.calloutVariant === 'pill') {
    calloutHtml = '<div style="background: #fff0f2; padding: 8px 14px; color: #ff2442; font-size: 13px; line-height: 1.6; border-radius: 20px; margin-top: 14px; display: inline-block;"><strong>核心要点：</strong>高效管理时间就是管理注意力</div>'
  } else if (s.calloutVariant === 'card') {
    calloutHtml = '<div style="background: #fff; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 8px; margin-top: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-left: 3px solid #07c160;"><strong style="color:#07c160;">关键结论：</strong>管理时间本质是管理注意力。</div>'
  } else if (s.calloutVariant === 'checklist') {
    calloutHtml = '<div style="background: #f6ffed; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.9; border-radius: 6px; margin-top: 14px;"><div style="color: #07c160; font-weight: 500;">✓ 列出今日最重要的 3 件事</div><div style="color: #07c160; font-weight: 500;">✓ 先完成最难的那一件</div><div style="color: #07c160; font-weight: 500;">✓ 时间块专注单线程</div></div>'
  } else {
    const borderStyle = s.calloutBorder === 'none' ? 'border: none;' : ('border-left: ' + s.calloutBorder + ';')
    calloutHtml = '<div style="background: ' + s.calloutBg + '; ' + borderStyle + ' padding: 12px 14px; color: ' + s.calloutColor + '; font-size: 13px; line-height: 1.6; border-radius: 0 6px 6px 0; margin-top: 14px;"><strong style="color:#1a1a1a;">关键结论：</strong>管理时间本质是管理注意力。</div>'
  }

  const headingStyleExtra = (headingBorderBottom !== 'none') ? ('padding-bottom: 6px; ') : ''
  const headingText = s.numbered ? '一、优先级排序：先做重要的事' : '01｜优先级排序：先做重要的事'

  return '<div style="background: ' + s.bg + '; padding: 24px; height: 100%; box-sizing: border-box; font-family: ' + s.font + '; overflow-y: auto; color: ' + s.bodyColor + ';">' +
    '<h1 style="font-size: ' + s.titleSize + '; font-weight: 700; color: ' + s.titleColor + '; margin: 0 0 12px; line-height: 1.4; text-align: ' + titleAlign + ';">如何高效管理时间</h1>' +
    '<div style="color: ' + s.metaColor + '; font-size: 12px; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid ' + s.metaBorder + '; text-align: ' + metaAlign + ';">2026-06-22 · 约 1500 字 · 风格:专业严谨</div>' +
    '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">时间对每个人来说都是公平的，但为什么有人能在 24 小时内完成更多事情？关键不在于你有多忙，而在于你如何管理注意力。</p>' +
    '<h3 style="font-size: ' + s.headingSize + '; font-weight: 600; color: ' + s.headingColor + '; border-left: ' + s.headingBorder + '; padding-left: ' + headingPl + '; border-bottom: ' + headingBorderBottom + '; ' + headingStyleExtra + 'margin: 18px 0 8px; text-align: ' + headingAlign + ';">' + headingText + '</h3>' +
    '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">很多人一早打开手机就被消息牵着走。高效的人会在每天开始前列出 3 件最重要的事，并优先完成它们。</p>' +
    calloutHtml +
    '</div>'
}

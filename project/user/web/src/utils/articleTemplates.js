/**
 * 创作页弹框右侧的大图预览 HTML。
 * 模板对象来自 useExportTemplates（API 加载），视觉样式取 t.visualStyle。
 */

import { DEFAULT_TEMPLATE_STYLE } from '@/composables/useExportTemplates.js'

export function buildLargePreview(t) {
  const s = (t && t.visualStyle) ? t.visualStyle : DEFAULT_TEMPLATE_STYLE
  const titleAlign = s.titleAlign || 'left'
  const metaAlign = s.metaAlign || 'left'
  const bodyAlign = s.bodyAlign || 'left'
  const headingAlign = s.headingAlign || 'left'
  const headingPl = s.headingPl || '0'
  const headingBorder = s.headingBorder || 'none'
  const headingBorderBottom = s.headingBorderBottom || 'none'
  const calloutBg = s.calloutBg || '#f6ffed'
  const calloutBorder = s.calloutBorder || 'none'
  const calloutColor = s.calloutColor || '#262626'

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
    const borderStyle = calloutBorder === 'none' ? 'border: none;' : ('border-left: ' + calloutBorder + ';')
    calloutHtml = '<div style="background: ' + calloutBg + '; ' + borderStyle + ' padding: 12px 14px; color: ' + calloutColor + '; font-size: 13px; line-height: 1.6; border-radius: 0 6px 6px 0; margin-top: 14px;"><strong style="color:#1a1a1a;">关键结论：</strong>管理时间本质是管理注意力。</div>'
  }

  const headingStyleExtra = (headingBorderBottom !== 'none') ? ('padding-bottom: 6px; ') : ''
  const headingText = s.numbered ? '一、优先级排序：先做重要的事' : '01｜优先级排序：先做重要的事'

  return '<div style="background: ' + s.bg + '; padding: 24px; height: 100%; box-sizing: border-box; font-family: ' + s.font + '; overflow-y: auto; color: ' + s.bodyColor + ';">' +
    '<h1 style="font-size: ' + s.titleSize + '; font-weight: 700; color: ' + s.titleColor + '; margin: 0 0 12px; line-height: 1.4; text-align: ' + titleAlign + ';">如何高效管理时间</h1>' +
    '<div style="color: ' + s.metaColor + '; font-size: 12px; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid ' + s.metaBorder + '; text-align: ' + metaAlign + ';">2026-06-22 · 约 1500 字 · 风格:专业严谨</div>' +
    '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">时间对每个人来说都是公平的，但为什么有人能在 24 小时内完成更多事情？关键不在于你有多忙，而在于你如何管理注意力。</p>' +
    '<h3 style="font-size: ' + s.headingSize + '; font-weight: 600; color: ' + s.headingColor + '; border-left: ' + headingBorder + '; padding-left: ' + headingPl + '; border-bottom: ' + headingBorderBottom + '; ' + headingStyleExtra + 'margin: 18px 0 8px; text-align: ' + headingAlign + ';">' + headingText + '</h3>' +
    '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">很多人一早打开手机就被消息牵着走。高效的人会在每天开始前列出 3 件最重要的事，并优先完成它们。</p>' +
    calloutHtml +
    '</div>'
}

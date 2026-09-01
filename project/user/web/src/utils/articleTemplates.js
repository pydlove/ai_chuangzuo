/**
 * 创作页弹框右侧的大图预览 HTML。
 * 模板对象来自 useExportTemplates（API 加载），视觉样式取 t.visualStyle。
 */

import { DEFAULT_TEMPLATE_STYLE } from '@/composables/useExportTemplates.js'

function buildTitleStyle(s) {
  const style = {
    'font-size': s.titleSize,
    'font-weight': s.titleFontWeight ?? 700,
    color: s.titleColor,
    'text-align': s.titleAlign || 'left',
    'line-height': s.titleLineHeight ?? 1.4,
    'letter-spacing': s.titleLetterSpacing ?? '0em',
    'margin-bottom': s.titleMarginBottom ?? '12px'
  }
  if (s.titleFontFamily) style['font-family'] = s.titleFontFamily
  if (s.titleFontStyle) style['font-style'] = s.titleFontStyle
  if (s.titleTextShadow) style['text-shadow'] = s.titleTextShadow
  if (s.titleBackground) style.background = s.titleBackground
  if (s.titlePadding) style.padding = s.titlePadding
  if (s.titleBorderRadius) style['border-radius'] = s.titleBorderRadius
  if (s.titleBorder) style.border = s.titleBorder
  if (s.titleTransform) style['text-transform'] = s.titleTransform
  if (s.titleDecoration) {
    style['text-decoration'] = s.titleDecoration
    if (s.titleDecorationColor) style['text-decoration-color'] = s.titleDecorationColor
    if (s.titleDecorationStyle) style['text-decoration-style'] = s.titleDecorationStyle
  }
  return styleToString(style)
}

function buildMetaStyle(s) {
  if (s.metaBackground) {
    return styleToString({
      color: s.metaColor,
      background: s.metaBackground,
      padding: s.metaPadding || '6px 12px',
      'border-radius': s.metaBorderRadius || '999px',
      'text-align': s.metaAlign || 'left',
      'font-size': '12px',
      'margin-bottom': '16px',
      display: 'inline-block'
    })
  }
  return styleToString({
    color: s.metaColor,
    'font-size': '12px',
    'margin-bottom': '16px',
    'padding-bottom': '10px',
    'border-bottom': `1px solid ${s.metaBorder}`,
    'text-align': s.metaAlign || 'left'
  })
}

function buildHeadingStyle(s) {
  const style = {
    'font-size': s.headingSize,
    'font-weight': s.headingFontWeight ?? 600,
    color: s.headingColor,
    margin: s.headingMargin ?? '18px 0 8px',
    'text-align': s.headingAlign || 'left'
  }
  if (s.headingFontFamily) style['font-family'] = s.headingFontFamily
  if (s.headingLetterSpacing) style['letter-spacing'] = s.headingLetterSpacing
  if (s.headingTextTransform) style['text-transform'] = s.headingTextTransform
  if (s.headingBackground) {
    style.background = s.headingBackground
    style.padding = s.headingPadding || '8px 12px'
    style['border-radius'] = s.headingBorderRadius || '6px'
  }
  if (s.headingBorder && s.headingBorder !== 'none') {
    style['border-left'] = s.headingBorder
    style['padding-left'] = s.headingPl ? `${s.headingPl}px` : '0'
  }
  if (s.headingBorderBottom) {
    style['border-bottom'] = s.headingBorderBottom
    style['padding-bottom'] = '6px'
  }
  if (s.headingTextShadow) style['text-shadow'] = s.headingTextShadow
  return styleToString(style)
}

function buildParagraphStyle(s) {
  return styleToString({
    'font-size': s.bodySize,
    'line-height': s.bodyLine,
    color: s.bodyColor,
    margin: '0 0 12px',
    'text-align': s.bodyAlign || 'left'
  })
}

function buildCalloutHtml(s) {
  const radius = s.calloutBorderRadius || '0 6px 6px 0'
  const shadow = s.calloutShadow || 'none'
  const base = `border-radius: ${radius}; box-shadow: ${shadow}; margin-top: 14px;`

  if (s.calloutVariant === 'cta') {
    return `<div style="background: ${s.calloutBg || '#fff'}; padding: 12px 14px; color: ${s.calloutColor || '#262626'}; font-size: 13px; line-height: 1.6; ${base} border: 2px solid ${s.calloutColor || '#cf1322'}; text-align: center; font-weight: 600;"><strong style="color: ${s.calloutColor || '#cf1322'};">限时优惠</strong> · 立即行动 · 别错过</div>`
  }
  if (s.calloutVariant === 'pill') {
    return `<div style="background: ${s.calloutBg || '#fff0f2'}; padding: 8px 14px; color: ${s.calloutColor || '#ff2442'}; font-size: 13px; line-height: 1.6; ${base} display: inline-block;"><strong>核心要点：</strong>高效管理时间就是管理注意力</div>`
  }
  if (s.calloutVariant === 'card') {
    return `<div style="background: ${s.calloutBg || '#fff'}; padding: 12px 14px; color: ${s.calloutColor || '#262626'}; font-size: 13px; line-height: 1.6; ${base} border-left: 3px solid ${s.headingColor || '#07c160'};"><strong style="color:${s.headingColor || '#07c160'};">关键结论：</strong>管理时间本质是管理注意力。</div>`
  }
  if (s.calloutVariant === 'checklist') {
    return `<div style="background: ${s.calloutBg || '#f6ffed'}; padding: 12px 14px; color: ${s.calloutColor || '#262626'}; font-size: 13px; line-height: 1.9; ${base}"><div style="color: ${s.headingColor || '#07c160'}; font-weight: 500;">✓ 列出今日最重要的 3 件事</div><div style="color: ${s.headingColor || '#07c160'}; font-weight: 500;">✓ 先完成最难的那一件</div><div style="color: ${s.headingColor || '#07c160'}; font-weight: 500;">✓ 时间块专注单线程</div></div>`
  }
  const borderStyle = s.calloutBorder && s.calloutBorder !== 'none' ? `border-left: ${s.calloutBorder};` : 'border: none;'
  return `<div style="background: ${s.calloutBg || '#f6ffed'}; ${borderStyle} padding: 12px 14px; color: ${s.calloutColor || '#262626'}; font-size: 13px; line-height: 1.6; ${base}"><strong style="color:#1a1a1a;">关键结论：</strong>管理时间本质是管理注意力。</div>`
}

function styleToString(obj) {
  return Object.entries(obj)
    .map(([k, v]) => `${k}: ${v}`)
    .join('; ')
}

export function buildLargePreview(t) {
  const s = (t && t.visualStyle) ? t.visualStyle : DEFAULT_TEMPLATE_STYLE
  const titleIcon = s.titleIcon ? `<span style="margin-right: 8px; font-size: 1.1em;">${s.titleIcon}</span>` : ''
  const headingText = s.numbered ? '一、优先级排序：先做重要的事' : '01｜优先级排序：先做重要的事'

  return '<div style="background: ' + s.bg + '; padding: 24px; height: 100%; box-sizing: border-box; font-family: ' + s.font + '; overflow-y: auto; color: ' + s.bodyColor + ';">' +
    '<h1 style="' + buildTitleStyle(s) + '">' + titleIcon + '如何高效管理时间</h1>' +
    '<div style="' + buildMetaStyle(s) + '">2026-06-22 · 约 1500 字 · 提示词：专业严谨</div>' +
    '<p style="' + buildParagraphStyle(s) + '">时间对每个人来说都是公平的，但为什么有人能在 24 小时内完成更多事情？关键不在于你有多忙，而在于你如何管理注意力。</p>' +
    '<h3 style="' + buildHeadingStyle(s) + '">' + headingText + '</h3>' +
    '<p style="' + buildParagraphStyle(s) + '">很多人一早打开手机就被消息牵着走。高效的人会在每天开始前列出 3 件最重要的事，并优先完成它们。</p>' +
    buildCalloutHtml(s) +
    '</div>'
}

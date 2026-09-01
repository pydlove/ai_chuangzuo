/**
 * Admin 端简化版：把 visual_style_json 字段反射到 inline style，
 * 渲染一篇占位文章做实时预览。
 *
 * 复用 user 端 PreviewIndex.vue 的渲染思路，但不跨包依赖 articleBlocks.js，
 * 自己实现一份。等 user 端改动时同步更新这一份。
 */

const FALLBACK_STYLE = {
  bg: '#fff',
  font: '-apple-system, sans-serif',
  titleColor: '#1a1a1a',
  titleSize: '22px',
  titleAlign: 'left',
  metaColor: '#8c8c8c',
  metaBorder: '#eee',
  metaAlign: 'left',
  bodyColor: '#262626',
  bodySize: '14px',
  bodyLine: '1.85',
  bodyAlign: 'left',
  headingColor: '#1a1a1a',
  headingSize: '16px',
  headingBorder: 'none',
  headingPl: '0',
  headingAlign: 'left',
  calloutBg: '#f6ffed',
  calloutBorder: '4px solid #07c160',
  calloutColor: '#262626',
  calloutVariant: null
}

export const DEFAULT_PREVIEW_STYLE = Object.freeze(FALLBACK_STYLE)

/**
 * 合并 visual_style_json 到兜底上：visualStyle 里有的字段覆盖兜底，没有的字段用兜底。
 */
export function mergeStyle(visualStyleJson) {
  if (!visualStyleJson) return { ...FALLBACK_STYLE }
  const parsed = typeof visualStyleJson === 'string' ? safeParse(visualStyleJson) : visualStyleJson
  if (!parsed || typeof parsed !== 'object') return { ...FALLBACK_STYLE }
  return { ...FALLBACK_STYLE, ...parsed }
}

function safeParse(s) {
  try {
    return JSON.parse(s)
  } catch (e) {
    return null
  }
}

function styleToString(obj) {
  return Object.entries(obj)
    .map(([k, v]) => `${k}: ${v}`)
    .join('; ')
}

export function buildTitleStyle(s) {
  const style = {
    'font-size': s.titleSize,
    'font-weight': s.titleFontWeight ?? 700,
    color: s.titleColor,
    'text-align': s.titleAlign || 'left',
    'line-height': s.titleLineHeight ?? 1.3,
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

export function buildMetaStyle(s) {
  if (s.metaBackground) {
    return styleToString({
      color: s.metaColor,
      background: s.metaBackground,
      padding: s.metaPadding || '6px 12px',
      'border-radius': s.metaBorderRadius || '999px',
      'text-align': s.metaAlign || 'left',
      'margin-bottom': '20px',
      display: 'inline-block'
    })
  }
  return styleToString({
    color: s.metaColor,
    'text-align': s.metaAlign || 'left',
    'padding-bottom': '12px',
    'margin-bottom': '20px',
    'border-bottom': `1px solid ${s.metaBorder}`
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

/**
 * 把占位文章 body 渲染成带 inline style 的 HTML 字符串。
 *
 * 支持的语法（和 user 端 PreviewIndex 一致）：
 * - ## / ###  → heading（H2/H3）
 * - >          → callout（按 calloutVariant 分 4 种样式）
 * - - / 1.    → list
 * - 空行       → 段落分隔
 * - 其他       → paragraph
 */
export function renderBodyHtml(body, style) {
  if (!body) return ''
  const s = style || FALLBACK_STYLE
  const parts = body.split(/\n\n+/)
  const html = parts.map((part) => renderPart(part.trim(), s)).filter(Boolean)
  return html.join('')
}

function renderPart(text, s) {
  if (!text) return ''

  const mdHeading = text.match(/^(#{1,6})\s+(.+)$/)
  if (mdHeading) {
    const level = Math.min(mdHeading[1].length, 3)
    const content = escapeHtml(mdHeading[2])
    return `<h${level} style="${buildHeadingStyle(s)}">${content}</h${level}>`
  }

  const calloutMatch = text.match(/^>\s+(.+)$/)
  if (calloutMatch) {
    return renderCallout(calloutMatch[1], s)
  }

  const listMatch = text.match(/^(?:[-•]|\d+\.)\s+(.*)$/)
  if (listMatch) {
    return `<ul style="margin: 8px 0; padding-left: 20px; color: ${s.bodyColor}; font-size: ${s.bodySize}; line-height: ${s.bodyLine};"><li>${escapeHtml(listMatch[1])}</li></ul>`
  }

  // 普通段落：段内换行转 <br>
  const lines = text.split('\n').map(escapeHtml)
  return `<p style="margin: 0 0 12px; font-size: ${s.bodySize}; line-height: ${s.bodyLine}; color: ${s.bodyColor}; text-align: ${s.bodyAlign || 'left'};">${lines.join('<br>')}</p>`
}

function renderCallout(content, s) {
  const variant = s.calloutVariant
  const escaped = escapeHtml(content)
  const radius = s.calloutBorderRadius || '0 6px 6px 0'
  const shadow = s.calloutShadow || 'none'
  const baseStyle = `margin: 12px 0; padding: 12px 14px; font-size: ${s.bodySize}; border-radius: ${radius}; box-shadow: ${shadow};`

  if (variant === 'pill') {
    return `<div style="${baseStyle} background: ${s.calloutBg}; text-align: center; color: ${s.calloutColor};">${escaped}</div>`
  }
  if (variant === 'card') {
    return `<div style="${baseStyle} background: ${s.calloutBg}; color: ${s.calloutColor}; border-left: 3px solid ${s.headingColor || '#07c160'};">${escaped}</div>`
  }
  if (variant === 'cta') {
    return `<div style="${baseStyle} background: ${s.calloutBg}; border: 2px solid ${s.calloutColor || '#ff4d4f'}; color: ${s.calloutColor}; text-align: center; font-weight: 600;">${escaped}</div>`
  }
  if (variant === 'checklist') {
    return `<div style="${baseStyle} background: ${s.calloutBg}; border-left: 4px solid ${s.headingColor || '#07c160'}; color: ${s.calloutColor};">✓ ${escaped}</div>`
  }
  // 默认：左竖线 + 浅底
  return `<div style="${baseStyle} background: ${s.calloutBg}; border-left: ${s.calloutBorder || '4px solid #07c160'}; color: ${s.calloutColor};">${escaped}</div>`
}

export function escapeHtml(text) {
  if (text == null) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/**
 * 占位文章：标题 + body。管理端编辑模板时用同一篇渲染，方便对比不同模板的视觉效果。
 */
export const SAMPLE_ARTICLE = Object.freeze({
  title: '工作 3 年没升职？可能是这 3 个习惯在拖后腿',
  description: '坦诚聊聊为什么「努力」不一定等于「被看见」，给 3 个可立即调整的工作习惯',
  body: [
    '上周和前同事吃饭，她说最近一次绩效面谈被 leader 点了一句：「你做的事不少，但存在感不够。」',
    '',
    '这话听着耳熟。我们身边总有这么一类同事：每天早出晚归，周报写得密密麻麻，工位上堆满草稿纸。但晋升名单里，他们的名字总是在最后一栏才出现。',
    '',
    '## 不是能力问题，是表达问题',
    '',
    '「努力」和「被看见」之间，隔着一段叫「汇报」的距离。',
    '',
    '> 关键不在你做了多少，而在 leader 知不知道你做了多少。',
    '',
    '## 3 个可以马上改的小习惯',
    '',
    '- 周报里加一段「本周最大的决策」，而不是流水账',
    '- 跨部门协作结束时，主动发一条 3 行的总结到群里',
    '- 月底找 leader 聊 15 分钟，反馈你的方向感',
    '',
    '## 写在最后',
    '',
    '这些习惯我都试过。说不上立竿见影，但半年后回头看，确实不一样。'
  ].join('\n')
})

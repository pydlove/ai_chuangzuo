const BLOCK_TYPES = {
  TITLE: 'title',
  HEADING: 'heading',
  PARAGRAPH: 'paragraph',
  HIGHLIGHT: 'highlight',
  LIST_ITEM: 'list-item'
}

const TYPE_LABELS = {
  [BLOCK_TYPES.TITLE]: '标题',
  [BLOCK_TYPES.HEADING]: '小标题',
  [BLOCK_TYPES.PARAGRAPH]: '正文段落',
  [BLOCK_TYPES.HIGHLIGHT]: '重点高亮',
  [BLOCK_TYPES.LIST_ITEM]: '列表项'
}

export function getBlockTypeLabel(type) {
  return TYPE_LABELS[type] || '内容'
}

/**
 * 把文章标题和正文解析为可编辑 block 数组
 * @param {string} title
 * @param {string} body
 * @returns {{ type: string, html: string }[]}
 */
export function parseBodyToBlocks(title, body) {
  const blocks = []

  if (title || title === '') {
    blocks.push({ type: BLOCK_TYPES.TITLE, html: escapeHtml(title) })
  }

  if (!body) return blocks

  const lines = body.split('\n')
  let listBuffer = []

  const flushList = () => {
    if (listBuffer.length === 0) return
    listBuffer.forEach(item => {
      blocks.push({ type: BLOCK_TYPES.LIST_ITEM, html: escapeHtml(item) })
    })
    listBuffer = []
  }

  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) return

    const headingMatch = trimmed.match(/^【([^】]+)】$/)
    if (headingMatch) {
      flushList()
      blocks.push({ type: BLOCK_TYPES.HEADING, html: escapeHtml(headingMatch[1]) })
      return
    }

    if (trimmed.startsWith('> ')) {
      flushList()
      blocks.push({ type: BLOCK_TYPES.HIGHLIGHT, html: escapeHtml(trimmed.slice(2)) })
      return
    }

    const listMatch = trimmed.match(/^(?:[-•]|\d+\.)\s+(.*)$/)
    if (listMatch) {
      listBuffer.push(listMatch[1])
      return
    }

    flushList()
    blocks.push({ type: BLOCK_TYPES.PARAGRAPH, html: escapeHtml(trimmed) })
  })

  flushList()
  return blocks
}

/**
 * 把 block 数组序列化为标题和正文
 * @param {{ type: string, html: string }[]} blocks
 * @returns {{ title: string, body: string }}
 */
export function serializeBlocksToArticle(blocks) {
  const titleBlock = blocks.find(b => b.type === BLOCK_TYPES.TITLE)
  const title = titleBlock ? stripHtml(titleBlock.html).trim() : ''

  const bodyBlocks = blocks.filter(b => b.type !== BLOCK_TYPES.TITLE)
  const parts = []

  for (let i = 0; i < bodyBlocks.length; i++) {
    const block = bodyBlocks[i]
    const text = stripHtml(block.html).trim()
    if (!text) continue

    switch (block.type) {
      case BLOCK_TYPES.HEADING:
        parts.push(`【${text}】`)
        break
      case BLOCK_TYPES.HIGHLIGHT:
        parts.push(`> ${text}`)
        break
      case BLOCK_TYPES.LIST_ITEM:
        parts.push(`- ${text}`)
        break
      case BLOCK_TYPES.PARAGRAPH:
      default:
        parts.push(text)
    }
  }

  return { title, body: parts.join('\n\n') }
}

function escapeHtml(text) {
  if (text == null) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function stripHtml(html) {
  if (html == null) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

export { BLOCK_TYPES }

export function bodyToHtmlWithStyles(body, styleOverrides) {
  const baseHtml = bodyToHtml(body || '')
  if (!styleOverrides || (!styleOverrides.blocks && !styleOverrides.inlines)) {
    return baseHtml
  }
  return applyStyleOverrides(baseHtml, styleOverrides)
}

export function htmlToBodyWithStyles(html) {
  if (!html) return { body: '', styleOverrides: { blocks: {}, inlines: [] } }

  const tmp = document.createElement('div')
  tmp.innerHTML = html

  const blocks = {}
  const inlines = []
  const parts = []

  // 行内递归:把 DOM 节点序列化为字符流 + 行内样式区间
  const extractInline = (node, blockIdx, charOffsetRef) => {
    if (node.nodeType === Node.TEXT_NODE) {
      return node.textContent
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return ''
    }
    if (node.tagName === 'BR') {
      return '\n'
    }

    const tag = node.tagName.toLowerCase()
    const inlineStyleMap = {
      b: { bold: true }, strong: { bold: true },
      i: { italic: true }, em: { italic: true },
      u: { underline: true },
      s: { strike: true }, del: { strike: true }, strike: { strike: true },
      code: { code: true }
    }
    let spanStyles = null
    if (tag === 'span' && node.getAttribute('style')) {
      const styleStr = node.getAttribute('style')
      spanStyles = {}
      const colorMatch = styleStr.match(/color\s*:\s*([^;]+)/i)
      if (colorMatch) spanStyles.color = colorMatch[1].trim()
      const bgMatch = styleStr.match(/background-color\s*:\s*([^;]+)/i)
      if (bgMatch) spanStyles.backgroundColor = bgMatch[1].trim()
      const sizeMatch = styleStr.match(/font-size\s*:\s*([^;]+)/i)
      if (sizeMatch) {
        const px = parseInt(sizeMatch[1], 10)
        spanStyles.fontSize = pxToFontSize(px)
      }
      const familyMatch = styleStr.match(/font-family\s*:\s*([^;]+)/i)
      if (familyMatch) spanStyles.fontFamily = familyMatch[1].trim()
      if (Object.keys(spanStyles).length === 0) spanStyles = null
    }

    const children = Array.from(node.childNodes)
    let buf = ''
    const childTexts = children.map(child => {
      const startOffset = charOffsetRef.value + buf.length
      const text = extractInline(child, blockIdx, { value: startOffset })
      buf += text
      return { startOffset, endOffset: startOffset + text.length, text }
    })
    const result = childTexts.map(c => c.text).join('')

    const baseStyles = inlineStyleMap[tag] || null
    const finalStyles = baseStyles || spanStyles
    if (finalStyles && result.length > 0) {
      const minStart = Math.min(...childTexts.map(c => c.startOffset))
      const maxEnd = Math.max(...childTexts.map(c => c.endOffset))
      inlines.push({
        block: blockIdx,
        start: minStart,
        end: maxEnd,
        styles: finalStyles
      })
    }
    return result
  }

  let blockIdx = 0
  Array.from(tmp.childNodes).forEach(node => {
    if (node.nodeType !== Node.ELEMENT_NODE) {
      if (node.nodeType === Node.TEXT_NODE && node.textContent.trim()) {
        const charOffsetRef = { value: 0 }
        const text = extractInline(node, blockIdx, charOffsetRef)
        parts.push(text.trim())
        blockIdx++
      }
      return
    }
    const tag = node.tagName.toLowerCase()
    const charOffsetRef = { value: 0 }
    const text = extractInline(node, blockIdx, charOffsetRef)
    const trimmed = text.trim()
    if (!trimmed) return

    const blockStyle = {}
    if (['h1', 'h2', 'h3', 'h4'].includes(tag)) {
      parts.push(`【${trimmed}】`)
    } else if (tag === 'blockquote') {
      parts.push(`> ${trimmed}`)
    } else if (tag === 'ul' || tag === 'ol') {
      Array.from(node.children).forEach(li => {
        if (li.tagName.toLowerCase() === 'li') {
          const liText = extractInline(li, blockIdx, { value: 0 }).trim()
          if (liText) parts.push(`- ${liText}`)
        }
      })
      collectBlockStyle(node, blockStyle)
      if (Object.keys(blockStyle).length > 0) blocks[blockIdx] = blockStyle
    } else {
      parts.push(trimmed)
    }
    if (tag !== 'ul' && tag !== 'ol') {
      collectBlockStyle(node, blockStyle)
      if (Object.keys(blockStyle).length > 0) blocks[blockIdx] = blockStyle
    }
    blockIdx++
  })

  return {
    body: parts.join('\n\n'),
    styleOverrides: { blocks, inlines }
  }
}

function collectBlockStyle(el, target) {
  const align = el.getAttribute && el.getAttribute('align')
  if (align) target.align = align
  const styleStr = el.getAttribute && el.getAttribute('style')
  if (!styleStr) return
  const textAlignMatch = styleStr.match(/text-align\s*:\s*([^;]+)/i)
  if (textAlignMatch) target.align = textAlignMatch[1].trim()
  const lineHeightMatch = styleStr.match(/line-height\s*:\s*([^;]+)/i)
  if (lineHeightMatch) target.lineHeight = lineHeightMatch[1].trim()
  const indentMatch = styleStr.match(/padding-left\s*:\s*([^;]+)/i)
  if (indentMatch) {
    const px = parseInt(indentMatch[1], 10)
    target.indent = Math.round(px / 24)
  }
  const sizeMatch = styleStr.match(/font-size\s*:\s*([^;]+)/i)
  if (sizeMatch) {
    const px = parseInt(sizeMatch[1], 10)
    target.fontSize = pxToFontSize(px)
  }
  const familyMatch = styleStr.match(/font-family\s*:\s*([^;]+)/i)
  if (familyMatch) target.fontFamily = familyMatch[1].trim()
}

function pxToFontSize(px) {
  if (px <= 12) return 'xs'
  if (px <= 14) return 'sm'
  if (px <= 15) return 'base'
  if (px <= 18) return 'lg'
  return 'xl'
}

export function applyStyleOverrides(html, styleOverrides) {
  if (!html || !styleOverrides) return html || ''
  const wrap = document.createElement('div')
  wrap.innerHTML = html

  const blockEls = Array.from(wrap.children)
  if (blockEls.length === 0) return html

  if (styleOverrides.blocks) {
    Object.entries(styleOverrides.blocks).forEach(([idx, style]) => {
      const el = blockEls[parseInt(idx, 10)]
      if (!el) return
      applyBlockStyle(el, style)
    })
  }

  if (Array.isArray(styleOverrides.inlines) && styleOverrides.inlines.length > 0) {
    const byBlock = {}
    styleOverrides.inlines.forEach(inline => {
      if (!byBlock[inline.block]) byBlock[inline.block] = []
      byBlock[inline.block].push(inline)
    })
    Object.entries(byBlock).forEach(([idx, list]) => {
      const el = blockEls[parseInt(idx, 10)]
      if (!el) return
      applyInlineStyle(el, list)
    })
  }

  return wrap.innerHTML
}

function applyBlockStyle(el, style) {
  if (style.align) {
    el.style.textAlign = style.align
    el.setAttribute('align', style.align)
  }
  if (style.lineHeight) {
    el.style.lineHeight = String(style.lineHeight)
  }
  if (typeof style.indent === 'number' && style.indent > 0) {
    el.style.paddingLeft = `${style.indent * 24}px`
  }
  if (style.fontSize) {
    el.style.fontSize = `${fontSizeToPx(style.fontSize)}px`
  }
  if (style.fontFamily) {
    el.style.fontFamily = style.fontFamily
  }
}

function applyInlineStyle(blockEl, inlineList) {
  const text = blockEl.textContent
  if (!text) return
  const ranges = inlineList
    .map(i => ({ start: Math.max(0, i.start), end: Math.min(text.length, i.end), styles: i.styles }))
    .filter(r => r.end > r.start)
    .sort((a, b) => a.start - b.start)
  if (ranges.length === 0) return
  const points = new Set([0, text.length])
  ranges.forEach(r => { points.add(r.start); points.add(r.end) })
  const sortedPoints = Array.from(points).sort((a, b) => a - b)
  const segments = []
  for (let i = 0; i < sortedPoints.length - 1; i++) {
    const s = sortedPoints[i], e = sortedPoints[i + 1]
    if (e <= s) continue
    const segmentStyles = ranges
      .filter(r => r.start <= s && r.end >= e)
      .reduce((acc, r) => Object.assign(acc, r.styles), {})
    segments.push({ text: text.slice(s, e), styles: segmentStyles })
  }
  blockEl.innerHTML = segments.map(seg => wrapInline(seg.text, seg.styles)).join('')
}

function wrapInline(text, styles) {
  if (Object.keys(styles).length === 0) return escapeHtml(text)
  const styleStr = buildStyleString(styles)
  let inner = escapeHtml(text)
  if (styles.code) inner = `<code>${inner}</code>`
  if (styles.bold) inner = `<strong>${inner}</strong>`
  if (styles.italic) inner = `<em>${inner}</em>`
  if (styles.underline) inner = `<u>${inner}</u>`
  if (styles.strike) inner = `<s>${inner}</s>`
  if (styleStr) inner = `<span style="${styleStr}">${inner}</span>`
  return inner
}

function buildStyleString(styles) {
  const parts = []
  if (styles.color) parts.push(`color: ${styles.color}`)
  if (styles.backgroundColor) parts.push(`background-color: ${styles.backgroundColor}`)
  return parts.join('; ')
}

function fontSizeToPx(size) {
  return { xs: 12, sm: 14, base: 15, lg: 18, xl: 22 }[size] || 15
}

function inlineMarkdownToHtml(text) {
  if (!text) return ''
  return text
    .replace(/\*\*\*(.+?)\*\*\*/g, '<b><i>$1</i></b>')
    .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
    .replace(/(^|[^*])\*([^*]+)\*([^*]|$)/g, '$1<i>$2</i>$3')
}

/**
 * 把正文纯文本语法转为富文本编辑器用的 HTML
 * 支持：【小标题】、> 引用、- 列表项、普通段落、*斜体*、**加粗**
 * @param {string} body
 * @returns {string}
 */
export function bodyToHtml(body) {
  if (!body) return '<p><br></p>'

  const parts = body.split(/\n\n+/)
  const htmlParts = []
  let listBuffer = []

  const flushList = () => {
    if (listBuffer.length === 0) return
    htmlParts.push('<ul>' + listBuffer.map(item => `<li>${inlineMarkdownToHtml(escapeHtml(item))}</li>`).join('') + '</ul>')
    listBuffer = []
  }

  parts.forEach((part) => {
    const trimmed = part.trim()
    if (!trimmed) return

    const headingMatch = trimmed.match(/^【([^】]+)】$/)
    if (headingMatch) {
      flushList()
      htmlParts.push(`<h2>${inlineMarkdownToHtml(escapeHtml(headingMatch[1]))}</h2>`)
      return
    }

    if (trimmed.startsWith('> ')) {
      flushList()
      htmlParts.push(`<blockquote>${inlineMarkdownToHtml(escapeHtml(trimmed.slice(2)))}</blockquote>`)
      return
    }

    const listMatch = trimmed.match(/^(?:[-•]|\d+\.)\s+(.*)$/)
    if (listMatch) {
      listBuffer.push(listMatch[1])
      return
    }

    flushList()
    const withInline = inlineMarkdownToHtml(escapeHtml(trimmed))
    const withBr = withInline.replace(/\n/g, '<br>')
    htmlParts.push(`<p>${withBr}</p>`)
  })

  flushList()

  if (htmlParts.length === 0) return '<p><br></p>'
  return htmlParts.join('')
}

/**
 * 把富文本编辑器里的 HTML 转回正文纯文本语法
 * @param {string} html
 * @returns {string}
 */
export function htmlToBody(html) {
  if (!html) return ''

  const tmp = document.createElement('div')
  tmp.innerHTML = html

  const parts = []

  const extractInline = (node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      return node.textContent
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return ''
    }
    if (node.tagName === 'BR') {
      return '\n'
    }
    const children = Array.from(node.childNodes).map(extractInline).join('')
    const tag = node.tagName.toLowerCase()
    if (tag === 'b' || tag === 'strong') {
      return `**${children}**`
    }
    if (tag === 'i' || tag === 'em') {
      return `*${children}*`
    }
    return children
  }

  const processBlock = (el) => {
    const tag = el.tagName.toLowerCase()
    const text = extractInline(el).trim()
    if (!text) return

    if (tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4') {
      parts.push(`【${text}】`)
      return
    }

    if (tag === 'blockquote') {
      parts.push(`> ${text}`)
      return
    }

    if (tag === 'ul' || tag === 'ol') {
      Array.from(el.children).forEach(li => {
        if (li.tagName.toLowerCase() === 'li') {
          const liText = extractInline(li).trim()
          if (liText) parts.push(`- ${liText}`)
        }
      })
      return
    }

    parts.push(text)
  }

  Array.from(tmp.childNodes).forEach(node => {
    if (node.nodeType === Node.ELEMENT_NODE) {
      processBlock(node)
    } else if (node.nodeType === Node.TEXT_NODE && node.textContent.trim()) {
      parts.push(node.textContent.trim())
    }
  })

  return parts.join('\n\n')
}

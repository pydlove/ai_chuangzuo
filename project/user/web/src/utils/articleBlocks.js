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
  // 详见后续步骤
  return bodyToHtml(body || '')
}

export function htmlToBodyWithStyles(html) {
  // 详见后续步骤
  const body = htmlToBody(html || '')
  return { body, styleOverrides: { blocks: {}, inlines: [] } }
}

export function applyStyleOverrides(html, styleOverrides) {
  // 详见后续步骤,本任务内先返回原 HTML
  return html || ''
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

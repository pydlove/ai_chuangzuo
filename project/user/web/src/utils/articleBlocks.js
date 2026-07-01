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

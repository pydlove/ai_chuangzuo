<template>
  <div class="learn-md" v-html="rendered"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github.css'

const props = defineProps({
  source: { type: String, default: '' }
})

const md = new MarkdownIt({
  // 允许行内 HTML 标签（如 <u>/<sub>/<sup>），对齐 md-editor-v3 工具栏（underline/sub/sup）的输出格式
  html: true,
  linkify: true,
  typographer: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (_) { /* noop */ }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const rendered = computed(() => md.render(props.source || ''))
</script>

<style scoped>
.learn-md { line-height: 1.75; color: #262626; font-size: 15px; }
.learn-md :deep(h1) { font-size: 24px; font-weight: 700; margin: 1.4em 0 0.6em; scroll-margin-top: 88px; }
.learn-md :deep(h2) { font-size: 20px; font-weight: 700; margin: 1.2em 0 0.5em; border-bottom: 1px solid #eee; padding-bottom: 6px; scroll-margin-top: 88px; }
.learn-md :deep(h3) { font-size: 17px; font-weight: 600; margin: 1em 0 0.4em; scroll-margin-top: 88px; }
.learn-md :deep(p) { margin: 0.8em 0; }
.learn-md :deep(blockquote) {
  margin: 1em 0; padding: 10px 16px; color: #555;
  background: #f8f8f8; border-left: 4px solid #FF2442;
}
.learn-md :deep(code) {
  background: #f6f8fa; padding: 2px 6px; border-radius: 4px;
  font-size: 13px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.learn-md :deep(pre) {
  background: #1f1f1f; color: #f5f5f5; padding: 14px 18px;
  border-radius: 6px; overflow-x: auto; font-size: 13px; line-height: 1.5;
}
.learn-md :deep(pre code) { background: transparent; padding: 0; color: inherit; }
.learn-md :deep(table) { border-collapse: collapse; width: 100%; margin: 1em 0; }
.learn-md :deep(th), .learn-md :deep(td) { border: 1px solid #e8e8e8; padding: 8px 12px; text-align: left; }
.learn-md :deep(th) { background: #fafafa; }
.learn-md :deep(ul), .learn-md :deep(ol) { padding-left: 1.5em; }
.learn-md :deep(img) { max-width: 100%; }
/* 行内 HTML 标签（来自 md-editor-v3 工具栏） */
.learn-md :deep(u) { text-decoration: underline; text-underline-offset: 2px; }
.learn-md :deep(sub) { font-size: 0.75em; vertical-align: sub; }
.learn-md :deep(sup) { font-size: 0.75em; vertical-align: super; }
.learn-md :deep(del) { color: #8c8c8c; }
.learn-md :deep(mark) { background: #FFF3E0; color: inherit; padding: 0 2px; border-radius: 2px; }
</style>

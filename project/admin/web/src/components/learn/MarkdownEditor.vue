<template>
  <MdEditor
    v-model="valueModel"
    :toolbars="toolbars"
    placeholder="开始用 Markdown 撰写…"
    language="zh-CN"
    style="height: 480px"
  />
</template>

<script setup>
import { computed } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const props = defineProps({ value: { type: String, default: '' } })
const emit = defineEmits(['update:value'])

const valueModel = computed({
  get() { return props.value },
  set(v) { emit('update:value', v) }
})

// md-editor-v3 的 toolbars 是数组（不是 mavon-editor 的对象）
// 完整可选项见 https://imzbf.github.io/md-editor-v3/zh-CN/docs#%F0%9F%A7%B0%20toolbars
const toolbars = [
  'bold', 'underline', 'italic', 'strikeThrough',
  'title', 'sub', 'sup',
  'quote', 'unorderedList', 'orderedList',
  'codeRow', 'code',
  'link', 'table',
  'revoke', 'next',
  'prettier',
  'pageFullscreen', 'fullscreen',
  'preview', 'htmlPreview', 'catalog'
]
</script>

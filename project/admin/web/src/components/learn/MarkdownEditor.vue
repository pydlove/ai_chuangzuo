<template>
  <mavon-editor
    v-model="valueModel"
    :toolbars="toolbars"
    placeholder="开始用 Markdown 撰写…"
    language="zh-CN"
    style="min-height: 420px"
  />
</template>

<script setup>
import { computed } from 'vue'
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'

const props = defineProps({ value: { type: String, default: '' } })
const emit = defineEmits(['update:value'])

const valueModel = computed({
  get() { return props.value },
  set(v) { emit('update:value', v) }
})

const toolbars = {
  bold: true, italic: true, header: true, underline: true, strikethrough: true,
  mark: true, superscript: true, subscript: true, quote: true, ol: true, ul: true,
  link: true, imagelink: false, code: true, table: true, fullscreen: true,
  readmodel: true, htmlcode: true, help: true, trash: true, undo: true, redo: true,
  navigation: true, alignleft: true, aligncenter: true, alignright: true,
  subfield: true, preview: true
}
</script>

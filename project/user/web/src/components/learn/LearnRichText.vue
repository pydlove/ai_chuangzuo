<template>
  <div class="learn-rt" v-html="safeHtml"></div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ html: { type: String, default: '' } })
const safeHtml = computed(() => props.html || '')
</script>

<style scoped>
.learn-rt { line-height: 1.75; color: #262626; font-size: 15px; }
.learn-rt :deep(*) { max-width: 100%; }
.learn-rt :deep(h1) { font-size: 24px; font-weight: 700; margin: 1.4em 0 0.6em; }
.learn-rt :deep(h2) { font-size: 20px; font-weight: 700; margin: 1.2em 0 0.5em; }
.learn-rt :deep(h3) { font-size: 17px; font-weight: 600; margin: 1em 0 0.4em; }
.learn-rt :deep(p) { margin: 0.8em 0; }
.learn-rt :deep(blockquote) {
  margin: 1em 0; padding: 10px 16px; color: #555;
  background: #f8f8f8; border-left: 4px solid #FF2442;
}
.learn-rt :deep(code) {
  background: #f6f8fa; padding: 2px 6px; border-radius: 4px; font-size: 13px;
}
.learn-rt :deep(pre) {
  background: #1f1f1f; color: #f5f5f5; padding: 14px 18px;
  border-radius: 6px; overflow-x: auto; font-size: 13px; line-height: 1.5;
}
.learn-rt :deep(table) { border-collapse: collapse; width: 100%; margin: 1em 0; }
.learn-rt :deep(th), .learn-rt :deep(td) { border: 1px solid #e8e8e8; padding: 8px 12px; text-align: left; }
.learn-rt :deep(th) { background: #fafafa; }
.learn-rt :deep(img) { max-width: 100%; }
</style>

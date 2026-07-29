<template>
  <div class="article-preview" :style="{ background: style.bg, fontFamily: style.font }">
    <h1
      class="article-title"
      :style="{
        color: style.titleColor,
        fontSize: style.titleSize,
        textAlign: style.titleAlign || 'left'
      }"
    >
      {{ article.title }}
    </h1>
    <div
      v-if="showMeta"
      class="article-meta"
      :style="{
        color: style.metaColor,
        borderBottomColor: style.metaBorder,
        textAlign: style.metaAlign || 'left'
      }"
    >
      <span>{{ article.description }}</span>
    </div>
    <div class="article-body" v-html="bodyHtml"></div>
    <div
      v-if="signatureText"
      class="article-signature"
      :style="{
        color: style.metaColor,
        borderTopColor: style.metaBorder,
        textAlign: 'center'
      }"
    >
      {{ signatureText }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { mergeStyle, renderBodyHtml, SAMPLE_ARTICLE } from '@/utils/articlePreview.js'

const props = defineProps({
  /** visual_style_json 字符串或对象。 */
  visualStyle: { type: [String, Object], default: null },
  /** 自定义文章，缺省用 SAMPLE_ARTICLE。 */
  article: { type: Object, default: () => SAMPLE_ARTICLE },
  /** 是否显示 meta 行。 */
  showMeta: { type: Boolean, default: true },
  /** 末尾签名文本（用于预览 signatureText 效果）。 */
  signatureText: { type: String, default: '' }
})

const style = computed(() => mergeStyle(props.visualStyle))
const bodyHtml = computed(() => renderBodyHtml(props.article.body, style.value))
</script>

<style scoped>
.article-preview {
  padding: 32px;
  border-radius: 8px;
  min-height: 400px;
}
.article-title {
  font-weight: 700;
  margin: 0 0 12px;
  line-height: 1.3;
}
.article-meta {
  font-size: 13px;
  padding-bottom: 12px;
  margin-bottom: 20px;
  border-bottom-style: solid;
  border-bottom-width: 1px;
}
.article-body :deep(p) {
  margin: 0 0 12px;
}
.article-body :deep(h2),
.article-body :deep(h3) {
  font-weight: 600;
  margin: 18px 0 8px;
}
.article-body :deep(ul) {
  margin: 8px 0;
  padding-left: 20px;
}
.article-signature {
  margin-top: 32px;
  padding-top: 16px;
  border-top-style: solid;
  border-top-width: 1px;
  font-size: 13px;
}
</style>
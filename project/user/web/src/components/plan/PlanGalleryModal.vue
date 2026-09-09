<template>
  <a-modal
    :open="open"
    title="运营方案库"
    :footer="null"
    :closable="true"
    width="720px"
    centered
    class="plan-gallery-modal"
    wrap-class-name="plan-gallery-modal-wrap"
    @cancel="$emit('update:open', false)"
  >
    <p class="plan-gallery-modal__tip">
      看看大家都在做什么方向。想发布某个方向的提示词？参考大家的运营方案，发布后被他人使用即可获得收益。
    </p>
    <PlanGalleryContent />
  </a-modal>
</template>

<script setup>
import PlanGalleryContent from '@/components/plan/PlanGalleryContent.vue'

defineProps({
  open: { type: Boolean, default: false }
})

defineEmits(['update:open'])
</script>

<style scoped>
.plan-gallery-modal__tip {
  font-size: 12px;
  color: #999;
  line-height: 1.6;
  background: #FFF8FA;
  border-radius: 8px;
  padding: 10px 12px;
  margin: 0 0 14px;
}

body[data-theme="dark"] .plan-gallery-modal__tip {
  background: #2a1a1d;
  color: #a6a6a6;
}
</style>

<style>
/* 非 scoped：弹框内容由 antdv 渲染，class 落在 .ant-modal 自身 */
.plan-gallery-modal-wrap .ant-modal-body {
  height: 70vh;
  overflow-y: auto;
  padding: 20px;
}
</style>

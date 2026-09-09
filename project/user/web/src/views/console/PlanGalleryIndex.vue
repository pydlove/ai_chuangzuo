<template>
  <div class="plan-gallery-page">
    <!-- PC 端页面标题 -->
    <header class="plan-gallery-header">
      <div>
        <h1 class="plan-gallery-title">运营方案库</h1>
        <p class="plan-gallery-subtitle">看看大家都在做什么方向，发布你的提示词即可获得收益</p>
      </div>
    </header>

    <div class="gallery-tip">
      看看大家都在做什么方向。想发布某个方向的提示词？参考大家的运营方案，发布后被他人使用即可获得收益。
    </div>

    <PlanGalleryContent />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import PlanGalleryContent from '@/components/plan/PlanGalleryContent.vue'
import { getCurrentPlanKey } from '@/utils/membershipLimits.js'

const router = useRouter()

onMounted(() => {
  // 运营方案库：专业版及以上可用
  const key = getCurrentPlanKey()
  if (key !== 'pro' && key !== 'flagship') {
    Modal.confirm({
      title: '运营方案库',
      content: '查阅全平台用户的运营方案为专业版及以上功能，升级后即可查看。别人使用你的提示词，你还能获得收益。',
      okText: '去升级',
      cancelText: '取消',
      centered: true,
      wrapClassName: 'membership-confirm-modal',
      onOk: () => router.push('/console/benefits'),
      onCancel: () => router.back()
    })
  }
})
</script>

<style scoped>
.plan-gallery-page {
  padding: 24px;
  max-width: 720px;
}

.plan-gallery-header {
  margin-bottom: 16px;
}

.plan-gallery-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.plan-gallery-subtitle {
  font-size: 13px;
  color: #999;
  margin: 6px 0 0;
}

.gallery-tip {
  font-size: 12px;
  color: #999;
  line-height: 1.6;
  background: #FFF8FA;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 14px;
}

body[data-theme="dark"] .plan-gallery-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .gallery-tip {
  background: #2a1a1d;
  color: #a6a6a6;
}

@media (max-width: 768px) {
  .plan-gallery-page {
    padding: 16px;
  }

  .plan-gallery-header {
    display: none;
  }
}
</style>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import FreeCreateModal from '@/views/console/create/FreeCreateModal.vue'
import { fetchCurrentPlan } from '@/api/selfMediaPlan.js'
import { getTodayDoneKey } from '@/constants/storage.js'

const router = useRouter()

const plan = reactive({
  platform: '小红书',
  niche: '35+ 职场转型',
  persona: '实战记录者',
  style: '真诚分享',
  postingFrequency: '每周 3 篇',
  contentFormat: '图文笔记',
  monetization: '知识付费',
  topics: ['职场转型', '副业探索', '个人成长']
})

const planLoaded = ref(false)

async function loadPlan() {
  try {
    const res = await fetchCurrentPlan()
    const data = res?.data || {}
    if (data && Object.keys(data).length) {
      Object.assign(plan, data)
      // 后端返回 platformKey/platformName、nicheKey/nicheName、personaKey/personaName，
      // 前端 plan 使用 platform/niche/persona 做展示与透传。
      if (data.platformName) plan.platform = data.platformName
      if (data.nicheName) plan.niche = data.nicheName
      if (data.personaName) plan.persona = data.personaName
    }
  } catch (e) {
    // 保持默认方案
  } finally {
    planLoaded.value = true
  }
}

onMounted(() => {
  loadPlan()
})

function setTodayDone() {
  localStorage.setItem(getTodayDoneKey(), '1')
}

function onSuccess(task) {
  setTodayDone()
}
</script>

<template>
  <FreeCreateModal v-if="planLoaded" page-mode :plan="plan" @success="onSuccess" />
  <div v-else class="create-page-loading">
    <a-spin />
  </div>
</template>

<style scoped>
.create-page-loading {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

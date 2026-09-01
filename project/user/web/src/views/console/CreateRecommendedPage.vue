<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import CreateFlowModal from '@/views/console/create/CreateFlowModal.vue'
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
  if (task?.id) {
    message.success('文章生成任务已创建')
  }
}
</script>

<template>
  <CreateFlowModal v-if="planLoaded" page-mode :plan="plan" @success="onSuccess" />
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

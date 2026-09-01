<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CompassOutlined, EditOutlined } from '@ant-design/icons-vue'
import { useDevice } from '@/composables/useDevice.js'
import CreateFlowModal from '@/views/console/create/CreateFlowModal.vue'
import FreeCreateModal from '@/views/console/create/FreeCreateModal.vue'
import { fetchCurrentPlan } from '@/api/selfMediaPlan.js'
import { getMyMembership } from '@/api/membership.js'
import { getTodayDoneKey } from '@/constants/storage.js'

const router = useRouter()
const { isMobile } = useDevice()

const createChoiceVisible = ref(false)
const createFlowVisible = ref(false)
const freeCreateVisible = ref(false)
const hasMembership = ref(false)
const membershipLoaded = ref(false)

const plan = reactive({
  platformKey: '',
  platform: '小红书',
  nicheKey: '',
  niche: '35+ 职场转型',
  personaKey: '',
  persona: '实战记录者',
  style: '真诚分享',
  postingFrequency: '每周 3 篇',
  contentFormat: '图文笔记',
  monetization: '知识付费',
  topics: ['职场转型', '副业探索', '个人成长']
})

async function loadMembership() {
  try {
    const res = await getMyMembership()
    const membership = res?.data || {}
    hasMembership.value = membership.hasMembership || false
  } catch (e) {
    hasMembership.value = false
  }
  membershipLoaded.value = true
}

async function loadPlan() {
  try {
    const res = await fetchCurrentPlan()
    const data = res?.data || {}
    if (data && Object.keys(data).length) {
      Object.assign(plan, data)
      // 后端返回 platformKey/platformName，前端 plan 使用 platform/niche/persona
      if (data.platformKey) plan.platformKey = data.platformKey
      if (data.platformName) plan.platform = data.platformName
      if (data.nicheKey) plan.nicheKey = data.nicheKey
      if (data.nicheName) plan.niche = data.nicheName
      if (data.personaKey) plan.personaKey = data.personaKey
      if (data.personaName) plan.persona = data.personaName
    }
  } catch (e) {
    // 保持默认方案
  }
}

async function ensureData() {
  if (!membershipLoaded.value) {
    await loadMembership()
  }
  await loadPlan()
}

function openCreateChoice() {
  ensureData().then(() => {
    if (!hasMembership.value) {
      message.warning('开始创作需要开通会员，请先订阅套餐')
      router.push('/pricing')
      return
    }
    createChoiceVisible.value = true
  })
}

function chooseRecommended() {
  createChoiceVisible.value = false
  if (isMobile.value) {
    router.push('/console/create/recommended')
    return
  }
  createFlowVisible.value = true
}

function chooseFreeCreate() {
  createChoiceVisible.value = false
  if (isMobile.value) {
    router.push('/console/create/free')
    return
  }
  freeCreateVisible.value = true
}

function setTodayDone() {
  localStorage.setItem(getTodayDoneKey(), '1')
}

const emit = defineEmits(['taskCreated'])

function onCreateStart(task) {
  setTodayDone()
  if (task?.id) {
    message.success('文章生成任务已创建')
  }
  emit('taskCreated', task)
}

function onFreeCreateSuccess(task) {
  setTodayDone()
  if (task?.id) {
    message.success('文章生成任务已创建')
  }
  emit('taskCreated', task)
}

defineExpose({ openCreateChoice })
</script>

<template>
  <CreateFlowModal v-model:visible="createFlowVisible" :plan="plan" @success="onCreateStart" />
  <FreeCreateModal v-model:visible="freeCreateVisible" :plan="plan" @success="onFreeCreateSuccess" />

  <!-- 创作方式选择弹窗 -->
  <a-modal
    v-model:open="createChoiceVisible"
    title="开始今日创作"
    width="640px"
    :footer="null"
    centered
    class="create-choice-modal"
    @cancel="createChoiceVisible = false"
  >
    <div class="create-choice-body">
      <div class="create-choice-options">
        <div class="create-choice-card recommended" @click="chooseRecommended">
          <div class="choice-icon-wrap">
            <CompassOutlined class="choice-icon" />
          </div>
          <div class="choice-title">按小爱推荐的方式创作</div>
          <div class="choice-desc">小爱针对你的运营方案，推荐选题、观点、字数等进行创作。</div>
          <div class="choice-tags">
            <span class="choice-tag">小爱推荐选题</span>
            <span class="choice-tag">低粉高赞案例</span>
          </div>
        </div>

        <div class="create-choice-card free" @click="chooseFreeCreate">
          <div class="choice-icon-wrap">
            <EditOutlined class="choice-icon" />
          </div>
          <div class="choice-title">自由创作</div>
          <div class="choice-desc">自己设置标题和核心观点，自主选择平台与字数，适合已有明确想法的人。</div>
          <div class="choice-tags">
            <span class="choice-tag">自定义标题</span>
            <span class="choice-tag">自主观点</span>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
/* 创作方式选择弹窗 */
.create-choice-modal :deep(.ant-modal-body) {
  padding: var(--space-lg);
}
.create-choice-body {
  padding: 8px 4px;
}
.create-choice-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
}
.create-choice-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: var(--space-lg);
  background: var(--color-bg-card);
  border: 1.5px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.25s ease;
}
.create-choice-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 8px 24px rgba(7, 193, 96, 0.12);
  transform: translateY(-2px);
}
.choice-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-full);
  background: var(--color-primary-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-md);
}
.choice-icon {
  font-size: 26px;
  color: var(--color-primary);
}
.choice-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}
.choice-desc {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: var(--space-md);
  flex: 1;
}
.choice-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.choice-tag {
  font-size: var(--font-caption);
  color: var(--color-primary);
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-light);
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

@media (max-width: 768px) {
  .create-choice-modal :deep(.ant-modal-body) {
    padding: 16px;
  }
  .create-choice-options {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .create-choice-card {
    padding: 16px;
  }
}
</style>
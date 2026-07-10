<template>
  <div class="generation-queue">
    <a-card :bordered="false" class="page-card">
      <div class="page-header">
        <h3 class="page-title">AI 创作</h3>
        <p class="page-desc">输入标题和描述，AI 帮你写一篇文章。生成过程异步进行，无需等待。</p>
      </div>

      <a-form layout="vertical" style="max-width: 720px" :model="form">
        <a-form-item label="标题" required>
          <a-input
            v-model:value="form.title"
            placeholder="例如：职场新人快速提升效率的 5 个方法"
            :maxlength="128"
            show-count
            allow-clear
          />
        </a-form-item>

        <a-form-item label="描述 / 核心观点">
          <a-textarea
            v-model:value="form.description"
            :rows="4"
            :maxlength="2000"
            placeholder="越具体 AI 越能写出想要的风格。例如：重点写时间管理技巧，加入具体案例，语气轻松、有干货。"
          />
        </a-form-item>

        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item label="平台">
              <a-select v-model:value="form.platform">
                <a-select-option value="wechat">公众号</a-select-option>
                <a-select-option value="xiaohongshu">小红书</a-select-option>
                <a-select-option value="toutiao">今日头条</a-select-option>
                <a-select-option value="baijiahao">百家号</a-select-option>
                <a-select-option value="zhihu">知乎</a-select-option>
                <a-select-option value="douyin">抖音</a-select-option>
                <a-select-option value="general">通用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="字数">
              <a-input-number
                v-model:value="form.wordCount"
                :min="100"
                :max="3000"
                :step="100"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="风格">
              <a-input v-model:value="form.styleRef" placeholder="选填" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="创作模板">
          <a-select
            v-model:value="form.templateId"
            placeholder="默认使用内置去 AI 味模板"
            allow-clear
          >
            <a-select-option
              v-for="t in availableTemplates"
              :key="t.id"
              :value="t.id"
            >
              {{ t.name }}
              <span v-if="t.isBuiltin" style="color: #07c160; margin-left: 4px;">(内置)</span>
              <span v-if="t.latestPublishedVersion" style="color: #8c8c8c; margin-left: 4px;">v{{ t.latestPublishedVersion }}</span>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" size="large" :loading="submitting" @click="onSubmit">
              <template #icon><RocketOutlined /></template>
              开始生成
            </a-button>
            <a-button @click="resetForm">清空</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 当前生成中 / 已生成 -->
    <a-card v-if="currentTask" :bordered="false" class="page-card current-card">
      <a-spin :spinning="submitting || isPolling" tip="生成中…">
        <div class="current-content">
          <div class="status-row">
            <a-tag
              :color="statusColor"
              class="status-tag"
            >{{ statusLabel }}</a-tag>
            <span class="task-title">{{ currentTask.title || currentTask.inputParam?.title || '（任务入队中…）' }}</span>
            <span class="muted">#{{ currentTask.id }}</span>
          </div>

          <a-descriptions :column="2" size="small" bordered class="meta-table">
            <a-descriptions-item label="平台">{{ currentTask.inputParam?.platform || '-' }}</a-descriptions-item>
            <a-descriptions-item label="字数">{{ currentTask.wordLimitTarget }}</a-descriptions-item>
            <a-descriptions-item label="提交时间">{{ formatTime(currentTask.createdAt) }}</a-descriptions-item>
            <a-descriptions-item label="完成时间">
              {{ currentTask.completedAt ? formatTime(currentTask.completedAt) : '-' }}
            </a-descriptions-item>
            <a-descriptions-item v-if="currentTask.failedReason" label="失败原因" :span="2">
              <span class="err-text">{{ currentTask.failedReason }}</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="action-row">
            <a-button v-if="isSucceeded" type="primary" @click="goArticle">
              查看作品
            </a-button>
            <a-button v-if="isFailed" type="primary" :loading="submitting" @click="onRetry">
              重新生成
            </a-button>
            <a-button @click="dismiss">收起</a-button>
          </div>
        </div>
      </a-spin>
    </a-card>

    <!-- 历史列表 -->
    <a-card :bordered="false" class="page-card">
      <div class="list-header">
        <h4 class="list-title">历史任务</h4>
        <a-button size="small" @click="loadList">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
      <a-table
        :columns="listColumns"
        :data-source="list"
        :loading="listLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColorOf(record.status)">{{ statusLabelOf(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'title'">
            <span>{{ record.title || record.inputParam?.title || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a v-if="record.status === 2" @click="openArticle(record)">查看</a>
            <a
              v-if="record.status === 3"
              class="retry-link"
              @click="onRetry(record.id)"
            >重新生成</a>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { RocketOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { listGenerationTasks, listPromptTemplates } from '@/api/generation'
import { useGenerationTask } from '@/composables/useGenerationTask'

const route = useRoute()
const router = useRouter()

const form = reactive({
  title: '',
  description: '',
  platform: 'wechat',
  wordCount: 800,
  styleRef: '',
  templateId: undefined
})

const submitting = ref(false)
const polling = ref(false)
const currentTask = ref(null)
const dismissed = ref(false)

const { task, loading, polling: isPolling, submit, retry, setOnDone, stop } = useGenerationTask()

watch(task, (t) => {
  if (t && !dismissed.value) {
    currentTask.value = t
    polling.value = isPolling.value
  }
})

watch(isPolling, (v) => { polling.value = v })

setOnDone(() => {
  // 任务结束（成功或失败）→ 重新拉一次历史列表
  loadList()
})

const list = ref([])
const listLoading = ref(false)
const availableTemplates = ref([])
const listColumns = [
  { title: '#', dataIndex: 'id', key: 'id', width: 60 },
  { title: '标题', key: 'title', width: 240 },
  { title: '状态', key: 'status', width: 100 },
  { title: '字数', dataIndex: 'wordLimitTarget', key: 'wordLimitTarget', width: 80 },
  { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'actions', width: 120 }
]

const loadList = async () => {
  listLoading.value = true
  try {
    const data = await listGenerationTasks({ page: 1, pageSize: 20 })
    list.value = data.list
  } catch (e) {
    message.error(e.message || '加载历史失败')
  } finally {
    listLoading.value = false
  }
}

const onSubmit = async () => {
  if (!form.title.trim()) {
    message.warning('请填写标题')
    return
  }
  if (form.wordCount < 100 || form.wordCount > 3000) {
    message.warning('字数需在 100-3000 之间')
    return
  }
  submitting.value = true
  dismissed.value = false
  try {
    await submit({
      title: form.title.trim(),
      description: form.description,
      platform: form.platform,
      styleRef: form.styleRef || null,
      wordCount: form.wordCount,
      templateId: form.templateId || null
    })
    message.success('已加入生成队列')
  } catch (e) {
    message.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const onRetry = async (id) => {
  const targetId = id || (currentTask.value && currentTask.value.id)
  if (!targetId) return
  submitting.value = true
  dismissed.value = false
  try {
    await retry(targetId)
    message.success('已重新加入队列')
  } catch (e) {
    message.error(e.message || '重新生成失败')
  } finally {
    submitting.value = false
  }
}

const dismiss = () => {
  dismissed.value = true
  currentTask.value = null
  stop()
}

const goArticle = () => {
  // 暂时没法直接从 task 拿到 biz_no，跳到作品列表
  router.push('/console/works')
}
const openArticle = (record) => {
  router.push({ path: '/console/works', query: { bizNo: record.bizNo } })
}

const isSucceeded = computed(() => currentTask.value && currentTask.value.status === 2)
const isFailed = computed(() => currentTask.value && currentTask.value.status === 3)
const statusLabel = computed(() => currentTask.value ? statusLabelOf(currentTask.value.status) : '')
const statusColor = computed(() => currentTask.value ? statusColorOf(currentTask.value.status) : 'blue')

function statusLabelOf(s) {
  return s === 0 ? '排队中' : s === 1 ? '生成中' : s === 2 ? '已完成' : s === 3 ? '失败' : '未知'
}
function statusColorOf(s) {
  return s === 0 ? 'blue' : s === 1 ? 'processing' : s === 2 ? 'green' : s === 3 ? 'red' : 'default'
}

function formatTime(s) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 19)
}

const resetForm = () => {
  form.title = ''
  form.description = ''
  form.styleRef = ''
  form.templateId = undefined
}

const loadAvailableTemplates = async () => {
  try {
    availableTemplates.value = await listPromptTemplates()
  } catch (e) {
    message.error(e.message || '加载创作模板失败')
  }
}

onMounted(() => {
  // 支持从 ?title=&description=&platform=&wordCount=&styleRef= 预填（从 CreateIndex 跳转过来）
  if (route.query.title) form.title = String(route.query.title)
  if (route.query.description) form.description = String(route.query.description)
  if (route.query.platform) form.platform = String(route.query.platform)
  if (route.query.wordCount) {
    const wc = Number(route.query.wordCount)
    if (!isNaN(wc) && wc >= 100 && wc <= 3000) form.wordCount = wc
  }
  if (route.query.styleRef) form.styleRef = String(route.query.styleRef)
  loadList()
  loadAvailableTemplates()
})
</script>

<style scoped>
.generation-queue {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-card {
  border-radius: 8px;
}
.page-header {
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}
.page-desc {
  color: #8c8c8c;
  font-size: 13px;
  margin: 0;
}
.current-card {
  background: #fafafa;
}
.current-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-tag {
  font-weight: 500;
}
.task-title {
  font-weight: 500;
  font-size: 14px;
  color: #262626;
}
.muted {
  color: #8c8c8c;
  font-size: 12px;
}
.meta-table {
  margin-top: 4px;
}
.action-row {
  display: flex;
  gap: 8px;
}
.err-text {
  color: #ff4d4f;
  font-size: 12px;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.list-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0;
}
.retry-link {
  color: #ff2442;
  margin-left: 8px;
}
</style>

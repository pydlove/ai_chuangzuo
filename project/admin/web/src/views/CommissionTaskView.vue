<template>
  <div class="commission-admin">
    <div class="toolbar">
      <a-input-search v-model:value="keyword" placeholder="任务标题或编号" style="width: 280px" @search="loadTasks" />
      <a-select v-model:value="status" allow-clear placeholder="全部状态" style="width: 160px" @change="loadTasks">
        <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="publishVisible = true">发布约稿任务</a-button>
    </div>

    <a-table :columns="columns" :data-source="records" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'reward'">{{ record.rewardCoin }} 创作币/篇</template>
        <template v-else-if="column.key === 'progress'">{{ record.adoptedCount }}/{{ record.neededCount }} 篇</template>
        <template v-else-if="column.key === 'deadline'">{{ formatTime(record.deadlineAt) }}</template>
        <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ taskStatus(record.status) }}</a-tag></template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openDetail(record.id)">详情/采纳</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" @click="closeTask(record)">截止</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="publishVisible" title="发布约稿任务" :confirm-loading="saving" @ok="publishTask">
      <a-form layout="vertical">
        <a-form-item label="任务标题" required><a-input v-model:value="form.title" :maxlength="128" /></a-form-item>
        <a-form-item label="需求描述" required><a-textarea v-model:value="form.description" :rows="5" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="最小字数" required><a-input-number v-model:value="form.minWordCount" :min="1" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="最大字数" required><a-input-number v-model:value="form.maxWordCount" :min="1" style="width:100%" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="风格提示"><a-input v-model:value="form.styleHint" :maxlength="128" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="每篇奖励" required><a-input-number v-model:value="form.rewardCoin" :min="5" style="width:100%" addon-after="创作币" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="需采纳数量" required><a-input-number v-model:value="form.neededCount" :min="1" style="width:100%" addon-after="篇" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="投稿截止时间" required><a-date-picker v-model:value="form.deadlineAt" show-time style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailVisible" title="任务详情与稿件采纳" width="760">
      <template v-if="detail?.task">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="任务">{{ detail.task.title }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ taskStatus(detail.task.status) }}</a-descriptions-item>
          <a-descriptions-item label="奖励">{{ detail.task.rewardCoin }} 创作币/篇</a-descriptions-item>
          <a-descriptions-item label="采纳进度">{{ detail.task.adoptedCount }}/{{ detail.task.neededCount }}</a-descriptions-item>
          <a-descriptions-item label="字数要求">{{ detail.task.minWordCount }}-{{ detail.task.maxWordCount }}</a-descriptions-item>
          <a-descriptions-item label="截止时间">{{ formatTime(detail.task.deadlineAt) }}</a-descriptions-item>
          <a-descriptions-item label="需求" :span="2">{{ detail.task.description }}</a-descriptions-item>
        </a-descriptions>

        <div class="submission-heading">
          <h3>投稿稿件</h3>
          <a-button type="primary" :disabled="!canAdopt" :loading="adopting" @click="adoptSelected">采纳所选并发奖</a-button>
        </div>
        <a-alert v-if="detail.task.status === 1" type="info" show-icon :message="`还可采纳 ${remainingCount} 篇，最多选择对应数量`" />
        <a-table :columns="submissionColumns" :data-source="detail.submissions || []" row-key="id" :pagination="false" :row-selection="rowSelection">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'article'">
              <a-button type="link" @click="previewSubmission = record">《{{ record.articleTitle }}》</a-button>
            </template>
            <template v-else-if="column.key === 'status'"><a-tag>{{ submissionStatus(record.status) }}</a-tag></template>
          </template>
        </a-table>
      </template>
    </a-drawer>

    <a-modal :open="!!previewSubmission" title="稿件内容" :footer="null" width="760" @cancel="previewSubmission = null">
      <template v-if="previewSubmission">
        <h2>{{ previewSubmission.articleTitle }}</h2>
        <p class="article-meta">{{ previewSubmission.wordCount }} 字</p>
        <div class="article-body">{{ previewSubmission.articleBody }}</div>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  adoptCommissionSubmissions, closeCommissionTask,
  createCommissionTask, fetchCommissionTask, fetchCommissionTasks
} from '@/api/commission.js'

const loading = ref(false)
const saving = ref(false)
const adopting = ref(false)
const records = ref([])
const keyword = ref('')
const status = ref(undefined)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const publishVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const selectedRowKeys = ref([])
const previewSubmission = ref(null)
const statusOptions = [0, 1, 2].map((value) => ({ value, label: taskStatus(value) }))
const columns = [
  { title: '任务编号', dataIndex: 'taskNo', width: 190 }, { title: '标题', dataIndex: 'title' },
  { title: '奖励', key: 'reward', width: 130 }, { title: '采纳进度', key: 'progress', width: 100 },
  { title: '截止时间', key: 'deadline', width: 180 }, { title: '状态', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 210 }
]
const submissionColumns = [
  { title: '投稿用户', dataIndex: 'submitterId', width: 100 }, { title: '稿件', key: 'article' },
  { title: '字数', dataIndex: 'wordCount', width: 90 }, { title: '状态', key: 'status', width: 110 }
]
const pagination = computed(() => ({ current: page.value, pageSize: pageSize.value, total: total.value, showSizeChanger: true }))
const remainingCount = computed(() => detail.value ? detail.value.task.neededCount - detail.value.task.adoptedCount : 0)
const canAdopt = computed(() => detail.value?.task.status === 1 && selectedRowKeys.value.length > 0 && selectedRowKeys.value.length <= remainingCount.value)
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys) => { selectedRowKeys.value = keys },
  getCheckboxProps: (record) => ({ disabled: record.status !== 0 })
}))
const form = reactive({ title: '', description: '', minWordCount: 600, maxWordCount: 1200, styleHint: '', rewardCoin: 30, neededCount: 1, deadlineAt: null })

async function loadTasks() {
  loading.value = true
  try {
    const data = await fetchCommissionTasks({ keyword: keyword.value || undefined, status: status.value, page: page.value, pageSize: pageSize.value })
    records.value = data.records || []
    total.value = data.total || 0
  } catch (error) { message.error(error.message || '任务加载失败') }
  finally { loading.value = false }
}
function onTableChange(p) { page.value = p.current; pageSize.value = p.pageSize; loadTasks() }
async function publishTask() {
  if (!form.title.trim() || !form.description.trim() || !form.deadlineAt || form.minWordCount > form.maxWordCount) return message.warning('请完整填写正确的任务信息')
  saving.value = true
  try {
    await createCommissionTask({ ...form, deadlineAt: form.deadlineAt.format('YYYY-MM-DDTHH:mm:ss') })
    publishVisible.value = false
    Object.assign(form, { title: '', description: '', minWordCount: 600, maxWordCount: 1200, styleHint: '', rewardCoin: 30, neededCount: 1, deadlineAt: null })
    message.success('约稿任务已发布')
    loadTasks()
  } catch (error) { message.error(error.message || '发布失败') }
  finally { saving.value = false }
}
async function openDetail(id) {
  try { detail.value = await fetchCommissionTask(id); selectedRowKeys.value = []; detailVisible.value = true }
  catch (error) { message.error(error.message || '详情加载失败') }
}
function closeTask(record) {
  Modal.confirm({ title: '确认截止该任务？', content: '仅当投稿截止时间已到时可截止。', onOk: async () => { try { await closeCommissionTask(record.id); message.success('任务已截止'); loadTasks() } catch (e) { message.error(e.message || '操作失败') } } })
}
async function adoptSelected() {
  adopting.value = true
  try {
    await adoptCommissionSubmissions(detail.value.task.id, selectedRowKeys.value)
    message.success('采纳成功，奖励已发放')
    await openDetail(detail.value.task.id)
    loadTasks()
  } catch (error) { message.error(error.message || '采纳发奖失败') }
  finally { adopting.value = false }
}
function taskStatus(value) { return ['招募中', '已截止待采纳', '已完成'][value] || '未知' }
function submissionStatus(value) { return ['待采纳', '已采纳', '未采纳', '已撤回'][value] || '未知' }
function statusColor(value) { return ['blue', 'orange', 'green'][value] || 'default' }
function formatTime(value) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }
loadTasks()
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.submission-heading { display: flex; align-items: center; justify-content: space-between; margin: 24px 0 12px; }
.submission-heading h3 { margin: 0; }
.article-meta { color: #999; }
.article-body { max-height: 60vh; overflow: auto; white-space: pre-wrap; line-height: 1.8; }
</style>

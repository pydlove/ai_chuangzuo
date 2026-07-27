<template>
  <div class="commission-admin">
    <div class="toolbar">
      <a-input-search v-model:value="keyword" placeholder="任务标题或编号" style="width: 280px" @search="loadTasks" />
      <a-select v-model:value="status" allow-clear placeholder="全部状态" style="width: 160px" @change="loadTasks">
        <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="openPublish">发布约稿任务</a-button>
    </div>

    <a-table :columns="columns" :data-source="records" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'reward'">{{ record.rewardCoin }} 创作币/篇</template>
        <template v-else-if="column.key === 'progress'">{{ record.adoptedCount }}/{{ record.neededCount }} 篇</template>
        <template v-else-if="column.key === 'deadline'">{{ formatTime(record.deadlineAt) }}</template>
        <template v-else-if="column.key === 'selectionDeadline'">{{ formatTime(record.selectionDeadlineAt) }}</template>
        <template v-else-if="column.key === 'status'"><a-tag :color="statusColor(record.status)">{{ taskStatus(record.status) }}</a-tag></template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openDetail(record.id)">详情/采纳</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" @click="endSubmission(record)">结束投递</a-button>
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
        <a-form-item label="投递截止时间" required><a-date-picker v-model:value="form.deadlineAt" show-time style="width:100%" /></a-form-item>
        <a-form-item label="评选截止时间" required><a-date-picker v-model:value="form.selectionDeadlineAt" show-time style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="editVisible" title="编辑约稿任务" :confirm-loading="updating" @ok="submitEdit">
      <a-alert v-if="editingDeadlinePassed" type="warning" show-icon message="投递截止已过，保存后下次访问会自动进入评选期" style="margin-bottom: 12px" />
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
        <a-form-item label="投递截止时间" required><a-date-picker v-model:value="form.deadlineAt" show-time style="width:100%" /></a-form-item>
        <a-form-item label="评选截止时间" required><a-date-picker v-model:value="form.selectionDeadlineAt" show-time style="width:100%" /></a-form-item>
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
          <a-descriptions-item label="投递截止">{{ formatTime(detail.task.deadlineAt) }}</a-descriptions-item>
          <a-descriptions-item label="评选截止">{{ formatTime(detail.task.selectionDeadlineAt) }}</a-descriptions-item>
          <a-descriptions-item label="需求" :span="2">{{ detail.task.description }}</a-descriptions-item>
        </a-descriptions>

        <div class="submission-heading">
          <h3>投稿稿件</h3>
          <a-button type="primary" :disabled="!canAdopt" :loading="adopting" @click="adoptSelected">采纳所选并发奖</a-button>
        </div>
        <a-alert v-if="detail.task.status === 1" type="info" show-icon :message="`还可采纳 ${remainingCount} 篇，最多选择对应数量`" />
        <a-alert v-else-if="detail.task.status === 2" type="success" show-icon message="任务已完成，不可再采纳" />
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
import dayjs from 'dayjs'
import {
  adoptCommissionSubmissions, closeCommissionTask,
  createCommissionTask, fetchCommissionTask, fetchCommissionTasks,
  updateCommissionTask
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
const editVisible = ref(false)
const editingTaskId = ref(null)
const updating = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const selectedRowKeys = ref([])
const previewSubmission = ref(null)
const statusOptions = [0, 1, 2].map((value) => ({ value, label: taskStatus(value) }))
const columns = [
  { title: '任务编号', dataIndex: 'taskNo', width: 180 }, { title: '标题', dataIndex: 'title' },
  { title: '奖励', key: 'reward', width: 130 }, { title: '采纳进度', key: 'progress', width: 100 },
  { title: '投递截止', key: 'deadline', width: 160 },
  { title: '评选截止', key: 'selectionDeadline', width: 160 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 240 }
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
const emptyForm = () => ({ title: '', description: '', minWordCount: 600, maxWordCount: 1200, styleHint: '', rewardCoin: 30, neededCount: 1, deadlineAt: null, selectionDeadlineAt: null })
const form = reactive(emptyForm())

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
function validateForm() {
  if (!form.title.trim() || !form.description.trim() || !form.deadlineAt || !form.selectionDeadlineAt) return '请完整填写任务信息'
  if (form.minWordCount > form.maxWordCount) return '最小字数不能大于最大字数'
  if (!form.selectionDeadlineAt.isAfter(form.deadlineAt)) return '评选截止必须晚于投递截止'
  return null
}
function formatDate(value) {
  return value ? value.format('YYYY-MM-DDTHH:mm:ss') : null
}
function openPublish() {
  Object.assign(form, emptyForm())
  publishVisible.value = true
}
async function publishTask() {
  const error = validateForm()
  if (error) return message.warning(error)
  saving.value = true
  try {
    await createCommissionTask({ ...form, deadlineAt: formatDate(form.deadlineAt), selectionDeadlineAt: formatDate(form.selectionDeadlineAt) })
    publishVisible.value = false
    message.success('约稿任务已发布')
    loadTasks()
  } catch (err) { message.error(err.message || '发布失败') }
  finally { saving.value = false }
}
function openEdit(record) {
  editingTaskId.value = record.id
  Object.assign(form, {
    title: record.title || '',
    description: record.description || '',
    minWordCount: record.minWordCount || 600,
    maxWordCount: record.maxWordCount || 1200,
    styleHint: record.styleHint || '',
    rewardCoin: record.rewardCoin,
    neededCount: record.neededCount,
    deadlineAt: dayjs(record.deadlineAt),
    selectionDeadlineAt: dayjs(record.selectionDeadlineAt)
  })
  editVisible.value = true
}
async function submitEdit() {
  const error = validateForm()
  if (error) return message.warning(error)
  updating.value = true
  try {
    await updateCommissionTask(editingTaskId.value, { ...form, deadlineAt: formatDate(form.deadlineAt), selectionDeadlineAt: formatDate(form.selectionDeadlineAt) })
    editVisible.value = false
    message.success('约稿任务已更新')
    loadTasks()
  } catch (err) { message.error(err.message || '更新失败') }
  finally { updating.value = false }
}
const editingDeadlinePassed = computed(() => form.deadlineAt && form.deadlineAt.isBefore(dayjs()))
async function openDetail(id) {
  try { detail.value = await fetchCommissionTask(id); selectedRowKeys.value = []; detailVisible.value = true }
  catch (error) { message.error(error.message || '详情加载失败') }
}
function endSubmission(record) {
  Modal.confirm({ title: '确认结束投递？', content: '结束后将进入评选期，不可再撤回或重新投递。', okType: 'danger', onOk: async () => { try { await closeCommissionTask(record.id); message.success('已进入评选期'); loadTasks() } catch (e) { message.error(e.message || '操作失败') } } })
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
function taskStatus(value) { return ['投递中', '评选中', '已完成'][value] || '未知' }
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
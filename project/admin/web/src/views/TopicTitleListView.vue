<template>
  <div class="topic-title-admin">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">标题管理</h3>
        <p class="page-desc">AI 批量生成选题标题入库，用户端「没灵感」胶囊从这里随机拉取；用过的标题按用户隔离</p>
      </div>

      <div class="toolbar">
        <div class="toolbar-left">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索标题关键字"
            style="width: 280px"
            allow-clear
            @search="onSearch"
          />
          <a-select
            v-model:value="usedStatus"
            placeholder="是否使用"
            style="width: 140px"
            allow-clear
            :options="usedStatusOptions"
            @change="onSearch"
          />
        </div>
        <a-button type="primary" @click="openGenerateModal">
          <template #icon><ThunderboltOutlined /></template>
          AI 生成标题
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <span class="cell-ellipsis" :class="{ 'used-strike': record.useCount > 0 }" :title="record.title">{{ record.title }}</span>
          </template>
          <template v-else-if="column.key === 'summary'">
            <span class="cell-ellipsis" :title="record.summary">{{ record.summary }}</span>
          </template>
          <template v-else-if="column.key === 'direction'">
            <span class="cell-ellipsis" :title="record.direction">{{ record.direction || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'used'">
            <a-tag :color="record.useCount > 0 ? 'default' : 'green'">
              {{ record.useCount > 0 ? '已使用' : '未使用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm
              title="确定删除该标题？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="onDelete(record)"
            >
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="generateModalOpen"
      title="AI 生成标题"
      :confirm-loading="generating"
      :mask-closable="false"
      ok-text="生成"
      cancel-text="取消"
      @ok="onGenerate"
    >
      <div class="generate-form">
        <div class="form-row">
          <span class="form-label">生成数量</span>
          <a-input-number v-model:value="generateCount" :min="1" :max="100" style="width: 120px" />
          <span class="form-hint">1-100 条</span>
        </div>
        <div class="form-row form-row-top">
          <span class="form-label">方向提示词</span>
          <a-textarea
            v-model:value="generateDirection"
            :rows="4"
            show-count
            placeholder="职场效率类，面向 25-35 岁打工人"
          />
        </div>
        <p class="generate-tip">生成过程同步调用 AI，可能需要等待数十秒，请勿重复点击。</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import { listTopicTitles, generateTopicTitles, deleteTopicTitle } from '@/api/topicTitle.js'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const usedStatus = ref(undefined)
const usedStatusOptions = [
  { value: 0, label: '未使用' },
  { value: 1, label: '已使用' }
]

const generateModalOpen = ref(false)
const generating = ref(false)
const generateCount = ref(10)
const generateDirection = ref('')

const columns = [
  { title: '标题', key: 'title', ellipsis: true },
  { title: '概要', key: 'summary', ellipsis: true },
  { title: '方向提示词', key: 'direction', width: 200, ellipsis: true },
  { title: '使用次数', dataIndex: 'useCount', width: 90 },
  { title: '是否使用', key: 'used', width: 90 },
  { title: '生成时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showTotal: (t) => `共 ${t} 条`,
  showSizeChanger: true
}))

const formatTime = (t) => (t ? new Date(t).toLocaleString('zh-CN') : '-')

const reload = async () => {
  loading.value = true
  try {
    const res = await listTopicTitles({
      keyword: keyword.value.trim() || undefined,
      usedStatus: usedStatus.value,
      page: page.value,
      pageSize: pageSize.value
    })
    list.value = res.list
    total.value = res.total
    page.value = res.page
    pageSize.value = res.pageSize
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  page.value = 1
  reload()
}

const onTableChange = (p) => {
  page.value = p.current
  pageSize.value = p.pageSize
  reload()
}

const openGenerateModal = () => {
  generateCount.value = 10
  generateDirection.value = ''
  generateModalOpen.value = true
}

const onGenerate = async () => {
  generating.value = true
  try {
    const res = await generateTopicTitles({
      count: generateCount.value,
      direction: generateDirection.value.trim() || undefined
    })
    message.success(`已生成 ${res.generated} 条入库`)
    generateModalOpen.value = false
    page.value = 1
    await reload()
  } catch (e) {
    message.error(e?.message || 'AI 生成失败，请重试')
  } finally {
    generating.value = false
  }
}

const onDelete = async (record) => {
  try {
    await deleteTopicTitle(record.id)
    message.success('已删除')
    await reload()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

onMounted(reload)
</script>

<style scoped>
.topic-title-admin { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-title { margin: 0 0 4px 0; font-size: 18px; font-weight: 600; }
.page-desc { margin: 0; color: #8c8c8c; font-size: 13px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.used-strike {
  text-decoration: line-through;
  color: #bfbfbf;
}
.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.danger-link { color: #ff4d4f; }

.generate-form { padding: 8px 0 0; }
.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.form-row-top { align-items: flex-start; }
.form-label {
  flex-shrink: 0;
  width: 70px;
  color: #595959;
  font-size: 13px;
}
.form-row-top .form-label { padding-top: 5px; }
.form-hint { color: #bfbfbf; font-size: 12px; }
.generate-tip {
  margin: 0 0 0 80px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>

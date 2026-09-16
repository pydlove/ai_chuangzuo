<template>
  <div class="skill-usage-records">
    <a-page-header title="提示词使用记录" sub-title="全部提示词（市场/个人）被使用的记录" />

    <div class="filter-bar">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索提示词 / 使用者 / 作者"
        style="width: 320px"
        allow-clear
        @search="onSearch"
      />
    </div>

    <a-table :columns="columns" :data-source="records" :loading="loading"
             :pagination="pagination" row-key="articleBizNo" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <span v-if="column.key === 'skillName'">
          <a-tooltip v-if="record.skillRef && record.skillRef !== record.skillName" :title="'ref: ' + record.skillRef">
            <span>{{ record.skillName }}</span>
          </a-tooltip>
          <span v-else>{{ record.skillName }}</span>
        </span>
        <span v-else-if="column.key === 'articleTitle'">
          <a-tooltip v-if="record.articleTitle" :title="record.articleTitle" placement="topLeft">
            <span class="cell-ellipsis">{{ record.articleTitle }}</span>
          </a-tooltip>
          <span v-else>-</span>
        </span>
        <span v-else-if="column.key === 'completedAt'">{{ formatTime(record.completedAt) }}</span>
        <span v-else>{{ record[column.key] ?? '-' }}</span>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { listGlobalUsageRecords } from '@/api/marketSkill.js'

const keyword = ref('')
const queryKeyword = ref('')
const records = ref([])
const loading = ref(false)
const query = ref({ page: 1, size: 20 })
const total = ref(0)

const pagination = computed(() => ({
  current: query.value.page,
  pageSize: query.value.size,
  total: total.value,
  showSizeChanger: true
}))

const columns = [
  { title: '提示词', key: 'skillName', width: 180, ellipsis: true },
  { title: '文章标题', key: 'articleTitle' },
  { title: '使用者', key: 'userNickname', width: 140, ellipsis: true },
  { title: '作者', key: 'publisherNickname', width: 140, ellipsis: true },
  { title: '时间', key: 'completedAt', width: 160 }
]

async function load() {
  loading.value = true
  try {
    const res = await listGlobalUsageRecords({
      keyword: queryKeyword.value,
      pageNum: query.value.page,
      pageSize: query.value.size
    })
    records.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    message.error(e.message || '加载使用记录失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  queryKeyword.value = keyword.value.trim()
  query.value.page = 1
  load()
}

function handleTableChange(pagination) {
  query.value.page = pagination.current
  query.value.size = pagination.pageSize
  load()
}

function formatTime(t) {
  if (!t) return '-'
  return dayjs(t).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(load)
</script>

<style scoped>
.skill-usage-records {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  min-height: calc(100vh - 112px);
}

.filter-bar {
  margin-bottom: 16px;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>

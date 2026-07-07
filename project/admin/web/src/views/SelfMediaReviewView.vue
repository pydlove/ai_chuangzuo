<template>
  <div class="self-media-review">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">自媒体审核</h3>
        <p class="page-desc">审核用户自媒体收入申报</p>
      </div>

      <div class="toolbar">
        <a-select v-model:value="status" style="width: 120px" @change="fetchSubmissions">
          <a-select-option :value="null">全部</a-select-option>
          <a-select-option :value="0">待审核</a-select-option>
          <a-select-option :value="1">已通过</a-select-option>
          <a-select-option :value="2">已拒绝</a-select-option>
        </a-select>
        <a-input v-model:value="periodMonth" placeholder="YYYY-MM" style="width: 140px" />
        <a-button type="primary" @click="fetchSubmissions">查询</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="submissions"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.auditStatus === 0" color="warning">待审核</a-tag>
            <a-tag v-else-if="record.auditStatus === 1" color="success">已通过</a-tag>
            <a-tag v-else color="error">已拒绝</a-tag>
          </template>
          <template v-if="column.key === 'actions'">
            <a-button v-if="record.auditStatus === 0" type="link" size="small" @click="approve(record.id)">通过</a-button>
            <a-button v-if="record.auditStatus === 0" type="link" size="small" danger @click="openReject(record)">拒绝</a-button>
          </template>
        </template>
      </a-table>

      <div class="pagination">
        <a-pagination
          :current="page"
          :page-size="size"
          :total="total"
          show-size-changer
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <a-modal v-model:open="rejectVisible" title="拒绝原因" @ok="confirmReject">
      <a-textarea v-model:value="rejectReason" placeholder="请输入拒绝原因" :rows="4" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useSelfMediaReview } from '@/composables/useSelfMediaReview.js'

const {
  status, periodMonth, submissions, total, page, size, loading,
  rejectVisible, rejectReason,
  fetchSubmissions, approve, openReject, confirmReject, handlePageChange
} = useSelfMediaReview()

const columns = [
  { title: '申报ID', dataIndex: 'id', key: 'id' },
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '月份', dataIndex: 'periodMonth', key: 'periodMonth' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '平台', dataIndex: 'platform', key: 'platform' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'actions' }
]

onMounted(fetchSubmissions)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0 0 4px; }
.page-desc { color: #8c8c8c; margin: 0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

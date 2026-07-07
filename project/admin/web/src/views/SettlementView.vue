<template>
  <div class="settlement">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">结算中心</h3>
        <p class="page-desc">按月批量结算用户收益</p>
      </div>

      <div class="toolbar">
        <a-input v-model:value="month" placeholder="YYYY-MM" style="width: 140px" />
        <a-button type="primary" :loading="settling" @click="settleAll">全部结算</a-button>
      </div>

      <a-statistic v-if="summary" title="待结算金额" :value="summary.totalAmount" />

      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="false"
        row-key="userId"
        size="middle"
        style="margin-top: 16px"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-button type="link" size="small" :loading="settling" @click="settleUser(record.userId)">结算</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useSettlement } from '@/composables/useSettlement.js'

const { month, summary, users, loading, settling, fetchSummary, settleAll, settleUser } = useSettlement()

const columns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '记录数', dataIndex: 'recordCount', key: 'recordCount' },
  { title: '未结算金额', dataIndex: 'unsettledAmount', key: 'unsettledAmount' },
  { title: '操作', key: 'actions' }
]

onMounted(fetchSummary)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0 0 4px; }
.page-desc { color: #8c8c8c; margin: 0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
</style>

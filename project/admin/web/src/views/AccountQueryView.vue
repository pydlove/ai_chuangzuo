<template>
  <div class="account-query">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">账户明细</h3>
        <p class="page-desc">查看用户账户综合信息</p>
      </div>

      <div class="toolbar">
        <a-input v-model:value="query.userId" placeholder="用户ID" style="width: 120px" />
        <a-input v-model:value="query.nickname" placeholder="昵称" style="width: 180px" />
        <a-input v-model:value="query.email" placeholder="邮箱" style="width: 200px" />
        <a-button type="primary" @click="fetchAccounts">查询</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="accounts"
        :loading="loading"
        :pagination="false"
        row-key="userId"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openDetail(record.userId)">查看详情</a-button>
          </template>
        </template>
      </a-table>

      <div class="pagination">
        <a-pagination
          :current="query.page"
          :page-size="query.size"
          :total="total"
          show-size-changer
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <a-drawer v-model:open="detailVisible" title="账户详情" :width="560" placement="right">
      <pre v-if="detail">{{ JSON.stringify(detail, null, 2) }}</pre>
    </a-drawer>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAccountQuery } from '@/composables/useAccountQuery.js'

const { accounts, total, loading, query, detail, detailVisible, fetchAccounts, openDetail, handlePageChange } = useAccountQuery()

const columns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '累计收益', dataIndex: 'totalEarnings', key: 'totalEarnings' },
  { title: '未结算', dataIndex: 'unsettledEarnings', key: 'unsettledEarnings' },
  { title: '创作币余额', dataIndex: 'coinBalance', key: 'coinBalance' },
  { title: '操作', key: 'actions' }
]

onMounted(fetchAccounts)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0 0 4px; }
.page-desc { color: #8c8c8c; margin: 0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

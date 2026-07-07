<template>
  <div class="leaderboard-award">
    <a-card title="奖励发放">
      <a-form layout="inline" :model="state" class="filter-bar">
        <a-form-item label="榜单">
          <a-select v-model:value="state.leaderboardType" style="width: 160px">
            <a-select-option :value="1">创作币榜</a-select-option>
            <a-select-option :value="2">自媒体收入榜</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="月份">
          <a-date-picker v-model:value="monthObj" value-format="YYYY-MM" picker="month" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handlePreview">预览 TOP10</a-button>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" danger :loading="state.loading" @click="handleGrant">发放奖励</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :data-source="state.top10"
        :columns="columns"
        :loading="state.loading"
        row-key="userId"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'reward'">
            {{ record.rewardAmount }} 创作币
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card title="发放记录" style="margin-top: 16px">
      <a-table
        :data-source="state.rewards.items"
        :columns="rewardColumns"
        :loading="state.loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
      />
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useLeaderboardAward } from '@/composables/useLeaderboardAward.js'

const { state, fetchTop10, grant, fetchRewards } = useLeaderboardAward()

const columns = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 80 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 160 },
  { title: '榜单金额', dataIndex: 'amount', key: 'amount', width: 140 },
  { title: '奖励', key: 'reward', width: 140 }
]

const rewardColumns = [
  { title: '榜单月份', dataIndex: 'periodMonth', key: 'periodMonth', width: 120 },
  { title: '排名', dataIndex: 'rankNo', key: 'rankNo', width: 80 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '奖励金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '发放时间', dataIndex: 'grantedAt', key: 'grantedAt', width: 180 }
]

const monthObj = ref('')
watch(monthObj, (v) => {
  state.periodMonth = v || ''
})

const pagination = computed(() => ({
  current: Number(state.rewards.page) || 1,
  pageSize: Number(state.rewards.size) || 20,
  total: Number(state.rewards.total) || 0,
  showSizeChanger: true
}))

const handlePreview = async () => {
  if (!state.periodMonth) return
  await fetchTop10()
}

const handleGrant = async () => {
  if (!state.periodMonth) return
  await grant()
}

const handleTableChange = (p) => {
  fetchRewards({ page: p.current, size: p.pageSize })
}

onMounted(() => {
  const now = new Date()
  monthObj.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  fetchRewards()
})
</script>

<style scoped>
.leaderboard-award .filter-bar {
  margin-bottom: 16px;
}
</style>

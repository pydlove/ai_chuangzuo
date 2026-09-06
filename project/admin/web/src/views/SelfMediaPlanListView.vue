<template>
  <div class="plan-list">
    <a-card :bordered="false" class="plan-list-card">
      <div class="plan-list-header">
        <h3 class="plan-list-title">运营方案管理</h3>
        <p class="plan-list-desc">查看平台所有用户的自媒体运营方案</p>
      </div>

      <!-- 工具栏 -->
      <div class="plan-list-toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="用户昵称/手机号/邮箱"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button @click="fetchPlans">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="plans"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 'max-content' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'">
            <div class="user-cell">
              <div v-if="record.userNickname" class="user-nickname">{{ record.userNickname }}</div>
              <div class="user-contact">
                <span v-if="record.userPhone">{{ record.userPhone }}</span>
                <span v-if="record.userPhone && record.userEmail"> / </span>
                <span v-if="record.userEmail">{{ record.userEmail }}</span>
                <span v-if="!record.userPhone && !record.userEmail" style="color: #8c8c8c">—</span>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'platformName'">
            {{ record.platformName || '—' }}
          </template>
          <template v-else-if="column.key === 'nicheName'">
            {{ record.nicheName || '—' }}
          </template>
          <template v-else-if="column.key === 'personaName'">
            {{ record.personaName || '—' }}
          </template>
          <template v-else-if="column.key === 'contentPillars'">
            <template v-if="record.contentPillars?.length">
              <a-tag v-for="(pillar, idx) in record.contentPillars" :key="idx" color="green">
                {{ pillar.name }}<span v-if="pillar.percent"> {{ pillar.percent }}%</span>
              </a-tag>
            </template>
            <span v-else style="color: #8c8c8c">—</span>
          </template>
          <template v-else-if="column.key === 'isRecommendedByAi'">
            <a-tag :color="record.isRecommendedByAi === 1 ? 'purple' : 'default'">
              {{ record.isRecommendedByAi === 1 ? 'AI 推荐' : '手动填写' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'updatedAt'">
            {{ formatDateTime(record.updatedAt) }}
          </template>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="plan-list-pagination">
        <a-pagination
          :current="page"
          :page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50']"
          show-size-changer
          show-total
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { listSelfMediaPlans } from '@/api/selfMediaPlan.js'

const plans = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户', key: 'user', width: 220 },
  { title: '主攻平台', dataIndex: 'platformName', key: 'platformName', width: 120 },
  { title: '细分赛道', dataIndex: 'nicheName', key: 'nicheName', width: 160 },
  { title: '人设', dataIndex: 'personaName', key: 'personaName', width: 140 },
  { title: '内容支柱', key: 'contentPillars', width: 320 },
  { title: '方案来源', dataIndex: 'isRecommendedByAi', key: 'isRecommendedByAi', width: 110 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 }
]

const formatDateTime = (s) => {
  if (!s) return '—'
  return s.replace('T', ' ').slice(0, 19)
}

const fetchPlans = async () => {
  loading.value = true
  try {
    const res = await listSelfMediaPlans({
      keyword: keyword.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    plans.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    message.error(error.message || '加载运营方案列表失败')
    plans.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchPlans()
}

const handleReset = () => {
  keyword.value = ''
  page.value = 1
  fetchPlans()
}

const handlePageChange = (p, size) => {
  page.value = p
  if (size) pageSize.value = size
  fetchPlans()
}

onMounted(() => {
  fetchPlans()
})
</script>

<style scoped>
.plan-list-card {
  border-radius: 8px;
}

.plan-list-header {
  margin-bottom: 16px;
}

.plan-list-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.plan-list-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.plan-list-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.plan-list-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.user-cell {
  line-height: 1.5;
}

.user-nickname {
  font-weight: 500;
  color: #262626;
}

.user-contact {
  color: #8c8c8c;
  font-size: 12px;
}
</style>

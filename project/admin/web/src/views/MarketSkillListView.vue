<template>
  <div class="market-style">
    <a-card :bordered="false" class="market-style-card">
      <div class="market-style-header">
        <h3 class="market-style-title">提示词市场</h3>
        <p class="market-style-desc">管理用户端提示词市场中展示的提示词条目</p>
      </div>

      <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
        <a-tab-pane key="list" tab="提示词列表">
          <div class="market-style-toolbar">
            <a-select
              v-model:value="status"
              style="width: 140px"
              :options="statusOptions"
              @change="handleSearch"
            />
            <a-select
              v-model:value="featured"
              style="width: 140px"
              :options="featuredOptions"
              @change="handleSearch"
            />
            <a-input
              v-model:value="keyword"
              placeholder="按提示词名或发布者搜索"
              allow-clear
              style="width: 240px"
              @press-enter="handleSearch"
            />
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
            <a-button type="primary" @click="gotoCreate">
              <template #icon><PlusOutlined /></template>
              新建提示词
            </a-button>
            <a-button
              danger
              :disabled="selectedRowKeys?.length === 0"
              @click="onBatchDelete"
            >
              <template #icon><DeleteOutlined /></template>
              批量删除
            </a-button>
          </div>

          <a-table
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 1300 }"
            :row-selection="rowSelection"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 'enabled' ? 'green' : 'default'">
                  {{ record.status === 'enabled' ? '已上架' : '已下架' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'featured'">
                <a-tag :color="record.featured === 1 ? 'red' : 'default'">
                  {{ record.featured === 1 ? '官方精选' : '普通' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'description'">
                <span class="cell-ellipsis" :title="record.description">{{ record.description || '—' }}</span>
              </template>
              <template v-else-if="column.key === 'publisherName'">
                <span>{{ record.publisherName || record.publisherUserId }}</span>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="openDetailModal(record)">详情</a-button>
                <a-button type="link" size="small" @click="gotoEdit(record)">编辑</a-button>
                <a-popconfirm
                  :title="record.status === 'enabled' ? '确定下架此提示词？' : '确定上架此提示词？'"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="toggleStatus(record)"
                >
                  <a-button type="link" size="small" :danger="record.status === 'enabled'">{{ record.status === 'enabled' ? '下架' : '上架' }}</a-button>
                </a-popconfirm>
                <a-popconfirm
                  title="确定删除此提示词市场条目？"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="confirmDelete(record)"
                >
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>

          <div class="market-style-pagination">
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
        </a-tab-pane>

        <a-tab-pane key="stats" tab="数据概览">
          <div class="stats-panel">
            <a-spin :spinning="statsLoading">
              <div class="stats-cards">
                <a-card v-for="card in statCards" :key="card.label" :bordered="false" class="stat-card">
                  <div class="stat-label">{{ card.label }}</div>
                  <div class="stat-value">{{ card.value }}</div>
                </a-card>
              </div>

              <a-card :bordered="false" class="chart-card">
                <div class="chart-header">
                  <h4 class="chart-title">近7天使用趋势</h4>
                </div>
                <v-chart :option="trendOption" style="height: 320px" autoresize />
              </a-card>

              <div class="stats-tables">
                <a-card :bordered="false" class="stat-table-card" title="热门提示词 Top10">
                  <a-table
                    :columns="topSkillColumns"
                    :data-source="topSkills"
                    :pagination="false"
                    :row-key="(record, index) => index"
                    size="small"
                  >
                    <template #bodyCell="{ column, index }">
                      <template v-if="column.key === 'rank'">
                        <span>{{ index + 1 }}</span>
                      </template>
                    </template>
                  </a-table>
                </a-card>
                <a-card :bordered="false" class="stat-table-card" title="热门发布者 Top10">
                  <a-table
                    :columns="topPublisherColumns"
                    :data-source="topPublishers"
                    :pagination="false"
                    row-key="publisherUserId"
                    size="small"
                  >
                    <template #bodyCell="{ column, index }">
                      <template v-if="column.key === 'rank'">
                        <span>{{ index + 1 }}</span>
                      </template>
                    </template>
                  </a-table>
                </a-card>
              </div>
            </a-spin>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 详情 Modal -->
    <a-modal
      v-model:open="detailVisible"
      title="提示词详情"
      :footer="null"
      :width="800"
    >
      <a-tabs v-model:activeKey="detailActiveTab" @change="onDetailTabChange">
        <a-tab-pane key="basic" tab="基本信息">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="ID">{{ detailRecord.id }}</a-descriptions-item>
            <a-descriptions-item label="提示词名称">{{ detailRecord.name }}</a-descriptions-item>
            <a-descriptions-item label="发布者">{{ detailRecord.publisherName || detailRecord.publisherUserId }}</a-descriptions-item>
            <a-descriptions-item label="价格">{{ detailRecord.price }}</a-descriptions-item>
            <a-descriptions-item label="累计使用">{{ detailRecord.totalUses }}</a-descriptions-item>
            <a-descriptions-item label="本周使用">{{ detailRecord.weeklyUses }}</a-descriptions-item>
            <a-descriptions-item label="本周收益">{{ detailRecord.weeklyEarnings }}</a-descriptions-item>
            <a-descriptions-item label="里程碑奖励">{{ detailRecord.milestoneBonus }}</a-descriptions-item>
            <a-descriptions-item label="官方精选">
              <a-tag :color="detailRecord.featured === 1 ? 'red' : 'default'">
                {{ detailRecord.featured === 1 ? '官方精选' : '普通' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="detailRecord.status === 'enabled' ? 'green' : 'default'">
                {{ detailRecord.status === 'enabled' ? '已上架' : '已下架' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailRecord.createdAt }}</a-descriptions-item>
            <a-descriptions-item label="适用范围" :span="2">{{ detailRecord.scope || '—' }}</a-descriptions-item>
            <a-descriptions-item label="提示词摘要" :span="2">{{ detailRecord.promptSummary || '—' }}</a-descriptions-item>
            <a-descriptions-item label="描述" :span="2">{{ detailRecord.description || '—' }}</a-descriptions-item>
            <a-descriptions-item label="提示词" :span="2">
              <a-textarea :value="detailRecord.prompt" :rows="8" disabled />
            </a-descriptions-item>
          </a-descriptions>
        </a-tab-pane>
        <a-tab-pane key="usage" tab="使用记录">
          <a-table
            :columns="usageColumns"
            :data-source="usageRecords"
            :loading="usageLoading"
            :pagination="false"
            row-key="taskBizNo"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'userNickname'">
                <span>{{ record.userNickname || `用户${record.userId}` }}</span>
              </template>
            </template>
          </a-table>
          <div class="detail-pagination">
            <a-pagination
              :current="usagePage"
              :page-size="usagePageSize"
              :total="usageTotal"
              :page-size-options="['10', '20', '50']"
              show-size-changer
              show-total
              @change="handleUsagePaginationChange"
              @show-size-change="handleUsagePaginationChange"
            />
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { useMarketStyleManagement } from '@/composables/useMarketStyleManagement.js'
import { useMarketSkillStats } from '@/composables/useMarketSkillStats.js'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()

const activeTab = ref('list')
const detailActiveTab = ref('basic')
const selectedRowKeys = ref([])

const rowSelection = {
  selectedRowKeys,
  onChange: (keys) => {
    selectedRowKeys.value = keys
  }
}

const {
  list,
  total,
  loading,
  page,
  pageSize,
  keyword,
  status,
  featured,
  fetch,
  handleSearch,
  handleReset,
  handlePageChange,
  handleUpdate,
  handleDelete,
  handleBatchDelete
} = useMarketStyleManagement()

const {
  stats,
  statsLoading,
  fetchStats,
  usageRecords,
  usageTotal,
  usagePage,
  usagePageSize,
  usageLoading,
  fetchUsageRecords,
  handleUsagePageChange,
  resetUsageRecords
} = useMarketSkillStats()

const onTabChange = (key) => {
  if (key === 'stats' && !stats.value && !statsLoading.value) {
    fetchStats()
  }
}

const onDetailTabChange = (key) => {
  if (key === 'usage' && detailRecord.value?.id) {
    fetchUsageRecords(detailRecord.value.id)
  }
}

const statCards = computed(() => {
  const o = stats.value?.overview || {}
  return [
    { label: '总提示词', value: o.totalSkills ?? 0 },
    { label: '已上架', value: o.enabledSkills ?? 0 },
    { label: '官方精选', value: o.featuredSkills ?? 0 },
    { label: '累计使用', value: o.totalUses ?? 0 },
    { label: '本周使用', value: o.weeklyUses ?? 0 },
    { label: '累计收益', value: `¥${o.totalEarnings ?? '0.00'}` },
    { label: '本周收益', value: `¥${o.weeklyEarnings ?? '0.00'}` }
  ]
})

const topSkills = computed(() => stats.value?.topSkills || [])
const topPublishers = computed(() => stats.value?.topPublishers || [])
const trendData = computed(() => stats.value?.usageTrend || [])

const trendOption = computed(() => {
  const data = trendData.value
  const dates = data.map(item => item.date)
  const uses = data.map(item => item.uses)
  const earnings = data.map(item => item.earnings)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['使用次数', '收益(元)'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      { type: 'value', name: '使用次数', position: 'left' },
      { type: 'value', name: '收益(元)', position: 'right' }
    ],
    series: [
      {
        name: '使用次数',
        type: 'line',
        data: uses,
        smooth: true,
        itemStyle: { color: '#07c160' },
        areaStyle: { color: 'rgba(7,193,96,0.1)' }
      },
      {
        name: '收益(元)',
        type: 'line',
        yAxisIndex: 1,
        data: earnings,
        smooth: true,
        itemStyle: { color: '#1890ff' }
      }
    ]
  }
})

const topSkillColumns = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '提示词名称', dataIndex: 'skillName', key: 'skillName' },
  { title: '发布者', dataIndex: 'publisherName', key: 'publisherName' },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 100 },
  { title: '本周使用', dataIndex: 'weeklyUses', key: 'weeklyUses', width: 100 }
]

const topPublisherColumns = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '发布者', dataIndex: 'publisherName', key: 'publisherName' },
  { title: '提示词数', dataIndex: 'skillCount', key: 'skillCount', width: 100 },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 100 },
  { title: '本周收益', dataIndex: 'weeklyEarnings', key: 'weeklyEarnings', width: 120 }
]

const usageColumns = [
  { title: '使用者', dataIndex: 'userNickname', key: 'userNickname' },
  { title: '任务编号', dataIndex: 'taskBizNo', key: 'taskBizNo', width: 180 },
  { title: '文章编号', dataIndex: 'articleBizNo', key: 'articleBizNo', width: 180 },
  { title: '完成时间', dataIndex: 'completedAt', key: 'completedAt', width: 170 }
]

const handleUsagePaginationChange = (newPage, newPageSize) => {
  handleUsagePageChange(newPage, newPageSize)
  if (detailRecord.value?.id) {
    fetchUsageRecords(detailRecord.value.id)
  }
}

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已上架', value: 1 },
  { label: '已下架', value: 0 }
]

const featuredOptions = [
  { label: '全部精选', value: '' },
  { label: '官方精选', value: 1 },
  { label: '普通', value: 0 }
]

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
  { title: '提示词名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description', width: 160 },
  { title: '发布者', dataIndex: 'publisherName', key: 'publisherName', width: 110 },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '官方精选', dataIndex: 'featured', key: 'featured', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 240, fixed: 'right' }
]

const detailVisible = ref(false)
const detailRecord = ref({})

const openDetailModal = (record) => {
  detailRecord.value = { ...record }
  detailActiveTab.value = 'basic'
  detailVisible.value = true
  resetUsageRecords()
}

const gotoCreate = () => router.push('/console/market-skills/new')
const gotoEdit = (record) => router.push(`/console/market-skills/${record.id}`)

const confirmDelete = async (record) => {
  await handleDelete(record.id)
}

const onBatchDelete = () => {
  const keys = selectedRowKeys.value
  if (!keys || keys.length === 0) {
    message.warning('请先选择要删除的条目')
    return
  }
  Modal.confirm({
    title: `确定批量删除 ${keys.length} 条提示词市场条目？`,
    content: '删除后可在数据库中恢复，前端列表将不再展示。',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        const ok = await handleBatchDelete(keys)
        if (ok) {
          selectedRowKeys.value = []
        }
      } catch (e) {
        message.error(e.message || '批量删除失败')
      }
    }
  })
}

const toggleStatus = async (record) => {
  const nextStatus = record.status === 'enabled' ? 0 : 1
  const payload = {
    skillName: record.name,
    publisherUserId: record.publisherUserId,
    description: record.description || '',
    prompt: record.prompt || '',
    promptSummary: record.promptSummary || '',
    scope: record.scope || '',
    totalUses: record.totalUses || 0,
    enableStatus: nextStatus,
    featured: record.featured || 0
  }
  const ok = await handleUpdate(record.id, payload)
  if (ok) {
    message.success(nextStatus === 1 ? '提示词已上架' : '提示词已下架')
  }
}

onMounted(() => {
  fetch()
})
</script>

<style scoped>
.market-style-card {
  border-radius: 8px;
}

.market-style-header {
  margin-bottom: 16px;
}

.market-style-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.market-style-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.market-style-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.market-style-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.cell-ellipsis {
  display: inline-block;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  margin-bottom: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}

.stats-tables {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stat-table-card {
  border-radius: 8px;
}

.detail-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .stats-tables {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="renewal-stats">
    <div class="page-header">
      <h3 class="page-title">续费统计</h3>
      <p class="page-desc">从订单历史分析用户复购与续费情况</p>
    </div>

   <!-- 统计卡片 -->
   <div class="stats-cards">
      <a-card v-for="card in cards" :key="card.label" :bordered="false" class="stat-card" @click="openCardDetail(card)">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.prefix }}{{ card.value }}{{ card.suffix || '' }}</div>
      </a-card>
    </div>

    <!-- 续费趋势 -->
    <a-card :bordered="false" class="chart-card">
      <div class="chart-header">
        <h4 class="chart-title">续费趋势</h4>
        <a-radio-group v-model:value="trendDays" size="small" @change="loadTrend">
          <a-radio-button :value="7">近7天</a-radio-button>
          <a-radio-button :value="30">近30天</a-radio-button>
          <a-radio-button :value="90">近90天</a-radio-button>
        </a-radio-group>
      </div>
      <v-chart :option="trendOption" style="height: 320px" autoresize />
    </a-card>

    <!-- 分布图 -->
    <div class="dist-row">
      <a-card :bordered="false" class="chart-card dist-card">
        <h4 class="chart-title">续费套餐分布</h4>
        <v-chart :option="planPieOption" style="height: 280px" autoresize />
      </a-card>
      <a-card :bordered="false" class="chart-card dist-card">
        <h4 class="chart-title">续费周期分布</h4>
        <v-chart :option="cyclePieOption" style="height: 280px" autoresize />
      </a-card>
    </div>

    <!-- 续费用户明细 -->
    <a-card :bordered="false" class="table-card">
      <div class="table-toolbar">
        <a-input v-model:value="query.keyword" placeholder="昵称/邮箱" style="width: 200px" />
        <a-select v-model:value="query.planKey" style="width: 140px" placeholder="套餐" allow-clear>
          <a-select-option value="basic">基础版</a-select-option>
          <a-select-option value="pro">专业版</a-select-option>
          <a-select-option value="flagship">旗舰版</a-select-option>
        </a-select>
        <a-select v-model:value="query.cycle" style="width: 120px" placeholder="周期" allow-clear>
          <a-select-option value="month">月付</a-select-option>
          <a-select-option value="quarter">季付</a-select-option>
          <a-select-option value="year">年付</a-select-option>
        </a-select>
        <a-date-picker v-model:value="query.startDate" format="YYYY-MM-DD" placeholder="开始日期" />
        <a-date-picker v-model:value="query.endDate" format="YYYY-MM-DD" placeholder="结束日期" />
        <a-button type="primary" @click="searchUsers">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
      </div>
      <a-table
        :columns="userColumns"
        :data-source="userList"
        :loading="userLoading"
        :pagination="{ current: query.page, pageSize: query.pageSize, total: userTotal, showSizeChanger: true }"
        row-key="userId"
        size="middle"
        @change="handlePageChange"
      >
        <template #bodyCell="{ column, record }">
          <span v-if="column.key === 'firstPaidAt'">{{ formatTime(record.firstPaidAt) }}</span>
          <span v-else-if="column.key === 'lastRenewalAt'">{{ formatTime(record.lastRenewalAt) }}</span>
          <span v-else-if="column.key === 'expiresAt'">{{ formatDate(record.expiresAt) }}</span>
          <span v-else>{{ record[column.dataIndex || column.key] ?? '-' }}</span>
        </template>
      </a-table>
    </a-card>
    
    <!-- 卡片明细弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="detailTitle"
      :width="1100"
      :footer="null"
      @cancel="closeDetail"
    >
      <div class="detail-toolbar">
        <a-input v-model:value="detailQuery.keyword" placeholder="昵称/邮箱/订单号" style="width: 200px" />
        <a-select v-model:value="detailQuery.planKey" style="width: 140px" placeholder="套餐" allow-clear>
          <a-select-option value="basic">基础版</a-select-option>
          <a-select-option value="pro">专业版</a-select-option>
          <a-select-option value="flagship">旗舰版</a-select-option>
        </a-select>
        <a-select v-model:value="detailQuery.cycle" style="width: 120px" placeholder="周期" allow-clear>
          <a-select-option value="month">月付</a-select-option>
          <a-select-option value="quarter">季付</a-select-option>
          <a-select-option value="year">年付</a-select-option>
        </a-select>
        <a-date-picker v-model:value="detailQuery.startDate" format="YYYY-MM-DD" placeholder="开始日期" />
        <a-date-picker v-model:value="detailQuery.endDate" format="YYYY-MM-DD" placeholder="结束日期" />
        <a-button type="primary" @click="searchDetail">查询</a-button>
        <a-button @click="resetDetailQuery">重置</a-button>
      </div>
      <a-table
        :columns="detailColumns"
        :data-source="detailList"
        :loading="detailLoading"
        :pagination="detailPagination"
        :row-key="detailRowKey"
        size="middle"
        @change="handleDetailPageChange"
      >
        <template #bodyCell="{ column, record }">
          <span v-if="column.key === 'firstPaidAt'">{{ formatTime(record.firstPaidAt) }}</span>
          <span v-else-if="column.key === 'lastRenewalAt'">{{ formatTime(record.lastRenewalAt) }}</span>
          <span v-else-if="column.key === 'expiresAt'">{{ formatDate(record.expiresAt) }}</span>
          <span v-else-if="column.key === 'paidAt'">{{ formatTime(record.paidAt) }}</span>
          <span v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</span>
          <span v-else-if="column.key === 'renewalAmount'">{{ formatMoney(record.renewalAmount) }}</span>
          <span v-else-if="column.key === 'user'">
            <div>{{ record.nickname || '-' }}</div>
            <div style="font-size: 12px; color: #8c8c8c">{{ record.email || '-' }}</div>
          </span>
          <span v-else>{{ record[column.dataIndex || column.key] ?? '-' }}</span>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import {
  getRenewalOverview,
  getRenewalTrend,
  getRenewalDistribution,
  getRenewalUserList,
  getRenewalPaidUsers,
  getRenewalOrderList
} from '@/api/order.js'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// ── 统计卡片 ──
const overview = ref(null)

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '累计付费用户', value: o.totalPaidUsers ?? 0, prefix: '', type: 'paidUsers', title: '累计付费用户明细' },
    { label: '续费用户', value: o.renewalUsers ?? 0, prefix: '', type: 'renewalUsers', title: '续费用户明细' },
    { label: '续费率', value: o.renewalRate ?? 0, prefix: '', suffix: '%', type: 'paidUsers', title: '累计付费用户明细' },
    { label: '续费订单', value: o.renewalOrders ?? 0, prefix: '', type: 'renewalOrders', title: '续费订单明细' },
    { label: '续费金额', value: o.renewalRevenue ?? '0.00', prefix: '¥', type: 'renewalOrders', title: '续费订单明细' },
    { label: '新购金额', value: o.firstRevenue ?? '0.00', prefix: '¥', type: 'firstOrders', title: '新购订单明细' }
  ]
})

function openCardDetail(card) {
  if (!card.type) return
  detailType.value = card.type
  detailTitle.value = card.title
  detailQuery.value = {
    keyword: '',
    planKey: null,
    cycle: null,
    startDate: null,
    endDate: null,
    page: 1,
    pageSize: 20
  }
  detailVisible.value = true
  loadDetail()
}

// ── 续费趋势 ──
const trendDays = ref(7)
const trendData = ref(null)

const trendOption = computed(() => {
  const d = trendData.value || { dates: [], revenues: [], orderCounts: [], userCounts: [] }
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['续费金额', '续费订单', '续费用户'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: d.dates },
    yAxis: [
      { type: 'value', name: '金额(元)', position: 'left' },
      { type: 'value', name: '数量', position: 'right' }
    ],
    series: [
      {
        name: '续费金额',
        type: 'line',
        data: d.revenues,
        smooth: true,
        itemStyle: { color: '#07c160' },
        areaStyle: { color: 'rgba(7,193,96,0.1)' }
      },
      {
        name: '续费订单',
        type: 'line',
        yAxisIndex: 1,
        data: d.orderCounts,
        smooth: true,
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '续费用户',
        type: 'line',
        yAxisIndex: 1,
        data: d.userCounts,
        smooth: true,
        lineStyle: { type: 'dashed' },
        itemStyle: { color: '#faad14' }
      }
    ]
  }
})

// ── 分布饼图 ──
const distData = ref(null)

const planPieOption = computed(() => {
  const plans = distData.value?.plans || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      data: plans.map(p => ({ name: p.planName, value: p.count })),
      label: { formatter: '{b}\n{d}%' }
    }]
  }
})

const cyclePieOption = computed(() => {
  const cycles = distData.value?.cycles || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      data: cycles.map(c => ({ name: c.cycleName, value: c.count })),
      label: { formatter: '{b}\n{d}%' }
    }]
  }
})

// ── 用户明细 ──
const userList = ref([])
const userTotal = ref(0)
const userLoading = ref(false)
const query = ref({
  keyword: '',
  planKey: null,
  cycle: null,
  startDate: null,
  endDate: null,
  page: 1,
  pageSize: 20
})

const userColumns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 140 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '首次购买', dataIndex: 'firstPaidAt', key: 'firstPaidAt', width: 150 },
  { title: '最近续费', dataIndex: 'lastRenewalAt', key: 'lastRenewalAt', width: 150 },
  { title: '续费次数', dataIndex: 'renewalCount', key: 'renewalCount', width: 100 },
  { title: '续费金额', dataIndex: 'renewalAmount', key: 'renewalAmount', width: 120 },
  { title: '当前套餐', dataIndex: 'currentLevel', key: 'currentLevel', width: 120 },
  { title: '到期时间', dataIndex: 'expiresAt', key: 'expiresAt', width: 120 }
]

// ── 卡片明细弹窗 ──
const detailVisible = ref(false)
const detailTitle = ref('')
const detailType = ref('')
const detailList = ref([])
const detailTotal = ref(0)
const detailLoading = ref(false)
const detailColumns = ref([])
const detailQuery = ref({
  keyword: '',
  planKey: null,
  cycle: null,
  startDate: null,
  endDate: null,
  page: 1,
  pageSize: 20
})
const detailRowKey = computed(() => {
  return detailType.value === 'paidUsers' || detailType.value === 'renewalUsers' ? 'userId' : 'id'
})
const detailPagination = computed(() => ({
  current: detailQuery.value.page,
  pageSize: detailQuery.value.pageSize,
  total: detailTotal.value,
  showSizeChanger: true
}))

const detailUserColumns = [
  { title: '用户', key: 'user', width: 200 },
  { title: '首次购买', dataIndex: 'firstPaidAt', key: 'firstPaidAt', width: 150 },
  { title: '最近续费', dataIndex: 'lastRenewalAt', key: 'lastRenewalAt', width: 150 },
  { title: '续费次数', dataIndex: 'renewalCount', key: 'renewalCount', width: 100 },
  { title: '续费金额', dataIndex: 'renewalAmount', key: 'renewalAmount', width: 120 },
  { title: '当前套餐', dataIndex: 'currentLevel', key: 'currentLevel', width: 120 },
  { title: '到期时间', dataIndex: 'expiresAt', key: 'expiresAt', width: 120 }
]

const orderColumns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 190 },
  { title: '用户', key: 'user', width: 200 },
  { title: '套餐', dataIndex: 'planName', key: 'planName', width: 120 },
  { title: '周期', dataIndex: 'cycleName', key: 'cycleName', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '支付时间', dataIndex: 'paidAt', key: 'paidAt', width: 160 }
]

async function loadDetail() {
  detailLoading.value = true
  try {
    const params = {
      keyword: detailQuery.value.keyword || undefined,
      planKey: detailQuery.value.planKey || undefined,
      cycle: detailQuery.value.cycle || undefined,
      startDate: detailQuery.value.startDate ? dayjs(detailQuery.value.startDate).format('YYYY-MM-DD') : undefined,
      endDate: detailQuery.value.endDate ? dayjs(detailQuery.value.endDate).format('YYYY-MM-DD') : undefined,
      page: detailQuery.value.page,
      pageSize: detailQuery.value.pageSize
    }
    if (detailType.value === 'paidUsers') {
      const res = await getRenewalPaidUsers(params)
      detailList.value = res.list || []
      detailTotal.value = res.total || 0
      detailColumns.value = detailUserColumns
    } else if (detailType.value === 'renewalUsers') {
      const res = await getRenewalUserList(params)
      detailList.value = res.list || []
      detailTotal.value = res.total || 0
      detailColumns.value = detailUserColumns
    } else {
      params.type = detailType.value === 'firstOrders' ? 'first' : 'renewal'
      const res = await getRenewalOrderList(params)
      detailList.value = res.list || []
      detailTotal.value = res.total || 0
      detailColumns.value = orderColumns
    }
  } catch (e) {
    /* handled */
  } finally {
    detailLoading.value = false
  }
}

function searchDetail() {
  detailQuery.value.page = 1
  loadDetail()
}

function resetDetailQuery() {
  detailQuery.value = {
    keyword: '',
    planKey: null,
    cycle: null,
    startDate: null,
    endDate: null,
    page: 1,
    pageSize: 20
  }
  loadDetail()
}

function handleDetailPageChange(pagination) {
  detailQuery.value.page = pagination.current
  detailQuery.value.pageSize = pagination.pageSize
  loadDetail()
}

function closeDetail() {
  detailVisible.value = false
}

function formatMoney(v) {
  return v ? `¥${Number(v).toFixed(2)}` : '-'
}

// ── 数据加载 ──
async function loadOverview() {
  try {
    overview.value = await getRenewalOverview()
  } catch (e) { /* handled */ }
}

async function loadTrend() {
  try {
    trendData.value = await getRenewalTrend(trendDays.value)
  } catch (e) { /* handled */ }
}

async function loadDistribution() {
  try {
    distData.value = await getRenewalDistribution()
  } catch (e) { /* handled */ }
}

async function loadUsers() {
  userLoading.value = true
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      planKey: query.value.planKey || undefined,
      cycle: query.value.cycle || undefined,
      startDate: query.value.startDate ? dayjs(query.value.startDate).format('YYYY-MM-DD') : undefined,
      endDate: query.value.endDate ? dayjs(query.value.endDate).format('YYYY-MM-DD') : undefined,
      page: query.value.page,
      pageSize: query.value.pageSize
    }
    const res = await getRenewalUserList(params)
    userList.value = res.list || []
    userTotal.value = res.total || 0
  } catch (e) {
    /* handled */
  } finally {
    userLoading.value = false
  }
}

function searchUsers() {
  query.value.page = 1
  loadUsers()
}

function resetQuery() {
  query.value = {
    keyword: '',
    planKey: null,
    cycle: null,
    startDate: null,
    endDate: null,
    page: 1,
    pageSize: 20
  }
  loadUsers()
}

function handlePageChange(pagination) {
  query.value.page = pagination.current
  query.value.pageSize = pagination.pageSize
  loadUsers()
}

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

function formatDate(d) {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadDistribution()
  loadUsers()
})
</script>

<style scoped>
.renewal-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  margin-bottom: 8px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.page-desc {
  color: #8c8c8c;
  margin: 0;
  font-size: 13px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  text-align: center;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}

.dist-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.dist-card {
  text-align: center;
}

.table-card {
  border-radius: 8px;
}

.table-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.detail-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>

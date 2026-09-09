<template>
  <div class="console-page">
    <div class="cockpit-header">
      <div class="cockpit-title">
        <h2 class="cockpit-heading">运营驾驶舱</h2>
        <p class="cockpit-sub">
          <span class="live-dot"></span>
          数据每 30 秒自动刷新<template v-if="lastUpdated"> · 最后更新 {{ lastUpdated }}</template>
        </p>
      </div>
      <a-button :loading="loading" @click="loadAll">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <div v-if="error" class="dashboard-error">
      看板数据加载失败：{{ error }}
    </div>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :sm="12" :xl="6">
        <div class="kpi-card kpi-hero">
          <div class="kpi-top">
            <span class="kpi-name">今日收入</span>
            <span class="kpi-badge" :class="deltaClass(todayPaidAmountDelta)">{{ deltaText(todayPaidAmountDelta) }}</span>
          </div>
          <div class="kpi-value">¥{{ formatAmount(stats.todayPaidAmount) }}</div>
          <div class="kpi-foot">昨日 ¥{{ formatAmount(yesterdayPaidAmount) }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :xl="6">
        <div class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-name"><i class="kpi-dot" style="background:#ff2442"></i>今日新增用户</span>
            <span class="kpi-badge" :class="deltaClass(todayNewUsersDelta)">{{ deltaText(todayNewUsersDelta) }}</span>
          </div>
          <div class="kpi-value">{{ formatCount(stats.todayNewUsers) }}</div>
          <div class="kpi-foot">累计用户 {{ formatCount(stats.totalUsers) }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :xl="6">
        <div class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-name"><i class="kpi-dot" style="background:#1989fa"></i>今日生成文章</span>
            <span class="kpi-badge" :class="deltaClass(todayArticlesDelta)">{{ deltaText(todayArticlesDelta) }}</span>
          </div>
          <div class="kpi-value">{{ formatCount(stats.todayArticles) }}</div>
          <div class="kpi-foot">累计文章 {{ formatCount(stats.totalArticles) }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :xl="6">
        <div class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-name"><i class="kpi-dot" style="background:#07c160"></i>当前在线</span>
          </div>
          <div class="kpi-value">{{ formatCount(stats.onlineCount) }}</div>
          <div class="kpi-foot">今日活跃 {{ formatCount(stats.todayActive) }}</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="cockpit-row">
      <a-col :xs="24" :xl="16">
        <a-card :bordered="false" class="cockpit-card">
          <template #title>
            <span class="card-title">30 天运营趋势</span>
          </template>
          <template #extra>
            <a-radio-group v-model:value="trendTab" size="small" option-type="button" :options="trendTabOptions" />
          </template>
          <v-chart v-if="trendPoints.length" :key="trendTab" :option="trendOption" style="height: 340px" autoresize />
          <a-empty v-else description="暂无趋势数据" :image-style="{ height: '80px' }" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="cockpit-card" title="累计收入构成">
          <v-chart v-if="distribution.paidAmountByPlan.length" :option="incomePieOption" style="height: 340px" autoresize />
          <a-empty v-else description="暂无收入数据" :image-style="{ height: '80px' }" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="cockpit-row">
      <a-col :xs="24" :md="12" :xl="8">
        <a-card :bordered="false" class="cockpit-card" title="作品平台分布">
          <v-chart v-if="distribution.articlePlatforms.length" :option="platformBarOption" style="height: 280px" autoresize />
          <a-empty v-else description="暂无作品数据" :image-style="{ height: '80px' }" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12" :xl="8">
        <a-card :bordered="false" class="cockpit-card" title="有效会员套餐分布">
          <div v-if="distribution.memberPlans.length" class="member-summary">
            <div class="member-total">{{ formatCount(stats.validMembers) }}<span>有效会员</span></div>
            <v-chart :option="memberPieOption" style="height: 220px" autoresize />
          </div>
          <a-empty v-else description="暂无会员数据" :image-style="{ height: '80px' }" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="24" :xl="8">
        <a-card :bordered="false" class="cockpit-card" title="待处理事项">
          <div class="todo-list">
            <router-link to="/console/earnings/withdrawals" class="todo-item">
              <div class="todo-info">
                <span class="todo-name">待审核提现</span>
                <span class="todo-desc">创作者提现申请等待审核</span>
              </div>
              <a-badge :count="stats.pendingWithdraws" :overflow-count="99" :number-style="todoBadgeStyle" />
            </router-link>
          </div>
          <div class="stat-divider"></div>
          <div class="accum-list">
            <div class="accum-item">
              <span class="accum-label">累计收入</span>
              <span class="accum-value">¥{{ formatAmount(stats.totalPaidAmount) }}</span>
            </div>
            <div class="accum-item">
              <span class="accum-label">累计生成文章</span>
              <span class="accum-value">{{ formatCount(stats.totalArticles) }}</span>
            </div>
            <div class="accum-item">
              <span class="accum-label">注册用户</span>
              <span class="accum-value">{{ formatCount(stats.totalUsers) }}</span>
            </div>
            <div class="accum-item">
              <span class="accum-label">今日支付订单</span>
              <span class="accum-value">{{ formatCount(stats.todayPaidOrders) }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { getDashboard, getDashboardDistribution, getDashboardTrend } from '@/api/stats'

use([CanvasRenderer, BarChart, LineChart, PieChart, TooltipComponent, LegendComponent, GridComponent])

const empty = {
  onlineCount: 0,
  todayActive: 0,
  totalUsers: 0,
  todayNewUsers: 0,
  validMembers: 0,
  totalArticles: 0,
  todayArticles: 0,
  todayPaidOrders: 0,
  todayPaidAmount: 0,
  totalPaidAmount: 0,
  pendingWithdraws: 0
}

const stats = reactive({ ...empty })
const trendPoints = ref([])
const distribution = reactive({ memberPlans: [], articlePlatforms: [], paidAmountByPlan: [] })
const error = ref('')
const loading = ref(false)
const lastUpdated = ref('')
const trendTab = ref('trade')

const trendTabOptions = [
  { label: '交易', value: 'trade' },
  { label: '用户', value: 'user' },
  { label: '创作', value: 'article' }
]

const todoBadgeStyle = { backgroundColor: '#ff2442' }

const CHART_COLORS = ['#ff2442', '#1989fa', '#07c160', '#fa8c16', '#9254de', '#13c2c2', '#eb2f96', '#faad14']
const AXIS_COLOR = '#8c8c8c'
const SPLIT_COLOR = '#f0f0f0'

let timer = null

function formatAmount(value) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatCount(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function calcDelta(today, yesterday) {
  today = Number(today || 0)
  yesterday = Number(yesterday || 0)
  if (today === yesterday) return 0
  if (yesterday === 0) return null
  return (today - yesterday) / yesterday
}

function deltaText(delta) {
  if (delta === null) return 'NEW'
  if (delta === 0) return '— 持平'
  const percent = Math.abs(delta * 100).toFixed(1)
  return delta > 0 ? `↑ ${percent}%` : `↓ ${percent}%`
}

function deltaClass(delta) {
  if (delta === null || delta > 0) return 'kpi-badge-up'
  if (delta < 0) return 'kpi-badge-down'
  return 'kpi-badge-flat'
}

const yesterdayPoint = computed(() => trendPoints.value.length >= 2
  ? trendPoints.value[trendPoints.value.length - 2]
  : null)

const todayPaidAmountDelta = computed(() => yesterdayPoint.value
  ? calcDelta(stats.todayPaidAmount, yesterdayPoint.value.paidAmount)
  : 0)
const yesterdayPaidAmount = computed(() => yesterdayPoint.value ? yesterdayPoint.value.paidAmount : 0)
const todayNewUsersDelta = computed(() => yesterdayPoint.value
  ? calcDelta(stats.todayNewUsers, yesterdayPoint.value.newUsers)
  : 0)
const todayArticlesDelta = computed(() => yesterdayPoint.value
  ? calcDelta(stats.todayArticles, yesterdayPoint.value.articles)
  : 0)

const dates = computed(() => trendPoints.value.map((p) => p.date.slice(5)))

function baseAxis() {
  return {
    xAxis: {
      type: 'category',
      data: dates.value,
      axisLine: { lineStyle: { color: SPLIT_COLOR } },
      axisTick: { show: false },
      axisLabel: { color: AXIS_COLOR }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: SPLIT_COLOR } },
      axisLabel: { color: AXIS_COLOR }
    }
  }
}

const trendOption = computed(() => {
  const axis = baseAxis()
  const tooltip = {
    trigger: 'axis',
    backgroundColor: 'rgba(255,255,255,0.96)',
    borderColor: '#eeeeee',
    textStyle: { color: '#262626', fontSize: 12 }
  }
  if (trendTab.value === 'trade') {
    return {
      color: CHART_COLORS,
      tooltip,
      legend: { top: 0, right: 0, textStyle: { color: AXIS_COLOR } },
      grid: { left: 8, right: 8, top: 36, bottom: 0, containLabel: true },
      ...axis,
      yAxis: [
        { ...axis.yAxis, axisLabel: { color: AXIS_COLOR, formatter: (v) => `¥${v}` } },
        { ...axis.yAxis, splitLine: { show: false }, axisLabel: { color: AXIS_COLOR } }
      ],
      series: [
        {
          name: '支付金额',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: false,
          lineStyle: { width: 3 },
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(255,36,66,0.16)' },
                { offset: 1, color: 'rgba(255,36,66,0.01)' }
              ]
            }
          },
          data: trendPoints.value.map((p) => Number(p.paidAmount))
        },
        {
          name: '支付订单',
          type: 'bar',
          yAxisIndex: 1,
          barMaxWidth: 14,
          itemStyle: { color: 'rgba(25,137,250,0.75)', borderRadius: [3, 3, 0, 0] },
          data: trendPoints.value.map((p) => p.paidOrders)
        }
      ]
    }
  }
  if (trendTab.value === 'user') {
    return {
      color: ['#ff2442', '#fa8c16'],
      tooltip,
      legend: { top: 0, right: 0, textStyle: { color: AXIS_COLOR } },
      grid: { left: 8, right: 8, top: 36, bottom: 0, containLabel: true },
      ...axis,
      series: [
        {
          name: '新增用户',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: false,
          lineStyle: { width: 3 },
          data: trendPoints.value.map((p) => p.newUsers)
        },
        {
          name: '活跃用户',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: false,
          lineStyle: { width: 3, type: 'dashed' },
          data: trendPoints.value.map((p) => p.activeUsers)
        }
      ]
    }
  }
  return {
    color: ['#1989fa'],
    tooltip,
    grid: { left: 8, right: 8, top: 24, bottom: 0, containLabel: true },
    ...axis,
    series: [
      {
        name: '生成文章',
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#1989fa' },
              { offset: 1, color: 'rgba(25,137,250,0.45)' }
            ]
          }
        },
        data: trendPoints.value.map((p) => p.articles)
      }
    ]
  }
})

const incomePieOption = computed(() => ({
  color: CHART_COLORS,
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(255,255,255,0.96)',
    borderColor: '#eeeeee',
    textStyle: { color: '#262626', fontSize: 12 },
    formatter: (p) => `${p.name}<br/>¥${formatAmount(p.value)}（${p.percent}%）`
  },
  legend: { bottom: 0, textStyle: { color: AXIS_COLOR } },
  series: [
    {
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, formatter: '{b}\n¥{c}', fontSize: 14, fontWeight: 600, color: '#262626' }
      },
      data: distribution.paidAmountByPlan.map((item) => ({ name: item.name, value: Number(item.amount) }))
    }
  ]
}))

const platformBarOption = computed(() => {
  const items = [...distribution.articlePlatforms].reverse()
  return {
    color: ['#07c160'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#eeeeee',
      textStyle: { color: '#262626', fontSize: 12 }
    },
    grid: { left: 8, right: 24, top: 8, bottom: 0, containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: SPLIT_COLOR } },
      axisLabel: { color: AXIS_COLOR }
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisLine: { lineStyle: { color: SPLIT_COLOR } },
      axisTick: { show: false },
      axisLabel: { color: '#595959' }
    },
    series: [
      {
        name: '作品数',
        type: 'bar',
        barMaxWidth: 14,
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: 'rgba(7,193,96,0.45)' },
              { offset: 1, color: '#07c160' }
            ]
          }
        },
        label: { show: true, position: 'right', color: AXIS_COLOR, fontSize: 11 },
        data: items.map((item) => item.count)
      }
    ]
  }
})

const memberPieOption = computed(() => ({
  color: CHART_COLORS,
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(255,255,255,0.96)',
    borderColor: '#eeeeee',
    textStyle: { color: '#262626', fontSize: 12 }
  },
  legend: { bottom: 0, textStyle: { color: AXIS_COLOR } },
  series: [
    {
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '46%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, formatter: '{b}\n{c} 人', fontSize: 13, fontWeight: 600, color: '#262626' }
      },
      data: distribution.memberPlans.map((item) => ({ name: item.name, value: item.count }))
    }
  ]
}))

async function loadAll() {
  loading.value = true
  try {
    const [overview, trend, dist] = await Promise.all([
      getDashboard(),
      getDashboardTrend(30),
      getDashboardDistribution()
    ])
    Object.assign(stats, empty, overview)
    trendPoints.value = trend.points || []
    Object.assign(distribution, {
      memberPlans: dist.memberPlans || [],
      articlePlatforms: dist.articlePlatforms || [],
      paidAmountByPlan: dist.paidAmountByPlan || []
    })
    error.value = ''
    lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) {
    error.value = e.message || '未知错误'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
  timer = setInterval(loadAll, 30000)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.console-page {
  padding: 24px;
}

.cockpit-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.cockpit-heading {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.cockpit-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 0 0;
  color: var(--color-text-placeholder);
  font-size: 13px;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 0 3px rgba(7, 193, 96, 0.15);
}

.dashboard-error {
  margin: 12px 0;
  color: var(--color-error);
}

.kpi-card {
  height: 100%;
  padding: 20px 22px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm2);
}

.kpi-hero {
  background: linear-gradient(135deg, #fff5f6 0%, #ffffff 70%);
  border-color: #ffd6dc;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.kpi-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.kpi-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.kpi-badge {
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.kpi-badge-up {
  color: #cf1322;
  background: #fff0f2;
}

.kpi-badge-down {
  color: #389e0d;
  background: #f6ffed;
}

.kpi-badge-flat {
  color: var(--color-text-placeholder);
  background: var(--color-bg-hover);
}

.kpi-value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-primary);
}

.kpi-foot {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.cockpit-row {
  margin-top: 16px;
}

.cockpit-card {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm2);
}

.cockpit-card :deep(.ant-card-head) {
  border-bottom: 1px solid var(--color-border-light);
  min-height: 48px;
}

.cockpit-card :deep(.ant-card-head-title) {
  font-weight: 600;
}

.card-title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.member-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.member-total {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.member-total span {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-placeholder);
}

.todo-list {
  display: flex;
  flex-direction: column;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-lg);
  transition: background 0.2s;
}

.todo-item:hover {
  background: var(--color-bg-hover);
}

.todo-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.todo-name {
  font-weight: 600;
  color: var(--color-text-regular);
}

.todo-desc {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.stat-divider {
  height: 1px;
  margin: 8px 0 12px;
  background: var(--color-border-light);
}

.accum-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.accum-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  background: var(--color-bg-page);
  border-radius: var(--radius-lg);
}

.accum-label {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.accum-value {
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-primary);
}
</style>

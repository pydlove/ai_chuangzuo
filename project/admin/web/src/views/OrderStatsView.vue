<template>
  <div class="order-stats">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <a-card v-for="card in cards" :key="card.label" :bordered="false" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.prefix }}{{ card.value }}</div>
      </a-card>
    </div>

    <!-- 收入趋势 -->
    <a-card :bordered="false" class="chart-card">
      <div class="chart-header">
        <h4 class="chart-title">收入趋势</h4>
        <a-radio-group v-model:value="trendDays" size="small" @change="loadTrend">
          <a-radio-button :value="7">近7天</a-radio-button>
          <a-radio-button :value="30">近30天</a-radio-button>
        </a-radio-group>
      </div>
      <v-chart :option="trendOption" style="height: 320px" autoresize />
    </a-card>

    <!-- 分布图 -->
    <div class="dist-row">
      <a-card :bordered="false" class="chart-card dist-card">
        <h4 class="chart-title">套餐分布</h4>
        <v-chart :option="planPieOption" style="height: 280px" autoresize />
      </a-card>
      <a-card :bordered="false" class="chart-card dist-card">
        <h4 class="chart-title">周期分布</h4>
        <v-chart :option="cyclePieOption" style="height: 280px" autoresize />
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { getOrderStatsOverview, getOrderTrend, getPlanDistribution } from '@/api/order.js'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// ── 统计卡片 ──
const overview = ref(null)

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '今日订单', value: o.todayOrderCount ?? 0, prefix: '' },
    { label: '今日收入', value: o.todayRevenue ?? '0.00', prefix: '¥' },
    { label: '本月订单', value: o.monthOrderCount ?? 0, prefix: '' },
    { label: '本月收入', value: o.monthRevenue ?? '0.00', prefix: '¥' },
    { label: '累计订单', value: o.totalOrderCount ?? 0, prefix: '' },
    { label: '累计收入', value: o.totalRevenue ?? '0.00', prefix: '¥' }
  ]
})

// ── 收入趋势 ──
const trendDays = ref(7)
const trendData = ref(null)

const trendOption = computed(() => {
  const d = trendData.value || { dates: [], revenues: [], orderCounts: [] }
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '订单数'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: d.dates },
    yAxis: [
      { type: 'value', name: '收入(元)', position: 'left' },
      { type: 'value', name: '订单数', position: 'right' }
    ],
    series: [
      {
        name: '收入',
        type: 'line',
        data: d.revenues,
        smooth: true,
        itemStyle: { color: '#07c160' },
        areaStyle: { color: 'rgba(7,193,96,0.1)' }
      },
      {
        name: '订单数',
        type: 'line',
        yAxisIndex: 1,
        data: d.orderCounts,
        smooth: true,
        lineStyle: { type: 'dashed' },
        itemStyle: { color: '#1890ff' }
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

// ── 数据加载 ──
async function loadOverview() {
  try {
    overview.value = await getOrderStatsOverview()
  } catch (e) { /* handled */ }
}

async function loadTrend() {
  try {
    trendData.value = await getOrderTrend(trendDays.value)
  } catch (e) { /* handled */ }
}

async function loadDistribution() {
  try {
    distData.value = await getPlanDistribution()
  } catch (e) { /* handled */ }
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadDistribution()
})
</script>

<style scoped>
.order-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
</style>

<template>
  <div class="console-page">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">管理控制台</h3>
        <p class="page-desc">平台实时运营概览</p>
      </div>

      <a-row :gutter="16">
        <a-col :span="8">
          <a-card class="stat-card">
            <div class="stat-value">{{ stats.onlineCount }}</div>
            <div class="stat-label">当前在线（5 分钟内活跃）</div>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card class="stat-card">
            <div class="stat-value">{{ stats.todayActive }}</div>
            <div class="stat-label">今日活跃</div>
          </a-card>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive } from 'vue'
import { getStatsOverview } from '@/api/stats'

const stats = reactive({
  onlineCount: 0,
  todayActive: 0
})

let timer = null

async function load() {
  try {
    const data = await getStatsOverview()
    stats.onlineCount = data.onlineCount ?? 0
    stats.todayActive = data.todayActive ?? 0
  } catch (e) {
    // 看板加载失败不打断页面
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 30000)
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

.page-header {
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
}

.page-desc {
  margin: 0;
  color: rgba(0, 0, 0, 0.45);
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-label {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.45);
}
</style>

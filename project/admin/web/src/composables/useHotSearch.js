import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import * as api from '@/api/hotSearch.js'

export function useHotSearch() {
  const state = reactive({
    platforms: [],
    daily: { items: [], total: 0, page: 1, size: 20 },
    config: { id: 1, cron: '', enabled: 1, topN: 50, connectTimeoutMillis: 5000, readTimeoutMillis: 10000 },
    lastRun: { lastRunAt: null, totalFetched: 0, successCount: 0, failCount: 0, results: [] },
    loading: false
  })

  const fetchPlatforms = async () => {
    state.loading = true
    try {
      state.platforms = await api.listPlatforms()
    } finally { state.loading = false }
  }
  const fetchDaily = async (params) => {
    state.loading = true
    try {
      state.daily = await api.listDaily(params || {})
    } finally { state.loading = false }
  }
  const fetchConfig = async () => {
    state.config = await api.getConfig()
  }
  const fetchLastRun = async () => {
    state.lastRun = await api.getLastRun()
  }

  return {
    state,
    fetchPlatforms, fetchDaily, fetchConfig, fetchLastRun,
    savePlatform: async (data) => { await api.createPlatform(data); message.success('已新增') },
    updatePlatform: async (id, data) => { await api.updatePlatform(id, data); message.success('已更新') },
    removePlatform: async (id) => { await api.deletePlatform(id); message.success('已删除') },
    saveDaily: async (data) => { await api.createDaily(data); message.success('已新增') },
    updateDaily: async (id, data) => { await api.updateDaily(id, data); message.success('已更新') },
    removeDaily: async (id) => { await api.deleteDaily(id); message.success('已删除') },
    saveConfig: async (data) => { await api.saveConfig(data); message.success('已保存，定时任务已重建') },
    crawlNow: async () => { const r = await api.crawlNow(); message.success('抓取完成'); return r },
    recrawlDaily: async (id) => { const r = await api.recrawlDaily(id); message.success('重抓完成'); return r }
  }
}

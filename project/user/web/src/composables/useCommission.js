import { ref } from 'vue'
import {
  getCommissionStats,
  getCommissionTask,
  listCommissionTasks,
  listMyCommissionSubmissions,
  submitCommissionArticle,
  withdrawCommissionSubmission
} from '@/api/commission'

const tasks = ref([])
const taskDetail = ref(null)
const mySubmissions = ref([])
const stats = ref({ activeTaskCount: 0, mySubmissionCount: 0, earnedCoinTotal: 0 })
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

export function useCommission() {
  async function loadTasks(params = {}) {
    loading.value = true
    try {
      const data = await listCommissionTasks({ page: page.value, pageSize: pageSize.value, ...params })
      tasks.value = data.records || data.list || []
      total.value = Number(data.total) || 0
      return data
    } finally {
      loading.value = false
    }
  }

  async function loadTask(taskId) {
    loading.value = true
    try {
      taskDetail.value = await getCommissionTask(taskId)
      return taskDetail.value
    } finally {
      loading.value = false
    }
  }

  async function loadMySubmissions(params = {}) {
    loading.value = true
    try {
      const data = await listMyCommissionSubmissions({ page: page.value, pageSize: pageSize.value, ...params })
      mySubmissions.value = data.records || data.list || []
      total.value = Number(data.total) || 0
      return data
    } finally {
      loading.value = false
    }
  }

  async function loadStats() {
    try {
      stats.value = await getCommissionStats()
      return stats.value
    } catch (error) {
      stats.value = { activeTaskCount: 0, mySubmissionCount: 0, earnedCoinTotal: 0 }
      throw error
    }
  }

  async function submitArticle(taskId, articleBizNo) {
    await submitCommissionArticle(taskId, articleBizNo)
    return loadTask(taskId)
  }

  async function withdrawSubmission(submissionId, taskId) {
    await withdrawCommissionSubmission(submissionId)
    return loadTask(taskId)
  }

  return {
    tasks,
    taskDetail,
    mySubmissions,
    stats,
    loading,
    page,
    pageSize,
    total,
    loadTasks,
    loadTask,
    loadMySubmissions,
    loadStats,
    submitArticle,
    withdrawSubmission
  }
}

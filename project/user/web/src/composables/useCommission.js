import { ref } from 'vue'
import {
  getCommissionTask,
  listCommissionTasks,
  listMyCommissionSubmissions,
  submitCommissionArticle,
  withdrawCommissionSubmission
} from '@/api/commission'

const tasks = ref([])
const taskDetail = ref(null)
const mySubmissions = ref([])
const loading = ref(false)

export function useCommission() {
  async function loadTasks(params = {}) {
    loading.value = true
    try {
      const data = await listCommissionTasks(params)
      tasks.value = data.records || data.list || []
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
    const data = await listMyCommissionSubmissions(params)
    mySubmissions.value = data.records || data.list || []
    return data
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
    loading,
    loadTasks,
    loadTask,
    loadMySubmissions,
    submitArticle,
    withdrawSubmission
  }
}

import { ref } from 'vue'
import {
  listArticles,
  getArticle as fetchArticle,
  saveArticle as saveArticleApi,
  updateArticle as updateArticleApi,
  deleteArticle as deleteArticleApi
} from '@/api/article.js'

const articles = ref([])
const total = ref(0)
const loading = ref(false)

function normalizeArticle(raw) {
  return {
    bizNo: raw.bizNo,
    title: raw.title || '未命名作品',
    platform: raw.platform || '',
    platformName: raw.platform || '未选择',
    style: raw.style || '',
    styleName: raw.style || '未选择',
    template: raw.template || '',
    wordCount: raw.wordCount || 0,
    completedAt: raw.completedAt,
    styleOverrides: raw.styleOverrides || null,
    body: raw.body || '',
    raw
  }
}

export function useWorks() {
  const load = async (params = {}) => {
    loading.value = true
    try {
      const data = await listArticles({
        page: 1,
        pageSize: 100,
        ...params
      })
      articles.value = (data.list || []).map(normalizeArticle)
      total.value = data.total || 0
    } finally {
      loading.value = false
    }
  }

  const fetchOne = async (bizNo) => {
    return await fetchArticle(bizNo)
  }

  const save = async (payload) => {
    return await saveArticleApi(payload)
  }

  const update = async (bizNo, payload) => {
    return await updateArticleApi(bizNo, payload)
  }

  const remove = async (bizNo) => {
    await deleteArticleApi(bizNo)
    articles.value = articles.value.filter((item) => item.bizNo !== bizNo)
    total.value = Math.max(0, total.value - 1)
  }

  return {
    articles,
    total,
    loading,
    load,
    fetchOne,
    save,
    update,
    remove
  }
}
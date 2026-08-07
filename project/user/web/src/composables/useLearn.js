import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCategoryTree, fetchCategoryDetail, fetchArticle, fetchBanners, fetchRecommendedArticles } from '@/api/learn'

const ACCESS_TOKEN_KEY = 'aichuangzuo_access_token'

function isLoggedIn() {
  return !!localStorage.getItem(ACCESS_TOKEN_KEY)
}

/**
 * 是否需要展示「付费」徽章。
 * 规则：付费文章 + 当前用户不可读（未登录 / 等级不足）→ 展示。
 * 服务端 isFree / canRead 是单一来源，客户端不做等级推算。
 */
function shouldShowPaidBadge(article) {
  if (!article) return false
  if (article.isFree === 1 || article.isFree == null) return false
  return article.canRead === false
}

export function useLearn(basePath = '/learn') {
  const route = useRoute()
  const router = useRouter()

  const categoryTree = ref([])
  const currentArticle = ref(null)
  const currentCategory = ref(null)
  const banners = ref([])
  const recommendedArticles = ref([])

  const activeCategoryId = computed(() => {
    if (route.params.id) return currentArticle.value?.categoryId ?? null
    return route.query.cat ? Number(route.query.cat) : null
  })

  const currentCategoryName = computed(() => {
    if (!currentArticle.value?.categoryId) return ''
    const targetId = currentArticle.value.categoryId
    const walk = nodes => {
      for (const n of nodes) {
        if (n.id === targetId) return n.name
        if (n.children?.length) {
          const found = walk(n.children)
          if (found) return found
        }
      }
      return ''
    }
    return walk(categoryTree.value)
  })

  const currentCategoryPath = computed(() => {
    const targetId = route.params.id
      ? (currentArticle.value?.categoryId ?? null)
      : (route.query.cat ? Number(route.query.cat) : null)
    if (!targetId) return []
    const result = []
    const walk = (nodes, trail) => {
      for (const n of nodes) {
        const current = [...trail, { id: n.id, name: n.name }]
        if (n.id === targetId) {
          result.push(...current)
          return true
        }
        if (n.children?.length && walk(n.children, current)) return true
      }
      return false
    }
    walk(categoryTree.value, [])
    return result
  })

  const topCategories = computed(() => categoryTree.value.slice(0, 4))
  const isEmptyState = computed(() => !route.params.id && !route.query.cat)

  const onSelectCategory = id => router.replace({ path: basePath, query: { cat: id } })
  const loadArticle = id => router.push(`${basePath}/article/${id}`)
  const goHome = () => router.replace({ path: basePath })

  /**
   * 文章卡片点击：
   * - 免费 / 等级足够 → 进详情
   * - 未登录 + 付费 → 跳登录（带 redirect 回来）
   * - 已登录 + 等级不够 → 进详情（详情页显示锁屏）
   */
  const handleArticleClick = article => {
    if (!article) return
    const free = article.isFree === 1 || article.isFree == null
    const canRead = article.canRead === true
    if (free || canRead) {
      loadArticle(article.id)
      return
    }
    if (!isLoggedIn()) {
      const redirect = encodeURIComponent(route.fullPath)
      router.push(`/login?redirect=${redirect}`)
      return
    }
    loadArticle(article.id)
  }

  async function loadRecommendedArticles() {
    try {
      const res = await fetchRecommendedArticles()
      recommendedArticles.value = res.data || []
    } catch (e) {
      recommendedArticles.value = []
    }
  }
  async function bootstrap() {
    try {
      const tree = await fetchCategoryTree()
      categoryTree.value = tree.data || []
    } catch (e) {
      categoryTree.value = []
    }

    if (route.params.id) {
      try {
        const res = await fetchArticle(route.params.id)
        currentArticle.value = res.data || null
      } catch (e) {
        currentArticle.value = null
      }
      currentCategory.value = null
    } else if (route.query.cat) {
      try {
        const detail = await fetchCategoryDetail(route.query.cat, 1, 50)
        currentCategory.value = detail.data || null
      } catch (e) {
        currentCategory.value = null
      }
      currentArticle.value = null
    } else {
      currentCategory.value = null
      currentArticle.value = null
    }

    if (!route.params.id && !route.query.cat) {
      try {
        const bannerRes = await fetchBanners()
        banners.value = bannerRes.data || []
      } catch (e) {
        banners.value = []
      }
      await loadRecommendedArticles()
    } else {
      recommendedArticles.value = []
    }
  }

  onMounted(bootstrap)
  watch(() => route.fullPath, (newPath, oldPath) => {
    bootstrap()
    if (route.params.id && newPath !== oldPath) {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  })

  return {
    categoryTree,
    currentArticle,
    currentCategory,
    banners,
    activeCategoryId,
    currentCategoryName,
    currentCategoryPath,
    topCategories,
    isEmptyState,
    recommendedArticles,
    onSelectCategory,
    loadArticle,
    goHome,
    handleArticleClick,
    shouldShowPaidBadge
  }
}

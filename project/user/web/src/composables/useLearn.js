import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCategoryTree, fetchCategoryDetail, fetchArticle, fetchBanners, fetchRecommendedArticles } from '@/api/learn'

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
    goHome
  }
}

<template>
  <div class="learn-page">
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <header class="learn-hero">
      <div class="learn-hero-deco learn-hero-deco-lg"></div>
      <div class="learn-hero-deco learn-hero-deco-sm"></div>
      <div class="learn-hero-inner">
        <h1 class="learn-hero-title">创作学院</h1>
        <p class="learn-hero-subtitle">从 0 到 1 的自媒体实战指南</p>
      </div>
    </header>

    <div class="learn-body">
      <aside class="learn-sidebar">
        <LearnSidebar
          v-if="categoryTree.length"
          :nodes="categoryTree"
          :active-id="activeCategoryId"
          @select="onSelectCategory"
        />
        <div v-else class="learn-empty">内容正在筹备中…</div>
      </aside>

      <main class="learn-main">
        <LearnContent
          :article="currentArticle"
          :category="currentCategory"
          :current-category-name="currentCategoryName"
          :category-path="currentCategoryPath"
          :top-categories="topCategories"
          @load-article="loadArticle"
          @select-category="onSelectCategory"
        />
      </main>
    </div>

    <MobileTreeSheet
      v-model:open="mobileSheetOpen"
      :nodes="categoryTree"
      :active-id="activeCategoryId"
      @select="onSelectCategoryFromSheet"
    />

    <button
      v-if="isMobile"
      class="learn-tree-fab"
      @click="mobileSheetOpen = true"
    >分类</button>

    <footer class="learn-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCategoryTree, fetchCategoryDetail, fetchArticle } from '@/api/learn'
import NavBar from '@/components/layout/NavBar.vue'
import LearnSidebar from '@/components/learn/LearnSidebar.vue'
import LearnContent from '@/components/learn/LearnContent.vue'
import MobileTreeSheet from '@/components/learn/MobileTreeSheet.vue'

const route = useRoute()
const router = useRouter()
const categoryTree = ref([])
const currentArticle = ref(null)
const currentCategory = ref(null)
const mobileSheetOpen = ref(false)
const isMobile = ref(window.innerWidth < 992)

const activeCategoryId = computed(() => {
  if (route.params.id) return currentArticle.value?.categoryId ?? null
  return route.query.cat ? Number(route.query.cat) : null
})

// 反查当前文章所属分类的名称，用于跨分类跳转提示
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

// 反查当前分类的完整路径（用于面包屑；文章详情和分类列表通用）
const currentCategoryPath = computed(() => {
  // 文章详情：用文章所属分类；分类列表：用 query.cat
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

// 空状态快捷入口：前 4 个顶级分类
const topCategories = computed(() => categoryTree.value.slice(0, 4))

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'

const onSelectCategory = id => router.replace({ path: '/learn', query: { cat: id } })
const onSelectCategoryFromSheet = id => {
  mobileSheetOpen.value = false
  onSelectCategory(id)
}

const loadArticle = id => router.push(`/learn/article/${id}`)

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
}

window.addEventListener('resize', () => {
  isMobile.value = window.innerWidth < 992
})

onMounted(bootstrap)
watch(() => route.fullPath, (newPath, oldPath) => {
  bootstrap()
  // 仅当切换的是文章（params.id 存在且路径变化）时滚动到顶部，分类切换不动
  if (route.params.id && newPath !== oldPath) {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
})
</script>

<style scoped>
.learn-page { min-height: 100vh; display: flex; flex-direction: column; background: #fafafa; }

/* Hero 区 */
.learn-hero {
  position: relative;
  background: linear-gradient(180deg, #FFF5F7 0%, #FFFFFF 100%);
  padding: 32px 24px;
  overflow: hidden;
}
.learn-hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}
.learn-hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.learn-hero-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 8px 0 0;
}
.learn-hero-deco {
  position: absolute;
  border-radius: 50%;
  background: #FFE8EC;
}
.learn-hero-deco-lg {
  width: 200px; height: 200px;
  top: -60px; right: -40px;
}
.learn-hero-deco-sm {
  width: 80px; height: 80px;
  top: 20px; right: 160px;
}
@media (max-width: 991px) {
  .learn-hero { padding: 20px 16px; }
  .learn-hero-title { font-size: 24px; }
}

.learn-body { display: flex; flex: 1; max-width: 1200px; width: 100%; margin: 0 auto; padding: 24px 16px; gap: 24px; }
.learn-sidebar { width: 240px; flex-shrink: 0; position: sticky; top: 88px; align-self: flex-start; max-height: calc(100vh - 88px); overflow-y: auto; background: #fff; border-radius: 8px; padding: 12px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.learn-main { flex: 1; min-width: 0; background: #fff; border-radius: 8px; padding: 28px 32px; }
.learn-empty { padding: 32px 16px; text-align: center; color: #999; }
.learn-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}
.learn-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}
.learn-tree-fab {
  position: fixed; bottom: 24px; right: 24px;
  background: #FF2442; color: #fff; border: 0; border-radius: 24px;
  padding: 10px 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.15); cursor: pointer; z-index: 50;
  display: none;
}
@media (max-width: 991px) {
  .learn-body { flex-direction: column; padding: 16px; }
  .learn-sidebar { display: none; }
  .learn-tree-fab { display: inline-flex; }
  .learn-main { padding: 20px 16px; }
}

/* 暗色主题（footer 与 Home 对齐） */
body[data-theme="dark"] .learn-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .learn-footer span + span::before {
  color: #303030;
}
</style>

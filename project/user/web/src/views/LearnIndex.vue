<template>
  <div class="learn-page">
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

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
          @load-article="loadArticle"
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
watch(() => route.fullPath, bootstrap)
</script>

<style scoped>
.learn-page { min-height: 100vh; display: flex; flex-direction: column; background: #fafafa; }
.learn-body { display: flex; flex: 1; max-width: 1200px; width: 100%; margin: 0 auto; padding: 24px 16px; gap: 24px; }
.learn-sidebar { width: 240px; flex-shrink: 0; position: sticky; top: 88px; align-self: flex-start; max-height: calc(100vh - 88px); overflow-y: auto; background: #fff; border-radius: 8px; padding: 12px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.learn-main { flex: 1; min-width: 0; background: #fff; border-radius: 8px; padding: 28px 32px; }
.learn-empty { padding: 32px 16px; text-align: center; color: #999; }
.learn-footer { padding: 32px 16px; text-align: center; color: #999; font-size: 13px; display: flex; flex-direction: column; gap: 4px; }
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
</style>

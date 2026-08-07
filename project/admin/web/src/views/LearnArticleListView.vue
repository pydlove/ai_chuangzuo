<template>
  <div class="article-list">
    <a-card :bordered="false">
      <template #title>文章管理</template>
      <template #extra>
        <a-button type="primary" @click="$router.push('/console/learn/article/edit')">新增文章</a-button>
      </template>

      <div class="filter-bar">
        <a-select
          v-model:value="filters.categoryId"
          placeholder="全部分类"
          allow-clear
          style="width: 240px"
          :options="categoryOptions"
        />
        <a-select
          v-model:value="filters.status"
          placeholder="全部状态"
          allow-clear
          style="width: 160px; margin-left: 12px"
          :options="statusOptions"
        />
        <a-input
          v-model:value="filters.keyword"
          placeholder="搜索标题或摘要"
          style="width: 240px; margin-left: 12px"
          @press-enter="onSearch"
        />
        <a-button type="primary" style="margin-left: 12px" @click="onSearch">查询</a-button>
      </div>

      <a-table
        :data-source="rows"
        :columns="columns"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'coverImageUrl'">
            <img
              v-if="record.coverImageUrl"
              :src="record.coverImageUrl"
              style="width: 64px; height: 36px; object-fit: cover; border-radius: 4px; display: block"
            />
            <span v-else style="color: #bfbfbf">—</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'published' ? 'green' : 'orange'">
              {{ record.status === 'published' ? '已发布' : '草稿' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'isRecommended'">
            <a-tag v-if="record.isRecommended === 1" color="red">推荐</a-tag>
            <span v-else style="color: #bfbfbf">—</span>
          </template>
          <template v-else-if="column.key === 'access'">
            <a-tag v-if="isFreeArticle(record)" color="green">免费</a-tag>
            <a-tag v-else color="orange">{{ planLabel(record.requiredPlanKey) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'contentType'">
            <a-tag>{{ record.contentType === 'markdown' ? 'Markdown' : '富文本' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a @click="$router.push(`/console/learn/article/edit/${record.id}`)">编辑</a>
            <a-divider type="vertical" />
            <a @click="onRecommend(record)">{{ record.isRecommended === 1 ? '取消推荐' : '推荐' }}</a>
            <a-divider type="vertical" />
            <a v-if="record.status !== 'published'" @click="onPublish(record)">发布</a>
            <a v-else @click="onUnpublish(record)">下线</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除？" @confirm="onDelete(record)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  fetchArticlePage,
  publishArticle,
  unpublishArticle,
  deleteArticle,
  recommendArticle,
  fetchCategoryTree
} from '@/api/learn'

const rows = ref([])
const loading = ref(false)
const categories = ref([])

const filters = reactive({ categoryId: null, status: null, keyword: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' }
]

const flatCategories = computed(() => flatten(categories.value))

const categoryOptions = computed(() =>
  flatCategories.value.map((c) => ({ label: '— '.repeat(c.depth) + c.name, value: c.id }))
)

const categoryNameMap = computed(() => {
  const m = {}
  for (const c of flatCategories.value) m[c.id] = c.name
  return m
})

function flatten(tree, depth = 0, acc = []) {
  for (const n of tree) {
    acc.push({ id: n.id, name: n.name, depth })
    flatten(n.children || [], depth + 1, acc)
  }
  return acc
}

const columns = [
  { title: '封面', key: 'coverImageUrl', width: 80 },
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '分类', key: 'category', customRender: ({ record }) => categoryNameMap.value[record.categoryId] || record.categoryId },
  { title: '类型', key: 'contentType', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '访问', key: 'access', width: 90 },
  { title: '推荐', key: 'isRecommended', width: 80 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'actions', width: 240, fixed: 'right' }
]

const PLAN_LABELS = { basic: '基础版', pro: '专业版', flagship: '旗舰版' }

function isFreeArticle(record) {
  return record.isFree == null || record.isFree === 1
}

function planLabel(key) {
  if (!key) return '付费'
  return PLAN_LABELS[key] || key
}

async function load() {
  loading.value = true
  try {
    const page = await fetchArticlePage({
      categoryId: filters.categoryId,
      status: filters.status,
      keyword: filters.keyword,
      page: pagination.current,
      size: pagination.pageSize
    })
    rows.value = page?.records || []
    pagination.total = page?.total || 0
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categories.value = await fetchCategoryTree()
}

function onSearch() { pagination.current = 1; load() }
function onTableChange(pag) { pagination.current = pag.current; pagination.pageSize = pag.pageSize; load() }

async function onPublish(r) {
  try { await publishArticle(r.id); message.success('已发布'); load() }
  catch (e) { message.error(e?.message || '发布失败') }
}
async function onUnpublish(r) {
  try { await unpublishArticle(r.id); message.success('已下线'); load() }
  catch (e) { message.error(e?.message || '下线失败') }
}
async function onDelete(r) {
  try { await deleteArticle(r.id); message.success('已删除'); load() }
  catch (e) { message.error(e?.message || '删除失败') }
}
async function onRecommend(r) {
  try {
    const next = r.isRecommended === 1 ? 0 : 1
    await recommendArticle(r.id, next)
    message.success(next === 1 ? '已推荐' : '已取消推荐')
    load()
  } catch (e) {
    message.error(e?.message || '操作失败')
  }
}

onMounted(() => { loadCategories(); load() })
</script>

<style scoped>
.article-list { padding: 0; }
.filter-bar { display: flex; margin-bottom: 12px; flex-wrap: wrap; align-items: center; }
.danger { color: #ff4d4f; }
</style>

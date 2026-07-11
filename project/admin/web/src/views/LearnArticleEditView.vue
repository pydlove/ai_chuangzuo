<template>
  <div class="article-editor">
    <a-card :bordered="false">
      <template #title>
        <a @click="$router.push('/console/learn/article')" style="margin-right: 12px">← 返回列表</a>
        {{ isEdit ? '编辑文章' : '新增文章' }}
      </template>
      <a-form layout="vertical" :model="form" ref="formRef">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" maxlength="128" />
        </a-form-item>
        <a-form-item label="分类" required>
          <a-tree-select
            v-model:value="form.categoryId"
            :tree-data="categories"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            :tree-default-expand-all="true"
            placeholder="选择分类"
          />
        </a-form-item>
        <a-form-item label="摘要">
          <a-textarea v-model:value="form.summary" :rows="2" maxlength="255" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" />
          <span style="margin-left: 16px">
            <a-checkbox v-model:checked="updatePublishedAt">编辑已发布文章时刷新发布时间</a-checkbox>
          </span>
        </a-form-item>
        <a-form-item label="正文类型">
          <a-radio-group v-model:value="form.contentType" :disabled="contentTypeLocked">
            <a-radio value="markdown">Markdown</a-radio>
            <a-radio value="rich_text">富文本</a-radio>
          </a-radio-group>
          <span v-if="contentTypeLocked" style="margin-left: 12px; color: #999; font-size: 12px">已发布文章不允许切换正文类型</span>
        </a-form-item>
        <a-form-item label="正文" required>
          <MarkdownEditor
            v-if="form.contentType === 'markdown'"
            v-model:value="form.content"
          />
          <RichTextEditor
            v-else
            v-model:html="form.content"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button @click="onSave('draft')">保存草稿</a-button>
            <a-button type="primary" :loading="saving" @click="onSave('published')">保存并发布</a-button>
            <a-button @click="$router.push('/console/learn/article')">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  fetchCategoryTree,
  fetchArticle,
  createArticle,
  updateArticle
} from '@/api/learn'
import MarkdownEditor from '@/components/learn/MarkdownEditor.vue'
import RichTextEditor from '@/components/learn/RichTextEditor.vue'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const form = reactive({
  categoryId: null,
  title: '',
  summary: '',
  sort: 0,
  contentType: 'markdown',
  content: ''
})
const dirty = ref(false)
const saving = ref(false)
const updatePublishedAt = ref(false)
const originalStatus = ref(null)
const switching = ref(false)

const contentTypeLocked = computed(() => isEdit.value && originalStatus.value === 'published')

const formRef = ref(null)

async function load() {
  categories.value = await fetchCategoryTree()
  if (route.params.id) {
    const a = await fetchArticle(route.params.id)
    Object.assign(form, {
      categoryId: a.categoryId,
      title: a.title,
      summary: a.summary,
      sort: a.sort,
      contentType: a.contentType,
      content: a.content
    })
    originalStatus.value = a.status
  }
}

watch(form, () => {
  if (!switching.value) dirty.value = true
}, { deep: true })

// Task 17: 草稿态切换正文类型时二次确认（会清空正文）
watch(() => form.contentType, (newType, oldType) => {
  if (oldType === undefined || newType === oldType) return
  if (originalStatus.value === 'published') return
  if (!form.content) return
  switching.value = true
  Modal.confirm({
    title: '切换正文类型',
    content: '切换会清空当前正文内容，是否继续？',
    okText: '继续切换',
    cancelText: '取消',
    onOk: () => { form.content = ''; switching.value = false },
    onCancel: () => { form.contentType = oldType; switching.value = false }
  })
})

function buildPayload(target) {
  return {
    categoryId: form.categoryId,
    title: form.title,
    summary: form.summary,
    sort: form.sort,
    contentType: form.contentType,
    content: form.content,
    status: target
  }
}

async function onSave(target) {
  if (!form.title?.trim()) { message.error('标题不能为空'); return }
  if (!form.categoryId) { message.error('请选择分类'); return }
  if (!form.content?.trim()) { message.error('正文不能为空'); return }
  saving.value = true
  try {
    if (!isEdit.value) {
      const newId = await createArticle(buildPayload(target))
      message.success(target === 'published' ? '已发布' : '已存草稿')
      dirty.value = false
      router.replace(`/console/learn/article/edit/${newId}`)
    } else {
      await updateArticle(route.params.id, buildPayload(target))
      message.success('已更新')
      dirty.value = false
    }
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onBeforeRouteLeave(() => {
  if (!dirty.value) return true
  return new Promise((resolve) => {
    Modal.confirm({
      title: '内容已修改，是否保存为草稿？',
      okText: '保存草稿',
      cancelText: '丢弃修改',
      onOk: async () => { await onSave('draft'); resolve(true) },
      onCancel: () => resolve(true)
    })
  })
})

onMounted(load)
</script>

<style scoped>
.article-editor { padding: 0; }
</style>

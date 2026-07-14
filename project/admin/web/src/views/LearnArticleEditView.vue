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
        <a-form-item label="封面图链接">
          <a-input v-model:value="form.coverImageUrl" maxlength="512" placeholder="https://... 图片 URL" />
          <img
            v-if="form.coverImageUrl"
            :src="form.coverImageUrl"
            class="cover-preview"
            @error="coverImgError = true"
            @load="coverImgError = false"
          />
          <div v-if="coverImgError" style="color: #ff4d4f; font-size: 12px; margin-top: 4px">图片加载失败，请检查链接</div>
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
          <div style="margin-bottom: 8px">
            <a-space>
              <a-button size="small" :loading="importing" @click="onImportMarkdown">
                导入 Markdown
              </a-button>
              <a-button size="small" :loading="importing" @click="onImportWord">
                导入 Word (.docx)
              </a-button>
              <span style="color: #999; font-size: 12px">
                导入会替换当前正文；Word 会按当前正文类型自动转成 Markdown 或富文本
              </span>
            </a-space>
          </div>
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
import mammoth from 'mammoth'
import TurndownService from 'turndown'
import {
  fetchCategoryTree,
  fetchArticle,
  createArticle,
  updateArticle
} from '@/api/learn'
import MarkdownEditor from '@/components/learn/MarkdownEditor.vue'
import RichTextEditor from '@/components/learn/RichTextEditor.vue'

const turndownService = new TurndownService({
  headingStyle: 'atx',
  codeBlockStyle: 'fenced',
  bulletListMarker: '-'
})

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const form = reactive({
  categoryId: null,
  title: '',
  summary: '',
  coverImageUrl: '',
  sort: 0,
  contentType: 'markdown',
  content: ''
})
const dirty = ref(false)
const saving = ref(false)
const importing = ref(false)
const updatePublishedAt = ref(false)
const coverImgError = ref(false)
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
      coverImageUrl: a.coverImageUrl || '',
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
    coverImageUrl: form.coverImageUrl,
    sort: form.sort,
    contentType: form.contentType,
    content: form.content,
    status: target
  }
}

// ---------- 导入：Markdown / Word ----------

function pickFile(accept) {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = accept
    input.onchange = (e) => resolve(e.target.files?.[0] || null)
    input.click()
  })
}

function confirmOverwrite() {
  if (!form.content?.trim()) return Promise.resolve(true)
  return new Promise((resolve) => {
    Modal.confirm({
      title: '导入会替换当前正文',
      content: '当前正文已有内容，导入后将被覆盖，是否继续？',
      okText: '继续导入',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false)
    })
  })
}

// 从 markdown 提取第一个 # 一级标题作为文章标题
function extractMdTitle(text) {
  const m = text.match(/^#\s+(.+?)\s*$/m)
  return m ? m[1].trim() : ''
}

// 从 HTML 提取第一个 <h1> 作为文章标题（去标签）
function extractHtmlTitle(html) {
  const m = html.match(/<h1[^>]*>([\s\S]+?)<\/h1>/i)
  return m ? m[1].replace(/<[^>]+>/g, '').trim() : ''
}

async function onImportMarkdown() {
  const file = await pickFile('.md,.markdown,text/markdown,text/plain')
  if (!file) return
  const ok = await confirmOverwrite()
  if (!ok) return
  importing.value = true
  try {
    const text = await file.text()
    if (form.contentType !== 'markdown') {
      form.contentType = 'markdown'
    }
    form.content = text
    if (!form.title?.trim()) {
      form.title = extractMdTitle(text) || file.name.replace(/\.(md|markdown|txt)$/i, '')
    }
    dirty.value = true
    message.success(`已导入 Markdown（${text.length} 字符）`)
  } catch (e) {
    message.error('Markdown 读取失败：' + (e?.message || '未知错误'))
  } finally {
    importing.value = false
  }
}

async function onImportWord() {
  const file = await pickFile('.docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document')
  if (!file) return
  const ok = await confirmOverwrite()
  if (!ok) return
  importing.value = true
  try {
    const arrayBuffer = await file.arrayBuffer()
    const result = await mammoth.convertToHtml({ arrayBuffer })
    const html = result.value || ''
    const warnings = (result.messages || []).filter(m => m.type === 'warning')
    if (!html.trim()) {
      message.warning('Word 文档没有可识别的文本内容')
      return
    }
    if (form.contentType === 'markdown') {
      form.content = turndownService.turndown(html)
    } else {
      form.content = html
    }
    if (!form.title?.trim()) {
      form.title = extractHtmlTitle(html) || file.name.replace(/\.docx?$/i, '')
    }
    dirty.value = true
    if (warnings.length) {
      message.warning(`已导入，但 ${warnings.length} 处格式被简化（如复杂表格/嵌套样式）`)
    } else {
      message.success('已导入 Word 文档')
    }
  } catch (e) {
    message.error('Word 解析失败：' + (e?.message || '请确认是 .docx 格式'))
  } finally {
    importing.value = false
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
      if (target === 'published') {
        // 发布成功 → 返回列表页，让用户立刻看到新文章
        router.replace('/console/learn/article')
      } else {
        // 草稿 → 留在编辑页（URL 换成 edit 路由，后续保存走 update 而不是 create）
        router.replace(`/console/learn/article/edit/${newId}`)
      }
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
.cover-preview { margin-top: 8px; max-width: 240px; max-height: 135px; border-radius: 8px; display: block; }
</style>

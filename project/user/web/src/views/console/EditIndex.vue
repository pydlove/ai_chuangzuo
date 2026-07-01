<template>
  <div class="edit-index">
    <div class="edit-header">
      <button class="back-btn" @click="$router.push('/console/preview')">
        ← 返回预览
      </button>
      <h2 class="edit-title">编辑内容</h2>
    </div>

    <div v-if="!blocks.length" class="edit-empty">
      <a-empty description="暂无文章内容">
        <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
      </a-empty>
    </div>

    <div v-else class="edit-blocks">
      <div v-for="(block, idx) in blocks" :key="idx" class="edit-block">
        <div class="edit-block-label">{{ getBlockTypeLabel(block.type) }} #{{ idx + 1 }}</div>
        <div
          :class="['edit-block-area', block.type]"
          contenteditable="true"
          @paste="onPaste"
          @input="onInput(idx, $event)"
          v-html="block.html"
        />
      </div>
    </div>

    <div class="edit-actions">
      <button class="cancel" @click="cancel">取消</button>
      <button class="save" @click="save">保存修改</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { loadCurrentArticle, saveCurrentArticle, syncArticleToQueue } from '@/utils/articleStorage.js'
import { parseBodyToBlocks, serializeBlocksToArticle, getBlockTypeLabel } from '@/utils/articleBlocks.js'

const router = useRouter()
const blocks = ref([])
const originalArticle = ref(null)

onMounted(() => {
  const article = loadCurrentArticle()
  if (!article) return
  originalArticle.value = JSON.parse(JSON.stringify(article))
  blocks.value = parseBodyToBlocks(article.title, article.body)
})

const onPaste = (e) => {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

const onInput = (idx, e) => {
  blocks.value[idx].html = e.target.innerHTML
}

const save = () => {
  const titleBlock = blocks.value.find(b => b.type === 'title')
  if (!titleBlock || !stripHtml(titleBlock.html).trim()) {
    message.error('标题不能为空')
    return
  }

  const { title, body } = serializeBlocksToArticle(blocks.value)
  const article = {
    ...originalArticle.value,
    title,
    body
  }

  if (!saveCurrentArticle(article)) {
    message.error('保存失败，请检查浏览器存储权限')
    return
  }

  syncArticleToQueue(article)
  message.success('内容已保存')
  router.push('/console/preview')
}

const cancel = () => {
  router.push('/console/preview')
}

function stripHtml(html) {
  if (!html) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}
</script>

<style scoped>
.edit-index {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
}

.edit-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-btn {
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.edit-title {
  font-size: 20px;
  margin: 0;
  color: #1a1a1a;
}

.edit-empty {
  padding: 60px 0;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.edit-blocks {
  padding-bottom: 100px;
}

.edit-block {
  margin-bottom: 16px;
}

.edit-block-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.edit-block-area {
  width: 100%;
  min-height: 60px;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.7;
  color: #262626;
  box-sizing: border-box;
  outline: none;
}

.edit-block-area:focus {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.15);
}

.edit-block-area.title {
  font-size: 22px;
  font-weight: 700;
}

.edit-block-area.heading {
  font-size: 18px;
  font-weight: 600;
}

.edit-block-area.highlight {
  background: #f6ffed;
  border-left: 4px solid #07c160;
}

.edit-actions {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 10003;
}

.edit-actions button {
  padding: 8px 18px;
  border-radius: 18px;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.edit-actions .cancel {
  background: #f5f5f5;
  color: #595959;
}

.edit-actions .save {
  background: #07c160;
  color: #fff;
}
</style>

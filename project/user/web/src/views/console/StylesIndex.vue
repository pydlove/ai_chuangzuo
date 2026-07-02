<template>
  <div class="styles-index">
    <div class="styles-header">
      <div>
        <h2 class="styles-title">我的风格</h2>
        <p class="styles-subtitle">提前设计你的专属写作风格，创作时一键选用</p>
      </div>
    </div>

    <div class="styles-tabs">
      <button
        :class="['styles-tab', { active: activeTab === 'my' }]"
        @click="activeTab = 'my'; editorMode = false"
      >
        我的风格
      </button>
      <button
        :class="['styles-tab', { active: activeTab === 'system' }]"
        @click="activeTab = 'system'; editorMode = false"
      >
        系统预设
      </button>
      <button
        :class="['styles-tab', { active: activeTab === 'learned' }]"
        @click="activeTab = 'learned'; editorMode = false"
      >
        学习的风格
      </button>
    </div>

    <!-- 我的风格 -->
    <div v-show="activeTab === 'my'" class="styles-content">
      <div v-if="editorMode" class="style-editor">
        <div class="style-editor-header">
          <button class="style-editor-back" @click="goBack">← 返回</button>
          <div class="style-editor-title">{{ editingStyle.originalName ? '编辑提示词' : '新建我的风格' }}</div>
        </div>
        <div class="style-editor-form">
          <div class="style-editor-field">
            <label class="style-editor-label">风格名称 <span class="required">*</span></label>
            <input
              v-model="editingStyle.name"
              type="text"
              class="style-editor-input"
              placeholder="例如：我的小红书风"
              maxlength="20"
            />
            <div v-if="errors.name" class="style-editor-error">{{ errors.name }}</div>
          </div>
          <div class="style-editor-field">
            <label class="style-editor-label">风格提示词 <span class="required">*</span></label>
            <textarea
              v-model="editingStyle.prompt"
              class="style-editor-textarea"
              placeholder="描述你希望 AI 采用的语气、结构、用词习惯等..."
              rows="5"
            ></textarea>
            <div class="style-editor-counter" :class="{ over: editingStyle.prompt.length > 1000 }">
              {{ editingStyle.prompt.length }} / 1000
            </div>
            <div v-if="errors.prompt" class="style-editor-error">{{ errors.prompt }}</div>
          </div>
          <div class="style-editor-presets">
            <div class="style-editor-preset-label">快速填充模板：</div>
            <div class="style-editor-preset-list">
              <div
                v-for="preset in systemStyles"
                :key="preset.name"
                class="style-preset-card"
                @click="editingStyle.prompt = preset.prompt"
              >
                <div class="style-preset-title">{{ preset.name }}</div>
                <div class="style-preset-desc">{{ preset.desc }}</div>
              </div>
            </div>
          </div>
          <button
            class="save-style-btn"
            :disabled="!isFormValid"
            @click="saveStyle"
          >
            保存
          </button>
        </div>
      </div>

      <div v-else>
        <div v-if="myStyles.length === 0" class="styles-empty">
          <a-empty description="还没有自定义风格">
            <button class="empty-btn" @click="goToCreate">去创建一个</button>
          </a-empty>
        </div>
        <div v-else class="styles-grid">
          <div class="style-add-card" @click="goToCreate">
            <div class="style-add-icon">+</div>
            <div class="style-add-text">新建我的风格</div>
          </div>
          <div
            v-for="s in myStyles"
            :key="s.name"
            class="style-card"
          >
            <div class="style-card-title">{{ s.name }}</div>
            <div class="style-card-desc">{{ s.desc }} · 已用 {{ s.count }} 次</div>
            <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
            <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
            <div class="style-card-actions">
              <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
              <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
                {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
              </button>
              <button class="style-action-btn" @click.stop="goToEdit(s)">编辑</button>
              <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(s.name)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统预设 -->
    <div v-show="activeTab === 'system'" class="styles-content">
      <div class="styles-grid">
        <div
          v-for="s in systemStyles"
          :key="s.name"
          class="style-card"
        >
          <div class="style-card-title">{{ s.name }}</div>
          <div class="style-card-desc">{{ s.desc }}</div>
          <div class="style-card-prompt">{{ s.promptSummary }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-actions">
            <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
            <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
              {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 学习的风格 -->
    <div v-show="activeTab === 'learned'" class="styles-content">
      <div class="learned-banner">
        上传或粘贴一篇文章，AI 会分析它的写作风格并保存为「我的风格」
      </div>
      <div class="learned-toolbar">
        <button class="learned-add-btn" @click="openImportDialog">+ 学习新风格</button>
      </div>
      <div v-if="learnedStyles.length === 0" class="learned-empty">
        还没有学习过的风格。点击上方按钮开始学习。
      </div>
      <div v-else class="styles-grid">
        <div
          v-for="s in learnedStyles"
          :key="s.name"
          class="style-card"
        >
          <div class="style-card-title">{{ s.name }}</div>
          <div class="style-card-source">
            来源：{{ s.sourceName }} · {{ s.sourceType.toUpperCase() }} · {{ s.createdAt.slice(0, 10) }}
          </div>
          <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-actions">
            <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
            <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
              {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
            </button>
            <button class="style-action-btn style-del-btn" @click.stop="deleteLearnedStyle(s.name)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 学习风格导入对话框 -->
  <a-modal
    :open="importDialogVisible"
    :footer="null"
    :width="640"
    centered
    @cancel="closeImportDialog"
  >
    <template #title>
      <div class="modal-title">学习写作风格</div>
    </template>

    <!-- 进度态 -->
    <div v-if="isLearning" class="learned-progress">
      <a-spin />
      <div class="learned-progress-text">● ● ● 分析中…</div>
    </div>

    <!-- 粘贴 / 上传 tab -->
    <template v-else-if="!learnedResult">
      <div class="learned-subtabs">
        <button
          :class="['learned-subtab', { active: importSubTab === 'paste' }]"
          @click="importSubTab = 'paste'"
        >粘贴正文</button>
        <button
          :class="['learned-subtab', { active: importSubTab === 'upload' }]"
          @click="importSubTab = 'upload'"
        >上传文件</button>
      </div>

      <!-- 粘贴 -->
      <div v-show="importSubTab === 'paste'" class="learned-pane">
        <textarea
          v-model="pasteText"
          class="learned-textarea"
          placeholder="将原文粘贴到这里…"
          maxlength="50000"
        ></textarea>
        <div class="learned-counter">{{ pasteText.length }} / 50000</div>
        <input
          v-model="pasteSourceName"
          type="text"
          class="learned-input"
          placeholder="来源标题（可选）"
          maxlength="50"
        />
        <div v-if="pasteError" class="learned-error">{{ pasteError }}</div>
        <button
          class="learned-submit-btn"
          :disabled="pasteText.trim().length < 200"
          @click="submitPaste"
        >开始学习</button>
      </div>

      <!-- 上传 -->
      <div v-show="importSubTab === 'upload'" class="learned-pane">
        <label class="learned-upload-zone">
          <input
            type="file"
            accept=".txt,.md,.docx"
            @change="onFileChange"
            style="display: none;"
          />
          <div v-if="!uploadFile" class="learned-upload-hint">
            点击选择文件或拖拽到此处<br/>
            <span class="learned-upload-types">支持 .txt / .md / .docx（最大 5MB）</span>
          </div>
          <div v-else class="learned-upload-info">
            ✓ {{ uploadFile.name }} ({{ Math.round(uploadFile.size / 1024) }} KB)
          </div>
        </label>
        <div v-if="uploadError" class="learned-error">{{ uploadError }}</div>
        <button
          class="learned-submit-btn"
          :disabled="!uploadFile"
          @click="submitUpload"
        >开始学习</button>
      </div>
    </template>

    <!-- 结果页 -->
    <div v-else>
      <div class="learned-result-title">学习结果 ✓ 已从「{{ learnedResult.sourceName }}」中提取风格</div>
      <div class="learned-result-field">
        <label class="learned-result-label">学到的提示词（可编辑）</label>
        <textarea
          v-model="learnedResult.prompt"
          class="learned-textarea"
          maxlength="1000"
        ></textarea>
        <div class="learned-counter" :class="{ over: learnedResult.prompt.length > 1000 }">
          {{ learnedResult.prompt.length }} / 1000
        </div>
      </div>
      <div class="learned-result-field">
        <label class="learned-result-label">原文风格示例</label>
        <div class="learned-excerpt">① {{ learnedResult.excerpt1 }}</div>
        <div class="learned-excerpt">② {{ learnedResult.excerpt2 }}</div>
      </div>
      <div class="learned-result-field">
        <label class="learned-result-label">命名 <span class="required">*</span></label>
        <input
          v-model="learnedResult.name"
          type="text"
          class="learned-input"
          placeholder="例如：我的小红书风"
          maxlength="20"
        />
        <div v-if="learnedResultError" class="learned-error">{{ learnedResultError }}</div>
      </div>
      <div class="learned-result-actions">
        <button class="learned-cancel-btn" @click="closeImportDialog">放弃</button>
        <button
          class="learned-submit-btn"
          :disabled="!canSaveLearnedResult"
          @click="saveLearnedResult"
        >保存到风格库</button>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  systemStyles,
  myStyles,
  applyStyle,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle,
  isStyleNameExists,
  learnedStyles,
  removeLearnedStyle,
  analyzeArticleStyle,
  isLearnedStyleNameExists,
  findLearnedStyleByHash,
  addLearnedStyle,
  isLearning,
  readFileAsText,
  readDocxAsText
} from '@/composables/useStyles.js'

const router = useRouter()
const activeTab = ref('my')

// 导入对话框状态
const importDialogVisible = ref(false)
const importSubTab = ref('paste')
const pasteText = ref('')
const pasteSourceName = ref('')
const pasteError = ref('')
const uploadFile = ref(null)
const uploadError = ref('')
const learnedResult = ref(null)
const learnedResultError = ref('')
const editorMode = ref(false)
const expandedNames = ref(new Set())

const editingStyle = reactive({
  originalName: '',
  name: '',
  prompt: ''
})

const errors = reactive({
  name: '',
  prompt: ''
})

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const togglePrompt = (name) => {
  const set = new Set(expandedNames.value)
  if (set.has(name)) {
    set.delete(name)
  } else {
    set.add(name)
  }
  expandedNames.value = set
}

const validate = () => {
  errors.name = ''
  errors.prompt = ''

  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  let valid = true

  if (!name) {
    errors.name = '请输入风格名称'
    valid = false
  } else if (name.length > 20) {
    errors.name = '风格名称最多 20 字'
    valid = false
  } else if (isStyleNameExists(name, editingStyle.originalName)) {
    errors.name = '该风格名称已存在'
    valid = false
  }

  if (!prompt) {
    errors.prompt = '请输入风格提示词'
    valid = false
  } else if (prompt.length > 1000) {
    errors.prompt = '风格提示词最多 1000 字'
    valid = false
  }

  return valid
}

const isFormValid = computed(() => {
  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  return name && name.length <= 20 && prompt && prompt.length <= 1000 && !isStyleNameExists(name, editingStyle.originalName)
})

const goToCreate = () => {
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.prompt = ''
  errors.name = ''
  errors.prompt = ''
  editorMode.value = true
}

const goToEdit = (style) => {
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.prompt = style.prompt
  errors.name = ''
  errors.prompt = ''
  editorMode.value = true
}

const goBack = () => {
  editorMode.value = false
}

const saveStyle = () => {
  if (!validate()) return
  if (editingStyle.originalName) {
    updateCustomStyle(editingStyle.originalName, {
      name: editingStyle.name,
      prompt: editingStyle.prompt
    })
  } else {
    addCustomStyle({
      name: editingStyle.name,
      prompt: editingStyle.prompt
    })
  }
  editorMode.value = false
}

const useStyle = (style) => {
  applyStyle(style)
  router.push('/console/create')
}

const deleteStyle = (name) => {
  removeCustomStyle(name)
}

const openImportDialog = () => {
  pasteText.value = ''
  pasteSourceName.value = ''
  pasteError.value = ''
  uploadFile.value = null
  uploadError.value = ''
  learnedResult.value = null
  learnedResultError.value = ''
  importSubTab.value = 'paste'
  importDialogVisible.value = true
}

const closeImportDialog = () => {
  importDialogVisible.value = false
}

const onFileChange = (e) => {
  uploadError.value = ''
  const file = e.target.files?.[0]
  if (!file) {
    uploadFile.value = null
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    uploadError.value = '文件过大（> 5MB）'
    uploadFile.value = null
    return
  }
  const ext = file.name.split('.').pop().toLowerCase()
  if (!['txt', 'md', 'docx'].includes(ext)) {
    uploadError.value = '仅支持 .txt / .md / .docx'
    uploadFile.value = null
    return
  }
  uploadFile.value = file
}

const submitPaste = async () => {
  pasteError.value = ''
  const text = pasteText.value.trim()
  if (text.length < 200) {
    pasteError.value = '正文过短（少于 200 字）'
    return
  }
  await runAnalysis(text, pasteSourceName.value.trim() || '粘贴的参考文章', 'paste')
}

const submitUpload = async () => {
  uploadError.value = ''
  if (!uploadFile.value) return
  try {
    const ext = uploadFile.value.name.split('.').pop().toLowerCase()
    let text
    if (ext === 'docx') {
      text = await readDocxAsText(uploadFile.value)
    } else {
      text = await readFileAsText(uploadFile.value)
    }
    if (text.trim().length < 200) {
      uploadError.value = '正文过短（少于 200 字）'
      return
    }
    await runAnalysis(text, uploadFile.value.name, ext)
  } catch (err) {
    uploadError.value = err.message || '文件读取失败'
  }
}

const runAnalysis = async (text, sourceName, sourceType) => {
  const tempResult = await analyzeArticleStyle(text, { sourceName, sourceType })
  const dup = findLearnedStyleByHash(tempResult.fileHash)
  if (dup) {
    if (sourceType === 'paste') {
      pasteError.value = '已学过这篇文章（命名：「' + dup.name + '」）'
    } else {
      uploadError.value = '已学过这篇文章（命名：「' + dup.name + '」）'
    }
    return
  }
  learnedResult.value = { ...tempResult, name: '' }
}

const canSaveLearnedResult = computed(() => {
  if (!learnedResult.value) return false
  const name = learnedResult.value.name.trim()
  if (!name || name.length > 20) return false
  if (learnedResult.value.prompt.length > 1000) return false
  if (isStyleNameExists(name) || isLearnedStyleNameExists(name)) return false
  return true
})

const saveLearnedResult = () => {
  if (!learnedResult.value) return
  const name = learnedResult.value.name.trim()
  if (isStyleNameExists(name) || isLearnedStyleNameExists(name)) {
    learnedResultError.value = '该风格名称已存在'
    return
  }
  if (name.length > 20) {
    learnedResultError.value = '风格名称最多 20 字'
    return
  }
  if (learnedResult.value.prompt.length > 1000) {
    learnedResultError.value = '提示词超过 1000 字'
    return
  }
  addLearnedStyle(learnedResult.value)
  closeImportDialog()
}

const deleteLearnedStyle = (name) => {
  if (!confirm('确定要删除「' + name + '」吗？')) return
  removeLearnedStyle(name)
}
</script>

<style scoped>
.styles-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.styles-header {
  margin-bottom: 20px;
}

.styles-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.styles-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.styles-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  margin-bottom: 20px;
  width: fit-content;
}

.styles-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.styles-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.styles-empty {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  margin-top: 12px;
}

.empty-btn:hover {
  background: #e61e3a;
}

.styles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.style-add-card {
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 160px;
}

.style-add-card:hover {
  border-color: #07c160;
  background: #f6ffed;
}

.style-add-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.style-add-text {
  font-size: 14px;
  color: #595959;
}

.style-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}

.style-card:hover {
  border-color: #ffd1d9;
  box-shadow: 0 2px 12px rgba(255, 36, 66, 0.08);
}

.style-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.style-card-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 10px;
}

.style-card-prompt {
  font-size: 12px;
  color: #bfbfbf;
  line-height: 1.5;
  margin-bottom: 12px;
  flex: 1;
  white-space: pre-line;
}

.style-prompt-full {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  background: #fafafa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  white-space: pre-line;
}

.style-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.style-action-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.style-action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.style-action-btn.style-del-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.style-editor {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
  max-width: 720px;
}

.style-editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.style-editor-back {
  background: none;
  border: none;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px 4px 0;
}

.style-editor-back:hover {
  color: #07c160;
}

.style-editor-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-editor-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.style-editor-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.style-editor-label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.style-editor-label .required {
  color: #ff4d4f;
  margin-left: 2px;
}

.style-editor-input,
.style-editor-textarea {
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.style-editor-input:focus,
.style-editor-textarea:focus {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.1);
}

.style-editor-error {
  color: #ff4d4f;
  font-size: 12px;
}

.style-editor-counter {
  text-align: right;
  font-size: 12px;
  color: #8c8c8c;
}

.style-editor-counter.over {
  color: #ff4d4f;
}

.style-editor-presets {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.style-editor-preset-label {
  font-size: 13px;
  color: #595959;
}

.style-editor-preset-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.style-preset-card {
  flex: 0 0 160px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.style-preset-card:hover {
  border-color: #07c160;
  background: #f6ffed;
}

.style-preset-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.style-preset-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.save-style-btn {
  padding: 10px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  width: fit-content;
}

.save-style-btn:hover {
  background: #e61e3a;
}

.save-style-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.learned-banner {
  padding: 12px 16px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  font-size: 13px;
  color: #389e0d;
  margin-bottom: 16px;
}

.learned-toolbar {
  margin-bottom: 16px;
}

.learned-add-btn {
  padding: 8px 16px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.learned-add-btn:hover {
  background: #06a050;
}

.learned-empty {
  padding: 60px 20px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
}

.style-card-source {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.learned-subtabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  margin-bottom: 16px;
  width: fit-content;
}

.learned-subtab {
  padding: 6px 14px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.learned-subtab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.learned-pane {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.learned-textarea {
  width: 100%;
  min-height: 200px;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.learned-textarea:focus {
  outline: none;
  border-color: #07c160;
}

.learned-counter {
  text-align: right;
  font-size: 12px;
  color: #8c8c8c;
}

.learned-counter.over {
  color: #ff4d4f;
}

.learned-input {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
}

.learned-input:focus {
  outline: none;
  border-color: #07c160;
}

.learned-upload-zone {
  display: block;
  padding: 40px 20px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.learned-upload-zone:hover {
  border-color: #07c160;
  background: #f6ffed;
}

.learned-upload-hint {
  font-size: 14px;
  color: #595959;
}

.learned-upload-types {
  font-size: 12px;
  color: #8c8c8c;
}

.learned-upload-info {
  font-size: 14px;
  color: #07c160;
}

.learned-error {
  color: #ff4d4f;
  font-size: 13px;
}

.learned-submit-btn {
  padding: 10px 20px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.learned-submit-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.learned-progress {
  text-align: center;
  padding: 40px 0;
}

.learned-progress-text {
  margin-top: 12px;
  font-size: 14px;
  color: #595959;
}

.learned-result-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
}

.learned-result-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.learned-result-label {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}

.learned-result-label .required {
  color: #ff4d4f;
}

.learned-excerpt {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin-bottom: 6px;
}

.learned-result-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.learned-cancel-btn {
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
</style>

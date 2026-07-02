<template>
  <div class="styles-index">
    <div class="styles-header">
      <div>
        <h2 class="styles-title">我的风格</h2>
        <p class="styles-subtitle">提前设计你的专属写作风格，创作时一键选用</p>
      </div>
    </div>

    <div class="styles-filter-bar">
      <div class="styles-tabs">
        <button
          :class="['styles-tab', { active: activeTab === 'my' }]"
          @click="activeTab = 'my'; editorMode = false"
        >
          我的风格
        </button>
        <button
          :class="['styles-tab', { active: activeTab === 'learned' }]"
          @click="activeTab = 'learned'; editorMode = false"
        >
          学习的风格
        </button>
        <button
          :class="['styles-tab', { active: activeTab === 'favorites' }]"
          @click="activeTab = 'favorites'; editorMode = false"
        >
          收藏的风格
        </button>
        <button
          :class="['styles-tab', { active: activeTab === 'system' }]"
          @click="activeTab = 'system'; editorMode = false"
        >
          系统预设风格
        </button>
      </div>

      <div class="styles-search">
        <input
          v-model="searchQuery"
          type="text"
          class="styles-search-input"
          placeholder="搜索风格名或适用范围"
        />
      </div>
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
          <div class="style-editor-field">
            <label class="style-editor-label">适用范围 <span class="required">*</span></label>
            <div class="style-scope-tags">
              <div
                v-for="tag in parseScopeTags(editingStyle.scope)"
                :key="tag"
                class="style-scope-tag"
              >
                {{ tag }}
                <span class="style-scope-tag-remove" @click="editingStyle.scope = removeScopeTag(editingStyle.scope, tag)">×</span>
              </div>
              <input
                v-if="parseScopeTags(editingStyle.scope).length < MAX_SCOPE_TAGS"
                v-model="editingStyleScopeInput"
                type="text"
                class="style-scope-tag-input"
                placeholder="输入标签后回车"
                :maxlength="MAX_SCOPE_TAG_LENGTH"
                @keydown.enter.prevent="addEditingStyleTag"
              />
            </div>
            <div class="style-scope-hint">最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字</div>
            <div v-if="errors.scope" class="style-editor-error">{{ errors.scope }}</div>
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
        <div v-if="filteredMyStyles.length === 0" class="styles-empty">
          <div class="style-add-card" @click="goToCreate">
            <div class="style-add-icon">+</div>
            <div class="style-add-text">新建我的风格</div>
          </div>
        </div>
        <div v-else class="styles-grid">
          <div class="style-add-card" @click="goToCreate">
            <div class="style-add-icon">+</div>
            <div class="style-add-text">新建我的风格</div>
          </div>
          <div
            v-for="s in filteredMyStyles"
            :key="s.name"
            class="style-card"
          >
            <div class="style-card-head">
              <div class="style-card-avatar">{{ s.name.charAt(0) }}</div>
              <div class="style-card-title-wrap">
                <div class="style-card-title-row">
                  <div class="style-card-title">{{ s.name }}</div>
                  <div
                    v-if="getMarketStatus(s.name)"
                    class="style-card-status"
                    :class="statusClass(s.name)"
                  >
                    {{ getMarketStatus(s.name) }}
                  </div>
                </div>
                <div class="style-card-meta">自定义风格 · 已用 {{ s.count }} 次</div>
              </div>
              <button class="style-card-remove" @click.stop="deleteStyle(s.name)">删除</button>
            </div>
            <div v-if="s.scope" class="style-card-scope-list">
              <span v-for="tag in parseScopeTags(s.scope)" :key="tag" class="style-card-scope">{{ tag }}</span>
            </div>
            <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
            <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
            <div class="style-card-footer">
              <div class="style-card-actions">
                <button class="style-action-btn primary" @click.stop="useStyle(s)">使用</button>
                <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
                  {{ expandedNames.has(s.name) ? '收起' : '查看' }}
                </button>
                <button class="style-action-btn" @click.stop="goToEdit(s)">编辑</button>
                <button
                  v-if="getMarketStatus(s.name) === '审核中'"
                  class="style-action-btn success"
                  @click.stop="simulateApprove(s.name)"
                >通过</button>
                <button
                  v-else-if="!getMarketStatus(s.name)"
                  class="style-action-btn primary"
                  @click.stop="openPublishConfirm(s, 'my')"
                >发布</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统预设 -->
    <div v-show="activeTab === 'system'" class="styles-content">
      <div v-if="filteredSystemStyles.length === 0" class="styles-empty">
        没有找到匹配的系统预设风格
      </div>
      <div v-else class="styles-grid">
        <div
          v-for="s in filteredSystemStyles"
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
      <div v-if="filteredLearnedStyles.length === 0" class="styles-empty">
        <div class="style-add-card" @click="openImportDialog">
          <div class="style-add-icon">+</div>
          <div class="style-add-text">学习新风格</div>
        </div>
      </div>
      <div v-else class="styles-grid">
        <div class="style-add-card" @click="openImportDialog">
          <div class="style-add-icon">+</div>
          <div class="style-add-text">学习新风格</div>
        </div>
        <div
          v-for="s in filteredLearnedStyles"
          :key="s.name"
          class="style-card"
        >
          <div class="style-card-head">
            <div class="style-card-avatar learned">{{ s.name.charAt(0) }}</div>
            <div class="style-card-title-wrap">
              <div class="style-card-title-row">
                <div class="style-card-title">{{ s.name }}</div>
                <div
                  v-if="getMarketStatus(s.name)"
                  class="style-card-status"
                  :class="statusClass(s.name)"
                >
                  {{ getMarketStatus(s.name) }}
                </div>
              </div>
              <div class="style-card-meta">
                {{ s.sourceType.toUpperCase() }} · {{ s.createdAt.slice(0, 10) }}
              </div>
            </div>
            <button class="style-card-remove" @click.stop="deleteLearnedStyle(s.name)">删除</button>
          </div>
          <div v-if="s.scope" class="style-card-scope-list">
            <span v-for="tag in parseScopeTags(s.scope)" :key="tag" class="style-card-scope">{{ tag }}</span>
          </div>
          <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-footer">
            <div class="style-card-actions">
              <button class="style-action-btn primary" @click.stop="useStyle(s)">使用</button>
              <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
                {{ expandedNames.has(s.name) ? '收起' : '查看' }}
              </button>
              <button class="style-action-btn" @click.stop="goToEditLearned(s)">编辑</button>
              <button
                v-if="getMarketStatus(s.name) === '审核中'"
                class="style-action-btn success"
                @click.stop="simulateApprove(s.name)"
              >通过</button>
              <button
                v-else-if="!getMarketStatus(s.name)"
                class="style-action-btn primary"
                @click.stop="openPublishConfirm(s, 'learned')"
              >发布</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 收藏的风格 -->
    <div v-show="activeTab === 'favorites'" class="styles-content">
      <div v-if="favoriteStyles.length === 0" class="styles-empty">
        还没有收藏的风格，去风格市场看看吧
      </div>
      <div v-else class="styles-grid">
        <div
          v-for="s in favoriteStyles"
          :key="s.id"
          class="style-card"
        >
          <div class="style-card-head">
            <div class="style-card-avatar">{{ s.name.charAt(0) }}</div>
            <div class="style-card-title-wrap">
              <div class="style-card-title-row">
                <div class="style-card-title">{{ s.name }}</div>
              </div>
              <div class="style-card-meta">by {{ s.creatorName }}</div>
            </div>
            <button class="style-card-remove" @click.stop="toggleFavorite(s.id)">取消收藏</button>
          </div>
          <div v-if="s.scope" class="style-card-scope-list">
            <span v-for="tag in parseScopeTags(s.scope)" :key="tag" class="style-card-scope">{{ tag }}</span>
          </div>
          <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-footer">
            <div class="style-card-actions">
              <button class="style-action-btn primary" @click.stop="useFavoriteStyle(s)">使用</button>
              <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
                {{ expandedNames.has(s.name) ? '收起' : '查看' }}
              </button>
            </div>
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
    class="learned-import-modal"
    @cancel="closeImportDialog"
  >
    <template #title>
      <div class="modal-title">{{ isEditingLearned ? '编辑学习的风格' : '学习写作风格' }}</div>
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
          maxlength="3000"
        ></textarea>
        <div class="learned-counter">{{ pasteText.length }} / 3000</div>
        <div v-if="pasteError" class="learned-error">{{ pasteError }}</div>
        <button
          class="learned-submit-btn"
          :disabled="pasteText.trim().length < 200 || pasteText.trim().length > 3000"
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
            <span class="learned-upload-types">支持 .txt / .md / .docx（最大 1MB）</span>
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
      <div class="learned-result-title">{{ isEditingLearned ? '编辑风格' : '学习结果 ✓ 已从参考文章中提取风格' }}</div>
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
        <label class="learned-result-label">适用范围 <span class="required">*</span></label>
        <div class="style-scope-tags">
          <div
            v-for="tag in parseScopeTags(learnedResult.scope)"
            :key="tag"
            class="style-scope-tag"
          >
            {{ tag }}
            <span class="style-scope-tag-remove" @click="learnedResult.scope = removeScopeTag(learnedResult.scope, tag)">×</span>
          </div>
          <input
            v-if="parseScopeTags(learnedResult.scope).length < MAX_SCOPE_TAGS"
            v-model="learnedResultScopeInput"
            type="text"
            class="style-scope-tag-input"
            placeholder="输入标签后回车"
            :maxlength="MAX_SCOPE_TAG_LENGTH"
            @keydown.enter.prevent="addLearnedResultTag"
          />
        </div>
        <div class="style-scope-hint">最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字</div>
        <div v-if="!learnedResult.scope || !parseScopeTags(learnedResult.scope).length" class="learned-hint">请至少添加一个适用范围标签</div>
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
        <div v-if="learnedNameConflict" class="learned-error">该风格名称已存在</div>
        <div v-else-if="learnedResult.name.trim().length > 20" class="learned-error">风格名称最多 20 字</div>
        <div v-else-if="learnedResultError" class="learned-error">{{ learnedResultError }}</div>
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

  <a-modal
    :open="publishConfirmVisible"
    title="发布风格到市场"
    :footer="null"
    :width="480"
    centered
    @cancel="closePublishConfirm"
  >
    <div class="publish-confirm-body">
      <p class="publish-confirm-title">确认发布「{{ pendingPublish.style?.name }}」？</p>
      <ol class="publish-confirm-list">
        <li>发布后将进入<span class="publish-confirm-highlight">平台审核流程</span>，审核通过后其他用户才可在风格市场中发现并使用该风格。</li>
        <li>审核期间该风格会显示<span class="publish-confirm-highlight">「审核中」</span>状态，你可以随时查看进度。</li>
        <li>风格被他人使用后，你将按照<span class="publish-confirm-highlight">收益规则</span>获得<span class="publish-confirm-highlight">创作币奖励</span>。</li>
      </ol>
      <p class="publish-confirm-tip">请确保风格提示词符合<span class="publish-confirm-highlight">平台规范</span>，避免违规内容。</p>
    </div>
    <div class="publish-confirm-actions">
      <button class="publish-confirm-cancel" @click="closePublishConfirm">取消</button>
      <button class="publish-confirm-submit" @click="confirmPublish">确认发布</button>
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
  readDocxAsText,
  updateLearnedStyle
} from '@/composables/useStyles.js'
import {
  marketStyles,
  shareStyleToMarket,
  approveMarketStyle,
  favoriteStyles,
  toggleFavorite,
  useMarketStyle
} from '@/composables/useStyleMarket.js'

const router = useRouter()
const activeTab = ref('my')
const searchQuery = ref('')

const MAX_SCOPE_TAGS = 3
const MAX_SCOPE_TAG_LENGTH = 8

const parseScopeTags = (scopeStr) => {
  if (!scopeStr) return []
  return scopeStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

const formatScopeTags = (tags) => tags.join(',')

const validateScopeTags = (tags) => {
  if (tags.length === 0) return '请至少添加一个适用范围标签'
  if (tags.length > MAX_SCOPE_TAGS) return `最多添加 ${MAX_SCOPE_TAGS} 个标签`
  for (const tag of tags) {
    if (tag.length > MAX_SCOPE_TAG_LENGTH) return `每个标签最多 ${MAX_SCOPE_TAG_LENGTH} 个字`
  }
  return ''
}

const addScopeTag = (scopeStr, inputRef) => {
  const raw = inputRef.value.trim()
  if (!raw) return scopeStr
  const tags = parseScopeTags(scopeStr)
  if (tags.length >= MAX_SCOPE_TAGS) {
    inputRef.value = ''
    return scopeStr
  }
  const newTags = raw.split(/[,，]/).map(t => t.trim()).filter(Boolean)
  for (const tag of newTags) {
    if (tags.length >= MAX_SCOPE_TAGS) break
    if (tag.length > MAX_SCOPE_TAG_LENGTH) continue
    if (!tags.includes(tag)) tags.push(tag)
  }
  inputRef.value = ''
  return formatScopeTags(tags)
}

const removeScopeTag = (scopeStr, tag) => {
  return formatScopeTags(parseScopeTags(scopeStr).filter(t => t !== tag))
}

const addEditingStyleTag = () => {
  editingStyle.scope = addScopeTag(editingStyle.scope, editingStyleScopeInput)
}

const addLearnedResultTag = () => {
  if (!learnedResult.value) return
  learnedResult.value.scope = addScopeTag(learnedResult.value.scope, learnedResultScopeInput)
}

const filterText = (s) => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return true
  return (
    s.name.toLowerCase().includes(q) ||
    (s.scope && s.scope.toLowerCase().includes(q)) ||
    (s.prompt && s.prompt.toLowerCase().includes(q))
  )
}

const filteredMyStyles = computed(() => myStyles.value.filter(filterText))
const filteredLearnedStyles = computed(() => learnedStyles.value.filter(filterText))
const filteredSystemStyles = computed(() =>
  systemStyles.filter(s => {
    const q = searchQuery.value.trim().toLowerCase()
    if (!q) return true
    return (
      s.name.toLowerCase().includes(q) ||
      (s.desc && s.desc.toLowerCase().includes(q)) ||
      (s.promptSummary && s.promptSummary.toLowerCase().includes(q)) ||
      (s.prompt && s.prompt.toLowerCase().includes(q))
    )
  })
)

// 导入对话框状态
const importDialogVisible = ref(false)
const importSubTab = ref('paste')
const pasteText = ref('')
const pasteError = ref('')
const uploadFile = ref(null)
const uploadError = ref('')
const learnedResult = ref(null)
const learnedResultScopeInput = ref('')
const learnedResultError = ref('')
const isEditingLearned = ref(false)
const editingLearnedOriginalName = ref('')
const editorMode = ref(false)
const expandedNames = ref(new Set())
const publishConfirmVisible = ref(false)
const pendingPublish = ref({ style: null, sourceType: '' })

const editingStyle = reactive({
  originalName: '',
  name: '',
  prompt: '',
  scope: ''
})
const editingStyleScopeInput = ref('')

const errors = reactive({
  name: '',
  prompt: '',
  scope: ''
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
  errors.scope = ''

  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  const scope = editingStyle.scope.trim()
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

  const scopeTags = parseScopeTags(scope)
  const scopeError = validateScopeTags(scopeTags)
  if (scopeError) {
    errors.scope = scopeError
    valid = false
  }

  return valid
}

const isFormValid = computed(() => {
  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  const scopeTags = parseScopeTags(editingStyle.scope)
  return name && name.length <= 20 && prompt && prompt.length <= 1000 && !validateScopeTags(scopeTags) && !isStyleNameExists(name, editingStyle.originalName)
})

const goToCreate = () => {
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.prompt = ''
  editingStyle.scope = ''
  editingStyleScopeInput.value = ''
  errors.name = ''
  errors.prompt = ''
  errors.scope = ''
  editorMode.value = true
}

const goToEdit = (style) => {
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.prompt = style.prompt
  editingStyle.scope = style.scope || ''
  editingStyleScopeInput.value = ''
  errors.name = ''
  errors.prompt = ''
  errors.scope = ''
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
      prompt: editingStyle.prompt,
      scope: editingStyle.scope
    })
  } else {
    addCustomStyle({
      name: editingStyle.name,
      prompt: editingStyle.prompt,
      scope: editingStyle.scope
    })
  }
  editorMode.value = false
}

const useStyle = (style) => {
  applyStyle(style)
  router.push('/console/create')
}

const useFavoriteStyle = (style) => {
  try {
    useMarketStyle(style.id)
    router.push(`/console/create?marketStyleId=${style.id}`)
  } catch (err) {
    alert(err.message)
  }
}

const deleteStyle = (name) => {
  removeCustomStyle(name)
}

const openImportDialog = () => {
  pasteText.value = ''
  pasteError.value = ''
  uploadFile.value = null
  uploadError.value = ''
  learnedResult.value = null
  learnedResultError.value = ''
  learnedResultScopeInput.value = ''
  isEditingLearned.value = false
  editingLearnedOriginalName.value = ''
  importSubTab.value = 'paste'
  importDialogVisible.value = true
}

const goToEditLearned = (style) => {
  learnedResult.value = { ...style }
  learnedResultError.value = ''
  learnedResultScopeInput.value = ''
  isEditingLearned.value = true
  editingLearnedOriginalName.value = style.name
  importDialogVisible.value = true
}

const closeImportDialog = () => {
  importDialogVisible.value = false
  isEditingLearned.value = false
  editingLearnedOriginalName.value = ''
}

const onFileChange = (e) => {
  uploadError.value = ''
  const file = e.target.files?.[0]
  if (!file) {
    uploadFile.value = null
    return
  }
  if (file.size > 1 * 1024 * 1024) {
    uploadError.value = '文件过大（> 1MB）'
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
  if (text.length > 3000) {
    pasteError.value = '正文过长（超过 3000 字）'
    return
  }
  await runAnalysis(text, 'paste')
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
    if (text.trim().length > 3000) {
      uploadError.value = '正文过长（超过 3000 字）'
      return
    }
    await runAnalysis(text, ext)
  } catch (err) {
    uploadError.value = err.message || '文件读取失败'
  }
}

const runAnalysis = async (text, sourceType) => {
  const tempResult = await analyzeArticleStyle(text, { sourceType })
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
  const scopeTags = parseScopeTags(learnedResult.value.scope)
  if (scopeTags.length === 0 || validateScopeTags(scopeTags)) return false
  const excludeName = isEditingLearned.value ? editingLearnedOriginalName.value : null
  if (isStyleNameExists(name, excludeName) || isLearnedStyleNameExists(name, excludeName)) return false
  return true
})

const learnedNameConflict = computed(() => {
  if (!learnedResult.value) return false
  const name = learnedResult.value.name.trim()
  if (!name) return false
  const excludeName = isEditingLearned.value ? editingLearnedOriginalName.value : null
  return isStyleNameExists(name, excludeName) || isLearnedStyleNameExists(name, excludeName)
})

const saveLearnedResult = () => {
  if (!learnedResult.value) return
  const name = learnedResult.value.name.trim()
  const excludeName = isEditingLearned.value ? editingLearnedOriginalName.value : null
  if (isStyleNameExists(name, excludeName) || isLearnedStyleNameExists(name, excludeName)) {
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
  if (!learnedResult.value.scope || !parseScopeTags(learnedResult.value.scope).length) {
    learnedResultError.value = '请填写适用范围'
    return
  }
  const scopeError = validateScopeTags(parseScopeTags(learnedResult.value.scope))
  if (scopeError) {
    learnedResultError.value = scopeError
    return
  }
  if (isEditingLearned.value) {
    updateLearnedStyle(editingLearnedOriginalName.value, learnedResult.value)
  } else {
    addLearnedStyle(learnedResult.value)
  }
  closeImportDialog()
}

const deleteLearnedStyle = (name) => {
  if (!confirm('确定要删除「' + name + '」吗？')) return
  removeLearnedStyle(name)
}

const getMarketStatus = (name) => {
  const s = marketStyles.value.find(
    m => m.originalName === name && m.creatorId === localStorage.getItem('aichuangzuo_user_id')
  )
  if (!s) return ''
  if (s.status === 'pending') return '审核中'
  if (s.status === 'approved') return '已上架'
  return ''
}

const statusClass = (name) => {
  const status = getMarketStatus(name)
  if (status === '已上架') return 'approved'
  if (status === '审核中') return 'pending'
  return ''
}

const shareStyle = (style, sourceType) => {
  try {
    shareStyleToMarket(style, sourceType)
  } catch (err) {
    alert(err.message)
  }
}

const openPublishConfirm = (style, sourceType) => {
  pendingPublish.value = { style, sourceType }
  publishConfirmVisible.value = true
}

const confirmPublish = () => {
  const { style, sourceType } = pendingPublish.value
  if (style && sourceType) {
    shareStyle(style, sourceType)
  }
  publishConfirmVisible.value = false
  pendingPublish.value = { style: null, sourceType: '' }
}

const closePublishConfirm = () => {
  publishConfirmVisible.value = false
  pendingPublish.value = { style: null, sourceType: '' }
}

const simulateApprove = (name) => {
  const s = marketStyles.value.find(
    m => m.originalName === name && m.creatorId === localStorage.getItem('aichuangzuo_user_id')
  )
  if (s) approveMarketStyle(s.id)
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

.styles-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.styles-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  height: 44px;
  width: fit-content;
}

.styles-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.styles-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.styles-search {
  display: flex;
  align-items: center;
}

.styles-search-input {
  width: 100%;
  min-width: 280px;
  max-width: 480px;
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.styles-search-input:focus {
  outline: none;
  border-color: #ff2442;
}

.styles-empty {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.styles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.style-add-card {
  border: 2px dashed #e8e8e8;
  border-radius: 20px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 270px;
  box-sizing: border-box;
}

.style-add-card:hover {
  border-color: #ff2442;
  background: #fff8f9;
}

.style-add-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.style-add-text {
  font-size: 15px;
  color: #595959;
}

.style-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.style-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 36px rgba(255, 36, 66, 0.13);
}

.style-card-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 16px;
}

.style-card-avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff2442, #ff8a9b);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
}

.style-card-avatar.learned {
  background: linear-gradient(135deg, #ff8a9b, #ffc2cb);
}

.style-card-title-wrap {
  flex: 1;
  min-width: 0;
}

.style-card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.style-card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.35;
  word-break: break-all;
}

.style-card-status {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 12px;
  white-space: nowrap;
}

.style-card-status.approved {
  background: #f6ffed;
  color: #52c41a;
}

.style-card-status.pending {
  background: #fff7e6;
  color: #fa8c16;
}

.style-card-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.style-card-remove {
  flex-shrink: 0;
  padding: 4px 10px;
  border: none;
  background: #fff1f0;
  color: #ff4d4f;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.style-card-remove:hover {
  background: #ffccc7;
}

.style-card-scope {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  font-size: 13px;
  color: #ff2442;
  background: #fff0f2;
  border: 1px solid #ffd1d9;
  padding: 4px 12px;
  border-radius: 20px;
}

.style-card-scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.style-card-scope::before {
  content: '#';
  opacity: 0.7;
}

.style-card-prompt {
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
  margin-bottom: 18px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.style-prompt-full {
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
  background: #fafafa;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 18px;
  white-space: pre-line;
}

.style-card-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #f5f5f5;
}

.style-card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.style-action-btn {
  padding: 8px 16px;
  border: 1px solid #e8e8e8;
  background: #fff;
  border-radius: 10px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.style-action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
  background: #fff0f2;
}

.style-action-btn.primary {
  background: #ff2442;
  color: #fff;
  border-color: #ff2442;
  min-width: 72px;
}

.style-action-btn.primary:hover {
  background: #e61e3a;
}

.style-action-btn.success {
  background: #f6ffed;
  color: #52c41a;
  border-color: #b7eb8f;
}

.style-action-btn.success:hover {
  background: #d9f7be;
}

.publish-confirm-body {
  padding: 8px 0 16px;
}

.publish-confirm-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.publish-confirm-list {
  margin: 0 0 14px;
  padding-left: 18px;
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
}

.publish-confirm-list li {
  margin-bottom: 8px;
}

.publish-confirm-tip {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.publish-confirm-highlight {
  color: #ff2442;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.publish-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.publish-confirm-cancel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
}

.publish-confirm-submit {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.publish-confirm-submit:hover {
  background: #e61e3a;
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

.style-scope-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #fff;
  min-height: 44px;
  box-sizing: border-box;
}

.style-scope-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff0f2;
  border: 1px solid #ffd1d9;
  border-radius: 16px;
  font-size: 13px;
  color: #ff2442;
}

.style-scope-tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  cursor: pointer;
  color: #ff8a9b;
  font-size: 14px;
  line-height: 1;
}

.style-scope-tag-remove:hover {
  color: #ff2442;
  background: #ffe0e5;
}

.style-scope-tag-input {
  flex: 1;
  min-width: 80px;
  border: none;
  outline: none;
  font-size: 14px;
  color: #1a1a1a;
  background: transparent;
  padding: 4px 2px;
}

.style-scope-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
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
  background: #fff0f2;
  border: 1px solid #ffd1d9;
  border-radius: 8px;
  font-size: 13px;
  color: #ff2442;
  margin-bottom: 16px;
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
  border-color: #ff2442;
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
  border-color: #ff2442;
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
  border-color: #ff2442;
  background: #fff0f2;
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
  color: #ff2442;
}

.learned-error {
  color: #ff4d4f;
  font-size: 13px;
}

.learned-submit-btn {
  padding: 10px 20px;
  background: #ff2442;
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

.learned-hint {
  font-size: 12px;
  color: #8c8c8c;
}

/* 深色模式 */
body[data-theme="dark"] .styles-tabs {
  background: #1a1a1a;
}

body[data-theme="dark"] .styles-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-tab.active {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .styles-search-input {
  background: #141414;
  border-color: #303030;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-card,
body[data-theme="dark"] .style-add-card,
body[data-theme="dark"] .style-preset-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-card-title,
body[data-theme="dark"] .modal-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-card-desc,
body[data-theme="dark"] .style-card-prompt,
body[data-theme="dark"] .style-card-meta,
body[data-theme="dark"] .learned-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-cancel-btn {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .styles-empty {
  background: #1f1f1f;
  border: 1px dashed #303030;
  border-radius: 16px;
  padding: 32px;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-add-card:hover {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-add-icon {
  color: var(--color-primary);
}

body[data-theme="dark"] .style-add-text {
  color: #d9d9d9;
}

body[data-theme="dark"] .style-card:hover {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-card-avatar {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-card-avatar.learned {
  background: rgba(255, 36, 66, 0.15);
  color: var(--color-primary);
}

body[data-theme="dark"] .style-card-status.approved {
  background: rgba(7, 193, 96, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .style-card-status.pending {
  background: rgba(250, 140, 22, 0.15);
  color: #ffa940;
}

body[data-theme="dark"] .style-card-remove {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-card-remove:hover {
  background: #2a2a2a;
  color: #ff4d4f;
}

body[data-theme="dark"] .style-card-scope {
  background: #2a2a2a;
  color: #d9d9d9;
}

body[data-theme="dark"] .style-card-prompt {
  background: #141414;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-action-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .style-action-btn.primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .style-action-btn.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .style-action-btn.success {
  background: rgba(7, 193, 96, 0.15);
  border-color: rgba(7, 193, 96, 0.4);
  color: #4ade80;
}

body[data-theme="dark"] .style-action-btn.success:hover {
  background: rgba(7, 193, 96, 0.25);
  border-color: #4ade80;
}

body[data-theme="dark"] .publish-confirm-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-confirm-list li,
body[data-theme="dark"] .publish-confirm-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .publish-confirm-highlight {
  color: var(--color-primary);
}

body[data-theme="dark"] .publish-confirm-cancel {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .publish-confirm-submit {
  background: var(--color-primary);
}

body[data-theme="dark"] .publish-confirm-submit:hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .style-editor {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-editor-back {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-editor-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-label {
  color: #d9d9d9;
}

body[data-theme="dark"] .style-editor-input,
body[data-theme="dark"] .style-editor-textarea {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-input::placeholder,
body[data-theme="dark"] .style-editor-textarea::placeholder {
  color: #737373;
}

body[data-theme="dark"] .style-editor-input:focus,
body[data-theme="dark"] .style-editor-textarea:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-editor-error {
  color: #ff7875;
}

body[data-theme="dark"] .style-scope-tag {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .style-scope-tag-remove {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-scope-tag-remove:hover {
  background: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-scope-tag-input {
  background: transparent;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-scope-hint {
  color: #737373;
}

body[data-theme="dark"] .style-editor-counter {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-editor-counter.over {
  color: #ff7875;
}

body[data-theme="dark"] .style-editor-preset-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-preset-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-preset-card:hover {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-preset-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-preset-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .save-style-btn {
  background: var(--color-primary);
}

body[data-theme="dark"] .save-style-btn:hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .save-style-btn:disabled {
  background: #434343;
  color: #737373;
}

body[data-theme="dark"] .learned-banner {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .learned-subtab {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-subtab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-subtab.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

body[data-theme="dark"] .learned-textarea,
body[data-theme="dark"] .learned-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-textarea::placeholder,
body[data-theme="dark"] .learned-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .learned-textarea:focus,
body[data-theme="dark"] .learned-input:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .learned-counter {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-counter.over {
  color: #ff7875;
}

body[data-theme="dark"] .learned-upload-zone {
  background: #1f1f1f;
  border-color: #434343;
}

body[data-theme="dark"] .learned-upload-zone:hover {
  border-color: var(--color-primary);
  background: #262626;
}

body[data-theme="dark"] .learned-upload-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-upload-types {
  color: #737373;
}

body[data-theme="dark"] .learned-error {
  color: #ff7875;
}

body[data-theme="dark"] .learned-submit-btn {
  background: var(--color-primary);
}

body[data-theme="dark"] .learned-submit-btn:disabled {
  background: #434343;
  color: #737373;
}

body[data-theme="dark"] .learned-progress-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-result-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-result-field {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .learned-result-label {
  color: #d9d9d9;
}

body[data-theme="dark"] .learned-excerpt {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

/* 适用范围 tag 输入容器（风格编辑器内 + 学习结果区） */
body[data-theme="dark"] .style-scope-tags {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-scope-tag {
  background: rgba(255, 36, 66, 0.15);
  border-color: rgba(255, 36, 66, 0.4);
  color: #ff4d6a;
}

body[data-theme="dark"] .style-scope-tag-remove {
  color: #ff8a9b;
}

body[data-theme="dark"] .style-scope-tag-remove:hover {
  background: rgba(255, 36, 66, 0.25);
  color: #ff2442;
}

body[data-theme="dark"] .style-scope-tag-input {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-scope-tag-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .style-scope-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .modal-title {
  color: #f0f0f0;
}
</style>

<style>
/* 学习风格导入对话框：teleport 到 body，需非 scoped 全局覆盖 */
body[data-theme="dark"] .learned-import-modal .ant-modal-content,
body[data-theme="dark"] .learned-import-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .learned-import-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .learned-import-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}
</style>

<template>
  <a-modal
    v-model:open="styleVisible"
    :footer="null"
    :width="720"
    centered
    class="style-modal"
  >
    <template #title>
      <div class="modal-title-wrap">
        <div class="modal-title">风格库</div>
        <div class="modal-subtitle">选择一套预设风格，让 AI 写出你想要的调性</div>
      </div>
    </template>

    <!-- 创建/编辑风格 -->
    <div v-if="createStyleMode" class="style-editor">
      <div class="style-editor-header">
        <button class="style-editor-back" @click="goBackToList">← 返回</button>
        <div class="style-editor-title">{{ editingStyle.name ? '编辑提示词' : '新建我的风格' }}</div>
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
        </div>
        <div class="style-editor-field">
          <label class="style-editor-label">风格提示词 <span class="required">*</span></label>
          <textarea
            v-model="editingStyle.prompt"
            class="style-editor-textarea"
            placeholder="描述你希望 AI 采用的语气、结构、用词习惯等..."
            rows="5"
          ></textarea>
          <div class="style-editor-hint">提示词会作为系统提示的一部分影响生成结果。</div>
        </div>
        <div class="style-editor-field">
          <label class="style-editor-label">适用范围 <span class="required">*</span></label>
          <input
            v-model="editingStyle.scope"
            type="text"
            class="style-editor-input"
            placeholder="例：公众号情感文 / 产品评测 / 小红书种草"
            maxlength="50"
          />
        </div>
        <div class="style-editor-presets">
          <div class="style-editor-preset-label">快速填充模板：</div>
          <div class="style-editor-preset-list">
            <div
              v-for="preset in stylePresets"
              :key="preset.name"
              class="style-preset-card"
              @click="editingStyle.prompt = preset.prompt"
            >
              <div class="style-preset-title">{{ preset.name }}</div>
              <div class="style-preset-desc">{{ preset.desc }}</div>
            </div>
          </div>
        </div>
        <button class="save-style-btn" @click="saveStyle">保存</button>
      </div>
    </div>

    <!-- 风格列表 -->
    <template v-else>
      <div class="style-tabs">
        <button
          :class="['style-tab', { active: styleTab === 'my' }]"
          @click="styleTab = 'my'; createStyleMode = false"
        >
          我的风格
        </button>
        <button
          :class="['style-tab', { active: styleTab === 'learned' }]"
          @click="styleTab = 'learned'; createStyleMode = false; loadLearnedStyles()"
        >
          学习的风格
        </button>
        <button
          :class="['style-tab', { active: styleTab === 'system' }]"
          @click="styleTab = 'system'; createStyleMode = false"
        >
          系统预设风格
        </button>
      </div>

      <div class="style-content">
        <!-- 系统预设 -->
        <div v-show="styleTab === 'system'" class="style-grid">
          <div
            v-for="s in systemStyles"
            :key="s.name"
            :class="['style-card', { selected: selectedStyleName === s.name }]"
            @click="selectStyle(s)"
          >
            <div class="style-card-title">{{ s.name }}</div>
            <div class="style-card-desc">{{ s.desc }}</div>
            <div class="style-card-prompt">{{ s.promptSummary }}</div>
          </div>
        </div>

        <!-- 我的风格 -->
        <div v-show="styleTab === 'my'" class="style-grid">
          <div class="style-add-card" @click="goToCreateStyle">
            <div class="style-add-icon">+</div>
            <div class="style-add-text">新建我的风格</div>
          </div>
          <div
            v-for="(m, idx) in myStyles"
            :key="m.name"
            :class="['style-card', { selected: selectedStyleName === m.name }]"
            @click="selectStyle(m)"
          >
            <div class="style-card-title">{{ m.name }}</div>
            <div class="style-card-desc">{{ m.desc }} · 已用 {{ m.count }} 次</div>
            <div v-if="m.scope" class="style-card-scope">适用：{{ m.scope }}</div>
            <div class="style-prompt-toggle" @click.stop="togglePrompt(idx)">
              {{ expandedPromptIdx === idx ? '收起 ▴' : '查看完整提示词 ▾' }}
            </div>
            <div v-show="expandedPromptIdx === idx" class="style-prompt-full">
              {{ m.prompt }}
            </div>
            <div v-show="expandedPromptIdx === idx" class="style-prompt-actions">
              <button class="style-action-btn" @click.stop="goToEditStyle(m)">编辑提示词</button>
              <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(m.name)">删除</button>
            </div>
          </div>
        </div>

        <!-- 学习的风格 -->
        <div v-show="styleTab === 'learned'" class="style-grid">
          <div
            v-if="learnedStyles.length === 0"
            class="style-empty style-empty-text"
          >
            还没有学习过的风格，请前往「我的风格」页面学习。
          </div>
          <div
            v-for="(l, idx) in learnedStyles"
            v-else
            :key="l.name"
            :class="['style-card', { selected: selectedStyleName === l.name }]"
            @click="selectStyle(l)"
          >
            <div class="style-card-title">{{ l.name }}</div>
            <div v-if="l.scope" class="style-card-scope">适用：{{ l.scope }}</div>
            <div class="style-prompt-toggle" @click.stop="toggleLearnedPrompt(idx)">
              {{ expandedLearnedIdx === idx ? '收起 ▴' : '查看完整提示词 ▾' }}
            </div>
            <div v-show="expandedLearnedIdx === idx" class="style-prompt-full">
              {{ l.prompt }}
            </div>
          </div>
        </div>
      </div>

      <div class="style-footer">
        <button
          class="style-apply-btn"
          :disabled="!selectedStyleName"
          @click="applyStyleLocal"
        >
          应用
        </button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { Modal } from 'ant-design-vue'
import {
  systemStyles,
  myStyles,
  applyStyle,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle,
  learnedStyles,
  loadMyStyles,
  loadLearnedStyles
} from '@/composables/useStyles.js'
import { useCreateForm } from '../useCreateForm.js'

const { styleVisible } = useCreateForm()

const styleTab = ref('my')
const selectedStyleName = ref(null)
const expandedPromptIdx = ref(null)
const expandedLearnedIdx = ref(null)
const createStyleMode = ref(false)
const editingStyle = reactive({ originalName: '', name: '', prompt: '', scope: '', isEdit: false })

// 弹框打开时重置到列表态并加载我的风格（原 openStyleModal 逻辑）
watch(styleVisible, async (open) => {
  if (!open) return
  styleTab.value = 'my'
  selectedStyleName.value = null
  expandedPromptIdx.value = null
  createStyleMode.value = false
  await loadMyStyles()
})

const selectStyle = (s) => {
  selectedStyleName.value = s.name
}

const applyStyleLocal = () => {
  if (!selectedStyleName.value) return
  const s = systemStyles.value.find(x => x.name === selectedStyleName.value) ||
            myStyles.value.find(x => x.name === selectedStyleName.value) ||
            learnedStyles.value.find(x => x.name === selectedStyleName.value)
  if (s) {
    applyStyle(s)
    styleVisible.value = false
  }
}

const goToCreateStyle = () => {
  createStyleMode.value = true
  editingStyle.isEdit = false
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.prompt = ''
  editingStyle.scope = ''
}

const goToEditStyle = (style) => {
  createStyleMode.value = true
  editingStyle.isEdit = true
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.prompt = style.prompt
  editingStyle.scope = style.scope || ''
}

const goBackToList = () => {
  createStyleMode.value = false
}

const saveStyle = async () => {
  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  const scope = editingStyle.scope.trim()
  if (!name || !prompt || !scope) return
  if (name.length > 20 || prompt.length > 1000 || scope.length > 50) return
  try {
    if (editingStyle.isEdit) {
      await updateCustomStyle(editingStyle.originalName, { name, prompt, scope })
    } else {
      await addCustomStyle({ name, prompt, scope })
    }
    createStyleMode.value = false
  } catch {
    // composable 已 message.error
  }
}

const deleteStyle = (name) => {
  Modal.confirm({
    title: '删除风格',
    content: `确定要删除风格「${name}」吗？删除后不可恢复。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: async () => {
      try {
        await removeCustomStyle(name)
        if (selectedStyleName.value === name) selectedStyleName.value = null
      } catch {
        // composable 已 message.error
      }
    }
  })
}

const togglePrompt = (idx) => {
  expandedPromptIdx.value = expandedPromptIdx.value === idx ? null : idx
}

const toggleLearnedPrompt = (idx) => {
  expandedLearnedIdx.value = expandedLearnedIdx.value === idx ? null : idx
}

const stylePresets = [
  { name: '产品评测', desc: '客观中立、参数对比', prompt: '你是客观的产品评测人：\n- 语气客观中立、有理有据\n- 结构：外观设计 → 核心性能 → 实际体验 → 优缺点总结\n- 必带参数对比表\n- 给出明确购买建议' },
  { name: '情感散文', desc: '细腻温暖、意象留白', prompt: '你是细腻的散文家：\n- 语气细腻、温暖，共情\n- 大量使用比喻、意象、留白\n- 第一人称叙述\n- 段落短而精，不要说教' },
  { name: '职场干货', desc: '专业务实、可执行', prompt: '你是资深职场导师：\n- 语气专业务实\n- 结构：行业痛点 → 核心方案 → 具体步骤\n- 必带可执行的 checklist\n- 避免假大空、避免鸡汤' },
  { name: '营销文案', desc: '紧迫感 + 利益点', prompt: '你是营销高手：\n- 开头制造紧迫感 / 共鸣痛点\n- 突出 3 个核心利益点\n- 必带强 CTA\n- 语气坚定、有说服力' }
]
</script>

<style scoped>
/* 风格选择 */
.style-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.style-tab {
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.style-tab.active {
  color: var(--color-primary);
  font-weight: 600;
  border-bottom-color: var(--color-primary);
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.style-content {
  height: 60vh;
  overflow-y: auto;
}

.style-editor {
  height: 60vh;
  overflow-y: auto;
}

.style-card {
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-card.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-card-title {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
  margin-bottom: 4px;
}

.style-card-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.style-card-scope {
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.style-card-prompt {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  white-space: pre-line;
}

.style-card-count {
  font-size: 12px;
  color: var(--color-primary);
}

.style-add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border: 2px dashed #d9d9d9;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 100px;
}

.style-add-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-add-icon {
  font-size: 24px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.style-add-text {
  font-size: 13px;
  color: #8c8c8c;
}

.style-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 32px 0;
}

.style-empty-text {
  color: #8c8c8c;
  font-size: 14px;
}

.style-footer {
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.style-apply-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: #d9d9d9;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: not-allowed;
}

.style-apply-btn:not(:disabled) {
  background: var(--color-primary);
  cursor: pointer;
}

.style-apply-btn:not(:disabled):hover {
  background: var(--color-primary-hover);
}

.style-prompt-toggle {
  font-size: 12px;
  color: var(--color-primary);
  cursor: pointer;
  margin-top: 8px;
}

.style-prompt-full {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  margin-top: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 6px;
  white-space: pre-wrap;
}

.style-prompt-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.style-action-btn {
  font-size: 12px;
  color: var(--color-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.style-action-btn:hover {
  text-decoration: underline;
}

.style-del-btn {
  color: #ff4d4f;
}

.style-editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.style-editor-back {
  background: none;
  border: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 14px;
}

.style-editor-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-editor-form {
  padding: 0;
}

.style-editor-field {
  margin-bottom: 16px;
}

.style-editor-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.style-editor-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  box-sizing: border-box;
}

.style-editor-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.style-editor-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.style-editor-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.style-editor-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
}

.style-editor-presets {
  margin-bottom: 16px;
}

.style-editor-preset-label {
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
}

.style-editor-preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-preset-card {
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-preset-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-preset-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-preset-desc {
  font-size: 11px;
  color: #8c8c8c;
}

.save-style-btn {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.save-style-btn:hover {
  background: var(--color-primary-hover);
}


/* 风格编辑内联表单 */
body[data-theme="dark"] .style-editor-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-label {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-hint {
  color: #a6a6a6;
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
  outline: none;
}

body[data-theme="dark"] .style-editor-preset {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .style-editor-preset:hover {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .style-preset-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-preset-card:hover {
  background: rgba(255, 36, 66, 0.12);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-preset-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-preset-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .style-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

body[data-theme="dark"] .style-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-card:hover,
body[data-theme="dark"] .style-card.selected {
  background: rgba(255, 36, 66, 0.12);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-card-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-card-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-card-scope {
  color: #4dabf7;
  background: rgba(24, 144, 255, 0.15);
}

body[data-theme="dark"] .style-card-prompt {
  color: #d9d9d9;
}

body[data-theme="dark"] .style-add-card {
  border-color: #434343;
  background: transparent;
}

body[data-theme="dark"] .style-add-card:hover {
  border-color: var(--color-primary);
  background: rgba(255, 36, 66, 0.08);
}

body[data-theme="dark"] .style-add-icon,
body[data-theme="dark"] .style-add-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-empty-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-footer {
  border-top-color: #303030;
}

body[data-theme="dark"] .style-apply-btn {
  background: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-apply-btn:not(:disabled) {
  background: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .style-apply-btn:not(:disabled):hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .style-prompt-toggle {
  color: var(--color-primary);
}
</style>

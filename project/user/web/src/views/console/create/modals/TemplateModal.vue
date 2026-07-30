<template>
  <a-modal
    v-model:open="templateVisible"
    :footer="null"
    :width="960"
    centered
    :closable="true"
    class="template-modal template-lib-modal"
    wrap-class-name="template-modal-wrap"
  >
    <template #title>
      <div class="modal-title-wrap">
        <div class="modal-title">导出模板库</div>
        <div class="modal-subtitle">共 {{ filteredTemplates.length }} 个模板 · 左侧实时预览 · 右侧选择模板</div>
      </div>
    </template>

    <!-- 顶部声明：目前不支持自定义导出模板 -->
    <div class="template-notice">
      <span class="template-notice-icon">i</span>
      当前仅提供系统预设模板，暂不支持自定义导出模板。可访问范围由所购套餐决定。
    </div>

    <!-- 平台标签 -->
    <div class="template-tabs">
      <button
        v-for="tab in templatePlatformTabs"
        :key="tab.key"
        :class="['template-tab', { active: templatePlatformTab === tab.key }]"
        @click="templatePlatformTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 2列布局 -->
    <div class="template-body">
      <!-- 左侧：大预览 -->
      <div class="template-preview-pane" v-html="currentTemplatePreview"></div>

      <!-- 右侧：模板列表 -->
      <div class="template-list-pane">
        <div
          v-for="t in filteredTemplates"
          :key="t.key"
          :class="['template-row', { selected: selectedTemplateKey === t.key, locked: !t.accessible }]"
          @click="selectTemplate(t)"
        >
          <div v-if="!t.accessible" class="template-row-badge locked">需升级</div>
          <div class="template-row-info">
            <div class="template-row-name">{{ t.name }}</div>
            <div class="template-row-desc">{{ t.desc }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="template-footer">
      <button class="template-apply-btn" @click="applyTemplate">应用</button>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { buildLargePreview } from '@/utils/articleTemplates.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useCreateForm } from '../useCreateForm.js'

const { templateVisible, selectedTemplateKey } = useCreateForm()
const { templates: apiTemplates } = useExportTemplates()
const allTemplates = computed(() => apiTemplates.value)

const templatePlatformTabs = [
  { key: 'all', label: '全部' },
  { key: 'wechat', label: '公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '今日头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'zhihu', label: '知乎' },
  { key: 'douyin', label: '抖音图文' },
  { key: 'general', label: '通用提示词' }
]
const templatePlatformTab = ref('all')

const filteredTemplates = computed(() => {
  if (templatePlatformTab.value === 'all') return allTemplates.value
  if (templatePlatformTab.value === 'general') return allTemplates.value.filter(t => ['business', 'marketing', 'academic', 'story', 'magazine', 'card', 'checklist', 'dark'].includes(t.key))
  return allTemplates.value.filter(t => t.key.startsWith(templatePlatformTab.value))
})

const currentTemplate = computed(() => allTemplates.value.find(t => t.key === selectedTemplateKey.value) || allTemplates.value[0])
const currentTemplatePreview = computed(() => buildLargePreview(currentTemplate.value))

// 弹框打开时：若当前选中模板被锁定，回退到第一个可访问的模板
watch(templateVisible, (open) => {
  if (!open) return
  const cur = currentTemplate.value
  if (cur && cur.accessible) {
    selectedTemplateKey.value = cur.key
    return
  }
  const firstAccessible = allTemplates.value.find(t => t.accessible)
  if (firstAccessible) selectedTemplateKey.value = firstAccessible.key
})

const selectTemplate = (t) => {
  if (!t.accessible) {
    message.info('该模板当前套餐不可用，请升级套餐后使用')
    return
  }
  selectedTemplateKey.value = t.key
}

const applyTemplate = () => {
  templateVisible.value = false
}
</script>

<style scoped>
/* 模板选择 */
:deep(.template-lib-modal .ant-modal-content) {
  width: 960px;
}

.template-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: #fff8e6;
  border: 1px solid #ffe58f;
  color: #874d00;
  font-size: 13px;
  line-height: 1.5;
}

.template-notice-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #faad14;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  font-style: italic;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

body[data-theme="dark"] .template-notice {
  background: rgba(250, 173, 20, 0.12);
  border-color: rgba(250, 173, 20, 0.35);
  color: #ffd666;
}

body[data-theme="dark"] .template-notice-icon {
  background: #faad14;
}

.template-tabs {
  display: flex;
  gap: 8px;
  padding: 0 0 14px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  overflow-x: auto;
}

.template-tab {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.template-tab.active {
  border-color: #ff2442;
  background: #fff0f2;
  color: #ff2442;
  font-weight: 600;
}

.template-body {
  display: flex;
  gap: 16px;
}

.template-preview-pane {
  flex: 0 0 420px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
  height: 480px;
  box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05);
}

.template-list-pane {
  flex: 1;
  min-width: 0;
  height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.template-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.template-row:hover {
  border-color: #ff2442;
  background: #fff0f2;
}

.template-row.selected {
  border-color: #ff2442;
  background: #fff0f2;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.25);
}

.template-row.locked {
  opacity: 0.55;
  cursor: not-allowed;
  background: #f5f5f5;
}

.template-row.locked:hover {
  border-color: #e8e8e8;
  background: #f5f5f5;
}

.template-row-badge {
  position: absolute;
  top: 4px;
  right: 6px;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
  pointer-events: none;
}

.template-row-badge.locked {
  color: #fff;
  background: linear-gradient(135deg, #ff9a4d, #ff2442);
}

.template-row-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
  margin-bottom: 2px;
}

.template-row-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.template-footer {
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.template-apply-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: #ff2442;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.template-apply-btn:hover {
  background: #e61e3a;
}


/* 模板弹框内行 */
body[data-theme="dark"] .template-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .template-tab {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .template-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .template-tab.active {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .template-preview-pane {
  background: #141414;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}

body[data-theme="dark"] .template-row {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .template-row:hover,
body[data-theme="dark"] .template-row.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  box-shadow: none;
}

body[data-theme="dark"] .template-row.locked {
  background: #2a2a2a;
  border-color: #303030;
  opacity: 0.45;
}

body[data-theme="dark"] .template-row.locked:hover {
  background: #2a2a2a;
  border-color: #303030;
}

body[data-theme="dark"] .template-row-name {
  color: #f0f0f0;
}

body[data-theme="dark"] .template-row-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .template-group-title {
  color: #a6a6a6;
}

body[data-theme="dark"] .template-footer {
  border-top-color: #303030;
}
</style>

<style>
@media (max-width: 768px) {
  .template-modal-wrap {
    display: flex !important;
    flex-direction: column !important;
    align-items: flex-start !important;
    justify-content: flex-start !important;
  }

  .template-modal-wrap .template-modal,
  .template-modal-wrap.ant-modal-centered .template-modal {
    top: 0 !important;
    margin: 0 auto !important;
    width: 100% !important;
    max-width: 100vw !important;
    height: 100vh !important;
    max-height: 100vh !important;
    display: flex !important;
    flex-direction: column !important;
  }

  .template-modal .ant-modal-content {
    border-radius: 0;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .template-modal .ant-modal-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 16px;
  }

  .template-modal .template-tabs {
    flex-shrink: 0;
    padding-bottom: 10px;
    margin-bottom: 12px;
  }

  .template-modal .template-body {
    flex: 1;
    flex-direction: column;
    gap: 12px;
    overflow: hidden;
    min-height: 0;
  }

  /* 选择器放在上方，横向滚动 */
  .template-modal .template-list-pane {
    order: 1;
    display: flex;
    gap: 10px;
    height: auto;
    max-height: 130px;
    overflow-x: auto;
    overflow-y: hidden;
    padding-right: 0;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .template-modal .template-list-pane::-webkit-scrollbar {
    display: none;
  }

  .template-modal .template-row {
    flex: 0 0 132px;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 12px;
    margin-bottom: 0;
  }

  .template-modal .template-row-name {
    font-size: 13px;
  }

  .template-modal .template-row-desc {
    font-size: 11px;
    line-height: 1.5;
  }

  /* 预览区放在下方，占据剩余空间 */
  .template-modal .template-preview-pane {
    order: 2;
    flex: 1;
    width: 100%;
    height: auto;
    max-height: 45vh;
    min-height: 200px;
    overflow-y: auto;
  }

  .template-modal .template-footer {
    order: 3;
    flex-shrink: 0;
    margin-top: 12px;
    padding: 12px 0 0;
    border-top: 1px solid #f0f0f0;
  }

  .template-modal .template-apply-btn {
    width: 100%;
  }
}

body[data-theme="dark"] .template-modal .template-footer {
  border-top-color: #303030;
}
</style>

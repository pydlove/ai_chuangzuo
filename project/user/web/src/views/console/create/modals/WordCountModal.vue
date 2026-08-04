<template>
  <a-modal
    v-model:open="wordCountVisible"
    :footer="null"
    :width="640"
    centered
    class="word-count-modal"
  >
    <template #title>
      <div class="modal-title-wrap">
        <div class="modal-title">设置文章字数</div>
        <div class="modal-subtitle">选择合适的字数，让 AI 写出更精准的内容</div>
      </div>
    </template>

    <div class="wc-tabs">
      <button
        v-for="tab in wordCountTabs"
        :key="tab.key"
        :class="['wc-tab', { active: wordCountTab === tab.key }]"
        @click="wordCountTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="wc-content">
      <!-- 按平台 -->
      <div v-if="wordCountTab === 'platform'" class="wc-grid">
        <div
          v-for="wc in platformWordCounts"
          :key="wc.count"
          :class="['wc-item', { selected: currentWordCount.count === wc.count, disabled: isLocked(wc.count) }]"
          @click="selectWordCount(wc)"
        >
          <div v-if="getWordCountBadge(wc.count)" :class="['wc-badge', getWordCountBadge(wc.count).tier]">{{ getWordCountBadge(wc.count).text }}</div>
          <div class="wc-count">{{ wc.count }} 字</div>
          <div class="wc-label">{{ wc.label }}</div>
        </div>
      </div>

      <!-- 按场景 -->
      <div v-else-if="wordCountTab === 'scenario'" class="wc-list">
        <div
          v-for="s in wordCountPresets.scenario"
          :key="s.count"
          :class="['wc-item-wide', { selected: currentWordCount.count === s.count, disabled: isLocked(s.count) }]"
          @click="selectWordCount(s)"
        >
          <div v-if="getWordCountBadge(s.count)" :class="['wc-badge', getWordCountBadge(s.count).tier]">{{ getWordCountBadge(s.count).text }}</div>
          <div class="wc-item-left">
            <div class="wc-count">{{ s.count }} 字</div>
            <div class="wc-label">{{ s.label }}</div>
          </div>
          <div class="wc-desc">{{ s.desc }}</div>
        </div>
      </div>

      <!-- 按档位 -->
      <div v-else-if="wordCountTab === 'tier'" class="wc-list">
        <div
          v-for="t in wordCountPresets.tier"
          :key="t.count"
          :class="['wc-item-wide', { selected: currentWordCount.count === t.count, disabled: isLocked(t.count) }]"
          @click="selectWordCount(t)"
        >
          <div v-if="getWordCountBadge(t.count)" :class="['wc-badge', getWordCountBadge(t.count).tier]">{{ getWordCountBadge(t.count).text }}</div>
          <div class="wc-item-left">
            <div class="wc-count">{{ t.count }} 字</div>
            <div class="wc-label">{{ t.label }}</div>
          </div>
          <div class="wc-desc">{{ t.desc }}</div>
        </div>
      </div>

      <!-- 自定义 -->
      <div v-else class="wc-custom">
        <div class="wc-custom-display">{{ localCustomWordCount }} 字</div>
        <input
          v-model="localCustomWordCount"
          type="number"
          class="wc-custom-input"
          min="1"
          :max="wordCountLimit"
          :placeholder="`输入 1-${wordCountLimit} 字`"
        />
        <input
          v-model="localCustomWordCount"
          type="range"
          class="wc-slider"
          min="1"
          :max="wordCountLimit"
        />
        <div class="wc-custom-hint">AI 将生成约 {{ localCustomWordCount }} 字的文章（当前套餐上限 {{ wordCountLimit }} 字）</div>

        <div class="wc-footer">
          <button class="wc-footer-btn ghost" @click="cancelCustom">取消</button>
          <button class="wc-footer-btn primary" @click="confirmCustom" :disabled="!isCustomValid">
            确定
          </button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { wordCountPresets, useCreateForm } from '../useCreateForm.js'
import { getWordCountLimit, getWordCountBadge } from '@/utils/membershipLimits.js'

const { wordCountVisible, currentPlatform, currentWordCount, customWordCount } = useCreateForm()

const wordCountTab = ref('tier')
const wordCountTabs = [
  { key: 'platform', label: '平台' },
  { key: 'scenario', label: '场景' },
  { key: 'tier', label: '档位' },
  { key: 'custom', label: '自定义' }
]

const wordCountLimit = computed(() => getWordCountLimit())

// 自定义字数的本地临时态：弹框打开时从 customWordCount 初始化，
// 只在点"确定"时才写回 customWordCount / currentWordCount。
const localCustomWordCount = ref(customWordCount.value)
const isCustomValid = computed(() => {
  const n = Number(localCustomWordCount.value)
  return Number.isFinite(n) && n >= 1 && n <= wordCountLimit.value
})

watch(wordCountVisible, (visible) => {
  if (visible) {
    // 打开时校正：超过上限就夹到上限
    const init = Math.max(1, Math.min(wordCountLimit.value, Number(customWordCount.value) || 1500))
    localCustomWordCount.value = init
  }
})

const platformWordCounts = computed(() => {
  const platform = currentPlatform.value?.key || 'wechat'
  return wordCountPresets.platform[platform] || wordCountPresets.platform.general
})

const isLocked = (count) => count > wordCountLimit.value

const selectWordCount = (wc) => {
  if (isLocked(wc.count)) {
    const badge = getWordCountBadge(wc.count)
    message.info(`该字数需要 ${badge?.text || '更高套餐'}，请升级套餐后使用`)
    return
  }
  currentWordCount.value = wc
  wordCountVisible.value = false
}

const confirmCustom = () => {
  if (!isCustomValid.value) return
  const count = Math.round(Number(localCustomWordCount.value))
  customWordCount.value = count
  currentWordCount.value = { count, label: '自定义', desc: '' }
  wordCountVisible.value = false
}

const cancelCustom = () => {
  wordCountVisible.value = false
}
</script>

<style scoped>
/* 字数选择 */
.wc-tabs {
  display: flex;
  gap: 6px;
  padding: 0 0 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.wc-tab {
  flex: 1;
  min-width: 0;
  padding: 8px 0;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wc-tab.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.wc-content {
  height: 300px;
  overflow-y: auto;
}

.wc-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.wc-item {
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.wc-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item,
.wc-item-wide {
  position: relative;
}

.wc-badge {
  position: absolute;
  top: 2px;
  right: 4px;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
  pointer-events: none;
}

.wc-badge.pro {
  color: #874d00;
  background: linear-gradient(135deg, #fff1b8, #ffd666);
}

.wc-badge.flagship {
  color: #fff;
  background: linear-gradient(135deg, #ffd591, #ff7a45);
}

.wc-item.disabled,
.wc-item-wide.disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background: #f5f5f5;
  border-color: #e8e8e8;
}

.wc-item.disabled:hover,
.wc-item-wide.disabled:hover {
  border-color: #e8e8e8;
  background: #f5f5f5;
}

.wc-count {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
  margin-top: 4px;
}

.wc-label {
  font-size: 12px;
  color: #8c8c8c;
}

.wc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wc-item-wide {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.wc-item-wide:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item-wide.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.wc-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.wc-custom {
  padding: 8px 4px;
}

.wc-custom-display {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
  text-align: center;
  margin: 16px 0;
}

.wc-custom-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 18px;
  text-align: center;
  box-sizing: border-box;
}

.wc-slider {
  width: 100%;
  margin-top: 16px;
  accent-color: var(--color-primary);
}

.wc-slider::-webkit-slider-thumb {
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--color-primary);
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.wc-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--color-primary);
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.wc-custom-hint {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 12px;
  text-align: center;
}

/* 底部操作按钮（自定义字数 tab 才需要确认，其它 tab 直接点选项即生效） */
.wc-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.wc-footer-btn {
  border: none;
  border-radius: 6px;
  font-size: 14px;
  padding: 8px 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.wc-footer-btn.ghost {
  background: #fff;
  border: 1px solid #d9d9d9;
  color: #595959;
}

.wc-footer-btn.ghost:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.wc-footer-btn.primary {
  background: var(--color-primary);
  color: #fff;
}

.wc-footer-btn.primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.wc-footer-btn.primary:disabled {
  background: #ffadd2;
  cursor: not-allowed;
}

/* 移动端：底部滑上全屏面板 */
@media (max-width: 768px) {
  :global(.word-count-modal .ant-modal) {
    width: 100% !important;
    max-width: 100%;
    margin: 0;
    top: auto !important;
    bottom: 0;
    transform: none !important;
    padding: 0;
  }

  :global(.word-count-modal .ant-modal-content) {
    border-radius: 20px 20px 0 0;
    height: 82vh;
    display: flex;
    flex-direction: column;
  }

  :global(.word-count-modal .ant-modal-header) {
    flex-shrink: 0;
    border-bottom: 1px solid #f0f0f0;
    padding: 16px 18px;
    border-radius: 20px 20px 0 0;
  }

  :global(.word-count-modal .ant-modal-body) {
    flex: 1;
    overflow: hidden;
    padding: 16px 18px calc(16px + env(safe-area-inset-bottom));
  }

  .wc-content {
    height: 100%;
  }

  .wc-tabs {
    padding-bottom: 12px;
  }

  .wc-tab {
    font-size: 13px;
    padding: 9px 2px;
    border-radius: 8px;
  }

  .wc-grid {
    gap: 12px;
  }

  .wc-item {
    padding: 16px 12px;
    border-radius: 12px;
  }

  .wc-item-wide {
    padding: 14px 12px;
    border-radius: 12px;
  }

  .wc-desc {
    max-width: 110px;
    text-align: right;
  }

  .wc-custom-display {
    font-size: 42px;
  }

  .wc-footer {
    margin-top: 24px;
  }

  .wc-footer-btn {
    flex: 1;
    padding: 12px 20px;
    border-radius: 10px;
  }
}

body[data-theme="dark"] .wc-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .wc-tab {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .wc-tab.active {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .wc-item,
body[data-theme="dark"] .wc-item-wide {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .wc-item.disabled,
body[data-theme="dark"] .wc-item-wide.disabled {
  background: #2a2a2a;
  border-color: #303030;
  opacity: 0.45;
}

body[data-theme="dark"] .wc-item:hover,
body[data-theme="dark"] .wc-item-wide:hover,
body[data-theme="dark"] .wc-item.selected,
body[data-theme="dark"] .wc-item-wide.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .wc-item.disabled:hover,
body[data-theme="dark"] .wc-item-wide.disabled:hover {
  background: #2a2a2a;
  border-color: #303030;
}

body[data-theme="dark"] .wc-count {
  color: #f0f0f0;
}

body[data-theme="dark"] .wc-label,
body[data-theme="dark"] .wc-desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .wc-custom-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .wc-custom-input:focus {
  border-color: var(--color-primary);
  outline: none;
}

body[data-theme="dark"] .wc-footer-btn.ghost {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .wc-footer-btn.primary:disabled {
  background: #5a2030;
}
</style>

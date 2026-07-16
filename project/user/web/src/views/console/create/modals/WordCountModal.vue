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
          :class="['wc-item', { selected: currentWordCount.count === wc.count }]"
          @click="selectWordCount(wc)"
        >
          <div class="wc-count">{{ wc.count }} 字</div>
          <div class="wc-label">{{ wc.label }}</div>
        </div>
      </div>

      <!-- 按场景 -->
      <div v-else-if="wordCountTab === 'scenario'" class="wc-list">
        <div
          v-for="s in wordCountPresets.scenario"
          :key="s.count"
          :class="['wc-item-wide', { selected: currentWordCount.count === s.count }]"
          @click="selectWordCount(s)"
        >
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
          :class="['wc-item-wide', { selected: currentWordCount.count === t.count }]"
          @click="selectWordCount(t)"
        >
          <div class="wc-item-left">
            <div class="wc-count">{{ t.count }} 字</div>
            <div class="wc-label">{{ t.label }}</div>
          </div>
          <div class="wc-desc">{{ t.desc }}</div>
        </div>
      </div>

      <!-- 自定义 -->
      <div v-else class="wc-custom">
        <div class="wc-custom-display">{{ customWordCount }} 字</div>
        <input
          v-model="customWordCount"
          type="number"
          class="wc-custom-input"
          min="1"
          max="3000"
          placeholder="输入 1-3000 字"
        />
        <input
          v-model="customWordCount"
          type="range"
          class="wc-slider"
          min="1"
          max="3000"
        />
        <div class="wc-custom-hint">AI 将生成约 {{ customWordCount }} 字的文章</div>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed } from 'vue'
import { wordCountPresets, useCreateForm } from '../useCreateForm.js'

const { wordCountVisible, currentPlatform, currentWordCount, customWordCount } = useCreateForm()

const wordCountTab = ref('tier')
const wordCountTabs = [
  { key: 'platform', label: '按平台推荐' },
  { key: 'scenario', label: '按内容场景' },
  { key: 'tier', label: '按字数档位' },
  { key: 'custom', label: '自定义字数' }
]

const platformWordCounts = computed(() => {
  const platform = currentPlatform.value?.key || 'wechat'
  return wordCountPresets.platform[platform] || wordCountPresets.platform.general
})

const selectWordCount = (wc) => {
  currentWordCount.value = wc
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
  overflow-x: auto;
}

.wc-tab {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
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

.wc-count {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
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

body[data-theme="dark"] .wc-item:hover,
body[data-theme="dark"] .wc-item-wide:hover,
body[data-theme="dark"] .wc-item.selected,
body[data-theme="dark"] .wc-item-wide.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
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
</style>

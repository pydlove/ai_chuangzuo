<template>
  <a-modal
    v-model:open="platformVisible"
    :footer="null"
    :width="560"
    centered
    :closable="true"
    class="platform-modal"
  >
    <template #title>
      <div class="modal-title-wrap">
        <div class="modal-title">选择发布平台</div>
        <div class="modal-subtitle">选择目标平台，AI 将按平台规则推荐模板、字数和标签</div>
      </div>
    </template>
    <div class="platform-grid">
      <div
        v-for="p in platforms"
        :key="p.key"
        :class="['platform-item', { selected: currentPlatform.key === p.key }]"
        @click="selectPlatform(p)"
      >
        <div class="platform-name">{{ p.name }}</div>
        <div class="platform-desc">{{ p.desc }}</div>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { platforms, useCreateForm } from '../useCreateForm.js'

const { platformVisible, currentPlatform } = useCreateForm()

const selectPlatform = (p) => {
  currentPlatform.value = p
  platformVisible.value = false
}
</script>

<style scoped>
/* 平台选择 */
.platform-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.platform-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.platform-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.platform-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.platform-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
}

.platform-desc {
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.5;
}

body[data-theme="dark"] .platform-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .platform-item:hover,
body[data-theme="dark"] .platform-item.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .platform-name {
  color: #f0f0f0;
}

body[data-theme="dark"] .platform-desc {
  color: #a6a6a6;
}
</style>

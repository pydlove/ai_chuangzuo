<template>
  <div :class="['tabs-wrap', { 'tabs-wrap--sticky': sticky }]">
    <div
      :class="[
        'tabs',
        `tabs--${variant}`,
        `tabs--active-${activeType}`,
        { 'tabs--equal': equalWidth, 'tabs--scrollable': scrollable, 'tabs--shape-pill': shape === 'pill' }
      ]"
      role="tablist"
    >
      <button
        v-for="tab in tabs"
        :key="String(tab.value)"
        type="button"
        role="tab"
        :aria-selected="modelValue === tab.value"
        :class="[
          'tabs__tab',
          { 'tabs__tab--active': modelValue === tab.value, 'tabs__tab--disabled': tab.disabled }
        ]"
        :disabled="tab.disabled"
        @click="onClick(tab)"
      >
        <span v-if="tab.dot" class="tabs__dot" />
        <span class="tabs__label">{{ tab.label }}</span>
        <span v-if="showCount(tab)" class="tabs__badge">{{ formatCount(tab.count) }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: [String, Number],
    required: true
  },
  tabs: {
    type: Array,
    default: () => []
  },
  variant: {
    type: String,
    default: 'pill',
    validator: (v) => ['pill', 'segment'].includes(v)
  },
  activeType: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'surface'].includes(v)
  },
  equalWidth: Boolean,
  scrollable: {
    type: Boolean,
    default: true
  },
  sticky: Boolean,
  shape: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'pill'].includes(v)
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'click'])

function onClick(tab) {
  if (tab.disabled) return
  emit('click', tab)
  if (tab.value === props.modelValue) return
  emit('update:modelValue', tab.value)
  emit('change', tab.value, tab)
}

function showCount(tab) {
  return tab.count != null && tab.count !== ''
}

function formatCount(count) {
  const n = Number(count)
  if (Number.isNaN(n)) return count
  return n > 99 ? '99+' : String(n)
}
</script>

<style scoped>
.tabs-wrap {
  width: 100%;
}

.tabs-wrap--sticky {
  position: sticky;
  top: 0;
  z-index: 40;
}

.tabs {
  display: flex;
  gap: 8px;
  width: 100%;
}

.tabs--scrollable {
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.tabs--scrollable::-webkit-scrollbar {
  display: none;
}

.tabs--equal .tabs__tab {
  flex: 1 1 0;
}

.tabs__tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  white-space: nowrap;
}

.tabs__tab--disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.tabs__label {
  line-height: 1.2;
}

.tabs__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.7;
}

.tabs__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
}

/* pill variant */
.tabs--pill .tabs__tab {
  flex-shrink: 0;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  background: #f5f5f5;
}

.tabs--pill .tabs__tab:hover:not(:disabled, .tabs__tab--active) {
  background: #e8e8e8;
}

.tabs--pill.tabs--active-primary .tabs__tab--active {
  color: #fff;
  background: var(--color-primary, #FF2442);
  font-weight: 600;
}

.tabs--pill.tabs--active-primary .tabs__tab--active .tabs__badge {
  color: var(--color-primary, #FF2442);
  background: rgba(255, 255, 255, 0.9);
}

.tabs--pill.tabs--active-surface .tabs__tab--active {
  color: var(--color-primary, #FF2442);
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  font-weight: 600;
}

.tabs--pill .tabs__badge {
  color: var(--color-primary, #FF2442);
  background: #fff;
}

.tabs--pill.tabs--active-surface .tabs__tab--active .tabs__badge {
  color: #fff;
  background: var(--color-primary, #FF2442);
}

/* segment variant */
.tabs--segment {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  background: #f5f5f5;
  border-radius: 12px;
  width: auto;
  max-width: 100%;
}

.tabs--segment.tabs--equal {
  display: flex;
  width: 100%;
}

@media (max-width: 768px) {
  .tabs--segment {
    display: flex;
    width: 100%;
  }
}

.tabs--segment.tabs--shape-pill {
  border-radius: 999px;
}

.tabs--segment .tabs__tab {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
}

.tabs--segment.tabs--shape-pill .tabs__tab {
  border-radius: 999px;
}

.tabs--segment.tabs--active-primary .tabs__tab--active {
  color: #fff;
  background: var(--color-primary, #FF2442);
  font-weight: 600;
}

.tabs--segment.tabs--active-surface .tabs__tab--active {
  color: #1a1a1a;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  font-weight: 600;
}

.tabs--segment .tabs__badge {
  color: var(--color-primary, #FF2442);
  background: #fff;
}

/* 暗色主题 */
body[data-theme="dark"] .tabs--pill .tabs__tab {
  color: #a6a6a6;
  background: #262626;
}

body[data-theme="dark"] .tabs--pill .tabs__tab:hover:not(:disabled, .tabs__tab--active) {
  background: #303030;
}

body[data-theme="dark"] .tabs--pill.tabs--active-primary .tabs__tab--active {
  background: var(--color-primary, #FF2442);
  color: #fff;
}

body[data-theme="dark"] .tabs--pill.tabs--active-surface .tabs__tab--active {
  background: #1f1f1f;
  color: #e0e0e0;
}

body[data-theme="dark"] .tabs--segment {
  background: #262626;
}

body[data-theme="dark"] .tabs--segment .tabs__tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .tabs--segment.tabs--active-primary .tabs__tab--active {
  background: var(--color-primary, #FF2442);
  color: #fff;
}

body[data-theme="dark"] .tabs--segment.tabs--active-surface .tabs__tab--active {
  background: #1f1f1f;
  color: #f5f5f5;
}
</style>

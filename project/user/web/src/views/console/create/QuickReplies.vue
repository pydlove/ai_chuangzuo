<template>
  <div class="quick-replies">
    <div class="quick-options">
      <button
        v-for="opt in options"
        :key="opt.key"
        :class="['quick-option', { selected: selected?.key === opt.key }]"
        :title="opt.label"
        @click="onPick(opt)"
      >
        <span v-if="opt.raw?.sourceType" :class="['chip-source', `chip-source-${opt.raw.sourceType}`]" :title="sourceLabel(opt.raw.sourceType)"></span>
        <span class="chip-label">{{ opt.label }}</span>
      </button>
    </div>
    <div v-if="selected" class="quick-preview">
      <slot name="preview" :option="selected" />
      <div class="quick-preview-actions">
        <button class="quick-confirm" @click="onConfirm">确认 ✓</button>
      </div>
    </div>
    <slot name="footer" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  options: { type: Array, default: () => [] },
  /** 外部选中的 key（响应式）；用户手动点 chip 也会同步到此 prop 之上 */
  selectedKey: { type: String, default: null }
})
const emit = defineEmits(['confirm', 'update:selectedKey'])
const selected = ref(null)

const SOURCE_LABEL = { my: '我的风格', learned: '学习风格', favorite: '收藏的风格', system: '系统预设' }
const sourceLabel = (t) => SOURCE_LABEL[t] || ''

// 外部传入 selectedKey（弹框应用后等场景）→ 同步内部选中态
watch(() => props.selectedKey, (k) => {
  if (!k) return
  const opt = props.options.find(o => o.key === k)
  if (opt && (!selected.value || selected.value.key !== k)) {
    selected.value = opt
  }
}, { immediate: true })

const onPick = (opt) => {
  selected.value = opt
  emit('select', opt)
}

const onConfirm = () => {
  if (!selected.value) return
  emit('confirm', selected.value)
}
</script>

<style scoped>
.quick-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--color-border-default);
  background: var(--color-bg-card);
  color: var(--color-text-regular);
  font-size: 13px;
  padding: 7px 14px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
  max-width: 220px;
}

.chip-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 来源色点：4 种来源对应 4 种颜色 */
.chip-source {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.chip-source-my { background: #ff2442; }
.chip-source-learned { background: #2f54eb; }
.chip-source-favorite { background: #fa8c16; }
.chip-source-system { background: #bfbfbf; }

.quick-option:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.quick-option.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 500;
}

.quick-preview {
  margin-top: 10px;
  border: 1px solid var(--color-primary-light);
  background: var(--color-primary-bg);
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-text-regular);
}

.quick-preview-actions {
  text-align: right;
  margin-top: 8px;
}

.quick-confirm {
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 18px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.quick-confirm:hover {
  background: var(--color-primary-hover);
}

@media (max-width: 768px) {
  .quick-options { flex-wrap: nowrap; overflow-x: auto; padding-bottom: 4px; }
  .quick-option { flex-shrink: 0; }
}
</style>

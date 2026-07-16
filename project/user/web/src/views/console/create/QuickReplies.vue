<template>
  <div class="quick-replies">
    <div class="quick-options">
      <button
        v-for="opt in options"
        :key="opt.key"
        :class="['quick-option', { selected: selected?.key === opt.key }]"
        @click="selected = opt"
      >
        {{ opt.label }}
      </button>
    </div>
    <div v-if="selected" class="quick-preview">
      <slot name="preview" :option="selected" />
      <div class="quick-preview-actions">
        <button class="quick-confirm" @click="onConfirm">确认 ✓</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({ options: { type: Array, default: () => [] } })
const emit = defineEmits(['confirm'])
const selected = ref(null)

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
  border: 1px solid var(--color-border-default);
  background: var(--color-bg-card);
  color: var(--color-text-regular);
  font-size: 13px;
  padding: 7px 14px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

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

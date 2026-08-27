<template>
  <div class="copy-row">
    <span v-if="label" class="copy-row__label">{{ label }}</span>
    <span class="copy-row__value">
      <slot name="value">{{ displayValue }}</slot>
    </span>
    <CopyButton
      :text="copyValue"
      :success-text="successText"
      :error-text="errorText"
      :empty-text="emptyText"
      :type="buttonType"
      :disabled="disabled"
      :label="buttonText"
      @success="$emit('success', $event)"
      @error="$emit('error', $event)"
    >
      <slot name="button" />
    </CopyButton>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import CopyButton from './CopyButton.vue'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  value: {
    type: [String, Number],
    default: ''
  },
  copyText: {
    type: [String, Number],
    default: ''
  },
  buttonText: {
    type: String,
    default: '复制'
  },
  successText: {
    type: String,
    default: '已复制'
  },
  errorText: {
    type: String,
    default: '复制失败'
  },
  emptyText: {
    type: String,
    default: '复制内容为空'
  },
  buttonType: {
    type: String,
    default: 'text'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['success', 'error'])

const displayValue = computed(() => {
  const v = props.value
  return v == null || v === '' ? '—' : String(v)
})

const copyValue = computed(() => {
  const v = props.copyText
  if (v != null && v !== '') return String(v)
  return props.value == null ? '' : String(props.value)
})
</script>

<style scoped>
.copy-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.copy-row__label {
  color: var(--color-text-secondary, #595959);
  font-size: 13px;
}

.copy-row__value {
  color: var(--color-text-primary, #262626);
  font-size: 13px;
  word-break: break-all;
}

body[data-theme="dark"] .copy-row__label {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .copy-row__value {
  color: rgba(255, 255, 255, 0.92);
}
</style>

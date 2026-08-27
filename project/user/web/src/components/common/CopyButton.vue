<template>
  <button
    type="button"
    :class="[
      'copy-button',
      `copy-button--${type}`,
      { 'copy-button--loading': loading, 'copy-button--disabled': disabled }
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <slot>
      <CopyOutlined v-if="type === 'icon'" />
      <span v-else>{{ label }}</span>
    </slot>
  </button>
</template>

<script setup>
import { CopyOutlined } from '@ant-design/icons-vue'
import { useCopy } from '@/composables/useCopy.js'

const props = defineProps({
  text: {
    type: [String, Function],
    required: true
  },
  label: {
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
  type: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'primary', 'text', 'icon'].includes(v)
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['success', 'error'])

const { loading, copy } = useCopy({
  successText: props.successText,
  errorText: props.errorText,
  emptyText: props.emptyText
})

const handleClick = async () => {
  if (loading.value || props.disabled) return
  try {
    await copy(props.text)
    emit('success')
  } catch (e) {
    emit('error', e)
  }
}
</script>

<style scoped>
.copy-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-primary, #ff2442);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-button--default {
  border-color: var(--color-primary, #ff2442);
  background: #fff;
}

.copy-button--default:hover:not(:disabled) {
  background: var(--color-primary-bg, rgba(255, 36, 66, 0.08));
}

.copy-button--primary {
  border-color: var(--color-primary, #ff2442);
  background: var(--color-primary, #ff2442);
  color: #fff;
}

.copy-button--primary:hover:not(:disabled) {
  background: #e61e3a;
  border-color: #e61e3a;
}

.copy-button--text {
  padding: 0;
  color: var(--color-primary, #ff2442);
}

.copy-button--text:hover:not(:disabled) {
  opacity: 0.85;
}

.copy-button--icon {
  padding: 4px;
  color: var(--color-text-tertiary, #bfbfbf);
}

.copy-button--icon:hover:not(:disabled) {
  color: var(--color-primary, #ff2442);
}

.copy-button--loading {
  opacity: 0.7;
  cursor: wait;
}

.copy-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

body[data-theme="dark"] .copy-button--default {
  background: transparent;
}

body[data-theme="dark"] .copy-button--primary {
  background: var(--color-primary, #ff2442);
}
</style>

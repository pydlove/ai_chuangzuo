<template>
  <div
    :class="[
      'action-group',
      `action-group--${variant}`,
      `action-group--${size}`,
      { 'action-group--vertical': vertical, 'action-group--wrap': wrap }
    ]"
  >
    <component
      :is="action.tag || 'button'"
      v-for="(action, idx) in visibleActions"
      :key="idx"
      :class="[
        'action-group__btn',
        `action-group__btn--${action.type || 'default'}`,
        action.class,
        { 'action-group__btn--disabled': action.disabled }
      ]"
      :disabled="action.disabled"
      :title="action.title"
      @click.stop="handleClick(action, $event)"
    >
      {{ action.label }}
      <span v-if="action.badge" :class="['action-group__badge', action.badge.class]">
        {{ action.badge.text }}
      </span>
    </component>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  actions: { type: Array, default: () => [] },
  variant: { type: String, default: 'default' },
  size: { type: String, default: 'default' },
  vertical: Boolean,
  wrap: { type: Boolean, default: true }
})

const emit = defineEmits(['action'])

const visibleActions = computed(() => props.actions.filter(a => a.visible !== false))

const handleClick = (action, event) => {
  if (action.disabled) return
  emit('action', action, event)
  if (typeof action.handler === 'function') {
    action.handler(event)
  }
}
</script>

<style scoped>
.action-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.action-group--wrap {
  flex-wrap: wrap;
}

.action-group--vertical {
  flex-direction: column;
  align-items: stretch;
}

.action-group--small {
  gap: 6px;
}

.action-group--large {
  gap: 12px;
}

.action-group__btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.4;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.action-group__btn:hover:not(:disabled) {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.action-group__btn--primary {
  background: #fff;
  color: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 600;
}

.action-group__btn--primary:hover:not(:disabled) {
  background: var(--color-primary-bg);
}

.action-group__btn--danger {
  color: #ff4d4f;
}

.action-group__btn--danger:hover:not(:disabled) {
  color: #ff4d4f;
  background: #fff1f0;
}

.action-group__btn--success {
  background: #f6ffed;
  color: #52c41a;
  border-color: #b7eb8f;
}

.action-group__btn--success:hover:not(:disabled) {
  background: #d9f7be;
}

.action-group__btn--active {
  color: var(--color-primary);
}

.action-group__btn--active:hover:not(:disabled) {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.action-group__btn:disabled,
.action-group__btn--disabled {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #bfbfbf;
  cursor: not-allowed;
}

.action-group__badge {
  position: absolute;
  top: -8px;
  right: -6px;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
  pointer-events: none;
  white-space: nowrap;
}

.action-group__badge.pro {
  color: #874d00;
  background: linear-gradient(135deg, #fff1b8, #ffd666);
}

.action-group__badge.flagship {
  color: #fff;
  background: linear-gradient(135deg, #ffd591, #ff7a45);
}

/* link 变体：轻量链接风格 */
.action-group--link {
  gap: 4px;
}

.action-group--link .action-group__btn {
  padding: 0 4px;
  background: transparent;
  border: none;
  border-radius: 0;
  color: var(--color-primary);
  font-size: 14px;
}

.action-group--link .action-group__btn:hover:not(:disabled) {
  color: var(--color-primary-hover);
  background: transparent;
  text-decoration: underline;
}

.action-group--link .action-group__btn--danger {
  color: #ff4d4f;
}

.action-group--link .action-group__btn--danger:hover:not(:disabled) {
  color: #ff7875;
  background: transparent;
}

.action-group--link .action-group__btn:disabled,
.action-group--link .action-group__btn--disabled {
  background: transparent;
  color: #bfbfbf;
}

/* bar 变体：底部浮动操作栏 */
.action-group--bar {
  gap: 12px;
}

.action-group--bar .action-group__btn {
  flex: 1;
  padding: 12px 20px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  background: #fff;
  color: #262626;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.action-group--bar .action-group__btn:hover:not(:disabled) {
  background: #fafafa;
  color: #262626;
}

.action-group--bar .action-group__btn--primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.action-group--bar .action-group__btn--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.action-group--bar .action-group__btn--outline {
  background: transparent;
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.action-group--bar .action-group__btn--outline:hover:not(:disabled) {
  background: var(--color-primary-bg);
}

.action-group--bar .action-group__btn--disabled,
.action-group--bar .action-group__btn:disabled {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #bfbfbf;
  cursor: not-allowed;
}

body[data-theme="dark"] .action-group__btn {
  background: transparent;
  border-color: transparent;
  color: #a6a6a6;
}

body[data-theme="dark"] .action-group__btn:hover:not(:disabled) {
  border-color: transparent;
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .action-group__btn--primary {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .action-group__btn--primary:hover:not(:disabled) {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .action-group__btn--danger {
  color: #ff7875;
}

body[data-theme="dark"] .action-group__btn--danger:hover:not(:disabled) {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
}

body[data-theme="dark"] .action-group__btn--success {
  background: rgba(7, 193, 96, 0.15);
  border-color: rgba(7, 193, 96, 0.4);
  color: #4ade80;
}

body[data-theme="dark"] .action-group__btn--success:hover:not(:disabled) {
  background: rgba(7, 193, 96, 0.25);
  border-color: #4ade80;
}

body[data-theme="dark"] .action-group__btn--active {
  color: #ff6b81;
}

body[data-theme="dark"] .action-group__btn--active:hover:not(:disabled) {
  color: #ff6b81;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .action-group__btn:disabled,
body[data-theme="dark"] .action-group__btn--disabled {
  background: rgba(255, 255, 255, 0.04);
  border-color: #434343;
  color: #595959;
}

body[data-theme="dark"] .action-group__badge.pro {
  color: #fff;
  background: linear-gradient(135deg, #d48806, #fa8c16);
}

body[data-theme="dark"] .action-group__badge.flagship {
  color: #fff;
  background: linear-gradient(135deg, #fa8c16, #ff4d4f);
}

body[data-theme="dark"] .action-group--bar .action-group__btn {
  background: #2c2c2c;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .action-group--bar .action-group__btn:hover:not(:disabled) {
  background: #363636;
  color: #d9d9d9;
}

body[data-theme="dark"] .action-group--bar .action-group__btn--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .action-group--bar .action-group__btn--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .action-group--bar .action-group__btn--outline {
  background: transparent;
  color: var(--color-primary);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .action-group--bar .action-group__btn--outline:hover:not(:disabled) {
  background: rgba(255, 36, 66, 0.12);
}

@media (max-width: 768px) {
  .action-group--bar {
    gap: 8px;
  }

  .action-group--bar .action-group__btn {
    padding: 10px 14px;
    font-size: 13px;
  }
}
</style>

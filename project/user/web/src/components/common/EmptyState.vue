<template>
  <div class="empty-state" :class="[`size-${size}`, { compact: compact || !hasVisual }]">
    <div v-if="$slots.icon || icon" class="empty-state-icon">
      <slot name="icon">
        <component :is="icon" v-if="isComponentIcon" />
        <span v-else-if="isEmoji" class="empty-state-emoji">{{ icon }}</span>
        <InboxOutlined v-else />
      </slot>
    </div>

    <div v-if="title || $slots.title" class="empty-state-title">
      <slot name="title">{{ title }}</slot>
    </div>

    <div v-if="description || $slots.description" class="empty-state-description">
      <slot name="description">{{ description }}</slot>
    </div>

    <div v-if="actionText || $slots.action" class="empty-state-action">
      <slot name="action">
        <button class="empty-btn" @click="handleAction">{{ actionText }}</button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { InboxOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  icon: {
    type: [String, Object, Function],
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  actionText: {
    type: String,
    default: ''
  },
  actionTo: {
    type: String,
    default: ''
  },
  actionHandler: {
    type: Function,
    default: null
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['action'])

const router = useRouter()

const isComponentIcon = computed(() => {
  return typeof props.icon === 'object' || typeof props.icon === 'function'
})

const isEmoji = computed(() => {
  if (typeof props.icon !== 'string') return false
  // 简单判断：长度 1-2 且包含非 ASCII 字符
  return props.icon.length <= 2 && /[^\x00-\x7F]/.test(props.icon)
})

const hasVisual = computed(() => {
  return props.icon || props.title || props.description
})

function handleAction() {
  emit('action')
  if (props.actionHandler) {
    props.actionHandler()
    return
  }
  if (props.actionTo) {
    router.push(props.actionTo)
  }
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px 16px;
  color: var(--color-text-secondary, #8c8c8c);
}

.empty-state.compact {
  padding: 16px;
}

.empty-state-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary, #bfbfbf);
  margin-bottom: 12px;
}

.size-lg .empty-state-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.size-md .empty-state-icon {
  font-size: 44px;
}

.size-sm .empty-state-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.empty-state-emoji {
  font-size: inherit;
  line-height: 1;
}

.empty-state-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary, #262626);
  margin-bottom: 6px;
}

.size-lg .empty-state-title {
  font-size: 17px;
}

.size-sm .empty-state-title {
  font-size: 14px;
}

.empty-state-description {
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary, #8c8c8c);
  max-width: 280px;
  margin-bottom: 16px;
}

.size-sm .empty-state-description {
  font-size: 12px;
  margin-bottom: 10px;
}

.empty-state.compact .empty-state-description {
  margin-bottom: 0;
}

.empty-state-action {
  display: flex;
  gap: 12px;
}

.empty-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 18px;
  border-radius: 17px;
  border: 1px solid var(--color-primary, #07c160);
  background: transparent;
  color: var(--color-primary, #07c160);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.empty-btn:hover {
  background: var(--color-primary-light, rgba(7, 193, 96, 0.08));
}

.empty-btn:active {
  background: var(--color-primary-light, rgba(7, 193, 96, 0.12));
}

body[data-theme="dark"] .empty-state-icon {
  color: var(--color-text-tertiary, #555);
}

body[data-theme="dark"] .empty-state-title {
  color: var(--color-text-primary, #e0e0e0);
}
</style>

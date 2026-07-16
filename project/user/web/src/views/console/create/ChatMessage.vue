<template>
  <div class="chat-msg" :class="role">
    <div v-if="role === 'ai'" class="ai-info">
      <img class="chat-avatar" src="/ai-avatar.png" alt="AI" />
      <div class="ai-name">小爱</div>
    </div>
    <div class="chat-bubble">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({ role: { type: String, default: 'ai' } })
</script>

<style scoped>
.chat-msg {
  display: flex;
  margin-bottom: 16px;
  gap: 10px;
}

.chat-msg.user {
  justify-content: flex-end;
}

.ai-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.chat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.ai-name {
  font-size: 11px;
  color: var(--color-text-secondary);
  line-height: 1;
}

.chat-bubble {
  max-width: 85%;
  background: var(--color-bg-card);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-regular);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* 含输入框的气泡扩到 100%，避免被 85% 压缩到内容宽度 */
.chat-bubble:has(.topic-input-row) {
  max-width: 100%;
  width: 100%;
}

.chat-msg.user .chat-bubble {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-msg.ai .chat-bubble {
  border-bottom-left-radius: 4px;
}
</style>

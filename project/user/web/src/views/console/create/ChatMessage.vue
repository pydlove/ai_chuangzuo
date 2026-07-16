<template>
  <div class="chat-msg" :class="role">
    <div v-if="role === 'ai'" class="chat-avatar">AI</div>
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

.chat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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

.chat-msg.user .chat-bubble {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-msg.ai .chat-bubble {
  border-bottom-left-radius: 4px;
}
</style>

<template>
  <component
    :is="tag"
    :class="[
      'list-card',
      { 'list-card--clickable': clickable, 'list-card--hover': hover },
      customClass
    ]"
    @click="handleClick"
  >
    <div v-if="$slots.header" class="list-card__header">
      <slot name="header" />
    </div>
    <div v-if="$slots.body" class="list-card__body">
      <slot name="body" />
    </div>
    <div v-if="$slots.footer" class="list-card__footer">
      <slot name="footer" />
    </div>
    <slot />
  </component>
</template>

<script setup>
const props = defineProps({
  tag: { type: String, default: 'div' },
  clickable: Boolean,
  hover: { type: Boolean, default: true },
  customClass: { type: String, default: '' }
})

const emit = defineEmits(['click'])

const handleClick = (event) => {
  if (props.clickable) {
    emit('click', event)
  }
}
</script>

<style scoped>
.list-card {
  position: relative;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  padding: 16px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.2s;
}

.list-card--hover:hover,
.list-card--clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  border-color: var(--color-primary);
}

.list-card--clickable {
  cursor: pointer;
}

.list-card__header,
.list-card__body,
.list-card__footer {
  display: flex;
  align-items: center;
}

.list-card__header {
  margin-bottom: 12px;
}

.list-card__body {
  flex: 1;
  margin-bottom: 12px;
}

.list-card__footer {
  margin-top: auto;
}

body[data-theme="dark"] .list-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .list-card--hover:hover,
body[data-theme="dark"] .list-card--clickable:hover {
  border-color: var(--color-primary);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

@media (max-width: 768px) {
  .list-card {
    padding: 12px;
    border-radius: 12px;
  }
}
</style>

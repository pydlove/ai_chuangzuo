<template>
  <div
    :class="[
      'skill-card',
      `skill-card--${size}`,
      { 'skill-card--selected': selected, 'skill-card--clickable': clickable }
    ]"
    @click="clickable && $emit('click', $event)"
  >
    <div class="skill-card__head">
      <div
        v-if="showAvatar"
        :class="['skill-card__avatar', `skill-card__avatar--${avatarVariant}`]"
      >
        {{ displayAvatar }}
      </div>
      <div class="skill-card__title-wrap">
        <div class="skill-card__title-row">
          <div class="skill-card__title">{{ name }}</div>
          <slot name="status" />
        </div>
        <div v-if="$slots.meta" class="skill-card__meta">
          <slot name="meta" />
        </div>
      </div>
    </div>

    <div v-if="scopeTags.length" class="skill-card__scope-list">
      <span v-for="tag in scopeTags" :key="tag" class="skill-card__scope">{{ tag }}</span>
    </div>

    <div
      v-if="!expanded"
      :class="['skill-card__prompt', { 'skill-card__prompt--desc': desc }]"
      :title="desc || undefined"
    >
      <template v-if="desc">{{ desc }}</template>
      <template v-else>{{ summary }}</template>
    </div>
    <div v-else class="skill-card__prompt skill-card__prompt--full">{{ prompt }}</div>

    <div v-if="$slots.extra" class="skill-card__extra">
      <slot name="extra" />
    </div>

    <div class="skill-card__footer">
      <slot name="footer">
        <button v-if="showViewBtn" class="skill-card__action-btn" @click.stop="$emit('view')">
          查看完整提示词
        </button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  prompt: { type: String, default: '' },
  desc: { type: String, default: '' },
  scope: { type: String, default: '' },
  avatar: { type: String, default: '' },
  showAvatar: { type: Boolean, default: true },
  selected: Boolean,
  clickable: Boolean,
  expanded: Boolean,
  size: { type: String, default: 'default' },
  avatarVariant: { type: String, default: 'default' },
  showViewBtn: Boolean,
  maxSummaryLength: { type: Number, default: 60 }
})

const emit = defineEmits(['click', 'view'])

const displayAvatar = computed(() => (props.avatar || props.name || '?').charAt(0))

const scopeTags = computed(() => {
  if (!props.scope) return []
  return props.scope.split(/[,，]/).map(t => t.trim()).filter(Boolean)
})

const summary = computed(() => {
  const p = props.prompt || ''
  return p.length > props.maxSummaryLength ? p.slice(0, props.maxSummaryLength) + '...' : p
})
</script>

<style scoped>
.skill-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.2s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.skill-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
}

.skill-card--default {
  padding: 20px;
}

.skill-card--compact {
  padding: 16px;
  min-height: 200px;
  cursor: pointer;
}

.skill-card--clickable:hover {
  border-color: var(--color-primary);
}

.skill-card--selected {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
}

.skill-card__head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.skill-card--compact .skill-card__head {
  gap: 12px;
  margin-bottom: 12px;
}

.skill-card__avatar {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #fff0f2;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.skill-card--compact .skill-card__avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  font-size: 16px;
}

.skill-card__avatar--learned {
  background: #fff5f7;
}

.skill-card__title-wrap {
  flex: 1;
  min-width: 0;
}

.skill-card__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.skill-card--compact .skill-card__title-row {
  margin-bottom: 4px;
}

.skill-card__title {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.35;
  word-break: break-all;
}

.skill-card--compact .skill-card__title {
  font-size: 15px;
}

.skill-card__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
}

.skill-card__scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.skill-card--compact .skill-card__scope-list {
  gap: 6px;
  margin-bottom: 10px;
}

.skill-card__scope {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  font-size: 12px;
  color: var(--color-primary);
  background: #fff5f7;
  border: 1px solid #ffd1d9;
  padding: 3px 10px;
  border-radius: 6px;
}

.skill-card--compact .skill-card__scope {
  font-size: 11px;
  padding: 2px 8px;
}

.skill-card__scope::before {
  content: '#';
  opacity: 0.8;
}

.skill-card__prompt {
  font-size: 14px;
  color: #262626;
  line-height: 1.7;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1 0 auto;
  white-space: pre-line;
}

.skill-card--compact .skill-card__prompt {
  font-size: 13px;
  margin-bottom: 12px;
}

.skill-card__prompt--full {
  background: #fafafa;
  border-radius: 12px;
  padding: 14px 16px;
  display: block;
  -webkit-line-clamp: unset;
  overflow: auto;
}

.skill-card__prompt--desc {
  color: #595959;
  -webkit-line-clamp: 2;
}

.skill-card__footer {
  margin-top: auto;
  padding-top: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.skill-card__extra {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.skill-card__action-btn {
  padding: 4px 8px;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.2s;
}

.skill-card__action-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

body[data-theme="dark"] .skill-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .skill-card:hover {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .skill-card--selected {
  background: rgba(255, 36, 66, 0.12);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .skill-card__avatar {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .skill-card__title {
  color: #f0f0f0;
}

body[data-theme="dark"] .skill-card__meta {
  color: #a6a6a6;
}

body[data-theme="dark"] .skill-card__scope {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.25);
  color: #ff6b81;
}

body[data-theme="dark"] .skill-card__prompt {
  background: transparent;
  color: #d9d9d9;
}

body[data-theme="dark"] .skill-card__prompt--desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .skill-card__prompt--full {
  background: #141414;
  color: #d9d9d9;
  border: 1px solid #303030;
}

body[data-theme="dark"] .skill-card__action-btn {
  background: transparent;
  border-color: transparent;
  color: #a6a6a6;
}

body[data-theme="dark"] .skill-card__action-btn:hover {
  border-color: transparent;
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}
</style>

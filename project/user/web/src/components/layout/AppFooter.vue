<template>
  <footer class="app-footer" :class="`app-footer--${variant}`">
    <template v-if="variant === 'desktop'">
      <span>{{ lineOne }}</span>
      <span>{{ icp }}</span>
    </template>
    <template v-else>
      <div>{{ lineOne }}</div>
      <div>{{ icp }}</div>
    </template>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import { siteFooter } from '@/data/siteConfig.js'

const props = defineProps({
  variant: { type: String, default: 'desktop' },
  showRights: { type: Boolean, default: true },
  copyright: { type: String, default: siteFooter.copyright },
  rights: { type: String, default: siteFooter.rights },
  icp: { type: String, default: siteFooter.icp }
})

const lineOne = computed(() => {
  if (props.variant === 'mobile' || !props.showRights || !props.rights) {
    return props.copyright
  }
  return `${props.copyright} · ${props.rights}`
})
</script>

<style scoped>
.app-footer {
  box-sizing: border-box;
}

.app-footer--desktop {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}

.app-footer--desktop span {
  display: inline-block;
}

.app-footer--desktop span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}

.app-footer--mobile {
  padding: 24px 20px 32px;
  border-top: 1px solid #f0f0f0;
  color: #8c8c8c;
  font-size: 12px;
  text-align: center;
  line-height: 1.8;
  background: #fff;
}

body[data-theme="dark"] .app-footer--desktop {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .app-footer--desktop span + span::before {
  color: #303030;
}

body[data-theme="dark"] .app-footer--mobile {
  background: #1f1f1f;
  border-top-color: #2a2a2a;
}
</style>

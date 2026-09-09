<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 会员等级：basic=VIP、pro=SVIP、flagship=SSVIP；非会员传空。 */
  level: { type: String, default: '' },
  /** 徽标高度（px），宽度按图标比例自适应。 */
  size: { type: Number, default: 16 }
})

const ICONS = {
  basic: '/assets/images/vip/VIP.svg',
  pro: '/assets/images/vip/SVIP.svg',
  flagship: '/assets/images/vip/SSVIP.svg'
}

const src = computed(() => ICONS[(props.level || '').toLowerCase()] || '')
</script>

<template>
  <img v-if="src" class="member-v-badge" :src="src" alt="会员标识" :style="{ height: size + 'px' }" />
</template>

<style scoped>
.member-v-badge {
  display: block;
  width: auto;
  object-fit: contain;
  pointer-events: none;
}
</style>

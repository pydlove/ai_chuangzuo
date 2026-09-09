<script setup>
/**
 * 收益榜 TOP3 头像花环装饰。
 * 参考风格：金色花环 + 双层花瓣 + 光晕 + 飘带高光 + 藤蔓卷须 + 星光。
 * variant 区分名次配色：gold=冠军、silver=亚军、bronze=季军。
 */
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'gold',
    validator: v => ['gold', 'silver', 'bronze'].includes(v)
  }
})

const PALETTES = {
  gold: {
    halo: '#f2cd6b',
    ring1: '#f8e08e', ring2: '#e5ae33', ring3: '#c78f17',
    ribbon1: '#f8e08e', ribbon2: '#dfa92e',
    petal: '#ffe9ad', petalInner: '#fff6d8', petalStroke: '#e0aa3a', core: '#e79a2e',
    leaf: '#d9a94e', bead: '#f2cd6b', spark: '#fff0b8', lantern: '#ffdf8e',
    glow: 'rgba(240, 201, 100, 0.45)'
  },
  silver: {
    halo: '#c9ced8',
    ring1: '#f4f6f9', ring2: '#b9bec9', ring3: '#959cad',
    ribbon1: '#f0f2f6', ribbon2: '#c2c7d1',
    petal: '#ffffff', petalInner: '#f1f3f7', petalStroke: '#a3a9b6', core: '#a9aeba',
    leaf: '#c6cbd5', bead: '#d6dae2', spark: '#ffffff', lantern: '#eceff4',
    glow: 'rgba(190, 196, 208, 0.45)'
  },
  bronze: {
    halo: '#e8bd84',
    ring1: '#f6d3a0', ring2: '#cf9048', ring3: '#9e5f28',
    ribbon1: '#f0c584', ribbon2: '#c07e38',
    petal: '#f6c98d', petalInner: '#fce9cd', petalStroke: '#a96c30', core: '#b26a2c',
    leaf: '#c08a52', bead: '#e0aa6c', spark: '#ffe0ae', lantern: '#f4c386',
    glow: 'rgba(214, 158, 94, 0.45)'
  }
}

const p = computed(() => PALETTES[props.variant] || PALETTES.gold)
const haloId = computed(() => `lw-halo-${props.variant}`)
const ringGradId = computed(() => `lw-ringg-${props.variant}`)
const ribbonGradId = computed(() => `lw-ribbong-${props.variant}`)
const haloUrl = computed(() => `url(#${haloId.value})`)
const ringGradUrl = computed(() => `url(#${ringGradId.value})`)
const ribbonGradUrl = computed(() => `url(#${ribbonGradId.value})`)
</script>

<template>
  <svg
    class="leaderboard-wreath"
    :class="'leaderboard-wreath--' + variant"
    viewBox="0 0 120 120"
    aria-hidden="true"
  >
    <defs>
      <!-- 每实例 id 带 variant 前缀，避免页面上三个组件的 defs id 冲突 -->
      <!-- 光晕：只在花环环带发光，圆心透明，避免罩住头像 -->
      <radialGradient :id="haloId" cx="50%" cy="50%" r="50%">
        <stop offset="0%" :stop-color="p.halo" stop-opacity="0" />
        <stop offset="66%" :stop-color="p.halo" stop-opacity="0" />
        <stop offset="86%" :stop-color="p.halo" stop-opacity="0.5" />
        <stop offset="100%" :stop-color="p.halo" stop-opacity="0" />
      </radialGradient>
      <linearGradient :id="ringGradId" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" :stop-color="p.ring1" />
        <stop offset="55%" :stop-color="p.ring2" />
        <stop offset="100%" :stop-color="p.ring3" />
      </linearGradient>
      <linearGradient :id="ribbonGradId" x1="0" y1="0" x2="1" y2="0.4">
        <stop offset="0%" :stop-color="p.ribbon1" />
        <stop offset="100%" :stop-color="p.ribbon2" />
      </linearGradient>

      <g id="lw-flower">
        <!-- 外层花瓣 -->
        <ellipse cx="0" cy="-7.2" rx="3.4" ry="6.2" />
        <ellipse cx="0" cy="-7.2" rx="3.4" ry="6.2" transform="rotate(72)" />
        <ellipse cx="0" cy="-7.2" rx="3.4" ry="6.2" transform="rotate(144)" />
        <ellipse cx="0" cy="-7.2" rx="3.4" ry="6.2" transform="rotate(216)" />
        <ellipse cx="0" cy="-7.2" rx="3.4" ry="6.2" transform="rotate(288)" />
        <!-- 内层花瓣（错角、浅色，叠出层次） -->
        <g style="fill: var(--lw-petal-inner)">
          <ellipse cx="0" cy="-4.6" rx="2.1" ry="3.9" transform="rotate(36)" />
          <ellipse cx="0" cy="-4.6" rx="2.1" ry="3.9" transform="rotate(108)" />
          <ellipse cx="0" cy="-4.6" rx="2.1" ry="3.9" transform="rotate(180)" />
          <ellipse cx="0" cy="-4.6" rx="2.1" ry="3.9" transform="rotate(252)" />
          <ellipse cx="0" cy="-4.6" rx="2.1" ry="3.9" transform="rotate(324)" />
        </g>
        <circle cx="0" cy="0" r="2.7" style="fill: var(--lw-core)" />
      </g>
      <path id="lw-spark" d="M0 -6.5 L1.7 -1.7 L6.5 0 L1.7 1.7 L0 6.5 L-1.7 1.7 L-6.5 0 L-1.7 -1.7 Z" />
      <g id="lw-leaf">
        <ellipse cx="2.7" cy="-4.8" rx="2" ry="4.6" transform="rotate(26 2.7 -4.8)" />
        <ellipse cx="-2.7" cy="-4.8" rx="2" ry="4.6" transform="rotate(-26 -2.7 -4.8)" />
      </g>
      <g id="lw-lantern">
        <line x1="0" y1="-8" x2="0" y2="-3.4" class="lw-line" />
        <ellipse cx="0" cy="2.2" rx="5.2" ry="6.2" style="fill: var(--lw-lantern)" />
        <ellipse cx="0" cy="2.2" rx="5.2" ry="6.2" class="lw-line-fill" />
        <line x1="0" y1="8.4" x2="0" y2="11.6" class="lw-line" />
        <circle cx="0" cy="12.8" r="1.3" style="fill: var(--lw-core)" />
      </g>
    </defs>

    <!-- 光晕 -->
    <circle class="lw-halo" cx="60" cy="60" r="57" :fill="haloUrl" />

    <!-- 飘带：渐变主带 + 丝质高光 -->
    <path class="lw-ribbon" d="M8 84 C 26 110, 70 116, 104 88 S 122 62, 112 52" :stroke="ribbonGradUrl" />
    <path class="lw-ribbon-sheen" d="M12 82 C 30 104, 68 110, 98 86" />
    <path class="lw-ribbon lw-ribbon--thin" d="M14 32 C 18 12, 42 2, 68 8" :stroke="ribbonGradUrl" />
    <path class="lw-ribbon-sheen lw-ribbon-sheen--thin" d="M18 29 C 24 14, 44 6, 62 10" />

    <!-- 环：渐变主线 + 珠串 -->
    <circle class="lw-ring" cx="60" cy="60" r="52" :stroke="ringGradUrl" />
    <circle class="lw-beads" cx="60" cy="60" r="52" />

    <!-- 藤蔓卷须 -->
    <path class="lw-vine" d="M21 35 q -9 -2 -7 -11 q 2 -7 9 -4" />
    <path class="lw-vine" d="M99 91 q 9 3 5 11 q -3 6 -10 3" />

    <!-- 叶 -->
    <g class="lw-leaves">
      <use href="#lw-leaf" transform="translate(31 29) rotate(-38) scale(1.1)" />
      <use href="#lw-leaf" transform="translate(101 57) rotate(62)" />
      <use href="#lw-leaf" transform="translate(62 104) rotate(158) scale(1.15)" />
      <use href="#lw-leaf" transform="translate(18 51) rotate(-74)" />
      <use href="#lw-leaf" transform="translate(91 96) rotate(120) scale(0.9)" />
      <use href="#lw-leaf" transform="translate(37 83) rotate(-150) scale(0.8)" />
    </g>

    <!-- 花：大花簇 + 小花苞 -->
    <g class="lw-flowers">
      <use href="#lw-flower" transform="translate(24 24) scale(1.32)" />
      <use href="#lw-flower" transform="translate(106 43) rotate(20) scale(1)" />
      <use href="#lw-flower" transform="translate(75 107) rotate(-14) scale(1.05)" />
      <use href="#lw-flower" transform="translate(98 81) rotate(35) scale(0.58)" />
      <use href="#lw-flower" transform="translate(33 95) rotate(-30) scale(0.55)" />
      <use href="#lw-flower" transform="translate(19 68) rotate(12) scale(0.48)" />
    </g>

    <!-- 浆果 -->
    <g class="lw-berries">
      <circle cx="14" cy="72" r="1.6" />
      <circle cx="18" cy="77" r="1.3" />
      <circle cx="11" cy="78" r="1.2" />
      <circle cx="109" cy="63" r="1.6" />
      <circle cx="105" cy="69" r="1.3" />
      <circle cx="112" cy="69" r="1.2" />
    </g>

    <!-- 灯笼挂饰（冠军专属） -->
    <g v-if="variant === 'gold'" transform="translate(99 18) rotate(16)">
      <circle cx="0" cy="2" r="10" :fill="haloUrl" />
      <use href="#lw-lantern" />
    </g>

    <!-- 星光 -->
    <g class="lw-sparks">
      <use href="#lw-spark" transform="translate(17 52) scale(0.85)" />
      <use href="#lw-spark" transform="translate(45 13) scale(0.55)" />
      <use href="#lw-spark" transform="translate(105 30) scale(0.75)" />
      <use href="#lw-spark" transform="translate(56 103) scale(0.5)" />
      <use href="#lw-spark" transform="translate(88 22) scale(0.5)" />
      <use href="#lw-spark" transform="translate(29 86) scale(0.6)" />
    </g>
  </svg>
</template>

<style scoped>
.leaderboard-wreath {
  display: block;
  overflow: visible;
  pointer-events: none;
  filter: drop-shadow(0 2px 5px var(--lw-glow));
}

/* 名次配色（平涂部分用 CSS 变量，渐变部分走 defs） */
.leaderboard-wreath--gold {
  --lw-petal: #ffe9ad;
  --lw-core: #e79a2e;
  --lw-petal-inner: #fff6d8;
  --lw-petal-stroke: #e0aa3a;
  --lw-leaf: #d9a94e;
  --lw-bead: #f2cd6b;
  --lw-spark: #fff0b8;
  --lw-lantern: #ffdf8e;
  --lw-glow: rgba(240, 201, 100, 0.45);
}

.leaderboard-wreath--silver {
  --lw-petal: #ffffff;
  --lw-core: #a9aeba;
  --lw-petal-inner: #f1f3f7;
  --lw-petal-stroke: #a3a9b6;
  --lw-leaf: #c6cbd5;
  --lw-bead: #d6dae2;
  --lw-spark: #ffffff;
  --lw-lantern: #eceff4;
  --lw-glow: rgba(190, 196, 208, 0.45);
}

.leaderboard-wreath--bronze {
  --lw-petal: #f6c98d;
  --lw-core: #b26a2c;
  --lw-petal-inner: #fce9cd;
  --lw-petal-stroke: #a96c30;
  --lw-leaf: #c08a52;
  --lw-bead: #e0aa6c;
  --lw-spark: #ffe0ae;
  --lw-lantern: #f4c386;
  --lw-glow: rgba(214, 158, 94, 0.45);
}

.lw-ring {
  fill: none;
  stroke-width: 2.4;
  stroke-linecap: round;
  opacity: 0.95;
}

.lw-beads {
  fill: none;
  stroke: var(--lw-bead);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-dasharray: 0.1 11.4;
  opacity: 0.8;
}

.lw-ribbon {
  fill: none;
  stroke-width: 4.8;
  stroke-linecap: round;
  opacity: 0.62;
}

.lw-ribbon--thin {
  stroke-width: 3.3;
  opacity: 0.5;
}

.lw-ribbon-sheen {
  fill: none;
  stroke: #ffffff;
  stroke-width: 1.3;
  stroke-linecap: round;
  opacity: 0.55;
}

.lw-ribbon-sheen--thin {
  stroke-width: 1;
  opacity: 0.45;
}

.lw-vine {
  fill: none;
  stroke: var(--lw-leaf);
  stroke-width: 1.6;
  stroke-linecap: round;
  opacity: 0.85;
}

.lw-line {
  stroke: var(--lw-core);
  stroke-width: 1.1;
}

.lw-line-fill {
  fill: none;
  stroke: var(--lw-core);
  stroke-width: 1;
}

.lw-flowers use {
  fill: var(--lw-petal);
  stroke: var(--lw-petal-stroke);
  stroke-width: 0.5;
}

.lw-leaves use {
  fill: var(--lw-leaf);
}

.lw-berries circle {
  fill: var(--lw-core);
  opacity: 0.85;
}

.lw-sparks use {
  fill: var(--lw-spark);
  opacity: 0.9;
  transform-box: fill-box;
  transform-origin: center;
  animation: lw-twinkle 3.2s ease-in-out infinite alternate;
}

.lw-sparks use:nth-child(2n) {
  animation-delay: 1.1s;
}

.lw-sparks use:nth-child(3n) {
  animation-delay: 2s;
}

@keyframes lw-twinkle {
  from { opacity: 0.45; }
  to { opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .lw-sparks use {
    animation: none;
  }
}
</style>

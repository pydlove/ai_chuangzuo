<template>
  <div class="grid-captcha" :class="{ 'is-passed': passed }">
    <!-- 顶部任务：印章框 -->
    <div class="grid-task">
      <span class="grid-task-label">请依次点击</span>
      <div class="grid-task-seal">
        <span
          v-for="(word, idx) in targetWords"
          :key="idx"
          class="grid-task-word"
          :class="{
            'is-done': word.state === 'done',
            'is-current': word.state === 'current',
            'is-pending': word.state === 'pending'
          }"
        >
          <svg v-if="word.state === 'done'" class="grid-task-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
          <span v-else>{{ word.text }}</span>
        </span>
      </div>
    </div>

    <!-- 3x3 网格 -->
    <div class="grid-board">
      <button
        v-for="(cell, idx) in cells"
        :key="cell.key"
        type="button"
        class="grid-cell"
        :class="cell.state"
        :disabled="passed"
        @click="onCellClick(idx)"
      >
        <span class="grid-cell-text">{{ cell.word }}</span>
        <span v-if="cell.state === 'clicked' || cell.state === 'target'" class="grid-cell-mark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </span>
      </button>
    </div>

    <!-- 底部：换一组 -->
    <div class="grid-footer">
      <button
        type="button"
        class="grid-refresh"
        :disabled="passed"
        @click="refresh"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10"/>
          <polyline points="1 20 1 14 7 14"/>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
        </svg>
        换一组
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { CAPTCHA_IDIOMS } from '@/data/captchaIdioms.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const passed = computed(() => props.modelValue)

// 4 字成语字库来自 captchaIdioms.js（成语内字不重复）
const IDIOM_POOL = CAPTCHA_IDIOMS

// 填充字库（避开成语字 + 形近字，避免用户在 9 格里同时看到「日」和「月」这种混淆）
const FILLER_POOL = [
  '天', '地', '人', '福', '岁', '月', '星', '辰',
  '光', '明', '美', '好', '安', '宁', '幸', '顺',
  '夏', '秋', '冬', '雨', '雪', '霜', '海', '川',
  '水', '草', '树', '木', '书', '墨', '笔', '砚',
  '琴', '棋', '诗', '酒', '茶', '香', '露', '珠',
  '笙', '箫', '笛', '鼓', '兰', '志', '气', '魂'
]

// 这些字很容易与成语字混淆（如「花」「风」「红」「大」「日」「千」），不放进 filler
const FUZZY_WITH_IDIOM = new Set([
  '花', '风', '红', '云', '秀', '图', '明', '清', '化', '荣',
  '大', '富', '日', '千', '玉', '力'
])

const targetWords = ref([])   // [{ text, state }]，state: pending | current | done
const cells = ref([])         // [{ word, key, state }]，state: idle | clicked | wrong | target
const clickedCount = ref(0)

const pickFiller = (n, exclude) => {
  const pool = FILLER_POOL.filter((w) => !exclude.includes(w) && !FUZZY_WITH_IDIOM.has(w))
  const shuffled = [...pool].sort(() => Math.random() - 0.5)
  return shuffled.slice(0, n)
}

const refresh = () => {
  // 选 1 个成语作为本轮目标
  const idiom = IDIOM_POOL[Math.floor(Math.random() * IDIOM_POOL.length)]
  targetWords.value = idiom.map((text, idx) => ({
    text,
    state: idx === 0 ? 'current' : 'pending'
  }))
  // 5 个填充字（避开成语字 + 易混字）
  const fillers = pickFiller(5, idiom)
  cells.value = [...idiom, ...fillers]
    .map((word, idx) => ({ word, key: idx, state: 'idle' }))
    .sort(() => Math.random() - 0.5)
  clickedCount.value = 0
}

const onCellClick = (idx) => {
  if (passed.value) return
  const cell = cells.value[idx]
  const expected = targetWords.value[clickedCount.value].text
  if (cell.word !== expected) {
    // 点错：格子红色抖动，0.6s 后换一组
    cells.value[idx] = { ...cell, state: 'wrong' }
    setTimeout(() => refresh(), 600)
    return
  }
  // 点对：标记为已点击，并更新顶部目标字状态
  cells.value[idx] = { ...cell, state: 'clicked' }
  clickedCount.value++
  targetWords.value = targetWords.value.map((w, i) => {
    if (i < clickedCount.value) return { ...w, state: 'done' }
    if (i === clickedCount.value) return { ...w, state: 'current' }
    return w
  })
  if (clickedCount.value >= targetWords.value.length) {
    emit('update:modelValue', true)
  }
}

// 外部把 modelValue 拨回 false（弹框重开时）→ 重新生成一组
watch(passed, (val) => {
  if (val) {
    // 通过：所有目标字格子显示红色印章感（filler 保持原 idle）
    cells.value = cells.value.map((cell) =>
      targetWords.value.some((w) => w.text === cell.word)
        ? { ...cell, state: 'target' }
        : cell
    )
    targetWords.value = targetWords.value.map((w) => ({ ...w, state: 'done' }))
  } else {
    refresh()
  }
})

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.grid-captcha {
  width: 100%;
  user-select: none;
  -webkit-user-select: none;
  /* 统一使用系统黑体 */
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', 'Hiragino Sans GB', 'WenQuanYi Micro Hei', 'Helvetica Neue', sans-serif;
  font-weight: 600;
}

/* ========== 顶部印章框 ========== */
.grid-task {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.grid-task-label {
  color: #FF2442;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 2px;
  flex-shrink: 0;
  /* 标签用系统字体 */
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif;
}

.grid-task-seal {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 2px solid #FF2442;
  border-radius: 4px;
  background: linear-gradient(135deg, rgba(255, 36, 66, 0.04) 0%, rgba(255, 36, 66, 0.09) 100%);
  box-shadow: inset 0 0 0 1px rgba(255, 36, 66, 0.18);
}

.grid-task-word {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 20px;
  font-weight: 600;
  color: #FF2442;
  letter-spacing: 4px;
  transition: all 0.25s;
}

.grid-task-word.is-done {
  background: #FF2442;
  color: #fff;
  border-color: #FF2442;
}

.grid-task-word.is-current {
  background: rgba(255, 36, 66, 0.15);
  border-color: #FF2442;
  border-style: dashed;
}

.grid-task-word.is-pending {
  opacity: 0.4;
}

.grid-task-check {
  width: 14px;
  height: 14px;
}

/* ========== 3x3 网格 ========== */
.grid-board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.grid-cell {
  position: relative;
  height: 72px;
  background: #FFF5F7;
  border: 2px solid #FFE5EB;
  border-radius: 6px;
  font-size: 30px;
  letter-spacing: 4px;
  color: #FF2442;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
  padding: 0;
}

.grid-cell:hover:not(:disabled) {
  border-color: #FF2442;
  background: rgba(255, 36, 66, 0.06);
}

.grid-cell:active:not(:disabled) {
  transform: scale(0.96);
}

.grid-cell.is-clicked,
.grid-cell.is-target {
  background: #FF2442;
  border-color: #FF2442;
  color: #fff;
  cursor: default;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.25);
}

.grid-cell.is-wrong {
  background: rgba(255, 36, 66, 0.08);
  border-color: #FF2442;
  color: #FF2442;
  animation: grid-shake 0.4s ease;
}

.grid-cell-mark {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  background: #fff;
  color: #FF2442;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.grid-cell-mark svg {
  width: 11px;
  height: 11px;
}

@keyframes grid-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-5px); }
  40% { transform: translateX(5px); }
  60% { transform: translateX(-3px); }
  80% { transform: translateX(3px); }
}

/* ========== 底部换一组 ========== */
.grid-footer {
  display: flex;
  justify-content: flex-end;
}

.grid-refresh {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: none;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  /* 按钮用系统字体 */
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif;
}

.grid-refresh:hover:not(:disabled) {
  color: #FF2442;
  background: rgba(255, 36, 66, 0.06);
}

.grid-refresh:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.grid-refresh svg {
  width: 12px;
  height: 12px;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .grid-task-label {
  color: #FF7A99;
}

body[data-theme="dark"] .grid-task-seal {
  background: rgba(255, 36, 66, 0.1);
  border-color: #FF4D6F;
  box-shadow: inset 0 0 0 1px rgba(255, 77, 111, 0.25);
}

body[data-theme="dark"] .grid-task-word {
  color: #FF4D6F;
}

body[data-theme="dark"] .grid-task-word.is-done {
  background: #FF4D6F;
  color: #1f1f1f;
  border-color: #FF4D6F;
}

body[data-theme="dark"] .grid-task-word.is-current {
  background: rgba(255, 77, 111, 0.18);
  border-color: #FF4D6F;
}

body[data-theme="dark"] .grid-cell {
  background: #2a1c1c;
  border-color: rgba(255, 36, 66, 0.4);
  color: #FF9EB0;
}

body[data-theme="dark"] .grid-cell:hover:not(:disabled) {
  border-color: #FF4D6F;
  background: rgba(255, 77, 111, 0.1);
}

body[data-theme="dark"] .grid-cell.is-clicked,
body[data-theme="dark"] .grid-cell.is-target {
  background: #FF4D6F;
  border-color: #FF4D6F;
  color: #1f1f1f;
}

body[data-theme="dark"] .grid-cell.is-wrong {
  background: rgba(255, 77, 111, 0.18);
  border-color: #ff4d6f;
  color: #ff7a99;
}

body[data-theme="dark"] .grid-cell-mark {
  background: #1f1f1f;
  color: #FF4D6F;
}

body[data-theme="dark"] .grid-refresh {
  color: #8c8c8c;
}

body[data-theme="dark"] .grid-refresh:hover:not(:disabled) {
  color: #FF4D6F;
  background: rgba(255, 77, 111, 0.08);
}
</style>

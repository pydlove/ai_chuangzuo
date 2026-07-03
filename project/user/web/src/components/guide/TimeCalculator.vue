<template>
  <div class="time-calculator">
    <h4 class="tc-title">算算你能省多少</h4>
    <div class="tc-fields">
      <div class="tc-field">
        <label>每周写几篇</label>
        <a-input-number v-model:value="articlesPerWeek" :min="0" :precision="0" class="tc-input" />
      </div>
      <div class="tc-field">
        <label>原来每篇花几小时</label>
        <a-input-number v-model:value="originalHours" :min="0" :precision="1" class="tc-input" />
      </div>
      <div class="tc-field">
        <label>时薪估算（元）</label>
        <a-input-number v-model:value="hourlyRate" :min="0" :precision="0" class="tc-input" />
      </div>
    </div>
    <div class="tc-result">
      <template v-if="isValid">
        <div class="tc-result-line">
          每月可节省 <span class="tc-result-num">{{ savedHours }}</span> 小时
        </div>
        <div class="tc-result-line">
          相当于 <span class="tc-result-num">{{ savedMoney }}</span> 元人工成本
        </div>
        <p class="tc-result-tip">省下的时间可以用来做选题、运营账号，或者直接再生产更多内容。</p>
      </template>
      <template v-else>
        <div class="tc-result-placeholder">请输入有效数字</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const articlesPerWeek = ref(5)
const originalHours = ref(2)
const hourlyRate = ref(50)

const isValid = computed(() => {
  return Number.isFinite(articlesPerWeek.value) && articlesPerWeek.value >= 0
    && Number.isFinite(originalHours.value) && originalHours.value >= 0
    && Number.isFinite(hourlyRate.value) && hourlyRate.value >= 0
})

const savedHours = computed(() => {
  if (!isValid.value) return 0
  const perWeek = articlesPerWeek.value * (originalHours.value - 0.05)
  return Number((perWeek * 4).toFixed(1))
})

const savedMoney = computed(() => {
  if (!isValid.value) return 0
  return Number((savedHours.value * hourlyRate.value).toFixed(0))
})
</script>

<style scoped>
.time-calculator {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
}
.tc-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.tc-fields {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.tc-field label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 6px;
}
.tc-input {
  width: 100%;
}
.tc-result {
  background: #fff5f7;
  border-radius: 10px;
  padding: 16px;
}
.tc-result-line {
  font-size: 15px;
  color: #1a1a1a;
  margin-bottom: 6px;
}
.tc-result-num {
  color: #ff2442;
  font-weight: 700;
  font-size: 18px;
}
.tc-result-tip {
  font-size: 13px;
  color: #8c8c8c;
  margin: 10px 0 0;
}
.tc-result-placeholder {
  color: #8c8c8c;
  font-size: 14px;
}

@media (max-width: 768px) {
  .tc-fields {
    grid-template-columns: 1fr;
  }
}

body[data-theme="dark"] .time-calculator {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .tc-title,
body[data-theme="dark"] .tc-result-line {
  color: #e0e0e0;
}
body[data-theme="dark"] .tc-result {
  background: rgba(255, 36, 66, 0.08);
}
body[data-theme="dark"] .tc-result-tip,
body[data-theme="dark"] .tc-result-placeholder,
body[data-theme="dark"] .tc-field label {
  color: #a6a6a6;
}
</style>

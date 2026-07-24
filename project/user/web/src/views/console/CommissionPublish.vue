<template>
  <div class="publish-page">
    <div class="publish-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h2 class="publish-title">发布约稿</h2>
      <p class="publish-subtitle">设置征集任务,平台抽成 10%,奖励以创作币结算</p>
    </div>

    <div class="publish-form">
      <div class="form-item">
        <label class="form-label">标题<span class="required">*</span></label>
        <input
          v-model="form.title"
          class="form-input"
          placeholder="一句话说清楚需要什么稿件"
          maxlength="30"
        />
        <div class="form-hint">{{ form.title.length }}/30</div>
      </div>

      <div class="form-item">
        <label class="form-label">需求描述<span class="required">*</span></label>
        <textarea
          v-model="form.description"
          class="form-textarea"
          placeholder="详细描述选题方向、读者人群、写作要点等"
          maxlength="500"
          rows="5"
        ></textarea>
        <div class="form-hint">{{ form.description.length }}/500</div>
      </div>

      <div class="form-item">
        <label class="form-label">字数范围<span class="required">*</span></label>
        <div class="form-row">
          <div class="form-row-item">
            <input
              v-model.number="form.minWordCount"
              type="number"
              class="form-input"
              placeholder="下限"
              min="100"
              max="5000"
            />
            <div class="form-hint-row">字 (下限)</div>
          </div>
          <span class="form-tilde">~</span>
          <div class="form-row-item">
            <input
              v-model.number="form.maxWordCount"
              type="number"
              class="form-input"
              placeholder="上限"
              min="100"
              max="5000"
            />
            <div class="form-hint-row">字 (上限)</div>
          </div>
        </div>
      </div>

      <div class="form-item">
        <label class="form-label">风格提示</label>
        <input
          v-model="form.styleHint"
          class="form-input"
          placeholder="可选,如:种草风、理性、走心..."
          maxlength="50"
        />
      </div>

      <div class="form-item">
        <label class="form-label">截止时间<span class="required">*</span></label>
        <div class="deadline-options">
          <button
            v-for="opt in COMMISSION_CONFIG.DEADLINE_OPTIONS"
            :key="opt.days"
            :class="['deadline-btn', { active: form.deadlineDays === opt.days }]"
            @click="form.deadlineDays = opt.days"
          >{{ opt.label }}</button>
        </div>
      </div>

      <div class="form-item">
        <label class="form-label">奖励(创作币)<span class="required">*</span></label>
        <div class="reward-row">
          <input
            v-model.number="form.rewardCoin"
            type="number"
            class="form-input reward-input"
            :min="COMMISSION_CONFIG.MIN_REWARD"
            :max="COMMISSION_CONFIG.MAX_REWARD"
            :step="5"
          />
          <span class="reward-coin-label">创作币</span>
        </div>
        <div class="form-hint">最低 {{ COMMISSION_CONFIG.MIN_REWARD }},最高 {{ COMMISSION_CONFIG.MAX_REWARD }} 创作币</div>
      </div>

      <!-- 实时预览 -->
      <div class="preview-card">
        <div class="preview-title">结算预览</div>
        <div class="preview-row">
          <span class="preview-label">需冻结</span>
          <span class="preview-value">{{ form.rewardCoin || 0 }} 创作币</span>
        </div>
        <div class="preview-row">
          <span class="preview-label">平台抽成(10%)</span>
          <span class="preview-value">{{ platformFee }} 创作币</span>
        </div>
        <div class="preview-row">
          <span class="preview-label">投稿者实得(90%)</span>
          <span class="preview-value highlight">{{ winnerPayout }} 创作币</span>
        </div>
        <div class="preview-divider"></div>
        <div class="preview-row">
          <span class="preview-label">当前余额</span>
          <span class="preview-value">{{ coinBalance }} 创作币</span>
        </div>
        <div class="preview-row">
          <span class="preview-label">冻结后余额</span>
          <span :class="['preview-value', { 'preview-negative': balanceAfterPublish < 0 }]">
            {{ balanceAfterPublish }} 创作币
          </span>
        </div>
      </div>

      <div class="form-actions">
        <button class="btn-secondary" @click="goBack">取消</button>
        <button class="btn-primary" @click="onSubmit">发布并冻结奖励</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useCommission } from '@/composables/useCommission'
import { COMMISSION_CONFIG } from '@/api/commission'

const router = useRouter()
const { coinBalance, createTask } = useCommission()

const form = reactive({
  title: '',
  description: '',
  minWordCount: 600,
  maxWordCount: 1200,
  styleHint: '',
  deadlineDays: 7,
  rewardCoin: 30
})

const platformFee = computed(() => Math.floor((form.rewardCoin || 0) * COMMISSION_CONFIG.PLATFORM_FEE_RATE))
const winnerPayout = computed(() => (form.rewardCoin || 0) - platformFee.value)
const balanceAfterPublish = computed(() => coinBalance.value - (form.rewardCoin || 0))

function goBack() {
  router.replace('/console/commission')
}

function onSubmit() {
  const result = createTask({
    title: form.title,
    description: form.description,
    requirements: {
      minWordCount: form.minWordCount,
      maxWordCount: form.maxWordCount,
      styleHint: form.styleHint
    },
    rewardCoin: form.rewardCoin,
    deadlineDays: form.deadlineDays
  })

  if (!result.ok) {
    if (result.insufficient) {
      Modal.confirm({
        title: '余额不足',
        content: `发布此任务需冻结 ${form.rewardCoin} 创作币,当前余额仅 ${coinBalance.value}。是否前往提现?`,
        okText: '去提现',
        cancelText: '取消',
        centered: true,
        onOk: () => router.push('/console/coin?from=commission')
      })
      return
    }
    message.warning(result.error)
    return
  }

  message.success('发布成功,奖励已冻结')
  router.replace(`/console/commission/${result.task.id}`)
}
</script>

<style scoped>
.publish-page {
  max-width: 640px;
  margin: 0 auto;
}

.publish-header {
  margin-bottom: 24px;
  position: relative;
}

.back-btn {
  position: absolute;
  left: -64px;
  top: 4px;
  background: none;
  border: none;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--color-primary-light, rgba(255, 36, 66, 0.08));
  color: var(--color-primary, #FF2442);
}

.publish-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 6px;
}

.publish-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.publish-form {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.form-item {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 13px;
  color: #262626;
  font-weight: 500;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
  margin-left: 4px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  box-sizing: border-box;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary, #FF2442);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--color-primary, #FF2442);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-hint {
  font-size: 11px;
  color: #8c8c8c;
  text-align: right;
  margin-top: 4px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-row-item {
  flex: 1;
}

.form-hint-row {
  font-size: 11px;
  color: #8c8c8c;
  text-align: center;
  margin-top: 4px;
}

.form-tilde {
  font-size: 18px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.deadline-options {
  display: flex;
  gap: 10px;
}

.deadline-btn {
  flex: 1;
  padding: 10px 0;
  background: #fafafa;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.deadline-btn:hover {
  border-color: var(--color-primary, #FF2442);
  color: var(--color-primary, #FF2442);
}

.deadline-btn.active {
  background: var(--color-primary-light, rgba(255, 36, 66, 0.08));
  border-color: var(--color-primary, #FF2442);
  color: var(--color-primary, #FF2442);
  font-weight: 600;
}

.reward-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reward-input {
  flex: 1;
}

.reward-coin-label {
  font-size: 14px;
  color: #595959;
  flex-shrink: 0;
}

.preview-card {
  margin-top: 24px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #fff5f7, #fff0f2);
  border: 1px dashed rgba(255, 36, 66, 0.25);
  border-radius: 12px;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary, #FF2442);
  margin-bottom: 12px;
}

.preview-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin-bottom: 6px;
}

.preview-row:last-child {
  margin-bottom: 0;
}

.preview-label {
  color: #595959;
}

.preview-value {
  color: #1a1a1a;
  font-weight: 500;
}

.preview-value.highlight {
  color: var(--color-primary, #FF2442);
  font-weight: 700;
}

.preview-value.preview-negative {
  color: #ff4d4f;
}

.preview-divider {
  height: 1px;
  background: rgba(255, 36, 66, 0.15);
  margin: 10px 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.btn-secondary,
.btn-primary {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-secondary {
  background: #f5f5f5;
  color: #595959;
}

.btn-secondary:hover {
  background: #e8e8e8;
}

.btn-primary {
  background: var(--color-primary, #FF2442);
  color: #fff;
}

.btn-primary:hover {
  background: #e0203b;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .publish-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .publish-form {
  background: #1f1f1f;
}

body[data-theme="dark"] .form-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .form-input,
body[data-theme="dark"] .form-textarea {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .form-input:focus,
body[data-theme="dark"] .form-textarea:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .deadline-btn {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .deadline-btn.active {
  background: rgba(255, 36, 66, 0.18);
  border-color: #ff4d6f;
  color: #ff4d6f;
}

body[data-theme="dark"] .preview-card {
  background: linear-gradient(135deg, #2a1015, #1f0d12);
  border-color: rgba(255, 36, 66, 0.4);
}

body[data-theme="dark"] .preview-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .preview-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .preview-value.highlight {
  color: #ff4d6f;
}

body[data-theme="dark"] .form-actions {
  border-top-color: #303030;
}

body[data-theme="dark"] .btn-secondary {
  background: #262626;
  color: #a6a6a6;
}

body[data-theme="dark"] .btn-secondary:hover {
  background: #303030;
}

/* ========== 移动端 ========== */
@media (max-width: 768px) {
  .back-btn {
    position: static;
    display: inline-block;
    margin-bottom: 12px;
  }

  .publish-form {
    padding: 18px 16px;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn-secondary,
  .btn-primary {
    width: 100%;
  }
}
</style>
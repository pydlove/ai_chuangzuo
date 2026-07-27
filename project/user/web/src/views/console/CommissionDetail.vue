<template>
  <div class="detail-page">
    <button class="back-btn" @click="router.push('/console/commission')">← 返回约稿大厅</button>

    <div v-if="loading" class="panel empty-block">加载中...</div>

    <template v-else-if="task">
      <div class="detail-grid">
        <!-- 左侧：任务内容 -->
        <section class="content-panel">
          <div class="content-head">
            <span :class="['status-tag', `status-${task.status}`]">{{ taskStatus(task.status) }}</span>
            <h1>{{ task.title }}</h1>
            <p class="source-line">来源：官方 / 管理员发布</p>
          </div>
          <div class="meta-row">
            <span><strong>{{ wordRangeText(task) }}</strong></span>
            <span>采纳 {{ task.adoptedCount }} / {{ task.neededCount }} 篇</span>
            <span>{{ deadlineText }}</span>
          </div>
          <div class="description-block">
            <h3>任务说明</h3>
            <div class="description">{{ task.description }}</div>
          </div>
          <div v-if="task.styleHint" class="style-hint-block">
            <h3>风格提示</h3>
            <p>{{ task.styleHint }}</p>
          </div>
        </section>

        <!-- 右侧：固定操作卡 -->
        <aside class="action-panel">
          <div class="reward-card">
            <span class="reward-label">单篇奖励</span>
            <strong class="reward-value">{{ task.rewardCoin }}<small> 创作币</small></strong>
            <p class="reward-note">采纳后全额发放至创作币账户</p>
          </div>

          <div class="action-card">
            <!-- 已投稿：显示我的投稿状态 -->
            <template v-if="mySubmission">
              <div class="action-head">
                <span class="action-title">我的投稿</span>
                <span :class="['status-tag', `submission-${mySubmission.status}`]">{{ submissionStatus(mySubmission.status) }}</span>
              </div>
              <div class="submission-info">
                <h4>《{{ mySubmission.articleTitle }}》</h4>
                <p>{{ mySubmission.wordCount }} 字</p>
              </div>
              <div v-if="mySubmission.status === 1" class="reward-result">
                已获得 {{ mySubmission.rewardCoin }} 创作币
              </div>
              <button v-if="canWithdraw" class="secondary-btn" @click="confirmWithdraw">撤回投稿</button>
            </template>

            <!-- 招募中且未投稿：投稿操作 -->
            <template v-else-if="task.status === 0">
              <div class="action-head">
                <span class="action-title">投递稿件</span>
                <span class="status-tag status-0">{{ taskStatus(0) }}</span>
              </div>
              <p class="action-desc">只能选择你在爱创作中已经生成完成、且字数符合要求的文章。</p>
              <button class="primary-btn" @click="openPicker">选择文章投稿</button>
            </template>

            <!-- 其它状态 -->
            <template v-else>
              <div class="action-head">
                <span class="action-title">任务已停止投稿</span>
              </div>
              <p class="action-desc">当前任务状态：{{ taskStatus(task.status) }}。请关注大厅中的其它招募中任务。</p>
            </template>
          </div>

          <div class="fact-card">
            <div class="fact-row">
              <span>计划采纳</span>
              <strong>{{ task.neededCount }} 篇</strong>
            </div>
            <div class="fact-row">
              <span>已采纳</span>
              <strong>{{ task.adoptedCount }} 篇</strong>
            </div>
            <div class="fact-row">
              <span>截止时间</span>
              <strong>{{ new Date(task.deadlineAt).toLocaleString() }}</strong>
            </div>
          </div>
        </aside>
      </div>

      <div v-if="canSubmit" class="mobile-submit-bar">
        <span>仅支持平台生成文章</span>
        <button class="primary-btn" @click="openPicker">选择文章投稿</button>
      </div>

      <a-modal v-model:open="pickerVisible" title="选择投稿文章" :footer="null" centered :width="560">
        <div v-if="articles.length === 0" class="empty picker-empty">暂无已生成文章</div>
        <div v-else class="article-list">
          <button v-for="article in articles" :key="article.bizNo"
                  :disabled="!inRange(article)"
                  :class="['article-item', { selected: selectedBizNo === article.bizNo }]"
                  @click="selectedBizNo = article.bizNo">
            <div class="article-main">
              <strong>{{ article.title }}</strong>
              <span class="article-meta">{{ article.wordCount }} 字 · {{ article.platformName }} · 完成于 {{ formatCompletedAt(article.completedAt) }}</span>
              <span v-if="!inRange(article)" class="article-warn">字数不符（要求 {{ wordRangeText(task) }}）</span>
            </div>
          </button>
        </div>
        <div class="modal-actions">
          <button class="secondary-btn" @click="pickerVisible = false">取消</button>
          <button class="primary-btn" :disabled="!selectedBizNo || submitting" @click="submit">确认投稿</button>
        </div>
      </a-modal>
    </template>

    <div v-else class="panel empty-block">任务不存在或已删除</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useCommission } from '@/composables/useCommission'
import { useWorks } from '@/composables/useWorks'

const route = useRoute()
const router = useRouter()
const { taskDetail, loading, loadTask, submitArticle, withdrawSubmission } = useCommission()
const { articles, load: loadWorks } = useWorks()
const pickerVisible = ref(false)
const selectedBizNo = ref('')
const submitting = ref(false)

const task = computed(() => taskDetail.value?.task || null)
const mySubmission = computed(() => taskDetail.value?.mySubmission || null)
const canSubmit = computed(() => task.value?.status === 0 && !mySubmission.value)
const canWithdraw = computed(() => task.value?.status === 0 && mySubmission.value?.status === 0)

const deadlineText = computed(() => {
  if (!task.value?.deadlineAt) return ''
  const deadline = new Date(task.value.deadlineAt)
  if (task.value.status !== 0 || deadline <= new Date()) return `截止于 ${deadline.toLocaleString()}`
  const hours = Math.max(1, Math.ceil((deadline - Date.now()) / 3600000))
  return hours > 24 ? `还剩 ${Math.ceil(hours / 24)} 天` : `还剩 ${hours} 小时`
})

onMounted(async () => {
  try {
    await Promise.all([
      loadTask(route.params.id),
      loadWorks({ page: 1, pageSize: 50 })
    ])
  } catch (error) {
    message.error(error.message || '约稿详情加载失败')
  }
})

function taskStatus(value) {
  return ['招募中', '已截止待采纳', '已完成'][value] || '未知状态'
}
function submissionStatus(value) {
  return ['等待采纳', '已采纳', '未采纳', '已撤回'][value] || '未知状态'
}
function inRange(article) {
  const min = task.value?.minWordCount
  const max = task.value?.maxWordCount
  if (min != null && article.wordCount < min) return false
  if (max != null && article.wordCount > max) return false
  return true
}
function wordRangeText(item) {
  if (!item) return ''
  const min = item.minWordCount
  const max = item.maxWordCount
  if (min != null && max != null) return `${min}-${max} 字`
  if (min != null) return `≥ ${min} 字`
  if (max != null) return `≤ ${max} 字`
  return '字数不限'
}
function formatCompletedAt(value) {
  if (!value) return '未知'
  return new Date(value).toLocaleString()
}
function openPicker() {
  selectedBizNo.value = ''
  pickerVisible.value = true
}
async function submit() {
  submitting.value = true
  try {
    await submitArticle(task.value.id, selectedBizNo.value)
    pickerVisible.value = false
    message.success('投稿成功，等待管理员采纳')
  } catch (error) {
    message.error(error.message || '投稿失败')
  } finally {
    submitting.value = false
  }
}
function confirmWithdraw() {
  Modal.confirm({
    title: '确认撤回投稿？',
    content: '截止前撤回后，可以改投其他文章。',
    okText: '确认撤回',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await withdrawSubmission(mySubmission.value.id, task.value.id)
        message.success('投稿已撤回')
      } catch (error) {
        message.error(error.message || '撤回失败')
      }
    }
  })
}
</script>

<style scoped>
.detail-page {
  max-width: 1120px;
  margin: 0 auto;
  padding-bottom: 100px;
}
.back-btn {
  margin-bottom: 16px;
  border: 0;
  background: none;
  color: #595959;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 0;
}
.back-btn:hover { color: var(--color-primary, #ff2442); }

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 1fr);
  gap: 20px;
  align-items: start;
}

.content-panel,
.action-panel > * {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}
.content-panel { padding: 28px 32px; }

.content-head { margin-bottom: 16px; }
.content-head .status-tag { margin-bottom: 12px; }
.content-head h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #1f1f1f;
  line-height: 1.4;
}
.source-line {
  margin: 0;
  font-size: 12px;
  color: #8c8c8c;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 14px 0;
  margin-bottom: 18px;
  border-top: 1px dashed #f0f0f0;
  border-bottom: 1px dashed #f0f0f0;
  color: #595959;
  font-size: 13px;
}
.meta-row strong { color: #1f1f1f; font-weight: 600; margin-right: 2px; }

.description-block,
.style-hint-block { margin-top: 18px; }
.description-block h3,
.style-hint-block h3 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
}
.description {
  white-space: pre-wrap;
  line-height: 1.85;
  color: #262626;
  font-size: 14px;
}
.style-hint-block p {
  margin: 0;
  padding: 14px 16px;
  border-radius: 12px;
  background: #fff7ed;
  color: #874800;
  font-size: 13px;
  line-height: 1.7;
}

.action-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 16px;
}

.reward-card {
  padding: 22px 24px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ffe7ec 0%, #fde7f3 100%);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.reward-label {
  font-size: 12px;
  color: #874800;
  letter-spacing: 1px;
}
.reward-value {
  font-size: 32px;
  color: var(--color-primary, #ff2442);
  font-weight: 700;
  line-height: 1.1;
}
.reward-value small {
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  margin-left: 4px;
}
.reward-note { margin: 4px 0 0; color: #874800; font-size: 12px; }

.action-card { padding: 22px 24px; }
.action-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.action-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
}
.action-desc {
  margin: 0 0 14px;
  color: #595959;
  font-size: 13px;
  line-height: 1.7;
}

.submission-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
}
.submission-info p { margin: 0; color: #595959; font-size: 12px; }

.reward-result {
  margin: 14px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #e6f7ed;
  color: #07c160;
  font-size: 13px;
  font-weight: 500;
}

.fact-card { padding: 18px 22px; }
.fact-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
  color: #595959;
}
.fact-row strong { color: #1f1f1f; font-weight: 600; }

.status-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.status-0 { color: #1677ff; background: #e6f4ff; }
.status-1 { color: #fa8c16; background: #fff4e6; }
.status-2 { color: #07c160; background: #e6f7ed; }
.submission-0 { color: #1677ff; background: #e6f4ff; }
.submission-1 { color: #07c160; background: #e6f7ed; }
.submission-2 { color: #8c8c8c; background: #f5f5f5; }
.submission-3 { color: #bfbfbf; background: #fafafa; }

.primary-btn,
.secondary-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 11px 20px;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  min-height: 40px;
  width: 100%;
}
.primary-btn { color: #fff; background: var(--color-primary, #ff2442); }
.primary-btn:disabled { opacity: .45; cursor: not-allowed; }
.secondary-btn { background: #f2f2f2; color: #555; }
.secondary-btn:disabled { opacity: .55; cursor: not-allowed; }

.empty-block {
  padding: 72px 20px;
  text-align: center;
  color: #8c8c8c;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.article-list {
  max-height: 420px;
  overflow: auto;
  display: grid;
  gap: 10px;
  padding-right: 2px;
}
.article-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #eee;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}
.article-item strong { font-size: 14px; color: #1f1f1f; }
.article-item .article-meta { font-size: 12px; color: #8c8c8c; }
.article-item .article-warn { font-size: 12px; color: #fa8c16; }
.article-item.selected {
  border-color: var(--color-primary, #ff2442);
  background: #fff5f7;
}
.article-item:disabled { opacity: .55; cursor: not-allowed; }
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.modal-actions .primary-btn,
.modal-actions .secondary-btn { width: auto; min-width: 96px; }

.mobile-submit-bar {
  display: none;
}

body[data-theme="dark"] .content-panel,
body[data-theme="dark"] .action-panel > *,
body[data-theme="dark"] .empty-block {
  background: #1f1f1f;
}
body[data-theme="dark"] .content-head h1,
body[data-theme="dark"] .action-title,
body[data-theme="dark"] .submission-info h4,
body[data-theme="dark"] .fact-row strong { color: #f5f5f5; }
body[data-theme="dark"] .description { color: #d9d9d9; }
body[data-theme="dark"] .meta-row,
body[data-theme="dark"] .fact-row,
body[data-theme="dark"] .action-desc,
body[data-theme="dark"] .source-line { color: #bfbfbf; }
body[data-theme="dark"] .meta-row { border-color: #303030; }
body[data-theme="dark"] .reward-card {
  background: linear-gradient(135deg, #3a1f2a 0%, #3a1f30 100%);
}
body[data-theme="dark"] .reward-label,
body[data-theme="dark"] .reward-note { color: #f5d0d8; }
body[data-theme="dark"] .reward-value small { color: #bfbfbf; }
body[data-theme="dark"] .style-hint-block p {
  background: #2a1f10;
  color: #f5d0a8;
}
body[data-theme="dark"] .article-item {
  background: #262626;
  border-color: #303030;
}
body[data-theme="dark"] .article-item strong { color: #f5f5f5; }
body[data-theme="dark"] .article-item.selected {
  background: rgba(255, 36, 66, 0.12);
  border-color: var(--color-primary, #ff2442);
}
body[data-theme="dark"] .secondary-btn { background: #2a2a2a; color: #d9d9d9; }
body[data-theme="dark"] .reward-result {
  background: rgba(7, 193, 96, 0.15);
}

@media (max-width: 1024px) {
  .detail-grid { grid-template-columns: 1fr; }
  .action-panel { position: static; }
}
@media (max-width: 768px) {
  .detail-page { padding-bottom: 104px; }
  .detail-grid { grid-template-columns: minmax(0, 1fr); }
  .content-panel { padding: 20px 18px; }
  .content-head h1 { font-size: 24px; }
  .meta-row,
  .action-head,
  .fact-row { flex-wrap: wrap; }
  .action-panel {
    position: static;
    top: auto;
  }
  .action-card,
  .fact-card,
  .reward-card { padding: 18px; }
  .reward-card { border-radius: 20px; }
  .reward-value { font-size: 28px; }
  .action-panel .primary-btn,
  .action-panel .secondary-btn {
    width: min(100%, 320px);
  }
  :deep(.ant-modal) {
    width: min(560px, calc(100vw - 24px)) !important;
    max-width: none;
  }
  .mobile-submit-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 12px;
    padding: 12px 14px;
    border: 1px solid #f0f0f0;
    border-radius: 14px;
    background: #fff;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    z-index: 50;
  }
  .mobile-submit-bar > span {
    flex: 1;
    color: #8c8c8c;
    font-size: 12px;
    line-height: 1.4;
  }
  .mobile-submit-bar .primary-btn {
    flex: 0 1 180px;
    width: auto;
    max-width: 100%;
    min-height: 44px;
  }
  body[data-theme="dark"] .mobile-submit-bar {
    background: #1f1f1f;
    border-color: #303030;
  }
  body[data-theme="dark"] .mobile-submit-bar > span { color: #bfbfbf; }
}
</style>
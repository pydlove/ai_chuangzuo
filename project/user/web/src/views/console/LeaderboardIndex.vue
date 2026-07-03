<template>
  <div class="leaderboard-page">
    <div class="leaderboard-header">
      <h2 class="leaderboard-title">收益排行榜</h2>
      <p class="leaderboard-subtitle">
        创作币榜按月度统计平台收益，自媒体收入榜需提交收入截图并通过审核后计入排名
        <span class="leaderboard-rules-link" @click="rulesVisible = true">规则说明</span>
      </p>
    </div>

    <div class="leaderboard-tabs">
      <button
        :class="['leaderboard-tab', { active: activeTab === 'coin' }]"
        @click="activeTab = 'coin'"
      >
        创作币榜
      </button>
      <button
        :class="['leaderboard-tab', { active: activeTab === 'income' }]"
        @click="activeTab = 'income'"
      >
        自媒体收入榜
      </button>
    </div>

    <!-- 创作币榜 -->
    <div v-show="activeTab === 'coin'" class="leaderboard-section">
      <div class="leaderboard-toolbar">
        <div class="leaderboard-toolbar-left">
          <span class="leaderboard-period-label">{{ currentCoinMonth }}</span>
        </div>
      </div>

      <div :class="['reward-banner', coinRewardBanner.class]">
        <div class="reward-banner-icon">🏆</div>
        <div class="reward-banner-text">
          <div class="reward-banner-title">{{ coinRewardBanner.title }}</div>
          <div class="reward-banner-desc">{{ coinRewardBanner.desc }}</div>
        </div>
      </div>

      <div v-if="myCoinStatus" class="my-reward-card">
        <div class="my-reward-rank">第 {{ myCoinItem.rank }} 名</div>
        <div class="my-reward-info">
          <div class="my-reward-label">{{ myCoinStatus.label }}</div>
          <div class="my-reward-desc">{{ myCoinStatus.desc }}</div>
        </div>
        <div class="my-reward-amount">+100 创作币</div>
      </div>

      <div class="leaderboard-top3">
        <div
          v-for="item in coinTop3"
          :key="item.userId"
          :class="['leaderboard-top-card', 'top-' + item.rank, { 'is-me': item.isMe }]"
        >
          <div class="top-rank">{{ item.rank }}</div>
          <div class="top-nickname">{{ item.nickname }}</div>
          <div class="top-amount">{{ item.amount.toFixed(2) }} 创作币</div>
          <div v-if="item.isMe" class="top-me-tag">我</div>
          <div v-if="coinRewardLabel(item)" :class="['top-reward', coinRewardLabel(item).type]">
            {{ coinRewardLabel(item).text }}
          </div>
        </div>
      </div>

      <div v-if="coinListAfter3.length === 0" class="leaderboard-empty">
        暂无排名数据
      </div>
      <div v-else class="leaderboard-list">
        <div
          v-for="item in coinListAfter3"
          :key="item.userId"
          :class="['leaderboard-item', { 'is-me': item.isMe }, 'rank-' + item.rank]"
        >
          <span class="leaderboard-rank">{{ item.rank }}</span>
          <span class="leaderboard-avatar">{{ item.nickname.charAt(0) }}</span>
          <span class="leaderboard-nickname">{{ item.nickname }}</span>
          <span v-if="item.isMe" class="leaderboard-me-tag">我</span>
          <div v-if="coinRewardLabel(item)" :class="['leaderboard-reward', coinRewardLabel(item).type]">
            {{ coinRewardLabel(item).text }}
          </div>
          <span class="leaderboard-amount">{{ item.amount.toFixed(2) }} 创作币</span>
        </div>
      </div>
    </div>

    <!-- 自媒体收入榜 -->
    <div v-show="activeTab === 'income'" class="leaderboard-section">
      <div class="leaderboard-toolbar">
        <div class="leaderboard-toolbar-left">
          <div class="leaderboard-period-tabs">
            <button
              :class="['leaderboard-period-tab', { active: incomePeriodType === 'month' }]"
              @click="setIncomePeriodType('month')"
            >
              月度
            </button>
            <button
              :class="['leaderboard-period-tab', { active: incomePeriodType === 'year' }]"
              @click="setIncomePeriodType('year')"
            >
              年度
            </button>
          </div>
          <span class="leaderboard-period-label">{{ incomePeriodLabel }}</span>
        </div>
        <button class="leaderboard-submit-btn" @click="openSubmitModal">
          申报收入
        </button>
      </div>

      <div v-if="incomePeriodType === 'month'" :class="['reward-banner', incomeRewardBanner.class]">
        <div class="reward-banner-icon">🏆</div>
        <div class="reward-banner-text">
          <div class="reward-banner-title">{{ incomeRewardBanner.title }}</div>
          <div class="reward-banner-desc">{{ incomeRewardBanner.desc }}</div>
        </div>
      </div>

      <div v-if="myIncomeStatus" class="my-reward-card">
        <div class="my-reward-rank">第 {{ myIncomeItem.rank }} 名</div>
        <div class="my-reward-info">
          <div class="my-reward-label">{{ myIncomeStatus.label }}</div>
          <div class="my-reward-desc">{{ myIncomeStatus.desc }}</div>
        </div>
        <div class="my-reward-amount">+100 创作币</div>
      </div>

      <div class="leaderboard-top3">
        <div
          v-for="item in incomeTop3"
          :key="item.userId"
          :class="['leaderboard-top-card', 'top-' + item.rank, { 'is-me': item.isMe }]"
        >
          <div class="top-rank">{{ item.rank }}</div>
          <div class="top-nickname">{{ item.nickname }}</div>
          <div class="top-amount">{{ item.amount.toFixed(2) }} 元</div>
          <div v-if="item.isMe" class="top-me-tag">我</div>
          <div v-if="incomeRewardLabel(item)" :class="['top-reward', incomeRewardLabel(item).type]">
            {{ incomeRewardLabel(item).text }}
          </div>
        </div>
      </div>

      <div v-if="incomeListAfter3.length === 0" class="leaderboard-empty">
        暂无排名数据
      </div>
      <div v-else class="leaderboard-list">
        <div
          v-for="item in incomeListAfter3"
          :key="item.userId"
          :class="['leaderboard-item', { 'is-me': item.isMe }, 'rank-' + item.rank]"
        >
          <span class="leaderboard-rank">{{ item.rank }}</span>
          <span class="leaderboard-avatar">{{ item.nickname.charAt(0) }}</span>
          <span class="leaderboard-nickname">{{ item.nickname }}</span>
          <span v-if="item.isMe" class="leaderboard-me-tag">我</span>
          <div v-if="incomeRewardLabel(item)" :class="['leaderboard-reward', incomeRewardLabel(item).type]">
            {{ incomeRewardLabel(item).text }}
          </div>
          <span class="leaderboard-amount">{{ item.amount.toFixed(2) }} 元</span>
        </div>
      </div>

      <!-- 我的申报 -->
      <div class="leaderboard-submissions">
        <div class="leaderboard-submissions-header">
          <span class="leaderboard-submissions-title">我的收入申报</span>
          <span class="leaderboard-submissions-hint">审核通过后金额将计入榜单</span>
        </div>
        <div v-if="mySubmissions.length === 0" class="leaderboard-empty">
          暂无申报记录
        </div>
        <div v-else class="leaderboard-submission-list">
          <div
            v-for="s in mySubmissions"
            :key="s.id"
            :class="['leaderboard-submission-item', s.status]"
          >
            <div class="submission-info">
              <div class="submission-month">{{ s.month }}</div>
              <div class="submission-meta">
                {{ s.amount.toFixed(2) }} 元 · {{ statusText(s.status) }}
              </div>
            </div>
            <div v-if="s.status === 'rejected' && s.rejectReason" class="submission-reason">
              原因：{{ s.rejectReason }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 规则说明 -->
    <a-modal
      v-model:open="rulesVisible"
      title="收益排行榜规则"
      :footer="null"
      :width="560"
      centered
      class="leaderboard-rules-modal"
    >
      <ol class="leaderboard-rules-list">
        <li><span class="leaderboard-rules-highlight">创作币榜</span>按自然月统计平台创作币收益，数据自动汇总，无需手动申报。</li>
        <li><span class="leaderboard-rules-highlight">自媒体收入榜</span>分为月度榜和年度榜，需填写收入金额并上传平台收益截图，审核通过后计入榜单。</li>
        <li>每个自然月的 <span class="leaderboard-rules-highlight">创作币榜 TOP 10</span> 与 <span class="leaderboard-rules-highlight">自媒体收入榜月度 TOP 10</span> 均可获得 <span class="leaderboard-rules-highlight">100 创作币</span>奖励。</li>
        <li>奖励在榜单结算后自动发放至账户余额，同一人同一榜单同一周期只发放一次。</li>
        <li>严禁提交虚假收入截图，一经查实将取消当月排名与奖励资格。</li>
      </ol>
      <div class="leaderboard-rules-footer">* 活动最终解释权归平台所有。</div>
      <div class="leaderboard-rules-guide-link">
        <router-link to="/guide">阅读完整玩法指南 →</router-link>
      </div>
    </a-modal>

    <!-- 收入申报 -->
    <a-modal
      v-model:open="submitVisible"
      title="申报自媒体收入"
      :footer="null"
      :width="480"
      centered
      class="leaderboard-submit-modal"
      @cancel="resetSubmitForm"
    >
      <div class="leaderboard-submit-form">
        <div class="form-row">
          <label class="form-label">所属月份</label>
          <div class="form-static">{{ currentMonth }}</div>
        </div>
        <div class="form-row">
          <label class="form-label">收入金额（元）</label>
          <input
            v-model.number="submitAmount"
            type="number"
            min="0"
            step="0.01"
            class="form-input"
            placeholder="请输入收入金额"
          />
        </div>
        <div class="form-row">
          <label class="form-label">收益截图 <span class="form-label-hint">可上传多张，支持多平台</span></label>
          <div class="form-upload-grid">
            <div
              v-for="(src, index) in submitScreenshots"
              :key="index"
              class="form-upload-item"
            >
              <img :src="src" class="form-upload-preview" />
              <button class="form-upload-remove" @click="removeScreenshot(index)">×</button>
            </div>
            <div class="form-upload form-upload-add">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                multiple
                class="form-file"
                @change="handleFileChange"
              />
              <div class="form-upload-placeholder">+</div>
            </div>
          </div>
        </div>
        <div class="form-actions">
          <button class="form-btn form-btn-default" @click="closeSubmitModal">取消</button>
          <button class="form-btn form-btn-primary" @click="handleSubmit">提交申报</button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  getCoinLeaderboard,
  getIncomeLeaderboard,
  submitIncomeSubmission,
  getMyIncomeSubmissions,
  getRewardRecord
} from '@/composables/useLeaderboard.js'

function getMonthOptions() {
  const options = []
  const now = new Date()
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return options
}

function getYearOptions() {
  const options = []
  const now = new Date()
  for (let i = 0; i < 3; i++) {
    options.push(String(now.getFullYear() - i))
  }
  return options
}

const monthOptions = getMonthOptions()
const yearOptions = getYearOptions()

const activeTab = ref('coin')
const incomePeriodType = ref('month')
const incomePeriodValue = ref(monthOptions[0])
const rulesVisible = ref(false)
const submitVisible = ref(false)

const incomePeriodLabel = computed(() => {
  return incomePeriodType.value === 'month' ? currentIncomeMonth : currentIncomeYear
})

function setIncomePeriodType(type) {
  incomePeriodType.value = type
  incomePeriodValue.value = type === 'month' ? currentIncomeMonth : currentIncomeYear
}

const coinList = computed(() => getCoinLeaderboard(currentCoinMonth))
const coinTop3 = computed(() => coinList.value.slice(0, 3))
const coinListAfter3 = computed(() => coinList.value.slice(3))

const incomeList = computed(() =>
  getIncomeLeaderboard(incomePeriodType.value, incomePeriodValue.value)
)
const incomeTop3 = computed(() => incomeList.value.slice(0, 3))
const incomeListAfter3 = computed(() => incomeList.value.slice(3))

const currentCoinMonth = monthOptions[0]
const currentIncomeMonth = monthOptions[0]
const currentIncomeYear = yearOptions[0]

const myCoinItem = computed(() => coinList.value.find(i => i.isMe))
const myIncomeItem = computed(() => incomeList.value.find(i => i.isMe))

const coinRewardBanner = computed(() => ({
  class: 'is-current',
  title: '本月 TOP 10 当月可获 100 创作币奖励',
  desc: '当前榜单进行中，下月 1 日自动结算，奖励发放至账户余额'
}))

const incomeRewardBanner = computed(() => {
  const isCurrent = incomePeriodValue.value === currentIncomeMonth
  return {
    class: isCurrent ? 'is-current' : 'is-past',
    title: isCurrent ? '本月度 TOP 10 当月可获 100 创作币奖励' : '历史月榜单已结算',
    desc: isCurrent
      ? '当前榜单进行中，下月 1 日自动结算，奖励发放至账户余额'
      : '该月榜单已结算，TOP 10 奖励已发放'
  }
})

function myRewardStatus(item, type) {
  if (!item || item.rank > 10) return null
  if (item.rank <= 3) {
    return {
      label: `已锁定 TOP ${item.rank}`,
      desc: '本月榜单进行中，结算后自动发放',
      type: 'pending'
    }
  }
  return {
    label: `进入 TOP 10（第 ${item.rank} 名）`,
    desc: '本月榜单进行中，结算后自动发放',
    type: 'pending'
  }
}

const myCoinStatus = computed(() => {
  if (!myCoinItem.value) return null
  if (myCoinItem.value.rank > 10) return null
  return myRewardStatus(myCoinItem.value, 'coin')
})

const myIncomeStatus = computed(() => {
  if (incomePeriodType.value !== 'month') return null
  if (!myIncomeItem.value) return null
  if (myIncomeItem.value.rank > 10) return null
  return myRewardStatus(myIncomeItem.value, 'income')
})

function coinRewardLabel(item) {
  if (item.rank > 10) return null
  const record = getRewardRecord('coin', currentCoinMonth, item.userId)
  if (record) return { text: '已获 100 创作币', type: 'awarded' }
  return { text: '本月榜单进行中，待结算', type: 'pending' }
}

function incomeRewardLabel(item) {
  const isCurrent =
    (incomePeriodType.value === 'month' && incomePeriodValue.value === currentIncomeMonth) ||
    (incomePeriodType.value === 'year' && incomePeriodValue.value === currentIncomeYear)
  if (item.rank > 10) return null
  const record = getRewardRecord('income', incomePeriodValue.value, item.userId)
  if (record) return { text: '已获 100 创作币', type: 'awarded' }
  if (isCurrent) return { text: '榜单进行中，待结算', type: 'pending' }
  return null
}

const mySubmissions = computed(() => getMyIncomeSubmissions())

function statusText(status) {
  const map = { pending: '审核中', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

// 申报表单
const currentMonth = monthOptions[0]
const submitAmount = ref('')
const submitScreenshots = ref([])
const fileInput = ref(null)

function openSubmitModal() {
  submitAmount.value = ''
  submitScreenshots.value = []
  if (fileInput.value) fileInput.value.value = ''
  submitVisible.value = true
}

function closeSubmitModal() {
  submitVisible.value = false
  resetSubmitForm()
}

function resetSubmitForm() {
  submitAmount.value = ''
  submitScreenshots.value = []
  if (fileInput.value) fileInput.value.value = ''
}

function handleFileChange(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  files.forEach(file => {
    const reader = new FileReader()
    reader.onload = (event) => {
      submitScreenshots.value.push(event.target.result)
    }
    reader.readAsDataURL(file)
  })
  if (fileInput.value) fileInput.value.value = ''
}

function removeScreenshot(index) {
  submitScreenshots.value.splice(index, 1)
}

function handleSubmit() {
  const amount = Number(submitAmount.value)
  if (!Number.isFinite(amount) || amount <= 0) {
    message.error('请输入有效的收入金额')
    return
  }
  if (submitScreenshots.value.length === 0) {
    message.error('请上传收益截图')
    return
  }
  try {
    submitIncomeSubmission({
      month: currentMonth,
      amount,
      screenshots: submitScreenshots.value
    })
    message.success('收入申报已提交，等待审核')
    closeSubmitModal()
  } catch (err) {
    message.error(err.message || '提交失败')
  }
}
</script>

<style scoped>
.leaderboard-page {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.leaderboard-header {
  margin-bottom: 20px;
}

.leaderboard-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.leaderboard-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.leaderboard-rules-link {
  color: #ff2442;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
  margin-left: 8px;
}

.leaderboard-rules-link:hover {
  color: #e61e3a;
}

.leaderboard-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  width: fit-content;
}

.leaderboard-tab {
  padding: 8px 20px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.leaderboard-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.leaderboard-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.leaderboard-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.leaderboard-toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.leaderboard-select {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  color: #1a1a1a;
  cursor: pointer;
}

.leaderboard-period-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
}

.leaderboard-period-tab {
  padding: 6px 14px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.leaderboard-period-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.leaderboard-period-label {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.leaderboard-submit-btn {
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 8px;
  background: #ff2442;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.leaderboard-submit-btn:hover {
  background: #e61e3a;
}

.reward-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid;
}

.reward-banner.is-current {
  background: linear-gradient(135deg, #fff7e6 0%, #fff 100%);
  border-color: #ffd591;
}

.reward-banner.is-past {
  background: #f5f5f5;
  border-color: #e8e8e8;
}

.reward-banner-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.reward-banner-text {
  flex: 1;
  min-width: 0;
}

.reward-banner-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.reward-banner-desc {
  font-size: 13px;
  color: #595959;
}

.my-reward-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fff0f2 0%, #fff 100%);
  border: 1px solid #ffd1d9;
  border-radius: 12px;
}

.my-reward-rank {
  font-size: 22px;
  font-weight: 700;
  color: #ff2442;
  flex-shrink: 0;
}

.my-reward-info {
  flex: 1;
  min-width: 0;
}

.my-reward-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.my-reward-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.my-reward-amount {
  font-size: 18px;
  font-weight: 700;
  color: #ff2442;
  flex-shrink: 0;
}

.leaderboard-top3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.leaderboard-top-card {
  position: relative;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 24px 16px;
  text-align: center;
}

.leaderboard-top-card.top-1 {
  background: linear-gradient(180deg, #fff7e6 0%, #fff 100%);
  border-color: #ffd591;
}

.leaderboard-top-card.top-2 {
  background: linear-gradient(180deg, #f6ffed 0%, #fff 100%);
  border-color: #b7eb8f;
}

.leaderboard-top-card.top-3 {
  background: linear-gradient(180deg, #e6f7ff 0%, #fff 100%);
  border-color: #91d5ff;
}

.top-rank {
  font-size: 28px;
  font-weight: 700;
  color: #d48806;
  margin-bottom: 8px;
}

.top-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.top-amount {
  font-size: 18px;
  font-weight: 700;
  color: #ff2442;
}

.top-me-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 11px;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}

.leaderboard-reward,
.top-reward {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.top-reward {
  position: absolute;
  top: 10px;
  right: 10px;
}

.leaderboard-reward.awarded,
.top-reward.awarded {
  background: #f6ffed;
  color: #389e0d;
}

.leaderboard-reward.pending,
.top-reward.pending {
  background: #f5f5f5;
  color: #8c8c8c;
}

.leaderboard-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}

.leaderboard-item.is-me {
  background: #fff0f2;
  border-color: #ffd1d9;
}

.leaderboard-rank {
  width: 28px;
  text-align: center;
  font-weight: 700;
  color: #8c8c8c;
}

.leaderboard-item.rank-1 .leaderboard-rank { color: #cf1322; }
.leaderboard-item.rank-2 .leaderboard-rank { color: #d48806; }
.leaderboard-item.rank-3 .leaderboard-rank { color: #389e0d; }

.leaderboard-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #595959;
}

.leaderboard-nickname {
  flex: 1;
  font-size: 15px;
  color: #1a1a1a;
}

.leaderboard-me-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}

.leaderboard-amount {
  font-size: 15px;
  font-weight: 600;
  color: #ff2442;
}

.leaderboard-empty {
  padding: 48px 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  background: #fff;
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
}

.leaderboard-submissions {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
}

.leaderboard-submissions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.leaderboard-submissions-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.leaderboard-submissions-hint {
  font-size: 12px;
  color: #8c8c8c;
}

.leaderboard-submission-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.leaderboard-submission-item {
  padding: 12px;
  border-radius: 8px;
  background: #fafafa;
  border-left: 3px solid #d9d9d9;
}

.leaderboard-submission-item.approved {
  border-left-color: #389e0d;
  background: #f6ffed;
}

.leaderboard-submission-item.rejected {
  border-left-color: #ff4d4f;
  background: #fff1f0;
}

.leaderboard-submission-item.pending {
  border-left-color: #faad14;
  background: #fffbe6;
}

.submission-month {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.submission-meta {
  font-size: 13px;
  color: #595959;
  margin-top: 2px;
}

.submission-reason {
  font-size: 12px;
  color: #ff4d4f;
  margin-top: 6px;
}

.leaderboard-rules-list {
  padding-left: 18px;
  margin: 0;
  color: #595959;
  font-size: 14px;
  line-height: 1.8;
}

.leaderboard-rules-list li {
  margin-bottom: 10px;
}

.leaderboard-rules-highlight {
  color: #ff2442;
  font-weight: 500;
}

.leaderboard-rules-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
}

.leaderboard-rules-guide-link {
  margin-top: 12px;
  font-size: 14px;
  text-align: right;
}

.leaderboard-rules-guide-link a {
  color: #ff2442;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.leaderboard-rules-guide-link a:hover {
  color: #e61e3a;
}

.leaderboard-submit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
}

.form-select,
.form-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  color: #1a1a1a;
}

.form-input::placeholder {
  color: #bfbfbf;
}

.form-static {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  background: #f5f5f5;
  color: #595959;
  display: flex;
  align-items: center;
}

.form-label-hint {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 400;
  margin-left: 4px;
}

.form-upload-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.form-upload-item {
  position: relative;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.form-upload {
  position: relative;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
}

.form-upload-add {
  background: #fafafa;
}

.form-upload-add .form-upload-placeholder {
  font-size: 28px;
  font-weight: 300;
  color: #bfbfbf;
}

.form-file {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  z-index: 1;
}

.form-upload-placeholder {
  font-size: 13px;
  color: #8c8c8c;
  pointer-events: none;
}

.form-upload-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-upload-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.form-upload-remove:hover {
  background: rgba(0, 0, 0, 0.7);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.form-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.form-btn-default {
  background: #fff;
  border-color: #d9d9d9;
  color: #595959;
}

.form-btn-default:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.form-btn-primary {
  background: #ff2442;
  color: #fff;
}

.form-btn-primary:hover {
  background: #e61e3a;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .leaderboard-page {
    padding: 16px;
  }

  .leaderboard-tabs {
    width: 100%;
  }

  .leaderboard-tab {
    flex: 1;
  }

  .leaderboard-top3 {
    grid-template-columns: 1fr;
  }

  .leaderboard-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .leaderboard-toolbar-left {
    justify-content: space-between;
  }

  .leaderboard-submit-btn {
    width: 100%;
  }

  .leaderboard-item {
    flex-wrap: wrap;
    gap: 8px;
  }

  .leaderboard-amount {
    margin-left: auto;
  }

  .leaderboard-reward {
    margin-left: 48px;
  }

  .leaderboard-submissions-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .reward-banner {
    padding: 12px 14px;
    gap: 10px;
  }

  .reward-banner-icon {
    font-size: 24px;
  }

  .reward-banner-title {
    font-size: 14px;
  }

  .reward-banner-desc {
    font-size: 12px;
  }

  .my-reward-card {
    flex-wrap: wrap;
    padding: 12px 14px;
    gap: 10px;
  }

  .my-reward-amount {
    margin-left: auto;
  }
}
</style>

<style>
/* 暗色主题：全局覆盖（弹框 teleport 到 body，需非 scoped） */
body[data-theme="dark"] .leaderboard-page {
  background: #141414;
}

body[data-theme="dark"] .leaderboard-title,
body[data-theme="dark"] .form-label,
body[data-theme="dark"] .submission-month,
body[data-theme="dark"] .leaderboard-submissions-title,
body[data-theme="dark"] .top-nickname,
body[data-theme="dark"] .leaderboard-nickname,
body[data-theme="dark"] .leaderboard-period-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .leaderboard-subtitle,
body[data-theme="dark"] .leaderboard-submissions-hint,
body[data-theme="dark"] .submission-meta,
body[data-theme="dark"] .form-upload-placeholder,
body[data-theme="dark"] .leaderboard-empty {
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-tabs,
body[data-theme="dark"] .leaderboard-period-tabs {
  background: #1f1f1f;
}

body[data-theme="dark"] .leaderboard-tab.active,
body[data-theme="dark"] .leaderboard-period-tab.active {
  background: #2a2a2a;
  color: #e0e0e0;
  box-shadow: none;
}

body[data-theme="dark"] .leaderboard-tab,
body[data-theme="dark"] .leaderboard-period-tab {
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-top-card,
body[data-theme="dark"] .leaderboard-item,
body[data-theme="dark"] .leaderboard-submissions,
body[data-theme="dark"] .form-select,
body[data-theme="dark"] .form-input,
body[data-theme="dark"] .leaderboard-select {
  background: #1f1f1f;
  border-color: #2a2a2a;
  color: #e0e0e0;
}

body[data-theme="dark"] .leaderboard-item.is-me {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .leaderboard-top-card.top-1 {
  background: linear-gradient(180deg, rgba(255, 247, 230, 0.1) 0%, #1f1f1f 100%);
  border-color: rgba(255, 213, 145, 0.3);
}

body[data-theme="dark"] .leaderboard-top-card.top-2 {
  background: linear-gradient(180deg, rgba(246, 255, 237, 0.1) 0%, #1f1f1f 100%);
  border-color: rgba(183, 235, 143, 0.3);
}

body[data-theme="dark"] .leaderboard-top-card.top-3 {
  background: linear-gradient(180deg, rgba(230, 247, 255, 0.1) 0%, #1f1f1f 100%);
  border-color: rgba(145, 213, 255, 0.3);
}

body[data-theme="dark"] .reward-banner.is-current {
  background: linear-gradient(135deg, rgba(255, 247, 230, 0.1) 0%, #1f1f1f 100%);
  border-color: rgba(255, 213, 145, 0.3);
}

body[data-theme="dark"] .reward-banner.is-past {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .reward-banner-title,
body[data-theme="dark"] .my-reward-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .reward-banner-desc,
body[data-theme="dark"] .my-reward-desc {
  color: #8c8c8c;
}

body[data-theme="dark"] .my-reward-card {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .leaderboard-avatar {
  background: #2a2a2a;
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-empty {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-submission-item {
  background: #141414;
}

body[data-theme="dark"] .form-upload,
body[data-theme="dark"] .form-upload-item,
body[data-theme="dark"] .form-upload-add {
  border-color: #2a2a2a;
  background: #1f1f1f;
}

body[data-theme="dark"] .form-static {
  background: #141414;
  border-color: #2a2a2a;
  color: #8c8c8c;
}

body[data-theme="dark"] .form-label-hint {
  color: #8c8c8c;
}

body[data-theme="dark"] .form-btn-default {
  background: #1f1f1f;
  border-color: #2a2a2a;
  color: #e0e0e0;
}

body[data-theme="dark"] .form-btn-default:hover {
  border-color: #ff2442;
  color: #ff2442;
}

body[data-theme="dark"] .leaderboard-rules-list {
  color: #b0b0b0;
}

body[data-theme="dark"] .leaderboard-rules-footer {
  border-top-color: #2a2a2a;
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-rules-guide-link a {
  color: #ff4d6f;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-content,
body[data-theme="dark"] .leaderboard-submit-modal .ant-modal-content,
body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-header,
body[data-theme="dark"] .leaderboard-submit-modal .ant-modal-header {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-title,
body[data-theme="dark"] .leaderboard-submit-modal .ant-modal-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-close,
body[data-theme="dark"] .leaderboard-submit-modal .ant-modal-close {
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-close:hover,
body[data-theme="dark"] .leaderboard-submit-modal .ant-modal-close:hover {
  color: #ff2442;
}
</style>

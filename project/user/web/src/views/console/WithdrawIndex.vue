<template>
  <div class="coin-page">
    <!-- 页面头部 -->
    <div class="coin-page-header">
      <div class="coin-page-title-wrap">
        <h2 class="coin-page-title">创作币 &amp; 提现</h2>
        <p class="coin-page-desc">
          查看你的创作币余额、提现记录，并申请提现到支付宝。
          <span class="coin-rules-link" @click="rulesVisible = true">提现规则</span>
        </p>
      </div>
      <button class="invite-btn invite-btn-secondary" @click="goBack">{{ backText }}</button>
    </div>

    <!-- 实名认证状态 -->
    <div class="coin-section">
      <div class="coin-section-header">
        <span class="coin-section-title">实名认证</span>
        <span :class="['coin-auth-status', realNameVerified ? 'verified' : 'unverified']">
          {{ realNameVerified ? '已认证' : '未认证' }}
        </span>
      </div>

      <div v-if="!realNameVerified" class="coin-auth-form">
        <div class="coin-auth-tip">为保障资金安全，申请提现前请先完成实名认证。仅需填写一次。</div>
        <div class="coin-form-row">
          <div class="coin-form-field">
            <label class="coin-form-label">真实姓名</label>
            <input v-model="realName" class="coin-form-input" placeholder="请输入真实姓名" maxlength="20" />
          </div>
          <div class="coin-form-field">
            <label class="coin-form-label">身份证号</label>
            <input v-model="idCard" class="coin-form-input" placeholder="请输入 18 位身份证号" maxlength="18" />
          </div>
        </div>
        <div class="coin-form-actions">
          <button class="invite-btn invite-btn-primary" :disabled="!canSubmitRealName" @click="submitRealName">
            提交实名认证
          </button>
        </div>
      </div>

      <div v-else class="coin-auth-display">
        <div class="coin-auth-display-row">
          <span class="coin-auth-display-label">真实姓名</span>
          <span class="coin-auth-display-value">{{ realName }}</span>
        </div>
        <div class="coin-auth-display-row">
          <span class="coin-auth-display-label">身份证号</span>
          <span class="coin-auth-display-value">{{ maskedIdCard }}</span>
        </div>
      </div>
    </div>

    <!-- 余额统计 -->
    <div class="coin-section">
      <div class="coin-section-header">
        <span class="coin-section-title">账户概览</span>
      </div>
      <div class="coin-stat-grid">
        <div class="coin-stat-card">
          <div class="coin-stat-label">可提现金额</div>
          <div class="coin-stat-value">{{ coinBalance }} <span class="coin-stat-unit">创作币</span></div>
          <div class="coin-stat-hint">1 创作币 = 1 元，满 100 可提现</div>
        </div>
        <div class="coin-stat-card">
          <div class="coin-stat-label">已提现金额</div>
          <div class="coin-stat-value">{{ withdrawnTotal }} <span class="coin-stat-unit">创作币</span></div>
          <div class="coin-stat-hint">含已成功到账与审核中金额</div>
        </div>
        <div class="coin-stat-card">
          <div class="coin-stat-label">累计获得</div>
          <div class="coin-stat-value">{{ totalEarned }} <span class="coin-stat-unit">创作币</span></div>
          <div class="coin-stat-hint">来自好友下单返佣</div>
        </div>
      </div>

      <div class="coin-eligibility">
        <div :class="['coin-eligibility-tag', eligibilityLevel]">{{ eligibilityText }}</div>
        <div class="coin-eligibility-tip">{{ eligibilityTip }}</div>
      </div>

      <div class="coin-withdraw-action">
        <button
          class="invite-btn invite-btn-primary coin-withdraw-btn"
          :disabled="!canApplyWithdraw"
          @click="openApplyModal"
        >
          {{ applyButtonText }}
        </button>
      </div>
    </div>

    <!-- 提现记录 -->
    <div class="coin-section">
      <div class="coin-section-header">
        <span class="coin-section-title">提现记录</span>
        <span class="coin-section-extra">共 {{ withdrawRecords.length }} 条</span>
      </div>

      <div v-if="withdrawRecords.length === 0" class="coin-records-empty">
        暂无提现记录
      </div>
      <div v-else class="coin-records-table">
        <div class="coin-records-head">
          <div class="coin-records-cell coin-records-time">申请时间</div>
          <div class="coin-records-cell coin-records-amount">提现金额</div>
          <div class="coin-records-cell coin-records-account">收款账号</div>
          <div class="coin-records-cell coin-records-status">状态</div>
        </div>
        <div v-for="r in withdrawRecords" :key="r.id" class="coin-records-row">
          <div class="coin-records-cell coin-records-time">{{ formatTime(r.createdAt) }}</div>
          <div class="coin-records-cell coin-records-amount">{{ r.amount }} 创作币</div>
          <div class="coin-records-cell coin-records-account">{{ r.account }}</div>
          <div class="coin-records-cell coin-records-status">
            <span :class="['coin-records-status-tag', r.status]">{{ statusText(r.status) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 申请提现弹框 -->
    <a-modal
      v-model:open="applyVisible"
      :footer="null"
      :width="420"
      centered
      class="coin-apply-modal"
    >
      <div class="coin-apply-panel">
        <div class="coin-apply-title">申请提现</div>
        <div class="coin-apply-item">
          <label class="coin-apply-label">可提现余额</label>
          <div class="coin-apply-balance">{{ coinBalance }} 创作币</div>
        </div>
        <div class="coin-apply-item">
          <label class="coin-apply-label">提现金额</label>
          <input
            v-model.number="applyAmount"
            class="coin-apply-input"
            type="number"
            min="100"
            :max="coinBalance"
            placeholder="最低 100"
          />
          <div class="coin-apply-hint">1 创作币 = 1 元，满 100 可提现</div>
        </div>
        <div class="coin-apply-item">
          <label class="coin-apply-label">支付宝账号</label>
          <input v-model="applyAccount" class="coin-apply-input" placeholder="请输入支付宝账号" />
        </div>
        <div class="coin-apply-item">
          <label class="coin-apply-label">真实姓名</label>
          <input
            v-model="applyName"
            class="coin-apply-input coin-apply-input-disabled"
            :disabled="true"
            :placeholder="realNameVerified ? realName : '请先完成实名认证'"
          />
        </div>
        <div class="coin-apply-agreement">
          <label class="coin-apply-agreement-label">
            <input v-model="agreementAccepted" type="checkbox" class="coin-apply-agreement-checkbox" @change="saveAgreement" />
            <span>我已阅读并同意</span>
            <span class="coin-apply-agreement-link" @click="agreementModalVisible = true">《提现服务协议》</span>
          </label>
        </div>
        <div class="coin-apply-actions">
          <button class="invite-btn invite-btn-secondary" @click="applyVisible = false">取消</button>
          <button class="invite-btn invite-btn-primary" :disabled="!canSubmitApply" @click="submitApply">提交申请</button>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="rulesVisible"
      title="提现规则"
      :footer="null"
      :width="560"
      centered
      class="coin-rules-modal"
    >
      <ol class="coin-rules-list">
        <li>仅<span class="coin-rules-highlight">付费邀请用户</span>可申请提现，且需至少邀请 <span class="coin-rules-highlight">3</span> 位<span class="coin-rules-highlight">付费好友</span>。</li>
        <li>创作币余额满 <span class="coin-rules-highlight">100</span> 即可提现，<span class="coin-rules-highlight">1 创作币 = 1 元</span>人民币。</li>
        <li>提现申请提交后约 <span class="coin-rules-highlight">1 个工作日</span>审核，预计 <span class="coin-rules-highlight">7 个工作日内</span>到账。</li>
        <li>目前仅支持<span class="coin-rules-highlight">支付宝</span>提现，请确保<span class="coin-rules-highlight">支付宝账号</span>和<span class="coin-rules-highlight">真实姓名</span>与本人一致。</li>
        <li>因<span class="coin-rules-highlight">账号信息错误</span>、<span class="coin-rules-highlight">账户异常</span>等原因导致的提现失败，平台概不负责。</li>
      </ol>
      <div class="coin-rules-footer">* 活动最终解释权归平台所有。</div>
    </a-modal>

    <a-modal
      v-model:open="agreementModalVisible"
      title="提现服务协议"
      :footer="null"
      :width="560"
      centered
      class="coin-rules-modal"
    >
      <ol class="coin-rules-list">
        <li>用户承诺提现资金来源合法，不得利用本平台进行洗钱、套现、赌博、诈骗等违法活动。</li>
        <li>用户申请提现的账户信息（支付宝账号、真实姓名）必须与本人实名认证信息一致。</li>
        <li>用户应确保邀请好友行为真实有效，禁止通过虚假注册、刷单、机器刷量等方式获取创作币。</li>
        <li>平台有权对异常提现行为进行审核、延迟到账、拒绝提现或冻结相关收益。</li>
        <li>因用户提供错误账户信息、账户异常或违反法律法规导致的提现失败，平台不承担任何责任。</li>
        <li>平台可根据法律法规及业务需要调整本协议内容，调整后会通过官方渠道通知用户。</li>
      </ol>
      <div class="coin-rules-footer">* 本协议内容仅供参考，具体以平台最终公示为准。</div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { useInviteStats } from '@/composables/useInviteStats'

const { coinBalance, inviteStats, loadInviteStats } = useInviteStats()

const WITHDRAW_REQUESTS_KEY = 'aichuangzuo_withdraw_requests'
const REAL_NAME_KEY = 'aichuangzuo_real_name_info'
const WITHDRAW_AGREEMENT_KEY = 'aichuangzuo_withdraw_agreement_accepted'

const router = useRouter()
const route = useRoute()

const realName = ref('')
const idCard = ref('')
const realNameVerified = ref(false)
const withdrawRecords = ref([])
const applyVisible = ref(false)
const applyAmount = ref(null)
const applyAccount = ref('')
const rulesVisible = ref(false)
const agreementModalVisible = ref(false)
const agreementAccepted = ref(false)

const totalEarned = computed(() => inviteStats.value.coinEarned || 0)

const maskedIdCard = computed(() => {
  const v = idCard.value || ''
  if (v.length < 8) return v
  return v.slice(0, 4) + '**********' + v.slice(-4)
})

const canSubmitRealName = computed(() => {
  return realName.value.trim().length >= 2 && /^\d{17}[\dXx]$/.test(idCard.value.trim())
})

const withdrawnTotal = computed(() => {
  return withdrawRecords.value
    .filter((r) => r.status !== 'rejected')
    .reduce((sum, r) => sum + (r.amount || 0), 0)
})

const eligibilityLevel = computed(() => {
  if (!realNameVerified.value) return 'disabled'
  if (coinBalance.value < 100) return 'low'
  if (withdrawRecords.value.some((r) => r.status === 'pending')) return 'pending'
  return 'ready'
})

const eligibilityText = computed(() => {
  switch (eligibilityLevel.value) {
    case 'disabled': return '暂不可提现'
    case 'low': return '余额不足'
    case 'pending': return '审核中'
    case 'ready': return '可申请提现'
    default: return ''
  }
})

const eligibilityTip = computed(() => {
  switch (eligibilityLevel.value) {
    case 'disabled': return '请先完成实名认证，再申请提现。'
    case 'low': return `当前余额 ${coinBalance.value} 创作币，满 100 才可提现。`
    case 'pending': return '你有提现申请正在审核中，请耐心等待。'
    case 'ready': return '账户状态良好，可立即申请提现。'
    default: return ''
  }
})

const canApplyWithdraw = computed(() => eligibilityLevel.value === 'ready')

const applyButtonText = computed(() => {
  if (!realNameVerified.value) return '请先实名认证'
  if (coinBalance.value < 100) return '余额不足 100'
  if (withdrawRecords.value.some((r) => r.status === 'pending')) return '审核中'
  return '申请提现'
})

const canSubmitApply = computed(() => {
  const amount = Number(applyAmount.value)
  if (!amount || amount < 100 || amount > coinBalance.value) return false
  if (!applyAccount.value.trim()) return false
  if (!realNameVerified.value) return false
  if (!agreementAccepted.value) return false
  return true
})

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const statusText = (status) => {
  switch (status) {
    case 'pending': return '审核中'
    case 'approved': return '已到账'
    case 'rejected': return '已拒绝'
    default: return status
  }
}

const loadRealName = () => {
  const raw = localStorage.getItem(REAL_NAME_KEY)
  if (raw) {
    try {
      const info = JSON.parse(raw)
      realName.value = info.realName || ''
      idCard.value = info.idCard || ''
      realNameVerified.value = true
    } catch (e) {
      realNameVerified.value = false
    }
  }
}

const loadWithdrawRecords = () => {
  const raw = localStorage.getItem(WITHDRAW_REQUESTS_KEY)
  if (raw) {
    try {
      withdrawRecords.value = JSON.parse(raw)
    } catch (e) {
      withdrawRecords.value = []
    }
  } else {
    withdrawRecords.value = []
  }
}

const loadAgreement = () => {
  const raw = localStorage.getItem(WITHDRAW_AGREEMENT_KEY)
  agreementAccepted.value = raw === 'true'
}

const saveAgreement = () => {
  localStorage.setItem(WITHDRAW_AGREEMENT_KEY, String(agreementAccepted.value))
}

const submitRealName = () => {
  if (!canSubmitRealName.value) {
    message.warning('请填写真实姓名和 18 位身份证号')
    return
  }
  const info = {
    realName: realName.value.trim(),
    idCard: idCard.value.trim(),
    verifiedAt: new Date().toISOString()
  }
  localStorage.setItem(REAL_NAME_KEY, JSON.stringify(info))
  realNameVerified.value = true
  message.success('实名认证成功')
}

const openApplyModal = () => {
  if (!realNameVerified.value) {
    message.warning('请先完成实名认证')
    return
  }
  if (coinBalance.value < 100) {
    message.warning('余额不足 100 创作币')
    return
  }
  if (withdrawRecords.value.some((r) => r.status === 'pending')) {
    message.warning('你有正在审核中的提现申请')
    return
  }
  loadAgreement()
  applyAmount.value = null
  applyAccount.value = ''
  applyVisible.value = true
}

const submitApply = () => {
  if (!agreementAccepted.value) {
    message.warning('请先阅读并同意《提现服务协议》')
    return
  }
  if (!canSubmitApply.value) {
    message.warning('请完整填写提现信息')
    return
  }
  const amount = Number(applyAmount.value)
  const record = {
    id: 'WD' + Date.now(),
    amount,
    account: applyAccount.value.trim(),
    name: realName.value.trim(),
    status: 'pending',
    createdAt: new Date().toISOString()
  }
  const list = [...withdrawRecords.value]
  list.unshift(record)
  localStorage.setItem(WITHDRAW_REQUESTS_KEY, JSON.stringify(list))
  withdrawRecords.value = list

  coinBalance.value = Math.max(0, coinBalance.value - amount)

  applyVisible.value = false
  message.success('提现申请已提交，预计 7 天内到账')
}

const backText = computed(() => {
  return route.query.from === 'account' ? '返回我的账户' : '返回邀请有礼'
})

const goBack = () => {
  if (route.query.from === 'account') {
    router.push('/console/earnings')
  } else {
    router.push({ path: '/console/create', query: { openInvite: '1' } })
  }
}

onMounted(() => {
  loadRealName()
  loadInviteStats()
  loadWithdrawRecords()
  loadAgreement()
})
</script>

<style scoped>
.coin-page {
  padding: 28px 32px;
  max-width: 880px;
  margin: 0 auto;
}

.coin-page-title-wrap {
  flex: 1;
  min-width: 0;
}

.coin-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.coin-page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary, #1f1f1f);
}

.coin-page-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary, #595959);
}

.coin-rules-link {
  color: #ff2442;
  cursor: pointer;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
  margin-left: 8px;
}

.coin-rules-link:hover {
  color: #e61e3a;
}

.coin-rules-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
}

.coin-rules-list li {
  margin-bottom: 10px;
}

.coin-rules-footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #8c8c8c;
}

.coin-rules-highlight {
  color: #ff2442;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

body[data-theme="dark"] .coin-rules-highlight {
  color: #ff4d6f;
}

.coin-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 22px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.coin-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.coin-section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #1f1f1f);
}

.coin-section-extra {
  font-size: 12px;
  color: #8c8c8c;
}

.coin-auth-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.coin-auth-status.verified {
  background: rgba(82, 196, 26, 0.12);
  color: #52c41a;
}

.coin-auth-status.unverified {
  background: rgba(255, 77, 79, 0.12);
  color: #ff4d4f;
}

.coin-auth-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid var(--color-primary, #FF2442);
}

.coin-form-row {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 14px;
}

.coin-form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.coin-form-label {
  font-size: 12px;
  color: #595959;
}

.coin-form-input {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.coin-form-input:focus {
  border-color: var(--color-primary, #FF2442);
}

.coin-form-actions {
  margin-top: 14px;
}

.coin-form-actions .invite-btn {
  min-width: 140px;
}

.coin-auth-display {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coin-auth-display-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.coin-auth-display-label {
  color: #8c8c8c;
  width: 80px;
}

.coin-auth-display-value {
  color: #1f1f1f;
  font-weight: 500;
}

.coin-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.coin-stat-card {
  padding: 16px;
  background: linear-gradient(180deg, #fff8f9 0%, #ffffff 100%);
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}

.coin-stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.coin-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary, #FF2442);
  line-height: 1.2;
}

.coin-stat-unit {
  font-size: 12px;
  font-weight: 500;
  color: #595959;
  margin-left: 4px;
}

.coin-stat-hint {
  font-size: 11px;
  color: #bfbfbf;
  margin-top: 6px;
}

.coin-eligibility {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fafafa;
  border-radius: 8px;
  margin-top: 8px;
}

.coin-eligibility-tag {
  font-size: 12px;
  padding: 3px 12px;
  border-radius: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.coin-eligibility-tag.ready {
  background: rgba(82, 196, 26, 0.14);
  color: #389e0d;
}

.coin-eligibility-tag.low,
.coin-eligibility-tag.disabled {
  background: rgba(140, 140, 140, 0.14);
  color: #595959;
}

.coin-eligibility-tag.pending {
  background: rgba(250, 173, 20, 0.16);
  color: #d48806;
}

.coin-eligibility-tip {
  font-size: 12px;
  color: #595959;
}

.coin-withdraw-action {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.coin-withdraw-btn {
  min-width: 140px;
}

.coin-records-empty {
  text-align: center;
  padding: 36px 0;
  color: #bfbfbf;
  font-size: 13px;
}

.coin-records-table {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.coin-records-head,
.coin-records-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1.2fr 0.9fr;
  align-items: center;
  padding: 12px 14px;
}

.coin-records-head {
  background: #fafafa;
  font-size: 12px;
  color: #595959;
  font-weight: 500;
}

.coin-records-row {
  font-size: 13px;
  color: #1f1f1f;
  border-top: 1px solid #f0f0f0;
}

.coin-records-cell {
  padding: 0 6px;
}

.coin-records-amount {
  font-weight: 600;
  color: var(--color-primary, #FF2442);
}

.coin-records-status-tag {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.coin-records-status-tag.pending {
  background: rgba(250, 173, 20, 0.16);
  color: #d48806;
}

.coin-records-status-tag.approved {
  background: rgba(82, 196, 26, 0.14);
  color: #389e0d;
}

.coin-records-status-tag.rejected {
  background: rgba(255, 77, 79, 0.14);
  color: #cf1322;
}

/* 申请提现弹框 */
.coin-apply-modal .ant-modal-body {
  padding: 0;
}

.coin-apply-panel {
  padding: 24px;
}

.coin-apply-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #1f1f1f);
  margin-bottom: 18px;
}

.coin-apply-item {
  margin-bottom: 14px;
}

.coin-apply-label {
  display: block;
  font-size: 12px;
  color: #595959;
  margin-bottom: 6px;
}

.coin-apply-balance {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary, #FF2442);
}

.coin-apply-input {
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.coin-apply-input:focus {
  border-color: var(--color-primary, #FF2442);
}

.coin-apply-input-disabled {
  background: #fafafa;
  color: #595959;
  cursor: not-allowed;
}

.coin-apply-hint {
  font-size: 11px;
  color: #bfbfbf;
  margin-top: 4px;
}

.coin-apply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

/* 暗色主题 */
body[data-theme="dark"] .coin-page-title,
body[data-theme="dark"] .coin-section-title {
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .coin-page-desc {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .coin-section {
  background: #1f1f1f;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .coin-section-extra,
body[data-theme="dark"] .coin-stat-label,
body[data-theme="dark"] .coin-stat-hint,
body[data-theme="dark"] .coin-eligibility-tip,
body[data-theme="dark"] .coin-apply-label,
body[data-theme="dark"] .coin-apply-hint,
body[data-theme="dark"] .coin-form-label,
body[data-theme="dark"] .coin-auth-display-label {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .coin-stat-hint {
  color: rgba(255, 255, 255, 0.4);
}

body[data-theme="dark"] .coin-auth-tip {
  background: #2a2a2a;
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .coin-form-input,
body[data-theme="dark"] .coin-apply-input {
  background: #141414;
  border-color: #303030;
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .coin-form-input:focus,
body[data-theme="dark"] .coin-apply-input:focus {
  border-color: var(--color-primary, #FF2442);
}

body[data-theme="dark"] .coin-apply-input-disabled {
  background: #262626;
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .coin-stat-card {
  background: linear-gradient(180deg, #2a1f23 0%, #1f1f1f 100%);
  border-color: #303030;
}

body[data-theme="dark"] .coin-eligibility {
  background: #262626;
}

body[data-theme="dark"] .coin-records-table {
  border-color: #303030;
}

body[data-theme="dark"] .coin-records-head {
  background: #262626;
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .coin-records-row {
  color: rgba(255, 255, 255, 0.85);
  border-top-color: #303030;
}

body[data-theme="dark"] .coin-auth-display-value {
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .coin-records-empty {
  color: rgba(255, 255, 255, 0.35);
}

body[data-theme="dark"] .coin-auth-status.verified {
  background: rgba(82, 196, 26, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .coin-auth-status.unverified {
  background: rgba(255, 77, 79, 0.15);
  color: #ff7875;
}

body[data-theme="dark"] .coin-eligibility-tag.ready {
  background: rgba(82, 196, 26, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .coin-eligibility-tag.low,
body[data-theme="dark"] .coin-eligibility-tag.disabled {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .coin-eligibility-tag.pending {
  background: rgba(250, 173, 20, 0.15);
  color: #ffa940;
}

body[data-theme="dark"] .coin-records-status-tag.pending {
  background: rgba(250, 173, 20, 0.15);
  color: #ffa940;
}

body[data-theme="dark"] .coin-records-status-tag.approved {
  background: rgba(82, 196, 26, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .coin-records-status-tag.rejected {
  background: rgba(255, 77, 79, 0.15);
  color: #ff7875;
}

body[data-theme="dark"] .coin-rules-list {
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .coin-rules-footer {
  border-top-color: #303030;
  color: rgba(255, 255, 255, 0.45);
}

body[data-theme="dark"] .coin-rules-highlight {
  color: #ff4d6f;
}

body[data-theme="dark"] .coin-form-input::placeholder,
body[data-theme="dark"] .coin-apply-input::placeholder {
  color: rgba(255, 255, 255, 0.35);
}

body[data-theme="dark"] .coin-auth-tip {
  border-left-color: var(--color-primary, #ff2442);
}

body[data-theme="dark"] .coin-stat-unit {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .coin-records-row {
  background: transparent;
}

body[data-theme="dark"] .coin-records-row:nth-child(even) {
  background: rgba(255, 255, 255, 0.03);
}

body[data-theme="dark"] .coin-page-header {
  background: #1f1f1f;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .coin-page-title-wrap {
  background: transparent;
}

body[data-theme="dark"] .coin-page-header .invite-btn-secondary {
  background: #2a2a2a;
  border-color: var(--color-primary, #ff2442);
  color: var(--color-primary, #ff2442);
}

body[data-theme="dark"] .coin-page-header .invite-btn-secondary:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .coin-section-header .invite-btn-secondary {
  background: #2a2a2a;
  border-color: var(--color-primary, #ff2442);
  color: var(--color-primary, #ff2442);
}

body[data-theme="dark"] .coin-section-header .invite-btn-secondary:hover {
  background: rgba(255, 36, 66, 0.15);
}

/* ============ 移动端 ============
   - 页面 padding 缩小
   - header 改为纵向：标题在上，返回按钮在下，避免遮挡
   - 表单 1fr 1.4fr 双列 → 改为单列堆叠
   - 3 列 stat-grid → 单列堆叠
   - tip / 规则链接等小元素自适应
*/
@media (max-width: 768px) {
  .coin-page {
    padding: 16px 12px;
    max-width: 100%;
  }

  /* 头部纵向排列：标题块占满一行，返回按钮下一行靠左 */
  .coin-page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    margin-bottom: 16px;
  }

  .coin-page-header .invite-btn-secondary {
    align-self: flex-start;
  }

  .coin-page-title {
    font-size: 19px;
  }

  .coin-page-desc {
    font-size: 12px;
  }

  /* 实名认证表单：双列 → 单列堆叠 */
  .coin-form-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .coin-form-input {
    height: 40px;
    font-size: 14px;
  }

  /* 账户概览：3 列 → 单列堆叠 */
  .coin-stat-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .coin-stat-card {
    padding: 14px;
  }

  .coin-stat-value {
    font-size: 22px;
  }

  /* 提现申请块 */
  .coin-section {
    padding: 14px;
  }

  .coin-section-title {
    font-size: 15px;
  }

  /* 提现记录列表 */
  .coin-table {
    font-size: 12px;
  }

  .coin-table th,
  .coin-table td {
    padding: 8px 6px;
  }

  /* 描述/提示文本 */
  .coin-auth-tip {
    font-size: 12px;
    padding: 10px 12px;
  }

  /* 申请提现 modal 在手机端宽度铺满 */
  .withdraw-modal .ant-modal {
    max-width: calc(100vw - 24px);
    margin: 12px;
  }
}

.coin-apply-agreement {
  margin-bottom: 18px;
}

.coin-apply-agreement-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.coin-apply-agreement-checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: #ff2442;
}

.coin-apply-agreement-link {
  color: #ff2442;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.coin-apply-agreement-link:hover {
  color: #e61e3a;
}

body[data-theme="dark"] .coin-apply-agreement-label {
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .coin-apply-agreement-link {
  color: #ff2442;
}

body[data-theme="dark"] .coin-apply-agreement-link:hover {
  color: #e61e3a;
}
</style>

<style>
/* 创作币/提现页面下的 a-modal（提现规则、提现服务协议）深色主题 */
body[data-theme="dark"] .coin-rules-modal .ant-modal-content {
  background: #141414;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}

body[data-theme="dark"] .coin-rules-modal .ant-modal-header {
  background: #141414;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .coin-rules-modal .ant-modal-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .coin-rules-modal .ant-modal-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .coin-rules-modal .ant-modal-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

/* 弹框内正文（scoped 样式无法作用到 portal 内的元素，这里强制覆盖） */
body[data-theme="dark"] .coin-rules-modal .coin-rules-list {
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .coin-rules-modal .coin-rules-footer {
  border-top-color: #303030;
  color: rgba(255, 255, 255, 0.45);
}

body[data-theme="dark"] .coin-rules-modal .coin-rules-highlight {
  color: #ff4d6f;
}

/* 深色主题下弹框遮罩更深，避免弹框视觉不突出 */
body[data-theme="dark"] .ant-modal-mask {
  background: rgba(0, 0, 0, 0.75) !important;
}
</style>
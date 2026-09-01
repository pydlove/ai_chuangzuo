<template>
  <a-modal
    v-model:open="visible"
    title="平台账号检测"
    width="560px"
    :footer="null"
    class="account-check-modal"
    @cancel="visible = false"
  >
    <div class="account-section">
      <a-alert
        v-if="nicknameCheckLimitReached"
        message="今日账号检测/昵称推荐次数已达上限"
        description="每个账号每天可检测/推荐次数有限，请明天再试。"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <div class="account-question">你已经有 {{ hasPlan ? plan.platform : '自媒体' }} 账号了吗？</div>
      <a-radio-group v-model:value="accountInfo.hasAccount" class="account-radio">
        <a-radio :value="true">已有账号或者想好了账号</a-radio>
        <a-radio :value="false">还没有</a-radio>
      </a-radio-group>

      <div v-if="accountInfo.hasAccount" class="account-form">
        <div class="account-hint">
          如果您已经有账号了，可以填写昵称检测下和您的自媒体定位是否相符，如果不符合，也会给您一些推荐。
        </div>
        <div class="form-row">
          <span class="form-label">账号名称</span>
          <a-input v-model:value="accountInfo.name" placeholder="输入你的账号昵称" />
          <a-button
            type="primary"
            class="validate-btn"
            :loading="checking"
            :disabled="nicknameCheckLimitReached || !accountInfo.name.trim()"
            @click="validateAccountName"
          >
            检测名称
          </a-button>
        </div>
        <div
          v-if="accountValidation"
          class="validation-result"
          :class="{ fit: accountFit === true, unfit: accountFit === false }"
        >
          <template v-if="accountFit === true">
            <CheckCircleOutlined class="result-icon" /> {{ accountValidation }}
          </template>
          <template v-else-if="accountFit === false">
            <InfoCircleOutlined class="result-icon" /> {{ accountValidation }}
          </template>
          <template v-else>
            {{ accountValidation }}
          </template>
        </div>
        <div v-if="accountReason" class="validation-reason">{{ accountReason }}</div>
        <div v-if="accountSuggestions.length" class="suggestion-list">
          <div class="suggestion-label">小爱建议昵称</div>
          <div class="suggestion-cards">
            <div
              v-for="(s, idx) in accountSuggestions"
              :key="idx"
              class="suggestion-card"
            >
              <div class="suggestion-card-row">
                <div class="suggestion-card-nickname">{{ s.nickname }}</div>
                <span class="suggestion-card-copy" @click.stop="copyText(s.nickname)">复制昵称</span>
              </div>
              <div class="suggestion-card-row">
                <div class="suggestion-card-bio">{{ s.bio }}</div>
                <span class="suggestion-card-copy" @click.stop="copyText(s.bio)">复制描述</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="register-guide">
        <div class="guide-title">注册 {{ hasPlan ? plan.platform : '自媒体' }} 账号建议</div>
        <div class="guide-list">
          <div class="guide-item">下载 {{ hasPlan ? plan.platform : '对应平台' }} App 或访问官网注册</div>
          <div class="guide-item">昵称包含赛道关键词，如「35+职场转型」</div>
          <div class="guide-item">简介说明价值，如「分享真实职场转型经验」</div>
          <div class="guide-item">头像使用真人或统一风格，提高信任感</div>
        </div>
        <div class="guide-doc-link">
          注册前可以查阅：
          <a href="https://fxbi16ko1px.feishu.cn/docx/BXVqdp4XwodssXxlfECcUfODnib?from=from_copylink" target="_blank" rel="noopener noreferrer">爱创作工坊新手教程</a>
        </div>
        <div class="recommend-row">
          <a-button
            type="primary"
            class="recommend-btn"
            :loading="recommending"
            :disabled="nicknameCheckLimitReached"
            @click="recommendAccountName"
          >
            推荐昵称
          </a-button>
        </div>
        <div v-if="recommendOptions.length" class="suggestion-list">
          <div class="suggestion-label">小爱推荐昵称</div>
          <div class="suggestion-cards">
            <div
              v-for="(opt, idx) in recommendOptions"
              :key="idx"
              class="suggestion-card"
            >
              <div class="suggestion-card-row">
                <div class="suggestion-card-nickname">{{ opt.nickname }}</div>
                <span class="suggestion-card-copy" @click.stop="copyText(opt.nickname)">复制昵称</span>
              </div>
              <div class="suggestion-card-row">
                <div class="suggestion-card-bio">{{ opt.bio }}</div>
                <span class="suggestion-card-copy" @click.stop="copyText(opt.bio)">复制描述</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { fetchCurrentPlan } from '@/api/selfMediaPlan.js'
import { checkNickname, recommendNickname } from '@/api/accountCheck.js'
import { useCopy } from '@/composables/useCopy.js'
import { STORAGE_KEYS, getAccountCheckLastKey, getAccountRecommendLastKey } from '@/constants/storage.js'

const visible = defineModel('visible', { type: Boolean, default: false })

const router = useRouter()

const plan = reactive({
  platform: '小红书',
  niche: '35+ 职场转型',
  persona: '实战记录者',
  pillars: [
    { name: '干货复盘', percent: 60 },
    { name: '个人故事', percent: 20 },
    { name: '热点解读', percent: 20 }
  ]
})
const hasPlan = ref(false)
const planLoaded = ref(false)

async function loadPlan() {
  try {
    const result = await fetchCurrentPlan()
    const data = result?.data || result
    if (data && typeof data === 'object' && data.platformKey) {
      hasPlan.value = true
      Object.assign(plan, {
        platform: data.platformName || data.platformKey || plan.platform,
        niche: data.nicheName || plan.niche,
        persona: data.personaName || plan.persona,
        pillars: Array.isArray(data.pillars) ? data.pillars : plan.pillars
      })
    } else {
      hasPlan.value = false
    }
  } catch (e) {
    hasPlan.value = false
  } finally {
    planLoaded.value = true
  }
}

const accountInfo = reactive({
  hasAccount: true,
  name: ''
})

const accountValidation = ref('')
const accountFit = ref(null)
const accountReason = ref('')
const checking = ref(false)
const nicknameCheckLimitReached = ref(false)

watch(() => accountInfo.name, () => {
  if (isRestoring.value) return
  accountValidation.value = ''
  accountFit.value = null
  accountReason.value = ''
}, { flush: 'sync' })

watch(visible, (val) => {
  if (val) {
    nicknameCheckLimitReached.value = false
    if (!planLoaded.value) {
      loadPlan().then(restoreAccountModalState)
    } else {
      restoreAccountModalState()
    }
  }
})

const accountSuggestions = ref([])
const recommendOptions = ref([])
const recommending = ref(false)
const isRestoring = ref(false)

const currentUserId = localStorage.getItem(STORAGE_KEYS.USER_ID) || ''
const ACCOUNT_CHECK_LAST_KEY = getAccountCheckLastKey(currentUserId)
const ACCOUNT_RECOMMEND_LAST_KEY = getAccountRecommendLastKey(currentUserId)

// 清理旧版未区分用户的缓存，避免切换账号后看到他人数据
if (currentUserId) {
  localStorage.removeItem(STORAGE_KEYS.ACCOUNT_CHECK_LAST)
  localStorage.removeItem(STORAGE_KEYS.ACCOUNT_RECOMMEND_LAST)
}

function buildPositioning() {
  const { platform, niche, persona, goal, pillars } = plan
  const parts = []
  if (platform) parts.push(`平台：${platform}`)
  if (niche) parts.push(`赛道：${niche}`)
  if (persona) parts.push(`人设：${persona}`)
  if (goal) parts.push(`核心目标：${goal}`)
  if (pillars?.length) {
    const pillarText = pillars.map((p) => `${p.name} ${p.percent}%`).join('，')
    parts.push(`内容支柱：${pillarText}`)
  }
  return parts.join('；')
}

function requirePlan() {
  if (!hasPlan.value) {
    message.info('请先制定自媒体运营方案，再检测账号名称')
    visible.value = false
    router.push('/console/onboarding')
    return false
  }
  return true
}

function validateAccountName() {
  if (!requirePlan()) return
  const name = accountInfo.name.trim()
  if (!name) {
    accountValidation.value = '请输入账号名称'
    return
  }
  doCheckNickname(name)
}

async function doCheckNickname(name) {
  checking.value = true
  accountValidation.value = ''
  accountFit.value = null
  accountReason.value = ''
  accountSuggestions.value = []
  try {
    const positioning = buildPositioning()
    if (!positioning) {
      message.warning('请先制定自媒体方案后再进行检测')
      return
    }
    const result = await checkNickname({
      nickname: name,
      platform: plan.platform || '',
      positioning
    })
    accountFit.value = result.fit === true
    accountReason.value = result.reason || ''
    accountSuggestions.value = Array.isArray(result.suggestions) ? result.suggestions : []
    if (accountFit.value) {
      accountValidation.value = '名称与定位契合'
    } else if (accountSuggestions.value.length) {
      accountValidation.value = '名称不够契合，可参考以下建议'
    } else {
      accountValidation.value = '检测完成'
    }
    saveLastCheckResult()
  } catch (err) {
    if (err?.code === 113008) {
      nicknameCheckLimitReached.value = true
    }
    accountValidation.value = err?.message || '检测失败，请重试'
    accountFit.value = false
  } finally {
    checking.value = false
  }
}

const { copy: copyText } = useCopy({
  successText: '已复制',
  errorText: '复制失败'
})

function saveLastCheckResult() {
  localStorage.setItem(ACCOUNT_CHECK_LAST_KEY, JSON.stringify({
    name: accountInfo.name,
    fit: accountFit.value,
    reason: accountReason.value,
    suggestions: accountSuggestions.value,
    validation: accountValidation.value
  }))
}

function saveLastRecommendResult() {
  localStorage.setItem(ACCOUNT_RECOMMEND_LAST_KEY, JSON.stringify(recommendOptions.value))
}

function restoreAccountModalState() {
  isRestoring.value = true
  try {
    const checkRaw = localStorage.getItem(ACCOUNT_CHECK_LAST_KEY)
    if (checkRaw) {
      try {
        const data = JSON.parse(checkRaw)
        accountInfo.name = data.name || ''
        accountFit.value = data.fit ?? null
        accountReason.value = data.reason || ''
        accountSuggestions.value = Array.isArray(data.suggestions) ? data.suggestions : []
        accountValidation.value = data.validation || ''
      } catch {
        localStorage.removeItem(ACCOUNT_CHECK_LAST_KEY)
      }
    }
    const recommendRaw = localStorage.getItem(ACCOUNT_RECOMMEND_LAST_KEY)
    if (recommendRaw) {
      try {
        recommendOptions.value = JSON.parse(recommendRaw) || []
      } catch {
        localStorage.removeItem(ACCOUNT_RECOMMEND_LAST_KEY)
      }
    }
  } finally {
    isRestoring.value = false
  }
}

async function recommendAccountName() {
  if (!requirePlan()) return
  recommending.value = true
  recommendOptions.value = []
  try {
    const result = await recommendNickname()
    const opts = Array.isArray(result?.options) ? result.options : []
    if (!opts.length && result?.nickname) {
      opts.push({ nickname: result.nickname, bio: result.bio || '' })
    }
    recommendOptions.value = opts
    saveLastRecommendResult()
  } catch (err) {
    if (err?.code === 113008) {
      nicknameCheckLimitReached.value = true
    }
    message.error(err?.message || '推荐失败，请重试')
  } finally {
    recommending.value = false
  }
}
</script>

<style scoped>
.account-check-modal :deep(.ant-modal-body) {
  max-height: calc(80vh - 110px);
  overflow-y: auto;
}

.account-section {
  padding: 4px;
}
.account-question {
  font-size: var(--font-small);
  color: var(--color-text-primary);
  margin-bottom: 8px;
  font-weight: 500;
}
.account-radio {
  margin-bottom: 12px;
}
.account-hint {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
}
.account-form {
  padding: 12px;
  background: var(--color-bg-page);
  border-radius: var(--radius-lg);
}
.form-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.form-row :deep(.ant-input-affix-wrapper) {
  flex: 1;
  min-width: 0;
}
.form-label {
  flex-shrink: 0;
  font-size: var(--font-body);
  color: var(--color-text-secondary);
}
.validate-btn {
  border-radius: var(--radius-md);
}
.validation-result {
  margin-top: 8px;
  font-size: var(--font-small);
  color: var(--color-error);
}
.validation-result.fit {
  color: var(--color-success);
}
.validation-result.unfit {
  color: var(--color-warning);
}
.validation-reason {
  margin-top: 6px;
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.result-icon {
  margin-right: 4px;
}
.suggestion-list {
  margin-top: 12px;
}
.suggestion-label {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-xs);
}
.suggestion-cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.suggestion-card {
  padding: var(--space-sm);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.2s;
}
.suggestion-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm2);
}
.suggestion-card-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
}
.suggestion-card-row + .suggestion-card-row {
  margin-top: 6px;
}
.suggestion-card-nickname {
  flex: 1;
  min-width: 0;
  font-size: var(--font-body);
  font-weight: 600;
  color: var(--color-primary);
}
.suggestion-card-bio {
  flex: 1;
  min-width: 0;
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.suggestion-card-copy {
  flex-shrink: 0;
  font-size: var(--font-caption);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}
.suggestion-card-copy:hover {
  color: var(--color-primary);
}
.register-guide {
  background: var(--color-bg-page);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
}
.guide-title {
  font-weight: 600;
  font-size: var(--font-body);
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
}
.guide-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.guide-item {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  position: relative;
  padding-left: 16px;
}
.guide-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 7px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}
.guide-doc-link {
  margin-top: 12px;
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}
.guide-doc-link a {
  color: var(--color-primary);
}
.recommend-row {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
.recommend-btn {
  border-radius: var(--radius-md);
}

/* 主题色覆盖，避免使用 Ant Design 默认蓝色 */
:global(.account-check-modal .ant-btn-primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
:global(.account-check-modal .ant-btn-primary:hover),
:global(.account-check-modal .ant-btn-primary:focus) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #fff;
}
:global(.account-check-modal .ant-btn-primary:disabled) {
  color: rgba(0, 0, 0, 0.25);
  background: #f5f5f5;
  border-color: #d9d9d9;
}
:global(.account-check-modal .ant-radio-checked .ant-radio-inner) {
  border-color: var(--color-primary);
  background: var(--color-primary);
}
:global(.account-check-modal .ant-radio-wrapper:hover .ant-radio),
:global(.account-check-modal .ant-radio:hover .ant-radio-inner) {
  border-color: var(--color-primary);
}
:global(.account-check-modal .ant-input:hover),
:global(.account-check-modal .ant-input:focus),
:global(.account-check-modal .ant-input-focused) {
  border-color: var(--color-primary);
}
:global(.account-check-modal .ant-input:focus),
:global(.account-check-modal .ant-input-focused) {
  box-shadow: 0 0 0 2px var(--color-primary-bg);
}

@media (max-width: 576px) {
  .account-check-modal :deep(.ant-modal) {
    max-width: calc(100vw - 32px);
    margin: 16px auto;
  }
  .account-check-modal :deep(.ant-modal-body) {
    max-height: calc(100vh - 140px);
  }
  .form-row {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .form-row :deep(.ant-input-affix-wrapper) {
    width: 100%;
  }
  .validate-btn,
  .recommend-btn {
    width: 100%;
  }
  .suggestion-card-row {
    flex-wrap: wrap;
  }
  .suggestion-card-copy {
    margin-left: auto;
  }
}
</style>

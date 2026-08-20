<template>
  <div class="workbench-index">
    <!-- 第一行：左侧（欢迎卡片 + 立即创作）+ 右侧（运营方案） -->
    <div class="top-row">
      <div class="left-column">
        <a-card class="wb-card welcome-card" :bordered="false">
          <div class="welcome-body">
            <!-- 左侧：对话区域 -->
            <div class="welcome-dialogue">
              <a-avatar
                :size="48"
                src="https://foruda.gitee.com/images/1787188720633617816/b28bf15b_8060302.png"
                alt="AI 顾问"
                class="ai-avatar"
              />
              <div class="dialogue-bubble">
                <div class="dialogue-title">
                  尊敬的{{ userInfo.nickname ? userInfo.nickname + '老师' : '老师' }}您好，我是您的专属自媒体顾问小爱
                </div>
                <div class="dialogue-greeting">
                  <span v-if="todayDone" class="done-text">今日创作目标已达成，继续保持！</span>
                  <span v-else class="todo-text">今日任务还没完成，点击「开始今日创作」去写一篇吧</span>
                </div>
              </div>
            </div>

            <!-- 中间：个人信息区域 -->
            <div class="welcome-info">
              <div class="info-header">
                <a-avatar :size="40" class="user-avatar-mini">
                  {{ userInfo.nickname ? userInfo.nickname[0] : 'U' }}
                </a-avatar>
                <span class="info-name">{{ userInfo.nickname || '未设置昵称' }}</span>
                <a-tag v-if="userInfo.vipLevel" class="vip-tag" color="#ff2442">
                  <CrownOutlined /> {{ userInfo.vipLevel }}
                </a-tag>
              </div>
              <div class="welcome-meta">
                <span class="meta-item"><MailOutlined /> {{ userInfo.email }}</span>
                <a-divider type="vertical" class="meta-divider" />
                <span class="meta-item">邀请码：{{ userInfo.inviteCode }}</span>
                <a-button type="link" size="small" class="copy-code-btn" @click="copyInviteCode">
                  复制
                </a-button>
              </div>
            </div>

            <!-- 右侧：账户区域 -->
            <div class="welcome-balance">
              <div class="balance-header">
                <span class="balance-label">账户余额</span>
                <a-button type="primary" size="small" class="withdraw-btn" @click="withdrawModalVisible = true">
                  可提现
                </a-button>
              </div>
              <div class="balance-value">
                {{ balance.coin }}
                <span class="balance-unit">创作币</span>
              </div>
            </div>
          </div>
        </a-card>

        <div class="create-section">
          <a-button type="primary" size="large" class="create-main-btn" @click="openCreateChoice">
            <EditOutlined />
            开始今日创作
          </a-button>
          <a-button size="large" class="weekly-data-btn" @click="weeklyDataVisible = true">
            <BarChartOutlined />
            本周数据
          </a-button>
        </div>
      </div>

      <a-card class="wb-card plan-card" :bordered="false" title="运营方案">
        <div v-if="hasPlan" class="plan-content">
          <div class="plan-grid">
            <div class="plan-row">
              <span class="plan-label">主攻平台</span>
              <span class="plan-value plan-platform">{{ plan.platform }}</span>
            </div>
            <div class="plan-row">
              <span class="plan-label">细分赛道</span>
              <span class="plan-value">{{ plan.niche }}</span>
            </div>
            <div class="plan-row">
              <span class="plan-label">人设定位</span>
              <span class="plan-value">{{ plan.persona }}</span>
            </div>
          </div>
          <div class="plan-pillars-inline">
            <span class="plan-label">内容支柱</span>
            <div class="plan-pillar-tags">
              <a-tag v-for="p in plan.pillars" :key="p.name" size="small">{{ p.name }} {{ p.percent }}%</a-tag>
            </div>
          </div>
          <div class="plan-actions">
            <a-button size="small" class="plan-btn" @click="router.push('/console/onboarding')">
              调整方案
            </a-button>
          </div>
        </div>
        <div v-else class="plan-empty">
          <div class="plan-empty-title">您还没有还没有专属运营方案，</div>
          <div class="plan-empty-desc">
            您的专属顾问小爱会为您量身定制一套专属的自媒体运营方案，陪您一起经营您的自媒体账号，快去行动吧。
          </div>
          <a-button type="primary" size="small" class="plan-empty-btn" @click="router.push('/console/onboarding')">
            立即制定
          </a-button>
        </div>
      </a-card>
    </div>

    <!-- 第二行：左侧（快捷操作 + 生成记录）+ 右侧占位卡片 -->
    <div class="bottom-row">
      <div class="left-column">
        <a-card class="wb-card shortcut-card" :bordered="false" title="快捷操作">
          <div class="shortcut-grid">
            <div
              v-for="item in shortcuts"
              :key="item.label"
              class="shortcut-item"
              @click="item.action ? item.action() : router.push(item.path)"
            >
              <div class="shortcut-icon-wrap">
                <component :is="item.icon" class="shortcut-icon" />
              </div>
              <span class="shortcut-label">{{ item.label }}</span>
            </div>
          </div>
        </a-card>

        <a-card class="wb-card generation-card" :bordered="false">
          <template #title>
            <div class="card-title-row">
              <span class="card-title">生成记录</span>
              <span class="record-count">最近 7 天</span>
            </div>
          </template>
          <template #extra>
            <div class="generation-extra">
              <a-button type="link" size="small" class="how-publish-btn" @click="openHowToPublish">
                如何发布
              </a-button>
              <a-button type="link" size="small" class="view-more-btn" @click="router.push('/console/works')">
                查看更多 <RightOutlined />
              </a-button>
            </div>
          </template>
          <div v-if="!recentRecords.length" class="generation-empty">
            <RocketOutlined class="empty-icon" />
            还没有生成记录，点击「开始今日创作」开始你的第一篇内容
          </div>
          <div v-else class="generation-list">
            <div
              v-for="record in recentRecords"
              :key="record.id"
              class="generation-item"
              @click="record.status === 'completed' ? openPublishGuide(record) : router.push('/console/works')"
            >
              <div class="generation-main">
                <div class="generation-title">{{ record.title }}</div>
                <div class="generation-meta">
                  <span>{{ record.createdAt }}</span>
                  <span class="dot-separator">·</span>
                  <span class="generation-status" :class="record.status">{{ statusText(record.status) }}</span>
                </div>
                <a-progress
                  v-if="record.status === 'generating'"
                  :percent="record.progress"
                  size="small"
                  status="active"
                  class="generation-progress"
                />
              </div>
              <RightOutlined class="generation-arrow" />
            </div>
          </div>
        </a-card>
      </div>

      <div class="activity-card-wrapper">
        <a-card class="wb-card activity-card" :bordered="false" title="热门活动">
          <div class="activity-list">
            <div
              v-for="item in activities"
              :key="item.label"
              class="activity-item"
              @click="router.push(item.path)"
            >
              <div class="activity-icon" :class="item.iconClass">
                <img :src="item.img" class="activity-icon-img" alt="" />
              </div>
              <div class="activity-info">
                <div class="activity-name">{{ item.label }}</div>
                <div class="activity-desc">{{ item.desc }}</div>
              </div>
              <RightOutlined class="activity-arrow" />
            </div>
          </div>
        </a-card>
      </div>
    </div>

    <!-- 发布建议弹窗 -->
    <a-modal
      :open="publishModalVisible"
      title="发布建议"
      width="700px"
      :footer="null"
      class="publish-modal"
      @cancel="publishModalVisible = false"
    >
      <div v-if="currentPublishRecord" class="publish-guide">
        <div class="publish-guide-section">
          <div class="publish-guide-label">建议发布时间</div>
          <div class="publish-guide-value">{{ publishTimeText }}</div>
          <div class="publish-guide-desc">基于你的主攻平台「{{ currentPublishRecord.platform }}」的流量高峰和账号冷启动效率推荐</div>
        </div>
        <div class="publish-guide-section">
          <div class="publish-guide-label">一文多发方案</div>
          <div class="publish-guide-platforms">
            <div v-for="p in publishPlatforms" :key="p.platform" class="publish-guide-platform-item">
              <div class="publish-guide-platform-name">{{ p.platform }}</div>
              <div class="publish-guide-platform-method">{{ p.method }}</div>
            </div>
          </div>
        </div>
        <div class="publish-guide-section">
          <div class="publish-guide-label">发送方式</div>
          <div class="publish-guide-value">{{ sendMethod.method }}</div>
          <a :href="sendMethod.docLink" target="_blank" class="publish-guide-doc-link">{{ sendMethod.docText }}</a>
        </div>
      </div>
    </a-modal>

    <!-- 账号检测弹窗 -->
    <a-modal
      v-model:open="accountModalVisible"
      title="平台账号检测"
      width="560px"
      :footer="null"
      class="account-modal"
      @cancel="accountModalVisible = false"
    >
      <div class="account-section">
        <div class="account-question">你已经有 {{ plan.platform }} 账号了吗？</div>
        <a-radio-group v-model:value="accountInfo.hasAccount" class="account-radio">
          <a-radio :value="true">已有账号</a-radio>
          <a-radio :value="false">还没有</a-radio>
        </a-radio-group>

        <div v-if="accountInfo.hasAccount" class="account-form">
          <div class="form-row">
            <span class="form-label">账号名称</span>
            <a-input v-model:value="accountInfo.name" placeholder="输入你的账号昵称" />
            <a-button
              type="primary"
              size="small"
              class="validate-btn"
              :loading="checking"
              :disabled="!accountInfo.name.trim()"
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
            <div class="suggestion-label">{{ accountFit === false ? 'AI 建议昵称：' : '推荐名称：' }}</div>
            <div class="suggestion-chips">
              <span
                v-for="s in accountSuggestions"
                :key="s"
                class="suggestion-item"
                @click="selectSuggestion(s)"
              >{{ s }}</span>
            </div>
          </div>
        </div>

        <div v-else class="register-guide">
          <div class="guide-title">注册 {{ plan.platform }} 账号建议</div>
          <div class="guide-list">
            <div class="guide-item">下载 {{ plan.platform }} App 或访问官网注册</div>
            <div class="guide-item">昵称包含赛道关键词，如「35+职场转型」</div>
            <div class="guide-item">简介说明价值，如「分享真实职场转型经验」</div>
            <div class="guide-item">头像使用真人或统一风格，提高信任感</div>
          </div>
          <div class="form-row register-check-row">
            <span class="form-label">想好的昵称</span>
            <a-input v-model:value="accountInfo.name" placeholder="输入你想好的昵称，AI 会先帮你检测" />
            <a-button
              type="primary"
              size="small"
              class="validate-btn"
              :loading="checking"
              :disabled="!accountInfo.name.trim()"
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
            <div class="suggestion-label">{{ accountFit === false ? 'AI 建议昵称：' : '推荐昵称：' }}</div>
            <div class="suggestion-chips">
              <span
                v-for="s in accountSuggestions"
                :key="s"
                class="suggestion-item"
                @click="selectSuggestion(s)"
              >{{ s }}</span>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 本周数据弹窗 -->
    <a-modal
      v-model:open="weeklyDataVisible"
      title="录入本周数据"
      width="600px"
      :footer="null"
      class="weekly-data-modal"
      @cancel="weeklyDataVisible = false"
    >
      <div class="weekly-data-summary">
        本周共发布 <strong>{{ weeklyArticles.length }}</strong> 篇，总阅读量 <strong>{{ totalWeeklyReads }}</strong>
      </div>
      <div class="weekly-data-list">
        <div
          v-for="(item, index) in weeklyArticles"
          :key="index"
          class="weekly-data-item"
        >
          <a-input v-model:value="item.title" placeholder="文章标题" class="weekly-data-title" />
          <a-input-number v-model:value="item.reads" placeholder="阅读量" :min="0" class="weekly-data-reads" />
        </div>
      </div>
      <div class="weekly-data-actions">
        <a-button type="primary" @click="saveWeeklyData">保存</a-button>
      </div>
    </a-modal>

    <!-- 提现进度弹窗 -->
    <a-modal
      v-model:open="withdrawModalVisible"
      title="提现进度"
      width="520px"
      :footer="null"
      class="withdraw-modal"
      @cancel="withdrawModalVisible = false"
    >
      <div class="withdraw-progress-section">
        <div class="withdraw-balance">
          <div class="withdraw-label">当前余额</div>
          <div class="withdraw-amount">
            {{ balance.coin }}
            <span>创作币</span>
          </div>
          <div class="withdraw-target">满 {{ balance.withdrawThreshold }} 创作币可提现</div>
        </div>
        <div class="withdraw-progress-row">
          <a-progress :percent="balancePercent" size="small" :show-info="false" class="withdraw-progress-bar" />
          <span class="withdraw-percent">{{ balancePercent }}%</span>
        </div>
        <div class="withdraw-status">
          还差 <strong>{{ coinsToWithdraw }}</strong> 创作币，完成下方任务即可提现
        </div>
      </div>
      <div class="withdraw-plan">
        <div class="withdraw-plan-title">快速达标方案</div>
        <div
          v-for="task in withdrawTasks"
          :key="task.label"
          class="withdraw-plan-item"
        >
          <div class="withdraw-plan-icon">{{ task.icon }}</div>
          <div class="withdraw-plan-info">
            <div class="withdraw-plan-label">{{ task.label }}</div>
            <div class="withdraw-plan-reward">+{{ task.reward }} 创作币</div>
          </div>
          <a-button type="primary" size="small" class="withdraw-plan-btn" @click="goWithdrawTask(task.path)">
            去完成
          </a-button>
        </div>
      </div>

      <div class="withdraw-marquee">
        <div class="withdraw-marquee-title">🎉 实时提现成功</div>
        <div class="withdraw-marquee-wrap">
          <div class="withdraw-marquee-list">
            <div
              v-for="item in withdrawRecords"
              :key="item.id"
              class="withdraw-marquee-item"
            >
              <span class="marquee-name">{{ item.name }}</span>
              <span>刚刚提现</span>
              <span class="marquee-amount">{{ item.amount }} 元</span>
              <span class="marquee-status">成功</span>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <CreateFlowModal v-model:visible="createFlowVisible" :plan="plan" @start="onCreateStart" />
    <FreeCreateModal v-model:visible="freeCreateVisible" :plan="plan" @success="onFreeCreateSuccess" />

    <!-- 创作方式选择弹窗 -->
    <a-modal
      v-model:open="createChoiceVisible"
      title="开始今日创作"
      width="640px"
      :footer="null"
      centered
      class="create-choice-modal"
      @cancel="createChoiceVisible = false"
    >
      <div class="create-choice-body">
        <div class="create-choice-options">
          <div class="create-choice-card recommended" @click="chooseRecommended">
            <div class="choice-icon-wrap">
              <CompassOutlined class="choice-icon" />
            </div>
            <div class="choice-title">按爱创作推荐方式创作</div>
            <div class="choice-desc">基于你的运营方案，AI 推荐选题、观点、字数、提示词和模板，适合想要灵感的人。</div>
            <div class="choice-tags">
              <span class="choice-tag">AI 推荐选题</span>
              <span class="choice-tag">低粉高赞案例</span>
            </div>
          </div>

          <div class="create-choice-card free" @click="chooseFreeCreate">
            <div class="choice-icon-wrap">
              <EditOutlined class="choice-icon" />
            </div>
            <div class="choice-title">自由创作</div>
            <div class="choice-desc">自己设置标题和核心观点，自主选择平台与字数，适合已有明确想法的人。</div>
            <div class="choice-tags">
              <span class="choice-tag">自定义标题</span>
              <span class="choice-tag">自主观点</span>
            </div>
          </div>
        </div>
      </div>
    </a-modal>
    <!-- 制定自媒体方案弹框 -->
    <a-modal
      v-model:open="planModalVisible"
      title="制定你的自媒体方案"
      width="560px"
      :footer="null"
      centered
      class="plan-modal"
      @cancel="dismissPlanModal"
    >
      <div class="plan-modal-body">
        <div class="plan-modal-title">让 AI 为你定制专属运营方案</div>
        <div class="plan-modal-desc">
          为了给你更精准的运营建议，请先回答几个简单问题，AI 会基于你的目标、时间与资源，为你定制一套更容易起号的自媒体运营方案。
        </div>
        <div class="plan-modal-actions">
          <a-button type="primary" size="large" block @click="goToPlan">去制定方案</a-button>
          <a-button size="large" block class="plan-modal-later" @click="dismissPlanModal">稍后再说</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  EditOutlined,
  CrownOutlined,
  MailOutlined,
  UserOutlined,
  RocketOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  FileTextOutlined,
  ShopOutlined,
  FireOutlined,
  RightOutlined,
  SafetyCertificateOutlined,
  BarChartOutlined,
  BulbOutlined,
  TrophyOutlined,
  BookOutlined,
  CodeOutlined,
  CreditCardOutlined,
  SafetyOutlined,
  TagOutlined,
  CompassOutlined
} from '@ant-design/icons-vue'
import CreateFlowModal from './create/CreateFlowModal.vue'
import FreeCreateModal from './create/FreeCreateModal.vue'
import { fetchCurrentPlan } from '@/api/selfMediaPlan.js'
import { checkNickname } from '@/api/accountCheck.js'
import { getMyProfile } from '@/api/user.js'
import { getAccountSummary } from '@/api/earnings.js'
import { getMyMembership } from '@/api/membership.js'

const router = useRouter()

const userInfo = reactive({
  nickname: '',
  email: '',
  inviteCode: '',
  vipLevel: '',
  vipExpire: ''
})

const balance = reactive({
  coin: 120,
  withdrawThreshold: 200
})

const balancePercent = computed(() => {
  return Math.min(Math.round((balance.coin / balance.withdrawThreshold) * 100), 100)
})

const coinsToWithdraw = computed(() => {
  return Math.max(balance.withdrawThreshold - balance.coin, 0)
})

const todayDone = ref(false)
const todayKey = computed(() => {
  const date = new Date()
  return `aichuangzuo_today_done_${date.getFullYear()}_${date.getMonth() + 1}_${date.getDate()}`
})

async function loadWelcomeData() {
  try {
    const [profileRes, summary, membershipRes] = await Promise.all([
      getMyProfile(),
      getAccountSummary(),
      getMyMembership()
    ])
    const profile = profileRes?.data || {}
    userInfo.nickname = profile.nickname || ''
    userInfo.email = profile.email || ''
    userInfo.inviteCode = profile.inviteCode || ''

    balance.coin = summary?.coinBalance || 0

    const membership = membershipRes?.data || {}
    if (membership.hasMembership) {
      userInfo.vipLevel = membership.levelName || ''
      userInfo.vipExpire = membership.expiresAt || ''
    } else {
      userInfo.vipLevel = ''
      userInfo.vipExpire = ''
    }
  } catch (err) {
    console.error('加载欢迎卡片数据失败', err)
  }
}

function setTodayDone() {
  todayDone.value = true
  localStorage.setItem(todayKey.value, '1')
}

function copyInviteCode() {
  navigator.clipboard.writeText(userInfo.inviteCode).then(() => {
    message.success('邀请码已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

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
const planModalVisible = ref(false)
const SELF_MEDIA_PLAN_MODAL_KEY = 'aichuangzuo_selfmedia_plan_modal_dismissed'

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
    console.warn('加载运营方案失败', e)
    hasPlan.value = false
  }
}

function goToPlan() {
  planModalVisible.value = false
  router.push('/console/onboarding')
}

function dismissPlanModal() {
  planModalVisible.value = false
  localStorage.setItem(SELF_MEDIA_PLAN_MODAL_KEY, '1')
}

onMounted(() => {
  todayDone.value = localStorage.getItem(todayKey.value) === '1'
  loadWelcomeData()
  loadPlan().then(() => {
    if (!hasPlan.value && !localStorage.getItem(SELF_MEDIA_PLAN_MODAL_KEY)) {
      planModalVisible.value = true
    }
  })
})

const createFlowVisible = ref(false)
const createChoiceVisible = ref(false)
const freeCreateVisible = ref(false)
const accountModalVisible = ref(false)
const weeklyDataVisible = ref(false)
const withdrawModalVisible = ref(false)

const withdrawTasks = [
  { label: '参加 2 个约稿任务', reward: 40, path: '/console/commission', icon: '📝' },
  { label: '发布 1 个提示词', reward: 20, path: '/console/skill-market', icon: '💡' },
  { label: '邀请 1 个好友', reward: 20, path: '/console/invite', icon: '🎁' }
]

const withdrawRecords = [
  { id: 1, name: '创作者小张', amount: 50 },
  { id: 2, name: '创作者小李', amount: 100 },
  { id: 3, name: '创作者阿伟', amount: 80 },
  { id: 4, name: '创作者王姐', amount: 200 },
  { id: 5, name: '创作者小赵', amount: 150 },
  { id: 6, name: '创作者陈哥', amount: 60 }
]

function goWithdrawTask(path) {
  withdrawModalVisible.value = false
  router.push(path)
}

const weeklyArticles = reactive([
  { title: '35+转型：如何从焦虑走向行动', reads: 1200 },
  { title: 'Remote 工作第一年，我踩过的 5 个坑', reads: 800 }
])

const totalWeeklyReads = computed(() => {
  return weeklyArticles.reduce((sum, item) => sum + (Number(item.reads) || 0), 0)
})

function saveWeeklyData() {
  message.success('本周数据已保存')
  weeklyDataVisible.value = false
}

const accountInfo = reactive({
  hasAccount: true,
  name: ''
})

const accountValidation = ref('')
const accountFit = ref(null)
const accountReason = ref('')
const checking = ref(false)

watch(() => accountInfo.name, () => {
  accountValidation.value = ''
  accountFit.value = null
  accountReason.value = ''
}, { flush: 'sync' })

const accountSuggestions = ref([])

const shortcuts = [
  { label: '账号名检测', icon: SafetyCertificateOutlined, action: () => { accountModalVisible.value = true } },
  { path: '/console/commission', label: '约稿中心', icon: ShopOutlined },
  { path: '/console/skill-market', label: '提示词市场', icon: BulbOutlined },
  { path: '/console/leaderboard', label: '收益排行榜', icon: TrophyOutlined },
  { path: '/console/hot-search', label: '热搜榜', icon: FireOutlined },
  { path: '/console/learn', label: '创作学院', icon: BookOutlined },
  { path: '/console/works', label: '我的作品', icon: FileTextOutlined },
  { path: '/console/my-skills', label: '我的提示词', icon: CodeOutlined },
  { path: '/console/account', label: '我的账户', icon: CreditCardOutlined },
  { path: '/console/benefits', label: '我的权益', icon: SafetyOutlined },
  { path: '/console/coupons', label: '我的优惠券', icon: TagOutlined }
]

const activities = [
  {
    label: '幸运抽奖',
    desc: '每日免费抽奖，创作币、会员时长、限定模板等好礼送不停',
    path: '/console/lottery',
    img: 'https://foruda.gitee.com/images/1787037155495964487/ad31b97f_8060302.jpg',
    iconClass: 'lottery'
  },
  {
    label: '约稿任务',
    desc: '精选品牌与创作者对接，完成任务即可获得丰厚创作币奖励',
    path: '/console/commission',
    img: 'https://foruda.gitee.com/images/1787037155375766510/8e8ea74b_8060302.jpg',
    iconClass: 'commission'
  },
  {
    label: '提示词市场',
    desc: '上传原创提示词，被他人使用即可持续获得收益分成',
    path: '/console/skill-market',
    img: 'https://foruda.gitee.com/images/1787037155374404643/8e3df6d3_8060302.jpg',
    iconClass: 'skill'
  },
  {
    label: '邀请有礼',
    desc: '邀请好友加入，双方均可获得创作币与会员权益奖励',
    path: '/console/invite',
    img: 'https://foruda.gitee.com/images/1787037155601899174/e2d0ea7e_8060302.jpg',
    iconClass: 'invite'
  },
  {
    label: '收益排行榜',
    desc: '实时查看平台创作者收益榜单，学习头部创作者的变现路径',
    path: '/console/leaderboard',
    img: 'https://foruda.gitee.com/images/1787037155488319561/776ff162_8060302.jpg',
    iconClass: 'rank'
  },
  {
    label: '创作学院',
    desc: '从选题、标题到爆款结构，系统化课程帮你快速提升创作能力',
    path: '/console/learn',
    img: 'https://foruda.gitee.com/images/1787037155375260626/2a681493_8060302.jpg',
    iconClass: 'learn'
  }
]

const generationRecords = reactive([
  {
    id: 1,
    title: '35+转型：如何从焦虑走向行动',
    platform: '小红书',
    status: 'completed',
    progress: 100,
    createdAt: '08-17 09:30',
    createdAtTimestamp: Date.now() - 2 * 24 * 60 * 60 * 1000,
    selectedTopic: { title: '35+转型：如何从焦虑走向行动' }
  },
  {
    id: 2,
    title: 'Remote 工作第一年，我踩过的 5 个坑',
    platform: '小红书',
    status: 'generating',
    progress: 42,
    createdAt: '08-17 10:15',
    createdAtTimestamp: Date.now() - 24 * 60 * 60 * 1000,
  },
  {
    id: 3,
    title: '中年转型，别让年龄定义你的可能性',
    platform: '小红书',
    status: 'failed',
    progress: 0,
    createdAt: '08-17 11:02',
    createdAtTimestamp: Date.now(),
  },
  {
    id: 4,
    title: '副业起步：下班后 2 小时能做些什么',
    platform: '今日头条',
    status: 'completed',
    progress: 100,
    createdAt: '08-16 14:20',
    createdAtTimestamp: Date.now() - 3 * 24 * 60 * 60 * 1000,
    selectedTopic: { title: '副业起步：下班后 2 小时能做些什么' }
  },
  {
    id: 5,
    title: '35+ 职场人如何建立个人品牌',
    platform: '百家号',
    status: 'completed',
    progress: 100,
    createdAt: '08-15 20:10',
    createdAtTimestamp: Date.now() - 4 * 24 * 60 * 60 * 1000,
    selectedTopic: { title: '35+ 职场人如何建立个人品牌' }
  },
  {
    id: 6,
    title: '远程办公第三年，我的效率工具清单',
    platform: '知乎',
    status: 'completed',
    progress: 100,
    createdAt: '08-14 08:45',
    createdAtTimestamp: Date.now() - 5 * 24 * 60 * 60 * 1000,
    selectedTopic: { title: '远程办公第三年，我的效率工具清单' }
  },
  {
    id: 7,
    title: '中年转行，我为什么会选择内容创作',
    platform: '微信公众号',
    status: 'pending',
    progress: 0,
    createdAt: '08-13 16:30',
    createdAtTimestamp: Date.now() - 6 * 24 * 60 * 60 * 1000,
  }
])

const recentRecords = computed(() => {
  const oneWeek = 7 * 24 * 60 * 60 * 1000
  const now = Date.now()
  return generationRecords.filter(r => {
    const ts = r.createdAtTimestamp
    return ts && now - ts <= oneWeek
  })
})

const publishModalVisible = ref(false)
const currentPublishRecord = ref(null)

const publishTimeText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '今晚 20:00'
  if (hour < 17) return '今晚 21:00'
  return '明天 08:00'
})

const publishPlatforms = computed(() => {
  if (!currentPublishRecord.value) return []
  const main = currentPublishRecord.value.platform
  const all = [
    { platform: '微信公众号', method: '作为首发生态发布，正文保留完整论述，标题用主标题，引导读者关注后阅读全文。' },
    { platform: '小红书', method: '截取文章前 3 段核心观点，配图 3-6 张，标题改为短句+emoji，正文加话题标签。' },
    { platform: '今日头条', method: '同步标题+正文，封面选文中高冲突段落截图，导语强调利益点，提升点击率。' },
    { platform: '百家号', method: '拆成 3 个小标题版本发布，强调搜索关键词覆盖，增加被搜索到的概率。' },
    { platform: '知乎', method: '把文章改成问答体，开头直接回应一个相关问题，增强专业感和互动。' },
    { platform: '抖音', method: '提取 3 个金句做口播脚本，配合文中截图做视频，引导回公众号看全文。' }
  ]
  const mainItems = all.filter(p => p.platform === main)
  const otherItems = all.filter(p => p.platform !== main)
  return mainItems.concat(otherItems).slice(0, 4)
})

const sendMethod = computed(() => {
  return {
    method: '手动复制到各平台发布（自动发布功能开发中）',
    docLink: '/docs/manual-publish',
    docText: '查看《手动发布操作文档》'
  }
})

function openCreateChoice() {
  createChoiceVisible.value = true
}

function chooseRecommended() {
  createChoiceVisible.value = false
  createFlowVisible.value = true
}

function chooseFreeCreate() {
  createChoiceVisible.value = false
  freeCreateVisible.value = true
}

function onCreateStart(payload) {
  setTodayDone()
  createGenerationRecord(payload)
}

function onFreeCreateSuccess(task) {
  setTodayDone()
  createGenerationRecord({
    title: task?.title || '自由创作内容',
    platform: plan.platform,
    status: 'generating',
    progress: 0,
    selectedTopic: { title: task?.title || '自由创作内容' }
  })
}

function createGenerationRecord(payload) {
  const record = {
    id: Date.now(),
    title: payload.selectedTopic?.title || '今日创作内容',
    platform: plan.platform,
    status: 'generating',
    progress: 0,
    createdAt: new Date().toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
    createdAtTimestamp: Date.now(),
    ...payload
  }
  generationRecords.unshift(record)
  simulateGeneration(record)
}

function simulateGeneration(record) {
  let p = 0
  const timer = setInterval(() => {
    p += Math.floor(Math.random() * 12) + 8
    if (p >= 100) {
      p = 100
      clearInterval(timer)
      record.status = 'completed'
      record.progress = 100
      message.success('文章生成完成，可点击查看或发布建议')
    }
    record.progress = p
  }, 400)
}

function openPublishGuide(record) {
  currentPublishRecord.value = record
  publishModalVisible.value = true
}

function openHowToPublish() {
  const completed = recentRecords.value.find(r => r.status === 'completed')
  currentPublishRecord.value = completed || recentRecords.value[0] || null
  publishModalVisible.value = true
}

function statusText(status) {
  const map = { pending: '排队中', generating: '生成中', completed: '已完成', failed: '生成失败' }
  return map[status] || status
}

function validateAccountName() {
  const name = accountInfo.name.trim()
  if (!name) {
    accountValidation.value = '请输入账号名称'
    return
  }
  doCheckNickname(name)
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
  } catch (err) {
    accountValidation.value = err?.message || '检测失败，请重试'
    accountFit.value = false
  } finally {
    checking.value = false
  }
}

function selectSuggestion(s) {
  accountInfo.name = s
  accountValidation.value = ''
  accountFit.value = null
  accountReason.value = ''
}
</script>

<style scoped>
.workbench-index {
  padding: var(--space-lg);
  background: var(--color-bg-page);
  min-height: 100%;
}

/* 卡片公共样式 */
.wb-card {
  border-radius: var(--radius-xl);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
  transition: box-shadow 0.25s ease;
}
.wb-card:hover {
  box-shadow: var(--shadow-md);
}
.wb-card :deep(.ant-card-body) {
  padding: var(--space-lg);
}
.wb-card :deep(.ant-card-head) {
  padding: 0 var(--space-lg);
  min-height: 54px;
  border-bottom: 1px solid var(--color-border-light);
}
.wb-card :deep(.ant-card-head-title) {
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}
.wb-card :deep(.ant-card-extra) {
  padding: 0;
}
.wb-card :deep(.ant-btn-primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.wb-card :deep(.ant-btn-primary:hover) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.card-title {
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}
.card-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.record-count {
  font-size: var(--font-caption);
  color: var(--color-text-secondary);
  background: var(--color-bg-hover);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: 500;
  line-height: 1.4;
}
.view-more-btn {
  padding: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-small);
}
.view-more-btn:hover {
  color: var(--color-primary);
}

/* 顶部行 */
.top-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
  align-items: stretch;
}
.left-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* 欢迎卡 */
.welcome-card {
  height: fit-content;
}
.welcome-body {
  display: flex;
  align-items: stretch;
  gap: var(--space-lg);
}
.welcome-dialogue {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  flex: 2;
  min-width: 0;
}
.ai-avatar {
  flex-shrink: 0;
  background: transparent;
}
.ai-avatar :deep(img) {
  object-fit: cover;
}
.dialogue-bubble {
  flex: 1;
  min-width: 0;
  background: var(--color-bg-page);
  border-radius: var(--radius-xl);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.dialogue-title {
  font-size: var(--font-body);
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.5;
}
.dialogue-greeting {
  font-size: var(--font-small);
  color: var(--color-text-regular);
  line-height: 1.5;
}
.welcome-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: var(--space-sm);
  padding: 0 var(--space-md);
  border-left: 1px solid var(--color-border-light);
  border-right: 1px solid var(--color-border-light);
}
.info-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}
.user-avatar-mini {
  background: var(--color-primary);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}
.info-name {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-text-primary);
}
.vip-tag {
  font-size: var(--font-small);
  border-radius: var(--radius-full);
  padding: 2px 10px;
  margin-inline-end: 0;
}
.welcome-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-xs);
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.meta-item :deep(.anticon) {
  color: var(--color-text-placeholder);
}
.meta-divider {
  background: var(--color-border-default);
}
.copy-code-btn {
  padding: 0;
  height: auto;
  font-size: var(--font-small);
}
.done-text {
  color: var(--color-success);
  font-weight: 600;
}
.todo-text {
  color: var(--color-primary);
  font-weight: 600;
}
.welcome-balance {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 var(--space-md);
  gap: 8px;
}
.balance-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.balance-label {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}
.balance-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1;
}
.balance-unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-left: 4px;
}
.withdraw-btn {
  border-radius: var(--radius-lg);
  font-size: var(--font-small);
  padding: 0 10px;
  height: 24px;
}
.balance-progress :deep(.ant-progress-bg) {
  background: var(--color-primary) !important;
}
.balance-tip {
  font-size: var(--font-caption);
  color: var(--color-text-secondary);
}
.balance-tip strong {
  color: var(--color-primary);
  font-weight: 600;
}
.balance-target {
  font-size: var(--font-caption);
  color: var(--color-text-secondary);
}

/* 运营方案卡 */
.plan-card {
  height: 100%;
  min-height: 0;
}
.plan-card :deep(.ant-card-body) {
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  height: calc(100% - 54px);
  overflow-y: auto;
}
.plan-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.plan-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}
.plan-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-body);
}
.plan-label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}
.plan-value {
  color: var(--color-text-primary);
  font-weight: 500;
}
.plan-platform {
  color: var(--color-primary);
  font-weight: 600;
}
.plan-pillars-inline {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  font-size: var(--font-body);
}
.plan-pillars-inline .plan-label {
  padding-top: 2px;
}
.plan-pillar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}
.plan-pillar-tags :deep(.ant-tag) {
  background: var(--color-primary-bg);
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}
.plan-actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: auto;
  padding-top: var(--space-sm);
}
.plan-btn {
  border-radius: var(--radius-lg);
}
.plan-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: var(--space-sm);
  padding: var(--space-md) 0;
}
.plan-empty-title {
  font-size: var(--font-h4);
  font-weight: 600;
  color: var(--color-text-primary);
}
.plan-empty-desc {
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  line-height: 1.6;
  max-width: 260px;
}
.plan-empty-btn {
  margin-top: var(--space-xs);
  border-radius: var(--radius-lg);
}

/* 制定方案弹框 */
.plan-modal-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--space-md) var(--space-sm) var(--space-sm);
  gap: var(--space-sm);
}
.plan-modal-title {
  font-size: var(--font-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}
.plan-modal-desc {
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  line-height: 1.6;
}
.plan-modal-actions {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}
.plan-modal-actions :deep(.ant-btn-primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  border-radius: var(--radius-lg);
}
.plan-modal-actions :deep(.ant-btn-primary:hover) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}
.plan-modal-later {
  border-radius: var(--radius-lg);
}

.create-section {
  display: flex;
  gap: var(--space-md);
}
.create-main-btn {
  flex: 1;
  height: 56px;
  font-size: 17px;
  font-weight: 600;
  border-radius: var(--radius-xl);
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 4px 14px rgba(7, 193, 96, 0.25);
}
.create-main-btn:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}
.create-main-btn :deep(.anticon) {
  font-size: 20px;
}
.weekly-data-btn {
  width: 140px;
  height: 56px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-xl);
  border-color: var(--color-border-default);
  color: var(--color-text-primary);
}
.weekly-data-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.weekly-data-btn :deep(.anticon) {
  font-size: 18px;
}

/* 第二行：快捷操作 + 生成记录 + 占位卡片 */
.bottom-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-lg);
  align-items: stretch;
}
.bottom-row > .left-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.shortcut-card {
  height: fit-content;
}
.shortcut-card :deep(.ant-card-body) {
  padding: var(--space-md) var(--space-lg);
}
.generation-card {
  height: fit-content;
  flex: 1;
  margin-bottom: var(--space-lg);
}
.generation-card :deep(.ant-card-body) {
  padding: 12px var(--space-lg);
  display: flex;
  flex-direction: column;
}
.activity-card-wrapper {
  display: flex;
  align-items: flex-start;
}
.activity-card {
  width: 100%;
  height: fit-content;
}
.activity-card :deep(.ant-card-body) {
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.activity-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: 12px 14px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background 0.2s;
}
.activity-item:hover {
  background: var(--color-bg-hover);
}
.activity-icon {
  width: 62px;
  height: 62px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.activity-icon.lottery {
  background: linear-gradient(135deg, #fff5e6, #ffe0b3);
}
.activity-icon.commission {
  background: var(--color-primary-bg);
}
.activity-icon.skill {
  background: #f0f5ff;
}
.activity-icon.invite {
  background: #fff0f3;
}
.activity-icon.rank {
  background: #fffbe6;
}
.activity-icon.learn {
  background: #f6ffed;
}
.activity-icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--radius-md);
}
.activity-info {
  flex: 1;
  min-width: 0;
}
.activity-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.activity-desc {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-top: 4px;
  line-height: 1.5;
}
.activity-arrow {
  font-size: 14px;
  color: var(--color-text-placeholder);
  flex-shrink: 0;
}

/* 快捷操作 */
.shortcut-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.shortcut-item {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: all 0.2s ease;
}
.shortcut-item:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary-light);
}
.shortcut-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  background: var(--color-primary-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.shortcut-icon {
  font-size: 16px;
  color: var(--color-primary);
}
.shortcut-label {
  font-size: var(--font-small);
  color: var(--color-text-primary);
  font-weight: 500;
  white-space: nowrap;
}

/* 生成记录 */
.generation-card {
  height: 100%;
}
.generation-card :deep(.ant-card-body) {
  padding: 12px var(--space-lg);
  height: calc(100% - 54px);
  display: flex;
  flex-direction: column;
}
.generation-extra {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}
.how-publish-btn {
  padding: 0;
  color: var(--color-primary);
  font-size: var(--font-small);
  font-weight: 500;
}
.how-publish-btn:hover {
  color: var(--color-primary-hover);
}
.generation-empty {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-bg-page);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  font-size: var(--font-small);
}
.empty-icon {
  font-size: 20px;
  color: var(--color-primary);
}
.generation-list {
  display: flex;
  flex-direction: column;
}
.generation-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border-light);
  background: transparent;
  transition: background 0.2s ease;
  cursor: pointer;
}
.generation-item:last-child {
  border-bottom: none;
}
.generation-item:hover {
  background: var(--color-bg-hover);
  margin: 0 calc(-1 * var(--space-lg));
  padding-left: var(--space-lg);
  padding-right: var(--space-lg);
}
.generation-main {
  flex: 1;
  min-width: 0;
}
.generation-title {
  font-size: var(--font-body);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.generation-meta {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot-separator {
  opacity: 0.6;
}
.generation-status.generating {
  color: var(--color-warning);
  font-weight: 500;
}
.generation-status.completed {
  color: var(--color-success);
  font-weight: 500;
}
.generation-status.failed {
  color: var(--color-error);
  font-weight: 500;
}
.generation-progress {
  margin-top: 6px;
}
.generation-progress :deep(.ant-progress-bg) {
  background: var(--color-primary) !important;
}
.generation-arrow {
  font-size: 12px;
  color: var(--color-text-placeholder);
  flex-shrink: 0;
  transition: color 0.2s ease;
}
.generation-item:hover .generation-arrow {
  color: var(--color-primary);
}

/* 发布建议弹窗 */
.publish-guide {
  padding: 4px;
}
.publish-guide-section {
  margin-bottom: var(--space-lg);
}
.publish-guide-section:last-child {
  margin-bottom: 0;
}
.publish-guide-label {
  font-size: var(--font-body);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
  display: flex;
  align-items: center;
}
.publish-guide-value {
  font-size: var(--font-h3);
  color: var(--color-primary);
  font-weight: 700;
  margin-bottom: 4px;
}
.publish-guide-desc {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.publish-guide-platforms {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.publish-guide-platform-item {
  padding: var(--space-md);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
}
.publish-guide-platform-name {
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}
.publish-guide-platform-method {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.6;
}
.publish-guide-doc-link {
  font-size: var(--font-small);
  color: var(--color-primary);
}
.publish-guide-doc-link:hover {
  color: var(--color-primary-hover);
}

/* 账号检测弹窗 */
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
.suggestion-list {
  margin-top: 12px;
}
.suggestion-label {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-xs);
}
.suggestion-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}
.suggestion-item {
  display: inline-block;
  padding: 4px 10px;
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-light);
  border-radius: var(--radius-full);
  font-size: var(--font-small);
  color: var(--color-primary);
  cursor: pointer;
  transition: all 0.2s;
}
.suggestion-item:hover {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
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

/* 创作方式选择弹窗 */
.create-choice-modal :deep(.ant-modal-body) {
  padding: var(--space-lg);
}
.create-choice-body {
  padding: 8px 4px;
}
.create-choice-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
}
.create-choice-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: var(--space-lg);
  background: var(--color-bg-card);
  border: 1.5px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.25s ease;
}
.create-choice-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 8px 24px rgba(7, 193, 96, 0.12);
  transform: translateY(-2px);
}
.choice-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-full);
  background: var(--color-primary-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-md);
}
.choice-icon {
  font-size: 26px;
  color: var(--color-primary);
}
.choice-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}
.choice-desc {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: var(--space-md);
  flex: 1;
}
.choice-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.choice-tag {
  font-size: var(--font-caption);
  color: var(--color-primary);
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-light);
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

/* 本周数据弹窗 */
.weekly-data-modal :deep(.ant-modal-body) {
  padding: var(--space-lg);
}
.weekly-data-summary {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: linear-gradient(135deg, var(--color-primary-bg) 0%, #fff 100%);
  border: 1px solid var(--color-primary-light);
  border-radius: var(--radius-xl);
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-lg);
}
.weekly-data-summary::before {
  content: '';
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: var(--color-primary);
  flex-shrink: 0;
}
.weekly-data-summary strong {
  color: var(--color-primary);
  font-weight: 700;
  margin: 0 2px;
}
.weekly-data-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}
.weekly-data-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
}
.weekly-data-item:focus-within {
  border-color: transparent;
  box-shadow: none;
}
.weekly-data-title,
.weekly-data-reads {
  border-radius: var(--radius-md);
}
.weekly-data-title :deep(.ant-input),
.weekly-data-reads :deep(.ant-input-number-input) {
  border-radius: var(--radius-md);
}
.weekly-data-title :deep(.ant-input:focus),
.weekly-data-title :deep(.ant-input-focused),
.weekly-data-reads :deep(.ant-input-number-focused) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-bg);
}
.weekly-data-title {
  flex: 1;
  min-width: 0;
}
.weekly-data-reads {
  width: 140px;
  flex-shrink: 0;
}
.weekly-data-reads :deep(.ant-input-number-handler-wrap) {
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}
.weekly-data-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
}
.weekly-data-actions .ant-btn-primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  border-radius: var(--radius-md);
  min-width: 88px;
}
.weekly-data-actions .ant-btn-primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

/* 提现进度弹窗 */
.withdraw-modal :deep(.ant-modal-body) {
  padding: var(--space-lg);
}
.withdraw-progress-section {
  text-align: center;
  padding: var(--space-lg);
  background: linear-gradient(135deg, var(--color-primary-bg) 0%, #fff 100%);
  border-radius: var(--radius-xl);
  margin-bottom: var(--space-lg);
}
.withdraw-balance {
  margin-bottom: var(--space-md);
}
.withdraw-label {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}
.withdraw-amount {
  font-size: 40px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
}
.withdraw-amount span {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-left: 4px;
}
.withdraw-target {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-top: 4px;
}
.withdraw-progress-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin: var(--space-md) 0;
}
.withdraw-progress-bar {
  flex: 1;
}
.withdraw-progress-bar :deep(.ant-progress-bg) {
  background: var(--color-primary) !important;
}
.withdraw-percent {
  font-size: var(--font-small);
  font-weight: 600;
  color: var(--color-primary);
  flex-shrink: 0;
}
.withdraw-status {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}
.withdraw-status strong {
  color: var(--color-primary);
  font-weight: 600;
}
.withdraw-plan {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.withdraw-plan-title {
  font-size: var(--font-body);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}
.withdraw-plan-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
}
.withdraw-plan-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: var(--color-primary-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.withdraw-plan-info {
  flex: 1;
  min-width: 0;
}
.withdraw-plan-label {
  font-size: var(--font-body);
  font-weight: 500;
  color: var(--color-text-primary);
}
.withdraw-plan-reward {
  font-size: var(--font-small);
  color: var(--color-primary);
  font-weight: 600;
  margin-top: 2px;
}
.withdraw-plan-btn {
  border-radius: var(--radius-lg);
}

/* 实时提现滚动 */
.withdraw-marquee {
  margin-top: var(--space-lg);
  padding: var(--space-md);
  background: var(--color-bg-page);
  border-radius: var(--radius-lg);
}
.withdraw-marquee-title {
  font-size: var(--font-small);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
  text-align: center;
}
.withdraw-marquee-wrap {
  height: 90px;
  overflow: hidden;
  position: relative;
}
.withdraw-marquee-wrap::before,
.withdraw-marquee-wrap::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 20px;
  z-index: 1;
  pointer-events: none;
}
.withdraw-marquee-wrap::before {
  top: 0;
  background: linear-gradient(to bottom, var(--color-bg-page), transparent);
}
.withdraw-marquee-wrap::after {
  bottom: 0;
  background: linear-gradient(to top, var(--color-bg-page), transparent);
}
.withdraw-marquee-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  animation: marquee-scroll 12s linear infinite;
}
@keyframes marquee-scroll {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-50%);
  }
}
.withdraw-marquee-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  white-space: nowrap;
}
.marquee-name {
  color: var(--color-text-primary);
  font-weight: 500;
}
.marquee-amount {
  color: var(--color-primary);
  font-weight: 600;
}
.marquee-status {
  color: var(--color-success);
  font-weight: 500;
}

/* 响应式 */
@media (max-width: 992px) {
  .top-row {
    grid-template-columns: 1fr;
  }
  .bottom-row {
    grid-template-columns: 1fr;
  }
  .welcome-body {
    gap: var(--space-md);
  }
}
@media (max-width: 768px) {
  .workbench-index {
    padding: var(--space-md);
  }
  .welcome-body {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }
  .welcome-info {
    padding: var(--space-md) 0;
    border-left: none;
    border-right: none;
    border-top: 1px solid var(--color-border-light);
    border-bottom: 1px solid var(--color-border-light);
  }
  .welcome-balance {
    width: 100%;
    padding: 0;
    border-top: none;
  }
  .welcome-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
  .meta-divider {
    display: none;
  }
  .create-section {
    flex-direction: column;
  }
  .weekly-data-btn {
    width: 100%;
  }
  .generation-item {
    gap: var(--space-sm);
  }
  .generation-item:hover {
    margin: 0 calc(-1 * var(--space-md));
    padding-left: var(--space-md);
    padding-right: var(--space-md);
  }
  .weekly-data-item {
    flex-direction: column;
    align-items: stretch;
  }
  .weekly-data-reads {
    width: 100%;
  }
  .weekly-data-actions {
    flex-direction: column;
  }
  .create-choice-options {
    grid-template-columns: 1fr;
  }
}


/* 账号检测弹窗主题色 */
.account-modal :deep(.ant-modal-title) {
  color: var(--color-primary);
  font-weight: 600;
}

.account-modal :deep(.ant-btn-primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.account-modal :deep(.ant-btn-primary:hover),
.account-modal :deep(.ant-btn-primary:focus) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.account-modal :deep(.ant-btn-primary:active) {
  background: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

.account-modal :deep(.ant-btn-primary[disabled]) {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #bfbfbf;
}

.validation-result {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}

.validation-result.fit {
  color: var(--color-success);
}

.validation-result.unfit {
  color: var(--color-primary);
}

.result-icon {
  font-size: 14px;
}

.validation-reason {
  margin-top: 6px;
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  line-height: 1.6;
  padding: 8px 10px;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
}

.register-check-row {
  margin-top: 12px;
}
</style>

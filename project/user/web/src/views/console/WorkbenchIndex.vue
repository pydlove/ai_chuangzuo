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
                :size="62"
                src="/assets/images/小爱-v1.png"
                alt="小爱"
                class="ai-avatar"
              />
              <div class="dialogue-bubble">
                <div class="dialogue-title">
                  尊敬的{{ userInfo.nickname ? userInfo.nickname + '老师' : '老师' }}您好，我是您的专属自媒体顾问小爱
                </div>
                <div class="mobile-dialogue-title">
                  <span class="mobile-title-main">嗨！我是小爱！</span>
                  <span class="mobile-title-tag">专属自媒体顾问</span>
                </div>
                <div class="dialogue-greeting">
                  <span v-if="!hasPlan" class="todo-text">
                    我可以帮您定制专属您的自媒体运营方案，<a href="javascript:;" class="plan-link" @click="goToOnboarding()">去制定</a>
                  </span>
                  <span v-else-if="todayDone" class="done-text">🎉 🎉 🎉 今日创作目标已达成，继续保持！</span>
                  <span v-else class="todo-text">今日任务还没完成，点击「开始今日创作」去写一篇吧</span>
                </div>
              </div>
            </div>

            <!-- 移动端右侧人物图 -->
            <div class="welcome-mascot">
              <img src="/assets/images/墨墨-V1.png" alt="墨墨" />
            </div>

            <!-- 中间：个人信息区域 -->
            <div class="welcome-info">
              <div class="info-header">
                <a-avatar :size="40" class="user-avatar-mini" :src="userInfo.avatarUrl">
                  {{ userInfo.nickname ? userInfo.nickname[0] : 'U' }}
                </a-avatar>
                <span class="info-name">{{ userInfo.nickname || '未设置昵称' }}</span>
                <a-tag v-if="userInfo.vipLevel" class="vip-tag" color="#ff2442">
                  <CrownOutlined /> {{ userInfo.vipLevel }}
                </a-tag>
              </div>
              <div class="welcome-meta">
                <span v-if="userInfo.email" class="meta-item"><MailOutlined /> {{ userInfo.email }}</span>
                <span v-if="userInfo.phone" class="meta-item"><PhoneOutlined /> {{ userInfo.phone }}</span>
                <span v-if="!userInfo.email && !userInfo.phone" class="meta-item">未绑定联系方式</span>
                <a-divider
                  v-if="(userInfo.email || userInfo.phone) && userInfo.inviteCode"
                  type="vertical"
                  class="meta-divider"
                />
                <span v-if="userInfo.inviteCode" class="meta-item">邀请码：{{ userInfo.inviteCode }}</span>
                <a-button
                  v-if="userInfo.inviteCode"
                  type="link"
                  size="small"
                  class="copy-code-btn"
                  @click="copyInviteCode"
                >
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
          <a-button type="primary" size="large" class="create-main-btn desktop-create-btn" @click="consoleActions.openCreateChoice?.()">
            <EditOutlined />
            开始今日创作{{ quotaText }}
          </a-button>
          <div class="mobile-create-btn" @click="consoleActions.openCreateChoice?.()">
            <span class="mobile-create-text">开始今日创作{{ quotaText }}</span>
          </div>
        </div>

        <!-- 手机端功能栏 -->
        <div class="feature-bar">
          <div class="feature-top-row">
            <div class="feature-large-card" @click="router.push('/console/commission')">
              <div class="feature-large-info">
                <div class="feature-large-title">赚创作币</div>
                <div class="feature-large-desc">精选任务赚创作币</div>
              </div>
              <div class="feature-large-icon">
                <img src="/assets/images/约稿任务-v1.png" alt="赚创作币" />
              </div>
            </div>
            <div class="feature-large-card" @click="router.push('/console/skill-market')">
              <div class="feature-large-info">
                <div class="feature-large-title">提示词市场</div>
                <div class="feature-large-desc">提升创作质量</div>
              </div>
              <div class="feature-large-icon">
                <img src="/assets/images/提示词市场-v1.png" alt="提示词市场" />
              </div>
            </div>
          </div>

          <div class="feature-grid">
            <div
              v-for="item in featureItems"
              :key="item.label"
              class="feature-grid-item"
              @click="item.action ? item.action() : router.push(item.path)"
            >
              <div class="feature-grid-icon">
                <img :src="item.image" :alt="item.label" />
              </div>
              <span class="feature-grid-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="plan-section">
        <div class="plan-section-header">
          <span class="plan-section-title">运营方案</span>
          <a-button
            v-if="hasPlan"
            size="small"
            class="plan-btn"
            @click="openAdjustPlanConfirm"
          >
            调整方案{{ planAdjustText }}
          </a-button>
        </div>
        <a-card class="wb-card plan-card" :bordered="false">
          <div v-if="hasPlan" class="plan-content">
            <div class="plan-grid">
              <div class="plan-row">
                <span class="plan-label">主攻平台</span>
                <div class="plan-platform">
                  <span class="plan-platform-text">{{ plan.platform }}</span>
                  <img class="plan-platform-icon" src="/assets/images/运营方案图标-v2.png" alt="" />
                </div>
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
          </div>
          <div v-else class="plan-empty">
            <img class="plan-empty-icon" src="/assets/images/运营方案空状态-v1.png" alt="运营方案" />
            <div class="plan-empty-title">您还没有专属运营方案</div>
            <div class="plan-empty-desc">
              您的专属顾问小爱会为您量身定制一套专属的自媒体运营方案，陪您一起经营您的自媒体账号，快去行动吧，
              <a href="javascript:;" class="plan-link" @click="goToOnboarding()">立即制定</a>
            </div>
          </div>
        </a-card>
      </div>
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
              <a-button
                type="text"
                size="small"
                class="refresh-records-btn"
                :loading="generationRecordsLoading"
                @click="loadGenerationRecords"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>
              </a-button>
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
            <a-empty description="暂无数据" />
          </div>
          <div v-else class="generation-list">
            <div
              v-for="record in recentRecords"
              :key="record.id"
              class="generation-item"
              :class="record.status"
              @click="record.status === 'completed' ? openArticleView(record) : router.push('/console/works')"
            >
              <div class="generation-main">
                <div class="generation-header">
                  <div class="generation-title">{{ record.title }}</div>
                </div>
                <div class="generation-meta">
                  <ClockCircleOutlined class="generation-time-icon" />
                  <span>{{ formatRecordTime(record.createdAtTimestamp) }}</span>
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
              <div
                v-if="record.status === 'completed'"
                class="generation-actions"
              >
                <a-button
                  type="link"
                  size="small"
                  class="repost-btn"
                  @click.stop="openRepostsPlan(record)"
                >
                  一文多发
                </a-button>
                <a-tag
                  v-if="!hasBenefit('repost_plan')"
                  color="#ff2442"
                  size="small"
                  class="repost-pro-tag"
                >
                  专业版
                </a-tag>
                <a-button
                  type="link"
                  size="small"
                  class="view-article-btn"
                  @click.stop="openArticleView(record)"
                >
                  查看
                </a-button>
              </div>
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
              <div class="activity-icon">
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
      <div class="publish-plan-spin">
        <a-spin :spinning="publishPlanLoading" tip="小爱正在为您准备发布建议...">
          <template v-if="!hasPlan">
            <a-empty description="请先制定自媒体运营方案，再生成发布计划">
              <a-button type="primary" @click="goToOnboarding()">去制定方案</a-button>
            </a-empty>
          </template>
          <template v-else-if="publishPlan">
            <div class="publish-guide-section">
              <div class="publish-guide-label">建议发布时间</div>
              <div class="publish-guide-value">{{ publishPlan.mainPlatform?.publishTime || '-' }}</div>
              <div class="publish-guide-desc">
                主攻平台「{{ publishPlan.mainPlatform?.platform || currentPublishRecord?.platform || plan.platform }}」：{{ publishPlan.mainPlatform?.reason || '基于流量高峰和账号冷启动效率推荐' }}
              </div>
            </div>
            <div class="publish-guide-section">
              <div class="publish-guide-label">冷启动策略</div>
              <div class="publish-guide-coldstart-duration">{{ publishPlan.coldStart?.duration || '发布后 30 分钟内' }}</div>
              <ul class="publish-guide-coldstart-list">
                <li v-for="(action, idx) in publishPlan.coldStart?.immediateActions" :key="idx">{{ action }}</li>
                <li v-if="!publishPlan.coldStart?.immediateActions?.length">发布后立即点赞、收藏并阅读一遍</li>
              </ul>
              <div v-if="publishPlan.coldStart?.sharingTips" class="publish-guide-coldstart-share">
                💡 {{ publishPlan.coldStart.sharingTips }}
              </div>
            </div>
            <div class="publish-guide-section">
              <div class="publish-guide-label">发送方式</div>
              <div class="publish-guide-value">{{ sendMethod.method }}</div>
              <a :href="sendMethod.docLink" target="_blank" class="publish-guide-doc-link">{{ sendMethod.docText }}</a>
            </div>
          </template>
          <template v-else>
            <a-empty description="暂无发布计划" />
          </template>
        </a-spin>
      </div>
    </a-modal>

    <!-- 一文多发方案弹窗 -->
    <a-modal
      :open="repostsModalVisible"
      title="一文多发方案"
      width="700px"
      :footer="null"
      class="reposts-modal"
      @cancel="repostsModalVisible = false"
    >
      <div class="reposts-modal-spin">
        <a-spin :spinning="repostsLoading" tip="小爱正在准备多平台方案…">
          <template v-if="!hasPlan">
            <a-empty description="请先制定自媒体运营方案，再生成多平台发布计划">
              <a-button type="primary" @click="goToOnboarding()">去制定方案</a-button>
            </a-empty>
          </template>
          <template v-else-if="currentRepostPlan?.reposts?.length">
            <div class="reposts-modal-list">
              <div
                v-for="(item, idx) in currentRepostPlan.reposts"
                :key="item.platform + idx"
                class="reposts-modal-card"
              >
                <div class="reposts-modal-header">
                  <span class="reposts-modal-platform">{{ item.platform }}</span>
                  <span class="reposts-modal-time">{{ item.publishTime }}</span>
                </div>
                <div class="reposts-modal-field">
                  <span class="reposts-modal-label">标题</span>
                  <span class="reposts-modal-value">{{ item.title || '-' }}</span>
                </div>
                <div class="reposts-modal-field">
                  <span class="reposts-modal-label">标签</span>
                  <div class="reposts-modal-tags">
                    <span
                      v-for="tag in item.tags"
                      :key="tag"
                      class="reposts-modal-tag"
                    >{{ tag }}</span>
                    <span v-if="!item.tags?.length" class="reposts-modal-value">-</span>
                  </div>
                </div>
                <div class="reposts-modal-field">
                  <span class="reposts-modal-label">配图建议</span>
                  <span class="reposts-modal-value">{{ item.imageSuggestions || '-' }}</span>
                </div>
                <div v-if="item.tips" class="reposts-modal-field">
                  <span class="reposts-modal-label">发布建议</span>
                  <span class="reposts-modal-value">{{ item.tips }}</span>
                </div>
              </div>
            </div>
          </template>
          <template v-else>
            <a-empty description="暂无多平台发布方案" />
          </template>
        </a-spin>
      </div>
    </a-modal>

    <AccountCheckModal v-model:visible="accountModalVisible" />

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
        本周共发布 <strong>{{ validWeeklyArticles.length }}</strong> 篇，总阅读量 <strong>{{ totalWeeklyReads }}</strong>
      </div>
      <div class="weekly-data-list">
        <div
          v-for="(item, index) in weeklyArticles"
          :key="index"
          class="weekly-data-item"
        >
          <a-input v-model:value="item.title" placeholder="文章标题" class="weekly-data-title" :maxlength="256" show-count />
          <a-input-number v-model:value="item.reads" placeholder="阅读量" :min="0" class="weekly-data-reads" />
          <a-button
            v-if="weeklyArticles.length > 1"
            type="text"
            danger
            class="weekly-data-remove"
            @click="removeWeeklyArticle(index)"
          >
            <DeleteOutlined />
          </a-button>
        </div>
      </div>
      <div class="weekly-data-actions">
        <a-button type="dashed" @click="addWeeklyArticle">
          <PlusOutlined />
          添加文章
        </a-button>
        <a-button type="primary" :loading="weeklyLoading" @click="saveWeeklyData">保存</a-button>
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
          <template v-if="balancePercent >= 100">
            已达到提现门槛，<a class="withdraw-go-link" @click="goToWithdrawPage">去提现</a>
          </template>
          <template v-else>
            还差 <strong>{{ coinsToWithdraw }}</strong> 创作币，完成下方任务即可提现
          </template>
        </div>
      </div>
      <div v-if="balancePercent < 100" class="withdraw-plan">
        <div class="withdraw-plan-title">快速达标方案</div>
        <div
          v-for="task in withdrawTasks"
          :key="task.label"
          class="withdraw-plan-item"
        >
          <div class="withdraw-plan-icon">
            <img :src="task.img" alt="" class="withdraw-plan-icon-img" />
          </div>
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
        <div class="withdraw-marquee-title">🎉 🎉 🎉 实时提现成功</div>
        <div class="withdraw-marquee-wrap">
          <div class="withdraw-marquee-list">
            <div
              v-for="item in withdrawRecords"
              :key="item.id"
              class="withdraw-marquee-item"
            >
              <span class="marquee-name">{{ item.name }}</span>
              <span>提现</span>
              <span class="marquee-amount">{{ item.amount }} 元</span>
              <span class="marquee-status">成功</span>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 制定自媒体方案弹框 -->
    <a-modal
      v-model:open="planModalVisible"
      title="定制你的自媒体方案"
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
    <!-- 调整方案确认弹框 -->
    <a-modal
      v-model:open="adjustPlanConfirmVisible"
      title="提示"
      width="420px"
      :footer="null"
      centered
      class="adjust-plan-confirm-modal"
      @cancel="adjustPlanConfirmVisible = false"
    >
      <div class="adjust-plan-confirm-body">
        老师，做自媒体最重要的是坚持，频繁修改运营方案会影响您的账号定位和流量，是否继续调整？
      </div>
      <div class="adjust-plan-confirm-footer">
        <a-button type="default" class="continue-btn" @click="confirmAdjustPlan">继续</a-button>
        <a-button @click="adjustPlanConfirmVisible = false">取消</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, onUnmounted, watch, inject } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  EditOutlined,
  CrownOutlined,
  MailOutlined,
  PhoneOutlined,
  UserOutlined,
  FileTextOutlined,
  ShopOutlined,
  FireOutlined,
  RightOutlined,
  SafetyCertificateOutlined,
  BulbOutlined,
  TrophyOutlined,
  BookOutlined,
  CodeOutlined,
  CreditCardOutlined,
  SafetyOutlined,
  TagOutlined,
  QuestionCircleOutlined,
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined,
  GiftOutlined,
  TeamOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'
import AccountCheckModal from '@/components/AccountCheckModal.vue'
import { fetchCurrentPlan, generatePublishPlan } from '@/api/selfMediaPlan.js'
import { getMyProfile } from '@/api/user.js'
import { getAccountSummary } from '@/api/earnings.js'
import { getMyMembership } from '@/api/membership.js'
import { listGenerationTasks } from '@/api/generation.js'
import { getWeeklyArticles, saveWeeklyArticles } from '@/api/workbench.js'
import { getArticleByTaskId } from '@/api/article.js'
import { useWithdraw } from '@/composables/useWithdraw.js'
import { useBenefits } from '@/composables/useBenefits.js'

const router = useRouter()

const isMobile = () => window.innerWidth <= 768

const consoleActions = inject('consoleActions', {})

const userInfo = reactive({
  nickname: '',
  email: '',
  phone: '',
  inviteCode: '',
  avatarUrl: '',
  vipLevel: '',
  vipExpire: ''
})

const balance = reactive({
  coin: 120,
  withdrawThreshold: 1000
})

const { withdrawRecords: rawWithdrawRecords, loadWithdrawals } = useWithdraw()
const { benefits, loadBenefits, hasBenefit } = useBenefits()

const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)
const quotaText = computed(() => {
  if (quotaTotal.value === 0) return ''
  return `（剩余 ${quotaRemaining.value} 次）`
})

const planAdjustTotal = computed(() => Number(benefits.value['plan_adjust_quota']?.value) || 0)
const planAdjustRemaining = computed(() => benefits.value['plan_adjust_quota']?.remaining ?? 0)
const planAdjustText = computed(() => {
  if (planAdjustTotal.value === 0) return ''
  return `（本月剩余 ${planAdjustRemaining.value} 次）`
})

const withdrawRecords = computed(() => {
  return rawWithdrawRecords.value
    .filter((r) => r.status === 'approved')
    .map((r) => ({
      id: r.id,
      name: r.nickname || r.name || '用户',
      amount: Number((r.amount / 10).toFixed(2))
    }))
    .sort((a, b) => b.id.localeCompare(a.id))
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

const hasMembership = ref(false)

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
    userInfo.phone = profile.phone || ''
    userInfo.inviteCode = profile.inviteCode || ''
    userInfo.avatarUrl = profile.avatarUrl || ''

    balance.coin = summary?.coinBalance || 0

    const membership = membershipRes?.data || {}
    hasMembership.value = membership.hasMembership || false
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

function goToOnboarding(reset = false) {
  if (!hasMembership.value) {
    message.warning('制定运营方案需要开通会员，请先订阅套餐')
    router.push('/pricing')
    return
  }
  const path = reset ? '/console/onboarding?reset=1' : '/console/onboarding'
  router.push(path)
}

function goToPlan() {
  planModalVisible.value = false
  goToOnboarding()
}

function openAdjustPlanConfirm() {
  if (planAdjustTotal.value > 0 && planAdjustRemaining.value <= 0) {
    message.warning('本月调整方案次数已用完，如需继续调整请升级套餐')
    return
  }
  adjustPlanConfirmVisible.value = true
}

function confirmAdjustPlan() {
  adjustPlanConfirmVisible.value = false
  goToOnboarding(true)
}

function dismissPlanModal() {
  planModalVisible.value = false
  localStorage.setItem(SELF_MEDIA_PLAN_MODAL_KEY, '1')
}

onMounted(() => {
  todayDone.value = localStorage.getItem(todayKey.value) === '1'
  loadBenefits()
  loadWelcomeData()
  loadGenerationRecords()
  loadWithdrawals()
  loadPlan().then(() => {
    if (!hasPlan.value && !localStorage.getItem(SELF_MEDIA_PLAN_MODAL_KEY)) {
      planModalVisible.value = true
    }
  })
  unregisterCreateTaskCallback = consoleActions.registerCreateTaskCallback?.((task) => {
    setTodayDone()
    loadGenerationRecords()
  })
})

onUnmounted(() => {
  if (unregisterCreateTaskCallback) {
    unregisterCreateTaskCallback()
    unregisterCreateTaskCallback = null
  }
})

const accountModalVisible = ref(false)
const weeklyDataVisible = ref(false)
const weeklyLoading = ref(false)
const withdrawModalVisible = ref(false)
const adjustPlanConfirmVisible = ref(false)

let unregisterCreateTaskCallback = null

const withdrawTasks = [
  { label: '参加 2 个约稿任务', reward: 40, path: '/console/commission', img: '/assets/images/约稿任务-v1.png' },
  { label: '发布 1 个提示词', reward: 20, path: '/console/skill-market', img: '/assets/images/提示词市场-v1.png' },
  { label: '邀请 1 个好友', reward: 20, path: '/console/invite', img: '/assets/images/邀请有礼-v1.png' }
]

function goWithdrawTask(path) {
  withdrawModalVisible.value = false
  router.push(path)
}

function goToWithdrawPage() {
  withdrawModalVisible.value = false
  router.push('/console/coin?from=workbench')
}

const weeklyArticles = reactive([])

const validWeeklyArticles = computed(() =>
  weeklyArticles.filter(item => (item.title || '').trim())
)

const totalWeeklyReads = computed(() => {
  return validWeeklyArticles.value.reduce((sum, item) => sum + (Number(item.reads) || 0), 0)
})

function addWeeklyArticle() {
  weeklyArticles.push({ title: '', reads: 0 })
}

function removeWeeklyArticle(index) {
  weeklyArticles.splice(index, 1)
  if (weeklyArticles.length === 0) {
    addWeeklyArticle()
  }
}

async function loadWeeklyArticles() {
  weeklyLoading.value = true
  try {
    const res = await getWeeklyArticles()
    const list = res?.data || []
    weeklyArticles.splice(0, weeklyArticles.length,
      ...list.map(item => ({ title: item.title || '', reads: item.reads ?? 0 })))
    if (weeklyArticles.length === 0) {
      addWeeklyArticle()
    }
  } catch (err) {
    message.error(err?.message || '加载本周数据失败')
  } finally {
    weeklyLoading.value = false
  }
}

watch(weeklyDataVisible, (visible) => {
  if (visible) {
    loadWeeklyArticles()
  }
})

async function saveWeeklyData() {
  const payload = weeklyArticles
    .map(item => ({ title: (item.title || '').trim(), reads: Number(item.reads) || 0 }))
    .filter(item => item.title)
  if (!payload.length) {
    message.warning('请至少填写一篇文章标题')
    return
  }
  weeklyLoading.value = true
  try {
    await saveWeeklyArticles({ articles: payload })
    message.success('本周数据已保存')
    weeklyDataVisible.value = false
  } catch (err) {
    message.error(err?.message || '保存失败')
  } finally {
    weeklyLoading.value = false
  }
}

const shortcuts = [
  { label: '账号名检测', icon: SafetyCertificateOutlined, action: () => { isMobile() ? router.push('/console/account-check') : (accountModalVisible.value = true) } },
  { path: '/console/commission', label: '约稿中心', icon: ShopOutlined },
  { path: '/console/skill-market', label: '提示词市场', icon: BulbOutlined },
  { path: '/console/leaderboard', label: '收益排行榜', icon: TrophyOutlined },
  { path: '/console/hot-search', label: '热搜榜', icon: FireOutlined },
  { path: '/console/learn', label: '创作学院', icon: BookOutlined },
  { path: '/console/works', label: '我的作品', icon: FileTextOutlined },
  { path: '/console/skills', label: '我的提示词', icon: CodeOutlined },
  { path: '/console/earnings', label: '我的账户', icon: CreditCardOutlined },
  { path: '/console/benefits', label: '我的权益', icon: SafetyOutlined },
  { path: '/console/coupons', label: '我的优惠券', icon: TagOutlined },
  {
    label: '帮助文档',
    icon: QuestionCircleOutlined,
    action: () => window.open('https://fxbi16ko1px.feishu.cn/docx/BXVqdp4XwodssXxlfECcUfODnib?from=from_copylink', '_blank')
  }
]

const featureItems = [
  { label: '账号检测', image: '/assets/images/账号检测-v1.jpg', action: () => { isMobile() ? router.push('/console/account-check') : (accountModalVisible.value = true) } },
  { path: '/console/leaderboard', label: '收益排行榜', image: '/assets/images/收益排行榜-v1.png' },
  { path: '/console/lottery', label: '幸运抽奖', image: '/assets/images/幸运抽奖-v1.png' },
  { path: '/console/invite', label: '邀请有礼', image: '/assets/images/邀请有礼-v1.png' },
  { label: '本周数据', image: '/assets/images/本周数据.png', action: () => { isMobile() ? router.push('/console/weekly-data') : (weeklyDataVisible.value = true) } }
]

const activities = [
  {
    label: '幸运抽奖',
    desc: '每日免费抽奖，创作币、会员时长、限定模板等好礼送不停',
    path: '/console/lottery',
    img: '/assets/images/幸运抽奖-v1.png'
  },
  {
    label: '约稿任务',
    desc: '精选品牌与创作者对接，完成任务即可获得丰厚创作币奖励',
    path: '/console/commission',
    img: '/assets/images/约稿任务-v1.png'
  },
  {
    label: '提示词市场',
    desc: '上传原创提示词，被他人使用即可持续获得收益分成',
    path: '/console/skill-market',
    img: '/assets/images/提示词市场-v1.png'
  },
  {
    label: '邀请有礼',
    desc: '邀请好友加入，双方均可获得创作币与会员权益奖励',
    path: '/console/invite',
    img: '/assets/images/邀请有礼-v1.png'
  },
  {
    label: '收益排行榜',
    desc: '实时查看平台创作者收益榜单，学习头部创作者的变现路径',
    path: '/console/leaderboard',
    img: '/assets/images/收益排行榜-v1.png'
  },
  {
    label: '创作学院',
    desc: '从选题、标题到爆款结构，系统化课程帮你快速提升创作能力',
    path: '/console/learn',
    img: '/assets/images/创作学院-v1.png'
  }
]

const generationRecords = reactive([])
const generationRecordsLoading = ref(false)

const statusCodeMap = {
  0: 'pending',
  1: 'generating',
  2: 'completed',
  3: 'failed'
}

const platformNameMap = {
  xiaohongshu: '小红书',
  wechat: '公众号',
  toutiao: '今日头条',
  baijiahao: '百家号',
  douyin: '抖音',
  zhihu: '知乎',
  bilibili: 'B站'
}

async function loadGenerationRecords() {
  generationRecordsLoading.value = true
  try {
    const res = await listGenerationTasks({ page: 1, pageSize: 20 })
    const list = res?.list || []
    generationRecords.length = 0
    list.forEach(item => {
      const ts = item.createdAt ? new Date(item.createdAt).getTime() : Date.now()
      generationRecords.push({
        id: item.id,
        bizNo: item.bizNo,
        articleBizNo: item.articleBizNo,
        title: item.title || '未命名创作',
        platform: item.inputParam?.platform || '',
        status: statusCodeMap[item.status] || 'generating',
        progress: item.progressPct || 0,
        createdAt: item.createdAt
          ? new Date(item.createdAt).toLocaleString('zh-CN', {
              month: 'numeric',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            }).replace(/\//g, '-')
          : '',
        createdAtTimestamp: ts
      })
    })
  } catch (e) {
    console.warn('加载生成记录失败', e)
  } finally {
    generationRecordsLoading.value = false
  }
}

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
const publishPlan = ref(null)
const publishPlanLoading = ref(false)

const repostsModalVisible = ref(false)
const currentRepostRecord = ref(null)
const currentRepostPlan = ref(null)
const repostsLoading = ref(false)

// 发布方案 AI 结果缓存：同一篇文章（articleBizNo / task bizNo）复用已调用结果，
// 避免重复点击「如何发布」「一文多发」时反复请求 AI。
const aiPublishPlanCache = reactive(new Map())
const aiPublishPlanInflight = reactive(new Map())

const sendMethod = computed(() => {
  return {
    method: '手动复制到各平台发布',
    docLink: 'https://fxbi16ko1px.feishu.cn/docx/BXVqdp4XwodssXxlfECcUfODnib?from=from_copylink',
    docText: '查看《手动发布操作文档》'
  }
})

function planCacheKey(record) {
  return record?.articleBizNo || record?.bizNo || record?.id || ''
}

function ensureRepostPlanBenefit() {
  if (!hasMembership.value) {
    message.warning('当前套餐不支持发布方案建议，请升级专业版及以上套餐')
    router.push('/pricing')
    return false
  }
  if (!hasBenefit('repost_plan')) {
    message.warning('当前套餐不支持发布方案建议，请升级专业版及以上套餐')
    return false
  }
  return true
}

async function loadPublishPlan(record) {
  publishPlan.value = null
  if (!hasPlan.value) return
  const title = record?.title?.trim() || `关于${plan.niche || '运营方向'}的内容`
  const mainPlatform = platformNameMap[record?.platform] || record?.platform || plan.platform
  if (!title || !mainPlatform) return

  const key = planCacheKey(record)
  if (!key) return

  const cached = aiPublishPlanCache.get(key)
  if (cached) {
    publishPlan.value = cached
    return
  }

  const inflight = aiPublishPlanInflight.get(key)
  if (inflight) {
    publishPlanLoading.value = true
    try {
      publishPlan.value = await inflight
    } finally {
      publishPlanLoading.value = false
    }
    return
  }

  publishPlanLoading.value = true
  const promise = generatePublishPlan({ articleTitle: title, mainPlatform })
    .then((res) => {
      const data = res?.data || null
      if (data) {
        aiPublishPlanCache.set(key, data)
      }
      return data
    })
    .catch((err) => {
      aiPublishPlanCache.delete(key)
      throw err
    })
    .finally(() => {
      aiPublishPlanInflight.delete(key)
    })
  aiPublishPlanInflight.set(key, promise)

  try {
    publishPlan.value = await promise
  } catch (err) {
    message.error(err?.message || '生成发布计划失败，请重试')
  } finally {
    publishPlanLoading.value = false
  }
}

function openPublishGuide(record) {
  if (!ensureRepostPlanBenefit()) return
  currentPublishRecord.value = record
  loadPublishPlan(record)
  publishModalVisible.value = true
}

function openHowToPublish() {
  if (!ensureRepostPlanBenefit()) return
  const completed = recentRecords.value.find(r => r.status === 'completed')
  const record = completed || recentRecords.value[0] || null
  currentPublishRecord.value = record
  loadPublishPlan(record)
  publishModalVisible.value = true
}

async function openRepostsPlan(record) {
  if (!ensureRepostPlanBenefit()) return
  if (!record) return
  currentRepostRecord.value = record
  currentRepostPlan.value = null
  repostsModalVisible.value = true
  if (!hasPlan.value) return
  const title = record.title?.trim() || `关于${plan.niche || '运营方向'}的内容`
  const mainPlatform = platformNameMap[record.platform] || record.platform || plan.platform
  if (!title || !mainPlatform) return

  const key = planCacheKey(record)
  if (!key) return

  const cached = aiPublishPlanCache.get(key)
  if (cached) {
    currentRepostPlan.value = cached
    return
  }

  const inflight = aiPublishPlanInflight.get(key)
  if (inflight) {
    repostsLoading.value = true
    try {
      currentRepostPlan.value = await inflight
    } finally {
      repostsLoading.value = false
    }
    return
  }

  repostsLoading.value = true
  const promise = generatePublishPlan({ articleTitle: title, mainPlatform })
    .then((res) => {
      const data = res?.data || null
      if (data) {
        aiPublishPlanCache.set(key, data)
      }
      return data
    })
    .catch((err) => {
      aiPublishPlanCache.delete(key)
      throw err
    })
    .finally(() => {
      aiPublishPlanInflight.delete(key)
    })
  aiPublishPlanInflight.set(key, promise)

  try {
    currentRepostPlan.value = await promise
  } catch (err) {
    message.error(err?.message || '生成多平台方案失败，请重试')
  } finally {
    repostsLoading.value = false
  }
}

async function openArticleView(record) {
  if (!record) return
  let bizNo = record.articleBizNo
  if (!bizNo && record.id) {
    try {
      const article = await getArticleByTaskId(record.id)
      bizNo = article?.bizNo
    } catch (err) {
      message.error(err?.message || '查看失败，请重试')
      return
    }
  }
  if (!bizNo) {
    message.error('作品尚未生成完成，暂无法查看')
    return
  }
  router.push(`/console/preview/${bizNo}`)
}

function formatRecordTime(ts) {
  if (!ts) return ''
  const now = new Date()
  const time = new Date(ts)
  const diff = Math.floor((now.getTime() - time.getTime()) / 1000)

  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`

  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (
    time.getFullYear() === yesterday.getFullYear() &&
    time.getMonth() === yesterday.getMonth() &&
    time.getDate() === yesterday.getDate()
  ) {
    return `昨天 ${String(time.getHours()).padStart(2, '0')}:${String(time.getMinutes()).padStart(2, '0')}`
  }

  return `${time.getMonth() + 1}月${time.getDate()}日 ${String(time.getHours()).padStart(2, '0')}:${String(time.getMinutes()).padStart(2, '0')}`
}

function statusText(status) {
  const map = { pending: '排队中', generating: '生成中', completed: '已完成', failed: '生成失败' }
  return map[status] || status
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
.refresh-records-btn {
  color: var(--color-text-secondary);
  margin-left: 4px;
}
.refresh-records-btn:hover {
  color: var(--color-primary);
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
  flex: 5;
  min-width: 0;
}
.ai-avatar {
  flex-shrink: 0;
  background: transparent;
  margin-top: 15px;
}
.ai-avatar :deep(img) {
  object-fit: cover;
}
.dialogue-bubble {
  flex: 1;
  min-width: 0;
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
  flex: 3.5;
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
.plan-link {
  color: var(--color-info, #1989fa);
  text-decoration: underline;
  cursor: pointer;
}
.plan-link:hover {
  color: #1478d2;
}
.welcome-balance {
  flex: 1.5;
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

/* 移动端欢迎卡片与功能栏专用元素，PC 端隐藏 */
.mobile-dialogue-title,
.welcome-mascot,
.feature-bar {
  display: none;
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
.plan-section {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.plan-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
}
.plan-section-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-text-primary);
}
.plan-card {
  flex: 1;
  min-height: 0;
  max-height: 220px;
}
.plan-card :deep(.ant-card-head) {
  display: none;
}
.plan-card :deep(.ant-card-body) {
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  height: 100%;
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
.plan-platform-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
  flex-shrink: 0;
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
.plan-btn {
  border-radius: var(--radius-lg);
}
.plan-btn:hover,
.plan-btn:focus {
  border-color: var(--color-primary);
  color: var(--color-primary);
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

/* 调整方案确认弹框 */
.adjust-plan-confirm-modal :deep(.ant-modal-body) {
  padding: var(--space-lg);
}
.adjust-plan-confirm-body {
  font-size: var(--font-body);
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: var(--space-lg);
}
.adjust-plan-confirm-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
}
.continue-btn {
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
.desktop-create-btn {
  display: inline-flex;
}
.mobile-create-btn {
  display: none;
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
.generation-card :deep(.ant-card-head) {
  border-bottom: none;
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
  overflow: hidden;
  background: #fff;
}
.activity-icon-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 4px;
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
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-sm);
}
.shortcut-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 12px;
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
.repost-btn {
  padding: 0 8px;
  color: var(--color-primary);
  font-size: var(--font-small);
  font-weight: 500;
  flex-shrink: 0;
}
.repost-btn:hover {
  color: var(--color-primary-hover);
}
.repost-pro-tag {
  margin-inline-start: 4px;
  font-size: 11px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 999px;
}
.view-article-btn {
  padding: 0 8px;
  color: var(--color-text-secondary);
  font-size: var(--font-small);
  flex-shrink: 0;
}
.view-article-btn:hover {
  color: var(--color-primary);
}
.generation-empty {
  padding: 24px 16px;
}
.generation-list {
  display: flex;
  flex-direction: column;
}
.generation-item {
  display: flex;
  align-items: stretch;
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
.generation-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: var(--space-sm);
}
.generation-title {
  flex: 1;
  min-width: 0;
  font-size: var(--font-body);
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: left;
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
  justify-content: flex-start;
  gap: 4px;
  text-align: left;
}
.generation-meta .generation-time-icon {
  font-size: 12px;
  color: var(--color-text-placeholder);
}
.dot-separator {
  opacity: 0.6;
}
.generation-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: var(--font-caption);
  font-weight: 600;
  flex-shrink: 0;
}
.generation-status::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.generation-status.generating {
  color: var(--color-warning);
  background: #fff7e6;
}
.generation-status.generating::before {
  background: var(--color-warning);
}
.generation-status.completed {
  color: var(--color-success);
  background: #e6f7ed;
}
.generation-status.completed::before {
  background: var(--color-success);
}
.generation-status.failed {
  color: var(--color-error);
  background: #fff1f0;
}
.generation-status.failed::before {
  background: var(--color-error);
}
.generation-actions {
  align-self: flex-end;
}
.generation-progress {
  margin-top: 6px;
}
.generation-progress :deep(.ant-progress-bg) {
  background: var(--color-primary) !important;
}

/* 发布建议弹窗 */
.publish-guide {
  padding: 4px;
}
.publish-plan-spin :deep(.ant-spin-text) {
  color: var(--color-primary);
}
.publish-plan-spin :deep(.ant-spin-dot-item) {
  background-color: var(--color-primary);
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
.publish-guide-doc-link {
  font-size: var(--font-small);
  color: var(--color-primary);
}
.publish-guide-doc-link:hover {
  color: var(--color-primary-hover);
}

.publish-guide-coldstart-duration {
  font-size: var(--font-body);
  color: var(--color-primary);
  font-weight: 600;
  margin-bottom: 8px;
}

.publish-guide-coldstart-list {
  margin: 0;
  padding-left: 18px;
  font-size: var(--font-small);
  color: var(--color-text-regular);
  line-height: 1.7;
}

.publish-guide-coldstart-list li {
  margin-bottom: 4px;
}

.publish-guide-coldstart-share {
  margin-top: 10px;
  padding: 10px 12px;
  background: var(--color-primary-bg);
  border-radius: var(--radius-md);
  font-size: var(--font-small);
  color: var(--color-text-regular);
  line-height: 1.5;
}

:global(.publish-modal .ant-btn-primary),
:global(.reposts-modal .ant-btn-primary),
:global(.plan-modal .ant-btn-primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  border-radius: var(--radius-lg);
}
:global(.publish-modal .ant-btn-primary:hover),
:global(.publish-modal .ant-btn-primary:focus),
:global(.reposts-modal .ant-btn-primary:hover),
:global(.reposts-modal .ant-btn-primary:focus),
:global(.plan-modal .ant-btn-primary:hover),
:global(.plan-modal .ant-btn-primary:focus) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

/* 一文多发方案弹窗 */
.reposts-modal-spin :deep(.ant-spin-text) {
  color: var(--color-primary);
}
.reposts-modal-spin :deep(.ant-spin-dot-item) {
  background-color: var(--color-primary);
}
.reposts-modal-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.reposts-modal-card {
  padding: var(--space-md);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
}
.reposts-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  margin-bottom: 10px;
}
.reposts-modal-platform {
  font-weight: 600;
  color: var(--color-text-primary);
  font-size: var(--font-body);
}
.reposts-modal-time {
  flex-shrink: 0;
  font-size: var(--font-small);
  color: var(--color-primary);
  font-weight: 500;
}
.reposts-modal-field {
  display: flex;
  gap: var(--space-sm);
  font-size: var(--font-small);
  line-height: 1.6;
}
.reposts-modal-field + .reposts-modal-field {
  margin-top: 6px;
}
.reposts-modal-label {
  flex-shrink: 0;
  color: var(--color-text-secondary);
  width: 60px;
}
.reposts-modal-value {
  flex: 1;
  color: var(--color-text-regular);
}
.reposts-modal-tags {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.reposts-modal-tag {
  padding: 2px 8px;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  border-radius: 10px;
  font-size: 12px;
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
.weekly-data-remove {
  flex-shrink: 0;
  padding: 0 8px;
}
.weekly-data-reads :deep(.ant-input-number-handler-wrap) {
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}
.weekly-data-actions {
  display: flex;
  justify-content: space-between;
  gap: var(--space-sm);
}
.weekly-data-actions .ant-btn-dashed {
  border-radius: var(--radius-md);
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
.withdraw-go-link {
  color: #1677ff;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.withdraw-go-link:hover {
  color: #4096ff;
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
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.withdraw-plan-icon-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 2px;
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
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.withdraw-plan-btn:hover,
.withdraw-plan-btn:focus {
  background: var(--color-primary-hover, #e61e3a);
  border-color: var(--color-primary-hover, #e61e3a);
  color: #fff;
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
    padding: 0 0 calc(20px + env(safe-area-inset-bottom));
    background:
      radial-gradient(140% 90% at 50% -8%, var(--color-primary-bg) 0%, transparent 55%),
      var(--color-bg-page);
  }

  .top-row,
  .bottom-row,
  .left-column {
    gap: 14px;
  }
  .top-row {
    margin-bottom: 14px;
  }

  /* 卡片：更圆润、更柔和的阴影，水平居中 */
  .wb-card {
    border-radius: 18px;
    background: var(--color-bg-card);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(0, 0, 0, 0.03);
    margin: 0 12px;
  }
  .wb-card :deep(.ant-card-body),
  .wb-card :deep(.ant-card-head) {
    padding-left: 16px;
    padding-right: 16px;
  }
  .wb-card :deep(.ant-card-head) {
    min-height: 50px;
    border-bottom: 1px solid #f5f5f5;
  }
  .wb-card :deep(.ant-card-head-title) {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: -0.2px;
  }

  /* 欢迎区：左侧标题+tag+副标题，右侧人物图，左侧甜甜圈装饰 */
  .welcome-card {
    position: relative;
    overflow: hidden;
    min-height: 170px;
    background: linear-gradient(135deg, #ffffff 0%, #FFEBEF 100%);
    border: 1px solid rgba(255, 36, 66, 0.06);
    box-shadow: 0 8px 28px rgba(255, 36, 66, 0.08);
    border-radius: 20px;
  }
  .welcome-card::before,
  .welcome-card::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    border: 18px solid rgba(255, 36, 66, 0.08);
    pointer-events: none;
  }
  .welcome-card::before {
    width: 120px;
    height: 120px;
    bottom: -30px;
    left: -30px;
  }
  .welcome-card::after {
    width: 80px;
    height: 80px;
    top: -20px;
    left: 60px;
    border-width: 14px;
    border-color: rgba(255, 36, 66, 0.06);
  }
  .welcome-card :deep(.ant-card-body) {
    position: relative;
    padding: 24px 18px;
  }
  .welcome-body {
    flex-direction: row;
    align-items: center;
    gap: 12px;
    text-align: left;
  }
  .ai-avatar {
    display: none;
  }
  .dialogue-bubble {
    background: transparent;
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
    border-radius: 0;
    padding: 0;
    gap: 8px;
    box-shadow: none;
    border: none;
  }
  .dialogue-title {
    display: none;
  }
  .mobile-dialogue-title {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  .mobile-title-main {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text-primary);
    line-height: 1.3;
  }
  .mobile-title-tag {
    font-size: 12px;
    font-weight: 500;
    color: #fff;
    background: var(--color-primary);
    padding: 3px 9px;
    border-radius: var(--radius-full);
    line-height: 1.4;
  }
  .dialogue-greeting {
    font-size: 13px;
    color: var(--color-text-secondary);
    line-height: 1.55;
  }
  .plan-link {
    color: var(--color-primary);
    font-weight: 600;
  }
  .welcome-mascot {
    display: block;
    width: 96px;
    height: 96px;
    flex-shrink: 0;
  }
  .welcome-mascot img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  /* 欢迎卡片：通栏无圆角 */
  .welcome-card.wb-card {
    margin: 0;
    border-radius: 0;
  }

  /* 手机端功能栏 */
  .feature-bar {
    display: flex;
    flex-direction: column;
    gap: 14px;
    margin: 0 12px;
    padding: 14px;
    background: var(--color-bg-card);
    border-radius: 18px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(0, 0, 0, 0.03);
  }
  .feature-top-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .feature-large-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    background: linear-gradient(135deg, #ffffff 0%, var(--color-primary-bg) 100%);
    border-radius: 14px;
    border: 1px solid rgba(255, 36, 66, 0.06);
    cursor: pointer;
  }
  .feature-large-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .feature-large-title {
    font-size: 15px;
    font-weight: 700;
    color: var(--color-text-primary);
  }
  .feature-large-desc {
    font-size: 12px;
    color: var(--color-text-secondary);
  }
  .feature-large-icon {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--color-primary-bg);
    color: var(--color-primary);
    border-radius: 16px;
    font-size: 20px;
    overflow: hidden;
  }
  .feature-large-icon img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    padding: 3px;
  }
  .feature-grid {
    display: flex;
    justify-content: space-between;
  }
  .feature-grid-item {
    flex: 0 0 auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }
  .feature-grid-label {
    font-size: 12px;
    color: var(--color-text-primary);
    white-space: nowrap;
  }
  .feature-grid-icon {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--color-primary-bg);
    color: var(--color-primary);
    border-radius: 16px;
    font-size: 22px;
    overflow: hidden;
  }
  .feature-grid-icon img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    padding: 6px;
  }
  .feature-grid-label {
    font-size: 12px;
    color: var(--color-text-primary);
  }

  /* 隐藏个人信息与账户 */
  .welcome-info,
  .welcome-balance {
    display: none;
  }

  /* 主操作区 */
  .create-section {
    flex-direction: column;
    gap: 10px;
    margin: 0 12px;
  }
  .desktop-create-btn {
    display: none;
  }
  .mobile-create-btn {
    display: block;
    position: relative;
    width: 100%;
    height: 54px;
    border-radius: 999px;
    background: linear-gradient(135deg, #ff2442 0%, #ff5c7c 35%, #ff9eb0 70%, #fff0f3 100%);
    box-shadow: 0 6px 18px rgba(255, 36, 66, 0.28), inset 0 1px 1px rgba(255, 255, 255, 0.35);
    cursor: pointer;
    user-select: none;
    -webkit-tap-highlight-color: transparent;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
    overflow: hidden;
  }
  .mobile-create-btn:active {
    transform: scale(0.98);
    box-shadow: 0 3px 10px rgba(255, 36, 66, 0.22), inset 0 1px 1px rgba(255, 255, 255, 0.35);
  }
  .mobile-create-btn::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 52%;
    background: linear-gradient(to bottom, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.05));
    border-radius: 999px 999px 0 0;
    pointer-events: none;
  }
  .mobile-create-btn::after {
    content: '';
    position: absolute;
    top: 18%;
    left: -20%;
    width: 80%;
    height: 35%;
    background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.55), transparent);
    transform: rotate(-45deg);
    pointer-events: none;
  }
  .mobile-create-text {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 16px;
    font-weight: 700;
    color: #ffffff;
    text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
    white-space: nowrap;
    pointer-events: none;
  }
  .create-main-btn.ant-btn-primary {
    height: 54px;
    font-size: 16px;
    font-weight: 700;
    border-radius: 16px;
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
    border: none;
    box-shadow: 0 8px 22px rgba(255, 36, 66, 0.28);
    transition: transform 0.15s ease, box-shadow 0.15s ease;
  }
  .create-main-btn.ant-btn-primary:active {
    transform: translateY(1px);
    box-shadow: 0 4px 12px rgba(255, 36, 66, 0.22);
  }
  .create-main-btn :deep(.anticon) {
    font-size: 20px;
  }

  /* 运营方案：更高级的卡片 */
  .plan-section {
    margin: 0 12px;
  }
  .plan-section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    padding: 0 16px;
  }
  .plan-section-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--color-text-primary);
  }
  .plan-card {
    max-height: none;
    margin: 0;
    background: #ffffff;
    border-radius: 20px;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.05);
    border: none;
    overflow: hidden;
  }
  .plan-card :deep(.ant-card-head) {
    display: none;
  }
  .plan-card :deep(.ant-card-body) {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .plan-content {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .plan-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
  .plan-grid .plan-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 14px;
    background: linear-gradient(135deg, #ffffff 0%, var(--color-primary-bg) 100%);
    border: 1px solid rgba(255, 36, 66, 0.06);
    border-radius: 14px;
    font-size: 14px;
    line-height: 1.4;
  }
  .plan-grid .plan-row:first-child {
    grid-column: 1 / -1;
    position: relative;
    padding: 14px 90px 14px 16px;
    background: linear-gradient(135deg, var(--color-primary-bg) 0%, #ffffff 100%);
    border: 1px solid rgba(255, 36, 66, 0.08);
  }
  .plan-grid .plan-row:first-child .plan-platform-icon {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    width: 72px;
    height: 72px;
  }
  .plan-grid .plan-row:first-child .plan-platform-text {
    font-size: 20px;
    font-weight: 700;
  }
  .plan-label {
    font-size: 11px;
    color: #8c8c8c;
    flex-shrink: 0;
  }
  .plan-value {
    color: var(--color-text-primary);
    font-weight: 600;
    text-align: left;
  }
  .plan-platform {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--color-primary);
  }
  .plan-platform-icon {
    width: 40px;
    height: 40px;
    object-fit: contain;
    flex-shrink: 0;
  }
  .plan-pillars-inline {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 14px;
    background: linear-gradient(135deg, #ffffff 0%, var(--color-primary-bg) 100%);
    border: 1px solid rgba(255, 36, 66, 0.06);
    border-radius: 14px;
    font-size: 14px;
  }
  .plan-pillar-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .plan-pillar-tags :deep(.ant-tag) {
    background: #ffffff;
    border: 1px solid var(--color-primary-light);
    color: var(--color-primary);
    font-weight: 500;
    border-radius: 999px;
    padding: 4px 10px;
    font-size: 12px;
    margin: 0;
  }
  .plan-btn {
    height: 30px;
    padding: 0 12px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 500;
    color: var(--color-text-secondary);
    background: transparent;
    border: 1px solid var(--color-border-light);
  }
  .plan-btn:hover {
    color: var(--color-primary);
    border-color: var(--color-primary-light);
    background: var(--color-primary-bg);
  }
  .plan-empty {
    align-items: center;
    text-align: center;
    gap: 10px;
    padding: 32px 20px;
    background: var(--color-bg-page);
    border-radius: 14px;
    border: none;
  }
  .plan-empty-icon {
    width: 96px;
    height: 96px;
    object-fit: contain;
    margin-bottom: 4px;
  }
  .plan-empty-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
  .plan-empty-desc {
    font-size: 12px;
    line-height: 1.65;
    max-width: none;
    color: var(--color-text-secondary);
  }

  /* 手机端隐藏快捷操作和热门活动 */
  .shortcut-card,
  .activity-card-wrapper {
    display: none;
  }

  /* 生成记录：与运营方案卡片风格统一 */
  .generation-card {
    background: transparent;
    border: none;
    box-shadow: none;
  }
  .generation-card :deep(.ant-card-head) {
    background: transparent;
    border-bottom: none;
  }
  .generation-card :deep(.ant-card-body) {
    padding: 0;
    background: transparent;
    height: auto;
  }
  .generation-empty {
    padding: 32px 16px;
  }
  .generation-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .generation-item {
    position: relative;
    flex-direction: column;
    gap: 6px;
    padding: 10px 16px;
    margin: 0;
    background: #ffffff;
    border: 1px solid rgba(255, 36, 66, 0.06);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
    transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
    cursor: pointer;
    overflow: hidden;
  }
  .generation-item.completed {
    background: linear-gradient(135deg, #ffffff 0%, var(--color-primary-bg) 100%);
  }
  .generation-item.generating {
    background: #fffbe6;
  }
  .generation-item.failed {
    background: #fff1f0;
  }
  .generation-item:last-child {
    border-bottom: 1px solid rgba(255, 36, 66, 0.06);
  }
  .generation-item:active {
    transform: scale(0.995);
  }
  .generation-item:hover {
    margin: 0;
    padding: 10px 16px;
    background: #ffffff;
    border-color: rgba(255, 36, 66, 0.12);
    box-shadow: 0 4px 14px rgba(255, 36, 66, 0.08);
  }
  .generation-item.completed:hover {
    background: linear-gradient(135deg, #ffffff 0%, var(--color-primary-bg) 100%);
  }
  .generation-item.generating:hover {
    background: #fffbe6;
  }
  .generation-item.failed:hover {
    background: #fff1f0;
  }
  .generation-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .generation-header {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 8px;
  }
  .generation-title {
    flex: 1;
    min-width: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
    text-align: left;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .generation-meta {
    font-size: 12px;
    color: var(--color-text-secondary);
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 4px;
    text-align: left;
  }
  .generation-meta .generation-time-icon {
    font-size: 11px;
    color: var(--color-text-placeholder);
  }
  .generation-status {
    padding: 3px 9px;
    font-size: 11px;
  }
  .generation-progress {
    margin-top: 4px;
  }
  .generation-actions {
    display: flex;
    justify-content: flex-end;
    gap: 6px;
    padding-top: 4px;
  }
  .repost-btn.ant-btn-link,
  .view-article-btn.ant-btn-link {
    height: 26px;
    padding: 0 8px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    line-height: 26px;
    border: none;
  }
  .repost-btn.ant-btn-link {
    background: #ffffff;
    color: var(--color-primary);
    border: 1px solid var(--color-primary-light);
  }
  .view-article-btn.ant-btn-link {
    background: #ffffff;
    color: var(--color-text-secondary);
    border: 1px solid rgba(0, 0, 0, 0.06);
  }

  /* 弹框统一宽度 */
  :global(.publish-modal .ant-modal),
  :global(.reposts-modal .ant-modal),
  :global(.account-modal .ant-modal),
  :global(.weekly-data-modal .ant-modal),
  :global(.withdraw-modal .ant-modal),
  :global(.plan-modal .ant-modal),
  :global(.adjust-plan-confirm-modal .ant-modal) {
    width: calc(100vw - 32px) !important;
    max-width: 100%;
    margin: 0 auto;
  }

  /* 本周数据弹窗 */
  .weekly-data-item {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-sm);
  }
  .weekly-data-reads {
    width: 100%;
  }
  .weekly-data-actions {
    flex-direction: column;
    gap: var(--space-sm);
  }
  .weekly-data-actions .ant-btn-dashed,
  .weekly-data-actions .ant-btn-primary {
    height: 42px;
    border-radius: 12px;
    font-weight: 600;
  }
}

@media (max-width: 480px) {
  /* 快捷操作在极窄屏下 3 列纵向排列（已隐藏，保留兼容） */
  .shortcut-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  .shortcut-item {
    flex-direction: column;
    justify-content: center;
    padding: 10px 4px;
    gap: 6px;
  }
  .shortcut-icon-wrap {
    width: 32px;
    height: 32px;
  }
  .shortcut-icon {
    font-size: 16px;
  }
  .shortcut-label {
    font-size: 12px;
    white-space: normal;
    text-align: center;
    line-height: 1.3;
  }
}


.guide-doc-link {
  margin-top: var(--space-sm);
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}

.guide-doc-link a {
  color: var(--color-info, #1989fa);
  text-decoration: underline;
}

.guide-doc-link a:hover {
  color: #1478d2;
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

.recommend-row {
  margin-top: var(--space-sm);
}

.recommend-btn {
  border-radius: var(--radius-md);
}

</style>

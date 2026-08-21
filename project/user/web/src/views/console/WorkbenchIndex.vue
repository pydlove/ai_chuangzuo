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
                  <span v-if="!hasPlan" class="todo-text">
                    我可以帮您定制专属您的自媒体运营方案，<a href="javascript:;" class="plan-link" @click="router.push('/console/onboarding')">去制定</a>
                  </span>
                  <span v-else-if="todayDone" class="done-text">今日创作目标已达成，继续保持！</span>
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
            开始今日创作{{ quotaText }}
          </a-button>
          <a-button size="large" class="weekly-data-btn" @click="weeklyDataVisible = true">
            <BarChartOutlined />
            本周数据
          </a-button>
        </div>
      </div>

      <a-card class="wb-card plan-card" :bordered="false">
        <template #title>
          <span>运营方案</span>
        </template>
        <template #extra>
          <a-button
            v-if="hasPlan"
            size="small"
            class="plan-btn"
            @click="openAdjustPlanConfirm"
          >
            调整方案
          </a-button>
        </template>
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
        </div>
        <div v-else class="plan-empty">
          <div class="plan-empty-title">您还没有还没有专属运营方案，</div>
          <div class="plan-empty-desc">
            您的专属顾问小爱会为您量身定制一套专属的自媒体运营方案，陪您一起经营您的自媒体账号，快去行动吧，
            <a href="javascript:;" class="plan-link" @click="router.push('/console/onboarding')">立即制定</a>
          </div>
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
              <a-button
                v-if="record.status === 'completed'"
                type="link"
                size="small"
                class="repost-btn"
                @click.stop="openRepostsPlan(record)"
              >
                一文多发
              </a-button>
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
                <component :is="item.icon" class="activity-icon-svg" />
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
              <a-button type="primary" @click="router.push('/console/onboarding')">去制定方案</a-button>
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
              <a-button type="primary" @click="router.push('/console/onboarding')">去制定方案</a-button>
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
              </div>
            </div>
          </template>
          <template v-else>
            <a-empty description="暂无多平台发布方案" />
          </template>
        </a-spin>
      </div>
    </a-modal>

    <!-- 查看文章弹窗 -->
    <a-modal
      :open="articleViewModalVisible"
      title="查看文章"
      width="760px"
      :footer="null"
      class="article-view-modal"
      @cancel="articleViewModalVisible = false"
    >
      <div class="article-view-spin">
        <a-spin :spinning="articleViewLoading" tip="正在加载文章…">
          <div v-if="currentViewArticle" class="article-view-content">
            <h2 class="article-view-title">{{ currentViewArticle.title }}</h2>
            <div class="article-view-meta">
              <span>{{ currentViewArticle.platform }}</span>
              <span>·</span>
              <span>{{ currentViewArticle.createdAt }}</span>
            </div>
            <div class="article-view-body" v-html="formatArticleBody(currentViewArticle.body)"></div>
          </div>
          <div v-else-if="!articleViewLoading" class="article-view-empty">
            <a-empty description="暂无文章内容" />
          </div>
        </a-spin>
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
                @click="selectSuggestion(s)"
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
            <a href="https://fxbi16ko1px.feishu.cn/docx/BXVqdp4XwodssXxlfECcUfODnib?from=from_copylink" target="_blank" rel="noopener noreferrer">爱创作新手教程</a>
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
          <div class="withdraw-plan-icon" :class="task.iconClass">
            <component :is="task.icon" class="withdraw-plan-icon-svg" />
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

    <CreateFlowModal v-model:visible="createFlowVisible" :plan="plan" @success="onCreateStart" />
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
            <div class="choice-title">按小爱推荐的方式创作</div>
            <div class="choice-desc">小爱针对你的运营方案，推荐选题、观点、字数等进行创作。</div>
            <div class="choice-tags">
              <span class="choice-tag">小爱推荐选题</span>
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
        <a-button type="primary" class="continue-btn" @click="confirmAdjustPlan">继续</a-button>
        <a-button @click="adjustPlanConfirmVisible = false">取消</a-button>
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
  CheckCircleOutlined,
  InfoCircleOutlined,
  FileTextOutlined,
  ShopOutlined,
  FireOutlined,
  GiftOutlined,
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
  TeamOutlined,
  CompassOutlined,
  QuestionCircleOutlined,
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import CreateFlowModal from './create/CreateFlowModal.vue'
import FreeCreateModal from './create/FreeCreateModal.vue'
import { fetchCurrentPlan, generatePublishPlan } from '@/api/selfMediaPlan.js'
import { checkNickname, recommendNickname } from '@/api/accountCheck.js'
import { getMyProfile } from '@/api/user.js'
import { getAccountSummary } from '@/api/earnings.js'
import { getMyMembership } from '@/api/membership.js'
import { listGenerationTasks } from '@/api/generation.js'
import { getWeeklyArticles, saveWeeklyArticles } from '@/api/workbench.js'
import { getArticle } from '@/api/article.js'
import { useWithdraw } from '@/composables/useWithdraw.js'
import { useBenefits } from '@/composables/useBenefits.js'

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
  withdrawThreshold: 1000
})

const { withdrawRecords: rawWithdrawRecords, loadWithdrawals } = useWithdraw()
const { benefits, loadBenefits } = useBenefits()

const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)
const quotaText = computed(() => {
  if (quotaTotal.value === 0) return ''
  return `（剩余 ${quotaRemaining.value} 次）`
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

function openAdjustPlanConfirm() {
  adjustPlanConfirmVisible.value = true
}

function confirmAdjustPlan() {
  adjustPlanConfirmVisible.value = false
  router.push('/console/onboarding?reset=1')
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
})

const createFlowVisible = ref(false)
const createChoiceVisible = ref(false)
const freeCreateVisible = ref(false)
const accountModalVisible = ref(false)
const weeklyDataVisible = ref(false)
const weeklyLoading = ref(false)
const withdrawModalVisible = ref(false)
const adjustPlanConfirmVisible = ref(false)

const withdrawTasks = [
  { label: '参加 2 个约稿任务', reward: 40, path: '/console/commission', icon: ShopOutlined, iconClass: 'commission' },
  { label: '发布 1 个提示词', reward: 20, path: '/console/skill-market', icon: BulbOutlined, iconClass: 'skill' },
  { label: '邀请 1 个好友', reward: 20, path: '/console/invite', icon: TeamOutlined, iconClass: 'invite' }
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

watch(accountModalVisible, (visible) => {
  if (visible) {
    nicknameCheckLimitReached.value = false
    restoreAccountModalState()
  }
})

const accountSuggestions = ref([])
const recommendOptions = ref([])
const recommending = ref(false)
const isRestoring = ref(false)

const ACCOUNT_CHECK_LAST_KEY = 'aichuangzuo_account_check_last'
const ACCOUNT_RECOMMEND_LAST_KEY = 'aichuangzuo_account_recommend_last'

const shortcuts = [
  { label: '账号名检测', icon: SafetyCertificateOutlined, action: () => { accountModalVisible.value = true } },
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

const activities = [
  {
    label: '幸运抽奖',
    desc: '每日免费抽奖，创作币、会员时长、限定模板等好礼送不停',
    path: '/console/lottery',
    icon: GiftOutlined,
    iconClass: 'lottery'
  },
  {
    label: '约稿任务',
    desc: '精选品牌与创作者对接，完成任务即可获得丰厚创作币奖励',
    path: '/console/commission',
    icon: ShopOutlined,
    iconClass: 'commission'
  },
  {
    label: '提示词市场',
    desc: '上传原创提示词，被他人使用即可持续获得收益分成',
    path: '/console/skill-market',
    icon: BulbOutlined,
    iconClass: 'skill'
  },
  {
    label: '邀请有礼',
    desc: '邀请好友加入，双方均可获得创作币与会员权益奖励',
    path: '/console/invite',
    icon: TeamOutlined,
    iconClass: 'invite'
  },
  {
    label: '收益排行榜',
    desc: '实时查看平台创作者收益榜单，学习头部创作者的变现路径',
    path: '/console/leaderboard',
    icon: TrophyOutlined,
    iconClass: 'rank'
  },
  {
    label: '创作学院',
    desc: '从选题、标题到爆款结构，系统化课程帮你快速提升创作能力',
    path: '/console/learn',
    icon: BookOutlined,
    iconClass: 'learn'
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

const articleViewModalVisible = ref(false)
const currentViewArticle = ref(null)
const articleViewLoading = ref(false)

const sendMethod = computed(() => {
  return {
    method: '手动复制到各平台发布',
    docLink: 'https://fxbi16ko1px.feishu.cn/docx/BXVqdp4XwodssXxlfECcUfODnib?from=from_copylink',
    docText: '查看《手动发布操作文档》'
  }
})

async function loadPublishPlan(record) {
  publishPlan.value = null
  if (!hasPlan.value) return
  const title = record?.title?.trim() || `关于${plan.niche || '运营方向'}的内容`
  const mainPlatform = platformNameMap[record?.platform] || record?.platform || plan.platform
  if (!title || !mainPlatform) return
  publishPlanLoading.value = true
  try {
    const res = await generatePublishPlan({ articleTitle: title, mainPlatform })
    publishPlan.value = res?.data || null
  } catch (err) {
    message.error(err?.message || '生成发布计划失败，请重试')
  } finally {
    publishPlanLoading.value = false
  }
}

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

function onCreateStart(task) {
  setTodayDone()
  loadGenerationRecords()
  if (task?.id) {
    message.success('文章生成任务已创建')
  }
}

function onFreeCreateSuccess(task) {
  setTodayDone()
  loadGenerationRecords()
  if (task?.id) {
    message.success('文章生成任务已创建')
  }
}

function openPublishGuide(record) {
  currentPublishRecord.value = record
  loadPublishPlan(record)
  publishModalVisible.value = true
}

function openHowToPublish() {
  const completed = recentRecords.value.find(r => r.status === 'completed')
  const record = completed || recentRecords.value[0] || null
  currentPublishRecord.value = record
  loadPublishPlan(record)
  publishModalVisible.value = true
}

async function openRepostsPlan(record) {
  if (!record) return
  currentRepostRecord.value = record
  currentRepostPlan.value = null
  repostsModalVisible.value = true
  if (!hasPlan.value) return
  const title = record.title?.trim() || `关于${plan.niche || '运营方向'}的内容`
  const mainPlatform = platformNameMap[record.platform] || record.platform || plan.platform
  if (!title || !mainPlatform) return
  repostsLoading.value = true
  try {
    const res = await generatePublishPlan({ articleTitle: title, mainPlatform })
    currentRepostPlan.value = res?.data || null
  } catch (err) {
    message.error(err?.message || '生成多平台方案失败，请重试')
  } finally {
    repostsLoading.value = false
  }
}

async function openArticleView(record) {
  if (!record?.bizNo) return
  currentViewArticle.value = null
  articleViewModalVisible.value = true
  articleViewLoading.value = true
  try {
    const data = await getArticle(record.bizNo)
    if (data) {
      currentViewArticle.value = {
        title: data.title || '未命名文章',
        body: data.body || '',
        platform: data.platformName || record.platform || plan.platform || '',
        createdAt: record.createdAt || ''
      }
    }
  } catch (err) {
    message.error(err?.message || '加载文章失败，请重试')
  } finally {
    articleViewLoading.value = false
  }
}

function formatArticleBody(body) {
  if (!body) return ''
  return body
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

function statusText(status) {
  const map = { pending: '排队中', generating: '生成中', completed: '已完成', failed: '生成失败' }
  return map[status] || status
}

function validateAccountName() {
  if (!hasPlan.value) {
    message.info('请先制定自媒体运营方案，再检测账号名称')
    accountModalVisible.value = false
    router.push('/console/onboarding')
    return
  }
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

function selectSuggestion(s) {
  accountInfo.name = s.nickname || ''
  accountValidation.value = ''
  accountFit.value = null
  accountReason.value = ''
}

async function copyText(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch {
    const input = document.createElement('textarea')
    input.value = text
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    message.success('已复制')
  }
}

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
  if (!hasPlan.value) {
    message.info('请先制定自媒体运营方案，再获取昵称推荐')
    accountModalVisible.value = false
    router.push('/console/onboarding')
    return
  }
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
  margin-top: 18px;
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
  max-height: 220px;
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
  overflow: hidden;
  font-size: 28px;
}
.activity-icon.lottery {
  background: linear-gradient(135deg, #fff5e6, #ffe0b3);
  color: #fa8c16;
}
.activity-icon.commission {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}
.activity-icon.skill {
  background: #f0f5ff;
  color: #2f54eb;
}
.activity-icon.invite {
  background: #fff0f3;
  color: #eb2f96;
}
.activity-icon.rank {
  background: #fffbe6;
  color: #faad14;
}
.activity-icon.learn {
  background: #f6ffed;
  color: #52c41a;
}
.activity-icon-svg {
  display: block;
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

/* 查看文章弹窗 */
.article-view-spin :deep(.ant-spin-text) {
  color: var(--color-primary);
}
.article-view-spin :deep(.ant-spin-dot-item) {
  background-color: var(--color-primary);
}
.article-view-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 4px;
}
.article-view-title {
  font-size: var(--font-h2);
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
  line-height: 1.4;
}
.article-view-meta {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--color-border-light);
}
.article-view-body {
  font-size: var(--font-body);
  line-height: 1.8;
  color: var(--color-text-regular);
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
  font-size: 22px;
}
.withdraw-plan-icon.commission {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}
.withdraw-plan-icon.skill {
  background: #f0f5ff;
  color: #2f54eb;
}
.withdraw-plan-icon.invite {
  background: #fff0f3;
  color: #eb2f96;
}
.withdraw-plan-icon-svg {
  display: block;
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

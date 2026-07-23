<template>
  <div class="console-layout">
    <!-- 侧边栏 -->
    <aside class="console-sidebar">
      <div class="console-sidebar-brand">
        <img
          :src="logoUrl"
          alt="爱创作"
          class="brand-logo"
        />
        <span class="brand-name">爱创作</span>
      </div>
      <nav class="console-sidebar-nav">
        <template v-for="item in navItems" :key="item.path || item.label">
          <div v-if="item.children" class="console-sidebar-group">
            <div class="console-sidebar-group-title">
              <component :is="item.icon" class="nav-icon" />
              <span>{{ item.label }}</span>
            </div>
            <router-link
              v-for="sub in item.children"
              :key="sub.path"
              :to="sub.path"
              class="console-sidebar-item sub-item"
              :class="{ active: isActive(sub.path) }"
            >
              <span class="nav-label">{{ sub.label }}</span>
            </router-link>
          </div>
          <router-link
            v-else
            :to="item.path"
            class="console-sidebar-item"
            :class="{ active: isActive(item.path) }"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ item.label }}</span>
          </router-link>
        </template>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <div class="console-main">
      <!-- 顶部栏 -->
      <header class="console-header">
        <div class="header-left">
        </div>

        <div class="header-right">
          <!-- 邀请有礼弹框 -->
          <a-modal
            v-model:open="inviteVisible"
            :footer="null"
            :width="640"
            centered
            class="invite-modal"
            :mask-style="{ background: 'transparent' }"
            @cancel="handleInviteClose"
          >
            <div class="invite-panel">
              <div class="invite-header">
                <span class="invite-title">🎁 邀请有礼</span>
                <a-tooltip title="点击复制 ID">
                  <button class="invite-user-id" @click="copyUserId">
                    <span class="invite-user-id-label">我的 ID</span>
                    <b class="invite-user-id-value">{{ userId }}</b>
                    <svg class="invite-user-id-copy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                    </svg>
                  </button>
                </a-tooltip>
              </div>

              <div class="invite-content">
                <!-- 活动规则 -->
                <div class="invite-rules">
                  <div class="invite-rules-header">
                    <span class="invite-rules-title">📌 活动规则</span>
                    <span class="invite-rules-tag">长期有效</span>
                  </div>
                  <div class="invite-rule-item">
                    <span class="invite-rule-label">🎁 邀请奖励</span>
                    <span class="invite-rule-text">累计邀请 3 人 +3 天会员、5 人 +5 天，超过 5 人后每多 1 人 +2 天专业版会员。</span>
                  </div>
                  <div class="invite-rule-item">
                    <span class="invite-rule-label">💰 创作币返利</span>
                    <span class="invite-rule-text">
                      <u class="invite-rule-underline">推荐新客下单即获得奖励，一次邀请终身享受订单返佣红利</u>。<br>
                      好友首次购买返 10%，续费返 5%（1 创作币 = 1 元，满 100 可提现至支付宝）。
                    </span>
                  </div>
                  <div class="invite-rule-item">
                    <span class="invite-rule-label">🌱 新用户福利</span>
                    <span class="invite-rule-text">新用户通过你的邀请码注册，立刻获得 5 创作币。</span>
                  </div>
                  <button class="invite-rules-detail-btn" @click="openInviteRulesDrawer">
                    <span>查看完整活动规则</span>
                    <span class="invite-rules-detail-arrow">›</span>
                  </button>
                </div>

                <!-- 统计卡片 -->
                <div class="invite-stats">
                  <div class="invite-stat-item">
                    <div class="invite-stat-value">{{ inviteStats.invitedCount }}</div>
                    <div class="invite-stat-label">已邀请</div>
                  </div>
                  <div class="invite-stat-item">
                    <div class="invite-stat-value">{{ inviteStats.membershipDaysEarned }}</div>
                    <div class="invite-stat-label">奖励会员天数</div>
                  </div>
                  <div class="invite-stat-item invite-stat-item-coin">
                    <div class="invite-stat-value">{{ coinBalance }}</div>
                    <div class="invite-stat-label-row">
                      <CoinInfoTooltip>
                        <div class="invite-stat-label invite-stat-label-tooltip">
                          <span>创作币余额</span>
                          <svg class="invite-info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"/>
                            <line x1="12" y1="16" x2="12" y2="12"/>
                            <line x1="12" y1="8" x2="12.01" y2="8"/>
                          </svg>
                        </div>
                      </CoinInfoTooltip>
                      <button class="invite-stat-go-withdraw" @click="goToWithdraw">去提现</button>
                    </div>
                  </div>
                </div>

                <!-- 邀请链接 -->
                <div class="invite-link-card">
                  <div class="invite-code-label">邀请链接</div>
                  <div class="invite-link-value">{{ inviteLink }}</div>
                  <div class="invite-link-actions">
                    <button class="invite-btn invite-btn-secondary" @click="copyInviteLink">复制链接</button>
                    <button class="invite-btn invite-btn-secondary" @click="openPosterModal">下载海报</button>
                  </div>
                </div>

                <!-- 邀请码 -->
                <div class="invite-code-card">
                  <div class="invite-code-box">
                    <div class="invite-code-label">我的邀请码</div>
                    <div class="invite-code-value">{{ inviteCode }}</div>
                  </div>
                  <button class="invite-btn invite-btn-primary" @click="copyInviteCode">复制邀请码</button>
                </div>

                <!-- 阶梯奖励 -->
                <div class="invite-progress-card">
                  <div class="invite-progress-title">阶梯奖励进度</div>
                  <div class="invite-progress-item">
                    <div class="invite-progress-bar">
                      <div class="invite-progress-fill" :style="{ width: Math.min(100, (inviteStats.invitedCount / 3) * 100) + '%' }"></div>
                    </div>
                    <div class="invite-progress-text">
                      {{ inviteStats.invitedCount >= 3 ? '+3 天' : `${inviteStats.invitedCount}/3` }}
                    </div>
                  </div>
                  <div class="invite-progress-item">
                    <div class="invite-progress-bar">
                      <div class="invite-progress-fill" :style="{ width: Math.min(100, (inviteStats.invitedCount / 5) * 100) + '%' }"></div>
                    </div>
                    <div class="invite-progress-text">
                      {{ inviteStats.invitedCount >= 5 ? '+5 天' : `${inviteStats.invitedCount}/5` }}
                    </div>
                  </div>
                  <div class="invite-progress-item">
                    <div class="invite-progress-desc">超过 5 人后，每多 1 人 +2 天专业版会员</div>
                    <div class="invite-progress-text">
                      {{ inviteStats.invitedCount > 5 ? `+${(inviteStats.invitedCount - 5) * 2} 天` : '—' }}
                    </div>
                  </div>
                </div>

                <!-- 邀请记录 -->
                <div class="invite-friend-card">
                  <div class="invite-friend-header">
                    <span class="invite-friend-title">邀请记录</span>
                  </div>
                  <div class="invite-friend-list">
                    <div v-if="inviteStats.friends.length === 0" class="invite-friend-empty">暂无邀请记录，快去分享邀请链接吧～</div>
                    <div v-for="f in inviteStats.friends" :key="f.email" class="invite-friend-item">
                      <div>
                        <span class="invite-friend-email">{{ f.email }}</span>
                      </div>
                      <span :class="['invite-friend-status', f.status]">
                        {{ f.status === 'purchased' ? `已购买 +${f.commission} 币` : '已注册' }}
                      </span>
                    </div>
                  </div>
                  <div class="invite-simulate">
                    <div class="invite-simulate-label">模拟：好友通过邀请链接注册</div>
                    <div class="invite-simulate-row">
                      <input v-model="simulateEmail" class="invite-form-input" placeholder="好友邮箱" />
                      <button class="invite-btn invite-btn-primary" @click="simulateInviteRegister">模拟注册</button>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="inviteAutoOpened" class="invite-modal-auto-footer">
                <a-checkbox v-model:checked="inviteModalDontShow">我已经知道，不再弹出</a-checkbox>
              </div>
            </div>
          </a-modal>

          <!-- 兑换码弹框 -->
          <a-modal
            v-model:open="redeemVisible"
            :footer="null"
            :width="420"
            centered
            class="redeem-modal"
          >
            <div class="redeem-panel">
              <div class="redeem-header">
                <span class="redeem-title">兑换码</span>
                <span class="redeem-subtitle">输入兑换码兑换奖励</span>
              </div>

              <input
                ref="redeemInputRef"
                v-model="redeemCode"
                class="redeem-input"
                placeholder="请输入兑换码"
                maxlength="32"
                @keydown.enter="submitRedeem"
              />

              <div v-if="redeemStatus" :class="['redeem-status', redeemStatus.type]">
                {{ redeemStatus.message }}
              </div>

              <button
                class="invite-btn invite-btn-primary redeem-submit"
                :disabled="!canSubmitRedeem"
                @click="submitRedeem"
              >
                {{ redeemLoading ? '兑换中...' : '立即兑换' }}
              </button>
            </div>
          </a-modal>

          <!-- 邀请活动完整规则抽屉 -->
          <a-drawer
            v-model:open="inviteRulesVisible"
            :width="480"
            :closable="false"
            :footer-style="{ padding: 0 }"
            placement="right"
            class="invite-rules-drawer"
          >
            <div class="invite-rules-detail">
              <div class="invite-rules-detail-header">
                <span class="invite-rules-detail-title">活动规则详情</span>
                <button class="invite-rules-detail-close" @click="inviteRulesVisible = false">×</button>
              </div>

              <div class="invite-rules-detail-body">
                <section class="invite-rules-detail-section">
                  <h4 class="invite-rules-detail-heading">邀请返佣机制</h4>
                  <div class="invite-rules-detail-callout">
                    推荐新客下单即获得奖励，<b>一次邀请终身享受订单返佣红利</b>。
                  </div>
                  <ul class="invite-rules-detail-list">
                    <li>通过专属邀请链接或邀请码建立邀请关系，好友注册成功即生效。</li>
                    <li>累计邀请 3 人奖励 3 天专业版会员；累计邀请 5 人奖励 5 天专业版会员。</li>
                    <li>超过 5 人后，每多邀请 1 人额外奖励 2 天专业版会员，会员天数可累计叠加。</li>
                    <li>好友首次购买会员可获 10% 创作币返佣，续费返佣 5%，返佣以创作币形式即时到账。</li>
                    <li>被邀请的好友通过你的链接首次下单后，该笔订单视为你邀请的返佣订单。</li>
                  </ul>
                </section>

                <section class="invite-rules-detail-section">
                  <h4 class="invite-rules-detail-heading">提现规则</h4>
                  <ul class="invite-rules-detail-list">
                    <li>仅付费邀请用户可申请提现，且需至少邀请 3 位付费好友。</li>
                    <li>创作币余额满 100 即可提现，1 创作币 = 1 元人民币。</li>
                    <li>提现申请提交后约 1 个工作日审核，预计 7 个工作日内到账。</li>
                    <li>目前仅支持支付宝提现，请确保支付宝账号和真实姓名与本人一致。</li>
                    <li>因账号信息错误、账户异常等原因导致的提现失败，平台概不负责。</li>
                  </ul>
                </section>

                <section class="invite-rules-detail-section">
                  <h4 class="invite-rules-detail-heading">声明</h4>
                  <ul class="invite-rules-detail-list">
                    <li>根据业务发展和市场环境变化，活动规则可能调整，会通过官方渠道提前通知。</li>
                    <li>平台保留对违规刷量、虚假邀请、机器注册等行为的处理权。</li>
                    <li>本活动最终解释权归爱创作所有，如有疑问请联系客服。</li>
                  </ul>
                </section>
              </div>
            </div>
          </a-drawer>

          <!-- 提现弹框 -->
          <a-modal
            v-model:open="withdrawVisible"
            :footer="null"
            :width="420"
            centered
            class="withdraw-modal"
          >
            <div class="withdraw-panel">
              <div class="withdraw-title">申请提现</div>
              <div class="withdraw-item">
                <label class="withdraw-label">可提现余额</label>
                <div class="withdraw-balance">{{ coinBalance }} 创作币</div>
              </div>
              <div class="withdraw-item">
                <label class="withdraw-label">提现金额</label>
                <input v-model.number="withdrawAmount" class="withdraw-input" type="number" min="100" :max="coinBalance" placeholder="最低 100" />
                <div class="withdraw-hint">1 创作币 = 1 元，满 100 可提现</div>
              </div>
              <div class="withdraw-item">
                <label class="withdraw-label">支付宝账号</label>
                <input v-model="withdrawAccount" class="withdraw-input" placeholder="支付宝账号" />
              </div>
              <div class="withdraw-item">
                <label class="withdraw-label">真实姓名</label>
                <input v-model="withdrawName" class="withdraw-input" placeholder="真实姓名" />
              </div>
              <div class="withdraw-actions">
                <button class="invite-btn invite-btn-secondary" @click="withdrawVisible = false">取消</button>
                <button class="invite-btn invite-btn-primary" @click="submitWithdraw">提交申请</button>
              </div>
            </div>
          </a-modal>

          <!-- 海报样式选择弹框 -->
          <a-modal
            v-model:open="posterVisible"
            :footer="null"
            :width="840"
            centered
            class="poster-modal"
          >
            <div class="poster-panel">
              <div class="poster-panel-header">
                <span class="poster-panel-title">选择海报样式</span>
                <span class="poster-panel-desc">为不同场景挑一款合适的海报，点击缩略图即可选中</span>
              </div>

              <div class="poster-grid">
                <div
                  v-for="t in posterTemplates"
                  :key="t.id"
                  :class="['poster-card', { active: posterSelectedId === t.id }]"
                  @click="posterSelectedId = t.id"
                >
                  <div class="poster-card-canvas-wrap">
                    <canvas
                      :ref="el => setPosterPreviewRef(t.id, el)"
                      :width="600"
                      :height="800"
                      class="poster-card-canvas"
                    ></canvas>
                  </div>
                  <div class="poster-card-name">{{ t.name }}</div>
                  <div class="poster-card-tag">{{ t.tag }}</div>
                  <div v-if="posterSelectedId === t.id" class="poster-card-check">✓</div>
                </div>
              </div>

              <div class="poster-actions">
                <button class="invite-btn invite-btn-secondary" @click="posterVisible = false">取消</button>
                <button class="invite-btn invite-btn-primary" @click="downloadSelectedPoster">
                  下载{{ selectedTemplate?.name }}
                </button>
              </div>
            </div>
          </a-modal>

          <!-- 邀请有礼按钮 -->
          <a-tooltip title="邀请有礼">
            <button class="console-icon-btn console-invite-btn" @click="openInviteModal">
              <span style="font-size: 16px;">🎁</span>
              <span>邀请有礼</span>
            </button>
          </a-tooltip>

          <!-- 消息弹框 -->
          <a-modal
            v-model:open="notifVisible"
            :footer="null"
            :width="640"
            centered
            class="notif-modal"
          >
            <div class="notif-panel">
              <!-- 面板头部 -->
              <div class="notif-header">
                <span class="notif-title">消息中心</span>
                <button
                  v-if="unreadCount > 0"
                  class="notif-read-all"
                  @click="markAllRead"
                >
                  全部已读
                </button>
              </div>

              <!-- Tab 栏 -->
              <div class="notif-tabs">
                <button
                  v-for="tab in notifTabs"
                  :key="tab.type"
                  :class="['notif-tab', { active: activeTab === tab.type }]"
                  @click="switchTab(tab.type)"
                >
                  {{ tab.label }}
                  <span v-if="getUnreadByType(tab.type) > 0" class="notif-tab-badge">
                    {{ getUnreadByType(tab.type) }}
                  </span>
                </button>
              </div>

              <!-- 消息列表 -->
              <div class="notif-list">
                <div v-if="currentNotifs.length === 0" class="notif-empty">
                  <a-empty :description="`暂无 ${activeTabLabel} 消息`" />
                </div>
                <div
                  v-for="n in currentNotifs"
                  :key="n.id"
                  :class="['notif-item', { unread: !n.read }]"
                  @click="handleNotifClick(n)"
                >
                  <div class="notif-item-dot" v-if="!n.read"></div>
                  <div class="notif-item-body">
                    <div class="notif-item-title">{{ n.title }}</div>
                    <div class="notif-item-summary">{{ n.summary }}</div>
                    <div class="notif-item-time">{{ formatTime(n.createdAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </a-modal>

          <!-- 消息详情弹框（announcement / feature / promotion / membership.subscribed） -->
          <a-modal
            v-model:open="notifDetailVisible"
            :width="640"
            centered
            class="notif-detail-modal"
            :destroy-on-close="true"
          >
            <div v-if="notifDetail" class="notif-detail-panel">
              <div class="notif-detail-type-chip">{{ notifTypeLabel(notifDetail) }}</div>
              <h3 class="notif-detail-title">{{ notifDetail.title }}</h3>
              <div class="notif-detail-time">{{ formatTime(notifDetail.createdAt) }}</div>
              <div class="notif-detail-content">{{ notifDetail.content || notifDetail.summary }}</div>
            </div>
            <template #footer>
              <a-button type="primary" @click="notifDetailVisible = false">我知道了</a-button>
            </template>
          </a-modal>

          <!-- 会员到期续订弹框 -->
          <a-modal
            v-model:open="renewalVisible"
            :width="520"
            centered
            class="renewal-modal"
            :destroy-on-close="true"
          >
            <div class="renewal-panel">
              <h3 class="renewal-title">{{ expiringNotif?.title || '您的会员即将到期' }}</h3>
              <div class="renewal-body">{{ expiringNotif?.content || RENEWAL_FALLBACK_COPY }}</div>
            </div>
            <template #footer>
              <a-button @click="renewalVisible = false">取消</a-button>
              <a-button type="primary" @click="goRenewal">去续订</a-button>
            </template>
          </a-modal>

          <!-- 消息铃铛 -->
          <div class="bell-wrap">
            <a-tooltip title="消息">
              <button class="console-icon-btn bell-btn" @click="notifVisible = true">
                <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/>
                  <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
                </svg>
                <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </button>
            </a-tooltip>
          </div>

          <!-- 教程弹框 -->
          <a-modal
            v-model:open="tutorialVisible"
            :footer="null"
            :width="560"
            centered
            class="tutorial-modal"
          >
            <div class="tutorial-panel">
              <div class="tutorial-header">
                <span class="tutorial-title">教程与帮助</span>
              </div>
              <div class="tutorial-item" @click="handleTutorial('doc')">
                <div class="tutorial-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                    <polyline points="10 9 9 9 8 9"/>
                  </svg>
                </div>
                <div class="tutorial-body">
                  <div class="tutorial-name">帮助 / 文档</div>
                  <div class="tutorial-desc">从基础到专业技巧的快速指南，助你充分利用爱创作的功能。</div>
                </div>
              </div>
              <div class="tutorial-item" @click="tutorialVisible = false; openWechatModal()">
                <div class="tutorial-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                  </svg>
                </div>
                <div class="tutorial-body">
                  <div class="tutorial-name">加入微信交流群</div>
                  <div class="tutorial-desc">一个充满活力的作者网络，提供帮助的渠道，分享创作技巧、经验和最佳实践。</div>
                </div>
              </div>
            </div>
          </a-modal>

          <!-- 教程按钮 -->
          <a-tooltip title="教程">
            <button class="console-icon-btn" @click="tutorialVisible = true">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
              </svg>
            </button>
          </a-tooltip>
          <!-- 反馈弹框(提交 / 我的反馈 tabs) -->
          <a-modal
            v-model:open="feedbackVisible"
            :footer="null"
            :width="640"
            centered
            class="feedback-modal"
            :destroy-on-close="true"
            @cancel="closeFeedbackModal"
          >
            <a-tabs v-model:active-key="feedbackTab" class="feedback-tabs">
              <a-tab-pane key="submit" tab="提交反馈">
                <div class="feedback-panel">
                  <div class="feedback-title">请告诉我们你的想法</div>
                  <div class="feedback-type">
                    <label class="feedback-label">反馈类型</label>
                    <div class="feedback-type-btns">
                      <button
                        v-for="t in feedbackTypes"
                        :key="t"
                        :class="['type-btn', { active: feedbackType === t }]"
                        @click="feedbackType = t"
                      >
                        {{ t }}
                      </button>
                    </div>
                  </div>
                  <div class="feedback-content">
                    <label class="feedback-label">反馈内容</label>
                    <textarea
                      v-model="feedbackContent"
                      class="feedback-textarea"
                      placeholder="请详细描述你的问题或建议..."
                      rows="6"
                      maxlength="2000"
                    ></textarea>
                  </div>
                  <button class="feedback-submit" :disabled="feedbackSubmitting" @click="submitFeedback">
                    {{ feedbackSubmitting ? '提交中...' : '提交反馈' }}
                  </button>
                </div>
              </a-tab-pane>
              <a-tab-pane key="history" tab="我的反馈">
                <div v-if="!historyDetail" class="history-list-pane">
                  <a-segmented
                    v-model:value="historyFilter"
                    :options="historyFilterOptions"
                    block
                    class="history-filter"
                    @change="onHistoryFilterChange"
                  />
                  <a-spin :spinning="historyLoading">
                    <div v-if="historyList.length === 0 && !historyLoading" class="history-empty">
                      <p>还没有反馈记录</p>
                      <a-button type="primary" class="history-go-submit" @click="feedbackTab = 'submit'">去提交</a-button>
                    </div>
                    <ul v-else class="history-list">
                      <li
                        v-for="fb in historyList"
                        :key="fb.id"
                        class="history-item"
                        @click="openHistoryDetail(fb)"
                      >
                        <div class="history-item-row1">
                          <a-tag class="history-type-tag">{{ fb.type }}</a-tag>
                          <a-tag :color="fb.status === 0 ? 'orange' : 'green'">
                            {{ fb.status === 0 ? '待回复' : '已回复' }}
                          </a-tag>
                          <span class="history-item-time">{{ formatTime(fb.createdAt) }}</span>
                        </div>
                        <div class="history-item-content">{{ fb.content }}</div>
                        <div v-if="fb.status === 1 && fb.replyContent" class="history-item-reply-preview">
                          管理员回复:{{ fb.replyContent }}
                        </div>
                      </li>
                    </ul>
                    <a-pagination
                      v-if="historyTotal > historySize"
                      :current="historyPage"
                      :page-size="historySize"
                      :total="historyTotal"
                      simple
                      class="history-pager"
                      @change="onHistoryPageChange"
                    />
                  </a-spin>
                </div>
                <div v-else class="history-detail-pane">
                  <a-button type="link" class="history-back-btn" @click="closeHistoryDetail">
                    ← 返回列表
                  </a-button>
                  <div class="detail-row">
                    <span class="detail-label">类型</span>
                    <a-tag class="history-type-tag">{{ historyDetail.type }}</a-tag>
                    <a-tag :color="historyDetail.status === 0 ? 'orange' : 'green'">
                      {{ historyDetail.status === 0 ? '待回复' : '已回复' }}
                    </a-tag>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">提交时间</span>
                    <span>{{ formatTime(historyDetail.createdAt) }}</span>
                  </div>
                  <div class="detail-row detail-row-stack">
                    <span class="detail-label">反馈内容</span>
                    <pre class="detail-content">{{ historyDetail.content }}</pre>
                  </div>
                  <template v-if="historyDetail.status === 1">
                    <a-divider />
                    <div class="detail-row detail-row-stack">
                      <span class="detail-label">管理员回复</span>
                      <pre class="detail-content detail-content-admin">{{ historyDetail.replyContent }}</pre>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">回复时间</span>
                      <span>{{ formatTime(historyDetail.repliedAt) }}</span>
                    </div>
                  </template>
                  <div v-else class="history-pending-hint">
                    提交成功,我们会尽快处理,谢谢反馈!
                  </div>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-modal>

          <!-- 反馈按钮 -->
          <a-tooltip title="反馈">
            <button class="console-icon-btn" @click="feedbackVisible = true">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </button>
          </a-tooltip>

          <!-- 关于我们弹框 -->
          <a-modal
            v-model:open="aboutVisible"
            :footer="null"
            :width="640"
            centered
            class="about-modal"
          >
            <div class="about-panel">
              <div class="about-header">
                <div class="about-logo">
                  <img src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png" alt="爱创作" />
                </div>
                <div class="about-brand">
                  <div class="about-name">爱创作</div>
                  <div class="about-tagline">创作者灵感旅程中的同行者</div>
                </div>
              </div>
              <div class="about-desc">
                <p>爱创作希望成为创作者灵感旅程中的同行者。我们希望让写作不再被"文笔"所限制，哪怕不擅长表达的人，也能把脑海里的想法顺利写出来。</p>
                <p>AI 在这里不是替代者，而是帮助作者整理思路、激发灵感、拓展想象的辅助工具。</p>
                <p>我们珍惜每一位作者投入在作品里的情绪、时间与热爱，也尊重原创应有的价值。</p>
              </div>
              <div class="about-links">
                <button class="about-link-btn" @click="openTermsModal">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                  用户协议
                </button>
                <button class="about-link-btn" @click="openPrivacyModal">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                  </svg>
                  隐私政策
                </button>
                <button class="about-link-btn" @click="openWechatModal">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                  </svg>
                  关注微信
                </button>
              </div>
              <div class="about-footer">
                © 2026 爱创作 · All Rights Reserved
              </div>
            </div>
          </a-modal>

          <!-- 关于我们按钮 -->
          <a-tooltip title="关于我们">
            <button class="console-icon-btn" @click="aboutVisible = true">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 16v-4"/>
                <path d="M12 8h.01"/>
              </svg>
            </button>
          </a-tooltip>
          <a-tooltip title="官网">
            <a href="http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/index.html" target="_blank" class="console-icon-btn">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/>
                <path d="M2 12h20"/>
              </svg>
            </a>
          </a-tooltip>
          <!-- 主题切换 -->
          <a-tooltip :title="currentTheme === 'light' ? '切换深色主题' : '切换浅色主题'">
            <button class="console-icon-btn" @click="toggleTheme">
              <svg
                v-if="currentTheme === 'light'"
                class="console-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
              <svg
                v-else
                class="console-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="12" cy="12" r="5" />
                <line x1="12" y1="1" x2="12" y2="3" />
                <line x1="12" y1="21" x2="12" y2="23" />
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                <line x1="1" y1="12" x2="3" y2="12" />
                <line x1="21" y1="12" x2="23" y2="12" />
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
              </svg>
            </button>
          </a-tooltip>

          <a-popover
            v-if="hasMembership"
            trigger="hover"
            placement="bottomRight"
            overlay-class-name="membership-benefits-popover"
          >
            <template #content>
              <div class="mbs-pop">
                <div class="mbs-head">
                  <span class="mbs-plan">{{ membershipLevel }}</span>
                  <span v-if="membershipExpiry" class="mbs-expiry">有效期至 {{ membershipExpiry }}</span>
                </div>
                <ul class="mbs-list">
                  <li
                    v-for="b in memberBenefits"
                    :key="b.code"
                    :class="{ off: b.off }"
                  >
                    <span class="mbs-name">{{ b.name }}</span>
                    <span class="mbs-val">{{ b.display }}</span>
                  </li>
                </ul>
                <div class="mbs-foot" @click="router.push('/pricing')">查看会员套餐 ›</div>
              </div>
            </template>
            <span class="console-membership-badge has-membership">
              {{ membershipLevel }}
            </span>
          </a-popover>
          <span
            v-else
            class="console-membership-badge no-membership"
            @click="handleMembershipClick"
          >
            开通会员
          </span>
          <!-- 个人中心下拉 -->
          <a-dropdown
            v-model:open="userCenterVisible"
            :trigger="['click']"
            placement="bottomRight"
          >
            <div class="console-avatar"><UserOutlined /></div>
            <template #overlay>
              <div class="user-center-panel">
                <!-- 会员卡 -->
                <div class="membership-card">
                  <div class="membership-left">
                    <div class="membership-label">当前会员</div>
                    <div class="membership-name">{{ hasMembership ? membershipLevel : '免费版' }}</div>
                    <div class="membership-expiry" v-if="hasMembership">有效期至 {{ membershipExpiry }}</div>
                  </div>
                  <div class="membership-right">
                    <button v-if="!hasMembership" class="membership-btn" @click="router.push('/pricing')">开通</button>
                    <button v-else-if="isNearExpiry" class="membership-btn" @click="router.push('/pricing')">续费</button>
                    <span v-else class="membership-link" @click="router.push('/pricing')">查看套餐 ›</span>
                  </div>
                </div>

                <!-- 账号信息 -->
                <div class="user-section">
                  <div class="user-section-title">账号信息</div>
                  <div class="user-row">
                    <span class="user-row-label">用户ID</span>
                    <span class="user-row-value user-row-copy" @click="copyAccountUserId">
                      {{ userProfile.profile.value?.userId || '—' }}
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                      </svg>
                    </span>
                  </div>
                  <div class="user-row" @click="openProfileModal">
                    <span class="user-row-label">昵称</span>
                    <span class="user-row-value user-row-edit">{{ userProfile.profile.value?.nickname || '点击设置' }} <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
                  </div>
                  <div class="user-row" @click="openEmailModal">
                    <span class="user-row-label">邮箱</span>
                    <span class="user-row-value user-row-edit">{{ userProfile.profile.value?.email || '点击设置' }} <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
                  </div>
                  <div
                    v-if="userProfile.profile.value?.inviterUserId == null"
                    class="user-row"
                    @click="openInviteBindingModal"
                  >
                    <span class="user-row-label">邀请人</span>
                    <span class="user-row-value user-row-edit">点击绑定 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
                  </div>
                  <div
                    v-else
                    class="user-row"
                  >
                    <span class="user-row-label">邀请人</span>
                    <span class="user-row-value">{{ userProfile.profile.value?.inviterNickname || '已绑定' }}</span>
                  </div>
                  <div class="user-row">
                    <span class="user-row-label">本月已生成</span>
                    <span class="user-row-value">{{ monthlyWorks }} 篇</span>
                  </div>
                </div>

                <!-- 快捷操作 -->
                <div class="user-section">
                  <div class="user-section-title">设置</div>
                  <div class="user-action" @click="openPasswordModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    修改密码
                  </div>
                  <div class="user-action" @click="openRedeemModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M2 9a3 3 0 0 1 3-3h.93a2 2 0 0 0 1.66-.9l.82-1.2A2 2 0 0 1 11.07 2h1.86a2 2 0 0 1 1.66.9l.82 1.2a2 2 0 0 0 1.66.9H19a3 3 0 0 1 3 3"/>
                      <path d="M2 9v9a3 3 0 0 0 3 3h14a3 3 0 0 0 3-3V9"/>
                      <path d="M8 16h.01"/>
                    </svg>
                    兑换码
                  </div>
                  <div class="user-action user-action-logout" @click="handleLogout">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                      <polyline points="16 17 21 12 16 7"/>
                      <line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                    退出登录
                  </div>
                </div>
              </div>
            </template>
          </a-dropdown>
        </div>
      </header>

      <!-- 手机端子页面头部（只在非 TabBar 页面显示） -->
      <div v-if="isMobile && !isTabbarPage" class="mobile-subpage-header">
        <div class="mobile-subpage-back" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
          <span>返回</span>
        </div>
        <div v-if="subpageTitle" class="mobile-subpage-title">{{ subpageTitle }}</div>
      </div>

      <!-- 新人首冲优惠横幅 -->
      <div v-if="newcomerBannerVisible" class="newcomer-banner">
        <span class="newcomer-banner-text" @click="goToNewcomerOffer">
          🎁 新人首冲特惠：旗舰版年包再享 8 折，<span class="newcomer-price-current">¥{{ newcomerOffer.finalPrice }}</span>
          <span class="newcomer-price-original">¥{{ newcomerOffer.originalPrice }}</span> 立即开通 →
        </span>
        <span class="newcomer-banner-close" @click="dismissNewcomerBanner">✕</span>
      </div>

      <!-- 玩法指南横幅 -->
      <div v-if="guideBannerVisible" class="guide-banner">
        <span class="guide-banner-text" @click="goToGuide">
          新手？3 分钟了解怎么在爱创作变现 →
        </span>
        <span class="guide-banner-close" @click="dismissGuideBanner">✕</span>
      </div>

      <!-- 内容区 -->
      <div class="console-content" :class="{ 'console-content-hidden': inviteVisible }">
        <PullToRefresh>
          <router-view />
        </PullToRefresh>
      </div>

      <!-- 底部 -->
      <footer class="console-footer">
        <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
        <span>浙ICP备XXXXXXXX号-1</span>
      </footer>
    </div>

    <!-- 手机端底部 TabBar（只在 ≤768px 显示） -->
    <nav class="console-tabbar" :class="{ 'tabbar-hidden': isMobile && !isTabbarPage }" aria-label="主导航">
      <router-link
        v-for="tab in tabbarItems"
        :key="tab.path"
        :to="tab.path"
        class="console-tabbar-item"
        :class="{ active: isActive(tab.path) }"
      >
        <component :is="tab.icon" class="console-tabbar-icon" />
        <span class="console-tabbar-label">{{ tab.label }}</span>
      </router-link>
    </nav>
  </div>

  <!-- 用户协议弹框 -->
  <a-modal
    v-model:open="termsVisible"
    title="用户协议"
    :footer="null"
    :width="640"
    centered
    class="terms-modal legal-modal"
  >
    <div class="terms-content">
      <p>欢迎使用爱创作服务（以下简称"本服务"）。在您使用本服务之前，请仔细阅读本用户协议。</p>
      <h4>一、服务说明</h4>
      <p>爱创作是一款 AI 自媒体写作助手，通过人工智能技术帮助用户生成文章内容。</p>
      <h4>二、账号注册</h4>
      <p>您在使用本服务前需要注册账号。您承诺提供真实、准确、完整的注册信息，并及时更新。</p>
      <h4>三、使用规范</h4>
      <p>您承诺不会利用本服务从事任何违法活动，包括但不限于：</p>
      <ul>
        <li>发布违反法律法规的内容</li>
        <li>侵犯他人知识产权的内容</li>
        <li>利用 AI 进行内容搬运、洗稿、恶意拼接等行为</li>
      </ul>
      <h4>四、知识产权</h4>
      <p>本服务生成的 AI 内容，仅作为创作过程中的参考与辅助，相关输出并不代表平台立场或价值观点。</p>
      <h4>五、免责声明</h4>
      <p>因使用本服务产生的任何直接或间接损失，由用户自行承担。</p>
      <h4>六、协议更新</h4>
      <p>本协议内容如有更新，平台将提前通知用户，更新后的协议自公布之日起生效。</p>
    </div>
  </a-modal>

  <!-- 隐私政策弹框 -->
  <a-modal
    v-model:open="privacyVisible"
    title="隐私政策"
    :footer="null"
    :width="640"
    centered
    class="privacy-modal legal-modal"
  >
    <div class="terms-content">
      <p>我们非常重视您的个人隐私保护，在您使用爱创作服务时，我们会按照本隐私政策的规定收集、使用、存储和保护您的个人信息。</p>
      <h4>一、信息收集</h4>
      <p>我们收集的信息包括：账号信息（邮箱）、创作内容、使用记录等。</p>
      <h4>二、信息使用</h4>
      <p>我们使用收集的信息用于：提供和改进服务、发送通知、账号安全保护等。</p>
      <h4>三、信息共享</h4>
      <p>未经您同意，我们不会与任何第三方共享您的个人信息，法律法规要求的除外。</p>
      <h4>四、信息存储</h4>
      <p>您的信息将存储在中华人民共和国境内的服务器上。</p>
      <h4>五、联系我们</h4>
      <p>如您对隐私政策有任何疑问，请通过官方渠道与我们联系。</p>
    </div>
  </a-modal>

  <!-- 客服微信弹框 -->
  <a-modal
    v-model:open="wechatVisible"
    title="客服微信"
    :footer="null"
    :width="400"
    centered
    class="wechat-modal"
  >
    <div class="wechat-modal-content">
      <img
        class="wechat-qr-large"
        src="https://foruda.gitee.com/images/1782817803473013600/4f94eac9_8060302.png"
        alt="客服微信"
      />
      <p class="wechat-hint">扫码添加客服微信</p>
    </div>
  </a-modal>

  <!-- 修改密码弹框 -->
  <a-modal
    v-model:open="passwordVisible"
    title="修改密码"
    :footer="null"
    :width="400"
    centered
    class="password-modal"
  >
    <div class="password-modal-content">
      <div class="password-item">
        <label class="password-label">当前密码</label>
        <input
          v-model="passwordForm.oldPassword"
          type="password"
          class="password-input"
          placeholder="请输入当前密码"
        />
      </div>
      <div class="password-item">
        <label class="password-label">新密码</label>
        <input
          v-model="passwordForm.newPassword"
          type="password"
          class="password-input"
          placeholder="6-20 位新密码"
        />
      </div>
      <div class="password-item">
        <label class="password-label">确认新密码</label>
        <input
          v-model="passwordForm.confirmPassword"
          type="password"
          class="password-input"
          placeholder="再次输入新密码"
        />
      </div>
      <button class="password-submit" @click="handlePasswordSubmit">确认修改</button>
    </div>
  </a-modal>

  <!-- 修改昵称弹框 -->
  <a-modal
    v-model:open="profileVisible"
    title="修改昵称"
    :footer="null"
    :width="400"
    centered
    class="profile-modal"
  >
    <div class="profile-modal-content">
      <div class="profile-item">
        <label class="profile-label">昵称</label>
        <input
          v-model="profileForm.nickname"
          type="text"
          class="profile-input"
          placeholder="请输入昵称"
          maxlength="20"
        />
      </div>
      <button class="profile-submit" @click="handleProfileSubmit">保存</button>
    </div>
  </a-modal>

  <!-- 修改邮箱弹框 -->
  <a-modal
    v-model:open="emailVisible"
    title="修改邮箱"
    :footer="null"
    :width="400"
    centered
    class="email-modal"
  >
    <div class="email-modal-content">
      <div class="email-item">
        <label class="email-label">新邮箱</label>
        <input
          v-model="emailForm.newEmail"
          type="email"
          class="email-input"
          placeholder="请输入新邮箱"
        />
      </div>
      <div class="email-item">
        <label class="email-label">验证码</label>
        <div class="email-code-row">
          <input
            v-model="emailForm.code"
            type="text"
            class="email-input email-code-input"
            placeholder="请输入验证码"
            maxlength="6"
          />
          <button
            class="email-code-btn"
            :disabled="codeCountdown > 0"
            @click="sendEmailCode"
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '获取验证码' }}
          </button>
        </div>
      </div>
      <button class="email-submit" @click="handleEmailSubmit">确认修改</button>
    </div>
  </a-modal>

  <!-- 手机端修改邮箱：发送验证码前的人机验证弹框 -->
  <a-modal
    v-model:open="emailSliderVisible"
    title="人机验证"
    :footer="null"
    :mask-closable="false"
    :keyboard="false"
    :width="420"
    centered
    class="slider-modal email-slider-modal"
  >
    <p class="slider-modal-tip">
      拖动滑块完成验证后将向
      <b>{{ emailForm.newEmail || '当前邮箱' }}</b>
      发送 6 位邮箱验证码
    </p>
    <SliderCaptcha v-model="emailSliderPassed" />
  </a-modal>

  <!-- 绑定邀请人弹框 -->
  <a-modal
    v-model:open="inviteBindingVisible"
    title="绑定邀请人"
    :footer="null"
    :width="400"
    centered
    class="invite-binding-modal"
  >
    <div class="invite-binding-modal-content">
      <p class="invite-binding-hint">注册 7 天内可补绑一位邀请人，绑定后不可修改。</p>
      <div class="invite-binding-item">
        <label class="invite-binding-label">邀请码</label>
        <input
          v-model="inviteBindingForm.inviteCode"
          type="text"
          class="invite-binding-input"
          placeholder="请输入 6 位邀请码"
          maxlength="6"
        />
      </div>
      <button class="invite-binding-submit" @click="handleInviteBindingSubmit">确认绑定</button>
    </div>
  </a-modal>

  <!-- 新人首冲优惠弹框 -->
  <a-modal
    v-model:open="newcomerModalVisible"
    title="新人首冲特惠"
    :footer="null"
    :mask-closable="false"
    :width="420"
    centered
    class="newcomer-modal"
    @cancel="closeNewcomerModal"
  >
    <div class="newcomer-modal-body">
      <p class="newcomer-modal-desc">开通旗舰版年包，再享 8 折限时优惠</p>
      <div class="newcomer-modal-price">
        <span class="newcomer-modal-final">¥{{ newcomerOffer.finalPrice }}</span>
        <span class="newcomer-modal-original">¥{{ newcomerOffer.originalPrice }}</span>
      </div>
      <ul v-if="newcomerOffer.benefits && newcomerOffer.benefits.length" class="newcomer-modal-benefits">
        <li v-for="(item, idx) in newcomerOffer.benefits" :key="idx">{{ item }}</li>
      </ul>
      <div class="newcomer-modal-actions">
        <a-checkbox v-model:checked="newcomerModalDontShow">我已经知道，不再弹出</a-checkbox>
        <div class="newcomer-modal-btns">
          <button class="newcomer-modal-later" @click="closeNewcomerModal">稍后再说</button>
          <button class="newcomer-modal-buy" @click="newcomerModalGoToPricing">立即开通</button>
        </div>
      </div>
    </div>
  </a-modal>

</template>

<script setup>
import { ref, computed, reactive, onMounted, watch, nextTick, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import QRCode from 'qrcode'
import CoinInfoTooltip from '@/components/CoinInfoTooltip.vue'
import PullToRefresh from '@/components/PullToRefresh.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import { useIsMobile } from '@/composables/useMobile.js'
import { logout as logoutApi, sendEmailCode as sendEmailCodeApi } from '@/api/auth'
import { useUserProfile } from '@/composables/useUserProfile'
import { useBenefits } from '@/composables/useBenefits'
import { getMessages, markMessageRead, markAllMessagesRead } from '@/api/message'
import { getMyMembership } from '@/api/membership'
import { getNewcomerOffer } from '@/api/membership'
import { submitFeedback as submitFeedbackApi, pageMyFeedbacks } from '@/api/feedback'
import { listArticles, getMonthlyCount } from '@/api/article'
const logoUrl = 'https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png'
import {
  EditOutlined,
  LoadingOutlined,
  EyeOutlined,
  FolderOutlined,
  SmileOutlined,
  FireOutlined,
  ShopOutlined,
  DollarOutlined,
  TrophyOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const userProfile = useUserProfile()
const { benefits, loadBenefits } = useBenefits()

const isMobile = useIsMobile()

// 手机端：只有 TabBar 四个主页面显示底部导航，其余子页面隐藏
const tabbarPaths = ['/console/create', '/console/works', '/console/leaderboard', '/console/mine']
const isTabbarPage = computed(() => tabbarPaths.includes(route.path))

// 手机端子页面顶部返回标题
const pageTitleMap = {
  '/console/create': '创作',
  '/console/works': '我的作品',
  '/console/styles': '我的风格',
  '/console/style-market': '风格市场',
  '/console/earnings': '我的账户',
  '/console/hot-search': '热搜榜',
  '/console/leaderboard': '收益排行榜',
  '/console/mine': '我的',
  '/console/edit': '编辑文章',
  '/console/preview': '预览文章',
  '/console/coin': '创作币'
}
const subpageTitle = computed(() => pageTitleMap[route.path] || '')
const goBack = () => router.back()

// 处理从提现页面"返回邀请有礼"时自动打开邀请弹框
watch(
  () => route.query.openInvite,
  (val) => {
    if (val === '1') {
      openInviteModal()
      router.replace({ query: {} })
    }
  },
  { immediate: true }
)

// ---------- 玩法指南新手横幅 ----------
const GUIDE_BANNER_DISMISSED_KEY = 'aichuangzuo_guide_banner_dismissed'
const NEWCOMER_BANNER_DISMISSED_KEY = 'aichuangzuo_newcomer_banner_dismissed'
const NEWCOMER_MODAL_DISMISSED_KEY = 'aichuangzuo_newcomer_modal_dismissed'
const INVITE_MODAL_DISMISSED_KEY = 'aichuangzuo_invite_modal_dismissed'

const hasWorks = async () => {
  try {
    const data = await listArticles({ page: 1, pageSize: 1 })
    return (data.total || 0) > 0
  } catch {
    return false
  }
}

// 本月已生成：从后端统计接口读取
const readMonthlyWorks = async () => {
  try {
    return await getMonthlyCount()
  } catch {
    return 0
  }
}
const monthlyWorks = ref(0)

const guideBannerVisible = ref(!localStorage.getItem(GUIDE_BANNER_DISMISSED_KEY))

const dismissGuideBanner = () => {
  guideBannerVisible.value = false
  localStorage.setItem(GUIDE_BANNER_DISMISSED_KEY, '1')
}

const goToGuide = () => {
  router.push('/guide')
}

// ---------- 新人首冲优惠横幅 ----------
const newcomerOffer = ref({ eligible: false })
const newcomerBannerVisible = ref(false)
const newcomerModalVisible = ref(false)
const newcomerModalDontShow = ref(false)

const loadNewcomerOffer = async () => {
  if (!localStorage.getItem('aichuangzuo_access_token')) return
  try {
    const res = await getNewcomerOffer()
    const data = res.data || res
    if (data && data.eligible) {
      newcomerOffer.value = data
      newcomerBannerVisible.value = !localStorage.getItem(NEWCOMER_BANNER_DISMISSED_KEY)
    }
  } catch {
    newcomerOffer.value = { eligible: false }
  }
}

const tryShowNewcomerModal = () => {
  if (route.path !== '/console/create') return
  if (!localStorage.getItem('aichuangzuo_access_token')) return
  if (localStorage.getItem(NEWCOMER_MODAL_DISMISSED_KEY)) return
  if (newcomerOffer.value.eligible) {
    newcomerModalVisible.value = true
  }
}

const closeNewcomerModal = () => {
  newcomerModalVisible.value = false
  if (newcomerModalDontShow.value) {
    localStorage.setItem(NEWCOMER_MODAL_DISMISSED_KEY, '1')
  }
  if (route.path === '/console/create' && localStorage.getItem('aichuangzuo_access_token') && !localStorage.getItem(INVITE_MODAL_DISMISSED_KEY)) {
    inviteAutoOpened.value = true
    openInviteModal()
  }
}

const inviteAutoOpened = ref(false)
const inviteModalDontShow = ref(false)

const handleInviteClose = () => {
  if (inviteAutoOpened.value && inviteModalDontShow.value) {
    localStorage.setItem(INVITE_MODAL_DISMISSED_KEY, '1')
  }
  inviteAutoOpened.value = false
  inviteModalDontShow.value = false
}

const newcomerModalGoToPricing = () => {
  newcomerModalVisible.value = false
  if (newcomerModalDontShow.value) {
    localStorage.setItem(NEWCOMER_MODAL_DISMISSED_KEY, '1')
  }
  router.push('/pricing?newcomer=1')
}

const dismissNewcomerBanner = () => {
  newcomerBannerVisible.value = false
  localStorage.setItem(NEWCOMER_BANNER_DISMISSED_KEY, '1')
}

const goToNewcomerOffer = () => {
  router.push('/pricing?newcomer=1')
}

const navItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/works', label: '我的作品', icon: FolderOutlined },
  { path: '/console/styles', label: '我的风格', icon: SmileOutlined },
  { path: '/console/style-market', label: '风格市场', icon: ShopOutlined },
  { path: '/console/earnings', label: '我的账户', icon: DollarOutlined },
  { path: '/console/hot-search', label: '热搜榜', icon: FireOutlined },
  { path: '/console/leaderboard', label: '收益排行榜', icon: TrophyOutlined }
]

// 手机端底部 TabBar：只保留 4 个高频入口，其余功能内聚到 "我的"
// 必须和 navItems 用同一套 isActive 判断，避免点 tab 时高亮不更新
const tabbarItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/works', label: '作品', icon: FolderOutlined },
  { path: '/console/leaderboard', label: '排行榜', icon: TrophyOutlined },
  { path: '/console/mine', label: '我的', icon: UserOutlined }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// ---------- 主题切换 ----------
const THEME_KEY = 'aichuangzuo_theme'
const currentTheme = ref('light')
const isThemeTransitioning = ref(false)

const toggleTheme = (event) => {
  if (isThemeTransitioning.value) return

  const btn = event?.currentTarget
  const rect = btn?.getBoundingClientRect()
  // 兜底：若触发源不可见（例如从手机端列表行触发），从屏幕中心扩散
  const cx = rect ? rect.left + rect.width / 2 : window.innerWidth / 2
  const cy = rect ? rect.top + rect.height / 2 : window.innerHeight / 2
  const vw = window.innerWidth
  const vh = window.innerHeight
  const maxR = Math.hypot(Math.max(cx, vw - cx), Math.max(cy, vh - cy))

  const next = currentTheme.value === 'light' ? 'dark' : 'light'
  // 多段 radial-gradient：中心 = 当前主题，过渡到中间灰，外圈 = 新主题
  const maskBg = next === 'dark'
    ? 'radial-gradient(circle, #f8f9fa 0%, #d8d8d8 25%, #8a8a8a 55%, #3a3a3a 80%, #141414 100%)'
    : 'radial-gradient(circle, #141414 0%, #3a3a3a 25%, #8a8a8a 55%, #d8d8d8 80%, #f8f9fa 100%)'

  const mask = document.createElement('div')
  Object.assign(mask.style, {
    position: 'fixed',
    left: cx + 'px',
    top: cy + 'px',
    width: '0px',
    height: '0px',
    borderRadius: '50%',
    background: maskBg,
    transform: 'translate(-50%, -50%)',
    zIndex: '9999',
    pointerEvents: 'none',
    willChange: 'width, height'
  })
  document.body.appendChild(mask)

  // 强制 reflow，确保动画从 0 开始
  void mask.offsetWidth
  mask.style.transition = 'width 0.75s cubic-bezier(0.4, 0, 0.2, 1), height 0.75s cubic-bezier(0.4, 0, 0.2, 1)'
  mask.style.width = (maxR * 2) + 'px'
  mask.style.height = (maxR * 2) + 'px'

  isThemeTransitioning.value = true

  // 动画 60% 时切换主题，新主题从圆形内透出来
  setTimeout(() => {
    currentTheme.value = next
    document.body.setAttribute('data-theme', next)
    localStorage.setItem(THEME_KEY, next)
  }, 450)

  // 动画结束后移除遮罩
  setTimeout(() => {
    if (mask.parentNode) mask.parentNode.removeChild(mask)
    isThemeTransitioning.value = false
  }, 800)
}

const loadTheme = () => {
  const saved = localStorage.getItem(THEME_KEY) || 'light'
  currentTheme.value = saved
  document.body.setAttribute('data-theme', saved)
}

// ---------- 消息通知 ----------
const notifVisible = ref(false)
const activeTab = ref('generation')
const notifications = ref([])

// 消息详情弹框(announcement / feature / promotion / membership.subscribed)
const notifDetailVisible = ref(false)
const notifDetail = ref(null)

// 会员到期续订弹框
const renewalVisible = ref(false)
const expiringNotif = ref(null)

// 续订 fallback 文案,后端无 content 时前端兜底
const RENEWAL_FALLBACK_COPY = '您的会员即将到期。\n续订后可继续享受无限生成、多平台爆款标题优化、专属客服等全部权益。\n错过将影响您的创作节奏,建议尽快续订。'

// 消息类型 → 显示名(membership 单独按 subType 区分)
const notifTypeLabel = (n) => {
  if (n.type === 'membership') {
    if (n.subType === 'expiring') return '会员到期'
    if (n.subType === 'subscribed') return '订阅成功'
    return '会员提醒'
  }
  const tab = notifTabs.find(t => t.type === n.type)
  return tab ? tab.label : n.type
}

// ---------- 教程 ----------
const tutorialVisible = ref(false)

const handleTutorial = (type) => {
  tutorialVisible.value = false
  // TODO: 跳转对应页面
  console.log('教程入口:', type)
}

// ---------- 反馈 ----------
const feedbackVisible = ref(false)
const feedbackTab = ref('submit')
const historyFilter = ref('all')
const historyFilterOptions = [
  { label: '全部',   value: 'all' },
  { label: '待回复', value: '0' },
  { label: '已回复', value: '1' }
]
const historyList = ref([])
const historyLoading = ref(false)
const historyTotal = ref(0)
const historyPage = ref(1)
const historySize = ref(20)
const historyDetail = ref(null)
const feedbackType = ref('功能建议')
const feedbackTypes = ['功能建议', '问题反馈', '其他']
const feedbackContent = ref('')
const feedbackSubmitting = ref(false)

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const params = { page: historyPage.value, size: historySize.value }
    if (historyFilter.value !== 'all') params.status = Number(historyFilter.value)
    const res = await pageMyFeedbacks(params)
    historyList.value = res.data?.list || []
    historyTotal.value = res.data?.total || 0
    historyPage.value = res.data?.page || 1
    historySize.value = res.data?.size || 20
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    historyLoading.value = false
  }
}

const onHistoryFilterChange = () => {
  historyPage.value = 1
  loadHistory()
}

const onHistoryPageChange = (p) => {
  historyPage.value = p
  loadHistory()
}

const openHistoryDetail = (fb) => {
  historyDetail.value = fb
}

const closeHistoryDetail = () => {
  historyDetail.value = null
}

const closeFeedbackModal = () => {
  feedbackTab.value = 'submit'
  historyDetail.value = null
  historyList.value = []
  historyPage.value = 1
  historyFilter.value = 'all'
}

watch(feedbackTab, (t) => {
  if (t === 'history' && historyList.value.length === 0 && !historyDetail.value) {
    loadHistory()
  }
})

const submitFeedback = async () => {
  if (!feedbackContent.value.trim()) {
    message.warning('请填写反馈内容')
    return
  }
  if (feedbackSubmitting.value) return
  feedbackSubmitting.value = true
  try {
    await submitFeedbackApi({
      type: feedbackType.value,
      content: feedbackContent.value
    })
    message.success('反馈已收到，我们会尽快处理')
    feedbackContent.value = ''
    feedbackType.value = '功能建议'
    // 切到「我的反馈」tab 并刷新,让用户立刻看到自己的新记录
    feedbackTab.value = 'history'
    historyPage.value = 1
    historyList.value = []  // 强制 watch 重新触发 load
    await loadHistory()
  } catch (e) {
    if (e?.code === 117001) {
      message.warning(e.message || '今日反馈次数已达上限，明天再来')
    } else {
      message.error(e?.message || '提交失败，请稍后再试')
    }
  } finally {
    feedbackSubmitting.value = false
  }
}

// ---------- 关于我们 ----------
const aboutVisible = ref(false)

const handleAboutLink = (type) => {
  aboutVisible.value = false
  console.log('关于链接:', type)
}

// ---------- 个人中心 ----------
const userCenterVisible = ref(false)

const openPasswordModal = () => {
  userCenterVisible.value = false
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

const termsVisible = ref(false)
const privacyVisible = ref(false)
const wechatVisible = ref(false)
const passwordVisible = ref(false)
const profileVisible = ref(false)
const emailVisible = ref(false)
const emailSliderVisible = ref(false)
const emailSliderPassed = ref(false)
let emailSliderSending = false
const inviteBindingVisible = ref(false)

const inviteBindingForm = reactive({
  inviteCode: ''
})

const openInviteBindingModal = () => {
  userCenterVisible.value = false
  inviteBindingForm.inviteCode = ''
  inviteBindingVisible.value = true
}

const handleInviteBindingSubmit = async () => {
  const code = inviteBindingForm.inviteCode.trim()
  if (!code) {
    message.warning('请输入邀请码')
    return
  }
  try {
    await userProfile.saveInviteCode(code)
    inviteBindingVisible.value = false
    inviteBindingForm.inviteCode = ''
  } catch {
    // composable 已 message.error
  }
}

// 表单字段直接派生自 profile：profile 没加载时为 ''，加载后双向绑定。
// 用 getter/setter 而非 computed：computed 在 reactive 里只读。
const profileForm = reactive({
  get nickname() { return userProfile.profile.value?.nickname ?? '' },
  set nickname(v) { userProfile.profile.value && (userProfile.profile.value.nickname = v) }
})

const emailForm = reactive({
  // 当前邮箱：只读展示，修改邮箱弹框里不要回填旧邮箱
  get email() { return userProfile.profile.value?.email ?? '' },
  set email(v) { /* 当前邮箱通过 saveEmail 成功后重新拉取 profile 更新 */ },
  newEmail: '',
  code: ''
})

const codeCountdown = ref(0)
let countdownTimer = null

const startEmailCodeCountdown = () => {
  codeCountdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const sendEmailCode = async () => {
  if (codeCountdown.value > 0) return
  const email = emailForm.newEmail.trim()
  if (!email) {
    message.warning('请输入邮箱')
    return
  }
  if (isMobile.value) {
    emailSliderPassed.value = false
    emailSliderVisible.value = true
    return
  }
  try {
    await sendEmailCodeApi({ email })
    message.success('验证码已发送，请查收邮箱')
    startEmailCodeCountdown()
  } catch (e) {
    message.error(e?.message || '验证码发送失败')
  }
}

watch(emailSliderPassed, async (val) => {
  if (!val || emailSliderSending) return
  emailSliderSending = true
  try {
    const email = emailForm.newEmail.trim()
    await sendEmailCodeApi({ email })
    startEmailCodeCountdown()
    message.success('验证码已发送，请查收邮箱')
  } catch (e) {
    message.error(e?.message || '验证码发送失败')
  } finally {
    emailSliderVisible.value = false
    emailSliderSending = false
  }
})

const openProfileModal = () => {
  userCenterVisible.value = false
  // 表单字段已经是 profile 的派生值，无需重置
  profileVisible.value = true
}

const openEmailModal = () => {
  userCenterVisible.value = false
  emailForm.newEmail = ''  // 新邮箱每次重新输入，不默认填旧邮箱
  emailForm.code = ''      // 验证码每次重新输入
  emailVisible.value = true
}

const copyAccountUserId = async () => {
  const id = userProfile.profile.value?.userId
  if (!id) return
  try {
    await navigator.clipboard.writeText(id)
    message.success('用户ID已复制')
  } catch {
    message.error('复制失败')
  }
}

const handleProfileSubmit = async () => {
  const trimmed = profileForm.nickname.trim()
  if (!trimmed) {
    message.warning('昵称不能为空')
    return
  }
  if (trimmed.length > 20) {
    message.warning('昵称长度不能超过 20 个字符')
    return
  }
  try {
    await userProfile.saveNickname(profileForm.nickname)
    profileVisible.value = false
  } catch {
    // composable 已 message.error，弹框保持打开让用户修改
  }
}

const handleEmailSubmit = async () => {
  if (!emailForm.newEmail.trim() || !emailForm.code.trim()) {
    message.warning('邮箱和验证码不能为空')
    return
  }
  try {
    await userProfile.saveEmail(emailForm.newEmail, emailForm.code)
    emailVisible.value = false
    emailForm.newEmail = ''
    emailForm.code = ''
  } catch {
    // composable 已 message.error
  }
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handlePasswordSubmit = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    message.warning('请填写完整')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.warning('两次密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) {
    message.warning('密码长度需 6-20 位')
    return
  }
  try {
    await userProfile.savePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordVisible.value = false
  } catch {
    // composable 已 message.error
  }
}

const openTermsModal = () => {
  aboutVisible.value = false
  termsVisible.value = true
}

const openPrivacyModal = () => {
  aboutVisible.value = false
  privacyVisible.value = true
}

const openWechatModal = () => {
  aboutVisible.value = false
  wechatVisible.value = true
}

const handleLogout = async () => {
  userCenterVisible.value = false
  try {
    await logoutApi()
  } catch (err) {
    // 退出接口失败也继续清理本地状态
  }
  localStorage.removeItem('aichuangzuo_access_token')
  localStorage.removeItem('aichuangzuo_refresh_token')
  localStorage.removeItem('aichuangzuo_membership')
  router.push('/login')
}

// ---------- 会员 ----------
const MEMBERSHIP_KEY = 'aichuangzuo_membership'
const hasMembership = ref(false)
const membershipLevel = ref('会员')
const membershipExpiry = ref('')

// 会员是否即将到期（7天内）
const isNearExpiry = computed(() => {
  if (!hasMembership.value || !membershipExpiry.value) return false
  const expiry = new Date(membershipExpiry.value)
  const now = new Date()
  const diffDays = (expiry - now) / (1000 * 60 * 60 * 24)
  return diffDays <= 7
})

const loadMembership = () => {
  const raw = localStorage.getItem(MEMBERSHIP_KEY)
  if (!raw) {
    hasMembership.value = false
    return
  }
  // 清理旧的演示数据（仅一个“年会员”字符串）
  if (raw === '年会员') {
    localStorage.removeItem(MEMBERSHIP_KEY)
    hasMembership.value = false
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      membershipLevel.value = parsed.level || '会员'
      membershipExpiry.value = parsed.expiresAt || ''
      hasMembership.value = true
      return
    }
  } catch {
    // 旧格式 string,迁移
  }
  // 旧 string 格式
  membershipLevel.value = raw
  const fallbackExpiry = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  membershipExpiry.value = fallbackExpiry
  hasMembership.value = true
  // 写入新格式
  localStorage.setItem(MEMBERSHIP_KEY, JSON.stringify({
    level: membershipLevel.value,
    expiresAt: membershipExpiry.value
  }))
}

const handleMembershipClick = () => {
  router.push('/pricing')
}

// ---------- header 会员徽章 hover 权益卡 ----------
// tier 类权益值 → 中文展示
const TIER_LABELS = {
  none: '不可用', preset: '预设风格', custom: '自定义',
  basic_8: '基础 8 款', all_20: '全部 20 款', all_custom: '全部 + 自定义',
  standard: '标准', priority: '优先', express: '极速'
}

const formatBenefitDisplay = (item) => {
  if (item.type === 'boolean') return item.value === 'true' ? '✓' : '—'
  if (item.type === 'tier') return TIER_LABELS[item.value] || item.value
  // quota
  if (item.code === 'history_days') return item.value === '-1' ? '永久' : `${item.value} 天`
  if (item.code === 'queue_max_tasks') return `${item.value} 个`
  if (item.remaining != null) return `剩余 ${item.remaining} / 共 ${item.value}`
  return item.value
}

// 会员权益列表（hover 弹框展示）；boolean=false 的行置灰
const memberBenefits = computed(() => {
  return Object.values(benefits.value || {}).map(item => ({
    code: item.code,
    name: item.name,
    display: formatBenefitDisplay(item),
    off: item.type === 'boolean' && item.value !== 'true'
  }))
})

const extendMembership = (days, level) => {
  const now = new Date()
  const currentExpiry = membershipExpiry.value ? new Date(membershipExpiry.value) : now
  const base = currentExpiry > now ? currentExpiry : now
  const newExpiry = new Date(base.getTime() + days * 24 * 60 * 60 * 1000)
  const isoDate = newExpiry.toISOString().split('T')[0]

  membershipLevel.value = level || membershipLevel.value || '会员'
  membershipExpiry.value = isoDate
  hasMembership.value = true

  localStorage.setItem(MEMBERSHIP_KEY, JSON.stringify({
    level: membershipLevel.value,
    expiresAt: isoDate
  }))
}

const refreshMembershipFromApi = async () => {
  try {
    const res = await getMyMembership()
    const data = res.data
    if (data && data.hasMembership) {
      membershipLevel.value = data.levelName || data.level
      membershipExpiry.value = data.expiresAt
      hasMembership.value = true
      localStorage.setItem(MEMBERSHIP_KEY, JSON.stringify({
        level: membershipLevel.value,
        expiresAt: membershipExpiry.value
      }))
    } else {
      // 后端确认当前无会员/已过期：同步清空本地缓存和状态
      hasMembership.value = false
      membershipLevel.value = '会员'
      membershipExpiry.value = ''
      localStorage.removeItem(MEMBERSHIP_KEY)
    }
  } catch (err) {
    // 静默失败，继续使用本地缓存
  }
}

// ---------- 邀请有礼 ----------
const INVITE_CODE_KEY = 'aichuangzuo_invite_code'
const INVITE_STATS_KEY = 'aichuangzuo_invite_stats'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const WITHDRAW_REQUESTS_KEY = 'aichuangzuo_withdraw_requests'
const COIN_BONUS_NEW_USER = 5

// ---------- 兑换码 ----------
const REDEEM_USED_KEY = 'aichuangzuo_redeem_codes'
const REDEEM_HISTORY_KEY = 'aichuangzuo_redeem_history'

const redeemVisible = ref(false)
const redeemCode = ref('')
const redeemLoading = ref(false)
const redeemStatus = ref(null)
const redeemInputRef = ref(null)

const canSubmitRedeem = computed(() => {
  return redeemCode.value.trim().length >= 6 && !redeemLoading.value
})

const openRedeemModal = () => {
  redeemVisible.value = true
  redeemCode.value = ''
  redeemStatus.value = null
  nextTick(() => {
    redeemInputRef.value?.focus()
  })
}

const REDEEM_PRESETS = {
  COIN100: { type: 'coin', reward: 100 },
  COIN500: { type: 'coin', reward: 500 },
  VIP7DAY: { type: 'membership', reward: 7, level: '专业版会员' },
  VIP30DAY: { type: 'membership', reward: 30, level: '专业版会员' }
}

const getRedeemedCodes = () => {
  try {
    const raw = localStorage.getItem(REDEEM_USED_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const saveRedeemedCode = (code) => {
  const codes = getRedeemedCodes()
  if (!codes.includes(code)) {
    codes.push(code)
    localStorage.setItem(REDEEM_USED_KEY, JSON.stringify(codes))
  }
}

const appendRedeemHistory = (record) => {
  try {
    const raw = localStorage.getItem(REDEEM_HISTORY_KEY)
    const history = raw ? JSON.parse(raw) : []
    history.unshift(record)
    localStorage.setItem(REDEEM_HISTORY_KEY, JSON.stringify(history))
  } catch {
    // ignore
  }
}

const submitRedeem = () => {
  const code = redeemCode.value.trim().toUpperCase()
  if (code.length < 6) {
    redeemStatus.value = { type: 'error', message: '兑换码格式不正确' }
    return
  }
  if (redeemLoading.value) return

  redeemLoading.value = true
  redeemStatus.value = null

  // 模拟网络请求
  setTimeout(() => {
    if (getRedeemedCodes().includes(code)) {
      redeemStatus.value = { type: 'error', message: '该兑换码已被使用过' }
      redeemLoading.value = false
      return
    }

    const preset = REDEEM_PRESETS[code]
    if (!preset) {
      redeemStatus.value = { type: 'error', message: '兑换码无效或已过期' }
      redeemLoading.value = false
      return
    }

    saveRedeemedCode(code)
    appendRedeemHistory({
      code,
      type: preset.type,
      reward: preset.reward,
      redeemedAt: new Date().toISOString()
    })

    if (preset.type === 'coin') {
      addCoin(preset.reward, `兑换码 ${code}`)
      redeemStatus.value = { type: 'success', message: `✅ 兑换成功 +${preset.reward} 创作币` }
    } else if (preset.type === 'membership') {
      extendMembership(preset.reward, preset.level)
      redeemStatus.value = { type: 'success', message: `✅ 兑换成功 +${preset.reward} 天${preset.level}` }
    }

    redeemLoading.value = false
    setTimeout(() => {
      redeemVisible.value = false
      redeemCode.value = ''
      redeemStatus.value = null
    }, 2000)
  }, 400)
}

const inviteVisible = ref(false)
const withdrawVisible = ref(false)
const inviteRulesVisible = ref(false)
const posterVisible = ref(false)
const posterSelectedId = ref('classic-red')
const posterPreviewRefs = ref({})
const inviteCode = ref('')
const coinBalance = ref(0)
const simulateEmail = ref('')
const withdrawAmount = ref(null)
const withdrawAccount = ref('')
const withdrawName = ref('')
const userId = computed(() => userProfile.profile.value?.userId || '88886666')

const inviteStats = ref({
  invitedCount: 0,
  membershipDaysEarned: 0,
  coinEarned: 0,
  friends: []
})

const inviteLink = computed(() => {
  return `${window.location.origin}/login?ref=${inviteCode.value}`
})

const generateInviteCode = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

const getStoredInviteCode = () => {
  let code = localStorage.getItem(INVITE_CODE_KEY)
  if (!code) {
    code = generateInviteCode()
    localStorage.setItem(INVITE_CODE_KEY, code)
  }
  return code
}

const loadInviteStats = () => {
  const raw = localStorage.getItem(INVITE_STATS_KEY)
  if (raw) {
    inviteStats.value = JSON.parse(raw)
  } else {
    inviteStats.value = {
      invitedCount: 0,
      membershipDaysEarned: 0,
      coinEarned: 0,
      friends: []
    }
  }
}

const saveInviteStats = () => {
  localStorage.setItem(INVITE_STATS_KEY, JSON.stringify(inviteStats.value))
}

const loadCoinBalance = () => {
  const raw = localStorage.getItem(COIN_BALANCE_KEY)
  coinBalance.value = raw ? parseInt(raw, 10) : 0
}

const setCoinBalance = (amount) => {
  coinBalance.value = amount
  localStorage.setItem(COIN_BALANCE_KEY, String(amount))
}

const addCoin = (amount, reason) => {
  const balance = coinBalance.value + amount
  setCoinBalance(balance)
  inviteStats.value.coinEarned += amount
  saveInviteStats()
  console.log('创作币变动:', reason, amount, '余额:', balance)
}

const calculateMembershipReward = (totalInvited) => {
  if (totalInvited === 3) return 3
  if (totalInvited === 5) return 5
  if (totalInvited > 5) return 2
  return 0
}

// ---------- 海报样式 ----------
const posterTemplates = [
  {
    id: 'classic-red',
    name: '品牌红',
    tag: '热情醒目',
    background: '#FF2442',
    accent: '#fff',
    textPrimary: '#ffffff',
    textSecondary: 'rgba(255,255,255,0.85)',
    codeBg: 'rgba(255,255,255,0.18)',
    codeBorder: 'rgba(255,255,255,0.4)',
    deco: 'circle',
    qrBg: '#ffffff',
    qrFg: '#1a1a1a',
    logoBg: '#fff'
  },
  {
    id: 'clean-white',
    name: '简约白',
    tag: '干净百搭',
    background: '#ffffff',
    accent: '#FF2442',
    textPrimary: '#1a1a1a',
    textSecondary: '#595959',
    codeBg: '#fff5f7',
    codeBorder: '#FF2442',
    deco: 'line',
    qrBg: '#ffffff',
    qrFg: '#1a1a1a',
    logoBg: 'transparent'
  },
  {
    id: 'dark-premium',
    name: '深色高级',
    tag: '沉稳质感',
    background: '#1a1a2e',
    accent: '#f5b942',
    textPrimary: '#ffffff',
    textSecondary: 'rgba(255,255,255,0.7)',
    codeBg: 'rgba(245,185,66,0.15)',
    codeBorder: '#f5b942',
    deco: 'diamond',
    qrBg: '#ffffff',
    qrFg: '#1a1a1a',
    logoBg: '#fff'
  },
  {
    id: 'fresh-green',
    name: '清新绿',
    tag: '自然清新',
    background: '#07c160',
    accent: '#fff',
    textPrimary: '#ffffff',
    textSecondary: 'rgba(255,255,255,0.85)',
    codeBg: 'rgba(255,255,255,0.18)',
    codeBorder: 'rgba(255,255,255,0.4)',
    deco: 'wave',
    qrBg: '#ffffff',
    qrFg: '#1a1a1a',
    logoBg: '#fff'
  }
]

const selectedTemplate = computed(() => {
  return posterTemplates.find(t => t.id === posterSelectedId.value) || posterTemplates[0]
})

// 缓存 logo image 元素 + 当前邀请链接对应的 QR code data URL
let logoImage = null
let logoAspect = 1210 / 648  // logo.png 原始宽高比 (~1.87:1，长方形)
const qrCache = new Map()

const loadLogo = () => {
  if (logoImage) return Promise.resolve(logoImage)
  return new Promise((resolve) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      logoImage = img
      if (img.naturalWidth && img.naturalHeight) {
        logoAspect = img.naturalWidth / img.naturalHeight
      }
      resolve(img)
    }
    img.onerror = () => resolve(null)
    img.src = logoUrl
  })
}

const buildQrDataUrl = (link) => {
  if (qrCache.has(link)) return Promise.resolve(qrCache.get(link))
  return QRCode.toDataURL(link, {
    errorCorrectionLevel: 'H',
    margin: 1,
    width: 320,
    color: { dark: '#1a1a1a', light: '#ffffff' }
  }).then(url => {
    qrCache.set(link, url)
    return url
  }).catch(() => null)
}

const loadQrImage = (dataUrl) => {
  return new Promise((resolve) => {
    if (!dataUrl) return resolve(null)
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => resolve(null)
    img.src = dataUrl
  })
}

const drawPoster = async (canvas, template, code, link) => {
  const ctx = canvas.getContext('2d')
  const W = canvas.width
  const H = canvas.height
  ctx.clearRect(0, 0, W, H)

  // 背景
  ctx.fillStyle = template.background
  ctx.fillRect(0, 0, W, H)

  // 装饰元素
  if (template.deco === 'circle') {
    ctx.fillStyle = 'rgba(255,255,255,0.08)'
    ctx.beginPath()
    ctx.arc(W - 30, 40, 80, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.arc(20, H - 30, 60, 0, Math.PI * 2)
    ctx.fill()
  } else if (template.deco === 'line') {
    ctx.strokeStyle = '#FF2442'
    ctx.lineWidth = 3
    ctx.beginPath()
    ctx.moveTo(0, H * 0.32)
    ctx.lineTo(W, H * 0.32)
    ctx.stroke()
    ctx.strokeStyle = 'rgba(255,36,66,0.15)'
    ctx.lineWidth = 1
    for (let i = 0; i < 5; i++) {
      ctx.beginPath()
      ctx.moveTo(20, H * 0.5 + i * 14)
      ctx.lineTo(W - 20, H * 0.5 + i * 14)
      ctx.stroke()
    }
  } else if (template.deco === 'diamond') {
    ctx.fillStyle = 'rgba(245,185,66,0.1)'
    ctx.save()
    ctx.translate(W / 2, H * 0.16)
    ctx.rotate(Math.PI / 4)
    ctx.fillRect(-40, -40, 80, 80)
    ctx.restore()
    ctx.strokeStyle = 'rgba(245,185,66,0.35)'
    ctx.lineWidth = 1
    for (let i = 0; i < 3; i++) {
      ctx.beginPath()
      ctx.arc(W / 2, H - 60, 30 + i * 12, 0, Math.PI * 2)
      ctx.stroke()
    }
  } else if (template.deco === 'wave') {
    ctx.fillStyle = 'rgba(255,255,255,0.08)'
    ctx.beginPath()
    ctx.moveTo(0, H * 0.7)
    ctx.quadraticCurveTo(W * 0.5, H * 0.55, W, H * 0.7)
    ctx.lineTo(W, H)
    ctx.lineTo(0, H)
    ctx.closePath()
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(0, H * 0.85)
    ctx.quadraticCurveTo(W * 0.5, H * 0.7, W, H * 0.85)
    ctx.lineTo(W, H)
    ctx.lineTo(0, H)
    ctx.closePath()
    ctx.fill()
  }

  // 顶部 logo + 品牌名（按 logo.png 原始长方形比例绘制，不扭曲）
  const logoHeight = Math.round(W * 0.16)
  const logoWidth = Math.round(logoHeight * logoAspect)
  const headerCenterY = H * 0.13
  if (logoImage) {
    if (template.logoBg && template.logoBg !== 'transparent') {
      // 圆角徽章比 logo 四周各留 8px
      const padX = 12
      const padY = 8
      ctx.fillStyle = template.logoBg
      roundRect(
        ctx,
        W / 2 - logoWidth / 2 - padX,
        headerCenterY - logoHeight / 2 - padY,
        logoWidth + padX * 2,
        logoHeight + padY * 2,
        10
      )
      ctx.fill()
    }
    ctx.drawImage(
      logoImage,
      W / 2 - logoWidth / 2,
      headerCenterY - logoHeight / 2,
      logoWidth,
      logoHeight
    )
  }

  ctx.textAlign = 'center'
  ctx.fillStyle = template.textPrimary
  ctx.font = `bold ${Math.round(W * 0.075)}px sans-serif`
  ctx.fillText('爱创作', W / 2, H * 0.27)

  ctx.fillStyle = template.textSecondary
  ctx.font = `${Math.round(W * 0.038)}px sans-serif`
  ctx.fillText('邀请你一起 AI 创作', W / 2, H * 0.32)

  // 高亮短语
  ctx.fillStyle = template.textPrimary
  ctx.font = `bold ${Math.round(W * 0.052)}px sans-serif`
  ctx.fillText('一次邀请 终身返佣', W / 2, H * 0.39)

  // 邀请码卡片
  const cardX = W * 0.12
  const cardY = H * 0.42
  const cardW = W * 0.76
  const cardH = H * 0.13
  ctx.fillStyle = template.codeBg
  roundRect(ctx, cardX, cardY, cardW, cardH, 8)
  ctx.fill()
  ctx.strokeStyle = template.codeBorder
  ctx.lineWidth = 2
  ctx.stroke()

  ctx.fillStyle = template.textSecondary
  ctx.font = `${Math.round(W * 0.03)}px sans-serif`
  ctx.fillText('我的邀请码', W / 2, cardY + cardH * 0.38)

  ctx.fillStyle = template.textPrimary
  ctx.font = `bold ${Math.round(W * 0.07)}px sans-serif`
  ctx.fillText(code, W / 2, cardY + cardH * 0.82)

  // 二维码区域
  const qrSize = Math.round(W * 0.34)
  const qrX = (W - qrSize) / 2
  const qrY = H * 0.6
  const qrImg = await loadQrImage(await buildQrDataUrl(link))
  if (qrImg) {
    ctx.fillStyle = template.qrBg
    roundRect(ctx, qrX - 8, qrY - 8, qrSize + 16, qrSize + 16, 10)
    ctx.fill()
    ctx.drawImage(qrImg, qrX, qrY, qrSize, qrSize)
  }

  // 底部文案
  ctx.fillStyle = template.textPrimary
  ctx.font = `bold ${Math.round(W * 0.04)}px sans-serif`
  ctx.fillText('扫码 / 输入邀请码 立即加入', W / 2, H * 0.92)

  ctx.fillStyle = template.accent
  ctx.font = `${Math.round(W * 0.034)}px sans-serif`
  ctx.fillText('立享专业版会员 + 创作币奖励', W / 2, H * 0.955)
}

const roundRect = (ctx, x, y, w, h, r) => {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

const renderPosterPreviews = async () => {
  await loadLogo()
  const code = inviteCode.value || 'ABC123'
  const link = inviteLink.value
  for (const t of posterTemplates) {
    const canvas = posterPreviewRefs.value[t.id]
    if (canvas) await drawPoster(canvas, t, code, link)
  }
}

const setPosterPreviewRef = (id, el) => {
  if (el) posterPreviewRefs.value[id] = el
}

const openPosterModal = () => {
  posterVisible.value = true
  // 等 modal 渲染完后再画预览
  setTimeout(renderPosterPreviews, 50)
}

const downloadSelectedPoster = async () => {
  const template = selectedTemplate.value
  await loadLogo()
  const canvas = document.createElement('canvas')
  canvas.width = 750
  canvas.height = 1000
  await drawPoster(canvas, template, inviteCode.value, inviteLink.value)
  const link = document.createElement('a')
  link.download = `invite-poster-${template.id}.png`
  link.href = canvas.toDataURL()
  link.click()
  message.success(`${template.name} 海报已保存`)
}

const openInviteModal = () => {
  inviteCode.value = getStoredInviteCode()
  loadInviteStats()
  loadCoinBalance()
  inviteVisible.value = true
}

const openInviteRulesDrawer = () => {
  inviteRulesVisible.value = true
}

const copyInviteCode = () => {
  navigator.clipboard.writeText(inviteCode.value).then(() => {
    message.success('邀请码已复制')
  })
}

const copyInviteLink = () => {
  navigator.clipboard.writeText(inviteLink.value).then(() => {
    message.success('邀请链接已复制')
  })
}

const copyUserId = () => {
  navigator.clipboard.writeText(userId.value).then(() => {
    message.success('ID 已复制')
  })
}

const simulateInviteRegister = () => {
  const email = simulateEmail.value.trim()
  if (!email || !email.includes('@')) {
    message.warning('请输入有效的邮箱')
    return
  }
  if (inviteStats.value.friends.some(f => f.email === email)) {
    message.warning('该邮箱已被邀请')
    return
  }
  inviteStats.value.invitedCount += 1
  const days = calculateMembershipReward(inviteStats.value.invitedCount)
  if (days > 0) {
    inviteStats.value.membershipDaysEarned += days
  }
  inviteStats.value.friends.unshift({
    email,
    status: 'registered',
    commission: 0,
    createdAt: new Date().toISOString()
  })
  saveInviteStats()
  simulateEmail.value = ''
  message.success('模拟注册成功')
}

const simulateFriendPurchase = (friendEmail, orderAmount, isFirst) => {
  const friend = inviteStats.value.friends.find(f => f.email === friendEmail)
  if (!friend) {
    message.warning('未找到该好友')
    return
  }
  const rate = isFirst ? 0.1 : 0.05
  const commission = Math.ceil(orderAmount * rate)
  friend.status = 'purchased'
  friend.commission += commission
  saveInviteStats()
  addCoin(commission, '好友购买返利 ' + friendEmail)
}

const openWithdrawModal = () => {
  withdrawAmount.value = null
  withdrawAccount.value = ''
  withdrawName.value = ''
  withdrawVisible.value = true
}

const goToWithdraw = () => {
  inviteVisible.value = false
  router.push('/console/coin?from=invite')
}

const submitWithdraw = () => {
  const amount = Number(withdrawAmount.value)
  const account = withdrawAccount.value.trim()
  const name = withdrawName.value.trim()
  if (!amount || amount < 100) {
    message.warning('提现金额最低 100 创作币')
    return
  }
  if (amount > coinBalance.value) {
    message.warning('提现金额不能超过余额')
    return
  }
  if (!account || !name) {
    message.warning('请填写支付宝账号和真实姓名')
    return
  }
  const requests = JSON.parse(localStorage.getItem(WITHDRAW_REQUESTS_KEY) || '[]')
  requests.push({ amount, account, name, status: 'pending', createdAt: new Date().toISOString() })
  localStorage.setItem(WITHDRAW_REQUESTS_KEY, JSON.stringify(requests))
  setCoinBalance(coinBalance.value - amount)
  withdrawVisible.value = false
  message.success('提现申请已提交，预计 7 天内到账')
}

const loadInviteData = () => {
  inviteCode.value = getStoredInviteCode()
  loadInviteStats()
  loadCoinBalance()
}

const notifTabs = [
  { type: 'announcement', label: '公告' },
  { type: 'feedback', label: '我的反馈' },
  { type: 'generation', label: '生成完成' },
  { type: 'membership', label: '会员提醒' },
  { type: 'feature', label: '新功能' },
  { type: 'promotion', label: '优惠活动' }
]

const activeTabLabel = computed(() => {
  return notifTabs.find(t => t.type === activeTab.value)?.label || ''
})

const currentNotifs = computed(() => {
  return notifications.value
    .filter(n => n.type === activeTab.value)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const getUnreadByType = (type) => {
  return notifications.value.filter(n => n.type === type && !n.read).length
}

const formatTime = (iso) => {
  const date = new Date(iso)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + ' 天前'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const switchTab = (type) => {
  activeTab.value = type
}

const loadNotifications = async () => {
  try {
    const res = await getMessages()
    notifications.value = res.data || []
  } catch (err) {
    notifications.value = []
  }
}

const markAllRead = async () => {
  try {
    await markAllMessagesRead()
    notifications.value.forEach(n => { n.read = true })
  } catch {
    // composable 已处理错误提示
  }
}

const handleNotifClick = async (n) => {
  if (!n.read) {
    try {
      await markMessageRead(n.id)
      n.read = true
    } catch {
      // 即使接口失败也允许跳转
    }
  }
  notifVisible.value = false

  if (n.link) {
    router.push(n.link)
    return
  }
  if (n.type === 'generation') {
    router.push('/console/works')
    return
  }
  if (n.type === 'membership' && n.subType === 'expiring') {
    expiringNotif.value = n
    renewalVisible.value = true
    return
  }
  // 其余(announcement / feature / promotion / membership.subscribed):通用详情弹框
  notifDetail.value = n
  notifDetailVisible.value = true
}

const goRenewal = async () => {
  renewalVisible.value = false
  await nextTick()
  router.push('/pricing')
}

onMounted(async () => {
  loadTheme()
  await loadNotifications()
  loadMembership()
  refreshMembershipFromApi()
  userProfile.loadProfile()
  loadBenefits()
  await loadNewcomerOffer()
  tryShowNewcomerModal()

  monthlyWorks.value = await readMonthlyWorks()
  guideBannerVisible.value = guideBannerVisible.value && !(await hasWorks())
})

// ---------- 向子页面（MineIndex）暴露弹框 / 主题 / 退出 / 用户状态 ----------
// MineIndex 是 app-style "我的" 页，原本散落在 header 的 modal 触发都从这里
// 注入，modal 的实际渲染仍由 ConsoleLayout 负责（状态/JSX 都在这里），
// 避免把 4000 行 modal 模板再在 MineIndex 里复制一份。
// 教程 / 反馈 / 关于 三个弹框原本只通过直接写 ref 打开，这里加一层包装方便注入。
const openTutorialModal = () => { tutorialVisible.value = true }
const openFeedbackModal = () => { feedbackVisible.value = true }
const openAboutModal = () => { aboutVisible.value = true }

provide('consoleActions', {
  openInviteModal,
  openInviteBindingModal,
  openRedeemModal,
  openWithdrawModal,
  openTutorialModal,
  openFeedbackModal,
  openAboutModal,
  openTermsModal,
  openPrivacyModal,
  openWechatModal,
  openPasswordModal,
  openProfileModal,
  openEmailModal,
  toggleTheme,
  handleLogout,
  // 用户状态（响应式，子页面读它会跟着变）
  currentTheme,
  profileForm,
  emailForm,
  coinBalance,
  inviteStats,
  membershipLevel,
  membershipExpiry,
  hasMembership,
  profile: userProfile.profile
})
</script>

<style scoped>
.console-layout {
  display: flex;
  height: 100vh;
}

/* 侧边栏 */
.console-sidebar {
  width: 200px;
  background: var(--color-bg-card);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.console-sidebar-brand {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
  font-weight: 700;
  font-size: 18px;
  color: var(--color-primary);
}

.brand-logo {
  height: 28px;
  width: auto;
  margin-right: 8px;
}

.brand-name {
  font-weight: 700;
  font-size: 16px;
  color: #000;
}

.console-sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.console-sidebar-group {
  margin-bottom: 4px;
}

.console-sidebar-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 14px;
  color: #595959;
  font-weight: 500;
}

.console-sidebar-item.sub-item {
  padding-left: 44px;
  font-size: 13px;
}

.console-sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--color-text-primary);
  transition: all 0.2s;
  font-size: 14px;
  cursor: pointer;
}

.console-sidebar-item:hover {
  background: var(--color-primary-light);
}

.console-sidebar-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
}

/* 主内容区 */
.console-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.console-content-hidden {
  visibility: hidden;
}

/* 顶部栏 */
.console-header {
  height: 56px;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.console-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.console-membership-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.console-membership-badge.has-membership {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.console-membership-badge.no-membership {
  background: #fff;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.console-membership-badge.no-membership:hover {
  background: var(--color-primary);
  color: #fff;
}

/* header 会员徽章 hover 权益卡（固定高度，列表内部滚动） */
.mbs-pop {
  width: 280px;
}

.mbs-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.mbs-plan {
  font-size: 14px;
  font-weight: 600;
  color: #fa8c16;
}

.mbs-expiry {
  font-size: 12px;
  color: #999;
}

.mbs-list {
  height: 264px;
  overflow-y: auto;
  margin: 0;
  padding: 6px 8px 6px 0;
  list-style: none;
}

.mbs-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  padding: 4px 0;
}

.mbs-list li.off {
  color: #c8c8c8;
}

.mbs-list li.off .mbs-val {
  color: #c8c8c8;
}

.mbs-name {
  color: #333;
}

.mbs-list li.off .mbs-name {
  color: #c8c8c8;
}

.mbs-val {
  color: #fa8c16;
  font-weight: 500;
}

.mbs-foot {
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
  text-align: center;
  font-size: 12px;
  color: var(--color-primary);
  cursor: pointer;
}

.mbs-foot:hover {
  text-decoration: underline;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.console-icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
  transition: all 0.2s;
}

.console-icon-btn:hover {
  background: var(--color-primary-light);
}

.console-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 内容区 */
.console-content {
  flex: 1;
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

/* 新人首冲优惠横幅 */
.newcomer-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 24px;
  background: linear-gradient(90deg, #fff5f7, #fff0f2);
  border-bottom: 1px solid #ffd1d9;
  font-size: 14px;
  flex-shrink: 0;
}
.newcomer-banner-text {
  color: #ff2442;
  cursor: pointer;
  font-weight: 500;
}
.newcomer-banner-text:hover {
  text-decoration: underline;
}
.newcomer-price-current {
  font-weight: 700;
  margin: 0 4px;
}
.newcomer-price-original {
  text-decoration: line-through;
  color: #8c8c8c;
  font-weight: 400;
  margin-left: 4px;
}
.newcomer-banner-close {
  color: #8c8c8c;
  cursor: pointer;
  font-size: 12px;
  padding: 0 4px;
}
.newcomer-banner-close:hover {
  color: #595959;
}

/* 新人首冲优惠弹框 */
.newcomer-modal .newcomer-modal-body {
  padding: 8px 4px 4px;
  text-align: center;
}
.newcomer-modal-desc {
  color: #595959;
  font-size: 14px;
  margin-bottom: 16px;
}
.newcomer-modal-price {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}
.newcomer-modal-final {
  font-size: 36px;
  font-weight: 800;
  color: #ff2442;
}
.newcomer-modal-original {
  font-size: 16px;
  color: #8c8c8c;
  text-decoration: line-through;
}
.newcomer-modal-benefits {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  text-align: left;
  background: #fafafa;
  border-radius: 10px;
  padding: 14px 18px;
}
.newcomer-modal-benefits li {
  position: relative;
  padding-left: 20px;
  color: #595959;
  font-size: 13px;
  line-height: 1.8;
}
.newcomer-modal-benefits li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #07c160;
  font-weight: 700;
}
.newcomer-modal-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}
.newcomer-modal-actions .ant-checkbox-wrapper {
  color: #8c8c8c;
  font-size: 13px;
}
.newcomer-modal-btns {
  display: flex;
  gap: 12px;
  justify-content: center;
  width: 100%;
}
.newcomer-modal-later,
.newcomer-modal-buy {
  flex: 1;
  padding: 10px 0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
}
.newcomer-modal-later {
  background: #f0f0f0;
  color: #595959;
}
.newcomer-modal-later:hover {
  background: #e0e0e0;
}
.newcomer-modal-buy {
  background: #ff2442;
  color: #fff;
}
.newcomer-modal-buy:hover {
  background: #e61e3a;
}

/* 玩法指南横幅 */
.guide-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 24px;
  background: #fff5f7;
  border-bottom: 1px solid #ffd1d9;
  font-size: 14px;
  flex-shrink: 0;
}
.guide-banner-text {
  color: #ff2442;
  cursor: pointer;
  font-weight: 500;
}
.guide-banner-text:hover {
  text-decoration: underline;
}
.guide-banner-close {
  color: #8c8c8c;
  cursor: pointer;
  font-size: 12px;
  padding: 0 4px;
}
.guide-banner-close:hover {
  color: #595959;
}

/* 底部 */
.console-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 13px;
  text-align: center;
  background: var(--color-bg-card);
}

.console-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: var(--color-border-light);
}

/* ========== 消息通知 ========== */
.bell-wrap {
  position: relative;
}

.bell-btn {
  position: relative;
}

.bell-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #FF2442;
  color: #fff;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  pointer-events: none;
}

/* 通知面板 */
.notif-panel {
  width: 100%;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  user-select: none;
}

.notif-modal .ant-modal-body {
  padding: 0;
}

.notif-modal .ant-modal-header {
  margin-bottom: 0;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.notif-title {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
}

.notif-read-all {
  font-size: 12px;
  color: var(--color-primary);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  transition: opacity 0.2s;
}

.notif-read-all:hover {
  opacity: 0.75;
}

/* Tab 栏 */
.notif-tabs {
  display: flex;
  padding: 0 8px;
  gap: 0;
  border-bottom: 1px solid #f0f0f0;
}

.notif-tab {
  flex: 1;
  padding: 10px 6px;
  font-size: 12px;
  color: #8c8c8c;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  white-space: nowrap;
}

.notif-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 600;
}

.notif-tab-badge {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #FF2442;
  color: #fff;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 消息列表 */
.notif-list {
  max-height: 520px;
  min-height: 320px;
  overflow-y: auto;
}

.notif-empty {
  padding: 40px 0;
  text-align: center;
}

.notif-empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.notif-empty-text {
  font-size: 13px;
  color: #8c8c8c;
}

/* 消息详情弹框 */
.notif-detail-modal .ant-modal-body {
  max-height: 70vh;
  overflow-y: auto;
  padding: 24px 28px 8px;
}

.notif-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notif-detail-type-chip {
  display: inline-block;
  align-self: flex-start;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: #fff0f2;
  color: #ff2442;
  font-weight: 500;
}

.notif-detail-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.4;
}

.notif-detail-time {
  font-size: 12px;
  color: #8c8c8c;
}

.notif-detail-content {
  white-space: pre-wrap;
  line-height: 1.75;
  font-size: 14px;
  color: #1a1a1a;
  padding: 12px 0 0;
  word-break: break-word;
}

/* 会员到期续订弹框 */
.renewal-panel {
  text-align: center;
  padding: 8px 0;
}

.renewal-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 14px;
  line-height: 1.4;
}

.renewal-body {
  max-height: 50vh;
  overflow-y: auto;
  text-align: left;
  white-space: pre-wrap;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
  background: #fafafa;
  padding: 16px 18px;
  border-radius: 8px;
  border-left: 3px solid #ff2442;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f9f9f9;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item:hover {
  background: #fafafa;
}

.notif-item.unread {
  background: #fff9f9;
}

.notif-item.unread:hover {
  background: #fff0f2;
}

.notif-item-dot {
  width: 8px;
  height: 8px;
  background: #FF2442;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.notif-item-body {
  flex: 1;
  min-width: 0;
}

.notif-item-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notif-item-summary {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.notif-item-time {
  font-size: 11px;
  color: #bfbfbf;
}

/* ========== 教程面板 ========== */
.tutorial-panel {
  width: 100%;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  user-select: none;
}

.tutorial-modal .ant-modal-body {
  padding: 0;
}

.tutorial-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.tutorial-title {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
}

.tutorial-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f0f0f0;
}

.tutorial-item:last-child {
  border-bottom: none;
}

.tutorial-item:hover {
  background: #fafafa;
}

.tutorial-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: var(--color-primary);
}

.tutorial-icon svg {
  width: 100%;
  height: 100%;
}

.tutorial-body {
  flex: 1;
  min-width: 0;
}

.tutorial-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.tutorial-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}

/* ========== 反馈面板 ========== */
.feedback-modal .ant-modal-body { max-height: 70vh; overflow-y: auto; }
.feedback-tabs { margin-top: -8px; }
.feedback-tabs :deep(.ant-tabs-nav) { margin-bottom: 16px; }
.feedback-tabs :deep(.ant-tabs-ink-bar) { background: #ff2442; }
.feedback-tabs :deep(.ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn) { color: #ff2442; }
.feedback-tabs :deep(.ant-tabs-tab:hover) { color: #ff2442; }

.history-list-pane { padding: 0 4px; }
.history-filter { margin-bottom: 12px; }
.history-list { list-style: none; padding: 0; margin: 0; }
.history-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.history-item:hover { background: #fafafa; }
.history-item-row1 {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px; flex-wrap: wrap;
}
.history-item-time { font-size: 12px; color: #8c8c8c; margin-left: auto; }
.history-item-content {
  font-size: 14px; color: #1a1a1a;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; text-overflow: ellipsis;
  line-height: 1.5;
}
.history-item-reply-preview {
  font-size: 12px; color: #ff2442; margin-top: 6px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.history-empty { text-align: center; padding: 32px 0; color: #8c8c8c; }
.history-empty p { margin-bottom: 12px; }
.history-pager { text-align: center; margin-top: 12px; }

/* 空状态 "去提交" 按钮：覆盖 ant-design 默认蓝,用品牌红 */
:deep(.history-go-submit.ant-btn-primary) {
  background: #ff2442;
  border-color: #ff2442;
}
:deep(.history-go-submit.ant-btn-primary:hover),
:deep(.history-go-submit.ant-btn-primary:focus) {
  background: #e61e3a;
  border-color: #e61e3a;
}

:deep(.history-type-tag) {
  background: #fff0f2;
  color: #ff2442;
  border: 1px solid #ffd6dd;
}

.history-detail-pane { padding: 0 4px; }
.history-back-btn { padding-left: 0; margin-bottom: 8px; }
.detail-row {
  display: flex; align-items: center; gap: 12px; margin-bottom: 10px;
}
.detail-row-stack { align-items: flex-start; flex-direction: column; gap: 6px; }
.detail-label {
  display: inline-block; min-width: 80px;
  color: #8c8c8c; font-size: 13px;
}
.detail-content {
  margin: 0; white-space: pre-wrap; word-break: break-word;
  background: #f7f7f7; padding: 12px; border-radius: 6px;
  font-family: inherit; font-size: 13px; line-height: 1.7;
  width: 100%; box-sizing: border-box;
}
.detail-content-admin { background: #fff0f2; }
.history-pending-hint {
  text-align: center; color: #8c8c8c; padding: 16px 0; font-size: 13px;
}

.feedback-panel {
  width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  user-select: none;
}

.feedback-modal .ant-modal-body {
  padding: 0;
}

.feedback-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 14px;
}

.feedback-label {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.feedback-type {
  margin-bottom: 12px;
}

.feedback-type-btns {
  display: flex;
  gap: 6px;
}

.type-btn {
  flex: 1;
  padding: 6px 0;
  font-size: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.type-btn.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.type-btn:hover:not(.active) {
  border-color: #bfbfbf;
  color: #1a1a1a;
}

.feedback-content {
  margin-bottom: 12px;
}

.feedback-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #1a1a1a;
  resize: vertical;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.feedback-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.feedback-textarea::placeholder {
  color: #bfbfbf;
}

.feedback-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.feedback-submit:hover {
  background: var(--color-primary-hover);
}

/* ========== 关于我们面板 ========== */
.about-panel {
  width: 100%;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  user-select: none;
}

.about-modal .ant-modal-body {
  padding: 0;
}

.about-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.about-logo {
  flex-shrink: 0;
}

.about-logo img {
  height: 44px;
  width: auto;
}

.about-brand {
  flex: 1;
}

.about-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 2px;
}

.about-tagline {
  font-size: 12px;
  color: #8c8c8c;
}

.about-desc {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.about-desc p {
  font-size: 12px;
  color: #595959;
  line-height: 1.7;
  margin-bottom: 8px;
}

.about-desc p:last-child {
  margin-bottom: 0;
}

.about-links {
  padding: 8px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.about-link-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  font-size: 12px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.about-link-btn svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.about-link-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.about-footer {
  padding: 12px 16px;
  text-align: center;
  font-size: 11px;
  color: #bfbfbf;
  border-top: 1px solid #f0f0f0;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .console-sidebar {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .console-sidebar-brand {
  border-color: #303030;
  color: #10b981;
}

body[data-theme="dark"] .brand-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .console-sidebar-item {
  color: #e0e0e0;
}

body[data-theme="dark"] .console-sidebar-item:hover,
body[data-theme="dark"] .console-sidebar-item.active {
  background: rgba(255, 36, 66, 0.15);
  color: var(--color-primary);
}

body[data-theme="dark"] .console-header {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .console-content {
  background: #141414;
}

body[data-theme="dark"] .newcomer-banner {
  background: linear-gradient(90deg, #331018, #2a0d12);
  border-bottom-color: #52222b;
}
body[data-theme="dark"] .newcomer-banner-text {
  color: #ff4d6f;
}
body[data-theme="dark"] .newcomer-price-original {
  color: #a6a6a6;
}
body[data-theme="dark"] .newcomer-banner-close {
  color: #a6a6a6;
}
body[data-theme="dark"] .newcomer-banner-close:hover {
  color: #e0e0e0;
}

body[data-theme="dark"] .newcomer-modal-desc {
  color: #a6a6a6;
}
body[data-theme="dark"] .newcomer-modal-original {
  color: #8c8c8c;
}
body[data-theme="dark"] .newcomer-modal-benefits {
  background: #1f1f1f;
}
body[data-theme="dark"] .newcomer-modal-benefits li {
  color: #a6a6a6;
}
body[data-theme="dark"] .newcomer-modal-actions .ant-checkbox-wrapper {
  color: #a6a6a6;
}
body[data-theme="dark"] .newcomer-modal-later {
  background: #2c2c2c;
  color: #bfbfbf;
}
body[data-theme="dark"] .newcomer-modal-later:hover {
  background: #3a3a3a;
}

body[data-theme="dark"] .guide-banner {
  background: #331018;
  border-bottom-color: #52222b;
}
body[data-theme="dark"] .guide-banner-text {
  color: #ff4d6f;
}
body[data-theme="dark"] .guide-banner-close {
  color: #a6a6a6;
}
body[data-theme="dark"] .guide-banner-close:hover {
  color: #e0e0e0;
}

body[data-theme="dark"] .console-footer {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .console-icon-btn {
  color: #e0e0e0;
}

body[data-theme="dark"] .console-icon-btn:hover {
  background: #2a2a2a;
}

body[data-theme="dark"] .console-membership-badge.has-membership {
  background: #2b2111;
  border-color: #594214;
  color: #ffa940;
}

body[data-theme="dark"] .notif-panel {
  background: #1f1f1f;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  border-radius: 8px;
}

body[data-theme="dark"] .tutorial-panel,
body[data-theme="dark"] .feedback-panel,
body[data-theme="dark"] .about-panel {
  background: #1f1f1f;
  border-radius: 8px;
}

body[data-theme="dark"] .notif-header,
body[data-theme="dark"] .notif-tabs,
body[data-theme="dark"] .about-header,
body[data-theme="dark"] .about-desc {
  border-color: #303030;
}

/* 给消息中心头部右侧留出关闭按钮位置，避免"全部已读"被 X 覆盖 */
body[data-theme="dark"] .notif-header {
  padding-right: 56px;
}

body[data-theme="dark"] .notif-title,
body[data-theme="dark"] .form-title,
body[data-theme="dark"] .about-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .notif-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .notif-tab.active {
  color: #ff4d6f;
  border-bottom-color: #ff4d6f;
}

body[data-theme="dark"] .notif-read-all {
  color: #ff4d6f;
}

body[data-theme="dark"] .notif-item {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .notif-item:hover {
  background: #2a2a2a;
}

body[data-theme="dark"] .notif-item.unread {
  background: rgba(255, 36, 66, 0.08);
}

body[data-theme="dark"] .notif-item.unread:hover {
  background: rgba(255, 36, 66, 0.14);
}

body[data-theme="dark"] .notif-item-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .tutorial-item {
  border-color: #303030;
}

body[data-theme="dark"] .tutorial-item:hover {
  background: rgba(255, 36, 66, 0.1);
}

body[data-theme="dark"] .tutorial-header {
  border-color: #303030;
}

body[data-theme="dark"] .tutorial-name,
body[data-theme="dark"] .tutorial-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .tutorial-icon {
  color: #ff4d6f;
}

body[data-theme="dark"] .feedback-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .feedback-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .feedback-textarea {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .feedback-textarea:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .feedback-submit {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .feedback-submit:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .type-btn {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .type-btn.active {
  border-color: #ff4d6f;
  color: #ff4d6f;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .about-logo {
}

body[data-theme="dark"] .about-tagline {
  color: #a6a6a6;
}

body[data-theme="dark"] .about-desc p {
  color: #a6a6a6;
}

body[data-theme="dark"] .about-link-btn {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .about-link-btn:hover {
  background: rgba(255, 36, 66, 0.12);
  border-color: #ff4d6f;
  color: #ff4d6f;
}

body[data-theme="dark"] .about-footer {
  border-color: #303030;
  color: #666;
}

/* ========== 个人中心面板 ========== */
.user-center-panel {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  user-select: none;
}

/* 会员卡 */
.membership-card {
  background: linear-gradient(135deg, #FF2442 0%, #cc1730 100%);
  padding: 16px 20px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.membership-left {
  flex: 1;
}

.membership-label {
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 4px;
}

.membership-name {
  font-size: 18px;
  font-weight: 700;
}

.membership-expiry {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 2px;
}

.membership-btn {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.membership-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.membership-link {
  font-size: 13px;
  color: #fff;
  cursor: pointer;
  opacity: 0.9;
}

.membership-link:hover {
  opacity: 1;
  text-decoration: underline;
}

/* 账号信息 */
.user-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.user-section:last-child {
  border-bottom: none;
}

.user-section-title {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 10px;
}

.user-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.user-row-label {
  font-size: 13px;
  color: #595959;
}

.user-row-value {
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 500;
}

/* 快捷操作 */
.user-action {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  font-size: 14px;
  color: #262626;
  cursor: pointer;
  transition: color 0.2s;
}

.user-action svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: #8c8c8c;
}

.user-action:hover {
  color: var(--color-primary);
}

.user-action:hover svg {
  color: var(--color-primary);
}

.user-action-logout {
  color: #ff4d4f;
}

.user-action-logout svg {
  color: #ff4d4f;
}

.user-action-logout:hover {
  color: #ff4d4f;
  opacity: 0.8;
}

.user-row-edit {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--color-primary);
}

.user-row-edit svg {
  width: 14px;
  height: 14px;
}

.user-row-edit:hover {
  opacity: 0.75;
}

.user-row-copy {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.user-row-copy svg {
  width: 14px;
  height: 14px;
  color: var(--color-primary);
}

.user-row-copy:hover {
  opacity: 0.75;
}

/* 修改昵称 */
.profile-modal-content {
  padding: 8px 0;
}

.profile-item {
  margin-bottom: 16px;
}

.profile-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.profile-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.profile-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.profile-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.profile-submit:hover {
  background: var(--color-primary-hover);
}

/* 修改邮箱 */
.email-modal-content {
  padding: 8px 0;
}

.email-item {
  margin-bottom: 16px;
}

.email-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.email-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.email-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.email-code-row {
  display: flex;
  gap: 10px;
}

.email-code-input {
  flex: 1;
}

.email-code-btn {
  padding: 0 14px;
  background: #fff;
  border: 1px solid var(--color-primary);
  border-radius: 8px;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.email-code-btn:hover:not(:disabled) {
  background: var(--color-primary);
  color: #fff;
}

.email-code-btn:disabled {
  border-color: #d9d9d9;
  color: #8c8c8c;
  cursor: not-allowed;
}

.email-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.email-submit:hover {
  background: var(--color-primary-hover);
}

/* 滑块人机验证弹框 */
.slider-modal-tip {
  font-size: 13px;
  color: #595959;
  margin: 0 0 16px;
  line-height: 1.6;
}

.slider-modal-tip b {
  color: #FF2442;
  font-weight: 500;
  word-break: break-all;
}

.slider-modal :deep(.ant-modal-header) {
  margin-bottom: 12px;
}

/* 绑定邀请人 */
.invite-binding-modal-content {
  padding: 8px 0;
}

.invite-binding-hint {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 16px;
  line-height: 1.5;
}

.invite-binding-item {
  margin-bottom: 16px;
}

.invite-binding-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.invite-binding-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  text-transform: uppercase;
}

.invite-binding-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.invite-binding-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.invite-binding-submit:hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .slider-modal-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .slider-modal-tip b {
  color: #ff4d6f;
}

/* 暗色主题 */
body[data-theme="dark"] .profile-label,
body[data-theme="dark"] .email-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .profile-input,
body[data-theme="dark"] .email-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .profile-input:focus,
body[data-theme="dark"] .email-input:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .email-code-btn {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .email-code-btn:hover:not(:disabled) {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .email-code-btn:disabled {
  border-color: #434343;
  color: #737373;
  background: transparent;
}

/* 绑定邀请人弹框 - 暗色主题 */
body[data-theme="dark"] .invite-binding-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .invite-binding-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .invite-binding-input {
  background: #262626;
  border-color: #404040;
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-binding-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .invite-binding-input:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

/* 暗色主题 */
body[data-theme="dark"] .user-center-panel {
  background: #1f1f1f;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .membership-card {
  background: linear-gradient(135deg, #b01030 0%, #8a0f25 100%);
}

body[data-theme="dark"] .user-section {
  border-color: #303030;
}

body[data-theme="dark"] .user-row-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .user-row-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .user-action {
  color: #e0e0e0;
}

body[data-theme="dark"] .user-action svg {
  color: #666;
}

/* 修改密码弹框 */
.password-modal-content {
  padding: 8px 0;
}

.password-item {
  margin-bottom: 16px;
}

.password-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.password-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.password-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.password-input::placeholder {
  color: #bfbfbf;
}

.password-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.password-submit:hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .password-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .password-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .password-input:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .password-input::placeholder {
  color: #737373;
}

/* 用户协议 / 隐私政策 */
.terms-content {
  max-height: 60vh;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
  color: #262626;
}

.terms-content h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 16px 0 8px;
}

.terms-content p {
  margin-bottom: 10px;
}

.terms-content ul {
  margin: 8px 0 12px 20px;
}

.terms-content li {
  margin-bottom: 4px;
}

/* 客服微信二维码 */
.wechat-qr {
  width: 120px;
  height: auto;
  display: block;
  margin: 0 auto;
  border-radius: 8px;
}

.wechat-modal-content {
  text-align: center;
  padding: 16px 0;
  user-select: none;
}

.wechat-qr-large {
  width: 200px;
  height: auto;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  pointer-events: none;
}

.wechat-hint {
  margin-top: 16px;
  font-size: 14px;
  color: #595959;
}

/* ========== 邀请有礼 ========== */
.console-invite-btn {
  width: auto;
  padding: 0 12px;
  gap: 6px;
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 16px;
}

.console-invite-btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

.console-invite-btn .console-icon {
  stroke-width: 2.2;
}

.invite-modal .ant-modal-body {
  padding: 0;
}

.invite-panel {
  width: 100%;
  background: #f5f5f5;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.invite-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.invite-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-bottom: 4px;
}

.invite-modal-auto-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 12px 20px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.invite-modal-auto-footer .ant-checkbox-wrapper {
  color: #8c8c8c;
  font-size: 13px;
}

.invite-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
}

.invite-user-id {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f8f9fa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.invite-user-id:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

.invite-user-id-label {
  color: #8c8c8c;
}

.invite-user-id-value {
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 0.5px;
}

.invite-user-id-copy {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  color: #bfbfbf;
  transition: color 0.2s;
}

.invite-user-id:hover .invite-user-id-copy {
  color: var(--color-primary);
}

.invite-stats {
  display: grid;
  grid-template-columns: 1fr 1fr 1.4fr;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  margin-top: 10px;
}

.invite-stat-item {
  text-align: center;
  padding: 14px;
  background: #f8f9fa;
  border-radius: 10px;
}

.invite-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
}

.invite-stat-label {
  font-size: 12px;
  color: #595959;
  margin-top: 4px;
}

.invite-stat-label-tooltip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  cursor: help;
  transition: color 0.2s;
}

.invite-stat-label-tooltip:hover {
  color: var(--color-primary);
}

.invite-info-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: #bfbfbf;
  transition: color 0.2s;
}

.invite-stat-label-tooltip:hover .invite-info-icon {
  color: var(--color-primary);
}

.invite-stat-item-coin {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.invite-stat-label-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
}

.invite-stat-go-withdraw {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  flex-shrink: 0;
}

.invite-stat-go-withdraw:hover {
  background: #e0203b;
  transform: translateY(-1px);
}

.invite-stat-go-withdraw:active {
  transform: translateY(0);
}

.invite-rules {
  margin: 12px 20px 0;
  padding: 14px 16px;
  background: var(--color-primary-light);
  border-radius: 12px;
  border: 1px dashed rgba(255, 36, 66, 0.25);
}

.invite-rules-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(255, 36, 66, 0.2);
}

.invite-rules-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.invite-rules-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 36, 66, 0.12);
  color: var(--color-primary);
  transform: translateY(-2px);
}

.invite-rule-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 12px;
  line-height: 1.6;
  color: #595959;
  margin-bottom: 8px;
}

.invite-rule-item:last-child {
  margin-bottom: 0;
}

.invite-rule-label {
  flex-shrink: 0;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 92px;
}

.invite-rule-text {
  flex: 1;
}

.invite-rule-underline {
  text-decoration: underline;
  text-decoration-color: #1a1a1a;
  text-underline-offset: 3px;
  text-decoration-thickness: 2px;
  color: #1a1a1a;
  font-weight: 700;
}

.invite-rules-detail-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 12px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid rgba(255, 36, 66, 0.25);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  cursor: pointer;
  transition: all 0.2s;
}

.invite-rules-detail-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

.invite-rules-detail-arrow {
  font-size: 18px;
  line-height: 1;
  color: var(--color-primary);
}

/* 完整规则抽屉 */
.invite-rules-drawer .ant-drawer-body {
  padding: 0;
}

.invite-rules-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.invite-rules-detail-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.invite-rules-detail-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
}

.invite-rules-detail-close {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.invite-rules-detail-close:hover {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.invite-rules-detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 28px;
}

.invite-rules-detail-section + .invite-rules-detail-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px dashed #eee;
}

.invite-rules-detail-heading {
  position: relative;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 12px;
  padding-left: 12px;
}

.invite-rules-detail-heading::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  border-radius: 2px;
  background: var(--color-primary);
}

.invite-rules-detail-callout {
  padding: 10px 14px;
  margin-bottom: 12px;
  background: rgba(255, 36, 66, 0.06);
  border-left: 3px solid var(--color-primary);
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  color: #262626;
}

.invite-rules-detail-callout b {
  color: var(--color-primary);
  font-weight: 700;
}

.invite-rules-detail-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.invite-rules-detail-list li {
  position: relative;
  padding-left: 18px;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #595959;
}

.invite-rules-detail-list li:last-child {
  margin-bottom: 0;
}

.invite-rules-detail-list li::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 9px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
}

.invite-code-card,
.invite-link-card,
.invite-progress-card,
.invite-friend-card {
  margin: 12px 20px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.invite-code-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.invite-code-box {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 14px 16px;
  flex: 1;
  min-width: 180px;
}

.invite-code-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.invite-code-value {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 2px;
}

.invite-link-value {
  font-size: 13px;
  color: #262626;
  word-break: break-all;
  margin-bottom: 12px;
  line-height: 1.5;
}

.invite-link-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.invite-progress-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.invite-progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.invite-progress-item:last-child {
  margin-bottom: 0;
}

.invite-progress-bar {
  flex: 1;
  height: 10px;
  background: #f0f0f0;
  border-radius: 5px;
  overflow: hidden;
}

.invite-progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 5px;
  transition: width 0.4s ease;
}

.invite-progress-text {
  width: 80px;
  font-size: 12px;
  color: #595959;
  text-align: right;
}

.invite-progress-desc {
  flex: 1;
  font-size: 12px;
  color: #595959;
}

.invite-friend-header {
  margin-bottom: 12px;
}

.invite-friend-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.invite-friend-list {
  max-height: 160px;
  overflow-y: auto;
}

.invite-friend-empty {
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
  color: #8c8c8c;
}

.invite-friend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.invite-friend-item:last-child {
  border-bottom: none;
}

.invite-friend-email {
  font-size: 13px;
  color: #262626;
}

.invite-friend-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.invite-friend-status.registered {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.invite-friend-status.purchased {
  background: #fff7e6;
  color: #fa8c16;
}

.invite-simulate {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
}

.invite-simulate-label {
  font-size: 12px;
  color: #595959;
  margin-bottom: 8px;
}

.invite-simulate-row {
  display: flex;
  gap: 10px;
}

.invite-form-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #1a1a1a;
  box-sizing: border-box;
}

.invite-form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

/* 提现弹框 */
.withdraw-modal .ant-modal-body {
  padding: 0;
}

.withdraw-panel {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
}

.withdraw-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
}

.withdraw-item {
  margin-bottom: 14px;
}

.withdraw-label {
  display: block;
  font-size: 12px;
  color: #595959;
  margin-bottom: 6px;
}

.withdraw-balance {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.withdraw-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #1a1a1a;
  box-sizing: border-box;
}

.withdraw-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.withdraw-hint {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 4px;
}

.withdraw-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

/* 海报样式选择弹框 */
.poster-modal .ant-modal-body {
  padding: 0;
}

.poster-panel {
  padding: 24px 26px 22px;
}

.poster-panel-header {
  margin-bottom: 18px;
}

.poster-panel-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #1f1f1f);
  margin-bottom: 4px;
}

.poster-panel-desc {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
}

.poster-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.poster-card {
  position: relative;
  border: 2px solid transparent;
  border-radius: 12px;
  padding: 10px 8px 12px;
  background: #fafafa;
  cursor: pointer;
  transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
  text-align: center;
}

.poster-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.poster-card.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.18);
}

.poster-card-canvas-wrap {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  aspect-ratio: 3 / 4;
  max-height: 240px;
}

.poster-card-canvas {
  max-width: 100%;
  max-height: 240px;
  width: auto;
  height: auto;
  display: block;
}

.poster-card-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f1f1f;
}

.poster-card-tag {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

.poster-card-check {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.poster-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

body[data-theme="dark"] .poster-panel-title {
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .poster-panel-desc,
body[data-theme="dark"] .poster-card-tag {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .poster-card {
  background: #262626;
}

body[data-theme="dark"] .poster-card-canvas-wrap {
  background: #141414;
}

body[data-theme="dark"] .poster-card-name {
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .poster-card.active {
  background: rgba(255, 36, 66, 0.18);
}

body[data-theme="dark"] .poster-actions {
  border-top-color: #303030;
}

/* 暗色主题 - 邀请有礼 */
body[data-theme="dark"] .console-invite-btn {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .console-invite-btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .invite-panel {
  background: #141414;
}

body[data-theme="dark"] .invite-header,
body[data-theme="dark"] .invite-code-card,
body[data-theme="dark"] .invite-link-card,
body[data-theme="dark"] .invite-progress-card,
body[data-theme="dark"] .invite-friend-card,
body[data-theme="dark"] .withdraw-panel {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .invite-title,
body[data-theme="dark"] .invite-progress-title,
body[data-theme="dark"] .invite-friend-title,
body[data-theme="dark"] .withdraw-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-user-id {
  background: #262626;
  border-color: #303030;
}

body[data-theme="dark"] .invite-user-id:hover {
  background: rgba(255, 36, 66, 0.15);
  border-color: rgba(255, 36, 66, 0.5);
}

body[data-theme="dark"] .invite-user-id-value {
  color: #ff4d6f;
}

body[data-theme="dark"] .invite-user-id:hover .invite-user-id-copy {
  color: #ff4d6f;
}

body[data-theme="dark"] .invite-stat-item,
body[data-theme="dark"] .invite-code-box {
  background: #262626;
}

body[data-theme="dark"] .invite-stats {
  background: #141414;
}

body[data-theme="dark"] .invite-progress-bar {
  background: #303030;
}

body[data-theme="dark"] .invite-rules {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .invite-rules-header {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .invite-rules-tag {
  background: rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .invite-rule-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-rule-text {
  color: #e8e8e8;
}

body[data-theme="dark"] .invite-rule-underline {
  color: #e0e0e0;
  text-decoration-color: #e0e0e0;
}

body[data-theme="dark"] .invite-rules-detail-btn {
  background: #262626;
  border-color: rgba(255, 36, 66, 0.35);
}

body[data-theme="dark"] .invite-rules-detail-btn:hover {
  background: rgba(255, 36, 66, 0.18);
}

body[data-theme="dark"] .invite-rules-detail {
  background: #1f1f1f;
}

body[data-theme="dark"] .invite-rules-detail-header {
  border-color: #303030;
}

body[data-theme="dark"] .invite-rules-detail-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-rules-detail-close:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .invite-rules-detail-section + .invite-rules-detail-section {
  border-top-color: #303030;
}

body[data-theme="dark"] .invite-rules-detail-list li {
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-rules-detail-list li::before {
  background: #ff4d6f;
}

body[data-theme="dark"] .invite-rules-detail-heading {
  color: #ff4d6f;
}

body[data-theme="dark"] .invite-rules-detail-heading::before {
  background: #ff4d6f;
}

body[data-theme="dark"] .invite-rules-detail-callout {
  background: rgba(255, 36, 66, 0.12);
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-rules-detail-callout b {
  color: #ff4d6f;
}

body[data-theme="dark"] .invite-stat-label,
body[data-theme="dark"] .invite-code-label,
body[data-theme="dark"] .invite-progress-desc,
body[data-theme="dark"] .invite-progress-text,
body[data-theme="dark"] .invite-simulate-label,
body[data-theme="dark"] .withdraw-label,
body[data-theme="dark"] .withdraw-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .invite-code-value,
body[data-theme="dark"] .invite-link-value,
body[data-theme="dark"] .invite-friend-email {
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-friend-empty {
  color: #666;
}

body[data-theme="dark"] .invite-friend-item {
  border-color: #303030;
}

body[data-theme="dark"] .invite-friend-status.registered {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme="dark"] .invite-friend-status.purchased {
  background: #2b2111;
  color: #ffa940;
}

body[data-theme="dark"] .invite-form-input,
body[data-theme="dark"] .withdraw-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-form-input:focus,
body[data-theme="dark"] .withdraw-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .invite-btn-secondary {
  background: #1f1f1f;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .invite-btn-secondary:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .invite-btn:disabled {
  background: #262626;
  color: #666;
  border-color: #404040;
}

/* ========== 兑换码弹框 ========== */
.redeem-modal .ant-modal-body {
  padding: 0;
}

.redeem-panel {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
}

.redeem-header {
  text-align: center;
  margin-bottom: 20px;
}

.redeem-title {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.redeem-subtitle {
  display: block;
  font-size: 13px;
  color: #8c8c8c;
}

.redeem-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 10px;
  font-size: 15px;
  color: #1a1a1a;
  letter-spacing: 1px;
  text-transform: uppercase;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.redeem-input::placeholder {
  text-transform: none;
  color: #bfbfbf;
}

.redeem-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.redeem-status {
  margin-top: 14px;
  font-size: 13px;
  text-align: center;
  min-height: 20px;
}

.redeem-status.error {
  color: #ff4d4f;
}

.redeem-status.success {
  color: #07c160;
}

.redeem-submit {
  width: 100%;
  margin-top: 14px;
  padding: 12px;
  font-size: 15px;
}

body[data-theme="dark"] .redeem-panel {
  background: #1f1f1f;
  border-radius: 8px;
}

body[data-theme="dark"] .redeem-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .redeem-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .redeem-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .redeem-input:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .redeem-status.error {
  color: #ff7875;
}

body[data-theme="dark"] .redeem-status.success {
  color: #10b981;
}

body[data-theme="dark"] .redeem-submit {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .redeem-submit:hover:not(:disabled) {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .profile-submit,
body[data-theme="dark"] .email-submit {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .profile-submit:hover,
body[data-theme="dark"] .email-submit:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

/* 移动端：App-style 布局
   - 隐藏左侧侧边栏（功能内聚到 /console/mine 的 "我的" 页）
   - 隐藏右侧 header 的图标按钮（功能也内聚到 "我的"）
   - 显示底部 TabBar（创作 / 作品 / 排行榜 / 我的）
   - 主内容区底部留出 tabbar 高度的空间
*/
.console-tabbar {
  display: none;
}

@media (max-width: 768px) {
  .console-layout {
    flex-direction: column;
  }

  /* 隐藏侧边栏 */
  .console-sidebar {
    display: none;
  }

  /* 主内容区占满宽度，底部留出 tabbar + 安全区高度 */
  .console-main {
    width: 100%;
    min-height: 100vh;
    padding-bottom: calc(60px + env(safe-area-inset-bottom));
  }

  /* 简化 header：隐藏所有图标按钮 / 头像 / 会员徽章
     各页面（特别是 MineIndex）自带所需 UI */
  .console-header {
    height: 0;
    min-height: 0;
    padding: 0;
    border-bottom: none;
    background: transparent;
    overflow: hidden;
  }

  .console-header .header-right > * {
    display: none !important;
  }

  /* 手机端子页面顶部返回栏 */
  .mobile-subpage-header {
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    height: 48px;
    padding: 0 12px;
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    z-index: 40;
  }

  .mobile-subpage-back {
    position: absolute;
    left: 12px;
    display: flex;
    align-items: center;
    gap: 2px;
    color: #595959;
    font-size: 14px;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
  }

  .mobile-subpage-back svg {
    width: 20px;
    height: 20px;
  }

  .mobile-subpage-title {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
  }

  /* 移动端不再显示新手横幅（MineIndex 顶部已有引导卡） */
  .guide-banner {
    margin: 8px 12px 0;
    border-radius: 10px;
    padding: 10px 14px;
    font-size: 12px;
  }

  /* 移动端隐藏底部 footer（TabBar 已经占位） */
  .console-footer {
    display: none;
  }

  .console-content {
    padding: 0;
    min-height: auto;
    overflow: hidden;
  }

  /* ============ 底部 TabBar ============ */
  .console-tabbar {
    display: flex;
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    height: calc(60px + env(safe-area-inset-bottom));
    padding-bottom: env(safe-area-inset-bottom);
    background: #fff;
    border-top: 1px solid #f0f0f0;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.04);
    z-index: 50;
  }

  .console-tabbar.tabbar-hidden {
    display: none;
  }

  .console-tabbar-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    color: #8c8c8c;
    text-decoration: none;
    transition: color 0.2s;
    -webkit-tap-highlight-color: transparent;
  }

  .console-tabbar-icon {
    font-size: 22px;
    line-height: 1;
  }

  .console-tabbar-label {
    font-size: 11px;
    line-height: 1;
  }

  .console-tabbar-item.active {
    color: #FF2442;
  }
}

/* 暗色主题 tabbar */
body[data-theme="dark"] .console-tabbar {
  background: #1f1f1f;
  border-top-color: #303030;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .console-tabbar-item {
  color: #a6a6a6;
}

body[data-theme="dark"] .console-tabbar-item.active {
  color: #ff4d6f;
}

body[data-theme="dark"] .mobile-subpage-header {
  background: #1f1f1f;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .mobile-subpage-back {
  color: #a6a6a6;
}

body[data-theme="dark"] .mobile-subpage-title {
  color: #e0e0e0;
}
</style>

<style>
/* 消息中心弹框 - ant-modal-body 由 ant-design-vue 生成在 scoped 块外,
   必须放在全局块里才能命中:notif-detail-modal/renewal-modal 类挂在 .ant-modal 上,
   ant-modal-body 是它的后代。 */
.notif-detail-modal .ant-modal-body {
  max-height: 70vh;
  overflow-y: auto;
  padding: 24px 28px 8px;
}

/* 续订弹框:滚动放在 .renewal-body 内部(不是 .ant-modal-body)。 */
.renewal-modal .ant-modal-body {
  padding: 24px 28px 8px;
}

/* 消息详情 / 续订弹框 primary 按钮统一用品牌红(#FF2442,小红书/爱创作主色),
   ant-design-vue 默认 primary 是蓝色,这里显式覆盖。 */
.notif-detail-modal .ant-btn-primary,
.renewal-modal .ant-btn-primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.notif-detail-modal .ant-btn-primary:hover,
.renewal-modal .ant-btn-primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #fff;
}

/* 邀请有礼 - 通用按钮样式（全局，供其他页面复用） */
.invite-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  line-height: 1.2;
}

.invite-btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.invite-btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.invite-btn-secondary {
  background: #fff;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.invite-btn-secondary:hover {
  background: var(--color-primary-light);
}

.invite-btn:disabled {
  background: #f5f5f5;
  color: #bfbfbf;
  cursor: not-allowed;
  border-color: transparent;
}

body[data-theme="dark"] .invite-btn:disabled {
  background: #262626;
  color: #666;
  border-color: #404040;
}

/*
 * 暗色主题 - Ant 弹层外壳适配（全局，非 scoped）
 * 原因：a-modal / a-drawer 的内容被 teleport 到 body，其 .ant-modal-content
 * / .ant-drawer-content 由 Ant 生成、不带组件 scope 属性，scoped 样式无法命中，
 * 暗色下会保留 #fff 白底。这里在全局块内按主题覆盖其外壳背景。
 */
body[data-theme="dark"] .invite-modal .ant-modal-content,
body[data-theme="dark"] .poster-modal .ant-modal-content,
body[data-theme="dark"] .notif-modal .ant-modal-content,
body[data-theme="dark"] .tutorial-modal .ant-modal-content,
body[data-theme="dark"] .feedback-modal .ant-modal-content,
body[data-theme="dark"] .about-modal .ant-modal-content,
body[data-theme="dark"] .profile-modal .ant-modal-content,
body[data-theme="dark"] .email-modal .ant-modal-content,
body[data-theme="dark"] .redeem-modal .ant-modal-content,
body[data-theme="dark"] .password-modal .ant-modal-content,
body[data-theme="dark"] .invite-binding-modal .ant-modal-content,
body[data-theme="dark"] .email-slider-modal .ant-modal-content,
body[data-theme="dark"] .withdraw-modal .ant-modal-content,
body[data-theme="dark"] .notif-detail-modal .ant-modal-content,
body[data-theme="dark"] .renewal-modal .ant-modal-content {
  background: #141414;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}

body[data-theme="dark"] .invite-modal-auto-footer {
  background: #141414;
  border-top-color: #2c2c2c;
}

body[data-theme="dark"] .invite-modal-auto-footer .ant-checkbox-wrapper {
  color: #a6a6a6;
}

/* 消息中心：去掉弹框内容区默认 padding，并把标题头背景也改成和面板一致，避免暗色下出现异色外圈 */
body[data-theme="dark"] .notif-modal .ant-modal-content {
  padding: 0;
}

body[data-theme="dark"] .notif-modal .ant-modal-header {
  background: #1f1f1f;
  border-bottom-color: #303030;
}

/* 教程 / 反馈 / 关于我们：同样处理 */
body[data-theme="dark"] .tutorial-modal .ant-modal-content,
body[data-theme="dark"] .feedback-modal .ant-modal-content,
body[data-theme="dark"] .about-modal .ant-modal-content,
body[data-theme="dark"] .redeem-modal .ant-modal-content {
  padding: 0;
}

body[data-theme="dark"] .tutorial-modal .ant-modal-header,
body[data-theme="dark"] .feedback-modal .ant-modal-header,
body[data-theme="dark"] .about-modal .ant-modal-header,
body[data-theme="dark"] .redeem-modal .ant-modal-header {
  background: #1f1f1f;
  border-bottom-color: #303030;
}

/* 消息详情 / 续订弹框：暗色样式 */
body[data-theme="dark"] .notif-detail-modal .ant-modal-content,
body[data-theme="dark"] .renewal-modal .ant-modal-content {
  background: #1f1f1f;
  border: 1px solid #303030;
}

body[data-theme="dark"] .notif-detail-title,
body[data-theme="dark"] .renewal-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .notif-detail-time,
body[data-theme="dark"] .notif-detail-content {
  color: #d9d9d9;
}

body[data-theme="dark"] .notif-detail-type-chip {
  background: rgba(255, 77, 111, 0.18);
  color: #ff4d6f;
}

body[data-theme="dark"] .renewal-body {
  background: #262626;
  color: #d9d9d9;
  border-left-color: #ff4d6f;
}

body[data-theme="dark"] .invite-modal .ant-modal-close,
body[data-theme="dark"] .poster-modal .ant-modal-close,
body[data-theme="dark"] .notif-modal .ant-modal-close,
body[data-theme="dark"] .tutorial-modal .ant-modal-close,
body[data-theme="dark"] .feedback-modal .ant-modal-close,
body[data-theme="dark"] .about-modal .ant-modal-close,
body[data-theme="dark"] .profile-modal .ant-modal-close,
body[data-theme="dark"] .email-modal .ant-modal-close,
body[data-theme="dark"] .invite-binding-modal .ant-modal-close,
body[data-theme="dark"] .redeem-modal .ant-modal-close,
body[data-theme="dark"] .password-modal .ant-modal-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .invite-modal .ant-modal-close:hover,
body[data-theme="dark"] .poster-modal .ant-modal-close:hover,
body[data-theme="dark"] .notif-modal .ant-modal-close:hover,
body[data-theme="dark"] .tutorial-modal .ant-modal-close:hover,
body[data-theme="dark"] .feedback-modal .ant-modal-close:hover,
body[data-theme="dark"] .about-modal .ant-modal-close:hover,
body[data-theme="dark"] .profile-modal .ant-modal-close:hover,
body[data-theme="dark"] .email-modal .ant-modal-close:hover,
body[data-theme="dark"] .invite-binding-modal .ant-modal-close:hover,
body[data-theme="dark"] .email-slider-modal .ant-modal-close:hover,
body[data-theme="dark"] .redeem-modal .ant-modal-close:hover,
body[data-theme="dark"] .password-modal .ant-modal-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

/* 修改昵称 / 修改邮箱 / 修改密码 / 绑定邀请人 的 Ant 标题头在暗色下需改为深底 */
body[data-theme="dark"] .profile-modal .ant-modal-header,
body[data-theme="dark"] .email-modal .ant-modal-header,
body[data-theme="dark"] .password-modal .ant-modal-header,
body[data-theme="dark"] .invite-binding-modal .ant-modal-header,
body[data-theme="dark"] .email-slider-modal .ant-modal-header {
  background: #141414;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .profile-modal .ant-modal-title,
body[data-theme="dark"] .email-modal .ant-modal-title,
body[data-theme="dark"] .password-modal .ant-modal-title,
body[data-theme="dark"] .invite-binding-modal .ant-modal-title,
body[data-theme="dark"] .email-slider-modal .ant-modal-title {
  color: #e0e0e0;
}

/* 用户协议 / 隐私政策弹框暗色主题 */
body[data-theme="dark"] .legal-modal .ant-modal-content {
  background: #141414;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}

body[data-theme="dark"] .legal-modal .ant-modal-header {
  background: #141414;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .legal-modal .ant-modal-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .legal-modal .ant-modal-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .legal-modal .ant-modal-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

body[data-theme="dark"] .terms-content {
  color: #a6a6a6;
}

body[data-theme="dark"] .terms-content h4 {
  color: #f0f0f0;
}

body[data-theme="dark"] .terms-content::-webkit-scrollbar {
  width: 4px;
}

body[data-theme="dark"] .terms-content::-webkit-scrollbar-thumb {
  background: #434343;
  border-radius: 2px;
}

/* 客服微信弹框暗色主题 */
body[data-theme="dark"] .wechat-modal .ant-modal-content {
  background: #141414;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}

body[data-theme="dark"] .wechat-modal .ant-modal-header {
  background: #141414;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .wechat-modal .ant-modal-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .wechat-modal .ant-modal-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .wechat-modal .ant-modal-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

body[data-theme="dark"] .wechat-modal-content .wechat-hint {
  color: #a6a6a6;
}

/* 注意：class="invite-rules-drawer" 直接挂在 .ant-drawer-content 上（同一元素），
 * 所以必须用复合选择器 .invite-rules-drawer.ant-drawer-content，而非后代选择器。 */
body[data-theme="dark"] .invite-rules-drawer.ant-drawer-content {
  background: #1f1f1f;
}
</style>

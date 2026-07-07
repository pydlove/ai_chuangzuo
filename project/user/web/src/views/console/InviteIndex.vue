<template>
  <div class="invite-page">
    <!-- 顶部头部 -->
    <div class="invite-header">
      <span class="invite-title">🎁 邀请有礼</span>
      <a-tooltip title="点击复制 ID">
        <button class="invite-user-id" @click="copyUserId">
          <span class="invite-user-id-label">我的 ID</span>
          <b class="invite-user-id-value">{{ userId }}</b>
        </button>
      </a-tooltip>
    </div>

    <!-- 统计卡片 -->
    <div class="invite-stats">
      <div class="invite-stat-item">
        <div class="invite-stat-value">{{ stats.invitedCount }}</div>
        <div class="invite-stat-label">已邀请</div>
      </div>
      <div class="invite-stat-item">
        <div class="invite-stat-value">{{ stats.membershipDaysEarned }}</div>
        <div class="invite-stat-label">奖励会员天数</div>
      </div>
      <div class="invite-stat-item invite-stat-item-coin">
        <div class="invite-stat-value">{{ coinBalance }}</div>
        <div class="invite-stat-label">创作币余额</div>
      </div>
    </div>

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
          推荐新客下单即获得奖励，一次邀请终身享受订单返佣红利。好友首次购买返 10%，续费返 5%。
        </span>
      </div>
      <div class="invite-rule-item">
        <span class="invite-rule-label">🌱 新用户福利</span>
        <span class="invite-rule-text">新用户通过你的邀请码注册，立刻获得 5 创作币。</span>
      </div>
      <button class="invite-rules-detail-btn" @click="$router.push('/console/invite-rules')">
        <span>查看完整活动规则</span>
        <span class="invite-rules-detail-arrow">›</span>
      </button>
    </div>

    <!-- 邀请链接 -->
    <div class="invite-link-card">
      <div class="invite-code-label">邀请链接</div>
      <div class="invite-link-value">{{ inviteLink }}</div>
      <div class="invite-link-actions">
        <button class="invite-btn invite-btn-secondary" @click="copyInviteLink">复制链接</button>
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
          <div class="invite-progress-fill" :style="{ width: Math.min(100, (stats.invitedCount / 3) * 100) + '%' }"></div>
        </div>
        <div class="invite-progress-text">
          {{ stats.invitedCount >= 3 ? '+3 天' : `${stats.invitedCount}/3` }}
        </div>
      </div>
      <div class="invite-progress-item">
        <div class="invite-progress-bar">
          <div class="invite-progress-fill" :style="{ width: Math.min(100, (stats.invitedCount / 5) * 100) + '%' }"></div>
        </div>
        <div class="invite-progress-text">
          {{ stats.invitedCount >= 5 ? '+5 天' : `${stats.invitedCount}/5` }}
        </div>
      </div>
      <div class="invite-progress-item">
        <div class="invite-progress-desc">超过 5 人后，每多 1 人 +2 天专业版会员</div>
        <div class="invite-progress-text">
          {{ stats.invitedCount > 5 ? `+${(stats.invitedCount - 5) * 2} 天` : '—' }}
        </div>
      </div>
    </div>

    <!-- 邀请记录 -->
    <div class="invite-friend-card">
      <div class="invite-friend-header">
        <span class="invite-friend-title">邀请记录</span>
      </div>
      <div class="invite-friend-list">
        <div v-if="stats.friends.length === 0" class="invite-friend-empty">
          暂无邀请记录，快去分享邀请链接吧～
        </div>
        <div v-for="f in stats.friends" :key="f.email" class="invite-friend-item">
          <span class="invite-friend-email">{{ f.email }}</span>
          <span :class="['invite-friend-status', f.status]">
            {{ f.status === 'purchased' ? `已购买 +${f.commission} 币` : '已注册' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { message } from 'ant-design-vue'

const INVITE_CODE_KEY = 'aichuangzuo_invite_code'
const INVITE_STATS_KEY = 'aichuangzuo_invite_stats'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'

const userId = ref(localStorage.getItem('aichuangzuo_user_id') || '88886666')
const inviteCode = ref('')
const coinBalance = ref(0)

const stats = ref({
  invitedCount: 0,
  membershipDaysEarned: 0,
  coinEarned: 0,
  friends: []
})

const generateInviteCode = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

const loadInviteCode = () => {
  let code = localStorage.getItem(INVITE_CODE_KEY)
  if (!code) {
    code = generateInviteCode()
    localStorage.setItem(INVITE_CODE_KEY, code)
  }
  inviteCode.value = code
}

const loadStats = () => {
  const raw = localStorage.getItem(INVITE_STATS_KEY)
  if (raw) {
    try {
      stats.value = JSON.parse(raw)
    } catch {
      // ignore parse error
    }
  }
}

const loadCoinBalance = () => {
  const raw = localStorage.getItem(COIN_BALANCE_KEY)
  coinBalance.value = raw ? parseInt(raw, 10) : 0
}

onMounted(() => {
  loadInviteCode()
  loadStats()
  loadCoinBalance()
})

const inviteLink = computed(() => {
  return `${window.location.origin}/login?ref=${inviteCode.value}`
})

const copyUserId = () => {
  navigator.clipboard.writeText(userId.value).then(() => {
    message.success('ID 已复制')
  })
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
</script>

<style scoped>
.invite-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 16px 32px;
}

.invite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.invite-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.invite-user-id {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff5f7;
  border: 1px solid #ffd1d9;
  border-radius: 20px;
  font-size: 12px;
  color: #ff2442;
  cursor: pointer;
  transition: all 0.2s;
}

.invite-user-id:hover {
  background: #ffe4ea;
}

.invite-user-id-label {
  color: #8c8c8c;
  font-size: 11px;
}

.invite-user-id-value {
  color: #ff2442;
  font-weight: 700;
  font-size: 13px;
}

.invite-stats {
  display: flex;
  background: #fff;
  border-radius: 14px;
  padding: 18px 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.invite-stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.invite-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.invite-stat-item-coin .invite-stat-value {
  color: #FF2442;
}

.invite-stat-label {
  font-size: 12px;
  color: #8c8c8c;
}

.invite-rules {
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.invite-rules-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.invite-rules-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.invite-rules-tag {
  font-size: 11px;
  color: #ff2442;
  background: #fff0f2;
  padding: 2px 8px;
  border-radius: 10px;
}

.invite-rule-item {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
}

.invite-rule-item:last-child {
  border-bottom: none;
}

.invite-rule-label {
  flex-shrink: 0;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 100px;
}

.invite-rule-text {
  flex: 1;
}

.invite-rules-detail-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 10px;
  padding: 10px 14px;
  background: #fff5f7;
  border: 1px dashed #ffd1d9;
  border-radius: 10px;
  font-size: 13px;
  color: #ff2442;
  cursor: pointer;
  transition: all 0.2s;
}

.invite-rules-detail-btn:hover {
  background: #ffe4ea;
}

.invite-rules-detail-arrow {
  font-size: 16px;
  line-height: 1;
}

.invite-link-card,
.invite-code-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.invite-code-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.invite-link-value {
  font-size: 13px;
  color: #1a1a1a;
  word-break: break-all;
  background: #f5f5f5;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 12px;
  font-family: 'SF Mono', Consolas, monospace;
}

.invite-code-box {
  margin-bottom: 12px;
}

.invite-code-value {
  font-size: 24px;
  font-weight: 700;
  color: #ff2442;
  letter-spacing: 2px;
  font-family: 'SF Mono', Consolas, monospace;
  text-align: center;
  padding: 12px;
  background: #fff5f7;
  border: 2px dashed #ffd1d9;
  border-radius: 10px;
}

.invite-link-actions {
  display: flex;
  gap: 10px;
}

.invite-btn {
  flex: 1;
  padding: 12px 18px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.invite-btn-secondary {
  background: #fff;
  color: #ff2442;
  border: 1px solid #ff2442;
}

.invite-btn-secondary:hover {
  background: #fff5f7;
}

.invite-btn-primary {
  background: #ff2442;
  color: #fff;
}

.invite-btn-primary:hover {
  background: #e61e3a;
}

.invite-progress-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.invite-progress-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 14px;
}

.invite-progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #595959;
}

.invite-progress-item:last-child {
  margin-bottom: 0;
}

.invite-progress-bar {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.invite-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff4d4f 0%, #FF2442 100%);
  border-radius: 4px;
  transition: width 0.4s;
}

.invite-progress-text {
  flex-shrink: 0;
  min-width: 60px;
  text-align: right;
  font-weight: 600;
  color: #ff2442;
}

.invite-progress-desc {
  flex: 1;
  font-size: 12px;
  color: #8c8c8c;
}

.invite-friend-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.invite-friend-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.invite-friend-empty {
  padding: 32px 0;
  text-align: center;
  font-size: 13px;
  color: #8c8c8c;
}

.invite-friend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
}

.invite-friend-item:last-child {
  border-bottom: none;
}

.invite-friend-email {
  color: #1a1a1a;
}

.invite-friend-status {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 10px;
}

.invite-friend-status.registered {
  background: #f5f5f5;
  color: #8c8c8c;
}

.invite-friend-status.purchased {
  background: #fff1f0;
  color: #cf1322;
}

@media (max-width: 768px) {
  .invite-page {
    padding: 16px 12px 24px;
  }

  .invite-title {
    font-size: 20px;
  }

  .invite-rule-label {
    min-width: 84px;
  }

  .invite-link-actions {
    flex-direction: column;
  }
}

/* 暗色主题 */
body[data-theme="dark"] .invite-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-stats,
body[data-theme="dark"] .invite-rules,
body[data-theme="dark"] .invite-link-card,
body[data-theme="dark"] .invite-code-card,
body[data-theme="dark"] .invite-progress-card,
body[data-theme="dark"] .invite-friend-card {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .invite-stat-value,
body[data-theme="dark"] .invite-rules-title,
body[data-theme="dark"] .invite-progress-title,
body[data-theme="dark"] .invite-friend-title,
body[data-theme="dark"] .invite-friend-email {
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-rule-item,
body[data-theme="dark"] .invite-progress-item {
  color: #a6a6a6;
  border-bottom-color: #262626;
}

body[data-theme="dark"] .invite-rule-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-link-value {
  background: #262626;
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-code-value {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.35);
}

body[data-theme="dark"] .invite-btn-secondary {
  background: #1f1f1f;
}

body[data-theme="dark"] .invite-progress-bar {
  background: #303030;
}

body[data-theme="dark"] .invite-friend-item {
  border-bottom-color: #262626;
}

body[data-theme="dark"] .invite-friend-status.registered {
  background: #262626;
}

body[data-theme="dark"] .invite-user-id {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.35);
}

body[data-theme="dark"] .invite-rules-detail-btn {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 36, 66, 0.3);
}

body[data-theme="dark"] .invite-rules-detail-btn:hover {
  background: rgba(255, 36, 66, 0.15);
}
</style>
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
        <div class="invite-stat-value">{{ inviteStats.invitedCount }}</div>
        <div class="invite-stat-label">已邀请</div>
      </div>
      <div class="invite-stat-item">
        <div class="invite-stat-value">{{ inviteStats.inviteCoinEarned }}</div>
        <div class="invite-stat-label">奖励创作币</div>
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
        <span class="invite-rule-text">累计邀请 3 人 +30 创作币、5 人 +50，超过 5 人后每多 1 人 +20 创作币。</span>
      </div>
      <div class="invite-rule-item">
        <span class="invite-rule-label">💰 创作币返利</span>
        <span class="invite-rule-text">
          推荐新客下单即获得奖励，一次邀请终身享受订单返佣红利。好友首次购买返 10%，续费返 5%。
        </span>
      </div>
      <div class="invite-rule-item">
        <span class="invite-rule-label">🌱 新用户福利</span>
        <span class="invite-rule-text">新用户通过你的邀请码注册，立刻获得 50 创作币。</span>
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
        <button class="invite-btn invite-btn-primary" @click="downloadPoster">下载海报</button>
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
          {{ inviteStats.invitedCount >= 3 ? '+30 币' : `${inviteStats.invitedCount}/3` }}
        </div>
      </div>
      <div class="invite-progress-item">
        <div class="invite-progress-bar">
          <div class="invite-progress-fill" :style="{ width: Math.min(100, (inviteStats.invitedCount / 5) * 100) + '%' }"></div>
        </div>
        <div class="invite-progress-text">
          {{ inviteStats.invitedCount >= 5 ? '+50 币' : `${inviteStats.invitedCount}/5` }}
        </div>
      </div>
      <div class="invite-progress-item">
        <div class="invite-progress-desc">超过 5 人后，每多 1 人 +20 创作币</div>
        <div class="invite-progress-text">
          {{ inviteStats.invitedCount > 5 ? `+${(inviteStats.invitedCount - 5) * 20} 币` : '—' }}
        </div>
      </div>
    </div>

    <!-- 邀请记录 -->
    <div class="invite-friend-card">
      <div class="invite-friend-header">
        <span class="invite-friend-title">邀请记录</span>
      </div>
      <div class="invite-friend-list">
        <div v-if="inviteStats.friends.length === 0" class="invite-friend-empty">
          暂无邀请记录，快去分享邀请链接吧～
        </div>
        <div v-for="f in inviteStats.friends" :key="f.email" class="invite-friend-item">
          <span class="invite-friend-email">{{ f.email }}</span>
          <span :class="['invite-friend-status', f.status]">
            {{ f.status === 'purchased' ? `已购买 +${f.commission} 币` : '已注册' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 微信环境海报保存提示弹框 -->
    <a-modal
      v-model:open="posterPreviewVisible"
      :footer="null"
      :width="400"
      centered
      class="poster-preview-modal"
    >
      <div class="poster-preview-panel">
        <div class="poster-preview-tip">
          <span class="poster-preview-tip-icon">💡</span>
          <span>长按下方图片，选择“保存到手机”</span>
        </div>
        <div class="poster-preview-image-wrap">
          <img
            v-if="posterPreviewUrl"
            :src="posterPreviewUrl"
            alt="邀请海报"
            class="poster-preview-image"
          />
        </div>
        <button class="invite-btn invite-btn-primary poster-preview-close" @click="posterPreviewVisible = false">
          知道了
        </button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import QRCode from 'qrcode'
import { useInviteStats } from '@/composables/useInviteStats.js'
import { useUserProfile } from '@/composables/useUserProfile.js'
import { copyToClipboard } from '@/utils/copy.js'
import { isWechatBrowser } from '@/utils/env.js'
import { getShareConfig } from '@/api/shareConfig.js'

const { inviteStats, coinBalance, loadInviteStats } = useInviteStats()
const { profile, loadProfile } = useUserProfile()

const posterPreviewVisible = ref(false)
const posterPreviewUrl = ref('')
const shareConfig = ref(null)

const userId = computed(() => profile.value?.userId || '')
const inviteCode = computed(() => profile.value?.inviteCode || '')

onMounted(() => {
  loadProfile()
  loadInviteStats()
  loadShareConfig()
})

const loadShareConfig = async () => {
  try {
    const res = await getShareConfig('invite')
    shareConfig.value = res.data
  } catch (e) {
    // ignore
  }
}

const inviteLink = computed(() => {
  return `https://www.ichuang.top/login?ref=${inviteCode.value}`
})

const inviteShareText = computed(() => {
  const url = inviteLink.value
  const code = inviteCode.value
  let text = shareConfig.value?.content
  if (!text) {
    text = `推荐你一个 AI 创作神器「爱创作」，注册即送 50 创作币，写公众号/小红书/头条都超方便！快来试试：\n{url}`
  }
  return text.replace(/{url}/g, url).replace(/{code}/g, code)
})

const copyUserId = async () => {
  try {
    await copyToClipboard(userId.value)
    message.success('ID 已复制')
  } catch {
    message.error('复制失败，请长按手动复制')
  }
}

const copyInviteCode = async () => {
  try {
    await copyToClipboard(inviteCode.value)
    message.success('邀请码已复制')
  } catch {
    message.error('复制失败，请长按手动复制')
  }
}

const copyInviteLink = async () => {
  try {
    await copyToClipboard(inviteShareText.value)
    message.success('邀请文案已复制')
  } catch {
    message.error('复制失败，请长按手动复制')
  }
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

const downloadPoster = async () => {
  const canvas = document.createElement('canvas')
  const W = 750
  const H = 1200
  canvas.width = W
  canvas.height = H
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 背景
  const gradient = ctx.createLinearGradient(0, 0, W, H)
  gradient.addColorStop(0, '#fff5f7')
  gradient.addColorStop(1, '#ffffff')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, W, H)

  // 装饰
  ctx.fillStyle = 'rgba(255, 36, 66, 0.06)'
  ctx.beginPath()
  ctx.arc(W - 80, 120, 160, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(80, H - 120, 120, 0, Math.PI * 2)
  ctx.fill()

  // 品牌名
  ctx.textAlign = 'center'
  ctx.fillStyle = '#ff2442'
  ctx.font = 'bold 70px sans-serif'
  ctx.fillText('爱创作', W / 2, 220)

  ctx.fillStyle = '#595959'
  ctx.font = '36px sans-serif'
  ctx.fillText('邀请你一起 AI 创作', W / 2, 290)

  // 主标题
  ctx.fillStyle = '#1a1a1a'
  ctx.font = 'bold 52px sans-serif'
  ctx.fillText('一次邀请 终身返佣', W / 2, 420)

  // 邀请码卡片
  const cardX = 110
  const cardY = 500
  const cardW = 530
  const cardH = 200
  ctx.fillStyle = '#ffffff'
  roundRect(ctx, cardX, cardY, cardW, cardH, 16)
  ctx.fill()
  ctx.strokeStyle = '#ffd1d9'
  ctx.lineWidth = 3
  ctx.stroke()

  ctx.fillStyle = '#8c8c8c'
  ctx.font = '34px sans-serif'
  ctx.fillText('我的邀请码', W / 2, cardY + 70)

  ctx.fillStyle = '#ff2442'
  ctx.font = 'bold 80px sans-serif'
  ctx.fillText(inviteCode.value || '', W / 2, cardY + 155)

  // 二维码
  const qrSize = 300
  const qrX = (W - qrSize) / 2
  const qrY = 760
  try {
    const qrDataUrl = await QRCode.toDataURL(inviteLink.value, {
      errorCorrectionLevel: 'H',
      margin: 1,
      width: 320,
      color: { dark: '#1a1a1a', light: '#ffffff' }
    })
    const qrImg = new Image()
    await new Promise((resolve) => {
      qrImg.onload = resolve
      qrImg.onerror = resolve
      qrImg.src = qrDataUrl
    })
    ctx.fillStyle = '#ffffff'
    roundRect(ctx, qrX - 16, qrY - 16, qrSize + 32, qrSize + 32, 16)
    ctx.fill()
    ctx.drawImage(qrImg, qrX, qrY, qrSize, qrSize)
  } catch (e) {
    message.error('二维码生成失败')
    return
  }

  // 底部文案
  ctx.fillStyle = '#1a1a1a'
  ctx.font = 'bold 38px sans-serif'
  ctx.fillText('扫码 / 输入邀请码 立即加入', W / 2, 1140)

  ctx.fillStyle = '#ff2442'
  ctx.font = '32px sans-serif'
  ctx.fillText('注册即得 50 创作币 + 阶梯奖励', W / 2, 1190)

  // 下载或预览
  const dataUrl = canvas.toDataURL('image/png')
  if (isWechatBrowser()) {
    posterPreviewUrl.value = dataUrl
    posterPreviewVisible.value = true
    return
  }

  const link = document.createElement('a')
  link.download = `爱创作邀请海报-${inviteCode.value || 'invite'}.png`
  link.href = dataUrl
  link.click()
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

/* 微信环境海报预览弹框 */
.poster-preview-modal .ant-modal-body {
  padding: 0;
}

.poster-preview-panel {
  padding: 20px;
  text-align: center;
}

.poster-preview-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  color: #595959;
  margin-bottom: 16px;
}

.poster-preview-tip-icon {
  font-size: 16px;
}

.poster-preview-image-wrap {
  width: 100%;
  max-height: 460px;
  overflow: auto;
  border-radius: 12px;
  background: #f5f5f5;
  margin-bottom: 16px;
}

.poster-preview-image {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 12px;
  -webkit-touch-callout: default;
  user-select: none;
}

.poster-preview-close {
  width: 100%;
}

body[data-theme="dark"] .poster-preview-panel {
  background: #1f1f1f;
}

body[data-theme="dark"] .poster-preview-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .poster-preview-image-wrap {
  background: #141414;
}
</style>
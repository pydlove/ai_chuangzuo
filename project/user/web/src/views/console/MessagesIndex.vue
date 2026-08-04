<template>
  <div class="messages-page">
    <!-- 顶部栏 -->
    <header class="messages-header">
      <div class="messages-header__main">
        <h1 class="messages-header__title">
          消息中心
          <span v-if="unreadCount > 0" class="messages-header__count">{{ unreadCount }}</span>
        </h1>
        <button
          v-if="unreadCount > 0"
          class="messages-header__readall"
          :disabled="markingAllRead"
          @click="markAllRead"
        >
          {{ markingAllRead ? '处理中…' : '全部已读' }}
        </button>
      </div>

      <!-- 类型筛选 -->
      <div class="messages-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.type"
          :class="['messages-tab', { active: activeTab === tab.type }]"
          @click="activeTab = tab.type"
        >
          {{ tab.label }}
          <span v-if="tab.count > 0" class="messages-tab__badge">{{ tab.count > 99 ? '99+' : tab.count }}</span>
        </button>
      </div>
    </header>

    <!-- 消息列表 -->
    <main class="messages-list">
      <div v-if="loading" class="messages-loading">
        <div v-for="i in 4" :key="i" class="message-skeleton">
          <div class="message-skeleton__icon"></div>
          <div class="message-skeleton__body">
            <div class="message-skeleton__title"></div>
            <div class="message-skeleton__summary"></div>
          </div>
        </div>
      </div>

      <div v-else-if="filteredMessages.length === 0" class="messages-empty">
        <div class="messages-empty__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
        </div>
        <div class="messages-empty__title">{{ emptyTitle }}</div>
        <div class="messages-empty__desc">有新活动或通知时，会第一时间出现在这里</div>
      </div>

      <div v-else class="messages-cards">
        <div
          v-for="msg in filteredMessages"
          :key="msg.id"
          :class="['message-card', { unread: !msg.read }]"
          @click="handleClick(msg)"
        >
          <div class="message-card__icon" :class="`message-card__icon--${msg.type}`">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" v-html="iconSvg(msg.type)"></svg>
          </div>

          <div class="message-card__body">
            <div class="message-card__head">
              <span class="message-card__title">{{ msg.title }}</span>
              <span class="message-card__time">{{ formatTimeAgo(msg.createdAt) }}</span>
            </div>
            <div class="message-card__summary">{{ msg.summary }}</div>
            <div class="message-card__meta">
              <span class="message-card__type">{{ typeLabel(msg.type) }}</span>
            </div>
          </div>

          <div v-if="!msg.read" class="message-card__unread"></div>
        </div>
      </div>
    </main>

    <!-- 消息详情弹框 -->
    <a-modal
      v-model:open="detailVisible"
      :footer="null"
      :width="520"
      centered
      class="message-detail-modal"
      :destroy-on-close="true"
    >
      <div v-if="detailMessage" class="message-detail">
        <div class="message-detail__icon" :class="`message-detail__icon--${detailMessage.type}`">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" v-html="iconSvg(detailMessage.type)"></svg>
        </div>
        <div class="message-detail__type">{{ typeLabel(detailMessage.type) }}</div>
        <h3 class="message-detail__title">{{ detailMessage.title }}</h3>
        <div class="message-detail__time">{{ formatTimeAgo(detailMessage.createdAt) }}</div>
        <div class="message-detail__content">{{ detailMessage.content || detailMessage.summary }}</div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message as antMessage } from 'ant-design-vue'
import { getMessages, markMessageRead, markAllMessagesRead } from '@/api/message'

const router = useRouter()

const messages = ref([])
const loading = ref(false)
const activeTab = ref('all')
const markingAllRead = ref(false)
const detailVisible = ref(false)
const detailMessage = ref(null)

const tabs = computed(() => [
  { type: 'all', label: '全部', count: unreadCountByType('all') },
  { type: 'announcement', label: '公告', count: unreadCountByType('announcement') },
  { type: 'generation', label: '创作', count: unreadCountByType('generation') },
  { type: 'membership', label: '会员', count: unreadCountByType('membership') }
])

const unreadCount = computed(() => messages.value.filter(m => !m.read).length)

const filteredMessages = computed(() => {
  if (activeTab.value === 'all') return messages.value
  return messages.value.filter(m => m.type === activeTab.value)
})

const emptyTitle = computed(() => {
  const map = {
    all: '暂无消息',
    announcement: '暂无公告',
    generation: '暂无创作通知',
    membership: '暂无会员消息'
  }
  return map[activeTab.value] || '暂无消息'
})

function unreadCountByType(type) {
  if (type === 'all') return unreadCount.value
  return messages.value.filter(m => m.type === type && !m.read).length
}

onMounted(loadMessages)

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessages()
    const list = (res.data || []).map((n) => {
      if (n.type === 'skill') n.type = 'style'
      return n
    })
    messages.value = list.sort((a, b) => {
      if (a.read !== b.read) return a.read ? 1 : -1
      return new Date(b.createdAt) - new Date(a.createdAt)
    })
  } catch (e) {
    messages.value = []
  } finally {
    loading.value = false
  }
}

async function markAllRead() {
  if (markingAllRead.value) return
  markingAllRead.value = true
  try {
    await markAllMessagesRead()
    messages.value.forEach(m => { m.read = true })
    antMessage.success('已全部标记为已读')
  } catch (e) {
    antMessage.error('操作失败，请重试')
  } finally {
    markingAllRead.value = false
  }
}

async function handleClick(msg) {
  if (!msg.read) {
    try {
      await markMessageRead(msg.id)
      msg.read = true
    } catch {
      // 即使失败也允许继续交互
    }
  }

  if (msg.link) {
    router.push(msg.link)
    return
  }
  if (msg.type === 'generation') {
    router.push('/console/works')
    return
  }
  detailMessage.value = msg
  detailVisible.value = true
}

function formatTimeAgo(iso) {
  if (!iso) return ''
  const date = new Date(iso)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)} 天前`
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function typeLabel(type) {
  const map = {
    announcement: '公告',
    feature: '功能',
    promotion: '活动',
    generation: '创作',
    membership: '会员',
    style: '提示词',
    reward: '奖励'
  }
  return map[type] || '通知'
}

function iconSvg(type) {
  const icons = {
    announcement: '<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>',
    feature: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>',
    promotion: '<polyline points="20 12 20 22 4 22 4 12"/><rect x="2" y="7" width="20" height="5"/><line x1="12" y1="22" x2="12" y2="7"/><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"/><path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"/>',
    generation: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>',
    membership: '<path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>',
    style: '<path d="M12 2a10 10 0 1 0 10 10H12V2z"/><path d="M12 2a10 10 0 0 1 10 10"/><path d="M12 12L2.5 6.5"/>',
    reward: '<circle cx="12" cy="8" r="7"/><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/>'
  }
  return icons[type] || icons.announcement
}
</script>

<style scoped>
.messages-page {
  min-height: 100%;
  background: #fafafa;
  box-sizing: border-box;
}

/* 顶部栏 */
.messages-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
  padding: 16px 16px 12px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}

.messages-header__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.messages-header__title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.messages-header__count {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: #ff2442;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.messages-header__readall {
  font-size: 13px;
  color: #595959;
  background: #f5f5f5;
  border: none;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.messages-header__readall:hover {
  color: #ff2442;
  background: #fff0f2;
}

.messages-header__readall:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 类型筛选 */
.messages-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  padding-bottom: 2px;
}

.messages-tabs::-webkit-scrollbar {
  display: none;
}

.messages-tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #595959;
  background: #f5f5f5;
  border: none;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.messages-tab.active {
  color: #fff;
  background: #ff2442;
  font-weight: 600;
}

.messages-tab__badge {
  font-size: 10px;
  font-weight: 600;
  color: #ff2442;
  background: #fff;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.messages-tab.active .messages-tab__badge {
  color: #ff2442;
  background: rgba(255, 255, 255, 0.9);
}

/* 消息列表 */
.messages-list {
  padding: 12px 16px 24px;
}

.messages-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
}

.message-card:active {
  transform: scale(0.99);
  background: #fafafa;
}

.message-card.unread {
  background: #fff9f9;
}

.message-card__icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.message-card__icon svg {
  width: 22px;
  height: 22px;
}

.message-card__icon--announcement,
.message-card__icon--feature,
.message-card__icon--promotion,
.message-card__icon--generation,
.message-card__icon--membership,
.message-card__icon--style,
.message-card__icon--reward {
  background: linear-gradient(135deg, #ff4d6f 0%, #ff2442 100%);
}

.message-card__body {
  flex: 1;
  min-width: 0;
}

.message-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.message-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}

.message-card.unread .message-card__title {
  color: #000;
}

.message-card__time {
  font-size: 12px;
  color: #bfbfbf;
  flex-shrink: 0;
}

.message-card__summary {
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.message-card__type {
  font-size: 11px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 2px 7px;
  border-radius: 999px;
}

.message-card__unread {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff2442;
  flex-shrink: 0;
  margin-top: 6px;
}

/* 加载骨架 */
.messages-loading {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-skeleton {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border-radius: 16px;
}

.message-skeleton__icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #f5f5f5;
  flex-shrink: 0;
}

.message-skeleton__body {
  flex: 1;
}

.message-skeleton__title {
  width: 40%;
  height: 15px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 10px;
}

.message-skeleton__summary {
  width: 70%;
  height: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

/* 空状态 */
.messages-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 72px 20px;
  text-align: center;
}

.messages-empty__icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #bfbfbf;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.messages-empty__icon svg {
  width: 30px;
  height: 30px;
}

.messages-empty__title {
  font-size: 16px;
  font-weight: 500;
  color: #595959;
  margin-bottom: 6px;
}

.messages-empty__desc {
  font-size: 13px;
  color: #8c8c8c;
}

/* 详情弹框 */
.message-detail {
  text-align: center;
  padding: 8px 8px 16px;
}

.message-detail__icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 14px;
}

.message-detail__icon svg {
  width: 28px;
  height: 28px;
}

.message-detail__icon--announcement,
.message-detail__icon--feature,
.message-detail__icon--promotion,
.message-detail__icon--generation,
.message-detail__icon--membership,
.message-detail__icon--style,
.message-detail__icon--reward {
  background: linear-gradient(135deg, #ff4d6f 0%, #ff2442 100%);
}

.message-detail__type {
  display: inline-block;
  font-size: 12px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 3px 10px;
  border-radius: 999px;
  margin-bottom: 10px;
}

.message-detail__title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 6px;
  line-height: 1.4;
}

.message-detail__time {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 18px;
}

.message-detail__content {
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
  text-align: left;
  white-space: pre-wrap;
}

/* 暗色主题 */
body[data-theme="dark"] .messages-page {
  background: #141414;
}

body[data-theme="dark"] .messages-header {
  background: #1f1f1f;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.05);
}

body[data-theme="dark"] .messages-header__title,
body[data-theme="dark"] .message-detail__title {
  color: #f5f5f5;
}

body[data-theme="dark"] .messages-header__readall,
body[data-theme="dark"] .messages-tab {
  color: #a6a6a6;
  background: #262626;
}

body[data-theme="dark"] .messages-header__readall:hover {
  color: #ff4d6f;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .messages-tab.active {
  color: #fff;
  background: #ff2442;
}

body[data-theme="dark"] .message-card,
body[data-theme="dark"] .message-skeleton {
  background: #1f1f1f;
}

body[data-theme="dark"] .message-card:active {
  background: #262626;
}

body[data-theme="dark"] .message-card.unread {
  background: rgba(255, 36, 66, 0.08);
}

body[data-theme="dark"] .message-card__title {
  color: #e0e0e0;
}

body[data-theme="dark"] .message-card.unread .message-card__title {
  color: #fff;
}

body[data-theme="dark"] .message-card__summary,
body[data-theme="dark"] .message-detail__content {
  color: #a6a6a6;
}

body[data-theme="dark"] .message-card__type,
body[data-theme="dark"] .message-detail__type {
  color: #8c8c8c;
  background: #262626;
}

body[data-theme="dark"] .messages-empty__icon,
body[data-theme="dark"] .message-skeleton__icon,
body[data-theme="dark"] .message-skeleton__title,
body[data-theme="dark"] .message-skeleton__summary {
  background: #262626;
}

body[data-theme="dark"] .messages-empty__title {
  color: #d9d9d9;
}

body[data-theme="dark"] .messages-empty__desc {
  color: #8c8c8c;
}

/* PC 端适配 */
@media (min-width: 769px) {
  .messages-page {
    max-width: 800px;
    margin: 0 auto;
  }

  .messages-header {
    padding: 24px 24px 16px;
    border-radius: 0 0 20px 20px;
  }

  .messages-list {
    padding: 20px 24px 32px;
  }
}
</style>

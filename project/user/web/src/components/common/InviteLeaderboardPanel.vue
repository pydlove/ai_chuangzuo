<template>
  <div class="invite-rank-panel">
    <div v-if="loading" class="invite-rank-empty">榜单加载中…</div>
    <template v-else>
      <div v-if="list.length === 0" class="invite-rank-empty">
        暂无榜单数据，快去邀请好友上榜吧～
      </div>

      <!-- TOP3 领奖台 -->
      <div v-if="top3.length" class="invite-rank-podium">
        <div v-if="top3[1]" class="podium-card podium-card--second">
          <div class="podium-card__rank">2</div>
          <div class="podium-avatar-wrap">
            <div class="podium-card__avatar">
              <img v-if="top3[1].avatarUrl" :src="top3[1].avatarUrl" alt="avatar" />
              <span v-else>{{ (top3[1].nickname || '?').charAt(0) }}</span>
            </div>
            <MemberVBadge :level="top3[1].memberLevel" :size="18" />
          </div>
          <div class="podium-card__name">{{ top3[1].nickname || '匿名用户' }}</div>
          <div class="podium-card__amount">{{ formatAmount(top3[1].amount) }}</div>
          <div class="podium-card__unit">邀请收益（创作币）</div>
          <div class="podium-card__invite">已邀请 {{ top3[1].inviteCount || 0 }} 人</div>
          <div v-if="top3[1].isMe" class="podium-card__me">我</div>
        </div>

        <div v-if="top3[0]" class="podium-card podium-card--first">
          <div class="podium-card__crown">👑</div>
          <div class="podium-avatar-wrap">
            <div class="podium-card__avatar">
              <img v-if="top3[0].avatarUrl" :src="top3[0].avatarUrl" alt="avatar" />
              <span v-else>{{ (top3[0].nickname || '?').charAt(0) }}</span>
            </div>
            <MemberVBadge :level="top3[0].memberLevel" :size="20" />
          </div>
          <div class="podium-card__name">{{ top3[0].nickname || '匿名用户' }}</div>
          <div class="podium-card__amount">{{ formatAmount(top3[0].amount) }}</div>
          <div class="podium-card__unit">邀请收益（创作币）</div>
          <div class="podium-card__invite">已邀请 {{ top3[0].inviteCount || 0 }} 人</div>
          <div v-if="top3[0].isMe" class="podium-card__me">我</div>
        </div>

        <div v-if="top3[2]" class="podium-card podium-card--third">
          <div class="podium-card__rank">3</div>
          <div class="podium-avatar-wrap">
            <div class="podium-card__avatar">
              <img v-if="top3[2].avatarUrl" :src="top3[2].avatarUrl" alt="avatar" />
              <span v-else>{{ (top3[2].nickname || '?').charAt(0) }}</span>
            </div>
            <MemberVBadge :level="top3[2].memberLevel" :size="18" />
          </div>
          <div class="podium-card__name">{{ top3[2].nickname || '匿名用户' }}</div>
          <div class="podium-card__amount">{{ formatAmount(top3[2].amount) }}</div>
          <div class="podium-card__unit">邀请收益（创作币）</div>
          <div class="podium-card__invite">已邀请 {{ top3[2].inviteCount || 0 }} 人</div>
          <div v-if="top3[2].isMe" class="podium-card__me">我</div>
        </div>
      </div>

      <!-- TOP 4-20 -->
      <div v-if="after3.length" class="invite-rank-list">
        <div
          v-for="item in after3"
          :key="item.userId"
          :class="['invite-rank-row', { 'is-me': item.isMe }]"
        >
          <div class="invite-rank-row__rank">{{ item.rank }}</div>
          <div class="invite-rank-row__avatar-wrap">
            <div class="invite-rank-row__avatar">
              <img v-if="item.avatarUrl" :src="item.avatarUrl" alt="avatar" />
              <span v-else>{{ (item.nickname || '?').charAt(0) }}</span>
            </div>
            <MemberVBadge :level="item.memberLevel" :size="14" />
          </div>
          <div class="invite-rank-row__info">
            <div class="invite-rank-row__name">
              {{ item.nickname || '匿名用户' }}
              <span v-if="item.isMe" class="invite-rank-row__me">我</span>
            </div>
            <div class="invite-rank-row__invite">已邀请 {{ item.inviteCount || 0 }} 人</div>
          </div>
          <div class="invite-rank-row__amount">
            <span class="invite-rank-row__num">{{ formatAmount(item.amount) }}</span>
            <span class="invite-rank-row__unit">邀请收益</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getInviteLeaderboard } from '@/api/leaderboard.js'
import MemberVBadge from '@/components/common/MemberVBadge.vue'

const loading = ref(false)
const list = ref([])

const top3 = computed(() => list.value.slice(0, 3))
const after3 = computed(() => list.value.slice(3))

function formatAmount(amount) {
  const value = Number(amount || 0)
  return Number.isInteger(value) ? String(value) : value.toFixed(2)
}

async function loadLeaderboard() {
  try {
    loading.value = true
    const res = await getInviteLeaderboard()
    const topList = res?.data?.topList || []
    const me = res?.data?.me
    if (me && me.rank != null && !topList.some(item => item.isMe)) {
      topList.push(me)
    }
    list.value = topList
  } catch (err) {
    message.error(err.message || '邀请排行榜加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadLeaderboard)
</script>

<style scoped>
.invite-rank-panel {
  width: 100%;
}

.invite-rank-empty {
  padding: 48px 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  background: #fff;
  border: 1px dashed #e8e8e8;
  border-radius: 16px;
}

/* TOP3 领奖台 */
.invite-rank-podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 12px;
  padding: 12px 0 20px;
}

.podium-card {
  position: relative;
  flex: 1;
  min-width: 0;
  max-width: 170px;
  text-align: center;
  border-radius: 20px;
  padding: 20px 12px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}

.podium-card--first {
  order: 2;
  padding-top: 18px;
  padding-bottom: 24px;
  background: linear-gradient(180deg, #fff9e6 0%, #fff 100%);
  border-color: #ffe082;
  box-shadow: 0 8px 28px rgba(255, 193, 7, 0.18);
  z-index: 2;
}

.podium-card--second {
  order: 1;
  transform: translateY(14px);
  background: linear-gradient(180deg, #f5f5f5 0%, #fff 100%);
  border-color: #d9d9d9;
}

.podium-card--third {
  order: 3;
  transform: translateY(24px);
  background: linear-gradient(180deg, #fff5e6 0%, #fff 100%);
  border-color: #ffcc80;
}

.podium-card__crown,
.podium-card__rank {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
}

.podium-card__crown {
  font-size: 24px;
}

.podium-card__rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #bfc0c2 0%, #e8e8e8 100%);
}

.podium-card--third .podium-card__rank {
  background: linear-gradient(135deg, #cd7f32 0%, #eebb77 100%);
}

.podium-avatar-wrap {
  position: relative;
  display: flex;
  width: fit-content;
  margin: 0 auto 10px;
}

.podium-avatar-wrap .member-v-badge {
  position: absolute;
  right: -2px;
  bottom: -2px;
  z-index: 2;
}

.podium-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: #595959;
  overflow: hidden;
}

.podium-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.podium-card--first .podium-card__avatar {
  width: 58px;
  height: 58px;
  font-size: 22px;
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(255, 193, 7, 0.35);
}

.podium-card--second .podium-card__avatar {
  background: linear-gradient(135deg, #bfc0c2 0%, #e8e8e8 100%);
  color: #fff;
}

.podium-card--third .podium-card__avatar {
  background: linear-gradient(135deg, #cd7f32 0%, #eebb77 100%);
  color: #fff;
}

.podium-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.podium-card__amount {
  font-size: 20px;
  font-weight: 800;
  color: #ff2442;
  line-height: 1;
}

.podium-card__unit {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

.podium-card__invite {
  display: inline-block;
  margin-top: 8px;
  padding: 2px 10px;
  font-size: 11px;
  color: #ad6800;
  background: #fff9e6;
  border-radius: 999px;
}

.podium-card__me {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}

/* TOP 4-20 列表 */
.invite-rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

.invite-rank-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f5f5f5;
}

.invite-rank-row.is-me {
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
  border-color: #ffd1d9;
}

.invite-rank-row__rank {
  width: 32px;
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  color: #8c8c8c;
  flex-shrink: 0;
}

.invite-rank-row__avatar-wrap {
  position: relative;
  display: flex;
  flex-shrink: 0;
}

.invite-rank-row__avatar-wrap .member-v-badge {
  position: absolute;
  right: -2px;
  bottom: -2px;
  z-index: 2;
}

.invite-rank-row__avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #595959;
  overflow: hidden;
}

.invite-rank-row__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.invite-rank-row__info {
  flex: 1;
  min-width: 0;
}

.invite-rank-row__name {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.invite-rank-row__me {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  background: #ff2442;
  color: #fff;
  border-radius: 8px;
  flex-shrink: 0;
}

.invite-rank-row__invite {
  font-size: 12px;
  color: #8c8c8c;
}

.invite-rank-row__amount {
  text-align: right;
  flex-shrink: 0;
}

.invite-rank-row__num {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #ff2442;
  line-height: 1;
}

.invite-rank-row__unit {
  font-size: 11px;
  color: #8c8c8c;
}

@media (max-width: 768px) {
  .invite-rank-podium {
    gap: 8px;
    padding: 8px 0 16px;
  }

  .podium-card {
    max-width: none;
    padding: 16px 8px 14px;
    border-radius: 16px;
  }

  .podium-card--first {
    padding-bottom: 20px;
  }

  .podium-card--second {
    transform: translateY(10px);
  }

  .podium-card--third {
    transform: translateY(18px);
  }

  .podium-card__avatar {
    width: 40px;
    height: 40px;
    font-size: 15px;
  }

  .podium-card--first .podium-card__avatar {
    width: 50px;
    height: 50px;
    font-size: 18px;
  }

  .podium-card__name {
    font-size: 12px;
  }

  .podium-card__amount {
    font-size: 16px;
  }

  .podium-card__unit {
    font-size: 10px;
  }

  .invite-rank-row {
    padding: 10px 12px;
  }
}
</style>

<style>
/* 暗色主题 */
body[data-theme="dark"] .invite-rank-empty {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .podium-card {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .podium-card--first {
  background: linear-gradient(180deg, #3a2a1a 0%, #1f1f1f 100%);
  border-color: rgba(255, 193, 7, 0.3);
}

body[data-theme="dark"] .podium-card--second {
  background: linear-gradient(180deg, #2a2a2a 0%, #1f1f1f 100%);
}

body[data-theme="dark"] .podium-card--third {
  background: linear-gradient(180deg, #3a2a1a 0%, #1f1f1f 100%);
  border-color: rgba(205, 127, 50, 0.3);
}

body[data-theme="dark"] .podium-card__name {
  color: #f0f0f0;
}

body[data-theme="dark"] .podium-card__amount,
body[data-theme="dark"] .invite-rank-row__num {
  color: #ff6b81;
}

body[data-theme="dark"] .podium-card__unit,
body[data-theme="dark"] .invite-rank-row__unit {
  color: #8c8c8c;
}

body[data-theme="dark"] .podium-card__invite {
  color: #ffd666;
  background: rgba(255, 193, 7, 0.12);
}

body[data-theme="dark"] .invite-rank-row__invite {
  color: #8c8c8c;
}

body[data-theme="dark"] .podium-card__avatar,
body[data-theme="dark"] .invite-rank-row__avatar {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .podium-card--first .podium-card__avatar,
body[data-theme="dark"] .podium-card--second .podium-card__avatar,
body[data-theme="dark"] .podium-card--third .podium-card__avatar {
  color: #fff;
}

body[data-theme="dark"] .invite-rank-row {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .invite-rank-row.is-me {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .invite-rank-row__name {
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-rank-row__rank {
  color: #8c8c8c;
}
</style>

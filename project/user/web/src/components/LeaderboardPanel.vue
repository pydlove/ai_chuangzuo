<template>
  <div class="leaderboard-panel">
    <div class="leaderboard-toolbar">
      <div class="leaderboard-toolbar-left">
        <span class="leaderboard-period-label">{{ currentCoinMonth }}</span>
      </div>
    </div>

    <div :class="['reward-banner', coinRewardBanner.class]">
      <div class="reward-banner-icon">🏆</div>
      <div class="reward-banner-text">
        <div class="reward-banner-title">{{ coinRewardBanner.title }}</div>
        <div class="reward-banner-desc">{{ coinRewardBanner.desc }}</div>
      </div>
    </div>

    <div v-if="myCoinStatus" class="my-reward-card">
      <div class="my-reward-rank">第 {{ myCoinItem.rank }} 名</div>
      <div class="my-reward-info">
        <div class="my-reward-label">{{ myCoinStatus.label }}</div>
        <div class="my-reward-desc">{{ myCoinStatus.desc }}</div>
      </div>
      <div class="my-reward-amount">+1000 创作币</div>
    </div>

    <div class="leaderboard-top3">
      <div
        v-for="item in coinTop3"
        :key="item.userId"
        :class="['leaderboard-top-card', 'top-' + item.rank, { 'is-me': item.isMe }]"
      >
        <div class="top-rank">{{ item.rank }}</div>
        <div class="top-nickname">{{ item.nickname || '匿名用户' }}</div>
        <div class="top-amount">{{ item.amount.toFixed(2) }} 创作币</div>
        <div v-if="item.isMe" class="top-me-tag">我</div>
        <div v-if="coinRewardLabel(item)" :class="['top-reward', coinRewardLabel(item).type]">
          {{ coinRewardLabel(item).text }}
        </div>
      </div>
    </div>

    <div v-if="coinListAfter3.length === 0" class="leaderboard-empty">
      暂无排名数据
    </div>
    <div v-else class="leaderboard-list">
      <div
        v-for="item in coinListAfter3"
        :key="item.userId"
        :class="['leaderboard-item', { 'is-me': item.isMe }, 'rank-' + item.rank]"
      >
        <span class="leaderboard-rank">{{ item.rank }}</span>
        <span class="leaderboard-avatar">{{ (item.nickname || '?').charAt(0) }}</span>
        <span class="leaderboard-nickname">{{ item.nickname || '匿名用户' }}</span>
        <span v-if="item.isMe" class="leaderboard-me-tag">我</span>
        <div v-if="coinRewardLabel(item)" :class="['leaderboard-reward', coinRewardLabel(item).type]">
          {{ coinRewardLabel(item).text }}
        </div>
        <span class="leaderboard-amount">{{ item.amount.toFixed(2) }} 创作币</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getCoinLeaderboard } from '@/api/leaderboard.js'

function getMonthOptions() {
  const options = []
  const now = new Date()
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return options
}

const monthOptions = getMonthOptions()
const currentCoinMonth = monthOptions[0]
const coinList = ref([])
const loading = ref(false)

async function loadCoinLeaderboard() {
  try {
    loading.value = true
    const res = await getCoinLeaderboard(currentCoinMonth)
    const list = res?.data?.topList || []
    const me = res?.data?.me
    if (me && me.rank != null && !list.some(item => item.isMe)) {
      list.push(me)
    }
    coinList.value = list
  } catch (err) {
    message.error(err.message || '创作币榜加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCoinLeaderboard()
})

const coinTop3 = computed(() => coinList.value.slice(0, 3))
const coinListAfter3 = computed(() => coinList.value.slice(3))

const myCoinItem = computed(() => coinList.value.find(i => i.isMe))

const coinRewardBanner = computed(() => ({
  class: 'is-current',
  title: '本月 TOP 10 当月可获 1000 创作币奖励',
  desc: '当前榜单进行中，下月 1 日自动结算，奖励发放至账户余额'
}))

function myRewardStatus(item) {
  if (!item || item.rank > 10) return null
  if (item.rank <= 3) {
    return {
      label: `已锁定 TOP ${item.rank}`,
      desc: '本月榜单进行中，结算后自动发放',
      type: 'pending'
    }
  }
  return {
    label: `进入 TOP 10（第 ${item.rank} 名）`,
    desc: '本月榜单进行中，结算后自动发放',
    type: 'pending'
  }
}

const myCoinStatus = computed(() => {
  if (!myCoinItem.value) return null
  if (!myCoinItem.value.rank || myCoinItem.value.rank > 10) return null
  return myRewardStatus(myCoinItem.value)
})

function coinRewardLabel(item) {
  if (item.rank > 10) return null
  return { text: '本月榜单进行中，待结算', type: 'pending' }
}
</script>

<style scoped>
.leaderboard-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.leaderboard-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.leaderboard-toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.leaderboard-period-label {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.reward-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid;
}

.reward-banner.is-current {
  background: #fff5f7;
  border-color: #ffd1d9;
}

.reward-banner.is-past {
  background: #fafafa;
  border-color: #e8e8e8;
}

.reward-banner-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.reward-banner-text {
  flex: 1;
  min-width: 0;
}

.reward-banner-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.reward-banner-desc {
  font-size: 13px;
  color: #595959;
}

.my-reward-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #fff5f7;
  border: 1px solid #ffd1d9;
  border-radius: 12px;
}

.my-reward-rank {
  font-size: 22px;
  font-weight: 700;
  color: #ff2442;
  flex-shrink: 0;
}

.my-reward-info {
  flex: 1;
  min-width: 0;
}

.my-reward-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.my-reward-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.my-reward-amount {
  font-size: 18px;
  font-weight: 700;
  color: #ff2442;
  flex-shrink: 0;
}

.leaderboard-top3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.leaderboard-top-card {
  position: relative;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 24px 16px;
  text-align: center;
}

.top-rank {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.top-amount {
  font-size: 20px;
  font-weight: 700;
  color: #ff2442;
}

.top-nickname {
  font-size: 15px;
  font-weight: 500;
  color: #595959;
  margin-bottom: 8px;
}

.top-me-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 11px;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}

.leaderboard-reward,
.top-reward {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.top-reward {
  position: absolute;
  top: 10px;
  right: 10px;
}

.leaderboard-reward.awarded,
.top-reward.awarded {
  background: #fff5f7;
  color: #389e0d;
}

.leaderboard-reward.pending,
.top-reward.pending {
  background: #f5f5f5;
  color: #8c8c8c;
}

.leaderboard-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}

.leaderboard-item.is-me {
  background: #fff5f7;
  border-color: #ffd1d9;
}

.leaderboard-rank {
  width: 28px;
  text-align: center;
  font-weight: 700;
  color: #8c8c8c;
}

.leaderboard-item.rank-1 .leaderboard-rank { color: #cf1322; }
.leaderboard-item.rank-2 .leaderboard-rank { color: #d48806; }
.leaderboard-item.rank-3 .leaderboard-rank { color: #389e0d; }

.leaderboard-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #595959;
}

.leaderboard-nickname {
  flex: 1;
  font-size: 15px;
  color: #1a1a1a;
}

.leaderboard-me-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}

.leaderboard-amount {
  font-size: 16px;
  font-weight: 700;
  color: #ff2442;
}

.leaderboard-empty {
  padding: 48px 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  background: #fff;
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
}

@media (max-width: 640px) {
  .leaderboard-top3 {
    grid-template-columns: 1fr;
  }

  .my-reward-card {
    flex-wrap: wrap;
  }

  .my-reward-amount {
    width: 100%;
    text-align: right;
  }
}

body[data-theme="dark"] .leaderboard-period-label { color: #e0e0e0; }
body[data-theme="dark"] .reward-banner.is-current {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.25);
}
body[data-theme="dark"] .reward-banner.is-past {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .reward-banner-title { color: #f0f0f0; }
body[data-theme="dark"] .reward-banner-desc { color: #a6a6a6; }
body[data-theme="dark"] .my-reward-card {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.25);
}
body[data-theme="dark"] .my-reward-label { color: #f0f0f0; }
body[data-theme="dark"] .my-reward-desc { color: #a6a6a6; }
body[data-theme="dark"] .leaderboard-top-card {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .top-rank { color: #f0f0f0; }
body[data-theme="dark"] .top-nickname { color: #a6a6a6; }
body[data-theme="dark"] .leaderboard-item {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .leaderboard-item.is-me {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.25);
}
body[data-theme="dark"] .leaderboard-nickname { color: #e0e0e0; }
body[data-theme="dark"] .leaderboard-avatar {
  background: #262626;
  color: #a6a6a6;
}
body[data-theme="dark"] .leaderboard-rank { color: #8c8c8c; }
body[data-theme="dark"] .leaderboard-empty {
  background: #1f1f1f;
  border-color: #303030;
  color: #8c8c8c;
}
body[data-theme="dark"] .leaderboard-reward.pending,
body[data-theme="dark"] .top-reward.pending {
  background: #262626;
  color: #8c8c8c;
}
body[data-theme="dark"] .leaderboard-reward.awarded,
body[data-theme="dark"] .top-reward.awarded {
  background: rgba(255, 36, 66, 0.12);
  color: #10b981;
}
</style>

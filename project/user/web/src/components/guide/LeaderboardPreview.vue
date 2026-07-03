<template>
  <div class="leaderboard-preview">
    <h4 class="lp-title">本月创作币榜 TOP 5</h4>
    <div class="lp-list">
      <div
        v-for="item in topList"
        :key="item.userId"
        :class="['lp-row', { me: item.isMe }]"
      >
        <span class="lp-rank">{{ item.rank }}</span>
        <span class="lp-name">{{ item.nickname }}</span>
        <span class="lp-amount">{{ formatAmount(item.amount) }} 创作币</span>
      </div>
    </div>
    <button class="lp-btn" @click="handleViewFull">查看完整榜单</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const MOCK_TOP = [
  { userId: 'm1', nickname: '创作者小王', amount: 12580, rank: 1 },
  { userId: 'm2', nickname: '文案阿杰', amount: 9200, rank: 2 },
  { userId: 'm3', nickname: '自媒体老李', amount: 7150, rank: 3 },
  { userId: 'm4', nickname: '写作喵', amount: 5400, rank: 4 },
  { userId: 'm5', nickname: '内容工匠', amount: 3880, rank: 5 }
]

const topList = ref([...MOCK_TOP])

function formatMonth(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

function formatAmount(n) {
  return Number(n || 0).toLocaleString()
}

onMounted(async () => {
  try {
    const mod = await import('@/composables/useLeaderboard.js')
    if (mod.getCoinLeaderboard) {
      const list = mod.getCoinLeaderboard(formatMonth(new Date()))
      if (Array.isArray(list) && list.length > 0) {
        topList.value = list.slice(0, 5)
      }
    }
  } catch (e) {
    // 保持 mock 数据
  }
})

const handleViewFull = () => {
  const isLoggedIn = !!localStorage.getItem('aichuangzuo_user_id')
  if (isLoggedIn) {
    router.push('/console/leaderboard')
  } else {
    router.push('/login?from=guide')
  }
}
</script>

<style scoped>
.leaderboard-preview {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
}
.lp-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.lp-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.lp-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
  font-size: 14px;
}
.lp-row.me {
  background: #fff5f7;
  font-weight: 600;
}
.lp-rank {
  width: 24px;
  color: #8c8c8c;
  font-weight: 600;
}
.lp-row:nth-child(1) .lp-rank { color: #faad14; }
.lp-row:nth-child(2) .lp-rank { color: #bfbfbf; }
.lp-row:nth-child(3) .lp-rank { color: #d48806; }
.lp-name {
  flex: 1;
  color: #1a1a1a;
}
.lp-amount {
  color: #ff2442;
  font-weight: 600;
}
.lp-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid #ff2442;
  background: #fff;
  color: #ff2442;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.lp-btn:hover {
  background: #fff5f7;
}

body[data-theme="dark"] .leaderboard-preview {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .lp-title,
body[data-theme="dark"] .lp-name {
  color: #e0e0e0;
}
body[data-theme="dark"] .lp-row {
  background: #141414;
}
body[data-theme="dark"] .lp-row.me {
  background: rgba(255, 36, 66, 0.12);
}
body[data-theme="dark"] .lp-btn {
  background: #1f1f1f;
  border-color: #ff4d6f;
  color: #ff4d6f;
}
body[data-theme="dark"] .lp-btn:hover {
  background: rgba(255, 36, 66, 0.12);
}
</style>

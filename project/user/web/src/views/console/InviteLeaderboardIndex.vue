<template>
  <div class="invite-rank-page">
    <header class="invite-rank-hero">
      <div class="invite-rank-hero__badge">🏆 邀请达人榜</div>
      <h1 class="invite-rank-hero__title">邀请排行榜</h1>
      <p class="invite-rank-hero__subtitle">按邀请带来的累计收益排名 · 展示 TOP 20</p>
    </header>

    <InviteLeaderboardPanel />
  </div>
</template>

<script setup>
import InviteLeaderboardPanel from '@/components/common/InviteLeaderboardPanel.vue'
</script>

<style scoped>
.invite-rank-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 16px 32px;
}

.invite-rank-hero {
  position: relative;
  border-radius: 20px;
  padding: 24px 20px;
  margin-bottom: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #fff5e6 0%, #fff0f2 40%, #f3e8ff 100%);
  box-shadow: 0 10px 40px rgba(255, 36, 66, 0.1);
  text-align: center;
}

.invite-rank-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #ff2442;
  background: rgba(255, 255, 255, 0.7);
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 12px;
}

.invite-rank-hero__title {
  font-size: 24px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 6px;
}

.invite-rank-hero__subtitle {
  font-size: 13px;
  color: #595959;
  margin: 0;
}

@media (max-width: 768px) {
  .invite-rank-page {
    padding: 16px 12px 24px;
  }

  .invite-rank-hero {
    padding: 20px 16px;
    border-radius: 16px;
  }

  .invite-rank-hero__title {
    font-size: 22px;
  }
}
</style>

<style>
body[data-theme="dark"] .invite-rank-hero {
  background: linear-gradient(135deg, #3a2a1a 0%, #3a1f2a 50%, #2a1f3d 100%);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

body[data-theme="dark"] .invite-rank-hero__badge {
  background: rgba(30, 30, 30, 0.7);
  color: #ff6b81;
}

body[data-theme="dark"] .invite-rank-hero__title {
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-rank-hero__subtitle {
  color: #a6a6a6;
}
</style>

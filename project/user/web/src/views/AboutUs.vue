<template>
  <div class="about-page">
    <NavBar :links="landingNavLinks" :cta-to="landingTopCta.to" :cta-label="landingTopCta.label" />

    <main class="about-main">
      <div class="about-hero">
        <h1 class="about-title">关于爱创作工坊</h1>
        <p class="about-subtitle">
          爱创作工坊是面向普通人的自媒体运营流水线，帮助零基础创作者快速找准平台、赛道与人设，持续获得差异化选题，生成有个人印记的内容，并掌握发布运营方法。
        </p>
      </div>

      <div class="about-content">
        <section class="about-section">
          <h2 class="about-section-title">我们的愿景</h2>
          <p class="about-section-text">
            让每一个愿意表达的普通人，都能低成本、可持续地做出有价值、有风格的自媒体内容，逐步建立副业收入或个人 IP。
          </p>
        </section>

        <section class="about-section">
          <h2 class="about-section-title">我们提供什么</h2>
          <div class="about-cards">
            <div v-for="item in features" :key="item.title" class="about-card">
              <div class="about-card-icon">
                <Icon :name="item.icon" :size="24" :stroke-width="2" />
              </div>
              <h3 class="about-card-title">{{ item.title }}</h3>
              <p class="about-card-text">{{ item.desc }}</p>
            </div>
          </div>
        </section>

        <section class="about-section">
          <h2 class="about-section-title">联系我们</h2>
          <p class="about-section-text">
            公司名称：杭州爱启云网络科技有限公司<br>
            官方网站：www.ichuang.top<br>
            商务与合作：aiocloud@126.com
          </p>
        </section>
      </div>
    </main>

    <AppFooter />
  </div>
</template>

<script setup>
import NavBar from '@/components/layout/NavBar.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import Icon from '@/components/common/Icon.vue'
import { landingNavLinks, landingTopCta } from '@/data/siteConfig.js'

const features = [
  { icon: 'globe', title: '定位辅助', desc: '根据你的兴趣与目标平台，推荐合适赛道与人设。' },
  { icon: 'zap', title: '选题灵感', desc: '结合热点与差异化角度，持续输出有竞争力的选题。' },
  { icon: 'file', title: '内容生成', desc: 'AI 辅助生成有个人印记、不易同质化的文章内容。' },
  { icon: 'trending-up', title: '运营方法', desc: '提供发布节奏、数据复盘与变现路径的实用建议。' }
]
</script>

<style scoped>
.about-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.about-main {
  flex: 1;
  padding: 64px 48px 80px;
}

.about-hero {
  max-width: 880px;
  margin: 0 auto 56px;
  text-align: center;
}

.about-title {
  font-size: 38px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
}

.about-subtitle {
  font-size: 17px;
  color: #595959;
  line-height: 1.8;
}

.about-content {
  max-width: 960px;
  margin: 0 auto;
}

.about-section {
  margin-bottom: 56px;
}

.about-section-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
}

.about-section-text {
  font-size: 15px;
  color: #595959;
  line-height: 1.8;
}

.about-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.about-card {
  padding: 28px;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  background: #fff;
  transition: all 0.25s ease;
}

.about-card:hover {
  border-color: #ffd6dd;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.08);
  transform: translateY(-4px);
}

.about-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
  color: #FF2442;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.about-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.about-card-text {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.65;
}

@media (max-width: 768px) {
  .about-main {
    padding: 40px 20px 56px;
  }

  .about-title {
    font-size: 28px;
  }

  .about-subtitle {
    font-size: 15px;
  }

  .about-section-title {
    font-size: 20px;
  }

  .about-cards {
    grid-template-columns: 1fr;
  }
}

body[data-theme="dark"] .about-page {
  background: #141414;
}

body[data-theme="dark"] .about-title,
body[data-theme="dark"] .about-section-title,
body[data-theme="dark"] .about-card-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .about-subtitle,
body[data-theme="dark"] .about-section-text,
body[data-theme="dark"] .about-card-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .about-card {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .about-card:hover {
  border-color: rgba(255, 77, 111, 0.4);
}

body[data-theme="dark"] .about-card-icon {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
</style>

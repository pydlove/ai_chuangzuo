<template>
  <div class="home-page">
    <!-- 导航栏 -->
    <header class="home-nav">
      <div class="nav-brand">
        <img
          src="https://foruda.gitee.com/images/1782805324201637771/ee4f5810_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
      </div>
      <div class="nav-links">
        <router-link to="/" class="nav-link active">首页</router-link>
        <router-link to="/pricing" class="nav-link">会员</router-link>
        <router-link to="/login" class="nav-cta">开始创作</router-link>
      </div>
    </header>

    <!-- Hero 区 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-badge">AI 自媒体写作助手</div>
        <h1 class="hero-title">3 分钟写一篇能变现的自媒体文章</h1>
        <p class="hero-desc">
          把每天 3 小时的写作时间，压缩成 3 分钟。AI 自动完成创作、正文、排版，一次生成适配公众号、百家号、今日头条、小红书、抖音图文。
        </p>
        <router-link to="/login" class="hero-btn">立即开始创作</router-link>
        <div class="hero-checkmarks">
          <span class="check-item"><span class="check-icon">✓</span>每天省下 2 小时</span>
          <span class="check-item"><span class="check-icon">✓</span>多平台一键分发</span>
          <span class="check-item"><span class="check-icon">✓</span>零基础也能出爆款</span>
        </div>
      </div>
    </section>

    <!-- 数据区 -->
    <section class="stats">
      <div class="stats-inner">
        <div class="stat-item">
          <div class="stat-num">10万+</div>
          <div class="stat-label">累计生成文章</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">1000+</div>
          <div class="stat-label">付费创作者</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">5+</div>
          <div class="stat-label">主流平台模板</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">3分钟</div>
          <div class="stat-label">平均成稿时间</div>
        </div>
      </div>
    </section>

    <!-- 特色功能 -->
    <section class="features">
      <div class="features-inner">
        <div class="features-header">
          <div class="features-tag">为什么选择爱创作</div>
          <h2 class="features-title">把时间变成收益</h2>
          <p class="features-subtitle">不教你写文案，只帮你把内容变成账号流量和收入</p>
        </div>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="feature-name">3 分钟成稿</div>
            <div class="feature-desc">告别 3 小时憋一篇文，输入写作方向，AI 自动完成标题、结构、正文。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
                <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
              </svg>
            </div>
            <div class="feature-name">多平台变现</div>
            <div class="feature-desc">一次生成，适配公众号、百家号、今日头条、小红书、抖音图文，流量不浪费。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <div class="feature-name">爆款结构</div>
            <div class="feature-desc">内置高打开率标题、钩子开头、金句结尾，不用懂写作也能写出爆款。</div>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff1f0;">
              <svg viewBox="0 0 24 24" fill="none" stroke="#FF2442" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
            </div>
            <div class="feature-name">导出即发布</div>
            <div class="feature-desc">生成后预览、调整、导出 Word，复制到各平台直接发布，快速变现。</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 使用步骤 -->
    <section class="steps">
      <div class="steps-inner">
        <h2 class="steps-title">3 步开始内容变现</h2>
        <p class="steps-subtitle">不用学习写作，只需要会复制粘贴</p>
        <div class="steps-list">
          <div class="step-item">
            <div class="step-num">1</div>
            <div class="step-name">输入主题</div>
            <div class="step-desc">填写写作方向</div>
          </div>
          <div class="step-item">
            <div class="step-num">2</div>
            <div class="step-name">AI 生成</div>
            <div class="step-desc">3 分钟获得完整文章</div>
          </div>
          <div class="step-item">
            <div class="step-num">3</div>
            <div class="step-name">发布变现</div>
            <div class="step-desc">多平台分发获取流量</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 最终 CTA -->
    <section class="cta-section">
      <h2 class="cta-title">现在注册，开始你的第一条变现内容</h2>
      <p class="cta-desc">每天少写 2 小时，多赚一份副业收入</p>
      <router-link to="/login" class="hero-btn">立即开始创作</router-link>
    </section>

    <!-- 底部 -->
    <footer class="home-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #fff;
}

/* 导航栏 */
.home-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 48px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  user-select: none;
  cursor: default;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-logo {
  height: 32px;
  width: auto;
}

.nav-brand-name {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-link {
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: color 0.2s;
}

.nav-link:hover,
.nav-link.active {
  color: #FF2442;
}

.nav-cta {
  padding: 8px 22px;
  background: #FF2442;
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-cta:hover {
  background: #E61E3A;
}

/* Hero */
.hero {
  background: linear-gradient(180deg, #FFE5EB 0%, #fff 100%);
  padding: 90px 48px 70px;
  text-align: center;
}

.hero-inner {
  max-width: 660px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-block;
  background: #fff;
  border: 1px solid #FFCBD4;
  color: #FF2442;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 46px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
  line-height: 1.15;
}

.hero-desc {
  font-size: 18px;
  color: #595959;
  margin-bottom: 32px;
  line-height: 1.7;
}

.hero-btn {
  display: inline-block;
  padding: 16px 44px;
  background: #FF2442;
  color: #fff;
  border-radius: 30px;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 10px 30px rgba(255,36,66,0.35);
  cursor: pointer;
  transition: all 0.2s;
}

.hero-btn:hover {
  background: #E61E3A;
}

.hero-checkmarks {
  display: flex;
  gap: 24px;
  justify-content: center;
  margin-top: 28px;
  flex-wrap: wrap;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #595959;
  font-size: 14px;
}

.check-icon {
  width: 18px;
  height: 18px;
  background: #FF2442;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 数据区 */
.stats {
  background: #f8f9fa;
  padding: 50px 48px;
}

.stats-inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 36px;
  font-weight: 700;
  color: #FF2442;
  margin-bottom: 6px;
}

.stat-label {
  color: #595959;
  font-size: 14px;
}

/* 特色功能 */
.features {
  padding: 70px 48px;
}

.features-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.features-header {
  text-align: center;
  margin-bottom: 48px;
}

.features-tag {
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.features-title {
  font-size: 30px;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.features-subtitle {
  color: #595959;
  font-size: 15px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.feature-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  text-align: left;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.feature-icon svg {
  width: 22px;
  height: 22px;
}

.feature-name {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
}

/* 使用步骤 */
.steps {
  background: linear-gradient(135deg, #FF2442 0%, #CC1730 100%);
  padding: 70px 48px;
  text-align: center;
}

.steps-inner {
  max-width: 900px;
  margin: 0 auto;
}

.steps-title {
  font-size: 30px;
  color: #fff;
  margin-bottom: 12px;
}

.steps-subtitle {
  color: rgba(255,255,255,0.9);
  font-size: 16px;
  margin-bottom: 48px;
}

.steps-list {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.step-item {
  flex: 1;
  background: rgba(255,255,255,0.12);
  border-radius: 16px;
  padding: 28px;
  text-align: center;
}

.step-num {
  width: 40px;
  height: 40px;
  background: #fff;
  color: #FF2442;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  margin: 0 auto 16px;
}

.step-name {
  font-weight: 600;
  color: #fff;
  font-size: 16px;
  margin-bottom: 8px;
}

.step-desc {
  color: rgba(255,255,255,0.85);
  font-size: 14px;
}

/* 最终 CTA */
.cta-section {
  padding: 70px 48px;
  text-align: center;
}

.cta-title {
  font-size: 28px;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.cta-desc {
  color: #595959;
  margin-bottom: 32px;
  font-size: 16px;
}

/* 底部 */
.home-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
}

.home-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}
</style>

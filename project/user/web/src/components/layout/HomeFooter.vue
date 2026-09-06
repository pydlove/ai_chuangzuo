<template>
  <div class="home-footer">
    <div class="home-footer-inner">
      <div class="home-footer-brand">
        <div class="home-footer-brand-top">
          <img
            src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
            alt="爱创作工坊"
            class="home-footer-logo"
          />
          <span class="home-footer-name">爱创作工坊</span>
        </div>
        <p class="home-footer-slogan">爱创作工坊，智绘未来</p>
        <div class="home-footer-contacts">
          <a href="mailto:aiocloud@126.com" class="home-footer-contact" title="联系邮箱" aria-label="联系邮箱">
            <Icon name="mail" :size="18" :stroke-width="2" />
          </a>
          <a href="https://www.ichuang.top" target="_blank" rel="noopener" class="home-footer-contact" title="官方网站" aria-label="官方网站">
            <Icon name="globe" :size="18" :stroke-width="2" />
          </a>
        </div>
        <div class="home-footer-qrcodes">
          <div class="home-footer-qrcode">
            <img src="https://foruda.gitee.com/images/1788417922298176039/649f9e43_8060302.jpg" alt="公众号二维码" />
            <span>公众号</span>
          </div>
          <div class="home-footer-qrcode">
            <img src="https://foruda.gitee.com/images/1782817803473013600/4f94eac9_8060302.png" alt="官方客服微信二维码" />
            <span>官方客服微信</span>
          </div>
        </div>
      </div>

      <div class="home-footer-columns">
        <div v-for="column in columns" :key="column.title" class="home-footer-column">
          <h4 class="home-footer-column-title">{{ column.title }}</h4>
          <nav class="home-footer-column-links">
            <template v-for="link in column.links" :key="link.to || link.href">
              <a
                v-if="link.href"
                :href="link.href"
                target="_blank"
                rel="noopener"
                class="home-footer-link"
              >{{ link.label }}</a>
              <router-link
                v-else
                :to="link.to"
                class="home-footer-link"
              >{{ link.label }}</router-link>
            </template>
          </nav>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import Icon from '@/components/common/Icon.vue'
import { landingFooterColumns } from '@/data/siteConfig.js'

const columns = landingFooterColumns
</script>

<style scoped>
.home-footer {
  background: linear-gradient(180deg, #fff 0%, #fff8f9 100%);
  border-top: 1px solid #ffe4e8;
  padding: 56px 48px 40px;
}

.home-footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 64px;
}

.home-footer-brand {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.home-footer-brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.home-footer-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 8px;
}

.home-footer-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
}

.home-footer-slogan {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.6;
  margin: 0;
}

.home-footer-contacts {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.home-footer-contact {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #f0f0f0;
  color: #595959;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  text-decoration: none;
}

.home-footer-contact:hover {
  border-color: #ffcbd4;
  color: #FF2442;
  background: #fff5f7;
}

.home-footer-qrcodes {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-top: 8px;
}

.home-footer-qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.home-footer-qrcode img {
  width: 88px;
  height: 88px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #fff;
}

.home-footer-qrcode span {
  font-size: 12px;
  color: #8c8c8c;
}

.home-footer-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
}

.home-footer-column-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
}

.home-footer-column-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.home-footer-link {
  font-size: 14px;
  color: #595959;
  text-decoration: none;
  transition: color 0.2s ease;
  width: max-content;
}

.home-footer-link:hover {
  color: #FF2442;
}

@media (max-width: 768px) {
  .home-footer {
    padding: 40px 20px 32px;
  }

  .home-footer-inner {
    grid-template-columns: 1fr;
    gap: 36px;
  }

  .home-footer-columns {
    grid-template-columns: repeat(2, 1fr);
    gap: 28px 20px;
  }

  .home-footer-qrcodes {
    gap: 12px;
  }

  .home-footer-qrcode img {
    width: 78px;
    height: 78px;
  }
}

body[data-theme="dark"] .home-footer {
  background: linear-gradient(180deg, #141414 0%, #1a1214 100%);
  border-top-color: #2a2a2a;
}

body[data-theme="dark"] .home-footer-name,
body[data-theme="dark"] .home-footer-column-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .home-footer-slogan,
body[data-theme="dark"] .home-footer-link {
  color: #a6a6a6;
}

body[data-theme="dark"] .home-footer-link:hover {
  color: #ff4d6f;
}

body[data-theme="dark"] .home-footer-contact {
  background: #1f1f1f;
  border-color: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .home-footer-contact:hover {
  border-color: rgba(255, 77, 111, 0.5);
  color: #ff4d6f;
  background: #2a2224;
}

body[data-theme="dark"] .home-footer-qrcode img {
  border-color: #2a2a2a;
  background: #fff;
}

body[data-theme="dark"] .home-footer-qrcode span {
  color: #a6a6a6;
}
</style>

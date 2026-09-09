<template>
  <div class="cs-gift">
    <p class="cs-gift__title">
      <Icon name="gift" :size="16" class="cs-gift__icon" />
      <span>添加客服微信，领取<b>平台大礼包</b>和<b>免费抽奖</b>机会</span>
    </p>
    <img class="cs-gift__qr" :src="CUSTOMER_SERVICE_QR" alt="客服微信二维码" />
    <p class="cs-gift__hint">微信扫码添加客服，领取专属福利</p>
  </div>
</template>

<script setup>
import Icon from '@/components/common/Icon.vue'

// 与 HomeFooter / ConsoleLayout 客服微信弹框使用同一张二维码图
const CUSTOMER_SERVICE_QR = 'https://foruda.gitee.com/images/1782817803473013600/4f94eac9_8060302.png'
</script>

<style scoped>
.cs-gift {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(255, 36, 66, 0.04);
  border: 1px solid rgba(255, 36, 66, 0.12);
}

.cs-gift__title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  line-height: 1.5;
}

.cs-gift__title b {
  color: #FF2442;
  font-weight: 600;
}

.cs-gift__icon {
  color: #FF2442;
  flex-shrink: 0;
}

.cs-gift__qr {
  width: 96px;
  height: 96px;
  padding: 4px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #f0f0f0;
  object-fit: contain;
}

.cs-gift__hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #8c8c8c;
}

body[data-theme="dark"] .cs-gift {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 77, 111, 0.25);
}

body[data-theme="dark"] .cs-gift__title {
  color: #e0e0e0;
}

body[data-theme="dark"] .cs-gift__title b {
  color: #ff4d6f;
}

body[data-theme="dark"] .cs-gift__icon {
  color: #ff4d6f;
}

body[data-theme="dark"] .cs-gift__hint {
  color: #737373;
}
</style>

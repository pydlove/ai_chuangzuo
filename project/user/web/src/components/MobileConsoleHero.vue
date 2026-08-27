<template>
  <section class="mobile-console-hero">
    <div class="mobile-console-hero__content">
      <span v-if="badge" class="mobile-console-hero__badge">{{ badge }}</span>
      <img
        v-if="logoUrl"
        :src="logoUrl"
        :alt="title"
        class="mobile-console-hero__logo"
      />
      <h1 v-else class="mobile-console-hero__title">{{ title }}</h1>
      <p v-if="desc" class="mobile-console-hero__desc">{{ desc }}</p>
    </div>
    <img
      v-if="imageUrl"
      :src="imageUrl"
      :alt="title"
      class="mobile-console-hero__image"
    />
  </section>
</template>

<script setup>
defineProps({
  badge: { type: String, default: '' },
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  logoUrl: { type: String, default: '' },
  imageUrl: { type: String, default: '' }
})
</script>

<style scoped>
.mobile-console-hero {
  display: none;
}

@media (max-width: 768px) {
  .mobile-console-hero {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 20px 16px;
    margin: 0 -12px 16px;
    background: linear-gradient(135deg, #FFF8F9 0%, #FFF0F2 100%);
    border-bottom: 1px solid #f0f0f0;
  }

  .mobile-console-hero__content {
    flex: 1;
    min-width: 0;
  }

  .mobile-console-hero__badge {
    display: inline-block;
    background: linear-gradient(135deg, #FFF0F2, #FFE4E8);
    color: #FF2442;
    font-size: 12px;
    font-weight: 600;
    padding: 5px 14px;
    border-radius: 16px;
    margin-bottom: 10px;
  }

  .mobile-console-hero__title {
    font-size: 22px;
    font-weight: 800;
    color: #1a1a1a;
    margin: 0 0 8px;
    line-height: 1.25;
  }

  .mobile-console-hero__logo {
    display: block;
    max-width: 180px;
    height: auto;
    max-height: 44px;
    object-fit: contain;
    margin-bottom: 8px;
  }

  .mobile-console-hero__desc {
    font-size: 13px;
    color: #8c8c8c;
    margin: 0;
    line-height: 1.5;
  }

  .mobile-console-hero__image {
    width: 88px;
    height: 88px;
    object-fit: contain;
    flex-shrink: 0;
  }
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-console-hero {
  background: linear-gradient(135deg, #2a1f22 0%, #1f1f1f 100%);
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .mobile-console-hero__title {
  color: #e0e0e0;
}

body[data-theme="dark"] .mobile-console-hero__desc {
  color: #a6a6a6;
}
</style>

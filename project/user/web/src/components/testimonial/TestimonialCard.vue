<template>
  <div class="testimonial-card">
    <div class="testimonial-header">
      <img v-if="avatarUrl" :src="avatarUrl" :alt="name" class="testimonial-avatar" />
      <div v-else class="testimonial-avatar fallback">{{ name ? name[0] : 'U' }}</div>
      <div class="testimonial-author">
        <div class="testimonial-name">{{ name }}</div>
        <div v-if="title" class="testimonial-title">{{ title }}</div>
      </div>
    </div>
    <div class="testimonial-stars">
      <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= starRating }">★</span>
    </div>
    <p class="testimonial-text">{{ reviewText }}</p>
  </div>
</template>

<script setup>
defineProps({
  avatarUrl: { type: String, default: '' },
  name: { type: String, required: true },
  title: { type: String, default: '' },
  starRating: { type: Number, default: 5 },
  reviewText: { type: String, required: true }
})
</script>

<style scoped>
.testimonial-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.2s;
  height: 100%;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.testimonial-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  border-color: transparent;
}
.testimonial-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.testimonial-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}
.testimonial-avatar.fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: #595959;
}
.testimonial-author {
  min-width: 0;
}
.testimonial-name {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
  line-height: 1.4;
}
.testimonial-title {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.testimonial-stars {
  margin-bottom: 12px;
}
.star {
  color: #e8e8e8;
  font-size: 16px;
  margin-right: 2px;
}
.star.filled {
  color: #ffb800;
}
.testimonial-text {
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
  margin: 0;
  flex: 1 1 auto;
  min-height: 0;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: break-word;
}

body[data-theme="dark"] .testimonial-card {
  background: #1f1f1f;
  border-color: #2a2a2a;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}
body[data-theme="dark"] .testimonial-card:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
}
body[data-theme="dark"] .testimonial-name {
  color: #e0e0e0;
}
body[data-theme="dark"] .testimonial-title,
body[data-theme="dark"] .testimonial-text {
  color: #a6a6a6;
}
body[data-theme="dark"] .testimonial-avatar.fallback {
  background: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .star {
  color: #3a3a3a;
}
</style>

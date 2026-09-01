<template>
  <div
    class="stat-card"
    :class="{
      [`stat-card--${variant}`]: true,
      'stat-card--value-first': valueFirst,
      'stat-card--with-image': image || $slots.image,
      'stat-card--with-hint': hint || $slots.hint,
      'stat-card--with-action': $slots.action
    }"
  >
    <div class="stat-card__body">
      <div class="stat-card__label">
        <slot name="label">{{ label }}</slot>
      </div>

      <div class="stat-card__value-wrap">
        <slot name="value">
          <span class="stat-card__value">{{ value }}</span>
          <span v-if="unit" class="stat-card__unit">{{ unit }}</span>
        </slot>
      </div>

      <div v-if="hint || $slots.hint" class="stat-card__hint">
        <slot name="hint">{{ hint }}</slot>
      </div>

      <div v-if="$slots.action" class="stat-card__action">
        <slot name="action" />
      </div>
    </div>

    <div v-if="image || $slots.image" class="stat-card__image">
      <slot name="image"
        ><img v-if="image" :src="image" :alt="label" /></slot
      >
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: '' },
  unit: { type: String, default: '' },
  hint: { type: String, default: '' },
  image: { type: String, default: '' },
  variant: {
    type: String,
    default: 'default',
    validator: (v) =>
      [
        'default',
        'flat',
        'primary',
        'gradient',
        'commission',
        'muted',
        'glass',
        'transparent'
      ].includes(v)
  },
  valueFirst: { type: Boolean, default: false }
})
</script>

<style scoped>
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

/* variants */
.stat-card--flat {
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: none;
}

.stat-card--flat:hover {
  transform: none;
  box-shadow: none;
}

.stat-card--primary {
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
  border-color: #ffd1d9;
}

.stat-card--gradient {
  background: linear-gradient(180deg, #fff8f9 0%, #fff 100%);
  border-color: #f0f0f0;
  border-radius: 10px;
  padding: 16px;
}

.stat-card--commission {
  background: linear-gradient(180deg, #fff0f3 0%, #fff 100%);
  border: none;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.stat-card--commission:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.stat-card--muted {
  text-align: center;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border: none;
  border-radius: 10px;
  padding: 14px;
  box-shadow: none;
}

.stat-card--muted:hover {
  transform: none;
  box-shadow: none;
}

.stat-card--muted .stat-card__body {
  align-items: center;
}

.stat-card--muted .stat-card__value {
  color: var(--color-primary, #ff2442);
  font-size: 24px;
}

.stat-card--glass {
  background: rgba(255, 255, 255, 0.78);
  border: none;
  border-radius: 16px;
  padding: 16px 18px;
  box-shadow: none;
  backdrop-filter: blur(6px);
}

.stat-card--glass:hover {
  transform: none;
  box-shadow: none;
}

.stat-card--transparent {
  text-align: center;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  padding: 0;
  box-shadow: none;
}

.stat-card--transparent:hover {
  transform: none;
  box-shadow: none;
}

.stat-card--transparent .stat-card__body {
  align-items: center;
}

.stat-card__body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

.stat-card--value-first .stat-card__body {
  gap: 8px;
}

.stat-card--with-image .stat-card__body {
  gap: 4px;
}

.stat-card__label {
  font-size: 13px;
  color: #8c8c8c;
  font-weight: 500;
}

.stat-card__value-wrap {
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
}

.stat-card__value {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
}

.stat-card__unit {
  font-size: 13px;
  color: #8c8c8c;
  font-weight: 500;
}

.stat-card__hint {
  font-size: 12px;
  color: #bfbfbf;
  line-height: 1.4;
}

.stat-card__action {
  margin-top: 4px;
}

.stat-card__image {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card__image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.stat-card--commission .stat-card__image {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 60px;
  height: 60px;
}

/* value-first layout: value on top, label below */
.stat-card--value-first .stat-card__value-wrap {
  order: -1;
}

.stat-card--value-first .stat-card__value {
  font-size: 30px;
}

.stat-card--value-first.stat-card--with-action .stat-card__body {
  gap: 10px;
}

@media (max-width: 768px) {
  .stat-card {
    padding: 16px;
    border-radius: 14px;
  }

  .stat-card__value {
    font-size: 22px;
  }

  .stat-card--value-first .stat-card__value {
    font-size: 26px;
  }

  .stat-card__image {
    width: 40px;
    height: 40px;
  }

  .stat-card--commission {
    border-radius: 14px;
    padding: 14px;
  }

  .stat-card--commission .stat-card__image {
    width: 52px;
    height: 52px;
    right: 6px;
    bottom: 6px;
  }

  .stat-card--muted {
    padding: 12px;
  }

  .stat-card--muted .stat-card__value {
    font-size: 20px;
  }
}

body[data-theme="dark"] .stat-card {
  background: #1f1f1f;
  border-color: #303030;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .stat-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
}

body[data-theme="dark"] .stat-card--flat {
  background: #1f1f1f;
  border-color: #303030;
  box-shadow: none;
}

body[data-theme="dark"] .stat-card--flat:hover {
  box-shadow: none;
}

body[data-theme="dark"] .stat-card--primary {
  background: linear-gradient(135deg, #2a1f22 0%, #2a1a1a 100%);
  border-color: rgba(255, 77, 111, 0.25);
}

body[data-theme="dark"] .stat-card--gradient {
  background: linear-gradient(180deg, #2a1f23 0%, #1f1f1f 100%);
  border-color: #303030;
}

body[data-theme="dark"] .stat-card--commission {
  background: linear-gradient(180deg, #2a1f22 0%, #1f1f1f 100%);
}

body[data-theme="dark"] .stat-card--muted {
  background: #262626;
}

body[data-theme="dark"] .stat-card--glass {
  background: rgba(31, 31, 31, 0.75);
}

body[data-theme="dark"] .stat-card--transparent {
  background: transparent;
}

body[data-theme="dark"] .stat-card__label,
body[data-theme="dark"] .stat-card__unit,
body[data-theme="dark"] .stat-card__hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .stat-card__value {
  color: #f0f0f0;
}

body[data-theme="dark"] .stat-card--primary .stat-card__value,
body[data-theme="dark"] .stat-card--muted .stat-card__value {
  color: #ff6b81;
}
</style>

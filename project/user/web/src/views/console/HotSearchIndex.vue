<template>
  <div class="hot-search-page">
    <!-- 日期选择 -->
    <div class="date-bar">
      <button
        v-for="date in dateList"
        :key="date.value"
        :class="['date-chip', { active: activeDate === date.value }]"
        @click="activeDate = date.value"
      >
        <span class="date-label">{{ date.label }}</span>
        <span class="date-value">{{ date.short }}</span>
      </button>
    </div>

    <!-- 页面标题 -->
    <div class="hot-search-header">
      <div class="hot-search-header-main">
        <div class="hot-search-icon">🔥</div>
        <div>
          <h2 class="hot-search-title">全网热搜榜</h2>
          <p class="hot-search-desc">聚合抖音、今日头条、B 站、微博、百度五大平台实时热点，追热点快人一步。</p>
        </div>
      </div>
      <div class="hot-search-current">{{ currentDateText }}</div>
    </div>

    <!-- 平台榜单 -->
    <div class="hot-search-section">
      <div class="platform-tabs">
        <button
          v-for="platform in platforms"
          :key="platform.code"
          :class="['platform-tab', { active: activePlatform === platform.code }, platform.code]"
          @click="activePlatform = platform.code"
        >
          <span class="platform-dot" />
          {{ platform.name }}
        </button>
      </div>

      <div class="hot-search-list">
        <a-spin :spinning="loading">
          <div
            v-for="item in list"
            :key="`${activePlatform}-${item.rank}`"
            class="hot-search-item"
            @click="item.url ? openUrl(item.url) : copyTitle(item.title)"
          >
            <span :class="['hot-search-rank', `rank-${item.rank}`]">{{ item.rank }}</span>
            <span class="hot-search-text" :title="item.title">{{ item.title }}</span>
            <span class="hot-search-heat">{{ item.hotValue }}</span>
          </div>
          <div v-if="!loading && list.length === 0" class="hot-search-empty">
            暂无数据，请稍后再试
          </div>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch'

const { platforms, loading, loadPlatforms, loadList } = useHotSearch()

const activePlatform = ref('')
const activeDate = ref('')

const pad = (n) => String(n).padStart(2, '0')
const formatDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const getWeekLabel = (d) => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[d.getDay()]
}

const today = new Date()
const dateList = Array.from({ length: 5 }, (_, i) => {
  const d = new Date(today)
  d.setDate(d.getDate() - i)
  const value = formatDate(d)
  return {
    value,
    label: i === 0 ? '今天' : i === 1 ? '昨天' : getWeekLabel(d),
    short: `${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
  }
})
activeDate.value = dateList[0].value

const currentDateText = computed(() => {
  const item = dateList.find((d) => d.value === activeDate.value)
  return item ? `${item.value} ${item.label}` : activeDate.value
})

const copyTitle = (title) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(title).then(() => message.success('标题已复制'))
  } else {
    message.info(title)
  }
}

const openUrl = (url) => {
  if (url) window.open(url, '_blank')
}

const refresh = async () => {
  if (!activePlatform.value || !activeDate.value) return
  await loadList(activePlatform.value, activeDate.value)
}

watch(activePlatform, refresh)
watch(activeDate, refresh)

onMounted(async () => {
  await loadPlatforms()
  if (platforms.value.length) {
    activePlatform.value = platforms.value[0].code
  }
})
</script>

<style scoped>
.hot-search-page {
  padding: 24px 32px;
  margin: 0 auto;
}

.date-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.date-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 2px;
}

.date-chip:hover {
  border-color: #d9d9d9;
}

.date-chip.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.18);
}

.date-label {
  font-size: 12px;
  font-weight: 500;
}

.date-value {
  font-size: 13px;
  font-weight: 600;
}

.hot-search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.hot-search-header-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hot-search-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

.hot-search-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary, #1f1f1f);
}

.hot-search-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-secondary, #595959);
}

.hot-search-current {
  font-size: 13px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 6px 12px;
  border-radius: 20px;
  flex-shrink: 0;
}

.hot-search-section {
  background: #fff;
  border-radius: 14px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.platform-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.platform-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.platform-tab:hover {
  background: #e8e8e8;
}

.platform-tab.active {
  background: var(--color-primary, #FF2442);
  color: #fff;
}

.platform-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.7;
}

.hot-search-list {
  display: flex;
  flex-direction: column;
}

.hot-search-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 15px 12px;
  border-radius: 10px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: all 0.15s;
}

.hot-search-item:last-child {
  border-bottom: none;
}

.hot-search-item:hover {
  background: #fafafa;
  transform: translateX(2px);
}

.hot-search-rank {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #8c8c8c;
  flex-shrink: 0;
}

.hot-search-rank.rank-1 {
  background: #fff1f0;
  color: #cf1322;
}

.hot-search-rank.rank-2 {
  background: #fff7e6;
  color: #d48806;
}

.hot-search-rank.rank-3 {
  background: #f6ffed;
  color: #389e0d;
}

.hot-search-text {
  flex: 1;
  font-size: 15px;
  color: var(--text-primary, #1f1f1f);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-search-heat {
  font-size: 13px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.hot-search-empty {
  text-align: center;
  padding: 40px 0;
  color: #8c8c8c;
  font-size: 14px;
}

/* 暗色主题 */
body[data-theme="dark"] .date-chip {
  background: #1f1f1f;
  border-color: #303030;
  color: rgba(255, 255, 255, 0.85);
}

body[data-theme="dark"] .date-chip:hover {
  border-color: #434343;
}

body[data-theme="dark"] .date-chip.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}

body[data-theme="dark"] .hot-search-title,
body[data-theme="dark"] .hot-search-text {
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .hot-search-desc,
body[data-theme="dark"] .hot-search-heat {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .hot-search-section {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .hot-search-current {
  background: #262626;
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .platform-tab {
  background: #262626;
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .platform-tab:hover {
  background: #303030;
}

body[data-theme="dark"] .hot-search-item {
  border-bottom-color: #262626;
}

body[data-theme="dark"] .hot-search-item:hover {
  background: #262626;
}

@media (max-width: 768px) {
  .hot-search-page {
    padding: 16px 12px;
  }

  .hot-search-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .hot-search-current {
    align-self: flex-start;
  }

  .date-bar {
    margin-left: -16px;
    margin-right: -16px;
    padding-left: 16px;
    padding-right: 16px;
    scrollbar-width: none;
    -webkit-mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
    mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
  }

  .date-bar::-webkit-scrollbar {
    display: none;
  }

  .date-chip {
    min-width: 60px;
    padding: 8px 12px;
  }

  .date-label,
  .date-value {
    font-size: 12px;
  }

  .platform-tabs {
    flex-wrap: wrap;
  }

  .platform-tab {
    padding: 6px 12px;
    font-size: 12px;
  }

  .hot-search-item {
    padding: 10px 12px;
    gap: 8px;
  }

  .hot-search-text {
    font-size: 13px;
  }

  .hot-search-heat {
    font-size: 11px;
    min-width: 56px;
  }
}
</style>

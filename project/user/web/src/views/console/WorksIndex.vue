<template>
  <div class="works-index">
    <div class="works-header">
      <h2 class="works-title">我的作品</h2>
      <div class="works-tabs">
        <button
          :class="['works-tab', { active: activeTab === 'works' }]"
          @click="activeTab = 'works'"
        >
          已生成
        </button>
        <button
          :class="['works-tab', { active: activeTab === 'drafts' }]"
          @click="activeTab = 'drafts'"
        >
          草稿箱
        </button>
      </div>
    </div>

    <!-- 已生成列表 -->
    <div v-if="activeTab === 'works'" class="works-list">
      <div v-if="worksList.length === 0" class="works-empty">
        <div class="empty-icon">📝</div>
        <div class="empty-text">还没有生成的文章</div>
        <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
      </div>
      <div v-else class="work-cards">
        <div v-for="work in worksList" :key="work.id" class="work-card">
          <div class="work-title">{{ work.title }}</div>
          <div class="work-meta">
            <span>{{ work.platform }}</span>
            <span>·</span>
            <span>{{ work.wordCount }} 字</span>
            <span>·</span>
            <span>{{ work.createdAt }}</span>
          </div>
          <div class="work-actions">
            <button class="work-action-btn" @click="resumeDraft(work)">继续编辑</button>
            <button class="work-action-btn" @click="deleteWork(work.id)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 草稿箱 -->
    <div v-if="activeTab === 'drafts'" class="drafts-list">
      <div v-if="draftsList.length === 0" class="works-empty">
        <div class="empty-icon">📁</div>
        <div class="empty-text">草稿箱是空的</div>
        <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
      </div>
      <div v-else class="work-cards">
        <div v-for="draft in draftsList" :key="draft.id" class="work-card draft-card">
          <div class="work-title">{{ draft.title || '未命名草稿' }}</div>
          <div class="work-meta">
            <span>{{ draft.platform?.name || '未选择平台' }}</span>
            <span>·</span>
            <span>{{ draft.wordCount?.count || 0 }} 字</span>
            <span>·</span>
            <span>保存于 {{ formatDate(draft.savedAt) }}</span>
          </div>
          <div class="work-actions">
            <button class="work-action-btn primary" @click="resumeDraft(draft)">继续编辑</button>
            <button class="work-action-btn" @click="deleteDraft(draft.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const activeTab = ref('drafts')

// 草稿列表
const draftsList = computed(() => {
  return JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
})

// 作品列表（模拟数据）
const worksList = ref([])

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${min}`
}

const resumeDraft = (draft) => {
  // 将选中草稿移到最前面，然后跳转到创作页
  const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  const idx = drafts.findIndex(d => d.id === draft.id)
  if (idx > -1) {
    drafts.splice(idx, 1)
    drafts.unshift(draft)
    localStorage.setItem('aichuangzuo_drafts', JSON.stringify(drafts))
  }
  router.push('/console/create')
}

const deleteDraft = (id) => {
  const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  const idx = drafts.findIndex(d => d.id === id)
  if (idx > -1) {
    drafts.splice(idx, 1)
    localStorage.setItem('aichuangzuo_drafts', JSON.stringify(drafts))
  }
}

const deleteWork = (id) => {
  const idx = worksList.value.findIndex(w => w.id === id)
  if (idx > -1) worksList.value.splice(idx, 1)
}
</script>

<style scoped>
.works-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.works-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.works-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.works-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
}

.works-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.works-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.works-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.empty-btn:hover {
  background: #e61e3a;
}

.work-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.work-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 16px;
}

.draft-card {
  border-color: #ffd1d9;
  background: #fff0f2;
}

.work-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.work-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.work-actions {
  display: flex;
  gap: 8px;
}

.work-action-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.work-action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.work-action-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.work-action-btn.primary:hover {
  background: #e61e3a;
  border-color: #e61e3a;
}
</style>
<template>
  <div class="styles-index">
    <div class="styles-header">
      <div>
        <h2 class="styles-title">我的风格</h2>
        <p class="styles-subtitle">提前设计你的专属写作风格，创作时一键选用</p>
      </div>
    </div>

    <div class="styles-tabs">
      <button
        :class="['styles-tab', { active: activeTab === 'my' }]"
        @click="activeTab = 'my'; editorMode = false"
      >
        我的风格
      </button>
      <button
        :class="['styles-tab', { active: activeTab === 'system' }]"
        @click="activeTab = 'system'; editorMode = false"
      >
        系统预设
      </button>
    </div>

    <!-- 我的风格 -->
    <div v-show="activeTab === 'my'" class="styles-content">
      <div v-if="myStyles.length === 0" class="styles-empty">
        <a-empty description="还没有自定义风格">
          <button class="empty-btn" @click="goToCreate">去创建一个</button>
        </a-empty>
      </div>
      <div v-else class="styles-grid">
        <div class="style-add-card" @click="goToCreate">
          <div class="style-add-icon">+</div>
          <div class="style-add-text">新建我的风格</div>
        </div>
        <div
          v-for="(s, idx) in myStyles"
          :key="s.name"
          class="style-card"
        >
          <div class="style-card-title">{{ s.name }}</div>
          <div class="style-card-desc">{{ s.desc }} · 已用 {{ s.count }} 次</div>
          <div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-actions">
            <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
            <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
              {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
            </button>
            <button class="style-action-btn" @click.stop="goToEdit(s)">编辑</button>
            <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(s.name)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统预设 -->
    <div v-show="activeTab === 'system'" class="styles-content">
      <div class="styles-grid">
        <div
          v-for="s in systemStyles"
          :key="s.name"
          class="style-card"
        >
          <div class="style-card-title">{{ s.name }}</div>
          <div class="style-card-desc">{{ s.desc }}</div>
          <div class="style-card-prompt">{{ s.promptSummary }}</div>
          <div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
          <div class="style-card-actions">
            <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
            <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
              {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  systemStyles,
  myStyles,
  applyStyle,
  removeCustomStyle
} from '@/composables/useStyles.js'

const router = useRouter()
const activeTab = ref('my')
const editorMode = ref(false)
const expandedNames = ref(new Set())

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const togglePrompt = (name) => {
  const set = new Set(expandedNames.value)
  if (set.has(name)) {
    set.delete(name)
  } else {
    set.add(name)
  }
  expandedNames.value = set
}

const goToCreate = () => {
  // 将在 Task 4 中实现
}

const goToEdit = (style) => {
  // 将在 Task 4 中实现
}

const useStyle = (style) => {
  applyStyle(style)
  router.push('/console/create')
}

const deleteStyle = (name) => {
  removeCustomStyle(name)
}
</script>

<style scoped>
.styles-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.styles-header {
  margin-bottom: 20px;
}

.styles-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.styles-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.styles-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  margin-bottom: 20px;
  width: fit-content;
}

.styles-tab {
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

.styles-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.styles-empty {
  padding: 60px 0;
  display: flex;
  justify-content: center;
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
  margin-top: 12px;
}

.empty-btn:hover {
  background: #e61e3a;
}

.styles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.style-add-card {
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 160px;
}

.style-add-card:hover {
  border-color: #07c160;
  background: #f6ffed;
}

.style-add-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.style-add-text {
  font-size: 14px;
  color: #595959;
}

.style-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}

.style-card:hover {
  border-color: #ffd1d9;
  box-shadow: 0 2px 12px rgba(255, 36, 66, 0.08);
}

.style-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.style-card-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 10px;
}

.style-card-prompt {
  font-size: 12px;
  color: #bfbfbf;
  line-height: 1.5;
  margin-bottom: 12px;
  flex: 1;
  white-space: pre-line;
}

.style-prompt-full {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  background: #fafafa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  white-space: pre-line;
}

.style-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.style-action-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.style-action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.style-action-btn.style-del-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}
</style>

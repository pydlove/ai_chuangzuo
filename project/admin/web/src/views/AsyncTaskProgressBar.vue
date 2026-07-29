<template>
  <transition name="async-task-slide">
    <div v-if="hasRunning" class="async-task-bar">
      <div
        v-for="task in runningList"
        :key="task.key"
        class="async-task-item"
        :class="{ 'is-failed': task.status === 3 }"
      >
        <div class="task-label">
          <LoadingOutlined v-if="task.status === 0 || task.status === 1" class="task-spin" />
          <CloseCircleOutlined v-else class="task-failed-icon" />
          <span>{{ task.label }}</span>
          <a-tag :color="statusColor(task.status)" size="small" style="margin-left: 8px">
            {{ statusText(task.status) }}
          </a-tag>
        </div>
        <a-progress
          :percent="task.status === 2 ? 100 : (task.status === 3 ? 100 : indeterminatePercent(task))"
          :status="task.status === 3 ? 'exception' : 'active'"
          :show-info="false"
          size="small"
          class="task-progress"
        />
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { LoadingOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { useAsyncTasksStore } from '@/stores/asyncTasks.js'

const store = useAsyncTasksStore()
const { runningList } = storeToRefs(store)
const hasRunning = computed(() => runningList.value.length > 0)

// 后端 status 是离散值（0/1/2/3），没有真实进度。前端用不确定进度动画表达「在跑」。
// 通过给一个低于 100 的随机基数 + status=1 时给到 80%，让 a-progress 自带的 active 动画生效。
const indeterminatePercent = (task) => {
  if (task.status === 1) return 80
  if (task.status === 0) return 30
  return 0
}

const statusText = (s) => {
  if (s === 0) return '排队中'
  if (s === 1) return '生成中'
  if (s === 2) return '已完成'
  if (s === 3) return '失败'
  return '-'
}

const statusColor = (s) => {
  if (s === 0) return 'default'
  if (s === 1) return 'processing'
  if (s === 2) return 'success'
  if (s === 3) return 'error'
  return 'default'
}
</script>

<style scoped>
.async-task-bar {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 320px;
  max-height: 50vh;
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid #eeeeee;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 12px 16px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.async-task-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.async-task-item.is-failed {
  opacity: 0.7;
}
.task-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #262626;
}
.task-spin {
  color: #1677ff;
  animation: async-task-spin 1.2s linear infinite;
}
.task-failed-icon {
  color: #ff4d4f;
}
.task-progress {
  margin: 0;
}
.async-task-slide-enter-active,
.async-task-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.async-task-slide-enter-from,
.async-task-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
@keyframes async-task-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
<template>
  <div class="console-layout">
    <!-- 侧边栏 -->
    <aside class="console-sidebar">
      <div class="console-sidebar-brand">
        <img
          src="https://foruda.gitee.com/images/1782805324201637771/ee4f5810_8060302.png"
          alt="爱创作"
          class="brand-logo"
        />
        <span class="brand-name">爱创作</span>
      </div>
      <nav class="console-sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="console-sidebar-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <div class="console-main">
      <!-- 顶部栏 -->
      <header class="console-header">
        <div class="header-left">
        </div>

        <div class="header-right">
          <a-tooltip title="消息">
            <button class="console-icon-btn">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9z"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
            </button>
          </a-tooltip>
          <a-tooltip title="教程">
            <button class="console-icon-btn">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
              </svg>
            </button>
          </a-tooltip>
          <a-tooltip title="切换主题">
            <button class="console-icon-btn" @click="toggleTheme">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
            </button>
          </a-tooltip>
          <a-tooltip title="反馈">
            <button class="console-icon-btn">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
              </svg>
            </button>
          </a-tooltip>
          <a-tooltip title="关于我们">
            <button class="console-icon-btn">
              <InfoCircleOutlined />
            </button>
          </a-tooltip>
          <span class="console-membership-badge">会员</span>
          <a-dropdown :trigger="['click']">
            <div class="console-avatar">U</div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="settings">
                  <router-link to="/settings">个人中心</router-link>
                </a-menu-item>
                <a-menu-item key="logout">
                  <router-link to="/login">退出登录</router-link>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <div class="console-content">
        <router-view />
      </div>

      <!-- 底部 -->
      <footer class="console-footer">
        <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
        <span>浙ICP备XXXXXXXX号-1</span>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import {
  EditOutlined,
  LoadingOutlined,
  EyeOutlined,
  FolderOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'

const route = useRoute()

const navItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/queue', label: '生成队列', icon: LoadingOutlined },
  { path: '/console/preview', label: '预览导出', icon: EyeOutlined },
  { path: '/console/works', label: '我的作品', icon: FolderOutlined }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const toggleTheme = () => {
  // TODO: 主题切换
}
</script>

<style scoped>
.console-layout {
  display: flex;
  height: 100vh;
}

/* 侧边栏 */
.console-sidebar {
  width: 200px;
  background: var(--color-bg-card);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.console-sidebar-brand {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
  font-weight: 700;
  font-size: 18px;
  cursor: pointer;
  color: var(--color-primary);
}

.brand-logo {
  height: 28px;
  width: auto;
  margin-right: 8px;
}

.brand-name {
  font-weight: 700;
  font-size: 16px;
  color: #000;
}

.console-sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.console-sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--color-text-primary);
  transition: all 0.2s;
  font-size: 14px;
  cursor: pointer;
}

.console-sidebar-item:hover {
  background: var(--color-primary-light);
}

.console-sidebar-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
}

/* 主内容区 */
.console-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 顶部栏 */
.console-header {
  height: 56px;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.console-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.console-membership-badge {
  padding: 4px 10px;
  border-radius: 12px;
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
  font-size: 12px;
  cursor: pointer;
}

.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.header-title {
  font-weight: 700;
  font-size: 16px;
  color: var(--color-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.console-icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
  transition: all 0.2s;
}

.console-icon-btn:hover {
  background: var(--color-primary-light);
}

.console-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 内容区 */
.console-content {
  flex: 1;
  padding: 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

/* 底部 */
.console-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 13px;
  text-align: center;
  background: var(--color-bg-card);
}

.console-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: var(--color-border-light);
}
</style>

<template>
  <a-layout class="admin-layout">
    <!-- 侧边栏 -->
    <a-layout-sider width="200" class="admin-sider">
      <div class="sider-brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="sider-logo"
        />
        <div class="sider-brand-text">
          <div class="sider-brand-name">爱创作</div>
          <div class="sider-brand-tag">管理控制台</div>
        </div>
      </div>
      <a-menu
        mode="inline"
        :selected-keys="[$route.path]"
        class="admin-menu"
        @click="handleMenuClick"
      >
        <a-menu-item key="/console/users">
          <template #icon>
            <UserOutlined />
          </template>
          用户管理
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶部 header -->
      <a-layout-header class="admin-header">
        <a-breadcrumb class="admin-breadcrumb">
          <a-breadcrumb-item>首页</a-breadcrumb-item>
          <a-breadcrumb-item>{{ currentMenuName }}</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="admin-user">
          <a-avatar class="admin-avatar">{{ userInitial }}</a-avatar>
          <span class="admin-user-name">{{ userName }}</span>
          <a-button type="link" size="small" @click="handleLogout">退出登录</a-button>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.name || '管理员')
const userInitial = computed(() => userName.value.charAt(0))
const currentMenuName = computed(() => {
  if (route.path === '/console/users') return '用户管理'
  return ''
})

const handleMenuClick = ({ key }) => {
  router.push(key)
}

const handleLogout = () => {
  userStore.clearToken()
  message.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-sider {
  background: #ffffff;
  border-right: 1px solid #eeeeee;
}

.sider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #eeeeee;
}

.sider-logo {
  height: 32px;
  width: auto;
}

.sider-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.sider-brand-name {
  font-weight: 700;
  font-size: 16px;
  color: #1a1a1a;
}

.sider-brand-tag {
  font-size: 11px;
  color: #8c8c8c;
}

.admin-menu {
  border-inline-end: none;
  padding: 8px 0;
}

.admin-menu :deep(.ant-menu-item) {
  margin: 4px 8px;
  border-radius: 8px;
  height: 40px;
  line-height: 40px;
}

.admin-menu :deep(.ant-menu-item-selected) {
  background: #fff0f2;
  color: #ff2442;
}

.admin-header {
  background: #ffffff;
  border-bottom: 1px solid #eeeeee;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  line-height: 56px;
}

.admin-breadcrumb {
  font-size: 14px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-avatar {
  background: #ff2442;
}

.admin-user-name {
  font-size: 14px;
  color: #262626;
}

.admin-content {
  background: #f8f9fa;
  padding: 24px;
  min-height: calc(100vh - 56px);
}
</style>

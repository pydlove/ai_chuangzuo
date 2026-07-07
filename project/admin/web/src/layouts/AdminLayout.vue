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
        :open-keys="openKeys"
        class="admin-menu"
        @click="handleMenuClick"
        @openChange="onOpenChange"
      >
        <a-menu-item key="/console/users">
          <template #icon>
            <UserOutlined />
          </template>
          用户管理
        </a-menu-item>
        <a-menu-item key="/console/styles">
          <template #icon>
            <AuditOutlined />
          </template>
          风格审核
        </a-menu-item>
        <a-sub-menu key="/console/settings">
          <template #icon>
            <SettingOutlined />
          </template>
          <template #title>系统设置</template>
          <a-menu-item key="/console/model-configs">
            <template #icon>
              <ApiOutlined />
            </template>
            模型配置
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="/console/hot-search">
          <template #icon>
            <FireOutlined />
          </template>
          <template #title>热度榜</template>
          <a-menu-item key="/console/hot-search/platforms">平台管理</a-menu-item>
          <a-menu-item key="/console/hot-search/daily">今日榜单</a-menu-item>
          <a-menu-item key="/console/hot-search/config">抓取配置</a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="/console/leaderboard">
          <template #icon>
            <TrophyOutlined />
          </template>
          <template #title>收益排行榜</template>
          <a-menu-item key="/console/leaderboard/review">收入审核</a-menu-item>
          <a-menu-item key="/console/leaderboard/award">奖励发放</a-menu-item>
        </a-sub-menu>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserOutlined, AuditOutlined, SettingOutlined, ApiOutlined, FireOutlined, TrophyOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user.js'

import { adminAuthLogout } from '@/api/auth.js'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '管理员')
const userInitial = computed(() => userName.value.charAt(0))
const openKeys = ref(['/console/settings', '/console/hot-search', '/console/leaderboard'])
const onOpenChange = (keys) => {
  openKeys.value = keys
}
const currentMenuName = computed(() => {
  if (route.path === '/console/users') return '用户管理'
  if (route.path === '/console/styles') return '风格审核'
  if (route.path === '/console/model-configs') return '模型配置'
  if (route.path === '/console/hot-search/platforms') return '平台管理'
  if (route.path === '/console/hot-search/daily') return '今日榜单'
  if (route.path === '/console/hot-search/config') return '抓取配置'
  if (route.path === '/console/leaderboard/review') return '收入审核'
  if (route.path === '/console/leaderboard/award') return '奖励发放'
  return ''
})

const handleMenuClick = ({ key }) => {
  router.push(key)
}

const handleLogout = async () => {
  try {
    await adminAuthLogout()
  } catch (err) {
    // 忽略网络错误，继续清理前端状态
  }
  userStore.clearToken()
  localStorage.removeItem('admin_refresh_token')
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

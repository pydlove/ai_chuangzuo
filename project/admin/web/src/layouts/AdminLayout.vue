<template>
  <a-layout class="admin-layout">
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
      <div class="admin-menu">
        <div
          v-for="group in menuGroups"
          :key="group.key"
          class="menu-group"
        >
          <div
            class="menu-group-title"
            :class="{ active: selectedKey === group.key || activeGroupKey === group.key }"
            @click="group.children ? toggleGroup(group.key) : handleMenuClick({ key: group.key })"
          >
            <span class="menu-group-icon"><component :is="group.icon" /></span>
            <span class="menu-group-label">{{ group.title }}</span>
            <span v-if="group.children" class="menu-group-arrow">
              <RightOutlined v-if="activeGroupKey !== group.key" />
              <DownOutlined v-else />
            </span>
          </div>
          <div v-if="group.children && activeGroupKey === group.key" class="menu-group-items">
            <div
              v-for="item in group.children"
              :key="item.key"
              class="menu-item"
              :class="{ selected: selectedKey === item.key || (item.matchPrefix && selectedKey.startsWith(item.matchPrefix)) }"
              @click="handleMenuClick({ key: item.key })"
            >
              <span v-if="item.icon" class="menu-item-icon"><component :is="item.icon" /></span>
              <span class="menu-item-label">{{ item.title }}</span>
            </div>
          </div>
        </div>
      </div>
    </a-layout-sider>

    <a-layout class="admin-main">
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

      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>

    <!-- 全局异步任务进度条（页脚右下角） -->
    <AsyncTaskProgressBar />
  </a-layout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  UserOutlined, SettingOutlined, ApiOutlined, FireOutlined, TrophyOutlined, DollarOutlined,
  BookOutlined, ReadOutlined, MessageOutlined, CommentOutlined, FileTextOutlined, ExperimentOutlined,
  UnorderedListOutlined, SlidersOutlined, PictureOutlined, ShoppingCartOutlined, BulbOutlined,
  TagsOutlined, ProfileOutlined, SafetyOutlined, FileSearchOutlined, RocketOutlined, ShareAltOutlined, AppstoreOutlined,
  ClockCircleOutlined, DownOutlined, RightOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user.js'

import { adminAuthLogout } from '@/api/auth.js'
import AsyncTaskProgressBar from '@/views/AsyncTaskProgressBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '管理员')
const userInitial = computed(() => userName.value.charAt(0))

const selectedKey = computed(() => route.path)

const menuGroups = [
  {
    key: '/console/self-media',
    title: '自媒体管理',
    icon: AppstoreOutlined,
    children: [
      { key: '/console/self-media/platforms', title: '平台管理' }
    ]
  },
  {
    key: '/console/creation',
    title: '创作管理',
    icon: ExperimentOutlined,
    children: [
      { key: '/console/creation-queue', title: '创作队列', icon: UnorderedListOutlined },
      { key: '/console/topic-titles', title: '标题管理', icon: BulbOutlined },
      { key: '/console/creation-settings', title: '创作设置', icon: SlidersOutlined },
      { key: '/console/export-templates', title: '导出模板', icon: ProfileOutlined },
      { key: '/console/prompt-templates', title: '创作提示词', icon: FileTextOutlined, matchPrefix: '/console/prompt-templates' }
    ]
  },
  {
    key: '/console/user-management',
    title: '用户管理',
    icon: UserOutlined,
    children: [
      { key: '/console/users', title: '注册用户' },
      { key: '/console/expire-reminder', title: '到期提醒' }
    ]
  },
  { key: '/console/commission-tasks', title: '约稿管理', icon: FileTextOutlined },
  {
    key: '/console/skill-management',
    title: '提示词管理',
    icon: BookOutlined,
    children: [
      { key: '/console/skills', title: '提示词审核' },
      { key: '/console/global-skills', title: '预设提示词' },
      { key: '/console/market-skills', title: '提示词市场' }
    ]
  },
  {
    key: '/console/learn',
    title: '创作学院',
    icon: ReadOutlined,
    children: [
      { key: '/console/learn/category', title: '分类管理' },
      { key: '/console/learn/article', title: '文章管理', matchPrefix: '/console/learn/article' },
      { key: '/console/learn/banner', title: 'Banner 管理' }
    ]
  },
  {
    key: '/console/hot-search',
    title: '热度榜',
    icon: FireOutlined,
    children: [
      { key: '/console/hot-search/platforms', title: '平台管理' },
      { key: '/console/hot-search/daily', title: '今日榜单' },
      { key: '/console/hot-search/config', title: '抓取配置' }
    ]
  },
  {
    key: '/console/leaderboard',
    title: '收益排行榜',
    icon: TrophyOutlined,
    children: [
      { key: '/console/leaderboard/award', title: '奖励发放' }
    ]
  },
  {
    key: '/console/earnings',
    title: '收益管理',
    icon: DollarOutlined,
    children: [
      { key: '/console/earnings/accounts', title: '账户明细' },
      { key: '/console/earnings/withdrawals', title: '创作币提现' }
    ]
  },
  {
    key: '/console/orders',
    title: '订单管理',
    icon: ShoppingCartOutlined,
    children: [
      { key: '/console/orders/list', title: '订单列表' },
      { key: '/console/orders/stats', title: '数据统计' },
      { key: '/console/orders/renewal', title: '续费统计' }
    ]
  },
  {
    key: '/console/operations',
    title: '运营管理',
    icon: RocketOutlined,
    children: [
      { key: '/console/share-config', title: '分享管理', icon: ShareAltOutlined },
      { key: '/console/lottery', title: '抽奖活动', icon: FireOutlined }
    ]
  },
  {
    key: '/console/settings',
    title: '系统设置',
    icon: SettingOutlined,
    children: [
      { key: '/console/ai-prompts', title: 'AI 提示词管理', matchPrefix: '/console/ai-prompts' },
      { key: '/console/plans', title: '套餐管理', icon: TagsOutlined },
      { key: '/console/model-configs', title: '模型配置', icon: ApiOutlined },
      { key: '/console/home-banner', title: '首页 Banner', icon: PictureOutlined },
      { key: '/console/messages', title: '消息管理', icon: MessageOutlined },
      { key: '/console/feedbacks', title: '用户反馈', icon: CommentOutlined },
      { key: '/console/security-settings', title: '安全设置', icon: SafetyOutlined },
      { key: '/console/sms-config', title: '短信配置', icon: MessageOutlined },
      { key: '/console/scheduled-tasks', title: '定时任务', icon: ClockCircleOutlined },
      { key: '/console/audit-logs', title: '操作审计', icon: FileSearchOutlined }
    ]
  }
]

function parentGroupKey(path) {
  for (const group of menuGroups) {
    if (group.key === path) return group.key
    if (group.children?.some(item => item.key === path || (item.matchPrefix && path.startsWith(item.matchPrefix)))) {
      return group.key
    }
  }
  return ''
}

const activeGroupKey = ref('')
watch(() => route.path, (path) => {
  activeGroupKey.value = parentGroupKey(path)
}, { immediate: true })

const toggleGroup = (key) => {
  activeGroupKey.value = activeGroupKey.value === key ? '' : key
}

const currentMenuName = computed(() => {
  for (const group of menuGroups) {
    if (group.key === route.path) return group.title
    const child = group.children?.find(item => item.key === route.path || (item.matchPrefix && route.path.startsWith(item.matchPrefix)))
    if (child) return child.title
  }
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
  height: 100vh;
  overflow: hidden;
}

.admin-sider {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 200px;
  height: 100vh;
  background: #ffffff;
  border-right: 1px solid #eeeeee;
  z-index: 100;
}

.admin-sider :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.admin-main {
  margin-left: 200px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sider-brand {
  flex: 0 0 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 20px;
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
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.menu-group {
  margin-bottom: 4px;
}

.menu-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 16px;
  margin: 0 8px;
  border-radius: 8px;
  color: #262626;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  user-select: none;
}

.menu-group-title:hover {
  background: #f5f5f5;
}

.menu-group-title.active {
  color: #ff2442;
}

.menu-group-icon {
  display: inline-flex;
  font-size: 14px;
  width: 16px;
  justify-content: center;
}

.menu-group-label {
  flex: 1;
  font-size: 14px;
}

.menu-group-arrow {
  font-size: 12px;
  color: #8c8c8c;
}

.menu-group-items {
  padding: 4px 8px 8px 44px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  color: #595959;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  font-size: 13px;
}

.menu-item:hover {
  background: #f5f5f5;
  color: #262626;
}

.menu-item.selected {
  background: #fff0f2;
  color: #ff2442;
}

.menu-item-icon {
  display: inline-flex;
  font-size: 14px;
}

.admin-header {
  flex: 0 0 64px;
  background: #ffffff;
  border-bottom: 1px solid #eeeeee;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  line-height: 64px;
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
  flex: 1;
  overflow-y: auto;
  background: #f8f9fa;
  padding: 24px;
}
</style>

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
        <a-sub-menu key="/console/creation">
          <template #icon>
            <ExperimentOutlined />
          </template>
          <template #title>创作管理</template>
          <a-menu-item key="/console/creation-queue">
            <template #icon>
              <UnorderedListOutlined />
            </template>
            创作队列
          </a-menu-item>
          <a-menu-item key="/console/topic-titles">
            <template #icon>
              <BulbOutlined />
            </template>
            标题管理
          </a-menu-item>
          <a-menu-item key="/console/creation-settings">
            <template #icon>
              <SlidersOutlined />
            </template>
            创作设置
          </a-menu-item>
          <a-menu-item key="/console/export-templates">
            <template #icon>
              <ProfileOutlined />
            </template>
            导出模板
          </a-menu-item>
          <a-menu-item key="/console/prompt-templates">
            <template #icon>
              <FileTextOutlined />
            </template>
            创作提示词
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="/console/user-management">
          <template #icon>
            <UserOutlined />
          </template>
          <template #title>用户管理</template>
          <a-menu-item key="/console/users">注册用户</a-menu-item>
          <a-menu-item key="/console/expire-reminder">到期提醒</a-menu-item>
        </a-sub-menu>
        <a-menu-item key="/console/commission-tasks">
          <template #icon>
            <FileTextOutlined />
          </template>
          约稿管理
        </a-menu-item>
        <a-menu-item key="/console/lottery">
          <template #icon>
            <FireOutlined />
          </template>
          抽奖活动
        </a-menu-item>
        <a-sub-menu key="/console/skill-management">
          <template #icon>
            <BookOutlined />
          </template>
          <template #title>提示词管理</template>
          <a-menu-item key="/console/skills">
            提示词审核
          </a-menu-item>
          <a-menu-item key="/console/global-skills">
            预设提示词
          </a-menu-item>
          <a-menu-item key="/console/market-skills">
            提示词市场
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="/console/learn">
          <template #icon>
            <ReadOutlined />
          </template>
          <template #title>创作学院</template>
          <a-menu-item key="/console/learn/category">分类管理</a-menu-item>
          <a-menu-item key="/console/learn/article">文章管理</a-menu-item>
          <a-menu-item key="/console/learn/banner">Banner 管理</a-menu-item>
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
          <!-- 自媒体收入榜功能暂时隐藏
          <a-menu-item key="/console/leaderboard/review">收入审核</a-menu-item>
          -->
          <a-menu-item key="/console/leaderboard/award">奖励发放</a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="/console/earnings">
          <template #icon>
            <DollarOutlined />
          </template>
          <template #title>收益管理</template>
          <a-menu-item key="/console/earnings/accounts">账户明细</a-menu-item>
          <a-menu-item key="/console/earnings/withdrawals">创作币提现</a-menu-item>
          <!-- 自媒体收入榜功能暂时隐藏
          <a-menu-item key="/console/earnings/self-media-review">自媒体审核</a-menu-item>
          <a-menu-item key="/console/earnings/leaderboard-awards">榜单发奖</a-menu-item>
          -->
        </a-sub-menu>
        <a-sub-menu key="/console/orders">
          <template #icon>
            <ShoppingCartOutlined />
          </template>
          <template #title>订单管理</template>
          <a-menu-item key="/console/orders/list">订单列表</a-menu-item>
          <a-menu-item key="/console/orders/stats">数据统计</a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="/console/operations">
          <template #icon>
            <RocketOutlined />
          </template>
          <template #title>运营管理</template>
          <a-menu-item key="/console/share-config">
            <template #icon>
              <ShareAltOutlined />
            </template>
            分享管理
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="/console/settings">
          <template #icon>
            <SettingOutlined />
          </template>
          <template #title>系统设置</template>
          <a-menu-item key="/console/plans">
            <template #icon>
              <TagsOutlined />
            </template>
            套餐管理
          </a-menu-item>
          <a-menu-item key="/console/model-configs">
            <template #icon>
              <ApiOutlined />
            </template>
            模型配置
          </a-menu-item>
          <a-menu-item key="/console/home-banner">
            <template #icon>
              <PictureOutlined />
            </template>
            首页 Banner
          </a-menu-item>
          <a-menu-item key="/console/messages">
            <template #icon>
              <MessageOutlined />
            </template>
            消息管理
          </a-menu-item>
          <a-menu-item key="/console/feedbacks">
            <template #icon>
              <CommentOutlined />
            </template>
            用户反馈
          </a-menu-item>
          <a-menu-item key="/console/security-settings">
            <template #icon>
              <SafetyOutlined />
            </template>
            安全设置
          </a-menu-item>
          <a-menu-item key="/console/audit-logs">
            <template #icon>
              <FileSearchOutlined />
            </template>
            操作审计
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout class="admin-main">
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

    <!-- 全局异步任务进度条（页脚右下角） -->
    <AsyncTaskProgressBar />
  </a-layout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserOutlined, AuditOutlined, AppstoreOutlined, SettingOutlined, ApiOutlined, FireOutlined, TrophyOutlined, DollarOutlined, BookOutlined, ReadOutlined, MessageOutlined, CommentOutlined, FileTextOutlined, ExperimentOutlined, UnorderedListOutlined, SlidersOutlined, PictureOutlined, ShoppingCartOutlined, BulbOutlined, TagsOutlined, ProfileOutlined, SafetyOutlined, FileSearchOutlined, RocketOutlined, ShareAltOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user.js'

import { adminAuthLogout } from '@/api/auth.js'
import AsyncTaskProgressBar from '@/views/AsyncTaskProgressBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '管理员')
const userInitial = computed(() => userName.value.charAt(0))

// 当前路由所在的一级菜单 key（手风琴模式只展开这一个）
const parentMenuKey = computed(() => {
  const p = route.path
  if (p === '/console/creation-queue' || p === '/console/creation-settings' || p === '/console/topic-titles' || p.startsWith('/console/prompt-templates') || p === '/console/export-templates') return '/console/creation'
  if (p === '/console/users' || p === '/console/expire-reminder') return '/console/user-management'
  if (p === '/console/lottery') return null
  if (p === '/console/skills' || p === '/console/global-skills' || p === '/console/market-skills') return '/console/skill-management'
  if (p.startsWith('/console/learn/')) return '/console/learn'
  if (p.startsWith('/console/hot-search/')) return '/console/hot-search'
  if (p.startsWith('/console/leaderboard/')) return '/console/leaderboard'
  if (p.startsWith('/console/earnings/')) return '/console/earnings'
  if (p.startsWith('/console/orders/')) return '/console/orders'
  if (p === '/console/share-config') return '/console/operations'
  if (p === '/console/model-configs' || p === '/console/home-banner' || p === '/console/plans' || p === '/console/messages' || p === '/console/feedbacks' || p === '/console/security-settings' || p === '/console/audit-logs') return '/console/settings'
  return null
})

const openKeys = ref([])
// 路由变化时只展开当前一级菜单（手风琴：其它自动折叠）
watch(parentMenuKey, (key) => {
  openKeys.value = key ? [key] : []
}, { immediate: true })

// 用户手动展开时也只保留最新一个（手风琴）
const onOpenChange = (keys) => {
  const latest = keys[keys.length - 1]
  openKeys.value = latest ? [latest] : []
}
const currentMenuName = computed(() => {
  if (route.path === '/console/users') return '用户管理'
  if (route.path === '/console/skills' || route.path === '/console/global-skills' || route.path === '/console/market-skills') return '提示词管理'
  if (route.path === '/console/model-configs') return '模型配置'
  if (route.path === '/console/prompt-templates' || route.path.startsWith('/console/prompt-templates/')) return '创作提示词'
  if (route.path === '/console/hot-search/platforms') return '平台管理'
  if (route.path === '/console/hot-search/daily') return '今日榜单'
  if (route.path === '/console/hot-search/config') return '抓取配置'
  // 自媒体收入榜功能暂时隐藏
  // if (route.path === '/console/leaderboard/review') return '收入审核'
  if (route.path === '/console/leaderboard/award') return '奖励发放'
  if (route.path === '/console/earnings/accounts') return '账户明细'
  if (route.path === '/console/earnings/withdrawals') return '创作币提现'
  if (route.path === '/console/earnings/settlements') return '结算中心'
  // 自媒体收入榜功能暂时隐藏
  // if (route.path === '/console/earnings/self-media-review') return '自媒体审核'
  // if (route.path === '/console/earnings/leaderboard-awards') return '榜单发奖'
  if (route.path === '/console/commission-tasks') return '约稿管理'
  if (route.path === '/console/lottery') return '抽奖活动'
  if (route.path === '/console/messages') return '消息管理'
  if (route.path === '/console/feedbacks') return '用户反馈'
  if (route.path === '/console/expire-reminder') return '到期提醒'
  if (route.path === '/console/creation-queue') return '创作队列'
  if (route.path === '/console/topic-titles') return '标题管理'
  if (route.path === '/console/creation-settings') return '创作设置'
  if (route.path === '/console/export-templates') return '导出模板'
  if (route.path === '/console/learn/category') return '分类管理'
  if (route.path === '/console/learn/article') return '文章管理'
  if (route.path.startsWith('/console/learn/article/edit')) return '文章编辑'
  if (route.path === '/console/orders/list') return '订单列表'
  if (route.path === '/console/orders/stats') return '数据统计'
  if (route.path === '/console/share-config') return '分享管理'
  if (route.path === '/console/home-banner') return '首页 Banner'
  if (route.path === '/console/plans') return '套餐管理'
  if (route.path === '/console/security-settings') return '安全设置'
  if (route.path === '/console/audit-logs') return '操作审计'
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

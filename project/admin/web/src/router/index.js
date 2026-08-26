import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user.js'

const routes = [
  {
    path: '/login',
    name: 'AdminLogin',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/console',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/console/users',
    children: [
     {
       path: 'users',
       name: 'AdminUserList',
       component: () => import('@/views/UserListView.vue')
     },
      {
        path: 'self-media/platforms',
        name: 'AdminSelfMediaPlatforms',
        component: () => import('@/views/SelfMediaPlatformView.vue')
      },
     {
       path: 'skills',
       name: 'AdminSkillReview',
        component: () => import('@/views/SkillReviewView.vue')
      },
      {
        path: 'global-skills',
        name: 'AdminGlobalSkillList',
        component: () => import('@/views/GlobalSkillListView.vue')
      },
      {
        path: 'market-skills',
        name: 'AdminMarketSkillList',
        component: () => import('@/views/MarketSkillListView.vue')
      },
      {
        path: 'model-configs',
        name: 'AdminModelConfig',
        component: () => import('@/views/ModelConfigView.vue')
      },
      {
        path: 'prompt-templates',
        name: 'AdminPromptTemplateList',
        component: () => import('@/views/PromptTemplateListView.vue')
      },
      {
        path: 'prompt-templates/new',
        name: 'AdminPromptTemplateCreate',
        component: () => import('@/views/PromptTemplateEditView.vue')
      },
      {
        path: 'prompt-templates/:id',
        name: 'AdminPromptTemplateEdit',
        component: () => import('@/views/PromptTemplateEditView.vue')
      },
      {
        path: 'creation-queue',
        name: 'AdminCreationQueue',
        component: () => import('@/views/CreationQueueView.vue')
      },
      {
        path: 'topic-titles',
        name: 'AdminTopicTitleList',
        component: () => import('@/views/TopicTitleListView.vue')
      },
      {
        path: 'creation-settings',
        name: 'AdminCreationSettings',
        component: () => import('@/views/CreationSettingsView.vue')
      },
      {
        path: 'export-templates',
        name: 'AdminExportTemplateEdit',
        component: () => import('@/views/ExportTemplateEditView.vue')
      },
      {
        path: 'hot-search/platforms',
        name: 'AdminHotSearchPlatforms',
        component: () => import('@/views/HotSearchPlatformView.vue')
      },
      {
        path: 'hot-search/daily',
        name: 'AdminHotSearchDaily',
        component: () => import('@/views/HotSearchDailyView.vue')
      },
      {
        path: 'hot-search/config',
        name: 'AdminHotSearchConfig',
        component: () => import('@/views/HotSearchConfigView.vue')
      },
      // 自媒体收入榜功能暂时隐藏
      // {
      //   path: 'leaderboard/review',
      //   name: 'AdminLeaderboardReview',
      //   component: () => import('@/views/LeaderboardReviewView.vue')
      // },
      {
        path: 'leaderboard/award',
        name: 'AdminLeaderboardAward',
        component: () => import('@/views/LeaderboardAwardView.vue')
      },
      {
        path: 'earnings/accounts',
        name: 'AdminEarningsAccounts',
        component: () => import('@/views/AccountQueryView.vue')
      },
      {
        path: 'earnings/withdrawals',
        name: 'AdminEarningsWithdrawals',
        component: () => import('@/views/WithdrawAdminView.vue')
      },
      // 自媒体收入榜功能暂时隐藏
      // {
      //   path: 'earnings/self-media-review',
      //   name: 'AdminEarningsSelfMediaReview',
      //   component: () => import('@/views/SelfMediaReviewView.vue')
      // },
      // {
      //   path: 'earnings/leaderboard-awards',
      //   name: 'AdminEarningsLeaderboardAwards',
      //   component: () => import('@/views/LeaderboardAwardView.vue')
      // },
      {
        path: 'commission-tasks',
        name: 'AdminCommissionTask',
        component: () => import('@/views/CommissionTaskView.vue')
      },
      {
        path: 'lottery',
        name: 'AdminLottery',
        component: () => import('@/views/LotteryAdminView.vue')
      },
      {
        path: 'share-config',
        name: 'AdminShareConfig',
        component: () => import('@/views/ShareConfigView.vue')
      },
      {
        path: 'messages',
        name: 'AdminMessageManagement',
        component: () => import('@/views/MessageAdminView.vue')
      },
      {
        path: 'feedbacks',
        name: 'AdminFeedbacks',
        component: () => import('@/views/FeedbackView.vue')
      },
      {
        path: 'expire-reminder',
        name: 'AdminExpireReminder',
        component: () => import('@/views/ExpireReminderView.vue')
      },
      {
        path: 'learn/category',
        name: 'AdminLearnCategory',
        component: () => import('@/views/LearnCategoryView.vue')
      },
      {
        path: 'learn/article',
        name: 'AdminLearnArticleList',
        component: () => import('@/views/LearnArticleListView.vue')
      },
      {
        path: 'learn/article/edit/:id?',
        name: 'AdminLearnArticleEdit',
        component: () => import('@/views/LearnArticleEditView.vue')
      },
      {
        path: 'learn/banner',
        name: 'AdminLearnBanner',
        component: () => import('@/views/LearnBannerView.vue')
      },
      {
        path: 'home-banner',
        name: 'AdminHomeBanner',
        component: () => import('@/views/HomeBannerView.vue')
      },
      {
        path: 'home-testimonials',
        name: 'AdminHomeTestimonials',
        component: () => import('@/views/HomeTestimonialView.vue')
      },
      {
        path: 'orders/list',
        name: 'AdminOrderList',
        component: () => import('@/views/OrderListView.vue')
      },
      {
        path: 'orders/stats',
        name: 'AdminOrderStats',
        component: () => import('@/views/OrderStatsView.vue')
      },
      {
        path: 'orders/renewal',
        name: 'AdminOrderRenewal',
        component: () => import('@/views/RenewalStatsView.vue')
      },
      {
        path: 'plans',
        name: 'AdminPlanList',
        component: () => import('@/views/PlanListView.vue')
      },
      {
        path: 'sms-config',
        name: 'AdminSmsConfig',
        component: () => import('@/views/SmsConfigView.vue')
      },
      {
        path: 'security-settings',
        name: 'AdminSecuritySettings',
        component: () => import('@/views/SecuritySettingsView.vue')
      },
      {
        path: 'audit-logs',
        name: 'AdminAuditLogList',
        component: () => import('@/views/AuditLogListView.vue')
      },
      {
        path: 'ai-prompts',
        name: 'AdminAiPromptList',
        component: () => import('@/views/AiPromptListView.vue')
      },
      {
        path: 'ai-prompts/new',
        name: 'AdminAiPromptCreate',
        component: () => import('@/views/AiPromptEditView.vue')
      },
      {
        path: 'ai-prompts/:id',
        name: 'AdminAiPromptEdit',
        component: () => import('@/views/AiPromptEditView.vue')
      },
      {
        path: 'scheduled-tasks',
        name: 'AdminScheduledTask',
        component: () => import('@/views/ScheduledTaskView.vue')
      },
      {
        path: 'tools',
        name: 'AdminToolManagement',
        component: () => import('@/views/ToolManagementView.vue')
      }
    ]
  },
  {
    path: '/',
    redirect: '/console'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && userStore.isLoggedIn) {
    return '/console/users'
  }
})

export default router

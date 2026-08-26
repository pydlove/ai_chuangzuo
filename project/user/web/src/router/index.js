import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/forgot',
    name: 'Forgot',
    component: () => import('@/views/Forgot.vue')
  },
  {
    path: '/pricing',
    name: 'Pricing',
    component: () => import('@/views/Pricing.vue')
  },
  {
    path: '/learn',
    name: 'Learn',
    component: () => import('@/views/LearnIndex.vue')
  },
  {
    path: '/learn/article/:id',
    name: 'LearnArticle',
    component: () => import('@/views/LearnIndex.vue')
  },
  {
    path: '/guide',
    name: 'Guide',
    component: () => import('@/views/GuideIndex.vue')
  },
  {
    path: '/lottery',
    name: 'Lottery',
    component: () => import('@/views/console/LotteryPage.vue'),
    meta: { title: '抽奖活动' }
  },
  {
    path: '/console',
    name: 'Console',
    component: () => import('@/views/console/ConsoleLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/console/create'
      },
      {
        path: 'workbench',
        name: 'ConsoleWorkbench',
        component: () => import('@/views/console/WorkbenchIndex.vue')
      },
      {
        path: 'onboarding',
        name: 'ConsoleOnboarding',
        component: () => import('@/views/console/OnboardingIndex.vue')
      },
      {
        path: 'create',
        name: 'ConsoleCreate',
        component: () => import('@/views/console/CreateIndex.vue')
      },
      {
        path: 'create/recommended',
        name: 'ConsoleCreateRecommended',
        component: () => import('@/views/console/CreateRecommendedPage.vue'),
        meta: { title: '小爱推荐', hideMobileSubpageHeader: true }
      },
      {
        path: 'create/free',
        name: 'ConsoleCreateFree',
        component: () => import('@/views/console/CreateFreePage.vue'),
        meta: { title: '自由创作', hideMobileSubpageHeader: true }
      },
      {
        path: 'queue',
        redirect: '/console/create'
      },
      {
        path: 'works',
        name: 'ConsoleWorks',
        component: () => import('@/views/console/WorksIndex.vue')
      },
      {
        path: 'skills',
        name: 'ConsoleSkills',
        component: () => import('@/views/console/SkillsIndex.vue')
      },
      {
        path: 'skill-market',
        name: 'ConsoleSkillMarket',
        component: () => import('@/views/console/SkillMarketIndex.vue')
      },
      {
        path: 'earnings',
        name: 'ConsoleEarnings',
        component: () => import('@/views/console/EarningsIndex.vue')
      },
      {
        path: 'edit/:bizNo',
        name: 'ConsoleEdit',
        component: () => import('@/views/console/EditIndex.vue')
      },
      {
        path: 'preview/:bizNo',
        name: 'ConsolePreview',
        component: () => import('@/views/console/PreviewIndex.vue')
      },
      {
        path: 'coin',
        name: 'ConsoleCoin',
        component: () => import('@/views/console/WithdrawIndex.vue')
      },
      {
        path: 'hot-search',
        name: 'ConsoleHotSearch',
        component: () => import('@/views/console/HotSearchIndex.vue')
      },
      {
        path: 'learn',
        name: 'ConsoleLearn',
        component: () => import('@/views/console/ConsoleLearnIndex.vue')
      },
      {
        path: 'learn/article/:id',
        name: 'ConsoleLearnArticle',
        component: () => import('@/views/console/ConsoleLearnIndex.vue')
      },
      {
        path: 'messages',
        name: 'ConsoleMessages',
        component: () => import('@/views/console/MessagesIndex.vue')
      },
      {
        path: 'leaderboard',
        name: 'ConsoleLeaderboard',
        component: () => import('@/views/console/LeaderboardIndex.vue')
      },
      {
        path: 'mine',
        name: 'ConsoleMine',
        component: () => import('@/views/console/MineIndex.vue')
      },
      {
        path: 'account-check',
        name: 'ConsoleAccountCheck',
        component: () => import('@/views/console/AccountCheckIndex.vue'),
        meta: { title: '平台账号检测' }
      },
      {
        path: 'weekly-data',
        name: 'ConsoleWeeklyData',
        component: () => import('@/views/console/WeeklyDataIndex.vue'),
        meta: { title: '录入本周数据' }
      },
      {
        path: 'benefits',
        name: 'ConsoleBenefits',
        component: () => import('@/views/console/BenefitsIndex.vue')
      },
      {
        path: 'invite',
        name: 'ConsoleInvite',
        component: () => import('@/views/console/InviteIndex.vue')
      },
      {
        path: 'invite-rules',
        name: 'ConsoleInviteRules',
        component: () => import('@/views/console/InviteRulesIndex.vue')
      },
      {
        path: 'activities',
        name: 'ConsoleActivities',
        component: () => import('@/views/console/ActivitiesIndex.vue')
      },
      {
        path: 'lottery',
        name: 'ConsoleLottery',
        component: () => import('@/views/console/LotteryPage.vue'),
        meta: { title: '抽奖活动' }
      },
      {
        path: 'commission',
        name: 'ConsoleCommission',
        component: () => import('@/views/console/CommissionIndex.vue')
      },
      {
        path: 'commission/:id',
        name: 'ConsoleCommissionDetail',
        component: () => import('@/views/console/CommissionDetail.vue')
      },
      {
        path: 'coupons',
        name: 'ConsoleCoupons',
        component: () => import('@/views/console/CouponIndex.vue'),
        meta: { title: '我的优惠券' }
      },
      {
        path: 'orders',
        name: 'ConsoleOrders',
        component: () => import('@/views/console/OrderIndex.vue'),
        meta: { title: '我的订单' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (to.hash) {
      return false
    }
    return savedPosition || { top: 0 }
  }
})

router.beforeEach((to) => {
  const token = localStorage.getItem('aichuangzuo_access_token')
  if (token && to.path === '/login') {
    return { path: '/console' }
  }
  if (!token && (to.path.startsWith('/console') || to.meta?.requireAuth)) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

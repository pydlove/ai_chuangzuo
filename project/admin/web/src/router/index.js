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
        path: 'styles',
        name: 'AdminStyleReview',
        component: () => import('@/views/StyleReviewView.vue')
      },
      {
        path: 'global-styles',
        name: 'AdminGlobalStyleList',
        component: () => import('@/views/GlobalStyleListView.vue')
      },
      {
        path: 'market-styles',
        name: 'AdminMarketStyleList',
        component: () => import('@/views/MarketStyleListView.vue')
      },
      {
        path: 'model-configs',
        name: 'AdminModelConfig',
        component: () => import('@/views/ModelConfigView.vue')
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
      {
        path: 'leaderboard/review',
        name: 'AdminLeaderboardReview',
        component: () => import('@/views/LeaderboardReviewView.vue')
      },
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
        path: 'earnings/settlements',
        name: 'AdminEarningsSettlements',
        component: () => import('@/views/SettlementView.vue')
      },
      {
        path: 'earnings/self-media-review',
        name: 'AdminEarningsSelfMediaReview',
        component: () => import('@/views/SelfMediaReviewView.vue')
      },
      {
        path: 'earnings/leaderboard-awards',
        name: 'AdminEarningsLeaderboardAwards',
        component: () => import('@/views/LeaderboardAwardView.vue')
      },
      {
        path: 'messages',
        name: 'AdminMessageManagement',
        component: () => import('@/views/MessageAdminView.vue')
      },
      {
        path: 'expire-reminder',
        name: 'AdminExpireReminder',
        component: () => import('@/views/ExpireReminderView.vue')
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

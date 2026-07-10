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
    path: '/guide',
    name: 'Guide',
    component: () => import('@/views/GuideIndex.vue')
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
        path: 'create',
        name: 'ConsoleCreate',
        component: () => import('@/views/console/CreateIndex.vue')
      },
      {
        path: 'queue',
        redirect: '/console/create'
      },
      {
        path: 'ai-generate',
        name: 'ConsoleAiGenerate',
        component: () => import('@/views/console/GenerationQueueIndex.vue')
      },
      {
        path: 'works',
        name: 'ConsoleWorks',
        component: () => import('@/views/console/WorksIndex.vue')
      },
      {
        path: 'styles',
        name: 'ConsoleStyles',
        component: () => import('@/views/console/StylesIndex.vue')
      },
      {
        path: 'style-market',
        name: 'ConsoleStyleMarket',
        component: () => import('@/views/console/StyleMarketIndex.vue')
      },
      {
        path: 'earnings',
        name: 'ConsoleEarnings',
        component: () => import('@/views/console/EarningsIndex.vue')
      },
      {
        path: 'edit',
        name: 'ConsoleEdit',
        component: () => import('@/views/console/EditIndex.vue')
      },
      {
        path: 'preview',
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
        path: 'invite',
        name: 'ConsoleInvite',
        component: () => import('@/views/console/InviteIndex.vue')
      },
      {
        path: 'invite-rules',
        name: 'ConsoleInviteRules',
        component: () => import('@/views/console/InviteRulesIndex.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('aichuangzuo_access_token')
  if (token && to.path === '/login') {
    return { path: '/console' }
  }
})

export default router

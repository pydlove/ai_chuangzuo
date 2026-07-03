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
    name: 'AdminConsole',
    component: () => import('@/views/ConsoleView.vue'),
    meta: { requiresAuth: true }
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
    return '/console'
  }
})

export default router

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
        path: 'model-configs',
        name: 'AdminModelConfig',
        component: () => import('@/views/ModelConfigView.vue')
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

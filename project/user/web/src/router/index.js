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
        redirect: '/console/works'
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
        path: 'preview',
        name: 'ConsolePreview',
        component: () => import('@/views/console/PreviewIndex.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

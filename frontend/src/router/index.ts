import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { withoutAuth: true }
    },
    {
      path: '/',
      name: 'Main',
      component: () => import('@/views/layout/MainLayout.vue'),
      redirect: '/projects',
      children: [
        {
          path: 'projects',
          name: 'ProjectList',
          component: () => import('@/views/project/ProjectList.vue'),
          meta: { title: '项目管理' }
        },
        {
          path: 'projects/:id',
          name: 'ProjectDetail',
          component: () => import('@/views/project/ProjectDetail.vue'),
          meta: { title: '项目详情' }
        },
        {
          path: 'documents',
          name: 'DocFileList',
          component: () => import('@/views/document/DocFileList.vue'),
          meta: { title: '文档管理' }
        },
        {
          path: 'catalogs',
          name: 'DocCatalogList',
          component: () => import('@/views/document/DocCatalogList.vue'),
          meta: { title: '文档目录' }
        },
        {
          path: 'meetings',
          name: 'MeetingList',
          component: () => import('@/views/review/MeetingList.vue'),
          meta: { title: '评审会议' }
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  if (!to.meta.withoutAuth && !isLoggedIn()) {
    next('/login')
  } else if (to.meta.withoutAuth && isLoggedIn()) {
    next('/')
  } else {
    next()
  }
})

export default router

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
        // 项目管理
        {
          path: 'projects',
          name: 'ProjectList',
          component: () => import('@/views/project/ProjectList.vue'),
          meta: { title: '项目管理' }
        },
        // 项目工作台 (7 tabs)
        {
          path: 'projects/:projectId',
          name: 'ProjectWorkspace',
          component: () => import('@/views/project/ProjectWorkspace.vue'),
          meta: { title: '项目工作台' },
          redirect: (to: any) => ({ name: 'ProjectOverview', params: { projectId: to.params.projectId } }),
          children: [
            {
              path: 'overview',
              name: 'ProjectOverview',
              component: () => import('@/views/project/ProjectDetail.vue'),
              meta: { title: '项目概览' }
            },
            {
              path: 'stages',
              name: 'ProjectStages',
              component: () => import('@/views/project/ProjectStageList.vue'),
              meta: { title: '阶段管理' }
            },
            {
              path: 'doc-ledger',
              name: 'ProjectDocLedger',
              component: () => import('@/views/document/DocKanbanBoard.vue'),
              meta: { title: '文档台账' }
            },
            {
              path: 'reviews',
              name: 'ProjectReviews',
              component: () => import('@/views/review/MeetingList.vue'),
              meta: { title: '评审管理' }
            },
            {
              path: 'members',
              name: 'ProjectMembers',
              component: () => import('@/views/project/ProjectMemberList.vue'),
              meta: { title: '项目成员' }
            },
            {
              path: 'input-files',
              name: 'ProjectInputFiles',
              component: () => import('@/views/project/ProjectInputFileList.vue'),
              meta: { title: '输入文件' }
            }
          ]
        },
        // 模版库
        {
          path: 'templates',
          name: 'TemplateList',
          component: () => import('@/views/template/TemplateList.vue'),
          meta: { title: '模版管理' }
        },
        {
          path: 'templates/create',
          name: 'TemplateEditor',
          component: () => import('@/views/template/TemplateEditor.vue'),
          meta: { title: '从模版创建文档' }
        },
        // 标准库
        {
          path: 'standards',
          name: 'StandardList',
          component: () => import('@/views/standard/StandardList.vue'),
          meta: { title: '标准库' }
        },
        {
          path: 'standards/:id',
          name: 'StandardDetail',
          component: () => import('@/views/standard/StandardDetail.vue'),
          meta: { title: '标准详情' }
        },
        // 知识库
        {
          path: 'knowledge',
          name: 'KnowledgeBaseList',
          component: () => import('@/views/knowledge/KnowledgeBaseList.vue'),
          meta: { title: '知识库' }
        },
        // 系统管理
        {
          path: 'users',
          name: 'UserList',
          component: () => import('@/views/system/UserList.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'roles',
          name: 'RoleList',
          component: () => import('@/views/system/RoleList.vue'),
          meta: { title: '角色管理' }
        },
        {
          path: 'dicts',
          name: 'DictList',
          component: () => import('@/views/system/DictList.vue'),
          meta: { title: '字典配置' }
        },
        {
          path: 'permissions',
          name: 'PermissionList',
          component: () => import('@/views/system/PermissionList.vue'),
          meta: { title: '权限管理' }
        },
        {
          path: 'embedding',
          name: 'EmbeddingManagement',
          component: () => import('@/views/system/EmbeddingManagement.vue'),
          meta: { title: '向量索引' }
        },
        // AI 模型训练
        {
          path: 'ai-training',
          name: 'AiTraining',
          component: () => import('@/views/ai/TrainingManagement.vue'),
          meta: { title: '模型训练' }
        },
        // Sub-feature routes (not in sidebar, accessed from detail drawers)
        {
          path: 'projects/:projectId/stage/:stageId/workbench',
          name: 'StageWorkbench',
          component: () => import('@/views/project/StageWorkbench.vue'),
          meta: { title: '阶段工作台' }
        },
        {
          path: 'projects/:projectId/stage/:stageId/configuration-items',
          name: 'StageConfigurationItems',
          component: () => import('@/views/project/ConfigurationItemsPage.vue'),
          meta: { title: '技术状态项' }
        },
        {
          path: 'projects/:projectId/stage/:stageId/baselines',
          name: 'StageBaselines',
          component: () => import('@/views/project/StageBaselinesPage.vue'),
          meta: { title: '基线管理' }
        },
        {
          path: 'projects/:projectId/stage/:stageId/change-requests',
          name: 'StageChangeRequests',
          component: () => import('@/views/project/ChangeRequestsPage.vue'),
          meta: { title: '更改管理' }
        },
        {
          path: 'projects/:projectId/stage/:stageId/status-accounting',
          name: 'StageStatusAccounting',
          component: () => import('@/views/project/StatusAccountingPage.vue'),
          meta: { title: '技术状态记实' }
        },
        {
          path: 'projects/:projectId/stage/:stageId/audits',
          name: 'StageAudits',
          component: () => import('@/views/project/AuditsPage.vue'),
          meta: { title: '技术状态审核' }
        },
        {
          path: 'projects/:projectId/transitions',
          name: 'StageTransitionList',
          component: () => import('@/views/project/StageTransitionList.vue'),
          meta: { title: '阶段转阶段检查' }
        },
        {
          path: 'documents/:docFileId/versions',
          name: 'DocVersionList',
          component: () => import('@/views/document/DocVersionList.vue'),
          meta: { title: '文档版本管理' }
        },
        {
          path: 'doc-sessions',
          name: 'DocEditSessionList',
          component: () => import('@/views/document/DocEditSessionList.vue'),
          meta: { title: '文档编辑会话' }
        },
        {
          path: 'doc-locks',
          name: 'DocEditLockList',
          component: () => import('@/views/document/DocEditLockList.vue'),
          meta: { title: '文档编辑锁定' }
        },
        {
          path: 'doc-baselines',
          name: 'DocEffectiveBaselineList',
          component: () => import('@/views/document/DocEffectiveBaselineList.vue'),
          meta: { title: '文档生效基线' }
        },
        {
          path: 'doc-change-impacts',
          name: 'DocChangeImpactList',
          component: () => import('@/views/document/DocChangeImpactList.vue'),
          meta: { title: '变更影响分析' }
        },
        {
          path: 'meetings/:meetingId/documents',
          name: 'ReviewMeetingDocumentList',
          component: () => import('@/views/review/ReviewMeetingDocumentList.vue'),
          meta: { title: '评审文档管理' }
        },
        {
          path: 'meetings/:meetingId/expert-opinions',
          name: 'ReviewExpertOpinionList',
          component: () => import('@/views/review/ReviewExpertOpinionList.vue'),
          meta: { title: '专家意见管理' }
        },
        {
          path: 'meetings/:meetingId/opinions',
          name: 'ReviewMeetingOpinionList',
          component: () => import('@/views/review/ReviewMeetingOpinionList.vue'),
          meta: { title: '会议意见汇总' }
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

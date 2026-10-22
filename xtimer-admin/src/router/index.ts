import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/timers' },
    {
      path: '/timers',
      name: 'timers',
      component: () => import('@/views/TimerList.vue'),
      meta: { title: '定时器管理' },
    },
    {
      path: '/tasks',
      name: 'tasks',
      component: () => import('@/views/TaskList.vue'),
      meta: { title: '执行记录' },
    },
  ],
})

export default router

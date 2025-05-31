// router.js
export default [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/main', // 新增主页面路由
    name: 'main',
    component: () => import('@/views/main/index.vue') // 假设路径为 views/main/index.vue
  }
];
// router.js
export default [
  {
    path: '/login',
    redirect: '/auth/login'
  },
  {
    path: '/register',
    redirect: '/auth/register'
  },
  {
    path: '/auth/login',
    name: 'auth.login',
    component: () => import('./login/index.vue')
  },
  {
    path: '/auth/register', // 新增主页面路由
    name: 'auth.register',
    component: () => import('./register/index.vue') // 假设路径为 views/main/index.vue
  },
];
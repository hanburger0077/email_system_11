// router.js
export default [
  {
    path: '/login',
    name: 'Login', // 确保名称与跳转逻辑一致
    component: () => import('@/views/login/index.vue') // 路径根据实际项目调整
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue') // 路径根据实际项目调整
  }
];
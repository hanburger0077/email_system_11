// router.js
export default [
  {
    path: '/old/register',
    name: 'old.register',
    component: () => import('@/views/register/index.vue') // 路径根据实际项目调整
  }
];
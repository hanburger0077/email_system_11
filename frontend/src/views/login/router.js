// router.js
export default [
  {
    path: '/old/login',
    name: 'old.login',
    component: () => import('@/views/login/index.vue')
  },
];
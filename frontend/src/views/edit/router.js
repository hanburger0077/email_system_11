// edit/router.js
export default [
  {
    path: '/edit', // 写信页面路由（保持原路径）
    name: 'edit',
    component: () => import('./index.vue')
  },
];
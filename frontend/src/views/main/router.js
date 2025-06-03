export default [
  {
    path: '/main',
    name: 'main',
    component: () => import('./index.vue')
  },
  {
    path: '/write',
    name: 'write',
    component: () => import('../edit/index.vue')
  },
  {
    path: '/draft',
    name: 'draft',
    component: () => import('../draft/index.vue')
  },
  {
    path: '/star',
    name: 'star',
    component: () => import('../star/index.vue')
  }
];    
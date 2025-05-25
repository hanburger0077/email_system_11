// 安全设置模块

export default [
  {
    path: '/edit',
    name: 'edit',
    component: () => import('./index.vue'),
  },
  {
    path: '/edit/sub',
    name: 'edit.sub',
    component: () =>
      import(/* webpackChunkName: "edit" */ './sub.vue'),
  },
];

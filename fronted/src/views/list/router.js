// 安全设置模块

export default [
  {
    path: '/list',
    name: 'list',
    component: () => import('./index.vue'),
  },
  {
    path: '/list/sub',
    name: 'list.sub',
    component: () =>
      import(/* webpackChunkName: "list" */ './sub.vue'),
  },
];

// 安全设置模块

export default [
  {
    path: '/login',
    name: 'login',
    component: () => import('./index.vue'),
  },
  {
    path: '/login/sub',
    name: 'login.sub',
    component: () =>
      import(/* webpackChunkName: "login" */ './sub.vue'),
  },
];

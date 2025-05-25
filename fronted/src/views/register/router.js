// 安全设置模块

export default [
  {
    path: '/register',
    name: 'register',
    component: () => import('./index.vue'),
  },
  {
    path: '/register/sub',
    name: 'register.sub',
    component: () =>
      import(/* webpackChunkName: "register" */ './sub.vue'),
  },
];

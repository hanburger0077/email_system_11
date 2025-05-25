// 安全设置模块

export default [
  {
    path: '/login/sub',
    name: 'login.sub',
    component: () =>
      import(/* webpackChunkName: "login" */ './sub.vue'),
  },
];

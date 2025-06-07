export default [
  // 在现有路由中添加
{
  path: '/mail-detail',
  name: 'mailDetail',
  component: () => import('@/views/mailDetail/index.vue')
}
];    
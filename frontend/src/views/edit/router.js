// edit/router.js
export default [
  {
    path: '/edit', // 写信页面路由（保持原路径）
    name: 'edit',
    component: () => import('./index.vue')
  },
  {
    // 新增主页面路由（相对路径：从 edit 目录向上一级，进入 main 目录）
    path: '/main', 
    name: 'main',
    component: () => import('../main/index.vue')
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
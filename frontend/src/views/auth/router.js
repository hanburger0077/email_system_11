// router.js
export default [
  {
    path: '/login',
    redirect: '/auth/login'
  },
  {
    path: '/register',
    redirect: '/auth/register'
  },
  {
    path: '/auth/login',
    name: 'auth.login',
    component: () => import('./login/index.vue')
  },
  {
    path: '/auth/register', 
    name: 'auth.register',
    component: () => import('./register/index.vue') 
  },
];
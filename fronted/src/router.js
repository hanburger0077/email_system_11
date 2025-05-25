import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    name: 'index',
    redirect: '/login',
  },
];

// Vite's way to import multiple modules from the views directory
// This will find all router.js files under ./views and its subdirectories
const modules = import.meta.glob('./views/**/router.js', { eager: true });

for (const path in modules) {
  const moduleContent = modules[path];
  // Ensure the module and its default export exist
  if (moduleContent && typeof moduleContent.default !== 'undefined') {
    const item = moduleContent.default; // item is the default export of a router.js file
    if (Array.isArray(item)) {
      // If it's an array of routes, filter out disabled ones and add them
      const filteredRoutes = item.filter(route => route && !route.disable);
      routes.push(...filteredRoutes);
    } else {
      // If it's a single route object, add it if not disabled
      if (item && !item.disable) {
        routes.push(item);
      }
    }
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes,
});
export default router;
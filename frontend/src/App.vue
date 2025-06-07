<template>
  <component :is="layoutComponent">
    <RouterView />
  </component>
</template>

<script setup>
import { RouterView, useRoute } from 'vue-router'
import { computed,  } from 'vue'
import defaultLayout from '@/components/emailLayout/index.vue';
import authLayout from '@/components/authLayout/index.vue';

const route = useRoute();
console.log(route.path);
const layoutMap = {
  'auth': authLayout,
}

const layoutComponent = computed(() => {
  const key = route.path.split('/')[1];
  return layoutMap[key] || defaultLayout;
});
</script>

<style scoped>
/* 引入mixin.scss */

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

</style>
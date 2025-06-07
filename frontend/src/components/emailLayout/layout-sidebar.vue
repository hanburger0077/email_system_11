<template>
  <div class="email-layout-sidebar">
    <div v-for="(item, index) in menuList"  :key="item.text"
      class="item-container" :class="{ active: activeIndex === index }"
      @click="handleClick(item, index)"
    >
      <div v-if="item.icon" class="item-icon"></div>
      <div class="item-text" >{{ item.text }}</div>
    </div>
  </div>
</template>

<script setup>
import { menuList } from './menuList';
import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
const activeIndex = ref(0);
const router = useRouter();
const route = useRoute();

const handleClick = (item, index) => {
  console.log(item);
  activeIndex.value = index;
  router.push(item.link);
};

// 根据当前路由路径更新activeIndex
const updateActiveIndex = (path) => {
  const foundIndex = menuList.findIndex(item => {
    // 检查当前路径是否匹配菜单项的链接
    // 使用startsWith是为了处理带查询参数的路径
    return path.startsWith(item.link);
  });
  
  if (foundIndex !== -1) {
    activeIndex.value = foundIndex;
  }
};

// 监听路由变化，实时更新侧边栏高亮状态
watch(
  () => route.path,
  (newPath) => {
    updateActiveIndex(newPath);
  }
);

// 组件挂载时，根据当前路径设置高亮
onMounted(() => {
  updateActiveIndex(route.path);
});
</script>

<style lang="scss" scoped>
.email-layout-sidebar {
  position: relative;
  flex-shrink: 0;
  width: 232px;
  background: #fff;
  box-shadow: 0 8px 8px #646e9018;
  overflow: initial;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  .item-container {
    display: flex;
    align-items: center;
    height: 52px;
    padding: 0 16px;
    cursor: pointer;
    &:hover {
      background-color: #f5f5f5;
    }
    .item-text{
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }
    &.active{
    font-weight: bold;
    background: #e6f2fb;
    border-left: 4px solid #1f74c0;
  }
  }
}
</style>
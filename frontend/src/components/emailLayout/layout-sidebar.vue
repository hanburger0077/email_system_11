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
import { ref } from 'vue';
import { useRouter } from 'vue-router';
const activeIndex = ref(0);
const router = useRouter();

const handleClick = (item, index) => {
  console.log(item);
  activeIndex.value = index;
  router.push(item.link);
};
</script>

<style lang="scss" scoped>
.email-layout-sidebar {
  position: relative;
  flex-shrink: 0;
  width: 200px;
  background: #fff;
  box-shadow: 0 8px 8px #646e9018;
  overflow: initial;
  height: calc(100vh -  48px);
  max-height: calc(100vh - 48px);
  .item-container {
    display: flex;
    align-items: center;
    height: 48px;
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
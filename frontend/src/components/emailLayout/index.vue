<template>
  <div class="email-layout">
    <layoutHeader class="email-layout-header"></layoutHeader>
    <div class="email-layout-main">
      <layoutSidebar class="email-layout-main-sidebar"></layoutSidebar>
      <div class="email-layout-main-content">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import layoutSidebar from './layout-sidebar.vue';
import layoutHeader from './layout-header.vue';

</script>

<style lang="scss" scoped>
.email-layout-main{
  padding-top: 70px;
  display: flex;
  background: #e6f2fb;
  overflow: hidden;
  .email-layout-main-content{
    padding: 16px;
    margin-left: 220px;
    flex: 1;
    background: #fff;
  }
}

</style>
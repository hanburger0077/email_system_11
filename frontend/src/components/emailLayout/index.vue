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
  margin-top: 48px;
  display: flex;
  // flex-direction: row;
  background: #e6f2fb;
  overflow: hidden;
  .email-layout-main-content{
    flex: 1;
    background: #fff;
  }
}

</style>
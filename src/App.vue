<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppHeader from './components/layout/AppHeader.vue';
import Sidebar from './components/layout/Sidebar.vue';

const route = useRoute();

// 判断当前是否为登录页
const isLoginPage = computed(() => {
  return route.path === '/login';
});
</script>

<template>
  <div class="app-container">
    <template v-if="!isLoginPage">
      <app-header />
      <div class="main-content">
        <sidebar />
        <div class="page-content">
          <router-view />
        </div>
      </div>
    </template>
    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<style lang="scss">
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

#app {
  height: 100%;
}

.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.page-content {
  flex: 1;
  overflow-y: auto;
  background-color: #f5f7fa;
  padding: 0;
}
</style>

<template>
  <div 
    class="sidebar" 
    :class="{ 'collapsed': layoutStore.isSidebarCollapsed }"
    @mouseenter="handleMouseEnter" 
    @mouseleave="handleMouseLeave"
  >
    <el-menu
      class="sidebar-menu"
      :collapse="layoutStore.isSidebarCollapsed"
      router
      :default-active="activeRoute"
      background-color="#111827"
      text-color="#fff"
      active-text-color="#ffd04b"
    >
      <div class="logo-container">
        <img src="../../assets/logo.svg" alt="Logo" class="logo" v-if="!layoutStore.isSidebarCollapsed" />
        <img src="../../assets/logo.svg" alt="Logo" class="logo-small" v-else />
      </div>
      
      <!-- 所有用户都可见的菜单 -->
      <el-menu-item index="/">
        <el-icon><Odometer /></el-icon>
        <template #title>仪表盘</template>
      </el-menu-item>
      
      <el-menu-item index="/data-analysis">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>数据分析</template>
      </el-menu-item>
      
      <el-menu-item index="/path-planner">
        <el-icon><Position /></el-icon>
        <template #title>路径规划</template>
      </el-menu-item>
      
      <!-- 管理员和共享单车管理者可见的菜单 -->
      <el-menu-item index="/dispatch" v-if="userStore.isBikeManager">
        <el-icon><Van /></el-icon>
        <template #title>调度管理</template>
      </el-menu-item>
      
      <!-- 系统设置菜单 (管理员和普通用户不可见) -->
      <el-menu-item index="/settings" v-if="!userStore.isAdmin && !userStore.isRegularUser">
        <el-icon><Setting /></el-icon>
        <template #title>系统设置</template>
      </el-menu-item>
      
      <!-- 个人中心菜单 -->
      <el-menu-item index="/profile">
        <el-icon><UserFilled /></el-icon>
        <template #title>个人中心</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import { 
  Odometer, 
  DataAnalysis, 
  Position, 
  Van, 
  Setting, 
  UserFilled
} from '@element-plus/icons-vue';
import { useUserStore } from '../../stores/userStore';
import { useLayoutStore } from '../../stores/layoutStore'; // 导入新的 store

const route = useRoute();
const userStore = useUserStore();
const layoutStore = useLayoutStore(); // 使用 store

// 当前激活的路由
const activeRoute = computed(() => {
  return route.path;
});

// 鼠标悬停时展开
const handleMouseEnter = () => {
  layoutStore.setSidebarCollapsed(false);
};

// 鼠标移出时折叠
const handleMouseLeave = () => {
  layoutStore.setSidebarCollapsed(true);
};
</script>

<style scoped lang="scss">
.sidebar {
  position: relative;
  height: 100%;
  background-color: #111827;
  transition: width 0.3s;
  width: 220px;
  
  &.collapsed {
    width: 64px;
  }
  
  .sidebar-menu {
    height: 100%;
    border-right: none;
    
    &:not(.el-menu--collapse) {
      width: 220px;
    }
    
    &.el-menu--collapse {
      width: 64px;
    }
    
    .logo-container {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 60px;
      background-color: #1f2937;
      overflow: hidden;
      
      .logo {
        height: 32px;
        margin: 0 auto;
      }
      
      .logo-small {
        height: 32px;
        width: 32px;
      }
    }
  }
}
</style> 
<template>
  <div class="sidebar">
    <el-menu
      class="sidebar-menu"
      :collapse="isCollapsed"
      router
      :default-active="activeRoute"
      background-color="#001528"
      text-color="#fff"
      active-text-color="#ffd04b"
    >
      <div class="logo-container">
        <img src="../../assets/logo.svg" alt="Logo" class="logo" v-if="!isCollapsed" />
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
      <el-menu-item index="/dispatch" v-if="userStore.isBikeManager || userStore.isAdmin">
        <el-icon><Van /></el-icon>
        <template #title>调度管理</template>
      </el-menu-item>
      
      <!-- 管理员专属菜单 -->
      <el-sub-menu index="/admin" v-if="userStore.isAdmin">
        <template #title>
          <el-icon><Management /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/bikes">
          <el-icon><Bicycle /></el-icon>
          <span>单车管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stations">
          <el-icon><LocationInformation /></el-icon>
          <span>站点管理</span>
        </el-menu-item>
      </el-sub-menu>
      
      <!-- 系统设置菜单 -->
      <el-menu-item index="/settings">
        <el-icon><Setting /></el-icon>
        <template #title>系统设置</template>
      </el-menu-item>
      
      <!-- 个人中心菜单 -->
      <el-menu-item index="/profile">
        <el-icon><UserFilled /></el-icon>
        <template #title>个人中心</template>
      </el-menu-item>
    </el-menu>
    
    <div class="collapse-btn" @click="toggleCollapse">
      <el-icon v-if="isCollapsed"><Expand /></el-icon>
      <el-icon v-else><Fold /></el-icon>
    </div>
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
  Fold, 
  Expand, 
  Management,
  User,
  Bicycle,
  LocationInformation,
  UserFilled
} from '@element-plus/icons-vue';
import { useUserStore } from '../../stores/userStore';

const route = useRoute();
const isCollapsed = ref(false);
const userStore = useUserStore();

// 当前激活的路由
const activeRoute = computed(() => {
  return route.path;
});

// 切换侧边栏折叠状态
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value;
};
</script>

<style scoped lang="scss">
.sidebar {
  position: relative;
  height: 100%;
  background-color: #001528;
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
      background-color: #002140;
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
  
  .collapse-btn {
    position: absolute;
    bottom: 20px;
    left: 0;
    right: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 32px;
    color: #fff;
    cursor: pointer;
    
    &:hover {
      color: #ffd04b;
    }
  }
}
</style> 
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

      <template v-for="routeItem in filteredMenuRoutes" :key="routeItem.path">
        <el-menu-item :index="routeItem.path">
          <el-icon>
            <component :is="getIconComponent(routeItem.meta.icon as string)" />
          </el-icon>
          <template #title>{{ routeItem.meta.title }}</template>
        </el-menu-item>
      </template>

    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { useUserStore } from '../../stores/userStore';
import { useLayoutStore } from '../../stores/layoutStore';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const layoutStore = useLayoutStore();

const getIconComponent = (iconName: string | undefined) => {
  if (!iconName) return null;
  // @ts-ignore
  return ElementPlusIconsVue[iconName];
};

const filteredMenuRoutes = computed(() => {
  const allRoutes = router.getRoutes();
  const userRole = userStore.userRole;

  return allRoutes.filter(r => {
    const meta = r.meta || {};
    if (!meta.showInMenu) {
      return false;
    }
    
    // Handle admin-only routes
    if (meta.requiresAdmin) {
      return userRole === 'ROLE_ADMIN';
    }

    // Handle routes with specific role access
    if (meta.allowedRoles) {
      if (Array.isArray(meta.allowedRoles)) {
        const allowedRolesWithPrefix = meta.allowedRoles.map(role => `ROLE_${role.toUpperCase()}`);
        return allowedRolesWithPrefix.includes(userRole);
      }
    }

    // For routes that don't specify any role requirements but are in the menu
    // (like Dashboard and Profile)
    return true;
  });
});

const activeRoute = computed(() => route.path);

const handleMouseEnter = () => {
  if (layoutStore.isSidebarCollapsed) {
    layoutStore.setSidebarCollapsed(false);
  }
};

const handleMouseLeave = () => {
  if (!layoutStore.isSidebarCollapsed) {
    layoutStore.setSidebarCollapsed(true);
  }
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
    .el-icon {
        width: 24px;
        height: 18px;
    }
  }
}
</style> 
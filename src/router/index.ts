import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import Dashboard from '../views/Dashboard.vue';
import Login from '../views/Login.vue';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/identity',
    name: 'identity',
    component: () => import('../views/Identity.vue'),
    meta: {
      title: '身份管理',
      requiresAdmin: true
    }
  },
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: {
      title: '登录',
      public: true  // 公开页面，无需登录验证
    }
  },
  {
    path: '/',
    name: 'dashboard',
    component: Dashboard,
    meta: {
      title: '仪表盘'
    }
  },
  {
    path: '/data-analysis',
    name: 'data-analysis',
    component: () => import('../views/DataAnalysis.vue'),
    meta: {
      title: '数据分析',
      requiresAuth: true,
      allowedRoles: ['manager','subway','admin']
    }
  },
  {
    path: '/dispatch',
    name: 'dispatch',
    component: () => import('../views/Dispatch.vue'),
    meta: {
      title: '调度管理',
      requiresAuth: true,
      allowedRoles: ['manager','subway','admin']
    }
  },
  {
    path: '/path-planner',
    name: 'path-planner',
    component: () => import('../views/PathPlanner.vue'),
    meta: {
      title: '路径规划',
      requiresAuth: true,
      allowedRoles: ['manager','subway','admin']
    }
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('../views/SystemSettings.vue'),
    meta: {
      title: '系统设置',
      requiresAdmin: true
    }
  },
  {    path: '/profile',    name: 'profile',    component: () => import('../views/Profile.vue'),    meta: {      title: '个人中心'    }  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

// 路由守卫，检查用户是否登录
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '智慧交通'} - 监控与调度平台`;
  
  // 检查页面是否需要登录验证
  const isPublicPage = to.meta.public;
  const isLoggedIn = localStorage.getItem('token');
  const requiresAdmin = to.meta.requiresAdmin;
  const userStore = (await import('../stores/userStore')).useUserStore();

  if (!isPublicPage && !isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else if (to.meta.allowedRoles) {
    const hasRole = (to.meta.allowedRoles as string[]).includes(userStore.userRole);
    if (!hasRole) {
      ElMessageBox.alert('当前功能暂未向普通用户开放','权限不足');
      return false;
    }
  }
  
  if (requiresAdmin && !userStore.isAdmin) {
    next({ name: 'dashboard' });
  } else {
    next();
  }
});

export default router;
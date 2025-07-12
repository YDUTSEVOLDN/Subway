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
  
  const isPublicPage = to.meta.public;
  const isLoggedIn = !!localStorage.getItem('token');
  
  // 如果是公開頁面，直接放行
  if (isPublicPage) {
    return next();
  }

  // 如果需要驗證但未登錄，重定向到登錄頁
  if (!isLoggedIn) {
    return next({ name: 'login', query: { redirect: to.fullPath } });
  }

  // 如果已登錄，進行權限檢查
  const userStore = (await import('../stores/userStore')).useUserStore();
  const { userRole, isAdmin } = userStore;
  const { requiresAdmin, allowedRoles } = to.meta;

  if (requiresAdmin && !isAdmin) {
    ElMessageBox.alert('您没有权限访问此页面', '权限不足');
    return next({ name: 'dashboard' });
  }

  if (allowedRoles && !(allowedRoles as string[]).includes(userRole)) {
    ElMessageBox.alert('当前功能暂未向您的角色开放', '权限不足');
    return next(false);
  }
  
  next();
});

export default router;
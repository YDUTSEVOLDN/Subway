import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import Dashboard from '../views/Dashboard.vue';
import Login from '../views/Login.vue';

const routes: Array<RouteRecordRaw> = [
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
      title: '数据分析'
    }
  },
  {
    path: '/dispatch',
    name: 'dispatch',
    component: () => import('../views/Dispatch.vue'),
    meta: {
      title: '调度管理'
    }
  },
  {
    path: '/path-planner',
    name: 'path-planner',
    component: () => import('../views/PathPlanner.vue'),
    meta: {
      title: '路径规划'
    }
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('../views/SystemSettings.vue'),
    meta: {
      title: '系统设置'
    }
  },
  {    path: '/profile',    name: 'profile',    component: () => import('../views/Profile.vue'),    meta: {      title: '个人中心'    }  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

// 路由守卫，检查用户是否登录
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '智慧交通'} - 监控与调度平台`;
  
  // 检查页面是否需要登录验证
  const isPublicPage = to.meta.public;
  const isLoggedIn = localStorage.getItem('token');
  
  if (!isPublicPage && !isLoggedIn) {
    // 如果页面需要登录但用户未登录，重定向到登录页
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else {
    next();
  }
});

export default router;
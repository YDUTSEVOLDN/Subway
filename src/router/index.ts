import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import Dashboard from '../views/Dashboard.vue';
import Login from '../views/Login.vue';
import { useUserStore } from '../stores/userStore'; // 直接导入
import DataAnalysis from '@/views/DataAnalysis.vue';
import PathPlanner from '@/views/PathPlanner.vue';
import Dispatch from '@/views/Dispatch.vue';
import SystemSettings from '@/views/SystemSettings.vue';
import Profile from '@/views/Profile.vue';
import Identity from '@/views/Identity.vue';
import SchedulingPlans from '@/views/SchedulingPlans.vue';
import IntelligentAssistant from '@/views/IntelligentAssistant.vue';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/identity',
    name: 'identity',
    component: () => import('../views/Identity.vue'),
    meta: {
      title: '身份管理',
      icon: 'User',
      showInMenu: true,
      requiresAdmin: true
    }
  },
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: {
      title: '登录',
      public: true,
      showInMenu: false
    }
  },
  {
    path: '/',
    name: 'dashboard',
    component: Dashboard,
    meta: {
      title: '地图界面',
      icon: 'Odometer',
      showInMenu: true
    }
  },
  {
    path: '/data-analysis',
    name: 'data-analysis',
    component: () => import('../views/DataAnalysis.vue'),
    meta: {
      title: '数据分析',
      icon: 'DataAnalysis',
      showInMenu: true,
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
      icon: 'Van',
      showInMenu: false, // Hide from sidebar for all roles
      requiresAuth: true,
      allowedRoles: ['admin'] // Only admin can access via URL
    }
  },
  {
    path: '/path-planner',
    name: 'path-planner',
    component: () => import('../views/PathPlanner.vue'),
    meta: {
      title: '调度规划',
      icon: 'Position',
      showInMenu: true,
      requiresAuth: true,
      allowedRoles: ['manager','subway','admin']
    }
  },
  {
    path: '/scheduling-plans',
    name: 'scheduling-plans',
    component: SchedulingPlans,
    meta: {
      title: '调度方案',
      icon: 'DocumentChecked',
      showInMenu: false,
      requiresAuth: true,
      allowedRoles: ['manager']
    }
  },
  /*
  {
    path: '/settings',
    name: 'settings',
    component: () => import('../views/SystemSettings.vue'),
    meta: {
      title: '系统设置',
      icon: 'Setting',
      showInMenu: true,
      requiresAdmin: true
    }
  },
  */
  {
    path: '/assistant',
    name: 'assistant',
    component: IntelligentAssistant,
    meta: {
      title: '智能助手',
      icon: 'ChatDotRound',
      showInMenu: true,
      requiresAuth: true
    }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile.vue'),
    meta: {
      title: '个人中心',
      icon: 'UserFilled',
      showInMenu: true
    }
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

// 路由守卫，检查用户是否登录
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  const platformName = '智慧地铁交通监控与调度平台';
  document.title = to.meta.title ? `${to.meta.title} - ${platformName}` : platformName;
  
  // 在路由守卫初始化时，Pinia store可能还未完全激活。
  // 我们直接使用 useUserStore() 来确保获取到最新的状态。
  const userStore = useUserStore();
  
  const isPublicPage = to.meta.public;
  const isAuthenticated = userStore.isAuthenticated;

  // 如果目标页面是公开的 (例如登录页)
  if (isPublicPage) {
    // 如果用户已登录，还想访问登录页，则直接送他回主页
    if (isAuthenticated && to.name === 'login') {
      return next({ name: 'dashboard' });
    }
    // 否则，正常访问公开页
    return next();
  }

  // 如果目标页面需要登录，但用户未认证，则重定向到登录页
  if (!isAuthenticated) {
    return next({ name: 'login', query: { redirect: to.fullPath } });
  }

  // 如果已登录，则进行后续的权限检查
  const userRole = userStore.userRole; // 从 getter 获取角色
  const { requiresAdmin, allowedRoles } = to.meta;

  if (requiresAdmin && userRole !== 'ROLE_ADMIN') {
    ElMessageBox.alert('您没有权限访问此页面，需要管理员权限', '权限不足', {
      confirmButtonText: '好的',
      type: 'warning',
    });
    return next({ name: 'dashboard' });
  }

  if (allowedRoles && !(allowedRoles as string[]).map(r => `ROLE_${r.toUpperCase()}`).includes(userRole)) {
     ElMessageBox.alert('当前功能暂未向您的角色开放', '权限不足', {
      confirmButtonText: '好的',
      type: 'warning',
    });
    return next(from.path && from.path !== to.path ? from.path : { name: 'dashboard' });
  }
  
  next();
});

export default router;
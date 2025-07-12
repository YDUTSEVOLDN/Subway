import { defineStore } from 'pinia';
import authService from '../services/authService';
import { UserRole, User, AuthState } from '../types';


const storedUser = JSON.parse(localStorage.getItem('user') || 'null');

export const useUserStore = defineStore({
  id: 'user',

  state: (): AuthState => (
    storedUser
    ? { token: storedUser.accessToken, user: storedUser, returnUrl: null }
    : { token: null, user: null, returnUrl: null }
  ),

  getters: {
    isAuthenticated: (state) => !!state.token,
    currentUser: (state) => state.user,
    userRole: (state) => state.user?.role || 'guest',
    
    // 检查用户是否有特定权限
    hasPermission: (state) => (permission: string) => {
      // This might need adjustment based on backend's permission model
      // For now, we assume role-based access
      if (!state.user) return false;
      const role = state.user.role;
      if (role === 'ROLE_ADMIN') return true;
      if (role === 'ROLE_MANAGER' && (permission.includes('bike') || permission.includes('dashboard'))) return true;
      if (role === 'ROLE_SUBWAY' && (permission.includes('subway') || permission.includes('dashboard'))) return true;
      if (role === 'ROLE_USER' && permission.startsWith('view')) return true;
      return false;
    },
    
    // 检查用户是否是管理员
    isAdmin: (state) => state.user?.role === 'ROLE_ADMIN',
    
    // 检查用户是否是共享单车管理者
    isBikeManager: (state) => state.user?.role === 'ROLE_MANAGER',
    isSubwayManager: (state) => state.user?.role === 'ROLE_SUBWAY',
    
    // 检查用户是否是普通用户
    isRegularUser: (state) => state.user?.role === 'ROLE_USER'
  },

  actions: {
    // 用户登录
    async login(username: string, password: string): Promise<boolean> {
      try {
        const user = await authService.login(username, password);
        if (user && user.accessToken) {
          this.token = user.accessToken;
          this.user = user;
          localStorage.setItem('user', JSON.stringify(user));
          return true;
        }
        return false;
      } catch (error) {
        console.error('Login failed:', error);
        this.logout(); // Ensure state is clean after failed login
        return false;
      }
    },
    
    // 用户注册
    async register(username: string, email: string, password: string, role: UserRole): Promise<boolean> {
      try {
        await authService.register(username, email, password, role);
        // Maybe auto-login after successful registration? For now, just return true.
        return true;
      } catch (error) {
        console.error('Registration failed:', error);
        return false;
      }
    },
    
    // 设置认证信息
    setAuth(token: string, user: User) {
      this.token = token;
      this.user = user;
      // localStorage is now managed by authService
    },
    
    // 用户退出
    logout() {
      authService.logout();
      this.token = null;
      this.user = null;
    },
    
    // 设置返回URL
    setReturnUrl(url: string) {
      this.returnUrl = url;
    },
    
    // 更新用户信息
    updateUserProfile(userData: Partial<User>) {
      if (this.user) {
        this.user = { ...this.user, ...userData };
        localStorage.setItem('user', JSON.stringify(this.user));
      }
    },

    // Action to update only the email
    updateUserEmail(newEmail: string) {
      if (this.user) {
        this.user.email = newEmail;
        // Also update the full user object in localStorage
        localStorage.setItem('user', JSON.stringify(this.user));
      }
    },
    
    // 更改用户角色 (This function is incomplete and unused, removing for now)
    /*
    changeUserRole(userId: number, newRole: UserRole) {
      if (this.user && this.user.id === userId) {
        // 根据角色设置权限
        let permissions: string[] = ['view_dashboard', 'view_traffic_data'];
        
        if (newRole === 'manager') {
          permissions.push('manage_bikes');
        } else if (newRole === 'admin') {
          permissions = ['view_dashboard', 'view_traffic_data', 'manage_users'];
        }
        
        this.user = { 
          ...this.user, 
          role: newRole,
          permissions
        };
        
        localStorage.setItem('user', JSON.stringify(this.user));
      }
    }
    */
  }
});
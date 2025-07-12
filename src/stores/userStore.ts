import { defineStore } from 'pinia';

// 定义用户角色类型
export type UserRole = 'user' | 'manager' | 'subway' | 'admin' | 'guest';

// 定义用户类型
export interface User {
  id?: string;
  username: string;
  email?: string;
  role?: UserRole;
  avatar?: string;
  permissions?: string[];
}

// 定义用户认证状态
export interface AuthState {
  token: string | null;
  user: User | null;
  returnUrl: string | null;
}

export const useUserStore = defineStore({
  id: 'user',
  
  state: (): AuthState => ({
    token: localStorage.getItem('token'),
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    returnUrl: null
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    currentUser: (state) => state.user,
    userRole: (state) => state.user?.role || 'guest',
    
    // 检查用户是否有特定权限
    hasPermission: (state) => (permission: string) => {
      if (!state.user || !state.user.permissions) return false;
      return state.user.permissions.includes(permission);
    },
    
    // 检查用户是否是管理员
    isAdmin: (state) => state.user?.role === 'admin',
    
    // 检查用户是否是共享单车管理者
    isBikeManager: (state) => state.user?.role === 'manager',
    isSubwayManager: (state) => state.user?.role === 'subway',
    
    // 检查用户是否是普通用户
    isRegularUser: (state) => state.user?.role === 'user'
  },
  
  actions: {
    // 用户登录
    async login(username: string, password: string): Promise<boolean> {
      try {
        // 实际项目中，这里应该是一个API请求
        // 这里使用模拟数据
        console.log('模拟登录请求:', { username, password });
        
        // 模拟不同用户角色
        let role: UserRole = 'user';
        let permissions: string[] = ['view_dashboard', 'view_traffic_data'];
        
        // 根据用户名分配不同角色（仅用于演示）
        if (username.includes('admin')) {
          role = 'admin';
          permissions = ['view_dashboard', 'view_traffic_data', 'manage_users'];
        } else if (username.includes('manager')) {
          role = username.includes('subway') ? 'subway' : 'manager';
          permissions = role === 'manager'
            ? ['view_dashboard', 'view_traffic_data', 'manage_bikes']
            : ['view_dashboard', 'view_traffic_data', 'manage_subway'];
        }
        
        // 假设登录成功
        const token = 'mock_token_' + Date.now();
        // 获取本地存储的用户信息以保留头像设置
        const storedUser = JSON.parse(localStorage.getItem('user') || 'null');
        const avatar = storedUser?.avatar || '/default-avatar.svg';

        const user = {
          id: Math.random().toString(36).substring(2, 10),
          username,
          email: storedUser?.email,
          role,
          permissions,
          avatar: avatar
        };
        
        // 更新状态
        this.setAuth(token, user);
        
        return true;
      } catch (error) {
        console.error('登录失败:', error);
        return false;
      }
    },
    
    // 用户注册
    async register(username: string, email: string, password: string, role: UserRole = 'user'): Promise<boolean> {
      try {
        // 实际项目中，这里应该是一个API请求
        console.log('模拟注册请求:', { username, email, password, role });
        
        // 设置基本权限
        const permissions = role === 'user' 
          ? ['view_dashboard', 'view_traffic_data']
          : ['view_dashboard', 'view_traffic_data', 'manage_bikes'];
        
        // 假设注册成功，自动登录
        const token = 'mock_token_' + Date.now();
        // 获取本地存储的用户信息以保留头像设置
        const storedUser = JSON.parse(localStorage.getItem('user') || 'null');
        const avatar = storedUser?.avatar || '/default-avatar.svg';

        const user = {
          id: Math.random().toString(36).substring(2, 10),
          username,
          email,
          role,
          permissions,
          avatar: avatar
        };
        
        // 更新状态
        this.setAuth(token, user);
        
        return true;
      } catch (error) {
        console.error('注册失败:', error);
        return false;
      }
    },
    
    // 设置认证信息
    setAuth(token: string, user: User) {
      this.token = token;
      this.user = user;
      
      // 保存到本地存储
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(user));
    },
    
    // 用户退出
    logout() {
      this.token = null;
      this.user = null;
      
      // 清除本地存储
      localStorage.removeItem('token');
      localStorage.removeItem('user');
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
    
    // 更改用户角色
    changeUserRole(userId: string, newRole: UserRole) {
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
  }
});
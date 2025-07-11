<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <img src="../assets/logo.svg" alt="Logo" class="logo" />
        <h2>智慧交通监控与调度平台</h2>
      </div>
      
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form 
            ref="loginForm$"
            :model="loginForm" 
            :rules="loginRules"
            label-position="top"
          >
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="loginForm.username" 
                prefix-icon="User"
                placeholder="请输入用户名"
              />
            </el-form-item>
            
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="loginForm.password" 
                prefix-icon="Lock"
                type="password" 
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item>
              <div class="remember-forgot">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
                <el-button link type="primary">忘记密码?</el-button>
              </div>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                :loading="loading" 
                class="submit-btn"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="注册" name="register">
          <el-form 
            ref="registerForm$"
            :model="registerForm" 
            :rules="registerRules"
            label-position="top"
          >
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="registerForm.username" 
                prefix-icon="User"
                placeholder="请输入用户名"
              />
            </el-form-item>
            
            <el-form-item label="邮箱" prop="email">
              <el-input 
                v-model="registerForm.email" 
                prefix-icon="Message"
                placeholder="请输入邮箱"
              />
            </el-form-item>
            
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="registerForm.password" 
                prefix-icon="Lock"
                type="password" 
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input 
                v-model="registerForm.confirmPassword" 
                prefix-icon="Lock"
                type="password" 
                placeholder="请再次输入密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="角色" prop="role" v-if="activeTab === 'register'">
              <div class="role-selection">
                <el-radio-group v-model="registerForm.role">
                  <el-radio-button value="user">普通用户</el-radio-button>
                  <el-radio-button value="manager">单车管理员</el-radio-button>
                  <el-radio-button value="subway">地铁管理员</el-radio-button>
                </el-radio-group>
              </div>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                :loading="loading" 
                class="submit-btn"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Message } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/userStore';
import type { FormItemRule } from 'element-plus';

const router = useRouter();
const route = useRoute();
const activeTab = ref('login');
const loading = ref(false);
const rememberMe = ref(false);
const userStore = useUserStore();

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
});

// 登录验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ]
};

// 注册表单
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: 'user' as 'user' | 'manager' | 'subway' // 新增用户类型
});

// 注册验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email' as const, message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ],
  role: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
};

// 登录表单引用
const loginForm$ = ref();
// 注册表单引用
const registerForm$ = ref();

// 处理登录
const handleLogin = () => {
  loginForm$.value.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    
    loading.value = true;
    
    try {
      // 调用用户登录接口
      const success = await userStore.login(loginForm.username, loginForm.password);
      
      if (success) {
        ElMessage.success('登录成功');
        
        // 获取重定向URL，如果有的话
        const redirectUrl = route.query.redirect as string;
        router.push(redirectUrl || '/');
      } else {
        ElMessage.error('登录失败，请检查用户名和密码');
      }
    } catch (error) {
      console.error('登录出错:', error);
      ElMessage.error('登录失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
};

// 处理注册
const handleRegister = () => {
  registerForm$.value.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    
    loading.value = true;
    
    try {
      // 调用用户注册接口
      const success = await userStore.register(
        registerForm.username,
        registerForm.email,
        registerForm.password,
        registerForm.role
      );
      
      if (success) {
        ElMessage.success('注册成功，已自动登录');
        
        // 获取重定向URL，如果有的话
        const redirectUrl = route.query.redirect as string;
        router.push(redirectUrl || '/');
      } else {
        ElMessage.error('注册失败，请稍后重试');
      }
    } catch (error) {
      console.error('注册出错:', error);
      ElMessage.error('注册失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
  background-image: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.login-box {
  width: 400px;
  padding: 30px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
  
  .logo {
    width: 80px;
    height: 80px;
    margin-bottom: 16px;
  }
  
  h2 {
    font-size: 22px;
    color: #303133;
    margin: 0;
  }
}

.login-tabs {
  width: 100%;
}

.submit-btn {
  width: 100%;
  margin-top: 10px;
}

.remember-forgot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>   
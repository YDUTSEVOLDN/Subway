<template>
  <div class="login-container">
    <div class="background-overlay"></div>
    <transition name="fade-in">
      <div class="login-box">
        <div class="login-header">
          <img src="../assets/logo.svg" alt="Logo" class="logo" />
          <h2 class="title">智慧地铁交通监控与调度平台</h2>
        </div>
        
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form 
              ref="loginForm$"
              :model="loginForm" 
              :rules="loginRules"
              label-position="top"
              class="login-form"
            >
              <el-form-item label="用户名" prop="username">
                <el-input 
                  v-model="loginForm.username" 
                  prefix-icon="User"
                  placeholder="请输入用户名"
                  size="large"
                />
              </el-form-item>
              
              <el-form-item label="密码" prop="password">
                <el-input 
                  v-model="loginForm.password" 
                  prefix-icon="Lock"
                  type="password" 
                  placeholder="请输入密码"
                  show-password
                  size="large"
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
                  size="large"
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
              class="login-form"
            >
              <el-form-item label="用户名" prop="username">
                <el-input 
                  v-model="registerForm.username" 
                  prefix-icon="User"
                  placeholder="请输入用户名"
                  size="large"
                />
              </el-form-item>
              
              <el-form-item label="邮箱" prop="email">
                <el-input 
                  v-model="registerForm.email" 
                  prefix-icon="Message"
                  placeholder="请输入邮箱"
                  size="large"
                />
              </el-form-item>

              <el-form-item label="验证码" prop="verificationCode">
                <el-input 
                  v-model="registerForm.verificationCode" 
                  prefix-icon="Key"
                  placeholder="请输入6位验证码"
                  size="large"
                >
                  <template #append>
                    <el-button 
                      @click="sendVerificationCode" 
                      :disabled="isSendingCode || countdown > 0"
                    >
                      {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item label="密码" prop="password">
                <el-input 
                  v-model="registerForm.password" 
                  prefix-icon="Lock"
                  type="password" 
                  placeholder="请输入密码"
                  show-password
                  size="large"
                />
              </el-form-item>
              
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input 
                  v-model="registerForm.confirmPassword" 
                  prefix-icon="Lock"
                  type="password" 
                  placeholder="请再次输入密码"
                  show-password
                  size="large"
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
                  size="large"
                >
                  注册
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Message, Key } from '@element-plus/icons-vue';
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
  role: 'user' as 'user' | 'manager' | 'subway', // 新增用户类型
  verificationCode: '', // 新增验证码字段
});

// 新增：验证码相关状态
const isSendingCode = ref(false);
const countdown = ref(0);
let timer: number | null = null;


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
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码应为6位', trigger: 'blur' },
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

// 新增：发送验证码
const sendVerificationCode = async () => {
  // 先对邮箱格式进行简单验证
  if (!registerForm.email) {
    ElMessage.warning('请输入邮箱地址');
    return;
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.warning('请输入有效的邮箱地址');
    return;
  }

  isSendingCode.value = true;
  try {
    // 调用 userStore 中的 sendCode 函数
    const success = await userStore.sendCode(registerForm.email);
    
    if (success) {
      ElMessage.success('验证码已发送，请注意查收');

      // 开始倒计时
      countdown.value = 60;
      timer = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) {
          if(timer) clearInterval(timer);
        }
      }, 1000);
    } else {
      ElMessage.error('验证码发送失败，请稍后重试');
    }
  } catch (error) {
    console.error('发送验证码失败:', error);
    ElMessage.error('验证码发送失败，请稍后重试');
  } finally {
    isSendingCode.value = false;
  }
};

// 处理注册
const handleRegister = () => {
  registerForm$.value.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    
    loading.value = true;
    
    try {
      const success = await userStore.register(
        registerForm.username,
        registerForm.email,
        registerForm.password,
        registerForm.role,
        registerForm.verificationCode
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
  height: 100vh;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.login-box {
  width: 420px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 15px;
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #fff;
  z-index: 10;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 2rem;

  .logo {
    height: 50px;
    margin-bottom: 1rem;
  }

  .title {
    font-size: 1.5rem;
    font-weight: 300;
    text-align: center;
  }
}

.login-form {
  margin-top: 1.5rem;
}

.remember-forgot {
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: center;
}

.submit-btn {
  width: 100%;
  font-size: 1rem;
  letter-spacing: 2px;
}

.role-selection {
  width: 100%;
  display: flex;
  justify-content: center;
  
  .el-radio-group {
    justify-content: center;
  }
}

/* Element Plus Overrides */
:deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 1rem;
  padding: 0 20px;
  height: 50px;
  line-height: 50px;

  &.is-active {
    color: #fff;
    font-weight: bold;
  }
}

:deep(.el-tabs__active-bar) {
  background-color: #fff;
  height: 3px;
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.2);
}

:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.2;
}

:deep(.el-input__inner) {
  background-color: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;

  &::placeholder {
    color: rgba(255, 255, 255, 0.5) !important;
  }
}

:deep(.el-input__prefix-inner),
:deep(.el-input__suffix-inner) {
  color: #fff !important;
}

:deep(.el-input-group__append) {
  background-color: transparent !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
}

:deep(.el-checkbox__label) {
  color: #fff;
}

:deep(.el-checkbox__inner) {
  background-color: transparent;
  border-color: rgba(255, 255, 255, 0.5);
}

:deep(.el-button--primary.is-link) {
  color: #fff;
  font-weight: bold;
}

.fade-in-enter-active, .fade-in-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.fade-in-enter-from, .fade-in-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>   
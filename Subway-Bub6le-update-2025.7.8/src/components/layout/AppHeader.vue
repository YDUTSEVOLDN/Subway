<template>
  <header class="app-header">
    <div class="logo-area">
      <img src="/logo.svg" alt="Logo" class="logo" />
      <h1>智慧交通监控与调度平台</h1>
    </div>
    
    <div class="header-right">
      <div class="time-display">
        {{ currentTime }}
      </div>
      
      <el-dropdown @command="handleCommand" trigger="click">
        <div class="user-info">
          <el-avatar :size="32" :src="userAvatar" />
          <span class="username">{{ username }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人资料
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>系统设置
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { User, Setting, SwitchButton } from '@element-plus/icons-vue';
import { useUserStore } from '../../stores/userStore';

const userStore = useUserStore();
const router = useRouter();

// 用户名
const username = computed(() => {
  return userStore.user?.username || '未登录';
});

// 用户头像
const userAvatar = computed(() => {
  return userStore.user?.avatar || '/default-avatar.png';
});

// 当前时间
const currentTime = ref(new Date().toLocaleString());
let timer: number;

// 更新时间
const updateTime = () => {
  currentTime.value = new Date().toLocaleString();
};

// 处理下拉菜单命令
const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'settings':
      router.push('/settings');
      break;
    case 'logout':
      confirmLogout();
      break;
    default:
      break;
  }
};

// 确认退出登录
const confirmLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout();
    router.push('/login');
  }).catch(() => {
    // 取消退出
  });
};

onMounted(() => {
  // 启动定时器，每秒更新一次时间
  timer = window.setInterval(updateTime, 1000);
});

onUnmounted(() => {
  // 清除定时器
  clearInterval(timer);
});
</script>

<style scoped lang="scss">
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  
  .logo-area {
    display: flex;
    align-items: center;
    
    .logo {
      width: 40px;
      height: 40px;
      margin-right: 12px;
    }
    
    h1 {
      font-size: 18px;
      font-weight: 600;
      margin: 0;
      color: #303133;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    
    .time-display {
      margin-right: 24px;
      font-size: 14px;
      color: #606266;
    }
    
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      
      .username {
        margin-left: 8px;
        font-size: 14px;
        color: #303133;
      }
    }
  }
}
</style> 
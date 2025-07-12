<template>
  <div class="settings-container">
    <h2>个人中心</h2>

    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
        </div>
      </template>

      <div class="user-info">
  <div class="avatar-container">
    <el-avatar :size="80" :src="user.avatar || '/default-avatar.svg'" class="user-avatar" />
    <el-button type="primary" size="small" @click="showAvatarSelector = true" class="change-avatar-btn">更换头像</el-button>
  </div>
  <el-descriptions :column="1" border class="user-details">
    <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
    <el-descriptions-item label="邮箱">{{ user.email || '未设置' }}</el-descriptions-item>
    <el-descriptions-item label="角色">{{ user.role === 'manager' ? '管理员' : '普通用户' }}</el-descriptions-item>
  </el-descriptions>
</div>

<el-dialog v-model="showAvatarSelector" title="选择头像" width="600px">
  <div class="avatar-selector">
    <div v-for="(avatar, index) in avatars" :key="index" class="avatar-item" @click="selectAvatar(index)">
      <el-avatar :size="60" :src="avatar" :class="{ 'selected': currentAvatarIndex === index }" />
    </div>
  </div>
  <template #footer>
    <el-button @click="showAvatarSelector = false">取消</el-button>
    <el-button type="primary" @click="confirmAvatarChange">确认</el-button>
  </template>
</el-dialog>
    </el-card>

    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>

      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-position="top">
        <el-form-item label="当前密码" prop="current">
          <el-input v-model="passwordForm.current" type="password" placeholder="请输入当前密码" show-password />
        </el-form-item>

        <el-form-item label="新密码" prop="new">
          <el-input v-model="passwordForm.new" type="password" placeholder="请输入新密码" show-password />
          <div class="form-item-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>密码长度至少6位字符</span>
          </div>
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirm">
          <el-input v-model="passwordForm.confirm" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="updatePassword" :loading="saving">更新密码</el-button>
          <el-button @click="resetPasswordForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>修改邮箱</span>
        </div>
      </template>

      <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-position="top">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="emailForm.currentPassword" type="password" placeholder="请输入当前密码" show-password />
        </el-form-item>

        <el-form-item label="新邮箱" prop="newEmail">
          <el-input v-model="emailForm.newEmail" type="email" placeholder="请输入新邮箱" />
          <div class="form-item-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>请输入有效的邮箱地址</span>
          </div>
        </el-form-item>

        <el-form-item label="确认新邮箱" prop="confirmEmail">
          <el-input v-model="emailForm.confirmEmail" type="email" placeholder="请再次输入新邮箱" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="updateEmail" :loading="emailSaving">更新邮箱</el-button>
          <el-button @click="resetEmailForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="form-actions">
      <el-button type="danger" @click="logout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import type { FormRules } from 'element-plus';
import { InfoFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import { useUserStore } from '../stores/userStore';

const userStore = useUserStore();
const user = userStore.currentUser || reactive({ username: '', email: '', role: 'user', avatar: '/default-avatar.svg' });

// 密码修改表单
const passwordForm = reactive({
  current: '',
  new: '',
  confirm: ''
});

const emailForm = reactive({
  currentPassword: '',
  newEmail: '',
  confirmEmail: ''
});

const passwordRules = {
  current: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  new: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== passwordForm.new) {
          callback(new Error('两次输入的新密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

const emailRules: FormRules<typeof emailForm> = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] }
  ],
  confirmEmail: [
    { required: true, message: '请确认新邮箱', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== emailForm.newEmail) {
          callback(new Error('两次输入的邮箱不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

const passwordFormRef = ref();
const emailFormRef = ref();
const saving = ref(false);
const emailSaving = ref(false);
const showAvatarSelector = ref(false);
const currentAvatarIndex = ref(-1);

// 内置头像列表
const avatars = Array.from({ length: 10 }, (_, i) => `/avatars/avatar-${i+1}.svg`);

// 初始化当前头像索引
onMounted(() => {
  if (user.avatar) {
    const index = avatars.findIndex(avatar => user.avatar?.includes(avatar));
    if (index !== -1) {
      currentAvatarIndex.value = index;
    }
  }
});

// 选择头像
const selectAvatar = (index: number) => {
  currentAvatarIndex.value = index;
};

// 确认更换头像
const confirmAvatarChange = () => {
  if (currentAvatarIndex.value !== -1) {
    const newAvatar = avatars[currentAvatarIndex.value];
    userStore.updateUserProfile({ avatar: newAvatar });
    user.avatar = newAvatar;
    ElMessage.success('头像更新成功');
    showAvatarSelector.value = false;
  }
};
const router = useRouter();

const updatePassword = () => {
  passwordFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;

    saving.value = true;
    // 模拟异步操作
    try {
      await new Promise(resolve => setTimeout(resolve, 1000));
      ElMessage.success('密码更新成功');
      passwordForm.current = '';
      passwordForm.new = '';
      passwordForm.confirm = '';
    } catch (error) {
      ElMessage.error('密码更新失败，请稍后重试');
    } finally {
      saving.value = false;
    }
  });
};

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields();
};

const resetEmailForm = () => {
  emailFormRef.value?.resetFields();
};

const updateEmail = () => {
  emailFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;

    emailSaving.value = true;
    // 模拟异步操作
    try {
      await new Promise(resolve => setTimeout(resolve, 1000));
      userStore.updateUserProfile({ email: emailForm.newEmail });
      user.email = emailForm.newEmail;
      ElMessage.success('邮箱更新成功');
      emailForm.currentPassword = '';
      emailForm.newEmail = '';
      emailForm.confirmEmail = '';
    } catch (error) {
      ElMessage.error('邮箱更新失败，请稍后重试');
    } finally {
      emailSaving.value = false;
    }
  });
};

const logout = () => {
  localStorage.removeItem('token');
  ElMessage.success('已退出登录');
  router.push('/login');
};
</script>

<style scoped lang="scss">
.settings-container {
  padding: 20px;

  h2 {
    margin-bottom: 20px;
  }

  .settings-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: bold;
      font-size: 16px;
    }
  }

  .user-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.change-avatar-btn {
  width: 100%;
}

.avatar-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-top: 10px;
}

.avatar-item {
  cursor: pointer;
  text-align: center;
  padding: 10px;
  border-radius: 4px;
  transition: all 0.2s;
}

.avatar-item:hover {
  background-color: #f5f5f5;
}

.avatar-item .selected {
  border: 2px solid #409EFF;
  box-shadow: 0 0 0 2px #409EFF;
}

  .user-details {
    flex: 1;
  }

  .form-item-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;

    .el-icon {
      margin-right: 4px;
    }
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>

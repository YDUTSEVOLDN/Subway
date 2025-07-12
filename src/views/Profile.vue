<template>
  <div class="settings-container">
    <h2>个人中心</h2>

    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
        </div>
      </template>

      <div class.native="user-info" v-if="user">
        <div class="avatar-container">
          <el-avatar :size="80" :src="user.avatar || '/default-avatar.svg'" class="user-avatar" />
          <el-button type="primary" size="small" @click="showAvatarSelector = true" class="change-avatar-btn">更换头像</el-button>
        </div>
        <el-descriptions :column="1" border class="user-details">
          <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user.email || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ formattedUserRole }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else>
        正在加载用户信息...
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

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
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
          <el-input v-model="emailForm.currentPassword" type="password" placeholder="请输入当前密码以验证身份" show-password />
        </el-form-item>

        <el-form-item label="新邮箱" prop="newEmail">
          <el-input v-model="emailForm.newEmail" type="email" placeholder="请输入新邮箱" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="updateEmail" :loading="emailSaving">更新邮箱</el-button>
          <el-button @click="resetEmailForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="form-actions">
      <el-button type="danger" @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { InfoFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/userStore';
import userService from '../services/userService'; // Import the new service

const router = useRouter();
const userStore = useUserStore();
const user = computed(() => userStore.currentUser);

const roleMap: { [key: string]: string } = {
  ROLE_USER: '普通用户',
  ROLE_MANAGER: '单车管理员',
  ROLE_SUBWAY: '地铁管理员',
  ROLE_ADMIN: '超级管理员',
};

const formattedUserRole = computed(() => {
  if (user.value?.role) {
    return roleMap[user.value.role] || '未知角色';
  }
  return '游客';
});

// --- Password Form ---
const passwordFormRef = ref<FormInstance>();
const saving = ref(false);
const passwordForm = reactive({
  current: '',
  newPassword: '',
  confirm: '',
});

const passwordRules = reactive<FormRules>({
  current: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
});

const updatePassword = async () => {
  if (!passwordFormRef.value) return;
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      await userService.updatePassword(passwordForm.current, passwordForm.newPassword);
      ElMessage.success('密码更新成功！');
      passwordFormRef.value?.resetFields();
    } catch (error: any) {
      const message = error.response?.data?.message || '密码更新失败，请稍后重试';
      ElMessage.error(message);
    } finally {
      saving.value = false;
    }
  });
};

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields();
};


// --- Email Form ---
const emailFormRef = ref<FormInstance>();
const emailSaving = ref(false);
const emailForm = reactive({
  currentPassword: '',
  newEmail: '',
});

const emailRules = reactive<FormRules>({
  currentPassword: [{ required: true, message: '请输入当前密码以验证', trigger: 'blur' }],
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] },
  ],
});

const updateEmail = async () => {
  if (!emailFormRef.value) return;
  await emailFormRef.value.validate(async (valid) => {
    if (!valid) return;
    emailSaving.value = true;
    try {
      await userService.updateEmail(emailForm.currentPassword, emailForm.newEmail);
      // Update the store and local state
      userStore.updateUserEmail(emailForm.newEmail);
      ElMessage.success('邮箱更新成功！');
      emailFormRef.value?.resetFields();
    } catch (error: any) {
      const message = error.response?.data?.message || '邮箱更新失败，请稍后重试';
      ElMessage.error(message);
    } finally {
      emailSaving.value = false;
    }
  });
};

const resetEmailForm = () => {
  emailFormRef.value?.resetFields();
};


// --- Avatar Logic ---
const showAvatarSelector = ref(false);
const currentAvatarIndex = ref(-1);
const avatars = Array.from({ length: 10 }, (_, i) => `/avatars/avatar-${i + 1}.svg`);

onMounted(() => {
  if (user.value?.avatar) {
    const index = avatars.findIndex(avatar => user.value?.avatar?.includes(avatar));
    if (index !== -1) {
      currentAvatarIndex.value = index;
    }
  }
});

const selectAvatar = (index: number) => {
  currentAvatarIndex.value = index;
};

const confirmAvatarChange = () => {
  if (currentAvatarIndex.value !== -1) {
    const newAvatar = avatars[currentAvatarIndex.value];
    userStore.updateUserProfile({ avatar: newAvatar });
    ElMessage.success('头像更新成功');
    showAvatarSelector.value = false;
  }
};

// --- Logout ---
const handleLogout = () => {
  userStore.logout();
  router.push({ name: 'login' });
};
</script>

<style scoped lang="scss">
.settings-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100%;
}

h2 {
  font-size: 24px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 20px;
}

.settings-card {
  margin-bottom: 20px;

  .card-header {
    font-size: 18px;
    font-weight: 500;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  border: 2px solid #e4e7ed;
}

.change-avatar-btn {
  margin-top: 8px;
}

.user-details {
  flex-grow: 1;
}

.form-item-tip {
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.form-actions {
  margin-top: 20px;
  text-align: right;
}

.avatar-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: center;
}

.avatar-item {
  cursor: pointer;

  .el-avatar {
    transition: all 0.2s ease-in-out;
    border: 3px solid transparent;
  }

  .selected {
    border-color: #409eff;
    transform: scale(1.1);
  }
}
</style>

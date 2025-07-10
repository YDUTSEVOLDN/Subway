<template>
  <div class="management-container">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <span>用户身份管理</span>
        </div>
      </template>

      <el-button type="primary" @click="router.push('/')" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>返回控制台
      </el-button>

      <el-table :data="users" style="width: 100%" v-loading="loading">
        <el-table-column prop="username" label="用户名" width="180" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-select 
              v-model="row.role" 
              @change="(val) => updateRole(row.id, val)"
              :disabled="!userStore.isAdmin"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/userStore';
import type { User, UserRole } from '../stores/userStore';

const router = useRouter();
const userStore = useUserStore();

const users = ref<User[]>([]);
const loading = ref(false);

const roleOptions = [
  { value: 'user', label: '普通用户' },
  { value: 'manager', label: '单车管理员' },
  { value: 'subway', label: '地铁管理员' },
  { value: 'admin', label: '系统管理员' }
];

// 获取用户列表（模拟API调用）
onMounted(async () => {
  loading.value = true;
  try {
    // 实际项目中应调用API接口
    users.value = JSON.parse(localStorage.getItem('allUsers') || '[]');
  } finally {
    loading.value = false;
  }
});

// 更新用户角色
const updateRole = (userId: string, newRole: UserRole) => {
  userStore.changeUserRole(userId, newRole as import('../stores/userStore').UserRole);
  // 更新本地存储（模拟数据持久化）
  localStorage.setItem('allUsers', JSON.stringify(users.value));
};
</script>

<style scoped>
.management-container {
  padding: 20px;
}

.management-card {
  margin-top: 20px;
}

.card-header {
  font-size: 18px;
  font-weight: 500;
}

.back-btn {
  margin-bottom: 20px;
}
</style> 
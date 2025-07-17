<template>
  <div class="management-container">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <span>用户身份管理</span>
          <el-button @click="fetchUsers" :icon="Refresh" circle :loading="loading" title="刷新列表" />
        </div>
      </template>

      <!-- User Stats -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="6">
          <el-statistic title="总用户数" :value="users.length" />
        </el-col>
        <el-col :span="6" v-for="(count, role) in userStats" :key="role">
           <el-statistic :title="roleMap[role] || '未知'" :value="count" />
        </el-col>
      </el-row>

      <el-table :data="users" style="width: 100%; margin-top: 20px;" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" sortable />
        <el-table-column prop="username" label="用户名" width="180" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="角色" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)">
              {{ roleMap[row.role] || '未知角色' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              :icon="Edit"
              @click="handleEdit(row)"
              :disabled="isCurrentUser(row.id)"
              size="small"
              circle
              title="编辑用户"
            />
            <el-button
              type="danger"
              :icon="Delete"
              @click="handleDeleteUser(row)"
              :disabled="isCurrentUser(row.id)"
              size="small"
              circle
              title="删除用户"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Edit User Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="30%">
      <div v-if="editingUser">
        <p><strong>用户名:</strong> {{ editingUser.username }}</p>
        <p><strong>邮箱:</strong> {{ editingUser.email }}</p>
        <el-form label-position="top">
          <el-form-item label="角色">
            <el-select v-model="editingUser.role" placeholder="选择角色" style="width: 100%;">
              <el-option
                v-for="role in roleOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateUser" :loading="isSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Refresh, Edit } from '@element-plus/icons-vue';
import adminService from '../services/adminService';
import { useUserStore } from '../stores/userStore';
import type { User, UserRole } from '../types';

interface UserData extends Omit<User, 'role' | 'id'> {
    id: number;
    role: UserRole;
}

const userStore = useUserStore();
const users = ref<UserData[]>([]);
const loading = ref(false);
const isSaving = ref(false);

const currentUserId = computed(() => userStore.currentUser?.id);
const isCurrentUser = (id: number) => currentUserId.value === id;

const roleMap: { [key: string]: string } = {
  ROLE_USER: '普通用户',
  ROLE_MANAGER: '单车管理员',
  ROLE_SUBWAY: '地铁管理员',
  ROLE_ADMIN: '超级管理员',
};

const roleOptions = Object.entries(roleMap).map(([value, label]) => ({ value, label }));

const getRoleTagType = (role: UserRole) => {
  switch (role) {
    case 'ROLE_ADMIN': return 'danger';
    case 'ROLE_MANAGER': return 'success';
    case 'ROLE_SUBWAY': return 'warning';
    default: return 'info';
  }
};

const userStats = computed(() => {
    const stats: { [key: string]: number } = {};
    for (const user of users.value) {
        if(roleMap[user.role]){
            stats[user.role] = (stats[user.role] || 0) + 1;
        }
    }
    return stats;
});

const fetchUsers = async () => {
  loading.value = true;
  try {
    const response = await adminService.getUsers();
    users.value = response.data;
  } catch (error) {
    ElMessage.error('获取用户列表失败');
  } finally {
    loading.value = false;
  }
};

// --- Edit Logic ---
const showEditDialog = ref(false);
const editingUser = ref<UserData | null>(null);

const handleEdit = (user: UserData) => {
  editingUser.value = { ...user }; // Create a copy to avoid modifying original data directly
  showEditDialog.value = true;
};

const handleUpdateUser = async () => {
  if (!editingUser.value) return;
  isSaving.value = true;
  try {
    await adminService.updateUserRole(editingUser.value.id, editingUser.value.role);
    ElMessage.success(`用户 ${editingUser.value.username} 的角色已更新`);
    showEditDialog.value = false;
    fetchUsers();
  } catch (error) {
    ElMessage.error('更新角色失败');
  } finally {
    isSaving.value = false;
  }
};

const handleDeleteUser = async (user: UserData) => {
  if (isCurrentUser(user.id)) {
    ElMessage.error('不能删除自己');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？此操作不可逆。`, '警告',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    );
    await adminService.deleteUser(user.id);
    ElMessage.success(`用户 ${user.username} 已被删除`);
    fetchUsers();
  } catch (error) {
    if (error !== 'cancel') {
        ElMessage.error('删除用户失败');
    }
  }
};

onMounted(() => {
  fetchUsers();
});
</script>

<style scoped>
.management-container {
  padding: 24px;
  background-color: #f7f8fa;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.1rem;
  font-weight: 600;
}
.stats-row {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}
</style> 
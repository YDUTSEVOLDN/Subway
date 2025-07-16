<template>
  <div class="scheduling-plans-page">
    <div class="page-header">
      <h2>调度方案管理</h2>
    </div>

    <el-card>
      <el-table :data="plans" v-loading="loading" style="width: 100%" row-key="id" @expand-change="handleExpandChange">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="plan-details">
              <el-descriptions title="方案详情" :column="2" border>
                <el-descriptions-item label="方案名称">{{ row.name }}</el-descriptions-item>
                <el-descriptions-item label="创建人">{{ row.createdByUsername }}</el-descriptions-item>
                <el-descriptions-item label="计划时间">{{ formatDate(row.scheduleTime) }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(row.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="起始站点">{{ row.sourceStationName }}</el-descriptions-item>
                <el-descriptions-item label="目标站点">{{ row.targetStationName }}</el-descriptions-item>
                <el-descriptions-item label="调度车辆数">{{ row.bikeCount }}</el-descriptions-item>
                <el-descriptions-item label="优先级">{{ row.priority }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusTagType(row.status)">{{ row.status }}</el-tag>
                </el-descriptions-item>
                 <el-descriptions-item label="更新时间">{{ formatDate(row.updatedAt) }}</el-descriptions-item>
                <el-descriptions-item label="描述" :span="2">{{ row.description }}</el-descriptions-item>
                <el-descriptions-item label="路径数据" :span="2">
                   <el-input type="textarea" :value="getFormattedPathData(row.pathData)" readonly autosize/>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="方案名称" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByUsername" label="创建人" />
        <el-table-column :formatter="row => formatDate(row.scheduleTime)" label="计划时间" />
        <el-table-column :formatter="row => formatDate(row.createdAt)" label="创建时间" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click.stop="handleEdit(row)" :icon="Edit">编辑</el-button>
            <el-button size="small" type="danger" @click.stop="handleDelete(row.id)" :icon="Delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="dialogVisible" title="编辑调度方案" width="50%">
      <el-form :model="currentPlan" label-width="100px">
        <el-form-item label="方案名称">
          <el-input v-model="currentPlan.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="currentPlan.description" type="textarea" />
        </el-form-item>
         <el-form-item label="状态">
          <el-select v-model="currentPlan.status">
            <el-option v-for="status in planStatuses" :key="status" :label="status" :value="status" />
          </el-select>
        </el-form-item>
         <el-form-item label="优先级">
          <el-input-number v-model="currentPlan.priority" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import { schedulingPlanService } from '@/services/schedulingPlanService';
import { SchedulingPlanDto, PlanStatus } from '@/types';
import { useUserStore } from '@/stores/userStore';

const userStore = useUserStore();
const plans = ref<SchedulingPlanDto[]>([]);
const loading = ref(true);
const saving = ref(false);

const dialogVisible = ref(false);
const currentPlan = ref<Partial<SchedulingPlanDto>>({});
const planStatuses = Object.values(PlanStatus);


const fetchPlans = async () => {
  loading.value = true;
  try {
    // Admin sees all plans, others see their own
    if (userStore.isAdmin) {
      plans.value = await schedulingPlanService.getAllPlans();
    } else {
      plans.value = await schedulingPlanService.getMyPlans();
    }
  } catch (error) {
    ElMessage.error('无法加载调度方案');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchPlans);

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleString();
};

const getFormattedPathData = (pathData: string) => {
  try {
    return JSON.stringify(JSON.parse(pathData), null, 2);
  } catch (e) {
    return pathData;
  }
}

const handleExpandChange = (row: SchedulingPlanDto, expandedRows: SchedulingPlanDto[]) => {
  console.log('Expanded row:', row, expandedRows);
};

const getStatusTagType = (status: PlanStatus) => {
  switch (status) {
    case PlanStatus.PENDING: return 'warning';
    case PlanStatus.IN_PROGRESS: return 'primary';
    case PlanStatus.COMPLETED: return 'success';
    case PlanStatus.CANCELLED: return 'info';
    default: return 'info';
  }
};

const handleEdit = (plan: SchedulingPlanDto) => {
  currentPlan.value = { ...plan };
  dialogVisible.value = true;
};

const handleSave = async () => {
  saving.value = true;
  try {
    if (currentPlan.value.id) {
      await schedulingPlanService.updatePlan(currentPlan.value.id, currentPlan.value);
      ElMessage.success('方案更新成功');
    }
    // Create functionality is removed from this page
    dialogVisible.value = false;
    await fetchPlans();
  } catch (error) {
    ElMessage.error('保存失败');
    console.error(error);
  } finally {
    saving.value = false;
  }
};

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除这个方案吗?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await schedulingPlanService.deletePlan(id);
      ElMessage.success('方案删除成功');
      await fetchPlans();
    } catch (error) {
      ElMessage.error('删除失败');
      console.error(error);
    }
  });
};
</script>

<style scoped>
.scheduling-plans-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.plan-details {
  padding: 16px;
  background-color: #f9f9f9;
}
</style> 
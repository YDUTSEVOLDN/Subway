<template>
  <div class="dispatch-container">
    <el-row :gutter="20">
      <!-- 左侧：地图和站点信息 -->
      <el-col :span="16">
        <div class="map-container">
          <div class="map-wrapper">
            <BaiduMap ref="mapRef" />
          </div>
          <div class="map-controls">
            <el-button-group>
              <el-tooltip content="显示/隐藏站点覆盖范围">
                <el-button
                  :type="showCoverage ? 'primary' : 'default'"
                  @click="toggleCoverage"
                >
                  <el-icon><Aim /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="显示/隐藏所有站点">
                <el-button
                  :type="showAllStations ? 'primary' : 'default'"
                  @click="toggleAllStations"
                >
                  <el-icon><LocationInformation /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="显示/隐藏共享单车">
                <el-button
                  :type="showBikes ? 'primary' : 'default'"
                  @click="toggleBikes"
                >
                  <el-icon><BicycleIcon /></el-icon>
                </el-button>
              </el-tooltip>
            </el-button-group>
          </div>
          
          <!-- 如果选中了站点，显示站点信息 -->
          <el-card v-if="selectedStation" class="station-info-panel">
            <template #header>
              <div class="card-header">
                <h3>{{ selectedStation.name }}</h3>
                <el-tag v-for="line in selectedStation.lines" :key="line" size="small" effect="dark">
                  {{ line }}
                </el-tag>
              </div>
            </template>
            
            <el-descriptions :column="1" border>
              <el-descriptions-item label="ID">{{ selectedStation.id }}</el-descriptions-item>
              <el-descriptions-item label="出入口数量">{{ selectedStation.entrances }}</el-descriptions-item>
              <el-descriptions-item label="周边单车数量">
                <el-badge :value="bikesNearStation.length" type="primary">
                  <span>共享单车</span>
                </el-badge>
              </el-descriptions-item>
              <el-descriptions-item label="可用率">
                {{ availableRate }}%
              </el-descriptions-item>
              <el-descriptions-item label="供需状态">
                <el-tag :type="supplyStatusType">{{ supplyStatus }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
            
            <div class="action-buttons">
              <el-button type="primary" @click="createDispatchPlan(selectedStation)" :disabled="!canCreatePlan">
                创建调度方案
              </el-button>
            </div>
          </el-card>
        </div>
      </el-col>
      
      <!-- 右侧：调度方案列表和表单 -->
      <el-col :span="8">
        <el-card class="dispatch-panel" shadow="hover">
          <template #header>
            <div class="card-header">
              <h3>调度方案</h3>
              <el-button type="success" size="small" @click="dialogVisible = true">
                <el-icon><Plus /></el-icon> 新建方案
              </el-button>
            </div>
          </template>
          
          <el-empty v-if="!dispatchPlans.length" description="暂无调度方案" />
          
          <el-collapse v-else accordion>
            <el-collapse-item
              v-for="plan in dispatchPlans"
              :key="plan.id"
              :name="plan.id"
              :title="plan.name"
            >
              <template #title>
                <div class="plan-title">
                  {{ plan.name }}
                  <el-tag size="small" :type="getPlanStatusType(plan.status)">
                    {{ getPlanStatusText(plan.status) }}
                  </el-tag>
                </div>
              </template>
              
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="起点站">{{ plan.source.name }}</el-descriptions-item>
                <el-descriptions-item label="目标站">{{ plan.target.name }}</el-descriptions-item>
                <el-descriptions-item label="调度单车数量">{{ plan.bikeCount }} 辆</el-descriptions-item>
                <el-descriptions-item label="路径点数量">{{ plan.path.length }} 个点</el-descriptions-item>
                <el-descriptions-item label="状态">
                  {{ getPlanStatusText(plan.status) }}
                </el-descriptions-item>
              </el-descriptions>
              
              <div class="plan-actions">
                <el-button 
                  type="primary" 
                  size="small"
                  :disabled="plan.status !== 'pending'" 
                  @click="executePlan(plan.id)"
                >
                  执行调度
                </el-button>
                <el-button 
                  type="info" 
                  size="small"
                  @click="showPath(plan)"
                >
                  查看路径
                </el-button>
                <el-button 
                  type="danger" 
                  size="small"
                  @click="deletePlan(plan.id)"
                  :disabled="plan.status === 'executing'"
                >
                  删除
                </el-button>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 新建调度方案对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="新建调度方案"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="dispatchForm"
        label-width="100px"
        :rules="formRules"
      >
        <el-form-item label="起点站" prop="sourceStationId">
          <el-select v-model="dispatchForm.sourceStationId" placeholder="选择起点站">
            <el-option
              v-for="station in stations"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="目标站" prop="targetStationId">
          <el-select v-model="dispatchForm.targetStationId" placeholder="选择目标站">
            <el-option
              v-for="station in targetStations"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="调度数量" prop="bikeCount">
          <el-input-number
            v-model="dispatchForm.bikeCount"
            :min="1"
            :max="50"
            :step="1"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispatchForm">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, h } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Aim, LocationInformation, Plus } from '@element-plus/icons-vue';
import { useMapStore } from '../stores/mapStore';
import BaiduMap from '../components/common/BaiduMap.vue';
import type { Station } from '../stores/mapStore';
import type { FormItemRule } from 'element-plus';

// 自定义单车图标组件
const BicycleIcon = {
  name: 'BicycleIcon',
  render() {
    return h('svg', 
      { 
        viewBox: '0 0 1024 1024', 
        width: '1em', 
        height: '1em',
        fill: 'currentColor'
      }, 
      [
        h('path', {
          d: 'M512 85.333c-235.648 0-426.667 191.019-426.667 426.667S276.352 938.667 512 938.667 938.667 747.648 938.667 512 747.648 85.333 512 85.333zm0 810.667c-212.096 0-384-171.904-384-384s171.904-384 384-384 384 171.904 384 384-171.904 384-384 384z'
        }),
        h('path', {
          d: 'M695.168 490.667h-147.2l81.067-135.168c6.4-10.667 2.731-24.533-7.936-30.933s-24.533-2.731-30.933 7.936L506.965 490.667h-71.936c-11.776 0-21.333 9.557-21.333 21.333s9.557 21.333 21.333 21.333h50.603L469.335 576h-51.669c-11.776 0-21.333 9.557-21.333 21.333S405.89 618.667 417.666 618.667h68.267l-42.667 71.168c-6.4 10.667-2.731 24.533 7.936 30.933 3.669 2.133 7.552 3.2 11.499 3.2 7.723 0 15.189-4.181 19.2-11.499L539.733 618.667h51.669c11.776 0 21.333-9.557 21.333-21.333s-9.557-21.333-21.333-21.333h-68.267l16.299-42.667h155.733c11.776 0 21.333-9.557 21.333-21.333s-9.557-21.333-21.333-21.333z'
        })
      ]
    );
  }
};

// 地图存储
const mapStore = useMapStore();
// 地图组件引用
const mapRef = ref<any>(null);

// 界面状态
const showCoverage = ref(false);
const showAllStations = ref(true);
const showBikes = ref(true);
const dialogVisible = ref(false);

// 表单与验证规则
const formRef = ref();
const dispatchForm = ref({
  sourceStationId: '',
  targetStationId: '',
  bikeCount: 10
});

const formRules = {
  sourceStationId: [{ required: true, message: '请选择起点站', trigger: 'change' }],
  targetStationId: [
    { required: true, message: '请选择目标站', trigger: 'change' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value === dispatchForm.value.sourceStationId) {
          callback(new Error('目标站不能与起点站相同'));
        } else {
          callback();
        }
      },
      trigger: 'change'
    }
  ],
  bikeCount: [
    { required: true, message: '请输入调度数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '最少调度1辆', trigger: 'blur' } as FormItemRule
  ]
};

// 计算属性
const stations = computed(() => mapStore.getStations);
const selectedStation = computed(() => mapStore.getSelectedStation);
const dispatchPlans = computed(() => mapStore.dispatchPlans);
const bikesNearStation = computed(() => mapStore.getBikesNearSelectedStation);
const availableRate = computed(() => {
  if (!bikesNearStation.value.length) return 0;
  const available = bikesNearStation.value.filter(bike => bike.available).length;
  return Math.round((available / bikesNearStation.value.length) * 100);
});
const supplyStatus = computed(() => mapStore.getSupplyStatus);
const supplyStatusType = computed(() => mapStore.getSupplyStatusType);
const canCreatePlan = computed(() => 
  selectedStation.value && 
  bikesNearStation.value.filter(bike => bike.available).length > 0
);

// 计算可选的目标站点，排除当前选中的站点
const targetStations = computed(() => {
  if (!dispatchForm.value.sourceStationId) return stations.value;
  return stations.value.filter(s => s.id !== dispatchForm.value.sourceStationId);
});

// 获取方案状态显示文本
const getPlanStatusText = (status: string) => {
  switch (status) {
    case 'pending': return '待执行';
    case 'executing': return '执行中';
    case 'completed': return '已完成';
    default: return '未知状态';
  }
};

// 获取方案状态标签类型
const getPlanStatusType = (status: string) => {
  switch (status) {
    case 'pending': return 'info';
    case 'executing': return 'warning';
    case 'completed': return 'success';
    default: return 'info';
  }
};

// 切换覆盖范围显示
const toggleCoverage = () => {
  showCoverage.value = !showCoverage.value;
  mapStore.showCoverageCircle = showCoverage.value;
};

// 切换所有站点显示
const toggleAllStations = () => {
  showAllStations.value = !showAllStations.value;
  // 这里假设BaiduMap组件提供了控制站点显示的方法
  if (mapRef.value) {
    // 暂未实现，可在BaiduMap组件中添加
  }
};

// 切换单车显示
const toggleBikes = () => {
  showBikes.value = !showBikes.value;
  // 这里假设BaiduMap组件提供了控制单车显示的方法
  if (mapRef.value) {
    // 暂未实现，可在BaiduMap组件中添加
  }
};

// 创建调度方案（从当前选中的站点）
const createDispatchPlan = (station: Station) => {
  dispatchForm.value.sourceStationId = station.id;
  dialogVisible.value = true;
};

// 提交调度方案表单
const submitDispatchForm = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    
    // 查找起点站和目标站
    const sourceStation = stations.value.find(s => s.id === dispatchForm.value.sourceStationId);
    const targetStation = stations.value.find(s => s.id === dispatchForm.value.targetStationId);
    
    if (!sourceStation || !targetStation) {
      ElMessage.error('站点信息不存在');
      return;
    }
    
    // 创建调度方案
    const planId = mapStore.addDispatchPlan(
      sourceStation,
      targetStation,
      dispatchForm.value.bikeCount
    );
    
    dialogVisible.value = false;
    ElMessage.success('调度方案创建成功');
    
    // 重置表单
    dispatchForm.value = {
      sourceStationId: '',
      targetStationId: '',
      bikeCount: 10
    };
    
  } catch (error) {
    console.error('表单验证失败:', error);
  }
};

// 执行调度方案
const executePlan = (planId: string) => {
  ElMessageBox.confirm(
    '确认执行该调度方案？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const result = mapStore.executeDispatchPlan(planId);
    if (result) {
      ElMessage.success('调度方案已开始执行');
    } else {
      ElMessage.error('执行失败，调度方案不存在或状态异常');
    }
  }).catch(() => {
    // 取消操作
  });
};

// 查看调度路径
const showPath = (plan: any) => {
  // 这里可以实现在地图上显示路径的功能
  // 需要BaiduMap组件支持
  console.log('显示路径:', plan.path);
  ElMessage.info('正在地图上显示路径');
};

// 删除调度方案
const deletePlan = (planId: string) => {
  ElMessageBox.confirm(
    '确认删除该调度方案？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 这里需要在mapStore中添加删除方案的方法
    // mapStore.deleteDispatchPlan(planId);
    ElMessage.success('调度方案已删除');
  }).catch(() => {
    // 取消操作
  });
};

// 监听选中站点变化
watch(() => mapStore.selectedStation, (station) => {
  if (station) {
    showCoverage.value = true;
    mapStore.showCoverageCircle = true;
  }
});

// 组件挂载时加载站点数据
onMounted(async () => {
  await mapStore.loadStations();
});
</script>

<style scoped lang="scss">
.dispatch-container {
  height: 100%;
  padding: 20px;
  
  .map-container {
    position: relative;
    height: calc(100vh - 120px);
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    
    .map-wrapper {
      width: 100%;
      height: 100%;
    }
    
    .map-controls {
      position: absolute;
      top: 20px;
      right: 20px;
      z-index: 100;
      background-color: rgba(255, 255, 255, 0.9);
      padding: 8px;
      border-radius: 4px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    }
    
    .station-info-panel {
      position: absolute;
      bottom: 20px;
      left: 20px;
      width: 300px;
      z-index: 100;
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
        
        h3 {
          margin: 0;
          flex: 1;
        }
      }
      
      .action-buttons {
        margin-top: 16px;
        text-align: center;
      }
    }
  }
  
  .dispatch-panel {
    height: calc(100vh - 120px);
    overflow-y: auto;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
      }
    }
    
    .plan-title {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .plan-actions {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  }
}
</style> 
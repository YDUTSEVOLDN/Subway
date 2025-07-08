<template>
  <div class="path-planner-container">
    <div class="page-header">
      <h2>路径规划</h2>
      <div class="actions">
        <el-button type="primary" @click="planPath" :loading="loading" :disabled="!sourceStation || !targetStation">
          <el-icon><Position /></el-icon>生成路径
        </el-button>
        <el-button @click="resetPath">
          <el-icon><Refresh /></el-icon>重置
        </el-button>
      </div>
    </div>
    
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="form-card">
          <template #header>
            <div class="card-header">
              <span>设置路径参数</span>
            </div>
          </template>
          
          <el-form :model="pathForm" label-position="top">
            <el-form-item label="起点站">
              <el-select v-model="sourceStation" placeholder="选择起点站" filterable clearable>
                <el-option
                  v-for="station in stations"
                  :key="station.id"
                  :label="station.name"
                  :value="station.id"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="终点站">
              <el-select 
                v-model="targetStation" 
                placeholder="选择终点站" 
                filterable 
                clearable
                :disabled="!sourceStation"
              >
                <el-option
                  v-for="station in availableTargetStations"
                  :key="station.id"
                  :label="station.name"
                  :value="station.id"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="调度单车数量">
              <el-input-number 
                v-model="pathForm.bikeCount" 
                :min="1" 
                :max="50" 
                :step="1" 
                controls-position="right"
              />
            </el-form-item>
            
            <el-form-item label="路径优化目标">
              <el-radio-group v-model="pathForm.optimizeFor">
                <el-radio-button label="distance">最短距离</el-radio-button>
                <el-radio-button label="time">最短时间</el-radio-button>
              </el-radio-group>
            </el-form-item>
            
            <el-divider />
            
            <el-form-item v-if="currentPath">
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="预计距离">
                  {{ pathDistance }} 公里
                </el-descriptions-item>
                <el-descriptions-item label="预计时间">
                  {{ pathDuration }} 分钟
                </el-descriptions-item>
                <el-descriptions-item label="途经点数">
                  {{ currentPath.path?.length || 0 }} 个
                </el-descriptions-item>
              </el-descriptions>
            </el-form-item>
            
            <el-form-item v-if="currentPath">
              <el-button type="success" @click="saveToDispatch" :disabled="!currentPath">保存到调度计划</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <el-card class="saved-paths-card" v-if="savedPaths.length > 0">
          <template #header>
            <div class="card-header">
              <span>保存的路径</span>
            </div>
          </template>
          
          <el-scrollbar height="300px">
            <el-timeline>
              <el-timeline-item
                v-for="(path, index) in savedPaths"
                :key="index"
                :type="path.color ? 'primary' : 'info'"
                :hollow="currentPath?.id !== path.id"
                :color="path.color || '#909399'"
              >
                <div class="saved-path-item" @click="selectPath(path)">
                  <div class="path-info">
                    <div class="path-name">{{ path.name || `路径 ${index + 1}` }}</div>
                    <div class="path-stations">
                      {{ path.source?.name }} → {{ path.target?.name }}
                    </div>
                  </div>
                  <div class="path-actions">
                    <el-button size="small" circle type="danger" @click.stop="deletePath(index)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-scrollbar>
        </el-card>
      </el-col>
      
      <el-col :span="18">
        <el-card class="map-card">
          <div class="map-container">
            <baidu-map class="map" ref="baiduMap" />
            <path-visualizer
              v-if="currentPath && mapInstance"
              :path="currentPath"
              :map-instance="mapInstance"
              :auto-fit="true"
              ref="pathVisualizer"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-dialog
      v-model="saveDialogVisible"
      title="保存到调度计划"
      width="30%"
    >
      <el-form :model="saveForm" label-position="top">
        <el-form-item label="调度名称">
          <el-input v-model="saveForm.name" placeholder="输入调度计划名称" />
        </el-form-item>
        
        <el-form-item label="调度时间">
          <el-date-picker
            v-model="saveForm.scheduleTime"
            type="datetime"
            placeholder="选择调度时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disablePastDate"
          />
        </el-form-item>
        
        <el-form-item label="优先级">
          <el-rate v-model="saveForm.priority" />
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input
            v-model="saveForm.note"
            type="textarea"
            placeholder="添加备注信息"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="saveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSave" :loading="saving">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { Position, Refresh, Delete } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import BaiduMap from '../components/common/BaiduMap.vue';
import PathVisualizer from '../components/common/PathVisualizer.vue';
import { useMapStore, Station } from '../stores/mapStore';

// 地图与站点数据
const mapStore = useMapStore();
const stations = ref<Station[]>([]);
const baiduMap = ref<InstanceType<typeof BaiduMap> | null>(null);
const mapInstance = ref(null);

// 路径参数
const pathForm = ref({
  bikeCount: 10,
  optimizeFor: 'distance'
});
const sourceStation = ref('');
const targetStation = ref('');
const loading = ref(false);

// 当前路径与保存的路径
const pathVisualizer = ref<InstanceType<typeof PathVisualizer> | null>(null);

interface PathData {
  id: string;
  source: Station;
  target: Station;
  path: {lng: number, lat: number}[];
  bikeCount: number;
  color: string;
  optimizeFor: string;
  name?: string;
  scheduleTime?: Date;
  priority?: number;
  note?: string;
  status?: string;
}

const currentPath = ref<PathData | null>(null);
const savedPaths = ref<PathData[]>([]);

// 保存对话框
const saveDialogVisible = ref(false);
const saving = ref(false);
const saveForm = ref({
  name: '',
  scheduleTime: new Date(Date.now() + 24 * 60 * 60 * 1000), // 默认为明天
  priority: 3,
  note: ''
});

// 计算属性
const availableTargetStations = computed(() => {
  if (!sourceStation.value) return [];
  // 过滤掉起点站
  return stations.value.filter(station => station.id !== sourceStation.value);
});

const pathDistance = computed(() => {
  if (!currentPath.value?.path || currentPath.value.path.length < 2) return '0.00';
  
  let totalDistance = 0;
  const path = currentPath.value.path;
  
  for (let i = 1; i < path.length; i++) {
    // 使用百度地图API计算路径距离
    if (window.BMap && window.BMap.geometry) {
      const p1 = new window.BMap.Point(path[i-1].lng, path[i-1].lat);
      const p2 = new window.BMap.Point(path[i].lng, path[i].lat);
      totalDistance += window.BMap.geometry.getDistance(p1, p2);
    } else {
      // 简化的距离计算（经纬度直接相减，不是实际距离）
      const dx = path[i].lng - path[i-1].lng;
      const dy = path[i].lat - path[i-1].lat;
      totalDistance += Math.sqrt(dx * dx + dy * dy) * 111.12; // 大约转换为km
    }
  }
  
  return (totalDistance / 1000).toFixed(2);
});

const pathDuration = computed(() => {
  const distance = parseFloat(pathDistance.value);
  // 假设平均速度为 20 km/h
  const hours = distance / 20;
  return Math.ceil(hours * 60);
});

// 方法
const loadStations = async () => {
  try {
    await mapStore.loadStations();
    stations.value = mapStore.getStations;
  } catch (error) {
    console.error('加载站点数据失败:', error);
    ElMessage.error('加载站点数据失败');
  }
};

const planPath = async () => {
  if (!sourceStation.value || !targetStation.value) {
    ElMessage.warning('请选择起点和终点站');
    return;
  }
  
  loading.value = true;
  
  try {
    // 查找起点和终点站的详细信息
    const source = stations.value.find(s => s.id === sourceStation.value);
    const target = stations.value.find(s => s.id === targetStation.value);
    
    if (!source || !target) {
      ElMessage.error('站点信息不完整');
      loading.value = false;
      return;
    }
    
    // 实际项目中这里应该调用后端API获取路径规划
    // 这里使用模拟数据
    await new Promise(resolve => setTimeout(resolve, 1000)); // 模拟API延迟
    
    // 生成随机路径点
    const generateRandomPoints = () => {
      const points = [];
      points.push({ lng: source.position.lng, lat: source.position.lat }); // 起点
      
      // 生成1-3个随机中间点
      const midPointCount = Math.floor(Math.random() * 3) + 1;
      for (let i = 0; i < midPointCount; i++) {
        // 在起点和终点之间随机生成中间点
        const ratio = (i + 1) / (midPointCount + 1);
        const midLng = source.position.lng + (target.position.lng - source.position.lng) * ratio + (Math.random() - 0.5) * 0.01;
        const midLat = source.position.lat + (target.position.lat - source.position.lat) * ratio + (Math.random() - 0.5) * 0.01;
        points.push({ lng: midLng, lat: midLat });
      }
      
      points.push({ lng: target.position.lng, lat: target.position.lat }); // 终点
      return points;
    };
    
    // 创建路径对象
    currentPath.value = {
      id: `path-${Date.now()}`,
      source,
      target,
      path: generateRandomPoints(),
      bikeCount: pathForm.value.bikeCount,
      color: '#409EFF',
      optimizeFor: pathForm.value.optimizeFor
    };
    
    ElMessage.success('路径规划完成');
  } catch (error) {
    console.error('路径规划失败:', error);
    ElMessage.error('路径规划失败');
  } finally {
    loading.value = false;
  }
};

const resetPath = () => {
  currentPath.value = null;
  sourceStation.value = '';
  targetStation.value = '';
  pathForm.value.bikeCount = 10;
  pathForm.value.optimizeFor = 'distance';
};

const selectPath = (path: PathData) => {
  currentPath.value = path;
  sourceStation.value = path.source.id;
  targetStation.value = path.target.id;
  pathForm.value.bikeCount = path.bikeCount;
  pathForm.value.optimizeFor = path.optimizeFor || 'distance';
  
  // 自动调整地图视野
  if (pathVisualizer.value) {
    pathVisualizer.value.drawPath();
  }
};

const deletePath = (index: number) => {
  const path = savedPaths.value[index];
  if (currentPath.value && currentPath.value.id === path.id) {
    currentPath.value = null;
  }
  savedPaths.value.splice(index, 1);
};

const saveToDispatch = () => {
  if (!currentPath.value) return;
  
  saveForm.value.name = `从 ${currentPath.value.source.name} 到 ${currentPath.value.target.name} 的调度`;
  saveDialogVisible.value = true;
};

const confirmSave = async () => {
  if (!currentPath.value) return;
  
  saving.value = true;
  
  try {
    // 模拟保存到后端API
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // 保存到本地调度计划列表
    const dispatchPlan = {
      ...currentPath.value,
      name: saveForm.value.name,
      scheduleTime: saveForm.value.scheduleTime,
      priority: saveForm.value.priority,
      note: saveForm.value.note,
      status: 'pending'
    };
    
    // 实际项目中，这里应该调用API保存到后端
    
    ElMessage.success('调度计划保存成功');
    saveDialogVisible.value = false;
    
    // 将当前路径添加到保存的路径列表
    if (currentPath.value && !savedPaths.value.some(p => p.id === currentPath.value!.id)) {
      savedPaths.value.push({...currentPath.value});
    }
  } catch (error) {
    console.error('保存调度计划失败:', error);
    ElMessage.error('保存调度计划失败');
  } finally {
    saving.value = false;
  }
};

// 禁用过去的日期
const disablePastDate = (date: Date) => {
  return date < new Date();
};

// 监听地图实例变化
watch(() => {
  if (baiduMap.value) {
    return (baiduMap.value as any).mapInstance;
  }
  return null;
}, (newVal) => {
  if (newVal) {
    mapInstance.value = newVal;
  }
});

// 组件挂载
onMounted(async () => {
  console.log('PathPlanner组件挂载');
  
  // 加载站点数据
  await loadStations();
  console.log('站点数据加载完成:', stations.value);
  
  // 确保地图组件初始化
  console.log('地图组件引用:', baiduMap.value);
  
  // 设置一个延迟，确保DOM完全渲染
  setTimeout(() => {
    if (baiduMap.value) {
      console.log('地图组件实例获取成功');
      // 地图实例会通过watch监听器自动设置
    } else {
      console.warn('地图组件实例获取失败');
    }
  }, 500);
  
  // 尝试从本地存储加载保存的路径
  const savedPathsStr = localStorage.getItem('savedPaths');
  if (savedPathsStr) {
    try {
      savedPaths.value = JSON.parse(savedPathsStr);
      console.log('从本地存储加载的路径:', savedPaths.value.length);
    } catch (e) {
      console.error('解析保存的路径失败:', e);
    }
  }
});

// 组件卸载
onUnmounted(() => {
  // 保存路径到本地存储
  localStorage.setItem('savedPaths', JSON.stringify(savedPaths.value));
});
</script>

<style scoped lang="scss">
.path-planner-container {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      font-size: 1.5rem;
    }
    
    .actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .form-card {
    margin-bottom: 20px;
  }
  
  .map-card {
    height: 700px;
    
    .map-container {
      height: 100%;
      position: relative;
      
      .map {
        height: 100%;
      }
    }
  }
  
  .saved-paths-card {
    .saved-path-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      cursor: pointer;
      
      &:hover {
        background-color: #f5f7fa;
      }
      
      .path-info {
        flex: 1;
        
        .path-name {
          font-weight: bold;
          margin-bottom: 5px;
        }
        
        .path-stations {
          font-size: 12px;
          color: #606266;
        }
      }
    }
  }
}
</style> 
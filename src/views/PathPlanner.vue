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
            
            <el-form-item label="交通方式">
              <el-radio-group v-model="pathForm.vehicleType">
                <el-radio-button value="bike">
                  <i class="fas fa-bicycle"></i> 骑行
                </el-radio-button>
                <el-radio-button value="car">
                  <i class="fas fa-car"></i> 驾车
                </el-radio-button>
                <el-radio-button value="walk">
                  <i class="fas fa-walking"></i> 步行
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="优化目标">
              <el-radio-group v-model="pathForm.optimizeFor">
                <el-radio-button value="distance">最短距离</el-radio-button>
                <el-radio-button value="time">最短时间</el-radio-button>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item v-if="pathForm.vehicleType === 'car'" label="避开选项">
              <el-checkbox v-model="pathForm.avoidHighways">避开高速</el-checkbox>
              <el-checkbox v-model="pathForm.avoidTolls">避开收费</el-checkbox>
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
              <div class="path-actions">
                <el-button type="primary" @click="showPathDetails">
                  <el-icon><InfoFilled /></el-icon>查看详情
                </el-button>
                <el-button type="success" @click="saveToDispatch" :disabled="!currentPath">保存到调度计划</el-button>
              </div>
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
            <a-map-component class="map" ref="aMapComponent" />
            <path-visualizer
              v-if="currentPath"
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
    
    <!-- 路径详情对话框 -->
    <el-dialog
      v-model="pathDetailsVisible"
      title="路径详情"
      width="50%"
      :close-on-click-modal="false"
    >
      <path-details 
        v-if="currentPath"
        :path-data="currentPath"
        @close="pathDetailsVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { Position, Refresh, Delete, InfoFilled } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AMapComponent from '../components/common/AMapComponent.vue';
import PathVisualizer from '../components/common/PathVisualizer.vue';
import PathDetails from '../components/common/PathDetails.vue';
import { useMapStore, type Station, type SavedPathData } from '../stores/mapStore';
import { pathPlanningService } from '../services/pathPlanningService';
import type { PathPlanningOptions } from '../types/path';

// Component setup
const mapStore = useMapStore();
const stations = computed(() => mapStore.stations);
const aMapComponent = ref<InstanceType<typeof AMapComponent> | null>(null);
const pathVisualizer = ref<InstanceType<typeof PathVisualizer> | null>(null);
const mapInstance = ref<any>(null);

const loading = ref(false);
const saving = ref(false);
const pathDetailsVisible = ref(false);
const saveDialogVisible = ref(false);

const pathForm = ref<PathPlanningOptions & { bikeCount: number }>({
  bikeCount: 10,
  optimizeFor: 'distance' as 'distance' | 'time',
  vehicleType: 'bike' as 'car' | 'bike' | 'walk',
  avoidHighways: false,
  avoidTolls: false,
});

const currentPath = ref<SavedPathData | null>(null);
const savedPaths = ref<SavedPathData[]>([]);

const sourceStation = ref('');
const targetStation = ref('');

const availableTargetStations = computed(() => {
  if (!sourceStation.value) {
    return stations.value;
  }
  return stations.value.filter(s => s.id !== sourceStation.value);
});

watch(sourceStation, () => {
  targetStation.value = '';
});

// 计算属性
const pathDistance = computed(() => currentPath.value?.distance.toFixed(2) ?? '0.00');
const pathDuration = computed(() => Math.round(currentPath.value?.duration ?? 0));

const findStationById = (id: string): Station | undefined => {
    return stations.value.find(station => station.id === id);
};

// Functions
const planPath = async () => {
  if (!sourceStation.value || !targetStation.value) {
    ElMessage.warning('请选择起点和终点站');
    return;
  }

  const source = findStationById(sourceStation.value);
  const target = findStationById(targetStation.value);

  if (!source || !target) {
    ElMessage.error('无法找到选择的站点信息');
    return;
  }

  loading.value = true;
  currentPath.value = null;

  try {
    const options: PathPlanningOptions = {
      optimizeFor: pathForm.value.optimizeFor,
      avoidHighways: pathForm.value.avoidHighways,
      avoidTolls: pathForm.value.avoidTolls,
      vehicleType: pathForm.value.vehicleType
    };

    const result = await pathPlanningService.planPath(source, target, options);

    const newPath: SavedPathData = {
      ...result,
      id: `path_${Date.now()}`,
      name: `${source.name} 至 ${target.name}`,
      source,
      target,
      bikeCount: pathForm.value.bikeCount,
      options,
      status: 'pending'
    };
    
    currentPath.value = newPath;
    ElMessage.success('路径规划成功');
  } catch (error: any) {
    console.error('路径规划失败:', error);
    ElMessage.error(`路径规划失败: ${error.message || '未知错误'}`);
    currentPath.value = null;
  } finally {
    loading.value = false;
  }
};

const resetPath = () => {
  sourceStation.value = '';
  targetStation.value = '';
  currentPath.value = null;
  pathForm.value.bikeCount = 1;
  mapStore.clearAllPaths();
  if (pathVisualizer.value) {
    pathVisualizer.value.clearPath();
  }
  ElMessage.info('路径已重置');
};

const showPathDetails = () => {
  if (currentPath.value) {
    pathDetailsVisible.value = true;
  } else {
    ElMessage.warning('没有可供查看详情的路径');
  }
};

const saveToDispatch = () => {
  if (!currentPath.value) {
    ElMessage.warning('没有可供保存的路径');
    return;
  }
  saveForm.value = {
    name: currentPath.value.name,
    scheduleTime: '',
    priority: 3,
    note: ''
  };
  saveDialogVisible.value = true;
};

const confirmSave = () => {
  if (!currentPath.value) return;
  saving.value = true;

  // Add the save form data to the current path
  const newSavedPath: SavedPathData = {
    ...currentPath.value,
    ...saveForm.value,
    color: generateRandomColor(),
  };

  // 模拟保存操作
  setTimeout(() => {
    savedPaths.value.push(newSavedPath);
    mapStore.addPath(newSavedPath); // Add to store for global access if needed
    ElMessage.success('成功保存到调度计划');
    saveDialogVisible.value = false;
    saving.value = false;
  }, 500);
};

const deletePath = (index: number) => {
  ElMessageBox.confirm('确定要删除这条保存的路径吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    const pathToDelete = savedPaths.value[index];
    if (currentPath.value?.id === pathToDelete.id) {
        currentPath.value = null;
    }
    savedPaths.value.splice(index, 1);
    mapStore.removePath(pathToDelete.id);
    ElMessage.success('路径删除成功');
  }).catch(() => {
    //
  });
};

const selectPath = (path: SavedPathData) => {
    currentPath.value = path;
};

const disablePastDate = (time: Date) => {
  return time.getTime() < Date.now() - 3600 * 1000 * 24;
};

const generateRandomColor = () => {
  return '#' + Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0');
};

onMounted(async () => {
  await mapStore.fetchStations();
  
  // 等待地圖組件完全加載
  await nextTick();
  
  // 嘗試獲取地圖實例
  const getMapInstance = async () => {
    if (aMapComponent.value) {
      try {
        mapInstance.value = await aMapComponent.value.getMapInstance();
        if (mapInstance.value) {
          console.log("地圖實例已在PathPlanner中獲取");
          return true;
        } else {
          console.error("在PathPlanner中未能獲取地圖實例");
          return false;
        }
      } catch (error) {
        console.error("獲取地圖實例失敗:", error);
        return false;
      }
    }
    return false;
  };
  
  // 重試機制，確保地圖實例可用
  let retryCount = 0;
  const maxRetries = 10;
  
  while (retryCount < maxRetries) {
    if (await getMapInstance()) {
      break;
    }
    retryCount++;
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  
  if (!mapInstance.value) {
    console.error("無法獲取地圖實例，路徑顯示功能可能受限");
  }
});

// 監聽地圖實例變化
watch(mapInstance, (newMapInstance) => {
  if (newMapInstance && currentPath.value) {
    // 如果地圖實例可用且有當前路徑，重新繪製路徑
    nextTick(() => {
      if (pathVisualizer.value) {
        pathVisualizer.value.drawPath();
      }
    });
  }
});

// 監聽當前路徑變化
watch(currentPath, (newPath) => {
  if (newPath && mapInstance.value) {
    // 路徑規劃成功後，確保路徑顯示
    nextTick(() => {
      if (pathVisualizer.value) {
        pathVisualizer.value.drawPath();
      }
    });
  }
});

onUnmounted(() => {
  pathPlanningService.destroy();
});

// Save Form
const saveForm = ref({
  name: '',
  scheduleTime: '',
  priority: 3,
  note: ''
});
</script>

<style scoped lang="scss">
.path-planner-container {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.map-card {
  height: 100%;
}
.map-container {
  width: 100%;
  height: 75vh; /* 强制设置一个明确的高度 */
  position: relative;
}
.map {
  width: 100%;
  height: 100%;
}
.form-card, .saved-paths-card {
  margin-bottom: 20px;
}
.saved-path-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
}
.saved-path-item:hover {
  background-color: #f5f7fa;
}
.path-info .path-name {
  font-weight: bold;
}
.path-info .path-stations {
  font-size: 12px;
  color: #909399;
}

.path-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.path-actions .el-button {
  flex: 1;
  min-width: 120px;
}
</style> 
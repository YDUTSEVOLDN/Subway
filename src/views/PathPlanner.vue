<template>
  <div class="path-planner-container">
    <div class="page-header">
      <h2>路径规划</h2>
      <div class="actions">
        <!-- 移除这里的按钮，已移到表单底部 -->
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
          
          <!-- 基本参数部分 - 始终显示 -->
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
              <el-alert
                v-if="recommendationDetails && recommendationDetails.best"
                :title="`智能推荐: ${recommendationDetails.best.name}`"
                type="success"
                :closable="false"
                class="recommendation-alert"
              >
                <div>
                  <p>
                    原因: 该站点车辆<strong style="color: #67C23A;">供过于求</strong> (供应: {{ recommendationDetails.best.supply }} | 需求: {{ recommendationDetails.best.demand }})
                    <br/>
                    且在所有可用站点中<strong style="color: #409EFF;">距离最短</strong> (约 {{ recommendationDetails.best.distance }} km)。
                  </p>
                  <el-popover placement="right" title="其他候选站点对比" width="350" trigger="click">
                    <el-table :data="recommendationDetails.others" stripe size="small">
                      <el-table-column property="name" label="站点名称" />
                      <el-table-column property="distance" label="距离终点(km)" width="150" />
                    </el-table>
                    <template #reference>
                      <el-button type="primary" link size="small">查看其他备选方案</el-button>
                    </template>
                  </el-popover>
                </div>
              </el-alert>
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
            
            <!-- 高级选项折叠部分 -->
            <div class="advanced-options">
              <el-divider content-position="center">
                <el-button 
                  type="text" 
                  @click="isAdvancedOptionsCollapsed = !isAdvancedOptionsCollapsed"
                  class="collapse-button"
                >
                  <span>{{ isAdvancedOptionsCollapsed ? '展开高级选项' : '收起高级选项' }}</span>
                  <el-icon>
                    <ArrowDown v-if="!isAdvancedOptionsCollapsed" />
                    <ArrowUp v-else />
                  </el-icon>
                </el-button>
              </el-divider>
              
              <el-collapse-transition>
                <div v-show="!isAdvancedOptionsCollapsed" class="advanced-options-content">
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
                </div>
              </el-collapse-transition>
            </div>
            
            <div class="form-actions">
              <el-button type="primary" @click="planPath" :loading="loading" :disabled="!sourceStation || !targetStation">
                <el-icon><Position /></el-icon>生成路径
              </el-button>
              <el-button @click="resetPath">
                <el-icon><Refresh /></el-icon>重置
              </el-button>
            </div>
            
            <div v-if="currentPath" class="detail-actions">
              <el-button type="primary" plain @click="showPathDetails" class="detail-button">
                <el-icon class="detail-icon"><InfoFilled /></el-icon>
                <span>查看详细路径信息</span>
                </el-button>
              </div>
          </el-form>
        </el-card>
        
        <!-- AI调度方案卡片 -->
        <el-card v-if="currentPath" class="ai-dispatch-card">
          <template #header>
            <div class="ai-card-header">
              <div class="ai-card-title">
                <el-icon class="ai-icon"><MagicStick /></el-icon>
                <span>AI调度方案</span>
              </div>
              <div class="ai-card-actions">
                <el-tag type="success" effect="dark" size="small" class="mr-2">智能生成</el-tag>
                <el-button v-if="dispatchSummary" type="primary" size="small" @click="exportToPDF">
                  <el-icon><Download /></el-icon> 导出PDF
                </el-button>
              </div>
            </div>
          </template>
          
          <div v-if="isGeneratingSummary" class="ai-loading-container">
            <div class="ai-loading-animation">
              <span></span>
              <span></span>
              <span></span>
              <span></span>
            </div>
            <p class="ai-loading-text">AI正在分析路径数据，生成最优调度方案...</p>
          </div>
          
          <div v-else-if="dispatchSummary" class="ai-summary-content" ref="dispatchSummaryContent">
            <div class="ai-summary-header">
              <el-input 
                v-model="dispatchSummary.title" 
                placeholder="方案标题" 
                class="ai-title-input"
              />
            </div>
            
            <el-tabs type="border-card" class="ai-summary-tabs">
              <el-tab-pane label="基本信息">
                <div class="ai-section">
                  <h4 class="ai-section-title">执行摘要</h4>
                  <el-input
                    v-model="dispatchSummary.summary"
                    type="textarea"
                    :rows="4"
                    placeholder="执行摘要内容"
                    resize="none"
                  />
                </div>
                
                <div class="ai-section">
                  <h4 class="ai-section-title">风险提示</h4>
                  <el-input
                    v-model="dispatchSummary.riskHint"
                    type="textarea"
                    :rows="3"
                    placeholder="风险提示内容"
                    resize="none"
                    class="warning-textarea"
                  />
                </div>
                
                <div class="ai-actions">
                  <el-button type="primary" @click="saveToDispatch">
                    <el-icon><DocumentAdd /></el-icon> 保存方案
                  </el-button>
                  <el-button @click="generateDispatchSummary">
                    <el-icon><Refresh /></el-icon> 重新生成
                  </el-button>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="详细计划">
                <div class="ai-section">
                  <h4 class="ai-section-title">执行计划</h4>
                  <el-input
                    v-model="dispatchSummary.executionPlan"
                    type="textarea"
                    :rows="5"
                    placeholder="详细执行计划"
                    resize="none"
                  />
                </div>
                
                <div class="ai-section">
                  <h4 class="ai-section-title">交通分析</h4>
                  <el-input
                    v-model="dispatchSummary.trafficAnalysis"
                    type="textarea"
                    :rows="4"
                    placeholder="交通状况分析"
                    resize="none"
                  />
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="资源与效益">
                <div class="ai-section">
                  <h4 class="ai-section-title">资源需求</h4>
                  <el-input
                    v-model="dispatchSummary.resourceRequirements"
                    type="textarea"
                    :rows="4"
                    placeholder="所需资源分析"
                    resize="none"
                  />
                </div>
                
                <div class="ai-section">
                  <h4 class="ai-section-title">预期效益</h4>
                  <el-input
                    v-model="dispatchSummary.expectedBenefits"
                    type="textarea"
                    :rows="4"
                    placeholder="预期效益分析"
                    resize="none"
                  />
                </div>
                
                <div class="ai-section">
                  <h4 class="ai-section-title">备选路线</h4>
                  <el-input
                    v-model="dispatchSummary.alternativeRoutes"
                    type="textarea"
                    :rows="4"
                    placeholder="备选路线建议"
                    resize="none"
                  />
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
          
          <div v-else class="ai-empty-state">
            <el-empty description="暂无调度方案" :image-size="60">
              <el-button type="primary" @click="generateDispatchSummary">
                <el-icon><MagicStick /></el-icon> 生成调度方案
              </el-button>
            </el-empty>
          </div>
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
              v-if="mapInstance"
              :path="currentPathForVisualizer"
              :map-instance="mapInstance"
              :auto-fit="true"
              @path-planned="onPathPlanned"
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
            v-model="saveForm.description"
            type="textarea"
            placeholder="添加备注信息"
            :rows="4"
          />
          <div class="mt-2">
            <el-button size="small" type="primary" @click="generateAndApplySummary" :loading="isGeneratingSummary">
              <el-icon><MagicStick /></el-icon> {{ saveForm.description ? '重新生成摘要' : '生成智能摘要' }}
            </el-button>
          </div>
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
        v-if="rawResultForDetails"
        :path-data="rawResultForDetails"
        :vehicleType="pathForm.vehicleType"
        @close="pathDetailsVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { Position, Refresh, Delete, InfoFilled, MagicStick, DocumentAdd, ArrowDown, ArrowUp, Download } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import AMapComponent from '../components/common/AMapComponent.vue';
import PathVisualizer from '../components/common/PathVisualizer.vue';
import PathDetails from '../components/common/PathDetails.vue';
import { useMapStore, type Station, type SavedPathData } from '../stores/mapStore';
import { PathPlanningService } from '../services/pathPlanningService';
import { schedulingPlanService } from '../services/schedulingPlanService';
import { bikeService } from '../services/bikeService';
import type { SchedulingPlanDto } from '../types';
import { PlanStatus } from '../types';
import type { PathPlanningOptions } from '../types/path';
import type { LLMDispatchSummaryResponse } from '../services/llmService';
// 导入PDF导出相关库
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

const mapStore = useMapStore();
const route = useRoute();
const stations = computed(() => mapStore.stations);
const aMapComponent = ref<InstanceType<typeof AMapComponent> | null>(null);
const pathVisualizer = ref<InstanceType<typeof PathVisualizer> | null>(null);
const mapInstance = ref<any>(null);

const recommendationDetails = ref<{ best: any; others: any[] } | null>(null);
const loading = ref(false);
const recommending = ref(false);
const saving = ref(false);
const pathDetailsVisible = ref(false);
const saveDialogVisible = ref(false);
const rawResultForDetails = ref<any>(null); // State for details dialog
const dispatchSummary = ref<LLMDispatchSummaryResponse | undefined>(undefined); // 调度方案摘要
const isGeneratingSummary = ref(false); // 是否正在生成调度方案摘要
const isAdvancedOptionsCollapsed = ref(true); // 高级选项是否折叠

const sourceStation = ref('');
const targetStation = ref('');
const detailedPathResult = ref<any>(null);

const pathForm = ref({
  bikeCount: 1,
  vehicleType: 'bike' as 'car' | 'bike' | 'walk',
  optimizeFor: 'distance' as 'distance' | 'time',
  avoidHighways: false,
  avoidTolls: false,
});

watch(targetStation, (newTargetId) => {
  if (newTargetId && stations.value.length > 0) {
    recommendSourceStation(newTargetId);
  } else {
    sourceStation.value = '';
    recommendationDetails.value = null;
  }
});

async function recommendSourceStation(targetId: string) {
  recommending.value = true;
  sourceStation.value = ''; // 清空当前起点
  recommendationDetails.value = null;

  const target = stations.value.find(s => s.id === targetId);
  console.group(`[起点推荐] 目标站: ${target?.name || targetId}`);

  try {
    if (!target) {
      console.warn('无法找到目标站点对象');
      return;
    }

    const date = mapStore.selectedDate || new Date().toISOString().split('T')[0];
    
    // 步骤 1: 先按直线距离筛选出“附近”的站点
    const reasonableDistanceLimit = 5000; // 5km
    const targetCoords = new window.AMap.LngLat(target.position.lng, target.position.lat);
    const nearbyStations = stations.value.filter(station => {
      if (station.id === target.id) return false; // 排除目标站自身
      const stationCoords = new window.AMap.LngLat(station.position.lng, station.position.lat);
      const distance = window.AMap.GeometryUtil.distance(stationCoords, targetCoords);
      return distance <= reasonableDistanceLimit;
    });

    console.log(`1. 在${reasonableDistanceLimit / 1000}km范围内找到 ${nearbyStations.length} 个附近站点。`);

    if (nearbyStations.length === 0) {
      ElMessage.info(`未在${reasonableDistanceLimit / 1000}km范围内找到任何其他站点。`);
      return;
    }

    // 步骤 2: 在“附近”的站点中，查找“供过于求”的站点
    const nearbyStationsStatus = await Promise.all(
      nearbyStations.map(s => bikeService.getBikeStatusForStation(s, date))
    );
    
    const candidateStationsInfo = nearbyStationsStatus
      .filter(status => status && status.status === 'surplus');

    if (candidateStationsInfo.length === 0) {
      ElMessage.warning(`附近${reasonableDistanceLimit / 1000}公里内没有车辆富余的站点。`);
      console.warn(`附近站点均不满足“供过于求”的条件。`);
      return;
    }

    const candidateStations = candidateStationsInfo
      .map(status => nearbyStations.find(s => s.id === status!.stationId))
      .filter((s): s is Station => s !== undefined);
    
    console.log(`2. 从附近站点中筛选出 ${candidateStations.length} 个“供过于求”的候选站点:`, candidateStations.map(s => s.name));

    // 步骤 3: 在这些“既近又富余”的站点中，计算精确路径并排序
    const pathPlanner = new PathPlanningService();
    await pathPlanner.initialize();

    const stationsWithDistance = await Promise.all(
      candidateStations.map(async (station) => {
        if (!station) return null;
        const path = await pathPlanner.planPath(station, target, { vehicleType: 'bike', optimizeFor: 'distance' });
        if (!path) return null;
        return { id: station.id, name: station.name, distance: path.distance || Infinity };
      })
    );

    const validStationsWithDistance = stationsWithDistance.filter(s => s);

    if (validStationsWithDistance.length === 0) {
      ElMessage.error(`无法为任何候选站点规划有效路径。`);
      return;
    }

    const sortedStations = validStationsWithDistance.sort((a, b) => a!.distance - b!.distance);

    // 步骤 4: 给出最终推荐
    const bestStationData = sortedStations[0];
    if (bestStationData) {
      const otherCandidatesData = sortedStations.slice(1, 5);
      const bestStationFullInfo = candidateStationsInfo.find(s => s!.stationId === bestStationData.id);

      recommendationDetails.value = {
        best: {
          name: bestStationData.name,
          distance: bestStationData.distance.toFixed(2),
          supply: bestStationFullInfo?.supply,
          demand: bestStationFullInfo?.demand,
        },
        others: otherCandidatesData.map(s => ({ name: s!.name, distance: s!.distance.toFixed(2) })),
      };

      sourceStation.value = bestStationData.id;
      ElMessage.success(`已为您推荐最合理的起点站。`);
      console.log(`✅ 最终推荐站点: ${bestStationData.name}, 骑行距离: ${bestStationData.distance.toFixed(2)} km`);
    }
  } catch (error) {
    console.error('推荐起点站时出错:', error);
    ElMessage.error('推荐起点站时发生错误。');
  } finally {
    recommending.value = false;
    console.groupEnd();
  }
}

const saveForm = ref({
  name: '',
  scheduleTime: '',
  priority: 3,
  description: '',
});

const currentPath = ref<SavedPathData | null>(null);
const savedPaths = ref<SavedPathData[]>([]); // This would be loaded from store or API

const pathPlanningSvc = new PathPlanningService();

const deletePath = (index: number) => {
  ElMessageBox.confirm('确定要删除这条保存的路径吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    savedPaths.value.splice(index, 1);
    ElMessage.success('路径已删除');
  });
};

const disablePastDate = (time: Date) => {
  return time.getTime() < Date.now();
};


onMounted(async () => {
  await mapStore.fetchStations();

  const endStationName = route.query.end as string;
  if (endStationName) {
    const station = stations.value.find(s => s.name === endStationName);
    if (station) {
      targetStation.value = station.id;
    }
  }

  const bikeCountParam = route.query.bikeCount as string;
  if (bikeCountParam) {
    const count = parseInt(bikeCountParam, 10);
    if (!isNaN(count) && count > 0) {
      pathForm.value.bikeCount = count;
    }
  }

  watch(aMapComponent, async (newVal) => {
    if (newVal) {
      mapInstance.value = await newVal.getMapInstance();
      if(mapInstance.value) {
        // Assume pathPlanningSvc is defined elsewhere and needs initialization
        // pathPlanningSvc.initialize(); 
      }
    }
  }, { immediate: true });
  
  // 添加窗口大小变化监听
  window.addEventListener('resize', handleWindowResize);
});

// 在组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize);
});

// 处理窗口大小变化
const handleWindowResize = () => {
  // 延迟执行以确保DOM已更新
  setTimeout(() => {
    refreshMap();
  }, 200);
};

const availableTargetStations = computed(() => {
  return stations.value.filter(s => s.id !== sourceStation.value);
});

const currentPathForVisualizer = computed(() => {
  if (!currentPath.value?.source || !currentPath.value?.target) {
    return undefined;
  }
  return {
    source: {
      name: currentPath.value.source.name,
      lng: currentPath.value.source.position.lng,
      lat: currentPath.value.source.position.lat
    },
    target: {
      name: currentPath.value.target.name,
      lng: currentPath.value.target.position.lng,
      lat: currentPath.value.target.position.lat
    },
    vehicleType: pathForm.value.vehicleType
  };
});

const planPath = async () => {
  if (!sourceStation.value || !targetStation.value) {
    ElMessage.warning('请先选择起点和终点站。');
    return;
  }

  loading.value = true;
  currentPath.value = null; // 重置当前路径
  dispatchSummary.value = undefined; // 重置调度方案摘要

  const source = stations.value.find(s => s.id === sourceStation.value);
  const target = stations.value.find(s => s.id === targetStation.value);

  if (source && target) {
    const options: PathPlanningOptions = {
      optimizeFor: pathForm.value.optimizeFor,
      vehicleType: pathForm.value.vehicleType,
      avoidHighways: pathForm.value.avoidHighways,
      avoidTolls: pathForm.value.avoidTolls,
    };

    const result = await pathPlanningSvc.planPath(source, target, options);

    if (result) {
    currentPath.value = {
        id: `${source.id}-${target.id}-${Date.now()}`,
        name: `${source.name} 到 ${target.name}`,
        source: source,
        target: target,
        bikeCount: pathForm.value.bikeCount,
        options: options,
      ...result,
      };
      detailedPathResult.value = result;
      ElMessage.success('路径生成成功！');
      generateDispatchSummary(); // 调用生成调度方案摘要
    } else {
      ElMessage.error('无法规划路径，两个站点之间可能没有可行的路线。');
      currentPath.value = null; // 确保在失败时也清空路径
    }
  }

    loading.value = false;
};

/**
 * 生成调度方案摘要
 */
const generateDispatchSummary = async () => {
  if (!currentPath.value || !currentPath.value.source || !currentPath.value.target) {
    return;
  }

  try {
    isGeneratingSummary.value = true; // 开始生成，显示加载动画
    
    // 导入LLM服务
    const { llmService } = await import('../services/llmService');
    
    // 获取起点和终点站的状态
    const date = mapStore.selectedDate || new Date().toISOString().split('T')[0];
    const sourceStatus = await bikeService.getBikeStatusForStation(currentPath.value.source, date);
    const targetStatus = await bikeService.getBikeStatusForStation(currentPath.value.target, date);
    
    if (!sourceStatus || !targetStatus) {
      console.warn('无法获取站点状态信息');
      return;
    }
    
    // 准备道路名称列表（如果有）
    let roadNames: string[] = [];
    if (detailedPathResult.value?.steps) {
      roadNames = detailedPathResult.value.steps
        .filter((step: any) => step.road && step.road.trim())
        .map((step: any) => step.road)
        .filter((road: string, index: number, self: string[]) => self.indexOf(road) === index)
        .slice(0, 5); // 只取前5个主要道路
    }
    
    // 准备请求参数
    const params = {
      sourceStation: {
        name: currentPath.value.source.name,
        position: currentPath.value.source.position,
        status: sourceStatus.status as 'surplus' | 'shortage' | 'balanced'
      },
      targetStation: {
        name: currentPath.value.target.name,
        position: currentPath.value.target.position,
        status: targetStatus.status as 'surplus' | 'shortage' | 'balanced'
      },
      bikeCount: currentPath.value.bikeCount,
      distance: currentPath.value.distance || 0,
      duration: currentPath.value.duration || 0,
      roadNames: roadNames.length > 0 ? roadNames : undefined,
      scheduledTime: saveForm.value.scheduleTime || undefined
    };
    
    // 调用LLM服务
    const summary = await llmService.generateDispatchSummary(params);
    
    // 更新摘要状态
    dispatchSummary.value = summary;
    
    // 更新表单
    updateSummary(summary);
    
  } catch (error) {
    console.error('生成调度方案摘要失败:', error);
    ElMessage.error('生成调度方案摘要失败，请稍后重试');
  } finally {
    isGeneratingSummary.value = false; // 结束生成，隐藏加载动画
    
    // 添加延时，确保DOM更新后再刷新地图
    setTimeout(() => {
      refreshMap();
    }, 300);
  }
};

const updateSummary = (summaryData: { 
  title: string; 
  summary: string; 
  riskHint: string;
  executionPlan?: string;
  trafficAnalysis?: string;
  resourceRequirements?: string;
  expectedBenefits?: string;
  alternativeRoutes?: string;
}) => {
  // 将LLM生成的摘要应用到保存表单中
  if (summaryData && currentPath.value) {
    saveForm.value.name = summaryData.title;
    
    // 构建完整的描述信息，包含所有详细字段
    let fullDescription = `${summaryData.summary}\n\n`;
    fullDescription += `【风险提示】\n${summaryData.riskHint}\n\n`;
    
    if (summaryData.executionPlan) {
      fullDescription += `【执行计划】\n${summaryData.executionPlan}\n\n`;
    }
    
    if (summaryData.trafficAnalysis) {
      fullDescription += `【交通分析】\n${summaryData.trafficAnalysis}\n\n`;
    }
    
    if (summaryData.resourceRequirements) {
      fullDescription += `【资源需求】\n${summaryData.resourceRequirements}\n\n`;
    }
    
    if (summaryData.expectedBenefits) {
      fullDescription += `【预期效益】\n${summaryData.expectedBenefits}\n\n`;
    }
    
    if (summaryData.alternativeRoutes) {
      fullDescription += `【备选路线】\n${summaryData.alternativeRoutes}`;
    }
    
    saveForm.value.description = fullDescription;
    ElMessage.success('调度方案摘要已生成');
  }
};

const resetPath = () => {
  sourceStation.value = '';
  targetStation.value = '';
  currentPath.value = null;
  detailedPathResult.value = null;
  pathVisualizer.value?.clearPath();
  ElMessage.info('路径已重置');
};

const saveToDispatch = () => {
  if (!currentPath.value) {
    ElMessage.warning('没有可保存的路径');
    return;
  }

  // 设置默认名称
  let defaultName = `调度: ${currentPath.value.source?.name} -> ${currentPath.value.target?.name}`;
  let defaultDescription = '';
  
  // 如果有AI生成的摘要，则使用它
  if (dispatchSummary.value) {
    defaultName = dispatchSummary.value.title;
    
    // 构建完整的描述信息，包含所有详细字段
    let fullDescription = `${dispatchSummary.value.summary}\n\n`;
    fullDescription += `【风险提示】\n${dispatchSummary.value.riskHint}\n\n`;
    
    if (dispatchSummary.value.executionPlan) {
      fullDescription += `【执行计划】\n${dispatchSummary.value.executionPlan}\n\n`;
    }
    
    if (dispatchSummary.value.trafficAnalysis) {
      fullDescription += `【交通分析】\n${dispatchSummary.value.trafficAnalysis}\n\n`;
    }
    
    if (dispatchSummary.value.resourceRequirements) {
      fullDescription += `【资源需求】\n${dispatchSummary.value.resourceRequirements}\n\n`;
    }
    
    if (dispatchSummary.value.expectedBenefits) {
      fullDescription += `【预期效益】\n${dispatchSummary.value.expectedBenefits}\n\n`;
    }
    
    if (dispatchSummary.value.alternativeRoutes) {
      fullDescription += `【备选路线】\n${dispatchSummary.value.alternativeRoutes}`;
    }
    
    defaultDescription = fullDescription;
  }
  
  // 重置表单并设置值
  saveForm.value = {
    name: defaultName,
    scheduleTime: '',
    priority: 3,
    description: defaultDescription,
  };
  
  saveDialogVisible.value = true;
};

const confirmSave = async () => {
  if (!currentPath.value || !saveForm.value.name) {
    ElMessage.warning('请填写调度名称。');
    return;
  }
  saving.value = true;
  try {
    const source = stations.value.find(s => s.id === sourceStation.value);
    const target = stations.value.find(s => s.id === targetStation.value);

    if (!source || !target) {
      ElMessage.error('无法找到起点或终点站信息。');
      return;
    }

    const newPlan: Omit<SchedulingPlanDto, 'id' | 'createdAt' | 'updatedAt'> = {
      name: saveForm.value.name,
      sourceStationName: source.name,
      targetStationName: target.name,
      bikeCount: pathForm.value.bikeCount,
      scheduleTime: saveForm.value.scheduleTime,
      description: saveForm.value.description,
      priority: saveForm.value.priority,
      pathData: JSON.stringify(currentPath.value),
      status: PlanStatus.PENDING,
    };

    await schedulingPlanService.createPlan(newPlan);
    ElMessage.success('调度计划保存成功！');
    saveDialogVisible.value = false;
  } catch (error: any) {
    console.error('保存调度计划失败:', error);
    const message = error.response?.data?.message || '保存失败，请稍后重试。';
    ElMessage.error(`保存失败: ${message}`);
  } finally {
    saving.value = false;
  }
};

const onPathPlanned = (result: any) => {
  loading.value = false;
  detailedPathResult.value = result;

  const sourceStationData = mapStore.findStationById(sourceStation.value);
  const targetStationData = mapStore.findStationById(targetStation.value);

  if (result && sourceStationData && targetStationData) {
    const newPath = {
      ...result,
      id: `${sourceStation.value}-${targetStation.value}-${Date.now()}`,
      name: `路径: ${sourceStationData.name} to ${targetStationData.name}`,
      source: sourceStationData,
      target: targetStationData,
      bikeCount: pathForm.value.bikeCount,
      options: { ...pathForm.value },
      raw: result, // Attach the raw result here
    };
    currentPath.value = newPath as SavedPathData & { raw: any };
  }
};

const selectPath = (path: SavedPathData) => {
  currentPath.value = path;
  sourceStation.value = path.source.id;
  targetStation.value = path.target.id;
  if(path.vehicleType) pathForm.value.vehicleType = path.vehicleType;
  if(path.options) {
      pathForm.value.optimizeFor = path.options.optimizeFor;
      pathForm.value.avoidHighways = path.options.avoidHighways;
      pathForm.value.avoidTolls = path.options.avoidTolls;
  }
  detailedPathResult.value = null; // Needs to be re-planned
};

const showPathDetails = async () => {
  if (!currentPath.value?.source || !currentPath.value.target) {
    ElMessage.warning('请先生成一条路径！');
    return;
  }

  const source = currentPath.value.source;
  const target = currentPath.value.target;
  const options = { vehicleType: pathForm.value.vehicleType };
  
  // Use AMap's service directly for details, ensuring separation
  const service = new (window as any).AMap[
      options.vehicleType === 'bike' ? 'Riding' :
      options.vehicleType === 'walk' ? 'Walking' : 'Driving'
  ]();

  const startLngLat = [source.position.lng, source.position.lat];
  const endLngLat = [target.position.lng, target.position.lat];

  service.search(startLngLat, endLngLat, (status: string, result: any) => {
    if (status === 'complete' && result.routes && result.routes.length > 0) {
      rawResultForDetails.value = result;
      pathDetailsVisible.value = true;
      
      // 同时生成调度方案摘要
      generateDispatchSummary();
    } else {
      ElMessage.error('获取路径详情失败');
    }
  });
};

const generateRandomColor = () => {
  return '#' + Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0');
};

/**
 * 生成调度方案摘要并直接应用到保存表单
 */
const generateAndApplySummary = async () => {
  if (!currentPath.value) return;
  
  try {
    isGeneratingSummary.value = true;
    
    // 导入LLM服务
    const { llmService } = await import('../services/llmService');
    
    // 获取起点和终点站的状态
    const date = mapStore.selectedDate || new Date().toISOString().split('T')[0];
    const sourceStatus = await bikeService.getBikeStatusForStation(currentPath.value.source, date);
    const targetStatus = await bikeService.getBikeStatusForStation(currentPath.value.target, date);
    
    if (!sourceStatus || !targetStatus) {
      console.warn('无法获取站点状态信息');
      return;
    }
    
    // 准备道路名称列表（如果有）
    let roadNames: string[] = [];
    if (detailedPathResult.value?.steps) {
      roadNames = detailedPathResult.value.steps
        .filter((step: any) => step.road && step.road.trim())
        .map((step: any) => step.road)
        .filter((road: string, index: number, self: string[]) => self.indexOf(road) === index)
        .slice(0, 5); // 只取前5个主要道路
    }
    
    // 准备请求参数
    const params = {
      sourceStation: {
        name: currentPath.value.source.name,
        position: currentPath.value.source.position,
        status: sourceStatus.status as 'surplus' | 'shortage' | 'balanced'
      },
      targetStation: {
        name: currentPath.value.target.name,
        position: currentPath.value.target.position,
        status: targetStatus.status as 'surplus' | 'shortage' | 'balanced'
      },
      bikeCount: currentPath.value.bikeCount,
      distance: currentPath.value.distance || 0,
      duration: currentPath.value.duration || 0,
      roadNames: roadNames.length > 0 ? roadNames : undefined,
      scheduledTime: saveForm.value.scheduleTime || undefined
    };
    
    // 调用LLM服务
    const summary = await llmService.generateDispatchSummary(params);
    
    // 更新摘要状态
    dispatchSummary.value = summary;
    
    // 直接更新表单
    saveForm.value.name = summary.title;
    saveForm.value.description = `${summary.summary}\n\n【风险提示】\n${summary.riskHint}\n\n`;
    
    if (summary.executionPlan) {
      saveForm.value.description += `【执行计划】\n${summary.executionPlan}\n\n`;
    }
    
    if (summary.trafficAnalysis) {
      saveForm.value.description += `【交通分析】\n${summary.trafficAnalysis}\n\n`;
    }
    
    if (summary.resourceRequirements) {
      saveForm.value.description += `【资源需求】\n${summary.resourceRequirements}\n\n`;
    }
    
    if (summary.expectedBenefits) {
      saveForm.value.description += `【预期效益】\n${summary.expectedBenefits}\n\n`;
    }
    
    if (summary.alternativeRoutes) {
      saveForm.value.description += `【备选路线】\n${summary.alternativeRoutes}`;
    }
    
    ElMessage.success('智能摘要已生成并应用');
  } catch (error) {
    console.error('生成调度方案摘要失败:', error);
    ElMessage.error('生成调度方案摘要失败，请稍后重试');
  } finally {
    isGeneratingSummary.value = false;
    
    // 刷新地图，确保摘要生成后地图显示正常
    setTimeout(() => {
      refreshMap();
    }, 300);
  }
};

/**
 * 导出调度方案为PDF
 */
const exportToPDF = async () => {
  if (!dispatchSummary.value) {
    ElMessage.warning('请先生成调度方案。');
    return;
  }

  try {
    ElMessage.info('正在生成PDF，请稍候...');
    
    // 创建一个临时的HTML元素来渲染调度方案
    const tempDiv = document.createElement('div');
    tempDiv.className = 'dispatch-pdf-container';
    tempDiv.style.width = '595px'; // A4宽度
    tempDiv.style.padding = '40px';
    tempDiv.style.position = 'absolute';
    tempDiv.style.left = '-9999px';
    tempDiv.style.fontFamily = 'Arial, sans-serif';
    
    // 添加调度方案内容
    tempDiv.innerHTML = `
      <div style="text-align: center; margin-bottom: 20px;">
        <h1 style="color: #409EFF; margin-bottom: 5px;">${dispatchSummary.value.title}</h1>
        <p style="color: #909399; font-size: 12px;">生成时间: ${new Date().toLocaleString('zh-CN')}</p>
      </div>
      
      <div style="margin-bottom: 20px;">
        <h2 style="color: #303133; border-bottom: 1px solid #EBEEF5; padding-bottom: 10px;">执行摘要</h2>
        <p style="color: #606266; line-height: 1.6;">${dispatchSummary.value.summary || '无执行摘要'}</p>
      </div>
      
      <div style="margin-bottom: 20px; background-color: #FDF6EC; padding: 15px; border-radius: 4px;">
        <h2 style="color: #E6A23C; margin-top: 0;">风险提示</h2>
        <p style="color: #606266; line-height: 1.6;">${dispatchSummary.value.riskHint || '无风险提示'}</p>
      </div>
      
      <div style="margin-bottom: 20px;">
        <h2 style="color: #303133; border-bottom: 1px solid #EBEEF5; padding-bottom: 10px;">详细执行计划</h2>
        <p style="color: #606266; line-height: 1.6;">${dispatchSummary.value.executionPlan || '无详细执行计划'}</p>
      </div>
      
      <div style="margin-bottom: 20px;">
        <h2 style="color: #303133; border-bottom: 1px solid #EBEEF5; padding-bottom: 10px;">交通分析</h2>
        <p style="color: #606266; line-height: 1.6;">${dispatchSummary.value.trafficAnalysis || '无交通分析'}</p>
      </div>
      
      <div style="margin-bottom: 20px;">
        <h2 style="color: #303133; border-bottom: 1px solid #EBEEF5; padding-bottom: 10px;">资源需求</h2>
        <p style="color: #606266; line-height: 1.6;">${dispatchSummary.value.resourceRequirements || '无资源需求'}</p>
      </div>
      
      <div style="margin-bottom: 20px;">
        <h2 style="color: #303133; border-bottom: 1px solid #EBEEF5; padding-bottom: 10px;">预期效益</h2>
        <p style="color: #606266; line-height: 1.6;">${dispatchSummary.value.expectedBenefits || '无预期效益'}</p>
      </div>
      
      <div style="margin-bottom: 20px;">
        <h2 style="color: #303133; border-bottom: 1px solid #EBEEF5; padding-bottom: 10px;">备选路线</h2>
        <p style="color: #606266; line-height: 1.6;">${dispatchSummary.value.alternativeRoutes || '无备选路线'}</p>
      </div>
      
      <div style="text-align: center; margin-top: 30px; font-size: 12px; color: #909399;">
        <p>智能交通监控与调度平台 - AI自动生成</p>
      </div>
    `;
    
    document.body.appendChild(tempDiv);
    
    // 使用html2canvas将HTML转换为canvas
    const canvas = await html2canvas(tempDiv, {
      scale: 2, // 提高清晰度
      useCORS: true,
      logging: false
    });
    
    // 创建PDF
    const pdf = new jsPDF('p', 'pt', 'a4');
    const imgData = canvas.toDataURL('image/png');
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = pdf.internal.pageSize.getHeight();
    const imgWidth = canvas.width;
    const imgHeight = canvas.height;
    const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight);
    const imgX = (pdfWidth - imgWidth * ratio) / 2;
    let imgY = 0;
    
    // 添加多页支持
    let heightLeft = imgHeight;
    let position = 0;
    
    // 添加第一页
    pdf.addImage(imgData, 'PNG', imgX, imgY, imgWidth * ratio, imgHeight * ratio);
    heightLeft -= pdfHeight;
    
    // 如果内容超过一页，添加新页面
    while (heightLeft > 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', imgX, position, imgWidth * ratio, imgHeight * ratio);
      heightLeft -= pdfHeight;
    }
    
    // 移除临时元素
    document.body.removeChild(tempDiv);
    
    // 保存PDF
    const fileName = `调度方案_${dispatchSummary.value.title}_${new Date().getTime()}.pdf`;
    pdf.save(fileName);
    
    ElMessage.success('PDF导出成功！');
    
    // 刷新地图，确保PDF导出后地图显示正常
    setTimeout(() => {
      refreshMap();
    }, 300);
  } catch (error) {
    console.error('导出PDF失败:', error);
    ElMessage.error('导出PDF失败，请稍后重试。');
  }
};

/**
 * 刷新地图实例，解决内容变化后地图显示不全的问题
 */
const refreshMap = () => {
  if (mapInstance.value) {
    // 触发地图重新渲染
    mapInstance.value.setFitView();
    // 如果上面方法不够，可以尝试以下方法
    mapInstance.value.resize();
  }
  
  // 使用AMapComponent组件的resizeMap方法
  if (aMapComponent.value) {
    aMapComponent.value.resizeMap();
  }
};
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
  margin-bottom: 20px;
}
.map-container {
  width: 100%;
  height: 85vh; /* 进一步增加地图容器高度 */
  position: relative;
  overflow: hidden; /* 防止内容溢出 */
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.map {
  width: 100%;
  height: 100%;
}

/* 添加媒体查询，在小屏幕上调整地图高度 */
@media screen and (max-height: 900px) {
  .map-container {
    height: 75vh;
  }
}

@media screen and (max-height: 700px) {
  .map-container {
    height: 65vh;
  }
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
  margin-bottom: 10px;
  
  .el-button {
    flex: 1;
  }
}

.detail-actions {
  display: flex;
  gap: 10px;
  
  .el-button {
  flex: 1;
  }
}

/* AI调度方案卡片样式 */
.ai-dispatch-card {
  margin-bottom: 20px;
  
  :deep(.el-card__header) {
    padding: 12px 15px;
    background-color: #f0f9eb;
    border-bottom: 1px solid #e1f3d8;
  }
  
  :deep(.el-card__body) {
    padding: 15px;
  }
}

.ai-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ai-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
  color: #67c23a;
  
  .ai-icon {
    font-size: 18px;
  }
}

.ai-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mr-2 {
  margin-right: 8px;
}

.ai-loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}

.ai-loading-animation {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
  
  span {
    display: inline-block;
    width: 10px;
    height: 10px;
    margin: 0 4px;
    background-color: #409EFF;
    border-radius: 50%;
    animation: ai-loading 1.4s ease-in-out infinite;
    
    &:nth-child(1) {
      animation-delay: 0s;
    }
    
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
    
    &:nth-child(4) {
      animation-delay: 0.6s;
    }
  }
}

@keyframes ai-loading {
  0%, 100% {
    transform: scale(0.5);
    opacity: 0.5;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}

.ai-loading-text {
  color: #606266;
  font-size: 14px;
  text-align: center;
}

.ai-summary-content {
  padding: 5px 0;
}

.ai-summary-header {
  margin-bottom: 15px;
}

.ai-title-input {
  width: 100%;
  
  :deep(.el-input__inner) {
    font-size: 18px;
    font-weight: bold;
    text-align: center;
    color: #303133;
    border: none;
    border-bottom: 1px dashed #DCDFE6;
    border-radius: 0;
    padding: 8px 0;
    
    &:focus {
      border-bottom-color: #409EFF;
    }
    
    &:hover {
      border-bottom-color: #C0C4CC;
    }
  }
}

.ai-summary-tabs {
  margin-top: 15px;
  
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
  
  :deep(.el-tabs__item) {
    font-size: 14px;
    height: 40px;
    line-height: 40px;
  }
  
  :deep(.el-tabs__content) {
    padding: 5px;
  }
}

.ai-section {
  margin-bottom: 20px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.ai-section-title {
  margin-bottom: 8px;
  font-size: 15px;
  color: #303133;
  font-weight: bold;
}

.ai-summary-box {
  background-color: #f8f8f8;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 15px;
  max-height: 120px;
  overflow-y: auto;
}

.ai-summary-text {
  line-height: 1.6;
  color: #606266;
  font-size: 14px;
  white-space: pre-line;
  margin: 0;
}

.warning-textarea {
  :deep(.el-textarea__inner) {
    background-color: #FDF6EC;
    border-color: #FAECD8;
    color: #E6A23C;
    
    &:focus {
      border-color: #E6A23C;
    }
  }
}

.ai-risk-box {
  margin-bottom: 15px;
}

.ai-risk-hint {
  font-size: 13px;
}

.ai-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  justify-content: space-between;
}

.ai-empty-state {
  padding: 20px 0;
}

.mt-2 {
  margin-top: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.collapse-button {
  display: flex;
  align-items: center;
  gap: 5px;
}

.form-footer {
  padding-top: 10px;
  
  &.collapsed-footer {
    padding-top: 0;
  }
}

.form-card {
  margin-bottom: 20px;
}

.advanced-options {
  margin: 15px 0;
  position: relative;
  z-index: 10;
  
  .el-divider {
    margin: 15px 0;
  }
}

.advanced-options-content {
  background-color: #f9f9f9;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border: 1px solid #ebeef5;
}

.collapse-button {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #606266;
  font-size: 14px;
  padding: 4px 8px;
  background-color: #f0f9eb;
  border-radius: 4px;
  
  .el-icon {
    font-size: 12px;
  }
  
  &:hover {
    color: #409EFF;
    background-color: #ecf5ff;
  }
}

.form-actions {
  display: flex;
  gap: 10px;
  margin: 20px 0 15px;
  
  .el-button {
    flex: 1;
    height: 40px;
  }
}

.detail-actions {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
}

.detail-button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  height: 40px;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
  
  .detail-icon {
    margin-right: 5px;
    font-size: 16px;
  }
}
</style> 
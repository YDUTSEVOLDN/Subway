<template>
  <div class="dashboard">
    <div class="map-section">
      <AMapComponent ref="mapRef" :station-statuses="allStationStatuses" />
      
      <transition name="slide-from-right">
        <div class="info-panel" v-if="mapStore.selectedStation">
          <div class="panel-header">
            <h3>{{ mapStore.selectedStation.name }}</h3>
            <el-tag size="small">{{ mapStore.selectedStation.lines.join('、') }}</el-tag>
            <el-button 
              type="info" 
              :icon="CloseBold" 
              circle 
              @click="closeInfoPanel" 
              class="close-btn"
            />
          </div>
          
          <el-divider></el-divider>
          
          <div class="flow-info">
            <h4>站点流量</h4>
            <el-skeleton :rows="5" animated v-if="isTrafficLoading" />
            <div v-else>
              <div class="flow-stats" v-if="stationTraffic">
                <div class="flow-item">
                  <div class="flow-label">实时客流</div>
                  <div class="flow-value">
                    <span class="inflow">↑ {{ stationTraffic.currentFlow.inflow }}</span>
                    <span class="outflow">↓ {{ stationTraffic.currentFlow.outflow }}</span>
                  </div>
                </div>
                
                <div class="flow-chart">
                  <TrafficChart :traffic-data="stationTraffic" />
                </div>
              </div>
              <div v-else class="no-data">
                <p>暂无流量数据</p>
              </div>
            </div>
          </div>
          
          <el-divider></el-divider>
          
          <div class="bike-info">
            <div class="bike-header">
              <h4>共享单车状况</h4>
              <el-switch
                v-model="mapStore.showCoverageCircle"
                active-text="显示覆盖范围"
                @change="handleToggleCoverage"
              ></el-switch>
            </div>
            
            <div class="supply-demand" v-if="bikeStatus">
              <div class="status-header">
                <span>供需比</span>
                <div class="status-tag" :class="bikeStatus.status">
                  {{ bikeStatusText }}
                </div>
              </div>
              <div class="supply-demand-bar">
                <div class="progress-bar">
                  <div 
                    class="progress" 
                    :class="getStatusClass(bikeStatus.ratio)"
                    :style="{width: `${Math.min(bikeStatus.ratio * 100, 100)}%`}"
                  ></div>
                </div>
                <div class="ratio-text">{{ (bikeStatus.ratio * 100).toFixed(0) }}%</div>
              </div>
              <div class="supply-demand-counts">
                <div class="count-item">
                  <div class="count-label">需求量</div>
                  <div class="count-value">{{ bikeStatus.demand }}</div>
                </div>
                <div class="count-item">
                  <div class="count-label">供应量</div>
                  <div class="count-value">{{ bikeStatus.supply }}</div>
                </div>
              </div>
            </div>
            
            <div class="alert-info" v-if="bikeStatus && bikeStatus.ratio < 0.8">
              <el-alert
                title="单车供应不足"
                type="warning"
                :description="`建议调配约${Math.ceil(bikeStatus.demand * 0.9 - bikeStatus.supply)}辆单车至该区域`"
                show-icon
              ></el-alert>
              <el-button type="primary" size="small" class="dispatch-btn" @click="createDispatchPlan">
                创建调度方案
              </el-button>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useMapStore } from '../stores/mapStore';
import { useUserStore } from '../stores/userStore'; // 导入用户 store
import AMapComponent from '../components/common/AMapComponent.vue';
import TrafficChart from '../components/charts/TrafficChart.vue';
import api from '../api';
import { useRouter } from 'vue-router';
import { bikeService } from '../services/bikeService';
import subwayData from '@/assets/subway_data.json'; // 导入地铁数据
import { CloseBold } from '@element-plus/icons-vue';
import type { StationBikeStatus } from '@/types';

// 定义接口类型
interface TrafficFlowData {
  stationId: string;
  currentFlow: {
    inflow: number;
    outflow: number;
    timestamp: string;
  };
  predictions: Array<{
    hour: number;
    inflow: number;
    outflow: number;
  }>;
}

interface BikeSupplyDemand {
  stationId: string;
  timestamp?: string;
  supply: number;
  demand: number;
  ratio: number;
  status: 'low' | 'medium' | 'high' | 'shortage' | 'balanced' | 'surplus';
}

interface Station {
  id: string;
  name: string;
  lines: string[];
  entrances: number;
  position: {
    lng: number;
    lat: number;
  };
}

const router = useRouter();
const mapStore = useMapStore();
const userStore = useUserStore(); // 使用用户 store
const mapRef = ref(null);
const allStationStatuses = ref<StationBikeStatus[]>([]);

// 新增：判断用户是否有权限查看调度信息
const canDispatch = computed(() => userStore.isAdmin || userStore.isSubwayManager);

// 当前选中站点的流量数据
const stationTraffic = ref<TrafficFlowData | null>(null);
const isTrafficLoading = ref(false);

// 当前选中站点的共享单车状态 (现在基于真实数据和流量数据计算)
const bikeStatus = ref<BikeSupplyDemand | null>(null);

// 共享单车供需状态文字
const bikeStatusText = computed(() => {
  if (!bikeStatus.value) return '';
  
  const ratio = bikeStatus.value.ratio;
  if (ratio < 0.8) return '供不应求';
  if (ratio > 1.2) return '供过于求';
  return '供需平衡';
});

// 获取供需比状态类名
const getStatusClass = (ratio: number) => {
  if (ratio < 0.8) return 'low';
  if (ratio > 1.2) return 'high';
  return 'medium';
};

// 合并 onMounted 逻辑，确保加载顺序
onMounted(async () => {
  // 1. 首先确保地铁站基础数据已加载
  if (mapStore.stations.length === 0) {
    await mapStore.loadSubwayData();
  }
  // 2. 然后再根据站点数据计算所有站点的供需状态
  allStationStatuses.value = await bikeService.getBikeStatusForAllStations();
});

// 新增：监听日期的变化，并重新计算所有站点的状态
watch(() => mapStore.selectedDate, async (newDate, oldDate) => {
  if (newDate && newDate !== oldDate) {
    // 当日期变化时，重新获取所有站点的供需状态
    allStationStatuses.value = await bikeService.getBikeStatusForAllStations();
  }
});


// 监听选中站点变化
watch(() => mapStore.selectedStation, async (station: Station | null) => {
  if (station) {
    // 重置状态并显示加载动画
    isTrafficLoading.value = true;
    stationTraffic.value = null;
    bikeStatus.value = null;

    try {
      // 修复：串行加载数据，因为 bikeStatus 依赖 traffic
      await loadStationTraffic(station.id);
      await loadBikeStatus(station);
    } catch (error) {
      console.error('加载站点详细数据失败:', error);
      // 即使失败也要清空，避免显示旧数据
      stationTraffic.value = null;
      bikeStatus.value = null;
    } finally {
      // 加载结束，隐藏动画
      isTrafficLoading.value = false;
    }
  } else {
    // 清除数据
    stationTraffic.value = null;
    bikeStatus.value = null;
  }
}, { deep: true, immediate: true });

watch(() => mapStore.selectedDate, async (newDate, oldDate) => {
  // 仅当一个站点已被选中，且日期确实发生了变化时，才重新加载数据
  if (mapStore.selectedStation && newDate !== oldDate) {
    isTrafficLoading.value = true;
    stationTraffic.value = null;
    bikeStatus.value = null;

    try {
      // 重新调用现有的加载函数
      await loadStationTraffic(mapStore.selectedStation.id);
      await loadBikeStatus(mapStore.selectedStation);
    } catch (error) {
      console.error('切换日期后加载站点详细数据失败:', error);
    } finally {
      isTrafficLoading.value = false;
    }
  }
});

// 加载站点流量数据
async function loadStationTraffic(stationId: string) {
  try {
    const date = mapStore.selectedDate;
    if (!date) {
      console.warn('未选择日期，无法加载流量数据');
      stationTraffic.value = null;
      return;
    }

    // 从导入的 JSON 数据中查找当天的数据
    const dailyData = (subwayData as Record<string, any[]>)[date] || [];
    
    // 筛选出当前选中站点所有24小时的条目
    const stationHourlyData = dailyData.filter(s => s.name === mapStore.selectedStation?.name);

    if (stationHourlyData && stationHourlyData.length > 0) {
      const currentHour = new Date().getHours();
      // 确保当前小时的数据存在，如果不存在，则使用最后一条数据作为补充
      const currentHourData = stationHourlyData[currentHour] || stationHourlyData[stationHourlyData.length - 1];

      // 格式化数据以匹配 TrafficFlowData 接口
      stationTraffic.value = {
        stationId,
        currentFlow: {
          inflow: currentHourData.in_num,
          outflow: currentHourData.out_num,
          timestamp: new Date().toISOString(),
        },
        // 直接使用每小时的数据作为预测
        predictions: stationHourlyData.map((data, index) => ({
          hour: index,
          inflow: data.in_num,
          outflow: data.out_num,
        })),
      };
    } else {
      console.warn(`在 ${date} 未找到站点 ${mapStore.selectedStation?.name} 的流量数据`);
      stationTraffic.value = null; // 未找到数据时清空
    }
  } catch (error) {
    console.error(`加载站点 ${stationId} 流量数据失败`, error);
    stationTraffic.value = null; // 失败时清空
  }
}
// 加载或计算单车供需状态
async function loadBikeStatus(station: Station) {
  const date = mapStore.selectedDate;
  if (!date) {
    bikeStatus.value = null;
    return;
  }
  bikeStatus.value = await bikeService.getBikeStatusForStation(station, date);
}

// 切换覆盖范围显示
function handleToggleCoverage() {
  if (mapRef.value) {
    // 通过组件引用调用地图组件方法
    // mapRef.value.toggleCoverageCircle();
  }
}

// 创建调度方案
const createDispatchPlan = () => {
  const station = mapStore.selectedStation;
  const status = bikeStatus.value;

  if (station && status && status.ratio < 0.8) {
    const bikeCountToDispatch = Math.ceil(status.demand * 0.9 - status.supply);
    
    router.push({
      path: '/path-planner',
      query: {
        end: station.name,
        bikeCount: bikeCountToDispatch > 0 ? bikeCountToDispatch : 1, // 确保至少调度1辆
      }
    });
  }
};

// 新增：关闭信息面板
function closeInfoPanel() {
  mapStore.selectedStation = null;
}
</script>

<style scoped lang="scss">
.dashboard {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  .top-stats {
    margin-bottom: 16px;
    
    .stat-card {
      height: 100px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      padding: 16px;
      display: flex;
      align-items: center;
      position: relative;
      overflow: hidden;
      transition: transform 0.3s ease;
      
      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 5px 15px 0 rgba(0, 0, 0, 0.1);
      }
      
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 4px;
      }
      
      &.primary::before { background-color: #409eff; }
      &.success::before { background-color: #67c23a; }
      &.warning::before { background-color: #e6a23c; }
      &.danger::before { background-color: #f56c6c; }
      
      .stat-icon {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;
        
        .el-icon {
          font-size: 24px;
          color: #fff;
        }
      }
      
      &.primary .stat-icon { background-color: #409eff; }
      &.success .stat-icon { background-color: #67c23a; }
      &.warning .stat-icon { background-color: #e6a23c; }
      &.danger .stat-icon { background-color: #f56c6c; }
      
      .stat-info {
        h3 {
          font-size: 14px;
          margin-bottom: 8px;
          color: #606266;
        }
        
        .stat-value {
          font-size: 24px;
          font-weight: bold;
          margin-bottom: 4px;
        }
        
        .stat-change {
          display: flex;
          align-items: center;
          font-size: 12px;
          
          &.up {
            color: #67c23a;
          }
          
          &.down {
            color: #f56c6c;
          }
          
          .el-icon {
            margin-right: 4px;
          }
        }
      }
    }
  }
  
  .map-section {
    flex: 1;
    position: relative;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    
    .info-panel {
      position: absolute;
      top: 100px; /* 修改：向下移动，为顶部状态栏留出空间 */
      right: 10px;
      width: 380px;
      max-height: calc(100% - 20px);
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      padding: 20px;
      overflow-y: auto;
      z-index: 100;
    }
    
    .panel-header {
      display: flex;
      align-items: center;
      gap: 10px;
      position: relative;

      .close-btn {
        position: absolute;
        top: -10px;
        right: -10px;
      }
    }
    
    .flow-info {
      h4 {
        margin-bottom: 16px;
      }
      
      .flow-stats {
        .flow-item {
          margin-bottom: 16px;
          
          .flow-label {
            font-size: 14px;
            color: #606266;
            margin-bottom: 8px;
          }
          
          .flow-value {
            display: flex;
            gap: 16px;
            
            .inflow {
              color: #409eff;
              font-weight: bold;
            }
            
            .outflow {
              color: #f56c6c;
              font-weight: bold;
            }
          }
        }
        
        .flow-chart {
          margin-bottom: 24px;
        }
      }
      
      .no-data {
        text-align: center;
        color: #909399;
        padding: 20px 0;
      }
    }
    
    .bike-info {
      .bike-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        
        h4 {
          margin: 0;
        }
      }
      
      .supply-demand {
        margin-bottom: 16px;
        
        .status-header {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
        }
        
        .supply-demand-bar {
          display: flex;
          align-items: center;
          margin-bottom: 16px;
          
          .progress-bar {
            flex: 1;
          }
          
          .ratio-text {
            margin-left: 8px;
            font-weight: bold;
          }
        }
        
        .supply-demand-counts {
          display: flex;
          justify-content: space-between;
          
          .count-item {
            text-align: center;
            flex: 1;
            
            .count-label {
              font-size: 14px;
              color: #606266;
              margin-bottom: 4px;
            }
            
            .count-value {
              font-size: 20px;
              font-weight: bold;
            }
          }
        }
      }
      
      .alert-info {
        .dispatch-btn {
          margin-top: 12px;
          width: 100%;
        }
      }
    }
  }
}

// 修改：从右侧滑入/滑出的动画
.slide-from-right-enter-active,
.slide-from-right-leave-active {
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.slide-from-right-enter-from,
.slide-from-right-leave-to {
  transform: translateX(120%);
  opacity: 0;
}
</style> 
<template>
  <div class="dashboard">
    <div class="top-stats">
      <el-row :gutter="16">
        <el-col :span="6" v-for="stat in stats" :key="stat.title">
          <div class="stat-card" :class="stat.type">
            <div class="stat-icon">
              <el-icon><component :is="stat.icon"></component></el-icon>
            </div>
            <div class="stat-info">
              <h3>{{ stat.title }}</h3>
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-change" :class="{ 'up': stat.change > 0, 'down': stat.change < 0 }">
                <el-icon v-if="stat.change > 0"><CaretTop /></el-icon>
                <el-icon v-else-if="stat.change < 0"><CaretBottom /></el-icon>
                {{ Math.abs(stat.change) }}%
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <div class="map-section">
      <BaiduMap ref="mapRef" />
      
      <div class="info-panel" v-if="mapStore.selectedStation">
        <div class="panel-header">
          <h3>{{ mapStore.selectedStation.name }}</h3>
          <el-tag size="small">{{ mapStore.selectedStation.lines.join('、') }}</el-tag>
        </div>
        
        <el-divider></el-divider>
        
        <div class="flow-info">
          <h4>站点流量</h4>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useMapStore } from '../stores/mapStore';
import BaiduMap from '../components/common/BaiduMap.vue';
import TrafficChart from '../components/charts/TrafficChart.vue';
import api from '../api';
import { useRouter } from 'vue-router';

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
const mapRef = ref(null);

// 初始数据
const stats = ref([
  {
    title: '今日客流',
    value: '23,522',
    change: 15.8,
    icon: 'UserFilled',
    type: 'primary'
  },
  {
    title: '单车总数',
    value: '6,421',
    change: 2.3,
    icon: 'Bicycle',
    type: 'success'
  },
  {
    title: '调度次数',
    value: '18',
    change: -5.4,
    icon: 'Location',
    type: 'warning'
  },
  {
    title: '预警事件',
    value: '3',
    change: -12.5,
    icon: 'Warning',
    type: 'danger'
  }
]);

// 当前选中站点的流量数据
const stationTraffic = ref<TrafficFlowData | null>(null);

// 当前选中站点的共享单车状态
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

// 监听选中站点变化
watch(() => mapStore.selectedStation, async (station) => {
  if (station) {
    await loadStationData(station.id);
  } else {
    stationTraffic.value = null;
    bikeStatus.value = null;
  }
}, { immediate: true });

// 加载站点相关数据
async function loadStationData(stationId: string) {
  try {
    // 获取流量预测数据
    const traffic = await api.getTrafficPredict(stationId);
    stationTraffic.value = traffic as TrafficFlowData;
    
    // 获取共享单车状态
    const status = await api.getBikeStatus(stationId);
    bikeStatus.value = status as BikeSupplyDemand;
    
    // 获取周边单车分布
    const bikes = await api.getBikesNearStation(stationId);
    // 转换bikes类型，确保type字段符合要求
    const typedBikes = bikes.map(bike => ({
      ...bike,
      type: bike.type === 'electric' || bike.type === 'regular' ? 
        bike.type as 'electric' | 'regular' : 'regular' as const
    }));
    // 使用正确的方法名或直接设置数据
    mapStore.bikesData = typedBikes;
  } catch (error) {
    console.error('加载站点数据失败', error);
  }
}

// 切换覆盖范围显示
function handleToggleCoverage() {
  if (mapRef.value) {
    // 通过组件引用调用地图组件方法
    // mapRef.value.toggleCoverageCircle();
  }
}

// 创建调度方案
function createDispatchPlan() {
  router.push('/dispatch');
}

onMounted(async () => {
  // 加载所有站点数据
  try {
    const stations = await api.getStations();
    // 转换stations类型，确保id字段为string类型
    const typedStations = stations.map(station => ({
      ...station,
      id: String(station.id)
    }));
    // 使用正确的方法名或直接设置数据
    mapStore.stations = typedStations as Station[];
  } catch (error) {
    console.error('加载站点数据失败', error);
  }
});
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
      top: 16px;
      right: 16px;
      width: 320px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      padding: 16px;
      z-index: 10;
      max-height: calc(100% - 32px);
      overflow-y: auto;
      
      .panel-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        
        h3 {
          margin: 0;
          font-size: 18px;
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
            height: 200px;
            margin-bottom: 16px;
          }
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
}
</style> 
<template>
  <div class="dashboard">
    <div class="map-section">
      <AMapComponent ref="mapRef" />
      
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
import AMapComponent from '../components/common/AMapComponent.vue';
import TrafficChart from '../components/charts/TrafficChart.vue';
import api from '../api';
import { useRouter } from 'vue-router';
import realBikeData from '@/assets/bike_data.json';

// 定义接口类型
interface BikeData {
  station_name: string;
  bike_count: number;
}

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

// 当前选中站点的流量数据
const stationTraffic = ref<TrafficFlowData | null>(null);

// 当前选中站点的共享单车状态 (现在基于真实数据和流量数据计算)
const bikeStatus = computed<BikeSupplyDemand | null>(() => {
  const station = mapStore.selectedStation;
  const traffic = stationTraffic.value;
  if (!station || !traffic) return null;

  const bikeInfo = (realBikeData as BikeData[]).find((b: BikeData) => b.station_name === station.name);
  const supply = bikeInfo ? bikeInfo.bike_count : 0;

  // 使用出站人流作为需求量的基础 (可以根据实际业务调整系数)
  const demand = Math.ceil(traffic.currentFlow.outflow * 0.1); 
  const ratio = supply / (demand || 1);

  return {
    stationId: station.id,
    supply,
    demand,
    ratio,
    status: getStatusClass(ratio)
  };
});

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
  }
}, { immediate: true });

// 加载站点相关数据
async function loadStationData(stationId: string) {
  try {
    // 获取流量预测数据
    const traffic = await api.getTrafficPredict(stationId);
    stationTraffic.value = traffic as TrafficFlowData;
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

// 在组件挂载时加载站点数据
onMounted(async () => {
  if (mapStore.stations.length === 0) {
    await mapStore.loadSubwayData();
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
        margin-bottom: 8px;
        
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
            margin-bottom: 24px;
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
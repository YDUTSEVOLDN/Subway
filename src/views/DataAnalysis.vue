<template>
  <div class="data-analysis-container">
    <div class="page-header">
      <h2>数据分析</h2>
      <div class="time-selector">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          :disabled-date="disableFutureDate"
          @change="handleDateChange"
        ></el-date-picker>
      </div>
    </div>
    
    <el-row :gutter="16" class="dashboard-cards">
      <el-col :span="8">
        <el-card class="total-card">
          <div class="card-header">
            <div class="title">总客流量</div>
            <el-icon class="icon"><PieChart /></el-icon>
          </div>
          <div class="card-content">
            <div class="big-number">128,293</div>
            <div class="comparison">
              <el-icon><CaretTop /></el-icon>
              <span>较前日增长 8.2%</span>
            </div>
          </div>
          <div class="mini-chart">
            <div ref="flowTrendChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="total-card">
          <div class="card-header">
            <div class="title">共享单车使用量</div>
            <el-icon class="icon"><Bicycle /></el-icon>
          </div>
          <div class="card-content">
            <div class="big-number">42,189</div>
            <div class="comparison">
              <el-icon><CaretTop /></el-icon>
              <span>较前日增长 5.6%</span>
            </div>
          </div>
          <div class="mini-chart">
            <div ref="bikeUsageChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="total-card">
          <div class="card-header">
            <div class="title">平均供需比</div>
            <el-icon class="icon"><DataAnalysis /></el-icon>
          </div>
          <div class="card-content">
            <div class="big-number">94.3%</div>
            <div class="comparison negative">
              <el-icon><CaretBottom /></el-icon>
              <span>较前日下降 2.1%</span>
            </div>
          </div>
          <div class="mini-chart">
            <div ref="supplyDemandChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <div class="analysis-charts">
      <el-row :gutter="16">
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="card-header-with-filter">
                <div class="title">站点客流量排名</div>
                <el-select v-model="flowRankType" placeholder="选择类型" size="small">
                  <el-option label="进站人数" value="inflow"></el-option>
                  <el-option label="出站人数" value="outflow"></el-option>
                  <el-option label="总客流量" value="total"></el-option>
                </el-select>
              </div>
            </template>
            <div class="chart-container">
              <div ref="stationRankChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="8">
          <el-card>
            <template #header>
              <div>站点类型分布</div>
            </template>
            <div class="chart-container">
              <div ref="stationTypeChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="16" class="second-row">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header-with-filter">
                <div class="title">时段客流量趋势</div>
                <el-radio-group v-model="timeRange" size="small">
                  <el-radio-button label="day">日</el-radio-button>
                  <el-radio-button label="week">周</el-radio-button>
                  <el-radio-button label="month">月</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div class="chart-container">
              <div ref="timeFlowChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header-with-filter">
                <div class="title">站点供需平衡状态</div>
                <el-select v-model="timeOfDay" placeholder="选择时段" size="small">
                  <el-option label="早高峰 (7:00-9:00)" value="morning"></el-option>
                  <el-option label="日间 (9:00-17:00)" value="day"></el-option>
                  <el-option label="晚高峰 (17:00-19:00)" value="evening"></el-option>
                  <el-option label="夜间 (19:00-23:00)" value="night"></el-option>
                </el-select>
              </div>
            </template>
            <div class="chart-container">
              <div ref="supplyDemandMapChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="16" class="third-row">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header-with-filter">
                <div class="title">周客流热力图</div>
                <el-select v-model="selectedStationForHeatmap" placeholder="选择站点" size="small">
                  <el-option
                    v-for="station in stations"
                    :key="station.id"
                    :label="station.name"
                    :value="station.id"
                  />
                </el-select>
              </div>
            </template>
            <div class="chart-container">
              <heat-map-chart
                :heatmap-data="heatmapData"
                title="周客流热力图"
                height="320px"
              />
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header-with-filter">
                <div class="title">单车周转情况</div>
                <el-tooltip content="导出分析报告">
                  <el-button
                    type="primary"
                    size="small"
                    circle
                    @click="exportReport"
                    :loading="exportLoading"
                  >
                    <el-icon><Download /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </template>
            <div class="chart-container">
              <div ref="bikeRotationChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="16" class="fourth-row">
        <el-col :span="24">
          <el-card>
            <template #header>
              <div class="card-header-with-filter">
                <div class="title">站点比较分析</div>
                <div class="filter-group">
                  <el-select
                    v-model="compareStations"
                    placeholder="选择站点"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    size="small"
                    style="width: 350px"
                    :max-collapse-tags="2"
                  >
                    <el-option
                      v-for="station in stations"
                      :key="station.id"
                      :label="station.name"
                      :value="station.id"
                    />
                  </el-select>
                  <el-select v-model="compareMetric" placeholder="选择指标" size="small">
                    <el-option label="客流量" value="flow"></el-option>
                    <el-option label="单车周转率" value="rotation"></el-option>
                    <el-option label="供需比" value="supply"></el-option>
                  </el-select>
                </div>
              </div>
            </template>
            <div class="chart-container" style="height: 400px">
              <div ref="stationCompareChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import HeatMapChart from '../components/charts/HeatMapChart.vue';
import { useMapStore } from '../stores/mapStore';
import { PieChart, CaretTop, CaretBottom, DataAnalysis, Download, Bicycle } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import type { Station } from '../stores/mapStore';

// 状态定义
const selectedDate = ref(new Date());
const flowRankType = ref('total');
const timeRange = ref('day');
const timeOfDay = ref('morning');
const selectedStationForHeatmap = ref('');
const compareStations = ref<string[]>([]);
const compareMetric = ref('flow');
const exportLoading = ref(false);

// 获取站点数据
const mapStore = useMapStore();
const stations = ref<Station[]>([]);

// 图表引用
const flowTrendChart = ref<HTMLElement | null>(null);
const bikeUsageChart = ref<HTMLElement | null>(null);
const supplyDemandChart = ref<HTMLElement | null>(null);
const stationRankChart = ref<HTMLElement | null>(null);
const stationTypeChart = ref<HTMLElement | null>(null);
const timeFlowChart = ref<HTMLElement | null>(null);
const supplyDemandMapChart = ref<HTMLElement | null>(null);
const bikeRotationChart = ref<HTMLElement | null>(null);
const stationCompareChart = ref<HTMLElement | null>(null);

// 图表实例
const charts: { [key: string]: echarts.ECharts | null } = {
  flowTrend: null,
  bikeUsage: null,
  supplyDemand: null,
  stationRank: null,
  stationType: null,
  timeFlow: null,
  supplyDemandMap: null,
  bikeRotation: null,
  stationCompare: null
};

// 热力图数据
const heatmapData = ref({
  hours: Array.from({ length: 24 }, (_, i) => `${i}:00`),
  days: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  data: [] as Array<[number, number, number]>
});

// 生成热力图数据
const generateHeatmapData = () => {
  const data: Array<[number, number, number]> = [];
  
  // 根据站点ID生成不同的数据模式
  const stationId = selectedStationForHeatmap.value;
  const baseValue = stationId ? parseInt(stationId.replace(/\D/g, '')) * 10 : 100;
  
  for (let day = 0; day < 7; day++) {
    for (let hour = 0; hour < 24; hour++) {
      // 创建高峰时段模式
      let value = baseValue;
      
      // 工作日模式
      if (day < 5) {
        // 早高峰 7-9点
        if (hour >= 7 && hour <= 9) {
          value += Math.floor(Math.random() * 300) + 200;
        }
        // 晚高峰 17-19点
        else if (hour >= 17 && hour <= 19) {
          value += Math.floor(Math.random() * 250) + 150;
        }
        // 中午
        else if (hour >= 12 && hour <= 13) {
          value += Math.floor(Math.random() * 150) + 100;
        }
        // 深夜
        else if (hour >= 0 && hour <= 5) {
          value += Math.floor(Math.random() * 30);
        }
        // 其他时段
        else {
          value += Math.floor(Math.random() * 100) + 50;
        }
      } 
      // 周末模式
      else {
        // 上午到下午
        if (hour >= 10 && hour <= 18) {
          value += Math.floor(Math.random() * 200) + 100;
        }
        // 深夜
        else if (hour >= 0 && hour <= 6) {
          value += Math.floor(Math.random() * 50);
        }
        // 其他时段
        else {
          value += Math.floor(Math.random() * 150) + 50;
        }
      }
      
      data.push([hour, day, value]);
    }
  }
  
  heatmapData.value.data = data;
};

// 导出分析报告
const exportReport = () => {
  exportLoading.value = true;
  
  // 模拟导出过程
  setTimeout(() => {
    // 创建一个Blob对象
    const reportData = {
      exportDate: new Date().toISOString(),
      selectedDate: selectedDate.value,
      totalFlow: 128293,
      bikeUsage: 42189,
      supplyDemandRatio: 94.3,
      stations: stations.value.map(station => ({
        id: station.id,
        name: station.name,
        inFlow: Math.floor(Math.random() * 10000) + 5000,
        outFlow: Math.floor(Math.random() * 10000) + 5000
      }))
    };
    
    const blob = new Blob([JSON.stringify(reportData, null, 2)], { type: 'application/json' });
    
    // 创建下载链接
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `交通分析报告_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    
    exportLoading.value = false;
    
    // 显示成功消息
    ElMessage({
      message: '分析报告导出成功',
      type: 'success'
    });
  }, 1500);
};

// 初始化单车周转情况图表
const initBikeRotationChart = () => {
  if (!bikeRotationChart.value) return;
  
  charts.bikeRotation = echarts.init(bikeRotationChart.value);
  
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  const rotationData = days.map(() => Math.floor(Math.random() * 5) + 3); // 每辆单车平均每天周转3-8次
  const utilizationData = days.map(() => Math.floor(Math.random() * 30) + 60); // 单车利用率 60-90%
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['周转次数', '单车利用率'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: days
    },
    yAxis: [
      {
        type: 'value',
        name: '周转次数',
        min: 0,
        max: 10,
        interval: 2,
        axisLabel: {
          formatter: '{value} 次'
        }
      },
      {
        type: 'value',
        name: '利用率',
        min: 0,
        max: 100,
        interval: 20,
        axisLabel: {
          formatter: '{value} %'
        }
      }
    ],
    series: [
      {
        name: '周转次数',
        type: 'bar',
        data: rotationData,
        barWidth: '40%',
        itemStyle: {
          color: '#409EFF'
        }
      },
      {
        name: '单车利用率',
        type: 'line',
        yAxisIndex: 1,
        data: utilizationData,
        lineStyle: {
          color: '#E6A23C',
          width: 3
        },
        itemStyle: {
          color: '#E6A23C'
        },
        symbol: 'circle',
        symbolSize: 8,
        smooth: true
      }
    ]
  };
  
  charts.bikeRotation.setOption(option);
};

// 初始化站点比较图表
const initStationCompareChart = () => {
  if (!stationCompareChart.value || compareStations.value.length === 0) return;
  
  charts.stationCompare = echarts.init(stationCompareChart.value);
  
  const stationNames = compareStations.value.map(id => {
    const station = stations.value.find(s => s.id === id);
    return station ? station.name : id;
  });
  
  const metricName = {
    'flow': '客流量',
    'rotation': '单车周转率',
    'supply': '供需比'
  }[compareMetric.value] || '客流量';
  
  // 生成7天的数据
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  const series = compareStations.value.map((id, index) => {
    const values = days.map(() => {
      if (compareMetric.value === 'flow') {
        return Math.floor(Math.random() * 5000) + 2000;
      } else if (compareMetric.value === 'rotation') {
        return (Math.random() * 5 + 3).toFixed(1);
      } else {
        return (Math.random() * 0.5 + 0.7).toFixed(2);
      }
    });
    
    return {
      name: stationNames[index],
      type: 'line',
      data: values,
      smooth: true,
      symbolSize: 6
    };
  });
  
  const option = {
    title: {
      text: `站点${metricName}比较`,
      left: 'center',
      top: 0
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: stationNames,
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '80px',
      containLabel: true
    },
    toolbox: {
      feature: {
        saveAsImage: {}
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: days
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => {
          if (compareMetric.value === 'flow') {
            return value;
          } else if (compareMetric.value === 'rotation') {
            return `${value}次`;
          } else {
            return `${value}`;
          }
        }
      }
    },
    series: series
  };
  
  charts.stationCompare.setOption(option);
};

// 初始化所有图表
const initAllCharts = () => {
  initFlowTrendChart();
  initBikeUsageChart();
  initSupplyDemandChart();
  initStationRankChart();
  initStationTypeChart();
  initTimeFlowChart();
  initSupplyDemandMapChart();
  initBikeRotationChart();
  if (compareStations.value.length > 0) {
    initStationCompareChart();
  }
};

// 禁用未来日期
const disableFutureDate = (date: Date) => {
  return date > new Date();
};

// 日期变更处理
const handleDateChange = () => {
  // 重新加载数据并更新图表
  initAllCharts();
  generateHeatmapData();
};

// 加载站点数据
const loadStations = async () => {
  try {
    await mapStore.loadStations();
    stations.value = mapStore.getStations;
    
    if (stations.value.length > 0) {
      // 默认选择第一个站点
      selectedStationForHeatmap.value = stations.value[0].id;
      // 默认选择前三个站点进行比较
      compareStations.value = stations.value.slice(0, Math.min(3, stations.value.length)).map(s => s.id);
    }
    
    generateHeatmapData();
  } catch (error) {
    console.error('加载站点数据失败:', error);
  }
};

// 监听站点和比较指标变化
watch([selectedStationForHeatmap], () => {
  generateHeatmapData();
});

watch([compareStations, compareMetric], () => {
  if (compareStations.value.length > 0) {
    initStationCompareChart();
  }
}, { deep: true });

// 监听排名和时段变化
watch(flowRankType, () => {
  initStationRankChart();
});

watch(timeRange, () => {
  initTimeFlowChart();
});

watch(timeOfDay, () => {
  initSupplyDemandMapChart();
});

// 组件挂载
onMounted(() => {
  loadStations();
  initAllCharts();
  window.addEventListener('resize', resizeCharts);
});

// 组件卸载
onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts);
  disposeCharts();
});

// 调整图表大小
const resizeCharts = () => {
  Object.values(charts).forEach(chart => {
    chart?.resize();
  });
};

// 销毁图表实例
const disposeCharts = () => {
  Object.values(charts).forEach(chart => {
    chart?.dispose();
  });
};

// 流量趋势迷你图表
const initFlowTrendChart = () => {
  if (!flowTrendChart.value) return;
  
  if (charts.flowTrend) {
    charts.flowTrend.dispose();
  }
  
  charts.flowTrend = echarts.init(flowTrendChart.value);
  
  const option = {
    grid: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    },
    xAxis: {
      type: 'category',
      show: false,
      data: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00']
    },
    yAxis: {
      type: 'value',
      show: false
    },
    series: [
      {
        data: [2200, 1800, 1600, 3800, 2900, 3100, 3900, 3000],
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: {
          color: '#409EFF',
          width: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(64, 158, 255, 0.3)'
              },
              {
                offset: 1,
                color: 'rgba(64, 158, 255, 0)'
              }
            ]
          }
        }
      }
    ],
    animation: false
  };
  
  charts.flowTrend.setOption(option);
};

// 单车使用量迷你图表
const initBikeUsageChart = () => {
  if (!bikeUsageChart.value) return;
  
  if (charts.bikeUsage) {
    charts.bikeUsage.dispose();
  }
  
  charts.bikeUsage = echarts.init(bikeUsageChart.value);
  
  const option = {
    grid: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    },
    xAxis: {
      type: 'category',
      show: false,
      data: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00']
    },
    yAxis: {
      type: 'value',
      show: false
    },
    series: [
      {
        data: [800, 600, 900, 2800, 1900, 1700, 2600, 1200],
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: {
          color: '#67C23A',
          width: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(103, 194, 58, 0.3)'
              },
              {
                offset: 1,
                color: 'rgba(103, 194, 58, 0)'
              }
            ]
          }
        }
      }
    ],
    animation: false
  };
  
  charts.bikeUsage.setOption(option);
};

// 供需比迷你图表
const initSupplyDemandChart = () => {
  if (!supplyDemandChart.value) return;
  
  if (charts.supplyDemand) {
    charts.supplyDemand.dispose();
  }
  
  charts.supplyDemand = echarts.init(supplyDemandChart.value);
  
  const option = {
    grid: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    },
    xAxis: {
      type: 'category',
      show: false,
      data: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00']
    },
    yAxis: {
      type: 'value',
      show: false,
      min: 0.6,
      max: 1.4
    },
    series: [
      {
        data: [1.2, 1.3, 1.1, 0.8, 0.9, 1.0, 0.7, 1.0],
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: {
          color: '#E6A23C',
          width: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(230, 162, 60, 0.3)'
              },
              {
                offset: 1,
                color: 'rgba(230, 162, 60, 0)'
              }
            ]
          }
        }
      }
    ],
    animation: false
  };
  
  charts.supplyDemand.setOption(option);
};

// 站点排名图表
const initStationRankChart = () => {
  if (!stationRankChart.value) return;
  
  if (charts.stationRank) {
    charts.stationRank.dispose();
  }
  
  charts.stationRank = echarts.init(stationRankChart.value);
  
  const stations = ['人民广场站', '东直门站', '西单站', '北京南站', '国贸站', '朝阳门站', '西直门站', '王府井站', '宣武门站', '菜市口站'];
  const values = [4230, 3820, 3650, 3320, 3050, 2780, 2650, 2520, 2340, 2120];
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: stations,
      inverse: true,
      axisLabel: {
        color: '#606266'
      }
    },
    series: [
      {
        name: '客流量',
        type: 'bar',
        data: values,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              {
                offset: 0,
                color: '#409EFF'
              },
              {
                offset: 1,
                color: '#a0cfff'
              }
            ]
          },
          borderRadius: [0, 4, 4, 0]
        }
      }
    ]
  };
  
  charts.stationRank.setOption(option);
};

// 站点类型分布图表
const initStationTypeChart = () => {
  if (!stationTypeChart.value) return;
  
  if (charts.stationType) {
    charts.stationType.dispose();
  }
  
  charts.stationType = echarts.init(stationTypeChart.value);
  
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      formatter: (name: string) => {
        const map: Record<string, { value: number, percent: string }> = {
          '交通枢纽': { value: 12, percent: '24%' },
          '商业区': { value: 18, percent: '36%' },
          '居民区': { value: 9, percent: '18%' },
          '办公区': { value: 8, percent: '16%' },
          '景点': { value: 3, percent: '6%' }
        };
        return `${name}: ${map[name].value} (${map[name].percent})`;
      }
    },
    series: [
      {
        name: '站点类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '14',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 12, name: '交通枢纽' },
          { value: 18, name: '商业区' },
          { value: 9, name: '居民区' },
          { value: 8, name: '办公区' },
          { value: 3, name: '景点' }
        ]
      }
    ]
  };
  
  charts.stationType.setOption(option);
};

// 时段客流量趋势图
const initTimeFlowChart = () => {
  if (!timeFlowChart.value) return;
  
  if (charts.timeFlow) {
    charts.timeFlow.dispose();
  }
  
  charts.timeFlow = echarts.init(timeFlowChart.value);
  
  const timeData = timeRange.value === 'day' 
    ? ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'] 
    : timeRange.value === 'week'
    ? ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    : ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
  
  const inflowData = timeRange.value === 'day' 
    ? [800, 600, 450, 400, 3500, 2800, 1500, 1800, 2000, 3800, 2500, 1200]
    : timeRange.value === 'week'
    ? [12500, 13200, 12800, 13500, 14800, 8500, 7200]
    : [320000, 302000, 301000, 334000, 390000, 330000, 320000, 301000, 352000, 374000, 390000, 420000];
  
  const outflowData = timeRange.value === 'day'
    ? [1000, 500, 400, 350, 1200, 3500, 2800, 1900, 1800, 1500, 3500, 2000]
    : timeRange.value === 'week'
    ? [12800, 13000, 12500, 13800, 15000, 8000, 7000]
    : [330000, 310000, 305000, 340000, 380000, 320000, 325000, 310000, 360000, 370000, 385000, 415000];
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['进站', '出站']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeData
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '进站',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: {
          width: 0
        },
        showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgb(128, 255, 165)'
              },
              {
                offset: 1,
                color: 'rgb(1, 191, 236)'
              }
            ]
          }
        },
        emphasis: {
          focus: 'series'
        },
        data: inflowData
      },
      {
        name: '出站',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: {
          width: 0
        },
        showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgb(0, 221, 255)'
              },
              {
                offset: 1,
                color: 'rgb(77, 119, 255)'
              }
            ]
          }
        },
        emphasis: {
          focus: 'series'
        },
        data: outflowData
      }
    ]
  };
  
  charts.timeFlow.setOption(option);
};

// 站点供需平衡状态图表（热力图）
const initSupplyDemandMapChart = () => {
  if (!supplyDemandMapChart.value) return;
  
  if (charts.supplyDemandMap) {
    charts.supplyDemandMap.dispose();
  }
  
  charts.supplyDemandMap = echarts.init(supplyDemandMapChart.value);
  
  // 模拟数据生成
  const hours = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23];
  const days = ['人民广场站', '东直门站', '西单站', '北京南站', '国贸站', '朝阳门站', '西直门站'];

  // 生成模拟供需比数据
  function getVirtualData() {
    const data = [];
    for (let i = 0; i < days.length; i++) {
      for (let j = 0; j < hours.length; j++) {
        let value = null;
        
        // 工作日早高峰模拟数据
        if (j >= 7 && j <= 9) {
          value = (Math.random() * 0.4 + 0.6).toFixed(2); // 0.6-1.0
        } 
        // 工作日晚高峰模拟数据
        else if (j >= 17 && j <= 19) {
          value = (Math.random() * 0.3 + 0.7).toFixed(2); // 0.7-1.0
        } 
        // 工作时间段
        else if (j >= 10 && j <= 16) {
          value = (Math.random() * 0.5 + 1.0).toFixed(2); // 1.0-1.5
        } 
        // 夜间时段
        else {
          value = (Math.random() * 0.6 + 0.8).toFixed(2); // 0.8-1.4
        }
        
        data.push([i, j, value]);
      }
    }
    return data;
  }

  const option = {
    tooltip: {
      position: 'top',
      formatter: function (params: any) {
        const value = params.data[2];
        let status = '平衡';
        if (parseFloat(value) < 0.8) {
          status = '供不应求';
        } else if (parseFloat(value) > 1.2) {
          status = '供过于求';
        }
        return `${days[params.data[0]]} ${params.data[1]}:00<br>供需比: ${params.data[2]}<br>状态: ${status}`;
      }
    },
    grid: {
      top: '10%',
      left: '3%',
      right: '10%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours,
      axisLabel: {
        formatter: '{value}:00'
      },
      splitArea: {
        show: true
      }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: {
        show: true
      }
    },
    visualMap: {
      min: 0.6,
      max: 1.4,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: [
          '#f56c6c', // 红色 - 严重供不应求
          '#e6a23c', // 黄色 - 轻微供不应求
          '#67c23a', // 绿色 - 供需平衡
          '#909399'  // 灰色 - 供过于求
        ]
      }
    },
    series: [{
      name: '供需比',
      type: 'heatmap',
      data: getVirtualData(),
      label: {
        show: false
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  };

  charts.supplyDemandMap.setOption(option);
};
</script>

<style scoped lang="scss">
.data-analysis-container {
  height: 100%;
  overflow-y: auto;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    h2 {
      margin: 0;
      font-size: 20px;
    }
  }
  
  .dashboard-cards {
    margin-bottom: 16px;
    
    .total-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        
        .title {
          font-size: 16px;
          color: #606266;
        }
        
        .icon {
          font-size: 24px;
          color: #409eff;
        }
      }
      
      .card-content {
        margin-bottom: 16px;
        
        .big-number {
          font-size: 24px;
          font-weight: bold;
          margin-bottom: 8px;
        }
        
        .comparison {
          font-size: 12px;
          color: #67c23a;
          display: flex;
          align-items: center;
          
          &.negative {
            color: #f56c6c;
          }
        }
      }
      
      .mini-chart {
        height: 50px;
        margin: 8px -20px -20px;
        
        .chart {
          height: 100%;
          width: 100%;
        }
      }
    }
  }
  
  .analysis-charts {
    .card-header-with-filter {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .chart-container {
      height: 320px;
      
      .chart {
        height: 100%;
        width: 100%;
      }
    }
    
    .second-row {
      margin-top: 16px;
    }
  }
}
</style> 
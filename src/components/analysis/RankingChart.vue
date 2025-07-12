<template>
  <el-card>
    <template #header>
      <div class="card-header-with-filter">
        <div class="title">站点客流量排名</div>
        <el-select v-model="flowRankType" placeholder="选择类型" size="small" @change="fetchRankingData">
          <el-option label="进站人数" value="inflow"></el-option>
          <el-option label="出站人数" value="outflow"></el-option>
          <el-option label="总客流量" value="total"></el-option>
        </el-select>
      </div>
    </template>
    <div class="chart-container">
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <div ref="stationRankChart" class="chart"></div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import * as echarts from 'echarts';
import api from '@/api';
// import { registerTheme } from '@/config/echartsTheme'; // 移除

// registerTheme(); // 移除

const emit = defineEmits(['station-click']);

interface StationRankItem {
  station: string;
  value: number;
}

const rankingData = ref<StationRankItem[]>([]);
const flowRankType = ref('total');
const stationRankChart = ref<HTMLElement | null>(null);
const loading = ref(false);
let chartInstance: echarts.ECharts | null = null;

const fixedDate = '2019-05-01'; // 使用真实数据日期

// 获取当前日期 yyyy-MM-dd
const getCurrentDate = () => {
  return fixedDate; // 返回固定日期
};

async function fetchRankingData() {
  loading.value = true;
  try {
    // 根据后端 /api/subway/date 接口改造
    // 注意：后端返回的数据结构可能与原 mock 不同，需要适配
    const data = await api.getDateAmount(getCurrentDate(), flowRankType.value);
    
    // 假设后端返回的 data 格式是: [{ "北京西站": 12345 }, { "国贸": 11111 }]
    // 需要转换成 RankingChart 所需的格式
    if (Array.isArray(data)) {
      rankingData.value = data.map((item: { [key: string]: number }) => {
        const station = Object.keys(item)[0];
        const value = Object.values(item)[0];
        return { station, value };
      }).slice(0, 10); // 仅取前10
    }
    updateChart(); // 获取数据后更新图表
  } catch (error) {
    console.error('获取排名数据失败', error);
  } finally {
    loading.value = false;
  }
};

const updateChart = () => {
  if (!chartInstance) return;
  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      top: '5%',
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01]
    },
    yAxis: {
      type: 'category',
      data: rankingData.value.map(item => item.station).reverse(),
      axisLabel: {
        interval: 0,
      }
    },
    series: [
      {
        name: '客流量',
        type: 'bar',
        data: rankingData.value.map(item => item.value).reverse(),
        barWidth: '60%',
      }
    ]
  });
}

function initChart() {
  if (stationRankChart.value) {
    chartInstance = echarts.init(stationRankChart.value, 'customTheme');
    
    chartInstance.on('click', (params) => {
      if (params.componentType === 'yAxis' || params.componentType === 'series') {
        const dataIndex = params.dataIndex ?? 0;
        const stationData = rankingData.value.slice().reverse()[dataIndex];
        if (stationData) {
          emit('station-click', stationData.station);
        }
      }
    });
  }
}

onMounted(() => {
  nextTick(() => {
    initChart();
    fetchRankingData();
    window.addEventListener('resize', () => chartInstance?.resize());
  });
});

watch(flowRankType, fetchRankingData);

onUnmounted(() => {
  window.removeEventListener('resize', () => chartInstance?.resize());
  chartInstance?.dispose();
});
</script>

<style scoped>
.card-header-with-filter {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-container {
  height: 400px;
  width: 100%;
  position: relative;
  overflow-y: auto;
}
.chart {
  height: 600px;
  width: 100%;
}
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 10;
}
.loading-spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style> 
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import api from '@/api';
import { registerTheme } from '@/config/echartsTheme';

registerTheme();

const emit = defineEmits(['station-click']);

interface RankingData {
  stationId: number | string;
  stationName: string;
  value: number;
}

const rankingData = ref<RankingData[]>([]);
const flowRankType = ref('total');
const stationRankChart = ref<HTMLElement | null>(null);
const loading = ref(false);
let chartInstance: echarts.ECharts | null = null;

const fetchRankingData = async () => {
  loading.value = true;
  try {
    rankingData.value = await api.getStationRanking(flowRankType.value, 10);
    await nextTick();
    if (chartInstance) {
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
          data: rankingData.value.map(item => item.stationName).reverse(),
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
  } catch (error) {
    console.error("Failed to fetch station ranking:", error);
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  if (stationRankChart.value) {
    chartInstance = echarts.init(stationRankChart.value, 'customTheme');
    
    chartInstance.on('click', (params) => {
      // @ts-ignore
      const stationName = params.name;
      const station = rankingData.value.find(s => s.stationName === stationName);
      if (station) {
        emit('station-click', station.stationId.toString());
      }
    });

    await fetchRankingData();
    window.addEventListener('resize', () => chartInstance?.resize());
  }
});

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
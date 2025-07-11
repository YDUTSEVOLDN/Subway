<template>
  <el-card>
    <template #header>
      <div class="clearfix">
        <el-radio-group v-model="timeRange" size="small" @change="fetchTrendData">
          <el-radio-button value="day">日</el-radio-button>
          <el-radio-button value="week">周</el-radio-button>
          <el-radio-button value="month">月</el-radio-button>
        </el-radio-group>
        <span style="margin-left: 20px;">客流趋势</span>
      </div>
    </template>
    <div class="chart-container" style="height: 400px; position: relative;">
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <div ref="timeFlowChart" class="chart"></div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import api from '@/api';
import { registerTheme } from '@/config/echartsTheme';

registerTheme();

interface TrendData {
  labels: string[];
  series: {
    name: string;
    data: number[];
  }[];
}

const timeRange = ref('week');
const timeFlowChart = ref<HTMLElement | null>(null);
const loading = ref(false);
let chartInstance: echarts.ECharts | null = null;

const fetchTrendData = async () => {
  loading.value = true;
  try {
    const rangeMap: { [key: string]: string } = { day: 'last24hours', week: 'last7days', month: 'last30days' };
    const data: TrendData = await api.getSystemTrend(timeRange.value, rangeMap[timeRange.value]);
    await nextTick();
    if (chartInstance) {
      chartInstance.setOption({
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: data.series.map(s => s.name)
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
          data: data.labels
        },
        yAxis: {
          type: 'value'
        },
        series: data.series.map(s => ({
          name: s.name,
          type: 'line',
          stack: '总量',
          areaStyle: {},
          emphasis: {
            focus: 'series'
          },
          data: s.data
        }))
      });
    }
  } catch (error) {
    console.error("Failed to fetch system trend:", error);
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  if (timeFlowChart.value) {
    chartInstance = echarts.init(timeFlowChart.value, 'customTheme');
    await fetchTrendData();
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
  height: 320px;
  width: 100%;
  position: relative;
}
.chart {
  height: 100%;
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
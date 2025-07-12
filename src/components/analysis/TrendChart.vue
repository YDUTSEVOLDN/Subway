<template>
  <div class="trend-chart-container">
    <div class="header">
      <span class="title">客流趋势</span>
      <el-radio-group v-model="timeRange" size="small">
        <el-radio-button label="day">日</el-radio-button>
        <el-radio-button label="week" disabled>周</el-radio-button>
        <el-radio-button label="month" disabled>月</el-radio-button>
      </el-radio-group>
    </div>
    <div ref="trendChart" class="chart-area"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import * as echarts from 'echarts';
import api from '@/api';
// import { registerTheme } from '@/config/echartsTheme'; // 移除

// registerTheme(); // 移除

interface DailyTotal {
  date: string;
  inNum: number;  // 修正：in_num -> inNum
  outNum: number; // 修正：out_num -> outNum
}

const timeRange = ref('day');
const trendData = ref<DailyTotal[]>([]);
const trendChart = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const fetchTrendData = async () => {
  try {
    // 使用新的 getDailyTotals API
    const data = await api.getDailyTotals();
    trendData.value = data;
    updateChart();
  } catch (error) {
    console.error("获取趋势数据失败:", error);
  }
};

const updateChart = () => {
  if (!chartInstance) return;
  
  // 对后端返回的日期字符串进行格式化，只取 yyyy-MM-dd 部分
  const dates = trendData.value.map(item => item.date.substring(0, 10));
  const inData = trendData.value.map(item => item.inNum); // 修正：item.in_num -> item.inNum
  const outData = trendData.value.map(item => item.outNum); // 修正：item.out_num -> item.outNum
  const totalData = trendData.value.map(item => item.inNum + item.outNum); // 修正

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['总客流', '进站', '出站']
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
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '总客流',
        type: 'line',
        data: totalData,
        smooth: true,
      },
      {
        name: '进站',
        type: 'line',
        data: inData,
        smooth: true,
      },
      {
        name: '出站',
        type: 'line',
        data: outData,
        smooth: true,
      }
    ]
  });
};

onMounted(() => {
  nextTick(() => {
    if (trendChart.value) {
      chartInstance = echarts.init(trendChart.value, 'customTheme'); // 修正主题名称
      fetchTrendData();
      window.addEventListener('resize', () => chartInstance?.resize());
    }
  });
});

watch(timeRange, fetchTrendData);

onUnmounted(() => {
  window.removeEventListener('resize', () => chartInstance?.resize());
  chartInstance?.dispose();
});
</script>

<style lang="scss" scoped>
.trend-chart-container {
  background-color: #fff;
  padding: 16px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  height: 400px; // 确保容器有高度
  display: flex;
  flex-direction: column;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .title {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .chart-area {
    flex-grow: 1; // 占据剩余空间
    width: 100%;
  }
}
</style> 
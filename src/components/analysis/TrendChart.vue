<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <div class="title">全系统客流趋势</div>
      </div>
    </template>
    <div class="chart-container" v-loading="loading">
      <div ref="trendChart" class="chart" style="height: 400px;"></div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';
import api from '@/api';
import { registerTheme } from '@/config/echartsTheme';

// 定义后端返回的数据类型
interface DateAmountDto {
  date: string;
  inNum: number;
  outNum: number;
}

registerTheme();

const trendChart = ref<HTMLElement | null>(null);
const loading = ref(true);
let chartInstance: echarts.ECharts | null = null;
const trendData = ref<DateAmountDto[]>([]);

const fetchData = async () => {
  loading.value = true;
  try {
    const result = await api.getSystemTrend();
    // @ts-ignore
    trendData.value = result;
    await nextTick();
    updateChart();
  } catch (error) {
    console.error("获取系统趋势数据失败:", error);
    ElMessage.error("获取系统趋势数据失败");
  } finally {
    loading.value = false;
  }
};

const updateChart = () => {
  if (!chartInstance || trendData.value.length === 0) return;

  const dates = trendData.value.map(item => {
    const d = new Date(item.date);
    return `${d.getMonth() + 1}-${d.getDate()}`;
  });
  const totalFlows = trendData.value.map(item => item.inNum + item.outNum);

  const options: echarts.EChartsOption = {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['总客流']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%', // Increase bottom margin for dataZoom
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: true,
          data: dates
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '总客流',
            type: 'bar',
            data: totalFlows,
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: '#5470c6' // 使用主题色
            },
            emphasis: {
              itemStyle: {
                color: '#3a52a0' // 悬浮时使用更深的蓝色
              }
            },
            markPoint: {
              data: [
                { type: 'max', name: '峰值' },
                { type: 'min', name: '谷值' }
              ]
            },
            markLine: {
              data: [{ type: 'average', name: '平均值' }]
            }
          }
        ],
        dataZoom: [
          {
            type: 'slider',
            start: 0,
            end: 100,
            bottom: 10,
            height: 20,
            showDetail: false,
          }
        ]
  };

  chartInstance.setOption(options);
};

onMounted(() => {
  if (trendChart.value) {
    chartInstance = echarts.init(trendChart.value, 'customTheme');
    fetchData();
    window.addEventListener('resize', () => chartInstance?.resize());
  }
});

onUnmounted(() => {
  if (chartInstance) {
    window.removeEventListener('resize', () => chartInstance?.resize());
    chartInstance.dispose();
  }
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-container {
  position: relative;
  height: 400px;
}
.chart {
  width: 100%;
  height: 100%;
}
</style>
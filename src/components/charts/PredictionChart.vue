<template>
  <div class="prediction-chart-container">
    <div class="chart-controls">
      <el-button 
        v-for="date in availableDates" 
        :key="date" 
        :type="selectedDate === date ? 'primary' : 'default'"
        @click="selectedDate = date"
        size="small"
      >
        {{ getButtonText(date) }}
      </el-button>
    </div>
    <div ref="chartRef" class="prediction-chart"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import * as echarts from 'echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  ToolboxComponent,
} from 'echarts/components';
import type { EChartsOption } from 'echarts';
import { registerTheme } from "@/config/echartsTheme";

// 注册 ECharts 主题
registerTheme();

// 注册 ECharts 组件
use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  ToolboxComponent,
]);

// 定义 props
const props = defineProps<{
  predictionData: { 
    [date: string]: Array<{
      name: string;
      in_num: number;
      out_num: number;
    }>
  } | null;
  stationName: string;
}>();

const chartRef = ref<HTMLDivElement | null>(null);
let myChart: echarts.ECharts | null = null;

const availableDates = computed(() => props.predictionData ? Object.keys(props.predictionData).slice(0, 2) : []);
const selectedDate = ref('');

watch(availableDates, (newDates) => {
  if (newDates.length > 0 && !newDates.includes(selectedDate.value)) {
    selectedDate.value = newDates[0];
  }
}, { immediate: true });

const getButtonText = (date: string) => {
  const index = availableDates.value.indexOf(date);
  if (index === 0) return '后1天';
  if (index === 1) return '后2天';
  return date;
};

const setChartOption = () => {
  if (!props.predictionData || !selectedDate.value || !myChart) {
    myChart?.clear();
    return;
  };

  const currentDayData = (props.predictionData as any)[selectedDate.value];
  if (!currentDayData) return;

  const stationData = currentDayData.filter((d: { name: string }) => d.name === props.stationName);
  
  const inData = stationData.map((d: { in_num: number }) => d.in_num);
  const outData = stationData.map((d: { out_num: number }) => d.out_num);
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`);

  const option: EChartsOption = {
    title: {
      text: `${selectedDate.value} 客流预测`,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['进站人次', '出站人次'],
      bottom: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      containLabel: true,
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: hours,
      },
    ],
    yAxis: [
      {
        type: 'value',
        name: '人次',
        axisLabel: {
          formatter: '{value}'
        }
      },
    ],
    series: [
      {
        name: '进站人次',
        type: 'line',
        smooth: true,
        data: inData,
        itemStyle: { color: '#5470C6' },
      },
      {
        name: '出站人次',
        type: 'line',
        smooth: true,
        data: outData,
        itemStyle: { color: '#91CC75' },
      }
    ],
  };

  myChart.setOption(option, true);
};

onMounted(() => {
  if (chartRef.value) {
    myChart = echarts.init(chartRef.value, 'customTheme');
    setChartOption();
    window.addEventListener('resize', () => {
      myChart?.resize();
    });
  }
});

watch(
  [() => props.predictionData, selectedDate],
  () => {
    if (myChart) {
      setChartOption();
    }
  },
  { deep: true }
);

</script>

<style scoped>
.prediction-chart-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.chart-controls {
  margin-bottom: 10px;
  text-align: center;
}
.prediction-chart {
  width: 100%;
  flex-grow: 1;
  min-height: 250px;
}
</style> 
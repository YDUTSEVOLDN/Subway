<template>
  <div class="heatmap-chart-container" ref="chartContainer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts/core';
import { HeatmapChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  VisualMapComponent,
  LegendComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsCoreOption } from 'echarts/core';

// 注册ECharts组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  VisualMapComponent,
  LegendComponent,
  HeatmapChart,
  CanvasRenderer
]);

// 定义热力图数据接口
interface HeatmapData {
  hours: string[];
  days: string[];
  data: Array<[number, number, number]>; // [hour_index, day_index, value]
}

// 定义组件属性
interface Props {
  heatmapData: HeatmapData;
  title?: string;
  height?: string;
  maxValue?: number;
}

const props = withDefaults(defineProps<Props>(), {
  title: '流量热力图',
  height: '300px',
  maxValue: 0 // 如果为0，则自动计算最大值
});

// 图表容器和实例
const chartContainer = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

// 初始化图表
const initChart = () => {
  if (!chartContainer.value) return;
  
  // 创建图表实例
  chartInstance = echarts.init(chartContainer.value);
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize);
  
  // 更新图表数据
  updateChart();
};

// 更新图表数据
const updateChart = () => {
  if (!chartInstance || !props.heatmapData) return;
  
  const { hours, days, data } = props.heatmapData;
  
  // 计算最大值
  let maxValue = props.maxValue;
  if (maxValue === 0 && data.length > 0) {
    maxValue = Math.max(...data.map(item => item[2]));
  }
  
  // 图表配置
  const option: EChartsCoreOption = {
    title: {
      text: props.title,
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      position: 'top',
      formatter: function (params: any) {
        return `${days[params.data[1]]} ${hours[params.data[0]]}<br>流量: ${params.data[2]}`;
      }
    },
    grid: {
      top: '60px',
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: {
        show: true
      },
      axisLabel: {
        interval: Math.floor(hours.length / 8),
        fontSize: 10
      }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: {
        show: true
      },
      axisLabel: {
        fontSize: 10
      }
    },
    visualMap: {
      min: 0,
      max: maxValue,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0',
      inRange: {
        color: ['#e5f5e0', '#c7e9c0', '#a1d99b', '#74c476', '#41ab5d', '#238b45', '#006d2c']
      }
    },
    series: [{
      name: '客流量',
      type: 'heatmap',
      data: data,
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
  
  // 设置图表配置
  if (chartInstance) {
    chartInstance.setOption(option);
  }
};

// 处理窗口大小变化
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

// 监听数据变化
watch(() => props.heatmapData, () => {
  updateChart();
}, { deep: true });

// 监听高度变化
watch(() => props.height, () => {
  if (chartInstance) {
    setTimeout(() => {
      chartInstance?.resize();
    }, 100);
  }
});

// 组件挂载时初始化图表
onMounted(() => {
  initChart();
});

// 组件卸载时清理资源
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<style scoped lang="scss">
.heatmap-chart-container {
  width: 100%;
  height: v-bind('props.height');
  transition: height 0.3s ease;
}
</style> 
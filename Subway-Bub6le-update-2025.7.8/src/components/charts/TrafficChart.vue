<template>
  <div class="traffic-chart-container">
    <div ref="chartContainer" class="chart-inner" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  LegendComponent
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import type { TrafficData } from '../../types';
import type { TrafficFlowData } from '../../types';

// 注册ECharts组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  LegendComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer,
  BarChart,
  LineChart
]);

// 定义组件属性
interface Props {
  trafficData?: TrafficFlowData | TrafficData[];
  predictionData?: TrafficData[];
  stationName?: string;
  showType?: 'all' | 'inFlow' | 'outFlow';
  height?: string;
}

const props = withDefaults(defineProps<Props>(), {
  stationName: '',
  showType: 'all',
  height: '300px',
  trafficData: () => [],
  predictionData: () => []
});

// 图表元素和实例
const chartContainer = ref<HTMLElement | null>(null);
const chartInstance = ref<echarts.ECharts | null>(null);

// 初始化图表
const initChart = () => {
  if (!chartContainer.value) return;
  
  // 创建图表实例
  chartInstance.value = echarts.init(chartContainer.value);
  
  // 更新图表数据
  updateChart();
  
  // 监听窗口大小变化，自动调整图表大小
  const resizeHandler = () => chartInstance.value?.resize();
  window.addEventListener('resize', resizeHandler);
  
  // 组件卸载时移除监听
  onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeHandler);
    chartInstance.value?.dispose();
  });
};

// 更新图表数据
const updateChart = () => {
  if (!chartInstance.value) return;
  
  // 处理不同类型的trafficData
  let processedTrafficData: TrafficData[] = [];
  
  // 如果是TrafficFlowData类型，转换为TrafficData数组
  if (props.trafficData && 'currentFlow' in props.trafficData) {
    // 从TrafficFlowData中提取数据
    const currentHour = new Date().getHours();
    
    // 添加当前流量
    processedTrafficData.push({
      time: `${currentHour}:00`,
      inFlow: props.trafficData.currentFlow.inflow,
      outFlow: props.trafficData.currentFlow.outflow
    });
    
    // 添加预测数据
    if (props.trafficData.predictions) {
      props.trafficData.predictions.forEach(pred => {
        processedTrafficData.push({
          time: `${pred.hour}:00`,
          inFlow: pred.inflow,
          outFlow: pred.outflow
        });
      });
    }
  } else if (Array.isArray(props.trafficData)) {
    // 如果已经是数组，直接使用
    processedTrafficData = props.trafficData;
  }
  
  // 合并历史数据和预测数据
  const allData = [...processedTrafficData, ...(props.predictionData || [])];
  
  // 提取时间点作为X轴数据
  const times = allData.map(item => item.time);
  const uniqueTimes = Array.from(new Set(times)).sort((a, b) => {
    const hourA = parseInt(a.split(':')[0]);
    const hourB = parseInt(b.split(':')[0]);
    return hourA - hourB;
  });
  
  // 构建入站流量和出站流量数据
  const inFlowData = uniqueTimes.map(time => {
    const item = allData.find(d => d.time === time);
    return item ? item.inFlow : 0;
  });
  
  const outFlowData = uniqueTimes.map(time => {
    const item = allData.find(d => d.time === time);
    return item ? item.outFlow : 0;
  });
  
  // 构建预测数据
  const predictionTimes = props.predictionData.map(item => item.time);
  const predInFlowData = uniqueTimes.map(time => {
    if (predictionTimes.includes(time)) {
      const item = props.predictionData.find(d => d.time === time);
      return item ? item.inFlow : null;
    }
    return null;
  });
  
  const predOutFlowData = uniqueTimes.map(time => {
    if (predictionTimes.includes(time)) {
      const item = props.predictionData.find(d => d.time === time);
      return item ? item.outFlow : null;
    }
    return null;
  });
  
  // 设置图表配置
  const option = {
    title: {
      text: props.stationName ? `${props.stationName} 流量数据` : '流量预测数据',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: function(params: any) {
        const time = params[0].axisValue;
        let result = `<div style="font-weight:bold;margin-bottom:5px;">${time}</div>`;
        
        params.forEach((param: any) => {
          // 添加颜色方块
          const color = param.color;
          const marker = `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background-color:${color};margin-right:5px;"></span>`;
          
          // 添加系列名称和值
          const value = param.value || '无数据';
          const isPrediction = param.seriesName.includes('预测');
          const seriesText = isPrediction ? 
            `<span style="color:#ff9800">${param.seriesName}</span>` : 
            param.seriesName;
          
          result += `${marker}${seriesText}: ${value}<br/>`;
        });
        
        return result;
      }
    },
    legend: {
      data: ['入站人流量', '出站人流量', '入站预测', '出站预测'],
      right: '5%'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: uniqueTimes,
      axisLabel: {
        interval: Math.floor(uniqueTimes.length / 8) // 控制显示的标签数量
      }
    },
    yAxis: {
      type: 'value',
      name: '人流量',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      // 入站流量
      {
        name: '入站人流量',
        type: 'bar',
        data: inFlowData,
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#409EFF'
        },
        showSymbol: false,
        barGap: 0,
        barCategoryGap: '30%',
        animationDuration: 1000
      },
      // 出站流量
      {
        name: '出站人流量',
        type: 'bar',
        data: outFlowData,
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#67C23A'
        },
        showSymbol: false,
        animationDuration: 1000
      },
      // 入站预测
      {
        name: '入站预测',
        type: 'line',
        data: predInFlowData,
        emphasis: {
          focus: 'series'
        },
        lineStyle: {
          color: '#E6A23C',
          width: 2,
          type: 'dashed'
        },
        itemStyle: {
          color: '#E6A23C'
        },
        showSymbol: true,
        symbolSize: 6,
        animationDuration: 1200
      },
      // 出站预测
      {
        name: '出站预测',
        type: 'line',
        data: predOutFlowData,
        emphasis: {
          focus: 'series'
        },
        lineStyle: {
          color: '#F56C6C',
          width: 2,
          type: 'dashed'
        },
        itemStyle: {
          color: '#F56C6C'
        },
        showSymbol: true,
        symbolSize: 6,
        animationDuration: 1200
      }
    ]
  };
  
  // 根据showType参数过滤系列
  if (props.showType !== 'all') {
    const filteredSeries = option.series.filter((series: any) => {
      if (props.showType === 'inFlow') {
        return series.name.includes('入站');
      } else {
        return series.name.includes('出站');
      }
    });
    option.series = filteredSeries;
  }
  
  // 设置图表选项
  chartInstance.value.setOption(option);
};

// 监听数据变化，更新图表
watch(
  () => [props.trafficData, props.predictionData, props.stationName, props.showType],
  () => {
    if (chartInstance.value) {
      updateChart();
    }
  },
  { deep: true }
);

// 组件挂载后初始化图表
onMounted(() => {
  initChart();
});
</script>

<style scoped lang="scss">
.traffic-chart-container {
  width: 100%;
  height: v-bind('props.height');
  border-radius: 4px;
  overflow: hidden;
  
  .chart-inner {
    width: 100%;
    height: 100%;
  }
}
</style> 
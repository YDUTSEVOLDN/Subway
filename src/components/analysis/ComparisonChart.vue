<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>站点客流量对比</span>
        <div class="station-selectors">
          <el-select
            v-model="stationA"
            placeholder="选择站点 A"
            filterable
            clearable
            @change="fetchData"
            style="width: 180px;"
          >
            <el-option
              v-for="station in availableStationsA"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
          <span class="vs-text">vs</span>
          <el-select
            v-model="stationB"
            placeholder="选择站点 B"
            filterable
            clearable
            @change="fetchData"
            style="width: 180px;"
          >
            <el-option
              v-for="station in availableStationsB"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
        </div>
      </div>
    </template>
    <div ref="chartRef" class="chart-container"></div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import * as echarts from 'echarts';
import subwayData from '@/assets/subway_data.json';
import type { Station } from '@/stores/mapStore';
import { registerTheme } from '@/config/echartsTheme';

registerTheme();

const props = defineProps<{
  stations: Station[];
  initialSelectedIds: string[];
}>();

const chartRef = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const stationA = ref('');
const stationB = ref('');

const availableStationsA = computed(() => props.stations.filter(s => s.id !== stationB.value));
const availableStationsB = computed(() => props.stations.filter(s => s.id !== stationA.value));

// 处理数据
function getCompareData() {
  const stationAName = props.stations.find(s => s.id === stationA.value)?.name;
  const stationBName = props.stations.find(s => s.id === stationB.value)?.name;
  if (!stationAName || !stationBName) return { labels: [], series: [] };

  // 按天总量
  const dailyDataA: { [date: string]: number } = {};
  const dailyDataB: { [date: string]: number } = {};
  
  // 遍历所有日期的数据
  Object.entries(subwayData).forEach(([date, dayData]) => {
    const dayArray = dayData as any[];
    const stationAData = dayArray.filter(item => item.name === stationAName);
    const stationBData = dayArray.filter(item => item.name === stationBName);
    
    if (stationAData.length > 0) {
      dailyDataA[date] = stationAData.reduce((sum, item) => sum + (item.in_num || 0) + (item.out_num || 0), 0);
    }
    if (stationBData.length > 0) {
      dailyDataB[date] = stationBData.reduce((sum, item) => sum + (item.in_num || 0) + (item.out_num || 0), 0);
  }
  });
  
  // 取所有日期的并集，排序
  const allDates = Array.from(new Set([...Object.keys(dailyDataA), ...Object.keys(dailyDataB)])).sort();
  
  return {
    labels: allDates.map(d => d.substring(5)), // 只显示月-日
    series: [
      { name: stationAName, type: 'line', smooth: true, data: allDates.map(d => dailyDataA[d] || 0) },
      { name: stationBName, type: 'line', smooth: true, data: allDates.map(d => dailyDataB[d] || 0) },
    ]
  };
}

const fetchData = async () => {
  await nextTick();
  if (!stationA.value || !stationB.value) {
    chartInstance?.clear();
    return;
  }
  chartInstance?.showLoading();
  try {
    const result = getCompareData();
    console.log('ComparisonChart 数据结果:', result);
    if (!result || result.labels.length === 0) {
      console.log('没有数据，清空图表');
      chartInstance?.clear();
      return;
    }
    chartInstance?.setOption({
        tooltip: { trigger: 'axis' },
      legend: { data: result.series.map(s => s.name), top: 'bottom' },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: result.labels },
      yAxis: { type: 'value', name: '总客流量' },
      series: result.series
      }, true);
  } catch (error) {
    console.error('数据处理失败:', error);
    chartInstance?.clear();
  } finally {
    chartInstance?.hideLoading();
  }
};

watch([stationA, stationB], fetchData);

watch(() => props.initialSelectedIds, (newIds) => {
  if (newIds && newIds.length >= 2) {
    stationA.value = newIds[0];
    stationB.value = newIds[1];
    fetchData();
  }
}, { immediate: true });

onMounted(() => {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value, 'customTheme');
    if (props.initialSelectedIds.length >= 2) {
      stationA.value = props.initialSelectedIds[0];
      stationB.value = props.initialSelectedIds[1];
      fetchData();
    }
    window.addEventListener('resize', () => chartInstance?.resize());
  }
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.station-selectors {
  display: flex;
  align-items: center;
  gap: 10px;
}
.vs-text {
  color: #909399;
  font-weight: bold;
}
.chart-container {
  height: 400px;
  width: 100%;
}
</style> 
<template>
  <el-card>
    <template #header>
      <div class="card-header-with-filter">
        <div class="title">站点比较分析</div>
        <div class="filter-group">
          <el-select
            v-model="selectedStationsModel"
            placeholder="输入站点名称进行搜索"
            multiple
            filterable
            remote
            :remote-method="handleRemoteSearch"
            :loading="searchLoading"
            collapse-tags
            collapse-tags-tooltip
            size="small"
            style="width: 350px"
            :max-collapse-tags="3"
          >
            <el-option
              v-for="station in displayOptions"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
          <el-select v-model="compareMetric" placeholder="指标" size="small" @change="fetchComparisonData">
            <el-option label="客流量" value="flow"></el-option>
            <el-option label="单车周转率" value="rotation"></el-option>
            <el-option label="供需比" value="supply"></el-option>
          </el-select>
        </div>
      </div>
    </template>
    <div class="chart-container" style="height: 400px; position: relative;">
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      
      <div v-if="!selectedStationsModel.length && !loading" class="empty-state">
        <el-empty description="请选择站点进行对比">
          <p>您可以点击上方排名图中的站点，或在下拉框中选择</p>
        </el-empty>
      </div>
      
      <div ref="stationCompareChart" class="chart"></div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue';
import * as echarts from 'echarts';
import api from '@/api';
import type { Station } from '@/stores/mapStore';
import { registerTheme } from '@/config/echartsTheme';

registerTheme();

interface ComparisonData {
  labels: string[];
  series: {
    stationName: string;
    data: number[];
  }[];
}

const props = defineProps<{
  stations: Station[];
  selectedStations: string[];
}>();

const emit = defineEmits(['update:selectedStations']);

const selectedStationsModel = computed({
  get: () => props.selectedStations,
  set: (value) => emit('update:selectedStations', value)
});

const filteredStations = ref<Station[]>([]);
const searchLoading = ref(false);

const displayOptions = computed(() => {
  const selected = props.selectedStations
    .map(id => props.stations.find(s => s.id === id))
    .filter((s): s is Station => s !== undefined);

  const combined = [...selected, ...filteredStations.value];
  const uniqueIds = new Set();
  return combined.filter(station => {
    if (!uniqueIds.has(station.id)) {
      uniqueIds.add(station.id);
      return true;
    }
    return false;
  });
});

const handleRemoteSearch = (query: string) => {
  if (query) {
    searchLoading.value = true;
    setTimeout(() => {
      filteredStations.value = props.stations.filter(station =>
        station.name.toLowerCase().includes(query.toLowerCase())
      );
      searchLoading.value = false;
    }, 200); // Simulate network delay
  } else {
    filteredStations.value = [];
  }
};

const compareMetric = ref('flow');
const stationCompareChart = ref<HTMLElement | null>(null);
const loading = ref(false);
let chartInstance: echarts.ECharts | null = null;

const fetchComparisonData = async () => {
  if (selectedStationsModel.value.length === 0) {
    chartInstance?.clear();
    return;
  }
  loading.value = true;
  try {
    const data: ComparisonData = await api.getStationComparison(
      selectedStationsModel.value,
      compareMetric.value,
      'last7days'
    );
    await nextTick();
    if (chartInstance) {
      chartInstance.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: data.series.map(s => s.stationName), top: 'bottom' },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: data.labels },
        yAxis: { type: 'value' },
        series: data.series.map(s => ({
          name: s.stationName,
          type: 'line',
          data: s.data
        }))
      }, true);
    }
  } catch (error) {
    console.error("Failed to fetch station comparison:", error);
  } finally {
    loading.value = false;
  }
};

watch(selectedStationsModel, () => {
  fetchComparisonData();
  // After selection, clear the filtered options to force user to type again
  // for a new search, which feels like the input is cleared.
  filteredStations.value = [];
}, { deep: true });

onMounted(() => {
  filteredStations.value = props.stations.slice(0, 10); // Show some initial options
  if (stationCompareChart.value) {
    chartInstance = echarts.init(stationCompareChart.value, 'customTheme');
    fetchComparisonData();
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
.filter-group {
  display: flex;
  gap: 10px;
}
.chart-container {
  height: 400px;
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
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  text-align: center;
  color: #909399;
}
.empty-state p {
  font-size: 14px;
  margin-top: 8px;
}
</style> 
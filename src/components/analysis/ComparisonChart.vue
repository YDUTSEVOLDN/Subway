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
              :value="station.name"
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
import { ElMessage } from 'element-plus';

interface DailyTotal {
  date: string;
  inNum: number;
  outNum: number;
}

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

const stationsToCompare = ref<string[]>([]);

const fetchComparisonData = async () => {
  if (props.selectedStations.length === 0) {
    updateChart([], []);
    return;
  }
  
  loading.value = true;
  try {
    const stationNames = props.selectedStations; 

    if (stationNames.length === 0) {
      updateChart([], []);
      loading.value = false;
      return;
    }

    const fixedDate = '2019-05-01'; // 使用真实数据日期
    const promises = stationNames.map(name => api.getWeeklyTotals(name, fixedDate));
    const results = await Promise.all(promises);

    if (results.length === 0 || results.some(r => r.length === 0)) {
        updateChart([], []);
        loading.value = false;
        ElMessage.info('部分站点无对比数据');
        return;
    }

    const labels = results[0].map(d => d.date.substring(5, 10)); 

    const series = results.map((stationData, index) => {
      return {
        stationName: stationNames[index],
        data: stationData.map(d => d.inNum + d.outNum)
      };
    });

    updateChart(labels, series);

  } catch (error) {
    console.error("获取对比数据失败:", error);
    ElMessage.error('获取站点对比数据失败');
    updateChart([], []); // 出错时清空图表
  } finally {
    loading.value = false;
  }
};

const updateChart = (labels: string[], series: any[]) => {
  if (chartInstance) {
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: series.map(s => s.stationName), top: 'bottom' },
      grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: labels },
      yAxis: { type: 'value' },
      series: series.map(s => ({
        name: s.stationName,
        type: 'line',
        data: s.data
      }))
    }, true);
  }
};

watch(selectedStationsModel, () => {
  // 移除错误的 if (newVal !== oldVal) 判断
  fetchComparisonData();
}, { deep: true });

onMounted(() => {
  nextTick(() => {
    if (stationCompareChart.value && !chartInstance) {
      chartInstance = echarts.init(stationCompareChart.value, 'customTheme'); // 修正主题名称
      window.addEventListener('resize', () => chartInstance?.resize());
    }
    // 首次加载数据
    fetchComparisonData(); 
  });
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
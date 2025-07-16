<template>
  <el-card>
    <template #header>
      <div class="card-header-with-filter">
        <div class="title">站点客流量排名</div>
        <div class="filter-controls">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            :disabled-date="disabledDates"
            format="MM-DD"
            value-format="YYYY-MM-DD"
            :clearable="false"
            size="small"
            style="width: 120px; margin-right: 10px;"
          />
          <el-radio-group v-model="flowRankType" size="small">
            <el-radio-button label="total">总流量</el-radio-button>
            <el-radio-button label="inflow">进站</el-radio-button>
            <el-radio-button label="outflow">出站</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </template>
    <div class="chart-container" v-loading="loading">
      <div ref="stationRankChart" class="chart" style="height: 400px;"></div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue';
import * as echarts from 'echarts';
import api from '@/api';
import { ElMessage } from 'element-plus';
import { registerTheme } from '@/config/echartsTheme';

registerTheme();

const selectedDate = ref('2025-07-01'); // 默认日期
const flowRankType = ref('total');
const loading = ref(true);
const rawData = ref<any[]>([]);
const stationRankChart = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const disabledDates = (time: Date) => {
  // 更新日期限制为2025年7月
  const start = new Date('2025-07-01');
  const end = new Date('2025-07-23');
  return time.getTime() < start.getTime() || time.getTime() > end.getTime();
};

const fetchData = async () => {
  if (!selectedDate.value) return;
  loading.value = true;
  try {
    const result = await api.getStationRanking(selectedDate.value);
    rawData.value = result;
  } catch (error) {
    console.error('获取站点排名数据失败:', error);
    ElMessage.error('获取站点排名数据失败');
    rawData.value = [];
  } finally {
    loading.value = false;
  }
};

const displayedRankingData = computed(() => {
  if (!rawData.value || rawData.value.length === 0) {
    return [];
  }
  const processedData = rawData.value.map(item => {
    let value = 0;
    if (flowRankType.value === 'inflow') {
      value = item.inNum;
    } else if (flowRankType.value === 'outflow') {
      value = item.outNum;
    } else { // total
      value = item.inNum + item.outNum;
    }
    return {
      name: item.station,
      value: value,
    };
  });
  
  processedData.sort((a, b) => a.value - b.value); 
  
  return processedData.slice(-10); 
});

const updateChart = () => {
  if (!chartInstance) return;

  const options: echarts.EChartsOption = {
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
      data: displayedRankingData.value.map(item => item.name),
          axisLabel: {
            interval: 0,
          }
        },
        series: [
          {
            name: '客流量',
            type: 'bar',
        data: displayedRankingData.value.map(item => item.value),
            barWidth: '60%',
          }
        ]
  };

  chartInstance.setOption(options);
};

onMounted(() => {
  if (stationRankChart.value) {
    chartInstance = echarts.init(stationRankChart.value, 'customTheme');
    window.addEventListener('resize', () => chartInstance?.resize());
    fetchData(); // 初始加载
  }
});

onUnmounted(() => {
  if (chartInstance) {
  window.removeEventListener('resize', () => chartInstance?.resize());
    chartInstance.dispose();
  }
});

// 监听日期变化，重新获取数据
watch(selectedDate, () => {
  fetchData();
});

// 监听计算属性的变化，更新图表
watch(displayedRankingData, () => {
  nextTick(() => {
    updateChart();
  });
}, { deep: true });

</script>

<style scoped>
.card-header-with-filter {
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
.filter-controls {
  display: flex;
  align-items: center;
}
</style> 
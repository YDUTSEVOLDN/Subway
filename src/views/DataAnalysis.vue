<template>
  <div class="data-analysis-page">
    <h2 class="page-title">数据分析总览</h2>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <RankingChart @station-click="handleStationClick" />
      </el-col>
      <el-col :span="12">
        <TrendChart />
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
        <ComparisonChart 
          :stations="stations"
          v-model:selected-stations="comparisonStationIds" 
        />
        </el-col>
      </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import RankingChart from '@/components/analysis/RankingChart.vue';
import TrendChart from '@/components/analysis/TrendChart.vue';
import ComparisonChart from '@/components/analysis/ComparisonChart.vue';
import { useMapStore } from '@/stores/mapStore';
import type { Station } from '@/stores/mapStore';
import { ElMessage } from 'element-plus';

const stations = ref<Station[]>([]);
const comparisonStationIds = ref<string[]>([]);
const mapStore = useMapStore();

const handleStationClick = (stationId: string) => {
  if (!comparisonStationIds.value.includes(stationId)) {
    if (comparisonStationIds.value.length >= 5) {
      ElMessage.warning('最多只能选择 5 个站点进行比较');
      return;
    }
    comparisonStationIds.value.push(stationId);
  }
};

onMounted(async () => {
  if (mapStore.stations.length === 0) {
    await mapStore.loadSubwayData();
  }
  // 使用模拟站点数据进行填充
  stations.value = mapStore.stations.length > 0 ? mapStore.stations : [
    { id: '1', name: '人民广场站', lines:[], entrances:0, position: {lng:0, lat:0}},
    { id: '2', name: '西单站', lines:[], entrances:0, position: {lng:0, lat:0}},
    { id: '3', name: '东直门站', lines:[], entrances:0, position: {lng:0, lat:0}},
    { id: '4', name: '北京南站', lines:[], entrances:0, position: {lng:0, lat:0}},
    { id: '5', name: '国贸站', lines:[], entrances:0, position: {lng:0, lat:0}},
  ];
});
</script>

<style scoped>
.data-analysis-page {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;
}
.page-title {
          font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 24px;
}
</style>

<style>
/* Global Card Style Overrides */
.el-card {
  border: none !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
  transition: all 0.3s ease;
}
.el-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08) !important;
  transform: translateY(-5px);
}
.el-card__header {
  border-bottom: 1px solid #f0f0f0 !important;
  font-weight: 600;
  color: #333;
}
</style> 
<template>
  <div class="data-analysis-page">
    <h2 class="page-title">数据分析总览</h2>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <RankingChart />
      </el-col>
      <el-col :span="12">
        <TrendChart />
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
        <ComparisonChart 
          :stations="stations"
          :initial-selected-ids="comparisonStationIds" 
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

const stations = ref<Station[]>([]);
const comparisonStationIds = ref<string[]>([]);
const mapStore = useMapStore();

onMounted(async () => {
  if (mapStore.stations.length === 0) {
    await mapStore.loadSubwayData();
  }
  stations.value = mapStore.stations;

  // 默认选中前两个站点进行比较
  if (stations.value.length >= 2) {
    comparisonStationIds.value = [stations.value[0].id, stations.value[1].id];
  }
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
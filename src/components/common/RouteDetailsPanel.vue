<template>
  <div class="route-details-panel">
    <el-card shadow="always">
      <template #header>
        <div class="panel-header">
          <span>导航详情</span>
          <el-button text circle @click="$emit('close')">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </template>

      <div v-if="pathData" class="panel-content">
        <div class="summary">
          <div class="summary-item">
            <el-icon><LocationInformation /></el-icon>
            <strong>{{ (pathData.distance || 0).toFixed(2) }}</strong> 公里
          </div>
          <div class="summary-item">
            <el-icon><Timer /></el-icon>
            <strong>{{ pathData.duration || 0 }}</strong> 分钟
          </div>
          <div class="summary-item">
            <el-icon><Guide /></el-icon>
            <strong>{{ pathData.steps?.length || 0 }}</strong> 步骤
          </div>
        </div>

        <el-divider />

        <el-scrollbar class="steps-container">
          <div v-for="(step, index) in pathData.steps" :key="index" class="step-item">
            <div class="step-icon">
              <el-tag type="success" size="small">{{ index + 1 }}</el-tag>
            </div>
            <div class="step-instruction">
              <div v-html="step.instruction"></div>
              <div class="step-sub-info">
                <span>距离: {{ step.distance }} 米</span>
                <span v-if="step.road">道路: {{ step.road }}</span>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
       <el-empty v-else description="暂无路径详情" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue';
import { Close, LocationInformation, Timer, Guide } from '@element-plus/icons-vue';

interface PathData {
  distance?: number;
  duration?: number;
  steps?: any[];
}

interface Props {
  pathData: PathData | null;
}

defineProps<Props>();
defineEmits(['close']);
</script>

<style scoped>
.route-details-panel {
  position: absolute;
  top: 90px;
  right: 20px;
  width: 380px;
  z-index: 1001;
  max-height: calc(100vh - 120px);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}
.panel-content {
  display: flex;
  flex-direction: column;
}
.summary {
  display: flex;
  justify-content: space-around;
  text-align: center;
  padding: 5px 0;
  font-size: 14px;
}
.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}
.summary-item .el-icon {
  font-size: 20px;
  color: #409EFF;
}
.summary-item strong {
  font-size: 18px;
}
.steps-container {
  height: 400px; /* 根据需要调整 */
}
.step-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}
.step-item:last-child {
  border-bottom: none;
}
.step-icon {
  margin-right: 15px;
}
.step-instruction {
  flex-grow: 1;
  font-size: 14px;
}
.step-sub-info {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.step-sub-info span {
  margin-right: 15px;
}
</style> 
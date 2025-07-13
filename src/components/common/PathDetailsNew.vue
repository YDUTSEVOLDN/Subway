<template>
  <div class="path-details-container" v-if="pathData">
    <!-- 摘要信息 -->
    <el-descriptions :column="2" border class="summary-info">
      <el-descriptions-item>
        <template #label><el-icon><LocationInformation /></el-icon> 起点</template>
        {{ pathData.source?.name }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label><el-icon><Flag /></el-icon> 终点</template>
        {{ pathData.target?.name }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label><el-icon><Sort /></el-icon> 总距离</template>
        {{ formatDistance(pathData.distance || 0) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label><el-icon><Timer /></el-icon> 预计时间</template>
        {{ formatDuration(pathData.duration || 0) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="pathData.tolls && pathData.tolls > 0" span="2">
        <template #label><el-icon><Money /></el-icon> 途经费用</template>
        {{ pathData.tolls }} 元
      </el-descriptions-item>
    </el-descriptions>

    <!-- 导航步骤 -->
    <div v-if="pathData.steps && pathData.steps.length > 0" class="navigation-steps">
      <h4 class="section-title">详细步骤</h4>
      <el-scrollbar height="350px">
        <div class="custom-steps-container">
          <step-item
            v-for="(step, index) in pathData.steps"
            :key="`${index}-${step.instruction}`"
            :step="step"
          />
        </div>
      </el-scrollbar>
    </div>
    <div v-else class="no-steps-info">
        <el-empty description="暂无详细步骤信息" />
    </div>

  </div>
  <div v-else class="no-path-data">
    <el-empty description="暂无路径数据" />
  </div>
</template>

<script setup lang="ts">
import {
  LocationInformation,
  Flag,
  Sort,
  Timer,
  Money,
} from '@element-plus/icons-vue';
import type { SavedPathData } from '../../stores/mapStore';
import StepItem from './StepItem.vue';

interface Props {
  pathData?: SavedPathData;
}

const props = defineProps<Props>();

// --- 格式化函数 ---

const formatDistance = (distanceInKm: number): string => {
  if (distanceInKm < 1) {
    return `${(distanceInKm * 1000).toFixed(0)} 米`;
  }
  return `${distanceInKm.toFixed(2)} 公里`;
};

const formatDuration = (durationInMinutes: number): string => {
  if (durationInMinutes < 1) {
      return '小于1分钟';
  }
  if (durationInMinutes < 60) {
    return `${Math.round(durationInMinutes)} 分钟`;
  }
  const hours = Math.floor(durationInMinutes / 60);
  const minutes = Math.round(durationInMinutes % 60);
  if (minutes === 0) {
    return `${hours} 小时`;
  }
  return `${hours} 小时 ${minutes} 分钟`;
};

</script>

<style scoped lang="scss">
.path-details-container, .no-path-data {
  padding: 10px;
}

.summary-info {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #303133;
}

.navigation-steps {
  .custom-steps-container {
    position: relative;
    padding-left: 10px; // 为时间线留出空间

    // 绘制垂直时间线
    &::before {
      content: '';
      position: absolute;
      left: 10px;
      top: 10px;
      bottom: 10px;
      width: 2px;
      background-color: #e4e7ed;
      transform: translateX(-50%);
    }
  }
}

:deep(.el-descriptions__label) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style> 
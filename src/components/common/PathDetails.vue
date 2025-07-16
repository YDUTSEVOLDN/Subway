<template>
  <div class="path-details-container" v-if="route">
    <!-- 移除LLM Generated Summary Section -->

    <!-- Summary Section -->
    <el-descriptions :column="2" border title="路径摘要">
      <el-descriptions-item label="起点">{{ startLocation }}</el-descriptions-item>
      <el-descriptions-item label="终点">{{ endLocation }}</el-descriptions-item>
      <el-descriptions-item label="总距离">{{ (route.distance / 1000).toFixed(2) }} 公里</el-descriptions-item>
      <el-descriptions-item label="预计时间">{{ formatDuration(route.time) }}</el-descriptions-item>
      <el-descriptions-item v-if="vehicleType === 'car' && route.tolls > 0" label="收费">
        {{ route.tolls }} 元
      </el-descriptions-item>
      <el-descriptions-item v-if="vehicleType === 'car'" label="路线策略">
        {{ route.policy }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- Steps Section -->
    <el-timeline class="navigation-steps">
      <el-timeline-item
        v-for="(step, index) in steps"
        :key="index"
        :type="index === 0 ? 'success' : 'primary'"
        :hollow="index > 0"
      >
        <template #icon>
            <ArrowLeft v-if="step.action && step.action.includes('左转')" />
            <ArrowRight v-else-if="step.action && step.action.includes('右转')" />
            <Location v-else-if="index === steps.length - 1" />
            <ArrowUp v-else />
        </template>
        <div class="step-content">
          <p class="step-instruction">{{ step.instruction }}</p>
          <div class="step-info">
            <span class="step-road" v-if="step.road">
              <i class="fas fa-road"></i> {{ step.road }}
            </span>
            <span class="step-distance">
              <i class="fas fa-ruler-horizontal"></i> {{ step.distance }} 米
            </span>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
  <div v-else class="no-data">
    <el-empty description="暂无详细路径数据" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ArrowLeft, ArrowRight, ArrowUp, Location } from '@element-plus/icons-vue';

interface Props {
  pathData?: any; // AMap planning result
  vehicleType?: 'car' | 'bike' | 'walk';
  // 移除 dispatchSummary 属性
}
const props = defineProps<Props>();

const route = computed(() => {
  if (!props.pathData || !props.pathData.routes || props.pathData.routes.length === 0) {
    return null;
  }
  return props.pathData.routes[0];
});

const startLocation = computed(() => {
    if (!route.value) return '未知起点';
    if (route.value.start_location && route.value.start_location.name) {
      return route.value.start_location.name;
    }
    return '起点';
});

const endLocation = computed(() => {
    if (!route.value) return '未知终点';
    if (route.value.end_location && route.value.end_location.name) {
      return route.value.end_location.name;
    }
    return '终点';
});

const steps = computed(() => {
  if (!route.value) return [];
  if (props.vehicleType === 'bike') {
    return route.value.rides || [];
  }
  return route.value.steps || [];
});

const formatDuration = (seconds: number): string => {
  if (!seconds) return '0分钟';
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  let result = '';
  if (hours > 0) result += `${hours} 小时 `;
  if (minutes > 0) result += `${minutes} 分钟`;
  return result.trim() || '小于1分钟';
};

</script>

<style scoped lang="scss">
.path-details-container {
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}
.navigation-steps {
  margin-top: 20px;
}
.step-content {
  .step-instruction {
    font-weight: bold;
    color: #303133;
    margin-bottom: 8px;
  }
  .step-info {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #606266;
  }
  .step-road, .step-distance {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}
.no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.dispatch-summary-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: bold;
    }
  }
  
  .summary-content {
    .summary-text {
      margin-bottom: 16px;
      line-height: 1.6;
    }
    
    .risk-hint {
      margin-top: 10px;
    }
  }
}

.mb-4 {
  margin-bottom: 16px;
}
</style> 
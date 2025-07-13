<template>
  <div class="path-details-container">
    <el-card class="path-info-card">
      <template #header>
        <div class="card-header">
          <span>路径详情</span>
          <el-button type="text" @click="$emit('close')">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </template>
      
      <div v-if="pathData" class="path-content">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border size="small" class="path-basic-info">
          <el-descriptions-item label="起点">{{ pathData.source?.name }}</el-descriptions-item>
          <el-descriptions-item label="终点">{{ pathData.target?.name }}</el-descriptions-item>
          <el-descriptions-item label="总距离">{{ formatDistance(pathData.distance || 0) }}</el-descriptions-item>
          <el-descriptions-item label="预计时间">{{ formatDuration(pathData.duration || 0) }}</el-descriptions-item>
          <el-descriptions-item v-if="pathData.tolls && pathData.tolls > 0" label="收费" :span="2">
            {{ pathData.tolls }} 元
          </el-descriptions-item>
          <el-descriptions-item v-if="pathData.tollDistance && pathData.tollDistance > 0" label="收费距离" :span="2">
            {{ pathData.tollDistance.toFixed(2) }} 公里
          </el-descriptions-item>
          
          <!-- 新增信息 -->
          <el-descriptions-item v-if="pathData.trafficInfo" label="交通状况" :span="2">
            {{ pathData.trafficInfo }}
          </el-descriptions-item>
          <el-descriptions-item v-if="pathData.fuelConsumption && pathData.fuelConsumption > 0" label="预计油耗" :span="2">
            {{ pathData.fuelConsumption.toFixed(2) }} 升
          </el-descriptions-item>
          <el-descriptions-item v-if="pathData.carbonEmission !== undefined" label="碳排放" :span="2">
            {{ pathData.carbonEmission.toFixed(0) }} 克
          </el-descriptions-item>
          <el-descriptions-item v-if="pathData.routeStrategy" label="路线策略" :span="2">
            {{ pathData.routeStrategy }}
          </el-descriptions-item>
          <el-descriptions-item v-if="pathData.alternativeRoutes && pathData.alternativeRoutes > 1" label="可选路线" :span="2">
            {{ pathData.alternativeRoutes }} 条
          </el-descriptions-item>
        </el-descriptions>
        
        <!-- 导航步骤 -->
        <div v-if="pathData.steps && pathData.steps.length > 0" class="navigation-steps">
          <h4>导航步骤</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(step, index) in pathData.steps"
              :key="index"
              :type="getStepType(index)"
              :color="getStepColor(index)"
              :hollow="index > 0"
            >
              <div class="step-content">
                <div class="step-instruction">{{ step.instruction }}</div>
                <div class="step-info">
                  <span class="step-distance">{{ formatDistance(step.distance / 1000) }}</span>
                  <span class="step-duration">{{ formatDuration(step.duration / 60) }}</span>
                </div>
                <!-- 新增步骤详细信息 -->
                <div v-if="step.road || step.action || step.traffic" class="step-details">
                  <span v-if="step.road" class="step-road">{{ step.road }}</span>
                  <span v-if="step.action" class="step-action">{{ step.action }}</span>
                  <span v-if="step.traffic" class="step-traffic">{{ step.traffic }}</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
        
        <!-- 路径点信息 -->
        <div v-if="pathData.path && pathData.path.length > 0" class="path-points">
          <h4>路径点 ({{ pathData.path.length }} 个)</h4>
          <el-scrollbar height="200px">
            <el-timeline>
              <el-timeline-item
                v-for="(point, index) in pathData.path"
                :key="index"
                :type="getPointType(index)"
                :color="getPointColor(index)"
              >
                <div class="point-info">
                  <span class="point-label">
                    {{ getPointLabel(index) }}
                  </span>
                  <span class="point-coordinates">
                    {{ formatCoordinates(point) }}
                  </span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-scrollbar>
        </div>
      </div>
      
      <div v-else class="no-path">
        <el-empty description="暂无路径信息" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Close } from '@element-plus/icons-vue';
import type { Station } from '../../stores/mapStore';

interface PathPoint {
  lng: number;
  lat: number;
}

interface PathStep {
  instruction: string;
  distance: number; // 米
  duration: number; // 秒
  path: PathPoint[];
  road?: string; // 新增
  action?: string; // 新增
  traffic?: string; // 新增
}

interface PathData {
  id?: string;
  name?: string;
  source?: Station;
  target?: Station;
  path?: PathPoint[];
  bikeCount?: number;
  color?: string;
  distance?: number; // 公里
  duration?: number; // 分钟
  steps?: PathStep[];
  tolls?: number;
  tollDistance?: number;
  trafficInfo?: string; // 新增
  fuelConsumption?: number; // 新增
  carbonEmission?: number; // 新增
  routeStrategy?: string; // 新增
  alternativeRoutes?: number; // 新增
}

interface Props {
  pathData?: PathData;
}

const props = defineProps<Props>();
const emit = defineEmits(['close']);

// 格式化距离
const formatDistance = (distance: number): string => {
  if (distance < 1) {
    return `${(distance * 1000).toFixed(0)} 米`;
  }
  return `${distance.toFixed(2)} 公里`;
};

// 格式化时间
const formatDuration = (duration: number): string => {
  if (duration < 60) {
    return `${Math.round(duration)} 分钟`;
  }
  const hours = Math.floor(duration / 60);
  const minutes = Math.round(duration % 60);
  if (minutes === 0) {
    return `${hours} 小时`;
  }
  return `${hours} 小时 ${minutes} 分钟`;
};

// 格式化坐标
const formatCoordinates = (point: PathPoint): string => {
  return `[${point.lng.toFixed(4)}, ${point.lat.toFixed(4)}]`;
};

// 获取步骤类型
const getStepType = (index: number) => {
  if (index === 0) return 'success';
  if (index === (props.pathData?.steps?.length || 0) - 1) return 'success';
  return 'primary';
};

// 获取步骤颜色
const getStepColor = (index: number) => {
  if (index === 0) return '#67C23A';
  if (index === (props.pathData?.steps?.length || 0) - 1) return '#E6A23C';
  return '#409EFF';
};

// 获取点位类型
const getPointType = (index: number) => {
  if (!props.pathData?.path) return 'primary';
  if (index === 0) return 'success';
  if (index === props.pathData.path.length - 1) return 'success';
  return 'primary';
};

// 获取点位颜色
const getPointColor = (index: number) => {
  if (!props.pathData?.path) return '#409EFF';
  if (index === 0) return '#67C23A';
  if (index === props.pathData.path.length - 1) return '#E6A23C';
  return '#409EFF';
};

// 获取点位标签
const getPointLabel = (index: number) => {
  if (!props.pathData?.path) return '';
  if (index === 0) return '起点';
  if (index === props.pathData.path.length - 1) return '终点';
  return `途经点 ${index}`;
};
</script>

<style scoped lang="scss">
.path-details-container {
  .path-info-card {
    width: 100%;
    max-width: 400px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .path-content {
      .path-basic-info {
        margin-bottom: 20px;
      }
      
      .navigation-steps {
        margin-bottom: 20px;
        
        h4 {
          margin-bottom: 12px;
          color: #303133;
        }
        
        .step-content {
          .step-instruction {
            font-weight: 500;
            margin-bottom: 4px;
          }
          
          .step-info {
            display: flex;
            gap: 12px;
            font-size: 12px;
            color: #666;
            margin-bottom: 4px;
            
            .step-distance {
              color: #409EFF;
            }
            
            .step-duration {
              color: #67C23A;
            }
          }
          
          .step-details {
            display: flex;
            gap: 8px;
            font-size: 11px;
            color: #999;
            margin-top: 4px;
            
            .step-road {
              color: #E6A23C;
              background: #fdf6ec;
              padding: 2px 6px;
              border-radius: 3px;
            }
            
            .step-action {
              color: #409EFF;
              background: #ecf5ff;
              padding: 2px 6px;
              border-radius: 3px;
            }
            
            .step-traffic {
              color: #F56C6C;
              background: #fef0f0;
              padding: 2px 6px;
              border-radius: 3px;
            }
          }
        }
      }
      
      .path-points {
        margin-top: 20px;
        
        h4 {
          margin-bottom: 12px;
          color: #303133;
        }
        
        .point-info {
          display: flex;
          justify-content: space-between;
          align-items: center;
          
          .point-label {
            font-weight: 500;
            color: #303133;
          }
          
          .point-coordinates {
            font-size: 12px;
            color: #909399;
            font-family: monospace;
          }
        }
      }
    }
    
    .no-path {
      text-align: center;
      padding: 40px 0;
    }
  }
}
</style> 
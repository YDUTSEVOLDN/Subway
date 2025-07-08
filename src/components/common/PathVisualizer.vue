<template>
  <div class="path-visualizer-container">
    <div ref="pathContainer" class="path-container"></div>
    
    <!-- 路径详情侧边栏 -->
    <el-card v-if="showDetails" class="path-details">
      <template #header>
        <div class="details-header">
          <span>路径详情</span>
          <el-button type="text" @click="showDetails = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </template>
      
      <div v-if="currentPath" class="details-content">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="起点站">{{ currentPath.source?.name || '未知起点' }}</el-descriptions-item>
          <el-descriptions-item label="终点站">{{ currentPath.target?.name || '未知终点' }}</el-descriptions-item>
          <el-descriptions-item label="路径长度">{{ getPathDistance() }} 公里</el-descriptions-item>
          <el-descriptions-item label="预计用时">{{ getPathDuration() }} 分钟</el-descriptions-item>
          <el-descriptions-item label="调度单车数">{{ currentPath.bikeCount || 0 }} 辆</el-descriptions-item>
        </el-descriptions>
        
        <div class="path-waypoints">
          <h4>途经点 ({{ currentPath.path?.length || 0 }} 个)</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(point, index) in currentPath?.path || []" 
              :key="index"
              :type="getPointType(index)"
              :color="getPointColor(index)"
            >
              <p class="waypoint-info">
                {{ index === 0 ? '起点' : (index === (currentPath?.path?.length || 0) - 1 ? '终点' : `途经点 ${index}`) }}
                <span class="coordinates">
                  {{ formatCoordinates(point) }}
                </span>
              </p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { Close } from '@element-plus/icons-vue';
import type { Station } from '../../stores/mapStore';

interface PathPoint {
  lng: number;
  lat: number;
}

interface PathData {
  id?: string;
  name?: string;
  source?: Station;
  target?: Station;
  path?: PathPoint[];
  bikeCount?: number;
  color?: string;
}

// 定义组件属性
interface Props {
  path?: PathData;
  mapInstance?: any;
  autoFit?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits(['path-click']);

// 内部状态
const pathContainer = ref<HTMLElement | null>(null);
const showDetails = ref(false);
const currentPath = ref<PathData | null>(null);
const pathOverlays = ref<any[]>([]);
const pathMarkers = ref<any[]>([]);

// BMap声明
declare global {
  interface Window {
    AMap: any;
  }
}

// 计算当前使用的地图
const map = computed(() => props.mapInstance || null);

// 监听路径数据变化
watch(() => props.path, (newPath) => {
  if (newPath) {
    currentPath.value = newPath;
    drawPath();
  } else {
    clearPath();
    currentPath.value = null;
  }
}, { deep: true });

// 计算路径距离（公里）
const getPathDistance = () => {
  if (!currentPath.value?.path || currentPath.value.path.length < 2) return '0.00';
  
  let totalDistance = 0;
  const path = currentPath.value.path;
  
  for (let i = 1; i < path.length; i++) {
    if (window.AMap) {
      // 使用高德地图的距离计算
      const p1 = [path[i-1].lng, path[i-1].lat];
      const p2 = [path[i].lng, path[i].lat];
      totalDistance += window.AMap.GeometryUtil ? window.AMap.GeometryUtil.distance(p1, p2) : 0;
    } else {
      // 简化的距离计算（经纬度直接相减，不是实际距离）
      const dx = path[i].lng - path[i-1].lng;
      const dy = path[i].lat - path[i-1].lat;
      totalDistance += Math.sqrt(dx * dx + dy * dy) * 111.12; // 大约转换为km
    }
  }
  
  return (totalDistance / 1000).toFixed(2);
};

// 计算路径预计用时（分钟）
const getPathDuration = () => {
  const distance = parseFloat(getPathDistance());
  // 假设平均速度为 20 km/h
  const hours = distance / 20;
  return Math.ceil(hours * 60);
};

// 获取点位类型
const getPointType = (index: number) => {
  if (!currentPath.value?.path) return 'primary';
  if (index === 0) return 'success';
  if (index === currentPath.value.path.length - 1) return 'success';
  return 'primary';
};

// 获取点位颜色
const getPointColor = (index: number) => {
  if (!currentPath.value?.path) return '#409EFF';
  if (index === 0) return '#67C23A';
  if (index === currentPath.value.path.length - 1) return '#E6A23C';
  return '#409EFF';
};

// 格式化坐标
const formatCoordinates = (point: PathPoint) => {
  if (!point) return '';
  return `[${point.lng.toFixed(4)}, ${point.lat.toFixed(4)}]`;
};

// 清除已绘制的路径
const clearPath = () => {
  if (!map.value) return;
  
  // 清除路径覆盖物
  pathOverlays.value.forEach(overlay => {
    map.value.remove(overlay);
  });
  
  // 清除标记点
  pathMarkers.value.forEach(marker => {
    map.value.remove(marker);
  });
  
  pathOverlays.value = [];
  pathMarkers.value = [];
};

// 绘制路径
const drawPath = () => {
  if (!map.value || !currentPath.value?.path || currentPath.value.path.length < 2) return;
  
  // 清除旧路径
  clearPath();
  
  try {
    if (window.AMap) {
      const path = currentPath.value.path;
      const points = path.map(p => new window.AMap.LngLat(p.lng, p.lat));
      
      // 创建折线覆盖物
      const polyline = new window.AMap.Polyline({
        path: points,
        strokeColor: currentPath.value.color || '#409EFF',
        strokeWeight: 5,
        strokeOpacity: 0.8
      });
      
      // 添加路径到地图
      map.value.add(polyline);
      pathOverlays.value.push(polyline);
      
      // 添加起点和终点标记
      const startIcon = new window.AMap.Icon({
        size: new window.AMap.Size(32, 32),
        image: '/start-icon.png', // 需要在public中添加此图标
        imageOffset: new window.AMap.Size(0, 0),
        imageSize: new window.AMap.Size(32, 32),
        anchor: new window.AMap.Size(16, 32)
      });
      
      const endIcon = new window.AMap.Icon({
        size: new window.AMap.Size(32, 32),
        image: '/end-icon.png', // 需要在public中添加此图标
        imageOffset: new window.AMap.Size(0, 0),
        imageSize: new window.AMap.Size(32, 32),
        anchor: new window.AMap.Size(16, 32)
      });
      
      // 起点标记
      const startMarker = new window.AMap.Marker({
        position: points[0],
        icon: startIcon
      });
      map.value.add(startMarker);
      pathMarkers.value.push(startMarker);
      
      // 终点标记
      const endMarker = new window.AMap.Marker({
        position: points[points.length - 1],
        icon: endIcon
      });
      map.value.add(endMarker);
      pathMarkers.value.push(endMarker);
      
      // 自动调整视野
      if (props.autoFit) {
        map.value.setFitView(points);
      }
      
      // 添加点击事件
      polyline.on('click', () => {
        showDetails.value = true;
        emit('path-click', currentPath.value);
      });
    } else {
      console.log('AMap API not available, cannot draw path');
    }
  } catch (error) {
    console.error('绘制路径失败:', error);
  }
};

// 组件挂载
onMounted(() => {
  if (props.path) {
    currentPath.value = props.path;
    drawPath();
  }
});

// 组件卸载
onUnmounted(() => {
  clearPath();
});

// 对外暴露方法
defineExpose({
  showPathDetails: () => { showDetails.value = true; },
  hidePathDetails: () => { showDetails.value = false; },
  togglePathDetails: () => { showDetails.value = !showDetails.value; },
  drawPath,
  clearPath
});
</script>

<style scoped lang="scss">
.path-visualizer-container {
  position: relative;
  width: 100%;
  height: 100%;
  
  .path-container {
    width: 100%;
    height: 100%;
  }
  
  .path-details {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 300px;
    max-height: calc(100% - 20px);
    overflow-y: auto;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    z-index: 1000;
    
    .details-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .details-content {
      font-size: 14px;
    }
    
    .path-waypoints {
      margin-top: 16px;
      
      h4 {
        margin-bottom: 12px;
      }
      
      .waypoint-info {
        margin: 0;
        
        .coordinates {
          font-size: 12px;
          color: #909399;
          margin-left: 8px;
        }
      }
    }
  }
}
</style> 
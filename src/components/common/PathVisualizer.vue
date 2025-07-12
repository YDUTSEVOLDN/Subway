<template>
  <div class="path-visualizer-container">
    <div ref="pathContainer" class="path-container">
    </div>
    
    <!-- 路徑信息浮動面板 -->
    <div v-if="currentPath" class="path-info-panel">
      <el-card class="path-info-card">
        <template #header>
          <div class="info-header">
            <span>路徑信息</span>
          </div>
        </template>
        
        <div class="info-content">
          <div class="info-item">
            <span class="label">起點:</span>
            <span class="value">{{ currentPath.source?.name }}</span>
          </div>
          <div class="info-item">
            <span class="label">終點:</span>
            <span class="value">{{ currentPath.target?.name }}</span>
          </div>
          <div class="info-item">
            <span class="label">距離:</span>
            <span class="value">{{ getPathDistance() }} 公里</span>
          </div>
          <div class="info-item">
            <span class="label">時間:</span>
            <span class="value">{{ getPathDuration() }} 分鐘</span>
          </div>
          <div v-if="currentPath.tolls && currentPath.tolls > 0" class="info-item">
            <span class="label">收費:</span>
            <span class="value">{{ currentPath.tolls }} 元</span>
          </div>
        </div>
      </el-card>
    </div>
    

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { Close } from '@element-plus/icons-vue';

// 定义一个更通用的路径节点类型，而不是强依赖于Station
interface PathNode {
  name: string;
}

interface PathPoint {
  lng: number;
  lat: number;
}

interface PathData {
  id?: string;
  name?: string;
  source?: PathNode; // 使用通用类型
  target?: PathNode; // 使用通用类型
  path?: PathPoint[];
  bikeCount?: number;
  color?: string;
  distance?: number;
  duration?: number;
  steps?: any[];
  tolls?: number;
  tollDistance?: number;
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

// 監聽地圖實例變化
watch(() => props.mapInstance, (newMapInstance) => {
  if (newMapInstance && currentPath.value) {
    // 地圖實例可用且有路徑數據時，重新繪製路徑
    drawPath();
  }
}, { immediate: true });

// 计算路径距离（公里）
const getPathDistance = () => {
  if (!currentPath.value?.distance) return '0.00';
  return currentPath.value.distance.toFixed(2);
};

// 计算路径预计用时（分钟）
const getPathDuration = () => {
  if (!currentPath.value?.duration) return 0;
  return currentPath.value.duration;
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
  if (!map.value || !currentPath.value?.path || currentPath.value.path.length < 2) {
    console.log('無法繪製路徑:', {
      hasMap: !!map.value,
      hasPath: !!currentPath.value?.path,
      pathLength: currentPath.value?.path?.length || 0
    });
    return;
  }
  
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
        strokeWeight: 14, // 进一步加粗
        strokeOpacity: 0.9,
        strokeStyle: 'solid',
        lineJoin: 'round',
        lineCap: 'round'
      });
      
      // 添加路径到地图
      map.value.add(polyline);
      pathOverlays.value.push(polyline);
      
      // 添加方向箭头
      const arrowPolyline = new window.AMap.Polyline({
        path: points,
        strokeColor: '#FF6B35',
        strokeWeight: 3,
        strokeOpacity: 0.6,
        strokeStyle: 'dashed',
        lineJoin: 'round',
        lineCap: 'round'
      });
      
      map.value.add(arrowPolyline);
      pathOverlays.value.push(arrowPolyline);
      
      // 在路徑中間添加方向標記
      if (points.length > 2) {
        const midIndex = Math.floor(points.length / 2);
        const midPoint = points[midIndex];
        
        const directionMarker = new window.AMap.Marker({
          position: midPoint,
          content: '<div style="background: #FF6B35; color: white; padding: 2px 6px; border-radius: 3px; font-size: 10px;">→</div>',
          offset: new window.AMap.Pixel(-10, -10)
        });
        
        map.value.add(directionMarker);
        pathMarkers.value.push(directionMarker);
      }
      
      // 起点标记
      const startMarker = new window.AMap.Marker({
        position: points[0],
        label: {
          content: '起点',
          direction: 'top',
          offset: [0, -10]
        },
        icon: new window.AMap.Icon({
          size: new window.AMap.Size(25, 34),
          image: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png',
          imageSize: new window.AMap.Size(25, 34)
        })
      });
      map.value.add(startMarker);
      pathMarkers.value.push(startMarker);
      
      // 终点标记
      const endMarker = new window.AMap.Marker({
        position: points[points.length - 1],
        label: {
          content: '终点',
          direction: 'top',
          offset: [0, -10]
        },
        icon: new window.AMap.Icon({
          size: new window.AMap.Size(25, 34),
          image: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
          imageSize: new window.AMap.Size(25, 34)
        })
      });
      map.value.add(endMarker);
      pathMarkers.value.push(endMarker);
      
      // 自动调整视野
      if (props.autoFit && points.length > 0) {
        try {
          // 使用 setBounds 而不是 setFitView，並傳遞正確的邊界對象
          const bounds = new window.AMap.Bounds(
            [Math.min(...points.map(p => p.getLng())), Math.min(...points.map(p => p.getLat()))],
            [Math.max(...points.map(p => p.getLng())), Math.max(...points.map(p => p.getLat()))]
          );
          map.value.setBounds(bounds, true, [50, 50, 50, 50]);
        } catch (error) {
          console.warn('自動調整視野失敗，使用備用方案:', error);
          // 備用方案：使用起點和終點
          if (points.length >= 2) {
            const center = [
              (points[0].getLng() + points[points.length - 1].getLng()) / 2,
              (points[0].getLat() + points[points.length - 1].getLat()) / 2
            ];
            map.value.setCenter(center);
            map.value.setZoom(14);
          }
        }
      }
      
      console.log('路徑繪製成功，路徑點數量:', points.length);
      
      // 顯示成功提示
      if (window.AMap && window.AMap.InfoWindow) {
        const infoWindow = new window.AMap.InfoWindow({
          content: `
            <div style="padding: 10px; text-align: center;">
              <h4 style="margin: 0 0 8px 0; color: #67C23A;">✓ 路径规划成功</h4>
              <p style="margin: 0; color: #606266; font-size: 12px;">
                路径已在地图上显示
              </p>
            </div>
          `,
          offset: new window.AMap.Pixel(0, -30),
          closeWhenClickMap: true
        });
        
        // 在路徑中間顯示提示
        if (points.length > 2) {
          const midIndex = Math.floor(points.length / 2);
          infoWindow.open(map.value, points[midIndex]);
          
          // 3秒後自動關閉
          setTimeout(() => {
            infoWindow.close();
          }, 3000);
        }
      }
      

    } else {
      console.log('AMap API not available, cannot draw path');
    }
  } catch (error) {
    console.error('繪製路徑失敗:', error);
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
    position: relative; /* 確保子元素的絕對定位相對於此容器 */
  }



  .path-info-panel {
    position: absolute;
    top: 70px;
    right: 10px;
    z-index: 1000;
    width: 300px;
    max-height: calc(100% - 90px);
    overflow-y: auto;
    box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.2);
    border-radius: 10px;
    backdrop-filter: blur(15px);
    border: 1px solid rgba(255, 255, 255, 0.3);
  }

  .path-info-card {
    .info-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;
      color: #303133;
      border-bottom: 1px solid #f0f0f0;
      padding-bottom: 8px;
    }

    .info-content {
      font-size: 14px;
      padding: 8px 0;
      
      .info-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        padding: 8px 0;
        border-bottom: 1px solid #f5f5f5;
        transition: background-color 0.2s ease;
        
        &:last-child {
          border-bottom: none;
          margin-bottom: 0;
        }
        
        &:hover {
          background-color: #f8f9fa;
          border-radius: 4px;
          padding-left: 8px;
          padding-right: 8px;
        }
        
        .label {
          font-weight: 600;
          color: #606266;
          display: flex;
          align-items: center;
          
          &::before {
            content: '';
            display: inline-block;
            width: 4px;
            height: 4px;
            background-color: #409EFF;
            border-radius: 50%;
            margin-right: 8px;
          }
        }
        
        .value {
          font-weight: 500;
          color: #303133;
          text-align: right;
          max-width: 60%;
          word-break: break-word;
        }
      }
    }
  }
  

}
</style> 
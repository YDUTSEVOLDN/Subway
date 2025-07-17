<template>
  <div class="path-visualizer-container" />
</template>

<script setup lang="ts">
import { ref, onUnmounted, watch, computed } from 'vue';

interface PathNode {
  name: string;
  lng: number;
  lat: number;
}

interface PathData {
  id?: string;
  name?: string;
  source?: PathNode;
  target?: PathNode;
  vehicleType?: 'car' | 'bike' | 'walk'; // 交通方式
  // 其他路径相关属性
}

interface Props {
  path?: PathData;
  mapInstance?: any;
  autoFit?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits(['path-planned']);

declare global {
  interface Window {
    AMap: any;
  }
}

const map = computed(() => props.mapInstance || null);
const pathPlanner = ref<any>(null);

const clearPath = () => {
  if (pathPlanner.value) {
    pathPlanner.value.clear();
    pathPlanner.value = null;
  }
};

const drawPath = (pathData: PathData) => {
  if (!map.value || !pathData.source || !pathData.target || !window.AMap) {
    return;
  }

  clearPath();

  const sourceLngLat = new window.AMap.LngLat(pathData.source.lng, pathData.source.lat);
  const targetLngLat = new window.AMap.LngLat(pathData.target.lng, pathData.target.lat);
  
  let options = {
    map: map.value,
    autoFitView: props.autoFit !== false,
    // 新增：自定义路径渲染样式
    renderOptions: {
        // 配置路线样式
        routeLineStyle: {
            strokeStyle: 'dashed',    // 设置为虚线样式
            strokeColor: '#FFFFFF',   // 虚线的颜色（白色）
            strokeOpacity: 1,
            strokeWeight: 18,         // 较粗的底层线宽
            // 关键：设置虚线样式，[15, 15] 表示画15像素再空15像素
            strokeDasharray: [15, 15] 
        },
        // 在底层再叠加一条纯黑色的实线，形成黑白交替效果
        borderLineStyle: {
            strokeColor: '#000000', // 边框颜色（黑色实线）
            strokeWeight: 20,       // 底层更粗的线宽
            strokeOpacity: 1
        },
        // 可以进一步配置起终点、途经点的样式
        startPointStyle: {
            radius: 12,
            fillColor: '#1087eb',
            lineWidth: 3,
            strokeColor: '#FFFFFF',
        },
        endPointStyle: {
            radius: 12,
            fillColor: '#F74444',
            lineWidth: 3,
            strokeColor: '#FFFFFF',
        }
    }
  };

  let PlannerService: any;
  switch (pathData.vehicleType) {
    case 'bike':
      PlannerService = window.AMap.Riding;
      break;
    case 'walk':
      PlannerService = window.AMap.Walking;
      break;
    case 'car':
    default:
      PlannerService = window.AMap.Driving;
      break;
  }

  pathPlanner.value = new PlannerService(options);
  
  pathPlanner.value.search(sourceLngLat, targetLngLat, (status: string, result: any) => {
    if (status === 'complete') {
      console.log('路径在地图上绘制成功', result);
    } else {
      console.error('地图路径绘制失败:', result);
    }
  });
};

watch(() => props.path, (newPath) => {
  if (newPath && newPath.source && newPath.target) {
    drawPath(newPath);
  } else {
    clearPath();
  }
}, { deep: true, immediate: true });

watch(() => props.mapInstance, (newMap) => {
  if (newMap && props.path && props.path.source && props.path.target) {
    drawPath(props.path);
  }
});

onUnmounted(() => {
  clearPath();
});

// 暴露方法给父组件
defineExpose({
  clearPath,
});

</script>

<style scoped>
.path-visualizer-container {
  /* 容器可以是空的，因为它只负责在地图上绘制 */
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none; /* 允许地图交互 */
}
</style> 
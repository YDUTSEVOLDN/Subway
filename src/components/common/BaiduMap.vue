<template>
  <div class="baidu-map-container" ref="mapContainer">
    <div v-if="!mapLoaded" class="map-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>地图加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useMapStore } from '../../stores/mapStore';

declare global {
  interface Window {
    BMap: any;
    BMapGL: any;
    loadBaiduMapAPI: () => void;
  }
}

// 地图相关属性
const mapContainer = ref<HTMLElement | null>(null);
const mapLoaded = ref(false);
const mapInstance = ref<any>(null);
const mapMarkers = ref<any[]>([]);
const coverageCircle = ref<any | null>(null);
const bikeMarkers = ref<any[]>([]);

const mapStore = useMapStore();

// 初始化地图
const initMap = () => {
  if (!mapContainer.value) {
    console.error('地图容器元素不存在');
    return;
  }
  
  try {
    console.log('开始初始化百度地图...');
    console.log('window.BMap是否存在:', !!window.BMap);
    
    // 检查百度地图API是否已加载
    if (window.BMap) {
      console.log('百度地图API已加载，创建地图实例');
      // 创建地图实例
      const map = new window.BMap.Map(mapContainer.value);
      const point = new window.BMap.Point(116.404, 39.915); // 北京市中心
      map.centerAndZoom(point, 13);
      map.enableScrollWheelZoom();
      map.addControl(new window.BMap.NavigationControl());
      
      // 添加定制样式
      map.setMapStyleV2({
        styleId: '4324bbff0762832be0ae4d213e9f2c29'
      });
      
      console.log('地图实例创建成功');
      mapInstance.value = map;
      mapStore.setMapInstance(map);
      mapLoaded.value = true;
      
      // 添加站点标记
      addStationMarkers();
    } else {
      // 如果API尚未加载，尝试重新加载
      console.warn('百度地图API未加载，尝试加载');
      
      if (typeof window.loadBaiduMapAPI === 'function') {
        window.loadBaiduMapAPI();
        
        // 设置一个计时器，每隔1秒检查一次API是否加载完成
        let checkCount = 0;
        const maxChecks = 10; // 最多检查10次
        
        const checkInterval = setInterval(() => {
          checkCount++;
          console.log(`检查百度地图API是否加载完成 (${checkCount}/${maxChecks})...`);
          
          if (window.BMap) {
            clearInterval(checkInterval);
            console.log('百度地图API加载完成，重新初始化地图');
            initMap();
          } else if (checkCount >= maxChecks) {
            clearInterval(checkInterval);
            console.error('百度地图API加载失败，使用模拟数据');
            useMockMap();
          }
        }, 1000);
      } else {
        console.error('百度地图API加载函数不存在，使用模拟数据');
        useMockMap();
      }
    }
  } catch (error) {
    console.error('初始化地图失败:', error);
    // 失败时使用模拟数据
    useMockMap();
  }
};

// 使用模拟地图（当实际API加载失败时）
const useMockMap = () => {
  console.log('使用模拟地图');
  
  // 创建模拟的地图对象
  const mockMap = {
    addOverlay: (overlay: any) => console.log('添加覆盖物', overlay),
    removeOverlay: (overlay: any) => console.log('移除覆盖物', overlay),
    panTo: (point: any) => console.log('平移地图至', point)
  };
  
  mapInstance.value = mockMap;
  mapStore.setMapInstance(mockMap);
  mapLoaded.value = true;
  
  // 添加站点标记
  setTimeout(() => {
    addStationMarkers();
  }, 500);
};

// 添加地铁站点标记
const addStationMarkers = () => {
  if (!mapInstance.value) return;
  
  // 清除现有标记
  clearMarkers();
  
  const stations = mapStore.getStations;
  if (!stations || stations.length === 0) return;
  
  // 创建新标记
  stations.forEach((station) => {
    try {
      let point: any = null;
      let marker: any = null;
      
      if (window.BMap) {
        point = new window.BMap.Point(station.position.lng, station.position.lat);
        // 创建自定义图标
        const icon = new window.BMap.Icon(
          '/station-icon.svg', // 使用SVG图标
          new window.BMap.Size(32, 32),
          {
            imageOffset: new window.BMap.Size(0, 0),
            anchor: new window.BMap.Size(16, 16)
          }
        );
        
        marker = new window.BMap.Marker(point, { icon });
        
        // 添加信息窗口
        const infoWindow = new window.BMap.InfoWindow(`
          <div class="station-info">
            <h4>${station.name}</h4>
            <p>线路: ${station.lines.join('、')}</p>
            <p>出入口数量: ${station.entrances}</p>
          </div>
        `);
        
        marker.addEventListener('click', function() {
          mapStore.selectStation(station);
          mapInstance.value.panTo(point);
          marker.openInfoWindow(infoWindow);
        });
        
        mapInstance.value.addOverlay(marker);
        mapMarkers.value.push(marker);
      } else {
        // 模拟模式下的简化处理
        console.log(`添加站点标记: ${station.name}`);
      }
    } catch (error) {
      console.error('添加站点标记失败:', error);
    }
  });
};

// 显示站点覆盖圈
const showStationCoverage = () => {
  if (!mapInstance.value || !mapStore.selectedStation) return;
  
  // 移除现有覆盖圈
  if (coverageCircle.value) {
    mapInstance.value.removeOverlay(coverageCircle.value);
    coverageCircle.value = null;
  }
  
  const station = mapStore.selectedStation;
  
  try {
    if (window.BMap) {
      const point: any = new window.BMap.Point(station.position.lng, station.position.lat);
      const circle = new window.BMap.Circle(point, 100, {
        strokeColor: '#409EFF',
        strokeWeight: 2,
        strokeOpacity: 0.6,
        fillColor: '#409EFF',
        fillOpacity: 0.2
      });
      
      mapInstance.value.addOverlay(circle);
      coverageCircle.value = circle;
    } else {
      // 模拟模式
      console.log(`显示站点 ${station.name} 的覆盖圈`);
    }
  } catch (error) {
    console.error('显示站点覆盖圈失败:', error);
  }
};

// 移除站点覆盖圈
const removeStationCoverage = () => {
  if (mapInstance.value && coverageCircle.value) {
    mapInstance.value.removeOverlay(coverageCircle.value);
    coverageCircle.value = null;
  }
};

// 添加共享单车标记
const addBikeMarkers = () => {
  if (!mapInstance.value) return;
  
  // 清除现有单车标记
  clearBikeMarkers();
  
  const bikes = mapStore.bikesData;
  if (!bikes || bikes.length === 0) return;
  
  bikes.forEach((bike) => {
    try {
      if (window.BMap) {
        const point: any = new window.BMap.Point(bike.position.lng, bike.position.lat);
        
        // 创建单车图标
        const icon = new window.BMap.Icon(
          bike.type === 'electric' ? '/bike-electric.svg' : '/bike-regular.svg',
          new window.BMap.Size(24, 24),
          {
            imageOffset: new window.BMap.Size(0, 0),
            anchor: new window.BMap.Size(12, 12)
          }
        );
        
        const marker: any = new window.BMap.Marker(point, { icon });
        
        // 非可用单车半透明显示
        if (!bike.available) {
          marker.setOpacity(0.5);
        }
        
        // 添加点击事件
        marker.addEventListener('click', function() {
          const infoWindow = new window.BMap.InfoWindow(`
            <div class="bike-info">
              <p>ID: ${bike.id}</p>
              <p>类型: ${bike.type === 'electric' ? '电动单车' : '普通单车'}</p>
              <p>电量: ${bike.battery}%</p>
              <p>状态: ${bike.available ? '可用' : '不可用'}</p>
              <p>距地铁站: ${bike.distanceToStation}米</p>
            </div>
          `);
          marker.openInfoWindow(infoWindow);
        });
        
        mapInstance.value.addOverlay(marker);
        bikeMarkers.value.push(marker);
      } else {
        // 模拟模式
        console.log(`添加单车标记: ${bike.id}`);
      }
    } catch (error) {
      console.error('添加单车标记失败:', error);
    }
  });
};

// 清除标记
const clearMarkers = () => {
  if (mapInstance.value) {
    mapMarkers.value.forEach(marker => {
      mapInstance.value.removeOverlay(marker);
    });
    mapMarkers.value = [];
  }
};

// 清除单车标记
const clearBikeMarkers = () => {
  if (mapInstance.value) {
    bikeMarkers.value.forEach(marker => {
      mapInstance.value.removeOverlay(marker);
    });
    bikeMarkers.value = [];
  }
};

// 对外暴露的切换覆盖圈方法
const toggleCoverageCircle = () => {
  if (coverageCircle.value) {
    removeStationCoverage();
  } else {
    showStationCoverage();
  }
};

// 监听选中站点变化
watch(() => mapStore.selectedStation, (station) => {
  if (station && mapStore.showCoverageCircle) {
    showStationCoverage();
  } else {
    removeStationCoverage();
  }
});

// 监听覆盖圈开关状态
watch(() => mapStore.showCoverageCircle, (show) => {
  if (show && mapStore.selectedStation) {
    showStationCoverage();
  } else {
    removeStationCoverage();
  }
});

// 监听共享单车数据变化
watch(() => mapStore.bikesData, () => {
  addBikeMarkers();
}, { deep: true });

// 监听站点数据变化
watch(() => mapStore.getStations, () => {
  addStationMarkers();
}, { deep: true });

// 组件挂载
onMounted(() => {
  // 初始化地图
  initMap();
});

// 组件卸载
onUnmounted(() => {
  clearMarkers();
  clearBikeMarkers();
});

// 向父组件暴露方法
defineExpose({
  toggleCoverageCircle
});
</script>

<style scoped lang="scss">
.baidu-map-container {
  width: 100%;
  height: 100%;
  position: relative;
  background-color: #f5f7fa;
}

.map-loading {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 100;
  
  .loading-icon {
    font-size: 32px;
    color: #409eff;
    margin-bottom: 16px;
    animation: rotating 2s linear infinite;
  }
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

:deep(.station-info) {
  h4 {
    margin-top: 0;
    color: #409eff;
    font-size: 16px;
  }
  
  p {
    margin: 5px 0;
    font-size: 14px;
  }
}

:deep(.bike-info) {
  p {
    margin: 5px 0;
    font-size: 14px;
  }
}
</style> 
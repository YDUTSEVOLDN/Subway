<template>
  <div class="amap-container" ref="mapContainer">
    <div v-if="!mapLoaded" class="map-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>地图加载中...</span>
    </div>
    
    <div class="map-controls" v-if="mapLoaded">
      <el-button-group>
        <el-button size="small" @click="toggleTraffic" type="primary" :plain="!showTraffic">
          <el-icon><Place /></el-icon> 路况
        </el-button>
        <el-button size="small" @click="toggleSatellite" type="primary" :plain="!showSatellite">
          <el-icon><Monitor /></el-icon> 卫星
        </el-button>
      </el-button-group>
    </div>
    
    <div class="search-box" v-if="mapLoaded">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索地点"
        prefix-icon="Search"
        clearable
        @keyup.enter="searchPOI"
      >
        <template #append>
          <el-button @click="searchPOI">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, defineExpose } from 'vue';
import { useMapStore } from '../../stores/mapStore';
import { Loading, Place, Monitor, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { AMapConfig } from '@/config/amap';

declare global {
  interface Window {
    AMap: any;
    _AMapSecurityConfig?: {
      securityJsCode: string;
    };
  }
}

// Refs
const mapContainer = ref<HTMLElement | null>(null);
const mapLoaded = ref(false);
const mapInstance = ref<any>(null);
const mapMarkers = ref<any[]>([]);
const coverageCircle = ref<any | null>(null);
const bikeMarkers = ref<any[]>([]);
const heatmapInstance = ref<any | null>(null);
const subwayLinesOverlay = ref<any[]>([]);
const stationLabels = ref<any[]>([]);
const searchKeyword = ref('');

const mapStore = useMapStore();

// Layer Controls
const showTraffic = ref(false);
const showSatellite = ref(false);
let trafficLayer: any = null;
let satelliteLayer: any = null;

const loadAMapScript = () => {
  return new Promise<void>((resolve, reject) => {
    if (window.AMap) {
      console.log('高德地图API已加载');
      return resolve();
    }
    console.log('开始加载高德地图API...');
    window._AMapSecurityConfig = { securityJsCode: AMapConfig.securityKey };
    console.log('已设置高德地图安全密钥');

    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = `https://webapi.amap.com/maps?v=${AMapConfig.version}&key=${AMapConfig.apiKey}&plugin=${AMapConfig.plugins.join(',')}`;
    
    script.onload = () => {
      console.log('高德地图API主脚本加载成功');
      const uiScript = document.createElement('script');
      uiScript.type = 'text/javascript';
      uiScript.src = `https://webapi.amap.com/ui/1.1/main.js?v=1.1.1`;
      uiScript.onload = () => {
        console.log('高德地图UI库加载成功');
        resolve();
      };
      uiScript.onerror = () => {
        console.error('高德地图UI库加载失败');
        reject(new Error('高德地图UI库加载失败'));
      };
      document.head.appendChild(uiScript);
    };
    script.onerror = () => {
      console.error('高德地图API主脚本加载失败');
      reject(new Error('高德地图API主脚本加载失败'));
    };
    document.head.appendChild(script);
  });
};

const clearStationLabels = () => {
  if (mapInstance.value && stationLabels.value.length > 0) {
    mapInstance.value.remove(stationLabels.value);
    stationLabels.value = [];
  }
};

const clearMarkers = () => {
  if (mapInstance.value && mapMarkers.value.length > 0) {
    mapInstance.value.remove(mapMarkers.value);
    mapMarkers.value = [];
  }
  clearStationLabels();
};

const addStationMarkers = () => {
  if (!mapInstance.value) return;
  clearMarkers();
  const stations = mapStore.getStations;
  if (!stations || stations.length === 0) return;

  stations.forEach((station) => {
        const isInterchange = station.lines.length > 1;
    const markerContent = isInterchange
      ? `<div style="width: 14px; height: 14px; background-color: #ffffff; border: 2px solid #555555; border-radius: 50%; box-shadow: 0 0 5px rgba(0,0,0,0.5);"></div>`
      : `<div style="width: 8px; height: 8px; background-color: #333333; border-radius: 50%;"></div>`;
    const markerOffset = new window.AMap.Pixel(isInterchange ? -9 : -4, isInterchange ? -9 : -4);

        const marker = new window.AMap.Marker({
          position: [station.position.lng, station.position.lat],
          title: station.name,
          content: markerContent,
          offset: markerOffset,
      zIndex: 60,
        });
        
        const infoWindow = new window.AMap.InfoWindow({
      content: `<div class="station-info"><h4>${station.name}</h4><p>线路: ${station.lines.join('、')}</p></div>`,
          offset: new window.AMap.Pixel(0, -32),
      autoMove: false,
        });
    (marker as any).infoWindow = infoWindow;
        
        marker.on('click', () => {
          mapInstance.value.setCenter([station.position.lng, station.position.lat]);
          mapStore.selectStation(station);
      infoWindow.open(mapInstance.value, marker.getPosition());
        });
        
        mapInstance.value.add(marker);
        mapMarkers.value.push(marker);

        const textLabel = new window.AMap.Text({
          text: station.name,
          position: [station.position.lng, station.position.lat],
      offset: new window.AMap.Pixel(18, -8),
          style: {
        'background-color': 'transparent', 'text-align': 'left', 'font-size': '16px',
        'font-weight': '600', 'color': '#2c3e50', 'border': 'none',
        'text-shadow': '1px 1px 0 #fff, -1px -1px 0 #fff, 1px -1px 0 #fff, -1px 1px 0 #fff',
          },
          zIndex: 61,
      zooms: [14, 20],
        });
        mapInstance.value.add(textLabel);
        stationLabels.value.push(textLabel);
  });
};

const clearSubwayLines = () => {
  if (mapInstance.value && subwayLinesOverlay.value.length > 0) {
    mapInstance.value.remove(subwayLinesOverlay.value);
    subwayLinesOverlay.value = [];
  }
};

const drawSubwayLines = () => {
  if (!mapInstance.value) return;
  clearSubwayLines();
  const lines = mapStore.subwayLines;
  if (!lines || lines.length === 0) return;

  const hasHighlights = mapStore.highlightedLines.size > 0;
  lines.forEach(line => {
    if (line.path.length < 2) return;
    const isHighlighted = mapStore.highlightedLines.has(line.name);
    const polyline = new window.AMap.Polyline({
      path: line.path,
      strokeColor: line.color,
      strokeOpacity: hasHighlights ? (isHighlighted ? 1.0 : 0.5) : 0.9,
      strokeWeight: hasHighlights ? (isHighlighted ? 8 : 4) : 6,
      isOutline: true,
      outlineColor: '#FFFFFF',
      borderWeight: 2,
      lineJoin: 'round',
      lineCap: 'round',
      zIndex: hasHighlights ? (isHighlighted ? 51 : 49) : 50,
    });
    mapInstance.value.add(polyline);
    subwayLinesOverlay.value.push(polyline);
  });
};

const drawHeatmap = () => {
  if (!mapInstance.value) return;
  const heatmapData = mapStore.heatmapData;
  if (!heatmapData || heatmapData.length === 0) return;
  if (!heatmapInstance.value) {
    heatmapInstance.value = new window.AMap.HeatMap(mapInstance.value, {
      radius: 120, opacity: [0, 0.85], zIndex: 40,
      '3d': { height: 500, gridSize: 50 },
    });
  }
  heatmapInstance.value.setDataSet({ data: heatmapData, max: 100 });
  heatmapInstance.value.show();
};

const showStationCoverage = () => {
  if (!mapInstance.value || !mapStore.selectedStation) return;
  if (coverageCircle.value) mapInstance.value.remove(coverageCircle.value);
  const station = mapStore.selectedStation;
  coverageCircle.value = new window.AMap.Circle({
        center: [station.position.lng, station.position.lat],
    radius: 100, strokeColor: '#409EFF', strokeWeight: 2,
    strokeOpacity: 0.6, fillColor: '#409EFF', fillOpacity: 0.2,
  });
  mapInstance.value.add(coverageCircle.value);
};

const removeStationCoverage = () => {
  if (mapInstance.value && coverageCircle.value) {
    mapInstance.value.remove(coverageCircle.value);
    coverageCircle.value = null;
  }
};

const clearBikeMarkers = () => {
  if (mapInstance.value && bikeMarkers.value.length > 0) {
    mapInstance.value.remove(bikeMarkers.value);
    bikeMarkers.value = [];
  }
};

const addBikeMarkers = () => {
  if (!mapInstance.value) return;
  clearBikeMarkers();
  const bikes = mapStore.bikesData;
  if (!bikes || bikes.length === 0) return;
  
  bikes.forEach((bike) => {
    const icon = { type: 'image', image: bike.type === 'electric' ? `/bike-electric.svg` : `/bike-regular.svg`, size: [24, 24], anchor: 'center' };
        const marker = new window.AMap.Marker({
          position: [bike.position.lng, bike.position.lat],
      icon: icon, offset: new window.AMap.Pixel(0, 0),
      opacity: bike.available ? 1 : 0.5,
        });
        const infoWindow = new window.AMap.InfoWindow({
      content: `<div class="bike-info"><h4>共享单车 #${bike.id}</h4><p>类型: ${bike.type === 'electric' ? '电动车' : '普通车'}</p><p>电量: ${bike.battery}%</p><p>状态: ${bike.available ? '可用' : '不可用'}</p><p>距离站点: ${bike.distanceToStation}米</p></div>`,
      offset: new window.AMap.Pixel(0, -12),
    });
    marker.on('click', () => infoWindow.open(mapInstance.value, marker.getPosition()));
        mapInstance.value.add(marker);
        bikeMarkers.value.push(marker);
  });
};

const toggleCoverageCircle = () => {
  mapStore.showCoverageCircle ? showStationCoverage() : removeStationCoverage();
};

const searchPOI = () => {
  if (!searchKeyword.value || !mapInstance.value) return;
  const station = mapStore.findStationByName(searchKeyword.value.trim());
  if (station) {
    const marker = mapMarkers.value.find(m => m.getTitle() === station.name);
    if (marker) {
      marker.emit('click');
      mapInstance.value.setZoom(17);
    } else {
      mapInstance.value.setZoomAndCenter(17, [station.position.lng, station.position.lat]);
      ElMessage.info(`已定位到 "${searchKeyword.value.trim()}"`);
    }
  } else {
    ElMessage.warning(`未找到名为 "${searchKeyword.value.trim()}" 的地铁站`);
  }
};

const initMap = async () => {
  if (!mapContainer.value) {
    console.error('地图容器元素不存在');
    return;
  }
  try {
    if (mapStore.getStations.length === 0) await mapStore.loadSubwayData();
    console.log('开始初始化高德地图...');
    const stations = mapStore.getStations;
    const mapCenter = (stations && stations.length > 0)
      ? [stations[0].position.lng, stations[0].position.lat]
      : [116.404, 39.915];
    
    const map = new window.AMap.Map(mapContainer.value, {
      center: mapCenter, zoom: 12, viewMode: '3D',
      mapStyle: 'amap://styles/light', // Use a standard style to avoid 500 error
      features: ['bg', 'point', 'road', 'building'],
      resizeEnable: true, rotateEnable: true, pitchEnable: true,
      pitch: 45, skyColor: '#1E1E1E', buildingAnimation: true,
    });

    window.AMap.plugin(
      ['AMap.ToolBar', 'AMap.Scale', 'AMap.MapType', 'AMap.Geolocation', 'AMap.Traffic'],
      () => {
        // This callback executes only after all plugins are loaded.
        map.addControl(new window.AMap.ToolBar({ position: 'RB' }));
        map.addControl(new window.AMap.Scale());
        map.addControl(new window.AMap.MapType({ defaultType: 0, showRoad: true }));
        map.addControl(new window.AMap.Geolocation({ position: 'LB', showButton: true }));
        
        // It's now safe to instantiate the Traffic layer.
        try {
          trafficLayer = new window.AMap.Traffic({ zIndex: 10 });
          map.add(trafficLayer);
          trafficLayer.hide(); // Hide by default
          mapStore.trafficLayer = trafficLayer;
          console.log('实时路况图层已创建并默认隐藏');
        } catch (e) {
          console.error("创建实时路况图层失败: ", e);
        }
      }
    );

    map.on('complete', () => {
      console.log('高德地图加载完成');
      new window.AMap.TileLayer();
      satelliteLayer = new window.AMap.TileLayer.Satellite();
      satelliteLayer.setMap(null);
      new window.AMap.Buildings({ zooms: [14, 20], zIndex: 10, heightFactor: 2 });
      addStationMarkers();
      drawSubwayLines();
      drawHeatmap();
    });

    console.log('地图实例创建成功');
    mapInstance.value = map;
    mapStore.setMapInstance(map);
    mapLoaded.value = true;
  } catch (error) {
    console.error('初始化地图失败:', error);
    ElMessage.error('初始化地图失败，请检查控制台。');
  }
};

const toggleTraffic = () => {
  if (!mapInstance.value || !trafficLayer) return;
  showTraffic.value = !showTraffic.value;
  showTraffic.value ? trafficLayer.show() : trafficLayer.hide();
};

const toggleSatellite = () => {
  if (!mapInstance.value) return;
  showSatellite.value = !showSatellite.value;
  if (!satelliteLayer) {
    satelliteLayer = new window.AMap.TileLayer.Satellite();
  }
  satelliteLayer.setMap(showSatellite.value ? mapInstance.value : null);
};

watch(() => mapStore.getStations, (newStations) => {
  if (mapLoaded.value && newStations.length > 0) {
    addStationMarkers();
    drawSubwayLines();
    drawHeatmap();
  }
}, { deep: true });

watch(() => mapStore.selectedStation, (station) => {
  if (mapLoaded.value && station) {
    showStationCoverage();
    mapInstance.value.setZoomAndCenter(17, [station.position.lng, station.position.lat], false, 500);
  } else {
    removeStationCoverage();
  }
});

watch(() => mapStore.bikesData, (newBikes) => {
  if (mapLoaded.value && newBikes.length > 0) {
    addBikeMarkers();
  } else {
    clearBikeMarkers();
  }
}, { deep: true });

watch(() => mapStore.showCoverageCircle, () => {
  if (mapLoaded.value) toggleCoverageCircle();
});

watch(() => mapStore.highlightedLines, () => {
  drawSubwayLines();
}, { deep: true });

onMounted(async () => {
          console.log('AMapComponent组件已挂载，初始化地图');
  try {
    await loadAMapScript();
          await initMap();
  } catch (error) {
    console.error('加载高德地图API或初始化地图时出错:', error);
    ElMessage.error('加载高德地图资源失败，请刷新页面重试。');
  }
        });

onUnmounted(() => {
  if (mapInstance.value) {
    mapInstance.value.destroy();
    mapInstance.value = null;
    mapLoaded.value = false;
    mapStore.setMapInstance(null);
    console.log('地图实例已销毁');
  }
});

defineExpose({
  getMapInstance: () => mapInstance.value,
  mapLoaded,
});
</script>

<style scoped>
.amap-container {
  width: 100%;
  height: 100%;
  position: relative;
}
  
  .map-loading {
    display: flex;
    justify-content: center;
    align-items: center;
  height: 100%;
  flex-direction: column;
  color: #909399;
}
    
    .loading-icon {
      font-size: 32px;
  animation: spin 2s linear infinite;
    }

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
  }
  
  .map-controls {
    position: absolute;
    top: 10px;
  right: 10px;
  z-index: 10;
  }
  
  .search-box {
    position: absolute;
    top: 10px;
  left: 10px;
    width: 300px;
  z-index: 10;
}

:deep(.station-info h4) {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #333;
}

:deep(.station-info p) {
  margin: 0;
  font-size: 12px;
  color: #666;
}

:deep(.bike-info h4) {
  margin: 0 0 5px 0;
}

:deep(.bike-info p) {
  margin: 0;
  font-size: 12px;
}
</style> 
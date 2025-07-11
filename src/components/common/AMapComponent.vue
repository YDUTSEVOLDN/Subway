<template>
  <div class="amap-container" ref="mapContainer">
    <div v-if="!mapLoaded" class="map-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>地图加载中...</span>
    </div>

    <!-- 新增：顶部可悬停滑出的工具栏 -->
    <div 
      class="header-controls-container" 
      v-if="mapLoaded"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
      :class="{ 'sidebar-collapsed': layoutStore.isSidebarCollapsed }"
    >
      <div class="controls-bar" :class="{ 'is-visible': showToolbar }">
        <!-- 左侧控件组 -->
        <div class="left-controls">
          <!-- 图层控制 -->
          <div class="layer-control-group">
            <el-radio-group v-model="mapType" size="small" @change="(type: string | number | boolean) => handleMapTypeChange(type as number)">
              <el-radio-button :label="0">标准图</el-radio-button>
              <el-radio-button :label="1">卫星图</el-radio-button>
            </el-radio-group>
            <el-checkbox v-model="showSubwayLines" label="路网" size="small" @change="(visible: string | number | boolean) => handleSubwayLinesChange(visible as boolean)" />
            <el-checkbox v-model="showHeatmap" label="热力图" size="small" @change="(visible: string | number | boolean) => handleHeatmapChange(visible as boolean)" />
            <el-checkbox v-model="showTraffic" label="路况" size="small" class="traffic-checkbox" @change="(visible: string | number | boolean) => handleTrafficChange(visible as boolean)" />
          </div>

          <!-- 日期选择器 -->
          <div class="date-picker-container">
            <el-date-picker
              v-model="mapStore.selectedDate"
              type="date"
              placeholder="选择日期"
              :clearable="false"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              size="large"
              @visible-change="(visible: boolean) => isDatePickerPopperVisible = visible"
            />
          </div>
        </div>
    
        <!-- 右侧控件组 -->
        <div class="right-controls">
          <!-- 搜索框 -->
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索地点"
              prefix-icon="Search"
              clearable
              @keyup.enter="searchPOI"
              size="large"
            >
              <template #append>
                <el-button @click="searchPOI">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          <!-- 新增：路线导航按钮 -->
          <el-button type="primary" circle @click="toggleNavigationPanel">
            <el-icon size="20"><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="toolbar-indicator" v-show="!showToolbar">
        <el-icon><ArrowDown /></el-icon>
      </div>
    </div>

    <!-- 新增：导航面板 -->
    <div class="navigation-panel" :class="{ 'is-visible': navigationPanelVisible }">
      <div class="panel-header">
        <div class="travel-modes">
          <el-button :type="currentTravelMode === 'driving' ? 'primary' : 'default'" circle @click="currentTravelMode = 'driving'"><el-icon size="24"><Van /></el-icon></el-button>
          <el-button :type="currentTravelMode === 'riding' ? 'primary' : 'default'" circle @click="currentTravelMode = 'riding'"><el-icon size="24"><Bicycle /></el-icon></el-button>
          <el-button :type="currentTravelMode === 'walking' ? 'primary' : 'default'" circle @click="currentTravelMode = 'walking'"><el-icon size="24"><Guide /></el-icon></el-button>
        </div>
        <el-button class="close-btn" @click="toggleNavigationPanel" circle>
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="panel-body">
        <div class="route-inputs">
          <el-input placeholder="请输入起点" v-model="routeStart" size="large"></el-input>
          <el-input placeholder="请输入终点" v-model="routeEnd" size="large"></el-input>
        </div>
        <el-button type="primary" class="search-route-btn" @click="searchRoute">开始导航</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, defineExpose } from 'vue';
import { useMapStore } from '../../stores/mapStore';
import { useLayoutStore } from '../../stores/layoutStore';
import { Loading, Search, ArrowDown, Promotion, Close, Van, Bicycle, Guide } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import bikeDataRaw from '../../assets/bike_data.json';

declare global {
  interface Window {
    AMap: any;
    loadAMapAPI: () => void;
  }
}

// 新增：地图元素可见性控制
const STATION_LABEL_VISIBILITY_ZOOM = 14;
const BIKE_MARKER_VISIBILITY_ZOOM = 14;

// 地图相关属性
const mapContainer = ref<HTMLElement | null>(null);
const mapLoaded = ref(false);
const mapInstance = ref<any>(null);
const mapMarkers = ref<any[]>([]);
const coverageCircle = ref<any | null>(null);
const bikeMarkers = ref<any[]>([]);
const heatmapInstance = ref<any | null>(null);
const subwayLinesOverlay = ref<any[]>([]); // 用于存储地铁线路的 Polyline
const stationLabels = ref<any[]>([]); // 用于存储站点名称标签

// 新增: 用于自定义图层切换的状态
const mapType = ref(0); // 0: 标准图, 1: 卫星图
const showTraffic = ref(false);
const standardLayer = ref<any>(null);
const satelliteLayer = ref<any>(null);
const trafficLayer = ref<any>(null);
const roadNetLayer = ref<any>(null); // 新增：用于控制原生路网图层

// 新增: 地图图层显隐状态
const showSubwayLines = ref(true); // 实际控制的是路网
const showHeatmap = ref(true);

// 新增: 用于智能控制工具栏显示的状态
const isToolbarHovered = ref(false); // 重命名，更清晰
const isDatePickerPopperVisible = ref(false);
const showToolbar = computed(() => isToolbarHovered.value || isDatePickerPopperVisible.value);
const hideTimer = ref<number | null>(null);

const handleMouseEnter = () => {
  if (hideTimer.value) {
    clearTimeout(hideTimer.value);
    hideTimer.value = null;
  }
  isToolbarHovered.value = true;
};

const handleMouseLeave = () => {
  hideTimer.value = setTimeout(() => {
    isToolbarHovered.value = false;
  }, 300); // 300ms的延迟，确保有足够时间移动到popper
};


const layoutStore = useLayoutStore();
const mapStore = useMapStore();

// 导航面板相关状态
const navigationPanelVisible = ref(false);
const currentTravelMode = ref('driving'); // driving, riding, walking
const routeStart = ref('');
const routeEnd = ref('');
const driving = ref<any>(null);
const riding = ref<any>(null);
const walking = ref<any>(null);

const toggleNavigationPanel = () => {
  navigationPanelVisible.value = !navigationPanelVisible.value;
};

const searchRoute = () => {
  console.log('开始执行 searchRoute 函数...');
  if (!routeStart.value || !routeEnd.value) {
    ElMessage.warning('请输入起点和终点');
    return;
  }

  console.log(`起点: ${routeStart.value}, 终点: ${routeEnd.value}, 模式: ${currentTravelMode.value}`);

  // 清除旧的路线
  driving.value?.clear();
  riding.value?.clear();
  walking.value?.clear();

  const start = routeStart.value;
  const end = routeEnd.value;

  let searchService;
  let successMsg = '';
  let errorMsg = '';

  switch (currentTravelMode.value) {
    case 'driving':
      searchService = driving.value;
      successMsg = '驾车路线规划成功';
      errorMsg = '驾车路线规划失败，请检查起终点是否有效';
      break;
    case 'riding':
      searchService = riding.value;
      successMsg = '骑行路线规划成功';
      errorMsg = '骑行路线规划失败，请检查起终点是否有效';
      break;
    case 'walking':
      searchService = walking.value;
      successMsg = '步行路线规划成功';
      errorMsg = '步行路线规划失败，请检查起终点是否有效';
      break;
    default:
      console.error('未知的交通模式');
      return;
  }

  console.log('选择的导航服务实例:', searchService);

  if (!searchService) {
    ElMessage.error('导航服务尚未初始化，请稍后再试');
    console.error('导航服务实例为空，无法执行搜索');
    return;
  }

  console.log('调用高德地图 search 方法...');
  searchService.search(start, end, (status: string, result: any) => {
    console.log(`高德地图回调状态: ${status}`);
    console.log('高德地图回调结果:', result);
    if (status === 'complete') {
      ElMessage.success(successMsg);
      // 路线规划成功后自动关闭面板，让用户专注于地图
      navigationPanelVisible.value = false;
    } else {
      ElMessage.error(errorMsg);
      console.error('高德地图路线规划失败，详细信息:', result);
    }
  });
};


// 搜索相关
const searchKeyword = ref('');
let placeSearch: any = null;

// 新增：单车数据和日期选择
const bikeData: Record<string, any[]> = bikeDataRaw;
const availableDates = ref<string[]>(Object.keys(bikeData));
// const selectedDate = ref<string>(''); // 不再使用本地状态


// 新增：处理地图缩放，控制元素显隐
const handleZoomVisibility = () => {
  if (!mapInstance.value) return;
  const currentZoom = mapInstance.value.getZoom();

  // 控制站点名称显隐
  const showLabels = currentZoom >= STATION_LABEL_VISIBILITY_ZOOM;
  stationLabels.value.forEach(label => {
    if (showLabels) {
      label.show();
    } else {
      label.hide();
    }
  });

  // 控制单车标记显隐
  const showBikes = currentZoom >= BIKE_MARKER_VISIBILITY_ZOOM;
  bikeMarkers.value.forEach(marker => {
    if (showBikes) {
      marker.show();
    } else {
      marker.hide();
    }
  });
};


// 搜索POI
const searchPOI = () => {
  if (!searchKeyword.value || !mapInstance.value) return;

  const keyword = searchKeyword.value.trim();
  const station = mapStore.findStationByName(keyword);

  if (station) {
    // 找到了地铁站，直接触发其marker的点击事件
    const marker = mapMarkers.value.find(m => m.getTitle() === station.name);
    if (marker) {
      marker.emit('click');
      // 点击后，再确保缩放级别是我们想要的
      mapInstance.value.setZoom(17);
    } else {
      // 备用方案
      mapInstance.value.setZoomAndCenter(17, [station.position.lng, station.position.lat]);
      ElMessage.info(`已定位到 "${keyword}"`);
    }
  } else {
    // 未找到，给出提示
    ElMessage.warning(`未找到名为 "${keyword}" 的地铁站`);
  }
};

// 绘制单车标记
const updateBikeMarkers = () => {
  if (!mapInstance.value || !mapStore.selectedDate || !bikeData[mapStore.selectedDate]) {
    return;
  }

  // 清除旧的单车标记
  clearBikeMarkers();

  const dailyBikeData = bikeData[mapStore.selectedDate];
  const currentZoom = mapInstance.value.getZoom();
  const showBikes = currentZoom >= BIKE_MARKER_VISIBILITY_ZOOM;

  dailyBikeData.forEach(station => {
    const markerContent = `
      <div class="bike-marker-container">
        <svg viewBox="0 0 512 512" class="bike-icon" xmlns="http://www.w3.org/2000/svg" width="40" height="40"><path d="M336 384h-48a16 16 0 010-32h48a16 16 0 010 32zm-160-64a16 16 0 01-16-16v-48a16 16 0 0132 0v48a16 16 0 01-16 16zm-80 64a80 80 0 1180-80 80.09 80.09 0 01-80 80zm0-128a48 48 0 1048 48 48.05 48.05 0 00-48-48zm272 64a80 80 0 1180-80 80.09 80.09 0 01-80 80zm0-128a48 48 0 1048 48 48.05 48.05 0 00-48-48zm-48 96l-48-96h-21.33l64 128h21.33l-16-32zM176 160h-48v32h-32v-32h-32v32h-32v-48a16 16 0 0116-16h96a16 16 0 0116 16v16a16 16 0 01-16 16z" fill="#2db7f5" data-original="#000000"></path></svg>
        <div class="bike-count">${station.bike_count}</div>
      </div>
    `;

    const marker = new window.AMap.Marker({
      position: station.lnglat,
      content: markerContent,
      offset: new window.AMap.Pixel(-20, -40), // 调整偏移量以使图标底部中心对准坐标点
      title: `${station.name} - ${station.bike_count}辆`
    });

    if (!showBikes) {
      marker.hide();
    }

    mapInstance.value.add(marker);
    bikeMarkers.value.push(marker);
  });
};

// 初始化地图
const initMap = async () => {
  if (!mapContainer.value) {
    console.error('地图容器元素不存在');
    return;
  }
  
  try {
    // 确保在初始化地图前，站点及相关数据已加载
    if (mapStore.getStations.length === 0) {
      await mapStore.loadSubwayData();
    }

    // 设置默认日期 (如果mapStore中还没有)
    if (!mapStore.selectedDate && availableDates.value.length > 0) {
      mapStore.selectedDate = availableDates.value[0];
    }
    
    console.log('开始初始化高德地图...');
    console.log('window.AMap是否存在:', !!window.AMap);
    
    // 获取站点数据，用于设置地图中心
    const stations = mapStore.getStations;
    let mapCenter = [116.404, 39.915]; // 默认北京市中心
    
    // 如果有站点数据，使用第一个站点作为地图中心
    if (stations && stations.length > 0) {
      mapCenter = [stations[0].position.lng, stations[0].position.lat];
      console.log('使用第一个站点作为地图中心:', mapCenter);
    }
    
    // 检查高德地图API是否已加载
    if (window.AMap) {
      console.log('高德地图API已加载，创建地图实例');
      // 创建地图实例
      const map = new window.AMap.Map(mapContainer.value, {
        center: mapCenter, // 使用站点位置或默认位置
        zoom: 12, // 调整缩放级别以适应整体视图
        viewMode: '3D',
        mapStyle: 'amap://styles/blank', // 使用空白地图样式，最大限度简化背景
        features: ['bg', 'point'], // 仅显示背景和兴趣点，隐藏道路等元素
        resizeEnable: true,
        rotateEnable: true,
        pitchEnable: true,
        pitch: 45,
        skyColor: '#1E1E1E',
        buildingAnimation: true // 楼块出现是否带动画
      });
      
      // 添加控件 - 大幅简化
      map.plugin(['AMap.ToolBar', 'AMap.Scale', 'AMap.Geolocation', 'AMap.Driving', 'AMap.Riding', 'AMap.Walking'], () => {
        // 工具条控件 (右下角)
        map.addControl(new window.AMap.ToolBar({
          position: 'RB'
        }));
        
        // 比例尺控件 (左下角)
        map.addControl(new window.AMap.Scale({
          position: { bottom: '20px', left: '20px' }
        }));
        
        // 定位控件 (左下角)
        map.addControl(new window.AMap.Geolocation({
          position: { bottom: '50px', left: '20px' },
          showButton: true
        }));

        // 初始化路线规划插件
        driving.value = new window.AMap.Driving({ map });
        riding.value = new window.AMap.Riding({ map });
        walking.value = new window.AMap.Walking({ map });
      });
      
      // 新增：手动创建图层实例，用于自定义切换
      standardLayer.value = new window.AMap.TileLayer(); // 修改：标准图就是一个瓦片图层
      satelliteLayer.value = new window.AMap.TileLayer.Satellite();
      trafficLayer.value = new window.AMap.TileLayer.Traffic({ zIndex: 10 });
      roadNetLayer.value = new window.AMap.TileLayer.RoadNet(); // 新增：创建路网图层实例
      
      map.add([standardLayer.value, roadNetLayer.value]); // 默认添加标准图和路网
      
      // 监听地图缩放结束事件，以便重新绘制线路
      map.on('zoomend', () => {
        handleZoomVisibility();
      });

      // 加载地图完成后
      map.on('complete', () => {
        console.log('高德地图加载完成');
        
        // 加载标准地图
        new window.AMap.TileLayer();
        
        // 添加3D楼块图层
        new window.AMap.Buildings({
          zooms: [14, 20],
          zIndex: 10,
          heightFactor: 2 // 楼块高度系数
        });
        
        // 添加站点标记和地铁线路
        addStationMarkers();
        drawSubwayLines(); // 绘制地铁线路
        drawHeatmap(); // 绘制热力图

        // 首次加载时也检查一次可见性
        handleZoomVisibility();
      });
      
      console.log('地图实例创建成功');
      mapInstance.value = map;
      mapStore.setMapInstance(map);
      mapLoaded.value = true;
      
      // 添加站点标记
      addStationMarkers();
      
      // 添加地图加载完成事件监听
      map.on('complete', () => {
        console.log('地图加载完成事件触发');
        // 再次添加站点标记和线路，确保显示
        setTimeout(() => {
          addStationMarkers();
          drawSubwayLines(); // 绘制地铁线路
          drawHeatmap(); // 绘制热力图
        }, 500);
      });
    } else {
      // 如果API尚未加载，尝试重新加载
      console.warn('高德地图API未加载，尝试加载');
      
      if (typeof window.loadAMapAPI === 'function') {
        window.loadAMapAPI();
        
        // 设置一个计时器，每隔1秒检查一次API是否加载完成
        let checkCount = 0;
        const maxChecks = 10; // 最多检查10次
        
        const checkInterval = setInterval(() => {
          checkCount++;
          console.log(`检查高德地图API是否加载完成 (${checkCount}/${maxChecks})...`);
          
          if (window.AMap) {
            clearInterval(checkInterval);
            console.log('高德地图API加载完成，重新初始化地图');
            initMap();
          } else if (checkCount >= maxChecks) {
            clearInterval(checkInterval);
            console.error('高德地图API加载失败，使用模拟数据');
            useMockMap();
          }
        }, 1000);
      } else {
        console.error('高德地图API加载函数不存在，使用模拟数据');
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
    add: (overlay: any) => console.log('添加覆盖物', overlay),
    remove: (overlay: any) => console.log('移除覆盖物', overlay),
    setCenter: (point: any) => console.log('平移地图至', point)
  };
  
  mapInstance.value = mockMap;
  mapStore.setMapInstance(mockMap);
  mapLoaded.value = true;
  
  // 添加站点标记
  setTimeout(() => {
    addStationMarkers();
  }, 500);
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

// 添加地铁站点标记
const addStationMarkers = () => {
  if (!mapInstance.value) return;
  
  // 清除现有标记
  clearMarkers();
  
  const stations = mapStore.getStations;
  const currentZoom = mapInstance.value.getZoom();
  const showLabels = currentZoom >= STATION_LABEL_VISIBILITY_ZOOM;

  stations.forEach((station) => {
    try {
      if (window.AMap) {
        const isInterchange = station.lines.length > 1;
        let markerContent, markerOffset;

        if (isInterchange) {
          // 换乘站样式: 白色圆环
          markerContent = `
            <div style="
              width: 14px;
              height: 14px;
              background-color: #ffffff;
              border: 2px solid #555555;
              border-radius: 50%;
              box-shadow: 0 0 5px rgba(0,0,0,0.5);
            "></div>
          `;
          markerOffset = new window.AMap.Pixel(-9, -9);
        } else {
          // 普通站样式: 黑色实心圆点
          markerContent = `
            <div style="
              width: 8px;
              height: 8px;
              background-color: #333333;
              border-radius: 50%;
            "></div>
          `;
          markerOffset = new window.AMap.Pixel(-4, -4);
        }

        const marker = new window.AMap.Marker({
          position: [station.position.lng, station.position.lat],
          title: station.name,
          content: markerContent,
          offset: markerOffset,
          zIndex: 60 // 确保站点标记在线路之上
        });
        
        // 添加信息窗口
        const infoWindow = new window.AMap.InfoWindow({
          content: `
            <div class="station-info">
              <h4>${station.name}</h4>
              <p>线路: ${station.lines.join('、')}</p>
            </div>
          `,
          offset: new window.AMap.Pixel(0, -32),
          autoMove: false // 关键：禁用信息窗口的自动平移，防止与我们的定位代码冲突
        });
        
        // 将 infoWindow 实例附加到 marker 上，方便从外部访问
        marker.infoWindow = infoWindow;
        
        // 点击 marker 时，居中、选中站点、打开窗口
        marker.on('click', () => {
          mapInstance.value.setCenter([station.position.lng, station.position.lat]);
          mapStore.selectStation(station);
          marker.infoWindow.open(mapInstance.value, marker.getPosition());
        });
        
        mapInstance.value.add(marker);
        mapMarkers.value.push(marker);

        // 为站点名称创建文本标签
        const text = new window.AMap.Text({
          text: station.name,
          position: [station.position.lng, station.position.lat],
          offset: new window.AMap.Pixel(18, -8), // 调整文本位置，使其在标记右侧偏上
          style: {
            'background-color': 'transparent',
            'text-align': 'left',
            'font-size': '16px',
            'font-weight': '600',
            'color': '#2c3e50',
            'border': 'none',
            'text-shadow': '1px 1px 0 #fff, -1px -1px 0 #fff, 1px -1px 0 #fff, -1px 1px 0 #fff'
          },
          zIndex: 61,
          zooms: [14, 20] // 设置文本仅在缩放级别 14 到 20 显示
        });

        if (!showLabels) {
          text.hide();
        }

        mapInstance.value.add(text);
        stationLabels.value.push(text);

      } else {
        // 模拟模式
        console.log(`添加站点标记: ${station.name}`);
      }
    } catch (error) {
      console.error('添加站点标记失败:', error);
    }
  });
};

// 清除所有地铁线路
const clearSubwayLines = () => {
  if (mapInstance.value && subwayLinesOverlay.value.length > 0) {
    mapInstance.value.remove(subwayLinesOverlay.value);
    subwayLinesOverlay.value = [];
  }
};

// 绘制地铁线路 - 使用 Polyline 实现平滑曲线和描边效果
const drawSubwayLines = () => {
  if (!mapInstance.value || !window.AMap) return;

  clearSubwayLines(); // 绘制前先清除旧的线路

  const lines = mapStore.subwayLines;
  if (!lines || lines.length === 0) return;

  const hasHighlights = mapStore.highlightedLines.size > 0;

  lines.forEach(line => {
    if (line.path.length < 2) return;

    const isHighlighted = mapStore.highlightedLines.has(line.name);
    let strokeWeight, strokeOpacity, zIndex;

    if (hasHighlights) {
      if (isHighlighted) {
        // 高亮样式
        strokeWeight = 8;
        strokeOpacity = 1.0;
        zIndex = 51;
      } else {
        // 其他线路变暗样式
        strokeWeight = 4;
        strokeOpacity = 0.5;
        zIndex = 49;
      }
    } else {
      // 默认样式
      strokeWeight = 6;
      strokeOpacity = 0.9;
      zIndex = 50;
    }

    const polyline = new window.AMap.Polyline({
      path: line.path,
      strokeColor: line.color,
      strokeOpacity: strokeOpacity,
      strokeWeight: strokeWeight,
      isOutline: true,       // 开启描边
      outlineColor: '#FFFFFF', // 描边颜色
      borderWeight: 2,       // 描边宽度
      lineJoin: 'round',     // 折线连接处样式
      lineCap: 'round',      // 折线两端头样式
      zIndex: zIndex,
    });
    
    mapInstance.value.add(polyline);
    subwayLinesOverlay.value.push(polyline);
  });
};

// 绘制热力图
const drawHeatmap = () => {
  if (!mapInstance.value) return;

  // 模拟热力图数据
  const heatmapData = mapStore.getStations.map(station => ({
    lng: station.position.lng,
    lat: station.position.lat,
    count: Math.floor(Math.pow(Math.random(), 3) * 1000) // 随机生成权重，拉大差异
  }));

  // 如果热力图实例已存在，先移除
  if (heatmapInstance.value) {
    heatmapInstance.value.hide(); // 使用 hide() 而不是 remove()
    heatmapInstance.value = null;
  }

  // 创建热力图实例
  heatmapInstance.value = new window.AMap.HeatMap(mapInstance.value, {
    radius: 120, // 再次增大半径，让热力点可以连接成片
    opacity: [0, 0.85], // 调整透明度使其更平滑
    zIndex: 40, // 层级低于地铁线
    '3d': {
      height: 500, // 增加3D高度，但需要配合视角
      gridSize: 50, // 调整格网大小
    }
  });

  // 设置热力图数据
  heatmapInstance.value.setDataSet({
    data: heatmapData,
    max: 100 // 降低最大值，让数据点显得更“热”，更容易融合
  });

  // 确保热力图是可见的
  heatmapInstance.value.show();
};

// 显示指定站点的覆盖范围（例如，一个圆形区域）
const showStationCoverage = () => {
  if (!mapInstance.value || !mapStore.selectedStation) return;
  
  // 移除旧的覆盖圈
  if (coverageCircle.value) {
    mapInstance.value.remove(coverageCircle.value);
  }
  
  const station = mapStore.selectedStation;
  
  // 创建新的覆盖圈
  coverageCircle.value = new window.AMap.Circle({
        center: [station.position.lng, station.position.lat],
    radius: 100, // 半径，单位：米
    strokeColor: '#409EFF',
    strokeWeight: 2,
    strokeOpacity: 0.6,
    fillColor: '#409EFF',
    fillOpacity: 0.2
  });
  
  mapInstance.value.add(coverageCircle.value);
};

// 移除站点的覆盖范围显示
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


// 新增：处理图层变化的函数
const handleMapTypeChange = (type: number) => {
  if (!mapInstance.value) return;
  if (type === 1) { // 切换到卫星图
    mapInstance.value.remove(standardLayer.value);
    mapInstance.value.add([satelliteLayer.value, roadNetLayer.value]);
  } else { // 切换到标准图
    mapInstance.value.remove([satelliteLayer.value, roadNetLayer.value]);
    mapInstance.value.add(standardLayer.value);
    // 标准图下，重新绘制地铁线路以确保显示
    if (showSubwayLines.value) {
      drawSubwayLines();
    }
  }
};

const handleTrafficChange = (visible: boolean) => {
  if (!mapInstance.value) return;
  if (visible) {
    trafficLayer.value.setMap(mapInstance.value);
  } else {
    trafficLayer.value.setMap(null);
  }
};

const handleSubwayLinesChange = (visible: boolean) => {
  if (visible) {
    drawSubwayLines();
  } else {
    clearSubwayLines();
  }
};

const handleHeatmapChange = (visible: boolean) => {
  if (!heatmapInstance.value) return;
  if (visible) {
    heatmapInstance.value.show();
  } else {
    heatmapInstance.value.hide();
  }
};


// 监听 store 中的变化
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
    // 选中站点时，将地图中心移到该站点，并放大
    mapInstance.value.setZoomAndCenter(17, [station.position.lng, station.position.lat], false, 500);
  } else {
    removeStationCoverage();
  }
});

// 监听日期变化，更新单车数据
watch(() => mapStore.selectedDate, () => {
  if (mapLoaded.value) {
    updateBikeMarkers();
  }
});

watch(() => mapStore.showCoverageCircle, (show) => {
  if (mapLoaded.value) {
    show ? showStationCoverage() : removeStationCoverage();
  }
});

watch(() => mapStore.highlightedLines, () => {
  if (showSubwayLines.value) {
    drawSubwayLines();
  }
}, { deep: true });


onMounted(async () => {
  console.log('AMapComponent组件已挂载，初始化地图');
  try {
    if (window.AMap) {
      console.log('AMap API 已存在，直接初始化');
      await initMap();
    } else {
      console.log('AMap API 不存在，等待全局加载...');
      // 等待由 main.ts 触发的API加载完成
    }
  } catch (error) {
    console.error('初始化地图时出错:', error);
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
  overflow: hidden; /* 防止子元素溢出 */
}

.map-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  flex-direction: column;
  color: #909399;
  background-color: #f4f4f5;
}

.loading-icon {
  font-size: 32px;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.header-controls-container {
  position: absolute;
  top: 0;
  left: 20px;
  right: 20px;
  z-index: 1000;
  transition: left 0.3s ease; /* 新增: 跟随侧边栏变化的动画 */
}

.header-controls-container.sidebar-collapsed {
  left: 20px; /* 侧边栏收起时的位置 */
}

.controls-bar {
  background-color: transparent; /* 修改为透明 */
  backdrop-filter: blur(5px); /* 新增毛玻璃效果 */
  padding: 15px 25px; /* 增加内边距，使其更大 */
  border-radius: 0 0 16px 16px; /* 调整圆角以匹配新尺寸 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transform: translateY(-100%);
  transition: transform 0.4s ease-in-out;
  border: 1px solid #e0e0e0;
  border-top: none;
}

.controls-bar.is-visible {
  transform: translateY(0);
}

.left-controls,
.right-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.layer-control-group {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 6px 10px; /* 增加内边距 */
  border-radius: 8px;
  background-color: #f5f7fa;
}

/* 增大 Checkbox 文字大小 */
.layer-control-group :deep(.el-checkbox__label) {
  font-size: 14px;
}

/* 增大 RadioButton 文字大小 */
.layer-control-group :deep(.el-radio-button__inner) {
  font-size: 14px;
}

.traffic-checkbox {
  margin-right: 0 !important; /* 强制覆盖el-plus的样式 */
}

.date-picker-container :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-radius: 8px;
  height: 38px; /* 增加高度 */
  font-size: 14px; /* 增加字体大小 */
}

.search-box {
  width: 350px; /* 增加宽度 */
}

.search-box :deep(.el-input__wrapper) {
  height: 38px; /* 增加高度 */
  font-size: 14px; /* 增加字体大小 */
}

.toolbar-indicator {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  top: 0;
  width: 120px; /* 再次加宽 */
  height: 32px; /* 再次加高 */
  background-color: rgba(255, 255, 255, 0.9); /* 换回浅白色 */
  border-radius: 0 0 12px 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  color: #606266; /* 图标换回深灰色 */
  cursor: pointer;
  transition: background-color 0.3s;
}

.toolbar-indicator:hover {
  background-color: rgba(255, 255, 255, 1); /* 悬停时不透明 */
}

.toolbar-indicator .el-icon {
  font-size: 24px; /* 再次放大图标 */
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

:deep(.bike-marker-container) {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
}

:deep(.bike-icon) {
  background-color: white;
  border-radius: 50%;
  padding: 4px;
  box-shadow: 0 0 0 2px #2db7f5;
}

:deep(.bike-count) {
  position: absolute;
  top: -8px;
  right: -8px;
  background-color: #f56c6c;
  color: white;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  font-weight: bold;
  border: 2px solid white;
}

.navigation-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 350px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  z-index: 1001;
  transform: translateX(120%);
  transition: transform 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  display: flex;
  flex-direction: column;
}

.navigation-panel.is-visible {
  transform: translateX(0);
}

.panel-header {
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}

.travel-modes {
  display: flex;
  gap: 15px;
}

.panel-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.search-route-btn {
  width: 100%;
}
</style> 
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
            <el-radio-group v-model="mapType" size="small" @change="(type: string | number | boolean | undefined) => handleMapTypeChange(type as number)">
              <el-radio-button :label="0">标准图</el-radio-button>
              <el-radio-button :label="1">卫星图</el-radio-button>
            </el-radio-group>
            <el-checkbox v-model="showSubwayLines" label="路网" size="small" @change="(visible: string | number | boolean | undefined) => handleSubwayLinesChange(visible as boolean)" />
            <el-checkbox v-model="showHeatmap" label="热力图" size="small" @change="(visible: string | number | boolean | undefined) => handleHeatmapChange(visible as boolean)" />
            <el-checkbox v-model="showTraffic" label="路况" size="small" class="traffic-checkbox" @change="(visible: string | number | boolean | undefined) => handleTrafficChange(visible as boolean)" />
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

    <!-- 统一的侧边面板容器，取代了旧的两个独立面板 -->
    <div class="side-panel-container">
      <transition name="slide-down" mode="out-in">
        <!-- 导航输入视图 -->
        <div v-if="panelState === 'input'" class="panel-wrapper" key="input">
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

        <!-- 导航详情视图 -->
        <route-details-panel
          v-else-if="panelState === 'details' && currentNavPath"
          class="panel-wrapper"
          key="details"
          :path-data="currentNavPath"
          @close="panelState = 'hidden'; currentNavPath = null"
        />
      </transition>
    </div>

    <!-- 路径可视化组件保持不变 -->
    <path-visualizer
      v-if="currentNavPath"
      :path="currentNavPath"
      :map-instance="mapInstance"
      :auto-fit="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, defineExpose } from 'vue';
import { useMapStore } from '../../stores/mapStore';
import { useLayoutStore } from '../../stores/layoutStore';
import { Loading, Search, ArrowDown, Promotion, Close, Van, Bicycle, Guide } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import bikeDataRaw from '../../assets/bike_data.json';
import subwayData from '../../assets/subway_data.json';
import PathVisualizer from './PathVisualizer.vue';
import RouteDetailsPanel from './RouteDetailsPanel.vue'; // 新增：导入详情面板
import type { StationBikeStatus } from '@/types';


// 为通用导航功能定义一个路径数据类型
interface PathDataForNav {
  id: string;
  path: { lng: number; lat: number }[];
  distance: number;
  duration: number;
  source: { name: string; lng: number; lat: number };
  target: { name: string; lng: number; lat: number };
  color?: string;
  steps?: any[]; // 新增：用于存储导航步骤
}

declare global {
  interface Window {
    AMap: any;
    loadAMapAPI: () => void;
  }
}

const props = defineProps<{
  stationStatuses?: StationBikeStatus[];
}>();

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
const shortageMarkers = ref<any[]>([]);
let shortageInfoWindow: any = null;
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
const showSubwayLines = ref(false); // 默认不显示路网
const showHeatmap = ref(false); // 默认不显示热力图

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

// 封装绘制逻辑为一个独立函数
function drawShortageMarkers() {
  const newStatuses = props.stationStatuses;
  if (!mapInstance.value || !newStatuses || newStatuses.length === 0) {
    return;
  }

  // 1. 清除旧的短缺标记
  if (shortageMarkers.value.length > 0) {
    mapInstance.value.remove(shortageMarkers.value);
    shortageMarkers.value = [];
  }

  // 2. 遍历新数据，为供不应求的站点创建新标记
  newStatuses.forEach(station => {
    if (station.status === 'shortage') {
      const marker = new window.AMap.Marker({
        position: new window.AMap.LngLat(station.longitude, station.latitude),
        icon: new window.AMap.Icon({
            size: new window.AMap.Size(25, 34),
            image: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-red.png',
            imageSize: new window.AMap.Size(25, 34),
        }),
        zIndex: 120, // 确保在最顶层
        extData: station, // 将站点所有信息存入extData
      });

      // 3. 绑定点击事件，弹出信息窗体
      marker.on('click', (e: any) => {
        const stationData = e.target.getExtData();
        const content = `
          <div style="padding: 10px; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '\\5FAE\\8F6F\\96C5\\9ED1', Arial, sans-serif;">
            <h4 style="margin: 0 0 8px 0; font-size: 16px;">${stationData.name} - <span style="color: #F56C6C; font-weight: bold;">车辆紧缺</span></h4>
            <p style="margin: 5px 0; font-size: 14px;">当前车辆: <strong>${stationData.supply}</strong> 辆</p>
            <p style="margin: 5px 0; font-size: 14px;">预计需求: <strong>${Math.round(stationData.demand)}</strong> 辆</p>
            <p style="margin: 5px 0; font-size: 14px;">车辆缺口: <strong style="color: #F56C6C; font-size: 15px;">${Math.round(stationData.demand - stationData.supply)}</strong> 辆</p>
          </div>
        `;
        
        if (!shortageInfoWindow) {
            shortageInfoWindow = new window.AMap.InfoWindow({
                offset: new window.AMap.Pixel(0, -35),
                closeWhenClickMap: true, // 点击地图其他地方时关闭
            });
        }
        
        shortageInfoWindow.setContent(content);
        shortageInfoWindow.open(mapInstance.value, marker.getPosition());
      });

      shortageMarkers.value.push(marker);
    }
  });

  // 4. 将所有新标记一次性添加到地图上
  if (shortageMarkers.value.length > 0) {
    mapInstance.value.add(shortageMarkers.value);
  }
}


watch(() => props.stationStatuses, () => {
  drawShortageMarkers();
}, { deep: true });


const layoutStore = useLayoutStore();
const mapStore = useMapStore();

// 新增：使用统一的 panelState 替代 navigationPanelVisible
const panelState = ref<'hidden' | 'input' | 'details'>('hidden');

// 导航面板相关状态
const currentTravelMode = ref('driving'); // driving, riding, walking
const routeStart = ref('');
const routeEnd = ref('');
const driving = ref<any>(null);
const riding = ref<any>(null);
const walking = ref<any>(null);
const currentNavPath = ref<PathDataForNav | null>(null);
const geocoder = ref<any>(null);

const toggleNavigationPanel = () => {
  // 如果任何面板是打开的，则关闭它。否则，打开输入面板。
  if (panelState.value === 'input' || panelState.value === 'details') {
    panelState.value = 'hidden';
    // 如果是从详情页关闭，也一并清除路径
    if (currentNavPath.value) {
      currentNavPath.value = null;
    }
  } else {
    panelState.value = 'input';
    currentNavPath.value = null; // 每次打开输入时，都清除旧路径
  }
};

const searchRoute = async () => {
  if (!routeStart.value || !routeEnd.value) {
    ElMessage.warning('请输入起点和终点');
    return;
  }

  currentNavPath.value = null;

  // 使用地理编码将地址转换为坐标
  const geocodeAddress = (address: string): Promise<[number, number]> => {
    return new Promise((resolve, reject) => {
      if (!geocoder.value) {
        reject(new Error('地理编码服务未初始化'));
        return;
      }
      geocoder.value.getLocation(address, (status: string, result: any) => {
        if (status === 'complete' && result.info === 'OK' && result.geocodes.length > 0) {
          const location = result.geocodes[0].location;
          resolve([location.lng, location.lat]);
        } else {
          reject(new Error(`地址解析失败: ${address}`));
        }
      });
    });
  };

  try {
    const [startCoords, endCoords] = await Promise.all([
      geocodeAddress(routeStart.value),
      geocodeAddress(routeEnd.value),
    ]);

    let searchService;
    let successMsg = '';
    let errorMsg = '';

    switch (currentTravelMode.value) {
      case 'driving':
        searchService = driving.value;
        successMsg = '驾车路线规划成功';
        errorMsg = '驾车路线规划失败';
        break;
      case 'riding':
        searchService = riding.value;
        successMsg = '骑行路线规划成功';
        errorMsg = '骑行路线规划失败';
        break;
      case 'walking':
        searchService = walking.value;
        successMsg = '步行路线规划成功';
        errorMsg = '步行路线规划失败';
        break;
      default:
        console.error('未知的交通模式');
        return;
    }

    if (!searchService) {
      ElMessage.error('导航服务尚未初始化，请稍后再试');
      return;
    }

    searchService.search(startCoords, endCoords, (status: string, result: any) => {
      if (status === 'complete' && result.routes && result.routes.length > 0) {
        ElMessage.success(successMsg);
        // 关键改动：切换面板状态以触发动画
        panelState.value = 'details';

        const route = result.routes[0];
        const pathCoords: { lng: number; lat: number }[] = [];
        if (route.steps) {
          route.steps.forEach((step: any) => {
            if (step.path) {
              step.path.forEach((p: any) => {
                if (p && typeof p.lng === 'number' && typeof p.lat === 'number') {
                  pathCoords.push({ lng: p.lng, lat: p.lat });
                }
              });
            }
          });
        }
        
        currentNavPath.value = {
          id: `nav-${Date.now()}`,
          path: pathCoords,
          distance: route.distance / 1000,
          duration: Math.round(route.time / 60),
          source: { name: routeStart.value, lng: startCoords[0], lat: startCoords[1] },
          target: { name: routeEnd.value, lng: endCoords[0], lat: endCoords[1] },
          color: '#4DD0E1',
          steps: route.steps, // 保存详细步骤
        };
        
        // 关键改动：切换面板状态以触发动画
        panelState.value = 'details';

      } else {
        ElMessage.error(errorMsg);
        console.error('高德地图路线规划失败:', result);
      }
    });
  } catch (error) {
    ElMessage.error((error as Error).message);
    console.error('路线规划前置步骤失败:', error);
  }
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
      map.plugin(['AMap.ToolBar', 'AMap.Scale', 'AMap.Geolocation', 'AMap.Driving', 'AMap.Riding', 'AMap.Walking', 'AMap.Geocoder'], () => {
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

        // 新增：初始化地理编码插件
        geocoder.value = new window.AMap.Geocoder({
          city: '全国'
        });

        // 初始化路线规划插件，但不与地图实例绑定 (map: null)
        driving.value = new window.AMap.Driving({ map: null, policy: window.AMap.DrivingPolicy.LEAST_TIME });
        riding.value = new window.AMap.Riding({ map: null });
        walking.value = new window.AMap.Walking({ map: null });
      });
      
      // 新增：手动创建图层实例，用于自定义切换
      standardLayer.value = new window.AMap.TileLayer(); // 修改：标准图就是一个瓦片图层
      satelliteLayer.value = new window.AMap.TileLayer.Satellite();
      trafficLayer.value = new window.AMap.TileLayer.Traffic({ zIndex: 10 });
      roadNetLayer.value = new window.AMap.TileLayer.RoadNet(); // 新增：创建路网图层实例
      
      // 默认只添加标准图层，不添加路网
      map.add([standardLayer.value]);
      
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
        // 只有在用户选择显示路网时才绘制
        if (showSubwayLines.value) {
          drawSubwayLines();
        }
        // 只有在用户选择显示热力图时才绘制
        if (showHeatmap.value) {
          drawHeatmap();
        }

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
          // 只有在用户选择显示路网时才绘制
          if (showSubwayLines.value) {
            drawSubwayLines();
          }
          // 只有在用户选择显示热力图时才绘制
          if (showHeatmap.value) {
            drawHeatmap();
          }
          drawShortageMarkers(); // 当地图加载完成后，也尝试绘制一次紧缺标记
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

  const date = mapStore.selectedDate;
  if (!date) {
    console.warn("未选择日期，无法生成热力图");
    if (heatmapInstance.value) heatmapInstance.value.hide();
    return;
  }

  const dailyData = (subwayData as Record<string, any[]>)[date] || [];
  if (dailyData.length === 0) {
    console.warn(`在 ${date} 未找到流量数据，无法生成热力图`);
    if (heatmapInstance.value) heatmapInstance.value.hide();
    return;
  }

  // 1. 计算每个站点的总人流量
  const stationTraffic: Record<string, number> = {};
  dailyData.forEach(item => {
    const totalFlow = item.in_num + item.out_num;
    stationTraffic[item.name] = (stationTraffic[item.name] || 0) + totalFlow;
  });

  // 2. 生成热力图数据
  const heatmapData = mapStore.getStations.map(station => ({
    lng: station.position.lng,
    lat: station.position.lat,
    count: stationTraffic[station.name] || 0 // 使用真实数据，如果某站没数据则为0
  }));

  // 3. 计算最大值用于渲染
  const maxTraffic = Math.max(...Object.values(stationTraffic).filter(v => v > 0));


  // 如果热力图实例已存在，先隐藏
  if (heatmapInstance.value) {
    heatmapInstance.value.hide();
  }

  // 创建或更新热力图实例
  if (!heatmapInstance.value) {
    heatmapInstance.value = new window.AMap.HeatMap(mapInstance.value, {
      radius: 120,
      opacity: [0, 0.85],
      zIndex: 40,
      '3d': {
        height: 500,
        gridSize: 50,
      }
    });
  }

  // 设置热力图数据
  heatmapInstance.value.setDataSet({
    data: heatmapData,
    max: maxTraffic // 使用真实数据中的最大值
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

// 处理地铁线路显示/隐藏
const handleSubwayLinesChange = (visible: boolean) => {
  if (visible) {
    // 添加路网图层
    if (roadNetLayer.value && mapInstance.value) {
      mapInstance.value.add(roadNetLayer.value);
    }
    // 绘制地铁线路
    drawSubwayLines();
  } else {
    // 清除地铁线路
    clearSubwayLines();
    // 移除路网图层
    if (roadNetLayer.value && mapInstance.value) {
      mapInstance.value.remove(roadNetLayer.value);
    }
  }
};

const handleHeatmapChange = (visible: boolean) => {
  console.log('热力图显示状态变更:', visible);
  
  if (visible) {
    // 如果热力图实例不存在，先创建
    if (!heatmapInstance.value && mapInstance.value) {
      console.log('创建热力图实例');
      // 绘制热力图
      drawHeatmap();
    } else if (heatmapInstance.value) {
      console.log('显示已有热力图');
      heatmapInstance.value.show();
    }
  } else {
    if (heatmapInstance.value) {
      console.log('隐藏热力图');
      heatmapInstance.value.hide();
    }
  }
};


// 监听 store 中的变化
watch(() => mapStore.getStations, (newStations) => {
  if (mapLoaded.value && newStations.length > 0) {
    addStationMarkers();
    if (showSubwayLines.value) {
      drawSubwayLines();
    }
    if (showHeatmap.value) {
      drawHeatmap();
    }
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
    // 只有在用户选择显示热力图时才更新
    if (showHeatmap.value) {
      drawHeatmap();
    }
  }
});

watch(() => mapStore.showCoverageCircle, (show) => {
  if (mapLoaded.value) {
    show ? showStationCoverage() : removeStationCoverage();
  }
});

watch(() => mapStore.highlightedLines, () => {
  // 只有在用户选择显示路网时才更新
  if (mapLoaded.value && showSubwayLines.value) {
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
  resizeMap: () => {
    if (mapInstance.value) {
      // 延迟执行resize以确保DOM已完全更新
      setTimeout(() => {
        mapInstance.value.resize();
        // 移除下面的代码，不再自动调整视图
        // if (mapMarkers.value.length > 0) {
        //   mapInstance.value.setFitView(mapMarkers.value);
        // }
      }, 100);
    }
  }
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

/* 为 RouteDetailsPanel 添加的动画 */
.slide-fade-enter-active {
  transition: all 0.4s ease-out;
}
.slide-fade-leave-active {
  transition: all 0.4s cubic-bezier(1, 0.5, 0.8, 1);
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateX(20px);
  opacity: 0;
}

/* 新增：侧边面板容器样式 */
.side-panel-container {
  position: absolute;
  top: 90px;
  right: 20px;
  width: 380px;
  z-index: 1001;
}

/* 新增：包裹器，用于统一添加背景和阴影 */
.panel-wrapper {
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
  overflow: hidden;
}

/* 新增：面板切换动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease-in-out;
}
.slide-down-enter-from {
  transform: translateY(-30px);
  opacity: 0;
}
.slide-down-leave-to {
  transform: translateY(30px);
  opacity: 0;
}
</style> 
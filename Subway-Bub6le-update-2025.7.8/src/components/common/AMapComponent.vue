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

declare global {
  interface Window {
    AMap: any;
    loadAMapAPI: () => void;
  }
}

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

const mapStore = useMapStore();

// 路况和卫星图层控制
const showTraffic = ref(false);
const showSatellite = ref(false);
let satelliteLayer = null as any;

// 搜索相关
const searchKeyword = ref('');
let placeSearch: any = null;

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
      
      // 添加控件
      map.plugin(['AMap.ToolBar', 'AMap.Scale', 'AMap.MapType', 'AMap.Geolocation', 'AMap.Traffic', 'AMap.HeatMap'], () => {
        // 工具条控件
        map.addControl(new window.AMap.ToolBar({
          position: 'RB'
        }));
        
        // 比例尺控件
        map.addControl(new window.AMap.Scale());
        
        // 地图类型切换控件
        map.addControl(new window.AMap.MapType({
          defaultType: 0, // 默认显示地图
          showRoad: true
        }));
        
        // 定位控件
        map.addControl(new window.AMap.Geolocation({
          position: 'LB',
          showButton: true
        }));
        
        // 实时路况图层
        const trafficLayer = new window.AMap.Traffic({
          zIndex: 10
        });
        trafficLayer.hide(); // 默认隐藏路况图层
        
        // 保存路况图层以便后续控制
        mapStore.trafficLayer = trafficLayer;
      });
      
      // 监听地图缩放结束事件，以便重新绘制线路
      map.on('zoomend', () => {
        // 缩放不再需要重绘，Polyline能自动处理
      });

      // 加载地图完成后
      map.on('complete', () => {
        console.log('高德地图加载完成');
        
        // 加载标准地图
        new window.AMap.TileLayer();
        
        // 加载卫星图层
        new window.AMap.TileLayer.Satellite();
        
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
  console.log('添加站点标记，站点数据:', stations);
  if (!stations || stations.length === 0) {
    console.warn('没有站点数据可显示');
    
    // 在没有站点数据的情况下添加一些默认POI点
    if (window.AMap) {
      // 添加一些北京市著名地标作为示例
      const defaultPOIs = [
        { name: '天安门', position: [116.397428, 39.90923] },
        { name: '故宫博物院', position: [116.403963, 39.917317] },
        { name: '北京西站', position: [116.322056, 39.895086] },
        { name: '北京站', position: [116.427666, 39.902969] },
        { name: '北京南站', position: [116.378248, 39.865282] },
        { name: '北京首都国际机场', position: [116.603039, 40.080304] },
        { name: '北京大学', position: [116.310905, 39.992806] },
        { name: '清华大学', position: [116.326862, 40.002161] },
        { name: '中关村', position: [116.318031, 39.984886] },
        { name: '奥林匹克公园', position: [116.3977, 40.0026] }
      ];
      
      // 设置地图中心点到默认位置
      mapInstance.value.setCenter([116.397428, 39.90923]);
      
      // 添加默认POI点
      defaultPOIs.forEach(poi => {
        const marker = new window.AMap.Marker({
          position: poi.position,
          title: poi.name,
          label: {
            content: poi.name,
            direction: 'bottom'
          }
        });
        
        mapInstance.value.add(marker);
        mapMarkers.value.push(marker);
      });
      
      console.log('已添加默认POI点');
    }
    
    return;
  }
  
  // 创建新标记
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
        const textLabel = new window.AMap.Text({
          text: station.name,
          position: [station.position.lng, station.position.lat],
          offset: new window.AMap.Pixel(15, -6), // 调整文本位置，使其在标记右侧偏上
          style: {
            'background-color': 'transparent',
            'text-align': 'left',
            'font-size': '12px',
            'font-weight': '500',
            'color': '#2f2f2f',
            'border': 'none',
            'text-shadow': '1px 0 0 #fff, -1px 0 0 #fff, 0 1px 0 #fff, 0 -1px 0 #fff' // 白色描边，提升可读性
          },
          zIndex: 61,
          zooms: [14, 20] // 设置文本仅在缩放级别 14 到 20 显示
        });

        mapInstance.value.add(textLabel);
        stationLabels.value.push(textLabel);

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

  const heatmapData = mapStore.heatmapData;
  if (!heatmapData || heatmapData.length === 0) return;

  // 如果热力图实例不存在，则创建
  if (!heatmapInstance.value) {
    heatmapInstance.value = new window.AMap.HeatMap(mapInstance.value, {
      radius: 120, // 再次增大半径，让热力点可以连接成片
      opacity: [0, 0.85], // 调整透明度使其更平滑
      zIndex: 40, // 层级低于地铁线
      '3d': {
        height: 500, // 增加3D高度，但需要配合视角
        gridSize: 50, // 调整格网大小
      }
    });
  }

  // 设置热力图数据
  heatmapInstance.value.setDataSet({
    data: heatmapData,
    max: 100 // 降低最大值，让数据点显得更“热”，更容易融合
  });

  // 确保热力图是可见的
  heatmapInstance.value.show();
};

// 显示站点覆盖圈
const showStationCoverage = () => {
  if (!mapInstance.value || !mapStore.selectedStation) return;
  
  // 移除现有覆盖圈
  if (coverageCircle.value) {
    mapInstance.value.remove(coverageCircle.value);
    coverageCircle.value = null;
  }
  
  const station = mapStore.selectedStation;
  
  try {
    if (window.AMap) {
      const circle = new window.AMap.Circle({
        center: [station.position.lng, station.position.lat],
        radius: 100,
        strokeColor: '#409EFF',
        strokeWeight: 2,
        strokeOpacity: 0.6,
        fillColor: '#409EFF',
        fillOpacity: 0.2
      });
      
      mapInstance.value.add(circle);
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
    mapInstance.value.remove(coverageCircle.value);
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
      if (window.AMap) {
        // 创建单车图标
        const icon = {
          type: 'image',
          image: bike.type === 'electric' ? `${window.location.origin}/bike-electric.svg` : `${window.location.origin}/bike-regular.svg`,
          size: [24, 24],
          anchor: 'center'
        };
        
        const marker = new window.AMap.Marker({
          position: [bike.position.lng, bike.position.lat],
          icon: icon,
          offset: new window.AMap.Pixel(0, 0),
          opacity: bike.available ? 1 : 0.5
        });
        
        // 添加信息窗口
        const infoWindow = new window.AMap.InfoWindow({
          content: `
            <div class="bike-info">
              <h4>共享单车 #${bike.id}</h4>
              <p>类型: ${bike.type === 'electric' ? '电动车' : '普通车'}</p>
              <p>电量: ${bike.battery}%</p>
              <p>状态: ${bike.available ? '可用' : '不可用'}</p>
              <p>距离站点: ${bike.distanceToStation}米</p>
            </div>
          `,
          offset: new window.AMap.Pixel(0, -12)
        });
        
        marker.on('click', () => {
          infoWindow.open(mapInstance.value, marker.getPosition());
        });
        
        mapInstance.value.add(marker);
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

// 执行搜索
const performSearch = () => {
  if (!placeSearch) return;
  
  placeSearch.search(searchKeyword.value, (status: string, result: any) => {
    if (status === 'complete' && result.poiList) {
      // 清除现有标记
      clearMarkers();
      
      // 添加搜索结果标记
      const pois = result.poiList.pois;
      if (pois && pois.length > 0) {
        console.log('搜索到的POI点:', pois);
        
        // 设置地图中心点到第一个结果位置
        mapInstance.value.setCenter([pois[0].location.lng, pois[0].location.lat]);
        
        // 添加标记
        pois.forEach((poi: any) => {
          const marker = new window.AMap.Marker({
            position: [poi.location.lng, poi.location.lat],
            title: poi.name,
            label: {
              content: poi.name,
              direction: 'bottom'
            }
          });
          
          // 添加信息窗口
          const infoWindow = new window.AMap.InfoWindow({
            content: `
              <div class="poi-info">
                <h4>${poi.name}</h4>
                <p>地址: ${poi.address}</p>
                <p>类型: ${poi.type}</p>
                ${poi.tel ? `<p>电话: ${poi.tel}</p>` : ''}
              </div>
            `,
            offset: new window.AMap.Pixel(0, -32)
          });
          
          marker.on('click', () => {
            infoWindow.open(mapInstance.value, marker.getPosition());
          });
          
          mapInstance.value.add(marker);
          mapMarkers.value.push(marker);
        });
      }
    } else {
      console.warn('POI搜索失败或无结果');
    }
  });
};

// 清除所有单车标记
const clearBikeMarkers = () => {
  if (mapInstance.value && bikeMarkers.value.length > 0) {
    bikeMarkers.value.forEach(marker => {
      mapInstance.value.remove(marker);
    });
    bikeMarkers.value = [];
  }
};

// 切换覆盖圈显示
const toggleCoverageCircle = () => {
  if (mapStore.showCoverageCircle) {
    showStationCoverage();
  } else {
    removeStationCoverage();
  }
};

// 切换路况图层
const toggleTraffic = () => {
  if (!mapInstance.value || !mapStore.trafficLayer) return;
  
  showTraffic.value = !showTraffic.value;
  if (showTraffic.value) {
    mapStore.trafficLayer.show();
  } else {
    mapStore.trafficLayer.hide();
  }
};

// 切换卫星图层
const toggleSatellite = () => {
  if (!mapInstance.value) return;
  
  showSatellite.value = !showSatellite.value;
  
  if (!satelliteLayer && window.AMap) {
    satelliteLayer = new window.AMap.TileLayer.Satellite();
  }
  
  if (showSatellite.value) {
    mapInstance.value.add(satelliteLayer);
  } else {
    mapInstance.value.remove(satelliteLayer);
  }
};

// 监听选中站点变化
watch(() => mapStore.selectedStation, (newStation) => {
  if (newStation) {
    // 如果地图已加载且有选中的站点，则显示覆盖圈
    if (mapStore.showCoverageCircle) {
      showStationCoverage();
    }
    
    // 添加单车标记
    addBikeMarkers();
  } else {
    // 如果没有选中站点，则移除覆盖圈和单车标记
    removeStationCoverage();
    clearBikeMarkers();
  }
});

// 监听覆盖圈显示状态
watch(() => mapStore.showCoverageCircle, toggleCoverageCircle);

// 监听单车数据变化
watch(() => mapStore.bikesData, () => {
  if (mapStore.selectedStation) {
    addBikeMarkers();
  }
});

// 监听高亮线路变化，并重绘
watch(() => mapStore.highlightedLines, () => {
  drawSubwayLines();
}, { deep: true });


// 监听站点数据变化
watch(() => mapStore.stations, (newStations) => {
  console.log('站点数据发生变化:', newStations);
  if (newStations && newStations.length > 0 && mapInstance.value) {
    console.log('检测到站点数据更新，添加标记');
    addStationMarkers();
  }
});

// 组件挂载时初始化地图
onMounted(async () => {
          console.log('AMapComponent组件已挂载，初始化地图');
          await initMap();
        });

// 组件卸载时清理资源
onUnmounted(() => {
  if (mapInstance.value) {
    clearMarkers();
    clearBikeMarkers();
    removeStationCoverage();
    clearSubwayLines(); // 清除地铁线路
    mapInstance.value = null;
  }
});

// 导出组件方法
defineExpose({
  getMapInstance: () => mapInstance.value,
  addStationMarkers,
  showStationCoverage,
  removeStationCoverage,
  addBikeMarkers,
  clearMarkers,
  clearBikeMarkers,
  toggleCoverageCircle,
  toggleTraffic,
  toggleSatellite,
  searchPOI
});
</script>

<style scoped lang="scss">
.amap-container {
  width: 100%;
  height: 100%;
  position: relative;
  
  .map-loading {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    background-color: rgba(255, 255, 255, 0.8);
    z-index: 100;
    
    .loading-icon {
      font-size: 32px;
      margin-bottom: 16px;
      animation: rotate 1.5s linear infinite;
    }
  }
  
  .map-controls {
    position: absolute;
    top: 10px;
    left: 10px;
    z-index: 90;
  }
  
  .search-box {
    position: absolute;
    top: 10px;
    left: 50%;
    transform: translateX(-50%);
    width: 300px;
    z-index: 90;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

:deep(.station-info) {
  h4 {
    margin: 0 0 8px 0;
    color: #303133;
  }
  
  p {
    margin: 4px 0;
    color: #606266;
  }
}

:deep(.bike-info) {
  h4 {
    margin: 0 0 8px 0;
    color: #303133;
  }
  
  p {
    margin: 4px 0;
    color: #606266;
  }
}

:deep(.poi-info) {
  h4 {
    margin: 0 0 8px 0;
    color: #303133;
  }
  
  p {
    margin: 4px 0;
    color: #606266;
  }
}
</style> 
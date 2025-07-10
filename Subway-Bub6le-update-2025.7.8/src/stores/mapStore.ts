import { defineStore } from 'pinia';
import subwayLinesData from '@/assets/subway-lines.json';

// 定义站点类型
export interface Station {
  id: string;
  name: string;
  lines: string[];
  entrances: number;
  position: {
    lng: number;
    lat: number;
  };
}

// 定义单车类型
export interface Bike {
  id: string;
  type: 'regular' | 'electric';
  battery: number;
  available: boolean;
  position: {
    lng: number;
    lat: number;
  };
  distanceToStation: number;
}

// 定义交通流量数据类型
export interface TrafficData {
  time: string;
  inFlow: number;
  outFlow: number;
}

export const useMapStore = defineStore({
  id: 'map',
  
  state: () => ({
    mapInstance: null as any,
    stations: [] as Station[],
    selectedStation: null as Station | null,
    showCoverageCircle: false,
    bikesData: [] as Bike[],
    trafficData: [] as TrafficData[],
    currentTime: new Date(),
    predictionData: [] as TrafficData[],
    trafficLayer: null as any, // 添加路况图层
    
    // 调度方案相关
    dispatchPlans: [] as {
      id: string;
      name: string;
      source: Station;
      target: Station;
      bikeCount: number;
      status: 'pending' | 'executing' | 'completed';
      path: {lng: number, lat: number}[];
    }[],
    subwayLines: [] as any[],
    heatmapData: [] as { lng: number; lat: number; count: number }[],
    highlightedLines: new Set<string>(), // 新增：用于存储高亮线路的名称
  }),
  
  getters: {
    getStations: (state) => state.stations,
    getSelectedStation: (state) => state.selectedStation,
    getBikesNearSelectedStation: (state) => {
      if (!state.selectedStation) return [];
      // 过滤100米范围内的单车
      return state.bikesData.filter(bike => bike.distanceToStation <= 100);
    },
    getAvailableBikes: (state) => {
      return state.bikesData.filter(bike => bike.available);
    },
    getBikeSupplyDemandRatio: (state) => {
      if (!state.selectedStation || !state.trafficData.length) return 1;
      
      // 获取当前小时的交通数据
      const hour = state.currentTime.getHours();
      const currentTraffic = state.trafficData.find(t => {
        const trafficHour = parseInt(t.time.split(':')[0]);
        return trafficHour === hour;
      });
      
      if (!currentTraffic) return 1;
      
      // 计算供需比：可用单车数量 / 出站人数
      const availableBikes = state.bikesData.filter(
        bike => bike.available && bike.distanceToStation <= 100
      ).length;
      
      // 防止除零错误
      const outFlow = currentTraffic.outFlow || 1;
      
      return availableBikes / (outFlow / 10); // 假设10%的人需要单车
    },
    getSupplyStatus: (state) => {
      const ratio = (state as any).getBikeSupplyDemandRatio;
      
      if (ratio < 0.5) return '严重不足';
      if (ratio < 0.8) return '供应不足';
      if (ratio <= 1.2) return '供需平衡';
      if (ratio <= 2) return '供应充足';
      return '供应过剩';
    },
    getSupplyStatusType: (state) => {
      const ratio = (state as any).getBikeSupplyDemandRatio;
      
      if (ratio < 0.5) return 'danger';
      if (ratio < 0.8) return 'warning';
      if (ratio <= 1.2) return 'success';
      if (ratio <= 2) return 'info';
      return 'warning';
    },
    getHeatmapData: (state) => state.heatmapData,
  },
  
  actions: {
    // 设置地图实例
    setMapInstance(map: any) {
      this.mapInstance = map;
    },
    
    findStationByName(name: string): Station | undefined {
      return this.stations.find(station => station.name === name);
    },

    // 新增：设置高亮线路
    setHighlightedLines(lines: string[]) {
      this.highlightedLines = new Set(lines);
    },

    async loadSubwayData() {
      // 从导入的 JSON 文件中处理数据
      const allStations = new Map<string, Station>();
      
      this.subwayLines = subwayLinesData.map(line => {
        line.stations.forEach(stationData => {
          const stationKey = `${stationData.coord[0]}-${stationData.coord[1]}`;
          if (!allStations.has(stationKey)) {
            allStations.set(stationKey, {
              id: stationKey,
              name: stationData.name,
              position: { lng: stationData.coord[0], lat: stationData.coord[1] },
              lines: [],
              entrances: 0, // 原始数据中没有入口信息，暂设为0
            });
          }
          const station = allStations.get(stationKey)!;
          if (!station.lines.includes(line.name)) {
            station.lines.push(line.name);
          }
        });
        
        return {
          name: line.name,
          color: line.color,
          path: line.stations.map(s => s.coord),
        };
      });

      this.stations = Array.from(allStations.values());
      
      console.log('完整的地铁线路数据已加载:', this.subwayLines);
      console.log('所有站点数据已生成:', this.stations);

      // 数据加载后，生成热力图
      this.generateHeatmapData();
    },

    // 生成热力图数据
    generateHeatmapData() {
      if (this.stations.length === 0) return;
      
      this.heatmapData = this.stations.map(station => ({
        lng: station.position.lng,
        lat: station.position.lat,
        // 模拟客流量，数值越大，热力越强
        count: Math.floor(Math.random() * 500) + 100 
      }));

      console.log('热力图数据已生成:', this.heatmapData);
    },
    
    // 选择站点
    selectStation(station: Station) {
      this.selectedStation = station;
      this.loadBikesData(station.id);
      this.loadTrafficData(station.id);
      this.loadPredictionData(station.id);
    },
    
    // 加载共享单车数据
    loadBikesData(stationId: string) {
      // 模拟异步加载
      setTimeout(() => {
        // 随机生成单车数据
        const bikes: Bike[] = [];
        const stationInfo = this.stations.find(s => s.id === stationId);
        
        if (!stationInfo) return;
        
        const baseLng = stationInfo.position.lng;
        const baseLat = stationInfo.position.lat;
        
        // 生成30-50辆单车
        const bikeCount = Math.floor(Math.random() * 20) + 30;
        
        for (let i = 0; i < bikeCount; i++) {
          // 随机位置，在站点100-500米范围内
          const distance = Math.random() * 400 + 100;
          const angle = Math.random() * Math.PI * 2;
          
          // 经纬度偏移量（粗略计算，仅用于演示）
          const lngOffset = Math.cos(angle) * distance * 0.00001;
          const latOffset = Math.sin(angle) * distance * 0.00001;
          
          bikes.push({
            id: `B${stationId.substring(1)}-${i.toString().padStart(3, '0')}`,
            type: Math.random() > 0.3 ? 'regular' : 'electric',
            battery: Math.floor(Math.random() * 100),
            available: Math.random() > 0.1, // 90%的单车可用
            position: {
              lng: baseLng + lngOffset,
              lat: baseLat + latOffset
            },
            distanceToStation: distance
          });
        }
        
        this.bikesData = bikes;
      }, 300);
    },
    
    // 加载交通流量数据
    loadTrafficData(stationId: string) {
      // 模拟异步加载
      setTimeout(() => {
        const trafficData: TrafficData[] = [];
        
        // 生成24小时数据
        for (let hour = 0; hour < 24; hour++) {
          // 早晚高峰时段流量更大
          let factor = 1;
          if (hour >= 7 && hour <= 9) factor = 3; // 早高峰
          if (hour >= 17 && hour <= 19) factor = 2.5; // 晚高峰
          
          trafficData.push({
            time: `${hour}:00`,
            // 生成随机流量数据
            inFlow: Math.floor(Math.random() * 500 * factor) + 100,
            outFlow: Math.floor(Math.random() * 500 * factor) + 100
          });
        }
        
        this.trafficData = trafficData;
      }, 300);
    },
    
    // 加载预测数据
    loadPredictionData(stationId: string) {
      // 模拟异步加载
      setTimeout(() => {
        const predictionData: TrafficData[] = [];
        const currentHour = this.currentTime.getHours();
        
        // 生成未来6小时的预测数据
        for (let i = 1; i <= 6; i++) {
          const hour = (currentHour + i) % 24;
          
          // 早晚高峰时段流量更大
          let factor = 1;
          if (hour >= 7 && hour <= 9) factor = 3; // 早高峰
          if (hour >= 17 && hour <= 19) factor = 2.5; // 晚高峰
          
          predictionData.push({
            time: `${hour}:00`,
            // 生成随机预测数据，略有波动
            inFlow: Math.floor(Math.random() * 600 * factor) + 100,
            outFlow: Math.floor(Math.random() * 600 * factor) + 100
          });
        }
        
        this.predictionData = predictionData;
      }, 400);
    },
    
    // 设置当前时间
    setCurrentTime(time: Date) {
      this.currentTime = time;
      
      // 如果已选择站点，更新相关数据
      if (this.selectedStation) {
        this.loadTrafficData(this.selectedStation.id);
        this.loadPredictionData(this.selectedStation.id);
      }
    },
    
    // 添加调度方案
    addDispatchPlan(sourceStation: Station, targetStation: Station, bikeCount: number) {
      const id = `DP${this.dispatchPlans.length + 1}`;
      
      // 创建路径点（简单演示，实际项目应使用路径规划API）
      const path = [];
      const pointCount = Math.floor(Math.random() * 5) + 3; // 3-7个路径点
      
      const startLng = sourceStation.position.lng;
      const startLat = sourceStation.position.lat;
      const endLng = targetStation.position.lng;
      const endLat = targetStation.position.lat;
      
      // 起点
      path.push({lng: startLng, lat: startLat});
      
      // 中间点
      for (let i = 1; i < pointCount - 1; i++) {
        const ratio = i / (pointCount - 1);
        const lng = startLng + (endLng - startLng) * ratio + (Math.random() - 0.5) * 0.01;
        const lat = startLat + (endLat - startLat) * ratio + (Math.random() - 0.5) * 0.01;
        path.push({lng, lat});
      }
      
      // 终点
      path.push({lng: endLng, lat: endLat});
      
      // 添加到调度方案列表
      this.dispatchPlans.push({
        id,
        name: `从${sourceStation.name}到${targetStation.name}的调度计划`,
        source: sourceStation,
        target: targetStation,
        bikeCount,
        status: 'pending',
        path
      });
      
      return id;
    },
    
    // 执行调度方案
    executeDispatchPlan(planId: string) {
      const plan = this.dispatchPlans.find(p => p.id === planId);
      if (!plan) return false;
      
      plan.status = 'executing';
      
      // 模拟执行过程
      setTimeout(() => {
        plan.status = 'completed';
      }, 5000);
      
      return true;
    }
  }
}); 
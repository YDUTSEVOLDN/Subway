import { defineStore } from 'pinia';

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
    }[]
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
    }
  },
  
  actions: {
    // 设置地图实例
    setMapInstance(map: any) {
      this.mapInstance = map;
    },
    
    // 加载站点数据
    loadStations() {
      // 模拟异步加载
      return new Promise<void>((resolve) => {
        setTimeout(() => {
          this.stations = [
            {
              id: 'S001',
              name: '西二旗站',
              lines: ['13号线', '昌平线'],
              entrances: 4,
              position: { lng: 116.307, lat: 40.052 }
            },
            {
              id: 'S002',
              name: '知春路站',
              lines: ['13号线', '10号线'],
              entrances: 6,
              position: { lng: 116.344, lat: 39.978 }
            },
            {
              id: 'S003',
              name: '五道口站',
              lines: ['13号线'],
              entrances: 3,
              position: { lng: 116.339, lat: 39.993 }
            },
            {
              id: 'S004',
              name: '中关村站',
              lines: ['4号线'],
              entrances: 4,
              position: { lng: 116.318, lat: 39.984 }
            }
          ];
          
          console.log('站点数据已加载:', this.stations);
          resolve();
        }, 100); // 减少延迟时间
      });
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
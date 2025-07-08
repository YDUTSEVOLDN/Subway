// 交通流量数据类型
export interface TrafficData {
  time: string;
  inFlow: number;
  outFlow: number;
}

// 站点类型
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

// 共享单车类型
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

// 流量预测数据类型
export interface TrafficFlowData {
  stationId: string;
  currentFlow: {
    inflow: number;
    outflow: number;
    timestamp: string;
  };
  predictions: Array<{
    hour: number;
    inflow: number;
    outflow: number;
  }>;
}

// 单车供需状态类型
export interface BikeSupplyDemand {
  stationId: string;
  timestamp?: string;
  supply: number;
  demand: number;
  ratio: number;
  status: 'low' | 'medium' | 'high' | 'shortage' | 'balanced' | 'surplus';
} 
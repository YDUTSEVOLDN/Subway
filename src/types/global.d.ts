// 百度地图全局定义
declare interface Window {
  BMap: any;
}

// 流量数据类型
declare interface TrafficFlowData {
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
  stationId: number;
}

// 共享单车供需状态
declare interface BikeSupplyDemand {
  stationId: number;
  timestamp: string;
  supply: number;
  demand: number;
  ratio: number;
  status: 'shortage' | 'balanced' | 'surplus';
} 
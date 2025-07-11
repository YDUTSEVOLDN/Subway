// 路径点接口
export interface PathPoint {
  lng: number;
  lat: number;
}

// 扩展的路径步骤接口
export interface PathStep {
  instruction: string; // 导航指令
  distance: number; // 步骤距离（米）
  duration: number; // 步骤时间（秒）
  path: PathPoint[]; // 步骤路径点
  road?: string; // 道路名称
  direction?: string; // 方向信息
  action?: string; // 动作（直行、左转、右转等）
  toll?: number; // 该段收费
  traffic?: string; // 交通状况
  poi?: string; // 途经兴趣点
}

// 扩展的路径规划结果接口
export interface PathPlanningResult {
  path: PathPoint[];
  distance: number; // 距离（公里）
  duration: number; // 时间（分钟）
  steps: PathStep[];
  tolls: number; // 收费（元）
  tollDistance: number; // 收费距离（公里）
  
  // 新增字段
  startLocation?: string; // 起点位置描述
  endLocation?: string; // 终点位置描述
  trafficInfo?: string; // 整体交通状况
  fuelConsumption?: number; // 预计油耗（升）
  carbonEmission?: number; // 碳排放（克）
  waypoints?: PathPoint[]; // 途经点
  alternativeRoutes?: number; // 可选路线数量
  routeStrategy?: string; // 路线策略
  avoidTolls?: boolean; // 是否避开收费
  avoidHighways?: boolean; // 是否避开高速
}

// 路径规划选项
export interface PathPlanningOptions {
  optimizeFor: 'distance' | 'time'; // 优化目标
  avoidHighways?: boolean; // 是否避开高速
  avoidTolls?: boolean; // 是否避开收费
  vehicleType?: 'car' | 'bike' | 'walk'; // 出行方式
  waypoints?: PathPoint[]; // 途经点
  strategy?: string; // 路线策略
} 
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
  line: string;
  lat: number;
  lng: number;
}

// Add User-related types
export type UserRole = 'user' | 'manager' | 'subway' | 'admin' | 'guest' | 'ROLE_USER' | 'ROLE_MANAGER' | 'ROLE_SUBWAY' | 'ROLE_ADMIN';

export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  accessToken?: string;
  avatar?: string;
  permissions?: string[];
}

export interface AuthState {
  token: string | null;
  user: User | null;
  returnUrl: string | null;
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
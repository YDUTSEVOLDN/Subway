import axios from 'axios';
import { 
  mockStations, 
  generateTrafficData, 
  generateBikesData, 
  getBikeSupplyDemand, 
  generateDispatchPlans,
  generateDispatchRoute,
  mockApiRequest
} from './mockData';

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 5000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加身份验证等逻辑
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    console.error('API请求错误', error);
    return Promise.reject(error);
  }
);

// API服务类
export default {
  // 获取所有地铁站点
  getStations() {
    // 实际项目中应该是: return api.get('/stations');
    return mockApiRequest(mockStations);
  },
  
  // 获取指定站点的流量预测
  getTrafficPredict(stationId: string | number, timeOffset: number = 0) {
    // 实际项目中应该是: return api.get(`/traffic/predict/${stationId}`);
    return mockApiRequest(generateTrafficData(stationId, timeOffset));
  },
  
  // 获取站点周边的共享单车分布
  getBikesNearStation(stationId: string | number, count: number = 50) {
    // 实际项目中应该是: return api.get(`/bikes/near-station/${stationId}`);
    return mockApiRequest(generateBikesData(stationId, count));
  },
  
  // 获取站点周边共享单车供需状态
  getBikeStatus(stationId: string | number, timeOffset: number = 0) {
    // 实际项目中应该是: return api.get(`/bikes/status/${stationId}`);
    return mockApiRequest(getBikeSupplyDemand(stationId, timeOffset));
  },
  
  // 获取调度方案列表
  getDispatchPlans() {
    // 实际项目中应该是: return api.get('/dispatch/plans');
    return mockApiRequest(generateDispatchPlans());
  },
  
  // 获取调度路径规划
  getDispatchRoute(sourceId: number, destinationId: number) {
    // 实际项目中应该是: return api.get(`/dispatch/plan?from=${sourceId}&to=${destinationId}`);
    return mockApiRequest(generateDispatchRoute(sourceId, destinationId));
  },
  
  // 执行调度方案
  executeDispatch(planId: string) {
    // 实际项目中应该是: return api.post(`/dispatch/execute/${planId}`);
    return mockApiRequest({ success: true, planId });
  }
}; 
import axios from 'axios';
import { 
  mockStations, 
  generateTrafficData, 
  generateBikesData, 
  getBikeSupplyDemand, 
  generateDispatchPlans,
  generateDispatchRoute,
  mockApiRequest,
  generateStationRanking,
  generateSystemTrend,
  generateStationComparison,
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
  },

  /**
   * 获取站点客流排名
   * @param metric - 排名依据 (inflow, outflow, total)
   * @param limit - 返回数量
   */
  getStationRanking(metric: string, limit = 10) {
    // 真实请求: return api.get('/ranking/stations', { params: { metric, limit } });
    return mockApiRequest(generateStationRanking(metric, limit));
  },

  /**
   * 获取整个地铁系统的客流趋势
   * @param granularity - 时间粒度 (daily, weekly)
   * @param range - 时间范围 (last7days, last30days)
   */
  getSystemTrend(granularity: string, range: string) {
    // 真实请求: return api.get('/trends/system', { params: { granularity, range } });
    return mockApiRequest(generateSystemTrend(granularity, range));
  },

  /**
   * 获取多个站点的客流对比数据
   * @param stationIds - 站点 ID 数组
   * @param metric - 统计指标
   * @param range - 时间范围
   */
  getStationComparison(stationIds: string[], metric: string, range: string) {
    const ids = stationIds.join(',');
    // 真实请求: return api.get('/trends/stations/compare', { params: { stationIds: ids, metric, range } });
    return mockApiRequest(generateStationComparison(stationIds, metric, range));
  }
}; 
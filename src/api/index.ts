import axios from 'axios';
import trafficService from '../services/trafficService';
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
  baseURL: 'http://localhost:10086/api',
  timeout: 5000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user && user.accessToken) {
        config.headers['Authorization'] = 'Bearer ' + user.accessToken;
      }
    }
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

const apiService = {
  // 获取所有地铁站点
  getStations() {
    return api.get('/stations/all');
  },
  
  // 获取指定站点的流量预测
  // getTrafficPredict(stationId: string | number, timeOffset: number = 0) {
  //   // 实际项目中应该是: return api.get(`/traffic/predict/${stationId}`);
  //   return mockApiRequest(generateTrafficData(stationId, timeOffset));
  // },
  
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
   * 获取站点客流排名 - 已修改为调用真实接口
   * @param date - 查询日期
   */
  getStationRanking(date: string) {
    // 调用 trafficService 中已经封装好的真实接口
    return trafficService.getStationRankingData(date);
  },

  /**
   * 获取整个地铁系统的客流趋势 - 已修改为调用真实接口
   */
  getSystemTrend() {
    // 后端 /api/subway/totals 不需要参数，并返回所有日期的总流量
    return api.get('/subway/totals');
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

export default apiService; // Keep the default export for existing mock-based services

export { api }; // Export the configured axios instance 
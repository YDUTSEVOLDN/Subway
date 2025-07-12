import axios from 'axios';

interface DailyTotal {
  date: string;
  in_num: number;
  out_num: number;
}

interface AmountDto {
  [key: string]: number;
}

interface TimeAmountDto {
    time: number;
    inNum: number;
    outNum: number;
}

// 创建axios实例
const api = axios.create({
  // baseURL 会自动加在请求地址前面
  baseURL: '/api/subway',
  timeout: 5000,
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 可以在这里添加身份验证等逻辑
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    console.error('API请求错误', error);
    return Promise.reject(error);
  }
);

// API服务类
export default {
  /**
   * 按日期查询各站流量 (对应原 getStationRanking)
   * @param date - 查询日期 (格式: yyyy-MM-dd)
   * @param requestType - 'in', 'out', 或 'total'
   */
  getDateAmount(date: string, requestType: string = 'total') {
    return api.get('/date', { params: { date, Request: requestType } });
  },

  /**
   * 获取历史每日总流量 (对应原 getSystemTrend)
   */
  getDailyTotals(): Promise<DailyTotal[]> {
    return api.get('/totals');
  },
  
  /**
   * 查询站点近7天流量
   * @param station - 站点名称
   * @param endDate - 结束日期
   */
  getWeeklyTotals(station: string, endDate: string): Promise<DailyTotal[]> {
    return api.get('/weekly', { params: { station, endDate } });
  },
  
  /**
   * 获取用于地图可视化的流量
   * @param date - 查询日期
   */
  getMapData(date: string): Promise<AmountDto[]> {
    return api.get('/map', { params: { date } });
  },

  /**
   * 获取单日分时流量趋势
   * @param date - 查询日期
   */
  getTrendData(date: string): Promise<TimeAmountDto[]> {
    return api.get('/trend', { params: { date } });
  },

  /*
    // 以下是原有的、但后端不存在对应接口的方法，暂时注释掉

    // 获取所有地铁站点
    getStations() {
      // return api.get('/stations');
      return Promise.resolve([]); // 返回一个空数组以避免页面报错
    },
    
    // 获取指定站点的流量预测
    getTrafficPredict(stationId: string | number, timeOffset: number = 0) {
      // return api.get(`/traffic/predict/${stationId}`);
      return Promise.resolve({});
    },
    
    // 获取站点周边的共享单车分布
    getBikesNearStation(stationId: string | number, count: number = 50) {
      // return api.get(`/bikes/near-station/${stationId}`);
      return Promise.resolve([]);
    },
    
    // 获取站点周边共享单车供需状态
    getBikeStatus(stationId: string | number, timeOffset: number = 0) {
      // return api.get(`/bikes/status/${stationId}`);
      return Promise.resolve({});
    },
    
    // 获取调度方案列表
    getDispatchPlans() {
      // return api.get('/dispatch/plans');
      return Promise.resolve([]);
    },
    
    // 获取调度路径规划
    getDispatchRoute(sourceId: number, destinationId: number) {
      // return api.get(`/dispatch/plan?from=${sourceId}&to=${destinationId}`);
      return Promise.resolve({});
    },
    
    // 执行调度方案
    executeDispatch(planId: string) {
      // return api.post(`/dispatch/execute/${planId}`);
      return Promise.resolve({ success: true, planId });
    },

    // 获取多个站点的客流对比数据
    getStationComparison(stationIds: string[], metric: string, range: string) {
      const ids = stationIds.join(',');
      // return api.get('/trends/stations/compare', { params: { stationIds: ids, metric, range } });
      return Promise.resolve([]);
    }
  */
}; 
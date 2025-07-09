// 模拟数据服务
// 在实际项目中，这里会被替换为真实的API调用

// 模拟地铁站点数据
export const mockStations = [
  {
    id: 1,
    name: '人民广场站',
    position: {
      lng: 116.404,
      lat: 39.915
    },
    entrances: 4,
    lines: ['1号线', '2号线', '8号线'],
  },
  {
    id: 2,
    name: '西单站',
    position: {
      lng: 116.382,
      lat: 39.913
    },
    entrances: 3,
    lines: ['1号线', '4号线'],
  },
  {
    id: 3,
    name: '东直门站',
    position: {
      lng: 116.437,
      lat: 39.941
    },
    entrances: 2,
    lines: ['2号线', '13号线', '机场线'],
  },
  {
    id: 4,
    name: '北京南站',
    position: {
      lng: 116.378,
      lat: 39.865
    },
    entrances: 5,
    lines: ['4号线', '14号线'],
  },
  {
    id: 5,
    name: '国贸站',
    position: {
      lng: 116.461,
      lat: 39.909
    },
    entrances: 3,
    lines: ['1号线', '10号线'],
  }
];

// 生成随机流量数据
export const generateTrafficData = (stationId: string | number, timeOffset: number = 0) => {
  const baseFlow = Math.floor(Math.random() * 1000) + 500;
  const stationIdNumber = typeof stationId === 'string' ? parseInt(stationId) : stationId;
  
  // 模拟一天内的流量变化趋势
  const now = new Date();
  const hours = (now.getHours() + timeOffset) % 24;
  
  // 早晚高峰流量倍数
  let multiplier = 1;
  if (hours >= 7 && hours <= 9) {
    // 早高峰
    multiplier = 2.5;
  } else if (hours >= 17 && hours <= 19) {
    // 晚高峰
    multiplier = 2.3;
  } else if (hours >= 23 || hours <= 5) {
    // 夜间
    multiplier = 0.3;
  }
  
  const flow = Math.floor(baseFlow * multiplier);
  
  // 生成未来4小时的预测数据
  const predictions = [];
  for (let i = 1; i <= 4; i++) {
    const predictionHour = (hours + i) % 24;
    let predMultiplier = 1;
    
    if (predictionHour >= 7 && predictionHour <= 9) {
      predMultiplier = 2.5;
    } else if (predictionHour >= 17 && predictionHour <= 19) {
      predMultiplier = 2.3;
    } else if (predictionHour >= 23 || predictionHour <= 5) {
      predMultiplier = 0.3;
    }
    
    predictions.push({
      hour: predictionHour,
      inflow: Math.floor(baseFlow * predMultiplier * (0.9 + Math.random() * 0.2)),
      outflow: Math.floor(baseFlow * predMultiplier * (0.8 + Math.random() * 0.3)),
    });
  }
  
  return {
    stationId,
    currentFlow: {
      inflow: Math.floor(flow * (0.9 + Math.random() * 0.2)),
      outflow: Math.floor(flow * (0.8 + Math.random() * 0.3)),
      timestamp: new Date().toISOString(),
    },
    predictions,
  };
};

// 生成共享单车数据
export const generateBikesData = (stationId: string | number, count: number = 50) => {
  const stationIdNumber = typeof stationId === 'string' ? parseInt(stationId) : stationId;
  const station = mockStations.find(s => s.id === stationIdNumber);
  if (!station) return [];
  
  const bikes = [];
  for (let i = 0; i < count; i++) {
    // 在站点周围随机生成共享单车
    const offsetLng = (Math.random() - 0.5) * 0.01;
    const offsetLat = (Math.random() - 0.5) * 0.01;
    
    bikes.push({
      id: `bike-${stationIdNumber}-${i}`,
      position: {
        lng: station.position.lng + offsetLng,
        lat: station.position.lat + offsetLat
      },
      battery: Math.floor(Math.random() * 100),
      available: Math.random() > 0.1, // 90%可用率
      type: Math.random() > 0.7 ? 'electric' : 'regular',
      distanceToStation: Math.floor(Math.random() * 200) // 0-200米距离
    });
  }
  
  return bikes;
};

// 生成站点周围共享单车供需状态
export const getBikeSupplyDemand = (stationId: string | number, timeOffset: number = 0) => {
  const stationIdNumber = typeof stationId === 'string' ? parseInt(stationId) : stationId;
  const now = new Date();
  const hours = (now.getHours() + timeOffset) % 24;
  
  // 模拟不同时间的供需比
  let ratio = 1.0;
  if (hours >= 7 && hours <= 9) {
    // 早高峰，单车需求大于供应
    ratio = 0.6 + Math.random() * 0.2;
  } else if (hours >= 17 && hours <= 19) {
    // 晚高峰，同样供不应求
    ratio = 0.7 + Math.random() * 0.2;
  } else if (hours >= 10 && hours <= 16) {
    // 工作时间，供应大于需求
    ratio = 1.2 + Math.random() * 0.4;
  } else {
    // 其他时间，大致平衡
    ratio = 0.9 + Math.random() * 0.3;
  }
  
  // 生成具体数量
  const demand = Math.floor(Math.random() * 50) + 50;
  const supply = Math.floor(demand * ratio);
  
  let status: 'shortage' | 'balanced' | 'surplus';
  if (supply / demand < 0.8) {
    status = 'shortage';
  } else if (supply / demand > 1.2) {
    status = 'surplus';
  } else {
    status = 'balanced';
  }
  
  return {
    stationId,
    timestamp: new Date().toISOString(),
    supply,
    demand,
    ratio: supply / demand,
    status
  };
};

// 生成调度方案数据
export const generateDispatchPlans = () => {
  return [
    {
      id: 'plan-1',
      title: '早高峰东直门站补充方案',
      source: 3, // 东直门站
      destination: 1, // 人民广场站
      bikeCount: 15,
      distance: 4.5, // 公里
      estimatedDuration: 30, // 分钟
      status: 'pending', // pending, in-progress, completed
      priority: 'high',
      createdAt: new Date().toISOString()
    },
    {
      id: 'plan-2',
      title: '国贸站单车优化调度',
      source: 5, // 国贸站
      destination: 2, // 西单站
      bikeCount: 10,
      distance: 6.2, // 公里
      estimatedDuration: 40, // 分钟
      status: 'in-progress',
      priority: 'medium',
      createdAt: new Date().toISOString()
    },
    {
      id: 'plan-3',
      title: '北京南站周边补充计划',
      source: 4, // 北京南站
      destination: 5, // 国贸站
      bikeCount: 8,
      distance: 7.1, // 公里
      estimatedDuration: 45, // 分钟
      status: 'completed',
      priority: 'low',
      createdAt: new Date(Date.now() - 86400000).toISOString() // 昨天
    }
  ];
};

// 生成路径规划
export const generateDispatchRoute = (sourceId: number, destinationId: number) => {
  const source = mockStations.find(s => s.id === sourceId);
  const destination = mockStations.find(s => s.id === destinationId);
  
  if (!source || !destination) return null;
  
  // 生成途经点（为了让路径更自然）
  const waypoints = [];
  const pointCount = Math.floor(Math.random() * 3) + 2;
  
  for (let i = 0; i < pointCount; i++) {
    const progress = (i + 1) / (pointCount + 1);
    
    // 基于起点和终点间的线性插值，加上一些随机扰动
    const lng = source.position.lng + progress * (destination.position.lng - source.position.lng) +
      (Math.random() - 0.5) * 0.01;
    const lat = source.position.lat + progress * (destination.position.lat - source.position.lat) +
      (Math.random() - 0.5) * 0.01;
    
    waypoints.push({ lng, lat });
  }
  
  // 完整路径包括起点、途经点和终点
  const path = [
    source.position,
    ...waypoints,
    destination.position
  ];
  
  return {
    sourceId,
    destinationId,
    distance: Math.floor(Math.random() * 5) + 3, // 3-8公里
    duration: Math.floor(Math.random() * 30) + 15, // 15-45分钟
    path
  };
};

// 模拟API请求，返回Promise
export const mockApiRequest = <T>(data: T, delay: number = 500): Promise<T> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(data);
    }, delay);
  });
}; 
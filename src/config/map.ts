// 地图配置
export const mapConfig = {
  // 高德地图API密钥
  // 注意：实际项目中应该使用环境变量存储API密钥
  amapKey: 'YourAMapApiKey', // 请替换为有效的高德地图API密钥
  
  // 地图中心点（北京）
  center: {
    lng: 116.404,
    lat: 39.915
  },
  
  // 默认缩放级别
  defaultZoom: 13,
  
  // 地图样式
  mapStyle: 'amap://styles/whitesmoke'
};

export default mapConfig; 
/**
 * 高德地圖配置文件
 * 包含API密鑰和安全密鑰配置
 */

export const AMapConfig = {
  // API密鑰（用於前端地圖顯示和路徑規劃）
  apiKey: 'bba0da8b9d68b88dfd30c470fdb26f97',
  
  // 安全密鑰（用於後端API調用，如果需要）
  securityKey: 'b857d9434164809b5490808f5ced3e93',
  
  // API版本
  version: '2.0',
  
  // 插件列表
  plugins: [
    'AMap.Scale',
    'AMap.ToolBar', 
    'AMap.MapType',
    'AMap.Geolocation',
    'AMap.Marker',
    'AMap.Autocomplete',
    'AMap.PlaceSearch',
    'AMap.DistrictSearch',
    'AMap.Traffic',
    'AMap.HeatMap',
    'AMap.PolyEditor',
    'AMap.Driving',
    'AMap.Walking',
    'AMap.Riding'
  ],
  
  // 地圖默認配置
  mapConfig: {
    zoom: 12,
    center: [116.397428, 39.90923], // 北京天安門
    mapStyle: 'amap://styles/normal'
  },
  
  // 路徑規劃配置
  pathPlanningConfig: {
    // 默認路徑規劃策略
    defaultPolicy: 'LEAST_TIME',
    
    // 路徑規劃選項
    options: {
      avoidHighways: false,
      avoidTolls: false,
      vehicleType: 'bike'
    }
  }
};

/**
 * 獲取API URL
 */
export function getAMapAPIUrl(): string {
  const plugins = AMapConfig.plugins.join(',');
  return `https://webapi.amap.com/maps?v=${AMapConfig.version}&key=${AMapConfig.apiKey}&plugin=${plugins}`;
}

/**
 * 獲取UI庫URL
 */
export function getAMapUILibUrl(): string {
  return 'https://webapi.amap.com/ui/1.1/main.js?v=1.1.1';
}

/**
 * 檢查API密鑰是否有效
 */
export function isValidAPIKey(): boolean {
  return Boolean(AMapConfig.apiKey && 
         AMapConfig.apiKey !== 'YOUR_API_KEY_HERE' && 
         AMapConfig.apiKey.length > 0);
}

/**
 * 獲取地圖配置
 */
export function getMapConfig() {
  return AMapConfig.mapConfig;
}

/**
 * 獲取路徑規劃配置
 */
export function getPathPlanningConfig() {
  return AMapConfig.pathPlanningConfig;
} 
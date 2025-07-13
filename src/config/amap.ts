/**
 * 高德地圖配置文件
 * 包含API密鑰和安全密鑰配置
 */

export const AMapConfig = {
  version: '2.0',
  apiKey: 'bba0da8b9d68b88dfd30c470fdb26f97', 
  securityKey: 'b857d9434164809b5490808f5ced3e93', 
  plugins: [
    'AMap.Scale',
    'AMap.ToolBar',
    'AMap.Geolocation',
    'AMap.HeatMap',
    'AMap.Driving',
    'AMap.Riding',
    'AMap.Walking',
    'AMap.PlaceSearch'
  ],
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
import { ElMessage } from 'element-plus';
import type { Station } from '../stores/mapStore';
import type { PathPlanningOptions, PathPlanningResult, PathStep, PathPoint } from '../types/path';

declare global {
  interface Window {
    AMap: any;
  }
}

/**
 * PathPlanningService 負責所有與路徑規劃相關的計算。
 * 它假定高德地圖API和所有必需的插件（Driving, Walking, Riding）
 * 都已經由應用的其他部分（如 AMapComponent）加載完畢。
 */
export class PathPlanningService {
  private driving: any = null;
  private walking: any = null;
  private riding: any = null;
  private isInitialized: boolean = false;

  /**
   * 初始化服務，創建路徑規劃插件的實例。
   * 這是調用任何規劃方法之前必須執行的步驟。
   * @returns {Promise<boolean>} 如果初始化成功則返回 true，否則返回 false。
   */
  public initialize(): Promise<boolean> {
    return new Promise((resolve) => {
      if (this.isInitialized) {
        resolve(true);
        return;
      }

      if (!window.AMap || !window.AMap.Driving || !window.AMap.Walking || !window.AMap.Riding) {
        console.error('路徑規劃插件尚未完全加載。');
        ElMessage.error('路徑規劃資源未就緒，請稍後重試。');
        resolve(false);
        return;
      }

      try {
        this.driving = new window.AMap.Driving({ policy: window.AMap.DrivingPolicy.LEAST_TIME, map: null });
        this.walking = new window.AMap.Walking({ map: null });
        this.riding = new window.AMap.Riding({ map: null });
        
        this.isInitialized = true;
        console.log('路径规划服务初始化成功');
        resolve(true);
      } catch (error) {
        console.error('創建路徑規劃實例時出錯:', error);
        ElMessage.error('創建路徑規劃實例失敗。');
        resolve(false);
      }
    });
  }

  /**
   * 根據指定的起點、終點和選項規劃路徑。
   * @param source 起點站
   * @param target 終點站
   * @param options 規劃選項
   * @returns {Promise<PathPlanningResult>} 包含路徑信息的 Promise。
   */
  public async planPath(
    source: Station,
    target: Station,
    options: PathPlanningOptions = { optimizeFor: 'distance' }
  ): Promise<PathPlanningResult> {
    if (!this.isInitialized) {
      console.log('服務尚未初始化，正在嘗試初始化...');
      const success = await this.initialize();
      if (!success) {
        throw new Error('路徑規劃服務初始化失敗，無法進行路徑規劃。');
      }
    }

    const startPoint: [number, number] = [source.position.lng, source.position.lat];
    const endPoint: [number, number] = [target.position.lng, target.position.lat];
    const vehicleType = options.vehicleType || 'bike';

    switch (vehicleType) {
      case 'car':
        return this.planDrivingPath(startPoint, endPoint, options);
      case 'bike':
        return this.planRidingPath(startPoint, endPoint, options);
      case 'walk':
        return this.planWalkingPath(startPoint, endPoint, options);
      default:
        throw new Error(`不支持的交通方式: ${vehicleType}`);
    }
  }
  
  private planDrivingPath(start: [number, number], end: [number, number], options: PathPlanningOptions): Promise<PathPlanningResult> {
    return new Promise((resolve, reject) => {
      // 設置路徑規劃策略
      const policy = options.optimizeFor === 'time' ? window.AMap.DrivingPolicy.LEAST_TIME : window.AMap.DrivingPolicy.LEAST_DISTANCE;
      this.driving.setPolicy(policy);
      
      // 設置避開選項
      if (options.avoidHighways) {
        this.driving.setAvoidHighways(true);
      }
      if (options.avoidTolls) {
        this.driving.setAvoidTolls(true);
      }
      
      this.driving.search(start, end, (status: string, result: any) => {
        console.log('駕車路徑規劃結果:', status, result);
        
        if (status === 'complete') {
          if (result.routes && result.routes.length > 0) {
            const route = result.routes[0];
            resolve({
              path: this.extractPathFromRoute(route),
              distance: route.distance / 1000,
              duration: route.time / 60,
              steps: this.extractStepsFromRoute(route),
              tolls: route.tolls || 0,
              tollDistance: route.toll_distance ? route.toll_distance / 1000 : 0,
              startLocation: route.startLocation || '',
              endLocation: route.endLocation || '',
              trafficInfo: route.trafficInfo || '',
              fuelConsumption: this.calculateFuelConsumption(route.distance / 1000),
              carbonEmission: this.calculateCarbonEmission(route.distance / 1000),
              alternativeRoutes: result.routes.length,
              routeStrategy: options.optimizeFor === 'time' ? '最短時間' : '最短距離',
              avoidTolls: options.avoidTolls || false,
              avoidHighways: options.avoidHighways || false
            });
          } else {
            reject(new Error('未找到駕車路線'));
          }
        } else {
          reject(new Error(`駕車路徑規劃失敗: ${result}`));
        }
      });
    });
  }

  private planRidingPath(start: [number, number], end: [number, number], options: PathPlanningOptions): Promise<PathPlanningResult> {
    return new Promise((resolve, reject) => {
      this.riding.search(start, end, (status: string, result: any) => {
        console.log('騎行路徑規劃結果:', status, result);
        
        if (status === 'complete') {
          if (result.routes && result.routes.length > 0) {
            const route = result.routes[0];
            console.log('騎行路徑詳細信息:', route);
            
            const path = this.extractPathFromRoute(route);
            const steps = this.extractStepsFromRoute(route);
            
            console.log('提取的路徑點:', path);
            console.log('提取的步驟:', steps);
            
            resolve({
              path,
              distance: route.distance / 1000,
              duration: route.time / 60,
              steps,
              tolls: 0,
              tollDistance: 0,
              startLocation: route.startLocation || '',
              endLocation: route.endLocation || '',
              trafficInfo: '騎行不受交通擁堵影響',
              carbonEmission: this.calculateCarbonEmission(route.distance / 1000, 'bike'),
              alternativeRoutes: result.routes.length,
              routeStrategy: '騎行路線',
              avoidTolls: false,
              avoidHighways: false
            });
          } else {
            reject(new Error('未找到騎行路線'));
          }
        } else {
          reject(new Error(`騎行路徑規劃失敗: ${result}`));
        }
      });
    });
  }

  private planWalkingPath(start: [number, number], end: [number, number], options: PathPlanningOptions): Promise<PathPlanningResult> {
    return new Promise((resolve, reject) => {
       this.walking.search(start, end, (status: string, result: any) => {
        console.log('步行路徑規劃結果:', status, result);
        
        if (status === 'complete') {
          if (result.routes && result.routes.length > 0) {
            const route = result.routes[0];
            resolve({
              path: this.extractPathFromRoute(route),
              distance: route.distance / 1000,
              duration: route.time / 60,
              steps: this.extractStepsFromRoute(route),
              tolls: 0,
              tollDistance: 0,
              startLocation: route.startLocation || '',
              endLocation: route.endLocation || '',
              trafficInfo: '步行不受交通擁堵影響',
              carbonEmission: 0, // 步行無碳排放
              alternativeRoutes: result.routes.length,
              routeStrategy: '步行路線',
              avoidTolls: false,
              avoidHighways: false
            });
          } else {
            reject(new Error('未找到步行路線'));
          }
        } else {
          reject(new Error(`步行路徑規劃失敗: ${result}`));
        }
      });
    });
  }

  private extractPathFromRoute(route: any): PathPoint[] {
    const path: PathPoint[] = [];
    
    console.log('開始提取路徑點，route:', route);
    
    if (route && route.steps) {
      console.log('路徑步驟數量:', route.steps.length);
      
      route.steps.forEach((step: any, index: number) => {
        console.log(`步驟 ${index}:`, step);
        
        if (step.path && Array.isArray(step.path)) {
          step.path.forEach((p: any) => {
            // 檢查點位對象的結構
            if (p && typeof p.getLng === 'function' && typeof p.getLat === 'function') {
              path.push({ lng: p.getLng(), lat: p.getLat() });
            } else if (p && typeof p.lng === 'number' && typeof p.lat === 'number') {
              path.push({ lng: p.lng, lat: p.lat });
            } else if (p && Array.isArray(p) && p.length >= 2) {
              path.push({ lng: p[0], lat: p[1] });
            }
          });
        }
      });
    }
    
    // 如果從步驟中沒有提取到路徑點，嘗試從其他屬性提取
    if (path.length === 0 && route.path) {
      console.log('嘗試從 route.path 提取路徑點');
      if (Array.isArray(route.path)) {
        route.path.forEach((p: any) => {
          if (p && typeof p.getLng === 'function' && typeof p.getLat === 'function') {
            path.push({ lng: p.getLng(), lat: p.getLat() });
          } else if (p && typeof p.lng === 'number' && typeof p.lat === 'number') {
            path.push({ lng: p.lng, lat: p.lat });
          } else if (p && Array.isArray(p) && p.length >= 2) {
            path.push({ lng: p[0], lat: p[1] });
          }
        });
      }
    }
    
    // 如果還是沒有路徑點，創建起點和終點
    if (path.length === 0 && route.origin && route.destination) {
      console.log('創建起點和終點路徑');
      path.push(
        { lng: route.origin.getLng(), lat: route.origin.getLat() },
        { lng: route.destination.getLng(), lat: route.destination.getLat() }
      );
    }
    
    console.log('提取的路徑點數量:', path.length);
    return path;
  }

  private extractStepsFromRoute(route: any): PathStep[] {
    if (!route || !route.steps) {
      return [];
    }
    return route.steps.map((step: any) => ({
      instruction: step.instruction || '',
      distance: step.distance || 0,
      duration: step.time || 0,
      path: step.path ? step.path.map((p: any) => ({ lng: p.getLng(), lat: p.getLat() })) : [],
      road: step.road || '',
      direction: step.direction || '',
      action: this.extractAction(step.instruction),
      toll: step.toll || 0,
      traffic: step.traffic || '',
      poi: step.poi || ''
    }));
  }

  private extractAction(instruction: string): string {
    if (!instruction) return '';
    
    if (instruction.includes('左转')) return '左转';
    if (instruction.includes('右转')) return '右转';
    if (instruction.includes('直行')) return '直行';
    if (instruction.includes('掉头')) return '掉头';
    if (instruction.includes('左转')) return '左转';
    if (instruction.includes('右转')) return '右转';
    if (instruction.includes('到达')) return '到达';
    if (instruction.includes('出发')) return '出发';
    
    return '继续';
  }

  private calculateFuelConsumption(distance: number): number {
    // 簡單的油耗計算：假設百公里油耗8升
    return (distance * 8) / 100;
  }

  private calculateCarbonEmission(distance: number, vehicleType: string = 'car'): number {
    // 碳排放計算（克/公里）
    const emissionFactors = {
      car: 150, // 汽車
      bike: 0,  // 自行車
      walk: 0   // 步行
    };
    
    return distance * (emissionFactors[vehicleType as keyof typeof emissionFactors] || 0);
  }

  /**
   * 清理資源。
   */
  public destroy(): void {
    this.driving = null;
    this.walking = null;
    this.riding = null;
    this.isInitialized = false;
    console.log('路径规划服务已销毁');
  }
}

// 創建並導出服務的單一實例
export const pathPlanningService = new PathPlanningService(); 
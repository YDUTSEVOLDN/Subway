import { useMapStore } from '@/stores/mapStore';
import type { Station } from '@/stores/mapStore';
import realBikeData from '@/assets/bike_data.json';
import subwayData from '@/assets/subway_data.json';
import type { BikeSupplyDemand, StationBikeStatus } from '@/types';
import allStations from '@/assets/subway_data.json';


// 修正：从 Dashboard.vue 提取的辅助函数，增加对0需求的处理
function getStatusClass(ratio: number, demand: number): 'shortage' | 'balanced' | 'surplus' {
    if (demand === 0) {
        return 'balanced'; // 没有需求，总是平衡的
    }
    if (ratio < 0.8) return 'shortage';
    if (ratio > 1.2) return 'surplus';
    return 'balanced';
}

class BikeService {
    public async getBikeStatusForStation(station: Station, date: string): Promise<BikeSupplyDemand | null> {
        try {
            // 1. 获取站点的实时流量数据 (逻辑从 Dashboard.vue 迁移)
            const dailyTrafficData = (subwayData as Record<string, any[]>)[date] || [];
            const stationHourlyData = dailyTrafficData.filter(s => s.name === station.name);

            let demand = 0;
            if (stationHourlyData.length > 0) {
                const currentHour = new Date().getHours();
                const currentHourData = stationHourlyData[currentHour] || stationHourlyData[stationHourlyData.length - 1];
                // 需求量基于出站人流估算, 修正回正确的字段名
                demand = Math.ceil(currentHourData.out_num * 0.01);
            }

            // 2. 获取站点的单车供应数据 (逻辑从 Dashboard.vue 迁移)
            const dailyBikeData = (realBikeData as Record<string, any[]>)[date] || [];
            const stationBikeInfo = dailyBikeData.find(s => s.name === station.name);
            const supply = stationBikeInfo ? stationBikeInfo.bike_count : 0;

            // 3. 计算供需比和状态
            const ratio = supply / (demand || 1);

            return {
                stationId: station.id,
                supply,
                demand,
                ratio,
                status: getStatusClass(ratio, demand),
                timestamp: new Date().toISOString(),
            };
        } catch (error) {
            console.error(`加载站点 ${station.id} 单车状态失败`, error);
            return null;
        }
    }

    public async getBikeStatusForAllStations(): Promise<StationBikeStatus[]> {
        const mapStore = useMapStore();
        const allStationsList = mapStore.stations; 
        const date = mapStore.selectedDate;

        const statuses: StationBikeStatus[] = [];

        for (const station of allStationsList) {
            const statusInfo = await this.getBikeStatusForStation(station, date);
            if (statusInfo) {
                statuses.push({
                    ...statusInfo,
                    name: station.name,
                    latitude: station.position.lat,
                    longitude: station.position.lng,
                });
            }
        }
        return statuses;
    }
}

export const bikeService = new BikeService(); 
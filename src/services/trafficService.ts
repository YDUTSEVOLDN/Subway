import axios from 'axios';
import type { HeatmapData } from '@/types';

const API_URL = 'http://localhost:10086/api';

/**
 * 获取用于热力图的流量数据
 * @param date 日期，格式 YYYY-MM-DD
 * @returns 
 */
async function getHeatmapData(date: string): Promise<HeatmapData[]> {
  const response = await axios.get(`${API_URL}/subway/map`, {
    params: { date }
  });
  // 后端返回的是 List<AmountDto>，我们需要转换成前端的 HeatmapData[]
  // AmountDto: { station, inNum, outNum, lat, lng }
  return response.data.map((item: any) => ({
    lng: item.lng,
    lat: item.lat,
    count: item.inNum + item.outNum, // 将进出站人数之和作为热力权重
  }));
}

/**
 * 获取指定日期的分时流量趋势
 * @param date 日期，格式 YYYY-MM-DD
 * @returns 
 */
async function getSubwayTrendByDate(date: string) {
  const response = await axios.get(`${API_URL}/subway/trend`, {
    params: { date }
  });
  return response.data;
}

/**
 * 获取用于排名的站点流量数据
 * @param date 日期，格式 YYYY-MM-DD
 * @returns 
 */
async function getStationRankingData(date: string) {
  const response = await axios.get(`${API_URL}/subway/map`, {
    params: { date }
  });
  // 后端返回 List<AmountDto>，直接返回即可，转换逻辑在组件中处理
  return response.data;
}

/**
 * 获取指定站点近7天的流量数据
 * @param station 站点名称
 * @param endDate 结束日期，格式 YYYY-MM-DD
 * @returns 
 */
async function getWeeklySubwayTotals(station: string, endDate: string) {
  const response = await axios.get(`${API_URL}/subway/weekly`, {
    params: { station, endDate }
  });
  return response.data;
}


const trafficService = {
  getHeatmapData,
  getSubwayTrendByDate,
  getStationRankingData,
  getWeeklySubwayTotals,
};

export default trafficService; 
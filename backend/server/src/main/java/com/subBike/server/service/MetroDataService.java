//package com.subBike.server.service;
//
//import com.subBike.server.entity.MetroHistoricalData;
//import com.subBike.server.mapper.HistoricalMapper;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.stream.Collectors;
//
//// 2. 数据获取服务类
//public class MetroDataService {
//    HistoricalMapper historicalMapper;
//
//    /**
//     * 获取历史三天的地铁流量数据
//     * @return [3天][24小时][特征数] 的数据
//     */
//    public float[][][] getHistoricalData() {
//        // 计算日期范围：从昨天往回数3天
//
//        LocalDate endDate = LocalDate.now().minusDays(1);
//        LocalDate startDate = endDate.minusDays(2);
//
//        // 从数据库查询历史数据
//        List<MetroHistoricalData> rawData = historicalMapper.findByDateBetween(startDate, endDate);
//
//        // 初始化结果数组
//        float[][][] data = new float[3][24][1]; // 3天，24小时，1个特征
//
//        // 按日期和小时分组数据
//        Map<String, List<MetroHistoricalData>> groupedData = rawData.stream()
//                .collect(Collectors.groupingBy(record ->
//                        record.getDate().toString() + "_" + record.getTimeSlot().getHour()));
//
//        // 填充数据
//        Random random = new Random();
//        for (int day = 0; day < 3; day++) {
//            LocalDate currentDate = startDate.plusDays(day);
//
//            for (int hour = 0; hour < 24; hour++) {
//                String key = currentDate.toString() + "_" + hour;
//                List<MetroHistoricalData> hourData = groupedData.get(key);
//
//                if (hourData != null && !hourData.isEmpty()) {
//                    // 计算该小时所有站点的平均流量
//                    double avgFlow = hourData.stream()
//                            .mapToDouble(record -> record.getInCount() + record.getOutCount())
//                            .average().orElse(0);
//                    data[day][hour][0] = (float) avgFlow;
//                } else {
//                    // 缺失数据时使用模拟数据
//                    if (hour >= 7 && hour <= 9 || hour >= 17 && hour <= 19) {
//                        data[day][hour][0] = 800 + random.nextFloat() * 400; // 高峰期
//                    } else {
//                        data[day][hour][0] = 200 + random.nextFloat() * 300; // 平峰期
//                    }
//                }
//            }
//        }
//        return data;
//    }
//    /**
//     * 保存预测结果到数据库
//     */
//    public void savePredictions(float[][] predictions) {
//        // 实际项目中保存到数据库
//        System.out.println("保存预测结果:");
//        for (int day = 0; day < predictions.length; day++) {
//            System.out.println("第" + (day + 1) + "天预测:");
//            for (int hour = 0; hour < predictions[day].length; hour++) {
//                System.out.printf("  %02d:00 - %.2f人次\n", hour, predictions[day][hour]);
//            }
//        }
//    }
//}
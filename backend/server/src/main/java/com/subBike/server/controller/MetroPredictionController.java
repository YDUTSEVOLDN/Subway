//package com.subBike.server.controller;
//
//
//// 3. REST接口控制器
//import ai.onnxruntime.OrtException;
//import com.subBike.server.service.MetroDataService;
//import com.subBike.server.service.MetroPredictService;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/metro")
//public class MetroPredictionController {
//
//    private MetroPredictService predictionService;
//    private MetroDataService dataService;
//
//    public MetroPredictionController() throws OrtException {
//        this.predictionService = new MetroPredictService();
//        this.dataService = new MetroDataService();
//    }
//
//    /**
//     * 手动触发预测接口
//     */
//    @GetMapping("/predict")
//    public Map<String, Object> predict() {
//        try {
//            // 获取历史数据
//            float[][][] historicalData = dataService.getHistoricalData();
//
//            // 执行预测
//            float[][] predictions = predictionService.predict(historicalData);
//
//            // 保存结果
//            dataService.savePredictions(predictions);
//
//            // 返回结果
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", "预测完成");
//            response.put("predictions", predictions);
//            return response;
//
//        } catch (Exception e) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", false);
//            response.put("error", e.getMessage());
//            return response;
//        }
//    }
//
//    /**
//     * 每天自动执行预测 - 凌晨1点执行
//     */
//    @Scheduled(cron = "0 0 1 * * ?")
//    public void scheduledPredict() {
//        System.out.println("开始执行定时预测任务...");
//        predict();
//        System.out.println("定时预测任务完成");
//    }
//}
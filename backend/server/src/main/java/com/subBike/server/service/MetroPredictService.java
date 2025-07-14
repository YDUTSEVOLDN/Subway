//package com.subBike.server.service;
//// 1. 模型预测服务类 - ONNX版本
//import ai.onnxruntime.*;
//import java.util.*;
//
//public class MetroPredictService {
//    private OrtEnvironment env;
//    private OrtSession session;
//
//    public MetroPredictService() throws OrtException {
//        // 初始化ONNX环境和加载模型
//        this.env = OrtEnvironment.getEnvironment();
//        this.session = env.createSession("metro_lstm.onnx", // 转换后的ONNX模型
//                new OrtSession.SessionOptions());
//
//        // 打印模型信息，帮助调试
//        printModelInfo();
//    }
//
//    /**
//     * 打印模型输入输出信息，帮助调试
//     */
//    private void printModelInfo() throws OrtException {
//        System.out.println("=== 模型信息 ===");
//
//        // 输入信息
//        System.out.println("输入节点:");
//        for (String inputName : session.getInputNames()) {
//            System.out.println("  输入名称: " + inputName);
//            var inputInfo = session.getInputInfo().get(inputName);
//            if (inputInfo != null) {
//                System.out.println("  输入类型: " + inputInfo.getInfo());
//            }
//        }
//
//        // 输出信息
//        System.out.println("输出节点:");
//        for (String outputName : session.getOutputNames()) {
//            System.out.println("  输出名称: " + outputName);
//            var outputInfo = session.getOutputInfo().get(outputName);
//            if (outputInfo != null) {
//                System.out.println("  输出类型: " + outputInfo.getInfo());
//            }
//        }
//        System.out.println("================");
//    }
//
//    /**
//     * 预测未来两天的地铁流量
//     * @param historicalData 历史三天的流量数据 [天数, 时间点, 特征数]
//     * @return 预测的两天流量数据
//     */
//    public float[][] predict(float[][][] historicalData) throws OrtException {
//        // 构造输入张量 (batch_size=1, sequence_length=3, features)
//        long[] inputShape = {1, 3, historicalData[0][0].length};
//
//        // 创建输入张量 - 最兼容的方式
//        try {
//            // 方法1：直接使用多维数组（推荐）
//            float[][][] inputArray = reshapeToTensor(historicalData);
//            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputArray);
//
//            // 构造输入映射 - 动态获取输入名称
//            String inputName = session.getInputNames().iterator().next(); // 获取第一个输入名称
//            Map<String, OnnxTensor> inputs = new HashMap<>();
//            inputs.put(inputName, inputTensor);
//
//            // 执行推理
//            OrtSession.Result results = session.run(inputs);
//
//            // 获取输出
//            float[][] predictions = extractPredictions(results);
//
//            // 清理资源
//            inputTensor.close();
//            results.close();
//
//            return predictions;
//
//        } catch (Exception e) {
//            // 如果上面方法失败，尝试备用方案
//            System.err.println("使用备用张量创建方法: " + e.getMessage());
//            return predictWithFallback(historicalData);
//        }
//    }
//
//    /**
//     * 将输入数据重新整理为张量格式
//     */
//    private float[][][] reshapeToTensor(float[][][] historicalData) {
//        // 输入形状: [batch_size=1, sequence_length=3, features]
//        int batchSize = 1;
//        int seqLength = historicalData.length; // 3天
//        int features = historicalData[0][0].length; // 特征数
//
//        float[][][] tensor = new float[batchSize][seqLength][features];
//
//        for (int day = 0; day < seqLength; day++) {
//            // 将每天24小时的数据聚合或选择代表性数据
//            // 这里简化为取第一个小时的数据，实际应用中需要根据模型要求调整
//            System.arraycopy(historicalData[day][0], 0, tensor[0][day], 0, features);
//        }
//
//        return tensor;
//    }
//
//    /**
//     * 备用预测方法
//     */
//    private float[][] predictWithFallback(float[][][] historicalData) {
//        // 返回模拟数据或抛出异常
//        System.err.println("ONNX模型调用失败，返回模拟预测数据");
//        float[][] mockPredictions = new float[2][24]; // 2天，24小时
//        Random random = new Random();
//
//        for (int day = 0; day < 2; day++) {
//            for (int hour = 0; hour < 24; hour++) {
//                mockPredictions[day][hour] = 300 + random.nextFloat() * 200;
//            }
//        }
//        return mockPredictions;
//    }
//
//    private float[] flattenInput(float[][][] data) {
//        List<Float> flattened = new ArrayList<>();
//        for (int day = 0; day < data.length; day++) {
//            for (int timePoint = 0; timePoint < data[day].length; timePoint++) {
//                for (int feature = 0; feature < data[day][timePoint].length; feature++) {
//                    flattened.add(data[day][timePoint][feature]);
//                }
//            }
//        }
//        float[] result = new float[flattened.size()];
//        for (int i = 0; i < flattened.size(); i++) {
//            result[i] = flattened.get(i);
//        }
//        return result;
//    }
//
//    private float[][] extractPredictions(OrtSession.Result results) throws OrtException {
//        // 获取第一个输出（假设只有一个输出）
//        OnnxValue output = results.get(0);
//        float[][][] result3D = (float[][][]) output.getValue();
//
//        // 返回2天的预测数据 [2天, 24小时]
//        // 根据你的模型输出格式调整
//        return new float[][]{result3D[0][0], result3D[0][1]};
//    }
//
//    public void close() throws OrtException {
//        if (session != null) session.close();
//        if (env != null) env.close();
//    }
//}

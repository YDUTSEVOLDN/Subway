import os
import onnxruntime as ort
from typing import Optional
import numpy as np

class ONNXModelAdapter:
    def __init__(self, model_path):
        """
        加载ONNX模型并初始化推理会话
        :param model_path: ONNX模型文件路径
        """
        try:
            # 初始化推理会话（自动选择可用硬件）
            self.session = ort.InferenceSession(model_path)
            self.input_name = self.session.get_inputs()[0].name
            self.output_names = [output.name for output in self.session.get_outputs()]
        except Exception as e:
            raise RuntimeError(f"ONNX模型加载失败: {str(e)}")

    def predict(self, features):
        """
        执行单条数据推理
        :param features: 预处理后的特征数据
        :return: (进站预测值, 出站预测值)
        """
        # 转换为ONNX需要的输入格式[5,7](@ref)
        input_data = np.array([features], dtype=np.float32)  # 添加batch维度

        # 执行推理[5,6](@ref)
        outputs = self.session.run(
            self.output_names,
            {self.input_name: input_data}
        )

        # 解析输出（假设第一个输出为进站量，第二个为出站量）
        return outputs[0][0], outputs[1][0]
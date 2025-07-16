import numpy as np
import onnxruntime as ort
from datetime import datetime, timedelta
from typing import Dict, List, Tuple
import pandas as pd

class MetroPredictor:
    def __init__(self):
        # 初始化ONNX运行时会话
        self.in_count_model = ort.InferenceSession("app/models/in_count_model.onnx")
        self.out_count_model = ort.InferenceSession("app/models/out_count_model.onnx")
        
        # 获取模型输入输出信息
        self.in_count_input_name = self.in_count_model.get_inputs()[0].name
        self.out_count_input_name = self.out_count_model.get_inputs()[0].name



    def preprocess_data(self, metro_data: List[Dict]) -> Tuple[np.ndarray, np.ndarray]:
        # 1. 转换为DataFrame
        df = pd.DataFrame(metro_data)
        
        # 2. 时间特征转换（严格复制训练时的处理）
        # 确保日期和时间槽是datetime类型
        df['date'] = pd.to_datetime(df['date'])
        df['time_slot'] = pd.to_datetime(df['time_slot'])
        
        # 2.1 生成minutes特征（小时*100+分钟）
        df['minutes'] = df['time_slot'].apply(
            lambda ts: ts.hour * 100 + ts.minute
        )
        
        # 2.2 生成day_of_week特征（周一=0，周日=6）
        df['day_of_week'] = df['date'].apply(
            lambda d: d.weekday()
        )
        
        # 3. 按站点分组创建滞后特征
        base_features = [
            'in_count', 'out_count', 'temperature', 
            'humidity', 'wind_speed', 'minutes', 'day_of_week'
        ]
        
        # 3.1 定义滞后特征创建函数
        def create_lag_features(group):
            group = group.sort_values('date')  # 确保时间顺序
            for i in range(1, 7):  # 滞后1-6天
                for feature in base_features:
                    group[f'lag{i}_{feature}'] = group[feature].shift(i)
            return group
        
        # 3.2 应用滞后特征生成
        lag_df = df.groupby('station').apply(create_lag_features).reset_index(drop=True)
        
        # 4. 构造42维特征数组
        lag_columns = [
            f'lag{i}_{feature}' 
            for i in range(1, 7) 
            for feature in base_features
        ]
        
        # 按训练时的特征顺序排序
        features = lag_df[lag_columns].values.astype(np.float32)
        
        # 5. 处理NaN值（复制训练时的方法）
        # 训练时使用：df = df.dropna()
        # 但在预测时我们不能直接删除，因为每条记录对应一个时间点
        # 解决方案：用列均值填充（需确保训练时也用了相同策略）
        col_means = np.nanmean(features, axis=0)
        nan_indices = np.isnan(features)
        features[nan_indices] = np.take(col_means, np.where(nan_indices)[1])
        
        # 6. 时间信息（用于结果映射）
        time_info = []
        for _, row in lag_df.iterrows():
            time_info.append({
                'date': row['date'],
                'time_slot': row['time_slot'],
                'station': row['station']
            })
        
        return features, time_info

    def predict(self, metro_data: List[Dict]) -> List[Dict]:
        """执行预测（带调试信息）"""
        try:
            print("[DEBUG] 开始预处理...")
            features, time_info = self.preprocess_data(metro_data)
            
            # 调试信息：检查特征和时间信息
            print(f"[DEBUG] 特征维度: {features.shape}")
            print(f"[DEBUG] 时间信息长度: {len(time_info)}")
            print(f"[DEBUG] 时间信息类型: {type(time_info)}")
            
            if len(time_info) > 0:
                print(f"[DEBUG] 第一条时间信息类型: {type(time_info[0])}")
                print(f"[DEBUG] 第一条时间信息内容: {time_info[0]}")
            
            # 维度验证
            expected_dim = self.in_count_model.get_inputs()[0].shape[1]
            if features.shape[1] != expected_dim:
                raise ValueError(
                    f"特征维度错误! 需要{expected_dim}维，得到{features.shape[1]}维"
                )
            
            print("[DEBUG] 执行进站预测...")
            in_count_pred = self.in_count_model.run(
                None, {'input': features}
            )[0].flatten()
            
            print("[DEBUG] 执行出站预测...")
            out_count_pred = self.out_count_model.run(
                None, {'input': features}
            )[0].flatten()
            
            print(f"[DEBUG] 预测结果长度: {len(in_count_pred)}")
            
            # 构建结果
            results = []
            for i in range(len(time_info)):
                try:
                    # 详细调试信息
                    print(f"[DEBUG] 处理第 {i} 条记录")
                    print(f"[DEBUG] 时间信息内容: {time_info[i]}")
                    print(f"[DEBUG] 类型: {type(time_info[i])}")
                    
                    # 尝试多种访问方式
                    if isinstance(time_info[i], tuple):
                        print("[DEBUG] 使用元组访问方式")
                        record = {
                            'date': time_info[i][0],
                            'time_slot': time_info[i][1],
                            'station': time_info[i][2],
                            'predicted_in_count': max(0, int(in_count_pred[i])),
                            'predicted_out_count': max(0, int(out_count_pred[i]))
                        }
                    elif isinstance(time_info[i], dict):
                        print("[DEBUG] 使用字典访问方式")
                        record = {
                            'date': time_info[i]['date'],
                            'time_slot': time_info[i]['time_slot'],
                            'station': time_info[i]['station'],
                            'predicted_in_count': max(0, int(in_count_pred[i])),
                            'predicted_out_count': max(0, int(out_count_pred[i]))
                        }
                    else:
                        # 未知类型处理
                        print("[DEBUG] 使用通用访问方式")
                        record = {
                            'date': str(time_info[i][0]) if hasattr(time_info[i], '__getitem__') else str(time_info[i]),
                            'time_slot': str(time_info[i][1]) if hasattr(time_info[i], '__getitem__') else "",
                            'station': str(time_info[i][2]) if hasattr(time_info[i], '__getitem__') else "",
                            'predicted_in_count': max(0, int(in_count_pred[i])),
                            'predicted_out_count': max(0, int(out_count_pred[i]))
                        }
                    
                    results.append(record)
                except Exception as e:
                    # 详细错误信息
                    print(f"[ERROR] 处理第 {i} 条记录时出错: {str(e)}")
                    print(f"[ERROR] 时间信息内容: {time_info[i] if i < len(time_info) else '索引越界'}")
                    print(f"[ERROR] 类型: {type(time_info[i]) if i < len(time_info) else 'N/A'}")
                    raise
            
            return results
        
        except Exception as e:
            print(f"[FATAL ERROR] 预测失败: {str(e)}")
            print(f"特征维度: {features.shape if 'features' in locals() else '未定义'}")
            print(f"时间信息长度: {len(time_info) if 'time_info' in locals() else '未定义'}")
            if 'time_info' in locals() and len(time_info) > 0:
                print(f"第一条时间信息: {time_info[0]}")
            raise

if __name__ == "__main__":
    print("prediction.py loaded successfully")
    print("MetroPredictor defined:", "MetroPredictor" in globals())
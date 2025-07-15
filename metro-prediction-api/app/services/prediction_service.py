from app import db
from app.models.metro_data import MetroData
from app.models.metro_predict import MetroPredict
from app.utils.feature_engineer import prepare_features
from datetime import datetime, timedelta

class PredictionService:
    def __init__(self, model):
        self.model = model  # 注入预测模型

    def batch_predict(self, start_date, end_date, station=None):
        """批量预测指定日期范围和站点的流量"""
        # 1. 查询原始数据 [6](@ref)
        query = MetroData.query.filter(
            MetroData.date.between(start_date, end_date)
        )

        if station:
            if isinstance(station, list):
                query = query.filter(MetroData.station.in_(station))
            else:
                query = query.filter(MetroData.station == station)

        raw_data = query.all()

        # 2. 准备预测数据
        predictions = []
        for record in raw_data:
            features = prepare_features(record)

            # 3. 使用模型预测 [6](@ref)
            # 假设模型返回格式: (in_count_pred, out_count_pred)
            in_pred, out_pred = self.model.predict(features)

            # 4. 创建预测记录
            predictions.append(MetroPredict(
                date=record.date,
                station=record.station,
                district=record.district,
                time_slot=record.time_slot,
                in_count=int(in_pred),
                out_count=int(out_pred)
            ))

        # 5. 批量保存预测结果 [6,8](@ref)
        db.session.bulk_save_objects(predictions)
        try:
            db.session.commit()
            return len(predictions)
        except Exception as e:
            db.session.rollback()
            raise RuntimeError(f"预测结果保存失败: {str(e)}")

    def predict_single(self, data_record):
        """预测单条数据记录"""
        features = prepare_features(data_record)
        in_pred, out_pred = self.model.predict(features)

        return {
            'date': data_record.date,
            'station': data_record.station,
            'time_slot': data_record.time_slot.strftime('%H:%M'),
            'predicted_in': int(in_pred),
            'predicted_out': int(out_pred)
        }
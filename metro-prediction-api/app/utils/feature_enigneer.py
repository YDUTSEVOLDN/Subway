from datetime import datetime

def prepare_features(data_record):
    """将原始数据转换为模型输入特征"""
    return {
        'date': data_record.date,
        'station': data_record.station,
        'district': data_record.district,
        'hour': data_record.time_slot.hour,
        'minute': data_record.time_slot.minute,
        'is_weekend': 1 if data_record.date.weekday() >= 5 else 0,
        'is_transfer': data_record.is_transfer,
        'temperature': float(data_record.temperature) if data_record.temperature else 0.0,
        'humidity': float(data_record.humidity) if data_record.humidity else 0.0,
        'wind_speed': float(data_record.wind_speed) if data_record.wind_speed else 0.0
    }
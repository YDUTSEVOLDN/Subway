from flask import Blueprint, request, jsonify
from app.services.prediction_service import PredictionService
from app import db
from app.models.metro_data import MetroData
from datetime import datetime

metro_bp = Blueprint('metro', __name__, url_prefix='/metro')

# 初始化预测服务 (实际应用中应在工厂函数中初始化)
# 假设my_model已在某处加载
from your_model_module import load_model
prediction_service = PredictionService(load_model())

@metro_bp.route('/predict', methods=['POST'])
def predict_metro_traffic():
    """地铁流量预测API端点"""
    data = request.json

    # 参数解析
    start_date = datetime.strptime(data['start_date'], '%Y-%m-%d').date()
    end_date = datetime.strptime(data['end_date'], '%Y-%m-%d').date()
    stations = data.get('stations')  # 可选站点列表

    try:
        # 执行批量预测 [6](@ref)
        count = prediction_service.batch_predict(start_date, end_date, stations)
        return jsonify({
            'status': 'success',
            'predicted_records': count,
            'message': f'成功生成{count}条预测记录'
        }), 200
    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500

@metro_bp.route('/predict/single', methods=['POST'])
def predict_single_record():
    """单条数据预测API"""
    data = request.json

    # 创建临时数据对象
    record = MetroData(
        date=datetime.strptime(data['date'], '%Y-%m-%d').date(),
        station=data['station'],
        district=data['district'],
        time_slot=datetime.strptime(data['time_slot'], '%H:%M').time(),
        is_transfer=data.get('is_transfer', 0),
        temperature=data.get('temperature'),
        humidity=data.get('humidity'),
        wind_speed=data.get('wind_speed')
    )

    try:
        result = prediction_service.predict_single(record)
        return jsonify({
            'status': 'success',
            'prediction': result
        }), 200
    except Exception as e:
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500
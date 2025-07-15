from flask import jsonify, request
from app.api import bp
from app.services.data_service import MetroDataService

# 初始化服务
data_service = MetroDataService()

@bp.route('/metro/data/historical-3days', methods=['GET'])
def get_historical_3days():
    """获取历史三天的地铁流量数据 - 老方法"""
    try:
        station = request.args.get('station')

        # 获取三天历史数据
        result = data_service.get_historical_data_3days(station)

        return jsonify({
            'success': True,
            'data': result,
            'message': f'获取{"指定站点" if station else "所有站点"}历史3天数据成功'
        })

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'message': '获取历史数据失败'
        }), 500

@bp.route('/metro/stations', methods=['GET'])
def get_stations():
    """获取所有地铁站点"""
    try:
        stations = data_service.get_all_stations()
        return jsonify({
            'success': True,
            'data': stations,
            'count': len(stations),
            'message': '获取站点列表成功'
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'message': '获取站点列表失败'
        }), 500

@bp.route('/metro/data/summary', methods=['GET'])
def get_data_summary():
    """获取数据概览"""
    try:
        summary = data_service.get_data_summary()
        return jsonify({
            'success': True,
            'data': summary,
            'message': '获取数据概览成功'
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'message': '获取数据概览失败'
        }), 500

@bp.route('/metro/data/raw', methods=['GET'])
def get_raw_data():
    """获取原始数据 - 用于调试"""
    try:
        station = request.args.get('station')
        limit = request.args.get('limit', 10, type=int)

        from app.models.metro_data import MetroHistoricalData

        query = MetroHistoricalData.query
        if station:
            query = query.filter(MetroHistoricalData.station == station)

        records = query.order_by(MetroHistoricalData.date.desc(),
                                 MetroHistoricalData.time_slot.desc()).limit(limit).all()

        return jsonify({
            'success': True,
            'data': [record.to_dict() for record in records],
            'count': len(records),
            'message': '获取原始数据成功'
        })

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'message': '获取原始数据失败'
        }), 500
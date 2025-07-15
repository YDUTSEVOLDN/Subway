from app.models.metro_data import MetroHistoricalData
from app import db
from datetime import datetime, timedelta
from sqlalchemy import and_, func
import random

class MetroDataService:
    """地铁数据服务类 - 实现老方法加载数据"""

    def get_historical_data_3days(self, station=None):
        """
        获取历史三天的地铁流量数据 - 老方法格式
        返回 [3天][24小时][特征数] 的数据
        """
        try:
            # 计算日期范围：从昨天往回数3天
            end_date = datetime.now().date() - timedelta(days=1)  # 昨天
            start_date = end_date - timedelta(days=2)  # 往前3天

            print(f"获取历史数据范围: {start_date} 到 {end_date}")

            # 从数据库查询数据
            query = MetroHistoricalData.query.filter(
                and_(
                    MetroHistoricalData.date >= start_date,
                    MetroHistoricalData.date <= end_date
                )
            )

            if station:
                query = query.filter(MetroHistoricalData.station == station)
                print(f"查询站点 '{station}' 的数据")
            else:
                print("查询所有站点的数据")

            raw_data = query.order_by(
                MetroHistoricalData.date.asc(),
                MetroHistoricalData.time_slot.asc()
            ).all()

            print(f"共找到 {len(raw_data)} 条记录")

            # 转换为模型输入格式
            return self._convert_to_model_input(raw_data, start_date, end_date, station)

        except Exception as e:
            print(f"获取历史数据失败: {str(e)}")
            # 返回模拟数据作为fallback
            return self._generate_mock_data(station)

    def _convert_to_model_input(self, raw_data, start_date, end_date, station):
        """将数据库数据转换为模型输入格式"""

        # 特征数量：in_count, out_count, temperature, humidity, wind_speed, is_transfer
        feature_count = 6
        days_count = 3
        hours_count = 24

        # 初始化输出数组
        data = [[[0.0 for _ in range(feature_count)]
                 for _ in range(hours_count)]
                for _ in range(days_count)]

        # 按日期和小时分组数据
        data_map = {}
        for record in raw_data:
            key = f"{record.date}_{record.time_slot.hour}"
            if key not in data_map:
                data_map[key] = []
            data_map[key].append(record)

        # 填充数据
        for day in range(days_count):
            current_date = start_date + timedelta(days=day)

            for hour in range(hours_count):
                key = f"{current_date}_{hour}"
                hour_data = data_map.get(key, [])

                if hour_data:
                    # 有数据：计算平均值（如果是多个站点）或使用单站点数据
                    self._fill_data_from_records(data[day][hour], hour_data, station is None)
                else:
                    # 缺失数据：使用插值或默认值
                    self._fill_missing_data(data[day][hour], day, hour, data)

        # 数据归一化（可选）
        self._normalize_data(data)

        return {
            'data': data,
            'start_date': start_date.isoformat(),
            'end_date': end_date.isoformat(),
            'station': station,
            'shape': [days_count, hours_count, feature_count],
            'features': ['in_count', 'out_count', 'temperature', 'humidity', 'wind_speed', 'is_transfer']
        }

    def _fill_data_from_records(self, hour_features, records, use_average):
        """从数据库记录填充特征数据"""
        if use_average:
            # 计算多个站点的平均值
            avg_in_count = sum(r.in_count for r in records) / len(records)
            avg_out_count = sum(r.out_count for r in records) / len(records)
            avg_temperature = sum(float(r.temperature) for r in records if r.temperature) / len([r for r in records if r.temperature]) if any(r.temperature for r in records) else 20.0
            avg_humidity = sum(float(r.humidity) for r in records if r.humidity) / len([r for r in records if r.humidity]) if any(r.humidity for r in records) else 50.0
            avg_wind_speed = sum(float(r.wind_speed) for r in records if r.wind_speed) / len([r for r in records if r.wind_speed]) if any(r.wind_speed for r in records) else 3.0
            avg_transfer = sum(r.is_transfer for r in records) / len(records)

            hour_features[0] = avg_in_count
            hour_features[1] = avg_out_count
            hour_features[2] = avg_temperature
            hour_features[3] = avg_humidity
            hour_features[4] = avg_wind_speed
            hour_features[5] = avg_transfer

        else:
            # 使用单个站点的数据（取第一条记录）
            record = records[0]

            hour_features[0] = float(record.in_count)
            hour_features[1] = float(record.out_count)
            hour_features[2] = float(record.temperature) if record.temperature else 20.0
            hour_features[3] = float(record.humidity) if record.humidity else 50.0
            hour_features[4] = float(record.wind_speed) if record.wind_speed else 3.0
            hour_features[5] = float(record.is_transfer)

    def _fill_missing_data(self, hour_features, day, hour, all_data):
        """填充缺失数据"""
        # 策略1：使用同一小时前一天的数据
        if day > 0:
            hour_features[:] = all_data[day - 1][hour][:]
            return

        # 策略2：使用相邻小时的数据
        if hour > 0:
            hour_features[:] = all_data[day][hour - 1][:]
            return

        # 策略3：使用默认值（基于时间的估算）
        if hour in [7, 8, 17, 18, 19]:  # 高峰期
            hour_features[0] = 800 + random.random() * 400  # 进站
            hour_features[1] = 750 + random.random() * 350  # 出站
        elif hour >= 22 or hour <= 5:  # 深夜/凌晨
            hour_features[0] = 50 + random.random() * 100
            hour_features[1] = 60 + random.random() * 120
        else:  # 平峰期
            hour_features[0] = 200 + random.random() * 300
            hour_features[1] = 220 + random.random() * 280

        # 默认天气数据
        hour_features[2] = 20.0  # 温度
        hour_features[3] = 50.0  # 湿度
        hour_features[4] = 3.0   # 风速
        hour_features[5] = 0.5   # 换乘比例

        print(f"使用默认数据填充: Day {day}, Hour {hour}")

    def _normalize_data(self, data):
        """数据归一化（可选）"""
        days_count = len(data)
        hours_count = len(data[0])

        for day in range(days_count):
            for hour in range(hours_count):
                # 流量数据缩放到0-1
                data[day][hour][0] = min(data[day][hour][0] / 2000.0, 1.0)  # 进站
                data[day][hour][1] = min(data[day][hour][1] / 2000.0, 1.0)  # 出站

                # 温度归一化到0-1 (假设-10到40度)
                data[day][hour][2] = (data[day][hour][2] + 10.0) / 50.0

                # 湿度已经是百分比，缩放到0-1
                data[day][hour][3] = data[day][hour][3] / 100.0

                # 风速归一化 (假设0-20m/s)
                data[day][hour][4] = min(data[day][hour][4] / 20.0, 1.0)

                # 换乘标志已经是0-1
                # data[day][hour][5] 保持不变

    def _generate_mock_data(self, station):
        """生成模拟数据作为fallback"""
        print("数据库无数据，使用模拟数据")

        feature_count = 6
        days_count = 3
        hours_count = 24

        data = [[[0.0 for _ in range(feature_count)]
                 for _ in range(hours_count)]
                for _ in range(days_count)]

        for day in range(days_count):
            for hour in range(hours_count):
                # 模拟早晚高峰流量模式
                if hour in [7, 8, 17, 18, 19]:
                    data[day][hour][0] = 800 + random.random() * 400  # 高峰期进站
                    data[day][hour][1] = 750 + random.random() * 350  # 高峰期出站
                else:
                    data[day][hour][0] = 200 + random.random() * 300  # 平峰期进站
                    data[day][hour][1] = 220 + random.random() * 280  # 平峰期出站

                # 模拟天气数据
                data[day][hour][2] = 20 + random.random() * 10  # 温度
                data[day][hour][3] = 45 + random.random() * 20  # 湿度
                data[day][hour][4] = 2 + random.random() * 4    # 风速
                data[day][hour][5] = random.choice([0, 1])      # 换乘

        return {
            'data': data,
            'start_date': (datetime.now().date() - timedelta(days=3)).isoformat(),
            'end_date': (datetime.now().date() - timedelta(days=1)).isoformat(),
            'station': station,
            'shape': [days_count, hours_count, feature_count],
            'features': ['in_count', 'out_count', 'temperature', 'humidity', 'wind_speed', 'is_transfer'],
            'source': 'mock_data'
        }

    def get_all_stations(self):
        """获取所有站点列表"""
        try:
            stations = db.session.query(
                MetroHistoricalData.station,
                MetroHistoricalData.district
            ).distinct().order_by(MetroHistoricalData.station).all()

            return [
                {"name": station, "district": district}
                for station, district in stations
            ]
        except Exception as e:
            print(f"获取站点列表失败: {str(e)}")
            return [
                {"name": "七里庄", "district": "丰台区"},
                {"name": "六里桥", "district": "丰台区"},
                {"name": "公主坟", "district": "海淀区"}
            ]

    def get_data_summary(self):
        """获取数据概览"""
        try:
            # 统计总记录数
            total_records = MetroHistoricalData.query.count()

            # 统计日期范围
            date_range = db.session.query(
                func.min(MetroHistoricalData.date).label('min_date'),
                func.max(MetroHistoricalData.date).label('max_date')
            ).first()

            # 统计站点数量
            station_count = db.session.query(MetroHistoricalData.station).distinct().count()

            return {
                'total_records': total_records,
                'date_range': {
                    'start': date_range.min_date.isoformat() if date_range.min_date else None,
                    'end': date_range.max_date.isoformat() if date_range.max_date else None
                },
                'station_count': station_count,
                'features': ['in_count', 'out_count', 'temperature', 'humidity', 'wind_speed', 'is_transfer']
            }
        except Exception as e:
            print(f"获取数据概览失败: {str(e)}")
            return {
                'total_records': 0,
                'date_range': {'start': None, 'end': None},
                'station_count': 0,
                'error': str(e)
            }
from datetime import date, datetime, timedelta
from sqlalchemy.sql import func
from typing import Generator, List, Dict, Optional

from app.database.connection import db_connection
from app.database.models import MetroHistoricalData

class DataLoader:
    """
    完整版数据加载器，包含所有特征工程所需字段
    """
    
    def __init__(self):
        """初始化并验证模型属性"""
        # 包含所有必要字段（进站/出站客流量 + 风速）
        required_attrs = ['date', 'time_slot', 'station', 
                         'in_count', 'out_count',  # 客流统计
                         'temperature', 'humidity', 'wind_speed',  # 天气字段
                         'is_transfer']  # 站点属性
        
        # 验证所有必要属性存在
        for attr in required_attrs:
            if not hasattr(MetroHistoricalData, attr):
                raise AttributeError(f'模型缺少必要属性: {attr}')
    
    def load_metro_data(self, 
                       start_date: Optional[date] = None, 
                       end_date: Optional[date] = None, 
                       stations: Optional[List[str]] = None, 
                       batch_size: int = 1000) -> Generator[List[Dict], None, None]:
        """
        加载地铁历史数据（包含特征工程所需的所有字段）
        返回生成器分批产生数据字典列表
        
        :param start_date: 开始日期
        :param end_date: 结束日期
        :param stations: 特定站点列表
        :param batch_size: 每批数据量
        :return: 生成器每次返回一批数据字典列表
        """
        session = db_connection.get_session()
        try:
            # 核心查询 - 包含所有必要字段
            query = session.query(
                MetroHistoricalData.date,          # 日期
                MetroHistoricalData.time_slot,     # 时间槽
                MetroHistoricalData.station,       # 站点
                MetroHistoricalData.in_count,      # 进站客流量
                MetroHistoricalData.out_count,     # 出站客流量
                MetroHistoricalData.temperature,  # 温度
                MetroHistoricalData.humidity,     # 湿度
                MetroHistoricalData.wind_speed,    # 风速 ★ 添加此字段
                MetroHistoricalData.is_transfer    # 是否换乘站
            )
            
            # 日期过滤条件（处理字符串输入）
            if start_date:
                if isinstance(start_date, str):
                    start_date = datetime.strptime(start_date, '%Y-%m-%d').date()
                query = query.filter(MetroHistoricalData.date >= start_date)
                
            if end_date:
                if isinstance(end_date, str):
                    end_date = datetime.strptime(end_date, '%Y-%m-%d').date()
                query = query.filter(MetroHistoricalData.date <= end_date)
            
            # 站点过滤条件（处理大小写不敏感）
            if stations:
                query = query.filter(
                    func.lower(MetroHistoricalData.station).in_(
                        [s.lower() for s in stations]
                    )
                )
            
            # 按日期和时间排序
            query = query.order_by(
                MetroHistoricalData.date,
                MetroHistoricalData.time_slot
            )
            
            # 分批加载
            offset = 0
            while True:
                batch = query.offset(offset).limit(batch_size).all()
                if not batch:
                    break
                
                # 将查询结果转换为字典列表
                batch_dicts = []
                for row in batch:
                    # 解包所有查询字段
                    (record_date, record_time_slot, station, 
                     in_count, out_count,  
                     temperature, humidity, wind_speed,  # ★ 包含wind_speed
                     is_transfer) = row
                    
                    batch_dicts.append({
                        'date': record_date.isoformat() if record_date else None,
                        'time_slot': str(record_time_slot) if record_time_slot else None,
                        'station': station,
                        'in_count': int(in_count) if in_count is not None else 0,
                        'out_count': int(out_count) if out_count is not None else 0,
                        'temperature': float(temperature) if temperature is not None else 0.0,
                        'humidity': float(humidity) if humidity is not None else 0.0,
                        'wind_speed': float(wind_speed) if wind_speed is not None else 0.0,  # ★ 添加此字段
                        'is_transfer': bool(is_transfer)  # 确保为布尔值
                    })
                
                yield batch_dicts
                offset += batch_size
                
        except Exception as e:
            # 详细的错误日志
            print(f"[ERROR] 数据加载失败: {str(e)}")
            # 重新抛出异常供上层处理
            raise
        finally:
            # 确保会话关闭
            db_connection.close_session(session)

    def load_last_n_days_data(self, days: int = 6, batch_size: int = 1000) -> Generator[List[Dict], None, None]:
        """
        加载最近N天的数据（便捷方法）
        
        :param days: 天数
        :param batch_size: 每批数据量
        :return: 生成器每次返回一批数据字典列表
        """
        end_date = date.today()
        start_date = end_date - timedelta(days=days)
        
        # 调用核心加载方法
        return self.load_metro_data(
            start_date=start_date, 
            end_date=end_date, 
            batch_size=batch_size
        )
